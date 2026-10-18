#!/usr/bin/env python3
"""Generate the 3D Skill Book's textures and model files (1.82.0).

usage: python3 tools/lines/gen_book_textures.py [--check]

writes (deterministic, no clock, no global randomness):
  assets/nusmp/textures/item/skill_book_3d/base.png          64 x 64   pages, gold plate, bronze, ribbon cloth, the clasp stone
  assets/nusmp/textures/item/skill_book_3d/<franchise>.png   128 x 128 the cover (GREYS ONLY: the client multiplies it by the line colour), one per franchise + generic.png
  assets/nusmp/models/item/skill_book_3d.json                the 3D book (cover texture: generic)
  assets/nusmp/models/item/skill_book_3d_<franchise>.json    the same body with the franchise's cover
  assets/nusmp/models/item/skill_book_st_<franchise>.json    neoforge:separate_transforms: that body in hand/ground/frames, the OLD sprite (skill_book_2d) in the GUI
  assets/nusmp/models/item/skill_book_2d.json                the 1.62.0 sprite model, byte for byte (the old skill_book.json), kept as the inventory icon
  assets/nusmp/models/item/skill_book.json                   the registered item's model: separate_transforms with one override per franchise (item property nusmp:franchise)
--check  compares the decoded pixels / parsed json with what is on disk and fails on a difference (the compressed bytes may differ between zlib versions, the pixels may not).

WHY a script: eleven covers that share a layout, a palette discipline (greys, so the tint is clean) and an embossing recipe are one function and eleven emblem drawings; by hand they
would drift apart. The geometry lives in book_model.py so the previewer reads the same numbers.
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
sys.path.insert(0, HERE)
import book_model as BM  # noqa: E402
from lines_lib import FRANCHISES  # noqa: E402

TEX_DIR = os.path.join(ASSETS, "textures", "item", "skill_book_3d")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
FRANCHISE_ORDER = list(FRANCHISES.keys())          # the order IS the value of the item property nusmp:franchise (index + 1) / PROP_STEPS: new franchises are only ever appended
assert len(FRANCHISE_ORDER) + 1 <= 20, "the item property (index + 1) / 20 must stay at or below 1: raise PROP_STEPS in gen_book_textures.py and gen_book_index.py before adding the 20th franchise"
PROP_STEPS = 20.0
# the 1.62.0 model of the Skill Book, kept as skill_book_2d.json (its text must stay what it was)
OLD_MODEL = {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/skill_book", "layer1": "nusmp:item/skill_book_cover"}}


# =========================================================================================== base atlas
def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def put(img, rect, arr):
    x0, y0, x1, y1 = rect
    img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA"), (x0, y0))


def base_atlas():
    rng = np.random.default_rng(1182)
    img = Image.new("RGBA", (BM.BASE_SIZE, BM.BASE_SIZE), (0, 0, 0, 0))
    # pages: a stack of sheets seen on edge; one sheet per pixel column/row, a darker gap every fourth, a little fibre noise
    for name, vertical in (("pageV", True), ("pageH", False)):
        x0, y0, x1, y1 = BM.BASE_REGIONS[name]
        w, h = x1 - x0, y1 - y0
        a = np.zeros((h, w, 4)); a[..., 3] = 255
        for i in range(h * w):
            y, x = divmod(i, w)
            k = x if vertical else y
            shade = 238 - (k % 4 == 3) * 26 - (k % 8 == 7) * 10 - (k % 2) * 6
            shade += rng.integers(-5, 6)
            a[y, x, :3] = (shade, shade - 10, shade - 42)
        edge = a.copy()
        # a faint stain on the outer ends of the stripes (the paper darkens where fingers touch it)
        if vertical:
            a[:2, :, :3] *= 0.92; a[-2:, :, :3] *= 0.92
        else:
            a[:, :2, :3] *= 0.92; a[:, -2:, :3] *= 0.92
        put(img, BM.BASE_REGIONS[name], a)
    x0, y0, x1, y1 = BM.BASE_REGIONS["paper"]
    a = np.zeros((y1 - y0, x1 - x0, 4)); a[..., 3] = 255
    base = np.array([232, 220, 186])
    a[..., :3] = base + rng.integers(-6, 7, (y1 - y0, x1 - x0, 1))
    put(img, BM.BASE_REGIONS["paper"], a)
    # ribbon: a grey cloth with a weave (tinted by the client, so grey only)
    x0, y0, x1, y1 = BM.BASE_REGIONS["ribbon"]
    a = np.zeros((y1 - y0, x1 - x0, 4)); a[..., 3] = 255
    for y in range(y1 - y0):
        for x in range(x1 - x0):
            v = 214 - ((x + y) % 2) * 22 - (x in (0, x1 - x0 - 1)) * 34
            a[y, x, :3] = v
    put(img, BM.BASE_REGIONS["ribbon"], a)
    # gem: a faceted cut stone, pale arcane cyan
    x0, y0, x1, y1 = BM.BASE_REGIONS["gem"]
    a = np.zeros((y1 - y0, x1 - x0, 4)); a[..., 3] = 255
    for y in range(8):
        for x in range(8):
            cx, cy = x - 3.5, y - 3.5
            ang = math.atan2(cy, cx)
            facet = int((ang + math.pi) / (math.pi / 3)) % 3
            d = max(abs(cx), abs(cy))
            base = [(110, 214, 255), (150, 232, 255), (70, 170, 226)][facet]
            k = 1.0 - 0.07 * d
            a[y, x, :3] = [min(255, c * k) for c in base]
    a[1:3, 1:3, :3] = (235, 252, 255)
    put(img, BM.BASE_REGIONS["gem"], a)
    # gold plate and dark bronze plate: a bevel (light top-left, dark bottom-right), brushed grain, four rivets
    for name, (hi, mid, lo) in (("metal", ((255, 232, 150), (206, 156, 62), (112, 74, 24))), ("metalDark", ((176, 140, 96), (116, 84, 48), (60, 40, 22)))):
        x0, y0, x1, y1 = BM.BASE_REGIONS[name]
        w, h = x1 - x0, y1 - y0
        a = np.zeros((h, w, 4)); a[..., 3] = 255
        for y in range(h):
            grain = rng.integers(-7, 8)
            for x in range(w):
                c = np.array(mid, dtype=float) + grain + rng.integers(-4, 5)
                a[y, x, :3] = c
        for i in range(3):
            a[i, i:w - i, :3] = np.array(hi) * (1 - i * 0.12)               # top bevel
            a[i:h - i, i, :3] = np.array(hi) * (1 - i * 0.12)               # left bevel
            a[h - 1 - i, i:w - i, :3] = np.array(lo) * (1 + i * 0.25)       # bottom bevel
            a[i:h - i, w - 1 - i, :3] = np.array(lo) * (1 + i * 0.25)       # right bevel
        for (rx, ry) in ((5, 5), (w - 6, 5), (5, h - 6), (w - 6, h - 6)):
            a[ry - 1:ry + 2, rx - 1:rx + 2, :3] = np.array(lo) * 0.9
            a[ry - 1, rx - 1, :3] = np.array(hi)
            a[ry, rx, :3] = np.array(mid) * 1.1
        put(img, BM.BASE_REGIONS[name], a)
    return img


# =========================================================================================== covers
def leather(rng, w, h, grain):
    """A neutral grey leather: soft vignette, fine grain, a few darker creases. Mean about 185, never below 104."""
    yy, xx = np.mgrid[0:h, 0:w]
    cx, cy = (w - 1) / 2, (h - 1) / 2
    d = np.maximum(abs(xx - cx) / cx, abs(yy - cy) / cy)
    base = 200 - 40 * d ** 2.2
    n = rng.integers(-grain, grain + 1, (h, w)).astype(float)
    n2 = np.asarray(Image.fromarray(rng.integers(0, 255, (h // 3 + 2, w // 3 + 2)).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255 - 0.5
    return base + n + n2 * 2.0 * grain


def emboss(canvas, mask, raised=True, amount=1.0):
    """Presses a 0/255 L mask into the cover as a relief: top-left edges catch the light (up to 255), bottom-right edges fall into shade, the face of the relief is lifted a little."""
    m = np.asarray(mask, float) / 255.0
    h, w = m.shape
    up = np.roll(np.roll(m, 1, 0), 1, 1)                  # the mask moved one pixel down-right
    down = np.roll(np.roll(m, -1, 0), -1, 1)              # and up-left
    light = np.clip(m - up, 0, 1)                         # pixels whose up-left neighbour is empty: the lit edge
    shade = np.clip(m - down, 0, 1)                       # pixels whose down-right neighbour is empty: the shaded edge
    sign = 1.0 if raised else -1.0
    out = canvas + sign * amount * (m * 34 + light * 90 - shade * 60)
    return out


def ring(mask_draw, cx, cy, r, width):
    mask_draw.ellipse((cx - r, cy - r, cx + r, cy + r), outline=255, width=width)


def poly(d, pts, fill=255):
    d.polygon([(float(x), float(y)) for x, y in pts], fill=fill)


def heart(d, cx, cy, size, rot, fill=255):
    pts = []
    for i in range(48):
        t = i / 48 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        c, s = math.cos(rot), math.sin(rot)
        pts.append((cx + size * (x * c - y * s), cy + size * (x * s + y * c)))
    poly(d, pts, fill)


def star(d, cx, cy, r_out, r_in, n=5, rot=-math.pi / 2, fill=255):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + i * math.pi / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    poly(d, pts, fill)


def bezier(p0, p1, p2, p3, n=40):
    out = []
    for i in range(n + 1):
        t = i / n
        a, b, c, e = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t ** 2, t ** 3
        out.append((a * p0[0] + b * p1[0] + c * p2[0] + e * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + e * p3[1]))
    return out


def brush(d, pts, r0, r1, fill=255):
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        r = r0 + (r1 - r0) * i / max(1, n - 1)
        d.ellipse((x - r, y - r, x + r, y + r), fill=fill)


# ---- the emblems: each draws into a 160 x 160 mask (white = relief) --------------------------------------------------------------
def em_exiled_heavy_knight(d):
    poly(d, [(28, 16), (132, 16), (132, 82), (80, 148), (28, 82)])                       # the heater shield
    poly(d, [(40, 28), (120, 28), (120, 78), (80, 130), (40, 78)], 0)                    # its field, cut out
    poly(d, [(75, 34), (85, 34), (88, 98), (80, 118), (72, 98)])                          # the sword
    d.rectangle((54, 60, 106, 70), fill=255)                                              # the cross guard
    d.ellipse((73, 22, 87, 36), fill=255)                                                 # the pommel (touches the rim, it is a seal)


def em_tensura(d):
    d.ellipse((24, 62, 136, 146), fill=255)                                               # the slime
    poly(d, [(80, 14), (56, 70), (104, 70)])                                              # its tip
    d.ellipse((50, 86, 70, 108), fill=0); d.ellipse((90, 86, 110, 108), fill=0)           # eyes
    d.arc((58, 100, 102, 130), 15, 165, fill=0, width=6)                                  # a smile
    d.ellipse((38, 74, 52, 88), fill=0)                                                   # a highlight


def em_black_clover(d):
    for ang in (0, 90, 180, 270):
        a = math.radians(ang - 45)
        heart(d, 80 + 40 * math.cos(a), 80 + 40 * math.sin(a), 1.7, a + math.pi / 2)
    brush(d, bezier((80, 84), (80, 112), (72, 130), (86, 154)), 5, 3)                    # the stem, between the two lower leaves
    d.ellipse((72, 72, 88, 88), fill=0)


def em_magi(d):
    ring(d, 80, 80, 64, 7)
    star(d, 80, 80, 54, 24, 8, -math.pi / 2)                                              # the eight-pointed magoi star
    d.ellipse((66, 66, 94, 94), fill=0)
    d.ellipse((72, 72, 88, 88), fill=255)


def em_solo_leveling(d):
    d.pieslice((20, 20, 140, 150), 180, 360, fill=255)                                    # the gate: an arch
    d.rectangle((20, 85, 140, 148), fill=255)
    d.pieslice((40, 38, 120, 130), 180, 360, fill=0)
    d.rectangle((40, 84, 120, 148), fill=0)
    poly(d, [(80, 46), (88, 66), (84, 112), (80, 126), (76, 112), (72, 66)])             # the dagger
    d.rectangle((66, 100, 94, 106), fill=255)


def em_danmachi(d):
    ring(d, 80, 82, 62, 8)
    brush(d, bezier((80, 128), (46, 104), (66, 76), (78, 34)), 15, 1)                     # the flame, one tongue
    brush(d, bezier((80, 128), (116, 102), (96, 80), (104, 56)), 12, 1)
    poly(d, [(60, 146), (80, 158), (100, 146), (100, 150), (80, 160), (60, 150)])         # a chevron: the way down


def em_fairy_tail(d):
    d.ellipse((18, 22, 78, 82), fill=255)                                                 # the head
    d.ellipse((36, 40, 60, 64), fill=0)
    brush(d, bezier((60, 76), (140, 64), (160, 110), (112, 140)), 20, 4)                 # the tail
    poly(d, [(96, 152), (128, 150), (126, 120)])
    star(d, 124, 34, 22, 8, 4, -math.pi / 2)


def em_tbate(d):
    poly(d, [(48, 22), (112, 22), (112, 30), (48, 30)])                                   # the hourglass
    poly(d, [(48, 138), (112, 138), (112, 130), (48, 130)])
    poly(d, [(54, 30), (106, 30), (86, 80), (106, 130), (54, 130), (74, 80)])
    poly(d, [(64, 38), (96, 38), (80, 70)], 0)
    poly(d, [(66, 124), (94, 124), (80, 100)], 0)
    poly(d, [(54, 14), (62, 0), (72, 12), (80, -2), (88, 12), (98, 0), (106, 14)])         # the crown over it


def em_tower_of_god(d):
    tiers = [(60, 100, 22, 40), (50, 110, 40, 62), (40, 120, 62, 92), (28, 132, 92, 126), (16, 144, 126, 148)]
    for x0, x1, y0, y1 in tiers:
        poly(d, [(x0, y1), (x1, y1), (x1 - 4, y0), (x0 + 4, y0)])
    for x0, x1, y0, y1 in tiers[1:]:
        for i in range(3):
            cx = x0 + (x1 - x0) * (i + 1) / 4
            d.rectangle((cx - 2, y0 + 6, cx + 2, y0 + 14), fill=0)
    star(d, 80, 12, 14, 5, 5, -math.pi / 2)


def em_god_of_high_school(d):
    d.ellipse((24, 24, 136, 136), fill=255)
    d.pieslice((24, 24, 136, 136), -90, 90, fill=0)
    d.ellipse((52, 24, 108, 80), fill=255)
    d.ellipse((52, 80, 108, 136), fill=0)
    d.ellipse((72, 44, 88, 60), fill=0)
    d.ellipse((72, 100, 88, 116), fill=255)
    ring(d, 80, 80, 66, 5)


def em_omniscient_readers_viewpoint(d):
    poly(d, [(14, 66)] + [(80 + 66 * math.cos(math.radians(a)) * 1.0, 66 - 38 * math.sin(math.radians(a))) for a in range(180, -1, -15)][::-1] + [(146, 66)])
    poly(d, [(14, 66)] + [(80 + 66 * math.cos(math.radians(a)), 66 + 38 * math.sin(math.radians(a))) for a in range(180, 361, 15)] + [(146, 66)])
    d.ellipse((56, 36, 104, 96), fill=0)
    d.ellipse((62, 42, 98, 90), fill=255)
    d.ellipse((72, 52, 88, 80), fill=0)
    for a in range(-60, 61, 30):
        x = 80 + 80 * math.sin(math.radians(a)); y = 4 + 0 * a
        brush(d, [(80 + 54 * math.sin(math.radians(a)), 22 - 4 * math.cos(math.radians(a))), (80 + 74 * math.sin(math.radians(a)), 6 + 12 * (1 - math.cos(math.radians(a))))], 3, 2)
    poly(d, [(16, 112), (80, 128), (144, 112), (144, 146), (80, 158), (16, 146)])        # the open book under the eye
    d.line((80, 128, 80, 158), fill=0, width=4)


def em_generic(d):
    ring(d, 80, 80, 62, 7)
    star(d, 80, 82, 46, 19, 5)
    d.ellipse((70, 72, 90, 92), fill=0)


EMBLEMS = {"exiled_heavy_knight": em_exiled_heavy_knight, "tensura": em_tensura, "black_clover": em_black_clover, "magi": em_magi, "solo_leveling": em_solo_leveling,
           "danmachi": em_danmachi, "fairy_tail": em_fairy_tail, "tbate": em_tbate, "tower_of_god": em_tower_of_god, "god_of_high_school": em_god_of_high_school,
           "omniscient_readers_viewpoint": em_omniscient_readers_viewpoint, "generic": em_generic}
# per franchise: how coarse the leather is, how deep the frame bites (the cover's character without a colour cast: the colour comes from the line)
CHARACTER = {"exiled_heavy_knight": dict(grain=9, frame="plain", seed=1), "tensura": dict(grain=3, frame="round", seed=2), "black_clover": dict(grain=7, frame="leaf", seed=3),
             "magi": dict(grain=4, frame="star", seed=4), "solo_leveling": dict(grain=5, frame="plain", seed=5), "danmachi": dict(grain=6, frame="round", seed=6),
             "fairy_tail": dict(grain=5, frame="star", seed=7), "tbate": dict(grain=8, frame="leaf", seed=8), "tower_of_god": dict(grain=4, frame="plain", seed=9),
             "god_of_high_school": dict(grain=7, frame="round", seed=10), "omniscient_readers_viewpoint": dict(grain=3, frame="star", seed=11), "generic": dict(grain=5, frame="plain", seed=0)}


def frame_mask(w, h, kind):
    """The embossed frame of the front cover: a double rule inset from the edge, corner ornaments by kind, a small row of marks on the title plate."""
    s = 4
    m = Image.new("L", (w * s, h * s), 0)
    d = ImageDraw.Draw(m)
    i1, i2 = 4 * s, 7 * s
    d.rectangle((i1, i1, w * s - i1 - 1, h * s - i1 - 1), outline=255, width=2 * s // 2)
    d.rectangle((i2, i2, w * s - i2 - 1, h * s - i2 - 1), outline=255, width=1 * s // 2)
    for cx, cy, sx, sy in ((i2, i2, 1, 1), (w * s - i2, i2, -1, 1), (i2, h * s - i2, 1, -1), (w * s - i2, h * s - i2, -1, -1)):
        r = 6 * s
        if kind == "round":
            d.ellipse((cx - r // 2, cy - r // 2, cx + r // 2, cy + r // 2), fill=255)
        elif kind == "leaf":
            poly(d, [(cx, cy), (cx + sx * r, cy + sy * r // 3), (cx + sx * r // 3, cy + sy * r)])
        elif kind == "star":
            star(d, cx + sx * r // 3, cy + sy * r // 3, r * 0.7, r * 0.28, 4)
        else:
            xa, xb = sorted((cx, cx + sx * r // 2)); ya, yb = sorted((cy, cy + sy * r // 2))
            d.rectangle((xa, ya, xb, yb), fill=255)
    # the title plate under the emblem and a row of three diamonds on it
    px0, px1, py0, py1 = 14 * s, (w - 14) * s, 84 * s, 94 * s
    d.rectangle((px0, py0, px1, py1), outline=255, width=s // 2)
    for k in (-1, 0, 1):
        cx = (w * s) // 2 + k * 14 * s
        poly(d, [(cx, (py0 + py1) // 2 - 3 * s), (cx + 3 * s, (py0 + py1) // 2), (cx, (py0 + py1) // 2 + 3 * s), (cx - 3 * s, (py0 + py1) // 2)])
    return m.resize((w, h), Image.LANCZOS).point(lambda v: 255 if v > 110 else 0)


def emblem_mask(name, w, h):
    s = 4
    big = Image.new("L", (160, 160), 0)
    EMBLEMS[name](ImageDraw.Draw(big))
    size = 52
    small = big.resize((size * s, size * s), Image.LANCZOS)
    m = Image.new("L", (w * s, h * s), 0)
    m.paste(small, ((w * s - size * s) // 2, 14 * s))
    return m.resize((w, h), Image.LANCZOS).point(lambda v: 255 if v > 110 else 0)


def cover_image(name):
    ch = CHARACTER[name]
    rng = np.random.default_rng(1000 + ch["seed"])
    S = BM.COVER_SIZE
    img = np.zeros((S, S), float) + 128
    fx0, fy0, fx1, fy1 = BM.COVER_REGIONS["front"]
    front = leather(rng, fx1 - fx0, fy1 - fy0, ch["grain"])
    fw, fh = fx1 - fx0, fy1 - fy0
    front = emboss(front, frame_mask(fw, fh, ch["frame"]), True, 0.8)
    front = emboss(front, emblem_mask(name, fw, fh), True, 1.0)
    # the dark shadow of the title plate's text row is the plate's own outline; nothing else
    img[fy0:fy1, fx0:fx1] = front
    # spine: leather with a long raised rule on both sides and a diamond ladder between them
    sx0, sy0, sx1, sy1 = BM.COVER_REGIONS["spine"]
    sw, sh = sx1 - sx0, sy1 - sy0
    sp = leather(rng, sw, sh, ch["grain"])
    m = Image.new("L", (sw * 4, sh * 4), 0)
    dm = ImageDraw.Draw(m)
    dm.rectangle((3 * 4, 4 * 4, sw * 4 - 3 * 4, sh * 4 - 4 * 4), outline=255, width=3)
    for k in range(6):
        cy = (14 + k * 15) * 4
        poly(dm, [(sw * 2, cy - 22), (sw * 2 + 22, cy), (sw * 2, cy + 22), (sw * 2 - 22, cy)])
    sp = emboss(sp, np.asarray(m.resize((sw, sh), Image.LANCZOS).point(lambda v: 255 if v > 110 else 0)), True, 0.9)
    img[sy0:sy1, sx0:sx1] = sp
    for key in ("edgeH", "edgeV"):
        x0, y0, x1, y1 = BM.COVER_REGIONS[key]
        e = leather(rng, x1 - x0, y1 - y0, ch["grain"])
        img[y0:y1, x0:x1] = e * 0.82 + 10
    # clamp: never darker than 104 (a tinted pixel must not turn black), highlights up to exactly white
    img = np.clip(img, 104, 255)
    out = np.zeros((S, S, 4), np.uint8)
    v = img.astype(np.uint8)
    out[..., 0] = out[..., 1] = out[..., 2] = v
    out[..., 3] = 0
    for key, (x0, y0, x1, y1) in BM.COVER_REGIONS.items():
        out[y0:y1, x0:x1, 3] = 255
    return Image.fromarray(out, "RGBA")


# =========================================================================================== models
def write_json(path, data, check, problems):
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if check:
        if not os.path.exists(path) or json.load(open(path, encoding="utf-8")) != data:
            problems.append(path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
        open(path, "w", encoding="utf-8", newline="\n").write(text)


def write_png(path, img, check, problems):
    if check:
        if not os.path.exists(path):
            problems.append(path)
            return
        old = np.asarray(Image.open(path).convert("RGBA"))
        if old.shape != np.asarray(img).shape or not np.array_equal(old, np.asarray(img)):
            problems.append(path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        old = np.asarray(Image.open(path).convert("RGBA"))
        if old.shape == np.asarray(img).shape and np.array_equal(old, np.asarray(img)):
            return                                                             # same pixels: leave the file (and its git state) alone
    img.save(path, optimize=True)


def separate(body_model, gui_model, gui_light="front"):
    # the top-level "particle" texture: the baked model's particle sprite (broken-item and eating particles) is looked up in the wrapper, not in the nested models
    return {"loader": "neoforge:separate_transforms", "gui_light": gui_light, "textures": {"particle": "nusmp:item/skill_book_3d/base"},
            "base": {"parent": body_model}, "perspectives": {"gui": {"parent": gui_model}}}


def main():
    check = "--check" in sys.argv
    problems = []
    write_png(os.path.join(TEX_DIR, "base.png"), base_atlas(), check, problems)
    names = FRANCHISE_ORDER + ["generic"]
    for n in names:
        write_png(os.path.join(TEX_DIR, n + ".png"), cover_image(n), check, problems)
    write_json(os.path.join(MODEL_DIR, "skill_book_3d.json"), BM.base_model("nusmp:item/skill_book_3d/generic"), check, problems)
    write_json(os.path.join(MODEL_DIR, "skill_book_2d.json"), OLD_MODEL, check, problems)
    overrides = []
    for i, f in enumerate(FRANCHISE_ORDER):
        write_json(os.path.join(MODEL_DIR, "skill_book_3d_%s.json" % f), {"parent": "nusmp:item/skill_book_3d", "textures": {"cover": "nusmp:item/skill_book_3d/%s" % f}}, check, problems)
        write_json(os.path.join(MODEL_DIR, "skill_book_st_%s.json" % f), separate("nusmp:item/skill_book_3d_%s" % f, "nusmp:item/skill_book_2d"), check, problems)
        overrides.append({"predicate": {"nusmp:franchise": round((i + 1) / PROP_STEPS - 0.5 / PROP_STEPS, 6)}, "model": "nusmp:item/skill_book_st_%s" % f})
    top = separate("nusmp:item/skill_book_3d", "nusmp:item/skill_book_2d")
    top["overrides"] = overrides
    write_json(os.path.join(MODEL_DIR, "skill_book.json"), top, check, problems)
    if check and problems:
        print("STALE:\n  " + "\n  ".join(problems))
        sys.exit(1)
    print("ok: base + %d covers + %d models %s" % (len(names), 3 + 2 * len(FRANCHISE_ORDER), "checked" if check else "written"))


if __name__ == "__main__":
    main()

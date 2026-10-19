"""VFX / prop sprites of the earth family (1.93.0): textures/particle/earthfam_<magic>_<name>.png, six magics with five sprites each.

    python3 -B tools/gen_earth_family_textures.py

White / light-grey shapes with alpha so the vertex colour tints them (the layer picks the magic's palette); the glow sprites (sigils, cracks, flares)
are brighter at the core so additive blending reads as light. Everything is supersampled 4x for clean edges and seeded, so the output is reproducible.

    sandstone  grain  stream  slab  sigil (hourglass circle)  haze
    rock       chunk  dust  crack (radial fissures)  ring (impact ring with debris)  sigil (jagged triangle circle)
    red_ochre  splat  hand (a handprint)  stripe (a brush stroke)  sun (sun glyph)  drop
    stone      block  rune  circle (runic circle)  shard  dust
    earth      clod  blade (grass)  root (a tendril)  circle (gnome sigil)  dust
    mineral    shard  flare (four-point star)  facet (hexagon)  circle (hex lattice)  ring (ring of crystals)
    all        trail (the streak a thrown body drags)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def canvas(w, h=None):
    h = h or w
    im = Image.new("L", (w * SS, h * SS), 0)
    return im, ImageDraw.Draw(im)


def finish(mask_im, w, h=None, shade=None, tint=(255, 255, 255), noise=0.0, seed=0, blur=0.0):
    """Mask (L, supersampled) -> RGBA: alpha = mask, rgb = tint * shade (an array h x w in 0..1 or None) with optional noise."""
    h = h or w
    if blur:
        mask_im = mask_im.filter(ImageFilter.GaussianBlur(blur * SS))
    a = np.asarray(mask_im.resize((w, h), Image.LANCZOS), float) / 255.0
    rng = np.random.default_rng(seed)
    s = np.ones((h, w)) if shade is None else shade
    if noise:
        s = np.clip(s * (1 - noise + noise * 2 * rng.random((h, w))), 0, 1.3)
    rgb = np.clip(np.array(tint, float)[None, None, :] * s[..., None], 0, 255)
    out = np.dstack([rgb, a * 255.0])
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def save(im, key, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, "earthfam_%s_%s.png" % (key, name)), optimize=True)
    print("wrote earthfam_%s_%s" % (key, name), im.size)


def radial(w, h=None, power=2.0, core=1.0):
    h = h or w
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.sqrt(((xx + 0.5) / w * 2 - 1) ** 2 + ((yy + 0.5) / h * 2 - 1) ** 2)
    return np.clip(1 - r, 0, 1) ** power * core


def disc_mask(w, h=None, power=2.0):
    a = radial(w, h, power)
    return Image.fromarray((a * 255).astype(np.uint8), "L").resize(((w * SS), ((h or w) * SS)), Image.BILINEAR)


def blob_poly(cx, cy, r, rng, n=9, rough=0.35):
    pts = []
    for i in range(n):
        a = i * math.tau / n + rng.random() * 0.3
        rr = r * (1 - rough + rough * 2 * rng.random())
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return pts


def facet_shade(w, h, rng, bands=5, angle=0.6):
    yy, xx = np.mgrid[0:h, 0:w]
    d = (xx / w * math.cos(angle) + yy / h * math.sin(angle)) * bands
    f = np.floor(d) % 3
    return 0.62 + 0.18 * f + 0.06 * rng.random((h, w))


def ring_pts(cx, cy, r, n, start=0.0):
    return [(cx + math.cos(start + i * math.tau / n) * r, cy + math.sin(start + i * math.tau / n) * r) for i in range(n)]


def circle_line(d, cx, cy, r, width, fill=255):
    d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=fill, width=int(width * SS))


def poly(d, pts, fill=255):
    d.polygon([(x * SS, y * SS) for x, y in pts], fill=fill)


def line(d, a, b, width, fill=255):
    d.line([(a[0] * SS, a[1] * SS), (b[0] * SS, b[1] * SS)], fill=fill, width=max(1, int(width * SS)))


# ================================================================================================================== sandstone
def sandstone():
    rng = np.random.default_rng(11)
    save(finish(disc_mask(64, 64, 1.4), 64, shade=radial(64, 64, 0.6) * 0.5 + 0.55, noise=0.15, seed=1), "sandstone", "grain")
    im, d = canvas(32, 128)                                                                    # a falling stream of sand: strands that thin out downward
    for k in range(9):
        x = 6 + k * 2.4 + rng.random() * 1.5
        line(d, (x, 0), (x + rng.random() * 3 - 1.5, 124), 1.4 + rng.random() * 1.6, int(150 + 100 * rng.random()))
    mask = np.asarray(im.resize((32, 128), Image.LANCZOS), float) / 255.0
    yy = np.mgrid[0:128, 0:32][0] / 128.0
    a = np.clip(mask * (1 - yy * 0.85), 0, 1)
    save(finish(Image.fromarray((a * 255).astype(np.uint8)).resize((32 * SS, 128 * SS)), 32, 128, shade=0.7 + 0.3 * rng.random((128, 32)), seed=2), "sandstone", "stream")
    im, d = canvas(64)                                                                         # a chip of strata
    poly(d, [(8, 22), (50, 12), (58, 36), (40, 54), (12, 48)])
    yy = np.mgrid[0:64, 0:64][0]
    sh = 0.78 + 0.18 * np.sin(yy * 0.9) + 0.04 * rng.random((64, 64))
    save(finish(im, 64, shade=sh, seed=3), "sandstone", "slab")
    im, d = canvas(256)                                                                        # the hourglass sigil: ring, ticks, an hourglass, sand dots
    circle_line(d, 128, 128, 120, 4)
    circle_line(d, 128, 128, 108, 2)
    circle_line(d, 128, 128, 66, 3)
    for i in range(24):
        a = i * math.tau / 24
        line(d, (128 + math.cos(a) * 108, 128 + math.sin(a) * 108), (128 + math.cos(a) * (96 if i % 3 else 90), 128 + math.sin(a) * (96 if i % 3 else 90)), 2.5)
    poly(d, [(98, 78), (158, 78), (134, 128), (158, 178), (98, 178), (122, 128)], 255)
    poly(d, [(106, 86), (150, 86), (130, 118), (126, 118)], 0)
    poly(d, [(106, 170), (150, 170), (136, 140), (120, 140)], 0)
    for k in range(7):
        d.ellipse([(125 + k % 2 * 2) * SS, (122 + k * 3) * SS, (131 + k % 2 * 2) * SS, (127 + k * 3) * SS], fill=255)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=4), "sandstone", "sigil")
    cloud = np.zeros((128, 128))
    for cell, amp in ((16, 0.5), (8, 0.3), (4, 0.2)):
        g = rng.random((128 // cell + 2, 128 // cell + 2))
        cloud += amp * np.asarray(Image.fromarray(g.astype(np.float32), mode="F").resize((128 + cell * 2, 128 + cell * 2), Image.BICUBIC))[:128, :128]
    a = np.clip(cloud, 0, 1) * radial(128, 128, 0.8)
    save(Image.fromarray(np.dstack([np.full((128, 128, 3), 255), a * 150]).astype(np.uint8), "RGBA"), "sandstone", "haze")


# ================================================================================================================== rock
def rock():
    rng = np.random.default_rng(23)
    im, d = canvas(64)
    poly(d, [(10, 30), (24, 8), (46, 12), (58, 34), (44, 56), (18, 52)])
    sh = facet_shade(64, 64, rng, 4, 0.9)
    save(finish(im, 64, shade=sh, noise=0.12, seed=5), "rock", "chunk")
    puff = np.zeros((64, 64))
    for cell, amp in ((12, 0.55), (6, 0.3), (3, 0.15)):
        g = rng.random((64 // cell + 2, 64 // cell + 2))
        puff += amp * np.asarray(Image.fromarray(g.astype(np.float32), mode="F").resize((64 + cell * 2, 64 + cell * 2), Image.BICUBIC))[:64, :64]
    a = np.clip(puff, 0, 1) * radial(64, 64, 0.9)
    save(Image.fromarray(np.dstack([np.full((64, 64, 3), 255), a * 255]).astype(np.uint8), "RGBA"), "rock", "dust")
    im, d = canvas(256)                                                                        # radial fissures, branching
    def crack(x, y, ang, length, width, depth):
        for _ in range(int(length // 5)):
            nx, ny = x + math.cos(ang) * 5, y + math.sin(ang) * 5
            line(d, (x, y), (nx, ny), width)
            if depth > 0 and rng.random() < 0.18:
                crack(nx, ny, ang + (rng.random() - 0.5) * 1.6, length * 0.45, width * 0.7, depth - 1)
            x, y, ang = nx, ny, ang + (rng.random() - 0.5) * 0.5
            width *= 0.97
    for i in range(9):
        crack(128, 128, i * math.tau / 9 + rng.random() * 0.3, 120, 4.5, 2)
    d.ellipse([112 * SS, 112 * SS, 144 * SS, 144 * SS], fill=255)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.5, seed=6), "rock", "crack")
    im, d = canvas(256)                                                                        # an impact ring with debris along its edge
    circle_line(d, 128, 128, 112, 7)
    circle_line(d, 128, 128, 98, 2)
    for i in range(18):
        a = i * math.tau / 18 + rng.random() * 0.2
        r = 112 + rng.random() * 8
        cx, cy = 128 + math.cos(a) * r, 128 + math.sin(a) * r
        poly(d, blob_poly(cx, cy, 6 + rng.random() * 6, rng, 6, 0.3))
    save(finish(im, 256, shade=0.75 + 0.2 * rng.random((256, 256)), seed=7, blur=0.3), "rock", "ring")
    im, d = canvas(256)                                                                        # jagged triangle in a broken circle
    for a0 in range(0, 360, 60):
        for k in range(8):
            a = math.radians(a0 + k * 6)
            line(d, (128 + math.cos(a) * 118, 128 + math.sin(a) * 118), (128 + math.cos(a + 0.1) * 118, 128 + math.sin(a + 0.1) * 118), 4)
    tri = ring_pts(128, 128, 84, 3, -math.pi / 2)
    for i in range(3):
        a, b = tri[i], tri[(i + 1) % 3]
        pts = [a]
        for k in range(1, 7):
            t = k / 7
            pts.append((a[0] + (b[0] - a[0]) * t + (rng.random() - 0.5) * 7, a[1] + (b[1] - a[1]) * t + (rng.random() - 0.5) * 7))
        pts.append(b)
        for j in range(len(pts) - 1):
            line(d, pts[j], pts[j + 1], 4)
    d.ellipse([112 * SS, 112 * SS, 144 * SS, 144 * SS], fill=255)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=8), "rock", "sigil")


# ================================================================================================================== red ochre
def red_ochre():
    rng = np.random.default_rng(37)
    im, d = canvas(128)                                                                        # a pigment splat: a blob, drops flung out of it
    poly(d, blob_poly(64, 64, 26, rng, 14, 0.4))
    for i in range(16):
        a = rng.random() * math.tau
        r = 34 + rng.random() * 26
        s = 2 + rng.random() * 6
        cx, cy = 64 + math.cos(a) * r, 64 + math.sin(a) * r
        d.ellipse([(cx - s) * SS, (cy - s) * SS, (cx + s) * SS, (cy + s) * SS], fill=255)
        if s > 4:
            line(d, (64 + math.cos(a) * 28, 64 + math.sin(a) * 28), (cx, cy), s * 0.7)
    save(finish(im, 128, shade=0.8 + 0.2 * rng.random((128, 128)), seed=9, blur=0.5), "red_ochre", "splat")
    im, d = canvas(128)                                                                        # a handprint: palm and five fingers
    d.ellipse([34 * SS, 56 * SS, 94 * SS, 112 * SS], fill=255)
    for i, (fx, fy, ang, ln) in enumerate(((38, 62, -0.5, 40), (50, 54, -0.18, 52), (64, 52, 0.0, 56), (78, 54, 0.18, 50), (92, 66, 0.95, 34))):
        x1, y1 = fx + math.sin(ang) * ln, fy - math.cos(ang) * ln
        line(d, (fx, fy), (x1, y1), 11 if i != 4 else 12)
        d.ellipse([(x1 - 5.5) * SS, (y1 - 5.5) * SS, (x1 + 5.5) * SS, (y1 + 5.5) * SS], fill=255)
    save(finish(im, 128, shade=0.85 + 0.15 * rng.random((128, 128)), seed=10, blur=0.5, noise=0.2), "red_ochre", "hand")
    im, d = canvas(128, 32)                                                                    # a brush stroke: bristle streaks, a ragged tail
    for k in range(10):
        y = 5 + k * 2.2
        x1 = 118 - rng.random() * 30 * (k % 3) / 2
        line(d, (6 + rng.random() * 10, y), (x1, y + rng.random() * 2 - 1), 2.4 + rng.random(), int(170 + 85 * rng.random()))
    save(finish(im, 128, 32, shade=0.8 + 0.2 * rng.random((32, 128)), seed=11), "red_ochre", "stripe")
    im, d = canvas(256)                                                                        # a sun glyph: disc, ring, rays, a spiral of dots
    d.ellipse([88 * SS, 88 * SS, 168 * SS, 168 * SS], fill=255)
    d.ellipse([98 * SS, 98 * SS, 158 * SS, 158 * SS], fill=0)
    d.ellipse([112 * SS, 112 * SS, 144 * SS, 144 * SS], fill=255)
    for i in range(16):
        a = i * math.tau / 16
        r0, r1 = 100, 122 if i % 2 else 112
        poly(d, [(128 + math.cos(a - 0.09) * r0, 128 + math.sin(a - 0.09) * r0), (128 + math.cos(a) * (r1 + 8), 128 + math.sin(a) * (r1 + 8)), (128 + math.cos(a + 0.09) * r0, 128 + math.sin(a + 0.09) * r0)])
    for i in range(24):
        a = i * 0.5
        r = 8 + i * 3.1
        d.ellipse([(128 + math.cos(a) * r - 2.5) * SS, (128 + math.sin(a) * r - 2.5) * SS, (128 + math.cos(a) * r + 2.5) * SS, (128 + math.sin(a) * r + 2.5) * SS], fill=0 if r < 40 else 255)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=12), "red_ochre", "sun")
    im, d = canvas(64)
    poly(d, [(32, 4), (46, 30), (50, 42), (44, 54), (32, 58), (20, 54), (14, 42), (18, 30)])
    d.ellipse([14 * SS, 30 * SS, 50 * SS, 60 * SS], fill=255)
    save(finish(im, 64, shade=0.95 - 0.35 * np.mgrid[0:64, 0:64][0] / 64.0 * 0 + 0.0, seed=13, blur=0.6), "red_ochre", "drop")


# ================================================================================================================== stone
def stone():
    rng = np.random.default_rng(41)
    im, d = canvas(64)
    poly(d, [(8, 14), (56, 14), (56, 50), (8, 50)])
    yy, xx = np.mgrid[0:64, 0:64]
    sh = 0.75 + 0.2 * (yy < 20) - 0.25 * (yy > 44) + 0.1 * (xx < 14) + 0.06 * rng.random((64, 64))
    save(finish(im, 64, shade=sh, seed=14), "stone", "block")
    im, d = canvas(64)                                                                         # one rune
    line(d, (32, 8), (32, 56), 5)
    line(d, (32, 24), (16, 12), 4)
    line(d, (32, 24), (48, 12), 4)
    line(d, (32, 40), (20, 52), 4)
    line(d, (32, 40), (44, 52), 4)
    save(finish(im, 64, shade=np.ones((64, 64)), blur=0.3, seed=15), "stone", "rune")
    im, d = canvas(256)                                                                        # runic circle: square ring, glyph blocks, inner circle
    circle_line(d, 128, 128, 120, 4)
    circle_line(d, 128, 128, 92, 3)
    for i in range(12):
        a = i * math.tau / 12
        cx, cy = 128 + math.cos(a) * 106, 128 + math.sin(a) * 106
        line(d, (cx, cy - 8), (cx, cy + 8), 4)
        line(d, (cx - 5, cy), (cx + 5, cy), 3)
    sq = [(60, 60), (196, 60), (196, 196), (60, 196)]
    for i in range(4):
        line(d, sq[i], sq[(i + 1) % 4], 3)
    d.rectangle([108 * SS, 108 * SS, 148 * SS, 148 * SS], outline=255, width=3 * SS)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=16), "stone", "circle")
    im, d = canvas(64)
    poly(d, [(30, 4), (38, 4), (42, 58), (26, 58)])
    save(finish(im, 64, shade=facet_shade(64, 64, rng, 3, 0.2), seed=17), "stone", "shard")
    puff = np.zeros((64, 64))
    for cell, amp in ((10, 0.6), (5, 0.4)):
        g = rng.random((64 // cell + 2, 64 // cell + 2))
        puff += amp * np.asarray(Image.fromarray(g.astype(np.float32), mode="F").resize((64 + cell * 2, 64 + cell * 2), Image.BICUBIC))[:64, :64]
    save(Image.fromarray(np.dstack([np.full((64, 64, 3), 240), np.clip(puff, 0, 1) * radial(64, 64, 0.9) * 230]).astype(np.uint8), "RGBA"), "stone", "dust")


# ================================================================================================================== earth
def earth():
    rng = np.random.default_rng(53)
    im, d = canvas(64)
    poly(d, blob_poly(32, 32, 24, rng, 11, 0.3))
    yy = np.mgrid[0:64, 0:64][0]
    save(finish(im, 64, shade=0.85 - 0.3 * yy / 64.0 + 0.15 * rng.random((64, 64)), seed=18, blur=0.4), "earth", "clod")
    im, d = canvas(32, 96)
    poly(d, [(6, 94), (16, 4), (26, 94)])
    yy = np.mgrid[0:96, 0:32][0]
    save(finish(im, 32, 96, shade=0.55 + 0.45 * (1 - yy / 96.0), seed=19), "earth", "blade")
    im, d = canvas(128, 48)                                                                    # a root tendril: a wavy tapering line with side shoots
    pts = [(4 + i * 6.2, 24 + 9 * math.sin(i * 0.7)) for i in range(20)]
    for i in range(len(pts) - 1):
        line(d, pts[i], pts[i + 1], 6.5 * (1 - i / 20) + 1)
        if i % 4 == 2:
            line(d, pts[i], (pts[i][0] + 8, pts[i][1] + (10 if i % 8 == 2 else -10)), 2)
    save(finish(im, 128, 48, shade=0.8 + 0.2 * rng.random((48, 128)), seed=20, blur=0.3), "earth", "root")
    im, d = canvas(256)                                                                        # the gnome sigil: two rings, a leaf-and-seed glyph, ticks
    circle_line(d, 128, 128, 120, 4)
    circle_line(d, 128, 128, 96, 2)
    for i in range(16):
        a = i * math.tau / 16
        line(d, (128 + math.cos(a) * 96, 128 + math.sin(a) * 96), (128 + math.cos(a) * 112, 128 + math.sin(a) * 112), 3)
    for sd in (-1, 1):
        poly(d, [(128, 176), (128 + sd * 6, 140), (128 + sd * 36, 100), (128 + sd * 52, 70), (128 + sd * 20, 84), (128 + sd * 4, 120)])
    line(d, (128, 180), (128, 118), 4)
    d.ellipse([116 * SS, 176 * SS, 140 * SS, 200 * SS], fill=255)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=21), "earth", "circle")
    puff = np.zeros((64, 64))
    for cell, amp in ((12, 0.55), (6, 0.3), (3, 0.15)):
        g = rng.random((64 // cell + 2, 64 // cell + 2))
        puff += amp * np.asarray(Image.fromarray(g.astype(np.float32), mode="F").resize((64 + cell * 2, 64 + cell * 2), Image.BICUBIC))[:64, :64]
    save(Image.fromarray(np.dstack([np.full((64, 64, 3), 255), np.clip(puff, 0, 1) * radial(64, 64, 0.9) * 230]).astype(np.uint8), "RGBA"), "earth", "dust")


# ================================================================================================================== mineral
def mineral():
    rng = np.random.default_rng(67)
    im, d = canvas(64, 128)                                                                    # a hexagonal prism seen from the side: a body and a pointed tip
    poly(d, [(20, 40), (32, 4), (44, 40), (44, 120), (20, 120)])
    shade = np.ones((128, 64))
    xx = np.mgrid[0:128, 0:64][1]
    shade = np.where(xx < 28, 0.62, np.where(xx < 38, 1.0, 0.8)) + 0.05 * rng.random((128, 64))
    save(finish(im, 64, 128, shade=shade, seed=22), "mineral", "shard")
    im, d = canvas(128)                                                                        # a four-point sparkle
    poly(d, [(64, 2), (70, 58), (126, 64), (70, 70), (64, 126), (58, 70), (2, 64), (58, 58)])
    poly(d, [(64, 24), (68, 60), (104, 64), (68, 68), (64, 104), (60, 68), (24, 64), (60, 60)])
    save(finish(im, 128, shade=np.maximum(radial(128, 128, 0.35), 0.5), blur=0.4, seed=23), "mineral", "flare")
    im, d = canvas(128)                                                                        # a hexagon with internal facets
    hexp = ring_pts(64, 64, 58, 6, math.pi / 6)
    poly(d, hexp)
    shade = np.ones((128, 128)) * 0.7
    yy, xx = np.mgrid[0:128, 0:128]
    ang = np.arctan2(yy - 64, xx - 64)
    shade = 0.5 + 0.18 * (np.floor((ang + math.pi) / (math.pi / 3)) % 3) + 0.2 * radial(128, 128, 1.0)
    save(finish(im, 128, shade=shade, seed=24), "mineral", "facet")
    im, d = canvas(256)                                                                        # a lattice of hexagons inside a ring
    circle_line(d, 128, 128, 120, 4)
    circle_line(d, 128, 128, 104, 2)
    for ring_r, n in ((0, 1), (36, 6), (72, 12)):
        for i in range(n):
            a = i * math.tau / max(1, n)
            cx, cy = 128 + math.cos(a) * ring_r, 128 + math.sin(a) * ring_r
            pts = ring_pts(cx, cy, 20 if ring_r < 72 else 16, 6, math.pi / 6)
            for j in range(6):
                line(d, pts[j], pts[(j + 1) % 6], 3)
    save(finish(im, 256, shade=np.ones((256, 256)), blur=0.4, seed=25), "mineral", "circle")
    im, d = canvas(256)                                                                        # a ring of small crystals standing outward
    circle_line(d, 128, 128, 100, 5)
    for i in range(20):
        a = i * math.tau / 20
        base = (128 + math.cos(a) * 100, 128 + math.sin(a) * 100)
        tip = (128 + math.cos(a) * (126 + (i % 3) * 3), 128 + math.sin(a) * (126 + (i % 3) * 3))
        nx, ny = -math.sin(a) * 7, math.cos(a) * 7
        poly(d, [(base[0] + nx, base[1] + ny), tip, (base[0] - nx, base[1] - ny)])
    save(finish(im, 256, shade=0.85 + 0.15 * rng.random((256, 256)), seed=26, blur=0.3), "mineral", "ring")


def shared():
    rng = np.random.default_rng(79)
    w, h = 128, 32                                                                             # the trail every magic's thrown body drags: a grainy streak, bright at the head (right)
    yy, xx = np.mgrid[0:h, 0:w]
    across = np.clip(1 - np.abs((yy + 0.5) / h * 2 - 1), 0, 1) ** 1.5
    along = ((xx + 0.5) / w) ** 1.6
    grain = 0.75 + 0.25 * rng.random((h, w))
    a = np.clip(across * along * grain * 1.2, 0, 1)
    save(Image.fromarray(np.dstack([np.full((h, w, 3), 255), a * 255]).astype(np.uint8), "RGBA"), "all", "trail")


if __name__ == "__main__":
    for fn in (sandstone, rock, red_ochre, stone, earth, mineral, shared):
        fn()

"""Spirit Dive, REEL look: writes the textures of the look to src/main/resources/assets/nusmp/textures/aura/ (all deterministic, redo with `python3 -B tools/gen_spirit_dive_reel_textures.py`).

The look is the V2 design (crystal-block crown with a gold ring halo, hex-membrane wing, gold rings) painted the way the owner's two showcase videos paint it: glowing BLOCKS (pixel cells with a bright
frame and a white inner light), thin white-gold ribbons, a leaf / feather wing, a comb of thin blades, a mosaic of blue block patches. Every texture is RGBA with RGB = 0 where alpha = 0
(SafeAdditive: checked by SpiritDiveReelAssetTest), whether it is drawn additive or alpha blended.

Per element (wind, water, fire, earth):
    sdr_cube_<el>.png          32x32   the glowing block: dark frame, bright rim, a white inner light (crown cubes, gauntlets, the fist cube)
    sdr_bar_<el>.png           16x64   the vertical bar of the crown (additive): white core, a tinted body, tip fading
    sdr_wing_<el>.png          wind 128x256: the hex-membrane leaf (u across, v root .. tip); the others 32x128: one blade of the comb (water feather, flame feather, crystal shard)
    sdr_wing_glow_<el>.png     the additive glow of the same: rim lines, quill, tip
    sdr_circle_<el>.png        128x128 the magic circle (wind: crosses, water: ripples, fire: sun rays, earth: squares) at the hilt of the weapon
    sdr_mosaic_<el>_<1..5>.png 64x64   the patchwork of glowing block patches laid over the 64x64 skin layout of the body parts; level k = the patches that are on after k fifths of the assembling
Shared:
    sdr_ribbon (32x128) the thin white-gold spiral ribbon, sdr_ring / sdr_ring_gold / sdr_halo_ring (128x128) thin rings, sdr_ground (256x256) the ground ring, sdr_flash (128x128) the flash,
    sdr_spark (32x32) a drifting fragment, sdr_gold (16x16) the gold of the limb bands, sdr_sword_wind / sdr_sword_fire (64x256) the spirit sword and the flame blade (tip at the top),
    sdr_spike_water (32x64) one blade of Noelle's spiky ball, sdr_spire_water (64x256) her crystal spire.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "aura")
ELS = ["wind", "water", "fire", "earth"]


def rgb(h):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], np.float32)


# per element: dark frame, body, light, bright core (the cube and the blocks), glow
PAL = {
    "wind": {"frame": 0x1FA58F, "body": 0x49DCC4, "light": 0xB6FFF0, "core": 0xFFFFFF, "glow": 0x98FFD0, "deep": 0x16806E},
    "water": {"frame": 0x1B64C8, "body": 0x4FA8FF, "light": 0xBDE6FF, "core": 0xFFFFFF, "glow": 0xA8E4FF, "deep": 0x1346A0},
    "fire": {"frame": 0xA81C08, "body": 0xFF6A18, "light": 0xFFC24A, "core": 0xFFF6C8, "glow": 0xFF8A2A, "deep": 0x6E1006},
    "earth": {"frame": 0x6A4E1C, "body": 0xC29A50, "light": 0xFFE08A, "core": 0xFFF8DC, "glow": 0xFFB040, "deep": 0x4A3612},
}


def save(rgba, name):
    """rgba float HxWx4 (0..255): RGB := 0 where alpha is 0 (SafeAdditive), then written."""
    os.makedirs(OUT, exist_ok=True)
    a = np.clip(np.rint(rgba), 0, 255).astype(np.uint8)
    a[a[..., 3] == 0, :3] = 0
    Image.fromarray(a, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)


def blank(h, w):
    return np.zeros((h, w, 4), np.float32)


def blend(img, mask, col, alpha):
    """Normal 'over' blending of col at alpha on the masked pixels."""
    a = np.clip(alpha, 0.0, 1.0)
    a = np.broadcast_to(a, mask.shape) if np.ndim(a) else np.full(mask.shape, float(a), np.float32)
    ca = img[..., 3:4] / 255.0
    m = mask[..., None]
    na = a[..., None]
    out_a = na + ca * (1 - na)
    out_rgb = (col[None, None, :] * na + img[..., :3] * ca * (1 - na)) / np.maximum(out_a, 1e-5)
    img[..., :3] = np.where(m, out_rgb, img[..., :3])
    img[..., 3:4] = np.where(m, out_a * 255.0, img[..., 3:4])
    return img


# ------------------------------------------------------------------ the cube
def cube(el):
    p = PAL[el]
    N = 32
    img = blank(N, N)
    yy, xx = np.mgrid[0:N, 0:N]
    frame, body, light, core, deep = (rgb(p[k]) for k in ("frame", "body", "light", "core", "deep"))
    rnd = random.Random(hash(el) & 0xFFFF)
    img[..., :3] = body
    img[..., 3] = 232
    # pixel cells of 2 px: a little variation in the body like the blocks of the video
    cell = np.array([[rnd.uniform(-1, 1) for _ in range(N // 2)] for _ in range(N // 2)], np.float32)
    var = np.kron(cell, np.ones((2, 2), np.float32))
    img[..., :3] = np.clip(body[None, None, :] * (1.0 + 0.10 * var[..., None]) + 14.0 * var[..., None], 0, 255)
    # lit upper left, shaded lower right (a bevel)
    d = (xx + yy) / (2.0 * (N - 1))
    img[..., :3] = img[..., :3] * (1.08 - 0.30 * d[..., None])
    edge = np.minimum(np.minimum(xx, yy), np.minimum(N - 1 - xx, N - 1 - yy))
    img[edge == 0, :3] = frame * 0.9                                                         # the dark outer frame
    ring = edge == 1
    img[ring, :3] = light                                                                    # the bright rim
    img[ring, 3] = 255
    ring2 = edge == 2
    img[ring2, :3] = np.clip(light * 0.55 + body * 0.45, 0, 255)
    # the inner light: a white panel with a soft border
    inner = (xx >= 9) & (xx < 23) & (yy >= 9) & (yy < 23)
    inner_edge = np.minimum(np.minimum(xx - 9, yy - 9), np.minimum(22 - xx, 22 - yy))
    img[inner, :3] = np.where((inner_edge[inner] < 2)[:, None], light, core)
    img[inner, 3] = 255
    # a diagonal gleam
    gl = (np.abs((xx - yy) - 3) < 1.2) & (xx < 14) & (yy < 14) & ~inner
    img[gl, :3] = core
    img[gl, 3] = 255
    # the four corner bolts (like pixel studs)
    for cx, cy in ((4, 4), (27, 4), (4, 27), (27, 27)):
        img[cy:cy + 1, cx:cx + 1, :3] = core
    save(img, "sdr_cube_" + el)


# ------------------------------------------------------------------ the crown bar
def bar(el):
    p = PAL[el]
    W, H = 16, 64
    img = blank(H, W)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    du = np.abs(xx - (W - 1) / 2.0) / (W / 2.0)
    body, light, core = rgb(p["body"]), rgb(p["light"]), rgb(p["core"])
    prof = np.clip(1.0 - du * 1.35, 0, 1) ** 0.7
    tip = np.clip(yy / 14.0, 0, 1) ** 0.8                                                  # fades toward the top (v = 0)
    foot = np.clip((H - 1 - yy) / 10.0, 0, 1) * 0.0 + 1.0
    a = prof * tip * foot
    t = np.clip(1.0 - du * 2.6, 0, 1)
    col = body[None, None, :] * (1 - t[..., None]) + core[None, None, :] * t[..., None]
    col = col * (1 - 0.35 * (1 - np.clip(du * 2.2, 0, 1))[..., None]) + light * 0.0
    img[..., :3] = col
    img[..., 3] = 255.0 * np.clip(a * 1.15, 0, 1)
    img[yy < 2, 3] = 0
    save(img, "sdr_bar_" + el)


# ------------------------------------------------------------------ the wind leaf (hex membrane)
def hex_lines(W, H, R, rnd):
    """A pointy-top honeycomb of circumradius R: the distance of each pixel to the nearest cell edge (px) and a random shade (0..1) per cell."""
    s3 = math.sqrt(3.0)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    q = (s3 / 3.0 * xx - 1.0 / 3.0 * yy) / R
    r = (2.0 / 3.0 * yy) / R
    x, z = q, r
    y = -x - z
    rx, ry, rz = np.rint(x), np.rint(y), np.rint(z)
    dx, dy, dz = np.abs(rx - x), np.abs(ry - y), np.abs(rz - z)
    fix_x = (dx > dy) & (dx > dz)
    fix_y = (~fix_x) & (dy > dz)
    rx = np.where(fix_x, -ry - rz, rx)
    rz = np.where(~fix_x & ~fix_y, -rx - ry, rz)
    cx = R * s3 * (rx + rz / 2.0)
    cy = R * 1.5 * rz
    dxp, dyp = np.abs(xx - cx), np.abs(yy - cy)
    e = np.maximum(dxp, dxp * 0.5 + dyp * s3 / 2.0)
    edge = R * s3 / 2.0 - e
    ids = (rx.astype(np.int64) * 73856093) ^ (rz.astype(np.int64) * 19349663)
    h = (ids * 2654435761) & 0xFFFF
    return edge, (h / 65535.0).astype(np.float32)


def wind_leaf():
    p = PAL["wind"]
    W, H = 128, 256
    rnd = random.Random(7)
    img = blank(H, W)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = xx / (W - 1)
    v = yy / (H - 1)
    body, light, core, frame, deep = (rgb(p[k]) for k in ("body", "light", "core", "frame", "deep"))
    edge, shade = hex_lines(W, H, 16.0, rnd)
    # the membrane: translucent teal cells, a little different each
    base_a = 0.42 + 0.20 * shade
    col = body[None, None, :] * (0.78 + 0.34 * shade[..., None]) + light[None, None, :] * 0.10
    img[..., :3] = col
    img[..., 3] = 255.0 * base_a
    # the cell walls: bright lines
    wall = np.clip(1.0 - edge / 1.9, 0, 1) ** 0.8
    img[..., :3] = img[..., :3] * (1 - wall[..., None]) + light[None, None, :] * wall[..., None]
    img[..., 3] = np.maximum(img[..., 3], 255.0 * 0.95 * wall)
    # veins: the midrib and side veins of a leaf
    mid = np.clip(1.0 - np.abs(u - 0.5) * W / 3.4, 0, 1)
    img[..., :3] = img[..., :3] * (1 - mid[..., None]) + core[None, None, :] * mid[..., None]
    img[..., 3] = np.maximum(img[..., 3], 255.0 * mid)
    for k in range(7):                                                                         # side veins sweeping from the midrib toward the tip
        v0 = 0.12 + 0.11 * k
        for side in (-1, 1):
            t = np.clip(((v - v0) / 0.33), 0, 1)
            target = 0.5 + side * 0.46 * np.sin(np.pi * np.clip(v0 + 0.1, 0, 1)) * 1.0
            xc = 0.5 + (target - 0.5) * t
            on = (np.abs(u - xc) * W < 1.1) & (v > v0) & (v < v0 + 0.33)
            img[on, :3] = light
            img[on, 3] = np.maximum(img[on, 3], 255 * 0.8)
    # the rim: a bright line at both edges of the leaf, and a dark thin outline outside it
    rim = np.clip(1.0 - np.minimum(u, 1 - u) * W / 4.0, 0, 1) ** 0.7
    img[..., :3] = img[..., :3] * (1 - rim[..., None]) + core[None, None, :] * rim[..., None]
    img[..., 3] = np.maximum(img[..., 3], 255 * rim)
    # the tip is whiter
    tipw = np.clip((v - 0.84) / 0.16, 0, 1) ** 1.3
    img[..., :3] = img[..., :3] * (1 - 0.7 * tipw[..., None]) + core[None, None, :] * 0.7 * tipw[..., None]
    img[..., 3] = np.maximum(img[..., 3], 255 * 0.7 * tipw)
    save(img, "sdr_wing_wind")
    # the glow: the rim, the midrib, the walls faintly, the tip
    g = blank(H, W)
    ga = np.clip(0.0 * u, 0, 1)
    ga = np.maximum(ga, rim * 0.95)
    ga = np.maximum(ga, mid * 0.9)
    ga = np.maximum(ga, wall * 0.40)
    ga = np.maximum(ga, tipw * 0.7)
    g[..., :3] = rgb(PAL["wind"]["glow"])[None, None, :] * (1 - 0.5 * np.maximum(rim, mid)[..., None]) + core[None, None, :] * 0.5 * np.maximum(rim, mid)[..., None]
    g[..., 3] = 255.0 * ga
    save(g, "sdr_wing_glow_wind")


# ------------------------------------------------------------------ the comb blades (water feather, flame feather, crystal shard)
def blade(el):
    p = PAL[el]
    W, H = 32, 128
    img = blank(H, W)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = xx / (W - 1)
    v = yy / (H - 1)
    du = np.abs(u - 0.5) * 2.0                                                              # 0 at the quill, 1 at the edge
    body, light, core, frame, deep = (rgb(p[k]) for k in ("body", "light", "core", "frame", "deep"))
    glow = blank(H, W)
    if el == "water":
        # a feather blade: pale blue, white at the tip, barbs slanting from the quill, a white quill and rim
        t = np.clip(v * 1.15, 0, 1)
        col = light[None, None, :] * (1 - 0.35 * du[..., None]) * (0.78 + 0.22 * t[..., None]) + body[None, None, :] * (0.22 * du[..., None] + 0.0)
        img[..., :3] = col
        img[..., 3] = 255 * (0.80 + 0.12 * (1 - du))
        barb = (np.abs(((yy * 0.5 + du * 9.0) % 6.0) - 3.0) < 0.7)
        img[barb, :3] = np.clip(img[barb, :3] + 40, 0, 255)
        wh = np.clip((v - 0.80) / 0.20, 0, 1) ** 1.1
        img[..., :3] = img[..., :3] * (1 - wh[..., None]) + core[None, None, :] * wh[..., None]
        quill = np.clip(1.0 - du * W / 3.2, 0, 1)
        img[..., :3] = img[..., :3] * (1 - quill[..., None]) + core[None, None, :] * quill[..., None]
        img[..., 3] = np.maximum(img[..., 3], 255 * quill)
        rim = np.clip(1.0 - (1 - du) * W / 3.0, 0, 1) ** 0.8
        img[..., :3] = img[..., :3] * (1 - 0.8 * rim[..., None]) + frame[None, None, :] * 0.0 + (light * 0.55 + core * 0.45)[None, None, :] * 0.8 * rim[..., None] + img[..., :3] * 0 * rim[..., None]
        img[..., 3] = np.maximum(img[..., 3], 255 * 0.95 * rim)
        ga = np.maximum(np.maximum(quill, rim * 0.9), wh * 0.8)
    elif el == "fire":
        # a flame feather: yellow core, orange, red edge; the edge licks
        lick = 0.12 * np.sin(v * 38.0 + du * 3.0) + 0.06 * np.sin(v * 71.0)
        d2 = du + lick * (0.3 + v)
        a = np.clip(1.0 - np.clip(d2 - 0.78, 0, 1) / 0.22, 0, 1)
        col = np.where((d2 < 0.28)[..., None], core, np.where((d2 < 0.55)[..., None], light, np.where((d2 < 0.78)[..., None], body, frame)))
        img[..., :3] = col
        img[..., 3] = 255 * a * (0.92 - 0.25 * v)
        barb = (np.abs(((yy * 0.45 + du * 8.0) % 7.0) - 3.5) < 0.6) & (du < 0.7)
        img[barb, :3] = np.clip(img[barb, :3] + 35, 0, 255)
        ga = np.clip(0.55 * (1 - d2) + 0.3 * (du < 0.3), 0, 1) * a * 0.9
    else:
        # a crystal shard: two facets split on the centre line, gold edge, a bright tip
        left = (u < 0.5)
        fac_l = light * 0.95
        fac_r = body * 0.85
        col = np.where(left[..., None], fac_l[None, None, :], fac_r[None, None, :])
        col = col * (0.82 + 0.28 * (1 - v)[..., None])
        img[..., :3] = col
        img[..., 3] = 255 * 0.94
        for k in range(3):                                                                    # facet lines slanting
            yk = 0.2 + 0.22 * k
            ln = (np.abs(v - (yk + du * 0.10)) * H < 0.9) & (du < 0.95)
            img[ln, :3] = core
        centre = np.clip(1.0 - du * W / 1.8, 0, 1)
        img[..., :3] = img[..., :3] * (1 - centre[..., None]) + core[None, None, :] * centre[..., None]
        rim = np.clip(1.0 - (1 - du) * W / 2.6, 0, 1)
        img[..., :3] = img[..., :3] * (1 - rim[..., None]) + rgb(0xFFD878)[None, None, :] * rim[..., None]
        img[..., 3] = np.maximum(img[..., 3], 255 * rim)
        wh = np.clip((v - 0.86) / 0.14, 0, 1)
        img[..., :3] = img[..., :3] * (1 - wh[..., None]) + core[None, None, :] * wh[..., None]
        ga = np.maximum(np.maximum(centre * 0.8, rim * 0.9), wh * 0.8)
    img[yy < 1, 3] = 0
    save(img, "sdr_wing_" + el)
    glow[..., :3] = rgb(p["glow"])[None, None, :] * 0.55 + core[None, None, :] * 0.45
    glow[..., 3] = 255 * np.clip(ga, 0, 1) * 0.9
    glow[yy < 1, 3] = 0
    save(glow, "sdr_wing_glow_" + el)


# ------------------------------------------------------------------ the magic circle
def circle(el):
    p = PAL[el]
    N = 128
    c = (N - 1) / 2.0
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    r = np.hypot(xx - c, yy - c)
    ang = np.arctan2(yy - c, xx - c)
    img = blank(N, N)
    core, light, body, glow = rgb(p["core"]), rgb(p["light"]), rgb(p["body"]), rgb(p["glow"])
    a = np.zeros((N, N), np.float32)

    def ring(r0, w, k=1.0):
        return np.clip(1.0 - np.abs(r - r0) / w, 0, 1) * k

    def line(p0, p1, w, k=1.0):
        x0, y0 = p0
        x1, y1 = p1
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy)
        t = np.clip(((xx - x0) * dx + (yy - y0) * dy) / (ln * ln), 0, 1)
        d = np.hypot(xx - (x0 + t * dx), yy - (y0 + t * dy))
        return np.clip(1.0 - d / w, 0, 1) * k

    a = np.maximum(a, ring(60, 1.8))
    a = np.maximum(a, ring(54, 1.1, 0.8))
    a = np.maximum(a, ring(34, 1.6))
    a = np.maximum(a, ring(14, 1.2, 0.9))
    tri = lambda off: [(c + 33 * math.cos(math.radians(off + 120 * k)), c + 33 * math.sin(math.radians(off + 120 * k))) for k in range(3)]
    for off in (-90, 90):
        pts = tri(off)
        for k in range(3):
            a = np.maximum(a, line(pts[k], pts[(k + 1) % 3], 1.2, 0.95))
    if el == "wind":                                                                           # the circle of connected crosses
        for k in range(12):
            t = math.radians(30 * k)
            cx, cy = c + 57 * math.cos(t), c + 57 * math.sin(t)
            a = np.maximum(a, line((cx - 4, cy), (cx + 4, cy), 1.0))
            a = np.maximum(a, line((cx, cy - 4), (cx, cy + 4), 1.0))
    elif el == "water":                                                                        # ripples
        for r0, k in ((46, 0.7), (24, 0.8), (8, 0.7)):
            a = np.maximum(a, ring(r0, 0.9, k))
    elif el == "fire":                                                                         # sun rays
        for k in range(16):
            t = math.radians(22.5 * k)
            a = np.maximum(a, line((c + 40 * math.cos(t), c + 40 * math.sin(t)), (c + 53 * math.cos(t), c + 53 * math.sin(t)), 1.4, 0.95))
    else:                                                                                      # squares
        for h in (44, 26):
            for (x0, y0, x1, y1) in ((c - h, c - h, c + h, c - h), (c + h, c - h, c + h, c + h), (c + h, c + h, c - h, c + h), (c - h, c + h, c - h, c - h)):
                a = np.maximum(a, line((x0, y0), (x1, y1), 1.1, 0.85))
    for k in range(24):                                                                        # ticks between the two outer rings
        t = math.radians(15 * k)
        a = np.maximum(a, line((c + 55 * math.cos(t), c + 55 * math.sin(t)), (c + 59 * math.cos(t), c + 59 * math.sin(t)), 0.8, 0.8))
    soft = np.array(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.2)), np.float32) / 255.0
    al = np.clip(np.maximum(a, soft * 0.55), 0, 1)
    img[..., :3] = glow[None, None, :] * (1 - np.clip(a, 0, 1)[..., None]) + core[None, None, :] * np.clip(a, 0, 1)[..., None] * 0.9 + light[None, None, :] * 0.0
    img[..., 3] = 255 * al
    save(img, "sdr_circle_" + el)


# ------------------------------------------------------------------ the mosaic
REGIONS = [
    # body
    (20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12),
    # right arm
    (44, 16, 4, 4), (48, 16, 4, 4), (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12),
    # right leg
    (4, 16, 4, 4), (8, 16, 4, 4), (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12), (12, 20, 4, 12),
    # left leg
    (20, 48, 4, 4), (24, 48, 4, 4), (16, 52, 4, 12), (20, 52, 4, 12), (24, 52, 4, 12), (28, 52, 4, 12),
    # left arm
    (36, 48, 4, 4), (40, 48, 4, 4), (32, 52, 4, 12), (36, 52, 4, 12), (40, 52, 4, 12), (44, 52, 4, 12)]
MOSAIC = {
    "water": {"cov": 0.99, "shades": [0x2C86EE, 0x4FA6FF, 0x7CC2FF, 0xA6DCFF, 0xDDF3FF], "w": [0.18, 0.34, 0.26, 0.16, 0.06], "alpha": 0.92},
    "wind": {"cov": 0.38, "shades": [0x1FA58F, 0x49DCC4, 0x86F0DC, 0xC8FFF4, 0xFFFFFF], "w": [0.22, 0.34, 0.24, 0.14, 0.06], "alpha": 0.90},
    "fire": {"cov": 0.38, "shades": [0xB82410, 0xEE5A14, 0xFF9A24, 0xFFC84A, 0xFFF0A8], "w": [0.22, 0.34, 0.24, 0.14, 0.06], "alpha": 0.90},
    "earth": {"cov": 0.38, "shades": [0x7A5A26, 0xB08A44, 0xD6B060, 0xFFD878, 0xFFF4D0], "w": [0.22, 0.34, 0.24, 0.14, 0.06], "alpha": 0.92},
}


def mosaic_cells(el):
    """The patchwork of every region: a list of (x, y, w, h, shade index, order 0..1, exists). The same cells for every level (deterministic): the levels only switch more of them on."""
    cfg = MOSAIC[el]
    rnd = random.Random(1000 + ELS.index(el))
    cells = []
    for (x0, y0, w, h) in REGIONS:
        occ = [[False] * w for _ in range(h)]
        # a smooth field of light so neighbouring patches share a shade (patches of lighter blues, not noise)
        fx, fy, fp = rnd.uniform(0.4, 1.0), rnd.uniform(0.4, 1.0), rnd.uniform(0, 6.28)
        for y in range(h):
            for x in range(w):
                if occ[y][x]:
                    continue
                opts = [(1, 1), (2, 1), (1, 2), (2, 2), (2, 2)] if (el == "water" or True) else [(1, 1)]
                rnd.shuffle(opts)
                cw, ch = 1, 1
                for (ow, oh) in opts:
                    if x + ow <= w and y + oh <= h and not any(occ[y + j][x + i] for j in range(oh) for i in range(ow)):
                        cw, ch = ow, oh
                        break
                for j in range(ch):
                    for i in range(cw):
                        occ[y + j][x + i] = True
                field = 0.5 + 0.5 * math.sin(fx * (x + cw / 2) + fy * (y + ch / 2) * 0.9 + fp)
                r = rnd.random()
                tot = 0.0
                s = 0
                target = min(0.999, max(0.0, 0.62 * field + 0.38 * r))
                acc = 0.0
                for k, wgt in enumerate(cfg["w"]):
                    acc += wgt
                    if target * 1.0 <= acc:
                        s = k
                        break
                else:
                    s = len(cfg["w"]) - 1
                s = min(len(cfg["shades"]) - 1, int(round(target * (len(cfg["shades"]) - 1) * 1.0)))
                if rnd.random() < 0.06:
                    s = len(cfg["shades"]) - 1
                ybottom = (y + ch / 2.0) / h                                                 # 1 at the bottom of the limb: those patches switch on first
                order = 0.62 * (1.0 - ybottom) + 0.38 * rnd.random()
                exists = rnd.random() < cfg["cov"]
                cells.append((x0 + x, y0 + y, cw, ch, s, order, exists))
    return cells


def mosaic(el):
    cfg = MOSAIC[el]
    cells = mosaic_cells(el)
    shades = [rgb(h) for h in cfg["shades"]]
    for level in range(1, 6):
        img = blank(64, 64)
        thr = level / 5.0 + (1e-6 if level == 5 else 0.0)
        for (x, y, w, h, s, order, exists) in cells:
            if not exists or (level < 5 and order > thr * 0.98) :
                continue
            col = shades[s]
            for j in range(h):
                for i in range(w):
                    px = col.copy()
                    if i == 0 and j == 0 and (w > 1 or h > 1):                                   # a lit edge on the top left of a big patch
                        px = np.minimum(px * 1.12 + 10, 255)
                    if i == w - 1 and j == h - 1 and (w > 1 or h > 1):
                        px = px * 0.9
                    img[y + j, x + i, :3] = px
                    img[y + j, x + i, 3] = 255 * cfg["alpha"]
        save(img, "sdr_mosaic_%s_%d" % (el, level))


# ------------------------------------------------------------------ the shared textures
def ribbon():
    W, H = 32, 128
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    du = np.abs(xx - (W - 1) / 2.0) / (W / 2.0)
    img = blank(H, W)
    core = np.exp(-(du / 0.22) ** 2)
    halo = np.exp(-(du / 0.62) ** 2)
    gold, white = rgb(0xFFDE8C), rgb(0xFFFFFF)
    img[..., :3] = white[None, None, :] * core[..., None] + gold[None, None, :] * (1 - core[..., None])
    img[..., 3] = 255 * np.clip(0.95 * core + 0.35 * halo, 0, 1)
    save(img, "sdr_ribbon")


def ring_tex(name, r0, w, col, core, soft, extra=None):
    N = 128
    c = (N - 1) / 2.0
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    r = np.hypot(xx - c, yy - c)
    a = np.clip(1.0 - np.abs(r - r0) / w, 0, 1) ** 0.8
    glow = np.exp(-((r - r0) / (w * 3.2)) ** 2) * soft
    if extra:
        for (rr, ww, kk) in extra:
            a = np.maximum(a, np.clip(1.0 - np.abs(r - rr) / ww, 0, 1) * kk)
    al = np.clip(np.maximum(a, glow), 0, 1)
    img = blank(N, N)
    t = np.clip(a, 0, 1)[..., None]
    img[..., :3] = col[None, None, :] * (1 - t) + core[None, None, :] * t
    img[..., 3] = 255 * al
    save(img, name)


def ground():
    N = 256
    c = (N - 1) / 2.0
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    r = np.hypot(xx - c, yy - c)
    ang = np.arctan2(yy - c, xx - c)
    a = np.clip(1.0 - np.abs(r - 122) / 2.4, 0, 1) ** 0.8
    a = np.maximum(a, np.clip(1.0 - np.abs(r - 100) / 1.4, 0, 1) * 0.5)
    tick = (np.abs(((ang / (2 * np.pi) * 48) % 1.0) - 0.5) < 0.12) & (r > 106) & (r < 114)
    a = np.maximum(a, tick * 0.55)
    soft = np.clip(1.0 - np.abs(r - 112) / 22.0, 0, 1) ** 2 * 0.16
    al = np.clip(np.maximum(a, soft), 0, 1)
    img = blank(N, N)
    img[..., :3] = np.array([255, 255, 255], np.float32)[None, None, :] * np.clip(a, 0, 1)[..., None] + np.array([200, 255, 245], np.float32)[None, None, :] * (1 - np.clip(a, 0, 1)[..., None])
    img[..., 3] = 255 * al
    save(img, "sdr_ground")


def flash():
    N = 128
    c = (N - 1) / 2.0
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    dx, dy = xx - c, yy - c
    r = np.hypot(dx, dy)
    core = np.exp(-(r / 14.0) ** 2)
    halo = np.exp(-(r / 36.0) ** 2) * 0.55
    ang = np.arctan2(dy, dx)
    rays = np.zeros((N, N), np.float32)
    for k in range(8):
        t = math.pi / 4 * k + math.pi / 8
        along = dx * math.cos(t) + dy * math.sin(t)
        across = -dx * math.sin(t) + dy * math.cos(t)
        L = 62 if k % 2 == 0 else 38
        rays = np.maximum(rays, np.exp(-(across / 1.8) ** 2) * np.clip(1 - np.abs(along) / L, 0, 1) ** 1.5 * (0.95 if k % 2 == 0 else 0.7))
    al = np.clip(np.maximum(np.maximum(core, halo), rays), 0, 1)
    img = blank(N, N)
    img[..., :3] = np.array([255, 255, 255], np.float32)[None, None, :] * np.clip(core + rays, 0, 1)[..., None] + np.array([210, 255, 246], np.float32)[None, None, :] * (1 - np.clip(core + rays, 0, 1)[..., None])
    img[..., 3] = 255 * al
    save(img, "sdr_flash")


def spark():
    N = 32
    c = (N - 1) / 2.0
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    d = (np.abs(xx - c) + np.abs(yy - c)) / 14.0
    a = np.clip(1.0 - d, 0, 1) ** 0.7
    core = np.clip(1.0 - d * 2.4, 0, 1)
    img = blank(N, N)
    img[..., :3] = np.array([255, 255, 255], np.float32)[None, None, :] * core[..., None] + np.array([215, 255, 248], np.float32)[None, None, :] * (1 - core[..., None])
    img[..., 3] = 255 * a
    save(img, "sdr_spark")


def gold():
    N = 16
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    img = blank(N, N)
    base = rgb(0xE8B63A)
    img[..., :3] = base[None, None, :] * (0.85 + 0.25 * (1 - yy / (N - 1)))[..., None]
    img[yy <= 2, :3] = rgb(0xFFF0A8)
    img[yy >= N - 3, :3] = rgb(0xA8741C)
    img[..., 3] = 255
    save(img, "sdr_gold")


def sword(el):
    W, H = 64, 256
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = (xx - (W - 1) / 2.0) / (W / 2.0)                                                       # -1..1
    v = yy / (H - 1)
    p = PAL[el]
    body, light, core, frame, glow = (rgb(p[k]) for k in ("body", "light", "core", "frame", "glow"))
    img = blank(H, W)
    # the blade: v 0 .. 0.70, half width 0.34 with a pointed tip over the first 12 percent
    tip = np.clip(v / 0.12, 0, 1)
    hw = 0.34 * np.where(v < 0.12, tip ** 0.9, 1.0)
    inb = (v < 0.70) & (np.abs(u) < hw)
    img[inb, :3] = body
    img[inb, 3] = 245
    edge = inb & (np.abs(u) > hw - 0.07)
    img[edge, :3] = light
    fuller = inb & (np.abs(u) < 0.07)
    img[fuller, :3] = core
    mid = inb & (np.abs(u) < 0.2) & (np.abs(u) >= 0.07)
    img[mid, :3] = np.clip(body * 1.15 + 18, 0, 255)
    if el == "fire":
        lick = 0.05 * np.sin(v * 60.0)
        img[inb & (np.abs(u) > hw - 0.12 + lick), :3] = rgb(0xFF8A2A)
        img[inb & (np.abs(u) < 0.14), :3] = rgb(0xFFF0A0)
    # the guard: a wide bar with raised ends
    gd = (v >= 0.70) & (v < 0.765) & (np.abs(u) < 0.98 - 0.35 * np.clip((np.abs(u) - 0.6) / 0.4, 0, 1) * (v - 0.70) / 0.065)
    img[gd, :3] = light
    img[gd, 3] = 250
    gd_core = gd & (np.abs(u) < 0.30)
    img[gd_core, :3] = core
    # tips of the guard curl up
    cu = (v >= 0.665) & (v < 0.70) & (np.abs(u) > 0.74) & (np.abs(u) < 0.98)
    img[cu, :3] = light
    img[cu, 3] = 240
    # the grip and the pommel
    gr = (v >= 0.765) & (v < 0.925) & (np.abs(u) < 0.11)
    img[gr, :3] = frame * 1.1 + 40
    img[gr, 3] = 245
    wrap = gr & (((yy * 0.9) % 6.0) < 1.5)
    img[wrap, :3] = light
    pom = np.hypot(u / 0.26, (v - 0.955) / 0.045) < 1.0
    img[pom, :3] = light
    img[pom, 3] = 250
    pc = np.hypot(u / 0.10, (v - 0.955) / 0.02) < 1.0
    img[pc, :3] = core
    # a soft rim of light outside the blade
    save(img, "sdr_sword_" + el)


def spike():
    W, H = 32, 64
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = np.abs(xx - (W - 1) / 2.0) / (W / 2.0)
    v = yy / (H - 1)
    img = blank(H, W)
    body, light, core, frame = rgb(0x7FAEE8), rgb(0xD6ECFF), rgb(0xFFFFFF), rgb(0x2E5FB0)
    col = body[None, None, :] * (1 - 0.35 * u[..., None]) + light[None, None, :] * (0.35 * (1 - v)[..., None])
    img[..., :3] = col
    img[..., 3] = 255 * 0.95
    centre = u < 0.12
    img[centre, :3] = frame
    edge = u > 0.78
    img[edge, :3] = core
    tipw = v < 0.18
    img[tipw, :3] = np.clip(img[tipw, :3] * 0.5 + core * 0.5, 0, 255)
    save(img, "sdr_spike_water")


def spire():
    W, H = 64, 256
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = (xx - (W - 1) / 2.0) / (W / 2.0)
    v = yy / (H - 1)
    img = blank(H, W)
    hw = np.where(v < 0.78, 0.62 * (v / 0.78) ** 0.85, 0.62 * (1 - (v - 0.78) / 0.22 * 0.35))
    inb = np.abs(u) < hw
    # four facet bands, dark to white
    band = np.floor((u / np.maximum(hw, 1e-3) + 1.0) * 2.0).clip(0, 3)
    cols = [rgb(0x3F8EF0), rgb(0x8FCBFF), rgb(0xD8F0FF), rgb(0x68B0F8)]
    for k in range(4):
        m = inb & (band == k)
        img[m, :3] = cols[k]
    img[inb, 3] = 235
    seam = inb & (np.abs((u / np.maximum(hw, 1e-3)) % 0.5) < 0.04)
    img[seam, :3] = rgb(0xFFFFFF)
    top = inb & (v < 0.06)
    img[top, :3] = rgb(0xFFFFFF)
    # a fade at the foot
    foot = np.clip((1.0 - v) / 0.14, 0, 1)
    img[..., 3] = img[..., 3] * foot
    # two little side spikes
    for (cx, y0, y1) in ((0.55, 0.45, 0.72), (-0.60, 0.55, 0.80)):
        h2 = 0.13 * np.clip((v - y0) / (y1 - y0), 0, 1) * np.clip((y1 + 0.02 - v) / 0.1, 0, 1)
        sp = (v > y0) & (v < y1) & (np.abs(u - cx) < h2 * 1.6)
        img[sp, :3] = rgb(0xBFE4FF)
        img[sp, 3] = 235
    save(img, "sdr_spire_water")


def main():
    for el in ELS:
        cube(el)
        bar(el)
        circle(el)
        mosaic(el)
    wind_leaf()
    for el in ("water", "fire", "earth"):
        blade(el)
    ribbon()
    ring_tex("sdr_ring", 58, 1.5, rgb(0xE6FFFA), rgb(0xFFFFFF), 0.22, [(52, 0.7, 0.25)])
    ring_tex("sdr_ring_gold", 58, 2.1, rgb(0xFFD66E), rgb(0xFFF4B8), 0.30, [(50, 1.0, 0.75), (62, 0.8, 0.5)])
    ring_tex("sdr_halo_ring", 56, 3.0, rgb(0xCFF0FF), rgb(0xFFFFFF), 0.45)
    ground()
    flash()
    spark()
    gold()
    sword("wind")
    sword("fire")
    spike()
    spire()
    print("written to", os.path.abspath(OUT))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
FIRE remake (new round), PREVIEW ONLY: the textures of direction A ("anime cel") and direction B ("volumetric glow") plus the shared sparks / embers.
Nothing here is registered in the game and nothing goes to assets/.  Direction C (real 3D toon meshes) has no flame textures: its flames are
geometry (tools/gen_fire_new_meshes.py); it only reuses the small glow / ember / spark sprites below.

    python tools/gen_fire_new_textures.py            # Pillow + numpy only, deterministic (fixed seeds)

Output (preview folder only): docs/previews/vfx_remake/fire_new/tex/<name>.png and tex_sheet.png (contact sheet).

Look sources (owner pack "Fire Magic VFX Effects", see docs/vfx_remake/fire_new.md):
  A  cel     the tutorial sprite sheets (b669a565 silhouette -> yellow highlight -> glow) and the pixel/cel tongue sheet (4aa0b176): ONE solid silhouette in
             red-orange, nested flat inner shapes (orange, gold, yellow, hot white) that sit low and lean with the flow, no black outline, hooked tongue tips.
  B  glow    the anime stills themselves (Combo_Eruption, Leo_Rugiens, Magna_Exploding_Fireball): saturated orange body, yellow-white centre, red rim, soft halo.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "fire_new", "tex")

SS = 4                                                           # supersampling of the cel shapes (hard edges, antialiased by the downscale)

# cel palette (hex from the owner's Fuegoleon art + the tutorial sheets)
C_RIM = (214, 52, 18)        # red-orange rim
C_BODY = (248, 104, 22)      # orange
C_LIGHT = (255, 158, 30)     # light orange
C_GOLD = (255, 198, 38)      # gold
C_CORE = (255, 232, 110)     # yellow
C_HOT = (255, 248, 214)      # hot white
C_DEEP = (150, 28, 14)       # deep red (shade side / inside of the mouth)


def rg(seed):
    return np.random.default_rng(seed)


# ------------------------------------------------------------------------------------------------ noise
def vnoise(h, w, cell, seed):
    r = rg(seed)
    gh, gw = h // cell + 3, w // cell + 3
    g = r.random((gh, gw)).astype(np.float32)
    im = Image.fromarray(g, "F").resize((gw * cell, gh * cell), Image.BICUBIC)
    return np.asarray(im)[cell:cell + h, cell:cell + w]


def fbm(h, w, seed, cells=(48, 24, 12, 6), amps=(1.0, 0.55, 0.3, 0.15)):
    a = np.zeros((h, w), np.float32)
    for i, (c, k) in enumerate(zip(cells, amps)):
        a += k * vnoise(h, w, max(2, c), seed + i * 17)
    return a / sum(amps)


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


# ------------------------------------------------------------------------------------------------ cel flame shapes
def tongue_pts(L, W, curl, seed, n=34, jag=0.16, flip=False):
    """Local outline of one flame tongue: base centre at (0, 0), pointing up (-y), widest low, a hooked tip that curls to +x (or -x when flip)."""
    r = rg(seed)
    ph = r.random(4) * 6.28
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        cx = curl * (t ** 2.1) * L * 0.42 + 0.06 * L * math.sin(t * 3.1 + ph[0]) * (1 - t)
        cy = -L * t
        tx = curl * 2.1 * (t ** 1.1) * 0.42 + 0.06 * math.cos(t * 3.1 + ph[0]) * 3.1 * (1 - t)
        ty = -1.0
        ln = math.hypot(tx, ty)
        nx, ny = -ty / ln, tx / ln                                # normal
        w = W * (1 - t) ** 0.72 * (0.62 + 0.38 * math.sin(min(1.0, t * 3.2) * math.pi / 2))
        w *= 1 + jag * (math.sin(t * 17 + ph[1]) * 0.5 + math.sin(t * 29 + ph[2]) * 0.5) * (0.4 + t)
        left.append((cx - nx * w, cy - ny * w))
        right.append((cx + nx * w, cy + ny * w))
    pts = left + right[::-1]
    if flip:
        pts = [(-x, y) for x, y in pts]
    return pts


def place(pts, x, y, ang=0.0, k=1.0):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(x + (px * ca - py * sa) * k, y + (px * sa + py * ca) * k) for px, py in pts]


def mask_of(polys, size, extra=None):
    im = Image.new("L", size, 0)
    d = ImageDraw.Draw(im)
    for p in polys:
        d.polygon(p, fill=255)
    if extra:
        extra(d)
    return np.asarray(im, np.float32) / 255.0


def shifted(m, dx, dy):
    return np.roll(np.roll(m, int(dy), axis=0), int(dx), axis=1)


def blur(m, r):
    im = Image.fromarray((m * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def dist_in(mask, cap=40, k=4):
    """inward distance (px) of every inside pixel: repeated 3x3 erosion on a 1/k copy (no scipy), upscaled smoothly"""
    h, w = mask.shape
    im = Image.fromarray(((mask > 0.5) * 255).astype(np.uint8), "L").resize((w // k, h // k), Image.BOX).point(lambda v: 255 if v > 127 else 0)
    d = np.zeros((h // k, w // k), np.float32)
    cur = im
    for i in range(cap):
        a = np.asarray(cur) > 0
        if not a.any():
            break
        d += a
        cur = cur.filter(ImageFilter.MinFilter(3))
    big = Image.fromarray(d, "F").resize((w, h), Image.BICUBIC)
    return np.asarray(big) * k


def cel_paint(mask, specs, seed, wobble=4.0):
    """Nested flat tones inside a silhouette.  specs[0] = colour of the whole silhouette; each further (colour, inset_px, dx, dy) paints the part that lies at least
    inset_px deep inside the silhouette after the silhouette is shifted by (dx, dy): an inner shape that sits lower and leans with the flow (the tutorial look).
    Returns float RGBA with hard edges (antialiased later by the downscale)."""
    h, w = mask.shape
    out = np.zeros((h, w, 4), np.float32)
    inside = mask > 0.5
    out[inside, :3] = np.array(specs[0], np.float32) / 255.0
    out[inside, 3] = 1.0
    nz = fbm(h, w, seed, cells=(int(0.16 * w), int(0.08 * w), int(0.04 * w)), amps=(1, 0.5, 0.25))
    for col, inset, dx, dy in specs[1:]:
        d = dist_in(shifted(mask, dx, dy))
        reg = (d + (nz - 0.5) * wobble * 2 > inset) & inside
        out[reg, :3] = np.array(col, np.float32) / 255.0
    return out


def down(rgba, size):
    im = Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")
    # premultiplied downscale so the antialiased edge keeps its colour
    a = np.asarray(im, np.float32) / 255.0
    pm = a.copy()
    pm[..., :3] *= a[..., 3:4]
    ims = [Image.fromarray((pm[..., i] * 255).astype(np.uint8)).resize(size, Image.LANCZOS) for i in range(4)]
    r = np.stack([np.asarray(i, np.float32) / 255.0 for i in ims], -1)
    al = r[..., 3:4]
    rgb = np.where(al > 1e-3, r[..., :3] / np.maximum(al, 1e-3), 0)
    return Image.fromarray((np.clip(np.concatenate([rgb, al], -1), 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


FLAME_SPECS = [C_RIM, (C_BODY, 14, 0, 6), (C_LIGHT, 30, 4, 22), (C_GOLD, 48, 8, 42), (C_CORE, 68, 10, 64), (C_HOT, 92, 10, 86)]


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))
    return img


# ------------------------------------------------------------------------------------------------ A: the cel textures
def cel_tongue(frame):
    w, h = 128 * SS, 256 * SS
    r = rg(100 + frame)
    cur = [0.55, 0.9, 0.35][frame]
    main = place(tongue_pts(h * 0.84, w * 0.40, cur, 200 + frame), w * 0.44, h * 0.90)
    polys = [main, [(w * 0.44 + w * 0.34 * math.cos(t), h * 0.83 + w * 0.24 * math.sin(t)) for t in np.linspace(0, 6.28318, 50)]]
    sats = [(-0.26, 0.58, 1.0, 0.24), (0.27, 0.5, 1.1, -0.2), (-0.10, 0.36, 1.2, 0.1)]
    for k, (ox, ln, cu, an) in enumerate(sats):
        base = (w * (0.44 + ox * 1.0), h * (0.90 - 0.02 * k))
        polys.append(place(tongue_pts(h * ln, w * 0.22, cu * (0.6 + 0.5 * r.random()), 300 + frame * 7 + k), base[0], base[1], ang=an))
    m = mask_of(polys, (w, h))
    img = cel_paint(m, FLAME_SPECS, 400 + frame)
    return save(down(img, (128, 256)), f"A_tongue_{'abc'[frame]}")


def cel_ball(frame):
    s = 128 * SS
    r = rg(500 + frame)
    th = np.linspace(0, 2 * math.pi, 220)
    ph = r.random(3) * 6.28
    rad = s * 0.33 * (1 + 0.06 * np.sin(7 * th + ph[0]) + 0.05 * np.sin(11 * th + ph[1]) + 0.03 * np.sin(17 * th + ph[2]))
    pts = [(s * 0.5 + rad[i] * math.cos(th[i]), s * 0.54 + rad[i] * math.sin(th[i])) for i in range(len(th))]
    polys = [pts]
    for k in range(5 - frame):                                        # small hooked tongues licking off the top: the "fire ball" of the tutorial sheet
        a = -math.pi / 2 + (k - 2) * 0.55 + frame * 0.12
        bx, by = s * 0.5 + s * 0.27 * math.cos(a), s * 0.54 + s * 0.27 * math.sin(a)
        polys.append(place(tongue_pts(s * (0.2 + 0.05 * r.random()), s * 0.07, 0.8 * (1 if k % 2 else -1) * 0.5 + 0.2, 600 + k + frame * 9), bx, by, ang=a + math.pi / 2))
    m = mask_of(polys, (s, s))
    specs = [C_RIM, (C_BODY, 12, -6, 10), (C_LIGHT, 30, -12, 20), (C_GOLD, 52, -18, 30), (C_CORE, 80, -22, 40), (C_HOT, 116, -24, 46)]
    img = cel_paint(m, specs, 700 + frame, wobble=5)
    return save(down(img, (128, 128)), f"A_ball_{'abc'[frame]}")


def cel_body(frame):
    """horizontal stream, tail at the left, head at the right, hooked tongues licking backwards (the Burst Javelin / Grand Slam look)"""
    w, h = 256 * SS, 128 * SS
    r = rg(800 + frame)
    cy = h * 0.58
    top, bot = [], []
    n = 40
    for i in range(n + 1):
        t = i / n
        wd = h * (0.04 + 0.20 * smooth(t * 1.15) ** 0.8) * (1.0 + 0.1 * math.sin(t * 19 + frame))
        x = w * (0.02 + 0.92 * t)
        top.append((x, cy - wd))
        bot.append((x, cy + wd * 0.9))
    head = [(w * 0.94, cy - h * 0.24)] + [(w * (0.94 + 0.055 * math.sin(a)), cy + h * 0.24 * -math.cos(a) * 1.0) for a in np.linspace(0, math.pi, 12)]
    polys = [top + bot[::-1]]
    polys.append([(w * 0.86 + (w * 0.13) * math.cos(a), cy + h * 0.25 * math.sin(a)) for a in np.linspace(0, 6.28, 40)])
    nt = 9
    for k in range(nt):
        t = 0.2 + 0.72 * k / (nt - 1) + (r.random() - 0.5) * 0.05
        bx = w * (0.02 + 0.92 * t)
        by = cy - h * (0.04 + 0.20 * smooth(t * 1.15) ** 0.8) * 0.8
        ln = h * (0.30 + 0.34 * t + 0.1 * r.random())
        polys.append(place(tongue_pts(ln, h * (0.06 + 0.05 * t), -0.9 - 0.3 * r.random(), 900 + k + frame * 11, flip=False), bx, by, ang=-0.38 - 0.1 * r.random()))
        if k % 3 == 1:
            polys.append(place(tongue_pts(ln * 0.6, h * 0.05, 0.9, 950 + k + frame * 5), bx + w * 0.02, cy + h * 0.14, ang=math.pi + 0.5))
    m = mask_of(polys, (w, h))
    specs = [C_RIM, (C_BODY, 9, 6, 6), (C_LIGHT, 20, 14, 12), (C_GOLD, 32, 24, 16), (C_CORE, 46, 34, 18), (C_HOT, 62, 44, 18)]
    img = cel_paint(m, specs, 1000 + frame, wobble=4)
    return save(down(img, (256, 128)), f"A_body_{'abc'[frame]}")


def cel_swoosh(frame):
    """a curved sheet of flame running left to right with a hooked tip (the wide swirl sheets of Leopold_spiral_flame / the comet flow of the tutorial)"""
    w, h = 256 * SS, 96 * SS
    r = rg(1700 + frame)
    main = place(tongue_pts(w * 0.94, h * 0.30, -1.5 + 0.3 * frame, 1710 + frame, jag=0.10), w * 0.02, h * 0.72, ang=math.pi / 2)
    polys = [main]
    polys.append(place(tongue_pts(w * 0.55, h * 0.16, -1.2, 1720 + frame), w * 0.30, h * 0.60, ang=math.pi / 2))
    m = mask_of(polys, (w, h))
    img = cel_paint(m, [C_RIM, (C_BODY, 8, 0, 4), (C_LIGHT, 16, 6, 8), (C_GOLD, 25, 12, 10), (C_CORE, 34, 20, 10), (C_HOT, 44, 30, 10)], 1730 + frame, wobble=3)
    return save(down(img, (256, 96)), f"A_swoosh_{'ab'[frame]}")


def cel_ring():
    """a ring of flame standing on the ground, seen from above-front like the tutorial ring (flat decal quad on the ground): flames point outward, not up"""
    s = 256 * SS
    r = rg(1100)
    polys = []
    n = 22
    base = s * 0.30
    polys.append([(s * 0.5 + base * 1.03 * math.cos(a), s * 0.5 + base * 1.03 * math.sin(a)) for a in np.linspace(0, 6.28318, 120)])
    for k in range(n):
        a = 6.28318 * k / n
        bx, by = s * 0.5 + base * math.cos(a), s * 0.5 + base * math.sin(a)
        ln = s * (0.12 + 0.09 * r.random())
        polys.append(place(tongue_pts(ln, s * 0.04, 0.6 * (1 if k % 2 else -1), 1200 + k), bx, by, ang=a + math.pi / 2))
    m = mask_of(polys, (s, s))
    hole = mask_of([[(s * 0.5 + base * 0.80 * math.cos(a), s * 0.5 + base * 0.80 * math.sin(a)) for a in np.linspace(0, 6.28318, 120)]], (s, s))
    m = m * (1 - hole)
    specs = [C_RIM, (C_BODY, 7, 0, 5), (C_LIGHT, 15, 0, 9), (C_GOLD, 24, 0, 12), (C_CORE, 34, 0, 14)]
    img = cel_paint(m, specs, 1300, wobble=3)
    return save(down(img, (256, 256)), "A_ring")


def cel_rays():
    """hard radial speed lines (Magna_Exploding_Fireball / Calidus_Brachium_Barrage): alternating spokes, yellow-white, tapered"""
    s = 256 * SS
    r = rg(1400)
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    n = 34
    for k in range(n):
        a = 6.28318 * k / n + (r.random() - 0.5) * 0.08
        ln = s * (0.30 + 0.20 * r.random()) if k % 2 == 0 else s * (0.18 + 0.10 * r.random())
        w0 = 0.055 if k % 2 == 0 else 0.03
        r0 = s * 0.09
        pts = [(s / 2 + r0 * math.cos(a - w0), s / 2 + r0 * math.sin(a - w0)), (s / 2 + ln * math.cos(a), s / 2 + ln * math.sin(a)), (s / 2 + r0 * math.cos(a + w0), s / 2 + r0 * math.sin(a + w0))]
        d.polygon(pts, fill=C_CORE + (255,) if k % 2 == 0 else C_GOLD + (255,))
    return save(im.resize((256, 256), Image.LANCZOS), "A_rays")


def cel_flash():
    s = 128 * SS
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = []
    for k in range(16):
        a = 6.28318 * k / 16
        rr = s * (0.47 if k % 2 == 0 else 0.16) * (1.0 if k % 4 else 0.8)
        pts.append((s / 2 + rr * math.cos(a), s / 2 + rr * math.sin(a)))
    d.polygon(pts, fill=C_CORE + (255,))
    pts2 = [(s / 2 + (0.33 if k % 2 == 0 else 0.12) * s * math.cos(6.28318 * k / 16 + 0.2), s / 2 + (0.33 if k % 2 == 0 else 0.12) * s * math.sin(6.28318 * k / 16 + 0.2)) for k in range(16)]
    d.polygon(pts2, fill=C_HOT + (255,))
    return save(im.resize((128, 128), Image.LANCZOS), "A_flash")


def cel_smoke(frame):
    s = 128 * SS
    r = rg(1500 + frame)
    cols = [(86, 70, 70), (128, 108, 104), (178, 156, 146)]
    polys = []
    for k in range(9):
        a = 6.28318 * k / 9 + frame
        rr = 0.19 + 0.05 * r.random()
        polys.append([(s * 0.5 + s * 0.24 * math.cos(a) + s * rr * math.cos(t), s * 0.52 + s * 0.2 * math.sin(a) + s * rr * math.sin(t)) for t in np.linspace(0, 6.28318, 40)])
    polys.append([(s * 0.5 + s * 0.26 * math.cos(t), s * 0.52 + s * 0.22 * math.sin(t)) for t in np.linspace(0, 6.28318, 40)])
    m = mask_of(polys, (s, s))
    img = cel_paint(m, [cols[0], (cols[1], 18, -10, -16), (cols[2], 52, -24, -38)], 1600 + frame, wobble=8)
    return save(down(img, (128, 128)), f"A_smoke_{'ab'[frame]}")


def cel_spark():
    s = 32 * SS
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = s / 2
    d.polygon([(c, 0), (c + s * 0.08, c - s * 0.08), (s, c), (c + s * 0.08, c + s * 0.08), (c, s), (c - s * 0.08, c + s * 0.08), (0, c), (c - s * 0.08, c - s * 0.08)], fill=C_CORE + (255,))
    d.polygon([(c, s * 0.2), (c + s * 0.05, c), (c, s * 0.8), (c - s * 0.05, c)], fill=C_HOT + (255,))
    return save(im.resize((32, 32), Image.LANCZOS), "A_spark")


def cel_chip(frame):
    s = 16 * SS
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    col = [C_CORE, C_BODY][frame]
    d.polygon([(s * 0.5, s * 0.08), (s * 0.82, s * 0.5), (s * 0.5, s * 0.92), (s * 0.2, s * 0.5)], fill=col + (255,))
    return save(im.resize((16, 16), Image.LANCZOS), f"A_chip_{'ab'[frame]}")


# ---- the Leo Rugiens lion head, side view, facing +x, mouth open, mane tongues streaming back (Fuegoleon_Leo_Rugiens still: open jaw, white-hot mouth)
def cel_lion(frame):
    S = SS
    w, h = 512 * S, 320 * S
    r = rg(2000 + frame)

    def P(*pts):
        return [(x * S, y * S) for x, y in pts]
    mane = []
    n = 15
    cx0, cy0 = 270, 150
    for k in range(n):
        a = math.radians(-105 - 230 * k / (n - 1))                           # from above the brow, over the back, down under the throat
        rad = 62 + 8 * r.random()
        bx, by = cx0 + rad * math.cos(a), cy0 + rad * math.sin(a) * 1.05
        ln = (118 + 56 * r.random()) * (0.75 if k in (0, 1, n - 1) else 1.0)
        ang = a + math.pi / 2
        lx = -math.sin(a)                                                    # world x of the local +x axis
        curl = (0.55 + 0.35 * r.random() + frame * 0.1) * (1 if lx < 0 else -1) * -1
        pts = tongue_pts(ln * S, (24 + 6 * r.random()) * S, curl, 2100 + k + frame * 13)
        mane.append(place(pts, bx * S, by * S, ang=ang))
    mane.append([(cx0 * S + 96 * S * math.cos(t), cy0 * S + 100 * S * math.sin(t)) for t in np.linspace(0, 6.28318, 60)])
    mmask = mask_of(mane, (w, h))
    mimg = cel_paint(mmask, [C_DEEP, (C_RIM, 20, 0, 8), (C_BODY, 54, -8, 20), (C_LIGHT, 118, -14, 44)], 2200 + frame, wobble=7)
    head = P((236, 112), (262, 84), (306, 70), (348, 76), (380, 92), (410, 108), (424, 124), (422, 142), (404, 150), (372, 148), (340, 152), (300, 158), (262, 160), (238, 140))
    jaw = P((296, 158), (346, 160), (390, 166), (420, 184), (428, 208), (410, 224), (372, 226), (330, 216), (292, 198), (278, 176))
    ear = P((258, 90), (262, 56), (292, 60), (302, 84))
    hm = mask_of([head, jaw, ear], (w, h))
    himg = cel_paint(hm, [C_RIM, (C_LIGHT, 12, 0, 6), (C_GOLD, 26, -6, 12), (C_CORE, 44, -10, 18)], 2300 + frame, wobble=4)
    base = mimg.copy()
    base = np.where(himg[..., 3:4] > 0.5, himg, base)

    def paint(polys, col):
        nonlocal base
        m = mask_of(polys, (w, h))[..., None] > 0.5
        base = np.where(m, np.array([*col, 255], np.float32)[None, None, :] / 255, base)
    paint([P((304, 158), (372, 150), (404, 152), (414, 170), (400, 196), (360, 200), (322, 190))], C_DEEP)
    paint([P((326, 168), (380, 160), (408, 166), (400, 186), (364, 190), (330, 184))], C_HOT)
    paint([P((384, 150), (398, 150), (392, 178)), P((406, 150), (418, 148), (414, 170)), P((390, 198), (402, 196), (398, 178)), P((408, 190), (418, 184), (414, 172))], C_HOT)
    paint([P((330, 98), (364, 94), (370, 106), (338, 112))], C_RIM)
    paint([P((344, 99), (364, 97), (366, 104), (348, 107))], C_HOT)
    paint([P((318, 88), (370, 84), (376, 92), (322, 96))], C_RIM)
    paint([P((408, 112), (424, 122), (420, 134), (406, 128))], C_DEEP)
    return save(down(base, (512, 320)), f"A_lion_{'ab'[frame]}")


# ------------------------------------------------------------------------------------------------ B: soft volumetric textures
def ramp(t, stops):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        m = (t >= t0) & (t <= t1)
        f = ((t - t0) / (t1 - t0))[..., None]
        out = np.where(m[..., None], np.array(c0, np.float32) * (1 - f) + np.array(c1, np.float32) * f, out)
    return out / 255.0


HEAT = [(0.0, (255, 252, 226)), (0.18, (255, 232, 120)), (0.42, (255, 176, 30)), (0.68, (255, 108, 22)), (0.88, (226, 56, 16)), (1.0, (150, 28, 14))]


def soft_puff(variant, size=128):
    """noisy radial flame puff: colour baked as a radial heat ramp (hot white centre -> gold -> orange -> red rim), alpha eroded by noise; the rim is red, so stacking layers never goes white"""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    cx = cy = size / 2
    r = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (size / 2)
    nz = fbm(size, size, 3000 + variant * 5, cells=(40, 20, 10, 5))
    d = r * (1.0 + 0.55 * (nz - 0.5) * 2)
    # lean the hot core slightly down-left so every puff has a direction
    heat = np.clip(d * 1.25 + 0.0, 0, 1.2)
    rgb = ramp(heat, HEAT)
    a = smooth((1.0 - d) / 0.55) ** 0.9
    a = np.clip(a * 1.05, 0, 1) * (1 - smooth((r - 0.72) / 0.28))
    img = np.concatenate([rgb, a[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), f"B_puff_{'abcd'[variant]}")


def soft_tongue(variant):
    """a soft flame body (taller than wide): same ramp, teardrop silhouette with noise-eroded edge, hot core low"""
    w, h = 96, 192
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    nz = fbm(h, w, 3500 + variant * 7, cells=(48, 24, 12, 6))
    t = 1 - yy / h                                                    # 0 at the bottom, 1 at the top
    sway = (nz - 0.5) * 0.35 + 0.10 * np.sin(t * 5 + variant * 2)
    wd = 0.60 * (1 - t) ** 0.8 * (0.5 + 0.5 * np.sin(np.minimum(1, t * 3.5) * math.pi / 2)) + 0.02
    dx = np.abs((xx / w - 0.5) - sway * t * 0.5) / np.maximum(wd, 1e-3)
    d = np.sqrt(dx ** 2 + 0.0)
    d = d * (1 + 0.5 * (nz - 0.5))
    a = smooth((1 - d) / 0.6)
    a *= smooth((1 - t) / 0.12) * (0.0 + 1.0) if False else 1.0
    a *= np.clip((1 - t * 0.98) * 1.6, 0, 1) ** 0.6
    a *= np.clip(yy / 6.0, 0, 1) * np.clip((h - yy) / 3.0, 0, 1) if False else 1.0
    heat = np.clip(d * 1.15 + t * 0.55 - 0.10, 0, 1.2)
    rgb = ramp(heat, HEAT)
    img = np.concatenate([rgb, np.clip(a, 0, 1)[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), f"B_tongue_{'ab'[variant]}")


def soft_halo():
    s = 256
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    r = np.sqrt((xx - s / 2) ** 2 + (yy - s / 2) ** 2) / (s / 2)
    a = np.clip(1 - r, 0, 1) ** 2.2
    rgb = ramp(np.clip(r * 1.1, 0, 1), [(0.0, (255, 224, 140)), (0.35, (255, 150, 40)), (0.8, (232, 70, 22)), (1.0, (200, 50, 16))])
    img = np.concatenate([rgb, a[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_halo")


def soft_streak():
    w, h = 256, 64
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xx / w
    v = np.abs(yy / h - 0.5) * 2
    prof = np.clip(1 - v, 0, 1) ** 1.6
    env = smooth(u / 0.25) * (1 - 0.25 * u)
    a = prof * env
    heat = np.clip(v * 1.2 + (1 - u) * 0.15, 0, 1)
    rgb = ramp(heat, HEAT[:5])
    img = np.concatenate([rgb, np.clip(a, 0, 1)[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_streak")


def soft_ember():
    s = 32
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    r = np.sqrt((xx - s / 2 + 0.5) ** 2 + (yy - s / 2 + 0.5) ** 2) / (s / 2)
    a = np.clip(1 - r, 0, 1) ** 1.2
    rgb = ramp(np.clip(r * 1.1, 0, 1), [(0.0, (255, 240, 190)), (0.4, (255, 190, 60)), (1.0, (240, 90, 20))])
    img = np.concatenate([rgb, a[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_ember")


def soft_smoke():
    s = 128
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    r = np.sqrt((xx - s / 2) ** 2 + (yy - s / 2) ** 2) / (s / 2)
    nz = fbm(s, s, 3900, cells=(40, 20, 10))
    d = r * (1 + 0.6 * (nz - 0.5))
    a = smooth((1 - d) / 0.7) * 0.9 * (1 - smooth((r - 0.7) / 0.3))
    c = 40 + 40 * (1 - d)
    img = np.stack([c * 1.0, c * 0.75, c * 0.65, a * 255], -1)
    return save(Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA"), "B_smoke")


def soft_rays():
    s = 256
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    dx, dy = xx - s / 2, yy - s / 2
    r = np.sqrt(dx ** 2 + dy ** 2) / (s / 2)
    th = np.arctan2(dy, dx)
    nz = vnoise(1, 1, 2, 1) if False else None
    spokes = (np.sin(th * 14) * 0.5 + 0.5) ** 3 * (0.6 + 0.4 * np.sin(th * 5 + 1))
    a = np.clip(spokes * np.clip(1 - r, 0, 1) ** 1.4 * 1.4, 0, 1) * smooth((r - 0.05) / 0.2)
    rgb = ramp(np.clip(r * 1.2, 0, 1), [(0.0, (255, 245, 200)), (0.5, (255, 190, 50)), (1.0, (240, 100, 24))])
    img = np.concatenate([rgb, a[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_rays")


def soft_ring():
    s = 256
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    r = np.sqrt((xx - s / 2) ** 2 + (yy - s / 2) ** 2) / (s / 2)
    nz = fbm(s, s, 3950, cells=(48, 24, 12))
    th = np.arctan2(yy - s / 2, xx - s / 2)
    band = np.exp(-(((r - 0.72) / 0.12) ** 2)) * (0.55 + 0.45 * np.sin(th * 23 + nz * 6))
    a = np.clip(band + np.exp(-(((r - 0.72) / 0.28) ** 2)) * 0.28, 0, 1) * (1 - smooth((r - 0.96) / 0.04))
    rgb = ramp(np.clip(np.abs(r - 0.72) * 3.2, 0, 1), HEAT[:5])
    img = np.concatenate([rgb, a[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_ring")


def soft_spark():
    s = 32
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    dx, dy = np.abs(xx - s / 2 + 0.5) / (s / 2), np.abs(yy - s / 2 + 0.5) / (s / 2)
    a = np.clip(1 - np.minimum(dx * 6 + dy * 0.6, dy * 6 + dx * 0.6), 0, 1) ** 1.3 + np.clip(1 - np.sqrt(dx ** 2 + dy ** 2) * 2.6, 0, 1) ** 1.5
    rgb = np.ones((s, s, 3), np.float32) * np.array([255, 236, 150]) / 255
    img = np.concatenate([rgb, np.clip(a, 0, 1)[..., None]], -1)
    return save(Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA"), "B_spark")


def contact_sheet():
    names = sorted(f for f in os.listdir(OUT) if f.endswith(".png") and f != "tex_sheet.png")
    cw = 200
    cols = 8
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cw, rows * (cw + 14)), (60, 62, 70))
    from PIL import ImageDraw as D
    d = D.Draw(sheet)
    for i, n in enumerate(names):
        im = Image.open(os.path.join(OUT, n)).convert("RGBA")
        k = min((cw - 8) / im.width, (cw - 8) / im.height)
        im = im.resize((max(1, int(im.width * k)), max(1, int(im.height * k))), Image.LANCZOS)
        x, y = (i % cols) * cw, (i // cols) * (cw + 14)
        sheet.paste(im, (x + 4, y + 14), im)
        d.text((x + 4, y + 1), n[:-4], fill=(255, 240, 180))
    sheet.save(os.path.join(OUT, "tex_sheet.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    for f in range(3):
        cel_tongue(f)
        cel_ball(f)
        cel_body(f)
    for f in range(2):
        cel_swoosh(f)
        cel_lion(f)
        cel_smoke(f)
        cel_chip(f)
    cel_ring()
    cel_rays()
    cel_flash()
    cel_spark()
    for v in range(4):
        soft_puff(v)
    for v in range(2):
        soft_tongue(v)
    soft_halo()
    soft_streak()
    soft_ember()
    soft_smoke()
    soft_rays()
    soft_ring()
    soft_spark()
    contact_sheet()
    print("textures:", len([f for f in os.listdir(OUT) if f.endswith(".png")]), "->", OUT)


if __name__ == "__main__":
    main()

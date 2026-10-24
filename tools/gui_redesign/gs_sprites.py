"""Great Spirit GUI mock-up textures: every sprite here is generated procedurally at the resolution it would ship at (2 texels per GUI pixel),
so the mock-up shows what the PNGs the mod would carry look like. Three directions (A holo, B parchment, C slime crest) share the generators and
differ by Theme. `TEXTURES` collects (name, w, h) of everything built, for the budget table."""
import math

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

from gs_kit import clamp01, lerp, mixc, ssaa

TEXTURES = {}


class Theme:
    def __init__(self, **kw):
        self.__dict__.update(kw)


A = Theme(
    key="A", name="Holo Grimoire", sub="cool cyan hologram, matrix-heavy",
    panel_top=(9, 24, 46, 205), panel_bot=(4, 11, 26, 220), neon=(84, 222, 255), neon2=(64, 132, 255), glowc=(60, 190, 255),
    text=(223, 246, 255), dim=(122, 164, 196), accent=(124, 240, 200), warn=(255, 176, 96), bad=(255, 96, 120),
    metal_hi=(232, 244, 255), metal_lo=(72, 100, 150), jewel=(90, 230, 255), outline=(10, 22, 44),
    page_top=(14, 40, 70, 150), page_bot=(8, 24, 48, 168), page_edge=(84, 222, 255), ink=(176, 216, 245), data=(110, 255, 230),
    data_dim=(52, 150, 150), cover=(14, 30, 58), cover_hi=(70, 120, 190), core_a=(30, 120, 255), core_b=(150, 235, 255), flow=(80, 255, 220),
    leaf3=((70, 150, 190), (30, 80, 120)), leaf4=((190, 250, 255), (60, 180, 230)), leaf5=((70, 28, 110), (8, 4, 20)), glow4=(110, 240, 255), glow5=(150, 70, 255),
    ripple=(120, 220, 255), btn_top=(24, 70, 120, 220), btn_bot=(10, 36, 78, 230),
)
B = Theme(
    key="B", name="Parchment & Ink", sub="warm parchment and ink, hologram only on data",
    panel_top=(52, 33, 20, 232), panel_bot=(34, 21, 12, 238), neon=(222, 178, 82), neon2=(255, 220, 140), glowc=(255, 200, 110),
    text=(246, 232, 200), dim=(196, 168, 120), accent=(110, 214, 255), warn=(255, 168, 70), bad=(240, 92, 80),
    metal_hi=(255, 232, 150), metal_lo=(112, 72, 18), jewel=(80, 196, 255), outline=(40, 24, 8),
    page_top=(236, 220, 178, 244), page_bot=(212, 190, 142, 244), page_edge=(120, 84, 40), ink=(58, 38, 22), data=(40, 170, 255),
    data_dim=(90, 130, 170), cover=(74, 44, 26), cover_hi=(150, 100, 56), core_a=(34, 120, 230), core_b=(170, 230, 255), flow=(255, 150, 60),
    leaf3=((88, 150, 84), (36, 80, 44)), leaf4=((150, 235, 120), (60, 150, 70)), leaf5=((60, 24, 84), (10, 4, 14)), glow4=(180, 255, 140), glow5=(200, 150, 70),
    ripple=(255, 226, 150), btn_top=(110, 74, 40, 240), btn_bot=(66, 40, 22, 240),
)
C = Theme(
    key="C", name="Slime Crest", sub="dark purple / black corrupt slime",
    panel_top=(22, 9, 36, 214), panel_bot=(9, 4, 17, 232), neon=(166, 90, 255), neon2=(255, 80, 214), glowc=(150, 70, 255),
    text=(236, 226, 248), dim=(158, 130, 184), accent=(130, 255, 96), warn=(255, 160, 80), bad=(255, 80, 120),
    metal_hi=(190, 150, 235), metal_lo=(26, 14, 44), jewel=(255, 80, 214), outline=(8, 3, 14),
    page_top=(30, 12, 46, 196), page_bot=(14, 6, 26, 214), page_edge=(166, 90, 255), ink=(204, 176, 236), data=(255, 110, 230),
    data_dim=(130, 60, 140), cover=(18, 8, 30), cover_hi=(110, 60, 160), core_a=(120, 40, 210), core_b=(255, 120, 230), flow=(190, 110, 255),
    leaf3=((96, 80, 120), (40, 30, 56)), leaf4=((200, 130, 255), (110, 50, 200)), leaf5=((44, 14, 70), (4, 2, 10)), glow4=(190, 110, 255), glow5=(255, 70, 210),
    ripple=(216, 140, 255), btn_top=(60, 26, 100, 224), btn_bot=(26, 10, 50, 236),
)
THEMES = {"A": A, "B": B, "C": C}


def reg(name, im):
    TEXTURES[name] = (im.width, im.height)
    return im


# ------------------------------------------------------------------------------------------------------ drawing helpers (unit space)
def stroke(d, pts, w0, w1, k, fill=255):
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        w = lerp(w0, w1, i / max(1, n - 1)) * k / 2.0
        d.ellipse([x * k - w, y * k - w, x * k + w, y * k + w], fill=fill)


def spiral(cx, cy, r0, r1, turns, a0, direction, n=160):
    pts = []
    for i in range(n):
        t = i / (n - 1.0)
        r = lerp(r0, r1, t ** 0.9)
        a = a0 + direction * turns * 2 * math.pi * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def leaf_poly(x, y, ang, length, width, n=24):
    pts = []
    for i in range(n + 1):
        t = i / n
        px = length * t
        py = width * 0.5 * math.sin(math.pi * t) ** 0.8
        pts.append((px, py))
    pts += [(p[0], -p[1]) for p in reversed(pts)]
    ca, sa = math.cos(ang), math.sin(ang)
    return [(x + p[0] * ca - p[1] * sa, y + p[0] * sa + p[1] * ca) for p in pts]


def heart_pts(cx, cy, size, ang, n=72):
    """Heart outline, tip at (cx, cy), axis along angle `ang` (radians, image coords)."""
    out = []
    ux, uy = math.cos(ang), math.sin(ang)       # axis direction (from tip outwards)
    vx, vy = -uy, ux                              # perpendicular
    for i in range(n):
        t = 2 * math.pi * i / n
        hx = 16 * math.sin(t) ** 3
        hy = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        a = (hy + 17.0) / 29.0 * size             # 0 at the tip .. size at the lobes' top
        b = hx / 16.0 * size * 0.52
        out.append((cx + ux * a + vx * b, cy + uy * a + vy * b))
    return out


def metal(mask, hi, lo, bevel=1.0, diag=0.0):
    """Colourise a float 0..1 mask as bevelled metal: gradient along the diagonal, lit from the top left."""
    h, w = mask.shape
    yy, xx = np.mgrid[0:h, 0:w]
    t = np.clip((xx / w + yy / h) / 2.0 + diag, 0, 1)
    base = np.array(hi, np.float32)[None, None, :] * (1 - t[..., None] ** 0.8) + np.array(lo, np.float32)[None, None, :] * t[..., None] ** 0.8
    k = max(1, int(round(w / 96.0 * bevel)))
    sh = lambda a, dx, dy: np.roll(np.roll(a, dy, 0), dx, 1)
    light = np.clip(mask - sh(mask, k, k), 0, 1)
    dark = np.clip(sh(mask, k, k) - mask, 0, 1) * 0 + np.clip(mask - sh(mask, -k, -k), 0, 1)
    rgb = base * (0.72 + 0.28 * mask[..., None]) + light[..., None] * 90.0 - dark[..., None] * 60.0
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = np.clip(rgb, 0, 255)
    out[..., 3] = mask * 255
    return out


def outline_under(arr, rgb, px=1):
    """Dark one-texel outline behind a float RGBA array (keeps ornaments readable on any background)."""
    a = Image.fromarray(arr[..., 3].astype(np.uint8), "L")
    grown = a.filter(ImageFilter.MaxFilter(2 * px + 1))
    base = np.zeros(arr.shape, np.float32)
    base[..., :3] = rgb
    base[..., 3] = np.asarray(grown).astype(np.float32) * 0.85
    al = arr[..., 3:4] / 255.0
    out = arr.copy()
    out[..., :3] = arr[..., :3] * al + base[..., :3] * (1 - al)
    out[..., 3] = np.maximum(arr[..., 3], base[..., 3])
    return out


# ------------------------------------------------------------------------------------------------------ filigree corner
def filigree_corner(th, size=96):
    """The ornate metallic corner of a Grimoire cover, top-left. Unit space 48 (2 texels per unit)."""
    ss = 8
    k = size / 48.0 * ss

    def draw(d, _):
        # long arms along the top and the left, tapering
        def arm(horizontal):
            pts = [(x, 4.5) for x in np.linspace(4.0, 47.0, 80)]
            stroke(d, pts if horizontal else [(y, x) for x, y in pts], 3.0, 0.9, k)
            pts2 = [(x, 8.3) for x in np.linspace(13.0, 38.0, 50)]
            stroke(d, pts2 if horizontal else [(y, x) for x, y in pts2], 0.9, 0.5, k)

        for hz in (True, False):
            arm(hz)
            sgn = (lambda p: p) if hz else (lambda p: (p[1], p[0]))
            # scroll curls under the arm
            sp = spiral(19.5, 13.0, 6.2, 1.1, 1.15, -math.pi * 0.85, 1, 180)
            stroke(d, [sgn(p) for p in sp], 1.7, 0.5, k)
            sp2 = spiral(29.5, 11.2, 3.6, 0.8, 1.05, -math.pi * 0.2, -1, 120)
            stroke(d, [sgn(p) for p in sp2], 1.2, 0.4, k)
            # leaves into the panel
            for lx, ang, ln, wd in ((24.0, 1.2, 8.0, 3.4), (35.0, 1.15, 6.0, 2.6), (44.0, 1.1, 3.8, 1.7)):
                poly = leaf_poly(lx, 5.0, ang, ln, wd)
                d.polygon([(sgn(p)[0] * k, sgn(p)[1] * k) for p in poly], fill=255)
            for bx in (31.0, 41.0):
                r = 0.95 * k
                px, py = sgn((bx, 4.5))
                d.ellipse([px * k - r, py * k - r, px * k + r, py * k + r], fill=255)
        # corner plate and its gem seat
        d.rounded_rectangle([2.5 * k, 2.5 * k, 13.5 * k, 13.5 * k], radius=2.5 * k, fill=255)
        for a in range(4):
            ang = math.pi / 4 + a * math.pi / 2
            cx, cy = 8.0 + 4.4 * math.cos(ang) * 0.0, 8.0
            r = 2.0 * k
            ox, oy = 8.0 + 3.0 * math.cos(ang), 8.0 + 3.0 * math.sin(ang)
            d.ellipse([ox * k - r, oy * k - r, ox * k + r, oy * k + r], fill=255)

    big = Image.new("L", (size * ss, size * ss), 0)
    draw(ImageDraw.Draw(big), ss)
    mask = np.asarray(big.resize((size, size), Image.LANCZOS)).astype(np.float32) / 255.0
    arr = metal(mask, th.metal_hi, th.metal_lo)
    arr = outline_under(arr, th.outline, 1)
    im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    # the gem
    d = ImageDraw.Draw(im)
    gx = gy = 8.0 * size / 48.0
    r = 3.0 * size / 48.0
    d.ellipse([gx - r, gy - r, gx + r, gy + r], fill=th.outline + (255,))
    r2 = r * 0.72
    d.ellipse([gx - r2, gy - r2, gx + r2, gy + r2], fill=th.jewel + (255,))
    d.ellipse([gx - r2 * 0.55, gy - r2 * 0.75, gx - r2 * 0.05, gy - r2 * 0.25], fill=(255, 255, 255, 220))
    if th.key == "C":
        im = add_drips(im, th, size)
    return reg("gui/gs_%s/corner_filigree" % th.key.lower(), im)


def add_drips(im, th, size):
    """Slime drips hanging from the corner's arms (direction C)."""
    ss = 6
    big = Image.new("RGBA", (size * ss, size * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    k = size / 48.0 * ss
    for ux, ln in ((17.0, 6.5), (33.0, 9.0), (42.0, 4.5)):
        for (x, y) in ((ux, 6.0), (6.0 + 0, ux)):
            pass
        for hz in (True, False):
            px, py = (ux, 6.5) if hz else (6.5, ux)
            w = 1.1 * k
            ex, ey = (px, py + ln) if hz else (px + ln, py)
            d.line([px * k, py * k, ex * k, ey * k], fill=th.neon2 + (230,), width=int(w))
            r = 1.35 * k
            d.ellipse([ex * k - r, ey * k - r, ex * k + r, ey * k + r], fill=th.neon2 + (230,))
            r = 0.5 * k
            d.ellipse([(ex - 0.5) * k - r, (ey - 0.7) * k - r, (ex - 0.5) * k + r, (ey - 0.7) * k + r], fill=(255, 255, 255, 170))
    big = big.resize((size, size), Image.LANCZOS)
    out = im.copy()
    out.alpha_composite(big)
    return out


# ------------------------------------------------------------------------------------------------------ 9-slice glass panel edge handled by the canvas; neon is drawn live (additive glow quads)


# ------------------------------------------------------------------------------------------------------ gauge textures
def ring_bezel(th, size=192):
    """The static rim of the concentric gauge: outer groove, ticks, four cardinal ornaments, inner rim. (the fill is drawn under it.)"""
    ss = 4
    S = size * ss
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2.0
    R = S / 2.0 - 2 * ss

    def ring(r0, r1, col):
        d.ellipse([c - r1, c - r1, c + r1, c + r1], fill=col)
        d.ellipse([c - r0, c - r0, c + r0, c + r0], fill=(0, 0, 0, 0))

    metal_c = mixc(th.metal_hi, th.metal_lo, 0.35)
    ring(R * 0.86, R, metal_c + (255,))                              # outer bezel
    ring(R * 0.865, R * 0.885, mixc(th.metal_hi, th.metal_lo, 0.0) + (255,))
    ring(R * 0.96, R * 0.985, mixc(th.metal_lo, (0, 0, 0), 0.4) + (255,))
    # ticks around the outer lane (between the mana lane and the bezel)
    for i in range(60):
        a = i / 60.0 * 2 * math.pi - math.pi / 2
        long = i % 5 == 0
        r0, r1 = R * (0.835 if long else 0.845), R * 0.86
        d.line([c + r0 * math.cos(a), c + r0 * math.sin(a), c + r1 * math.cos(a), c + r1 * math.sin(a)], fill=th.metal_hi + (230,), width=int((1.4 if long else 0.8) * ss))
    # divider between mana lane and the core
    ring(R * 0.545, R * 0.60, metal_c + (255,))
    ring(R * 0.55, R * 0.565, th.metal_hi + (255,))
    # four cardinal diamonds
    for a in range(4):
        ang = a * math.pi / 2 - math.pi / 2
        px, py = c + R * 0.93 * math.cos(ang), c + R * 0.93 * math.sin(ang)
        s_ = R * 0.07
        d.polygon([(px, py - s_ * 1.4), (px + s_, py), (px, py + s_ * 1.4), (px - s_, py)], fill=th.metal_hi + (255,), outline=th.outline + (255,))
        d.ellipse([px - s_ * 0.35, py - s_ * 0.35, px + s_ * 0.35, py + s_ * 0.35], fill=th.jewel + (255,))
    im = im.resize((size, size), Image.LANCZOS)
    return reg("gui/gs_%s/gauge_bezel" % th.key.lower(), im)


def flow_strip(th, w=256, h=24, seed=3):
    """White-on-transparent particle strip tiled along the mana lane (u along the arc, v across the lane); tinted by the grimoire element in game."""
    rng = np.random.RandomState(seed)
    ss = 4
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # lane body: faint continuous fill so an unlit lane still reads
    for y in range(h * ss):
        v = (y / (h * ss) - 0.5) * 2
        a = int(70 * (1 - abs(v) ** 2))
        d.line([0, y, w * ss, y], fill=(255, 255, 255, a))
    n = 46
    for i in range(n):
        x = rng.uniform(0, w) * ss
        y = rng.uniform(0.12, 0.88) * h * ss
        ln = rng.uniform(8, 34) * ss if th.key != "C" else rng.uniform(4, 14) * ss
        wd = rng.uniform(0.9, 2.3) * ss
        for j in range(int(ln)):
            t = j / ln
            a = int(255 * (1 - t) ** 1.4)
            xx = (x - j) % (w * ss)
            r = wd * (1 - 0.6 * t) / 2 if th.key != "B" else wd * (0.6 + 0.4 * (1 - t)) / 2
            d.ellipse([xx - r, y - r, xx + r, y + r], fill=(255, 255, 255, a))
        # head spark
        r = wd * 0.9
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 255))
    for i in range(30):
        x, y = rng.uniform(0, w) * ss, rng.uniform(0.05, 0.95) * h * ss
        r = rng.uniform(0.4, 0.9) * ss
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 230))
    im = im.resize((w, h), Image.LANCZOS)
    return reg("gui/gs_%s/gauge_flow_strip" % th.key.lower(), im)


def core_swirl(th, size=128, seed=5):
    """The slime core's swirling liquid (greyscale light/dark arms + bubbles, alpha = body), rotated and scrolled in game."""
    rng = np.random.RandomState(seed)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    cx = cy = size / 2.0 - 0.5
    dx, dy = xx - cx, yy - cy
    r = np.sqrt(dx * dx + dy * dy) / (size / 2.0)
    th_ = np.arctan2(dy, dx)
    arms = 0.5 + 0.5 * np.sin(th_ * 3 + r * 7.5 + 0.4 * np.sin(th_ * 5 - r * 4))
    arms2 = 0.5 + 0.5 * np.sin(th_ * 2 - r * 11 + 1.3)
    body = np.clip((1 - r) * 5, 0, 1)
    val = 0.35 + 0.45 * arms * (0.6 + 0.4 * arms2)
    val = val * (0.65 + 0.35 * (1 - r))
    img = np.zeros((size, size, 4), np.float32)
    img[..., :3] = (val * 255)[..., None]
    img[..., 3] = body * 255
    im = Image.fromarray(img.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    for i in range(7):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0.15, 0.75) * size / 2
        bx, by = cx + rr * math.cos(a), cy + rr * math.sin(a)
        br = rng.uniform(1.6, 4.2)
        d.ellipse([bx - br, by - br, bx + br, by + br], outline=(255, 255, 255, 200))
        d.ellipse([bx - br * 0.5, by - br * 0.7, bx - br * 0.05, by - br * 0.2], fill=(255, 255, 255, 230))
    return reg("gui/gs_%s/gauge_core_swirl" % th.key.lower(), im)


# ------------------------------------------------------------------------------------------------------ clover icon frames
def clover_frame(th, leaves, size=96):
    """The clover plate for an icon: leaves around a medallion hole (alpha 0, the skill art shows through). Returns (frame, glow, drips)."""
    ss = 6
    S = size * ss
    c = S / 2.0
    n = leaves
    tier = {3: 3, 4: 4, 5: 5}[n]
    fill_hi, fill_lo = {3: th.leaf3, 4: th.leaf4, 5: th.leaf5}[tier]
    leaf_len = S * (0.36 if n == 3 else 0.34 if n == 4 else 0.30) / 1.62 * 1.0
    start = -math.pi / 2
    body = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gl = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)
    angs = [start + i * 2 * math.pi / n for i in range(n)]
    for a in angs:
        ox, oy = c + math.cos(a) * S * 0.06, c + math.sin(a) * S * 0.06
        md.polygon(heart_pts(ox, oy, leaf_len * 1.62, a), fill=255)
    # a short stem for the three- and four-leaf (down-right), none for five (it is wrapped in slime)
    if n == 3:
        for t in np.linspace(0.0, 1.0, 40):
            x, y = c + math.sin(t * 2.2) * S * 0.05 + t * S * 0.02, c + S * 0.14 + t * S * 0.34
            md.ellipse([x - S * 0.014, y - S * 0.014, x + S * 0.014, y + S * 0.014], fill=255)
    elif n == 4:
        for t in np.linspace(0.0, 1.0, 40):
            x, y = c + S * 0.14 + t * S * 0.22 + math.sin(t * 2.5) * S * 0.02, c + S * 0.14 + t * S * 0.34
            md.ellipse([x - S * 0.014, y - S * 0.014, x + S * 0.014, y + S * 0.014], fill=255)
    m = np.asarray(mask).astype(np.float32) / 255.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    t = np.clip(((xx - c) * 0.5 + (yy - c) * 0.8) / S + 0.5, 0, 1)
    col = np.array(fill_hi, np.float32)[None, None, :] * (1 - t[..., None]) + np.array(fill_lo, np.float32)[None, None, :] * t[..., None]
    if tier == 5:
        # corrupt slime: dark body, violet rim light on the lit side, glossy blobs
        rim = np.clip(m - np.roll(np.roll(m, 14, 0), 14, 1), 0, 1)
        col = col + rim[..., None] * np.array(th.glow5, np.float32)[None, None, :] * 0.9
    elif tier == 4:
        rim = np.clip(m - np.roll(np.roll(m, 10, 0), 10, 1), 0, 1)
        col = col + rim[..., None] * 90.0
    arr = np.zeros((S, S, 4), np.float32)
    arr[..., :3] = np.clip(col, 0, 255)
    arr[..., 3] = m * 255
    body = Image.fromarray(arr.astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(body)
    # veins and outlines
    edge = Image.fromarray((np.clip(m - np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(9))).astype(np.float32) / 255.0, 0, 1) * 255).astype(np.uint8), "L")
    oc = {3: mixc(th.neon, th.outline, 0.35), 4: th.metal_hi, 5: th.glow5}[tier]
    oa = {3: 150, 4: 255, 5: 230}[tier]
    edge_img = Image.new("RGBA", (S, S), oc + (0,))
    edge_img.putalpha(edge.point(lambda v: int(v * oa / 255)))
    body.alpha_composite(edge_img)
    for a in angs:
        ox, oy = c + math.cos(a) * S * 0.10, c + math.sin(a) * S * 0.10
        ex, ey = c + math.cos(a) * (S * 0.10 + leaf_len * 1.05), c + math.sin(a) * (S * 0.10 + leaf_len * 1.05)
        vc = {3: mixc(fill_hi, fill_lo, 0.5), 4: (255, 255, 255), 5: th.glow5}[tier]
        d.line([ox, oy, ex, ey], fill=vc + ({3: 110, 4: 190, 5: 200}[tier],), width=int(S * 0.008))
    if tier == 5:
        for a in angs:                                       # glossy slime highlights
            hx, hy = c + math.cos(a) * S * 0.30 - S * 0.02, c + math.sin(a) * S * 0.30 - S * 0.03
            d.ellipse([hx - S * 0.035, hy - S * 0.022, hx + S * 0.035, hy + S * 0.022], fill=(255, 235, 255, 150))
    # medallion ring + hole
    holeR = S * 0.175
    rim = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    rd = ImageDraw.Draw(rim)
    rimc = {3: mixc(th.metal_hi, th.metal_lo, 0.5), 4: th.metal_hi, 5: mixc(th.glow5, (30, 10, 50), 0.4)}[tier]
    rd.ellipse([c - holeR - S * 0.035, c - holeR - S * 0.035, c + holeR + S * 0.035, c + holeR + S * 0.035], fill=rimc + (255,))
    rd.ellipse([c - holeR - S * 0.012, c - holeR - S * 0.012, c + holeR + S * 0.012, c + holeR + S * 0.012], fill=th.outline + (255,))
    body.alpha_composite(rim)
    hole = Image.new("L", (S, S), 255)
    ImageDraw.Draw(hole).ellipse([c - holeR, c - holeR, c + holeR, c + holeR], fill=0)
    body.putalpha(ImageChops.multiply(body.getchannel("A"), hole))
    # soft contact shadow under the plate keeps it readable on any panel
    drips = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    if tier == 5:
        dd = ImageDraw.Draw(drips)
        for a in angs:
            bx = c + math.cos(a) * S * 0.38
            by = c + math.sin(a) * S * 0.38
            if by < c - S * 0.1:
                continue
            ln = S * (0.07 + 0.05 * abs(math.sin(a * 3)))
            w_ = S * 0.017
            dd.line([bx, by, bx, by + ln], fill=th.leaf5[0] + (255,), width=int(w_ * 2))
            dd.ellipse([bx - w_ * 1.4, by + ln - w_, bx + w_ * 1.4, by + ln + w_ * 2.2], fill=mixc(th.leaf5[0], th.glow5, 0.35) + (255,))
            dd.ellipse([bx - w_ * 0.8, by + ln - w_ * 0.3, bx - w_ * 0.1, by + ln + w_ * 0.5], fill=(255, 235, 255, 170))
    # glow layer (additive in game; pulses)
    gm = mask.filter(ImageFilter.GaussianBlur(S * 0.05))
    gc = {3: th.neon, 4: th.glow4, 5: th.glow5}[tier]
    ga = {3: 0, 4: 150, 5: 120}[tier]
    gl = Image.new("RGBA", (S, S), gc + (0,))
    gl.putalpha(gm.point(lambda v: int(min(255, v * ga / 255 * 1.6))))
    body = body.resize((size, size), Image.LANCZOS)
    gl = gl.resize((size, size), Image.LANCZOS)
    drips = drips.resize((size, size), Image.LANCZOS)
    reg("gui/gs_%s/clover%d_frame" % (th.key.lower(), n), body)
    if tier >= 4:
        reg("gui/gs_%s/clover%d_glow" % (th.key.lower(), n), gl)
    if tier == 5:
        reg("gui/gs_%s/clover5_drips" % th.key.lower(), drips)
    return body, gl, drips


# ------------------------------------------------------------------------------------------------------ crests for the alert widget
def crest(th, size=96):
    ss = 6
    S = size * ss
    c = S / 2.0
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if th.key == "A":                       # Magic Knight squad insignia: winged shield with a four-leaf clover
        for sgn in (-1, 1):
            for i in range(6):
                a0 = -0.35 + i * 0.16
                x0, y0 = c + sgn * S * 0.17, c - S * 0.06 + i * S * 0.027
                x1, y1 = c + sgn * (S * 0.46 - i * S * 0.015), c - S * 0.22 + i * S * 0.062 + (i * i) * S * 0.004
                d.polygon([(x0, y0), (x1, y1), (x1 - sgn * S * 0.03, y1 + S * 0.05), (x0, y0 + S * 0.05)], fill=mixc(th.metal_hi, th.metal_lo, i / 7.0) + (255,), outline=th.outline + (255,))
        sh = [(c - S * 0.2, c - S * 0.27), (c + S * 0.2, c - S * 0.27), (c + S * 0.2, c + S * 0.05), (c, c + S * 0.31), (c - S * 0.2, c + S * 0.05)]
        d.polygon(sh, fill=(10, 28, 54, 255), outline=th.neon + (255,))
        d.line(sh + [sh[0]], fill=th.neon + (255,), width=int(S * 0.018))
        inner = [(x * 0.82 + c * 0.18, y * 0.82 + c * 0.18 + S * 0.01) for x, y in sh]
        d.line(inner + [inner[0]], fill=th.metal_hi + (200,), width=int(S * 0.008))
        for a in range(4):
            ang = math.pi / 4 + a * math.pi / 2
            d.polygon(heart_pts(c + math.cos(ang) * S * 0.014, c - S * 0.07 + math.sin(ang) * S * 0.014, S * 0.13, ang), fill=th.neon + (255,), outline=th.outline + (255,))
        d.ellipse([c - S * 0.018, c - S * 0.088, c + S * 0.018, c - S * 0.052], fill=(255, 255, 255, 255))
    elif th.key == "B":                     # a gilded seal: ring of beads, parchment disc, four-leaf clover, ink eye
        R = S * 0.42
        d.ellipse([c - R, c - R, c + R, c + R], fill=mixc(th.metal_hi, th.metal_lo, 0.35) + (255,), outline=th.outline + (255,))
        for i in range(36):
            a = i / 36 * 2 * math.pi
            r = S * 0.014
            d.ellipse([c + R * 0.9 * math.cos(a) - r, c + R * 0.9 * math.sin(a) - r, c + R * 0.9 * math.cos(a) + r, c + R * 0.9 * math.sin(a) + r], fill=th.metal_hi + (255,))
        R2 = R * 0.78
        d.ellipse([c - R2, c - R2, c + R2, c + R2], fill=(222, 202, 156, 255), outline=th.metal_lo + (255,), width=int(S * 0.012))
        for a in range(4):
            ang = math.pi / 4 + a * math.pi / 2
            d.polygon(heart_pts(c + math.cos(ang) * S * 0.01, c + math.sin(ang) * S * 0.01, S * 0.24, ang), fill=mixc(th.leaf4[0], th.leaf4[1], 0.5) + (255,), outline=th.outline + (255,))
        d.ellipse([c - S * 0.03, c - S * 0.03, c + S * 0.03, c + S * 0.03], fill=th.jewel + (255,), outline=th.outline + (255,))
    else:                                   # a corrupt slime face
        R = S * 0.38
        pts = []
        for i in range(120):
            a = i / 120 * 2 * math.pi
            rr = R * (1.0 + 0.05 * math.sin(a * 3 + 0.5))
            pts.append((c + rr * math.cos(a), c + S * 0.03 + rr * 0.92 * math.sin(a)))
        d.polygon(pts, fill=(34, 12, 56, 255), outline=th.neon + (255,))
        d.line(pts + [pts[0]], fill=th.neon + (255,), width=int(S * 0.016))
        for bx, ln in ((-0.22, 0.14), (0.05, 0.19), (0.24, 0.1)):
            x, y = c + bx * S, c + S * 0.36
            d.line([x, y, x, y + ln * S * 0.6], fill=(34, 12, 56, 255), width=int(S * 0.034))
            d.ellipse([x - S * 0.026, y + ln * S * 0.6 - S * 0.012, x + S * 0.026, y + ln * S * 0.6 + S * 0.04], fill=(34, 12, 56, 255))
        for sx in (-1, 1):
            ex, ey = c + sx * S * 0.15, c - S * 0.02
            d.ellipse([ex - S * 0.07, ey - S * 0.095, ex + S * 0.07, ey + S * 0.095], fill=(8, 2, 14, 255))
            d.ellipse([ex - S * 0.045, ey - S * 0.07, ex + S * 0.045, ey + S * 0.07], fill=th.neon2 + (255,))
            d.ellipse([ex - S * 0.022, ey - S * 0.062, ex + S * 0.006, ey - S * 0.024], fill=(255, 255, 255, 230))
        d.arc([c - S * 0.16, c + S * 0.0, c + S * 0.16, c + S * 0.2], 15, 165, fill=th.neon2 + (255,), width=int(S * 0.022))
        for a in range(4):                                          # clover mark on the brow
            ang = math.pi / 4 + a * math.pi / 2
            d.polygon(heart_pts(c + math.cos(ang) * S * 0.006, c - S * 0.20 + math.sin(ang) * S * 0.006, S * 0.095, ang), fill=th.accent + (255,))
        d.ellipse([c - S * 0.30, c - S * 0.24, c - S * 0.1, c - S * 0.12], fill=(255, 255, 255, 40))
    im = im.resize((size, size), Image.LANCZOS)
    return reg("gui/gs_%s/alert_crest" % th.key.lower(), im)


def ripple_ring(th, size=64):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.sqrt((xx - size / 2 + 0.5) ** 2 + (yy - size / 2 + 0.5) ** 2) / (size / 2.0)
    a = np.exp(-((r - 0.82) / 0.07) ** 2) + 0.35 * np.exp(-((r - 0.62) / 0.05) ** 2)
    a *= np.clip((1 - r) * 5, 0, 1)
    arr = np.zeros((size, size, 4), np.uint8)
    arr[..., :3] = 255
    arr[..., 3] = np.clip(a * 255, 0, 255).astype(np.uint8)
    return reg("gui/gs_%s/ripple_ring" % th.key.lower(), Image.fromarray(arr, "RGBA"))

"""Generates the Food Magic VFX textures (Glutton's Banquet look: manga ink, a furred beast with a gaping maw, giant cutlery).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/food_*.png

    python3 -B tools/gen_food_textures.py

Look reference (owner art, docs/attributes/art_reference/pack_food.webp): black-and-white manga ink: a huge shaggy beast with
hatched fur, slit eyes and a fanged maw, a giant fork and knife, a heap of food debris, thick radial speed lines and a spiky
impact bubble. So the textures are ink (alpha = linework, tinted dark in game) and light (white, tinted gold / cream).
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 3


def save(name, rgb, alpha):
    """rgb: HxWx3 float 0..1 (or None for white), alpha: HxW float 0..1."""
    a = np.clip(alpha, 0, 1)
    if rgb is None:
        rgb = np.ones(a.shape + (3,))
    img = np.dstack([np.clip(rgb, 0, 1), a])
    Image.fromarray((img * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "food_%s.png" % name))


def noise(n, seed, cells=(4, 8, 16, 32)):
    rng = np.random.RandomState(seed)
    acc = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for c in cells:
        g = rng.rand(c, c).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC)
        acc += np.asarray(im, dtype=np.float64) / 255.0 * amp
        tot += amp
        amp *= 0.55
    return acc / tot


def mask_img(n):
    return Image.new("L", (n * SS, n * SS), 0)


def down(im, n):
    return np.asarray(im.resize((n, n), Image.LANCZOS), dtype=np.float64) / 255.0


def grid(n):
    y, x = np.mgrid[0:n, 0:n]
    return (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1


def dilate(m, px):
    if px <= 0:
        return m
    return m.filter(ImageFilter.MaxFilter(px * 2 + 1))


def erode(m, px):
    if px <= 0:
        return m
    return m.filter(ImageFilter.MinFilter(px * 2 + 1))


def soft(arr, e0, e1):
    t = np.clip((arr - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# ------------------------------------------------------------------------------------------------ cutlery (steel + ink)
def steel_piece(name, poly_fn, n=256, seed=1):
    """poly_fn(draw, s) draws the silhouette in white on a mask; shaded as ink-outlined steel with hatch shading lines."""
    m = mask_img(n)
    poly_fn(ImageDraw.Draw(m), n * SS / 256.0)
    outer = m
    inner = erode(m, 3 * SS // 2 + 2)
    o, i = down(outer, n), down(inner, n)
    x, y = grid(n)
    # steel: bright left, darker right, a bright rim line, diagonal ink hatching on the shaded side
    shade = 0.92 - 0.38 * soft(x, -0.1, 0.35) + 0.08 * (noise(n, seed) - 0.5)
    hatch = ((x * 0.7 + y) * 38.0) % 1.0
    lines = soft(np.abs(hatch - 0.5), 0.34, 0.5) * soft(x, 0.02, 0.28)
    lum = shade * (1 - 0.55 * lines)
    edge = np.clip(o - i, 0, 1)
    lum = np.where(edge > 0.5, 0.06, lum)
    lum = lum * (0.7 + 0.3 * i) + 0.03
    rgb = np.dstack([lum * 1.0, lum * 0.98, lum * 0.94])
    save(name, rgb, o)


def fork_poly(d, s):
    def P(pts):
        return [(x * s, y * s) for x, y in pts]
    for k in range(4):
        x0 = 86 + k * 22
        d.polygon(P([(x0, 100), (x0 + 15, 100), (x0 + 15, 26), (x0 + 7.5, 6), (x0, 26)]), fill=255)
    d.polygon(P([(86, 92), (165, 92), (165, 112), (152, 138), (140, 156), (137, 248), (118, 248), (115, 156), (103, 138), (86, 112)]), fill=255)
    d.ellipse([112 * s, 236 * s, 143 * s, 254 * s], fill=255)


def knife_poly(d, s):
    def P(pts):
        return [(x * s, y * s) for x, y in pts]
    d.polygon(P([(112, 4), (131, 22), (145, 56), (153, 100), (156, 160), (112, 160)]), fill=255)
    d.rectangle([108 * s, 158 * s, 160 * s, 172 * s], fill=255)
    d.rounded_rectangle([114 * s, 168 * s, 154 * s, 250 * s], radius=int(16 * s), fill=255)


def gen_cutlery():
    steel_piece("fork", fork_poly, seed=11)
    steel_piece("knife", knife_poly, seed=12)


# ------------------------------------------------------------------------------------------------------- the beast maw
def gen_maw(n=256):
    rnd = random.Random(31)
    ink = mask_img(n)
    light = mask_img(n)
    di, dl = ImageDraw.Draw(ink), ImageDraw.Draw(light)
    S = n * SS / 256.0
    cx, cy = 128 * S, 134 * S
    rx, ry = 104 * S, 62 * S
    # dark throat: a wide ragged ellipse
    pts = []
    for k in range(72):
        a = k / 72 * math.tau
        r = 1 + 0.045 * math.sin(a * 7 + 1) + rnd.uniform(-0.025, 0.025)
        pts.append((cx + math.cos(a) * rx * r, cy + math.sin(a) * ry * r))
    di.polygon(pts, fill=255)
    # fangs: teeth hang from the top lip and rise from the bottom lip; carved out of the dark, then drawn in the light layer
    tooth_masks = mask_img(n)
    dt = ImageDraw.Draw(tooth_masks)
    for row in (0, 1):
        cnt = 9
        for k in range(cnt):
            u = (k + 0.5) / cnt * 2 - 1
            if abs(u) > 0.92:
                continue
            ex = cx + u * rx * 0.93
            ey = cy + (-1 if row == 0 else 1) * ry * math.sqrt(max(0.0, 1 - u * u * 0.86)) * 0.97
            big = abs(u) < 0.18 or abs(abs(u) - 0.62) < 0.1
            ln = (50 if big else 26) * S * (0.8 + 0.4 * rnd.random()) * (0.75 if row else 1.0)
            wd = (15 if big else 11) * S
            dirv = 1 if row == 0 else -1
            tip = (ex + u * 5 * S, ey + dirv * ln)
            dt.polygon([(ex - wd, ey - dirv * 6 * S), (ex + wd, ey - dirv * 6 * S), tip], fill=255)
    tm = tooth_masks
    # outline the teeth in ink, then punch the tooth bodies so the light layer shows through
    ink_arr = ImageChops_subtract(ink, tm)
    outline = ImageChops_subtract(dilate(tm, SS + 1), erode(tm, SS // 2))
    ink = ImageChops_add(ink_arr, outline)
    # lip line, thick ink arcs above and below
    di = ImageDraw.Draw(ink)
    for sgn in (-1, 1):
        for w in (7, 3):
            box = [cx - rx * 1.04, cy - ry * 1.12, cx + rx * 1.04, cy + ry * 1.12]
            di.arc(box, 180 if sgn < 0 else 0, 360 if sgn < 0 else 180, fill=255, width=int(w * S))
    # drool: thin vertical strings from the lower teeth
    for k in range(5):
        x0 = cx + (k - 2) * 30 * S + rnd.uniform(-8, 8) * S
        y0 = cy + ry * 0.9
        ln = rnd.uniform(14, 40) * S
        di.line([(x0, y0), (x0 + rnd.uniform(-3, 3) * S, y0 + ln)], fill=255, width=int(2.2 * S))
        di.ellipse([x0 - 3 * S, y0 + ln - 2 * S, x0 + 3 * S, y0 + ln + 5 * S], fill=255)
    # tongue glow + teeth in the light layer; slit eyes above the maw
    tongue = mask_img(n)
    dtn = ImageDraw.Draw(tongue)
    dtn.ellipse([cx - 56 * S, cy + 6 * S, cx + 56 * S, cy + 52 * S], fill=170)
    light_arr = ImageChops_add(ImageChops_add(light, tm), tongue)
    ld = ImageDraw.Draw(light_arr)
    for sx in (-1, 1):
        ex, ey = cx + sx * 62 * S, 30 * S
        ld.polygon([(ex - 24 * S, ey + 6 * S), (ex, ey - 8 * S), (ex + 24 * S, ey + 6 * S), (ex, ey + 12 * S)], fill=255)
        di.polygon([(ex - 30 * S, ey + 9 * S), (ex, ey - 13 * S), (ex + 30 * S, ey + 9 * S), (ex, ey + 18 * S)], outline=255, width=int(3 * S))
        di.ellipse([ex - 4 * S, ey - 5 * S, ex + 4 * S, ey + 7 * S], fill=255)
    # a heavy nose bridge ink
    di.polygon([(cx - 14 * S, 58 * S), (cx + 14 * S, 58 * S), (cx, 82 * S)], fill=255)
    a_ink = down(ink.filter(ImageFilter.GaussianBlur(0.6 * SS)), n)
    a_light = down(light_arr.filter(ImageFilter.GaussianBlur(0.6 * SS)), n)
    nz = noise(n, 33, (6, 16, 40))
    a_light = a_light * (0.8 + 0.2 * nz)
    save("maw_ink", None, a_ink)
    save("maw_light", None, a_light)


def ImageChops_subtract(a, b):
    from PIL import ImageChops
    return ImageChops.subtract(a, b)


def ImageChops_add(a, b):
    from PIL import ImageChops
    return ImageChops.add(a, b)


# ------------------------------------------------------------------------------------------------------ fur / speed lines
def spikes(name, n, seed, count, r0, r1, l0, l1, curl, width, fine=0):
    rnd = random.Random(seed)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    S = n * SS / 256.0
    c = 128 * S
    for k in range(count):
        a = (k + rnd.uniform(-0.45, 0.45)) / count * math.tau
        r_a = rnd.uniform(r0, r1) * 128 * S
        ln = rnd.uniform(l0, l1) * 128 * S
        bend = rnd.uniform(-curl, curl)
        w = width * S * rnd.uniform(0.6, 1.4)
        ca, sa = math.cos(a), math.sin(a)
        ba = a + bend
        tip = (c + math.cos(ba) * (r_a + ln), c + math.sin(ba) * (r_a + ln))
        mid = (c + math.cos(a + bend * 0.4) * (r_a + ln * 0.5), c + math.sin(a + bend * 0.4) * (r_a + ln * 0.5))
        b0 = (c + ca * r_a, c + sa * r_a)
        px, py = -sa * w, ca * w
        d.polygon([(b0[0] - px, b0[1] - py), (mid[0] - px * 0.6, mid[1] - py * 0.6), tip, (mid[0] + px * 0.6, mid[1] + py * 0.6), (b0[0] + px, b0[1] + py)], fill=255)
    for k in range(fine):
        a = rnd.uniform(0, math.tau)
        r_a = rnd.uniform(r0 - 0.1, r1) * 128 * S
        ln = rnd.uniform(0.05, 0.16) * 128 * S
        d.line([(c + math.cos(a) * r_a, c + math.sin(a) * r_a), (c + math.cos(a + 0.05) * (r_a + ln), c + math.sin(a + 0.05) * (r_a + ln))], fill=255, width=max(1, int(1.2 * S)))
    return m


def gen_fur_speed():
    m = spikes("fur", 256, 41, 120, 0.46, 0.62, 0.16, 0.38, 0.25, 4.6, fine=260)
    a = down(m.filter(ImageFilter.GaussianBlur(0.5 * SS)), 256)
    x, y = grid(256)
    r = np.sqrt(x * x + y * y)
    a *= soft(1.0 - r, 0.0, 0.05)
    save("fur", None, a)
    # radial speed lines (ink): long needles from a clear centre to the rim
    m = spikes("speed", 256, 42, 90, 0.2, 0.55, 0.25, 0.5, 0.0, 2.6)
    a = down(m.filter(ImageFilter.GaussianBlur(0.4 * SS)), 256)
    r = np.sqrt(x * x + y * y)
    a *= soft(1.0 - r, 0.0, 0.1) * soft(r, 0.18, 0.3)
    save("speed", None, a)


def gen_burst(n=256):
    """Spiky manga impact bubble: white body with a bright core, ragged tips."""
    rnd = random.Random(51)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    S = n * SS / 256.0
    pts = []
    k = 22
    for i in range(k * 2):
        a = i / (k * 2) * math.tau
        r = (0.96 if i % 2 == 0 else 0.52) * (0.8 + 0.2 * rnd.random() if i % 2 == 0 else 0.9 + 0.1 * rnd.random())
        pts.append((128 * S + math.cos(a) * r * 126 * S, 128 * S + math.sin(a) * r * 126 * S))
    d.polygon(pts, fill=255)
    a = down(m.filter(ImageFilter.GaussianBlur(0.5 * SS)), n)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    core = soft(1 - r * 1.15, 0, 1)
    rgb = np.dstack([np.ones_like(a), np.ones_like(a), np.ones_like(a)])
    save("burst", rgb, a * (0.55 + 0.45 * core))
    inner = down(erode(m, 4 * SS), n)
    save("burst_ink", None, np.clip(a - inner, 0, 1))


def gen_hatch(n=128):
    """Beam strip: U across, V along. Ink streaks along V, dense in the centre, plus a few crossing hatch ticks."""
    rnd = random.Random(61)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    S = n * SS / 128.0
    for k in range(46):
        u = rnd.gauss(0.5, 0.2)
        if not 0.04 < u < 0.96:
            continue
        v0 = rnd.uniform(-0.2, 1.0)
        ln = rnd.uniform(0.25, 0.9)
        w = rnd.uniform(0.8, 2.6) * S * (1.4 - abs(u - 0.5))
        d.line([(u * n * SS, v0 * n * SS), (u * n * SS + rnd.uniform(-2, 2) * S, (v0 + ln) * n * SS)], fill=255, width=int(max(1, w)))
    for k in range(10):
        u = rnd.uniform(0.2, 0.8)
        v = rnd.uniform(0, 1)
        d.line([((u - 0.12) * n * SS, (v + 0.04) * n * SS), ((u + 0.12) * n * SS, (v - 0.04) * n * SS)], fill=255, width=int(1.2 * S))
    a = down(m.filter(ImageFilter.GaussianBlur(0.4 * SS)), n)
    x, y = grid(n)
    env = soft(1 - np.abs(x), 0.0, 0.4)
    save("hatch", None, a * env)
    # a soft light core strip for the gold beam
    core = np.exp(-(x * 2.2) ** 2) * (0.75 + 0.25 * noise(n, 62, (4, 12, 30)))
    save("beam", None, core)


# ---------------------------------------------------------------------------------------------------------- plate sigil
def gen_plate(n=256):
    rnd = random.Random(71)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    S = n * SS / 256.0
    c = 128 * S

    def circ(r, w):
        d.ellipse([c - r * S, c - r * S, c + r * S, c + r * S], outline=255, width=int(w * S))
    circ(124, 5)
    circ(114, 2)
    circ(88, 3)
    circ(80, 1.5)
    circ(46, 2.5)
    # scalloped rim: 16 semicircle notches between the outer rings
    for k in range(16):
        a = k / 16 * math.tau
        px, py = c + math.cos(a) * 119 * S, c + math.sin(a) * 119 * S
        d.ellipse([px - 6 * S, py - 6 * S, px + 6 * S, py + 6 * S], outline=255, width=int(2 * S))
    # tick marks between the middle rings, as on a clock of cutlery
    for k in range(48):
        a = k / 48 * math.tau
        r0, r1 = 91, 100 if k % 4 else 108
        d.line([(c + math.cos(a) * r0 * S, c + math.sin(a) * r0 * S), (c + math.cos(a) * r1 * S, c + math.sin(a) * r1 * S)], fill=255, width=int(1.6 * S))
    # inner: eight fork-tine marks pointing at the centre
    for k in range(8):
        a = k / 8 * math.tau + 0.2
        for o in (-0.08, 0, 0.08):
            aa = a + o
            d.line([(c + math.cos(aa) * 52 * S, c + math.sin(aa) * 52 * S), (c + math.cos(aa) * 76 * S, c + math.sin(aa) * 76 * S)], fill=255, width=int(2 * S))
    a = down(m.filter(ImageFilter.GaussianBlur(0.4 * SS)), n)
    wear = noise(n, 72, (8, 24, 64))
    a *= 0.75 + 0.25 * soft(wear, 0.2, 0.7)
    save("plate", None, a)
    # a soft disc to lay under it (light)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    disc = soft(1 - r, 0, 0.06) * (0.22 + 0.2 * soft(np.abs(r - 0.66), 0.0, 0.2)) * (0.8 + 0.2 * noise(n, 73))
    save("disc", None, disc)


# -------------------------------------------------------------------------------------------------------------- crumbs
def crumb(name, kind, seed):
    n = 64
    rnd = random.Random(seed)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    s = n * SS / 64.0
    c = 32 * s
    if kind == 0:  # bread chunk
        pts = [(c + math.cos(a) * (22 + rnd.uniform(-4, 4)) * s * (1.0 if int(a * 10) % 2 else 0.88),
                c + math.sin(a) * (17 + rnd.uniform(-3, 3)) * s) for a in [i / 11 * math.tau for i in range(11)]]
        d.polygon(pts, fill=255)
    elif kind == 1:  # drumstick: meat bulb and a bone with a knob
        d.ellipse([6 * s, 12 * s, 40 * s, 46 * s], fill=255)
        d.polygon([(34 * s, 24 * s), (52 * s, 28 * s), (52 * s, 36 * s), (34 * s, 38 * s)], fill=255)
        d.ellipse([48 * s, 24 * s, 60 * s, 34 * s], fill=255)
        d.ellipse([48 * s, 30 * s, 60 * s, 40 * s], fill=255)
    else:  # cheese wedge
        d.polygon([(6 * s, 46 * s), (58 * s, 22 * s), (58 * s, 44 * s), (6 * s, 46 * s)], fill=255)
        d.polygon([(6 * s, 46 * s), (58 * s, 22 * s), (50 * s, 14 * s), (10 * s, 36 * s)], fill=255)
    outer = m
    inner = erode(m, int(1.6 * SS))
    o, i = down(outer, n), down(inner, n)
    x, y = grid(n)
    nz = noise(n, seed + 5, (4, 10, 24))
    lum = 0.8 + 0.2 * nz - 0.25 * soft(x * 0.5 + y * 0.5, 0.0, 0.8)
    pores = soft(noise(n, seed + 9, (10, 20)), 0.62, 0.72)
    lum = lum * (1 - 0.5 * pores)
    if kind == 1:
        lum = np.where(x > 0.45, 0.95, lum * 0.85)
    edge = o - i
    lum = np.where(edge > 0.5, 0.05, lum)
    save(name, np.dstack([lum, lum * 0.97, lum * 0.92]), o)


def gen_crumbs():
    crumb("crumb0", 0, 81)
    crumb("crumb1", 1, 82)
    crumb("crumb2", 2, 83)


def gen_swirl(n=256):
    """Swallowing vortex: curved hatch arms spiralling inward (ground plane), ink."""
    rnd = random.Random(91)
    m = mask_img(n)
    d = ImageDraw.Draw(m)
    S = n * SS / 256.0
    c = 128 * S
    for arm in range(7):
        base = arm / 7 * math.tau
        for lane in range(3):
            pts = []
            for k in range(40):
                t = k / 39
                r = (118 - 92 * t) * S
                a = base + t * 2.3 + lane * 0.05
                pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
            d.line(pts, fill=255, width=int((4.5 - lane * 1.3) * S))
    a = down(m.filter(ImageFilter.GaussianBlur(0.5 * SS)), n)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    a *= soft(1 - r, 0, 0.12) * soft(r, 0.2, 0.34) * (0.6 + 0.4 * noise(n, 92))
    save("swirl", None, a)


def main():
    os.makedirs(OUT, exist_ok=True)
    gen_cutlery()
    gen_maw()
    gen_fur_speed()
    gen_burst()
    gen_hatch()
    gen_plate()
    gen_crumbs()
    gen_swirl()


if __name__ == "__main__":
    main()

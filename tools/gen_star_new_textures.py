#!/usr/bin/env python3
"""
STAR MAGIC remake (phase 1), PREVIEW ONLY: the textures of direction A ("anime cel") and direction B ("volumetric glow") plus the small shared sprites.
Nothing here is registered in the game and nothing goes to assets/.  Direction C (real 3D toon meshes) is geometry built in tools/vfx_preview/star_new_preview.py; it only
reuses the small glow / flare sprites below.

    python tools/gen_star_new_textures.py            # Pillow + numpy only, deterministic (fixed seeds)

Output (preview folder only): docs/previews/vfx_remake/star/tex/<name>.png and tex_sheet.png (contact sheet).

Look (no canon image was available to this session, see docs/vfx_remake/star_magic_design.md section 0): the series draws Star Magic as small bright four-point stars joined by thin
straight lines of light; the mod's colours are a design choice: white-hot core, gold rim, starlight blue for lines and barrier faces.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "star", "tex")
SS = 4

WHITE = (255, 252, 240)
PALE = (255, 240, 170)
GOLD = (255, 205, 80)
AMBER = (240, 150, 40)
BLUE = (150, 205, 255)
BLUE_D = (80, 140, 230)
INDIGO = (50, 70, 170)

_saved = {}


def save(name, im):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    _saved[name] = im


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def done(im, w, h):
    return im.resize((w, h), Image.LANCZOS)


def star_poly(cx, cy, r_out, r_in, n=4, rot=-math.pi / 2, squash=1.0):
    pts = []
    for i in range(n * 2):
        a = rot + i * math.pi / n
        r = r_out if i % 2 == 0 else r_in
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r * squash))
    return pts


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def to_img(rgb, a):
    out = np.zeros(a.shape + (4,), np.float32)
    out[..., :3] = rgb
    out[..., 3] = np.clip(a, 0, 1)
    return Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def tint(rgb, a):
    r = np.array(rgb, np.float32) / 255.0
    return np.broadcast_to(r, a.shape + (3,))


def rng(seed):
    return np.random.default_rng(seed)


def noise(w, h, seed, scale=8, octaves=4):
    r = rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = max(2, scale * 2 ** o)
        small = r.random((n, n)).astype(np.float32)
        im = Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, np.float32) / 255.0 * amp
        tot += amp
        amp *= 0.5
    return out / tot


# ====================================================================================================== A  anime cel
def a_star4():
    """a four-point cel star: gold body, pale-yellow inner star, white core, tiny diagonal accents; flat tones, no outline"""
    w = h = 128
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.polygon(star_poly(c, c, c * 0.98, c * 0.18), fill=GOLD + (255,))
    d.polygon(star_poly(c, c, c * 0.72, c * 0.15), fill=PALE + (255,))
    d.polygon(star_poly(c, c, c * 0.40, c * 0.11), fill=WHITE + (255,))
    d.polygon(star_poly(c, c, c * 0.34, c * 0.16, rot=-math.pi / 4), fill=PALE + (255,))
    d.ellipse([c - c * 0.17, c - c * 0.17, c + c * 0.17, c + c * 0.17], fill=WHITE + (255,))
    save("A_star4", done(im, w, h))


def a_star4_small():
    w = h = 64
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.polygon(star_poly(c, c, c * 0.98, c * 0.14), fill=PALE + (255,))
    d.polygon(star_poly(c, c, c * 0.55, c * 0.11), fill=WHITE + (255,))
    save("A_spark", done(im, w, h))


def a_burst():
    """flash: long 4-point star, short diagonal star and a hard disc; the flat 'impact star' of cel animation"""
    w = h = 160
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.polygon(star_poly(c, c, c * 0.99, c * 0.10, n=4), fill=GOLD + (255,))
    d.polygon(star_poly(c, c, c * 0.66, c * 0.12, n=4, rot=-math.pi / 4), fill=GOLD + (255,))
    d.polygon(star_poly(c, c, c * 0.80, c * 0.08, n=4), fill=PALE + (255,))
    d.polygon(star_poly(c, c, c * 0.50, c * 0.08, n=4, rot=-math.pi / 4), fill=PALE + (255,))
    d.ellipse([c * 0.62, c * 0.62, c * 1.38, c * 1.38], fill=PALE + (255,))
    d.ellipse([c * 0.74, c * 0.74, c * 1.26, c * 1.26], fill=WHITE + (255,))
    save("A_burst", done(im, w, h))


def a_ring():
    w = h = 192
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    for r, wd, col in ((0.96, 0.07, GOLD), (0.84, 0.03, WHITE), (0.70, 0.015, PALE)):
        d.ellipse([c - c * r, c - c * r, c + c * r, c + c * r], outline=col + (255,), width=max(1, int(c * wd)))
    save("A_ring", done(im, w, h))


def a_sigil():
    """ground sigil: gold ring, thin white ring, four small stars on the cross points, dashed inner ring"""
    w = h = 256
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.ellipse([c * 0.04, c * 0.04, c * 1.96, c * 1.96], outline=GOLD + (255,), width=int(c * 0.035))
    d.ellipse([c * 0.12, c * 0.12, c * 1.88, c * 1.88], outline=WHITE + (255,), width=int(c * 0.012))
    d.ellipse([c * 0.52, c * 0.52, c * 1.48, c * 1.48], outline=PALE + (255,), width=int(c * 0.012))
    for i in range(16):
        a0 = i * 2 * math.pi / 16
        a1 = a0 + 0.22
        pts = [(c + math.cos(a) * c * 0.40, c + math.sin(a) * c * 0.40) for a in np.linspace(a0, a1, 6)]
        d.line(pts, fill=GOLD + (255,), width=int(c * 0.02))
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        sx, sy = c + math.cos(a) * c * 0.96, c + math.sin(a) * c * 0.96
        d.polygon(star_poly(sx, sy, c * 0.15, c * 0.035), fill=PALE + (255,))
        d.polygon(star_poly(sx, sy, c * 0.07, c * 0.025), fill=WHITE + (255,))
    save("A_sigil", done(im, w, h))


def a_line():
    """a straight cel line of light: white core, pale-yellow band, gold rim, flat, ends square; texture u runs along the line"""
    w, h = 256, 32
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    W, H = w * SS, h * SS
    d.rectangle([0, H * 0.18, W, H * 0.82], fill=BLUE + (255,))
    d.rectangle([0, H * 0.30, W, H * 0.70], fill=WHITE + (255,))
    d.rectangle([0, H * 0.42, W, H * 0.58], fill=WHITE + (255,))
    save("A_line", done(im, w, h))


def a_line_gold():
    w, h = 256, 32
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    W, H = w * SS, h * SS
    d.rectangle([0, H * 0.16, W, H * 0.84], fill=GOLD + (255,))
    d.rectangle([0, H * 0.30, W, H * 0.70], fill=PALE + (255,))
    d.rectangle([0, H * 0.42, W, H * 0.58], fill=WHITE + (255,))
    save("A_line_gold", done(im, w, h))


def a_beam():
    """spear / laser: pointed head on the right, hard bands (blue rim, pale, white core), a few cel speed streaks"""
    w, h = 512, 64
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    W, H = w * SS, h * SS
    m = H / 2

    def band(f, col, head=0.07):
        d.polygon([(0, m - m * f), (W * (1 - head), m - m * f), (W, m), (W * (1 - head), m + m * f), (0, m + m * f)], fill=col + (255,))

    band(0.98, BLUE, 0.06)
    band(0.74, PALE, 0.075)
    band(0.46, WHITE, 0.09)
    r = rng(5)
    for _ in range(9):
        x0 = r.random() * W * 0.8
        ln = W * (0.08 + r.random() * 0.12)
        y = m + (r.random() - 0.5) * m * 1.5
        d.line([(x0, y), (x0 + ln, y)], fill=WHITE + (255,), width=int(H * 0.025))
    save("A_beam", done(im, w, h))


def a_whip():
    """tapered ribbon: thick on the left (u=0, the hand) to a needle on the right"""
    w, h = 512, 48
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    W, H = w * SS, h * SS
    m = H / 2

    def band(f, col):
        pts = []
        for i in range(0, 41):
            x = W * i / 40
            t = i / 40
            pts.append((x, m - m * f * (1 - t * 0.93)))
        for i in range(40, -1, -1):
            x = W * i / 40
            t = i / 40
            pts.append((x, m + m * f * (1 - t * 0.93)))
        d.polygon(pts, fill=col + (255,))

    band(0.95, BLUE)
    band(0.62, WHITE)
    band(0.30, WHITE)
    save("A_whip", done(im, w, h))


def a_pane():
    """barrier face: pale-blue translucent fill, bright flat border, two diagonal glare bands, small corner notches; drawn on a square-ish card"""
    w, h = 256, 192
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    W, H = w * SS, h * SS
    d.rectangle([W * 0.015, H * 0.02, W * 0.985, H * 0.98], fill=BLUE_D + (80,))
    d.rectangle([W * 0.015, H * 0.02, W * 0.985, H * 0.98], outline=WHITE + (255,), width=int(W * 0.012))
    d.rectangle([W * 0.045, H * 0.06, W * 0.955, H * 0.94], outline=BLUE + (200,), width=int(W * 0.006))
    for x0, wd in ((0.18, 0.10), (0.36, 0.035)):
        d.polygon([(W * x0, H * 0.06), (W * (x0 + wd), H * 0.06), (W * (x0 + wd - 0.18), H * 0.94), (W * (x0 - 0.18), H * 0.94)], fill=WHITE + (110,))
    im2 = done(im, w, h)
    save("A_pane", im2)


def a_crack():
    """whip crack / hit star: 12-point spiky flat star"""
    w = h = 128
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.polygon(star_poly(c, c, c * 0.99, c * 0.34, n=8), fill=GOLD + (255,))
    d.polygon(star_poly(c, c, c * 0.74, c * 0.30, n=8, rot=-math.pi / 2 + math.pi / 8), fill=PALE + (255,))
    d.polygon(star_poly(c, c, c * 0.48, c * 0.22, n=8), fill=WHITE + (255,))
    save("A_crack", done(im, w, h))


def a_rays():
    """radial speed lines: thin hard wedges around the centre, leaving the middle empty"""
    w = h = 256
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    r = rng(11)
    for i in range(28):
        a = i * 2 * math.pi / 28 + r.random() * 0.05
        r0 = c * (0.22 + r.random() * 0.12)
        r1 = c * (0.70 + r.random() * 0.28)
        wd = 0.012 + r.random() * 0.03
        d.polygon([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a + wd) * r1, c + math.sin(a + wd) * r1), (c + math.cos(a - wd) * r1, c + math.sin(a - wd) * r1)],
                  fill=(WHITE if i % 2 else PALE) + (255,))
    save("A_rays", done(im, w, h))


def a_shard():
    w = h = 64
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.polygon([(c, c * 0.05), (c * 1.55, c), (c, c * 1.95), (c * 0.45, c)], fill=BLUE + (255,))
    d.polygon([(c, c * 0.30), (c * 1.30, c), (c, c * 1.55), (c * 0.72, c)], fill=WHITE + (255,))
    save("A_shard", done(im, w, h))


def a_disc():
    """flat bright disc with a hard rim: node core / impact"""
    w = h = 96
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = w * SS / 2
    d.ellipse([c * 0.04, c * 0.04, c * 1.96, c * 1.96], fill=GOLD + (255,))
    d.ellipse([c * 0.22, c * 0.22, c * 1.78, c * 1.78], fill=PALE + (255,))
    d.ellipse([c * 0.50, c * 0.50, c * 1.50, c * 1.50], fill=WHITE + (255,))
    save("A_disc", done(im, w, h))


# ====================================================================================================== B  volumetric glow
def b_glow():
    w = h = 128
    x, y = grid(w, h)
    r = np.hypot(x - 0.5, y - 0.5) * 2
    a = np.exp(-(r * 2.6) ** 2) * 0.85 + np.exp(-(r * 1.1) ** 2) * 0.35
    a *= np.clip(1 - r ** 3, 0, 1)
    save("B_glow", to_img(tint(WHITE, a), a))


def b_glow_blue():
    w = h = 128
    x, y = grid(w, h)
    r = np.hypot(x - 0.5, y - 0.5) * 2
    a = np.exp(-(r * 1.7) ** 2) * 0.8 * np.clip(1 - r ** 3, 0, 1)
    col = np.array(BLUE, np.float32) / 255.0
    save("B_glow_blue", to_img(np.broadcast_to(col, a.shape + (3,)), a))


def b_flare():
    """anamorphic lens flare: long thin horizontal streak, short vertical one, a hot core, a faint ring"""
    w, h = 256, 256
    x, y = grid(w, h)
    dx, dy = (x - 0.5) * 2, (y - 0.5) * 2
    r = np.hypot(dx, dy)
    streak_h = np.exp(-(dy * 22) ** 2) * np.exp(-(np.abs(dx) * 1.6) ** 2)
    streak_v = np.exp(-(dx * 28) ** 2) * np.exp(-(np.abs(dy) * 2.6) ** 2) * 0.7
    core = np.exp(-(r * 5.5) ** 2)
    halo = np.exp(-(r * 2.1) ** 2) * 0.4
    ring = np.exp(-((r - 0.72) * 18) ** 2) * 0.18
    a = np.clip(streak_h + streak_v + core + halo + ring, 0, 1) * np.clip(1 - r ** 4, 0, 1)
    rgb = np.zeros(a.shape + (3,), np.float32)
    k = np.clip(core * 1.2 + streak_h * 0.3, 0, 1)[..., None]
    rgb[:] = np.array(BLUE, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_flare", to_img(rgb, a))


def b_star():
    """soft 4-point bloom star: thin bright arms that fade, round hot core, faint diagonal arms"""
    w = h = 192
    x, y = grid(w, h)
    dx, dy = (x - 0.5) * 2, (y - 0.5) * 2
    r = np.hypot(dx, dy)
    arm = lambda u, v: np.exp(-(u * 26) ** 2) * np.exp(-(np.abs(v) * 2.3) ** 2)
    a = arm(dx, dy) + arm(dy, dx)
    d2x, d2y = (dx + dy) * 0.7071, (dy - dx) * 0.7071
    a += 0.35 * (arm(d2x, d2y) + arm(d2y, d2x))
    a += np.exp(-(r * 5) ** 2) * 1.2 + np.exp(-(r * 2.2) ** 2) * 0.35
    a = np.clip(a, 0, 1) * np.clip(1 - r ** 4, 0, 1)
    k = np.clip(np.exp(-(r * 4.5) ** 2) * 1.4, 0, 1)[..., None]
    rgb = np.array(PALE, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_star", to_img(rgb, a))


def b_dust(i):
    w = h = 128
    x, y = grid(w, h)
    r = np.hypot(x - 0.5, y - 0.5) * 2
    n = noise(w, h, 40 + i, scale=4, octaves=4)
    a = np.clip((n * 1.5 - 0.45), 0, 1) * np.clip(1 - r ** 1.6, 0, 1) ** 1.4
    t = n[..., None]
    rgb = np.array(BLUE, np.float32) / 255.0 * (1 - t) + np.array(PALE, np.float32) / 255.0 * t
    save("B_dust_" + "abc"[i], to_img(rgb, a * 0.8))


def b_line():
    """soft filament: gaussian cross-section, hot centre, faint random brightness along the length; u runs along it"""
    w, h = 256, 32
    x, y = grid(w, h)
    dy = (y - 0.5) * 2
    n = noise(w, h, 71, scale=6, octaves=3)
    prof = np.exp(-(dy * 3.2) ** 2) * 0.55 + np.exp(-(dy * 9) ** 2) * 0.9
    a = np.clip(prof * (0.82 + 0.3 * n), 0, 1) * np.clip(np.minimum(x, 1 - x) * 14, 0, 1)
    k = np.clip(np.exp(-(dy * 8) ** 2) * 1.3, 0, 1)[..., None]
    rgb = np.array(BLUE, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_line", to_img(rgb, a))


def b_beam():
    """soft laser: wide faint halo, mid blue, very hot thin core, brighter towards the head (u=1)"""
    w, h = 512, 64
    x, y = grid(w, h)
    dy = (y - 0.5) * 2
    n = noise(w, h, 72, scale=5, octaves=3)
    halo = np.exp(-(dy * 2.2) ** 2) * 0.55
    core = np.exp(-(dy * 7.5) ** 2) * 1.0
    a = np.clip((halo + core) * (0.78 + 0.28 * n) * (0.55 + 0.45 * x), 0, 1) * np.clip(np.minimum(x * 20, (1 - x) * 40), 0, 1)
    k = np.clip(np.exp(-(dy * 7) ** 2) * 1.3, 0, 1)[..., None]
    rgb = np.array(BLUE_D, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_beam", to_img(rgb, a))


def b_trail():
    """comet tail: bright head on the right, long tapering fade to the left"""
    w, h = 256, 64
    x, y = grid(w, h)
    dy = (y - 0.5) * 2
    width = 0.15 + 0.85 * x ** 1.2
    a = np.exp(-(dy / np.maximum(width, 0.05) * 1.8) ** 2) * x ** 1.5
    n = noise(w, h, 73, scale=6, octaves=3)
    a = np.clip(a * (0.8 + 0.4 * n), 0, 1)
    k = np.clip(x ** 4 * 1.4, 0, 1)[..., None]
    rgb = np.array(BLUE, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_trail", to_img(rgb, a))


def b_pane():
    """volumetric barrier face: transparent in the middle, bright fresnel edge glow, faint aurora bands and fine grid, brighter corners"""
    w, h = 256, 192
    x, y = grid(w, h)
    ex = np.minimum(x, 1 - x)
    ey = np.minimum(y, 1 - y)
    e = np.minimum(ex * (w / h) * 1.0, ey)
    edge = np.exp(-(e * 22) ** 2) * 0.95 + np.exp(-(e * 6) ** 2) * 0.28
    n = noise(w, h, 74, scale=3, octaves=4)
    band = np.clip(np.sin((x * 6.0 + y * 9.0 + n * 3.0) * math.pi) * 0.5 + 0.5, 0, 1) ** 3 * 0.10
    gx = np.abs(np.sin(x * 20 * math.pi)) ** 40 * 0.05 + np.abs(np.sin(y * 15 * math.pi)) ** 40 * 0.05
    fill = 0.09 + 0.07 * n
    a = np.clip(edge + band + gx + fill, 0, 1) * (ex > 0.003) * (ey > 0.003)
    k = np.clip(edge * 1.1, 0, 1)[..., None]
    rgb = np.array(BLUE_D, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_pane", to_img(rgb, a))


def b_ring():
    w = h = 192
    x, y = grid(w, h)
    r = np.hypot(x - 0.5, y - 0.5) * 2
    a = np.exp(-((r - 0.82) * 14) ** 2) * 0.9 + np.exp(-((r - 0.82) * 4.5) ** 2) * 0.28
    a *= np.clip((1 - r) * 8, 0, 1)
    k = np.clip(np.exp(-((r - 0.82) * 20) ** 2) * 1.2, 0, 1)[..., None]
    rgb = np.array(BLUE, np.float32) / 255.0 * (1 - k) + np.array(WHITE, np.float32) / 255.0 * k
    save("B_ring", to_img(rgb, a))


def b_rays():
    w = h = 256
    x, y = grid(w, h)
    dx, dy = (x - 0.5) * 2, (y - 0.5) * 2
    r = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    n = noise(w, h, 75, scale=2, octaves=2)
    rr = rng(8).random(24)
    ray = np.zeros_like(r)
    for i in range(24):
        a0 = i * 2 * math.pi / 24
        dd = np.abs(((ang - a0 + math.pi) % (2 * math.pi)) - math.pi)
        ray += np.exp(-(dd * 14) ** 2) * (0.4 + 0.6 * rr[i])
    a = np.clip(ray, 0, 1) * np.exp(-(r * 1.8) ** 2) * np.clip(r * 6, 0, 1) * (0.7 + 0.5 * n)
    save("B_rays", to_img(tint(PALE, a), np.clip(a, 0, 1) * 0.9))


def b_sigil():
    w = h = 256
    x, y = grid(w, h)
    dx, dy = (x - 0.5) * 2, (y - 0.5) * 2
    r = np.hypot(dx, dy)
    a = np.exp(-((r - 0.9) * 26) ** 2) * 0.9 + np.exp(-((r - 0.5) * 30) ** 2) * 0.35 + np.exp(-(r * 4) ** 2) * 0.35
    ang = np.arctan2(dy, dx)
    for i in range(4):
        a0 = i * math.pi / 2 + math.pi / 4
        dd = np.abs(((ang - a0 + math.pi) % (2 * math.pi)) - math.pi)
        a += np.exp(-(dd * 22) ** 2) * np.exp(-((r - 0.7) * 5) ** 2) * 0.6
    a *= np.clip((1 - r) * 12, 0, 1)
    save("B_sigil", to_img(tint(PALE, a), np.clip(a, 0, 1)))


def contact_sheet():
    names = sorted(_saved)
    cols = 6
    cell = 150
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), (14, 18, 40))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        im = _saved[n]
        k = min((cell - 20) / im.width, (cell - 20) / im.height)
        sm = im.resize((max(1, int(im.width * k)), max(1, int(im.height * k))), Image.LANCZOS)
        x, y = (i % cols) * cell, (i // cols) * cell
        sheet.paste(sm, (x + 6, y + 14), sm)
        d.text((x + 4, y + 2), n, fill=(255, 235, 150))
    sheet.save(os.path.join(OUT, "tex_sheet.png"))


def main():
    a_star4(); a_star4_small(); a_burst(); a_ring(); a_sigil(); a_line(); a_line_gold(); a_beam(); a_whip(); a_pane(); a_crack(); a_rays(); a_shard(); a_disc()
    b_glow(); b_glow_blue(); b_flare(); b_star()
    for i in range(3):
        b_dust(i)
    b_line(); b_beam(); b_trail(); b_pane(); b_ring(); b_rays(); b_sigil()
    contact_sheet()
    print(len(_saved), "textures ->", OUT)


if __name__ == "__main__":
    main()

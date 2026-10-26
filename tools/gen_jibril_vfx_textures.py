"""Generates the Veldanava "Jibril, Lord of Rigor" VFX textures (vfx.client.layer.JibrilLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/jibril_*.png  (the same names as the 3.15 set are redrawn; the new ones are added)

    python tools/gen_jibril_vfx_textures.py            # all
    python tools/gen_jibril_vfx_textures.py wing glyph # only these

Identity (3.16): DAWN / LAW OF THE SKY. A pearl-white core, rose-gold and amber warmth, an azure-to-lavender cool shadow and an IRIDESCENT prismatic rim
(a soft pink -> gold -> aqua -> violet gradient on the edges, halos and feathers, like light through an angel's wing). Motifs: layered feather wings, nested halos
wreathed in tiny feathers, flowing ribbons / banners of light, unrolling law scrolls with calligraphic script lines, an open book under a crown of rays, braided
ribbons of light with feather-shaped links, a spiral braid in the pillar and falling petals. It deliberately shares none of the look of Sandalphon's judgment set
(cold steel, black cracks, clock rings, hexagrams, a scales-and-sword emblem) nor of Israfil's cyan maze.

Nearly every texture carries its colour (baked) and is drawn with a (near) white vertex colour; the vertex colour is then only a tint / alpha. Nothing carries colour
under a fully transparent pixel (rgb is zeroed where alpha is 0). Sizes are powers of two, at most 1024 px, aspect at most 8:1.

    jibril_ring       thin prismatic circle with beads and tiny petal marks (shock rings, parry rings, rims)
    jibril_glyph      the law disc (1024): script band, a rosette of feather petals, a crown of rays over an open book
    jibril_seal       the floor seal (1024): a rose window of feather petals, a script band, concentric rings
    jibril_halo       nested halos wreathed in tiny feathers (512)
    jibril_wave       a rippling braid front with a spiral wake of petals (512)
    jibril_rays       a feathered sunburst in prismatic colours (512)
    jibril_pillar     a pillar with a three-strand spiral braid, falling petals and prismatic haze (128 x 512, seamless in V)
    jibril_bars       a hanging law banner with columns of script (128 x 512)
    jibril_curtain    a dawn curtain: rose at the foot, pearl, aqua and violet above, with folds (128 x 512)
    jibril_beam       a braided beam with feather chevrons (128 x 512, seamless in V)
    jibril_chain      a braided ribbon of light with feather-shaped links (128 x 512, seamless in V)
    jibril_chainring  a ring of feather links with a braid through it (512)
    jibril_feather    one feather with an iridescent rim, tip up (128)
    jibril_wing       one wing of layered feathers with a prismatic rim (1024 x 512)
    jibril_flare      a crown-of-rays flare: a soft core, twelve tapered rays, a prismatic lens ring (256)
    jibril_mote       a small six-ray sparkle (64)
    jibril_scroll     an unrolling law scroll with calligraphic script (1024 x 512)
    jibril_emblem     the emblem: an open book under a crown of rays (512)
    jibril_petal      a falling petal (64)
    jibril_ribbon     a flowing ribbon of light with a prismatic gradient across it (128 x 512, seamless in V)
    jibril_spiral     six ribbons spiralling out from the middle (512)
    jibril_prism      a soft prismatic bloom: a pearl core and a rainbow ring (256)
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")

PEARL = (255, 250, 244)
PEACH = (255, 214, 192)
ROSEGOLD = (240, 166, 138)
AMBER = (255, 184, 92)
AZURE = (132, 176, 255)
LAV = (182, 160, 244)
SHADOW = (150, 170, 238)
WHITE = (255, 255, 255)
# the prismatic cycle: pink -> gold -> aqua -> violet -> (pink)
IRIS = np.array([(255, 150, 205), (255, 214, 130), (138, 238, 232), (178, 150, 244)], np.float32) / 255.0


# ---------------------------------------------------------------------------------------------------- io
def save(rgb, alpha, name):
    """Writes an RGBA png from float arrays (rgb 0..1, alpha 0..1). rgb is zeroed under a fully transparent pixel."""
    a = np.clip(alpha, 0, 1)
    rgb = np.clip(rgb, 0, 1)
    px = np.dstack([rgb[..., 0], rgb[..., 1], rgb[..., 2], a])
    px = (px * 255 + 0.5).astype(np.uint8)
    px[px[..., 3] == 0, :3] = 0
    im = Image.fromarray(px, "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


# ---------------------------------------------------------------------------------------------------- maths
def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def disc_coords(size):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


def blur(a, radius):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0


def col(c):
    return np.array(c, np.float32) / 255.0


def lerp3(c0, c1, t):
    t = np.clip(t, 0, 1)[..., None]
    return col(c0) * (1 - t) + col(c1) * t


def iris(t, pastel=0.22):
    """The prismatic cycle at phase t (any float array, period 1), pulled a little towards white so it stays a light colour."""
    t = np.mod(t, 1.0) * 4.0
    i0 = np.floor(t).astype(np.int32) % 4
    f = (t - np.floor(t)).astype(np.float32)
    f = f * f * (3 - 2 * f)
    c = IRIS[i0] * (1 - f[..., None]) + IRIS[(i0 + 1) % 4] * f[..., None]
    return c * (1 - pastel) + pastel


def mask_of(im):
    return np.asarray(im.convert("L"), dtype=np.float32) / 255.0


def new_mask(W, H):
    im = Image.new("L", (W, H), 0)
    return im, ImageDraw.Draw(im)


def finish(name, sharp, phase, glow=((1.6, 0.85), (5.0, 0.42)), core=PEARL, tint=0.22):
    """Line art -> a baked-colour texture: the sharp lines are pearl (a little prismatic), the glow around them takes the prismatic colour of `phase`."""
    s = np.clip(sharp, 0, 1)
    g = np.zeros_like(s)
    for r, k in glow:
        g = g + blur(s, r) * k
    g = np.clip(g, 0, 1) * (1 - s * 0.7)
    a = np.clip(s + g, 0, 1)
    ir = iris(phase)
    corec = col(core) * (1 - tint) + ir * tint
    rgb = (corec * s[..., None] + ir * g[..., None]) / np.maximum(s + g, 1e-4)[..., None]
    save(rgb, a, name)
    return rgb, a


def downsample(im, w, h):
    return mask_of(im.resize((w, h), Image.LANCZOS))


# ---------------------------------------------------------------------------------------------------- calligraphy
def word_points(rng, s0, length, height, loops=None):
    """One cursive word as a polyline in (s along the line, h across it): a trochoid with a breathing amplitude, so it reads as running handwriting."""
    n = max(40, int(length / height * 30))
    c = loops if loops else max(2, int(round(length / (height * 0.50 * rng.uniform(0.85, 1.25)))))   # enough turns that the trochoid really loops (cursive)
    ph = rng.uniform(0, 6.28)
    ph2 = rng.uniform(0, 6.28)
    a = height * 0.34
    pts = []
    for i in range(n + 1):
        t = i / n
        th = 2 * math.pi * c * t + ph
        amp = a * (0.55 + 0.45 * math.sin(3.1 * t * math.pi + ph2))
        s = s0 + length * t - 0.55 * amp * math.sin(th)
        h = amp * math.cos(th) * 0.9
        if t > 0.78 and t < 0.86:
            h -= height * 0.30 * math.sin((t - 0.78) / 0.08 * math.pi)       # an ascender stroke
        pts.append((s, h))
    return pts


def script_line(rng, length, height, gap=0.03, s_start=0.0):
    """A run of words: [(points, ...)] along s in [s_start, s_start + length]."""
    words = []
    s = s_start
    end = s_start + length
    while s < end - length * 0.05:
        lw = min(rng.uniform(length * 0.07, length * 0.22), end - s)
        words.append(word_points(rng, s, lw, height))
        s += lw + length * gap * rng.uniform(0.7, 1.6)
    return words


def draw_words(d, words, place, width, fill=255):
    """Draws script words through a placement function (s, h) -> (x, y)."""
    for pts in words:
        xy = [place(s, h) for s, h in pts]
        for i in range(len(xy) - 1):
            slope = abs(pts[i + 1][1] - pts[i][1]) / (abs(pts[i + 1][0] - pts[i][0]) + 1e-3)
            wv = width * (0.8 + 0.45 * min(1.0, slope))                  # a downstroke is a little heavier than a hairline
            d.line([xy[i], xy[i + 1]], fill=fill, width=max(1, int(round(wv))))


# ---------------------------------------------------------------------------------------------------- shared drawing: the emblem (an open book under a crown of rays)
def draw_emblem(d, cx, cy, R, ss, w=2.0, fill=255):
    """Crown of rays over an open book, centred on (cx, cy), emblem radius R (pixels of the supersampled canvas)."""
    def P(x, y):
        return (cx + x * R, cy + y * R)

    lw = max(1, int(w * ss))
    # the book: two pages with a curved top edge, a spine, a second page edge and three script lines on each page
    for sx in (-1, 1):
        top = [P(sx * 0.02, 0.30), P(sx * 0.20, 0.17), P(sx * 0.46, 0.12), P(sx * 0.72, 0.20)]
        d.line(top + [P(sx * 0.74, 0.55), P(sx * 0.46, 0.50), P(sx * 0.20, 0.54), P(sx * 0.02, 0.66)], fill=fill, width=lw, joint="curve")
        d.line([P(sx * 0.02, 0.30), P(sx * 0.02, 0.66)], fill=fill, width=lw)
        d.line([P(sx * 0.05, 0.69), P(sx * 0.26, 0.58), P(sx * 0.48, 0.56), P(sx * 0.78, 0.61)], fill=int(fill * 0.7), width=max(1, lw - 1), joint="curve")
        for k in range(4):
            y0 = 0.25 + 0.075 * k
            d.line([P(sx * 0.11, y0 + 0.07), P(sx * 0.31, y0 + 0.02), P(sx * 0.56, y0 + 0.01 + 0.02 * (k % 2))], fill=int(fill * 0.85), width=max(1, lw - 1), joint="curve")
    # the sun and its crown of rays: a half disc over the spine, 17 tapered rays over the top half, a dot on the end of every long one
    d.arc([cx - 0.20 * R, cy - 0.30 * R + 0.0 * R, cx + 0.20 * R, cy + 0.10 * R], 180, 360, fill=fill, width=lw)
    d.line([P(-0.20, -0.10), P(0.20, -0.10)], fill=fill, width=lw)
    n = 17
    for k in range(n):
        a = math.pi + math.pi * (k + 0.5) / n
        long_ = k % 2 == 0
        r0 = 0.26
        r1 = 0.56 if long_ else 0.42
        if k == n // 2:
            r1 = 0.70
        ox, oy = 0.0, -0.10
        d.line([(cx + (ox + math.cos(a) * r0) * R, cy + (oy + math.sin(a) * r0) * R), (cx + (ox + math.cos(a) * r1) * R, cy + (oy + math.sin(a) * r1) * R)], fill=fill, width=lw if long_ else max(1, lw - 1))
        if long_:
            bx, by = cx + (ox + math.cos(a) * (r1 + 0.045)) * R, cy + (oy + math.sin(a) * (r1 + 0.045)) * R
            e = 1.6 * ss
            d.ellipse([bx - e, by - e, bx + e, by + e], fill=fill)


# ---------------------------------------------------------------------------------------------------- ring
def ring(size=512, ss=3):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)
    for k in range(96):                                    # beads just inside the ring, a bigger one every 6th
        a = math.radians(k * 3.75)
        big = k % 6 == 0
        e = (2.6 if big else 1.2) * ss
        r = 0.925 if big else 0.935
        bx, by = c + math.cos(a) * R * r, c + math.sin(a) * R * r
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255 if big else 170)
    for k in range(12):                                    # petal marks outside the ring (lens shapes pointing outwards)
        a = math.radians(30 * k)
        r0, r1 = 0.972, 1.0
        L = (r1 - r0)
        left, right = [], []
        for t in range(9):
            u = t / 8
            rr = r0 + L * u
            wd = math.sin(u * math.pi) * 0.012
            left.append((c + math.cos(a) * R * rr - math.sin(a) * R * wd, c + math.sin(a) * R * rr + math.cos(a) * R * wd))
            right.append((c + math.cos(a) * R * rr + math.sin(a) * R * wd, c + math.sin(a) * R * rr - math.cos(a) * R * wd))
        d.polygon(left + right[::-1], fill=255)
    big = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    core = np.exp(-(((r - 0.955) / 0.0045) ** 2))
    ripple = 0.5 + 0.5 * np.cos(th * 24)
    inner = np.exp(-(((r - 0.905) / 0.0030) ** 2)) * 0.45
    sharp = np.clip(core * (0.88 + 0.12 * ripple) + inner + big, 0, 1) * smooth(1.0, 0.985, r)
    finish("jibril_ring", sharp, th / math.tau + r * 0.6, glow=((2.2, 1.1), (7.0, 0.5)))


# ---------------------------------------------------------------------------------------------------- law glyph disc
def glyph(size=1024, ss=2):
    rng = np.random.default_rng(7117)
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def circle(rad, w, fill=255):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss * 1.4)))

    circle(0.985, 3.0)
    circle(0.955, 1.3, 190)
    circle(0.775, 2.2)
    circle(0.748, 1.0, 150)
    circle(0.495, 2.0)
    circle(0.472, 1.0, 140)
    # two rows of calligraphy between the outer rings: the lines run round the disc
    for rb, hh, nl in ((0.885, 0.062, 1), (0.842, 0.040, 1)):
        Lc = 2 * math.pi * rb * R
        words = script_line(rng, Lc, hh * R * 2.0, 0.025)
        draw_words(d, words, lambda s, h, rb=rb: (c + (rb * R + h) * math.cos(s / (rb * R)), c + (rb * R + h) * math.sin(s / (rb * R))), 2.3 * ss)
    # a rosette of 24 feather petals (outlines with a spine) between the two inner rings
    n = 24
    for k in range(n):
        a = 2 * math.pi * k / n
        r0, r1 = 0.505, 0.745
        left, right = [], []
        for t in range(13):
            u = t / 12
            rr = r0 + (r1 - r0) * u
            wd = math.sin(math.pi * min(1.0, u ** 0.7)) ** 0.9 * 0.052 * (1 - 0.25 * u)
            bend = 0.012 * math.sin(u * math.pi)
            ax = a + bend
            left.append((c + math.cos(ax) * R * rr - math.sin(ax) * R * wd, c + math.sin(ax) * R * rr + math.cos(ax) * R * wd))
            right.append((c + math.cos(ax) * R * rr + math.sin(ax) * R * wd, c + math.sin(ax) * R * rr - math.cos(ax) * R * wd))
        d.line(left + right[::-1] + [left[0]], fill=255 if k % 2 == 0 else 190, width=int(2.1 * ss), joint="curve")
        d.line([(c + math.cos(a) * R * r0, c + math.sin(a) * R * r0), (c + math.cos(a) * R * (r1 - 0.02), c + math.sin(a) * R * (r1 - 0.02))], fill=170, width=int(1.1 * ss))
        for t in (4, 6, 8):                                # barbs
            u = t / 12
            rr = r0 + (r1 - r0) * u
            wd = math.sin(math.pi * min(1.0, u ** 0.7)) ** 0.9 * 0.052 * (1 - 0.25 * u)
            d.line([(c + math.cos(a) * R * rr, c + math.sin(a) * R * rr), left[min(12, t + 2)]], fill=110, width=max(1, int(0.8 * ss)))
            d.line([(c + math.cos(a) * R * rr, c + math.sin(a) * R * rr), right[min(12, t + 2)]], fill=110, width=max(1, int(0.8 * ss)))
    for k in range(48):                                    # a bead on the rim of the rosette
        a = 2 * math.pi * (k + 0.5) / 48
        bx, by = c + math.cos(a) * R * 0.762, c + math.sin(a) * R * 0.762
        e = (2.0 if k % 2 == 0 else 1.2) * ss
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255)
    # the emblem in the middle: a crown of rays over an open book
    draw_emblem(d, c, c + 0.02 * R, R * 0.44, ss, 1.8)
    big = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    sharp = big * smooth(1.0, 0.985, r)
    finish("jibril_glyph", sharp, th / math.tau * 1.0 + r * 0.8, glow=((2.2, 0.8), (7.0, 0.42)))


# ---------------------------------------------------------------------------------------------------- floor seal: a rose window
def seal(size=1024, ss=2):
    rng = np.random.default_rng(4040)
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def circle(rad, w, fill=255):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss * 1.4)))

    circle(0.985, 3.4)
    circle(0.958, 1.4, 190)
    circle(0.842, 2.0)
    circle(0.818, 1.0, 150)
    circle(0.30, 2.0)
    circle(0.275, 1.0, 140)
    words = script_line(rng, 2 * math.pi * 0.90 * R, 0.11 * R, 0.025)
    draw_words(d, words, lambda s, h: (c + (0.90 * R + h) * math.cos(s / (0.90 * R)), c + (0.90 * R + h) * math.sin(s / (0.90 * R))), 2.4 * ss)

    def petal(a, r0, r1, wmax, fill, spine=True, bend=0.0):
        left, right = [], []
        for t in range(15):
            u = t / 14
            rr = r0 + (r1 - r0) * u
            wd = math.sin(math.pi * min(1.0, u ** 0.62)) ** 0.9 * wmax * (1 - 0.3 * u)
            ax = a + bend * math.sin(u * math.pi)
            left.append((c + math.cos(ax) * R * rr - math.sin(ax) * R * wd, c + math.sin(ax) * R * rr + math.cos(ax) * R * wd))
            right.append((c + math.cos(ax) * R * rr + math.sin(ax) * R * wd, c + math.sin(ax) * R * rr - math.cos(ax) * R * wd))
        d.line(left + right[::-1] + [left[0]], fill=fill, width=int(2.3 * ss), joint="curve")
        if spine:
            d.line([(c + math.cos(a) * R * r0, c + math.sin(a) * R * r0), (c + math.cos(a) * R * (r1 - 0.02), c + math.sin(a) * R * (r1 - 0.02))], fill=int(fill * 0.7), width=int(1.1 * ss))
            for t in range(3, 12, 2):
                u = t / 14
                rr = r0 + (r1 - r0) * u
                d.line([(c + math.cos(a) * R * rr, c + math.sin(a) * R * rr), left[min(14, t + 2)]], fill=100, width=max(1, int(0.8 * ss)))
                d.line([(c + math.cos(a) * R * rr, c + math.sin(a) * R * rr), right[min(14, t + 2)]], fill=100, width=max(1, int(0.8 * ss)))

    for k in range(16):                                    # sixteen long feather petals, sixteen short ones between them, sixteen tiny at the rim
        a = 2 * math.pi * k / 16
        petal(a, 0.32, 0.805, 0.078, 255, True, 0.01)
        petal(a + math.pi / 16, 0.46, 0.76, 0.050, 200, True, -0.01)
    for k in range(48):
        a = 2 * math.pi * (k + 0.5) / 48
        bx, by = c + math.cos(a) * R * 0.830, c + math.sin(a) * R * 0.830
        e = 1.7 * ss
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255)
    for k in range(8):                                     # the heart: eight small petals round a dot
        a = 2 * math.pi * k / 8
        petal(a, 0.04, 0.26, 0.050, 230, False)
    d.ellipse([c - 0.04 * R, c - 0.04 * R, c + 0.04 * R, c + 0.04 * R], fill=255)
    big = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    sharp = big * smooth(1.0, 0.985, r)
    finish("jibril_seal", sharp, th / math.tau * 2.0 + r * 1.2, glow=((2.0, 0.7), (6.5, 0.38)))


# ---------------------------------------------------------------------------------------------------- halo: nested rings wreathed in tiny feathers
def halo(size=512, ss=3):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def small_feather(a, rr, L, wd, fill=255):
        # a tiny feather lying along the circle (tangent), centred on radius rr
        tx, ty = -math.sin(a), math.cos(a)
        nx, ny = math.cos(a), math.sin(a)
        px, py = c + nx * R * rr, c + ny * R * rr
        left, right = [], []
        for t in range(9):
            u = t / 8
            along = (u - 0.5) * L
            w_ = math.sin(math.pi * min(1.0, u ** 0.7)) * wd
            left.append((px + tx * R * along + nx * R * w_, py + ty * R * along + ny * R * w_))
            right.append((px + tx * R * along - nx * R * w_, py + ty * R * along - ny * R * w_))
        d.polygon(left + right[::-1], fill=fill)
        d.line([(px - tx * R * L * 0.5, py - ty * R * L * 0.5), (px + tx * R * L * 0.5, py + ty * R * L * 0.5)], fill=int(fill * 0.45), width=max(1, int(ss * 0.7)))

    for k in range(18):                                    # a wreath of small feathers between the two inner rings, all leaning the same way
        a = 2 * math.pi * k / 18
        small_feather(a, 0.66, 0.17, 0.034)
    for k in range(18):
        a = 2 * math.pi * (k + 0.5) / 18
        small_feather(a, 0.57, 0.12, 0.022, 200)
    for k in range(24):                                    # beads on the outer ring
        a = math.radians(15 * k)
        bx, by = c + math.cos(a) * R * 0.835, c + math.sin(a) * R * 0.835
        e = (2.4 if k % 2 == 0 else 1.5) * ss
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255)
    big = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    a = np.exp(-(((r - 0.915) / 0.011) ** 2)) + np.exp(-(((r - 0.915) / 0.036) ** 2)) * 0.18
    a = a + np.exp(-(((r - 0.835) / 0.0042) ** 2)) * 0.75
    a = a + np.exp(-(((r - 0.50) / 0.0040) ** 2)) * 0.8 + np.exp(-(((r - 0.485) / 0.0030) ** 2)) * 0.4
    a = a + big * 0.95
    a *= smooth(1.0, 0.975, r)
    finish("jibril_halo", a, th / math.tau * 2.0 + r * 1.0, glow=((2.4, 1.0), (8.0, 0.45)))


# ---------------------------------------------------------------------------------------------------- holy wave front
def wave(size=512):
    rng = np.random.default_rng(909)
    x, y, r, th = disc_coords(size)
    rip = 0.007 * np.sin(th * 18 + 0.6)                    # a rippling front, with a second thinner strand braided through it
    lead = np.exp(-(((r - 0.950 - rip) / 0.0060) ** 2))
    strand = np.exp(-(((r - 0.950 + rip * 1.0 - 0.0) / 0.0036) ** 2)) * 0.7
    lead_halo = np.exp(-(((r - 0.950) / 0.034) ** 2)) * 0.30
    wake = np.zeros_like(r)
    swirl = 0.85
    for k in range(48):                                    # a spiral wake of petals: each a short curved streak that trails inside the front
        ak = 2 * math.pi * k / 48 + rng.uniform(-0.04, 0.04)
        ln = rng.uniform(0.10, 0.40)
        dth = np.angle(np.exp(1j * (th - ak - swirl * (0.950 - r))))
        width = 0.011 + 0.012 * (0.95 - r)
        prof = np.exp(-((dth * r / width) ** 2))
        trail = np.clip((0.950 - r) / ln, 0, 1)
        wake = wake + prof * np.where(r < 0.950, (1 - trail) ** 1.4, 0.0) * rng.uniform(0.45, 0.9)
    band = np.where(r < 0.950, np.exp(-(0.950 - r) / 0.20) * 0.10, 0.0)
    sharp = np.clip(lead + strand, 0, 1)
    soft = np.clip(lead_halo + wake * 0.75 + band, 0, 1)
    a = np.clip(sharp + soft * (1 - sharp * 0.6), 0, 1) * smooth(1.0, 0.975, r) * smooth(0.0, 0.10, r)
    # colour: pearl on the front, the glow takes the prismatic colour that cycles with the angle and with the depth behind the front
    ir = iris(th / math.tau * 2.0 + (0.95 - r) * 1.6)
    corec = col(PEARL) * 0.85 + ir * 0.15
    w = np.clip(sharp, 0, 1)[..., None]
    rgb = corec * w + ir * (1 - w)
    save(rgb, a, "jibril_wave")


# ---------------------------------------------------------------------------------------------------- rays: a feathered sunburst
def rays(size=512):
    rng = np.random.default_rng(1313)
    x, y, r, th = disc_coords(size)
    a = np.zeros_like(r)
    ph = np.zeros_like(r)
    wsum = np.zeros_like(r)
    n = 28
    for k in range(n):
        ak = 2 * math.pi * k / n + rng.uniform(-0.04, 0.04)
        wid = rng.uniform(0.035, 0.075)
        ln = rng.uniform(0.60, 1.0)
        curl = 0.55                                         # the rays curl a little, like feather tips
        dth = np.angle(np.exp(1j * (th - ak - curl * r * r)))
        v = np.exp(-((dth / (wid * (1.2 - 0.7 * r))) ** 2)) * np.clip(1 - r / ln, 0, 1) ** 1.3 * rng.uniform(0.5, 1.0)
        a = a + v
        ph = ph + v * (k / n)
        wsum = wsum + v
    a = a * smooth(0.02, 0.16, r)
    glow = np.exp(-((r / 0.17) ** 2)) * 0.6
    a = np.clip(a + glow, 0, 1) * smooth(1.0, 0.94, r)
    phase = ph / np.maximum(wsum, 1e-4) + r * 0.5
    ir = iris(phase, 0.35)
    core = np.clip(a ** 2.2, 0, 1)[..., None]
    rgb = ir * (1 - core) + col(PEARL) * core
    save(rgb, a, "jibril_rays")


# ---------------------------------------------------------------------------------------------------- shared: a petal / a leaf shape on a periodic strip
def strip_coords(w, h):
    xx, yy = grid(w, h)
    return (xx - (w - 1) / 2) / ((w - 1) / 2), yy / h


def leaf_field(x, v, cx, cv, half_w, half_l, tilt=0.0, period=True):
    """Soft mask of a leaf centred at (cx, cv) in a strip (V periodic), half width / half length (strip units), tilted by `tilt` (radians, 0 = long axis along V)."""
    dv = v - cv
    if period:
        dv = ((dv + 0.5) % 1.0) - 0.5
    dx = x - cx
    ct, st = math.cos(tilt), math.sin(tilt)
    ax = dx * ct - dv * 2.0 * st                          # V is stretched ~ 2 (the strip is 1:4 but x spans 2 units)
    av = (dx * st + dv * 2.0 * ct)
    q = (ax / half_w) ** 2 + (av / (half_l * 2.0)) ** 2
    return np.exp(-q * 1.8)


# ---------------------------------------------------------------------------------------------------- pillar
def pillar(w=128, h=512):
    rng = np.random.default_rng(555)
    x, v = strip_coords(w, h)
    core = np.exp(-((x / 0.12) ** 2))
    mid = np.exp(-((x / 0.34) ** 2)) * 0.50
    hz = np.exp(-((x / 0.78) ** 2)) * 0.30
    # the spiral braid: three strands, each a sine across the pillar, seamless (3 turns per texture); the strand in front is bright, the one behind is dim
    strands = np.zeros_like(x)
    sph = np.zeros_like(x)
    sw = np.zeros_like(x)
    for k in range(3):
        t = 2 * math.pi * (3 * v + k / 3.0)
        xs = 0.50 * np.sin(t)
        depth = 0.5 + 0.5 * np.cos(t)                      # 1 = in front
        wd = 0.060 + 0.030 * depth
        s = np.exp(-(((x - xs) / wd) ** 2)) * (0.30 + 0.70 * depth)
        strands = strands + s
        sph = sph + s * (k / 3.0 + 0.1 * np.sin(t))
        sw = sw + s
    # falling petals and rising dust
    petals = np.zeros_like(x)
    pph = np.zeros_like(x)
    for _ in range(12):
        cx = rng.uniform(-0.72, 0.72)
        cv = rng.uniform(0, 1)
        f = leaf_field(x, v, cx, cv, rng.uniform(0.045, 0.08), rng.uniform(0.020, 0.034), rng.uniform(-0.9, 0.9))
        petals = petals + f * rng.uniform(0.55, 0.95)
        pph = pph + f * rng.uniform(0, 1)
    for _ in range(26):
        cx = rng.uniform(-0.85, 0.85)
        cv = rng.uniform(0, 1)
        dv = ((v - cv + 0.5) % 1.0) - 0.5
        petals = petals + np.exp(-(((x - cx) / 0.020) ** 2 + (dv / 0.0045) ** 2)) * rng.uniform(0.4, 0.8)
    body = (core + mid + hz) * (0.82 + 0.18 * np.cos(2 * math.pi * 4 * v))
    a = body + strands * 0.85 + petals * 0.8
    a = np.clip(a, 0, 1) * smooth(1.0, 0.80, np.abs(x))
    # colour: pearl core, the haze goes rose on the left and aqua / violet on the right, the strands are gold -> aqua -> violet, petals are rose gold
    haze_c = iris(0.02 + 0.30 * (x * 0.5 + 0.5) + 0.2 * np.sin(2 * math.pi * 2 * v), 0.25)
    strand_c = iris(sph / np.maximum(sw, 1e-4) * 0.6 + 0.12 + v * 1.0, 0.15)
    petal_c = lerp3(ROSEGOLD, PEACH, np.clip(pph / np.maximum(petals, 1e-3), 0, 1))
    wc = np.clip(core * 1.1, 0, 1)[..., None]
    base_c = haze_c * (1 - wc) + col(PEARL) * wc
    ws = np.clip(strands * 0.9, 0, 1)[..., None]
    wp = np.clip(petals * 0.9, 0, 1)[..., None]
    rgb = base_c * (1 - ws) + strand_c * ws
    rgb = rgb * (1 - wp * 0.7) + petal_c * wp * 0.7
    save(rgb, a, "jibril_pillar")


# ---------------------------------------------------------------------------------------------------- hanging law banner (columns of script)
def bars(w=128, h=512, ss=4):
    rng = np.random.default_rng(2222)
    W, H = w * ss, h * ss
    cx = W / 2
    im, d = new_mask(W, H)
    bw = 0.58                                              # banner half width (fraction of the half width of the strip)
    top, bot = 0.045, 0.93
    hw = W * 0.5 * bw * 1.3
    # the cross bar with two end knobs and tassel cords
    d.line([(cx - W * 0.46, H * top), (cx + W * 0.46, H * top)], fill=255, width=int(3.0 * ss))
    for sx in (-1, 1):
        e = 4.5 * ss
        d.ellipse([cx + sx * W * 0.46 - e, H * top - e, cx + sx * W * 0.46 + e, H * top + e], fill=255)
    # the banner body: a long rectangle with a swallow tail, a double border
    body = [(cx - hw, H * (top + 0.012)), (cx + hw, H * (top + 0.012)), (cx + hw, H * bot), (cx, H * (bot - 0.065)), (cx - hw, H * bot)]
    d.line(body + [body[0]], fill=255, width=int(2.2 * ss), joint="curve")
    inset = hw * 0.80
    body2 = [(cx - inset, H * (top + 0.035)), (cx + inset, H * (top + 0.035)), (cx + inset, H * (bot - 0.025)), (cx, H * (bot - 0.085)), (cx - inset, H * (bot - 0.025))]
    d.line(body2 + [body2[0]], fill=150, width=int(1.1 * ss), joint="curve")
    # three columns of script down the banner (the line runs along V)
    for k, ox in enumerate((-0.40, 0.0, 0.40)):
        Lc = (bot - top - 0.14) * H
        words = script_line(rng, Lc, hw * 0.44, 0.03)
        draw_words(d, words, lambda s, hh, ox=ox: (cx + ox * hw * 1.0 + hh * 0.9, H * (top + 0.07) + s), 1.4 * ss)
    # a crest at the top of the banner: a small crown of rays
    draw_emblem(d, cx, H * (top + 0.115), hw * 0.34, ss, 1.0)
    # a feather fringe along the swallow tail
    m = downsample(im, w, h)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    inside = ((np.abs(x) < bw * 1.3) & (v > top + 0.012) & (v < bot - 0.07 * (1 - np.abs(x) / (bw * 1.3)))).astype(np.float32)
    inside = blur(inside, 1.2)
    fill = inside * (0.14 + 0.12 * np.cos(x * 5) ** 2) * smooth(top, top + 0.05, v)
    sharp = np.clip(m, 0, 1)
    s = sharp
    g = np.clip(blur(s, 1.6) * 0.8 + blur(s, 5.0) * 0.4, 0, 1) * (1 - s * 0.7)
    a = np.clip(s + g + fill, 0, 1) * smooth(1.0, 0.92, np.abs(x))
    ir = iris(v * 0.9 + 0.3 * x + 0.1)
    corec = col(PEARL) * 0.85 + iris(v * 0.9 + 0.1, 0.3) * 0.15
    wgt = np.clip(s, 0, 1)[..., None]
    soft = ir
    rgb = corec * wgt + soft * (1 - wgt)
    # the banner's fill is rose gold at the foot and lavender at the head
    fillc = lerp3(LAV, ROSEGOLD, smooth(top, bot, v))
    wf = np.clip(fill / np.maximum(a, 1e-3), 0, 1)[..., None]
    rgb = rgb * (1 - wf * 0.6) + fillc * wf * 0.6
    save(rgb, a, "jibril_bars")


# ---------------------------------------------------------------------------------------------------- curtain
def curtain(w=128, h=512):
    rng = np.random.default_rng(6060)
    x, v0 = strip_coords(w, h)
    v = v0                                                 # 0 at the top, 1 at the foot
    foot = v ** 1.5
    fold = 0.5 + 0.5 * np.cos(x * math.tau * 1.25 + 0.8 + 0.9 * np.sin(v * 5.0))
    a = foot * (0.50 + 0.50 * fold) * 1.0 + 0.20 * v ** 1.2 * (0.6 + 0.4 * fold)
    ray = np.zeros_like(x)
    for _ in range(9):                                     # fine rays up the curtain, each reaching its own height
        xk = rng.uniform(-0.8, 0.8)
        wid = rng.uniform(0.018, 0.045)
        top = rng.uniform(0.05, 0.55)
        ray = ray + np.exp(-(((x - xk) / wid) ** 2)) * np.clip((v - top) / (1 - top), 0, 1) ** 0.8 * rng.uniform(0.3, 0.7)
    a = a + ray * 0.8
    # a few falling petals in the folds
    for _ in range(7):
        a = a + leaf_field(x, v, rng.uniform(-0.7, 0.7), rng.uniform(0.2, 0.95), 0.06, 0.03, rng.uniform(-0.8, 0.8), period=False) * 0.5
    a += np.exp(-(((v - 0.985) / 0.018) ** 2)) * 0.7
    a = np.clip(a, 0, 1) * smooth(1.0, 0.66, np.abs(x)) * smooth(0.0, 0.12, v)
    # dawn gradient up the curtain: rose gold at the foot, pearl, then aqua, violet at the top
    grad = np.clip(1 - v, 0, 1)
    c_low = lerp3(ROSEGOLD, PEARL, smooth(0.0, 0.40, grad))
    c_up = lerp3(PEARL, AZURE, smooth(0.40, 0.75, grad))
    c_top = lerp3((178, 150, 244), (178, 150, 244), smooth(0.7, 1.0, grad))
    rgb = np.where((grad < 0.4)[..., None], c_low, c_up)
    rgb = rgb * (1 - smooth(0.70, 1.0, grad)[..., None]) + c_top * smooth(0.70, 1.0, grad)[..., None]
    rgb = rgb * 0.82 + iris(x * 0.4 + v * 0.8, 0.3) * 0.18
    save(rgb, a, "jibril_curtain")


# ---------------------------------------------------------------------------------------------------- reflect beam: a braid with feather chevrons
def beam(w=128, h=512):
    rng = np.random.default_rng(8080)
    x, v = strip_coords(w, h)
    core = np.exp(-((x / 0.10) ** 2))
    mid = np.exp(-((x / 0.30) ** 2)) * 0.46
    hz = np.exp(-((x / 0.72) ** 2)) * 0.22
    strands = np.zeros_like(x)
    sph = np.zeros_like(x)
    sw = np.zeros_like(x)
    for k in range(2):
        t = 2 * math.pi * (4 * v + k / 2.0)
        xs = 0.42 * np.sin(t)
        depth = 0.5 + 0.5 * np.cos(t)
        s = np.exp(-(((x - xs) / (0.05 + 0.025 * depth)) ** 2)) * (0.35 + 0.65 * depth)
        strands = strands + s
        sph = sph + s * k * 0.5
        sw = sw + s
    chev = np.zeros_like(x)
    for k in range(4):                                     # feather chevrons: a V of two barbs that crosses the beam, pointing down the V axis
        v0 = (k + rng.uniform(-0.03, 0.03)) / 4.0
        pv = ((v - v0 - 0.30 * np.abs(x) + 0.5) % 1.0) - 0.5
        chev = chev + np.exp(-((pv / 0.016) ** 2)) * np.exp(-((x / 0.70) ** 2)) * rng.uniform(0.6, 0.95)
    a = (core + mid + hz) * (0.78 + 0.22 * np.cos(v * math.tau * 2)) + strands * 0.8 + chev * 0.8
    a = np.clip(a, 0, 1) * smooth(1.0, 0.82, np.abs(x))
    haze_c = iris(0.04 + 0.35 * (x * 0.5 + 0.5), 0.3)
    wc = np.clip(core * 1.2 + chev * 0.6, 0, 1)[..., None]
    base = haze_c * (1 - wc) + col(PEARL) * wc
    ws = np.clip(strands, 0, 1)[..., None]
    rgb = base * (1 - ws * 0.7) + iris(sph / np.maximum(sw, 1e-4) + 0.12, 0.2) * ws * 0.7
    save(rgb, a, "jibril_beam")


# ---------------------------------------------------------------------------------------------------- chain: a braided ribbon with feather-shaped links
def leaf_poly(cx, cy, L, wd, ang, n=10):
    """The outline of a feather-shaped link (a leaf pointing along `ang`) centred on (cx, cy); returns (left, right, spine)."""
    tx, ty = math.cos(ang), math.sin(ang)
    nx, ny = -ty, tx
    left, right = [], []
    for i in range(n + 1):
        u = i / n
        along = (u - 0.5) * L
        w_ = math.sin(math.pi * min(1.0, u ** 0.8 * 0.98 + 0.01)) ** 0.9 * wd
        left.append((cx + tx * along + nx * w_, cy + ty * along + ny * w_))
        right.append((cx + tx * along - nx * w_, cy + ty * along - ny * w_))
    return left, right


def chain(w=128, h=512, ss=4):
    W, H = w * ss, h * ss
    cx = W / 2
    im, d = new_mask(W, H)
    cyc = 4                                                # the braid turns 4 times per texture (seamless)
    # the two braid strands, drawn as polylines with a depth-dependent width: the strand in front is thick
    for k in range(2):
        pts = []
        for i in range(0, 1025):
            v = i / 1024
            t = 2 * math.pi * (cyc * v + k * 0.5)
            pts.append((cx + math.sin(t) * W * 0.17, v * H, 0.5 + 0.5 * math.cos(t)))
        for i in range(len(pts) - 1):
            wdt = (2.2 + 3.2 * pts[i][2]) * ss
            d.line([(pts[i][0], pts[i][1]), (pts[i + 1][0], pts[i + 1][1])], fill=int(150 + 105 * pts[i][2]), width=max(1, int(wdt)))
    # feather-shaped links: a leaf across the braid at every crossing, alternately tilted
    links = Image.new("L", (W, H), 0)
    ld = ImageDraw.Draw(links)
    for k in range(cyc * 2):
        cyv = (k + 0.5) / (cyc * 2) * H
        for yoff in (0, -H, H):
            left, right = leaf_poly(cx, cyv + yoff, H / (cyc * 2) * 0.96, W * 0.075, math.pi / 2 + (0.28 if k % 2 == 0 else -0.28))
            ld.polygon(left + right[::-1], fill=60)
            ld.line(left + right[::-1] + [left[0]], fill=255, width=int(1.7 * ss), joint="curve")
            ld.line([(left[0][0] / 2 + right[0][0] / 2, left[0][1] / 2 + right[0][1] / 2), (left[-1][0] / 2 + right[-1][0] / 2, left[-1][1] / 2 + right[-1][1] / 2)], fill=140, width=max(1, int(1.0 * ss)))
    lm = downsample(im, w, h)
    ll = downsample(links, w, h)
    xx, yy = grid(w, h)
    sharp = np.clip(np.maximum(lm, ll), 0, 1)
    v = yy / h
    g = np.clip(blur(sharp, 1.4) * 0.9 + blur(sharp, 4.0) * 0.4, 0, 1) * (1 - sharp * 0.7)
    a = np.clip(sharp + g, 0, 1)
    ir = iris(v * 2.0 + 0.08, 0.28)
    base = lerp3(ROSEGOLD, PEARL, np.clip(sharp * 1.1, 0, 1))
    rgb = base * (np.clip(sharp, 0, 1)[..., None]) + ir * (1 - np.clip(sharp, 0, 1))[..., None]
    rgb = rgb * 0.85 + ir * 0.15 * np.clip(sharp, 0, 1)[..., None]
    save(rgb, a, "jibril_chain")


def chainring(size=512, ss=3):
    S = size * ss
    c = S / 2
    R = c * 0.82
    n = 18
    im, d = new_mask(S, S)
    links = Image.new("L", (S, S), 0)
    ld = ImageDraw.Draw(links)
    for k in range(n):                                     # feather links lying along the ring, each leaning a little
        a = 2 * math.pi * (k + 0.5) / n
        px, py = c + math.cos(a) * R, c + math.sin(a) * R
        pitch = 2 * math.pi * R / n
        left, right = leaf_poly(px, py, pitch * 1.18, S * 0.026 * (1 if k % 2 == 0 else 0.8), a + math.pi / 2 + (0.30 if k % 2 == 0 else -0.30))
        ld.polygon(left + right[::-1], fill=70)
        ld.line(left + right[::-1] + [left[0]], fill=255, width=int(1.8 * ss), joint="curve")
        ld.line([((left[0][0] + right[0][0]) / 2, (left[0][1] + right[0][1]) / 2), ((left[-1][0] + right[-1][0]) / 2, (left[-1][1] + right[-1][1]) / 2)], fill=150, width=max(1, int(1.0 * ss)))
    for kk in range(2):                                    # two braid strands through the links
        pts = []
        for i in range(721):
            a = 2 * math.pi * i / 720
            t = n * a + kk * math.pi
            rr = R + math.sin(t) * S * 0.014
            pts.append((c + math.cos(a) * rr, c + math.sin(a) * rr, 0.5 + 0.5 * math.cos(t)))
        for i in range(len(pts) - 1):
            d.line([pts[i][:2], pts[i + 1][:2]], fill=int(130 + 125 * pts[i][2]), width=max(1, int((1.8 + 2.4 * pts[i][2]) * ss)))
    lk = downsample(links, size, size)
    br = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    sharp = np.clip(np.maximum(lk, br), 0, 1)
    g = np.clip(blur(sharp, 1.8) * 0.85 + blur(sharp, 6.0) * 0.4, 0, 1) * (1 - sharp * 0.7)
    a = np.clip(sharp + g, 0, 1) * smooth(1.0, 0.97, r)
    ir = iris(th / math.tau * 2 + 0.1, 0.28)
    base = lerp3(ROSEGOLD, PEARL, np.clip(sharp * 1.1, 0, 1))
    w_ = np.clip(sharp, 0, 1)[..., None]
    rgb = base * w_ + ir * (1 - w_)
    rgb = rgb * 0.88 + ir * 0.12 * w_
    save(rgb, a, "jibril_chainring")


# ---------------------------------------------------------------------------------------------------- feathers
def feather_mask(base, tip, wmax, bend, ss):
    """Outline of one feather from `base` to `tip` (pixels, before supersampling): a leaf with a round shoulder, a rachis line and a curve of `bend` (fraction of the length)."""
    bx, by = base
    tx, ty = tip
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    nx, ny = -uy, ux
    left, right, centre = [], [], []
    N = 20
    for i in range(N + 1):
        s = i / N
        off = bend * L * math.sin(s * math.pi) * 0.5
        wd = wmax * (math.sin(math.pi * min(1.0, s ** 0.62 * 0.98 + 0.015)) ** 0.85) * (1 - 0.30 * s)
        px, py = bx + ux * L * s + nx * off, by + uy * L * s + ny * off
        left.append(((px + nx * wd) * ss, (py + ny * wd) * ss))
        right.append(((px - nx * wd) * ss, (py - ny * wd) * ss))
        centre.append((px * ss, py * ss))
    return left, right, centre


def paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, tint_base, tint_tip, hue0, shade=1.0, shade_strength=1.0, rim_amt=0.62):
    """Paints one feather over the picture (painter's order): a pearl vane that warms to peach at the root, a prismatic rim whose hue runs along the feather, a rachis and barbs, and a cool lavender shadow on the feathers below."""
    x0 = int(max(0, min(p[0] for p in left + right) / ss - 6))
    x1 = int(min(W, max(p[0] for p in left + right) / ss + 7))
    y0 = int(max(0, min(p[1] for p in left + right) / ss - 6))
    y1 = int(min(H, max(p[1] for p in left + right) / ss + 7))
    if x1 <= x0 or y1 <= y0:
        return
    w, h = x1 - x0, y1 - y0

    def shift(pts):
        return [(px - x0 * ss, py - y0 * ss) for px, py in pts]

    L_, R_, C_ = shift(left), shift(right), shift(centre)
    fill_im, fd = new_mask(w * ss, h * ss)
    fd.polygon(L_ + R_[::-1], fill=255)
    fill = mask_of(fill_im.resize((w, h), Image.LANCZOS))
    edge_im, ed = new_mask(w * ss, h * ss)
    ed.line(L_ + R_[::-1] + [L_[0]], fill=255, width=max(1, int(1.2 * ss)), joint="curve")
    edge = mask_of(edge_im.resize((w, h), Image.LANCZOS))
    line_im, ld = new_mask(w * ss, h * ss)
    ld.line(C_, fill=255, width=max(1, int(1.0 * ss)))
    for i in range(1, len(C_) - 2):                        # barbs: short lines from the rachis out to the edges, bent towards the tip
        ld.line([C_[i], L_[i + 2]], fill=105, width=max(1, int(0.6 * ss)))
        ld.line([C_[i], R_[i + 2]], fill=105, width=max(1, int(0.6 * ss)))
    lines = mask_of(line_im.resize((w, h), Image.LANCZOS))
    xx, yy = grid(w, h)
    cx0, cy0 = C_[0][0] / ss, C_[0][1] / ss
    cx1, cy1 = C_[-1][0] / ss, C_[-1][1] / ss
    dx, dy = cx1 - cx0, cy1 - cy0
    L2 = dx * dx + dy * dy
    s = np.clip(((xx - cx0) * dx + (yy - cy0) * dy) / L2, 0, 1)
    body = lerp3(tint_base, tint_tip, smooth(0.0, 0.62, s))
    body = body * shade + col(SHADOW) * (1 - shade) * 0.55
    rim_c = iris(hue0 + s * 0.85, 0.12)
    rgb = body * (1 - edge[..., None] * rim_amt) + rim_c * edge[..., None] * rim_amt
    rgb = rgb * (1 - lines[..., None] * 0.50) + (col(LAV) * 0.6 + col(AZURE) * 0.4) * lines[..., None] * 0.50
    # the tip of the vane takes the prismatic colour too (a soft wash over the last third)
    tipw = smooth(0.62, 1.0, s)[..., None] * fill[..., None] * 0.30
    rgb = rgb * (1 - tipw) + rim_c * tipw
    al = fill * (0.80 + 0.16 * smooth(0.0, 0.6, s)) * (0.90 + 0.10 * shade)
    al = np.clip(al + edge * 0.4 * fill, 0, 1)
    sub_rgb = acc_rgb[y0:y1, x0:x1]
    sub_a = acc_a[y0:y1, x0:x1]
    shadow = np.clip(blur(fill, 2.4 * (W / 512.0) ** 0.5) * 0.55 - fill * 0.55, 0, 1) * np.clip(sub_a, 0, 1) * shade_strength
    sub_rgb[:] = sub_rgb * (1 - shadow[..., None] * 1.15) + (col(LAV) * 0.5 + col(SHADOW) * 0.5) * 0.55 * shadow[..., None] * 1.15
    sub_rgb[:] = rgb * al[..., None] + sub_rgb * (1 - al[..., None])
    sub_a[:] = al + sub_a * (1 - al)


def feather(size=128, ss=5):
    W = H = size
    acc_rgb = np.zeros((H, W, 3), np.float32)
    acc_a = np.zeros((H, W), np.float32)
    left, right, centre = feather_mask((size * 0.46, size * 0.97), (size * 0.58, size * 0.03), size * 0.19, -0.20, ss)
    paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, PEACH, PEARL, 0.05, rim_amt=0.7)
    acc_a = np.clip(acc_a + blur(acc_a, 2.0) * 0.40, 0, 1)
    covered = acc_rgb.sum(axis=2, keepdims=True) > 0.05
    acc_rgb = np.where(covered, acc_rgb, col(PEARL))
    save(acc_rgb, acc_a, "jibril_feather")


def wing(w=1024, h=512, ss=2):
    """One wing seen from behind: a leading-edge arm along the top from the root (left) over the wrist to the hand, long primaries fanning out to the right and down, shorter secondaries
    hanging below the arm and a row of coverts over their roots. Pearl vanes, peach roots, a prismatic rim that cycles along the feather row, lavender shadows."""
    W, H = w, h
    acc_rgb = np.zeros((H, W, 3), np.float32)
    acc_a = np.zeros((H, W), np.float32)
    k = W / 512.0

    def bez(t):
        p0, p1, p2, p3 = (0.02, 0.58), (0.06, 0.08), (0.30, 0.03), (0.86, 0.16)       # in picture fractions
        mt = 1 - t
        return (mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0],
                mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1])

    rng = np.random.default_rng(31337)
    # a back row of long, dim feathers (they give the wing its depth)
    nb = 12
    for i in range(nb):
        f = (i + 0.5) / nb
        bx, by = bez(0.14 + 0.78 * f)
        tipx = 0.10 + 0.84 * f ** 0.85
        tipy = 0.97 - 0.52 * f ** 1.15
        left, right, centre = feather_mask((bx * W, by * H + 4), (tipx * W, tipy * H), 12.0 * k * 1.5, -0.12 + 0.14 * f, ss)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, (238, 170, 170), LAV, 0.02 + 0.5 * f, 0.55, 1.0)
    # the long feathers, drawn from the inner one (hanging) to the outer one (the longest, pointing out) so the outer ones lie on top
    nl = 16
    for i in range(nl):
        f = i / (nl - 1)
        bx, by = bez(0.12 + 0.84 * f)
        tipx = 0.07 + 0.925 * f ** 0.8
        tipy = 0.985 - 0.56 * f ** 1.15
        base = (bx * W, by * H + 3)
        tip = (tipx * W, tipy * H)
        wm = (13.0 + 7.0 * math.sin(math.pi * min(1.0, f * 0.85 + 0.15))) * k * 1.5
        left, right, centre = feather_mask(base, tip, wm, -0.10 + 0.14 * f + rng.uniform(-0.02, 0.02), ss)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, (250, 184, 160), PEARL, 0.0 + 0.9 * f, 0.84 + 0.16 * f)
    # a second, shorter row between the long ones and the arm: they fill the gaps
    ns = 15
    for i in range(ns):
        f = (i + 0.5) / ns
        bx, by = bez(0.10 + 0.84 * f)
        tipx = bx + 0.04 + 0.16 * f
        tipy = by + 0.50 - 0.17 * f
        left, right, centre = feather_mask((bx * W, by * H + 2), (tipx * W, tipy * H), 11.0 * k * 1.5, -0.08, ss)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, (255, 206, 186), WHITE, 0.15 + 0.8 * f, 0.92)
    # the coverts: small round feathers over the roots of the long ones
    nc = 24
    for i in range(nc):
        f = i / (nc - 1)
        bx, by = bez(0.06 + 0.88 * f)
        tipx = bx + 0.03 + 0.03 * f
        tipy = by + 0.20 - 0.06 * f
        left, right, centre = feather_mask((bx * W, by * H), (tipx * W, tipy * H), 8.0 * k * 1.5, -0.06, ss)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, (255, 222, 206), WHITE, 0.3 + 0.7 * f, 0.97, rim_amt=0.55)
    # the leading edge: a thick rose-gold arm with a pearl sheen and a round joint at the wrist
    arm_im, ad = new_mask(W * ss, H * ss)
    pts = [(bez(t / 60)[0] * W * ss, bez(t / 60)[1] * H * ss) for t in range(61)]
    ad.line(pts, fill=255, width=int(7.0 * k * ss), joint="curve")
    wx, wy = bez(0.70)
    ad.ellipse([wx * W * ss - 7.0 * k * ss, wy * H * ss - 7.0 * k * ss, wx * W * ss + 7.0 * k * ss, wy * H * ss + 7.0 * k * ss], fill=255)
    sheen_im, sd = new_mask(W * ss, H * ss)
    sd.line([(x, y - 1.6 * k * ss) for x, y in pts], fill=255, width=int(2.2 * k * ss), joint="curve")
    arm = mask_of(arm_im.resize((W, H), Image.LANCZOS))
    sheen = mask_of(sheen_im.resize((W, H), Image.LANCZOS))
    a3 = arm[..., None]
    xx, yy = grid(W, H)
    arm_c = lerp3(ROSEGOLD, AMBER, smooth(0.0, 0.8, xx / W))
    arm_rgb = arm_c * (1 - sheen[..., None]) + col(PEARL) * sheen[..., None]
    acc_rgb = acc_rgb * (1 - a3) + arm_rgb * a3
    acc_a = np.clip(acc_a + arm * 0.95, 0, 1)
    acc_a *= smooth(0.0, 4.0, np.minimum(xx, W - 1 - xx)) * smooth(0.0, 3.0, np.minimum(yy, H - 1 - yy))
    acc_a = np.clip(acc_a + blur(acc_a, 3.5) * 0.30, 0, 1)
    covered = acc_rgb.sum(axis=2, keepdims=True) > 0.05
    acc_rgb = np.where(covered, acc_rgb, col(PEACH) * 0.6 + col(PEARL) * 0.4)
    save(acc_rgb, acc_a, "jibril_wing")


# ---------------------------------------------------------------------------------------------------- flare / mote
def flare(size=256):
    """A crown-of-rays flare: a soft pearl core, twelve tapered rays of two lengths, a prismatic lens ring and a faint outer halo (not a plain four-point cross)."""
    x, y, r, th = disc_coords(size)
    core = np.exp(-(r * r) / 0.006)
    halo_ = np.exp(-(r * r) / 0.06) * 0.40
    rays_ = np.zeros_like(r)
    for k in range(12):
        ak = 2 * math.pi * k / 12 + 0.13
        long_ = k % 2 == 0
        ln = 1.0 if long_ else 0.52
        dth = np.angle(np.exp(1j * (th - ak)))
        wid = 0.020 if long_ else 0.026
        rays_ = rays_ + np.exp(-((dth * np.maximum(r, 0.02) / wid) ** 2)) * np.clip(1 - r / ln, 0, 1) ** 1.7 * (0.6 if long_ else 0.4)
    lens = np.exp(-(((r - 0.40) / 0.012) ** 2)) * 0.32 + np.exp(-(((r - 0.62) / 0.008) ** 2)) * 0.16
    a = np.clip(core + halo_ + rays_ + lens, 0, 1) * smooth(1.0, 0.92, r)
    ir = iris(th / math.tau * 2 + r * 0.6, 0.30)
    w_ = np.clip(core + halo_ * 0.6 + rays_ * 0.9, 0, 1)[..., None]
    rgb = col(PEARL) * w_ + ir * (1 - w_)
    save(rgb, a, "jibril_flare")


def mote(size=64):
    """A small six-ray sparkle with a pearl core and a faint prismatic fringe."""
    x, y, r, th = disc_coords(size)
    arms = np.zeros_like(r)
    for k in range(3):
        ak = math.pi * k / 3 + 0.2
        dth = np.angle(np.exp(1j * (th - ak)))
        dth2 = np.angle(np.exp(1j * (th - ak - math.pi)))
        arms = arms + (np.exp(-((dth * np.maximum(r, 0.02) / 0.05) ** 2)) + np.exp(-((dth2 * np.maximum(r, 0.02) / 0.05) ** 2))) * np.clip(1 - r, 0, 1) ** 1.5
    core = np.exp(-(r * r) / 0.035)
    a = np.clip(arms * 0.8 + core, 0, 1) * smooth(1.0, 0.85, r)
    ir = iris(th / math.tau + r, 0.35)
    w_ = np.clip(core * 1.2 + arms * 0.4, 0, 1)[..., None]
    rgb = col(PEARL) * w_ + ir * (1 - w_)
    save(rgb, a, "jibril_mote")


# ---------------------------------------------------------------------------------------------------- the law scroll
def scroll(w=1024, h=512, ss=2):
    rng = np.random.default_rng(1717)
    W, H = w * ss, h * ss
    im, d = new_mask(W, H)
    soft_im, sd = new_mask(W, H)
    top, bot = 0.20, 0.80
    left, right = 0.065, 0.935
    # the parchment: a faint panel (soft fill), a double border with small corner dots
    sd.rectangle([W * left, H * top, W * right, H * bot], fill=70)
    d.rectangle([W * left, H * top, W * right, H * bot], outline=255, width=int(2.0 * ss))
    d.rectangle([W * (left + 0.014), H * (top + 0.026), W * (right - 0.014), H * (bot - 0.026)], outline=150, width=int(1.0 * ss))
    # the two rolls: tall cylinders at the ends, with rings
    for sx, x0 in ((0, 0.018), (1, 0.935)):
        d.rounded_rectangle([W * x0, H * 0.13, W * (x0 + 0.047), H * 0.87], radius=W * 0.02, outline=255, width=int(2.4 * ss))
        sd.rounded_rectangle([W * x0, H * 0.13, W * (x0 + 0.047), H * 0.87], radius=W * 0.02, fill=110)
        for yy_ in (0.19, 0.81):
            d.line([(W * x0, H * yy_), (W * (x0 + 0.047), H * yy_)], fill=200, width=int(1.6 * ss))
        for t in range(6):
            yy_ = 0.30 + 0.08 * t
            d.line([(W * (x0 + 0.012), H * yy_), (W * (x0 + 0.035), H * yy_)], fill=120, width=max(1, int(1.0 * ss)))
        e = 5.0 * ss
        d.ellipse([W * (x0 + 0.0235) - e, H * 0.115 - e, W * (x0 + 0.0235) + e, H * 0.115 + e], fill=255)
        d.ellipse([W * (x0 + 0.0235) - e, H * 0.885 - e, W * (x0 + 0.0235) + e, H * 0.885 + e], fill=255)
    # the crown-of-rays seal in the middle top and the script: seven lines of calligraphy
    draw_emblem(d, W * 0.5, H * (top + 0.095), H * 0.075, ss, 1.3)
    nlines = 7
    y_first = top + 0.19
    for k in range(nlines):
        y = H * (y_first + (bot - 0.06 - y_first) * k / (nlines - 1))
        x_start = W * (left + 0.04)
        Lc = W * (right - left - 0.08)
        if k == nlines - 1:
            Lc *= 0.55
        words = script_line(rng, Lc, H * 0.052, 0.02, 0.0)
        draw_words(d, words, lambda s, hh, y=y, x_start=x_start: (x_start + s, y + hh), 1.6 * ss)
    big = downsample(im, w, h)
    soft = downsample(soft_im, w, h)
    xx, yy = grid(w, h)
    u = xx / w
    v = yy / h
    sharp = np.clip(big, 0, 1)
    g = np.clip(blur(sharp, 1.8) * 0.85 + blur(sharp, 6.0) * 0.4, 0, 1) * (1 - sharp * 0.7)
    a = np.clip(sharp + g + soft * 0.5, 0, 1)
    ir = iris(u * 1.2 + v * 0.5, 0.28)
    parch = lerp3(PEACH, LAV, smooth(top, bot, v))
    w_ = np.clip(sharp, 0, 1)[..., None]
    rgb = col(PEARL) * 0.9 * w_ + col((255, 236, 220)) * 0.1 * w_ + (ir * 0.6 + parch * 0.4) * (1 - w_)
    save(rgb, a, "jibril_scroll")


# ---------------------------------------------------------------------------------------------------- the emblem
def emblem(size=512, ss=3):
    S = size * ss
    c = S / 2
    im, d = new_mask(S, S)
    R = c * 0.86
    d.ellipse([c - R * 1.08, c - R * 1.08, c + R * 1.08, c + R * 1.08], outline=255, width=int(2.4 * ss))
    d.ellipse([c - R * 1.02, c - R * 1.02, c + R * 1.02, c + R * 1.02], outline=150, width=int(1.0 * ss))
    for k in range(36):
        a = 2 * math.pi * k / 36
        e = (2.2 if k % 3 == 0 else 1.2) * ss
        bx, by = c + math.cos(a) * R * 1.045, c + math.sin(a) * R * 1.045
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255 if k % 3 == 0 else 150)
    draw_emblem(d, c, c + 0.02 * S, R * 0.98, ss, 2.4)
    big = downsample(im, size, size)
    x, y, r, th = disc_coords(size)
    finish("jibril_emblem", big * smooth(1.0, 0.985, r), th / math.tau + r * 0.7, glow=((2.0, 0.9), (7.0, 0.45)))


# ---------------------------------------------------------------------------------------------------- a falling petal
def petal(size=64):
    x, y, r, th = disc_coords(size)
    # a petal: an almond lying diagonally with a notch at the tip and a soft fold line
    u = (x * 0.8 + y * 0.6)
    v = (-x * 0.6 + y * 0.8)
    wdt = 0.42 * np.sqrt(np.clip(1 - (u / 0.88) ** 2, 0, 1)) * (1 - 0.2 * np.clip(u, -1, 1))
    m = smooth(wdt + 0.04, wdt - 0.04, np.abs(v)) * (np.abs(u) < 0.9)
    notch = np.exp(-(((u - 0.86) / 0.07) ** 2) - ((v / 0.05) ** 2)) * 0.7
    fold = np.exp(-((v / 0.04) ** 2)) * 0.35 * (np.abs(u) < 0.8)
    a = np.clip(m - notch, 0, 1)
    shade = np.clip(0.5 + 0.5 * (-v / 0.45), 0, 1)
    rgb = lerp3(ROSEGOLD, PEACH, np.clip(0.5 + 0.55 * u, 0, 1)) * (1 - 0.25 * shade[..., None]) + col(PEARL) * fold[..., None] * 0.7
    rgb = rgb * 0.9 + iris(u * 0.5 + 0.1, 0.3) * 0.1
    a = np.clip(a + blur(a, 1.2) * 0.25, 0, 1) * smooth(1.0, 0.9, r)
    save(rgb, a, "jibril_petal")


# ---------------------------------------------------------------------------------------------------- ribbon of light
def ribbon(w=128, h=512):
    x, v = strip_coords(w, h)
    t = 2 * math.pi * 2 * v
    xc = 0.34 * np.sin(t)                                  # the ribbon's centre line meanders (seamless: two waves)
    wd = 0.20 + 0.06 * np.cos(t * 1.0 + 1.0)
    xr = (x - xc) / wd
    body = smooth(1.0, 0.72, np.abs(xr))
    edge = np.exp(-(((np.abs(xr) - 0.93) / 0.07) ** 2)) * 0.9
    centre = np.exp(-((xr / 0.14) ** 2)) * 0.65
    fold = 0.55 + 0.45 * np.cos(2 * math.pi * 6 * v + xr * 1.6)
    hz = np.exp(-((x - xc) / (wd * 2.4)) ** 2) * 0.22
    a = np.clip(body * (0.38 + 0.30 * fold) + edge + centre + hz, 0, 1) * smooth(1.0, 0.82, np.abs(x))
    # the colour runs across the ribbon: pink -> gold -> aqua -> violet from one edge to the other, and cycles slowly along it
    ph = (xr * 0.5 + 0.5) * 0.85 + 0.15 * v
    ir = iris(ph, 0.18)
    wc = np.clip(centre * 1.3 + edge * 0.5, 0, 1)[..., None]
    rgb = ir * (1 - wc) + col(PEARL) * wc
    save(rgb, a, "jibril_ribbon")


# ---------------------------------------------------------------------------------------------------- spiral of six ribbons
def spiral(size=512):
    rng = np.random.default_rng(2929)
    x, y, r, th = disc_coords(size)
    a = np.zeros_like(r)
    ph = np.zeros_like(r)
    ws = np.zeros_like(r)
    n = 6
    for k in range(n):
        ak = 2 * math.pi * k / n
        dth = np.angle(np.exp(1j * (th - ak - 2.6 * r - 0.5 * r * r)))
        wid = 0.020 + 0.062 * r
        prof = np.exp(-((dth * np.maximum(r, 0.03) / wid) ** 2))
        sheen = 0.55 + 0.45 * np.cos(dth * np.maximum(r, 0.03) / wid * 2.2)
        v = prof * smooth(0.04, 0.22, r) * smooth(1.0, 0.80, r) * (0.55 + 0.45 * r)
        a = a + v * sheen
        ph = ph + v * (k / n)
        ws = ws + v
    # fine counter strands between the ribbons
    for k in range(12):
        ak = 2 * math.pi * (k + 0.5) / 12
        dth = np.angle(np.exp(1j * (th - ak + 1.6 * r)))
        a = a + np.exp(-((dth * np.maximum(r, 0.03) / 0.010) ** 2)) * smooth(0.10, 0.4, r) * smooth(1.0, 0.7, r) * 0.28
    a = np.clip(a + np.exp(-((r / 0.12) ** 2)) * 0.5, 0, 1) * smooth(1.0, 0.94, r)
    phase = ph / np.maximum(ws, 1e-3) + r * 0.7
    ir = iris(phase, 0.3)
    core = np.clip(a ** 2.0, 0, 1)[..., None]
    rgb = ir * (1 - core) + col(PEARL) * core
    save(rgb, a, "jibril_spiral")


# ---------------------------------------------------------------------------------------------------- prism bloom
def prism(size=256):
    x, y, r, th = disc_coords(size)
    core = np.exp(-(r * r) / 0.035)
    soft = np.exp(-(r * r) / 0.30) * 0.40
    bow = np.exp(-(((r - 0.60) / 0.13) ** 2)) * 0.55          # a broad rainbow ring, like the 22 degree halo round the sun
    bow2 = np.exp(-(((r - 0.82) / 0.07) ** 2)) * 0.22
    a = np.clip(core + soft + bow + bow2, 0, 1) * smooth(1.0, 0.90, r)
    phase = (r - 0.45) * 2.4                                # the colour runs across the ring: red inside, violet outside
    ir = iris(phase + 0.02, 0.15)
    wc = np.clip((core + soft) * 1.1, 0, 1)[..., None]
    rgb = ir * (1 - wc) + col(PEARL) * wc
    save(rgb, a, "jibril_prism")


ALL = ["ring", "glyph", "seal", "halo", "wave", "rays", "pillar", "bars", "curtain", "beam", "chain", "chainring", "feather", "wing", "flare", "mote",
       "scroll", "emblem", "petal", "ribbon", "spiral", "prism"]

if __name__ == "__main__":
    want = sys.argv[1:] or ALL
    for n in want:
        globals()[n]()

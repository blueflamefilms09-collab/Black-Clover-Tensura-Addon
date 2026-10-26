"""Generates the Veldanava "Jibril, Lord of Rigor" VFX textures (vfx.client.layer.JibrilLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/jibril_*.png  (new files only; nothing existing is touched)

    python tools/gen_jibril_vfx_textures.py

Theme: the angel of law and rigor. White and warm gold light, pale sky-blue shadow, small rose-white accents; halos, law glyphs (angular rune strokes, a scales-of-law
star), chains of light, feathered wings, a pillar from the sky, curtains of light. Most textures are white (+ alpha) so the vertex colour tints them. The ones with baked colour
(chain, chainring, feather, wing) carry the palette themselves and are drawn with a white vertex colour. Nothing carries colour under a fully transparent pixel (rgb is zeroed where alpha is 0).

    jibril_ring       thin crisp circle with fine ticks and a soft halo (shock rings, parry rings, rims)
    jibril_glyph      a law-glyph disc: three rings, a band of rune strokes, a scales star and a spoked centre
    jibril_seal       a big floor seal: rune cells in a band, concentric rings, a twelve-pointed star (drawn faint, huge)
    jibril_halo       a halo: a double ring with beads and short spikes
    jibril_wave       a holy wave front: a bright edge with fine light spokes trailing inside it
    jibril_rays       a sunburst of long rays (rotating sweeping light on the floor)
    jibril_pillar     a pillar of light with rising streaks (seamless in V, it scrolls)
    jibril_bars       a vertical rune bar: a bright rod with a rune block in every cell (seamless in V)
    jibril_curtain    a curtain of light: strong at the foot, fine rays above it
    jibril_beam       a reflected beam with chevron pulses (seamless in V)
    jibril_chain      a chain of light links (seamless in V, baked colour)
    jibril_chainring  a ring made of chain links, for wrapping round a target (baked colour)
    jibril_feather    one feather, tip up (baked colour)
    jibril_wing       one wing of feathers, root at the left, tip to the right (baked colour)
    jibril_flare      a four-point star flare with a lens ring
    jibril_mote       a tiny plus-shaped sparkle
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
IVORY = (255, 246, 222)
GOLD = (255, 216, 122)
GOLD_DK = (214, 156, 58)
SKY = (189, 233, 255)
SKY_DK = (120, 190, 238)
ROSE = (255, 214, 224)
WHITE = (255, 255, 255)


def save(arr_rgb, alpha, name):
    """Writes an RGBA png from float arrays (rgb 0..1, alpha 0..1). rgb is zeroed under a fully transparent pixel."""
    a = np.clip(alpha, 0, 1)
    rgb = np.clip(arr_rgb, 0, 1)
    px = np.dstack([rgb[..., 0], rgb[..., 1], rgb[..., 2], a])
    px = (px * 255 + 0.5).astype(np.uint8)
    px[px[..., 3] == 0, :3] = 0
    im = Image.fromarray(px, "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def save_white(a, name, tint=(255, 255, 255)):
    h, w = a.shape
    rgb = np.dstack([np.full((h, w), tint[0] / 255.0), np.full((h, w), tint[1] / 255.0), np.full((h, w), tint[2] / 255.0)])
    save(rgb, a, name)


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


def glow_add(a, radius, gain):
    return np.clip(a + blur(a, radius) * gain, 0, 1)


def col(c):
    return np.array(c, np.float32) / 255.0


def lerp3(c0, c1, t):
    t = np.clip(t, 0, 1)[..., None]
    return col(c0) * (1 - t) + col(c1) * t


def mask_of(im):
    return np.asarray(im.convert("L"), dtype=np.float32) / 255.0


def new_mask(W, H):
    im = Image.new("L", (W, H), 0)
    return im, ImageDraw.Draw(im)


def rune_strokes(rng):
    """A random rune in a unit cell: 3-5 straight strokes between points of a 3 x 4 grid, always with a spine, so it reads as a written sign (not noise)."""
    pts = [(gx / 2.0, gy / 3.0) for gx in range(3) for gy in range(4)]
    strokes = [((0.5, 0.0), (0.5, 1.0))]
    for _ in range(int(rng.integers(3, 6))):
        a, b = rng.choice(len(pts), 2, replace=False)
        strokes.append((pts[a], pts[b]))
    return strokes


def draw_rune(d, strokes, cx, cy, w, h, rot, width, fill=255):
    """Draws a rune of size w x h centred on (cx, cy), turned by rot radians."""
    cr, sr = math.cos(rot), math.sin(rot)
    for (x0, y0), (x1, y1) in strokes:
        pts = []
        for (ux, uy) in ((x0, y0), (x1, y1)):
            lx, ly = (ux - 0.5) * w, (uy - 0.5) * h
            pts.append((cx + lx * cr - ly * sr, cy + lx * sr + ly * cr))
        d.line(pts, fill=fill, width=max(1, int(width)))


# ---------------------------------------------------------------------------------------------------- ring
def ring(size=256, ss=4):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)
    for k in range(120):                                   # fine ticks just inside the ring, a longer one every 30 degrees
        a = math.radians(k * 3)
        long_ = k % 10 == 0
        r1 = 0.90 if long_ else 0.935
        d.line([(c + math.cos(a) * R * 0.955, c + math.sin(a) * R * 0.955), (c + math.cos(a) * R * r1, c + math.sin(a) * R * r1)], fill=255 if long_ else 140, width=int((2.2 if long_ else 1.2) * ss))
    for k in range(4):                                     # small diamonds on the four points, outside the ring
        a = math.radians(90 * k)
        px, py = c + math.cos(a) * R * 0.99, c + math.sin(a) * R * 0.99
        e = 3.4 * ss
        d.polygon([(px + math.cos(a) * e * 1.4, py + math.sin(a) * e * 1.4), (px - math.sin(a) * e * 0.8, py + math.cos(a) * e * 0.8), (px - math.cos(a) * e * 0.2, py - math.sin(a) * e * 0.2), (px + math.sin(a) * e * 0.8, py - math.cos(a) * e * 0.8)], fill=255)
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    core = np.exp(-(((r - 0.965) / 0.0085) ** 2))
    halo = np.exp(-(((r - 0.965) / 0.040) ** 2)) * 0.34
    inner = np.exp(-(((r - 0.915) / 0.0045) ** 2)) * 0.30
    a = (core + halo + inner) * (0.94 + 0.06 * np.cos(th * 6)) + big * 0.7
    a *= smooth(1.0, 0.975, r)
    save_white(a, "jibril_ring")


# ---------------------------------------------------------------------------------------------------- law glyph disc
def glyph(size=512, ss=3):
    rng = np.random.default_rng(7117)
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def circle(rad, w, fill=255):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss)))

    circle(0.985, 3.0)
    circle(0.93, 1.4, 200)
    circle(0.70, 2.2)
    circle(0.665, 1.0, 150)
    circle(0.40, 2.0)
    circle(0.095, 1.8)
    n = 36                                                 # the rune band between the outer rings: 36 runes, each turned to stand upright on the ring
    for k in range(n):
        a = 2 * math.pi * (k + 0.5) / n
        draw_rune(d, rune_strokes(rng), c + math.cos(a) * R * 0.815, c + math.sin(a) * R * 0.815, R * 0.050, R * 0.130, a + math.pi / 2, 2.0 * ss)
        if k % 3 == 0:                                     # a bead between every third pair
            ba = 2 * math.pi * k / n
            bx, by = c + math.cos(ba) * R * 0.815, c + math.sin(ba) * R * 0.815
            d.ellipse([bx - 1.6 * ss, by - 1.6 * ss, bx + 1.6 * ss, by + 1.6 * ss], fill=255)
    for k in range(6):                                     # the scales star: two interlocked triangles inside the middle ring
        a0 = math.radians(60 * k - 90)
        a1 = math.radians(60 * (k + 2) - 90)
        d.line([(c + math.cos(a0) * R * 0.62, c + math.sin(a0) * R * 0.62), (c + math.cos(a1) * R * 0.62, c + math.sin(a1) * R * 0.62)], fill=255 if k % 2 == 0 else 200, width=int(2.0 * ss))
    for k in range(12):                                    # spokes from the middle disc to the star, and ticks on the middle ring
        a = math.radians(30 * k)
        r0, r1 = 0.12, 0.38 if k % 2 == 0 else 0.26
        d.line([(c + math.cos(a) * R * r0, c + math.sin(a) * R * r0), (c + math.cos(a) * R * r1, c + math.sin(a) * R * r1)], fill=230, width=int(1.5 * ss))
        d.line([(c + math.cos(a) * R * 0.70, c + math.sin(a) * R * 0.70), (c + math.cos(a) * R * 0.745, c + math.sin(a) * R * 0.745)], fill=255, width=int(2.0 * ss))
    # the scales of law in the middle: a beam, a pillar and two pans
    d.line([(c - R * 0.22, c - R * 0.17), (c + R * 0.22, c - R * 0.17)], fill=255, width=int(2.4 * ss))
    d.line([(c, c - R * 0.17), (c, c + R * 0.20)], fill=255, width=int(2.4 * ss))
    for sx in (-1, 1):
        px = c + sx * R * 0.22
        d.line([(px, c - R * 0.17), (px - R * 0.07, c - R * 0.03), (px + R * 0.07, c - R * 0.03), (px, c - R * 0.17)], fill=255, width=int(1.6 * ss))
        d.arc([px - R * 0.075, c - R * 0.06, px + R * 0.075, c + R * 0.02], 0, 180, fill=255, width=int(2.0 * ss))
    d.line([(c - R * 0.09, c + R * 0.20), (c + R * 0.09, c + R * 0.20)], fill=255, width=int(2.4 * ss))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    a = glow_add(big, 2.0, 0.5) * smooth(1.0, 0.975, r)
    save_white(a, "jibril_glyph")


# ---------------------------------------------------------------------------------------------------- floor seal (faint, huge)
def seal(size=512, ss=3):
    rng = np.random.default_rng(4040)
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)

    def circle(rad, w, fill=255):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss)))

    circle(0.985, 3.4)
    circle(0.955, 1.4, 180)
    circle(0.80, 2.0)
    circle(0.775, 1.0, 140)
    circle(0.52, 2.0)
    circle(0.20, 2.0)
    n = 72                                                 # a band of rune cells between the outer ring and the second one
    for k in range(n):
        a = 2 * math.pi * (k + 0.5) / n
        draw_rune(d, rune_strokes(rng), c + math.cos(a) * R * 0.875, c + math.sin(a) * R * 0.875, R * 0.032, R * 0.085, a + math.pi / 2, 1.8 * ss)
    for k in range(12):                                    # a twelve-pointed star {12/5} between the second ring and the third
        a0 = math.radians(30 * k)
        a1 = math.radians(30 * (k + 5))
        d.line([(c + math.cos(a0) * R * 0.77, c + math.sin(a0) * R * 0.77), (c + math.cos(a1) * R * 0.77, c + math.sin(a1) * R * 0.77)], fill=210, width=int(1.5 * ss))
    for k in range(24):                                    # spokes and dots in the inner field
        a = math.radians(15 * k)
        r0, r1 = 0.22, 0.50
        d.line([(c + math.cos(a) * R * r0, c + math.sin(a) * R * r0), (c + math.cos(a) * R * r1, c + math.sin(a) * R * r1)], fill=150 if k % 2 else 230, width=int(1.2 * ss))
        bx, by = c + math.cos(a) * R * 0.52, c + math.sin(a) * R * 0.52
        d.ellipse([bx - 2.4 * ss, by - 2.4 * ss, bx + 2.4 * ss, by + 2.4 * ss], fill=255)
    for k in range(6):                                     # a triangle pair at the middle
        a0 = math.radians(60 * k - 90)
        a1 = math.radians(60 * (k + 2) - 90)
        d.line([(c + math.cos(a0) * R * 0.19, c + math.sin(a0) * R * 0.19), (c + math.cos(a1) * R * 0.19, c + math.sin(a1) * R * 0.19)], fill=255, width=int(1.6 * ss))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    a = glow_add(big, 1.6, 0.55) * smooth(1.0, 0.975, r)
    save_white(a, "jibril_seal")


# ---------------------------------------------------------------------------------------------------- halo
def halo(size=256, ss=4):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im, d = new_mask(S, S)
    for k in range(24):                                    # beads between the two rings
        a = math.radians(15 * k)
        bx, by = c + math.cos(a) * R * 0.835, c + math.sin(a) * R * 0.835
        e = (2.6 if k % 2 == 0 else 1.7) * ss
        d.ellipse([bx - e, by - e, bx + e, by + e], fill=255)
    for k in range(12):                                    # short spikes outside
        a = math.radians(30 * k + 15)
        d.line([(c + math.cos(a) * R * 0.955, c + math.sin(a) * R * 0.955), (c + math.cos(a) * R * 0.995, c + math.sin(a) * R * 0.995)], fill=255, width=int(2.2 * ss))
    big = mask_of(im.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    a = np.exp(-(((r - 0.915) / 0.012) ** 2)) + np.exp(-(((r - 0.915) / 0.050) ** 2)) * 0.30
    a = a + np.exp(-(((r - 0.76) / 0.008) ** 2)) * 0.85 + np.exp(-(((r - 0.76) / 0.034) ** 2)) * 0.20
    a = a + big * 0.9
    a *= smooth(1.0, 0.97, r)
    save_white(a, "jibril_halo")


# ---------------------------------------------------------------------------------------------------- holy wave front
def wave(size=256):
    rng = np.random.default_rng(909)
    x, y, r, th = disc_coords(size)
    lead = np.exp(-(((r - 0.955) / 0.0095) ** 2))
    lead_halo = np.exp(-(((r - 0.955) / 0.040) ** 2)) * 0.50
    spokes = np.zeros_like(r)
    for k in range(60):                                    # fine light spokes that trail inside the front, each with its own length
        ak = 2 * math.pi * k / 60 + rng.uniform(-0.03, 0.03)
        ln = rng.uniform(0.10, 0.42)
        dth = np.angle(np.exp(1j * (th - ak)))
        width = 0.010 + 0.014 * (0.955 - r)
        prof = np.exp(-((dth / np.maximum(width, 0.004)) ** 2 * 0.03))
        prof = np.exp(-((dth * r / 0.012) ** 2))
        trail = np.clip((0.955 - r) / ln, 0, 1)
        spokes = spokes + prof * np.where(r < 0.955, (1 - trail) ** 1.6, 0.0) * rng.uniform(0.45, 0.85)
    band = np.where(r < 0.955, np.exp(-(0.955 - r) / 0.22) * 0.12, 0.0)
    a = (lead + lead_halo + spokes * 0.8 + band) * (0.9 + 0.1 * np.cos(th * 10))
    a *= smooth(1.0, 0.975, r) * smooth(0.0, 0.10, r)
    save_white(a, "jibril_wave")


# ---------------------------------------------------------------------------------------------------- rays
def rays(size=256):
    rng = np.random.default_rng(1313)
    x, y, r, th = disc_coords(size)
    a = np.zeros_like(r)
    for k in range(32):
        ak = 2 * math.pi * k / 32 + rng.uniform(-0.05, 0.05)
        wid = rng.uniform(0.035, 0.085)
        ln = rng.uniform(0.55, 1.0)
        dth = np.angle(np.exp(1j * (th - ak)))
        a = a + np.exp(-((dth / wid) ** 2)) * np.clip(1 - r / ln, 0, 1) ** 1.3 * rng.uniform(0.45, 1.0)
    a = a * smooth(0.02, 0.18, r)                          # open at the very middle
    a += np.exp(-((r / 0.16) ** 2)) * 0.5
    a *= smooth(1.0, 0.94, r)
    save_white(a, "jibril_rays")


# ---------------------------------------------------------------------------------------------------- pillar
def pillar(w=64, h=256):
    rng = np.random.default_rng(555)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    core = np.exp(-((x / 0.14) ** 2))
    mid = np.exp(-((x / 0.38) ** 2)) * 0.55
    halo_ = np.exp(-((x / 0.80) ** 2)) * 0.28
    a = core + mid + halo_
    streak = np.zeros_like(a)
    for _ in range(9):                                     # rising streaks: each a bright head with a tail below it, periodic in V
        xk = rng.uniform(-0.62, 0.62)
        head = rng.uniform(0, 1)
        length = rng.uniform(0.22, 0.5)
        wid = rng.uniform(0.04, 0.09)
        pv = ((v - head + 0.5) % 1.0) - 0.5                # > 0 = below the head (V grows downwards, the beam scrolls up)
        tail = np.where((pv >= 0) & (pv < length), np.clip(1 - pv / length, 0, 1) ** 1.5, 0.0)
        streak = streak + np.exp(-(((x - xk) / wid) ** 2)) * (tail * 0.85 + np.exp(-((pv / 0.014) ** 2)) * 0.8) * rng.uniform(0.5, 1.0)
    a = a * (0.80 + 0.20 * np.cos(v * math.tau * 4)) + streak * 0.75
    a *= smooth(1.0, 0.82, np.abs(x))
    save_white(a, "jibril_pillar")


# ---------------------------------------------------------------------------------------------------- rune bars
def bars(w=64, h=256, ss=6):
    rng = np.random.default_rng(2222)
    W, H = w * ss, h * ss
    cx = W / 2
    im, d = new_mask(W, H)
    cells = 4
    ch = H / cells
    d.rectangle([cx - W * 0.055, 0, cx + W * 0.055, H], fill=255)                         # the rod
    d.line([(cx - W * 0.16, 0), (cx - W * 0.16, H)], fill=120, width=int(1.4 * ss))        # thin side rails
    d.line([(cx + W * 0.16, 0), (cx + W * 0.16, H)], fill=120, width=int(1.4 * ss))
    for k in range(cells):
        y0 = k * ch
        d.line([(cx - W * 0.30, y0 + 1), (cx + W * 0.30, y0 + 1)], fill=230, width=int(2.2 * ss))     # a collar at every cell boundary
        d.polygon([(cx - W * 0.08, y0 + 1), (cx, y0 + ch * 0.07), (cx + W * 0.08, y0 + 1)], fill=255)
        draw_rune(d, rune_strokes(rng), cx, y0 + ch * 0.55, W * 0.30, ch * 0.46, 0.0, 2.6 * ss)
        d.ellipse([cx - 3 * ss, y0 + ch * 0.86 - 3 * ss, cx + 3 * ss, y0 + ch * 0.86 + 3 * ss], fill=255)
    m = mask_of(im.resize((w, h), Image.LANCZOS))
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    soft = np.exp(-((x / 0.42) ** 2)) * 0.30
    a = glow_add(m, 1.6, 0.55) + soft
    a *= smooth(1.0, 0.86, np.abs(x))
    save_white(a, "jibril_bars")


# ---------------------------------------------------------------------------------------------------- curtain
def curtain(w=64, h=256):
    rng = np.random.default_rng(6060)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / (h - 1)                                       # 0 at the top, 1 at the foot
    foot = v ** 1.7
    fold = 0.55 + 0.45 * np.cos(x * math.tau * 1.5 + 0.8)
    a = foot * (0.5 + 0.5 * fold) * 0.9
    for _ in range(7):                                     # fine rays up the curtain
        xk = rng.uniform(-0.8, 0.8)
        wid = rng.uniform(0.02, 0.05)
        top = rng.uniform(0.1, 0.5)
        a = a + np.exp(-(((x - xk) / wid) ** 2)) * np.clip((v - top) / (1 - top), 0, 1) ** 0.9 * rng.uniform(0.3, 0.7)
    a += np.exp(-(((v - 0.985) / 0.02) ** 2)) * 0.6        # a bright line at the foot
    a *= smooth(1.0, 0.70, np.abs(x)) * smooth(0.0, 0.10, v)
    save_white(a, "jibril_curtain")


# ---------------------------------------------------------------------------------------------------- reflect beam
def beam(w=64, h=256):
    rng = np.random.default_rng(8080)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    core = np.exp(-((x / 0.11) ** 2))
    mid = np.exp(-((x / 0.34) ** 2)) * 0.50
    halo_ = np.exp(-((x / 0.75) ** 2)) * 0.24
    a = core + mid + halo_
    pulse = np.zeros_like(a)
    for k in range(4):                                     # chevron pulses that cross the beam (they point down the V axis), periodic in V
        v0 = (k + rng.uniform(-0.05, 0.05)) / 4.0
        pv = ((v - v0 - 0.55 * np.abs(x) + 0.5) % 1.0) - 0.5
        pulse = pulse + np.exp(-((pv / 0.022) ** 2)) * np.exp(-((x / 0.72) ** 2)) * rng.uniform(0.55, 0.9)
    a = a * (0.74 + 0.26 * np.cos(v * math.tau * 2)) + pulse
    a *= smooth(1.0, 0.86, np.abs(x))
    save_white(a, "jibril_beam")


# ---------------------------------------------------------------------------------------------------- chain
def chain(w=64, h=256, ss=6):
    W, H = w * ss, h * ss
    pitch = H / 8.0
    cx = W / 2
    link, ld = new_mask(W, H)
    inner, idr = new_mask(W, H)
    for k in range(8):
        y0 = k * pitch
        if k % 2 == 0:                                     # a link seen face on: an upright oval
            ld.rounded_rectangle([cx - W * 0.17, y0 - pitch * 0.18, cx + W * 0.17, y0 + pitch * 1.18], radius=W * 0.17, outline=255, width=int(4.4 * ss))
            idr.rounded_rectangle([cx - W * 0.17, y0 - pitch * 0.18, cx + W * 0.17, y0 + pitch * 1.18], radius=W * 0.17, outline=255, width=int(1.6 * ss))
        else:                                              # a link seen edge on: a thin bar
            ld.rounded_rectangle([cx - W * 0.06, y0 - pitch * 0.12, cx + W * 0.06, y0 + pitch * 1.12], radius=W * 0.05, fill=255)
            idr.rounded_rectangle([cx - W * 0.02, y0 - pitch * 0.06, cx + W * 0.02, y0 + pitch * 1.06], radius=W * 0.02, fill=255)
    lk = mask_of(link.resize((w, h), Image.LANCZOS))
    hi = mask_of(inner.resize((w, h), Image.LANCZOS))
    alpha = np.clip(glow_add(lk, 1.4, 0.55), 0, 1)
    rgb = np.zeros((h, w, 3), np.float32)
    rgb[...] = col(GOLD_DK) * (1 - lk[..., None]) + col(GOLD) * lk[..., None]
    h3 = np.clip(hi * 1.2, 0, 1)[..., None]
    rgb = rgb * (1 - h3) + col(IVORY) * h3
    save(rgb, alpha, "jibril_chain")


def chainring(size=256, ss=4):
    S = size * ss
    c = S / 2
    R = c * 0.80
    n = 20
    link, ld = new_mask(S, S)
    inner, idr = new_mask(S, S)
    for k in range(n):
        a = 2 * math.pi * k / n
        px, py = c + math.cos(a) * R, c + math.sin(a) * R
        L = math.pi * 2 * R / n * 0.78                     # a link is a bit longer than the pitch so they overlap
        if k % 2 == 0:
            # an oval link lying along the ring
            hw = L * 0.34
            poly = []
            for t in range(24):
                u = 2 * math.pi * t / 24
                lx, ly = math.cos(u) * L * 0.62, math.sin(u) * hw
                poly.append((px + lx * (-math.sin(a)) + ly * math.cos(a), py + lx * math.cos(a) + ly * math.sin(a)))
            ld.line(poly + [poly[0]], fill=255, width=int(3.6 * ss), joint="curve")
            idr.line(poly + [poly[0]], fill=255, width=int(1.3 * ss), joint="curve")
        else:
            hw = L * 0.10
            tx, ty = -math.sin(a), math.cos(a)
            d_ = [(px - tx * L * 0.58, py - ty * L * 0.58), (px + tx * L * 0.58, py + ty * L * 0.58)]
            ld.line(d_, fill=255, width=int(5.0 * ss))
            idr.line(d_, fill=255, width=int(1.6 * ss))
    lk = mask_of(link.resize((size, size), Image.LANCZOS))
    hi = mask_of(inner.resize((size, size), Image.LANCZOS))
    x, y, r, th = disc_coords(size)
    alpha = glow_add(lk, 1.6, 0.6)
    alpha = np.clip(alpha + np.exp(-(((r - 0.80) / 0.09) ** 2)) * 0.10, 0, 1) * smooth(1.0, 0.97, r)
    rgb = col(GOLD_DK) * (1 - lk[..., None]) + col(GOLD) * lk[..., None]
    h3 = np.clip(hi * 1.2, 0, 1)[..., None]
    rgb = rgb * (1 - h3) + col(IVORY) * h3
    save(rgb, alpha, "jibril_chainring")


# ---------------------------------------------------------------------------------------------------- feather shapes
def feather_mask(base, tip, wmax, bend, size, ss, W, H):
    """Mask of one feather from `base` to `tip` (pixel coordinates in the W x H picture, before supersampling): a leaf with a round shoulder, a rachis line and a curve of `bend` (fraction of the length)."""
    bx, by = base
    tx, ty = tip
    L = math.hypot(tx - bx, ty - by)
    ux, uy = (tx - bx) / L, (ty - by) / L
    nx, ny = -uy, ux
    left, right, centre = [], [], []
    N = 18
    for i in range(N + 1):
        s = i / N
        off = bend * L * math.sin(s * math.pi) * 0.5
        wd = wmax * (math.sin(math.pi * min(1.0, s ** 0.62 * 0.98 + 0.015)) ** 0.85) * (1 - 0.30 * s)
        px, py = bx + ux * L * s + nx * off, by + uy * L * s + ny * off
        left.append(((px + nx * wd) * ss, (py + ny * wd) * ss))
        right.append(((px - nx * wd) * ss, (py - ny * wd) * ss))
        centre.append((px * ss, py * ss))
    return left, right, centre


def paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, tint_base, tint_tip, rim, shade=1.0, shade_strength=1.0):
    """Paints one feather over the accumulated picture (painter's order): vane fill, soft rim, rachis and barbs. All arrays are float, W x H."""
    fill_im, fd = new_mask(W * ss, H * ss)
    fd.polygon(left + right[::-1], fill=255)
    fill = mask_of(fill_im.resize((W, H), Image.LANCZOS))
    edge_im, ed = new_mask(W * ss, H * ss)
    ed.line(left + right[::-1] + [left[0]], fill=255, width=max(1, int(1.1 * ss)), joint="curve")
    edge = mask_of(edge_im.resize((W, H), Image.LANCZOS))
    line_im, ld = new_mask(W * ss, H * ss)
    ld.line(centre, fill=255, width=max(1, int(1.0 * ss)))
    for i in range(1, len(centre) - 2):                    # barbs: short lines from the rachis out to the edges, bent towards the tip
        ld.line([centre[i], left[i + 2]], fill=110, width=max(1, int(0.6 * ss)))
        ld.line([centre[i], right[i + 2]], fill=110, width=max(1, int(0.6 * ss)))
    lines = mask_of(line_im.resize((W, H), Image.LANCZOS))
    xx, yy = grid(W, H)
    cx0, cy0 = centre[0][0] / ss, centre[0][1] / ss
    cx1, cy1 = centre[-1][0] / ss, centre[-1][1] / ss
    dx, dy = cx1 - cx0, cy1 - cy0
    L2 = dx * dx + dy * dy
    s = np.clip(((xx - cx0) * dx + (yy - cy0) * dy) / L2, 0, 1)
    body = lerp3(tint_base, tint_tip, smooth(0.0, 0.85, s))
    body = body * shade
    rgb = body * (1 - edge[..., None] * 0.55) + col(rim) * edge[..., None] * 0.55
    rgb = rgb * (1 - lines[..., None] * 0.55) + col(SKY_DK) * lines[..., None] * 0.55
    al = fill * (0.74 + 0.20 * smooth(0.0, 0.6, s)) * (0.90 + 0.10 * shade)
    al = np.clip(al + edge * 0.4 * fill, 0, 1)
    # the feather lies on the ones below it and shades them: a soft cool shadow along its edge (this is what makes the layers of a wing readable)
    shadow = np.clip(blur(fill, 2.2 * (W / 256.0) ** 0.5) * 0.55 - fill * 0.55, 0, 1) * np.clip(acc_a, 0, 1) * shade_strength
    acc_rgb[:] = acc_rgb * (1 - shadow[..., None]) + col(SKY_DK) * 0.55 * shadow[..., None]
    acc_rgb[:] = rgb * al[..., None] + acc_rgb * (1 - al[..., None])
    acc_a[:] = al + acc_a * (1 - al)


def feather(size=64, ss=6):
    W = H = size
    acc_rgb = np.zeros((H, W, 3), np.float32)
    acc_a = np.zeros((H, W), np.float32)
    left, right, centre = feather_mask((size * 0.46, size * 0.97), (size * 0.58, size * 0.03), size * 0.19, -0.20, size, ss, W, H)
    paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, GOLD, WHITE, GOLD_DK)
    acc_a = glow_add(acc_a, 1.6, 0.45)
    acc_rgb = np.where(acc_a[..., None] > 0, np.where(acc_rgb.sum(axis=2, keepdims=True) > 0.05, acc_rgb, col(IVORY)), 0)
    save(acc_rgb, acc_a, "jibril_feather")


def wing(w=512, h=256, ss=3):
    """One wing seen from behind: a leading-edge arm along the top from the root (left) over the wrist to the hand, long primaries fanning out to the right and down, shorter secondaries
    hanging below the arm and a row of coverts over their roots."""
    W, H = w, h
    acc_rgb = np.zeros((H, W, 3), np.float32)
    acc_a = np.zeros((H, W), np.float32)

    def bez(t):
        p0, p1, p2, p3 = (0.02, 0.58), (0.06, 0.08), (0.30, 0.03), (0.86, 0.16)       # in picture fractions
        mt = 1 - t
        return (mt ** 3 * p0[0] + 3 * mt * mt * t * p1[0] + 3 * mt * t * t * p2[0] + t ** 3 * p3[0],
                mt ** 3 * p0[1] + 3 * mt * mt * t * p1[1] + 3 * mt * t * t * p2[1] + t ** 3 * p3[1])

    rng = np.random.default_rng(31337)
    # the long feathers, drawn from the inner one (hanging) to the outer one (the longest, pointing out) so the outer ones lie on top
    nl = 14
    for i in range(nl):
        f = i / (nl - 1)
        bx, by = bez(0.12 + 0.84 * f)
        tipx = 0.07 + 0.925 * f ** 0.8
        tipy = 0.985 - 0.56 * f ** 1.15
        base = (bx * W, by * H + 3)
        tip = (tipx * W, tipy * H)
        wm = (13.0 + 7.0 * math.sin(math.pi * min(1.0, f * 0.85 + 0.15))) * (W / 512.0) * 1.6
        left, right, centre = feather_mask(base, tip, wm, -0.10 + 0.14 * f + rng.uniform(-0.02, 0.02), 1, ss, W, H)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, GOLD, WHITE, GOLD_DK, 0.88 + 0.12 * f)
    # a second, shorter row between the long ones and the arm: they fill the gaps and give the wing its depth
    ns = 13
    for i in range(ns):
        f = (i + 0.5) / ns
        bx, by = bez(0.10 + 0.84 * f)
        tipx = bx + 0.04 + 0.16 * f
        tipy = by + 0.50 - 0.17 * f
        left, right, centre = feather_mask((bx * W, by * H + 2), (tipx * W, tipy * H), 12.0 * (W / 512.0) * 1.6, -0.08, 1, ss, W, H)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, IVORY, WHITE, GOLD, 0.95)
    # the coverts: small round feathers over the roots of the long ones
    nc = 20
    for i in range(nc):
        f = i / (nc - 1)
        bx, by = bez(0.06 + 0.88 * f)
        tipx = bx + 0.03 + 0.03 * f
        tipy = by + 0.20 - 0.06 * f
        left, right, centre = feather_mask((bx * W, by * H), (tipx * W, tipy * H), 8.5 * (W / 512.0) * 1.6, -0.06, 1, ss, W, H)
        paint_feather(acc_rgb, acc_a, left, right, centre, W, H, ss, IVORY, WHITE, GOLD, 1.0)
    # the leading edge: a thick gold arm with an ivory sheen and a round joint at the wrist
    arm_im, ad = new_mask(W * ss, H * ss)
    pts = [(bez(t / 60)[0] * W * ss, bez(t / 60)[1] * H * ss) for t in range(61)]
    ad.line(pts, fill=255, width=int(7.0 * ss), joint="curve")
    wx, wy = bez(0.70)
    ad.ellipse([wx * W * ss - 7.0 * ss, wy * H * ss - 7.0 * ss, wx * W * ss + 7.0 * ss, wy * H * ss + 7.0 * ss], fill=255)
    sheen_im, sd = new_mask(W * ss, H * ss)
    sd.line([(x, y - 1.6 * ss) for x, y in pts], fill=255, width=int(2.2 * ss), joint="curve")
    arm = mask_of(arm_im.resize((W, H), Image.LANCZOS))
    sheen = mask_of(sheen_im.resize((W, H), Image.LANCZOS))
    a3 = arm[..., None]
    arm_rgb = col(GOLD) * (1 - sheen[..., None]) + col(IVORY) * sheen[..., None]
    acc_rgb = acc_rgb * (1 - a3) + arm_rgb * a3
    acc_a = np.clip(acc_a + arm * 0.95, 0, 1)
    xx, yy = grid(W, H)
    acc_a *= smooth(0.0, 4.0, np.minimum(xx, W - 1 - xx)) * smooth(0.0, 3.0, np.minimum(yy, H - 1 - yy))
    acc_a = glow_add(acc_a, 3.0, 0.35)
    covered = acc_rgb.sum(axis=2, keepdims=True) > 0.05
    acc_rgb = np.where(covered, acc_rgb, col(GOLD) * 0.6 + col(IVORY) * 0.4)
    save(acc_rgb, acc_a, "jibril_wing")


# ---------------------------------------------------------------------------------------------------- flare / mote
def flare(size=128):
    x, y, r, th = disc_coords(size)
    ax, ay = np.abs(x), np.abs(y)
    core = np.exp(-(r * r) / 0.008)
    halo_ = np.exp(-(r * r) / 0.07) * 0.42
    hor = np.exp(-((ay / 0.018) ** 2)) * np.clip(1 - ax, 0, 1) ** 1.4
    ver = np.exp(-((ax / 0.018) ** 2)) * np.clip(1 - ay, 0, 1) ** 1.4
    u, v = (x + y) * 0.7071, (x - y) * 0.7071
    d1 = np.exp(-((np.abs(v) / 0.020) ** 2)) * np.clip(1 - np.abs(u) * 2.4, 0, 1) ** 2.0 * 0.40
    d2 = np.exp(-((np.abs(u) / 0.020) ** 2)) * np.clip(1 - np.abs(v) * 2.4, 0, 1) ** 2.0 * 0.40
    lens = np.exp(-(((r - 0.36) / 0.012) ** 2)) * 0.28
    a = (core + halo_ + hor + ver + d1 + d2 + lens) * smooth(1.0, 0.92, r)
    save_white(a, "jibril_flare")


def mote(size=32):
    x, y, r, th = disc_coords(size)
    ax, ay = np.abs(x), np.abs(y)
    arms = (np.exp(-((ay / 0.07) ** 2)) * np.clip(1 - ax, 0, 1) ** 1.6 + np.exp(-((ax / 0.07) ** 2)) * np.clip(1 - ay, 0, 1) ** 1.6) * 0.8
    core = np.exp(-(r * r) / 0.04)
    a = np.clip(arms + core, 0, 1) * smooth(1.0, 0.85, r)
    save_white(a, "jibril_mote")


if __name__ == "__main__":
    ring()
    glyph()
    seal()
    halo()
    wave()
    rays()
    pillar()
    bars()
    curtain()
    beam()
    chain()
    chainring()
    feather()
    wing()
    flare()
    mote()

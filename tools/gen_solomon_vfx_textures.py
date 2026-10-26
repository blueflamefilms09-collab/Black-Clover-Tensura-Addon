"""Generates the Veldanava "Solomon, Lord of Sorcery" VFX textures (vfx.client.layer.SolomonLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/solomon_*.png

    python tools/gen_solomon_vfx_textures.py             # all
    python tools/gen_solomon_vfx_textures.py ring page   # only these

Identity: a SORCERER'S NIGHT. Deep indigo and violet, antique gold, parchment white, and a small prismatic accent for the six elements (flame orange, tide blue,
wind green, earth amber, storm yellow, frost cyan; light white and dark violet for the pillars). Motifs: grimoires with turning pages, hexagrams and Solomon's seal
rings, magic circles of nested runic rings, chains of linked circles, runes, drifting parchment pages, tall pillars of elemental light, a honeycomb mana shell.
It deliberately shares nothing with Jibril (pearl white / rose gold, feathers, wings, ribbons), Sandalphon (steel, clock rings, black cracks) or Israfil (a cyan maze),
and never touches the Time Magic textures.

Nearly every texture carries its colour (baked) and is drawn with a (near) white vertex colour; the pillar, beam, flare and mote are light neutral shapes the layer
tints per element. Nothing carries colour under a fully transparent pixel. Sizes are powers of two, at most 1024 px, aspect at most 8:1.

    solomon_ring      two thin gold rings with ticks and beads (shock rings, scan rings, mana rings)                   512
    solomon_hexagram  Solomon's seal: a hexagram in a double ring with a rune band and six small circles               1024
    solomon_circle    a magic circle of nested runic rings, a dodecagon of small circles and an octagram               1024
    solomon_page      a glowing parchment page with lines of script and a hexagram head                                256
    solomon_runes     an atlas of eight glowing runes (4 x 2)                                                          512 x 256
    solomon_pillar    a pillar of light with fine streaks and drifting rune marks, seamless in V (tinted per element)  128 x 512
    solomon_rune_wave a wave front with a trail of rune dust behind it                                                 512
    solomon_chain     a chain of linked rings, seamless in V                                                            128 x 512
    solomon_book      a closed grimoire: violet cover, gold corners and a hexagram                                      256
    solomon_beam      a thin stream of light with rune dashes, seamless in V (tinted)                                   128 x 512
    solomon_flare     a six-point star flare with a lens ring (tinted)                                                 256
    solomon_mote      a small four-point sparkle (tinted)                                                               64
    solomon_shell     the mana skin: an egg of honeycomb with a bright rim                                            512 x 1024
    solomon_crack     cracks of light across the shell                                                                  512 x 1024
    solomon_sigils    the eight element sigils (flame, tide, gale, stone, storm, frost, light, dark) in rings (4 x 2)   1024 x 512
    solomon_crater    a scorched crater scar with violet embers (alpha-blended)                                         512
    solomon_veil      a hanging veil of rune columns, bright at the foot                                               128 x 512
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")

SS = 2   # supersampling of the line art

GOLD = (255, 226, 150)       # bright antique gold (the lines)
GOLD_D = (226, 172, 70)      # deep gold (its glow)
PARCH = (252, 240, 212)
LILAC = (214, 192, 255)
VIOLET = (138, 88, 236)
INDIGO = (74, 52, 170)
WHITE = (255, 255, 255)

ELEMENTS = [  # flame, tide, gale, stone, storm, frost, light, dark: (core, glow)
    ((255, 205, 140), (255, 120, 40)),
    ((170, 215, 255), (50, 130, 255)),
    ((190, 255, 205), (80, 220, 120)),
    ((255, 226, 150), (214, 150, 50)),
    ((255, 250, 170), (255, 215, 40)),
    ((215, 250, 255), (90, 215, 250)),
    ((255, 255, 245), (255, 238, 190)),
    ((200, 160, 255), (110, 50, 210)),
]


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


def col(c):
    return np.array(c, np.float32) / 255.0


def blur(a, radius):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


# ---------------------------------------------------------------------------------------------------- line art
class Mask:
    """A line-art mask drawn at SS x the final size; all coordinates are in final pixels."""

    def __init__(self, W, H):
        self.W, self.H = W, H
        self.im = Image.new("L", (W * SS, H * SS), 0)
        self.d = ImageDraw.Draw(self.im)

    def final(self):
        return np.asarray(self.im.resize((self.W, self.H), Image.LANCZOS), dtype=np.float32) / 255.0

    def _w(self, w):
        return max(1, int(round(w * SS)))

    def line(self, pts, w=1.5, v=255, round_ends=True):
        p = [(x * SS, y * SS) for x, y in pts]
        ww = self._w(w)
        self.d.line(p, fill=v, width=ww, joint="curve")
        if round_ends and ww > 2:
            r = ww / 2.0
            for x, y in (p[0], p[-1]):
                self.d.ellipse([x - r, y - r, x + r, y + r], fill=v)

    def circle(self, cx, cy, r, w=1.5, v=255):
        if r <= 0:
            return
        self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=v, width=self._w(w))

    def disc(self, cx, cy, r, v=255):
        self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=v)

    def poly(self, pts, w=1.5, v=255, close=True):
        p = list(pts) + ([pts[0]] if close else [])
        self.line(p, w, v, round_ends=False)

    def fillpoly(self, pts, v=255):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=v)


def ngon(cx, cy, r, n, rot=-math.pi / 2):
    return [(cx + math.cos(rot + 2 * math.pi * i / n) * r, cy + math.sin(rot + 2 * math.pi * i / n) * r) for i in range(n)]


def hexagram(m, cx, cy, r, w=2.0, v=255):
    m.poly(ngon(cx, cy, r, 3, -math.pi / 2), w, v)
    m.poly(ngon(cx, cy, r, 3, math.pi / 2), w, v)


def star_poly(m, cx, cy, r, n, k, w=1.5, rot=-math.pi / 2, v=255):
    pts = ngon(cx, cy, r, n, rot)
    seen = set()
    i = 0
    path = []
    while i not in seen:
        seen.add(i)
        path.append(pts[i])
        i = (i + k) % n
    m.poly(path, w, v)



def _spiral(r0, r1, turns, n=36):
    return [(math.cos(t * turns * 2 * math.pi) * (r0 + (r1 - r0) * t), math.sin(t * turns * 2 * math.pi) * (r0 + (r1 - r0) * t)) for t in np.linspace(0, 1, n)]


DESIGNED = [
    [("l", [(0, -0.5), (0, 0.5)]), ("l", [(-0.4, 0.5), (0, 0.02), (0.4, 0.5)]), ("d", 0, -0.42, 0.05)],                     # algiz: a trident
    [("l", [(0, 0.5), (0.38, 0.12), (0, -0.2), (-0.38, 0.12), (0, 0.5)]), ("l", [(0, -0.2), (0.3, -0.5)]), ("l", [(0, -0.2), (-0.3, -0.5)])],   # othala
    [("l", [(0, -0.5), (0, 0.5)]), ("l", [(0, 0.34), (0.4, 0.02), (0, -0.28)]), ("d", 0.4, 0.38, 0.05)],                   # thurisaz
    [("l", [(-0.4, 0.45), (0.4, -0.45), (0.4, 0.45), (-0.4, -0.45), (-0.4, 0.45)]), ("d", 0, 0, 0.05)],                    # dagaz
    [("c", 0, 0.12, 0.3), ("d", 0, 0.12, 0.07), ("l", [(0, -0.18), (0, -0.5)]), ("l", [(-0.22, -0.38), (0.22, -0.38)])],    # a sun on a stem
    [("l", [(-0.38, -0.5), (-0.38, 0.5), (0, 0.08), (0.38, 0.5), (0.38, -0.5)]), ("l", [(-0.38, -0.12), (0.38, -0.12)])],   # mannaz
    [("l", _spiral(0.05, 0.4, 1.4)), ("d", 0, 0, 0.05), ("l", [(0.4, 0), (0.46, -0.5)])],                                  # a spiral with a tail
    [("l", [(0, -0.5), (0, 0.5)]), ("l", [(-0.4, -0.2), (0.4, 0.2)]), ("l", [(-0.4, 0.2), (0.4, -0.2)]), ("c", 0, 0, 0.14)],  # an asterisk with a ring
]

def make_rune(rng):
    """One rune as a list of strokes in unit coordinates (x, y in -0.5..0.5, y up): a stem with branches, sometimes a ring, a bar or a dot."""
    if rng.random() < 0.3:
        return DESIGNED[rng.randrange(len(DESIGNED))]
    strokes = []
    bend = rng.uniform(-0.08, 0.08) if rng.random() < 0.4 else 0.0
    strokes.append(("l", [(0, -0.5), (bend, 0.0), (0, 0.5)]))
    for _ in range(rng.choice([1, 2, 2, 3])):
        y = rng.uniform(-0.38, 0.38)
        sgn = rng.choice([-1, 1])
        ex = sgn * rng.uniform(0.2, 0.46)
        ey = y + rng.uniform(-0.34, 0.34)
        if rng.random() < 0.35:
            strokes.append(("l", [(0, y), (ex * 0.6, y + (ey - y) * 0.2), (ex, ey), (ex * 0.55, ey + rng.uniform(-0.2, 0.2))]))
        else:
            strokes.append(("l", [(0, y), (ex, ey)]))
    if rng.random() < 0.3:
        strokes.append(("c", rng.uniform(-0.2, 0.2), rng.uniform(-0.3, 0.3), rng.uniform(0.08, 0.15)))
    if rng.random() < 0.3:
        y = rng.uniform(-0.3, 0.3)
        strokes.append(("l", [(-0.3, y), (0.3, y + rng.uniform(-0.1, 0.1))]))
    if rng.random() < 0.4:
        strokes.append(("d", rng.choice([-1, 1]) * 0.28, rng.choice([-1, 1]) * 0.42, 0.05))
    return strokes


def draw_rune(m, rune, cx, cy, size, rot=0.0, w=1.5, v=255):
    c, s = math.cos(rot), math.sin(rot)

    def tf(x, y):
        y = -y
        return cx + (x * c - y * s) * size, cy + (x * s + y * c) * size

    for st in rune:
        if st[0] == "l":
            m.line([tf(x, y) for x, y in st[1]], w, v)
        elif st[0] == "c":
            x, y = tf(st[1], st[2])
            m.circle(x, y, st[3] * size, w, v)
        else:
            x, y = tf(st[1], st[2])
            m.disc(x, y, max(w * 0.8, st[3] * size), v)


def rune_ring(m, cx, cy, r, count, size, rng, w=1.5, v=255, tangent=True, skip=0.0):
    """Runes round a circle of radius r, standing on it (their stem runs radially) or lying along it (tangent)."""
    for i in range(count):
        if skip and rng.random() < skip:
            continue
        a = 2 * math.pi * i / count + rng.uniform(-0.01, 0.01)
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        rot = a + (0.0 if not tangent else 0.0)
        # unit rune has its stem along y (up); rotate so the stem points along the radius
        draw_rune(m, make_rune(rng), x, y, size, rot + math.pi / 2, w, v)


def compose(name, W, H, layers, extra_alpha=None):
    """layers: (Mask, core colour, glow colour, glow spec ((radius, gain), ...)). Everything is added (premultiplied), the colour is the weighted mix."""
    P = np.zeros((H, W, 3), np.float32)
    A = np.zeros((H, W), np.float32)
    for m, core, glow, spec in layers:
        s = np.clip(m.final(), 0, 1)
        g = np.zeros_like(s)
        for r, k in spec:
            g = g + blur(s, r) * k
        g = np.clip(g, 0, 1) * (1 - s * 0.7)
        P += col(core) * s[..., None] + col(glow) * g[..., None]
        A += s + g
    if extra_alpha is not None:
        A = A * extra_alpha
        P = P * extra_alpha[..., None]
    Ac = np.clip(A, 0, 1)
    rgb = P / np.maximum(Ac, 1e-4)[..., None]
    save(rgb, Ac, name)
    return rgb, Ac


def GLOWS(s=1.0):
    return ((1.6 * s, 0.85), (5.0 * s, 0.40))


# ---------------------------------------------------------------------------------------------------- textures
def tex_ring():
    W = 512
    c = W / 2
    g, v = Mask(W, W), Mask(W, W)
    R = 0.965 * c
    g.circle(c, c, R, 3.4)
    g.circle(c, c, R * 0.945, 1.3)
    v.circle(c, c, R * 0.90, 1.6)
    for i in range(120):
        a = 2 * math.pi * i / 120
        r0, r1 = R * 0.915, R * (0.935 if i % 5 else 0.945)
        v.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], 1.2)
    for i in range(12):
        a = 2 * math.pi * i / 12
        g.disc(c + math.cos(a) * R * 0.972, c + math.sin(a) * R * 0.972, 3.2)
        g.fillpoly(ngon(c + math.cos(a + math.pi / 12) * R * 0.972, c + math.sin(a + math.pi / 12) * R * 0.972, 4.5, 4, a + math.pi / 12))
    compose("solomon_ring", W, W, [(v, LILAC, VIOLET, GLOWS()), (g, GOLD, GOLD_D, GLOWS())])


def tex_hexagram():
    W = 1024
    c = W / 2
    rng = random.Random(72)
    gold, vio = Mask(W, W), Mask(W, W)
    R = 0.985 * c
    gold.circle(c, c, R, 5)
    gold.circle(c, c, R * 0.968, 2)
    gold.circle(c, c, R * 0.875, 3)
    gold.circle(c, c, R * 0.862, 1.5)
    # the rune band between the two rings
    rune_ring(vio, c, c, R * 0.918, 60, R * 0.036, rng, 1.8)
    for i in range(60):
        a = 2 * math.pi * (i + 0.5) / 60
        vio.disc(c + math.cos(a) * R * 0.918, c + math.sin(a) * R * 0.918, 1.6)
    # the hexagram: a double line, a small circle on each point
    rh = R * 0.80
    hexagram(gold, c, c, rh, 5.5)
    hexagram(vio, c, c, rh * 0.945, 2.2)
    gold.circle(c, c, rh * 0.5, 3.5)
    vio.circle(c, c, rh * 0.47, 1.4)
    for i in range(6):
        a = -math.pi / 2 + math.pi / 3 * i
        x, y = c + math.cos(a) * rh, c + math.sin(a) * rh
        gold.circle(x, y, R * 0.065, 3.5)
        vio.circle(x, y, R * 0.045, 1.6)
        gold.disc(x, y, R * 0.017)
    # the inner seal: a small hexagram, six spokes and a core
    hexagram(gold, c, c, rh * 0.36, 2.6)
    gold.circle(c, c, rh * 0.13, 2.6)
    gold.disc(c, c, rh * 0.04)
    for i in range(6):
        a = -math.pi / 2 + math.pi / 3 * i + math.pi / 6
        vio.line([(c + math.cos(a) * rh * 0.18, c + math.sin(a) * rh * 0.18), (c + math.cos(a) * rh * 0.46, c + math.sin(a) * rh * 0.46)], 1.6)
        draw_rune(vio, make_rune(rng), c + math.cos(a) * rh * 0.62, c + math.sin(a) * rh * 0.62, R * 0.055, a + math.pi / 2, 1.8)
    compose("solomon_hexagram", W, W, [(vio, LILAC, VIOLET, GLOWS(1.6)), (gold, GOLD, GOLD_D, GLOWS(1.6))])


def tex_circle():
    W = 1024
    c = W / 2
    rng = random.Random(7)
    gold, vio = Mask(W, W), Mask(W, W)
    R = 0.985 * c
    for r, w in ((1.0, 5), (0.962, 2), (0.80, 3.5), (0.79, 1.4), (0.665, 3.5), (0.61, 2), (0.19, 3)):
        gold.circle(c, c, R * r, w)
    # outer rune band (fine) and the middle band (large)
    rune_ring(vio, c, c, R * 0.88, 72, R * 0.036, rng, 1.8)
    rune_ring(vio, c, c, R * 0.72, 40, R * 0.046, rng, 2.2)
    gold.circle(c, c, R * 0.945, 1.2)
    # 12 spokes between the bands and 12 small circles on the dodecagon
    for i in range(12):
        a = 2 * math.pi * i / 12
        gold.line([(c + math.cos(a) * R * 0.80, c + math.sin(a) * R * 0.80), (c + math.cos(a) * R * 0.962, c + math.sin(a) * R * 0.962)], 2.2)
        gold.line([(c + math.cos(a) * R * 0.61, c + math.sin(a) * R * 0.61), (c + math.cos(a) * R * 0.665, c + math.sin(a) * R * 0.665)], 2.2)
        x, y = c + math.cos(a) * R * 0.50, c + math.sin(a) * R * 0.50
        gold.circle(x, y, R * 0.048, 3)
        gold.disc(x, y, R * 0.013)
    gold.poly(ngon(c, c, R * 0.50, 12), 1.8)
    gold.poly(ngon(c, c, R * 0.405, 12), 1.4)
    star_poly(gold, c, c, R * 0.405, 12, 5, 1.8)
    star_poly(vio, c, c, R * 0.30, 8, 3, 2.0)
    star_poly(vio, c, c, R * 0.30, 8, 1, 1.4)
    for i in range(8):
        a = 2 * math.pi * (i + 0.5) / 8
        draw_rune(vio, make_rune(rng), c + math.cos(a) * R * 0.255, c + math.sin(a) * R * 0.255, R * 0.04, a + math.pi / 2, 1.6)
    gold.disc(c, c, R * 0.045)
    compose("solomon_circle", W, W, [(vio, LILAC, VIOLET, GLOWS(1.6)), (gold, GOLD, GOLD_D, GLOWS(1.6))])


def tex_page():
    W = 256
    rng = random.Random(33)
    px0, px1, py0, py1 = 52, 204, 22, 236
    body = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(body)
    # the sheet, a curled lower right corner
    pts = [(px0, py0), (px1, py0), (px1, py1 - 34), (px1 - 34, py1), (px0, py1)]
    d.polygon(pts, fill=255)
    sheet = np.asarray(body, dtype=np.float32) / 255.0
    sheet = blur(sheet, 0.6)
    edge = np.clip(sheet - blur(sheet, 5.0) * 0.9, 0, 1) * 0.0
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    fibre = 0.5 + 0.5 * np.sin(yy * 0.35 + np.sin(xx * 0.05) * 2.0) * 0.4
    fill = col(PARCH)[None, None, :] * (0.62 + 0.14 * fibre[..., None])
    # the lines of script: runs of tiny strokes that glow indigo-violet on the sheet (darker = quieter in an additive blend)
    ink, glow = Mask(W, W), Mask(W, W)
    hexagram(glow, W / 2, py0 + 34, 17, 1.6)
    glow.circle(W / 2, py0 + 34, 22, 1.4)
    for row in range(11):
        y = py0 + 74 + row * 14
        x = px0 + 14
        while x < px1 - 18:
            n = rng.choice([2, 3, 4, 5])
            wd = n * 5.0
            if x + wd > px1 - 14:
                break
            for k in range(n):
                draw_rune(ink, make_rune(rng), x + k * 5.0, y, 6.0, 0, 1.0)
            x += wd + rng.choice([6, 9])
    inkm = np.clip(ink.final(), 0, 1)
    glm = np.clip(glow.final(), 0, 1)
    rgb = fill * (1 - inkm[..., None] * 0.75) + col(GOLD)[None, None, :] * glm[..., None] * 0.9
    # a gold border, and a corner curl shading
    bord = Mask(W, W)
    bord.poly(pts, 1.6)
    bm = bord.final()
    rgb = rgb * (1 - bm[..., None]) + col(GOLD) * bm[..., None]
    curl = np.clip(((xx - (px1 - 34)) + (yy - (py1 - 34))) / 34.0, 0, 1)
    rgb = rgb * (1 - 0.35 * curl[..., None]) + col(LILAC) * 0.15 * curl[..., None]
    halo = blur(sheet, 6.0)
    a = np.clip(sheet * 0.85 + halo * 0.5, 0, 1)
    rgb = (rgb * (sheet * 0.85)[..., None] + col(GOLD_D) * (halo * 0.5 * (1 - sheet))[..., None]) / np.maximum(a, 1e-4)[..., None]
    save(rgb, a, "solomon_page")


def tex_runes():
    W, H = 512, 256
    rng = random.Random(1234)
    gold, vio = Mask(W, H), Mask(W, H)
    for i in range(8):
        cx, cy = (i % 4) * 128 + 64, (i // 4) * 128 + 64
        rune = DESIGNED[i]
        draw_rune(gold, rune, cx, cy, 78, 0, 4.2)
        draw_rune(vio, rune, cx, cy, 78, 0, 8.5)
    compose("solomon_runes", W, H, [(vio, VIOLET, INDIGO, ((3.0, 0.7),)), (gold, GOLD, GOLD_D, GLOWS(1.4))])


def tex_pillar():
    W, H = 128, 512
    rng = np.random.RandomState(5)
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1      # -1..1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H
    X, Y = np.meshgrid(xs, ys)
    prof = np.exp(-(X / 0.56) ** 2 * 2.2)                         # bright middle
    edge = smooth(1.0, 0.6, np.abs(X))                           # soft sides
    streak = np.zeros_like(prof)
    for i in range(14):                                          # fine vertical streaks, each periodic in V
        x0 = rng.uniform(-0.8, 0.8)
        wd = rng.uniform(0.012, 0.035)
        per = rng.choice([1, 2, 3, 4])
        ph = rng.uniform(0, 6.28)
        mod = 0.45 + 0.55 * (0.5 + 0.5 * np.sin(2 * np.pi * per * Y + ph))
        streak += np.exp(-((X - x0) / wd) ** 2) * mod * rng.uniform(0.35, 0.8)
    # rune marks drifting up the sides, seamless
    marks = Mask(W, H)
    rr = random.Random(9)
    for i in range(7):
        y = (i + rr.random() * 0.6) / 7 * H
        x = rr.uniform(20, W - 20)
        for off in (-H, 0, H):
            draw_rune(marks, make_rune(rr), x, y + off, 15, 0, 1.2)
    mk = marks.final()
    core = np.exp(-(X / 0.24) ** 2)
    a = np.clip((prof * 0.62 + streak * 0.5 + core * 0.45 + mk * 0.7) * edge, 0, 1)
    tint = col((228, 214, 255))[None, None, :] * (1 - core[..., None]) + col(WHITE)[None, None, :] * core[..., None]
    rgb = tint * (0.78 + 0.22 * np.clip(streak, 0, 1)[..., None])
    save(rgb, a, "solomon_pillar")


def tex_rune_wave():
    W = 512
    c = W / 2
    rng = random.Random(21)
    xx, yy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(W, dtype=np.float32))
    r = np.sqrt((xx - c + 0.5) ** 2 + (yy - c + 0.5) ** 2) / c
    front = np.exp(-((r - 0.955) / 0.012) ** 2) * 1.0 + np.exp(-((r - 0.955) / 0.045) ** 2) * 0.35
    trail = smooth(0.50, 0.95, r) * (1 - smooth(0.95, 0.985, r)) * 0.20
    fine, dust = Mask(W, W), Mask(W, W)
    R = c * 0.955
    for rr in (0.90, 0.84, 0.77, 0.69):                          # broken arcs behind the front
        a0 = rng.uniform(0, 6.28)
        for k in range(rng.randint(3, 5)):
            a1 = a0 + rng.uniform(0.2, 0.7)
            pts = [(c + math.cos(a) * R * rr / 0.955 * 0.955, c + math.sin(a) * R * rr / 0.955 * 0.955) for a in np.linspace(a0, a1, 16)]
            fine.line(pts, 1.4)
            a0 = a1 + rng.uniform(0.25, 0.9)
    for i in range(150):                                         # rune dust: tiny runes, thinning inwards
        rad = R * (1 - abs(rng.gauss(0.0, 0.26)))
        if rad < R * 0.5:
            continue
        a = rng.uniform(0, 6.28)
        draw_rune(dust, make_rune(rng), c + math.cos(a) * rad, c + math.sin(a) * rad, rng.uniform(5, 10), a + math.pi / 2, 1.3)
    fm, dm = fine.final(), dust.final()
    fall = smooth(0.42, 0.93, r)
    a = np.clip(front + trail + fm * 0.6 * fall + dm * 0.85 * fall, 0, 1) * (1 - smooth(0.975, 1.0, r))
    mix = np.clip(front, 0, 1)[..., None]
    rgb = col(LILAC)[None, None, :] * (1 - mix) + col((255, 246, 220)) * mix
    rgb = rgb * (1 - 0.35 * (dm * fall)[..., None]) + col(GOLD) * 0.35 * (dm * fall)[..., None]
    save(rgb, a, "solomon_rune_wave")


def tex_chain():
    W, H = 128, 512
    gold, vio = Mask(W, H), Mask(W, H)
    per = 128
    for off in (-H, 0, H):
        for i in range(4):
            y0 = i * per + off
            # a link seen face on, then one seen edge on, alternating
            cy = y0 + 32
            gold.d.ellipse([(64 - 27) * SS, (cy - 40) * SS, (64 + 27) * SS, (cy + 40) * SS], outline=255, width=7 * SS)
            vio.d.ellipse([(64 - 19) * SS, (cy - 31) * SS, (64 + 19) * SS, (cy + 31) * SS], outline=255, width=2 * SS)
            cy2 = y0 + 96
            gold.d.ellipse([(64 - 9) * SS, (cy2 - 40) * SS, (64 + 9) * SS, (cy2 + 40) * SS], outline=255, width=6 * SS)
            vio.d.ellipse([(64 - 3.5) * SS, (cy2 - 28) * SS, (64 + 3.5) * SS, (cy2 + 28) * SS], fill=255)
    layers = [(vio, LILAC, VIOLET, ((2.0, 0.7),)), (gold, GOLD, GOLD_D, ((1.4, 0.8), (4.0, 0.4)))]
    compose("solomon_chain", W, H, layers)
    # make the glow seamless in V: it was blurred without wrapping, so rebuild with a wrapped canvas
    P = np.zeros((H, W, 3), np.float32)
    A = np.zeros((H, W), np.float32)
    for m, core, glow, spec in layers:
        s = np.clip(m.final(), 0, 1)
        s3 = np.vstack([s, s, s])
        g = np.zeros_like(s3)
        for rad, k in spec:
            g = g + blur(s3, rad) * k
        g = np.clip(g, 0, 1)[H:2 * H] * (1 - s * 0.7)
        P += col(core) * s[..., None] + col(glow) * g[..., None]
        A += s + g
    Ac = np.clip(A, 0, 1)
    save(P / np.maximum(Ac, 1e-4)[..., None], Ac, "solomon_chain")


def tex_book():
    W = 256
    gold, vio = Mask(W, W), Mask(W, W)
    x0, x1, y0, y1 = 58, 198, 24, 232
    cover = Image.new("L", (W, W), 0)
    ImageDraw.Draw(cover).rounded_rectangle([x0, y0, x1, y1], radius=7, fill=255)
    cm = blur(np.asarray(cover, dtype=np.float32) / 255.0, 0.5)
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    leather = 0.5 + 0.5 * np.sin(xx * 1.9 + np.sin(yy * 0.31) * 2.0) * np.sin(yy * 1.3)
    base = col(INDIGO)[None, None, :] * (0.78 + 0.22 * leather[..., None]) + col(VIOLET) * 0.18
    gold.d.rounded_rectangle([(x0 + 7) * SS, (y0 + 7) * SS, (x1 - 7) * SS, (y1 - 7) * SS], radius=4 * SS, outline=255, width=2 * SS)
    gold.d.rounded_rectangle([(x0 + 12) * SS, (y0 + 12) * SS, (x1 - 12) * SS, (y1 - 12) * SS], radius=3 * SS, outline=255, width=1 * SS)
    cx, cy = (x0 + x1) / 2 + 3, 110
    hexagram(gold, cx, cy, 36, 2.6)
    gold.circle(cx, cy, 45, 2.4)
    gold.circle(cx, cy, 30, 1.2)
    gold.disc(cx, cy, 4)
    rng = random.Random(6)
    for i in range(6):
        a = -math.pi / 2 + math.pi / 3 * i + math.pi / 6
        draw_rune(vio, make_rune(rng), cx + math.cos(a) * 56, cy + math.sin(a) * 56, 9, a + math.pi / 2, 1.6)
    for yy_ in (170, 180, 190, 200):                                   # title bars
        gold.line([(cx - 36 + (yy_ - 170) * 0.8, yy_), (cx + 36 - (yy_ - 170) * 0.8, yy_)], 1.6)
    for (px, py) in ((x0 + 9, y0 + 9), (x1 - 9, y0 + 9), (x0 + 9, y1 - 9), (x1 - 9, y1 - 9)):   # corner studs
        gold.disc(px, py, 4.5)
    gold.line([(x0 + 14, y0), (x0 + 14, y1)], 3)                      # the spine
    gm, vm = np.clip(gold.final(), 0, 1), np.clip(vio.final(), 0, 1)
    glow = np.clip(blur(gm, 2.2) * 0.7 + blur(gm, 6) * 0.35, 0, 1) * (1 - gm)
    rgb = base * cm[..., None]
    rgb = rgb * (1 - gm[..., None]) + col(GOLD) * gm[..., None]
    rgb = rgb * (1 - vm[..., None] * 0.7) + col(LILAC) * vm[..., None] * 0.7
    halo = blur(cm, 7) * 0.5
    a = np.clip(cm * 0.9 + halo, 0, 1)
    out = (rgb * (cm * 0.9)[..., None] + col(GOLD_D) * (halo * (1 - cm))[..., None] + col(GOLD_D) * glow[..., None] * cm[..., None] * 0.4) / np.maximum(a, 1e-4)[..., None]
    save(out, a, "solomon_book")


def tex_beam():
    W, H = 128, 512
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H
    X, Y = np.meshgrid(xs, ys)
    core = np.exp(-(X / 0.09) ** 2)
    halo = np.exp(-(X / 0.34) ** 2) * 0.45
    dash = np.zeros_like(X)
    for per, ph, w in ((4, 0.0, 0.07), (3, 0.4, 0.05), (6, 0.15, 0.04)):         # dashes (rune ticks) running along it
        d = np.abs(((Y * per + ph) % 1.0) - 0.5)
        dash += smooth(0.16, 0.0, d) * np.exp(-(X / (w * 4)) ** 2) * 0.55
    ch = Mask(W, H)
    for off in (-H, 0, H):                                                       # tiny chevrons either side
        for i in range(8):
            y = i * 64 + 14 + off
            ch.line([(38, y + 10), (46, y), (38, y - 10)], 1.4)
            ch.line([(W - 38, y + 10), (W - 46, y), (W - 38, y - 10)], 1.4)
    a = np.clip(core * 0.95 + halo + dash + ch.final() * 0.6, 0, 1) * smooth(1.0, 0.7, np.abs(X))
    rgb = col((226, 210, 255))[None, None, :] * (1 - core[..., None]) + col(WHITE) * core[..., None]
    save(rgb, a, "solomon_beam")


def tex_flare():
    W = 256
    c = W / 2
    xx, yy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(W, dtype=np.float32))
    dx, dy = xx - c + 0.5, yy - c + 0.5
    r = np.sqrt(dx * dx + dy * dy) / c
    ang = np.arctan2(dy, dx)
    core = np.exp(-(r / 0.07) ** 2) + np.exp(-(r / 0.22) ** 2) * 0.55 + np.exp(-(r / 0.5) ** 2) * 0.18
    rays = np.zeros_like(r)
    for k in range(6):                                           # six long points of the hexagram star
        a0 = -math.pi / 2 + k * math.pi / 3
        da = np.abs(np.arctan2(np.sin(ang - a0), np.cos(ang - a0)))
        rays += np.exp(-(da / 0.035) ** 2) * np.exp(-(r / 0.62) ** 2) * 0.9
    for k in range(6):                                           # six short ones between them
        a0 = -math.pi / 2 + k * math.pi / 3 + math.pi / 6
        da = np.abs(np.arctan2(np.sin(ang - a0), np.cos(ang - a0)))
        rays += np.exp(-(da / 0.05) ** 2) * np.exp(-(r / 0.28) ** 2) * 0.45
    lens = np.exp(-((r - 0.62) / 0.012) ** 2) * 0.45 + np.exp(-((r - 0.74) / 0.008) ** 2) * 0.25
    a = np.clip(core + rays + lens, 0, 1) * (1 - smooth(0.93, 1.0, r))
    mix = np.clip(core, 0, 1)[..., None]
    rgb = col((232, 218, 255))[None, None, :] * (1 - mix) + col(WHITE) * mix
    save(rgb, a, "solomon_flare")


def tex_mote():
    W = 64
    c = W / 2
    xx, yy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(W, dtype=np.float32))
    dx, dy = np.abs(xx - c + 0.5) / c, np.abs(yy - c + 0.5) / c
    core = np.exp(-((dx * dx + dy * dy) / 0.012))
    cross = (np.exp(-(dx / 0.05) ** 2) * np.exp(-(dy / 0.8) ** 2) + np.exp(-(dy / 0.05) ** 2) * np.exp(-(dx / 0.8) ** 2)) * 0.8
    diag = (np.exp(-(np.abs(dx - dy) / 0.06) ** 2) * np.exp(-((dx + dy) / 0.5) ** 2)) * 0.35
    soft = np.exp(-((dx * dx + dy * dy) / 0.12)) * 0.35
    a = np.clip(core + cross + diag + soft, 0, 1) * (1 - smooth(0.92, 1.0, np.sqrt(dx * dx + dy * dy)))
    rgb = np.ones((W, W, 3), np.float32) * col((236, 224, 255))
    save(rgb, a, "solomon_mote")


def tex_shell():
    W, H = 512, 1024
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H * 2 - 1
    X, Y = np.meshgrid(xs, ys)
    # an upright egg: a little narrower at the top
    wid = 0.92 - 0.07 * Y
    e = np.sqrt((X / wid) ** 2 + (Y / 0.96) ** 2)
    inside = e < 1
    fres = smooth(0.55, 1.0, e) ** 1.7
    rim = np.exp(-((e - 0.975) / 0.018) ** 2) * 0.95 + np.exp(-((e - 0.93) / 0.05) ** 2) * 0.3
    # honeycomb: hexagonal cell edges, drawn per pixel as the distance to the nearest cell border
    s = 17.0
    px, py = X * W / 2 / s, Y * H / 2 / s
    q = (px * math.sqrt(3) / 3 - py / 3) * 1.0
    rr = py * 2 / 3
    # axial -> cube rounding
    cx_, cz_ = q, rr
    cy_ = -cx_ - cz_
    rx, ry, rz = np.round(cx_), np.round(cy_), np.round(cz_)
    dxr, dyr, dzr = np.abs(rx - cx_), np.abs(ry - cy_), np.abs(rz - cz_)
    fix_x = (dxr > dyr) & (dxr > dzr)
    fix_y = (~fix_x) & (dyr > dzr)
    rx = np.where(fix_x, -ry - rz, rx)
    rz = np.where(~fix_x & ~fix_y, -rx - ry, rz)
    fx, fz = cx_ - rx, cz_ - rz
    fy = -fx - fz
    edgeD = 0.5 - np.maximum(np.maximum(np.abs(fx), np.abs(fy)), np.abs(fz))
    lat = np.exp(-(edgeD / 0.035) ** 2)
    cell_id = (rx * 7.3 + rz * 13.1)
    cell_glint = (0.5 + 0.5 * np.sin(cell_id * 1.7)) ** 3
    a = np.where(inside, 0.05 + 0.30 * fres + lat * (0.20 + 0.55 * fres) + cell_glint * 0.10 + rim, 0.0)
    a = a + np.exp(-((e - 1.0) / 0.05) ** 2) * (~inside) * 0.35
    a = np.clip(a, 0, 1)
    mixv = np.clip(fres + rim * 0.8, 0, 1)[..., None]
    rgb = col((176, 140, 255))[None, None, :] * (1 - mixv) + col((255, 232, 168)) * mixv
    rgb = rgb * (1 - lat[..., None] * 0.35) + col((255, 246, 224)) * lat[..., None] * 0.35
    save(rgb, a, "solomon_shell")


def tex_crack():
    W, H = 512, 1024
    rng = random.Random(404)
    m, m2 = Mask(W, H), Mask(W, H)

    def crack(x, y, ang, length, depth, w):
        pts = [(x, y)]
        step = length / 9.0
        for i in range(9):
            ang += rng.uniform(-0.55, 0.55)
            x += math.cos(ang) * step
            y += math.sin(ang) * step
            pts.append((x, y))
            if depth > 0 and i in (2, 5) and rng.random() < 0.8:
                crack(x, y, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0), length * 0.55, depth - 1, w * 0.65)
        m.line(pts, w)
        m2.line(pts, w * 2.6)

    for sx, sy, a0 in ((250, 380, 0.3), (250, 380, 2.4), (250, 380, 4.4), (180, 650, 0.0), (330, 700, 3.2), (260, 220, 5.4), (250, 820, 4.8)):
        crack(sx, sy, a0, 190, 2, 4.2)
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H * 2 - 1
    X, Y = np.meshgrid(xs, ys)
    e = np.sqrt((X / (0.92 - 0.07 * Y)) ** 2 + (Y / 0.96) ** 2)
    inside = (1 - smooth(0.93, 1.0, e))
    s = np.clip(m.final(), 0, 1) * inside
    s2 = np.clip(m2.final(), 0, 1) * inside
    g = np.clip(blur(s2, 4) * 0.9 + blur(s2, 12) * 0.5, 0, 1) * (1 - s * 0.7)
    a = np.clip(s + g * 0.8, 0, 1)
    rgb = (col((255, 252, 236)) * s[..., None] + col((255, 190, 90)) * (g * 0.8)[..., None]) / np.maximum(s + g * 0.8, 1e-4)[..., None]
    save(rgb, a, "solomon_crack")


def sigil(m, i, cx, cy, R, w):
    """The element sigil i (flame, tide, gale, stone, storm, frost, light, dark) with its two rings, centred on (cx, cy) with outer radius R."""
    m.circle(cx, cy, R * 0.94, w * 1.4)
    m.circle(cx, cy, R * 0.83, w * 0.7)
    for k in range(24):
        a = 2 * math.pi * k / 24
        m.line([(cx + math.cos(a) * R * 0.83, cy + math.sin(a) * R * 0.83), (cx + math.cos(a) * R * 0.88, cy + math.sin(a) * R * 0.88)], w * 0.6)
    u = R * 0.56

    def P(pts):
        return [(cx + x * u, cy + y * u) for x, y in pts]

    if i == 0:      # flame
        m.poly(P([(0.05, -1.0), (0.5, -0.35), (0.7, 0.2), (0.5, 0.7), (0.0, 0.95), (-0.5, 0.7), (-0.7, 0.2), (-0.45, -0.2), (-0.2, 0.05), (0.05, -1.0)]), w * 1.3, close=False)
        m.poly(P([(0.0, 0.2), (0.25, 0.5), (0.0, 0.8), (-0.25, 0.5)]), w)
    elif i == 1:    # tide
        for yy in (-0.55, 0.0, 0.55):
            m.line(P([(x, yy + 0.2 * math.sin(x * 4.2 + 0.4)) for x in np.linspace(-0.9, 0.9, 24)]), w * 1.2)
    elif i == 2:    # gale
        for ph in (0.0, math.pi):
            m.line(P([(math.cos(t + ph) * (0.12 + t * 0.2), math.sin(t + ph) * (0.12 + t * 0.2)) for t in np.linspace(0, 4.4, 40)]), w * 1.2)
        m.line(P([(0.9, 0.35), (0.5, 0.45), (0.2, 0.3)]), w)
        m.line(P([(-0.9, -0.35), (-0.5, -0.45), (-0.2, -0.3)]), w)
    elif i == 3:    # stone
        m.poly(P([(0, -0.95), (0.85, 0), (0, 0.95), (-0.85, 0)]), w * 1.3)
        m.line(P([(-0.85, 0), (0.85, 0)]), w)
        m.line(P([(0, -0.95), (0, 0.95)]), w)
        m.disc(cx, cy, w * 1.6)
    elif i == 4:    # storm
        m.poly(P([(0.25, -1.0), (-0.5, 0.1), (-0.05, 0.1), (-0.3, 1.0), (0.55, -0.2), (0.08, -0.2)]), w * 1.3)
    elif i == 5:    # frost
        for k in range(6):
            a = -math.pi / 2 + k * math.pi / 3
            ca, sa = math.cos(a), math.sin(a)
            m.line(P([(0, 0), (ca, sa)]), w * 1.2)
            for t in (0.45, 0.72):
                for sg in (-1, 1):
                    b = a + sg * 0.62
                    m.line(P([(ca * t, sa * t), (ca * t + math.cos(b) * 0.28, sa * t + math.sin(b) * 0.28)]), w)
    elif i == 6:    # light
        m.circle(cx, cy, u * 0.38, w * 1.3)
        m.disc(cx, cy, u * 0.12)
        for k in range(16):
            a = 2 * math.pi * k / 16
            r1 = u * (0.62 if k % 2 else 0.5)
            r2 = u * (1.0 if k % 2 == 0 else 0.78)
            m.line([(cx + math.cos(a) * r1, cy + math.sin(a) * r1), (cx + math.cos(a) * r2, cy + math.sin(a) * r2)], w)
    else:           # dark: a crescent and three small stars
        pts = []
        for t in np.linspace(0, 2 * math.pi, 120):
            x, y = math.cos(t) * 0.95, math.sin(t) * 0.95
            pts.append((x, y))
        m.poly(P(pts), w * 1.3)
        run = []
        for t in np.linspace(0, 2 * math.pi, 140):
            x, y = 0.4 + math.cos(t) * 0.78, -0.12 + math.sin(t) * 0.78
            if x * x + y * y < 0.9:
                run.append((x, y))
            elif len(run) > 1:
                m.line(P(run), w * 1.1)
                run = []
            else:
                run = []
        if len(run) > 1:
            m.line(P(run), w * 1.1)
        for (x, y) in ((0.55, 0.1), (0.35, 0.55), (0.62, -0.4)):
            m.disc(cx + x * u, cy + y * u, w * 1.6)


def tex_sigils():
    W, H = 1024, 512
    layers = []
    for i, (core, glow) in enumerate(ELEMENTS):
        m = Mask(W, H)
        cx, cy = (i % 4) * 256 + 128, (i // 4) * 256 + 128
        sigil(m, i, cx, cy, 122, 3.0)
        layers.append((m, core, glow, GLOWS(1.1)))
    compose("solomon_sigils", W, H, layers)


def tex_crater():
    W = 512
    c = W / 2
    rng = np.random.RandomState(8)
    xx, yy = np.meshgrid(np.arange(W, dtype=np.float32), np.arange(W, dtype=np.float32))
    dx, dy = (xx - c + 0.5) / c, (yy - c + 0.5) / c
    r = np.sqrt(dx * dx + dy * dy)
    ang = np.arctan2(dy, dx)
    noise = np.zeros_like(r)
    for k, amp in ((3, 0.035), (5, 0.03), (9, 0.022), (14, 0.016)):
        noise += amp * np.sin(ang * k + rng.uniform(0, 6.28))
    edge = r + noise
    disc = smooth(0.92, 0.62, edge)
    n2 = blur(rng.rand(W, W).astype(np.float32), 6) * 2.4
    n2 = (n2 - n2.mean()) * 3 + 0.5
    dark = np.clip(disc * (0.60 + 0.25 * np.clip(n2, 0, 1)), 0, 0.82)
    cr = Mask(W, W)
    rr = random.Random(11)
    for k in range(11):
        a = rr.uniform(0, 6.28)
        pts = []
        rad = rr.uniform(0.05, 0.16) * c
        while rad < c * rr.uniform(0.62, 0.86):
            a += rr.uniform(-0.2, 0.2)
            rad += c * rr.uniform(0.03, 0.07)
            pts.append((c + math.cos(a) * rad, c + math.sin(a) * rad))
        if len(pts) > 1:
            cr.line(pts, rr.uniform(1.5, 2.8))
    crm = np.clip(cr.final(), 0, 1) * disc
    cg = np.clip(blur(crm, 3.0) * 1.1, 0, 1)
    spk = np.clip((blur(rng.rand(W, W).astype(np.float32), 1.2) - 0.5) * 9.0, 0, 1)
    ember = np.exp(-((edge - 0.80) / 0.07) ** 2) * (0.15 + 0.85 * spk) * 0.7
    a = np.clip(dark + crm * 0.9 + cg * 0.4 + ember, 0, 1)
    cdark = col((14, 8, 32))[None, None, :]
    rgb = cdark * (dark[..., None])
    rgb = rgb + col((178, 130, 255)) * (crm * 0.9 + cg * 0.4)[..., None] + col((255, 190, 90)) * ember[..., None]
    rgb = rgb / np.maximum(a, 1e-4)[..., None]
    save(rgb, a, "solomon_crater")


def tex_veil():
    W, H = 128, 512
    rng = random.Random(15)
    gold, vio = Mask(W, H), Mask(W, H)
    for cxm in (24, 64, 104):
        y = rng.uniform(0, 60)
        while y < H - 20:
            draw_rune(vio, make_rune(rng), cxm + rng.uniform(-2, 2), H - y, 18, 0, 1.7)
            y += rng.uniform(34, 52)
    gold.line([(6, 0), (6, H)], 2.0)
    gold.line([(W - 6, 0), (W - 6, H)], 2.0)
    for yv in (H - 4, H - 40):
        gold.line([(6, yv), (W - 6, yv)], 2.2)
    xs = (np.arange(W, dtype=np.float32) + 0.5) / W * 2 - 1
    ys = (np.arange(H, dtype=np.float32) + 0.5) / H
    X, Y = np.meshgrid(xs, ys)
    vert = (Y ** 1.7)                                            # bright at the foot (V = 1), gone at the top
    wave = 0.5 + 0.5 * np.sin(X * 9 + Y * 5)
    sheet = vert * (0.14 + 0.08 * wave) * smooth(1.0, 0.85, np.abs(X))
    rgb_sheet = col((130, 90, 235))[None, None, :] * np.ones((H, W, 1), np.float32)
    layers = [(vio, LILAC, VIOLET, ((2.4, 0.8),)), (gold, GOLD, GOLD_D, ((1.4, 0.8), (4.0, 0.4)))]
    P = rgb_sheet * sheet[..., None]
    A = sheet.copy()
    for m, core, glow, spec in layers:
        s = np.clip(m.final(), 0, 1) * (0.25 + 0.75 * vert)
        g = np.zeros_like(s)
        for rad, k in spec:
            g += blur(s, rad) * k
        g = np.clip(g, 0, 1) * (1 - s * 0.7)
        P = P + col(core) * s[..., None] + col(glow) * g[..., None]
        A = A + s + g
    Ac = np.clip(A, 0, 1)
    save(P / np.maximum(Ac, 1e-4)[..., None], Ac, "solomon_veil")


ALL = {
    "ring": tex_ring, "hexagram": tex_hexagram, "circle": tex_circle, "page": tex_page, "runes": tex_runes, "pillar": tex_pillar,
    "rune_wave": tex_rune_wave, "chain": tex_chain, "book": tex_book, "beam": tex_beam, "flare": tex_flare, "mote": tex_mote,
    "shell": tex_shell, "crack": tex_crack, "sigils": tex_sigils, "crater": tex_crater, "veil": tex_veil,
}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()

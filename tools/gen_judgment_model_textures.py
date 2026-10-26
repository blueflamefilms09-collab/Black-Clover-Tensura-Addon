"""Generates the material atlas of the 3D models of "Sandalphon, Lord of Judgment" (3.21.0: vfx.client.layer.JudgmentModels / JudgmentMeshDraw / JudgmentLayer).

Deterministic and font-free (every machine writes the same pixels). Written for this mod, same palette as the layer and tools/gen_judgment_vfx_textures.py: pale white-gold light, pale steel, hairline black. It only
ADDS one file; the 14 flat textures of gen_judgment_vfx_textures.py stay and are still drawn when the setting `judgmentModels` is off. The pseudo-script of the glyph panels is the one of the flat glyph band
(`glyph_cell` of gen_judgment_vfx_textures.py, imported, not changed).
Output: src/main/resources/assets/nusmp/textures/particle/judgment_model_atlas.png

    python tools/gen_judgment_model_textures.py

    judgment_model_atlas     4 x 4 cells of 128 px (512 x 512), alpha-blended, lit like metal / stone from the upper left and shaded again by the layer per face:
        0 pale gold plate: hairline black border, a column of rivets and an engraved line down the middle (the strap is cropped to that column)
        1 pale steel, brushed along V, a hairline gold seam                       2 the glowing edge: white core, gold sides (U along the edge)
        3 white marble with hairline black cracks                                  4 dark slate with faint gold hairlines
        5 glyph panel A: five rows of glowing pseudo-script on dark stone          6 glyph panel B: four rows and a gold rule with a diamond
        7 crest: a gold ring round a balance and a sword                           8 chain links (cut out, cropped to the middle column)
        9 gold dish: bright rim at the top (V = 0), scalloped engraving            10 white-hot core
        11 broken stone: pale, with a web of glowing gold cracks                   12 guillotine blade face: darker steel, diagonal streaks, hairline black rules, gold chevrons
        13 aged gold with dark wrap bands (grip, socket, collars)                  14 a white-gold feather (cut out), base at the bottom
        15 the fuller: a glowing groove down the middle (U across)
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_judgment_vfx_textures as j   # the palette of the layer, the pseudo-script, the output folder (imported, not changed)

S = 128
CELL = {"gold": 0, "steel": 1, "edge": 2, "stone": 3, "stone_dark": 4, "glyph_a": 5, "glyph_b": 6, "crest": 7, "chain": 8, "pan": 9, "hot": 10, "shard": 11, "blade_face": 12, "gold_dark": 13, "feather": 14, "fuller": 15}

# the palette (the layer's WHITE_GOLD 0xFFE3A0, STEEL 0xAFC3DC, BLACK 0x06050B, and their shades)
WHITE_GOLD = (255, 227, 160)
GOLD_SH = (92, 66, 28)
GOLD_MID = (206, 170, 92)
GOLD_LT = (255, 232, 160)
GOLD_HI = (255, 250, 222)
STEEL = (175, 195, 220)
STEEL_SH = (58, 68, 88)
STEEL_LT = (226, 234, 246)
BLACK = (6, 5, 11)
STONE_LT = (240, 234, 220)
STONE_MID = (206, 196, 178)
STONE_SH = (120, 110, 100)
SLATE = (70, 66, 78)


def col(c):
    return np.array(c, np.float32) / 255.0


def grid01(w=S, h=S):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def smooth(e0, e1, x):
    return j.smooth(e0, e1, x)


def edge_dist(w=S, h=S):
    u, v = grid01(w, h)
    return np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))


def bevel(width=0.14):
    return smooth(0.0, width, edge_dist())


def noise(seed, cells=(32, 16, 8, 4), gains=(0.4, 0.3, 0.2, 0.1), w=S, h=S, aniso=(1, 1)):
    """Fractal value noise 0..1 (random grids resized bicubic); aniso = (x, y) stretch factors."""
    rs = np.random.RandomState(seed)
    out = np.zeros((h, w), np.float32)
    for c, g in zip(cells, gains):
        cx, cy = max(2, int(c / aniso[0])), max(2, int(c / aniso[1]))
        a = rs.rand(cy, cx).astype(np.float32)
        im = Image.fromarray((a * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, dtype=np.float32) / 255.0 * g
    return np.clip(out / sum(gains), 0, 1)


def ramp(t, stops):
    """Colour ramp: t 0..1 -> rgb, stops = [(pos, (r, g, b)), ...]."""
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (p0, c0), (p1, c1) in zip(stops[:-1], stops[1:]):
        m = ((t >= p0) & (t <= p1))[..., None]
        k = np.clip((t - p0) / max(p1 - p0, 1e-6), 0, 1)[..., None]
        out = np.where(m, col(c0) * (1 - k) + col(c1) * k, out)
    return out


GOLD_RAMP = [(0.0, GOLD_SH), (0.45, GOLD_MID), (0.8, GOLD_LT), (1.0, GOLD_HI)]
STEEL_RAMP = [(0.0, STEEL_SH), (0.5, STEEL), (0.85, STEEL_LT), (1.0, (252, 253, 255))]
STONE_RAMP = [(0.0, STONE_SH), (0.5, STONE_MID), (1.0, STONE_LT)]


def lit(h, k=9.0, ambient=0.30, spec=0.45, light=(-0.55, -0.62, 0.56)):
    """Height map -> lighting 0..1 (lambert from the upper left plus a little specular)."""
    gy, gx = np.gradient(np.asarray(Image.fromarray((np.clip(h, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8)), dtype=np.float32) / 255.0)
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(h)
    nn = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * light[0] + ny * light[1] + nz * light[2]) / nn, 0, 1)
    hv = np.array([light[0] * 0.5, light[1] * 0.5, light[2] * 0.5 + 0.5])
    hv = hv / np.linalg.norm(hv)
    sp = np.clip((nx * hv[0] + ny * hv[1] + nz * hv[2]) / nn, 0, 1) ** 28
    return np.clip(ambient + (1 - ambient) * lam * 0.9 + spec * sp, 0, 1)


def mix(rgb, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return rgb * (1 - a) + col(colour)[None, None, :] * a


def line_u(u, centre, width):
    return np.exp(-((u - centre) / width) ** 2)


def border_line(inset_px, w_px):
    d = edge_dist() * S
    return np.exp(-((d - inset_px) / (w_px * 0.55)) ** 2)


def solid():
    return np.ones((S, S), np.float32)


def rivet(rgb, px, py, r=4.0):
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    d = np.sqrt((xx - px) ** 2 + (yy - py) ** 2)
    m = smooth(r, r - 1.5, d)
    hi = np.exp(-(((xx - px + 1.2) ** 2 + (yy - py + 1.2) ** 2) / (r * 0.45) ** 2))
    ring = smooth(r + 1.8, r, d) * (1 - m)
    rgb = rgb * (1 - 0.5 * ring[..., None])
    rgb = rgb * (1 - m[..., None]) + col(GOLD_LT)[None, None, :] * (m * 0.75)[..., None] + col(GOLD_HI)[None, None, :] * (hi * m * 0.9)[..., None]
    return rgb


# ---------------------------------------------------------------------------------------------------- the cells
def cell_gold():
    u, v = grid01()
    h = bevel(0.12) * 0.7 + (noise(5) - 0.5) * 0.06
    rgb = ramp(lit(h), GOLD_RAMP)
    rgb = mix(rgb, BLACK, border_line(3.2, 1.7) * 0.85)                          # the hairline black border
    rgb = mix(rgb, GOLD_HI, border_line(6.4, 1.6) * 0.5)
    rgb = mix(rgb, BLACK, line_u(u, 0.5, 0.006) * smooth(0.06, 0.12, v) * smooth(0.06, 0.12, 1 - v) * 0.55)       # an engraved line down the middle
    for py in (0.14, 0.5, 0.86):                                                  # a column of rivets (the strap is cropped to this column)
        rgb = rivet(rgb, 0.5 * S, py * S)
    for px in (14, S - 14):
        for py in (14, S - 14):
            rgb = rivet(rgb, px, py, 3.2)
    return np.clip(rgb, 0, 1), solid()


def cell_steel():
    u, v = grid01()
    streak = noise(11, cells=(48, 24, 12), gains=(0.5, 0.3, 0.2), aniso=(1, 8))   # brushed along V
    h = bevel(0.10) * 0.5 + (streak - 0.5) * 0.10
    rgb = ramp(lit(h, k=7.0, ambient=0.34), STEEL_RAMP)
    rgb = mix(rgb, GOLD_LT, border_line(4.0, 1.6) * 0.55)
    rgb = mix(rgb, BLACK, border_line(7.5, 1.2) * 0.35)
    return np.clip(rgb, 0, 1), solid()


def cell_edge():
    u, v = grid01()
    t = 1 - np.abs(v - 0.5) * 2
    flick = 0.92 + 0.08 * np.sin(u * 61.0) * np.sin(u * 17.0 + 1.0)
    core = np.exp(-(((1 - t) / 0.33) ** 2))
    rgb = ramp(np.clip(0.55 + 0.45 * core, 0, 1) * flick, [(0.0, WHITE_GOLD), (0.6, (255, 244, 205)), (1.0, (255, 255, 252))])
    return np.clip(rgb, 0, 1), solid()


def veins(seed, n=3):
    rs = np.random.RandomState(seed)
    im = Image.new("L", (S * 3, S * 3), 0)
    d = ImageDraw.Draw(im)
    for _ in range(n):
        x, y = rs.rand() * S * 3, rs.rand() * S * 3
        ang = rs.rand() * math.tau
        pts = [(x, y)]
        for _ in range(18):
            ang += (rs.rand() - 0.5) * 1.1
            x += math.cos(ang) * 14
            y += math.sin(ang) * 14
            pts.append((x, y))
        d.line(pts, fill=255, width=2)
    m = np.asarray(im.resize((S, S), Image.LANCZOS), dtype=np.float32) / 255.0
    return m


def cell_stone():
    n = noise(21, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = bevel(0.10) * 0.6 + (n - 0.5) * 0.10
    rgb = ramp(lit(h, k=6.0, ambient=0.45), STONE_RAMP) * (0.92 + 0.12 * n[..., None])
    soft = noise(23, cells=(16, 8, 4), gains=(0.5, 0.3, 0.2))
    rgb = mix(rgb, (190, 160, 120), np.clip(soft - 0.55, 0, 1) * 0.6)             # warm veining
    cr = veins(7, 3)
    rgb = mix(rgb, BLACK, cr * 0.8)                                                # hairline black cracks
    rgb = mix(rgb, GOLD_LT, border_line(3.4, 1.6) * 0.45)
    return np.clip(rgb, 0, 1), solid()


def cell_stone_dark():
    n = noise(31, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = bevel(0.10) * 0.6 + (n - 0.5) * 0.12
    rgb = ramp(lit(h, k=6.0, ambient=0.40), [(0.0, (34, 32, 42)), (0.6, SLATE), (1.0, (150, 144, 156))])
    cr = veins(13, 2)
    rgb = mix(rgb, GOLD_MID, cr * 0.55)
    rgb = mix(rgb, GOLD_LT, border_line(3.4, 1.4) * 0.3)
    return np.clip(rgb, 0, 1), solid()


def script_rows(rows, cols, seed, x0, x1, y0, y1, thin=2):
    """The pseudo-script of the flat glyph band as a 0..1 mask inside the box (fractions of the cell)."""
    ss = 3
    W = S * ss
    im = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(im)
    rng = np.random.default_rng(seed)
    cw = (x1 - x0) * W / cols
    ch = (y1 - y0) * W / rows
    for r in range(rows):
        for c in range(cols):
            if rng.random() < 0.08:
                continue
            j.glyph_cell(_Gray(d), x0 * W + c * cw, y0 * W + r * ch, cw, ch, rng, thin)
    return np.asarray(im.resize((S, S), Image.LANCZOS), dtype=np.float32) / 255.0


class _Gray:
    """Adapts the RGBA-drawing glyph_cell to a grey canvas (fill tuples become 255)."""

    def __init__(self, d):
        self.d = d

    def _f(self, kw):
        kw = dict(kw)
        for k in ("fill", "outline"):
            if k in kw and isinstance(kw[k], tuple):
                kw[k] = 255
        return kw

    def line(self, *a, **kw): self.d.line(*a, **self._f(kw))
    def ellipse(self, *a, **kw): self.d.ellipse(*a, **self._f(kw))
    def polygon(self, *a, **kw): self.d.polygon(*a, **self._f(kw))
    def arc(self, *a, **kw): self.d.arc(*a, **self._f(kw))


def glow_text(rgb, mask, strength=1.0):
    bl = np.asarray(Image.fromarray((np.clip(mask, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.2)), dtype=np.float32) / 255.0
    rgb = mix(rgb, GOLD_MID, bl * 0.55 * strength)
    rgb = mix(rgb, WHITE_GOLD, np.clip(mask * 1.2, 0, 1) * strength)
    rgb = mix(rgb, (255, 255, 250), np.clip(mask - 0.55, 0, 1) * 1.6 * strength)
    return rgb


def glyph_panel(seed, rows, cols, rule):
    u, v = grid01()
    n = noise(seed, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = bevel(0.10) * 0.6 + (n - 0.5) * 0.10
    rgb = ramp(lit(h, k=6.0, ambient=0.45), STONE_RAMP) * (0.92 + 0.1 * n[..., None])        # the pale stone frame
    inner = smooth(0.10, 0.115, u) * smooth(0.10, 0.115, 1 - u) * smooth(0.07, 0.085, v) * smooth(0.07, 0.085, 1 - v)
    dark = ramp(0.18 + 0.2 * n, [(0.0, (26, 24, 34)), (1.0, (84, 80, 92))])
    rgb = rgb * (1 - inner[..., None]) + dark * inner[..., None]                              # the inset panel
    frame = np.exp(-((np.minimum(np.abs(u - 0.5) - 0.40, 0) * 0 + 0) ** 2))
    d = np.maximum(np.abs(u - 0.5) / 0.40, np.abs(v - 0.5) / 0.43)
    rgb = mix(rgb, GOLD_LT, np.exp(-((d - 1.0) / 0.035) ** 2) * 0.9)                          # a gold hairline round the panel
    mask = script_rows(rows, cols, seed, 0.15, 0.85, 0.11 if not rule else 0.09, 0.89 if not rule else 0.45)
    if rule:
        mask = np.maximum(mask, script_rows(2, cols, seed + 5, 0.15, 0.85, 0.57, 0.89))
        mask = np.maximum(mask, line_u(v, 0.5, 0.008) * smooth(0.14, 0.2, u) * smooth(0.14, 0.2, 1 - u))
        dia = np.clip(1 - (np.abs(u - 0.5) + np.abs(v - 0.5)) * 9.0, 0, 1)
        mask = np.maximum(mask, dia)
    rgb = glow_text(rgb, mask * inner)
    return np.clip(rgb, 0, 1), solid()


def cell_glyph_a():
    return glyph_panel(41, 5, 5, False)


def cell_glyph_b():
    return glyph_panel(43, 4, 5, True)


def cell_crest():
    u, v = grid01()
    n = noise(51, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = bevel(0.10) * 0.6 + (n - 0.5) * 0.10
    rgb = ramp(lit(h, k=6.0, ambient=0.45), STONE_RAMP) * (0.92 + 0.1 * n[..., None])
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2)
    disc = smooth(0.40, 0.385, r)
    dark = ramp(0.2 + 0.2 * n, [(0.0, (26, 24, 34)), (1.0, (84, 80, 92))])
    rgb = rgb * (1 - disc[..., None]) + dark * disc[..., None]
    ss = 4
    W = S * ss
    im = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(im)
    c = W / 2
    lw = int(2.2 * ss)
    d.ellipse([c - 0.40 * W, c - 0.40 * W, c + 0.40 * W, c + 0.40 * W], outline=255, width=int(2.6 * ss))
    d.ellipse([c - 0.345 * W, c - 0.345 * W, c + 0.345 * W, c + 0.345 * W], outline=255, width=int(1.2 * ss))
    d.line([(c, 0.2 * W), (c, 0.82 * W)], fill=255, width=lw)                                       # the post
    d.polygon([(c - 0.04 * W, 0.80 * W), (c + 0.04 * W, 0.80 * W), (c, 0.88 * W)], fill=255)        # a blade point below
    d.line([(c - 0.25 * W, 0.34 * W), (c + 0.25 * W, 0.34 * W)], fill=255, width=lw)               # the beam
    for sx in (-1, 1):
        ex = c + sx * 0.25 * W
        d.line([(ex, 0.34 * W), (ex - 0.09 * W, 0.50 * W)], fill=255, width=int(1.4 * ss))
        d.line([(ex, 0.34 * W), (ex + 0.09 * W, 0.50 * W)], fill=255, width=int(1.4 * ss))
        d.pieslice([ex - 0.11 * W, 0.46 * W, ex + 0.11 * W, 0.58 * W], 0, 180, fill=255)
    d.ellipse([c - 0.03 * W, 0.17 * W, c + 0.03 * W, 0.23 * W], fill=255)
    mask = np.asarray(im.resize((S, S), Image.LANCZOS), dtype=np.float32) / 255.0
    rgb = glow_text(rgb, mask)
    rgb = mix(rgb, GOLD_LT, border_line(3.4, 1.6) * 0.45)
    return np.clip(rgb, 0, 1), solid()


def cell_chain():
    """Chain links, cut out, in the middle 14 px column; 9 links down the cell (face-on rings and edge-on bars alternate)."""
    ss = 4
    W, H = 16 * ss, S * ss
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)
    link = H / 9
    for k in range(9):
        y0 = k * link
        if k % 2 == 0:
            d.ellipse([1.5 * ss, y0 + 0.5 * ss, W - 1.5 * ss, y0 + link * 1.55 - 0.5 * ss], outline=255, width=int(2.6 * ss))
        else:
            d.rounded_rectangle([W / 2 - 1.6 * ss, y0 + 0.4 * ss, W / 2 + 1.6 * ss, y0 + link * 1.6], radius=1.4 * ss, fill=255)
    a = np.asarray(im.resize((16, S), Image.LANCZOS), dtype=np.float32) / 255.0
    uu = (np.arange(16, dtype=np.float32) + 0.5) / 16
    shade = 0.55 + 0.45 * np.clip(1.0 - np.abs(uu - 0.38) * 3.0, 0, 1)[None, :]
    t = ramp(np.clip(shade, 0, 1), GOLD_RAMP)
    rgb = np.zeros((S, S, 3), np.float32)
    al = np.zeros((S, S), np.float32)
    rgb[:, :] = col(GOLD_MID)
    rgb[:, 56:72] = t * (0.85 + 0.25 * a[..., None])
    al[:, 56:72] = a
    return np.clip(rgb, 0, 1), al


def cell_pan():
    u, v = grid01()
    n = noise(61, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = (n - 0.5) * 0.08 + 0.5 * smooth(0.0, 0.5, v) * 0.3
    t = lit(h, k=5.0, ambient=0.42)
    shade = np.clip(t * (0.55 + 0.55 * (1 - v)), 0, 1)                        # darker toward the foot, bright rim
    rgb = ramp(shade, GOLD_RAMP)
    rgb = mix(rgb, GOLD_HI, np.exp(-((v - 0.06) / 0.035) ** 2) * 0.9)         # the polished rim (V = 0 is the rim)
    rgb = mix(rgb, BLACK, np.exp(-((v - 0.115) / 0.014) ** 2) * 0.6)
    sc = np.abs(np.sin(u * math.pi * 6))                                      # a row of scallops
    rgb = mix(rgb, BLACK, np.exp(-((v - (0.26 + 0.07 * sc)) / 0.014) ** 2) * 0.55)
    rgb = mix(rgb, GOLD_HI, np.exp(-((v - (0.29 + 0.07 * sc)) / 0.012) ** 2) * 0.55)
    rgb = mix(rgb, BLACK, np.exp(-((v - 0.62) / 0.012) ** 2) * 0.45)
    return np.clip(rgb, 0, 1), solid()


def cell_hot():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    t = np.clip(1.0 - r, 0, 1) ** 0.7
    rgb = ramp(0.6 + 0.4 * t, [(0.0, WHITE_GOLD), (0.6, (255, 246, 214)), (1.0, (255, 255, 255))])
    return np.clip(rgb, 0, 1), solid()


def voronoi_cracks(seed, n_pts=11, width=0.045):
    rs = np.random.RandomState(seed)
    pts = rs.rand(n_pts, 2).astype(np.float32)
    u, v = grid01()
    d = []
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for p in pts:
                d.append(np.sqrt((u - (p[0] + dx)) ** 2 + (v - (p[1] + dy)) ** 2))
    d = np.sort(np.stack(d, 0), 0)
    return np.exp(-((d[1] - d[0]) / width) ** 2)


def cell_shard():
    n = noise(71, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    h = (n - 0.5) * 0.2 + bevel(0.08) * 0.3
    rgb = ramp(lit(h, k=6.0, ambient=0.5), STONE_RAMP) * (0.9 + 0.2 * n[..., None])
    wide = voronoi_cracks(9, 10, 0.09)
    thin = voronoi_cracks(9, 10, 0.028)
    rgb = mix(rgb, GOLD_MID, wide * 0.6)
    rgb = mix(rgb, GOLD_LT, thin * 0.95)
    rgb = mix(rgb, (255, 255, 250), np.clip(thin - 0.6, 0, 1) * 2.0)
    return np.clip(rgb, 0, 1), solid()


def cell_blade_face():
    u, v = grid01()
    streak = noise(81, cells=(48, 24, 12), gains=(0.5, 0.3, 0.2), aniso=(1, 6))
    diag = 0.5 + 0.5 * np.sin((u * 0.55 + v) * 70.0 + streak * 4.0)
    h = bevel(0.09) * 0.5 + (streak - 0.5) * 0.10 + diag * 0.03
    rgb = ramp(lit(h, k=7.0, ambient=0.32), [(0.0, (40, 48, 66)), (0.5, (128, 146, 172)), (0.9, STEEL_LT), (1.0, (250, 252, 255))])
    for off in (4.0, 8.0):                                                      # hairline black rules along the edges
        rgb = mix(rgb, BLACK, border_line(off, 1.1) * (0.8 if off < 5 else 0.45))
    rgb = mix(rgb, GOLD_LT, border_line(6.0, 1.2) * 0.5)
    for cy in (0.13, 0.87):                                                     # gold chevrons top and bottom
        for k in range(3):
            line = np.abs((np.abs(u - 0.5) * 0.9 + (v - cy) * (1 if cy < 0.5 else -1) + k * 0.045 - 0.0))
            m = np.exp(-(line / 0.012) ** 2) * smooth(0.08, 0.14, u) * smooth(0.08, 0.14, 1 - u) * (np.abs(v - cy) < 0.09)
            rgb = mix(rgb, GOLD_LT, m * 0.7)
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2)                                # an engraved balance in a ring
    rgb = mix(rgb, GOLD_MID, np.exp(-((r - 0.2) / 0.012) ** 2) * 0.7)
    rgb = mix(rgb, GOLD_MID, line_u(u, 0.5, 0.008) * (np.abs(v - 0.5) < 0.17) * 0.7)
    rgb = mix(rgb, GOLD_MID, line_u(v, 0.4, 0.008) * (np.abs(u - 0.5) < 0.15) * 0.7)
    return np.clip(rgb, 0, 1), solid()


def cell_gold_dark():
    u, v = grid01()
    h = bevel(0.10) * 0.6 + (noise(91) - 0.5) * 0.08
    rgb = ramp(lit(h, ambient=0.28), [(0.0, (46, 32, 14)), (0.45, (140, 108, 54)), (0.85, (214, 176, 96)), (1.0, GOLD_LT)])
    band = (np.sin((u + v) * math.pi * 7.0) > 0.45).astype(np.float32)           # dark wrap bands
    band = np.asarray(Image.fromarray((band * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8)), dtype=np.float32) / 255.0
    rgb = mix(rgb, (24, 18, 12), band * 0.7)
    rgb = mix(rgb, GOLD_LT, border_line(3.0, 1.4) * 0.5)
    return np.clip(rgb, 0, 1), solid()


def cell_feather():
    ss = 3
    W = S * ss
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    u, v = (xx + 0.5) / W, (yy + 0.5) / W
    t = np.clip((0.97 - v) / 0.94, 0, 1)                                      # 0 at the base (bottom), 1 at the tip
    half = 0.5 * 0.82 * np.clip(np.sin(math.pi * np.clip(t, 0, 1) ** 0.75), 0, 1) ** 0.85 * (1 - 0.12 * t)
    cx = 0.5 + 0.045 * np.sin(t * math.pi)
    dx = u - cx
    inside = (np.abs(dx) < half) & (v > 0.03) & (v < 0.97)
    edge = np.clip((half - np.abs(dx)) * W / 1.6, 0, 1)
    alpha = np.where((v > 0.03) & (v < 0.97), edge, 0.0)
    fold = np.clip(np.abs(dx) / np.maximum(half, 1e-3), 0, 1)
    barbs = 0.5 + 0.5 * np.sin((t * 34.0 - np.abs(dx) * 38.0) * math.pi)
    shaft = np.exp(-(np.abs(dx) / 0.011) ** 2)
    tt = 0.62 + 0.25 * (1 - fold) - 0.10 * barbs * (fold < 0.96) + 0.25 * shaft
    rgb = ramp(np.clip(tt, 0, 1), [(0.0, (150, 130, 96)), (0.55, WHITE_GOLD), (0.85, (255, 244, 214)), (1.0, (255, 255, 250))])
    rim = np.clip(1 - np.clip((half - np.abs(dx)) * W / 3.2, 0, 1), 0, 1) * inside
    rgb = rgb * (1 - rim[..., None] * 0.55) + col(GOLD_MID)[None, None, :] * (rim[..., None] * 0.55)
    rgb = np.where(inside[..., None], rgb, col(GOLD_MID)[None, None, :])
    rgb = rgb.reshape(S, ss, S, ss, 3).mean((1, 3))
    alpha = alpha.reshape(S, ss, S, ss).mean((1, 3))
    return np.clip(rgb, 0, 1), np.clip(alpha, 0, 1)


def cell_fuller():
    u, v = grid01()
    c = np.abs(u - 0.5) * 2
    core = np.exp(-(c / 0.35) ** 2)
    rgb = ramp(0.45 + 0.55 * core, [(0.0, GOLD_MID), (0.6, WHITE_GOLD), (1.0, (255, 255, 250))])
    rgb = mix(rgb, GOLD_SH, smooth(0.62, 1.0, c) * 0.75)                      # darker groove walls
    rgb = rgb * (0.94 + 0.06 * np.sin(v * 40.0))[..., None]
    return np.clip(rgb, 0, 1), solid()


CELLS = [cell_gold, cell_steel, cell_edge, cell_stone, cell_stone_dark, cell_glyph_a, cell_glyph_b, cell_crest, cell_chain, cell_pan, cell_hot, cell_shard, cell_blade_face, cell_gold_dark, cell_feather, cell_fuller]


def tex_atlas():
    P = np.zeros((4 * S, 4 * S, 3), np.float32)
    A = np.zeros((4 * S, 4 * S), np.float32)
    for i, fn in enumerate(CELLS):
        rgb, a = fn()
        cx, cy = i % 4, i // 4
        P[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = rgb
        A[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = a
    # a cut-out keeps a gold colour under its clear pixels (no black fringe in the mipmaps), a solid cell never has black
    clear = A <= 0.0
    P[clear] = col(GOLD_MID)
    px = np.dstack([np.clip(P, 0, 1), A])
    px = (px * 255 + 0.5).astype(np.uint8)
    im = Image.fromarray(px, "RGBA")
    os.makedirs(j.OUT, exist_ok=True)
    im.save(os.path.join(j.OUT, "judgment_model_atlas.png"), optimize=True)
    print("wrote judgment_model_atlas", im.size)


ALL = {"atlas": tex_atlas}

if __name__ == "__main__":
    for name in (sys.argv[1:] or list(ALL)):
        ALL[name]()

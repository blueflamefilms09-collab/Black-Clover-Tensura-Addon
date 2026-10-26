"""Generates the material atlas of the 3D models of "Israfil, Lord of Trials" (3.21.0: vfx.client.layer.IsrafilModels / IsrafilMeshDraw / TrialLayer).

Deterministic and font-free (every machine writes the same pixels). Written for this mod, same palette as tools/gen_trial_vfx_textures.py (imported, not changed): deep indigo and midnight teal, cyan-white
edges and a little brass. It only ADDS one file; the 18 flat textures of gen_trial_vfx_textures.py stay and are still drawn when the setting `israfilModels` is off.
Output: src/main/resources/assets/nusmp/textures/particle/israfil_model_atlas.png

    python tools/gen_israfil_model_textures.py

    israfil_model_atlas     4 x 4 cells of 128 px (512 x 512), alpha-blended, lit from the upper left and shaded again by the layer per face:
        0 engraved brass plate with cyan sound-arc inlay   1 dark brass with ridges and rivets      2 polished gold band with cyan gems         3 bell brass with a fan of engraved arcs
        4 the glowing mouth of the horn (cut-out disc)     5 indigo slab top with a coordinate engraving (ticks, crosshair)                         6 indigo slab side with a brass trim
        7 maze relief (a raised maze on indigo stone)      8 wall cap: stone with a brass border    9 a chain with tally marks (cut-out, drawn in the middle third of the cell)
        10 a glowing rune tile                             11 gate frame: stone with a cyan inlay line and ticks       12 glass crystal facet (teal)  13 glass crystal facet (violet)
        14 dark core facet with bright edges               15 an angel's feather (cut-out)
    The cells 12, 13 and 14 are TRIANGLE cells: a triangle face draws the lower right half of its cell (bottom left, bottom right, top right corner), so their art lives there.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_trial_vfx_textures as T   # the palette and the maze builder of the flat textures (imported, not changed)

S = 128
SS = 4
OUT = T.OUT


def col(c):
    return np.array(c, np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grid01():
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    return (xx + 0.5) / S, (yy + 0.5) / S


def vnoise(cells, seed):
    rs = np.random.RandomState(seed)
    g = rs.rand(cells + 1, cells + 1).astype(np.float32)
    im = Image.fromarray(g, "F").resize((S, S), Image.BICUBIC)
    return np.asarray(im, np.float32).clip(0, 1)


def fbm(seed, cells=(32, 16, 8, 4), gains=(0.4, 0.3, 0.2, 0.1)):
    out = np.zeros((S, S), np.float32)
    for i, (c, g) in enumerate(zip(cells, gains)):
        out += vnoise(c, seed + 31 * i) * g
    return out / sum(gains)


class Mask:
    """Line art drawn at SS x and brought down to the cell size."""

    def __init__(self):
        self.im = Image.new("L", (S * SS, S * SS), 0)
        self.d = ImageDraw.Draw(self.im)

    def line(self, pts, w=1.5, v=255):
        self.d.line([(x * SS, y * SS) for x, y in pts], fill=v, width=max(1, int(w * SS)), joint="curve")

    def rect(self, x0, y0, x1, y1, v=255, outline=None, w=1.0):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS, y1 * SS], fill=v if outline is None else None, outline=outline, width=max(1, int(w * SS)))

    def poly(self, pts, v=255):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=v)

    def circle(self, cx, cy, r, v=255, outline=False, w=1.0):
        box = [(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS]
        if outline:
            self.d.ellipse(box, outline=v, width=max(1, int(w * SS)))
        else:
            self.d.ellipse(box, fill=v)

    def arc(self, cx, cy, r, a0, a1, w=1.5, v=255):
        self.d.arc([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], a0, a1, fill=v, width=max(1, int(w * SS)))

    def final(self):
        return np.asarray(self.im.resize((S, S), Image.LANCZOS), np.float32).clip(0, 255) / 255.0


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255.0


def edge_dist():
    u, v = grid01()
    return np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))


def bevel(width=0.14):
    return smooth(0.0, width, edge_dist())


def ramp_of(stops):
    def f(t):
        t = np.clip(t, 0, 1)
        out = np.zeros(t.shape + (3,), np.float32)
        for (t0, c0), (t1, c1) in zip(stops[:-1], stops[1:]):
            m = (t >= t0) & (t <= t1)
            u = ((t - t0) / (t1 - t0))[..., None]
            out = np.where(m[..., None], col(c0) * (1 - u) + col(c1) * u, out)
        return out
    return f


BRASS_R = ramp_of([(0.0, (38, 24, 10)), (0.25, (104, 66, 22)), (0.5, (186, 134, 50)), (0.75, (238, 196, 104)), (1.0, (255, 238, 184))])
STONE_R = ramp_of([(0.0, (6, 6, 22)), (0.35, (20, 20, 62)), (0.7, (44, 48, 118)), (1.0, (112, 134, 206))])
ICE, CYAN, TEAL, INDIGO, DEEP, VIOLET, BRASS, BRASS_HI = T.ICE, T.CYAN, T.TEAL, T.INDIGO, T.DEEP, T.VIOLET, T.BRASS, T.BRASS_HI


def lit(height, ramp, k=9.0, ambient=0.22, spec_pow=22.0, spec_gain=0.45, light=(-0.55, -0.62, 0.56)):
    gy, gx = np.gradient(height)
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(height)
    n = np.sqrt(nx * nx + ny * ny + nz * nz)
    L = np.array(light, np.float32)
    L /= np.linalg.norm(L)
    lam = np.clip((nx * L[0] + ny * L[1] + nz * L[2]) / n, 0, 1)
    t = ambient + (1 - ambient) * lam
    spec = lam ** spec_pow
    return ramp(t) + col(ICE)[None, None, :] * (spec * spec_gain)[..., None]


def mix(rgb, c, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return rgb * (1 - a) + col(c)[None, None, :] * a


def glow_lines(rgb, mask, c=CYAN, core=ICE, halo=0.55, r=2.2):
    """Emissive line art: a coloured halo under a bright core."""
    return mix(mix(rgb, c, blur(mask, r) * halo * 1.6), core, mask)


def opaque():
    return np.ones((S, S), np.float32)


def rivets(rgb, pts, r=3.0, c=BRASS_HI):
    xx, yy = np.meshgrid(np.arange(S, dtype=np.float32) + 0.5, np.arange(S, dtype=np.float32) + 0.5)
    for (px, py) in pts:
        d = np.sqrt((xx - px) ** 2 + (yy - py) ** 2)
        m = smooth(r, r - 1.2, d)
        hi = np.exp(-(((xx - px + 1.0) ** 2 + (yy - py + 1.0) ** 2) / (r * 0.4) ** 2))
        rgb = mix(rgb, c, m * 0.85)
        rgb = np.clip(rgb + col(ICE)[None, None, :] * (hi * m * 0.7)[..., None], 0, 1)
        rgb = rgb * (1 - 0.4 * (smooth(r + 1.5, r, d) * (1 - m))[..., None])
    return rgb


# ---------------------------------------------------------------------------------------------------- the cells
def brass_base(seed, dark=1.0, bevel_w=0.12, hammer=0.08):
    h = bevel(bevel_w) * 0.7 + (fbm(seed) - 0.5) * hammer
    return h, np.clip(lit(h, BRASS_R, k=9.0, ambient=0.2) * dark, 0, 1)


def cell_brass():
    h, rgb = brass_base(3)
    u, v = grid01()
    m = Mask()
    for k in range(3):                                  # engraved sound arcs running along the plate, with a cyan inlay in the middle one
        m.arc(0.1 * S, 0.5 * S, (0.36 + 0.16 * k) * S, -50, 50, 1.4, 255 if k == 1 else 150)
    arcs = m.final()
    rgb = rgb * (1 - 0.5 * blur(arcs, 1.0)[..., None])
    m2 = Mask()
    m2.arc(0.1 * S, 0.5 * S, 0.52 * S, -50, 50, 1.6)
    rgb = glow_lines(rgb, m2.final() * 0.9, CYAN, ICE, 0.4, 1.8)
    edge = np.exp(-((np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v)) * S - 3.4) / 1.3) ** 2)
    rgb = mix(rgb, BRASS_HI, edge * 0.7)
    rgb = rivets(rgb, [(14, 14), (S - 14, 14), (14, S - 14), (S - 14, S - 14)], 2.8)
    return np.clip(rgb, 0, 1), opaque()


def cell_brass_dark():
    h, rgb = brass_base(11, dark=0.62, bevel_w=0.10)
    u, v = grid01()
    ridge = np.exp(-((v - 0.5) / 0.05) ** 2) * 0.8 + np.exp(-((v - 0.2) / 0.02) ** 2) * 0.4 + np.exp(-((v - 0.8) / 0.02) ** 2) * 0.4
    rgb = np.clip(rgb + col(BRASS)[None, None, :] * ridge[..., None] * 0.55, 0, 1)
    rgb = rgb * (0.65 + 0.35 * bevel(0.10)[..., None])
    rgb = rivets(rgb, [(16, 64), (S - 16, 64)], 3.0, BRASS)
    return np.clip(rgb, 0, 1), opaque()


def cell_band():
    h, rgb = brass_base(21, dark=1.18, bevel_w=0.14, hammer=0.04)
    u, v = grid01()
    m = Mask()
    for k in range(8):                                  # a zigzag of engraved chevrons
        x0 = k / 8 * S
        m.line([(x0, 0.64 * S), (x0 + S / 16, 0.34 * S), (x0 + S / 8, 0.64 * S)], 1.6)
    chev = m.final()
    rgb = rgb * (1 - 0.55 * blur(chev, 0.8)[..., None])
    rgb = mix(rgb, BRASS_HI, np.clip(chev - 0.2, 0, 1) * 0.5)
    gem = np.zeros((S, S), np.float32)
    for gx in (0.25, 0.5, 0.75):
        gem = np.maximum(gem, smooth(5.5, 3.5, np.sqrt((u * S - gx * S) ** 2 + (v * S - 0.84 * S) ** 2)))
    rgb = glow_lines(rgb, gem, CYAN, ICE, 0.5, 2.4)
    for yy in (0.1, 0.9):
        rgb = mix(rgb, BRASS_HI, np.exp(-((v - yy) / 0.012) ** 2) * 0.8)
    return np.clip(rgb, 0, 1), opaque()


def cell_bell():
    h, rgb = brass_base(31, dark=1.05, bevel_w=0.12, hammer=0.06)
    u, v = grid01()
    m = Mask()
    for k in range(5):                                  # a fan of engraved arches that opens toward the lip (the top of the cell is the rim end)
        r = (0.18 + 0.12 * k) * S
        m.arc(0.5 * S, 1.15 * S, r, 215, 325, 1.3, 255 if k % 2 == 0 else 160)
    arcs = m.final()
    rgb = rgb * (1 - 0.45 * blur(arcs, 0.9)[..., None])
    rgb = glow_lines(rgb, np.clip(arcs - 0.3, 0, 1) * 1.4, CYAN, ICE, 0.35, 1.8)
    rgb = mix(rgb, BRASS_HI, np.exp(-((edge_dist() * S - 3.2) / 1.2) ** 2) * 0.55)
    return np.clip(rgb, 0, 1), opaque()


def cell_mouth():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    core = np.clip(1 - r * 1.05, 0, 1) ** 0.8
    rgb = np.zeros((S, S, 3), np.float32)
    rgb += col(CYAN)[None, None, :] * (1 - core[..., None]) + col((255, 255, 255))[None, None, :] * core[..., None]
    swirl = 0.5 + 0.5 * np.sin(np.arctan2(v - 0.5, u - 0.5) * 6 + r * 9)
    rgb = mix(rgb, ICE, swirl * core * 0.35)
    ringl = np.exp(-((r - 0.9) / 0.05) ** 2)
    rgb = mix(rgb, BRASS_HI, ringl * 0.6)
    a = np.clip(smooth(1.0, 0.86, r) * (0.85 + 0.3 * core), 0, 1)
    return np.clip(rgb, 0, 1), a


def cell_slab_top():
    h = bevel(0.10) * 0.6 + (fbm(41) - 0.5) * 0.07
    rgb = lit(h, STONE_R, k=8.0, ambient=0.28, spec_gain=0.25)
    u, v = grid01()
    m = Mask()
    m.rect(0.08 * S, 0.08 * S, 0.92 * S, 0.92 * S, outline=255, w=1.4)
    for k in range(1, 12):                              # a ruler along the outer edge (the bottom of the cell) and along the sides
        x = (0.08 + 0.84 * k / 12) * S
        ln = 0.13 if k % 4 == 0 else 0.07
        m.line([(x, 0.92 * S), (x, (0.92 - ln) * S)], 1.2)
        m.line([(0.08 * S, x), ((0.08 + ln * 0.7) * S, x)], 1.0, 200)
        m.line([(0.92 * S, x), ((0.92 - ln * 0.7) * S, x)], 1.0, 200)
    m.line([(0.5 * S, 0.2 * S), (0.5 * S, 0.8 * S)], 1.2, 210)       # a crosshair with a gap in the middle
    m.line([(0.2 * S, 0.5 * S), (0.8 * S, 0.5 * S)], 1.2, 210)
    m.circle(0.5 * S, 0.5 * S, 7, 255, True, 1.4)
    m.circle(0.5 * S, 0.5 * S, 2.0)
    for gx in (0.3, 0.7):
        for gy in (0.3, 0.7):
            m.circle(gx * S, gy * S, 1.6, 200)
    lines = m.final()
    rgb = rgb * (1 - 0.35 * blur(lines, 1.2)[..., None])
    rgb = glow_lines(rgb, lines, CYAN, ICE, 0.5, 2.0)
    rgb = mix(rgb, BRASS, np.exp(-((edge_dist() * S - 2.2) / 1.0) ** 2) * 0.8)
    return np.clip(rgb, 0, 1), opaque()


def cell_slab_side():
    h = bevel(0.12) * 0.6 + (fbm(47) - 0.5) * 0.08
    rgb = lit(h, STONE_R, k=8.0, ambient=0.24, spec_gain=0.2)
    u, v = grid01()
    rgb = mix(rgb, BRASS, np.exp(-((v - 0.16) / 0.035) ** 2) * 0.85)           # a brass trim band along the top
    rgb = mix(rgb, BRASS_HI, np.exp(-((v - 0.16) / 0.012) ** 2) * 0.9)
    m = Mask()
    for k in range(1, 8):
        x = k / 8 * S
        m.line([(x, 0.34 * S), (x, (0.34 + (0.22 if k % 2 == 0 else 0.12)) * S)], 1.2)
    ticks = m.final()
    rgb = glow_lines(rgb, ticks * 0.9, TEAL, CYAN, 0.35, 1.6)
    rgb = rgb * (0.7 + 0.3 * bevel(0.12)[..., None])
    return np.clip(rgb, 0, 1), opaque()


def cell_maze():
    rng = np.random.default_rng(404)
    cols_, rows_ = 7, 7
    segs = T.rect_maze(rng, cols_, rows_)
    pad = 0.09
    cw = (1 - 2 * pad) * S / cols_
    m = Mask()
    for (x0, y0), (x1, y1) in segs:
        m.line([(pad * S + x0 * cw, pad * S + y0 * cw), (pad * S + x1 * cw, pad * S + y1 * cw)], 5.2)
    m.rect(pad * S, pad * S, (1 - pad) * S, (1 - pad) * S, outline=255, w=5.2)
    walls = m.final()
    h = np.clip(blur(walls, 1.3), 0, 1) * 0.9 + bevel(0.06) * 0.2 + (fbm(53) - 0.5) * 0.05
    rgb = lit(h, STONE_R, k=14.0, ambient=0.2, spec_gain=0.3)
    floor = 1 - np.clip(walls * 1.5, 0, 1)
    rgb = mix(rgb, DEEP, floor * 0.35)
    top = np.clip(walls - blur(walls, 3.0) * 0.6, 0, 1)
    rgb = glow_lines(rgb, top * 0.75, TEAL, CYAN, 0.4, 2.0)                    # the raised walls carry a cyan-teal edge light
    rgb = mix(rgb, BRASS_HI, np.exp(-((edge_dist() * S - 2.6) / 1.1) ** 2) * 0.5)
    return np.clip(rgb, 0, 1), opaque()


def cell_wall_top():
    h = bevel(0.10) * 0.7 + (fbm(59) - 0.5) * 0.07
    rgb = lit(h, STONE_R, k=8.0, ambient=0.3, spec_gain=0.3)
    d = edge_dist() * S
    rgb = mix(rgb, BRASS, np.exp(-((d - 5.0) / 2.0) ** 2) * 0.9)
    rgb = mix(rgb, BRASS_HI, np.exp(-((d - 5.0) / 0.8) ** 2) * 0.9)
    u, v = grid01()
    c = np.exp(-(((u - 0.5) ** 2 + (v - 0.5) ** 2) / 0.006))
    m = Mask()
    m.circle(0.5 * S, 0.5 * S, 12, 255, True, 1.3)
    m.line([(0.5 * S - 18, 0.5 * S), (0.5 * S + 18, 0.5 * S)], 1.2)
    m.line([(0.5 * S, 0.5 * S - 18), (0.5 * S, 0.5 * S + 18)], 1.2)
    rgb = glow_lines(rgb, m.final() * 0.9 + c * 0.4, CYAN, ICE, 0.5, 2.0)
    return np.clip(rgb, 0, 1), opaque()


def cell_chain():
    """A chain in the middle third of the cell (the layer crops u to 0.31 .. 0.69): upright links and edge-on links, tally marks on the sides."""
    m = Mask()
    cx = S * 0.5
    pitch = S / 6.0
    for k in range(6):
        y0 = k * pitch
        if k % 2 == 0:
            m.d.rounded_rectangle([(cx - S * 0.125) * SS, (y0 - pitch * 0.22) * SS, (cx + S * 0.125) * SS, (y0 + pitch * 1.22) * SS], radius=S * 0.125 * SS, outline=255, width=int(4.2 * SS))
        else:
            m.d.rounded_rectangle([(cx - S * 0.045) * SS, (y0 - pitch * 0.15) * SS, (cx + S * 0.045) * SS, (y0 + pitch * 1.15) * SS], radius=S * 0.04 * SS, fill=255)
    links = m.final()
    mt = Mask()
    for k, yc in enumerate((0.1, 0.4, 0.7, 0.95)):
        side = 1 if k % 2 == 0 else -1
        bx = cx + side * S * 0.155
        for s in range(3):
            sx = bx + (s - 1) * 3.2
            mt.line([(sx, yc * S - 4), (sx, yc * S + 4)], 1.3)
    tally = mt.final()
    alpha = np.clip(np.maximum(links, tally * 0.9) + blur(links, 1.6) * 0.5, 0, 1)
    u, v = grid01()
    sh = 0.5 + 0.5 * np.sin(v * 18 + u * 5)
    rgb = col(TEAL)[None, None, :] * 0.6 * (1 - links[..., None]) + (col(ICE)[None, None, :] * (0.7 + 0.3 * sh[..., None])) * links[..., None]
    rgb = mix(rgb, BRASS_HI, np.clip(tally * 1.3, 0, 1))
    return np.clip(rgb, 0, 1), alpha


def glyph_mask(seed=5):
    m = Mask()
    c = S / 2
    m.circle(c, c, 41, 255, True, 2.2)
    m.circle(c, c, 30, 200, True, 1.4)
    for k in range(6):
        a = math.radians(60 * k + 30)
        m.line([(c + math.cos(a) * 30, c + math.sin(a) * 30), (c + math.cos(a) * 41, c + math.sin(a) * 41)], 2.0)
    m.poly([(c, c - 22), (c + 19, c + 11), (c - 19, c + 11)], 0)
    m.line([(c, c - 24), (c + 21, c + 12), (c - 21, c + 12), (c, c - 24)], 2.2)
    m.line([(c, c + 24), (c + 21, c - 12), (c - 21, c - 12), (c, c + 24)], 1.6, 190)
    m.circle(c, c, 4.0)
    return m.final()


def cell_rune():
    h = bevel(0.12) * 0.5
    rgb = lit(h, STONE_R, k=8.0, ambient=0.3, spec_gain=0.1) * 0.8
    g = glyph_mask()
    rgb = mix(rgb, DEEP, 0.4)
    rgb = glow_lines(rgb, g, CYAN, (255, 255, 255), 0.9, 3.4)
    u, v = grid01()
    c = np.exp(-(((u - 0.5) ** 2 + (v - 0.5) ** 2) / 0.05)) * 0.35
    rgb = mix(rgb, ICE, c)
    rgb = mix(rgb, BRASS, np.exp(-((edge_dist() * S - 3.0) / 1.3) ** 2) * 0.9)
    return np.clip(rgb, 0, 1), opaque()


def cell_gate():
    """The arch strip: u runs along the arch (stretched), v across it. A cyan inlay line down the middle with ticks, brass edges, an indigo stone ground."""
    h = bevel(0.08) * 0.5 + (fbm(67) - 0.5) * 0.06
    rgb = lit(h, STONE_R, k=8.0, ambient=0.3, spec_gain=0.2)
    u, v = grid01()
    rgb = mix(rgb, BRASS, np.exp(-((v - 0.1) / 0.05) ** 2) * 0.9)
    rgb = mix(rgb, BRASS, np.exp(-((v - 0.9) / 0.05) ** 2) * 0.9)
    rgb = mix(rgb, BRASS_HI, (np.exp(-((v - 0.1) / 0.016) ** 2) + np.exp(-((v - 0.9) / 0.016) ** 2)) * 0.8)
    inlay = np.exp(-((v - 0.5) / 0.07) ** 2)
    ticks = np.zeros((S, S), np.float32)
    for k in range(10):
        ticks = np.maximum(ticks, np.exp(-((u - (k + 0.5) / 10) / 0.012) ** 2) * smooth(0.34, 0.26, np.abs(v - 0.5)) * (1 if k % 3 else 1.5))
    rgb = glow_lines(rgb, np.clip(inlay * 0.9 + ticks * 0.8, 0, 1), CYAN, ICE, 0.6, 2.0)
    return np.clip(rgb, 0, 1), opaque()


def tri_coords():
    """Edge distances of the lower right triangle (bottom left, bottom right, top right) in 0..~0.5, and the barycentric-like position."""
    u, v = grid01()
    e_bottom = 1 - v
    e_right = 1 - u
    e_diag = (u + v - 1) * 0.7071
    return u, v, e_bottom, e_right, e_diag


def crystal(seed, c_dark, c_mid, c_edge):
    u, v, eb, er, ed = tri_coords()
    m = np.minimum(np.minimum(eb, er), ed * 1.4142)         # the distance to the nearest edge in cell units (the diagonal edge is scaled back)
    inside = (u + v >= 1.0)
    n = fbm(seed, cells=(16, 8, 4), gains=(0.4, 0.4, 0.2))
    t = np.clip(m * 2.6, 0, 1)                               # 0 at an edge, 1 in the middle
    # facet lines inside: three lines from the corners to the centroid
    cu, cv = 2 / 3, 2 / 3                                    # the centroid of (0,1) (1,1) (1,0) is (2/3, 2/3)
    fac = np.zeros((S, S), np.float32)
    for (px, py) in ((0.0, 1.0), (1.0, 1.0), (1.0, 0.0)):
        dx, dy = cu - px, cv - py
        ln = math.hypot(dx, dy)
        dist = np.abs((u - px) * dy - (v - py) * dx) / ln
        along = ((u - px) * dx + (v - py) * dy) / ln
        fac = np.maximum(fac, np.exp(-(dist / 0.012) ** 2) * smooth(0.0, 0.1, along) * smooth(ln, ln * 0.7, along))
    base = col(c_dark)[None, None, :] * (1 - t[..., None]) * 0.0 + col(c_mid)[None, None, :] * (0.55 + 0.45 * n[..., None])
    rgb = base * (0.55 + 0.45 * t[..., None]) + col(c_dark)[None, None, :] * (0.35 * (1 - t[..., None]))
    g = np.clip(1.2 - np.sqrt((u - 0.8) ** 2 + (v - 0.55) ** 2) * 2.2, 0, 1)              # a bright core toward the right
    rgb = np.clip(rgb + col(c_edge)[None, None, :] * (g[..., None] * 0.55), 0, 1)
    edge = np.exp(-(m / 0.022) ** 2)
    rgb = mix(rgb, c_edge, edge * 0.95)
    rgb = mix(rgb, (255, 255, 255), fac * 0.5)
    alpha = np.clip(0.72 + 0.28 * (1 - t) + 0.12 * g, 0, 1)
    out_ = ~inside                                           # the unused half: the edge colour, so a filtered sample on the diagonal edge stays clean
    rgb = np.where(out_[..., None], col(c_edge)[None, None, :] * 0.9, rgb)
    alpha = np.where(out_, 0.95, alpha)
    return np.clip(rgb, 0, 1), alpha


def cell_crystal_a():
    return crystal(71, (14, 70, 96), (60, 190, 215), (214, 252, 255))


def cell_crystal_b():
    return crystal(79, (36, 24, 100), (110, 92, 220), (200, 190, 255))


def cell_core():
    u, v, eb, er, ed = tri_coords()
    m = np.minimum(np.minimum(eb, er), ed * 1.4142)
    inside = (u + v >= 1.0)
    n = fbm(83, cells=(16, 8, 4), gains=(0.4, 0.4, 0.2))
    rgb = col((8, 5, 26))[None, None, :] * (0.8 + 0.5 * n[..., None])
    sw = 0.5 + 0.5 * np.sin((u * 5.0 + v * 3.0) * math.pi * 2 + n * 4)
    rgb = mix(rgb, (28, 14, 72), sw * 0.5)
    rgb = mix(rgb, DEEP, np.exp(-(m / 0.12) ** 2) * 0.55)
    edge = np.exp(-(m / 0.024) ** 2)
    halo = np.exp(-(m / 0.07) ** 2)
    rgb = mix(rgb, TEAL, halo * 0.55)
    rgb = mix(rgb, CYAN, edge * 0.9)
    rgb = mix(rgb, ICE, np.clip(edge - 0.4, 0, 1) * 1.4)
    rgb = np.where((~inside)[..., None], col(CYAN)[None, None, :] * 0.7, rgb)
    return np.clip(rgb, 0, 1), opaque()


def cell_feather():
    """An angel's feather: base at the bottom, tip at the top, ice white with a brass quill and cyan barbs; a cut-out."""
    ss = 3
    W = S * ss
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    u = (xx + 0.5) / W
    v = (yy + 0.5) / W
    t = np.clip((1.0 - v - 0.02) / 0.96, 0, 1)               # 0 at the base, 1 at the tip
    halfw = 0.5 * 0.80 * np.sin(math.pi * t ** 0.7) * (1 - 0.2 * t) + 0.02
    cx = 0.5 + 0.06 * np.sin(t * math.pi)
    dx = u - cx
    inside = (np.abs(dx) < halfw) & (v > 0.02) & (v < 0.98)
    edge = np.clip((halfw - np.abs(dx)) * W / 1.8, 0, 1)
    alpha = np.where((v > 0.02) & (v < 0.98), edge, 0.0)
    fold = np.clip(np.abs(dx) / np.maximum(halfw, 1e-3), 0, 1)
    barbs = 0.5 + 0.5 * np.sin((t * 26.0 - np.abs(dx) * 22.0) * math.pi)
    quill = np.exp(-((np.abs(dx) / 0.011)) ** 2)
    base = col(ICE)[None, None, :] * (0.85 - 0.2 * fold[..., None]) + col(CYAN)[None, None, :] * (0.25 * barbs[..., None] * (1 - fold[..., None]))
    rgb = base * (1 - quill[..., None]) + col(BRASS_HI)[None, None, :] * quill[..., None]
    rgb = mix(rgb, CYAN, np.clip((fold - 0.8) * 4, 0, 1) * 0.5)
    rgb = np.where(inside[..., None], rgb, col(CYAN)[None, None, :])
    rgb = rgb.reshape(S, ss, S, ss, 3).mean((1, 3))
    alpha = alpha.reshape(S, ss, S, ss).mean((1, 3))
    return np.clip(rgb, 0, 1), np.clip(alpha, 0, 1)


CELLS = [cell_brass, cell_brass_dark, cell_band, cell_bell, cell_mouth, cell_slab_top, cell_slab_side, cell_maze, cell_wall_top, cell_chain, cell_rune, cell_gate, cell_crystal_a, cell_crystal_b, cell_core, cell_feather]


def tex_atlas():
    P = np.zeros((4 * S, 4 * S, 3), np.float32)
    A = np.zeros((4 * S, 4 * S), np.float32)
    for i, fn in enumerate(CELLS):
        rgb, a = fn()
        cx, cy = i % 4, i // 4
        P[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = rgb
        A[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = a
    T.save(P, A, "israfil_model_atlas")


ALL = {"atlas": tex_atlas}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()

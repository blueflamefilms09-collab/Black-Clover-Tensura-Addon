"""Materials, metal plates, gems and the five rarity trim atlases of the 3.26.0 Skill Book (painting code; the driver is gen_book_textures_v2.py).

The painting method (book_paint_v2.py): a height map per material, lit from the top left, ornaments as separate raised layers with their own bevel and drop shadow, emissive layers
for runes and cracks, wear along the edges, stitches, rivets. Deterministic: seeded generators only.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
sys.path.insert(0, HERE)
import book_model_v2 as BM  # noqa: E402
import book_paint_v2 as P  # noqa: E402
from lines_lib import FRANCHISES  # noqa: E402

TEX_DIR = os.path.join(ASSETS, "textures", "item", "skill_book_v2")
ICON_DIR = os.path.join(ASSETS, "textures", "item", "skill_book_icons")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
FRANCHISE_ORDER = list(FRANCHISES.keys())
assert len(FRANCHISE_ORDER) + 1 <= 20, "the item property (index + 1) / 20 must stay at or below 1"

FW, FH = 144, 208                       # the front cover in pixels (9 x 13 units, 16 px a unit)
PANEL2 = (20, 20, 124, 188)             # panel 2 inside the front image (x0, y0, x1, y1)
HOLLOW = (72, 120)                      # the centre of the motif: the rarity gems sit here (px of the front image)
PLAQUE_PX = (72, 60)                    # the tinted emblem plaque
LOW_PX = (72, 166)                      # the franchise accent at the foot


def rgbimg(a):
    return a


# =========================================================================================== materials
def mat_leather(h, w, rng, grain=1.0):
    """Hand-tooled leather: broad soft undulation, a fine low grain (kept quiet so the embossing reads), a few long creases."""
    n1 = P.fbm(h, w, rng, (40, 20, 10), 0.6)
    g = rng.random((h, w)).astype(np.float32)
    g = P.gauss(g, 1.1)
    g = (g - g.mean()) / (g.std() + 1e-6)
    ridge = 1 - np.abs(2 * P.fbm(h, w, rng, (14, 7), 0.5) - 1)
    crease = np.clip((ridge - 0.9) * 6, 0, 1)
    t = 0.52 + 0.12 * (n1 - 0.5) * 2 + 0.035 * g * grain - 0.14 * crease
    H = 0.2 * g * grain - 0.7 * crease + 0.8 * (n1 - 0.5)
    return np.clip(t, 0, 1), H


def mat_cloth(h, w, rng, grain=1.0):
    yy, xx = np.mgrid[0:h, 0:w]
    weave = (((xx // 2) + (yy // 2)) % 2).astype(np.float32)
    thread_x = 0.5 + 0.5 * np.sin(xx * math.pi)
    thread_y = 0.5 + 0.5 * np.sin(yy * math.pi)
    n1 = P.fbm(h, w, rng, (30, 14, 6), 0.6)
    t = 0.5 + 0.09 * (weave - 0.5) + 0.07 * (thread_x * (1 - weave) + thread_y * weave - 0.5) + 0.2 * (n1 - 0.5)
    H = 0.6 * (thread_x * (1 - weave) + thread_y * weave) + 0.5 * (n1 - 0.5)
    return np.clip(t, 0, 1), H


def mat_wood(h, w, rng, grain=1.0, plank=36):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    n = P.value_noise(h, w, rng, 6)
    warp = P.fbm(h, w, rng, (28, 14), 0.5)
    stripes = 0.5 + 0.5 * np.sin((xx * 0.9 + warp * 18 + 3 * n) * 1.2)
    fine = P.gauss(rng.random((h, w)).astype(np.float32), 0.6)
    seam = (np.mod(xx, plank) < 1.4).astype(np.float32)
    knots = P.fbm(h, w, rng, (60, 30), 0.5)
    t = 0.5 + 0.13 * (stripes - 0.5) + 0.08 * (fine - 0.5) * 2 + 0.2 * (knots - 0.5) - 0.28 * seam
    H = 0.5 * stripes + 0.3 * fine - 2.4 * seam
    return np.clip(t, 0, 1), H


def mat_stone(h, w, rng, n=26, grain=1.0):
    d1, d2, idx = P.voronoi(h, w, rng, n)
    edge = d2 - d1
    plate_tone = np.random.default_rng(int(rng.integers(1, 10 ** 6))).random(n).astype(np.float32)[idx]
    rough = P.fbm(h, w, rng, (16, 8, 4), 0.6)
    crack = np.clip(1 - edge / 2.6, 0, 1)
    t = 0.48 + 0.2 * (plate_tone - 0.5) + 0.2 * (rough - 0.5) - 0.34 * crack
    H = np.clip(edge / 5.0, 0, 1) * 3.0 + 0.6 * rough
    return np.clip(t, 0, 1), H, edge


# =========================================================================================== metal and small parts (shared by trims and covers)
def metal_plate(h, w, rng, stops, rim=5.0, rivets=True, engrave=None, streak=0.07, corner_round=0):
    """A bevelled metal plate: raised rim, brushed streaks, optional engraving mask, four rivets. Returns RGB float (h, w, 3)."""
    d = P.edge_dist(h, w)
    prof = np.clip(d / rim, 0, 1)
    prof = prof * prof * (3 - 2 * prof)
    H = prof * 2.4
    st = P.gauss(rng.random((h, w)).astype(np.float32), 0.6)
    st = P.gauss(st, 0.3) + 0.0
    streaks = P.resize_f(rng.random((h, 1 + w // 8)).astype(np.float32), w, h) - 0.5
    if engrave is not None:
        H = H - P.gauss(engrave, 0.7) * 1.6
    if rivets:
        m = P.Mask(w, h)
        for (rx, ry) in ((rim + 3, rim + 3), (w - rim - 3, rim + 3), (rim + 3, h - rim - 3), (w - rim - 3, h - rim - 3)):
            m.ellipse(rx - 2.2, ry - 2.2, rx + 2.2, ry + 2.2)
        H = H + P.gauss(m.get(), 0.7) * 2.0
    L = P.slope_light(H, 1.5)
    t = 0.46 + 0.36 * L + streak * streaks * 2 + 0.05 * (st - 0.5)
    t -= 0.22 * np.clip(P.cavity(H, 2.0) * 0.8, 0, 1)
    t += 0.30 * np.clip(L - 0.45, 0, 1) ** 1.2
    return P.ramp(np.clip(t, 0, 1), stops)


def faceted_gem(size, stops, rim_stops=None, facets=8, rot=0.0, round_alpha=True, grey=False):
    """A round cut stone seen from the front: radial facets, a table, a bright highlight, a dark girdle. Returns RGBA float (size, size, 4)."""
    S = size
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    cx = cy = (S - 1) / 2
    dx, dy = xx - cx, yy - cy
    r = np.sqrt(dx * dx + dy * dy) / (S / 2 - 0.5)
    ang = np.arctan2(dy, dx) + rot
    fac = np.floor((ang + math.pi) / (2 * math.pi / facets)).astype(int)
    lightdir = np.cos(ang - math.radians(-130))                # facets facing the top left are bright
    base = 0.5 + 0.32 * lightdir * np.clip(r * 1.6, 0.0, 1)
    jitter = (np.sin(fac * 12.9898) * 43758.5453) % 1.0
    t = base + 0.12 * (jitter - 0.5)
    table = r < 0.42
    t = np.where(table, 0.62 + 0.18 * np.cos(ang - math.radians(-130)) * (r / 0.42) - 0.1 * (1 - r / 0.42), t)
    t = np.where((r > 0.40) & (r < 0.45), t - 0.18, t)            # the edge of the table
    t = np.where(r > 0.9, t - 0.4 * (r - 0.9) / 0.1, t)         # the girdle
    hi = np.exp(-(((xx - (cx - S * 0.18)) ** 2 + (yy - (cy - S * 0.2)) ** 2) / (2 * (S * 0.07) ** 2)))
    t = np.clip(t + 0.55 * hi, 0, 1)
    col = P.ramp(t, stops)
    a = np.clip((1.0 - r) * (S / 2) * 1.2, 0, 1) if round_alpha else np.ones_like(r)
    rgba = np.zeros((S, S, 4), np.float32)
    rgba[..., :3] = col
    rgba[..., 3] = a * 255
    return rgba


def paste(dst, rect, arr):
    x0, y0, x1, y1 = rect
    a = np.asarray(arr, np.float32)
    if a.shape[2] == 3:
        a = np.concatenate([a, np.full(a.shape[:2] + (1,), 255, np.float32)], 2)
    dst[y0:y1, x0:x1] = a[:y1 - y0, :x1 - x0]


# =========================================================================================== the trim atlases (one per rarity)
TRIM_PAL = {
    "common": dict(metal=[(18, 20, 26), (62, 68, 82), (128, 136, 152), (214, 220, 232)], dark=[(10, 10, 14), (34, 36, 46), (74, 78, 92)],
                   leather=[(30, 16, 8), (82, 48, 26), (146, 96, 54)], gem=[(46, 50, 58), (126, 134, 150), (236, 242, 252)], page=((226, 212, 176), (178, 160, 120)), cloth=[(60, 60, 70), (130, 130, 140), (190, 190, 200)]),
    "uncommon": dict(metal=[(40, 18, 6), (122, 64, 24), (214, 138, 60), (255, 218, 142)], dark=[(24, 10, 4), (66, 34, 14), (130, 76, 34)],
                     leather=[(34, 18, 8), (92, 54, 28), (156, 104, 58)], gem=[(8, 60, 28), (36, 164, 84), (214, 255, 214)], page=((230, 216, 178), (182, 160, 118)), cloth=[(12, 54, 24), (36, 128, 56), (132, 214, 120)]),
    "rare": dict(metal=[(36, 44, 62), (118, 134, 164), (218, 228, 246), (255, 255, 255)], dark=[(20, 26, 40), (62, 74, 98), (130, 146, 176)],
                 leather=[(14, 18, 40), (38, 52, 104), (84, 112, 188)], gem=[(8, 22, 110), (36, 104, 230), (196, 232, 255)], page=((232, 224, 196), (160, 172, 196)), cloth=[(20, 30, 80), (50, 90, 190), (150, 190, 255)]),
    "epic": dict(metal=[(58, 34, 6), (170, 114, 22), (240, 184, 52), (255, 242, 168)], dark=[(36, 20, 4), (96, 60, 12), (176, 120, 34)],
                 leather=[(18, 10, 26), (52, 28, 76), (104, 66, 150)], gem=[(40, 8, 96), (150, 52, 226), (236, 190, 255)], page=((236, 196, 92), (176, 120, 30)), cloth=[(40, 14, 70), (110, 44, 170), (190, 130, 240)]),
    "mythic": dict(metal=[(70, 34, 4), (196, 124, 20), (255, 206, 70), (255, 250, 190)], dark=[(40, 14, 4), (112, 50, 10), (196, 112, 30)],
                   leather=[(40, 6, 10), (120, 16, 24), (206, 56, 52)], gem=[(70, 0, 8), (226, 22, 34), (255, 170, 160)], page=((250, 190, 70), (190, 90, 20)), cloth=[(70, 6, 10), (180, 24, 30), (255, 120, 100)]),
}


def paint_pages(kind_v, w, h, rng, pal, vertical):
    """Pages seen from the side (vertical sheets) or from above (horizontal sheets): one sheet a pixel row, a darker gap now and then, fibre noise, shadow where the covers press."""
    light, dark = np.array(pal[0], np.float32), np.array(pal[1], np.float32)
    a = np.zeros((h, w, 3), np.float32)
    n = (w if vertical else h)
    k = np.arange(n)
    shade = 0.72 + 0.2 * ((k % 3) == 0) + 0.08 * ((k % 7) == 3) - 0.18 * ((k % 5) == 4)
    shade = shade + (rng.random(n) - 0.5) * 0.08
    if vertical:
        s = np.tile(shade[None, :], (h, 1))
        along = np.linspace(-1, 1, h)[:, None]
        cross = np.linspace(-1, 1, w)[None, :]
    else:
        s = np.tile(shade[:, None], (1, w))
        along = np.linspace(-1, 1, w)[None, :]
        cross = np.linspace(-1, 1, h)[:, None]
    s = s * (1 - 0.18 * np.abs(cross) ** 3) * (1 - 0.14 * np.abs(along) ** 4)
    fibre = P.gauss(rng.random((h, w)).astype(np.float32), 0.6)
    s = s + 0.06 * (fibre - 0.5)
    a = dark[None, None, :] + (light - dark)[None, None, :] * np.clip(s, 0, 1)[..., None]
    return a


def paint_trim(rarity):
    ri = BM.RARITIES.index(rarity)
    pal = TRIM_PAL[rarity]
    rng = np.random.default_rng(7300 + ri)
    R = BM.TRIM.regions
    img = np.zeros((256, 256, 4), np.float32)
    # pages
    x0, y0, x1, y1 = R["pageV"]
    paste(img, R["pageV"], paint_pages(True, x1 - x0, y1 - y0, rng, pal["page"], True))
    x0, y0, x1, y1 = R["pageH"]
    paste(img, R["pageH"], paint_pages(True, x1 - x0, y1 - y0, rng, pal["page"], False))
    # metal plates: a corner plate with an engraved leaf at the point, and a darker one
    eng = P.Mask(64, 64)
    eng.poly([(14, 14), (50, 14), (50, 20), (20, 20), (20, 50), (14, 50)], 255)           # an engraved L along the plate's edge
    eng.ellipse(26, 26, 38, 38, fill=255)
    paste(img, R["metal"], metal_plate(64, 64, rng, pal["metal"], engrave=eng.get()))
    paste(img, R["metalDark"], metal_plate(64, 64, rng, pal["dark"], rivets=False))
    # strap: stitched leather (64 x 32), a metal stripe for rare and above
    sw, sh = 64, 32
    t, H = mat_leather(sh, sw, rng, 1.0)
    col = P.ramp(t, pal["leather"])
    st = P.dashed_line_mask(sh, sw, [(2, 5), (sw - 2, 5)], 3, 2, 0.9) + P.dashed_line_mask(sh, sw, [(2, sh - 6), (sw - 2, sh - 6)], 3, 2, 0.9)
    H = H - 1.2 * np.clip(st, 0, 1)
    col = P.lit(col, H, 0.5, spec=0.12, ao=0.3, ao_sigma=1.5)
    col = P.mix(col, P.ramp(np.full((sh, sw), 0.9, np.float32), pal["metal"]), np.clip(P.edge_dist(sh, sw) < 0.8, 0, 1).astype(np.float32) * (0.9 if ri >= 2 else 0.0))
    if ri >= 2:
        band = np.zeros((sh, sw), np.float32)
        band[12:20, :] = 1
        bh = P.gauss(band, 0.6) * 2
        bc = P.lit(metal_plate(sh, sw, rng, pal["metal"], rim=2, rivets=False), bh, 0.2)
        col = P.mix(col, bc, band * 0.9)
    paste(img, R["strap"], col)
    # cloth (32 x 32): a ribbon weave with a lighter edge stitch
    t, H = mat_cloth(32, 32, rng)
    col = P.lit(P.ramp(t, pal["cloth"]), H, 0.5, ao=0.2, ao_sigma=1.2)
    col[:2, :] *= 0.8
    col[-2:, :] *= 0.8
    paste(img, R["cloth"], col)
    # the line's ribbon: grey weave, bright (the client tints it)
    t, H = mat_cloth(32, 32, rng)
    g = (205 + 38 * (t - 0.5) * 2)[..., None] * np.ones((1, 1, 3), np.float32)
    g = P.lit(g, H, 0.18)
    g[:, :2] *= 0.82
    g[:, -2:] *= 0.82
    paste(img, R["ribbon"], np.clip(g, 0, 255))
    # gems
    paste(img, R["gemDome"], faceted_gem(32, pal["gem"], facets=8))
    side = np.zeros((16, 16, 3), np.float32)
    side[:] = P.ramp(np.linspace(0.75, 0.25, 16, dtype=np.float32)[:, None] * np.ones((1, 16), np.float32), pal["gem"])
    paste(img, R["gemSide"], side)
    stud = faceted_gem(8, [(8, 70, 70), (40, 200, 190), (210, 255, 250)], facets=6)
    stud[..., 3] = 255
    paste(img, R["stud"], stud)
    # chain links: steel ring flat (alpha hole) and on edge
    link = np.zeros((32, 32, 4), np.float32)
    yy, xx = np.mgrid[0:32, 0:32].astype(np.float32)
    ex, ey = (xx - 15.5) / 15.0, (yy - 15.5) / 8.4
    rr = np.sqrt(ex * ex + ey * ey)
    ring = (rr < 1.0) & (rr > 0.5)
    Hh = np.clip(1 - np.abs(rr - 0.75) / 0.25, 0, 1) * 3
    cstops = [(24, 26, 34), (96, 102, 118), (190, 198, 214), (250, 252, 255)] if ri < 3 else pal["metal"]
    lc = P.ramp(np.clip(0.45 + 0.4 * P.slope_light(Hh, 1.3) + 0.2 * np.clip(P.slope_light(Hh, 1.3) - 0.4, 0, 1), 0, 1), cstops)
    link[..., :3] = lc
    link[..., 3] = ring * 255
    paste(img, R["chain"], link)
    edge = np.zeros((32, 16, 4), np.float32)
    ex = (np.arange(16, dtype=np.float32) - 7.5) / 7.5
    prof = np.sqrt(np.clip(1 - ex * ex, 0, 1))
    ec = P.ramp(np.clip(0.3 + 0.55 * prof[None, :] * np.ones((32, 1), np.float32) - 0.12 * (ex[None, :] > 0.2), 0, 1), cstops)
    edge[..., :3] = ec
    edge[..., 3] = 255
    paste(img, R["chainEdge"], edge)
    # claw: dark horn that pales towards the point, with a ridge line
    cl = np.zeros((32, 32, 3), np.float32)
    yy, xx = np.mgrid[0:32, 0:32].astype(np.float32)
    ridge = np.exp(-((xx - 11) / 3.2) ** 2)
    ht = (xx / 31.0)
    horn = 0.36 + 0.34 * ridge + 0.1 * np.sin(yy * 0.9) * 0.0 + 0.18 * (1 - ht) * 0 + 0.12 * (P.fbm(32, 32, rng, (8, 4), 0.5) - 0.5)
    horn = np.clip(horn + 0.2 * np.clip(yy / 31.0 - 0.6, 0, 1), 0, 1)
    paste(img, R["claw"], P.ramp(horn, [(40, 28, 28), (122, 92, 72), (214, 190, 156), (255, 246, 222)]))
    fang = np.zeros((32, 32, 3), np.float32)
    f = 0.35 + 0.4 * np.exp(-((xx - 12) / 5.0) ** 2) + 0.1 * (P.fbm(32, 32, rng, (8, 4), 0.5) - 0.5)
    paste(img, R["fang"], P.ramp(np.clip(f, 0, 1), [(70, 74, 90), (170, 176, 192), (240, 244, 255), (255, 255, 255)]))
    # the glowing inlay (epic: violet, mythic: orange-red), a runic circle with an eight-pointed star; empty for the lower rarities
    inl = np.zeros((64, 64, 4), np.float32)
    if ri >= 3:
        glowc = (190, 90, 255) if ri == 3 else (255, 110, 40)
        m = P.Mask(64, 64)
        m.ellipse(2, 2, 62, 62, fill=None, outline=255, width=1.6)
        m.ellipse(8, 8, 56, 56, fill=None, outline=255, width=1.0)
        m.poly(P.star(32, 32, 28, 11, 8), 255) if False else None
        for a in range(0, 360, 45):
            ra = math.radians(a)
            m.line([(32 + 20 * math.cos(ra), 32 + 20 * math.sin(ra)), (32 + 29 * math.cos(ra), 32 + 29 * math.sin(ra))], 1.5)
        for a in range(0, 360, 30):
            ra = math.radians(a + 15)
            m.ellipse(32 + 24 * math.cos(ra) - 1.2, 32 + 24 * math.sin(ra) - 1.2, 32 + 24 * math.cos(ra) + 1.2, 32 + 24 * math.sin(ra) + 1.2)
        inl = P.glow_layer(m.get(), glowc, halo_sigma=1.8, halo=0.5)
    paste(img, R["inlay"], inl)
    # the plaque frame: a bevelled square ring of the rarity's metal with a dark hollow
    hollow = np.zeros((64, 64), np.float32)
    hollow[11:53, 11:53] = 1
    pf = metal_plate(64, 64, rng, pal["metal"], rim=4, rivets=True, engrave=None)
    inner = P.slope_light(P.gauss(1 - hollow, 1.0) * 3, 1.3)
    pf = pf * (1 + 0.25 * -inner)[..., None]
    pf = P.mix(pf, np.array([18, 14, 16], np.float32) * np.ones((64, 64, 3), np.float32), hollow * 0.9)
    paste(img, R["plaqueFrame"], pf)
    # the small tinted gem (grey, bright)
    tg = faceted_gem(32, [(120, 120, 120), (200, 200, 200), (255, 255, 255)], facets=6, round_alpha=False)
    tg[..., 3] = 255
    paste(img, R["tinted"], tg)
    # a bezel
    paste(img, R["bezel"], metal_plate(32, 32, rng, pal["metal"], rim=3, rivets=False))
    return img


def paint_glow_mythic():
    """The mythic page edge: 8 frames of 32 x 32, a vertical-sheet half and a horizontal-sheet half; the brightness pulses (sine over the 8 frames, looping) and a pale band travels along the sheets."""
    F, n = 32, BM.GLOW_FRAMES
    rng = np.random.default_rng(777)
    frames = []
    base_v = rng.random(F).astype(np.float32)
    for k in range(n):
        ph = k / n
        pulse = 0.72 + 0.28 * math.sin(2 * math.pi * ph)
        fr = np.zeros((F, F, 4), np.float32)
        fr[..., 3] = 255
        # vertical sheets (left half): sheet i is column i
        yy, xx = np.mgrid[0:F, 0:16].astype(np.float32)
        sheet = 0.5 + 0.5 * (((xx.astype(int) % 3) == 0) * 0.7 + 0.3 * base_v[:16][None, :].repeat(F, 0))
        band = np.exp(-(((yy - (ph * (F + 12) - 6)) / 4.0) ** 2))
        tt = np.clip(sheet * pulse * 0.78 + 0.34 * band, 0, 1)
        fr[:, :16, :3] = P.ramp(tt, [(70, 8, 4), (214, 56, 12), (255, 160, 40), (255, 236, 150)])
        # horizontal sheets (right half, upper square)
        yy2, xx2 = np.mgrid[0:16, 0:16].astype(np.float32)
        sheet2 = 0.5 + 0.5 * (((yy2.astype(int) % 3) == 0) * 0.7 + 0.3 * base_v[:16][:, None].repeat(16, 1))
        band2 = np.exp(-(((xx2 - (ph * 28 - 6)) / 4.0) ** 2))
        tt2 = np.clip(sheet2 * pulse * 0.78 + 0.34 * band2, 0, 1)
        fr[:16, 16:, :3] = P.ramp(tt2, [(70, 8, 4), (214, 56, 12), (255, 160, 40), (255, 236, 150)])
        fr[16:, 16:, :3] = fr[:16, 16:, :3]
        frames.append(fr)
    return np.concatenate(frames, 0)

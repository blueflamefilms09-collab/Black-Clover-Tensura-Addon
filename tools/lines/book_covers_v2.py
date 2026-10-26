"""The eleven franchise covers of the 3.26.0 Skill Book (painting code; the driver is gen_book_textures_v2.py).

Each franchise paints a cover atlas (front, back, spine, edges, tinted emblem plaque, strap leather, accent metal, accent slab, accent drop) and an overlay atlas (raised relief ornaments with
alpha, an emissive glow layer with alpha). The ornaments are separate layers because the model puts them on their own plane a hair above the cover, and they carry their own bevel and drop
shadow. References (owner): 1 = the purple grimoire with the pink-white embossed dragon, 2 = book set III by Saarl, 3 = book icons by Explorer Games.
"""
import math

import numpy as np
from PIL import Image, ImageDraw

import book_model_v2 as BM
import book_paint_v2 as P
from book_parts_v2 import metal_plate, mat_leather, mat_cloth, mat_wood, mat_stone, paste, FW, FH, PANEL2, HOLLOW, PLAQUE_PX, LOW_PX


# =========================================================================================== small helpers
def rgbarr(c):
    return np.array(c, np.float32)


def grad(h, w, top, bot):
    t = np.linspace(0, 1, h, dtype=np.float32)[:, None, None]
    return rgbarr(top) * (1 - t) + rgbarr(bot) * t + np.zeros((h, w, 3), np.float32)


def over(dst, src):
    """Porter-Duff over for two float RGBA images (straight alpha, 0..255)."""
    sa, da = src[..., 3:4] / 255.0, dst[..., 3:4] / 255.0
    oa = sa + da * (1 - sa)
    rgb_ = np.where(oa > 1e-5, (src[..., :3] * sa + dst[..., :3] * da * (1 - sa)) / np.maximum(oa, 1e-5), 0)
    return np.concatenate([rgb_, oa * 255.0], 2)


def box_mask(h, w, rect):
    m = np.zeros((h, w), np.float32)
    x0, y0, x1, y1 = rect
    m[y0:y1, x0:x1] = 1
    return m


def clip_panel(m):
    return m * box_mask(m.shape[0], m.shape[1], PANEL2)


def RL(mask, top, bot=None, **kw):
    """A raised ornament layer from a mask and a vertical colour gradient."""
    h, w = mask.shape
    mask = clip_panel(mask)
    return P.relief_layer(mask, grad(h, w, top, bot if bot is not None else top), **kw)


def GL(mask, color, **kw):
    return P.glow_layer(clip_panel(mask), color, **kw)


# =========================================================================================== specs and materials
SPECS = {
    "exiled_heavy_knight": dict(mat="leather", ramp=[(30, 8, 46), (82, 28, 106), (140, 62, 156), (206, 130, 214)], seed=1, gold=(236, 184, 72), grain=1.2),
    "tensura": dict(mat="leather", ramp=[(6, 40, 58), (14, 96, 120), (32, 150, 166), (120, 214, 218)], seed=2, gold=None, grain=0.7),
    "black_clover": dict(mat="leather", ramp=[(4, 4, 8), (18, 18, 24), (44, 46, 56), (96, 100, 114)], seed=3, gold=(206, 212, 226), grain=1.4),
    "magi": dict(mat="leather", ramp=[(34, 18, 8), (92, 52, 24), (150, 96, 50), (206, 150, 92)], seed=4, gold=None, grain=1.0),
    "solo_leveling": dict(mat="leather", ramp=[(2, 2, 8), (12, 10, 22), (30, 24, 50), (74, 58, 118)], seed=5, gold=None, grain=0.9),
    "danmachi": dict(mat="leather", ramp=[(40, 4, 8), (104, 14, 22), (164, 30, 36), (222, 92, 82)], seed=6, gold=(240, 196, 84), grain=0.9),
    "tbate": dict(mat="cloth", ramp=[(14, 36, 16), (40, 92, 34), (86, 150, 60), (166, 206, 112)], seed=7, gold=None, grain=1.0),
    "tower_of_god": dict(mat="wood", ramp=[(16, 28, 42), (44, 70, 98), (84, 118, 148), (150, 184, 208)], seed=8, gold=(190, 204, 222), grain=1.0),
    "god_of_high_school": dict(mat="lava", ramp=[(14, 4, 4), (62, 14, 8), (128, 34, 14), (206, 84, 26)], seed=9, gold=None, grain=1.0),
    "omniscient_readers_viewpoint": dict(mat="stone", ramp=[(28, 26, 44), (68, 62, 96), (116, 108, 146), (186, 180, 212)], seed=10, gold=(210, 214, 232), grain=1.0),
    "fairy_tail": dict(mat="leather", ramp=[(40, 20, 8), (104, 58, 26), (168, 108, 56), (224, 170, 106)], seed=11, gold=(240, 194, 84), grain=1.1),
}

ACCENT_METAL = {
    "exiled_heavy_knight": [(30, 26, 36), (92, 84, 100), (180, 172, 188), (250, 244, 255)],
    "tensura": [(10, 52, 66), (40, 130, 148), (130, 210, 220), (230, 255, 255)],
    "black_clover": [(30, 32, 40), (110, 116, 132), (206, 212, 228), (255, 255, 255)],
    "magi": [(60, 34, 12), (160, 100, 40), (230, 176, 88), (255, 236, 170)],
    "solo_leveling": [(14, 10, 30), (56, 40, 110), (130, 96, 210), (214, 190, 255)],
    "danmachi": [(70, 40, 6), (190, 128, 30), (250, 204, 90), (255, 248, 190)],
    "tbate": [(50, 66, 40), (130, 160, 96), (206, 226, 170), (252, 255, 238)],
    "tower_of_god": [(26, 34, 46), (96, 112, 136), (190, 206, 226), (252, 254, 255)],
    "god_of_high_school": [(70, 38, 6), (176, 112, 28), (240, 188, 70), (255, 240, 170)],
    "omniscient_readers_viewpoint": [(40, 42, 58), (130, 134, 164), (214, 218, 240), (255, 255, 255)],
    "fairy_tail": [(70, 40, 6), (200, 140, 30), (252, 206, 84), (255, 248, 190)],
}


def material(spec, h, w, rng):
    m = spec["mat"]
    if m == "leather":
        t, H = mat_leather(h, w, rng, spec["grain"])
        return t, H, None
    if m == "cloth":
        t, H = mat_cloth(h, w, rng, spec["grain"])
        return t, H, None
    if m == "wood":
        t, H = mat_wood(h, w, rng, spec["grain"])
        return t, H, None
    if m == "stone":
        t, H, e = mat_stone(h, w, rng, 15)
        return t, H, e
    if m == "lava":
        t, H, e = mat_stone(h, w, rng, 34)
        return t, H, e
    raise ValueError(m)


def base_color(spec, t, e):
    col = P.ramp(t, spec["ramp"])
    if spec["mat"] == "lava" and e is not None:
        crack = np.clip(1 - e / 2.2, 0, 1)
        glowc = P.ramp(np.clip(crack * 1.2, 0, 1), [(150, 30, 8), (255, 120, 20), (255, 214, 90)])
        col = P.mix(col, glowc, np.clip(crack * 1.3, 0, 1))
    return col


def step_heights(h, w):
    s = np.zeros((h, w), np.float32)
    s += box_mask(h, w, (10, 10, w - 10, h - 10)) * 1.4
    s += box_mask(h, w, (20, 20, w - 20, h - 20)) * 1.4
    return P.gauss(s, 0.8)


def stitch_ring(h, w, inset):
    pts = [(inset, inset), (w - inset, inset), (w - inset, h - inset), (inset, h - inset), (inset, inset)]
    return P.dashed_line_mask(h, w, pts, 4, 3, 1.0)


def thread_color(spec):
    return np.clip(P.ramp(np.array([0.8], np.float32), spec["ramp"])[0] * 1.12, 0, 255)


# =========================================================================================== the parts of the atlas
def paint_front(spec, rng):
    h, w = FH, FW
    t, Hm, e = material(spec, h, w, rng)
    col = base_color(spec, t, e)
    H = Hm * 0.8 + step_heights(h, w)
    rule = P.Mask(w, h)
    rule.d.rectangle((26 * 4, 26 * 4, (w - 26) * 4, (h - 26) * 4), outline=255, width=3)
    rule.d.rectangle((14 * 4, 14 * 4, (w - 14) * 4, (h - 14) * 4), outline=255, width=2)
    rm = rule.get()
    H = H - 1.3 * P.gauss(rm, 0.5)
    col = P.lit(col, H, 0.62, spec=0.10, ao=0.45, ao_sigma=2.2)
    if spec["gold"] is not None:
        gl = P.lit(np.ones((h, w, 3), np.float32) * rgbarr(spec["gold"]), P.gauss(rm, 0.9) * 2.0, 0.7, spec=0.4)
        col = P.mix(col, gl, np.clip(P.gauss(rm, 0.4) * 1.4, 0, 1) * 0.9)
    stc = stitch_ring(h, w, 5)
    col = P.mix(col, np.ones((h, w, 3), np.float32) * thread_color(spec), stc * 0.9)
    wmask = P.wear(h, w, rng, None, 7.0, 0.8)
    col = P.mix(col, P.ramp(np.full((h, w), 0.93, np.float32), spec["ramp"]) * 1.02, wmask * 0.7)
    return np.clip(col, 0, 255)


def paint_back(spec, rng):
    h, w = FH, FW
    t, Hm, e = material(spec, h, w, rng)
    col = base_color(spec, t, e)
    H = Hm * 0.8 + step_heights(h, w)
    rule = P.Mask(w, h)
    rule.d.rectangle((24 * 4, 24 * 4, (w - 24) * 4, (h - 24) * 4), outline=255, width=3)
    rule.ellipse(w / 2 - 18, h / 2 - 18, w / 2 + 18, h / 2 + 18, fill=None, outline=255, width=2)
    rule.poly([(w / 2, h / 2 - 12), (w / 2 + 12, h / 2), (w / 2, h / 2 + 12), (w / 2 - 12, h / 2)])
    H = H + 1.0 * P.gauss(rule.get(), 0.8)
    col = P.lit(col, H, 0.62, spec=0.08, ao=0.4, ao_sigma=2.2)
    col = P.mix(col, np.ones((h, w, 3), np.float32) * thread_color(spec), stitch_ring(h, w, 5) * 0.9)
    wmask = P.wear(h, w, rng, None, 8.0, 0.9)
    col = P.mix(col, P.ramp(np.full((h, w), 0.93, np.float32), spec["ramp"]), wmask * 0.7)
    return np.clip(col, 0, 255)


def paint_spine(spec, rng):
    h, w = FH, 64
    t, Hm, e = material(spec, h, w, rng)
    col = base_color(spec, t, e)
    H = Hm * 0.8
    m = P.Mask(w, h)
    m.rect(8, 84, w - 8, 148, fill=255)
    for (v0, v1) in ((20, 64), (166, 190)):
        n = max(1, (v1 - v0) // 22)
        for k in range(n):
            cy = v0 + (k + 0.5) * (v1 - v0) / n
            m.poly([(w / 2, cy - 9), (w / 2 + 9, cy), (w / 2, cy + 9), (w / 2 - 9, cy)])
    plate = m.get()
    inner = P.Mask(w, h)
    inner.d.rectangle((16 * 4, 92 * 4, (w - 16) * 4, 140 * 4), outline=255, width=3)
    H = H + 1.8 * P.gauss(plate, 0.9) - 0.8 * P.gauss(inner.get(), 0.9)
    col = P.lit(col, H, 0.62, spec=0.1, ao=0.4, ao_sigma=2.0)
    if spec["gold"] is not None:
        rimg = P.Mask(w, h)
        rimg.d.rectangle((8 * 4, 84 * 4, (w - 8) * 4, 148 * 4), outline=255, width=5)
        col = P.mix(col, np.ones((h, w, 3), np.float32) * rgbarr(spec["gold"]) * 0.95, P.gauss(rimg.get(), 0.4))
    x = np.linspace(-1, 1, w, dtype=np.float32)[None, :]
    col = col * (1 - 0.33 * np.abs(x) ** 2.5)[..., None]
    wmask = P.wear(h, w, rng, None, 5.0, 0.6)
    col = P.mix(col, P.ramp(np.full((h, w), 0.92, np.float32), spec["ramp"]), wmask * 0.5)
    return np.clip(col, 0, 255)


def paint_edge(spec, rng, w, h, horizontal):
    t, Hm, e = material(spec, h, w, rng)
    col = base_color(spec, t * 0.75, e) * 0.82
    prof = np.sin(np.linspace(0, math.pi, h))[:, None] if horizontal else np.sin(np.linspace(0, math.pi, w))[None, :]
    col = P.lit(col, Hm * 0.5 + 0.6 * prof, 0.5)
    return np.clip(col, 0, 255)


def em_fairy_tail(d):
    d.ellipse((46, 52, 100, 106), fill=255)
    d.ellipse((62, 66, 84, 88), fill=0)
    d.ellipse((68, 72, 78, 82), fill=255)
    for (x0, y0, x1, y1, x2, y2) in ((100, 70, 130, 40, 150, 30), (96, 84, 134, 70, 156, 66), (92, 96, 130, 98, 154, 108)):
        d.polygon([(x0, y0), (x1, y1), (x2, y2), (x1 + 4, y1 + 22)], fill=255)
    d.polygon([(52, 100), (30, 128), (18, 150), (46, 124), (64, 108)], fill=255)


def emblem_mask160(name):
    big = Image.new("L", (160, 160), 0)
    if name == "fairy_tail":
        em_fairy_tail(ImageDraw.Draw(big))
    else:
        import gen_book_textures as OLD
        OLD.EMBLEMS[name](ImageDraw.Draw(big))
    return big


def paint_emblem(name, rng):
    big = emblem_mask160(name)
    S = 4
    small = np.asarray(big.resize((40 * S, 40 * S), Image.LANCZOS).resize((40, 40), Image.BOX), np.float32) / 255.0
    m = np.zeros((64, 64), np.float32)
    m[12:52, 12:52] = small
    plate = np.zeros((64, 64), np.float32)
    plate[5:59, 5:59] = 1
    H = P.gauss(plate, 1.0) * 1.2 + P.gauss(m, 0.8) * 2.4
    base = np.full((64, 64, 3), 150.0, np.float32) + (P.fbm(64, 64, rng, (8, 4), 0.5) - 0.5)[..., None] * 24
    base = P.mix(base, np.full((64, 64, 3), 244.0, np.float32), np.clip(m * 1.2, 0, 1))
    col = P.lit(base, H, 0.42, spec=0.12, spec_color=(255, 255, 255), ao=0.3, ao_sigma=1.6)
    edge = 1 - P.gauss(plate, 0.7)
    col = col * (1 - 0.28 * np.clip(edge * 1.5, 0, 1))[..., None]
    return np.clip(col, 0, 255)


def paint_leather_swatch(spec, rng):
    t, H, e = material(dict(spec, mat="leather"), 32, 64, rng)
    col = P.ramp(t * 0.85 + 0.05, spec["ramp"])
    st = P.dashed_line_mask(32, 64, [(2, 4), (62, 4)], 3, 2, 0.9) + P.dashed_line_mask(32, 64, [(2, 27), (62, 27)], 3, 2, 0.9)
    H = H - 1.0 * np.clip(st, 0, 1)
    col = P.lit(col, H, 0.5, spec=0.1, ao=0.3, ao_sigma=1.5)
    col = P.mix(col, np.ones((32, 64, 3), np.float32) * thread_color(spec), np.clip(st, 0, 1) * 0.8)
    return np.clip(col, 0, 255)


def paint_slab(spec, rng):
    """The franchise's accent material (a chunk of the cover's own material, lighter and more worn): scales, stone, wood, cloth, cracked crust."""
    t, H, e = material(spec, 64, 64, rng)
    col = base_color(spec, np.clip(t * 0.9 + 0.12, 0, 1), e)
    H = H + P.gauss(box_mask(64, 64, (3, 3, 61, 61)), 1.2) * 2.0
    col = P.lit(col, H, 0.7, spec=0.12, ao=0.4, ao_sigma=2.0)
    return np.clip(col, 0, 255)


def paint_drop(name, rng):
    S = 32
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    cx = cy = (S - 1) / 2
    r = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (S / 2)
    if name == "tensura":
        t = np.clip(0.55 - 0.3 * r + 0.25 * np.exp(-(((xx - 10) ** 2 + (yy - 9) ** 2) / 30.0)), 0, 1)
        col = P.ramp(t, [(8, 70, 90), (30, 170, 190), (130, 236, 240), (255, 255, 255)])
    elif name == "tbate":
        vein = np.exp(-((xx - cx) / 1.1) ** 2)
        t = np.clip(0.5 + 0.25 * vein - 0.25 * r + 0.1 * (P.fbm(S, S, rng, (8, 4), 0.5) - 0.5), 0, 1)
        col = P.ramp(t, [(16, 56, 18), (60, 130, 44), (140, 200, 90), (230, 255, 190)])
    else:
        t = np.clip(1.15 - r * 1.1, 0, 1)
        col = P.ramp(t, [(255, 190, 80), (255, 230, 140), (255, 252, 220), (255, 255, 255)])
    return np.clip(col, 0, 255)


# =========================================================================================== the motifs (relief + glow), one per franchise
CX, CY = HOLLOW


def ring_mask(h, w, cx, cy, r, width, a0=None, a1=None):
    m = P.Mask(w, h)
    if a0 is None:
        m.ellipse(cx - r, cy - r, cx + r, cy + r, fill=None, outline=255, width=width)
    else:
        m.arc(cx - r, cy - r, cx + r, cy + r, a0, a1, width)
    return m


def dragon_head(m, hx, hy, tx, ty, size=1.0):
    """A dragon head at (hx, hy) looking along the unit vector (tx, ty): skull, tapering snout, an open jaw notch, two swept-back horns, an eye hole."""
    nx, ny = -ty, tx
    def P_(a, b):
        return (hx + tx * a * size + nx * b * size, hy + ty * a * size + ny * b * size)
    m.poly([P_(-5, -4.6), P_(1, -5.4), P_(9, -2.2), P_(14, -0.9), P_(14, 1.2), P_(8, 2.6), P_(1, 5.4), P_(-5, 4.6)])
    for s_ in (-1, 1):                                                  # horns, swept back
        m.brush([P_(-2, s_ * 3.6), P_(-8, s_ * 6.5), P_(-15, s_ * 7.6), P_(-20, s_ * 6.4)], 2.0 * size, 0.5)
    m.poly([P_(14, -0.9), P_(19, -0.2), P_(14, 0.9)], 0)                # the jaw notch
    m.ellipse(*(lambda e: (e[0] - 1.1, e[1] - 1.1, e[0] + 1.1, e[1] + 1.1))(P_(4.5, -2.2)), fill=0)


def motif_exiled(h, w, rng):
    """Image 1: two pink-white dragons, a thinner body and a clear head each, coiled round the red gem, embossed on the purple leather; tendrils of flame at the corners."""
    m = P.Mask(w, h)
    for flip in (0, 1):
        a0 = 215 + 180 * flip
        pts = P.spiral(CX, CY, 52, 38, 1.0, math.radians(a0), 120)
        # the body: thick at the head, tapering to the tail
        m.brush(pts, 4.6, 0.9)
        # scale ridge: short spines along the outside of the coil
        for k in range(2, 11):
            i = k * 11
            px, py = pts[i]
            ang = math.atan2(py - CY, px - CX)
            m.brush([(px, py), (px + 5.5 * math.cos(ang) * (1 - k / 14), py + 5.5 * math.sin(ang) * (1 - k / 14))], 1.5 * (1 - k / 14) + 0.5, 0.3)
        hx, hy = pts[0]
        tx, ty = pts[0][0] - pts[2][0], pts[0][1] - pts[2][1]
        L = math.hypot(tx, ty)
        dragon_head(m, hx, hy, tx / L, ty / L, 1.15)
        # a hind claw / wing hook where the coil crosses the middle
        i = 60
        px, py = pts[i]
        ang = math.atan2(py - CY, px - CX)
        m.brush(P.bezier((px, py), (px + 9 * math.cos(ang + 0.5), py + 9 * math.sin(ang + 0.5)), (px + 15 * math.cos(ang + 0.2), py + 15 * math.sin(ang + 0.2)), (px + 19 * math.cos(ang + 0.7), py + 19 * math.sin(ang + 0.7))), 1.8, 0.4)
    for sx in (0, 1):
        for sy in (0, 1):
            x0 = 32 if sx == 0 else w - 32
            y0 = 52 + 0 if sy == 0 else h - 40
            dirx = 1 if sx == 0 else -1
            diry = 1 if sy == 0 else -1
            m.brush(P.bezier((x0, y0), (x0 + dirx * 18, y0 + diry * 4), (x0 + dirx * 10, y0 + diry * 22), (x0 + dirx * 28, y0 + diry * 24)), 2.6, 0.6)
    relief = RL(m.get(), (255, 236, 248), (214, 112, 176), bevel=1.0, spec=0.45)
    return relief, None


def motif_tensura(h, w, rng):
    """Reference 2, row 1 col 1: raised teal swirls like cresting waves."""
    m = P.Mask(w, h)
    for (cx, cy, r0, turns, fl) in ((34, 100, 17, 2.1, 1), (110, 96, 15, 2.0, -1), (34, 148, 12, 1.8, -1), (110, 144, 13, 1.9, 1), (72, 176, 8, 1.5, 1)):
        pts = P.spiral(cx, cy, r0, 2.5, turns, 0.4, 100, fl)
        m.brush(pts, 3.0, 1.4)
    m.arc(CX - 41, CY - 41, CX + 41, CY + 41, 215, 325, 3.0)
    m.arc(CX - 41, CY - 41, CX + 41, CY + 41, 35, 145, 3.0)
    for (x0, y0, x1, y1) in ((44, 76, 64, 96), (100, 74, 82, 94), (44, 170, 60, 150), (100, 166, 84, 150)):
        m.brush(P.bezier((x0, y0), ((x0 + x1) / 2 - 6, y0 - 8), ((x0 + x1) / 2 + 6, y1 + 8), (x1, y1)), 2.2, 1.0)
    relief = RL(m.get(), (206, 255, 250), (62, 164, 178), bevel=1.35, spec=0.5)
    return relief, None


def motif_black_clover(h, w, rng):
    """Reference 2, row 1 col 2: black leather with raised silver filigree vines, rings and clover leaves."""
    m = P.Mask(w, h)
    m.ellipse(CX - 40, CY - 40, CX + 40, CY + 40, fill=None, outline=255, width=3.0)
    m.ellipse(CX - 46, CY - 46, CX + 46, CY + 46, fill=None, outline=255, width=1.4)
    for sx in (-1, 1):
        for sy in (-1, 1):
            px, py = CX + sx * 57, CY + sy * 52
            m.brush(P.spiral(px, py, 11, 2, 1.7, 0.6 if sx * sy > 0 else 2.6, 70, sx * sy), 2.4, 1.0)
            for k in range(3):                                                    # a trefoil of leaves at the corner of the panel
                a = k * 2 * math.pi / 3 + (0.4 if sx > 0 else 1.3)
                lx, ly = CX + sx * 40 + math.cos(a) * 6, CY + sy * 78 + math.sin(a) * 6
                m.poly(P.heart(lx, ly, 0.42, a + math.pi / 2))
    m.brush([(72, 88), (72, 80)], 2.0)
    for sx in (-1, 1):
        m.brush(P.bezier((CX + sx * 40, CY), (CX + sx * 52, CY - 12), (CX + sx * 48, CY - 34), (CX + sx * 36, CY - 42)), 2.0, 0.8)
        m.brush(P.bezier((CX + sx * 40, CY), (CX + sx * 52, CY + 12), (CX + sx * 48, CY + 34), (CX + sx * 36, CY + 42)), 2.0, 0.8)
    relief = RL(m.get(), (240, 244, 252), (130, 136, 156), bevel=1.1, spec=0.55)
    return relief, None


def motif_magi(h, w, rng):
    """Reference 2, row 1 col 3 / row 2 col 4: tooled brown leather with a knotted border, a leather strap and a glowing blue rune."""
    m = P.Mask(w, h)
    for k in range(13):                                                            # a row of knots along the border
        for (x, y) in ((28 + k * 7, 28), (28 + k * 7, h - 28)):
            m.ellipse(x - 2.4, y - 2.4, x + 2.4, y + 2.4)
    for k in range(20):
        for (x, y) in ((28, 36 + k * 7.6), (w - 28, 36 + k * 7.6)):
            m.ellipse(x - 2.4, y - 2.4, x + 2.4, y + 2.4)
    m.ellipse(CX - 46, CY - 46, CX + 46, CY + 46, fill=None, outline=255, width=2.4)
    for a in range(0, 360, 30):
        ra = math.radians(a)
        m.ellipse(CX + 46 * math.cos(ra) - 3, CY + 46 * math.sin(ra) - 3, CX + 46 * math.cos(ra) + 3, CY + 46 * math.sin(ra) + 3)
    relief = RL(m.get(), (222, 166, 100), (140, 86, 40), bevel=1.0, spec=0.2)
    g = P.Mask(w, h)
    g.ellipse(CX - 38, CY - 38, CX + 38, CY + 38, fill=None, outline=255, width=2.0)
    for a in range(0, 360, 45):                                                    # rune ticks
        ra = math.radians(a)
        g.line([(CX + 33 * math.cos(ra), CY + 33 * math.sin(ra)), (CX + 41 * math.cos(ra), CY + 41 * math.sin(ra))], 2.2)
    g.line([(CX - 18, CY + 22), (CX, CY - 24), (CX + 18, CY + 22)], 2.4)           # the rune: an arrow head with a bar and a fork
    g.line([(CX - 12, CY + 6), (CX + 12, CY + 6)], 2.2)
    g.line([(CX, CY - 24), (CX, CY - 32)], 2.2)
    return relief, GL(g.get(), (96, 196, 255), halo_sigma=2.6)


def motif_solo(h, w, rng):
    """Black cover, a glowing violet rune with a sword, jagged shadow lines and wisps of smoke."""
    m = P.Mask(w, h)
    for sx in (0, 1):
        pts = [(24, 40), (34, 52), (26, 64), (36, 78), (28, 92), (38, 108)] if sx == 0 else [(w - 24, 40), (w - 34, 52), (w - 26, 64), (w - 36, 78), (w - 28, 92), (w - 38, 108)]
        m.line(pts, 2.4)
        pts2 = [(24, h - 40), (34, h - 52), (26, h - 64), (36, h - 78), (28, h - 92), (38, h - 108)] if sx == 0 else [(w - 24, h - 40), (w - 34, h - 52), (w - 26, h - 64), (w - 36, h - 78), (w - 28, h - 92), (w - 38, h - 108)]
        m.line(pts2, 2.4)
    for (x, y, s) in ((40, 150, 1), (100, 156, -1), (72, 140, 1), (52, 176, -1), (92, 178, 1)):          # smoke curls
        m.brush(P.bezier((x, y), (x + s * 10, y - 12), (x - s * 8, y - 24), (x + s * 6, y - 36)), 3.2, 0.5)
    relief = RL(m.get(), (110, 84, 170), (36, 28, 64), bevel=1.3, spec=0.3)
    g = P.Mask(w, h)
    g.ellipse(CX - 40, CY - 40, CX + 40, CY + 40, fill=None, outline=255, width=1.8)
    g.arc(CX - 33, CY - 33, CX + 33, CY + 33, 20, 160, 1.6)
    g.arc(CX - 33, CY - 33, CX + 33, CY + 33, 200, 340, 1.6)
    g.line([(CX - 30, CY - 10), (CX - 20, CY + 4), (CX - 28, CY + 18)], 2.2)                   # shadow rune: ᛋ-like zigzags both sides
    g.line([(CX + 30, CY - 10), (CX + 20, CY + 4), (CX + 28, CY + 18)], 2.2)
    g.line([(CX, CY - 62), (CX, CY - 44)], 2.0)
    g.line([(CX - 6, CY + 44), (CX, CY + 60), (CX + 6, CY + 44)], 2.0)
    return relief, GL(g.get(), (176, 100, 255), halo_sigma=2.6)


def motif_danmachi(h, w, rng):
    """Deep red leather with raised gold scrollwork and white firefly lights (reference 2, row 4 col 2)."""
    m = P.Mask(w, h)
    for sx in (-1, 1):
        for sy in (-1, 1):
            px, py = CX + sx * 44, CY + sy * 50
            m.brush(P.spiral(px, py, 12, 2, 2.0, 0.0 if sx > 0 else math.pi, 90, sx * sy), 2.6, 1.1)
            m.brush(P.bezier((px, py), (px + sx * 14, py - sy * 10), (px + sx * 24, py - sy * 24), (px + sx * 14, py - sy * 38)), 2.0, 0.6)
    m.ellipse(CX - 40, CY - 40, CX + 40, CY + 40, fill=None, outline=255, width=2.4)
    m.ellipse(CX - 45, CY - 45, CX + 45, CY + 45, fill=None, outline=255, width=1.1)
    for a in range(0, 360, 45):
        ra = math.radians(a + 22.5)
        m.poly([(CX + 45 * math.cos(ra), CY + 45 * math.sin(ra) - 4), (CX + 51 * math.cos(ra) + 3, CY + 51 * math.sin(ra)), (CX + 45 * math.cos(ra), CY + 45 * math.sin(ra) + 4), (CX + 39 * math.cos(ra) - 3, CY + 39 * math.sin(ra))])
    relief = RL(m.get(), (255, 232, 140), (190, 124, 30), bevel=1.2, spec=0.6)
    g = P.Mask(w, h)
    rr = np.random.default_rng(66)
    for _ in range(16):
        x, y = rr.integers(26, w - 26), rr.integers(30, h - 30)
        if (x - CX) ** 2 + (y - CY) ** 2 < 36 ** 2 or (x - PLAQUE_PX[0]) ** 2 + (y - PLAQUE_PX[1]) ** 2 < 32 ** 2:
            continue
        g.ellipse(x - 1.4, y - 1.4, x + 1.4, y + 1.4)
    return relief, GL(g.get(), (255, 244, 190), halo_sigma=2.2, halo=0.7)


def motif_tbate(h, w, rng):
    """Reference 2, row 3 col 2: a white tree with spreading branches on green cloth."""
    m = P.Mask(w, h)
    m.brush(P.bezier((72, 190), (72, 176), (70, 164), (72, 150)), 8, 5.5)
    for sx in (-1, 1):
        x = lambda v: 72 + sx * (v - 72) * 1.0
        b1 = P.bezier((72, 156), (72 + sx * 30, 150), (72 + sx * 54, 130), (72 + sx * 46, 98))
        m.brush(b1, 5.0, 2.2)
        b2 = P.bezier((72 + sx * 46, 104), (72 + sx * 52, 84), (72 + sx * 44, 64), (72 + sx * 30, 52))
        m.brush(b2, 3.0, 1.2)
        b3 = P.bezier((72 + sx * 44, 128), (72 + sx * 58, 118), (72 + sx * 58, 96), (72 + sx * 54, 78))
        m.brush(b3, 2.4, 1.0)
        b4 = P.bezier((72 + sx * 20, 152), (72 + sx * 32, 146), (72 + sx * 38, 150), (72 + sx * 52, 150))
        m.brush(b4, 2.4, 0.9)
        b5 = P.bezier((72 + sx * 8, 52), (72 + sx * 8, 42), (72 + sx * 20, 34), (72 + sx * 22, 26))
        m.brush(b5, 2.0, 0.8)
    for sx in (-1, 1):
        for i, (px, py) in enumerate(((46, 98), (30, 52), (54, 78), (52, 150), (22, 26), (54, 118), (38, 62), (58, 104))):
            m.ellipse(72 + sx * px - 2.6, py - 2.2, 72 + sx * px + 2.6, py + 2.2)
    relief = RL(m.get(), (252, 255, 244), (172, 208, 156), bevel=1.2, spec=0.3)
    return relief, None


def motif_tower(h, w, rng):
    """Reference 2, row 4 col 3: blue-grey planks, steel nails, a carved ring and a cyan gem."""
    m = P.Mask(w, h)
    for sx in (36, 72, 108):
        for y in range(34, h - 24, 36):
            if abs(sx - 72) < 3 and 100 < y < 150:
                continue
            m.ellipse(sx - 3.0, y - 3.0, sx + 3.0, y + 3.0)
    m.ellipse(CX - 42, CY - 42, CX + 42, CY + 42, fill=None, outline=255, width=2.0)
    for a in range(0, 360, 90):
        ra = math.radians(a)
        m.line([(CX + 42 * math.cos(ra), CY + 42 * math.sin(ra)), (CX + 52 * math.cos(ra), CY + 52 * math.sin(ra))], 3.0)
    relief = RL(m.get(), (226, 236, 246), (96, 110, 132), bevel=1.0, spec=0.6)
    g = P.Mask(w, h)
    g.ellipse(CX - 44, CY - 44, CX + 44, CY + 44, fill=None, outline=255, width=1.2)
    g.ellipse(CX - 13, CY - 13, CX + 13, CY + 13)
    return relief, GL(g.get(), (80, 226, 255), halo_sigma=2.8)


def motif_goh(h, w, rng):
    """Reference 2, row 3 col 4: cracked lava crust; the cracks glow. The relief layer is a few raised ash flakes."""
    d1, d2, idx = P.voronoi(h, w, np.random.default_rng(919), 34)
    edge = d2 - d1
    crack = np.clip(1 - edge / 2.0, 0, 1)
    g = GL(crack, (255, 150, 30), halo_sigma=2.0, halo=0.5)
    return None, g


def motif_omniscient(h, w, rng):
    """Reference 2, row 2 col 2: grey-violet stone plates with raised silver rings joined by lines."""
    m = P.Mask(w, h)
    nodes = [(34, 94), (110, 94), (34, 144), (110, 144)]
    m.ellipse(CX - 40, CY - 40, CX + 40, CY + 40, fill=None, outline=255, width=2.2)
    for (x, y) in nodes:
        m.ellipse(x - 8, y - 8, x + 8, y + 8, fill=None, outline=255, width=2.4)
        m.ellipse(x - 2.4, y - 2.4, x + 2.4, y + 2.4)
        ang = math.atan2(y - CY, x - CX)
        m.line([(x - 8 * math.cos(ang), y - 8 * math.sin(ang)), (CX + 40 * math.cos(ang), CY + 40 * math.sin(ang))], 2.0)
    m.line([(34, 86), (34, 78), (46, 70)], 2.0)
    m.line([(110, 86), (110, 78), (98, 70)], 2.0)
    relief = RL(m.get(), (240, 242, 252), (126, 130, 160), bevel=1.1, spec=0.6)
    g = P.Mask(w, h)
    for (x, y) in nodes:
        g.ellipse(x - 2.0, y - 2.0, x + 2.0, y + 2.0)
    g.ellipse(CX - 2.4, CY - 2.4, CX + 2.4, CY + 2.4)
    return relief, GL(g.get(), (200, 150, 255), halo_sigma=2.4)


def motif_fairy(h, w, rng):
    """Warm brown leather, the gold guild wings (five tapering feathers each, fanning up and out) round a red enamel ring (reference 2, row 3 col 3 and row 4 col 1)."""
    m = P.Mask(w, h)
    m.ellipse(CX - 41, CY - 41, CX + 41, CY + 41, fill=None, outline=255, width=2.6)
    for sx in (-1, 1):                                          # wings: five feathers each, a long one on top, shorter ones below
        for k in range(5):
            a = math.radians(-78 + k * 26)                       # fan angle, from nearly straight up to outward and down
            ln = 46 - 5.5 * k
            base = (CX + sx * 36, CY - 10 + 6 * k)
            tip = (base[0] + sx * ln * math.cos(a + math.pi / 2 * 0.0) * 0.62 + sx * 4, base[1] + ln * math.sin(a) * 0.95)
            ctrl1 = (base[0] + sx * 8, base[1] + ln * math.sin(a) * 0.5)
            ctrl2 = (tip[0] - sx * 3, tip[1] + 12)
            m.brush(P.bezier(base, ctrl1, ctrl2, tip), 4.4 - 0.3 * k, 0.7)
    for sx in (-1, 1):                                          # the two tail curls at the foot of the ring
        m.brush(P.bezier((CX + sx * 24, CY + 40), (CX + sx * 34, CY + 52), (CX + sx * 26, CY + 62), (CX + sx * 40, CY + 68)), 3.0, 0.9)
    gold = RL(m.get(), (255, 236, 140), (200, 128, 28), bevel=1.2, spec=0.7)
    e = P.Mask(w, h)
    e.ellipse(CX - 36, CY - 36, CX + 36, CY + 36, fill=None, outline=255, width=3.6)
    red = RL(e.get(), (255, 96, 84), (150, 14, 20), bevel=1.1, spec=0.7, shadow=0.0)
    return over(gold, red), None


MOTIFS = {"exiled_heavy_knight": motif_exiled, "tensura": motif_tensura, "black_clover": motif_black_clover, "magi": motif_magi, "solo_leveling": motif_solo, "danmachi": motif_danmachi,
          "tbate": motif_tbate, "tower_of_god": motif_tower, "god_of_high_school": motif_goh, "omniscient_readers_viewpoint": motif_omniscient, "fairy_tail": motif_fairy}


# =========================================================================================== the whole atlases of one franchise
def cover_atlas(franchise):
    spec = SPECS[franchise]
    rng = np.random.default_rng(5000 + spec["seed"])
    R = BM.COVER.regions
    img = np.zeros((256, 512, 4), np.float32)
    paste(img, R["front"], paint_front(spec, rng))
    paste(img, R["back"], paint_back(spec, rng))
    paste(img, R["spine"], paint_spine(spec, rng))
    paste(img, R["edgeV"], paint_edge(spec, rng, 16, 208, False))
    paste(img, R["edgeH"], paint_edge(spec, rng, 144, 16, True))
    paste(img, R["edgeH2"], paint_edge(spec, rng, 144, 16, True))
    paste(img, R["emblem"], paint_emblem(franchise, rng))
    paste(img, R["leather"], paint_leather_swatch(spec, rng))
    paste(img, R["metal"], metal_plate(64, 64, rng, ACCENT_METAL[franchise], rim=6, rivets=False))
    paste(img, R["slab"], paint_slab(spec, rng))
    paste(img, R["drop"], paint_drop(franchise, rng))
    return img


def fx_atlas(franchise):
    spec = SPECS[franchise]
    rng = np.random.default_rng(6000 + spec["seed"])
    img = np.zeros((208, 288, 4), np.float32)
    relief, glow = MOTIFS[franchise](FH, FW, rng)
    if relief is not None:
        paste(img, BM.FX.regions["relief"], relief)
    if glow is not None:
        paste(img, BM.FX.regions["glow"], glow)
    return img

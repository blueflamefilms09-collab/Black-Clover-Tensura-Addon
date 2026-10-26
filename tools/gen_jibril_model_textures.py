"""Generates the material atlas of the 3D models of "Jibril, Lord of Rigor" (3.21.0: vfx.client.layer.JibrilModels / JibrilMeshDraw / JibrilLayer).

Deterministic and font-free (every machine writes the same pixels). Written for this mod, same palette as tools/gen_jibril_vfx_textures.py (pearl white, peach and rose gold, amber, azure and lavender
shadow, the prismatic rim). It only ADDS one file; the 22 flat sprites of gen_jibril_vfx_textures.py stay and are still drawn when the setting `jibrilModels` is off.
Output: src/main/resources/assets/nusmp/textures/particle/jibril_model_atlas.png

    python tools/gen_jibril_model_textures.py

    jibril_model_atlas   4 x 4 cells of 128 px (512 x 512), alpha-blended, lit like polished gold / enamel from the upper left and shaded again by the layer per face:
        0 polished gold with an amber inlay line         1 deep rose gold                               2 white-gold glow (spare)             3 pearl enamel with a prismatic sheen
        4 azure lacquer with gold border and stars       5 lavender crystal (spare)                     6 feather (cut out, pearl to peach)   7 feather (cut out, peach to rose gold)
        8 gold band with engraved law script (a stripe)  9 parchment page with glowing columns of script   10 the glowing seal plate (rose window on gold)
        11 law plate: azure panel, pearl border, columns of script, the emblem on top                 12 holy light crystal (faceted, pearl to aqua)
        13 gold chain link with an engraved line         14 azure leather cover with gold filigree and the emblem     15 pearl enamel with gold trim lines and a vertical inlay
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_jibril_vfx_textures as j   # the palette, the script drawing, the feather painter, the emblem and the iris of the flat sprites (imported, not changed)

S = 128
GOLD_STOPS = [(0.00, (84, 46, 24)), (0.30, (176, 108, 50)), (0.58, (236, 170, 80)), (0.80, (255, 220, 136)), (1.00, (255, 248, 214))]
INK = (196, 108, 56)


def ramp_gold(t):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (t0, c0), (t1, c1) in zip(GOLD_STOPS[:-1], GOLD_STOPS[1:]):
        m = (t >= t0) & (t <= t1)
        f = ((t - t0) / (t1 - t0))[..., None]
        out = np.where(m[..., None], j.col(c0) * (1 - f) + j.col(c1) * f, out)
    return out


def grid01(w=S, h=S):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def noise(w, h, seed, scale):
    """Smooth value noise 0..1 (a coarse random grid, bilinearly stretched)."""
    rs = np.random.RandomState(seed)
    gw, gh = max(2, int(w / scale)), max(2, int(h / scale))
    g = rs.rand(gh + 1, gw + 1).astype(np.float32)
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def fbm(w, h, seed):
    return 0.5 * noise(w, h, seed, 32) + 0.3 * noise(w, h, seed + 1, 12) + 0.2 * noise(w, h, seed + 2, 5)


def edge_dist(w=S, h=S):
    u, v = grid01(w, h)
    return np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))


def bevel(width=0.14):
    return j.smooth(0.0, width, edge_dist())


def lit(h, ambient=0.30, k=10.0, spec=0.55):
    """A lambert + a little specular from the upper left of a height map h (0..1-ish)."""
    gy, gx = np.gradient(h)
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(h)
    nn = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * -0.5 + ny * -0.6 + nz * 0.62) / nn, 0, 1)
    hx, hy, hz = -0.35, -0.42, 0.84
    sp = np.clip((nx * hx + ny * hy + nz * hz) / (nn * math.sqrt(hx * hx + hy * hy + hz * hz)), 0, 1) ** 24
    return np.clip(ambient + (1 - ambient) * lam * 0.9 + spec * sp, 0, 1)


def mix(rgb, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return rgb * (1 - a) + j.col(colour)[None, None, :] * a


def inset_line(px, w_px=1.4, w=S, h=S):
    d = edge_dist(w, h) * min(w, h)
    return np.exp(-((d - px) / w_px) ** 2), np.exp(-((d - px) / (w_px * 3.2)) ** 2) * 0.4


def opaque():
    return np.ones((S, S), np.float32)


def ss_mask(draw_fn, ss=4):
    """Draws a line-art mask at ss x resolution with draw_fn(d, ss) and returns it at the cell size."""
    im, d = j.new_mask(S * ss, S * ss)
    draw_fn(d, ss)
    return j.downsample(im, S, S)


# ---------------------------------------------------------------------------------------------------- the cells
def gold_body(seed, dark=1.0, bevel_w=0.15, streak=0.018):
    u, v = grid01()
    brushed = (noise(S, S, seed, 3) - 0.5)
    brushed = np.asarray(Image.fromarray(((brushed * 0.5 + 0.5) * 255).astype(np.uint8)).resize((S, S)), np.float32) / 255.0 - 0.5
    h = bevel(bevel_w) * 0.75 + (fbm(S, S, seed + 10) - 0.5) * 0.07 + brushed * streak * 2
    t = lit(h, ambient=0.34, k=11.0, spec=0.6)
    rgb = ramp_gold(0.12 + 0.88 * t) * dark
    return np.clip(rgb, 0, 1), h


def cell_gold():
    rgb, h = gold_body(3)
    core, halo = inset_line(5.5, 1.3)
    rgb = mix(rgb, j.AMBER, halo * 0.55)
    rgb = mix(rgb, (255, 246, 214), core * 0.95)
    return rgb, opaque()


def cell_gold_dark():
    rgb, h = gold_body(9, dark=0.74)
    rgb = mix(rgb, j.ROSEGOLD, 0.18 * np.ones((S, S), np.float32))
    core, halo = inset_line(5.0, 1.3)
    rgb = mix(rgb, j.ROSEGOLD, halo * 0.5)
    rgb = mix(rgb, (255, 224, 170), core * 0.8)
    return np.clip(rgb, 0, 1), opaque()


def cell_seam():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    t = np.clip(1.0 - r * 0.85, 0, 1) ** 0.8
    rgb = ramp_gold(0.66 + 0.34 * t)
    rgb = rgb * (1 - t[..., None] * 0.6) + j.col((255, 252, 240))[None, None, :] * (t[..., None] * 0.6)
    return np.clip(rgb, 0, 1), opaque()


def cell_pearl():
    u, v = grid01()
    n = fbm(S, S, 21)
    h = bevel(0.14) * 0.6 + (n - 0.5) * 0.04
    t = lit(h, ambient=0.50, k=7.0, spec=0.35)
    base = j.lerp3((176, 184, 226), (255, 246, 240), t)
    sheen = j.iris(u * 0.8 + v * 0.6 + n * 0.8, 0.45)
    rgb = base * 0.80 + sheen * 0.20 * (0.5 + 0.5 * t[..., None])
    core, halo = inset_line(5.0, 1.2)
    rgb = mix(rgb, j.AMBER, halo * 0.45)
    rgb = mix(rgb, (255, 232, 168), core * 0.9)
    return np.clip(rgb, 0, 1), opaque()


def cell_azure():
    u, v = grid01()
    n = fbm(S, S, 31)
    h = bevel(0.12) * 0.5 + (n - 0.5) * 0.05
    t = lit(h, ambient=0.40, k=7.0, spec=0.4)
    base = j.lerp3((38, 54, 128), (104, 132, 224), t)
    rs = np.random.RandomState(5)
    stars = np.zeros((S, S), np.float32)
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    for _ in range(26):
        sx, sy = rs.uniform(14, S - 14), rs.uniform(14, S - 14)
        stars = np.maximum(stars, np.exp(-(((xx - sx) ** 2 + (yy - sy) ** 2) / 1.6)) * rs.uniform(0.5, 1.0))
    rgb = mix(base, (230, 240, 255), stars * 0.85)
    core, halo = inset_line(5.0, 1.3)
    rgb = mix(rgb, j.AMBER, halo * 0.5)
    rgb = mix(rgb, (255, 230, 160), core * 0.9)
    return np.clip(rgb, 0, 1), opaque()


def cell_lavender():
    rgb, _ = cell_blade()
    return np.clip(rgb * np.array([0.92, 0.86, 1.05], np.float32)[None, None, :], 0, 1), opaque()


def cell_feather(warm):
    ss = 3
    W = S * ss
    acc_rgb = np.zeros((W, W, 3), np.float32)
    acc_a = np.zeros((W, W), np.float32)
    left, right, centre = j.feather_mask((W * 0.47, W * 0.97), (W * 0.54, W * 0.03), W * 0.20, -0.16, 1)
    tint_b, tint_t = ((j.PEACH, j.PEARL) if not warm else (j.ROSEGOLD, j.PEACH))
    j.paint_feather(acc_rgb, acc_a, left, right, centre, W, W, 1, tint_b, tint_t, 0.05 + 0.3 * warm, rim_amt=0.72)
    a = np.clip(acc_a + j.blur(acc_a, 2.0) * 0.30, 0, 1)
    rgb = np.where(acc_rgb.sum(axis=2, keepdims=True) > 0.05, acc_rgb, j.col(j.PEARL)[None, None, :])
    rgb = rgb.reshape(S, ss, S, ss, 3).mean((1, 3))
    a = a.reshape(S, ss, S, ss).mean((1, 3))
    a = np.clip(a * 1.15, 0, 1)
    return np.clip(rgb * 1.05, 0, 1), a


def cell_script_band():
    rgb, h = gold_body(41, dark=0.94, bevel_w=0.10, streak=0.01)
    rs = np.random.RandomState(1204)

    def art(d, ss):
        for k, (y0, hh) in enumerate(((0.30, 0.10), (0.50, 0.10), (0.70, 0.10))):
            words = j.script_line(rs, S * ss * 0.92, S * ss * hh, 0.03, 0.0)
            j.draw_words(d, words, lambda s, hh_, y0=y0: (S * ss * 0.04 + s, S * ss * y0 + hh_), 1.5 * ss)
    ink = ss_mask(art)
    core = np.exp(-((np.abs(np.mgrid[0:S, 0:S][0].astype(np.float32) / S - 0.5) - 0.34) / 0.012) ** 2)    # two engraved rules above and below the lines
    rgb = rgb * (1 - 0.55 * ink[..., None])
    rgb = mix(rgb, (90, 44, 20), ink * 0.5)
    glow = j.blur(ink, 2.2)
    rgb = mix(rgb, j.AMBER, glow * 0.5 * (1 - ink))
    rgb = mix(rgb, (255, 244, 210), core * 0.6)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_page():
    u, v = grid01()
    n = fbm(S, S, 51)
    fibre = noise(S, S, 53, 2.5)
    base = j.lerp3((236, 224, 216), (255, 250, 242), np.clip(n * 1.1, 0, 1))
    rgb = base * (0.94 + 0.06 * fibre[..., None])
    edge = j.smooth(0.0, 0.16, edge_dist())
    rgb = rgb * (0.80 + 0.20 * edge[..., None])
    rs = np.random.RandomState(2024)

    def art(d, ss):
        W = S * ss
        for k in range(9):
            y = W * (0.14 + 0.085 * k)
            Lc = W * (0.74 if k != 8 else 0.42)
            words = j.script_line(rs, Lc, W * 0.062, 0.025, 0.0)
            j.draw_words(d, words, lambda s, hh, y=y: (W * 0.13 + s, y + hh), 1.4 * ss)
        d.rectangle([W * 0.09, W * 0.07, W * 0.91, W * 0.93], outline=255, width=int(1.0 * ss))
    ink = ss_mask(art)
    glow = j.blur(ink, 2.4)
    rgb = mix(rgb, j.AMBER, glow * 0.55 * (1 - ink))
    rgb = mix(rgb, (214, 120, 56), ink * 0.85)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_seal():
    rgb, h = gold_body(61, dark=1.0, bevel_w=0.13)
    ss = 4

    def art(d, ss):
        W = S * ss
        c = W / 2
        for r, w in ((0.40, 1.6), (0.33, 1.0), (0.17, 1.4)):
            d.ellipse([c - W * r, c - W * r, c + W * r, c + W * r], outline=255, width=max(1, int(w * ss)))
        for k in range(12):                                   # a rose window of twelve petals
            a = 2 * math.pi * k / 12
            d.line([(c + math.cos(a) * W * 0.17, c + math.sin(a) * W * 0.17), (c + math.cos(a) * W * 0.33, c + math.sin(a) * W * 0.33)], fill=255, width=max(1, int(1.2 * ss)))
            a2 = a + math.pi / 12
            d.line([(c + math.cos(a2) * W * 0.33, c + math.sin(a2) * W * 0.33), (c + math.cos(a2) * W * 0.40, c + math.sin(a2) * W * 0.40)], fill=200, width=max(1, int(1.0 * ss)))
    ink = ss_mask(art, ss)
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2)
    disc = j.smooth(0.43, 0.38, r)
    inner = np.exp(-(r / 0.18) ** 2)
    rgb = mix(rgb, (60, 34, 20), disc * 0.55)                                         # a dark enamel disc so the light reads
    rgb = mix(rgb, j.AMBER, j.blur(ink, 2.0) * disc * 0.8)
    rgb = mix(rgb, (255, 246, 214), ink * disc * 0.95)
    rgb = mix(rgb, (255, 252, 244), inner * 0.9)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_tablet():
    u, v = grid01()
    n = fbm(S, S, 71)
    panel = j.lerp3((52, 66, 148), (112, 138, 226), np.clip(v * 0.9 + (n - 0.5) * 0.5, 0, 1))
    rim = bevel(0.10)
    pearl = j.col(j.PEARL)[None, None, :] * (0.78 + 0.22 * rim[..., None])
    inside = j.smooth(0.12, 0.14, edge_dist())
    rgb = pearl * (1 - inside[..., None]) + panel * inside[..., None]
    rs = np.random.RandomState(88)

    def art(d, ss):
        W = S * ss
        j.draw_emblem(d, W * 0.5, W * 0.23, W * 0.095, ss, 1.1)
        for k in range(6):
            y = W * (0.40 + 0.088 * k)
            Lc = W * (0.62 if k != 5 else 0.34)
            words = j.script_line(rs, Lc, W * 0.056, 0.03, 0.0)
            j.draw_words(d, words, lambda s, hh, y=y: (W * 0.19 + s, y + hh), 1.3 * ss)
    ink = ss_mask(art)
    glow = j.blur(ink, 2.2)
    rgb = mix(rgb, j.AMBER, glow * 0.45 * (1 - ink) * inside)
    rgb = mix(rgb, (255, 236, 176), ink * inside * 0.95)
    core, halo = inset_line(4.0, 1.1)
    rgb = mix(rgb, j.AMBER, halo * 0.5)
    rgb = mix(rgb, (255, 226, 150), core * 0.9)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_blade():
    u, v = grid01()
    n = fbm(S, S, 81)
    # a faceted crystal: a bright core line down the middle (the ridge) and darker sides; V = 0 at the tip
    ridge = np.exp(-(np.abs(u - 0.5) / 0.10) ** 2)
    facet = np.abs(u - 0.5) * 2
    t = np.clip(0.52 + 0.40 * ridge - 0.22 * facet + (n - 0.5) * 0.15 + (1 - v) * 0.12, 0, 1)
    base = j.lerp3((120, 160, 236), (255, 252, 246), t)
    hue = j.iris(v * 0.5 + u * 0.4 + n * 0.5, 0.55)
    rgb = base * 0.78 + hue * 0.22
    for ang, off in ((0.55, 0.0), (-0.45, 0.25), (0.9, -0.2)):                       # a few glints inside the crystal
        line = np.abs((v - 0.5) - (u - 0.5) * math.tan(ang) * 1.2 - off)
        rgb = mix(rgb, (255, 255, 255), np.exp(-(line / 0.018) ** 2) * 0.55)
    rim = j.smooth(0.0, 0.07, edge_dist())
    rgb = rgb * (0.80 + 0.20 * rim[..., None])
    rgb = mix(rgb, (255, 250, 240), np.exp(-(np.abs(u - 0.5) / 0.012) ** 2) * 0.7)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_link():
    rgb, h = gold_body(91, dark=0.96, bevel_w=0.22, streak=0.012)
    u, v = grid01()
    rgb = mix(rgb, (255, 240, 190), np.exp(-(np.abs(v - 0.5) / 0.05) ** 2) * 0.55)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_cover():
    u, v = grid01()
    n = fbm(S, S, 101)
    grain = noise(S, S, 103, 2.2)
    base = j.lerp3((30, 44, 112), (76, 98, 186), np.clip(0.4 + (n - 0.5) * 0.9 + (grain - 0.5) * 0.3, 0, 1))
    rim = bevel(0.12)
    rgb = base * (0.76 + 0.24 * rim[..., None])

    def art(d, ss):
        W = S * ss
        for sx in (0, 1):                                                              # corner filigree: arcs and curls in the four corners
            for sy in (0, 1):
                cx, cy = W * (0.0 if sx == 0 else 1.0), W * (0.0 if sy == 0 else 1.0)
                for r in (0.20, 0.27):
                    d.arc([cx - W * r, cy - W * r, cx + W * r, cy + W * r], 0, 360, fill=255, width=max(1, int(1.2 * ss)))
        d.rectangle([W * 0.07, W * 0.07, W * 0.93, W * 0.93], outline=230, width=max(1, int(1.4 * ss)))
        d.rectangle([W * 0.10, W * 0.10, W * 0.90, W * 0.90], outline=150, width=max(1, int(0.8 * ss)))
        j.draw_emblem(d, W * 0.5, W * 0.5, W * 0.23, ss, 1.6)
    ink = ss_mask(art)
    glow = j.blur(ink, 2.0)
    rgb = mix(rgb, j.AMBER, glow * 0.55 * (1 - ink))
    rgb = mix(rgb, (255, 232, 160), ink * 0.95)
    return np.clip(rgb, 0.02, 1), opaque()


def cell_enamel():
    u, v = grid01()
    n = fbm(S, S, 111)
    h = bevel(0.10) * 0.45 + (n - 0.5) * 0.04
    t = lit(h, ambient=0.52, k=6.0, spec=0.3)
    base = j.lerp3((182, 190, 232), (255, 248, 244), t)
    sheen = j.iris(u * 0.5 + v * 0.9 + n * 0.7, 0.5)
    rgb = base * 0.84 + sheen * 0.16
    for y in (0.07, 0.13, 0.87, 0.93):                                                 # gold trim lines near the top and the bottom edge
        line = np.exp(-(np.abs(v - y) / 0.012) ** 2)
        rgb = mix(rgb, j.AMBER, line * 0.55)
        rgb = mix(rgb, (255, 238, 180), line * 0.7)
    mid = np.exp(-(np.abs(u - 0.5) / 0.022) ** 2) * j.smooth(0.0, 0.1, v) * j.smooth(1.0, 0.9, v)     # a vertical inlay down the middle
    rgb = mix(rgb, j.AMBER, np.exp(-(np.abs(u - 0.5) / 0.06) ** 2) * 0.4)
    rgb = mix(rgb, (255, 240, 190), mid * 0.9)
    return np.clip(rgb, 0.02, 1), opaque()


CELLS = [cell_gold, cell_gold_dark, cell_seam, cell_pearl, cell_azure, cell_lavender, lambda: cell_feather(0), lambda: cell_feather(1), cell_script_band, cell_page, cell_seal, cell_tablet, cell_blade, cell_link, cell_cover, cell_enamel]


def tex_atlas():
    P = np.zeros((4 * S, 4 * S, 3), np.float32)
    A = np.zeros((4 * S, 4 * S), np.float32)
    for i, fn in enumerate(CELLS):
        rgb, a = fn()
        cx, cy = i % 4, i // 4
        P[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = rgb
        A[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = a
    j.save(P, A, "jibril_model_atlas")


ALL = {"atlas": tex_atlas}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()

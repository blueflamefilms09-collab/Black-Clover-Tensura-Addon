"""Generates the material atlas of the 3D models of "Hercules, Lord of Martial Prowess" (3.20.0: vfx.client.layer.HerculesModels / HerculesMeshDraw / HerculesLayer).

Deterministic and font-free (every machine writes the same pixels). Written for this mod, same palette as tools/gen_hercules_vfx_textures.py (burnished bronze, bright gold, warm ivory marble,
ember orange, a little deep crimson). It only ADDS two files; the 17 flat sprites of gen_hercules_vfx_textures.py stay and are still drawn when the setting `herculesModels` is off.
Output: src/main/resources/assets/nusmp/textures/particle/hercules_model_*.png

    python tools/gen_hercules_model_textures.py            # all
    python tools/gen_hercules_model_textures.py atlas      # only the atlas

    hercules_model_atlas     4 x 4 cells of 128 px (512 x 512), alpha-blended, lit like metal / stone from the upper left and shaded again by the layer per face:
        0 bronze plate with gold seams and rivets       1 dark bronze finger plate with a ridge and gold joint lines    2 glowing gold seam
        3 ivory marble                                 4 basalt with ember cracks                                      5 grey-brown rock
        6 laurel leaf (cut out)                        7 bronze band with a laurel chevron                             8 knuckle plate with a raised boss
        9 white-gold / ember heat                      10 palm plate with engraved lines                               11 rock with gold cracks
        12 bronze laurel leaf (cut out)                13 crimson cloth ribbon                                         14 polished bronze with a gold inlay
        15 dark basalt
    hercules_model_fissure   a ground fissure strip: dark broken lips round a glowing core (U across, V along), 64 x 256, alpha-blended (tinted by the layer as it cools)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_hercules_vfx_textures as g   # the palette, the line-art mask and the metal ramp of the flat sprites (imported, not changed)

S = 128
CELL = {"plate": 0, "plate_dark": 1, "seam": 2, "ivory": 3, "ember_rock": 4, "rock": 5, "leaf": 6, "band": 7, "knuckle": 8, "hot": 9, "palm": 10, "rock_crack": 11, "leaf_b": 12, "crimson": 13, "plate_light": 14, "rock_dark": 15}


def grid01(w=S, h=S):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def edge_dist(w=S, h=S):
    u, v = grid01(w, h)
    return np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))


def bevel(width=0.14):
    d = edge_dist()
    return g.smooth(0.0, width, d)


def seam_line(inset_px, w_px, tint=g.GOLD, strength=1.0):
    """A glowing gold line a few pixels inside the cell edge (the seam between two plates)."""
    d = edge_dist() * S
    core = np.exp(-((d - inset_px) / (w_px * 0.55)) ** 2)
    halo = np.exp(-((d - inset_px) / (w_px * 1.9)) ** 2) * 0.45
    return core * strength, halo * strength


def rivets(rgb, pts, r=3.2):
    xx, yy = g.grid(S, S)
    for (px, py) in pts:
        d = np.sqrt((xx - px) ** 2 + (yy - py) ** 2)
        m = g.smooth(r, r - 1.4, d)
        hi = np.exp(-(((xx - px + 1.0) ** 2 + (yy - py + 1.0) ** 2) / (r * 0.45) ** 2))
        rgb = rgb * (1 - 0.7 * m[..., None]) + g.col(g.BRONZE_L)[None, None, :] * (0.7 * m[..., None]) + g.col(g.IVORY)[None, None, :] * (hi * m * 0.8)[..., None]
        ring = g.smooth(r + 1.6, r, d) * (1 - m)
        rgb = rgb * (1 - 0.45 * ring[..., None])
    return rgb


def over(rgb, add_rgb, amount):
    return np.clip(rgb + add_rgb[None, None, :] * amount[..., None], 0, 1)


def mix_over(rgb, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return rgb * (1 - a) + g.col(colour)[None, None, :] * a


# ---------------------------------------------------------------------------------------------------- the cells
def metal_cell(seed, bevel_w=0.13, hammer=0.10, k=9.0, dark=1.0, ambient=0.16):
    h = bevel(bevel_w) * 0.7 + (g.fbm(S, S, seed, cells=(32, 16, 8, 4), gains=(0.4, 0.3, 0.2, 0.1)) - 0.5) * hammer
    rgb = g.lit_metal(h, k=k, ambient=ambient, spec_gain=0.45)
    return np.clip(rgb * dark, 0, 1)


def cell_plate():
    rgb = metal_cell(3)
    core, halo = seam_line(4.2, 2.4)
    rgb = mix_over(rgb, g.GOLD, halo * 0.5)
    rgb = mix_over(rgb, g.GOLD_L, core)
    rgb = rivets(rgb, [(15, 15), (S - 15, 15), (15, S - 15), (S - 15, S - 15)])
    return rgb, np.ones((S, S), np.float32)


def cell_plate_dark():
    rgb = metal_cell(11, bevel_w=0.10, dark=0.66, ambient=0.12)
    u, v = grid01()
    ridge = np.exp(-((u - 0.5) / 0.035) ** 2) * 0.9                                # the ridge down a finger
    rgb = over(rgb, g.col(g.BRONZE_L) * 0.9, ridge * 0.7)
    rgb = rgb * (1 - 0.35 * g.smooth(0.0, 0.12, np.abs(u - 0.5)) * 0)[..., None]
    for yv in (3.4, S - 3.4):                                                      # gold joint lines at both ends (where one phalanx meets the next)
        d = np.abs(np.mgrid[0:S, 0:S][0].astype(np.float32) + 0.5 - yv)
        core = np.exp(-(d / 1.5) ** 2)
        halo = np.exp(-(d / 4.2) ** 2) * 0.4
        rgb = mix_over(rgb, g.GOLD, halo * 0.5)
        rgb = mix_over(rgb, g.GOLD_L, core)
    edge = g.smooth(0.18, 0.0, np.minimum(u, 1 - u))
    rgb = rgb * (1 - 0.35 * edge[..., None])
    return rgb, np.ones((S, S), np.float32)


def cell_seam():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    t = np.clip(1.0 - r * 0.9, 0, 1)
    rgb = g.ramp(0.62 + 0.38 * t)
    fine = 0.9 + 0.1 * np.sin(v * 90)
    return np.clip(rgb * fine[..., None], 0, 1), np.ones((S, S), np.float32)


def cell_ivory():
    u, v = grid01()
    n = g.fbm(S, S, 21, cells=(64, 32, 16), gains=(0.5, 0.3, 0.2))
    n2 = g.fbm(S, S, 29, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2))
    veins = np.exp(-(np.abs(np.sin((u * 3.1 + v * 1.7 + n * 2.6) * math.pi)) / 0.07) ** 2) * 0.55
    veins += np.exp(-(np.abs(np.sin((u * -1.4 + v * 3.3 + n2 * 2.2) * math.pi)) / 0.05) ** 2) * 0.35
    base = g.col(g.IVORY)[None, None, :] * (0.86 + 0.14 * n[..., None])
    rgb = base * (1 - 0.35 * veins[..., None]) + g.col((176, 140, 90))[None, None, :] * (0.25 * veins[..., None])
    b = bevel(0.12)
    rgb = rgb * (0.72 + 0.28 * b[..., None])
    core, halo = seam_line(3.6, 2.0)
    rgb = mix_over(rgb, g.GOLD, halo * 0.4 + core * 0.8)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def voronoi_cracks(seed, n_pts=11, width=0.045):
    """Crack lines = cells of a wrapped voronoi diagram: (F2 - F1) small. Returns the crack mask 0..1 and F1."""
    rs = np.random.RandomState(seed)
    pts = rs.rand(n_pts, 2).astype(np.float32)
    u, v = grid01()
    d = []
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for p in pts:
                d.append(np.sqrt((u - (p[0] + dx)) ** 2 + (v - (p[1] + dy)) ** 2))
    d = np.sort(np.stack(d, 0), 0)
    diff = d[1] - d[0]
    mask = np.exp(-(diff / width) ** 2)
    return mask, d[0]


def stone(seed, light=1.0):
    n = g.fbm(S, S, seed, cells=(32, 16, 8, 4), gains=(0.35, 0.3, 0.2, 0.15))
    h = n * 0.8
    gy, gx = np.gradient(g.blurf(h, 1.2))
    nx, ny, nz = -gx * 14, -gy * 14, np.ones_like(h)
    nn = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * -0.5 + ny * -0.62 + nz * 0.6) / nn, 0, 1)
    dark = g.col(g.STONE_D)[None, None, :]
    lite = g.col((214, 190, 156))[None, None, :]
    rgb = (dark * (1 - lam[..., None]) + lite * lam[..., None]) * (0.78 + 0.34 * n[..., None]) * light
    return np.clip(rgb, 0, 1)


def cell_rock():
    return stone(41), np.ones((S, S), np.float32)


def cell_rock_dark():
    return stone(47, light=0.55) * np.array([0.9, 0.85, 0.82], np.float32)[None, None, :], np.ones((S, S), np.float32)


def cell_rock_crack():
    rgb = stone(53, light=0.8)
    cr, f1 = voronoi_cracks(7, 9, 0.032)
    wide = voronoi_cracks(7, 9, 0.085)[0]
    rgb = rgb * (1 - 0.5 * wide[..., None])
    rgb = mix_over(rgb, g.GOLD, wide * 0.45)
    rgb = mix_over(rgb, g.GOLD_L, cr)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_ember_rock():
    base = stone(59, light=0.38) * np.array([0.8, 0.72, 0.7], np.float32)[None, None, :]
    cr, f1 = voronoi_cracks(13, 12, 0.03)
    wide = voronoi_cracks(13, 12, 0.10)[0]
    rgb = base * (1 - 0.35 * wide[..., None])
    rgb = mix_over(rgb, g.EMBER, wide * 0.55)
    rgb = mix_over(rgb, g.GOLD, cr * 0.9)
    rgb = mix_over(rgb, g.GOLD_L, np.clip(cr - 0.55, 0, 1) * 2.0)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def leaf_cell(bronze, seed):
    """A laurel leaf, base at the bottom (V = 1) and tip at the top, cut out: two halves folded along the vein, lit from the upper left."""
    ss = 3
    W = S * ss
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    u = (xx + 0.5) / W
    v = (yy + 0.5) / W
    t = np.clip(1.0 - (v - 0.04) / 0.92, 0, 1)                                       # 0 at the base, 1 at the tip
    halfw = 0.5 * 0.66 * np.sin(math.pi * np.clip(t, 0, 1) ** 0.8) * (1 - 0.15 * t)
    # a gentle curve of the leaf
    cx = 0.5 + 0.05 * np.sin(t * math.pi)
    dx = u - cx
    inside = (np.abs(dx) < halfw) & (v > 0.03) & (v < 0.97)
    edge = np.clip((halfw - np.abs(dx)) * W / 1.6, 0, 1)
    alpha = np.where((v > 0.03) & (v < 0.97), edge, 0.0)
    side = np.sign(dx)
    fold = np.clip(np.abs(dx) / np.maximum(halfw, 1e-3), 0, 1)
    lam = 0.55 + 0.35 * (-side) * (1 - fold) + 0.2 * (1 - fold)
    lam = np.clip(lam, 0, 1)
    veins = np.exp(-((np.abs(dx) / 0.012)) ** 2) * 0.9
    sec = np.exp(-(np.abs(np.sin((t * 9.0 - np.abs(dx) * 14.0) * math.pi))) ** 2 / 0.025) * 0.22 * (fold < 0.95)
    tt = np.clip(0.18 + 0.7 * lam + 0.18 * veins - 0.1 * sec, 0, 1)
    if bronze:
        tt = tt * 0.78
    rgb = g.ramp(tt)
    rgb = np.where(inside[..., None], rgb, g.col(g.BRONZE_D)[None, None, :])
    rim = np.clip(1 - np.clip((halfw - np.abs(dx)) * W / 3.0, 0, 1), 0, 1) * inside
    rgb = mix_over(rgb, g.GOLD_L if not bronze else g.GOLD, rim * 0.7)
    # downsample
    rgb = rgb.reshape(S, ss, S, ss, 3).mean((1, 3))
    alpha = alpha.reshape(S, ss, S, ss).mean((1, 3))
    return np.clip(rgb, 0, 1), np.clip(alpha, 0, 1)


def cell_leaf():
    return leaf_cell(False, 1)


def cell_leaf_b():
    return leaf_cell(True, 2)


def cell_band():
    rgb = metal_cell(61, bevel_w=0.11, dark=0.9)
    u, v = grid01()
    for k in range(3):                                                              # laurel chevrons: three rows of gold V strokes
        cy = 0.2 + 0.3 * k
        line = np.abs((v - cy) - np.abs(u - 0.5) * 0.55)
        core = np.exp(-(line / 0.022) ** 2)
        halo = np.exp(-(line / 0.06) ** 2) * 0.4
        rgb = mix_over(rgb, g.GOLD, halo * 0.4)
        rgb = mix_over(rgb, g.GOLD_L, core * 0.95)
    core, halo = seam_line(3.2, 2.0)
    rgb = mix_over(rgb, g.GOLD, halo * 0.35 + core * 0.7)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_knuckle():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2)
    boss = np.sqrt(np.clip(1 - (r / 0.40) ** 2, 0, 1)) * (r < 0.40)
    h = bevel(0.12) * 0.55 + boss * 0.75 + (g.fbm(S, S, 71, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2)) - 0.5) * 0.06
    rgb = g.lit_metal(h, k=9.0, ambient=0.16, spec_gain=0.6)
    ring = np.exp(-((r - 0.42) / 0.018) ** 2)
    rgb = mix_over(rgb, g.GOLD_L, ring * 0.9)
    core, halo = seam_line(3.6, 2.2)
    rgb = mix_over(rgb, g.GOLD, halo * 0.4 + core * 0.9)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_hot():
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    t = np.clip(1.0 - r, 0, 1) ** 0.7
    rgb = g.ramp(0.55 + 0.45 * t)
    rgb = rgb * (1 - t[..., None]) + g.col((255, 250, 230))[None, None, :] * (t[..., None] * 0.75) + rgb * (t[..., None] * 0.25)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_palm():
    rgb = metal_cell(83, bevel_w=0.10, dark=0.78)
    u, v = grid01()
    for k, rr in enumerate((0.55, 0.38, 0.22)):                                      # engraved arcs
        arc = np.abs(np.sqrt((u - 0.5) ** 2 + (v - 1.05) ** 2) - rr)
        core = np.exp(-(arc / 0.012) ** 2)
        rgb = rgb * (1 - 0.4 * np.exp(-(arc / 0.03) ** 2)[..., None])
        rgb = mix_over(rgb, g.GOLD, core * 0.8)
    core, halo = seam_line(4.0, 2.2)
    rgb = mix_over(rgb, g.GOLD, halo * 0.4 + core * 0.9)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_crimson():
    u, v = grid01()
    folds = 0.5 + 0.5 * np.sin((u * 6.0 + g.fbm(S, S, 91, cells=(32, 16), gains=(0.6, 0.4)) * 2.0) * math.pi)
    rgb = g.col(g.CRIMSON)[None, None, :] * (0.55 + 0.55 * folds[..., None])
    rgb = mix_over(rgb, (214, 60, 52), g.smooth(0.55, 1.0, folds) * 0.35)
    edge = bevel(0.10)
    rgb = rgb * (0.7 + 0.3 * edge[..., None])
    core, halo = seam_line(3.2, 2.0)
    rgb = mix_over(rgb, g.GOLD, halo * 0.3 + core * 0.8)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


def cell_plate_light():
    h = bevel(0.12) * 0.7 + (g.fbm(S, S, 97, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2)) - 0.5) * 0.05
    rgb = g.lit_metal(h, k=8.0, ambient=0.34, spec_gain=0.7)
    u, v = grid01()
    for cx in (0.5,):                                                               # a gold inlay cross
        a = np.exp(-(np.abs(u - cx) / 0.02) ** 2) * g.smooth(0.12, 0.2, v) * g.smooth(0.12, 0.2, 1 - v)
        b = np.exp(-(np.abs(v - 0.5) / 0.02) ** 2) * g.smooth(0.12, 0.2, u) * g.smooth(0.12, 0.2, 1 - u)
        rgb = mix_over(rgb, g.GOLD, np.maximum(a, b) * 0.95)
    core, halo = seam_line(4.0, 2.4)
    rgb = mix_over(rgb, g.GOLD, halo * 0.45)
    rgb = mix_over(rgb, g.GOLD_L, core)
    return np.clip(rgb, 0, 1), np.ones((S, S), np.float32)


CELLS = [cell_plate, cell_plate_dark, cell_seam, cell_ivory, cell_ember_rock, cell_rock, cell_leaf, cell_band, cell_knuckle, cell_hot, cell_palm, cell_rock_crack, cell_leaf_b, cell_crimson, cell_plate_light, cell_rock_dark]


def tex_atlas():
    P = np.zeros((4 * S, 4 * S, 3), np.float32)
    A = np.zeros((4 * S, 4 * S), np.float32)
    for i, fn in enumerate(CELLS):
        rgb, a = fn()
        cx, cy = i % 4, i // 4
        P[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = rgb
        A[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = a
    g.save(P, A, "hercules_model_atlas")


def tex_fissure():
    W, H = 64, 256
    u, v = grid01(W, H)
    n = g.fbm(W, H, 131, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2))
    n2 = g.fbm(W, H, 137, cells=(16, 8, 4), gains=(0.5, 0.3, 0.2))
    x = (u - 0.5) * 2
    wob = (n - 0.5) * 0.35
    ax = np.abs(x - wob)
    core = g.smooth(0.36, 0.08, ax)                                                  # the glowing gap
    lip_w = 0.70 + 0.28 * n2
    lip = g.smooth(lip_w, 0.18, ax) * (1 - core)                                     # broken dark lips
    ends = g.smooth(0.0, 0.10, v) * g.smooth(1.0, 0.86, v)
    inner = np.exp(-(ax / 0.14) ** 2)
    rgb = g.col((30, 20, 14))[None, None, :] * (lip * (0.6 + 0.6 * n2))[..., None]
    glow = g.ramp(0.62 + 0.38 * inner)
    rgb = rgb * (1 - core[..., None]) + glow * core[..., None]
    rgb = rgb + g.col(g.EMBER)[None, None, :] * (np.exp(-(ax / 0.30) ** 2) * 0.25 * (1 - core))[..., None]
    a = np.clip((lip * 0.88 + core) * ends, 0, 1)
    g.save(np.clip(rgb, 0, 1), a, "hercules_model_fissure")


ALL = {"atlas": tex_atlas, "fissure": tex_fissure}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()

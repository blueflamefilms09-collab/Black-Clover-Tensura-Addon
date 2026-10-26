"""Animated Israfil (Lord of Trials) Ultimate skill icon: a cyan labyrinth of broken rings whose wall gaps shift, a lit brass
trumpet at the centre sending sound-rings out through the maze, a brass rim with rivets and gems, drifting motes and mist.
Original art drawn from scratch with numpy + Pillow (see tools/icon_anim_lib.py).  Run: python3 tools/gen_israfil_icon_anim.py

Layers (every motion is a whole number of cycles per 3.2 s loop):
  1. maze rings   5 rings turning at integer speeds (2, -1, 1, -2, 1 turns); at t = 0 every main gap lines up on the bell axis
  2. sound rings  3 cyan arcs leave the bell mouth, 2 pulses per loop; they also flash the maze walls they pass
  3. core + bell  breathing cyan core behind the horn, bell mouth glow synced with the pulses
  4. motes        26 cyan motes drifting through the maze (1-2 cycles each, smooth fade in/out)
  5. mist + grid  slow mist and a faint lattice at the edges, a travelling shimmer
  6. light sweep  one soft specular band across the brass horn and a second one across the rim; gems twinkle
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from icon_anim_lib import build  # noqa: E402

TAU = 2 * math.pi
S0 = 256
CEN = (S0 - 1) / 2.0
YY, XX = np.mgrid[0:S0, 0:S0].astype(np.float64)
DX, DY = XX - CEN, YY - CEN
RR = np.hypot(DX, DY)
AA = np.arctan2(DY, DX)

L2 = np.array([-0.55, -0.83])           # screen-space direction towards the light (up-left)
L2 /= np.linalg.norm(L2)
LZ = 0.62
H3 = np.array([L2[0] * 0.78, L2[1] * 0.78, LZ + 1.0])
H3 /= np.linalg.norm(H3)

BRASS_POS = [0.0, 0.2, 0.45, 0.7, 0.9, 1.0]
BRASS = np.array([(30, 13, 10), (92, 47, 14), (176, 112, 30), (234, 168, 62), (255, 216, 124), (255, 242, 196)], np.float64)
CYAN_GEM = np.array([(4, 40, 70), (12, 110, 170), (40, 200, 235), (150, 245, 255), (255, 255, 255)], np.float64)
CYAN_POS = [0.0, 0.3, 0.6, 0.85, 1.0]


def ramp(v, pos, cols):
    v = np.clip(v, 0, 1)
    return np.stack([np.interp(v, pos, cols[:, k]) for k in range(3)], axis=-1)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def over(dst_rgb, dst_a, src_rgb, src_a):
    """Straight-alpha 'over' on float arrays; returns (rgb, a)."""
    a = src_a + dst_a * (1 - src_a)
    rgb = (src_rgb * src_a[..., None] + dst_rgb * (dst_a * (1 - src_a))[..., None]) / np.maximum(a, 1e-6)[..., None]
    return rgb, a


def blur(arr, radius):
    """Gaussian blur of a float RGB (or single channel) array via PIL (values clipped to 0..255)."""
    a = np.clip(arr, 0, 255).astype(np.uint8)
    if a.ndim == 2:
        return np.asarray(Image.fromarray(a, "L").filter(ImageFilter.GaussianBlur(radius)), np.float64)
    return np.asarray(Image.fromarray(a, "RGB").filter(ImageFilter.GaussianBlur(radius)), np.float64)


# ------------------------------------------------------------------------------------------------ shapes with 3D shading
def shade_brass(d, spec, s, lc, edge, refl_strength=0.5):
    col = ramp(d, BRASS_POS, BRASS)
    refl = np.clip(-s * lc, 0, 1) * smooth(0.35, 1.0, np.abs(s))
    col = col + np.array([40, 175, 215]) * (0.5 * refl_strength * refl)[..., None]
    col = col + np.array([255, 244, 205]) * (0.95 * spec)[..., None]
    ol = np.clip(edge / 1.5, 0, 1)
    col = col * (0.42 + 0.58 * ol)[..., None]
    return np.clip(col, 0, 255)


def tube(P, W, band=None, gloss=1.0):
    """A shaded brass tube along polyline P (N,2) with half widths W (N,). Returns full-size rgb, alpha, sweep weight."""
    P = np.asarray(P, np.float64)
    W = np.asarray(W, np.float64) * SC * 1.2
    n = len(P)
    T = np.gradient(P, axis=0)
    T /= np.maximum(np.linalg.norm(T, axis=1, keepdims=True), 1e-9)
    Nr = np.stack([-T[:, 1], T[:, 0]], axis=1)
    x0 = int(max(0, math.floor(P[:, 0].min() - W.max() - 3)))
    x1 = int(min(S0, math.ceil(P[:, 0].max() + W.max() + 3)))
    y0 = int(max(0, math.floor(P[:, 1].min() - W.max() - 3)))
    y1 = int(min(S0, math.ceil(P[:, 1].max() + W.max() + 3)))
    px, py = XX[y0:y1, x0:x1], YY[y0:y1, x0:x1]
    d2 = (px[..., None] - P[:, 0]) ** 2 + (py[..., None] - P[:, 1]) ** 2
    j = np.argmin(d2, axis=-1)
    rx, ry = px - P[j, 0], py - P[j, 1]
    along = rx * T[j, 0] + ry * T[j, 1]
    lat = rx * Nr[j, 0] + ry * Nr[j, 1]
    w = W[j]
    edge = w - np.abs(lat)
    edge = np.where(j == 0, np.minimum(edge, along), edge)
    edge = np.where(j == n - 1, np.minimum(edge, -along), edge)
    alpha = np.clip(edge + 0.5, 0, 1)
    s = np.clip(lat / w, -1, 1)
    nz = np.sqrt(1 - s * s)
    lc = Nr[j, 0] * L2[0] + Nr[j, 1] * L2[1]
    diff = s * lc * 0.78 + nz * LZ
    bb = band[j] if band is not None else 0.0
    d = np.clip(diff * 0.95 + 0.3 + 0.16 * bb, 0, 1)
    hn = s * (Nr[j, 0] * H3[0] + Nr[j, 1] * H3[1]) + nz * H3[2]
    spec = np.clip(hn, 0, 1) ** 38 * gloss + np.clip(hn, 0, 1) ** 8 * 0.12 * gloss
    col = shade_brass(d, spec, s, lc, edge)
    rgb = np.zeros((S0, S0, 3))
    a = np.zeros((S0, S0))
    sw = np.zeros((S0, S0))
    rgb[y0:y1, x0:x1] = col
    a[y0:y1, x0:x1] = alpha
    sw[y0:y1, x0:x1] = 0.3 + 0.7 * d
    return rgb, a, sw


def sphere(cx, cy, r, cols, pos, gloss=1.0):
    x0, x1 = int(max(0, cx - r - 3)), int(min(S0, cx + r + 4))
    y0, y1 = int(max(0, cy - r - 3)), int(min(S0, cy + r + 4))
    px, py = XX[y0:y1, x0:x1] - cx, YY[y0:y1, x0:x1] - cy
    dist = np.hypot(px, py)
    edge = r - dist
    alpha = np.clip(edge + 0.5, 0, 1)
    nx, ny = px / r, py / r
    nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1))
    diff = nx * L2[0] * 0.78 + ny * L2[1] * 0.78 + nz * LZ
    d = np.clip(diff * 0.9 + 0.22, 0, 1)
    hn = nx * H3[0] + ny * H3[1] + nz * H3[2]
    spec = np.clip(hn, 0, 1) ** 30 * gloss
    col = ramp(d, pos, cols) + np.array([255, 250, 235]) * spec[..., None]
    refl = np.clip(-(nx * L2[0] + ny * L2[1]), 0, 1) * smooth(0.5, 1, dist / r)
    if cols is BRASS:
        col = col + np.array([40, 175, 215]) * (0.28 * refl)[..., None]
    col = col * (0.5 + 0.5 * np.clip(edge / 1.2, 0, 1))[..., None]
    rgb = np.zeros((S0, S0, 3))
    a = np.zeros((S0, S0))
    rgb[y0:y1, x0:x1] = np.clip(col, 0, 255)
    a[y0:y1, x0:x1] = alpha
    return rgb, a


# ------------------------------------------------------------------------------------------------ the trumpet (static)
THETA = math.radians(-32.0)
CT, ST = math.cos(THETA), math.sin(THETA)
TC = np.array([123.0, 132.0])
SC = 1.22                                      # horn scale
AXIS = np.array([CT, ST])


def loc(x, y):
    return TC + SC * np.array([x * CT - y * ST, x * ST + y * CT])


def loc_arr(pts):
    pts = np.asarray(pts, np.float64)
    return np.stack([TC[0] + SC * (pts[:, 0] * CT - pts[:, 1] * ST), TC[1] + SC * (pts[:, 0] * ST + pts[:, 1] * CT)], axis=1)


def line_pts(a, b, step=0.5):
    n = max(2, int(math.hypot(b[0] - a[0], b[1] - a[1]) / step))
    return [(a[0] + (b[0] - a[0]) * i / n, a[1] + (b[1] - a[1]) * i / n) for i in range(n + 1)]


def build_trumpet():
    layers = []                                    # (rgb, a, sw)

    # coil loop under the main tube
    pts = line_pts((18, 0), (-30, 0))
    pts += [(-30 + 11 * math.cos(th), 11 + 11 * math.sin(th)) for th in np.linspace(-math.pi / 2, -3 * math.pi / 2, 60)]
    pts += line_pts((-30, 22), (18, 22))
    pts += [(18 + 11 * math.cos(th), 11 + 11 * math.sin(th)) for th in np.linspace(math.pi / 2, -math.pi / 2, 60)]
    loop_w = np.full(len(pts), 3.5)
    band = np.zeros(len(pts))
    layers.append(tube(loc_arr(pts), loop_w, band))
    # loop ferrules (straight bits near the ends)
    for x in (-10.0, 4.0):
        layers.append(tube(loc_arr(line_pts((x, 22 - 0.1), (x + 2.6, 22 - 0.1))), np.full(len(line_pts((x, 22), (x + 2.6, 22))), 4.7),
                           np.ones(len(line_pts((x, 22), (x + 2.6, 22))))))

    # mouthpiece
    xs = np.linspace(-70, -55, 40)
    w = np.where(xs < -60, 3.1 + 2.9 * (np.clip(-60 - xs, 0, None) / 13.0) ** 1.6, 3.1)
    bnd = np.where(xs < -67.5, 1.0, 0.0)
    layers.append(tube(loc_arr([(x, 0) for x in xs]), w, bnd))

    # main tube + bell as one tube
    xs = np.arange(-58, 64.01, 0.5)
    w = 3.9 + np.where(xs > 24, 20.5 * (np.clip((xs - 24) / 40.0, 0, 1) ** 2.15), 0.0)
    bnd = np.zeros(len(xs))
    for fx, fw, fb in ((-54, 3, 1.3), (-47, 2, 1.0), (22, 3, 1.3), (60.5, 5, 1.6)):
        k = np.exp(-(((xs - fx) / (fw * 0.5)) ** 2))
        w = w + fb * k
        bnd = np.maximum(bnd, k)
    layers.append(tube(loc_arr([(x, 0) for x in xs]), w, bnd))

    # three piston valves with caps, stems and cyan gems
    gems = []
    for vx in (-20.0, -6.0, 8.0):
        c = line_pts((vx, -2), (vx, -16.5))
        layers.append(tube(loc_arr(c), np.full(len(c), 4.3), np.zeros(len(c))))
        c = line_pts((vx, -16.5), (vx, -20.5))
        layers.append(tube(loc_arr(c), np.full(len(c), 5.7), np.ones(len(c))))
        c = line_pts((vx, -20.5), (vx, -24.5))
        layers.append(tube(loc_arr(c), np.full(len(c), 1.9), np.zeros(len(c))))
        g = loc(vx, -27.5)
        gems.append(g)
        _gr, _ga = sphere(g[0], g[1], 3.9 * SC, CYAN_GEM, CYAN_POS)
        layers.append((_gr, _ga, np.zeros_like(_ga)))

    rgb = np.zeros((S0, S0, 3))
    a = np.zeros((S0, S0))
    sw = np.zeros((S0, S0))
    for lr, la, ls in layers:
        rgb, a = over(rgb, a, lr, la)
        sw = np.where(la > 0.5, ls, sw)
    return rgb, a, sw, gems


TR_RGB, TR_A, TR_SW, VALVE_GEMS = build_trumpet()
MOUTH = loc(65.5, 0)                                   # centre of the bell mouth
SRC = MOUTH + AXIS * 6.0                               # where the sound-rings start
# soft dark shadow so the horn separates from the bright maze
SHADOW = np.clip(blur(np.where(TR_A > 0.3, 255.0, 0.0), 5.5) / 255.0 * 1.7, 0, 1)
_sh = np.zeros_like(SHADOW)
_sh[3:, 2:] = SHADOW[:-3, :-2]                         # offset (+2, +3)
SHADOW = np.clip(_sh * 0.8, 0, 1)

# ------------------------------------------------------------------------------------------------ static backdrop + rim
LIGHT_DOT = (DX * L2[0] + DY * L2[1]) / 120.0


def build_background():
    r = RR / 120.0
    pos = [0.0, 0.35, 0.65, 0.88, 1.0]
    cols = np.array([(30, 30, 104), (20, 18, 80), (12, 9, 52), (7, 5, 33), (4, 3, 18)], np.float64)
    bg = ramp(r, pos, cols) + np.array([10, 14, 34]) * (np.clip(LIGHT_DOT, -1, 1) * 0.5 + 0.2)[..., None] * (1 - r)[..., None]
    return bg


BG = build_background()

# rim: a brass torus between r 106 and 118.5 with engraved ticks, rivets and four cyan gems
RIM_IN, RIM_OUT = 105.5, 118.8
RIM_MID = 0.5 * (RIM_IN + RIM_OUT)
RIM_HW = 0.5 * (RIM_OUT - RIM_IN)


def build_rim():
    s = np.clip((RR - RIM_MID) / RIM_HW, -1, 1)
    nz = np.sqrt(1 - s * s)
    radx, rady = DX / np.maximum(RR, 1e-6), DY / np.maximum(RR, 1e-6)
    lc = radx * L2[0] + rady * L2[1]
    diff = s * lc * 0.8 + nz * 0.55
    d = np.clip(diff * 0.9 + 0.26, 0, 1)
    hn = s * (radx * H3[0] + rady * H3[1]) + nz * H3[2]
    spec = np.clip(hn, 0, 1) ** 24 * 0.8
    edge = RIM_HW - np.abs(RR - RIM_MID)
    alpha = np.clip(edge + 0.5, 0, 1)
    col = ramp(d, BRASS_POS, BRASS) + np.array([255, 244, 205]) * spec[..., None]
    refl = np.clip(-s * lc, 0, 1) * smooth(0.3, 1, np.abs(s))
    col = col + np.array([30, 150, 200]) * (0.25 * refl)[..., None]
    # engraved ticks on the middle of the band: 96 fine ticks, every 8th one longer, plus a pair of fine grooves
    ang = (AA % TAU) / TAU * 96.0
    tick = np.abs(((ang + 0.5) % 1.0) - 0.5)               # 0 at the tick centre
    longt = (np.round(ang) % 8 == 0)
    ext = np.where(longt, 4.6, 2.7)
    tmask = smooth(0.2, 0.06, tick) * smooth(ext + 0.8, ext - 0.2, np.abs(RR - RIM_MID))
    groove = np.exp(-(((RR - (RIM_MID + 5.2)) / 0.55) ** 2)) + np.exp(-(((RR - (RIM_MID - 5.2)) / 0.55) ** 2))
    col = col * (1 - 0.55 * tmask - 0.45 * groove)[..., None]
    col = col * (0.5 + 0.5 * np.clip(edge / 1.3, 0, 1))[..., None]
    rgb = np.clip(col, 0, 255)
    a = alpha
    # rivets and gems
    gems = []
    rv = RIM_MID
    for k in range(12):
        ang_k = k * math.pi / 6
        cx, cy = CEN + rv * math.cos(ang_k), CEN + rv * math.sin(ang_k)
        if k % 3 == 0:
            br, ba = sphere(cx, cy, 6.8, BRASS, BRASS_POS, 1.2)
            rgb, a = over(rgb, a, br, ba)
            gr, ga = sphere(cx, cy, 4.3, CYAN_GEM, CYAN_POS, 1.0)
            gems.append((cx, cy, k // 3))
            rgb, a = over(rgb, a, gr, ga)
        else:
            br, ba = sphere(cx, cy, 3.9, BRASS, BRASS_POS, 1.3)
            rgb, a = over(rgb, a, br, ba)
    return rgb, a, gems


RIM_RGB, RIM_A, RIM_GEMS = build_rim()
RIM_SW = np.where(RIM_A > 0.5, 0.7, 0.0)

# soft mask of the whole medallion (outside stays transparent)
DISC = np.clip((RIM_OUT + 0.4 - RR) / 1.1 + 0.5, 0, 1)


# ------------------------------------------------------------------------------------------------ maze
RING_R = [46.0, 58.5, 71.0, 83.5, 96.0]
RING_K = [2, -1, 1, -2, 1]
HW = 3.4
GAP_PX = [30.0, 29.0, 28.0, 27.0, 26.0]
# extra gaps (angle in radians from the main gap, width in px) per ring: they make it a maze, not a plain ring
EXTRA = [
    [(2.35, 15.0), (4.25, 12.0)],
    [(1.7, 14.0), (3.4, 13.0), (5.0, 10.0)],
    [(2.6, 16.0), (4.6, 12.0)],
    [(1.3, 12.0), (3.0, 15.0), (4.7, 12.0)],
    [(2.1, 14.0), (3.7, 12.0), (5.3, 12.0)],
]
BRIDGES = [(0.9, 3.9), (1.2, 4.6), (2.0, 5.3), (0.7, 3.6)]    # (angle offset, second angle offset) of the 2 spokes joining ring i and i+1
AXIS_ANG = math.atan2(AXIS[1], AXIS[0])


def arcs_of(i):
    """Arc list [(start, end)] of ring i in ring-local angles, with the main gap centred on 0."""
    R = RING_R[i]
    gaps = [(0.0, GAP_PX[i] / R)] + [(a, w / R) for a, w in EXTRA[i]]
    gaps.sort()
    out = []
    for g in range(len(gaps)):
        a_end = gaps[g][0] + gaps[g][1] / 2
        nxt = gaps[(g + 1) % len(gaps)]
        a_next = nxt[0] - nxt[1] / 2 + (TAU if g == len(gaps) - 1 else 0)
        pad = HW / R
        if a_next - a_end > 2 * pad:
            out.append((a_end + pad, a_next - pad))
    return out


ARCS = [arcs_of(i) for i in range(5)]


def ring_dist(i, rot):
    """Distance field to the (capsule) arcs of ring i turned by rot."""
    R = RING_R[i]
    la = (AA - AXIS_ANG - rot) % TAU
    best = np.full(AA.shape, 1e9)
    dr = RR - R
    for a0, a1 in ARCS[i]:
        # angular distance outside [a0, a1] (the arc may wrap past TAU)
        for shift in (0.0, -TAU):
            lo, hi = a0 + shift, a1 + shift
            if hi < -1.0 or lo > TAU + 1.0:
                continue
            da = np.where(la < lo, lo - la, np.where(la > hi, la - hi, 0.0))
            da = np.minimum(da, TAU - da) if False else da
            dd = np.hypot(dr, R * da)
            best = np.minimum(best, dd)
    return best


def bridge_dist(i, rot):
    """Distance to the radial spokes joining ring i and i+1 (they ride on ring i)."""
    R0, R1 = RING_R[i], RING_R[i + 1]
    la = (AA - AXIS_ANG - rot) % TAU
    best = np.full(AA.shape, 1e9)
    for off in BRIDGES[i][:2]:
        base = (off + (0.4 if i % 2 else 0.0)) % TAU
        da = np.abs(((la - base + math.pi) % TAU) - math.pi)
        lat = RR * da
        rad = np.clip(RR, R0, R1)
        dd = np.hypot(lat, RR - rad)
        best = np.minimum(best, dd)
    return best


RADX, RADY = DX / np.maximum(RR, 1e-6), DY / np.maximum(RR, 1e-6)
LR = RADX * L2[0] + RADY * L2[1]


def maze_layer(t, boost):
    rgb = np.zeros((S0, S0, 3))
    cov_all = np.zeros((S0, S0))
    for i in range(5):
        rot = TAU * RING_K[i] * t
        dist = ring_dist(i, rot)
        cov = np.clip(HW - dist + 0.6, 0, 1)
        core = np.clip(1 - dist / (HW * 0.62), 0, 1) ** 1.3
        srad = np.clip((RR - RING_R[i]) / HW, -1, 1)
        shade = 1 + 0.32 * srad * LR
        bright = 0.8 - 0.05 * i
        edge_c = np.array([16, 118, 214])
        core_c = np.array([205, 255, 255])
        col = edge_c + (core_c - edge_c) * core[..., None]
        col = col * (shade * bright)[..., None]
        rgb = rgb + col * cov[..., None]
        cov_all = np.maximum(cov_all, cov)
        # cap highlights: arc ends get a small bright bead
    for i in range(4):
        rot = TAU * RING_K[i] * t
        dist = bridge_dist(i, rot)
        cov = np.clip(1.9 - dist + 0.6, 0, 1) * (1 - cov_all * 0.0)
        core = np.clip(1 - dist / 1.6, 0, 1)
        col = (np.array([14, 100, 190]) + (np.array([150, 235, 255]) - np.array([14, 100, 190])) * core[..., None]) * 0.85
        rgb = rgb + col * cov[..., None] * (1 - np.minimum(1, rgb.max(axis=-1, keepdims=True) / 200.0))
        cov_all = np.maximum(cov_all, cov)
    rgb = rgb * (1 + 1.5 * boost)[..., None]
    return rgb, cov_all


def pulses(t):
    """Sound-rings: 3 big arcs per pulse spread from near the horn through every maze ring (2 pulses per loop, strongest
    towards the bell axis), plus short bright arcs that leave the bell mouth. Returns (arc light, boost for the maze)."""
    Q = np.array([CEN, CEN]) + 0.3 * (SRC - np.array([CEN, CEN]))
    qx, qy = XX - Q[0], YY - Q[1]
    qd = np.hypot(qx, qy)
    qcos = (qx * AXIS[0] + qy * AXIS[1]) / np.maximum(qd, 1e-6)
    arc = np.zeros((S0, S0))
    boost = np.zeros((S0, S0))
    wide = 0.4 + 0.6 * smooth(-0.7, 0.85, qcos)
    for j in range(3):
        p = (2 * t + j / 3.0) % 1.0
        Rj = 6 + p * 126
        al = math.sin(math.pi * p) ** 1.2
        g = np.exp(-(((qd - Rj) / (2.4 + 1.3 * p)) ** 2)) * smooth(110, 99, RR)
        arc += g * wide * al * 0.85
        gb = np.exp(-(((qd - Rj) / 9.0) ** 2)) * smooth(110, 99, RR)
        boost += gb * wide * al
    sx, sy = XX - SRC[0], YY - SRC[1]
    sd = np.hypot(sx, sy)
    scos = (sx * AXIS[0] + sy * AXIS[1]) / np.maximum(sd, 1e-6)
    for j in range(2):
        p = (2 * t + j / 2.0) % 1.0
        Rj = 5 + p * 34
        win = np.clip((scos - 0.15) / 0.5, 0, 1)
        al = math.sin(math.pi * p) ** 1.2
        arc += np.exp(-(((sd - Rj) / (1.8 + 0.8 * p)) ** 2)) * win * al * 1.1 * smooth(108, 97, RR)
    return arc, boost


# ------------------------------------------------------------------------------------------------ motes, mist, grid
_rng = np.random.default_rng(7)
MOTES = []
for _ in range(26):
    ang = _rng.uniform(0, TAU)
    rad = _rng.uniform(38, 106)
    MOTES.append(dict(ang=ang, rad=rad, k=int(_rng.integers(1, 3)), off=_rng.uniform(0, 1), size=_rng.uniform(1.1, 2.3),
                      drift=_rng.uniform(8, 18), swirl=_rng.uniform(0.25, 0.7) * (1 if _rng.random() < 0.5 else -1),
                      wob=_rng.uniform(0, TAU), tint=_rng.uniform(0, 1)))

MIST_WAVES = [(0.045, 0.031, 1, 0.3, 0.5), (-0.037, 0.052, -1, 1.9, 0.4), (0.061, -0.022, 2, 4.1, 0.3), (0.018, 0.07, -2, 5.2, 0.25)]
EDGE_W = smooth(62, 112, RR)
GRID = None


def build_grid():
    sp = 13.0
    gx = np.abs(((XX + 0.0) % sp) - sp / 2)
    gy = np.abs(((YY + 0.0) % sp) - sp / 2)
    lx = np.clip(1 - np.abs(gx - sp / 2) / 0.9, 0, 1)
    ly = np.clip(1 - np.abs(gy - sp / 2) / 0.9, 0, 1)
    return np.maximum(lx, ly)


GRID = build_grid()


def motes_layer(t):
    lay = np.zeros((S0, S0, 3))
    for m in MOTES:
        p = (t * m["k"] + m["off"]) % 1.0
        al = math.sin(math.pi * p) ** 2
        if al < 0.02:
            continue
        a = m["ang"] + m["swirl"] * p * 0.6 + 0.04 * math.sin(TAU * t * m["k"] + m["wob"])
        r = m["rad"] + m["drift"] * p
        cx, cy = CEN + r * math.cos(a), CEN + r * math.sin(a)
        if r > 104:
            continue
        sz = m["size"]
        x0, x1 = int(max(0, cx - 7)), int(min(S0, cx + 8))
        y0, y1 = int(max(0, cy - 7)), int(min(S0, cy + 8))
        px, py = XX[y0:y1, x0:x1] - cx, YY[y0:y1, x0:x1] - cy
        d2 = px * px + py * py
        core = np.exp(-d2 / (2 * (sz * 0.55) ** 2))
        halo = np.exp(-d2 / (2 * (sz * 1.8) ** 2)) * 0.35
        w = (core + halo) * al
        col = np.array([90, 225, 255]) * (1 - 0.4 * m["tint"]) + np.array([240, 255, 255]) * 0.4 * m["tint"]
        lay[y0:y1, x0:x1] += w[..., None] * col
    return lay


def core_glow(t):
    breathe = 0.5 + 0.5 * math.sin(TAU * t)
    g = np.exp(-(RR / (48 + 6 * breathe)) ** 2)
    return g[..., None] * np.array([36, 120, 190]) * (0.4 + 0.3 * breathe)


def mouth_layers(t):
    """The bell mouth: brass rim ellipse with a glowing cyan opening. Returns rgb, alpha, glow light."""
    pulse = 0.5 + 0.5 * math.sin(TAU * (2 * t) - 0.9)
    rel = np.stack([XX - MOUTH[0], YY - MOUTH[1]], axis=-1)
    u = rel[..., 0] * AXIS[0] + rel[..., 1] * AXIS[1]
    nrm = np.array([-AXIS[1], AXIS[0]])
    v = rel[..., 0] * nrm[0] + rel[..., 1] * nrm[1]
    eb, ea = 25.4 * SC, 6.4 * SC
    q = np.sqrt((u / ea) ** 2 + (v / eb) ** 2)
    alpha_out = np.clip((1 - q) * ea * 0.9 + 0.5, 0, 1)
    # rim shading from the ellipse normal
    nu, nv = u / ea ** 2, v / eb ** 2
    nl = np.maximum(np.hypot(nu, nv), 1e-9)
    nsx = (nu * AXIS[0] + nv * nrm[0]) / nl
    nsy = (nu * AXIS[1] + nv * nrm[1]) / nl
    dif = np.clip(0.5 + 0.5 * (nsx * L2[0] + nsy * L2[1]), 0, 1)
    rimd = np.clip(0.2 + 0.85 * dif, 0, 1)
    hn = (nsx * H3[0] + nsy * H3[1]) * 0.8 + 0.6
    rimspec = np.clip(hn, 0, 1) ** 22 * 0.8
    rim_rgb = ramp(rimd, BRASS_POS, BRASS) + np.array([255, 244, 205]) * rimspec[..., None]
    rim_rgb = rim_rgb * (0.55 + 0.45 * np.clip((1 - q) * 6, 0, 1))[..., None]
    # opening
    qin = q / 0.8
    inside = np.clip((1 - qin) * 3.0 + 0.5, 0, 1)
    tt = np.clip(qin, 0, 1)
    open_rgb = (np.array([255, 255, 255]) * (1 - tt ** 0.6)[..., None] + np.array([30, 150, 215]) * (tt ** 0.6)[..., None])
    open_rgb = open_rgb * (0.82 + 0.3 * pulse)
    # darker towards the near rim (we look into the bore)
    open_rgb = open_rgb * (1 - 0.35 * np.clip(-(v * 0 + u) / ea, 0, 1))[..., None]
    rgb = rim_rgb * (1 - inside)[..., None] + open_rgb * inside[..., None]
    glow_i = np.exp(-(((XX - SRC[0]) ** 2 + (YY - SRC[1]) ** 2) / (2 * 14.0 ** 2)))
    glow = glow_i[..., None] * np.array([90, 225, 255]) * (0.6 + 0.5 * pulse)
    return np.clip(rgb, 0, 255), alpha_out, glow


def sweep(q, qc, sig):
    return np.exp(-(((q - qc) / sig) ** 2))


SWQ = (DX * 0.8 + DY * (-0.6)) / 100.0


def draw(t, S):
    assert S == S0
    arc, boost = pulses(t)
    img = BG.copy()

    # mist + grid at the edges (additive light)
    mist = np.zeros((S0, S0))
    for fx, fy, n, ph, amp in MIST_WAVES:
        mist += amp * np.sin(XX * fx * TAU / 6.0 + YY * fy * TAU / 6.0 + TAU * n * t + ph)
    mist = np.clip(0.5 + 0.5 * mist / 1.1, 0, 1) ** 1.6
    img += (mist * EDGE_W * 0.55)[..., None] * np.array([46, 92, 190]) * 0.55
    shimmer = 0.5 + 0.5 * np.sin(TAU * (t - RR / 150.0))
    img += (GRID * EDGE_W * (0.05 + 0.1 * shimmer))[..., None] * np.array([110, 200, 255])

    img += core_glow(t)

    # maze, with dark depth halo and bloom
    maze, cov = maze_layer(t, boost * 0.9)
    halo = blur(cov * 255, 3.0) / 255.0
    img *= (1 - 0.45 * halo)[..., None]
    img += blur(maze, 6.0) * 0.26 + blur(maze, 2.2) * 0.12
    img = img * (1 - cov[..., None]) + np.minimum(maze, 255)

    # sound rings
    arc_rgb = arc[..., None] * np.array([110, 235, 255]) + (arc ** 2)[..., None] * np.array([160, 160, 150])
    img += arc_rgb * 0.8 + blur(arc_rgb, 4.0) * 0.7

    # motes
    mo = motes_layer(t)
    img += mo + blur(mo, 3.0) * 0.8

    # trumpet: shadow, brass, light sweep, mouth
    img *= (1 - SHADOW)[..., None]
    qc = -1.5 + 3.0 * t
    sw = sweep(SWQ, qc, 0.12)
    # soft sheen band riding on the brass, stronger where the surface faces the light
    tr = TR_RGB + (sw * TR_SW * 0.7)[..., None] * np.array([255, 235, 170]) * 0.9
    tr = np.clip(tr, 0, 255)
    # flicker-free gem pulses on the valve buttons
    img = img * (1 - TR_A[..., None]) + tr * TR_A[..., None]
    mrgb, ma, mglow = mouth_layers(t)
    img += mglow + blur(mglow, 6.0) * 0.6
    img = img * (1 - ma[..., None]) + mrgb * ma[..., None]
    # bloom of the bright parts of the horn (specular + gems)
    hot = np.clip(np.concatenate([tr, mrgb]) , 0, 255)
    hotimg = np.where((TR_A[..., None] > 0.5) & (tr > 215), tr, 0)
    img += blur(hotimg, 3.0) * 0.45
    for k, g in enumerate(VALVE_GEMS):
        tw = 0.8 + 0.2 * math.sin(TAU * (t + k / 3.0))
        x0, x1 = int(g[0] - 14), int(g[0] + 15)
        y0, y1 = int(g[1] - 14), int(g[1] + 15)
        px, py = XX[y0:y1, x0:x1] - g[0], YY[y0:y1, x0:x1] - g[1]
        img[y0:y1, x0:x1] += (np.exp(-(px * px + py * py) / (2 * 4.5 ** 2)) * 70 * tw)[..., None] * np.array([0.35, 0.9, 1.0])

    # vignette (before the rim so the rim stays bright)
    img *= (1 - 0.5 * smooth(70, 108, RR))[..., None]

    # rim, with gem twinkle and its own light sweep
    rim = RIM_RGB.copy()
    rsw = sweep(SWQ, -1.5 + 3.0 * ((t + 0.5) % 1.0), 0.1)
    rim = rim + (rsw * 0.9)[..., None] * np.array([255, 235, 170]) * 0.4 * (RIM_A[..., None] > 0.5)
    for (cx, cy, k) in RIM_GEMS:
        tw = 0.85 + 0.15 * math.sin(TAU * (t + k / 4.0))
        x0, x1 = int(cx - 8), int(cx + 9)
        y0, y1 = int(cy - 8), int(cy + 9)
        px, py = XX[y0:y1, x0:x1] - cx, YY[y0:y1, x0:x1] - cy
        d2 = px * px + py * py
        rim[y0:y1, x0:x1] += (np.exp(-d2 / (2 * 2.2 ** 2)) * 70 * tw)[..., None] * np.array([0.35, 0.9, 1.0])
    img = img * (1 - RIM_A[..., None]) + np.clip(rim, 0, 255) * RIM_A[..., None]
    # small bloom from the gems onto the surrounding rim / maze
    gl = np.zeros((S0, S0, 3))
    for (cx, cy, k) in RIM_GEMS:
        tw = 0.85 + 0.15 * math.sin(TAU * (t + k / 4.0))
        d2 = (XX - cx) ** 2 + (YY - cy) ** 2
        gl += (np.exp(-d2 / (2 * 7.0 ** 2)) * 50 * tw)[..., None] * np.array([0.3, 0.85, 1.0])
    img += gl

    # thin cyan inner light line just inside the rim
    inner = np.exp(-(((RR - (RIM_IN - 1.6)) / 0.9) ** 2))
    img += inner[..., None] * np.array([40, 150, 190]) * 0.8

    out = np.zeros((S0, S0, 4), np.uint8)
    out[..., :3] = np.clip(img, 0, 255).astype(np.uint8)
    out[..., 3] = np.clip(DISC * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


if __name__ == "__main__":
    build("israfil", draw, hero_t=0.0)

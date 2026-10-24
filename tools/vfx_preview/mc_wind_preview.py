#!/usr/bin/env python3
"""
WIND, "Minecraft style" remake, PHASE A = PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

For every wind shape three versions are built as QUADS (the same data the real layers hand to VfxVertexBuffer) and rasterised with the repo's
own preview rasteriser (nearest-neighbour textures, vertex colours, ALPHA / ADD in the game's order), next to the CURRENT look which is
produced by the real layer class through the repo's Java preview:

    A  vanilla-faithful   flat 16x16 / 32x32 sprites, vanilla-like grey-white particle palette, billboards, stepped fades
    B  voxel              real cubes (camera-facing faces only: a cube costs 1..3 quads), positions snapped to 1/8 block, vanilla face shading
    C  pixel-anime        32x32 sprites with a dark outline, 2-frame flip-book that swaps every 3 ticks, motion "on twos"

    python tools/gen_wind_mc_textures.py            # once: the pixel textures (preview folder only)
    python tools/vfx_preview/mc_wind_preview.py     # sheets + compare + budget table (needs JDK 21, numpy, Pillow, like preview.py)

Output: docs/previews/vfx_remake/mc/wind_mc_{A,B,C}.png, wind_mc_compare.png, wind_mc_budget.json.
Honest limits: headless software rasteriser, painter's-order sort instead of a depth buffer, no bloom, no world light, never seen in a game.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402  (the repo's own preview tool)

ROOT = pv.ROOT
TEXDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "mc", "wind_tex")
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "mc")
CW, CH = 560, 420

UP = np.array([0.0, 1.0, 0.0])


def V(*a):
    return np.array(a, float)


def norm(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def ease_out(t):
    t = min(1.0, max(0.0, t))
    return 1 - (1 - t) ** 3


def snap(v, g=0.125):
    return np.round(np.asarray(v, float) / g) * g


def q4(a):
    return round(min(1.0, max(0.0, a)) * 4) / 4


def argb(a=1.0, shade=1.0):
    s = int(round(min(1.0, shade) * 255))
    return (int(round(min(1.0, max(0.0, a)) * 255)) << 24) | (s << 16) | (s << 8) | s


def hsh(seed, i, salt):
    x = math.sin(i * 12.9898 + salt * 78.233 + seed * 0.001) * 43758.5453
    return x - math.floor(x)


# ------------------------------------------------------------------------------------------------ quad collector
class Out:
    def __init__(self, cam, target, age, dur, seed=12345):
        self.cam, self.target = np.array(cam, float), np.array(target, float)
        self.f, self.r, self.u = pv.basis(self.cam, self.target)
        self.age, self.dur, self.seed = age, dur, seed
        self.q = []
        self.cubes = 0

    # -- primitives ------------------------------------------------------------------------------------------
    def plane(self, c, ax, ay, hw, hh, tex, color=0xFFFFFFFF, flip=False, u=(0.0, 1.0), blend="ALPHA"):
        c, ax, ay = np.asarray(c, float), np.asarray(ax, float), np.asarray(ay, float)
        u0, u1 = (u[1], u[0]) if flip else u
        pts = [c - ax * hw + ay * hh, c + ax * hw + ay * hh, c + ax * hw - ay * hh, c - ax * hw - ay * hh]
        uv = [(u0, 0), (u1, 0), (u1, 1), (u0, 1)]
        self._emit(pts, uv, tex, color, blend)

    def _emit(self, pts, uv, tex, color, blend):
        mid = sum(pts) / 4.0
        self.q.append({"t": "mc/" + tex, "b": blend, "o": 1 if blend == "ADD" else 0, "d": float(np.linalg.norm(mid - self.cam)),
                       "v": [[*(p - self.cam), uv[i][0], uv[i][1], color] for i, p in enumerate(pts)]})

    def bb(self, c, w, h, tex, roll=0.0, color=0xFFFFFFFF, flip=False, blend="ALPHA"):
        ca, sa = math.cos(roll), math.sin(roll)
        self.plane(c, self.r * ca + self.u * sa, -self.r * sa + self.u * ca, w / 2, h / 2, tex, color, flip, blend=blend)

    def strip(self, a, b, width, tex, color=0xFFFFFFFF, tile=1.0, uoff=0.0, flip=False, blend="ALPHA"):
        """camera-facing strip from a to b; the texture's +x runs from a to b"""
        a, b = np.asarray(a, float), np.asarray(b, float)
        d = b - a
        ln = np.linalg.norm(d)
        if ln < 1e-6:
            return
        ax = d / ln
        mid = (a + b) / 2
        ay = norm(np.cross(ax, mid - self.cam))
        self.plane(mid, ax, ay, ln / 2, width / 2, tex, color, flip, u=(uoff, uoff + tile), blend=blend)

    def cube(self, c, s, yaw, tex, alpha=1.0, grid=0.125):
        """axis box (yawed around Y) with only its camera-facing faces; every face is one quad, shaded like vanilla (top 1.0, sides 0.6/0.8, bottom 0.5)"""
        c = snap(c, grid)
        sx, sy, sz = (s, s, s) if np.isscalar(s) else s
        cy, sy_ = math.cos(yaw), math.sin(yaw)
        ex, ey, ez = V(cy, 0, -sy_), UP, V(sy_, 0, cy)
        hx, hy, hz = sx / 2, sy / 2, sz / 2
        self.cubes += 1
        for n, a, b, ha, hb, h_n in ((ex, ez, ey, hz, hy, hx), (-ex, -ez, ey, hz, hy, hx), (ey, ex, ez, hx, hz, hy), (-ey, ex, -ez, hx, hz, hy),
                                     (ez, -ex, ey, hx, hy, hz), (-ez, ex, ey, hx, hy, hz)):
            fc = c + n * h_n
            if np.dot(n, fc - self.cam) >= 0:
                continue
            shade = 1.0 if n[1] > 0.5 else 0.5 if n[1] < -0.5 else 0.6 * n[0] ** 2 + 0.8 * n[2] ** 2
            pts = [fc - a * ha + b * hb, fc + a * ha + b * hb, fc + a * ha - b * hb, fc - a * ha - b * hb]
            self._emit(pts, [(0, 0), (1, 0), (1, 1), (0, 1)], tex, argb(alpha, shade), "ALPHA")

    def finish(self):
        self.q.sort(key=lambda q: (q["o"], -q["d"]))                  # painter's order (far first) instead of a depth buffer
        return self.q


def fade(age, dur, fin=3, fout=5):
    a = min(1.0, (age + 1) / fin) * min(1.0, (dur - age) / fout)
    return q4(a)


def fr(age):                                                          # C: flip-book frame, swaps every 3 ticks
    return int(age // 3) % 2


# ------------------------------------------------------------------------------------------------ shape helpers
def frame_dir(F, T):
    d = norm(np.array(T, float) - np.array(F, float))
    s = norm(np.cross(d, UP))
    return d, s


def crescent_axes(d, s, roll, lean=0.35):
    """plane of a flying crescent: faces the flight direction (leaning back by `lean` rad), texture +x (tips) pointing up-and-sideways by `roll`"""
    n = norm(d * math.cos(lean) + UP * math.sin(lean))
    want = UP * math.cos(roll) + s * math.sin(roll)
    ax = norm(want - n * np.dot(want, n))
    ay = norm(np.cross(n, ax))
    return n, ax, ay


# ================================================================================================ SLASH (Kamaitachi) ==
def slash_pos(o, F, T, age, dur, start=0.6):
    d = norm(T - F)
    L = np.linalg.norm(T - F)
    return F + d * (start + (L - start) * ease_out(age / (dur * 0.9)))


def slash_A(o, F, T, p):
    d, s = frame_dir(F, T)
    pos = slash_pos(o, F, T, o.age, o.dur)
    a = fade(o.age, o.dur, 2, 5)
    R = 1.7 * p
    n, ax, ay = crescent_axes(d, s, 0.5)
    o.plane(pos, ax, ay, R, R, "A_crescent", argb(a))
    tip1, tip2 = pos + ax * R * 0.72 + ay * R * 0.82, pos + ax * R * 0.72 - ay * R * 0.82
    for i in range(5):                                                  # puff trail
        k = 0.9 + 0.85 * i
        o.bb(pos - d * k + s * (hsh(o.seed, i, 1) - 0.5) * 0.9 + UP * (hsh(o.seed, i, 2) - 0.5) * 0.7, 0.8 - 0.1 * i, 0.8 - 0.1 * i, "A_puff", 0.3 * i, argb(q4(a * (1 - i * 0.18))))
    for i in range(4):                                                  # speed streaks
        off = (i - 1.5) * 0.55
        b = pos - d * (0.5 + 0.35 * i) + UP * off * 0.8 + s * off * 0.5
        o.strip(b - d * 2.4, b, 0.2, "A_streak", argb(q4(a * 0.8)))
    for t in (tip1, tip2):
        o.bb(t, 0.5, 0.5, "A_spark", 0.0, argb(a))


def slash_B(o, F, T, p):
    d, s = frame_dir(F, T)
    pos = slash_pos(o, F, T, o.age, o.dur)
    a = fade(o.age, o.dur, 2, 5)
    sc = 0.0 if a <= 0 else 1.0
    R = 1.7 * p
    n, ax, ay = crescent_axes(d, s, 0.5)
    yaw = math.atan2(d[0], d[2])
    for i in range(11):                                                 # outer blade: 11 cubes on the arc
        ph = math.radians(-105 + i * 21)
        c = pos + ax * R * (0.35 - math.cos(ph)) + ay * R * math.sin(ph)
        size = max(0.375, round(0.75 * (0.5 + 0.5 * math.cos(ph * 0.8)) / 0.125) * 0.125)
        o.cube(c, size * sc * (0.5 + 0.5 * a), yaw, "B_cube_white", 1.0)
    for i in range(5):                                                  # inner row (thickness)
        ph = math.radians(-48 + i * 24)
        c = pos + ax * R * (0.35 - 0.8 * math.cos(ph)) + ay * R * 0.8 * math.sin(ph)
        o.cube(c, 0.375 * (0.5 + 0.5 * a), yaw, "B_cube_mint", 1.0)
    for i in range(6):                                                  # trail
        c = pos - d * (1.0 + 0.7 * i) + s * (hsh(o.seed, i, 3) - 0.5) * 1.2 + UP * (hsh(o.seed, i, 4) - 0.5) * 1.0
        o.cube(c, 0.375 if i < 3 else 0.25, yaw, "B_cube_lime", 1.0)


def slash_C(o, F, T, p):
    d, s = frame_dir(F, T)
    pos = slash_pos(o, F, T, int(o.age // 2 * 2), o.dur)               # on twos
    a = fade(int(o.age // 2 * 2), o.dur, 2, 5)
    f = fr(o.age)
    R = 1.7 * p
    n, ax, ay = crescent_axes(d, s, 0.5)
    for k, al in ((2, 0.35), (1, 0.6), (0, 1.0)):                       # held smear: two ghosts behind the blade
        o.plane(pos - d * 0.9 * k, ax, ay, R, R, f"C_crescent_{f}", argb(q4(a * al)))
    for i in range(2):
        b = pos - d * 0.8 + s * (i - 0.5) * 1.5 + UP * (0.25 - 0.5 * i)
        o.strip(b - d * 2.3, b, 0.5, f"C_ribbon_{f}", argb(q4(a * 0.85)))
    for sg in (1, -1):
        o.bb(pos + ax * R * 0.72 + ay * R * 0.82 * sg, 0.6, 0.6, f"C_spark_{f}")
    if o.age < 3:
        o.bb(pos, 1.6, 1.6, "C_flash_%d" % f)


# ================================================================================================ EMPEROR ==
def emp_R(o, p):
    return 3.5 * p * (1 + 0.45 * ease_out(o.age / (o.dur * 0.8)))


def emp_A(o, F, T, p):
    d, s = frame_dir(F, T)
    pos = slash_pos(o, F, T, o.age, o.dur, 2.0)
    a = fade(o.age, o.dur, 2, 5)
    R = emp_R(o, p)
    n, ax, ay = crescent_axes(d, s, 0.35, 0.25)
    o.plane(pos, ax, ay, R, R, "A_crescent", argb(a))
    for i, ang in enumerate((-0.5, 0.0, 0.5)):                          # three white ribbons crossing the blade
        dirv = ax * math.cos(ang) + ay * math.sin(ang)
        o.strip(pos - dirv * R * 0.9, pos + dirv * R * 0.9, 0.9, "A_ribbon", argb(q4(a * 0.9)), flip=(i == 1))
    for i in range(6):
        ph = math.radians(-90 + i * 36)
        o.bb(pos + ax * R * (0.35 - math.cos(ph)) + ay * R * math.sin(ph), 0.9, 0.9, "A_spark", 0, argb(a))
    for i in range(4):
        o.bb(pos - d * (1.5 + 1.3 * i) + UP * (hsh(o.seed, i, 5) - 0.5) * 2.2 + s * (hsh(o.seed, i, 6) - 0.5) * 3.0, 1.4 - 0.2 * i, 1.4 - 0.2 * i, "A_puff", 0.5 * i, argb(q4(a * 0.7)))


def emp_B(o, F, T, p):
    d, s = frame_dir(F, T)
    pos = slash_pos(o, F, T, o.age, o.dur, 2.0)
    a = fade(o.age, o.dur, 2, 5)
    R = emp_R(o, p)
    n, ax, ay = crescent_axes(d, s, 0.35, 0.25)
    yaw = math.atan2(d[0], d[2])
    k = 0.5 + 0.5 * a
    for i in range(14):
        ph = math.radians(-110 + i * (220 / 13))
        c = pos + ax * R * (0.35 - math.cos(ph)) + ay * R * math.sin(ph)
        size = max(0.625, round(1.25 * (0.5 + 0.5 * math.cos(ph * 0.8)) / 0.125) * 0.125)
        o.cube(c, size * k, yaw, "B_cube_white")
    for i in range(7):
        ph = math.radians(-60 + i * 20)
        o.cube(pos + ax * R * (0.35 - 0.78 * math.cos(ph)) + ay * R * 0.78 * math.sin(ph), 0.625 * k, yaw, "B_cube_mint")
    for i in range(4):                                                  # the wind sword's spine through the blade
        o.cube(pos - d * (0.4 + 0.9 * i) + ax * R * -0.2, (0.375, 0.375, 0.9) if i % 2 == 0 else 0.375, yaw, "B_cube_lime")


def emp_C(o, F, T, p):
    d, s = frame_dir(F, T)
    ta = int(o.age // 2 * 2)
    pos = slash_pos(o, F, T, ta, o.dur, 2.0)
    a = fade(ta, o.dur, 2, 5)
    f = fr(o.age)
    R = emp_R(o, p)
    n, ax, ay = crescent_axes(d, s, 0.35, 0.25)
    for k, al in ((2, 0.3), (1, 0.55), (0, 1.0)):
        o.plane(pos - d * 1.4 * k, ax, ay, R, R, f"C_crescent_{f}", argb(q4(a * al)))
    for i, ang in enumerate((-0.55, 0.0, 0.55)):
        dirv = ax * math.cos(ang) + ay * math.sin(ang)
        o.strip(pos - dirv * R * 0.95, pos + dirv * R * 0.95, 1.1, f"C_ribbon_{f}", argb(q4(a * 0.9)), flip=(i == 1))
    for i in range(6):
        ph = math.radians(-100 + i * 40)
        o.bb(pos + ax * R * (0.35 - math.cos(ph)) + ay * R * math.sin(ph), 0.9, 0.9, f"C_spark_{(f + i) % 2}")
    for i in range(6):                                                  # thin rays from the blade centre
        ang = math.radians(15 + i * 60)
        dirv = ax * math.cos(ang) + ay * math.sin(ang)
        o.strip(pos + dirv * R * 0.3, pos + dirv * R * 0.75, 0.22, f"C_streak_{f}", argb(q4(a * 0.7)))


# ================================================================================================ TORNADO ==
H_T, RB, RT = 11.0, 0.9, 3.8


def tor_layer(k, n):
    f = k / (n - 1)
    return f, 0.4 + (H_T - 0.8) * f, RB + (RT - RB) * f ** 0.9


def tor_A(o, F, T, p):
    a = fade(o.age, o.dur, 6, 12)
    w = o.age * 0.2
    for k in range(7):                                                  # 7 rings of 6 cloud puffs: the vanilla "particle tornado"
        f, y, r = tor_layer(k, 7)
        for j in range(6):
            th = w * (1.4 - 0.8 * f) + j * 2 * math.pi / 6 + k * 0.9
            c = F + V(r * math.cos(th), y, r * math.sin(th))
            sz = 1.1 + 0.9 * f
            o.bb(c, sz, sz, "A_puff", 0.4 * j + k, argb(q4(a * 0.9)))
    for j in range(4):                                                  # foot cloud
        th = w * 1.2 + j * math.pi / 2
        o.bb(F + V(2.0 * math.cos(th), 0.45, 2.0 * math.sin(th)), 2.6, 1.3, "A_cloud", 0, argb(q4(a * 0.8)))
    for j in range(5):                                                  # leaves
        th = w * 0.9 + j * 2 * math.pi / 5
        f = 0.15 + 0.15 * j
        r = RB + (RT - RB) * f + 1.2
        o.bb(F + V(r * math.cos(th), 0.4 + H_T * f * 0.9, r * math.sin(th)), 0.7, 0.7, "A_leaf", o.age * 0.3 + j, argb(a))


def tor_B(o, F, T, p):
    a = fade(o.age, o.dur, 6, 12)
    k_ = 0.4 + 0.6 * a
    w = o.age * 0.2
    counts = (4, 4, 5, 5, 6, 5)
    for k in range(6):
        f, y, r = tor_layer(k, 6)
        sz = round((0.6 + 0.9 * f) / 0.125) * 0.125
        for j in range(counts[k]):
            th = w * (1.4 - 0.8 * f) + j * 2 * math.pi / counts[k] + k * 1.1
            c = F + V(r * math.cos(th), y, r * math.sin(th))
            tex = "B_cube_white" if (j + k) % 3 else "B_cube_mint"
            o.cube(c, (sz * 1.5 * k_, sz * 0.7 * k_, sz * k_), -th, tex)
    for j in range(2):
        th = w * 0.9 + j * 3.1
        f = 0.3 + 0.35 * j
        r = RB + (RT - RB) * f + 1.2
        o.cube(F + V(r * math.cos(th), 0.4 + H_T * f * 0.9, r * math.sin(th)), 0.375, th, "B_cube_lime")


def tor_C(o, F, T, p):
    ta = int(o.age // 2 * 2)
    a = fade(ta, o.dur, 6, 12)
    f_ = fr(o.age)
    w = ta * 0.2
    for k in range(6):
        f, y, r = tor_layer(k, 6)
        for j in range(3):                                              # cel bands wrapped round the funnel (vertical slabs, tangent to it)
            th = w * (1.4 - 0.8 * f) + j * 2 * math.pi / 3 + k * 1.3
            c = F + V(r * math.cos(th), y, r * math.sin(th))
            tang = V(-math.sin(th), 0, math.cos(th))
            o.plane(c, tang, UP, r * 1.25, 0.75 + 0.4 * f, f"C_ribbon_{(f_ + k) % 2}", argb(a), flip=(k % 2 == 1))
    for j in range(6):                                                  # puff foot
        th = w * 1.2 + j * math.pi / 3
        o.bb(F + V(2.0 * math.cos(th), 0.4, 2.0 * math.sin(th)), 1.2, 1.2, f"C_puff_{f_}", 0, argb(a))
    for j in range(5):
        th = w * 0.9 + j * 2 * math.pi / 5
        f = 0.15 + 0.15 * j
        r = RB + (RT - RB) * f + 1.2
        o.bb(F + V(r * math.cos(th), 0.4 + H_T * f * 0.9, r * math.sin(th)), 0.8, 0.8, f"C_leaf_{(f_ + j) % 2}", 0, argb(a))
    o.bb(F + V(0, H_T + 0.8, 0), 1.6, 1.6, f"C_spark_{f_}")


# ================================================================================================ GALE (lane, also FX_WIND_GUST) ==
def gale_head(o, F, T):
    d = norm(T - F)
    L = np.linalg.norm(T - F)
    return d, L, F + d * (1.0 + (L - 2.0) * ease_out(o.age / (o.dur * 0.9)))


def gale_A(o, F, T, p):
    d, s = frame_dir(F, T)
    d, L, head = gale_head(o, F, T)
    a = fade(o.age, o.dur, 2, 6)
    up2 = norm(np.cross(s, d))
    for i in range(5):                                                  # streaks racing down the lane
        t = (hsh(o.seed, i, 7) + o.age * (0.045 + 0.01 * i)) % 1.0
        pos = F + d * (t * L)
        ang = i * 2.4
        pos = pos + s * math.cos(ang) * 0.6 + up2 * math.sin(ang) * 0.6
        o.strip(pos - d * 2.8, pos, 0.4, "A_streak", argb(q4(a * (1 - abs(t - 0.5)))))
    for i in range(3):                                                  # rolling rings
        t = (i / 3 + o.age * 0.03) % 1.0
        c = F + d * (t * L)
        o.plane(c, s, up2, 1.3, 1.3, "A_ring", argb(q4(a * (1 - t))))
    for i in range(4):
        o.bb(head - d * (0.6 + 0.9 * i) + s * (hsh(o.seed, i, 8) - 0.5) * 0.8, 1.2 - 0.15 * i, 1.2 - 0.15 * i, "A_puff", i, argb(q4(a * 0.8)))
    for i in range(4):
        t = (hsh(o.seed, i, 9) + o.age * 0.035) % 1.0
        c = F + d * (t * L) + s * (hsh(o.seed, i, 10) - 0.5) * 1.8 + UP * (hsh(o.seed, i, 11) - 0.5) * 1.2
        o.bb(c, 0.6, 0.6, "A_leaf", o.age * 0.25 + i, argb(a))


def gale_B(o, F, T, p):
    d, s = frame_dir(F, T)
    d, L, head = gale_head(o, F, T)
    a = fade(o.age, o.dur, 2, 6)
    k = 0.4 + 0.6 * a
    yaw = math.atan2(d[0], d[2])
    flap = (int(o.age // 3) % 2) * 0.25
    o.cube(head, (0.625, 0.625, 1.375), yaw, "B_cube_white")            # voxel hawk
    o.cube(head + d * 0.75 + UP * 0.12, 0.375, yaw, "B_cube_mint")
    for sg in (1, -1):
        for i in range(3):
            o.cube(head - d * (0.05 + 0.2 * i) + s * sg * (0.75 + 0.8 * i) + UP * (0.1 + 0.3 * i + flap * (i + 1) * 0.5), 0.625 - 0.0625 * i, yaw, "B_cube_white" if i < 2 else "B_cube_mint")
    for i in range(2):
        o.cube(head - d * (0.9 + 0.4 * i), 0.375 - 0.0625 * i, yaw, "B_cube_mint")
    for i in range(2):                                                  # two cube trains
        for j in range(3):
            t = max(0.0, (o.age * 0.5 + j * 2.4 + i * 1.1) % 8.0)
            c = head - d * (1.2 + t) + s * (0.9 if i else -0.9) + UP * (0.25 + 0.5 * i)
            o.cube(c, 0.375 * k, yaw, "B_cube_lime")
    for i in range(6):                                                  # ring of small cubes round the lane behind the hawk
        th = i * math.pi / 3 + o.age * 0.15
        c = head - d * 2.6 + s * math.cos(th) * 0.8 + norm(np.cross(s, d)) * math.sin(th) * 0.8
        o.cube(c, 0.25, yaw, "B_cube_mint")


def gale_C(o, F, T, p):
    d, s = frame_dir(F, T)
    ta = int(o.age // 2 * 2)
    d, L, head = gale_head(o, F, T)
    a = fade(ta, o.dur, 2, 6)
    f = fr(o.age)
    up2 = norm(np.cross(s, d))
    o.plane(head, d, UP, 2.3, 2.3, f"C_hawk_{f}", argb(a))                # the white hawk (Whirlwind White Hawk, manga)
    for i in range(4):
        off = (i - 1.5) * 0.5
        b = head - d * (2.4 + 0.5 * i) + s * off * 1.6 + up2 * (0.4 if i % 2 else -0.4)
        o.strip(b - d * 4.2, b, 0.8, f"C_ribbon_{(f + i) % 2}", argb(q4(a * 0.7)), flip=(i % 2 == 1))
    for i in range(3):
        t = (hsh(o.seed, i, 12) + ta * 0.05) % 1.0
        c = F + d * (t * L) + s * (i - 1) * 0.7
        o.strip(c - d * 2.4, c, 0.3, f"C_streak_{f}", argb(q4(a * 0.8)))
    for i in range(2):
        o.bb(head + d * 1.3 + s * (i - 0.5) * 0.9 + UP * 0.2 * i, 0.5, 0.5, f"C_spark_{(f + i) % 2}")
    for i in range(3):
        t = (hsh(o.seed, i, 13) + ta * 0.04) % 1.0
        o.bb(F + d * (t * L) + s * (hsh(o.seed, i, 14) - 0.5) * 1.8 + UP * 0.3, 0.45, 0.45, f"C_leaf_{(f + i) % 2}", 0, argb(a))


# ================================================================================================ ZEPHYR ==
def wing_axes(sg, ang):
    """a wing sits behind the shoulder in the screen plane: texture +x (tips) up and outward by `ang`, mirrored for the left wing"""
    return V(sg * math.cos(ang), math.sin(ang), 0.0), V(-sg * math.sin(ang), math.cos(ang), 0.0)


def zeph_A(o, F, T, p):
    a = fade(o.age, 600, 8, 20)
    flap = math.sin(o.age * 0.12) * 0.12
    for sg in (1, -1):                                                  # two crescent wings behind the shoulders (caster faces +z, camera sees his front)
        ax, ay = wing_axes(sg, math.radians(38) + flap)
        o.plane(F + V(sg * 1.8, 1.8, 0.5), ax, ay, 1.35, 1.35, "A_crescent", argb(a))
    o.plane(F + V(0, 2.55, 0), V(1, 0, 0), V(0, 0, 1), 0.7, 0.7, "A_ring", argb(a))   # halo
    for i in range(8):
        th = i * math.pi / 4 + o.age * 0.05
        o.bb(F + V(0.7 * math.cos(th), 2.55, 0.7 * math.sin(th)), 0.3, 0.3, "A_spark", 0, argb(a))
    o.plane(F + V(0, 0.05, 0), V(1, 0, 0), V(0, 0, 1), 1.5, 1.5, "A_ring", argb(q4(a * 0.8)))
    for j in range(6):
        th = o.age * 0.12 + j * math.pi / 3
        o.bb(F + V(1.1 * math.cos(th), 0.4 + 0.3 * j, 1.1 * math.sin(th)), 0.55, 0.55, "A_puff", j, argb(q4(a * 0.8)))
    for j in range(3):
        th = -o.age * 0.1 + j * 2.1
        o.bb(F + V(1.4 * math.cos(th), 1.0 + 0.6 * j, 1.4 * math.sin(th)), 0.35, 0.35, "A_leaf", o.age * 0.2 + j, argb(a))


def zeph_B(o, F, T, p):
    a = fade(o.age, 600, 8, 20)
    flap = (int(o.age // 6) % 2) * 0.125
    for sg in (1, -1):                                                  # staircase wings
        for i in range(6):
            c = F + V(sg * (0.6 + 0.4 * i), 1.4 + 0.35 * i + flap * i, 0.45 + 0.05 * i)
            o.cube(c, max(0.375, 0.625 - 0.0625 * i), 0, "B_cube_white" if i < 4 else "B_cube_mint")
        for i in range(2):                                              # hanging feathers
            c = F + V(sg * (1.2 + 0.6 * i), 1.05 - 0.15 * i, 0.55)
            o.cube(c, (0.375, 0.625, 0.375), 0, "B_cube_mint")
    for i in range(8):                                                  # halo
        th = i * math.pi / 4 + int(o.age // 3) * 0.2
        o.cube(F + V(0.75 * math.cos(th), 2.5, 0.75 * math.sin(th)), 0.1875, 0, "B_cube_lime" if i % 2 else "B_cube_white")
    for j in range(3):
        th = -o.age * 0.1 + j * 2.1
        o.cube(F + V(1.5 * math.cos(th), 0.8 + 0.6 * j, 1.5 * math.sin(th)), 0.25, th, "B_cube_lime")


def zeph_C(o, F, T, p):
    ta = int(o.age // 2 * 2)
    a = fade(ta, 600, 8, 20)
    f = fr(o.age)
    flap = (0.1 if f else -0.1)
    for sg in (1, -1):
        ax, ay = wing_axes(sg, math.radians(38) + flap)
        o.plane(F + V(sg * 1.9, 1.85, 0.5), ax, ay, 1.45, 1.45, f"C_crescent_{f}", argb(a))
    for i in range(8):                                                  # halo of crosses (Spirit Dive: crown of connected crosses)
        th = i * math.pi / 4 + ta * 0.04
        o.bb(F + V(0.75 * math.cos(th), 2.55, 0.75 * math.sin(th)), 0.4, 0.4, f"C_spark_{(f + i) % 2}")
    o.plane(F + V(0, 0.05, 0), V(1, 0, 0), V(0, 0, 1), 1.3, 1.3, f"C_swirl_{f}", argb(a))
    for j in range(3):                                                  # ribbons orbiting the body
        th = ta * 0.1 + j * 2.1
        c = F + V(1.0 * math.cos(th), 0.8 + 0.5 * j, 1.0 * math.sin(th))
        tang = V(-math.sin(th), 0, math.cos(th))
        o.plane(c, tang, UP, 0.8, 0.28, f"C_ribbon_{(f + j) % 2}", argb(q4(a * 0.9)))
    for j in range(3):
        th = -ta * 0.1 + j * 2.1 + 1
        o.bb(F + V(1.5 * math.cos(th), 1.0 + 0.6 * j, 1.5 * math.sin(th)), 0.45, 0.45, f"C_leaf_{(f + j) % 2}", 0, argb(a))


# ================================================================================================ RING ==
def ring_r(o, k=0):
    t = (o.age - 5 * k) / (o.dur * 0.9)
    return (0.6 + 6.2 * ease_out(t)) * (1 - 0.3 * k) if t > 0 else 0.0


def ring_a(o, k=0):
    t = (o.age - 5 * k) / o.dur
    return q4(1 - max(0.0, t - 0.4) / 0.6) if t > 0 else 0.0


def ring_A(o, F, T, p):
    for k in range(2):
        r, a = ring_r(o, k), ring_a(o, k)
        if a <= 0:
            continue
        o.plane(F + V(0, 0.06 * (k + 1), 0), V(1, 0, 0), V(0, 0, 1), r, r, "A_ring", argb(a))
    r, a = ring_r(o), ring_a(o)
    if a > 0:
        for j in range(8):
            th = j * math.pi / 4 + 0.3
            o.bb(F + V(r * 0.97 * math.cos(th), 0.3, r * 0.97 * math.sin(th)), 0.9, 0.9, "A_puff", j, argb(q4(a * 0.85)))
        for j in range(4):
            th = j * math.pi / 2 + o.age * 0.15
            rr = r * 0.5
            c = F + V(rr * math.cos(th), 0.2 + o.age * 0.06, rr * math.sin(th))
            o.bb(c, 0.5, 0.5, "A_spark", 0, argb(a))
            tang = V(-math.sin(th), 0, math.cos(th))
            o.strip(F + V(rr * 1.1 * math.cos(th), 0.3, rr * 1.1 * math.sin(th)) - tang * 1.0, F + V(rr * 1.1 * math.cos(th), 0.3, rr * 1.1 * math.sin(th)), 0.18, "A_streak", argb(q4(a * 0.8)))


def ring_B(o, F, T, p):
    for k, n in ((0, 14), (1, 10)):
        r, a = ring_r(o, k), ring_a(o, k)
        if a <= 0:
            continue
        sc = 0.4 + 0.6 * a
        for j in range(n):
            th = j * 2 * math.pi / n + 0.2 * k
            c = F + V(r * math.cos(th), 0.12 + 0.2 * k, r * math.sin(th))
            o.cube(c, (0.875 * sc, 0.375 * sc, 0.625 * sc) if k == 0 else (0.625 * sc, 0.25, 0.5 * sc), -th, "B_cube_white" if (j % 2 == 0) else "B_cube_mint")
    r, a = ring_r(o), ring_a(o)
    if a > 0:
        for j in range(4):
            th = j * math.pi / 2 + o.age * 0.18
            rr = r * 0.55
            o.cube(F + V(rr * math.cos(th), 0.4 + o.age * 0.07 + 0.3 * j, rr * math.sin(th)), 0.375, th, "B_cube_lime")


def ring_C(o, F, T, p):
    ta = int(o.age // 2 * 2)
    f = fr(o.age)
    for k in range(2):
        t = ta
        sub = o.age
        r, a = ring_r(o, k), ring_a(o, k)
        if a <= 0:
            continue
        o.plane(F + V(0, 0.06 * (k + 1), 0), V(1, 0, 0), V(0, 0, 1), r, r, f"C_ring_{(f + k) % 2}", argb(a))
    r, a = ring_r(o), ring_a(o)
    if a > 0:
        o.plane(F + V(0, 0.1, 0), V(1, 0, 0), V(0, 0, 1), 1.2, 1.2, f"C_swirl_{f}", argb(a))
        for j in range(6):
            th = j * math.pi / 3 + ta * 0.12
            rr = r * 0.55
            c = F + V(rr * math.cos(th), 0.7 + ta * 0.03, rr * math.sin(th))
            tang = V(-math.sin(th), 0, math.cos(th))
            o.plane(c, tang, UP, 0.7, 0.35, f"C_ribbon_{(f + j) % 2}", argb(q4(a * 0.9)), flip=(j % 2 == 1))
        for j in range(4):
            th = j * math.pi / 2 + 0.8
            o.bb(F + V(r * 0.5 * math.cos(th), 0.5 + ta * 0.05, r * 0.5 * math.sin(th)), 0.45, 0.45, f"C_spark_{(f + j) % 2}")


# ================================================================================================ BEAM (Spirit Storm palm beam, optional) ==
def beam_len(o, T, F):
    L = np.linalg.norm(T - F)
    return L * ease_out(o.age / 6.0)


def beam_A(o, F, T, p):
    d, s = frame_dir(F, T)
    L = beam_len(o, T, F)
    a = fade(o.age, o.dur, 2, 5)
    up2 = norm(np.cross(s, d))
    if L < 0.3:
        return
    tile = L / 2.0
    uo = -o.age * 0.18
    end = F + d * L
    for ang in (0.0, math.pi / 2):                                      # two crossed quads = a round beam
        side = s * math.cos(ang) + up2 * math.sin(ang)
        o.plane((F + end) / 2, d, side, L / 2, 0.45, "A_beam", argb(a), u=(uo, uo + tile))
    for j in range(3):
        o.bb(F + d * 0.2 + s * (j - 1) * 0.15, 1.1 - 0.2 * j, 1.1 - 0.2 * j, "A_puff", j, argb(q4(a * (1 - 0.2 * j))))
    for i in range(5):
        t = (i / 5 + o.age * 0.045) % 1.0
        if t * 16 < L:
            o.plane(F + d * (t * 16), s, up2, 0.7 + 0.15 * i % 2, 0.7 + 0.15 * i % 2, "A_swirl", argb(q4(a * 0.9)))
    for i in range(4):
        o.bb(end + (s * (i - 1.5) + up2 * ((i % 2) - 0.5)) * 0.6, 0.45, 0.45, "A_spark", 0, argb(a))


def beam_B(o, F, T, p):
    d, s = frame_dir(F, T)
    L = beam_len(o, T, F)
    a = fade(o.age, o.dur, 2, 5)
    up2 = norm(np.cross(s, d))
    yaw = math.atan2(d[0], d[2])
    k = 0.5 + 0.5 * a
    n = int(L / 1.25)
    for i in range(n):
        o.cube(F + d * (0.6 + 1.25 * i), 0.5 * k, yaw, "B_cube_white" if i % 2 == 0 else "B_cube_mint")
    for g in range(4):
        for j in range(4):
            pos = (g * 4.2 + j * 0.0 + o.age * 0.8) % 16.0
            if pos > L:
                continue
            th = j * math.pi / 2 + pos * 0.9 + o.age * 0.2
            o.cube(F + d * pos + (s * math.cos(th) + up2 * math.sin(th)) * 0.75, 0.25 * k, yaw, "B_cube_lime")


def beam_C(o, F, T, p):
    d, s = frame_dir(F, T)
    L = beam_len(o, T, F)
    ta = int(o.age // 2 * 2)
    a = fade(ta, o.dur, 2, 5)
    f = fr(o.age)
    up2 = norm(np.cross(s, d))
    if L < 0.3:
        return
    tile = L / 2.0
    uo = -(ta % 8) * 0.0625
    end = F + d * L
    for ang in (0.0, math.pi / 2):
        side = s * math.cos(ang) + up2 * math.sin(ang)
        o.plane((F + end) / 2, d, side, L / 2, 0.5, f"C_beam_{f}", argb(a), u=(uo, uo + tile))
    o.bb(F + d * 0.25, 1.8, 1.8, f"C_flash_{f}")
    for i in range(5):
        t = (i / 5 + ta * 0.045) % 1.0
        if t * 16 < L:
            o.plane(F + d * (t * 16), s, up2, 0.8, 0.8, f"C_swirl_{(f + i) % 2}", argb(q4(a * 0.9)))
    for i in range(3):
        t = (hsh(o.seed, i, 15) + ta * 0.06) % 1.0
        c = F + d * (t * min(L, 16)) + (s * (i - 1) + up2 * 0.3) * 0.8
        o.strip(c - d * 1.8, c, 0.18, f"C_streak_{f}", argb(q4(a * 0.8)))
    for i in range(4):
        o.bb(end + (s * (i - 1.5) + up2 * ((i % 2) - 0.5)) * 0.6, 0.5, 0.5, f"C_spark_{(f + i) % 2}")


# ================================================================================================ DRILL (Tornado Fang, optional) ==
LF, RBD = 6.0, 1.8


def drill_head(o, F, T):
    d = norm(T - F)
    L = np.linalg.norm(T - F)
    return d, F + d * (LF + (L - LF - 0.5) * ease_out(o.age / (o.dur * 0.9)))


def drill_frame(d):
    s = norm(np.cross(d, UP))
    return s, norm(np.cross(s, d))


def drill_A(o, F, T, p):
    d, head = drill_head(o, F, T)
    s, u2 = drill_frame(d)
    a = fade(o.age, o.dur, 2, 5)
    for k in range(5):
        g = k / 4
        c = head - d * LF * (1 - g)
        r = RBD * (1 - g) ** 0.9 + 0.15
        for j in range(5):
            th = o.age * 0.55 + j * 2 * math.pi / 5 + k * 0.9
            o.bb(c + (s * math.cos(th) + u2 * math.sin(th)) * r, 0.8 + 0.4 * (1 - g), 0.8 + 0.4 * (1 - g), "A_puff", j + k, argb(q4(a * 0.9)))
    for i in range(4):
        off = (s * math.cos(i * 1.6) + u2 * math.sin(i * 1.6)) * 1.1
        o.strip(head - d * 7.5 + off, head - d * 3.0 + off * 0.6, 0.35, "A_streak", argb(q4(a * 0.8)))
    for i in range(4):
        t = (hsh(o.seed, i, 16) + o.age * 0.06) % 1.0
        o.bb(head - d * (1 + 6 * t) + (s * (hsh(o.seed, i, 17) - 0.5) + u2 * (hsh(o.seed, i, 18) - 0.5)) * 2.6, 1.3, 0.8, "A_cloud", 0, argb(q4(a * (1 - t))))


def drill_B(o, F, T, p):
    d, head = drill_head(o, F, T)
    s, u2 = drill_frame(d)
    a = fade(o.age, o.dur, 2, 5)
    k_ = 0.4 + 0.6 * a
    yaw = math.atan2(d[0], d[2])
    counts = (6, 5, 4, 3, 1)
    for k in range(5):
        g = k / 4
        c = head - d * LF * (1 - g)
        r = RBD * (1 - g) ** 0.9 + 0.15
        sz = round((0.75 - 0.375 * g) / 0.125) * 0.125
        for j in range(counts[k]):
            th = o.age * 0.55 * (1 if k % 2 else -1) + j * 2 * math.pi / counts[k] + k * 0.8
            o.cube(c + (s * math.cos(th) + u2 * math.sin(th)) * (r if counts[k] > 1 else 0), max(0.375, sz) * k_, yaw, "B_cube_white" if (j + k) % 2 else "B_cube_mint")
    for i in range(4):                                                  # spine
        o.cube(head - d * (1.2 + 1.4 * i), 0.375 * k_, yaw, "B_cube_lime")


def drill_C(o, F, T, p):
    d, head = drill_head(o, F, T)
    s, u2 = drill_frame(d)
    ta = int(o.age // 2 * 2)
    a = fade(ta, o.dur, 2, 5)
    f = fr(o.age)

    def sp(g, j):                                                       # point on the cone surface, spiral j
        g = min(1.0, g)
        c = head - d * LF * (1 - g)
        r = RBD * (1 - g) ** 0.9 + 0.15
        th = ta * 0.5 + j * 1.05 + g * 3.2
        return c + (s * math.cos(th) + u2 * math.sin(th)) * r
    for j in range(6):                                                  # six cel bands spiralling round the cone (Tornado Fang: a drill of white bands)
        g0 = (j % 3) * 0.28
        o.strip(sp(g0, j), sp(g0 + 0.5, j), 0.9 - 0.5 * g0, f"C_ribbon_{(f + j) % 2}", argb(a), flip=(j % 2 == 1))
    for i in range(3):
        off = (s * math.cos(i * 2.1 + 0.5) + u2 * math.sin(i * 2.1 + 0.5)) * 1.0
        o.strip(head - d * 7.5 + off, head - d * 1.0 + off * 0.15, 0.45, f"C_streak_{(f + i) % 2}", argb(q4(a * 0.85)))
    o.bb(head + d * 0.2, 1.8, 1.8, f"C_flash_{f}")
    for i in range(3):
        o.bb(head - d * (1.5 + 1.2 * i) + (s * math.cos(i * 2) + u2 * math.sin(i * 2)) * 1.9, 0.6, 0.6, f"C_spark_{(f + i) % 2}")


# ================================================================================================ the scene table
V3 = V
SCENES = [
    # key, title, baseline (layer, shape or None), F, T, power, dur, cam, target, ages, figures, current-peak-quads, pack source, extra
    dict(key="slash", title="WIND_SLASH  Crescent Kamaitachi", layer="WindMagicLayer", shape="WIND_SLASH", F=V3(0, 1.5, 0), T=V3(0, 1.5, 14), p=1.0, dur=16,
         cam=(3.0, 2.2, 0.5), target=(0, 1.6, 6), ages=[1, 3, 6], figures=[[0, 0, 0, 1.8]], cap=72, gen=(slash_A, slash_B, slash_C),
         src="Black_Crescent_Kamaitachi + Yuno_crescent_wind_sickle (manga crescents)"),
    dict(key="emperor", title="WIND_EMPEROR  Slicing Wind Emperor", layer="WindSpellLayer", shape="WIND_EMPEROR", F=V3(0, 1.6, 0), T=V3(0, 1.6, 22), p=1.3, dur=20,
         cam=(3.5, 3.0, -1.0), target=(0, 2.0, 10), ages=[1, 3, 6], figures=[[0, 0, -1, 1.8], [0, 0, 22.5, 1.8]], cap=80, gen=(emp_A, emp_B, emp_C),
         src="Black_Crescent_Kamaitachi (giant black moon) + Yuno_ark_of_heavenly_wind (white swirl bands) + Gale_White_Bow (white S ribbons)"),
    dict(key="tornado", title="WIND_TORNADO  Towering Tornado", layer="WindSpellLayer", shape="WIND_TORNADO", F=V3(0, 0, 0), T=V3(0, 1, 0), p=5.0, dur=100,
         cam=(0, 4, -17), target=(0, 5, 0), ages=[6, 40, 92], figures=[[0, 0, 0, 1.8], [3, 0, 1, 1.8]], cap=96, gen=(tor_A, tor_B, tor_C),
         src="Yuno_Towering_Tornado (stacked white bands, anime) + images(1).jpg tornado concept + ae85e6bf green funnel art", sky=[0.42, 0.5, 0.58]),
    dict(key="gale", title="WIND_GALE / FX_WIND_GUST  Gust Lane", layer="WindSpellLayer", shape="WIND_GALE", F=V3(0, 1.2, 0), T=V3(0, 1.2, 12), p=1.0, dur=24,
         cam=(6.0, 2.6, 1.5), target=(0, 1.3, 6.5), ages=[5, 10, 16], figures=[[0, 0, -0.5, 1.8]], cap=66, gen=(gale_A, gale_B, gale_C),
         src="Yuno_White_Hawk (manga hawk of wind) + Gale_White_Bow (anime white ribbons + leaves)", sky=[0.42, 0.5, 0.58]),
    dict(key="zephyr", title="WIND_ZEPHYR  Spirit of Zephyr", layer="WindSpellLayer", shape="WIND_ZEPHYR", F=V3(0, 0, 0), T=V3(0, 1, 0), p=1.0, dur=600,
         cam=(0, 2, -5.5), target=(0, 1.3, 0), ages=[6, 40, 200], figures=[[0, 0, 0, 1.8]], cap=92, gen=(zeph_A, zeph_B, zeph_C),
         src="images (2).jpg Spirit of Euros (wing, crown of crosses) + owner's Spirit of Zephyr frame (phase 1)"),
    dict(key="ring", title="WIND_RING  Tempest Ring", layer="WindMagicLayer", shape="WIND_RING", F=V3(0, 0.2, 0), T=V3(0, 1, 0), p=1.0, dur=30,
         cam=(0, 5.5, -11), target=(0, 0.6, 0), ages=[4, 10, 18, 26], figures=[[0, 0, 0, 1.8]], cap=100, gen=(ring_A, ring_B, ring_C),
         src="ae85e6bf green art (ground ring with rim ticks, rotating rings) + images.jpg (dust ripples)"),
    dict(key="beam", title="WIND_BEAM (new, optional)  Spirit Storm palm beam", layer="WindSpellLayer", shape="WIND_TORNADO", F=V3(0, 1.5, 0), T=V3(0, 1.5, 16), p=3.0, dur=24,
         cam=(4.0, 2.4, -3.0), target=(0, 1.5, 7), ages=[3, 10, 19], figures=[[0, 0, 0, 1.8]], cap=100, gen=(beam_A, beam_B, beam_C), before_note="no beam shape today: Spirit Storm is drawn as the tornado",
         src="ae85e6bf green art (straight green beam with ring + sparks) + images (2).jpg (Euros arrow), canon: Spirit Storm = beam from the palm", before_scene="tornado"),
    dict(key="drill", title="WIND_DRILL (new, optional)  Tornado Fang", layer="WindSpellLayer", shape="WIND_TORNADO", F=V3(0, 1.5, 0), T=V3(0, 1.5, 14), p=2.0, dur=20,
         cam=(7.5, 2.4, 5.0), target=(0, 1.5, 5.5), ages=[3, 6, 10], figures=[[0, 0, 0, 1.8]], cap=100, gen=(drill_A, drill_B, drill_C), before_note="no drill shape today: nearest is the vertical tornado",
         src="Tornado_Fang + Black_Tornado_Fang (manga: horizontal drill with spiral debris and black shards)", before_scene="tornado"),
]


# ================================================================================================ render
def load_textures():
    for fn in sorted(os.listdir(TEXDIR)):
        if fn.endswith(".png"):
            pv._tex_cache["mc/" + fn[:-4]] = np.asarray(Image.open(os.path.join(TEXDIR, fn)).convert("RGBA"), dtype=np.float32) / 255.0


def make_quads(sc, ver, age, seed=12345):
    o = Out(sc["cam"], sc["target"], age, sc["dur"], seed)
    sc["gen"]["ABC".index(ver)](o, sc["F"], sc["T"], sc["p"])
    return o.finish(), o.cubes


def peak_stats(sc, ver):
    """worst case over every tick of the effect's life (capped at 120 ticks, the effects of 600 ticks repeat)"""
    pk_q = pk_c = 0
    for age in range(0, min(sc["dur"], 120) + 1):
        q, c = make_quads(sc, ver, age)
        pk_q, pk_c = max(pk_q, len(q)), max(pk_c, c)
    return pk_q, pk_c


def scene_dict(sc):
    d = {"layer": sc["layer"], "shape": sc["shape"], "from": list(map(float, sc["F"])), "to": list(map(float, sc["T"])), "power": sc["p"], "duration": sc["dur"],
         "cam": list(sc["cam"]), "target": list(sc["target"]), "ages": sc["ages"], "figures": sc["figures"]}
    if sc.get("before_scene") == "tornado":                            # no such shape today: show Spirit Storm as it is now (tornado at the caster)
        d.update({"from": [0, 0, 0], "to": [0, 1, 0], "power": 3.0, "duration": 100, "figures": [[0, 0, 0, 1.8], [3, 0, 1, 1.8]]})
        d["cam"] = [sc["cam"][0] * 1.4, sc["cam"][1] + 1.2, sc["cam"][2] * 1.8]
        d["target"] = [0, 3.5, 0]
        d["ages"] = [40 for _ in sc["ages"]]
    for k in ("sky",):
        if k in sc:
            d[k] = sc[k]
    return d


def tile_for(quads, sd, cam, target, label, sub):
    img = pv.background(sd, cam, target)
    img = pv.draw_figures(img, sd, cam, target)
    img = pv.raster(img, quads, cam, target)
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, CW, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, CH - 13), sub, fill=(255, 235, 150))
    return t


def main():
    pv.W, pv.H = CW, CH
    load_textures()
    os.makedirs(pv.BUILD, exist_ok=True)
    classes = pv.build([sc["layer"] for sc in SCENES])
    budget = {}
    before = {}
    for sc in SCENES:                                                   # the current look, produced by the real layer class
        sd = scene_dict(sc)
        data = pv.run_java(sd, classes, os.path.join(pv.BUILD, "mc_before_" + sc["key"] + ".json"))
        before[sc["key"]] = (sd, data)
        bq = max(len(fr_["quads"]) for fr_ in data["frames"])
        budget[sc["key"]] = {"title": sc["title"], "cap_current_quads": sc["cap"], "before_peak_quads_in_preview": bq}
    sheets = {v: [] for v in "ABC"}
    cmp_rows = []
    names = {"A": "A vanilla-faithful", "B": "B voxel", "C": "C pixel-anime"}
    for sc in SCENES:
        rows = {v: [] for v in "ABC"}
        sd, data = before[sc["key"]]
        pk = {v: peak_stats(sc, v) for v in "ABC"}
        budget[sc["key"]].update({v: {"peak_quads": pk[v][0], "peak_cubes": pk[v][1], "worst_case_quads": pk[v][0] if pk[v][1] == 0 else None} for v in "ABC"})
        for v in "ABC":
            for age in sc["ages"]:
                q, c = make_quads(sc, v, age)
                rows[v].append(tile_for(q, sd, sc["cam"], sc["target"], f"{sc['key']} {names[v]}  t={age}/{sc['dur']}  {len(q)} quads" + (f", {c} cubes" if c else ""), ""))
            sheets[v].append((sc, rows[v], pk[v]))
        mid = len(sc["ages"]) // 2
        age = sc["ages"][mid]
        btile = tile_for(data["frames"][mid]["quads"], sd, sd["cam"], sd["target"], f"BEFORE (current)  t={data['frames'][mid]['age']:.0f}  {data['frames'][mid]['vertices'] // 4} quads", sc.get("before_note", ""))
        tiles = [btile] + [rows[v][mid] for v in "ABC"]
        cmp_rows.append((sc, tiles, pk))
    # per-version sheets: one row per effect, columns = ages
    for v in "ABC":
        maxc = max(len(r[1]) for r in sheets[v])
        sheet = Image.new("RGB", (CW * maxc, CH * len(sheets[v]) + 30), (16, 18, 22))
        ImageDraw.Draw(sheet).text((8, 8), f"WIND  Minecraft-style  MC-{names[v]}   (software render, no bloom, not seen in game; peak quads / cubes over the whole life in the title bars of the budget doc)", fill=(255, 255, 255))
        for i, (sc, tiles, pkv) in enumerate(sheets[v]):
            for j, t in enumerate(tiles):
                sheet.paste(t, (j * CW, 30 + i * CH))
            ImageDraw.Draw(sheet).text((CW * len(tiles) + 6 if len(tiles) < maxc else 8, 30 + i * CH + (4 if len(tiles) < maxc else CH - 28)),
                                       f"{sc['title']}: peak {pkv[0]} quads" + (f" ({pkv[1]} cubes)" if pkv[1] else "") + f", current cap {sc['cap']}", fill=(180, 255, 180))
        sheet.save(os.path.join(OUTDIR, f"wind_mc_{v}.png"))
    cmp = Image.new("RGB", (CW * 4, CH * len(cmp_rows) + 30), (16, 18, 22))
    ImageDraw.Draw(cmp).text((8, 8), "WIND  BEFORE | A vanilla-faithful | B voxel | C pixel-anime   (same camera, same tick; software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, (sc, tiles, pkv) in enumerate(cmp_rows):
        for j, t in enumerate(tiles):
            cmp.paste(t, (j * CW, 30 + i * CH))
    cmp.save(os.path.join(OUTDIR, "wind_mc_compare.png"))
    with open(os.path.join(OUTDIR, "wind_mc_budget.json"), "w") as f:
        json.dump(budget, f, indent=1)
    for k, b in budget.items():
        print(k, {v: (b[v]["peak_quads"], b[v]["peak_cubes"]) for v in "ABC"}, "cap", b["cap_current_quads"])


if __name__ == "__main__":
    main()

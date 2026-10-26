"""Animated Ultimate skill icon: Sandalphon, Lord of Judgment.

A cold white-gold rimmed medallion of midnight navy. A sword of light hangs point-down over a balance-cross, slowly
descending; where it lands a shock-ring of white-gold light pulses. Fine black cracks spread over the disc and heal.
Clock-ring rims with engraved ticks turn at different speeds in opposite directions, pale-steel motes drift, and a
highlight sweeps down the blade.

Layers (cycles per 3.2 s loop): blade descent 1, shock rings 1, cracks 1 (staggered), tick ring A +2 steps, ring B -2 steps,
ring C +1 step, ring D -1 step, god-rays 1/14 turn, rim glint 1 turn, blade sweep 1, motes 1-2, pans sway 1.

Run:  python tools/gen_sandalphon_icon_anim.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from icon_anim_lib import *  # noqa: E402,F401,F403

TAU = 2 * math.pi
CX = 127.5            # design units: the icon is designed on a 256 grid
SS = 4                # supersampling of the shape masks
HERO_T = 0.59

# ---- palette ------------------------------------------------------------------------------------------------------
NAVY_EDGE = np.array((3, 6, 18), np.float64)
NAVY_MID = np.array((12, 22, 54), np.float64)
NAVY_CORE = np.array((58, 86, 150), np.float64)
WHITE_GOLD = np.array((255, 244, 208), np.float64)
GOLD = np.array((226, 186, 104), np.float64)
STEEL = np.array((170, 188, 214), np.float64)
ICE = np.array((205, 224, 255), np.float64)

GOLD_STOPS = [(0.0, (46, 32, 18)), (0.30, (128, 92, 44)), (0.55, (214, 170, 92)), (0.80, (250, 228, 168)), (1.0, (255, 252, 236))]


def ramp(v, stops=GOLD_STOPS):
    v = np.clip(v, 0, 1)
    xs = [s[0] for s in stops]
    return np.stack([np.interp(v, xs, [s[1][c] for s in stops]) for c in range(3)], axis=-1)


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


# ---- grids / blur -------------------------------------------------------------------------------------------------
_G = {}


def grids(S):
    if S not in _G:
        k = S / 256.0
        yy, xx = np.mgrid[0:S, 0:S].astype(np.float64)
        X, Y = (xx + 0.5) / k, (yy + 0.5) / k          # design units
        dx, dy = X - CX - 0.5, Y - CX - 0.5
        R = np.hypot(dx, dy)
        TH = np.arctan2(dy, dx)
        P = S + 64
        fy = np.fft.fftfreq(P)[:, None]
        fx = np.fft.rfftfreq(P)[None, :]
        _G[S] = dict(X=X, Y=Y, dx=dx, dy=dy, R=R, TH=TH, P=P, f2=fx * fx + fy * fy, k=k)
    return _G[S]


def gblur(a, sigma, S):
    """Gaussian blur of a 2-D float array (sigma in design units) via FFT with zero padding."""
    g = grids(S)
    P = g["P"]
    s = sigma * g["k"]
    pad = np.zeros((P, P))
    o = (P - S) // 2
    pad[o:o + S, o:o + S] = a
    F = np.fft.rfft2(pad) * np.exp(-2 * math.pi ** 2 * s * s * g["f2"])
    return np.fft.irfft2(F, s=(P, P))[o:o + S, o:o + S]


class Cv:
    """Anti-aliased shape mask painter, coordinates in design units (256 grid)."""

    def __init__(self, S):
        self.S = S
        self.f = SS * S / 256.0
        self.im = Image.new("L", (S * SS, S * SS), 0)
        self.d = ImageDraw.Draw(self.im)

    def pts(self, p):
        return [(x * self.f, y * self.f) for x, y in p]

    def poly(self, p, v=255):
        self.d.polygon(self.pts(p), fill=v)

    def line(self, p, w, v=255, caps=True):
        q = self.pts(p)
        self.d.line(q, fill=v, width=max(1, int(round(w * self.f))), joint="curve")
        if caps:
            r = w * self.f / 2
            for x, y in (q[0], q[-1]):
                self.d.ellipse((x - r, y - r, x + r, y + r), fill=v)

    def ell(self, cx, cy, rx, ry=None, v=255):
        ry = rx if ry is None else ry
        f = self.f
        self.d.ellipse(((cx - rx) * f, (cy - ry) * f, (cx + rx) * f, (cy + ry) * f), fill=v)

    def arr(self):
        return np.asarray(self.im.resize((self.S, self.S), Image.BOX), np.float64) / 255.0


def over(img, col, m):
    m = m[..., None]
    img[:] = img * (1 - m) + np.asarray(col, np.float64) * m


def add(img, col, m, k=1.0):
    img += np.asarray(col, np.float64) * (m * k)[..., None]


def metal_shade(m, S, sigma=1.6, gain=3.2, ambient=0.50):
    """Fake bevel lighting from the mask's own edges (light from the upper left). Returns a 0..1 shade."""
    g = grids(S)
    h = gblur(m, sigma, S)
    gy, gx = np.gradient(h)
    gx, gy = gx * g["k"], gy * g["k"]
    dot = -(gx * -0.62 + gy * -0.78) * gain * sigma
    inner = np.clip(h, 0, 1)
    shade = ambient + dot * 0.5 + (inner - 0.5) * 0.12 - (g["Y"] - 128) / 256.0 * 0.20
    return np.clip(shade, 0, 1)


# ---- static pieces ------------------------------------------------------------------------------------------------
_ST = {}


def build_cracks(seed=11):
    """Cracks as (main polyline, [(start_px, branch polyline)]) from the inner rim toward the core."""
    rng = np.random.default_rng(seed)
    out = []
    n = 11
    for i in range(n):
        a = (i + rng.uniform(-0.28, 0.28)) * TAU / n + 0.2
        px, py = CX + math.cos(a) * 104, CX + math.sin(a) * 104
        d = a + math.pi + rng.normal(0, 0.12)
        total = rng.uniform(46, 86)
        main = [(px, py)]
        L = 0.0
        branches = []
        while L < total:
            step = rng.uniform(3.5, 7.0)
            d += rng.normal(0, 0.2) + (0.28 if (len(main) % 2) else -0.28) * 0.5
            px += math.cos(d) * step
            py += math.sin(d) * step
            L += step
            main.append((px, py))
            if rng.random() < 0.15 and L > 10 and L < total - 10:
                bd = d + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0)
                bl = rng.uniform(12, 30)
                bp = [(px, py)]
                bx, by, bL = px, py, 0.0
                while bL < bl:
                    st = rng.uniform(3, 5.5)
                    bd += rng.normal(0, 0.22)
                    bx += math.cos(bd) * st
                    by += math.sin(bd) * st
                    bL += st
                    bp.append((bx, by))
                branches.append((L, bp, bL))
        out.append((main, L, branches, rng.uniform(-0.06, 0.06)))
    return out


def partial(poly, vis):
    """Points of the polyline up to path length vis (with a cut last segment) + per-point length."""
    pts = [poly[0]]
    acc = 0.0
    for a, b in zip(poly[:-1], poly[1:]):
        sl = math.hypot(b[0] - a[0], b[1] - a[1])
        if acc + sl >= vis:
            u = (vis - acc) / max(sl, 1e-6)
            pts.append((a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u))
            return pts
        pts.append(b)
        acc += sl
    return pts


def crack_mask(S, t):
    cv = Cv(S)
    heal_total = np.zeros(1)
    for main, L, branches, delay in _ST["cracks"]:
        tau = TAU * (t - delay)
        f = smooth(0.02, 0.62, 0.5 - 0.5 * math.cos(tau))
        if f <= 0:
            continue
        reach = L + max(b[0] + b[2] for b in branches) if branches else L
        g = f * max(L, reach * 0.95)
        pm = partial(main, min(g, L))
        # tapered: thick at the rim, hair-thin at the tip
        for j in range(len(pm) - 1):
            w = 2.7 - 1.7 * (j / max(1, len(main) - 1))
            cv.line([pm[j], pm[j + 1]], max(0.7, w), caps=True)
        for s, bp, bL in branches:
            vis = min(max(g - s, 0), bL)
            if vis > 0.3:
                pb = partial(bp, vis)
                for j in range(len(pb) - 1):
                    cv.line([pb[j], pb[j + 1]], max(0.6, 1.6 - 0.2 * j), caps=True)
    return cv.arr()


def crack_heal(t):
    """0..1 amount of white-gold light bleeding out of the cracks while they close (mean over the staggered cracks)."""
    v = 0.0
    for _, _, _, delay in _ST["cracks"]:
        v += max(0.0, -math.sin(TAU * (t - delay)))
    return v / len(_ST["cracks"])


def build_motes(seed=5):
    rng = np.random.default_rng(seed)
    motes = []
    for i in range(34):
        a = rng.uniform(0, TAU)
        r = math.sqrt(rng.uniform(0.02, 1.0)) * 96
        motes.append(dict(x=CX + math.cos(a) * r, y=CX + math.sin(a) * r + 14, vy=rng.uniform(10, 26), vx=rng.uniform(-5, 5),
                          ph=rng.random(), k=1 if i % 3 else 2, size=rng.uniform(0.7, 1.9), wob=rng.uniform(1.0, 3.0),
                          wph=rng.random(), cold=rng.random() < 0.55))
    return motes


def ensure_static():
    if "cracks" not in _ST:
        _ST["cracks"] = build_cracks()
        _ST["motes"] = build_motes()


# ---- animated pieces ----------------------------------------------------------------------------------------------

def ticks(R, TH, r0, r1, n, shift_steps, t, duty=0.22, major=0, major_r0=None, aa=0.7, k=1.0):
    """Analytic tick ring: n ticks, rotating by shift_steps tick spacings per loop. Returns a 0..1 mask."""
    sp = TAU / n
    ph = ((TH - shift_steps * sp * t) / sp) % 1.0
    tang = (ph - 0.5) * sp * R                                   # signed distance along the ring in design units
    half = duty * sp * R / 2
    m_t = 1 - smooth(half - aa, half + aa, np.abs(tang))
    if major:
        idx = np.round(((TH - shift_steps * sp * t) / sp)).astype(np.int64) % n
        is_major = (idx % major) == 0
        rr0 = np.where(is_major, major_r0 if major_r0 is not None else r0 - 2, r0)
    else:
        rr0 = r0
    m_r = smooth(rr0 - aa, rr0 + aa, R) * (1 - smooth(r1 - aa, r1 + aa, R))
    return m_t * m_r


def ring_line(R, r, w=0.5, aa=0.7):
    return 1 - smooth(w, w + aa * 2, np.abs(R - r))


def draw_background(img, t, S):
    g = grids(S)
    R, TH, dx, dy = g["R"], g["TH"], g["dx"], g["dy"]
    d = np.hypot(dx, dy - 0) / 120.0
    core = np.exp(-(np.hypot(dx, dy + 22) / 62.0) ** 2)
    mid = np.exp(-(d / 0.78) ** 2)
    bg = NAVY_EDGE + (NAVY_MID - NAVY_EDGE) * mid[..., None] + (NAVY_CORE - NAVY_MID) * core[..., None]
    img[:] = bg
    # slow god-rays from behind the sword (rotating 1/14 turn per loop)
    ray = np.maximum(0, np.sin(14 * TH - TAU * t)) ** 3
    ray2 = np.maximum(0, np.sin(9 * TH + TAU * t + 1.3)) ** 4
    fall = np.exp(-(np.hypot(dx, dy + 14) / 78.0) ** 1.4)
    add(img, (120, 150, 215), (ray * 0.16 + ray2 * 0.07) * fall)


def draw_rings(img, t, S):
    g = grids(S)
    R, TH = g["R"], g["TH"]
    # ring D (inner, thin steel circle with 8 beads), turns -1 step
    dline = ring_line(R, 60.5, 0.35) * 0.35
    add(img, STEEL, dline)
    n8 = 8
    sp = TAU / n8
    a = (TH + 1 * sp * t)
    bead = np.exp(-(np.hypot(R - 60.5, (((a / sp) % 1.0) - 0.5) * sp * R) / 1.7) ** 2)
    add(img, ICE, bead, 0.8)
    # ring C: 12 steel arcs with gaps, turns +1 step; fine lines at the edges
    add(img, STEEL, ring_line(R, 73.0, 0.3) * 0.28)
    add(img, STEEL, ring_line(R, 81.0, 0.3) * 0.28)
    n12 = 12
    sp = TAU / n12
    ph = ((TH - 1 * sp * t) / sp) % 1.0
    arc = smooth(0.06, 0.12, ph) * (1 - smooth(0.62, 0.68, ph))
    band = smooth(74.0, 74.8, R) * (1 - smooth(79.6, 80.4, R))
    add(img, STEEL, arc * band, 0.55)
    # ring B: chunky pale-gold blocks between two gold lines, turns -2 steps
    add(img, GOLD, ring_line(R, 88.0, 0.45) * 0.75)
    add(img, WHITE_GOLD, ring_line(R, 98.4, 0.45) * 0.8)
    blk = ticks(R, TH, 90.2, 96.2, 24, -2, t, duty=0.46, major=4, major_r0=89.4, k=1)
    add(img, WHITE_GOLD, blk, 0.55)
    small = ticks(R, TH, 90.2, 92.4, 72, -6, t, duty=0.3)
    add(img, GOLD, small, 0.4 * (1 - blk))


def draw_blade_light(img, blade_m, pulse, S):
    gl = gblur(blade_m, 5.0, S) * 0.85 + gblur(blade_m, 14.0, S) * 0.7 + gblur(blade_m, 34.0, S) * 0.45
    add(img, (255, 238, 190), gl, 0.46 * pulse)
    add(img, (120, 160, 235), gblur(blade_m, 26.0, S), 0.30 * pulse)


def balance_geometry(t):
    """Returns the balance parts in design units (beam tilt sways +-1.4 deg once per loop)."""
    ang = 0.025 * math.sin(TAU * t)
    px, py = CX, 182.0
    c, s = math.cos(ang), math.sin(ang)

    def rot(x, y):
        x, y = x - px, y - py
        return px + x * c - y * s, py + x * s + y * c

    return rot


def draw_balance(img, t, S, glowk):
    rot = balance_geometry(t)
    cv = Cv(S)
    half = 54
    # beam: lens shaped, slightly arched
    top = [rot(CX + u, 182 - 3.4 * (1 - (u / half) ** 2) - 0.9) for u in np.linspace(-half, half, 17)]
    bot = [rot(CX + u, 182 + 2.6 * (1 - (u / half) ** 2) + 0.9) for u in np.linspace(half, -half, 17)]
    cv.poly(top + bot)
    for sx in (-1, 1):
        ex, ey = rot(CX + sx * half, 182)
        cv.ell(ex, ey, 3.6)
        cv.ell(ex, ey - 3.6, 1.6)
        # chains and pans
        bx, by = rot(CX + sx * (half), 182 + 23)
        for o in (-11, 11):
            cv.line([(ex, ey + 1), (bx + o, by)], 0.7, caps=False)
        pan = [(bx + 14 * math.cos(a), by + 7.5 * math.sin(a)) for a in np.linspace(0, math.pi, 14)]
        cv.poly(pan)
        cv.line([(bx - 14, by), (bx + 14, by)], 1.1)
        cv.ell(bx, by + 8.8, 2.0, 1.4)
    # stem + base
    cv.poly([(CX - 3.4, 184), (CX + 3.4, 184), (CX + 2.5, 214), (CX - 2.5, 214)])
    cv.poly([(CX - 14, 220.5), (CX - 8, 213.5), (CX + 8, 213.5), (CX + 14, 220.5)])
    cv.ell(CX, 207, 4.4, 2.2)
    m = cv.arr()
    # drop shadow
    sh = gblur(np.roll(np.roll(m, 2, 0), 1, 1), 2.2, S)
    img *= (1 - 0.55 * np.clip(sh, 0, 1))[..., None]
    shade = metal_shade(m, S)
    over(img, ramp(shade * 0.92 + 0.04), m)
    # pivot gem (cold white-gold diamond) that flares when the blade lands
    gx, gy = rot(CX, 181.2)
    gm = Cv(S)
    gm.poly([(gx, gy - 8), (gx + 6.2, gy), (gx, gy + 8), (gx - 6.2, gy)])
    gem = gm.arr()
    gcol = np.array((150, 190, 255)) * (1 - glowk) + np.array((255, 252, 240)) * glowk
    over(img, gcol, gem * 0.95)
    gi = Cv(S)
    gi.poly([(gx, gy - 8), (gx + 6.2, gy), (gx, gy + 0.5)])
    over(img, (240, 248, 255), gi.arr() * 0.8)
    add(img, (255, 240, 200), gblur(gem, 5, S), 0.9 * (0.35 + 0.65 * glowk))
    return m


def sword_masks(dy, S, t):
    """Sword parts shifted by dy. Returns (blade_full, blade_left_face, edge, metal)."""
    b = Cv(S)
    bl = Cv(S)
    cx = CX
    hw = 13.4
    ys, ye = 82.0, 171.0
    half = [(hw, ys), (hw - 0.8, 120), (hw * 0.64, 148), (hw * 0.28, 162), (0.0, ye)]
    blade = [(cx + x, y + dy) for x, y in half] + [(cx - x, y + dy) for x, y in reversed(half[:-1])]
    b.poly(blade)
    bl.poly([(cx - x, y + dy) for x, y in half] + [(cx, ys + dy)])
    full = b.arr()
    left = bl.arr()
    # metal: grip, guard, pommel
    mt = Cv(S)
    mt.poly([(cx - 4.8, 49 + dy), (cx + 4.8, 49 + dy), (cx + 4.2, 72 + dy), (cx - 4.2, 72 + dy)])
    # winged guard
    right = [(6, 63.5), (16, 60.5), (31, 60.5), (46, 65), (58, 76), (50, 77), (38, 73.5), (25, 73), (15, 77), (9, 82.5), (0, 85)]
    pts = [(cx + x, y + dy) for x, y in right] + [(cx - x, y + dy) for x, y in reversed(right)]
    mt.poly(pts)
    mt.ell(cx, 44 + dy, 6.4)
    mt.poly([(cx - 3, 36.5 + dy), (cx + 3, 36.5 + dy), (cx, 33 + dy)])
    mt.poly([(cx - 4.2, 84 + dy), (cx + 4.2, 84 + dy), (cx, 92 + dy)])
    return full, left, mt.arr()


def draw_sword(img, t, S, pulse, impact):
    dy = -8.0 * math.cos(TAU * t)
    full, left, metal = sword_masks(dy, S, t)
    g = grids(S)
    X, Y = g["X"], g["Y"]
    # dark halo so the silhouette reads on bright backgrounds
    halo = np.clip(gblur(np.maximum(full, metal), 2.4, S) * 1.5, 0, 1)
    img *= (1 - 0.5 * halo)[..., None]
    # metal parts
    shade = metal_shade(metal, S, sigma=1.4)
    over(img, ramp(shade * 0.95 + 0.03), metal)
    # grip wraps
    wr = np.where(((Y - dy) % 4.2) < 1.2, 1.0, 0.0) * (np.abs(X - CX) < 4.2) * ((Y - dy) > 51) * ((Y - dy) < 62)
    over(img, (40, 34, 52), metal * wr * 0.7)
    # pommel gem
    gm = np.exp(-(np.hypot(g["X"] - CX, g["Y"] - 44 - dy) / 2.6) ** 2)
    add(img, (170, 215, 255), gm, 0.9)
    # blade: two faces + gradient + edge
    vy = np.clip((Y - dy - 82) / 89.0, 0, 1)
    top_col = np.array((246, 226, 168))
    tip_col = np.array((255, 253, 244))
    col = top_col + (tip_col - top_col) * smooth(0.0, 0.8, vy)[..., None]
    right_col = col * np.array((0.80, 0.88, 1.0)) + np.array((-4, 0, 6))
    face = np.where((X < CX)[..., None], col, right_col)
    over(img, face, full)
    # soft fuller (slim navy groove with a bright lip), centre ridge
    fuller = np.exp(-((X - CX) / 2.3) ** 2) * smooth(84, 94, Y - dy) * (1 - smooth(132, 150, Y - dy)) * full
    over(img, (190, 206, 238), fuller * 0.30)
    ridge = np.exp(-((X - CX) / 0.7) ** 2) * full * (1 - smooth(150, 166, Y - dy))
    add(img, (255, 255, 255), ridge, 0.9)
    # bright edge rim (gold white)
    er = np.clip(full - gblur(full, 1.4, S) * 1.0 + 0.0, 0, 1) * 1.6
    add(img, WHITE_GOLD, np.clip(er, 0, 1) * full, 0.9)
    # sweeping highlight (diagonal band travelling down the blade, once per loop)
    sy = 66 + 120 * ((t * 1) % 1.0)
    band = np.exp(-(((Y - dy) - 0.55 * (X - CX) - sy) / 5.5) ** 2) * full
    add(img, (255, 255, 252), band, 0.95)
    add(img, (255, 236, 190), gblur(band, 3.0, S), 0.6)
    # tip sparkle (brightest at the landing)
    tipx, tipy = CX, 170.0 + dy
    st = np.exp(-(((X - tipx) / 16.0) ** 2 + ((Y - tipy) / 1.5) ** 2)) + np.exp(-(((X - tipx) / 1.5) ** 2 + ((Y - tipy) / 16.0) ** 2))
    glowt = np.exp(-(np.hypot(X - tipx, Y - tipy) / 7.0) ** 2)
    add(img, (255, 248, 225), st, 0.35 + 0.65 * impact)
    add(img, (255, 240, 200), glowt, 0.5 + 0.7 * impact)
    return full, dy


def draw_shock(img, t, S):
    g = grids(S)
    X, Y, R = g["X"], g["Y"], g["R"]
    fade_disc = 1 - smooth(88, 104, R)
    for delay, size, strength in ((0.0, 1.0, 1.0), (0.11, 0.7, 0.5)):
        p = ((t - 0.5 - delay) % 1.0)
        if p > 0.7:
            continue
        u = p / 0.7
        e = 1 - (1 - u) ** 2.2
        rx = 6 + 84 * size * e
        ry = rx * 0.27
        ex = (X - CX) / rx
        ey = (Y - 179.5) / ry
        dist = (np.hypot(ex, ey) - 1.0) * rx
        w = 2.0 + 3.0 * u
        amp = (1 - u) ** 1.6 * smooth(0.0, 0.06, u) * strength
        ringm = np.exp(-(dist / w) ** 2)
        front = 0.65 + 0.35 * (Y > 179.5)
        add(img, (255, 238, 188), ringm * front * fade_disc, 1.25 * amp)
        add(img, WHITE_GOLD, gblur(ringm * front * fade_disc, 3.5, S), 1.1 * amp)
        add(img, (150, 190, 255), np.exp(-(dist / (w * 3.5)) ** 2) * fade_disc * 0.45 * front, amp)


def draw_motes(img, t, S):
    g = grids(S)
    X, Y, R = g["X"], g["Y"], g["R"]
    cv = Cv(S)
    big = Cv(S)
    cols = []
    for m in _ST["motes"]:
        ph = (t * m["k"] + m["ph"]) % 1.0
        y = m["y"] - m["vy"] * ph * 1.6
        x = m["x"] + m["vx"] * ph + m["wob"] * math.sin(TAU * (t + m["wph"]))
        a = math.sin(math.pi * ph) ** 2
        r = math.hypot(x - CX, y - CX)
        if r > 100 or a < 0.03:
            continue
        a *= 1 - smooth(88, 100, r)
        v = int(255 * min(1, a))
        (big if m["size"] > 1.4 else cv).ell(x, y, m["size"], m["size"], v)
    sm, bg = cv.arr(), big.arr()
    add(img, STEEL, sm, 1.0)
    add(img, (225, 238, 255), bg, 1.0)
    add(img, (170, 200, 255), gblur(bg, 3.0, S), 1.2)


def draw_rim(img, t, S):
    g = grids(S)
    R, TH = g["R"], g["TH"]
    # groove with ring A ticks (turning +2 steps)
    groove = smooth(103.4, 104.4, R) * (1 - smooth(111.0, 112.0, R))
    over(img, (6, 9, 24), groove * 0.95)
    tk = ticks(R, TH, 105.6, 109.8, 60, 2, t, duty=0.2, major=5, major_r0=104.6)
    over(img, WHITE_GOLD * 0.9 + GOLD * 0.1, tk * groove, )
    tk2 = ticks(R, TH, 105.6, 107.6, 120, 4, t, duty=0.28)
    add(img, GOLD, tk2 * groove, 0.3 * (1 - tk))
    # polished outer band
    r0, r1 = 111.2, 120.4
    band = smooth(r0 - 0.5, r0 + 0.5, R) * (1 - smooth(r1 - 0.8, r1 + 0.5, R))
    u = np.clip((R - r0) / (r1 - r0), 0, 1)
    nr = np.cos(math.pi * u)                         # +1 outer edge, -1 inner edge
    rh = np.stack([np.cos(TH), np.sin(TH)], -1)
    lit = rh[..., 0] * -0.62 + rh[..., 1] * -0.78
    shade = 0.52 + 0.30 * nr * lit + 0.12 * np.sqrt(np.clip(1 - nr * nr, 0, 1)) + 0.10 * lit
    # engraved bands inside the rim (fine groove)
    shade -= 0.16 * np.exp(-((R - 115.6) / 0.55) ** 2)
    shade += 0.14 * np.exp(-((R - 112.2) / 0.5) ** 2)
    shade += 0.20 * np.exp(-((R - 119.4) / 0.7) ** 2) * (0.5 + 0.5 * lit)
    # glint sweeping once around the rim, plus a faint counter glint
    d1 = np.angle(np.exp(1j * (TH - (TAU * t - 2.2))))
    d2 = np.angle(np.exp(1j * (TH - (TAU * t + 1.7 + math.pi))))
    spec = np.exp(-(d1 / 0.30) ** 2) * 0.55 + np.exp(-(d2 / 0.2) ** 2) * 0.18
    shade = np.clip(shade + spec * (0.55 + 0.45 * (1 - np.abs(nr))), 0, 1)
    over(img, ramp(shade), band)
    # tiny gems every 45 deg set into the band
    for k in range(8):
        a = k * TAU / 8 + math.pi / 8
        gx, gy = CX + math.cos(a) * 115.6, CX + math.sin(a) * 115.6
        gem = np.exp(-(np.hypot(g["X"] - gx, g["Y"] - gy) / 2.1) ** 2)
        over(img, (14, 22, 52), np.clip(gem * 1.8, 0, 1) * band)
        add(img, (170, 210, 255), np.exp(-(np.hypot(g["X"] - gx + 0.5, g["Y"] - gy + 0.5) / 1.0) ** 2), 0.9)
    # inner lip highlight line between groove and field
    add(img, WHITE_GOLD, ring_line(R, 103.6, 0.3), 0.6)


def draw(t, S):
    ensure_static()
    g = grids(S)
    R = g["R"]
    img = np.zeros((S, S, 3))
    cosd = math.cos(TAU * (t - 0.5))
    pulse = 0.80 + 0.20 * (0.5 + 0.5 * cosd)               # blade brightest at the landing
    impact = (0.5 + 0.5 * cosd) ** 5

    draw_background(img, t, S)
    draw_rings(img, t, S)

    # cracks: black with a pale lip, and a gold-white bleed while they heal
    cm = crack_mask(S, t)
    if cm.max() > 0:
        lip = np.clip(gblur(cm, 1.3, S) * 2.2 - cm * 1.4, 0, 1)
        add(img, (190, 210, 245), lip, 0.4)
        heal = crack_heal(t)
        add(img, (255, 236, 180), gblur(cm, 2.2, S), min(1.0, heal * 2.6) * 1.1)
        over(img, (2, 3, 8), np.clip(cm, 0, 1) * 0.96)

    # blade light bleeds on the background first
    dy = -8.0 * math.cos(TAU * t)
    _, left, _ = sword_masks(dy, S, t)
    full0 = Cv(S)
    blade_poly = [(CX - 13, 82 + dy), (CX + 13, 82 + dy), (CX + 4, 160 + dy), (CX, 171 + dy), (CX - 4, 160 + dy)]
    full0.poly(blade_poly)
    draw_blade_light(img, full0.arr(), pulse, S)

    draw_balance(img, t, S, impact)
    draw_shock(img, t, S)
    draw_sword(img, t, S, pulse, impact)
    draw_motes(img, t, S)

    # depth: vignette toward the edge, soft gloss at the upper left
    vig = 1 - 0.62 * smooth(48, 112, R) ** 1.3
    img *= vig[..., None]
    gloss = np.exp(-(np.hypot(g["X"] - 78, g["Y"] - 62) / 52.0) ** 2) * (1 - smooth(92, 106, R))
    add(img, (150, 185, 255), gloss, 0.05)

    draw_rim(img, t, S)

    rgb = np.clip(img, 0, 255).astype(np.uint8)
    alpha = np.array(disc_mask(S, 0.47))
    out = np.dstack([rgb, alpha])
    return Image.fromarray(out, "RGBA")


if __name__ == "__main__":
    build("sandalphon", draw, hero_t=HERO_T)

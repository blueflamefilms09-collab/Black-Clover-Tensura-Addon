"""Solomon, Lord of Wisdom - animated Ultimate skill icon (new art, no classic file).

A deep violet medallion with a gold rim, a band of rune glyphs, an emerald tick ring, an interlaced gold hexagram
(emerald inlay, emerald gems on the tips) and an open grimoire whose pages throw a white-hot emerald light.
Spell lights (emerald / gold / violet) orbit at three radii with fading trails, motes rise from the book, star-dust
drifts, and a highlight sweeps over the gold.

Every motion is a whole number of loops (or a whole number of the layer's own symmetry steps), so t=0 and t=1 match.
Run:  python3 tools/gen_solomon_icon_anim.py
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
from icon_anim_lib import *  # noqa: F401,F403  (build, disc_mask, glow, lerp, canvas ...)

S = 256
K = 2                      # mask supersampling
C = S / 2.0
TAU = math.tau

# ---- palette ---------------------------------------------------------------------------------------------------------
EMER = np.array([0.18, 1.0, 0.46])
EMER_HI = np.array([0.66, 1.0, 0.74])
GOLD_L = np.array([1.0, 0.82, 0.34])
VIOL_L = np.array([0.78, 0.55, 1.0])
WHITE = np.array([1.0, 1.0, 1.0])

yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
X = xx - (S - 1) / 2.0
Y = yy - (S - 1) / 2.0
RR = np.hypot(X, Y)
TH = np.arctan2(Y, X)

_cache = {}


# ---- small numpy helpers ---------------------------------------------------------------------------------------------
def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def ramp(v, stops):
    """Colour ramp: v in 0..1 -> RGB (0..1); stops = [(pos, (r,g,b) 0..255), ...]."""
    pos = [p for p, _ in stops]
    out = np.zeros(v.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(v, pos, [col[c] / 255.0 for _, col in stops])
    return out


def blur_arr(a, r):
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def blur_rgb(a, r):
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "RGB").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def to_arr(maskL):
    """512 'L' mask -> 256 float array."""
    return np.asarray(maskL.resize((S, S), Image.LANCZOS), np.float32) / 255.0


def add_blob(E, cx, cy, sig, col, amp):
    r = int(sig * 3.3) + 1
    x0, x1 = max(0, int(cx) - r), min(S, int(cx) + r + 2)
    y0, y1 = max(0, int(cy) - r), min(S, int(cy) + r + 2)
    if x1 <= x0 or y1 <= y0:
        return
    d2 = (xx[y0:y1, x0:x1] - cx) ** 2 + (yy[y0:y1, x0:x1] - cy) ** 2
    sub = np.exp(-d2 / (2 * sig * sig)) * amp
    E[y0:y1, x0:x1] += sub[..., None] * col


def pol(r, deg):
    a = math.radians(deg)
    return C + r * math.cos(a), C + r * math.sin(a)


def mk():
    return Image.new("L", (S * K, S * K), 0)


def ell(d, cx, cy, r, fill):
    d.ellipse([(cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K], fill=fill)


def ring(d, r0, r1, fill, inner_fill=0):
    ell(d, C, C, r1, fill)
    ell(d, C, C, r0, inner_fill)


def line(d, p, q, w, fill=255):
    d.line([p[0] * K, p[1] * K, q[0] * K, q[1] * K], fill=fill, width=max(1, int(round(w * K))))


# ---- static art (built once) -----------------------------------------------------------------------------------------
def build_static():
    rng = np.random.default_rng(72)

    # background: violet field with depth + faint magic-circle engraving
    d = RR / 120.0
    base = ramp(d, [(0.0, (132, 58, 206)), (0.28, (98, 40, 168)), (0.6, (56, 20, 108)), (0.85, (26, 10, 56)), (1.0, (8, 3, 18))])
    nz = blur_arr(rng.random((S, S)).astype(np.float32), 14)
    nz = (nz - nz.mean()) / (nz.std() + 1e-6)
    base *= (1.0 + 0.10 * nz[..., None])
    # faint engraved concentric lines and 12 spokes inside the hexagram field
    eng = np.zeros((S, S), np.float32)
    for rr_ in (30, 52, 66):
        eng += np.exp(-((RR - rr_) / 0.9) ** 2) * 0.35
    spokes = np.abs(np.sin(TH * 6.0)) < 0.045
    eng += (spokes & (RR > 14) & (RR < 80)) * 0.18
    base += eng[..., None] * np.array([0.30, 0.20, 0.55])
    # static tiny stars
    st = np.zeros((S, S), np.float32)
    for _ in range(46):
        a = rng.uniform(0, TAU)
        r = 14 + 80 * math.sqrt(rng.random())
        px, py = C + r * math.cos(a), C + r * math.sin(a)
        st += np.exp(-((xx - px) ** 2 + (yy - py) ** 2) / (2 * 0.55 ** 2)) * rng.uniform(0.12, 0.35)
    base += st[..., None] * np.array([0.8, 0.75, 1.0])
    # rune band panel (darker, inset)
    band = smoothstep(95.0, 96.5, RR) * (1 - smoothstep(109.5, 111.0, RR))
    shade = 1 - 0.42 * band
    inner_shadow = np.exp(-((RR - 96.0) / 2.5) ** 2) * 0.25 + np.exp(-((RR - 110.0) / 3.0) ** 2) * 0.30
    base = base * shade[..., None] * (1 - inner_shadow[..., None])
    base += band[..., None] * np.array([0.085, 0.030, 0.130])
    # tick ring zone slightly darker too
    zone = smoothstep(84.0, 85.0, RR) * (1 - smoothstep(94.5, 95.5, RR))
    base *= (1 - 0.35 * zone)[..., None]
    _cache["bg"] = np.clip(base, 0, 1)

    # gold: static plates (rim, thin rings, book cover)
    g = mk()
    dg = ImageDraw.Draw(g)
    ring(dg, 110.0, 119.6, 225)                  # rim plate
    ring(dg, 117.6, 119.6, 235)                  # outer lip
    ring(dg, 110.0, 112.2, 235)                  # inner lip
    ring(dg, 112.4, 113.0, 185, 0)                # engraved groove (hairline)
    for i in range(24):                          # beads
        x, y = pol(115.2, i * 15.0 + 7.5)
        ell(dg, x, y, 3.4, 255)
    for i in range(24):                          # tiny diamonds between the beads (filigree)
        x, y = pol(115.2, i * 15.0)
        ell(dg, x, y, 0.8, 200)
    ring(dg, 94.6, 97.2, 240)                    # band outer ring
    ring(dg, 85.0, 86.4, 220)                    # tick-ring outer hairline
    ring(dg, 79.6, 83.8, 250)                    # hexagram ring
    # book cover (gold-trimmed leather edge), mirrored halves
    def cover(sign):
        pts = [(128, 156.5), (128 + sign * 14, 154.0), (128 + sign * 36.5, 147.8), (128 + sign * 38.5, 131.5),
               (128 + sign * 37.5, 129.2), (128 + sign * 20, 134.0), (128, 140.0)]
        dg.polygon([(x * K, y * K) for x, y in pts], fill=215)
    cover(-1)
    cover(1)
    _cache["gold_static"] = g

    # rune glyphs (6 distinct, repeated 3x -> 18 around, 3-fold symmetric so a 120 degree turn is seamless)
    r2 = np.random.default_rng(11)
    glyphs = []
    for _ in range(6):
        pts_ = [(u, v) for u in (-1, 0, 1) for v in (-1.5, -0.5, 0.5, 1.5)]
        cur = pts_[int(r2.integers(len(pts_)))]
        segs = []
        for _s in range(3 + int(r2.integers(2))):
            nxt = cur
            while nxt == cur or (abs(nxt[0] - cur[0]) == 0 and abs(nxt[1] - cur[1]) == 0):
                nxt = pts_[int(r2.integers(len(pts_)))]
            segs.append((cur, nxt))
            cur = nxt
        stem = bool(r2.integers(2))
        glyphs.append((segs, stem))
    rn = mk()
    dr = ImageDraw.Draw(rn)
    for i in range(18):
        ang = i * 20.0
        a = math.radians(ang)
        ux, uy = -math.sin(a), math.cos(a)        # tangent
        vx, vy = math.cos(a), math.sin(a)         # radial
        cx0, cy0 = C + 103.0 * vx, C + 103.0 * vy
        segs, stem = glyphs[i % 6]

        def wp(p):
            return (cx0 + ux * p[0] * 3.1 + vx * p[1] * 3.3, cy0 + uy * p[0] * 3.1 + vy * p[1] * 3.3)
        for (p, q) in segs:
            line(dr, wp(p), wp(q), 2.4)
        if stem:
            line(dr, wp((0, -1.5)), wp((0, 1.5)), 1.1, 200)
        # separator diamond between glyphs
        sx, sy = pol(103.0, ang + 10.0)
        ell(dr, sx, sy, 1.8, 230)
    _cache["runes"] = rn

    # hexagram (interlaced) + its emerald inlay channel
    def hexagram(width, cut_scale, tip_bezel):
        Rh = 77.0
        A, B = mk(), mk()
        dA, dB = ImageDraw.Draw(A), ImageDraw.Draw(B)
        tipsA = [pol(Rh, a) for a in (-90, 30, 150)]
        tipsB = [pol(Rh, a) for a in (90, 210, 330)]
        for dd, tips in ((dA, tipsA), (dB, tipsB)):
            for i in range(3):
                line(dd, tips[i], tips[(i + 1) % 3], width)
            for p in tips:
                ell(dd, p[0], p[1], width / 2.0, 255)
        rho = Rh / math.sqrt(3.0)
        for k in range(6):
            px, py = pol(rho, 60 * k)
            tgt = ImageDraw.Draw(B if k % 2 == 0 else A)
            ell(tgt, px, py, 1.95 * 3.55 * cut_scale, 0)
        arrA = np.asarray(A, np.uint8)
        arrB = np.asarray(B, np.uint8)
        m = Image.fromarray(np.maximum(arrA, arrB), "L")
        if tip_bezel:
            dm = ImageDraw.Draw(m)
            for p in tipsA + tipsB:
                ell(dm, p[0], p[1], 7.4, 255)
        return m
    _cache["hex"] = hexagram(9.0, 1.0, True)
    _cache["hex_chan"] = hexagram(1.7, 1.0, False)

    # emerald tick ring (72 ticks, longer every 6th)
    tk = mk()
    dt = ImageDraw.Draw(tk)
    for i in range(72):
        long_ = (i % 6 == 0)
        r0, r1 = (87.0, 94.0) if long_ else (89.5, 92.5)
        line(dt, pol(r0, i * 5.0), pol(r1, i * 5.0), 1.6 if long_ else 1.1)
    _cache["ticks"] = tk

    # gems on the rim (cardinal = emerald, diagonal = violet)
    gm = mk()
    dgm = ImageDraw.Draw(gm)
    gem_pos = []
    for i in range(8):
        card = (i % 2 == 0)
        x, y = pol(115.0, i * 45.0 - 90.0)
        ell(dgm, x, y, 6.4 if card else 4.6, 255)
        gem_pos.append((x, y, card))
    _cache["gem_bezel"] = gm
    _cache["gem_pos"] = gem_pos

    # book pages (mask) + text lines
    pg = mk()
    dp = ImageDraw.Draw(pg)
    tl = mk()
    dtl = ImageDraw.Draw(tl)

    def page(sign):
        pts = [(128, 134.5), (128 + sign * 12, 129.6), (128 + sign * 24, 127.4), (128 + sign * 35.0, 128.2),
               (128 + sign * 34.2, 144.0), (128 + sign * 22, 145.4), (128 + sign * 10, 149.2), (128, 152.4)]
        dp.polygon([(x * K, y * K) for x, y in pts], fill=255)
        for j in range(4):
            y0 = 132.0 + j * 3.4
            line(dtl, (128 + sign * 5, y0 + 2.2), (128 + sign * 30, y0 - 1.0 + j * 0.3), 0.9, 255)
    page(-1)
    page(1)
    _cache["pages"] = pg
    _cache["page_lines"] = tl


def get(k):
    if not _cache:
        build_static()
    return _cache[k]


def rot(mask, deg):
    return to_arr(mask.rotate(deg, resample=Image.BICUBIC, center=(S * K / 2.0, S * K / 2.0)))


# ---- gold shading ----------------------------------------------------------------------------------------------------
GOLD_STOPS = [(0.0, (42, 24, 6)), (0.28, (118, 72, 14)), (0.5, (206, 146, 38)), (0.72, (250, 205, 90)),
              (0.9, (255, 236, 160)), (1.0, (255, 252, 226))]


def shade_gold(h, t):
    hb = 0.5 * blur_arr(h, 0.9) + 0.5 * blur_arr(h, 2.4)
    gy, gx = np.gradient(hb)
    lit = -(gx * -0.62 + gy * -0.78) * 11.0           # light from the top-left
    tone = 0.52 + 0.44 * np.clip(lit, -1, 1) + 0.18 * hb
    tone += 0.10 * (0.5 - (Y + 128) / 256.0)           # a little brighter toward the top
    tone += 0.06 * np.cos(2 * TH + 0.9)                # soft environment banding
    # sweeping highlight
    tp = (t + 0.2) % 1.0
    u0 = -0.70 + 1.40 * tp
    ph = math.radians(38)
    u = (X * math.cos(ph) + Y * math.sin(ph)) / S
    sw = np.exp(-((u - u0) / 0.055) ** 2) + 0.28 * np.exp(-((u - u0 + 0.075) / 0.11) ** 2)
    tone += 0.55 * sw
    return ramp(np.clip(tone, 0, 1), GOLD_STOPS), sw


# ---- one frame -------------------------------------------------------------------------------------------------------
def draw(t, S_):
    assert S_ == S
    bg = get("bg").copy()
    pulse = 0.5 + 0.5 * math.cos(TAU * 2 * t)           # 2 eased pulses per loop
    pulse1 = 0.5 + 0.5 * math.cos(TAU * t)

    # violet field breathing + emerald under-glow behind the book
    base = bg * (1.0 + 0.10 * pulse1 * np.exp(-(RR / 60.0) ** 2))[..., None]
    base = base * (1.0 - 0.30 * smoothstep(0.50, 0.92, RR / 120.0))[..., None]
    ocx, ocy = 128.0, 112.0
    under = np.exp(-(((xx - ocx) ** 2) + ((yy - 124.0) ** 2) * 1.2) / (2 * 34.0 ** 2)) * (0.16 + 0.06 * pulse)
    base = base + under[..., None] * np.array([0.02, 0.50, 0.34]) * smoothstep(84.0, 70.0, RR)[..., None]

    # --- gold height map -------------------------------------------------------------------------------------
    gs = to_arr(get("gold_static"))
    gr = rot(get("runes"), -120.0 * t)                 # clockwise, 120 deg per loop (3-fold symmetric)
    gh = rot(get("hex"), -60.0 * t)                    # clockwise, 60 deg per loop (6-fold symmetric)
    hchan = rot(get("hex_chan"), -60.0 * t)
    H = np.maximum.reduce([gs, gr * 0.8, gh * 0.95])
    gem_b = to_arr(get("gem_bezel"))
    H = np.maximum(H, gem_b)
    alpha_g = smoothstep(0.10, 0.45, H)

    # drop shadow on the violet field
    sh = np.roll(np.roll(blur_arr(np.clip(H * 1.6, 0, 1), 2.2), 2, axis=0), 1, axis=1)
    base = base * (1 - 0.55 * sh)[..., None]

    gold_rgb, sw = shade_gold(H, t)
    # emerald inlay carved into the hexagram: darken the gold there
    inlay = smoothstep(0.35, 0.8, hchan) * smoothstep(0.3, 0.6, gh)
    gold_rgb = gold_rgb * (1 - 0.88 * inlay[..., None]) + inlay[..., None] * np.array([0.01, 0.10, 0.08])
    out = base * (1 - alpha_g[..., None]) + gold_rgb * alpha_g[..., None]

    # --- book pages ---------------------------------------------------------------------------------------------
    pages = to_arr(get("pages"))
    plines = to_arr(get("page_lines"))
    dist = np.hypot(xx - 128.0, (yy - 112.0) * 0.8)
    lightk = np.exp(-dist / (30.0 + 4 * pulse))
    pcol = ramp(np.clip(lightk * 1.6, 0, 1), [(0.0, (120, 96, 200)), (0.3, (196, 190, 240)), (0.6, (226, 252, 238)),
                                             (1.0, (250, 255, 252))])
    # page curl: darker toward the outer edge + the spine crease
    edge = np.clip(np.abs(xx - 128.0) / 36.0, 0, 1)
    pcol *= (1.0 - 0.22 * edge)[..., None]
    crease = np.exp(-((xx - 128.0) / 1.1) ** 2)
    pcol *= (1.0 - 0.55 * crease)[..., None]
    pa = smoothstep(0.35, 0.65, pages)
    outline = np.clip(blur_arr(pa, 1.6) * 1.8 - pa, 0, 1)
    out = out * (1 - 0.75 * outline[..., None])
    out = out * (1 - pa[..., None]) + pcol * pa[..., None]
    # text lines glow emerald
    tl_a = plines * pa
    out = out * (1 - 0.55 * tl_a[..., None]) + tl_a[..., None] * 0.55 * np.array([0.10, 0.72, 0.52]) * (0.75 + 0.25 * pulse)

    # --- emissive layer (everything that glows) ---------------------------------------------------------------
    E = np.zeros((S, S, 3), np.float32)

    # emerald tick ring, counter-rotating (60 deg per loop = 2 symmetry steps of 30 deg)
    tk = rot(get("ticks"), 60.0 * t)
    tkw = 0.45 + 0.30 * (0.5 + 0.5 * np.cos(TH * 12 - TAU * 1 * t * 0))   # fixed 12-fold shimmer pattern, rotates by itself below
    tkw = 0.40 + 0.35 * (0.5 + 0.5 * np.cos(TH * 6 - TAU * 3 * t))      # a soft wave runs around the ring (3 loops)
    E += (tk * tkw)[..., None] * EMER

    # hexagram emerald inlay: pulses travel around (2 loops)
    chan_i = 0.50 + 0.50 * np.cos(6 * TH - TAU * 2 * t)
    chan_i = chan_i ** 1.6 * 0.6 + 0.12
    E += (hchan * smoothstep(0.3, 0.6, gh))[..., None] * chan_i[..., None] * np.array([0.14, 0.95, 0.60])

    # gems: tips of the hexagram (rotate with it) + rim gems
    for i in range(6):
        a = (-90.0 + 60.0 * i) + 60.0 * t   # screen angle grows clockwise, same as the mask turning clockwise
        gx_, gy_ = pol(77.0, a)
        ph_ = 0.5 + 0.5 * math.cos(TAU * 2 * t + i * 1.0471975)   # each gem breathes, 2 loops
        add_blob(E, gx_, gy_, 4.2, EMER, 0.75 + 0.2 * ph_)
        add_blob(E, gx_, gy_, 2.0, EMER_HI, 0.9)
    for (gx_, gy_, card) in get("gem_pos"):
        if card:
            add_blob(E, gx_, gy_, 3.6, EMER, 0.9)
            add_blob(E, gx_, gy_, 1.7, EMER_HI, 0.9)
        else:
            add_blob(E, gx_, gy_, 2.8, VIOL_L, 0.85)
            add_blob(E, gx_, gy_, 1.3, WHITE * 0.95, 0.8)

    # orb: halo, core, 4-point star (pulses 2x per loop)
    add_blob(E, ocx, ocy, 22.0, EMER, 0.14 + 0.06 * pulse)
    add_blob(E, ocx, ocy, 10.0, EMER, 0.42 + 0.12 * pulse)
    add_blob(E, ocx, ocy, 5.2, EMER_HI, 0.9)
    add_blob(E, ocx, ocy, 3.0, WHITE, 1.0)
    dx, dy = xx - ocx, yy - ocy
    star_len = 24.0 + 5.0 * pulse
    sv = np.exp(-(dx / 1.5) ** 2) * np.exp(-np.abs(dy) / star_len) * (np.abs(dy) < 40)
    sh_ = np.exp(-(dy / 1.5) ** 2) * np.exp(-np.abs(dx) / (star_len * 0.9)) * (np.abs(dx) < 40)
    d2x, d2y = (dx + dy) * 0.7071, (dy - dx) * 0.7071
    sd = (np.exp(-(d2x / 1.2) ** 2) * np.exp(-np.abs(d2y) / (star_len * 0.38)) +
          np.exp(-(d2y / 1.2) ** 2) * np.exp(-np.abs(d2x) / (star_len * 0.38))) * 0.5
    star = (sv + sh_ + sd) * (0.42 + 0.26 * pulse)
    E += star[..., None] * np.array([0.65, 1.0, 0.85])
    # glow rising from the page surface
    add_blob(E, 128.0, 134.0, 11.0, EMER, 0.30 + 0.12 * pulse)

    # --- orbiting spell lights with trails -------------------------------------------------------------------
    def light(posf, col, k_trail=16, dt=0.0105, size=1.0):
        for j in range(k_trail, 0, -1):
            f = 1.0 - j / (k_trail + 1.0)
            px, py = posf(t - j * dt)
            add_blob(E, px, py, (1.1 + 2.2 * f) * size, col, 0.6 * f ** 1.5)
        px, py = posf(t)
        add_blob(E, px, py, 7.5 * size, col, 0.26)
        add_blob(E, px, py, 3.8 * size, col, 0.95)
        add_blob(E, px, py, 2.0 * size, WHITE, 1.0)

    def circ(r, k, a0, cx=C, cy=C):
        return lambda tt: (cx + r * math.cos(TAU * k * tt + a0), cy + r * math.sin(TAU * k * tt + a0))

    def ellip(a, b, tilt, k, a0):
        ct, st_ = math.cos(tilt), math.sin(tilt)

        def f(tt):
            ex, ey = a * math.cos(TAU * k * tt + a0), b * math.sin(TAU * k * tt + a0)
            return C + ex * ct - ey * st_, C + ex * st_ + ey * ct
        return f

    # outer: 3 lights, +1 loop, over the rune band
    light(circ(103.0, 1, math.radians(-135)), EMER)
    light(circ(103.0, 1, math.radians(-15)), GOLD_L)
    light(circ(103.0, 1, math.radians(105)), VIOL_L)
    # middle: 2 lights on a tilted ellipse, -1 loop... plus -2 for variety (second pair counter-rotates faster)
    light(ellip(62.0, 47.0, math.radians(-25), -2, math.radians(30)), GOLD_L * 0.9 + 0.1, size=0.9)
    light(ellip(62.0, 47.0, math.radians(-25), -2, math.radians(210)), EMER, size=0.9)
    # inner: 2 tight lights around the orb, +2 loops
    light(circ(19.0, 2, math.radians(90), ocx, ocy), VIOL_L * 0.8 + 0.2, k_trail=12, dt=0.0075, size=0.75)
    light(circ(19.0, 2, math.radians(270), ocx, ocy), EMER_HI, k_trail=12, dt=0.0075, size=0.75)

    # --- motes rising from the book into the orb (phase based, fade in and out) ----------------------------
    mr = np.random.default_rng(5)
    for i in range(9):
        off = i / 9.0
        ph = (t + off) % 1.0
        x0 = 128.0 + (mr.random() - 0.5) * 50.0
        sway = 3.0 * math.sin(TAU * (ph * 1 + mr.random()))
        px = x0 + (128.0 - x0) * ph * 0.75 + sway * (1 - ph)
        py = 140.0 - 40.0 * ph
        a = math.sin(math.pi * ph) ** 1.4
        col = GOLD_L if i % 3 == 0 else EMER_HI
        add_blob(E, px, py, 3.0, col, 0.30 * a)
        add_blob(E, px, py, 1.2, WHITE, 0.75 * a)

    # --- star-dust drifting over the field -------------------------------------------------------------------
    dr_ = np.random.default_rng(99)
    for i in range(34):
        a0 = dr_.uniform(0, TAU)
        r0 = 12 + 90 * math.sqrt(dr_.random())
        px0, py0 = C + r0 * math.cos(a0), C + r0 * math.sin(a0)
        vang = dr_.uniform(0, TAU)
        sp = dr_.uniform(6, 16)
        off = dr_.random()
        ph = (t + off) % 1.0
        px, py = px0 + sp * math.cos(vang) * (ph - 0.5), py0 + sp * math.sin(vang) * (ph - 0.5)
        if math.hypot(px - C, py - C) > 94 or math.hypot(px - C, py - C) < 10:
            continue
        a = math.sin(math.pi * ph) ** 2 * dr_.uniform(0.4, 0.9)
        col = [VIOL_L, EMER_HI, GOLD_L][i % 3]
        add_blob(E, px, py, 1.7, col, 0.30 * a)
        add_blob(E, px, py, 0.8, WHITE, 0.7 * a)

    # --- bloom + composite -------------------------------------------------------------------------------------
    Ec = np.clip(E, 0, 1.0)
    bloom = Ec + 0.65 * blur_rgb(Ec, 3.0) + 0.40 * blur_rgb(Ec, 8.0) + 0.20 * blur_rgb(Ec, 16.0)
    # sweep also lights the violet bloom slightly; emissives glow over the gold only as much as they are bright
    res = 1.0 - (1.0 - np.clip(out, 0, 1)) * (1.0 - np.clip(bloom, 0, 1.4) * 0.92)

    # soft sweep halo over the whole gold (white-gold bloom), restrained
    halo = blur_arr(alpha_g * np.clip(sw, 0, 1), 4.0)
    res = np.clip(res + halo[..., None] * np.array([0.30, 0.24, 0.10]), 0, 1)

    # final vignette + rim darkening for depth, then the disc
    vig = 1.0 - 0.10 * smoothstep(0.80, 1.0, RR / 120.0)
    res *= vig[..., None]
    rgb = np.clip(res * 255.0, 0, 255).astype(np.uint8)
    img = Image.fromarray(rgb, "RGB").convert("RGBA")
    img.putalpha(disc_mask(S, 0.47))
    return img


if __name__ == "__main__":
    build("solomon", draw, hero_t=0.0)

"""Animated Ultimate skill icon: Hercules, Lord of Might (crimson + bronze + gold + ember orange).

Motif: a raised bronze-and-gold clenched fist inside a laurel wreath, framed by two fluted pillars, in a crimson-black
medallion. Layers (all motion is a whole number of cycles per 3.2 s loop, so the loop is seamless):
  1. golden light rays behind the fist, slowly turning (one ray-pitch per loop, counter-rotating second set)
  2. the power pulse: one beat per loop (fist flash + a shock ring) and a smaller echo beat half a loop later
  3. heat shimmer: a gentle rising sine warp of the background (2 cycles)
  4. rising embers / sparks, staggered phases, 1-2 rises per loop, with short trails
  5. a molten-gold highlight band sweeping across the bronze (fist, pillars, wreath, rim), once per loop
  6. twelve studs on the rim (the twelve labours) lighting up one after another around the circumference

Run:  python3 tools/gen_hercules_icon_anim.py     (writes the game files + previews in /tmp/claude-0/icons/hercules/)
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from icon_anim_lib import *  # noqa: E402,F401,F403
import numpy as np  # noqa: E402
from PIL import Image, ImageDraw, ImageFilter  # noqa: E402

TAU = 2 * math.pi
CX, CY = 127.5, 127.5           # medallion centre
FX, FY = 127.5, 118.0           # fist centre (pulse origin)
R_IN = 102.0                     # inner edge of the rim
R_OUT = 119.5                   # outer edge of the rim
_C = {}


# ------------------------------------------------------------------ small helpers
def sstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def ramp(I, stops):
    xs = [s[0] for s in stops]
    return np.stack([np.interp(I, xs, [s[1][c] for s in stops]) for c in range(3)], -1)


def col(r, g, b):
    return np.array([r, g, b], np.float32) / 255.0


BRONZE = [(0.00, (14, 3, 4)), (0.18, (58, 20, 14)), (0.38, (126, 60, 28)), (0.58, (196, 118, 48)),
          (0.78, (246, 182, 74)), (0.92, (255, 224, 140)), (1.00, (255, 246, 214))]
BRONZE = [(a, tuple(c / 255.0 for c in b)) for a, b in BRONZE]


def peri(t, c):
    """signed periodic distance of t from c in (-0.5, 0.5]"""
    return ((t - c + 0.5) % 1.0) - 0.5


def env_pulse(t):
    d1 = peri(t, 0.30)
    d2 = peri(t, 0.50)
    return math.exp(-(d1 / 0.075) ** 2), 0.42 * math.exp(-(d2 / 0.065) ** 2)


# ------------------------------------------------------------------ static art
def blob(X, Y, cx, cy, rx, ry, h, rot=0.0, pw=2.0, pwx=None):
    dx, dy = X - cx, Y - cy
    if rot:
        c, s = math.cos(rot), math.sin(rot)
        dx, dy = dx * c + dy * s, -dx * s + dy * c
    q = 1 - np.abs(dx / rx) ** (pwx or pw) - np.abs(dy / ry) ** pw
    hh = h * np.sqrt(np.clip(q, 0, None))
    al = np.clip(0.5 + q * min(rx, ry) / 2.0, 0, 1)
    return hh, al


def build_fist(S, X, Y):
    KS = 1.13
    X = 127.5 + (X - 127.5) / KS
    Y = 122.0 + (Y - 122.0) / KS
    xs = [128 - 29.5, 128 - 10.0, 128 + 9.5, 128 + 29.0]
    dyk = [4.0, 0.0, 0.5, 4.5]
    blobs = [(128, 128, 41, 52, 0.45, 0.0, 2.6)]
    for i, x in enumerate(xs):
        blobs.append((x, 95 + dyk[i], 9.8, 11.0, 1.00, 0.0, 2.0))              # knuckle caps
        blobs.append((x, 123 + dyk[i], 9.3, 15.0, 0.85, 0.0, 2.8, 2.0))        # curled finger segments
    blobs += [
        (121, 152, 30, 10.5, 1.10, -0.12, 2.0),     # thumb shaft across the front
        (151, 150, 11.0, 10.5, 1.12, 0.0, 2.0),     # thumb tip
        (94, 154, 12.0, 15.5, 0.80, 0.0, 2.0),      # thumb base
    ]
    H = np.zeros_like(X)
    A = np.zeros_like(X)
    for b in blobs:
        hh, al = blob(X, Y, *b)
        H = np.maximum(H, hh)
        A = np.maximum(A, al)
    # wrist (cylinder) with a gold cuff
    dx = X - 127.5
    cyl = np.sqrt(np.clip(1 - (dx / 23.5) ** 2, 0, None))
    ymask = sstep(146, 156, Y) * (1 - sstep(179.0, 180.2, Y))
    cuff_y = (Y - 163.0) / 15.0
    cuff = np.where((cuff_y > 0) & (cuff_y < 1), 0.16 * np.clip(np.sin(np.pi * np.clip(cuff_y, 0, 1)), 0, 1) ** 0.7, 0.0)
    wrist_h = (0.62 * cyl + cuff) * ymask
    wrist_a = np.clip(0.5 + (23.5 - np.abs(dx)) / 1.6, 0, 1) * np.clip(0.5 + (179.5 - Y) / 1.2, 0, 1) * (Y > 140)
    H = np.maximum(H, wrist_h)
    A = np.maximum(A, wrist_a)
    cuffm = ((Y > 162.5) & (Y < 178.5)).astype(np.float32) * (np.abs(dx) < 23.5)
    # soften the height a little so the normals are smooth, then light it
    Hs = np.array(Image.fromarray((np.clip(H / 1.25, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.9)), np.float32) / 255.0 * 1.25
    ZS = 22.0
    gy, gx = np.gradient(Hs * ZS)
    n = np.stack([-gx, -gy, np.ones_like(gx)], -1)
    n /= np.linalg.norm(n, axis=-1, keepdims=True)
    L = np.array([-0.52, -0.62, 0.60]); L /= np.linalg.norm(L)
    diff = np.clip(n @ L, 0, 1)
    Hh = L + np.array([0, 0, 1.0]); Hh /= np.linalg.norm(Hh)
    spec = np.clip(n @ Hh, 0, 1) ** 38
    spec2 = np.clip(n @ Hh, 0, 1) ** 9
    # occlusion: crevices between the phalanges go dark (height deficit against a blurred copy)
    blur = np.array(Image.fromarray((np.clip(H / 1.25, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3.6)), np.float32) / 255.0 * 1.25
    ao = np.clip(1.0 - 2.0 * np.clip(blur - H, 0, 1), 0.22, 1)
    # rim light from the lower right (forge glow)
    rim = np.clip(1 - n[..., 2], 0, 1) ** 2.2 * np.clip(0.55 + 0.9 * (n[..., 0] * 0.6 + n[..., 1] * 0.8), 0, 1)
    I = (0.12 + 0.86 * diff) * ao
    I = I + 0.10 * cuffm
    edge = np.array(Image.fromarray((np.clip(A, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.6)), np.float32) / 255.0
    outline = 0.40 + 0.60 * sstep(0.60, 0.97, edge)
    I = I * outline
    return dict(A=A.astype(np.float32), I=I.astype(np.float32), spec=(spec * ao).astype(np.float32),
                spec2=(spec2 * ao).astype(np.float32), rim=rim.astype(np.float32), ao=ao.astype(np.float32),
                H=Hs.astype(np.float32))


def build_pillars(S, X, Y):
    """Two fluted columns behind the fist; returns RGB + alpha."""
    rgb = np.zeros((S, S, 3), np.float32)
    a = np.zeros((S, S), np.float32)
    for sgn in (-1, 1):
        xc = 127.5 + sgn * 80.0
        hw = 9.5
        nx = (X - xc) / hw
        inside = np.clip((hw - np.abs(X - xc)) / 1.0 + 0.5, 0, 1)
        prof = np.sqrt(np.clip(1 - nx ** 2, 0, None))
        flute = 0.10 * np.cos(nx * math.pi * 3.0) * prof
        dot = -0.55 * nx + 0.83 * prof + flute
        I = (0.12 + 0.55 * np.clip(dot, 0, 1.2)) * 0.82
        # astragal bands (ring mouldings) near both ends and a capital/base block
        dy = Y - 127.5
        for yb, wb in ((-47, 4.2), (47, 4.2), (-38, 1.6), (38, 1.6)):
            band = np.exp(-((dy - yb) / wb) ** 2)
            I = I + 0.20 * band * (0.5 + prof)
        # darker toward the extremities (occlusion by the rim)
        I = I * (0.55 + 0.45 * (1 - sstep(40, 70, np.abs(dy))))
        c = ramp(np.clip(I, 0, 1), BRONZE)
        rgb = np.where(inside[..., None] > 0, c, rgb)
        a = np.maximum(a, inside)
    return rgb, a


def leaf_poly(cx, cy, ang, length, width, n=9):
    """pointed leaf outline from the base (cx, cy) along ang"""
    pts_a, pts_b = [], []
    ca, sa = math.cos(ang), math.sin(ang)
    for i in range(n + 1):
        u = i / n
        w = width * math.sin(math.pi * (u ** 0.8)) * (1 - 0.25 * u)
        x, y = u * length, w
        pts_a.append((cx + x * ca - y * sa, cy + x * sa + y * ca))
        pts_b.append((cx + x * ca + y * sa, cy + x * sa - y * ca))
    return pts_a, pts_b


def build_wreath(S):
    Q = 2
    W = S * Q
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    C = (127.5, 134.0)
    R = 62.0
    rng = np.random.default_rng(7)
    lightv = np.array([-0.6, -0.8])
    shade_cols = [(0.85, 0.50, 0.20), (0.97, 0.74, 0.30), (1.0, 0.88, 0.50)]
    # stems
    for sgn in (-1, 1):
        pts = []
        for i in range(0, 41):
            u = i / 40
            th = math.radians(90 - 128 * u)
            x = C[0] + sgn * R * math.cos(th)
            y = C[1] + R * math.sin(th)
            pts.append((x * Q, y * Q))
        d.line(pts, fill=(150, 84, 36, 255), width=int(2.6 * Q))
        d.line([(x - 0.5, y - 0.5) for x, y in pts], fill=(236, 170, 70, 255), width=int(1.0 * Q))
    # leaves
    for sgn in (-1, 1):
        for i in range(0, 17):
            u = i / 16
            th = math.radians(90 - 126 * u)
            px = C[0] + sgn * R * math.cos(th)
            py = C[1] + R * math.sin(th)
            # tangent towards the tip of the branch
            tx = sgn * (-math.sin(th)) * (-1)
            ty = math.cos(th) * (-1)
            tx, ty = sgn * math.sin(th), -math.cos(th)
            base_ang = math.atan2(ty, tx)
            L = 19.0 - 7.0 * u
            Wd = 6.6 - 2.0 * u
            for side in (-1, 1):
                ang = base_ang + side * math.radians(46 + rng.uniform(-6, 6))
                pa, pb = leaf_poly(px * Q, py * Q, ang, L * Q, Wd * Q)
                # decide which half is lit by the light direction
                nrm = np.array([-math.sin(ang), math.cos(ang)])
                lit_a = (nrm @ lightv) > 0
                tone = rng.uniform(0, 1)
                base = np.array(shade_cols[1]) * (0.88 + 0.2 * tone)
                hi = np.clip(np.array(shade_cols[2]) * (0.92 + 0.15 * tone), 0, 1)
                lo = np.array(shade_cols[0]) * (0.7 + 0.2 * tone)
                half_lit, half_dark = (pa, pb) if lit_a else (pb, pa)
                mid = [(px * Q + 0 * Q, py * Q)]
                mid_pts = [(px * Q + (L * Q * i / 9) * math.cos(ang), py * Q + (L * Q * i / 9) * math.sin(ang)) for i in range(10)]
                d.polygon(half_dark + list(reversed(mid_pts)), fill=tuple(int(255 * v) for v in lo) + (255,))
                d.polygon(half_lit + list(reversed(mid_pts)), fill=tuple(int(255 * v) for v in hi) + (255,))
                d.line(mid_pts, fill=(110, 52, 22, 255), width=max(1, int(0.8 * Q)))
    # berries / small round fruit near the bottom crossing
    # bow & jewel at the bottom: crossed stems tied with a ruby
    gx, gy = C[0], C[1] + R + 3.0
    gxq, gyq = gx * Q, gy * Q
    d.ellipse([(gxq - 9 * Q), (gyq - 9 * Q), (gxq + 9 * Q), (gyq + 9 * Q)], fill=(120, 58, 24, 255))
    d.ellipse([(gxq - 8 * Q), (gyq - 8 * Q), (gxq + 8 * Q), (gyq + 8 * Q)], fill=(238, 176, 74, 255))
    d.ellipse([(gxq - 6.2 * Q), (gyq - 6.2 * Q), (gxq + 6.2 * Q), (gyq + 6.2 * Q)], fill=(132, 14, 20, 255))
    d.ellipse([(gxq - 4.6 * Q), (gyq - 4.6 * Q), (gxq + 3.2 * Q), (gyq + 3.2 * Q)], fill=(222, 44, 40, 255))
    d.ellipse([(gxq - 3.8 * Q), (gyq - 4.2 * Q), (gxq - 0.6 * Q), (gyq - 1.4 * Q)], fill=(255, 190, 170, 255))
    img = img.resize((S, S), Image.LANCZOS)
    a = np.array(img).astype(np.float32) / 255.0
    return a[..., :3], a[..., 3], (gx, gy)


def build_rim(S, X, Y):
    dx, dy = X - CX, Y - CY
    r = np.hypot(dx, dy)
    ang = np.arctan2(dy, dx)
    v = np.clip((r - R_IN) / (R_OUT - R_IN), 0, 1)
    prof = np.clip(np.sin(np.pi * v ** 0.9), 0, 1) ** 0.55
    slope = np.cos(np.pi * v ** 0.9) * 0.9
    er = np.stack([np.cos(ang), np.sin(ang)], -1)
    nxy = -slope[..., None] * er
    Lxy = np.array([-0.62, -0.78])
    dot = nxy @ Lxy
    I = 0.40 + 0.30 * prof + 0.72 * dot
    chan = np.exp(-(((r - 110.2) / 5.2) ** 4))
    I = I - 0.30 * chan
    # engraved ticks on the inner shoulder + gilded outer line + inner groove
    tick_a = (ang * 72 / TAU)
    tick = np.abs(((tick_a + 0.5) % 1.0) - 0.5)
    big = np.abs(((ang * 12 / TAU + 0.5) % 1.0) - 0.5)
    tickm = (r > R_IN + 1.2) & (r < R_IN + 4.2 + 2.0 * (big < 0.02))
    I = np.where(tickm & (tick < 0.12), I - 0.18, I)
    I = I + 0.22 * np.exp(-((r - (R_OUT - 1.6)) / 0.9) ** 2)
    I = I - 0.35 * np.exp(-((r - (R_IN + 0.6)) / 0.9) ** 2)
    # soft scroll pattern between studs (72 small diamonds on the outer shoulder)
    cell = np.abs(((ang * 24 / TAU + 0.5) % 1.0) - 0.5)
    dia = np.exp(-((cell * 4.4) ** 2 + ((r - (R_OUT - 5.8)) / 1.6) ** 2))
    I = I + 0.18 * dia
    c = ramp(np.clip(I, 0, 1.0), BRONZE)
    c = c * (1 - 0.35 * chan[..., None]) + chan[..., None] * col(70, 6, 14)[None, None, :] * 0.35
    inner = np.clip((r - R_IN) / 1.0 + 0.5, 0, 1)
    outer = np.clip((R_OUT - r) / 1.0 + 0.5, 0, 1)
    a = inner * outer
    return c.astype(np.float32), a.astype(np.float32), I.astype(np.float32)


def init(S):
    if _C.get("S") == S:
        return
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    X, Y = xx, yy
    C = _C
    C["S"] = S
    C["X"], C["Y"] = X, Y
    r = np.hypot(X - CX, Y - CY)
    C["r"] = r
    C["ang"] = np.arctan2(Y - CY, X - CX)
    C["rf"] = np.hypot(X - FX, Y - FY)
    C["interior"] = sstep(R_IN + 1.0, R_IN - 2.0, r).astype(np.float32)  # 1 inside the rim
    # background: crimson-black radial painting with soft low-frequency mottling
    rng = np.random.default_rng(11)
    noise = rng.random((18, 18)).astype(np.float32)
    nz = np.array(Image.fromarray((noise * 255).astype(np.uint8)).resize((S, S), Image.BICUBIC), np.float32) / 255.0
    nz = nz * 2 - 1
    rb = np.hypot(X - FX, Y - (FY + 8))
    g = np.exp(-(rb / 78.0) ** 1.7)
    bg = (col(12, 1, 6)[None, None, :] * (1 - g[..., None]) + col(178, 14, 30)[None, None, :] * g[..., None])
    bg = bg * (1 + 0.16 * nz[..., None])
    # forge glow from below, dark vignette to the edge
    low = np.exp(-(np.hypot((X - CX) / 70.0, (Y - 212) / 40.0)) ** 2)
    bg = bg + low[..., None] * col(120, 34, 10)[None, None, :] * 0.55
    vig = 1.0 - 0.72 * sstep(48, 104, r)
    C["bg"] = (bg * vig[..., None]).astype(np.float32)
    C["vig"] = vig.astype(np.float32)
    # fist
    C["fist"] = build_fist(S, X, Y)
    f = C["fist"]
    halo = np.array(Image.fromarray((f["A"] * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(9)), np.float32) / 255.0
    C["fist_halo"] = halo
    C["fist_shadow"] = np.array(Image.fromarray((f["A"] * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4.5)), np.float32) / 255.0
    # pillars + wreath + rim
    C["pil_rgb"], C["pil_a"] = build_pillars(S, X, Y)
    C["wr_rgb"], C["wr_a"], C["gem"] = build_wreath(S)
    C["rim_rgb"], C["rim_a"], C["rim_I"] = build_rim(S, X, Y)
    # metal coverage for the sweep
    C["metal"] = np.clip(np.maximum.reduce([C["pil_a"] * 0.7, C["wr_a"], C["rim_a"], f["A"]]), 0, 1)
    # shimmer amplitude
    C["shim"] = (1.7 * sstep(R_IN, 40, r) * (0.55 + 0.45 * sstep(190, 110, Y))).astype(np.float32)
    # embers
    er = np.random.default_rng(23)
    n = 46
    C["embers"] = [dict(
        x0=128 + er.normal(0, 34), ys=er.uniform(110, 196), rise=er.uniform(70, 118),
        k=1 if i % 3 else 2, off=er.random(), sway=er.uniform(2.0, 7.0), m=int(er.integers(1, 3)),
        ph=er.random(), size=er.uniform(1.1, 2.3), heat=er.uniform(0.75, 1.0), back=(i % 2 == 0)) for i in range(n)]


def splat(layer, x, y, sigma, color, amp):
    S = layer.shape[0]
    rad = int(math.ceil(sigma * 3)) + 1
    x0, x1 = int(x) - rad, int(x) + rad + 1
    y0, y1 = int(y) - rad, int(y) + rad + 1
    if x1 <= 0 or y1 <= 0 or x0 >= S or y0 >= S:
        return
    x0c, x1c, y0c, y1c = max(x0, 0), min(x1, S), max(y0, 0), min(y1, S)
    gx = np.arange(x0c, x1c, dtype=np.float32) - x
    gy = np.arange(y0c, y1c, dtype=np.float32) - y
    w = np.exp(-(gx[None, :] ** 2 + gy[:, None] ** 2) / (2 * sigma * sigma)) * amp
    layer[y0c:y1c, x0c:x1c] += w[..., None] * color[None, None, :]


def ember_color(p, heat):
    young = col(255, 232, 170)
    mid = col(255, 128, 30)
    old = col(176, 30, 18)
    p = min(max(p, 0.0), 1.0)
    if p < 0.35:
        k = p / 0.35
        return young * (1 - k) + mid * k
    k = (p - 0.35) / 0.65
    return mid * (1 - k) + old * k


def embers_layer(S, t, back):
    lay = np.zeros((S, S, 3), np.float32)
    for e in _C["embers"]:
        if e["back"] != back:
            continue
        for j in range(5):
            p = (t * e["k"] + e["off"]) % 1.0 - 0.014 * j * e["k"]
            if p <= 0.0 or p >= 1.0:
                continue
            env = math.sin(math.pi * p) ** 1.3
            x = e["x0"] + (FX - e["x0"]) * 0.25 * p + e["sway"] * math.sin(TAU * (p * e["m"] + e["ph"]))
            y = e["ys"] - e["rise"] * (p + 0.18 * p * p) / 1.18
            flick = 0.85 + 0.15 * math.sin(TAU * (t * 3 + e["ph"]))
            amp = env * flick * (0.62 ** j) * 1.25
            sig = e["size"] * (1.0 - 0.45 * p) * (0.92 ** j)
            splat(lay, x, y, max(0.7, sig * 1.25), ember_color(p, e["heat"]), amp * (1.25 if j == 0 else 0.8))
    # keep them inside the rim
    return lay * _C["interior"][..., None]


def shimmer(img, t):
    X, Y = _C["X"], _C["Y"]
    S = img.shape[0]
    dx = _C["shim"] * np.sin(TAU * (Y / 21.0 - 2 * t))
    xs = np.clip(X + dx, 0, S - 1.001)
    x0 = np.floor(xs).astype(np.int32)
    fr = (xs - x0)[..., None]
    yi = Y.astype(np.int32)
    return img[yi, x0] * (1 - fr) + img[yi, np.minimum(x0 + 1, S - 1)] * fr


def ring_term(rf, R, w, amp):
    return amp * np.exp(-((rf - R) / w) ** 2)


def draw(t, S):
    init(S)
    X, Y, r, ang, rf = _C["X"], _C["Y"], _C["r"], _C["ang"], _C["rf"]
    interior = _C["interior"]
    e1, e2 = env_pulse(t)
    env = e1 + e2
    gold = col(255, 178, 60)
    orange = col(255, 112, 26)
    white = col(255, 240, 200)

    # ---- 1. background + rays
    img = _C["bg"].copy()
    a_rel = ang - TAU * 0.0
    rays = 0.5 + 0.5 * np.cos(12 * ang - TAU * t)
    rays2 = 0.5 + 0.5 * np.cos(7 * ang + TAU * t + 0.8)
    rr = 0.70 * rays ** 2.6 + 0.30 * rays2 ** 3.0
    rfall = np.exp(-(np.hypot(X - FX, Y - FY) / 120.0) ** 1.6) * (0.35 + 0.65 * sstep(6, 38, np.hypot(X - FX, Y - FY)))
    breathe = 0.80 + 0.20 * math.sin(TAU * t + 0.6)
    img = img + (rr * rfall * breathe * (0.50 + 0.25 * env))[..., None] * col(255, 140, 40)[None, None, :]
    # warm core glow behind the fist
    core = np.exp(-(rf / 54.0) ** 2)
    img = img + core[..., None] * (col(190, 18, 24)[None, None, :] * (0.40 + 0.45 * env))

    # ---- 2. shock rings from the fist
    def ring(birth, life, rmax, amp):
        p = ((t - birth) % 1.0) / life
        if p >= 1.0:
            return 0
        e = 1 - (1 - p) ** 2.6
        R = 36.0 + (rmax - 36.0) * e
        w = 2.6 + 6.0 * p
        fade = sstep(0.0, 0.07, p) * (1 - p) ** 1.5
        return amp * fade * np.exp(-((rf - R) / w) ** 2)
    ringI = ring(0.20, 0.58, 112.0, 1.7) + ring(0.43, 0.44, 86.0, 0.8)
    if not np.isscalar(ringI):
        img = img + ringI[..., None] * (orange[None, None, :] * 0.75 + white[None, None, :] * 0.28)

    # ---- 3. heat shimmer on everything painted so far
    img = shimmer(img, t) * interior[..., None]
    img = img * _C["vig"][..., None] ** 0.5

    def over(base, rgb, a):
        return base * (1 - a[..., None]) + rgb * a[..., None]

    # ---- 4. back embers, pillars, wreath
    img = img + embers_layer(S, t, True)
    pil_rgb = _C["pil_rgb"]
    img = over(img, pil_rgb, _C["pil_a"] * interior)
    img = over(img, _C["wr_rgb"] * 0.66, _C["wr_a"])

    # ---- 5. fist halo + fist
    f = _C["fist"]
    halo = _C["fist_halo"] * (1 - f["A"] * 0.8)
    img = img * (1 - 0.55 * _C["fist_shadow"] * (1 - f["A"]))[..., None]
    img = img + halo[..., None] * (orange[None, None, :] * (0.14 + 0.42 * env))
    I = f["I"] * (1.0 + 0.26 * e1 + 0.12 * e2)
    fc = ramp(np.clip(I, 0, 1.0), BRONZE)
    fc = fc + (f["spec"] * (0.30 + 0.55 * env))[..., None] * white[None, None, :]
    fc = fc + (f["spec2"] * 0.16)[..., None] * gold[None, None, :]
    fc = fc + (f["rim"] * (0.40 + 0.70 * env))[..., None] * orange[None, None, :] * 0.85
    # ember-lit crevices flash with the beat
    fc = fc + ((1 - f["ao"]) * (0.5 * env))[..., None] * orange[None, None, :] * 0.8
    img = over(img, fc, f["A"])

    # ---- 6. rim + studs
    img = over(img, _C["rim_rgb"], _C["rim_a"])

    # ---- 7. molten gold sweep over the bronze
    tt = (t + 0.2) % 1.0
    s = ((X - CX) * math.cos(math.radians(32)) + (Y - CY) * math.sin(math.radians(32)) + 125.0) / 250.0
    s0 = -0.22 + 1.44 * tt
    band = np.exp(-((s - s0) / 0.055) ** 2) + 0.35 * np.exp(-((s - s0 + 0.09) / 0.16) ** 2)
    lum = np.clip(img.mean(-1) * 2.0, 0, 1) ** 1.3
    img = img + (band * _C["metal"] * (1 - 0.7 * f["A"]) * (0.25 + 0.75 * lum) * 0.62)[..., None] * col(255, 190, 80)[None, None, :]

    # ---- 8. front embers
    img = img + embers_layer(S, t, False)

    # ---- 9. gem glint (ruby on the wreath) and the twelve studs
    glow_layer = np.zeros((S, S, 3), np.float32)
    gx, gy = _C["gem"]
    gl = 0.5 + 0.5 * math.sin(TAU * t - 1.0)
    splat(glow_layer, gx - 2.0, gy - 2.0, 3.4, col(255, 60, 50), 0.28 + 0.30 * gl + 0.35 * e1)
    for i in range(12):
        a_i = math.radians(-90 + 30 * i)
        sx = CX + 110.2 * math.cos(a_i)
        sy = CY + 110.2 * math.sin(a_i)
        ph = (t - i / 12.0) % 1.0
        lit = float(sstep(0.0, 0.05, ph) * math.exp(-ph * 4.2))
        # gem body
        d = np.hypot(X[int(sy) - 8:int(sy) + 9, int(sx) - 8:int(sx) + 9] - sx, Y[int(sy) - 8:int(sy) + 9, int(sx) - 8:int(sx) + 9] - sy)
        y0, y1, x0, x1 = int(sy) - 8, int(sy) + 9, int(sx) - 8, int(sx) + 9
        sock = np.clip((6.6 - d) / 1.0 + 0.5, 0, 1)
        gem = np.clip((5.0 - d) / 1.0 + 0.5, 0, 1)
        # shaded gem: dark crimson -> orange/white when lit, highlight top-left
        hl = np.exp(-(((X[y0:y1, x0:x1] - sx + 1.6) ** 2 + (Y[y0:y1, x0:x1] - sy + 1.8) ** 2) / 2.2))
        base = col(86, 8, 14) * (1 - lit) + col(255, 150, 50) * lit
        gcol = base[None, None, :] * (0.55 + 0.6 * (1 - d / 5.0).clip(0, 1)[..., None]) + (hl * (0.5 + 0.5 * lit))[..., None] * white[None, None, :]
        sc = col(28, 6, 6)
        patch = img[y0:y1, x0:x1]
        patch[:] = patch * (1 - sock[..., None]) + sc[None, None, :] * sock[..., None]
        patch[:] = patch * (1 - gem[..., None]) + gcol * gem[..., None]
        splat(glow_layer, sx, sy, 7.5, col(255, 150, 50), 0.62 * lit)
        splat(glow_layer, sx, sy, 3.0, white, 0.55 * lit)
    img = img + glow_layer

    # ---- 10. tone map (soft shoulder, white-hot desaturation), alpha
    x = np.clip(img, 0, None)
    lumx = x.max(-1, keepdims=True)
    hot = np.clip(lumx - 0.92, 0, None)
    x = x + hot * 0.55
    x = np.where(x < 0.8, x, 0.8 + 0.2 * np.tanh((x - 0.8) / 0.2))
    alpha = np.array(disc_mask(S, 0.47), np.float32) / 255.0
    out = np.concatenate([np.clip(x, 0, 1), alpha[..., None]], -1)
    return Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")


if __name__ == "__main__":
    build("hercules", draw, hero_t=10 / 32)

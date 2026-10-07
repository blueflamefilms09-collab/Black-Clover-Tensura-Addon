"""Generates the Chain Magic VFX textures (deterministic, Pillow + numpy, no external images).

    python3 -B tools/gen_chain_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/chain_*.png

Look reference: docs/attributes/art_reference/pack_chain.webp : long bright steel chains (every link catching a highlight) that whip and
coil round a white burst of light with grey smoke inside it; each chain ends in a flat grey-blue kunai / spear blade. So:
  chain_strip   64x256   four interlocked links (two face-on ovals, two edge-on bars), tiles vertically (V repeats every 4 links)
  chain_band    256x64   the same links lying horizontally, tiles along U (ring bands, ground rims)
  chain_blade   128x256  a kunai blade pointing up (tip on top), faceted, with a socket and an eyelet ring at the bottom
  chain_flash   256      white burst: hot core, soft halo, radial rays, a faint ring, dark smoke swirls inside
  chain_smoke   128      grey billowing smoke puff (ALPHA)
  chain_sigil   256      ground sigil: chain-link rim ring, tick ring, hexagram drawn in tiny links
  chain_seal    256      inner counter-rotating seal: eight blade points aimed at a keyhole, thin rings
  chain_cuff    128      a steel manacle ring with a hinge and a rivetted lock plate
  chain_shard   64       a jagged splinter of steel (faceted)
  chain_spark   64       a bright cross-shaped metal spark with a long horizontal streak
  chain_shock   128      a crisp shock ring with tick marks (ADD)
All are white / grey with alpha so the vertex colour tints them.
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


# ------------------------------------------------------------------ helpers
def vnoise(w, h, cell, seed, tile=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(w // cell)), max(1, int(h // cell))
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, base, seed, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += amp * vnoise(w, h, max(1, base // (2 ** o)), seed + o * 17)
        tot += amp
        amp *= 0.5
    return out / tot


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


L = np.array([-0.45, -0.55, 0.70], np.float32)
L /= np.linalg.norm(L)
HV = L + np.array([0, 0, 1], np.float32)
HV /= np.linalg.norm(HV)


def steel(nx, ny, nz):
    """Polished steel: key light from the upper left, a hard specular, banded environment reflection, dark rim."""
    diff = np.clip(nx * L[0] + ny * L[1] + nz * L[2], 0, 1)
    spec = np.clip(nx * HV[0] + ny * HV[1] + nz * HV[2], 0, 1) ** 36
    env = 0.5 + 0.5 * np.sin(ny * 3.4 + nx * 1.1 + 0.7)
    low = np.clip(ny, 0, 1) ** 2 * 0.35
    v = 0.16 + 0.42 * diff + 0.26 * env * nz + 0.18 * low + 1.15 * spec
    rim = (1 - nz) ** 3
    v = v * (1 - 0.5 * rim)
    back = np.clip(-(nx * L[0] + ny * L[1]), 0, 1) ** 2 * (1 - nz) * 0.5
    return np.clip(v + back, 0, 1)


class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.P = np.zeros((h * SS, w * SS), np.float32)
        self.A = np.zeros((h * SS, w * SS), np.float32)

    def window(self, x0, y0, x1, y1):
        x0, y0 = int(math.floor(x0)), int(math.floor(y0))
        x1, y1 = int(math.ceil(x1)), int(math.ceil(y1))
        cx0, cy0, cx1, cy1 = max(0, x0), max(0, y0), min(self.w, x1), min(self.h, y1)
        if cx1 <= cx0 or cy1 <= cy0:
            return None
        xs = (np.arange(cx0 * SS, cx1 * SS) + 0.5) / SS
        ys = (np.arange(cy0 * SS, cy1 * SS) + 0.5) / SS
        X, Y = np.meshgrid(xs, ys)
        return cx0, cy0, cx1, cy1, X, Y

    def over(self, win, shade, alpha):
        cx0, cy0, cx1, cy1 = win[:4]
        sl = (slice(cy0 * SS, cy1 * SS), slice(cx0 * SS, cx1 * SS))
        alpha = alpha.astype(np.float32)
        self.P[sl] = shade * alpha + self.P[sl] * (1 - alpha)
        self.A[sl] = alpha + self.A[sl] * (1 - alpha)

    def darken(self, win, mask, strength):
        cx0, cy0, cx1, cy1 = win[:4]
        sl = (slice(cy0 * SS, cy1 * SS), slice(cx0 * SS, cx1 * SS))
        self.P[sl] *= 1 - strength * mask * (self.A[sl] > 0)

    def save(self, name, tint_alpha_only=False):
        a = self.A.reshape(self.h, SS, self.w, SS).mean((1, 3))
        p = self.P.reshape(self.h, SS, self.w, SS).mean((1, 3))
        sh = np.where(a > 1e-4, p / np.maximum(a, 1e-4), 0)
        rgba = np.dstack([sh, sh, sh, a])
        im = Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")
        os.makedirs(OUT, exist_ok=True)
        im.save(os.path.join(OUT, name + ".png"), optimize=True)
        print("wrote", name, im.size)


def face_link(cv, cx, cy, ang, a, b, r, grain=0.0, seed=1):
    """Oval link seen face-on. Centreline half-width a, half-length b (along the local y axis, turned by ang), tube radius r."""
    m = max(a, b) + r + 2
    win = cv.window(cx - m, cy - m, cx + m, cy + m)
    if win is None:
        return
    X, Y = win[4], win[5]
    c, s = math.cos(ang), math.sin(ang)
    dx, dy = X - cx, Y - cy
    lx, ly = dx * c + dy * s, -dx * s + dy * c
    rho = np.sqrt((lx / a) ** 2 + (ly / b) ** 2) + 1e-6
    gx, gy = lx / (a * a * rho), ly / (b * b * rho)
    gl = np.sqrt(gx * gx + gy * gy) + 1e-6
    d = (rho - 1) / gl
    nxl, nyl = gx / gl, gy / gl
    u = np.clip(d / r, -1, 1)
    nz = np.sqrt(np.clip(1 - u * u, 0, 1))
    nxl, nyl = nxl * u, nyl * u
    nxw, nyw = nxl * c - nyl * s, nxl * s + nyl * c
    v = steel(nxw, nyw, nz)
    v = v * np.where(u < 0, 0.9, 1.0)
    if grain:
        v = v * (1 + grain * (np.sin(lx * 9.0 + ly * 0.3 + seed) * 0.5))
    al = np.clip((r - np.abs(d)) * SS * 0.5, 0, 1)
    cv.over(win, v, al)


def edge_link(cv, cx, cy, ang, half_len, r):
    """The same link seen edge-on: a capsule of radius r."""
    m = half_len + r + 2
    win = cv.window(cx - m, cy - m, cx + m, cy + m)
    if win is None:
        return
    X, Y = win[4], win[5]
    c, s = math.cos(ang), math.sin(ang)
    dx, dy = X - cx, Y - cy
    lx, ly = dx * c + dy * s, -dx * s + dy * c
    hl = max(0.0, half_len - r)
    qy = np.sign(ly) * np.clip(np.abs(ly) - hl, 0, None)
    d = np.sqrt(lx * lx + qy * qy)
    nxl, nyl = np.clip(lx / r, -1, 1), np.clip(qy / r, -1, 1)
    q2 = np.clip(nxl * nxl + nyl * nyl, 0, 1)
    nz = np.sqrt(1 - q2)
    nxw, nyw = nxl * c - nyl * s, nxl * s + nyl * c
    v = steel(nxw, nyw, nz)
    al = np.clip((r - d) * SS * 0.5, 0, 1)
    # soft contact shadow on whatever lies below it
    cv.darken(win, np.clip(1 - np.maximum(d - r, 0) / (r * 0.9), 0, 1) * 0.55, 1.0)
    cv.over(win, v, al)


def line(cv, p0, p1, w, val=1.0, alpha=1.0, soft=0.6):
    x0, y0 = p0
    x1, y1 = p1
    m = w + 3
    win = cv.window(min(x0, x1) - m, min(y0, y1) - m, max(x0, x1) + m, max(y0, y1) + m)
    if win is None:
        return
    X, Y = win[4], win[5]
    vx, vy = x1 - x0, y1 - y0
    ln2 = vx * vx + vy * vy + 1e-9
    t = np.clip(((X - x0) * vx + (Y - y0) * vy) / ln2, 0, 1)
    d = np.hypot(X - (x0 + t * vx), Y - (y0 + t * vy))
    al = np.clip((w * 0.5 - d) / max(soft / SS * 2, 1e-3) + 0.5, 0, 1) * alpha
    cv.over(win, val, al)


def circle(cv, cx, cy, r, w, val=1.0, alpha=1.0):
    win = cv.window(cx - r - w - 2, cy - r - w - 2, cx + r + w + 2, cy + r + w + 2)
    if win is None:
        return
    X, Y = win[4], win[5]
    d = np.abs(np.hypot(X - cx, Y - cy) - r)
    cv.over(win, val, np.clip((w * 0.5 - d) * SS * 0.6 + 0.5, 0, 1) * alpha)


def poly(cv, pts, val, alpha=1.0, edge=None):
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    win = cv.window(min(xs) - 2, min(ys) - 2, max(xs) + 2, max(ys) + 2)
    if win is None:
        return
    X, Y = win[4], win[5]
    inside = np.ones(X.shape, bool)
    n = len(pts)
    sgn = 1 if sum((pts[(i + 1) % n][0] - pts[i][0]) * (pts[(i + 1) % n][1] + pts[i][1]) for i in range(n)) < 0 else -1
    for i in range(n):
        ax, ay = pts[i]
        bx, by = pts[(i + 1) % n]
        cr = (bx - ax) * (Y - ay) - (by - ay) * (X - ax)
        inside &= (cr * sgn >= 0)
    cv.over(win, val, inside.astype(np.float32) * alpha)


# ------------------------------------------------------------------ chain strip / band
def make_strip():
    W, H = 64, 256
    cv = Canvas(W, H)
    pitch = 64
    a, b, r = 18.5, 34.0, 7.0
    # faces first, edge-on bars after so that they sit in front (the faces pass through them)
    for k in range(4):
        cy = 32 + pitch * k
        if k % 2 == 0:
            for off in (-H, 0, H):
                face_link(cv, 32, cy + off, 0.0, a, b, r, grain=0.05, seed=k)
    for k in range(4):
        cy = 32 + pitch * k
        if k % 2 == 1:
            for off in (-H, 0, H):
                edge_link(cv, 32, cy + off, 0.0, b + r, r * 0.98)
    return cv


def tex_strip():
    make_strip().save("chain_strip")


def tex_band():
    cv = make_strip()
    a = cv.A.reshape(cv.h * SS, cv.w * SS)
    p = cv.P
    # lay the strip down along U; keep the light on the upper side
    ra, rp = np.rot90(a, -1).copy(), np.rot90(p, -1).copy()
    ra, rp = ra[::-1, :], rp[::-1, :]
    out = Canvas(256, 64)
    out.A[:] = ra
    out.P[:] = rp
    out.save("chain_band")


# ------------------------------------------------------------------ kunai blade
def tex_blade():
    W, H = 128, 256
    cv = Canvas(W, H)
    cx = 64.0
    win = cv.window(0, 0, W, H)
    X, Y = win[4], win[5]
    # blade profile: pointed leaf from y=4 (tip) to the shoulder at y=150
    t = np.clip((Y - 4) / 146.0, 0, 1)
    hw = 40 * np.sin(t * math.pi * 0.5) ** 0.85
    hw = np.where(Y > 150, 40 - (Y - 150) * 0.55, hw)
    inb = (Y >= 4) & (Y <= 174) & (np.abs(X - cx) < hw)
    tt = (X - cx) / np.maximum(hw, 1e-3)
    sgn = np.sign(tt)
    at = np.abs(tt)
    # three facets per side: flat bevel, a fuller line, the cutting edge bevel
    nx = np.where(at < 0.14, sgn * 0.15, np.where(at < 0.72, sgn * 0.62, sgn * 0.95))
    nz = np.sqrt(np.clip(1 - nx * nx, 0.02, 1))
    ny = np.full_like(nx, -0.15)
    ny = np.where(Y > 150, 0.0, ny)
    v = steel(nx, ny, nz)
    ridge = np.exp(-(((X - cx)) / 0.8) ** 2) * 0.35
    v = np.clip(v + ridge, 0, 1)
    # the fuller groove: a darker long slot inside the bevel
    groove = (np.abs(at - 0.28) < 0.05) & (Y > 40) & (Y < 140)
    v = np.where(groove, v * 0.7, v)
    edge = np.clip((hw - np.abs(X - cx)) * SS * 0.5, 0, 1)
    v = v * (0.9 + 0.1 * np.clip((hw - np.abs(X - cx)) / 5, 0, 1))
    cv.over(win, v, inb.astype(np.float32) * edge)
    # socket: a short cylinder with two bands
    cw = 12.5
    win = cv.window(cx - 20, 148, cx + 20, 200)
    X, Y = win[4], win[5]
    u = np.clip((X - cx) / cw, -1, 1)
    nzs = np.sqrt(1 - u * u)
    vs = steel(u, np.zeros_like(u) - 0.1, nzs)
    band = ((Y > 156) & (Y < 160)) | ((Y > 182) & (Y < 185))
    vs = np.where(band, vs * 0.55, vs)
    ins = (np.abs(X - cx) < cw) & (Y > 150) & (Y < 192)
    cv.over(win, vs, ins.astype(np.float32) * np.clip((cw - np.abs(X - cx)) * SS * 0.5, 0, 1))
    # eyelet ring for the chain
    face_link(cv, cx, 214, 0.0, 19.0, 19.0, 5.5)
    cv.save("chain_blade")


# ------------------------------------------------------------------ flash
def tex_flash():
    N = 256
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    dx, dy = (xx + 0.5 - N / 2) / (N / 2), (yy + 0.5 - N / 2) / (N / 2)
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    n1 = fbm(N, N, 32, 5, 4)
    n2 = fbm(N, N, 16, 9, 3)
    core = np.exp(-(r / 0.13) ** 2)
    halo = np.exp(-r * 3.3) * 0.75
    wob = (n1 - 0.5) * 1.2
    rays = (0.5 + 0.5 * np.cos(th * 9 + wob * 2.0)) ** 5 * np.exp(-r * 2.2) * smooth(0.03, 0.22, r) * 0.8
    ring = np.exp(-((r - 0.6) / 0.05) ** 2) * 0.12
    v = core + halo + rays + ring
    # smoke swirls in the burst: grey billows pulled out of the mid area
    smoke = smooth(0.45, 0.7, fbm(N, N, 40, 31, 4)) * smooth(0.12, 0.4, r) * (1 - smooth(0.6, 0.95, r))
    v = v * (1 - 0.5 * smoke)
    v = v * (1 - smooth(0.72, 1.0, r))
    v = np.clip(v, 0, 1)
    sh = np.clip(0.82 + 0.18 * v, 0, 1)
    rgba = np.dstack([sh, sh, sh, v])
    Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "chain_flash.png"), optimize=True)
    print("wrote chain_flash (256, 256)")


def tex_smoke():
    N = 128
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    dx, dy = (xx + 0.5 - N / 2) / (N / 2), (yy + 0.5 - N / 2) / (N / 2)
    r = np.sqrt(dx * dx + dy * dy)
    n = fbm(N, N, 24, 77, 4)
    n2 = fbm(N, N, 12, 91, 3)
    body = smooth(0.95, 0.35, r + (n - 0.5) * 0.9)
    shade = np.clip(0.35 + 0.55 * n2 + 0.2 * (-dy * 0.5 + 0.5) * body, 0, 1)
    a = np.clip(body * (0.55 + 0.6 * n) * (1 - smooth(0.8, 1.0, r)), 0, 1)
    rgba = np.dstack([shade, shade, shade, a])
    Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "chain_smoke.png"), optimize=True)
    print("wrote chain_smoke (128, 128)")


# ------------------------------------------------------------------ sigil / seal
def links_on_line(cv, p0, p1, step, a, b, r):
    x0, y0 = p0
    x1, y1 = p1
    ln = math.hypot(x1 - x0, y1 - y0)
    ang = math.atan2(y1 - y0, x1 - x0) - math.pi / 2
    n = max(1, int(ln // step))
    for i in range(n):
        t = (i + 0.5) / n
        cx, cy = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        if i % 2 == 0:
            face_link(cv, cx, cy, ang, a, b, r)
        else:
            edge_link(cv, cx, cy, ang, b + r - 1.5, r * 0.95)


def tex_sigil():
    N = 256
    cv = Canvas(N, N)
    c = N / 2
    circle(cv, c, c, 124.5, 1.6, 0.85)
    circle(cv, c, c, 119.0, 1.0, 0.7)
    # chain rim: links round the circle
    nl = 30
    R = 110.0
    for i in range(nl):
        th = 2 * math.pi * i / nl
        cx, cy = c + R * math.cos(th), c + R * math.sin(th)
        ang = th  # local y axis along the tangent: ang = th puts +y at tangent direction
        if i % 2 == 0:
            face_link(cv, cx, cy, ang, 4.4, 9.2, 2.1)
        else:
            edge_link(cv, cx, cy, ang, 10.5, 2.0)
    circle(cv, c, c, 101.0, 1.0, 0.8)
    for i in range(72):
        th = 2 * math.pi * i / 72
        r0, r1 = (96, 100) if i % 6 else (92, 100)
        line(cv, (c + r0 * math.cos(th), c + r0 * math.sin(th)), (c + r1 * math.cos(th), c + r1 * math.sin(th)), 1.0 if i % 6 else 1.8, 0.9)
    circle(cv, c, c, 90.0, 1.2, 0.8)
    # hexagram out of tiny links
    rr = 90.0
    pts = [(c + rr * math.cos(math.radians(90 + 60 * k)), c - rr * math.sin(math.radians(90 + 60 * k))) for k in range(6)]
    for k in range(6):
        links_on_line(cv, pts[k], pts[(k + 2) % 6], 11.0, 2.6, 6.2, 1.35)
    circle(cv, c, c, 46.0, 1.4, 0.85)
    circle(cv, c, c, 40.0, 0.8, 0.6)
    cv.save("chain_sigil")


def tex_seal():
    N = 256
    cv = Canvas(N, N)
    c = N / 2
    circle(cv, c, c, 122.0, 1.8, 0.9)
    circle(cv, c, c, 104.0, 1.2, 0.75)
    circle(cv, c, c, 56.0, 1.6, 0.9)
    # eight blade points aimed at the centre
    for i in range(8):
        th = math.radians(45 * i)
        ux, uy = math.cos(th), math.sin(th)
        px, py = -uy, ux
        tip = (c + ux * 62, c + uy * 62)
        base_c = (c + ux * 100, c + uy * 100)
        left = (base_c[0] + px * 9, base_c[1] + py * 9)
        right = (base_c[0] - px * 9, base_c[1] - py * 9)
        shoulder_l = (c + ux * 84 + px * 11, c + uy * 84 + py * 11)
        shoulder_r = (c + ux * 84 - px * 11, c + uy * 84 - py * 11)
        poly(cv, [tip, shoulder_r, right, left, shoulder_l], 0.62)
        # lit half
        poly(cv, [tip, shoulder_l, left, (base_c[0], base_c[1])], 1.0, 0.9)
        line(cv, tip, base_c, 1.0, 0.35, 0.8)
    for i in range(8):
        th = math.radians(45 * i + 22.5)
        line(cv, (c + 106 * math.cos(th), c + 106 * math.sin(th)), (c + 118 * math.cos(th), c + 118 * math.sin(th)), 2.2, 0.95)
    # keyhole
    circle(cv, c, c, 30.0, 1.0, 0.7)
    win = cv.window(c - 28, c - 28, c + 28, c + 28)
    X, Y = win[4], win[5]
    disk = np.hypot(X - c, Y - (c - 6)) < 9.0
    slot = (np.abs(X - c) < 4.5 + (Y - (c - 2)) * 0.06) & (Y > c - 4) & (Y < c + 20)
    cv.over(win, np.full(X.shape, 1.0, np.float32), (disk | slot).astype(np.float32))
    cv.save("chain_seal")


# ------------------------------------------------------------------ cuff, shard, spark, shock
def rrect(cv, cx, cy, hw, hh, rad, bevel, rivets=()):
    win = cv.window(cx - hw - 2, cy - hh - 2, cx + hw + 2, cy + hh + 2)
    X, Y = win[4], win[5]
    px, py = X - cx, Y - cy
    qx, qy = np.abs(px) - (hw - rad), np.abs(py) - (hh - rad)
    ox, oy = np.clip(qx, 0, None), np.clip(qy, 0, None)
    out = np.hypot(ox, oy)
    d = out + np.minimum(np.maximum(qx, qy), 0) - rad
    inside = d < 0
    ln = out + 1e-6
    nxo, nyo = np.where(out > 1e-4, np.sign(px) * ox / ln, 0), np.where(out > 1e-4, np.sign(py) * oy / ln, 0)
    axis_x = qx > qy
    nxi = np.where(axis_x, np.sign(px), 0.0)
    nyi = np.where(axis_x, 0.0, np.sign(py))
    nx2 = np.where(out > 1e-4, nxo, nxi)
    ny2 = np.where(out > 1e-4, nyo, nyi)
    tilt = np.clip(1 + d / bevel, 0, 1)
    nx, ny = nx2 * tilt * 0.95, ny2 * tilt * 0.95
    nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0.02, 1))
    v = steel(nx, ny, nz)
    al = np.clip(-d * SS * 0.5, 0, 1)
    cv.over(win, v, al)
    for (rx, ry) in rivets:
        face_link(cv, cx + rx, cy + ry, 0.0, 0.01, 0.01, 2.4)


def tex_cuff():
    N = 128
    cv = Canvas(N, N)
    c = N / 2
    face_link(cv, c, c, 0.0, 46.0, 46.0, 10.0, grain=0.04, seed=3)
    # inner groove
    circle(cv, c, c, 46.0, 1.0, 0.25, 0.5)
    rrect(cv, c, c + 49, 21, 11, 4.5, 6.0, rivets=((-12, 0), (12, 0)))
    rrect(cv, c, c - 49, 18, 11, 4.5, 6.0, rivets=((-11, 0), (11, 0)))
    win = cv.window(c - 6, c - 58, c + 6, c - 40)
    X, Y = win[4], win[5]
    cv.over(win, np.full(X.shape, 0.08, np.float32), ((np.hypot(X - c, Y - (c - 51)) < 2.6) | ((np.abs(X - c) < 1.1) & (Y > c - 51) & (Y < c - 46))).astype(np.float32))
    cv.save("chain_cuff")


def tex_shard():
    N = 64
    cv = Canvas(N, N)
    rng = np.random.default_rng(404)
    pts = [(8, 36), (24, 6), (36, 20), (56, 14), (46, 38), (30, 58), (14, 50)]
    cx = sum(p[0] for p in pts) / len(pts)
    cy = sum(p[1] for p in pts) / len(pts)
    n = len(pts)
    for i in range(n):
        tri = [(cx, cy), pts[i], pts[(i + 1) % n]]
        t = rng.uniform(-0.8, 0.8, 2)
        nz = math.sqrt(max(0.05, 1 - t[0] ** 2 * 0.5 - t[1] ** 2 * 0.5))
        val = float(steel(np.float32(t[0] * 0.7), np.float32(t[1] * 0.7), np.float32(nz)))
        poly(cv, tri, 0.25 + 0.75 * val)
    for i in range(n):
        line(cv, pts[i], pts[(i + 1) % n], 1.0, 0.95, 0.9)
    cv.save("chain_shard")


def tex_spark():
    N = 64
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    dx, dy = (xx + 0.5 - N / 2) / (N / 2), (yy + 0.5 - N / 2) / (N / 2)
    r = np.sqrt(dx * dx + dy * dy)
    core = np.exp(-(r / 0.12) ** 2)
    h = np.exp(-(dy / 0.05) ** 2) * np.exp(-np.abs(dx) * 2.4)
    v = np.exp(-(dx / 0.045) ** 2) * np.exp(-np.abs(dy) * 3.6) * 0.8
    dg1 = np.exp(-(((dx + dy) * 0.7071) / 0.04) ** 2) * np.exp(-r * 4.0) * 0.5
    dg2 = np.exp(-(((dx - dy) * 0.7071) / 0.04) ** 2) * np.exp(-r * 4.0) * 0.5
    halo = np.exp(-r * 4.5) * 0.35
    a = np.clip(core + h + v + dg1 + dg2 + halo, 0, 1) * (1 - smooth(0.8, 1.0, r))
    rgba = np.dstack([np.ones_like(a), np.ones_like(a), np.ones_like(a), a])
    Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "chain_spark.png"), optimize=True)
    print("wrote chain_spark (64, 64)")


def tex_shock():
    N = 128
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    dx, dy = (xx + 0.5 - N / 2) / (N / 2), (yy + 0.5 - N / 2) / (N / 2)
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    n = fbm(N, N, 16, 12, 3)
    edge = np.exp(-((r - 0.82) / 0.028) ** 2)
    glow = np.exp(-((r - 0.82) / 0.12) ** 2) * 0.45
    inner = smooth(0.2, 0.82, r) * smooth(0.9, 0.8, r) * 0.18
    ticks = (0.5 + 0.5 * np.cos(th * 36)) ** 10 * np.exp(-((r - 0.72) / 0.05) ** 2) * 0.7
    a = np.clip((edge * (0.75 + 0.5 * n) + glow + inner + ticks), 0, 1) * (1 - smooth(0.9, 1.0, r))
    rgba = np.dstack([np.ones_like(a), np.ones_like(a), np.ones_like(a), a])
    Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "chain_shock.png"), optimize=True)
    print("wrote chain_shock (128, 128)")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex_strip()
    tex_band()
    tex_blade()
    tex_flash()
    tex_smoke()
    tex_sigil()
    tex_seal()
    tex_cuff()
    tex_shard()
    tex_spark()
    tex_shock()

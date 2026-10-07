"""Generates the Cherry Blossom Magic VFX textures (deterministic, no fonts, no external images).

    python3 -B tools/gen_cherry_blossom_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/cherry_blossom_*.png  (only these files are written)

Look reference (owner art): the Black Clover Sakura caster stills. A sea of soft pink cumulus made of petals, cel-shaded
(light top-left, a darker mauve underside, white speckles), solid pale-pink petals with a notched tip and fine veins, thin
clean linework, pink / white / mauve only.

Textures (white / grey with alpha so the vertex colour tints them; the petals carry their own shading):
  petal      128   one sakura petal, notched tip, veins, light tip / darker base
  flower     128   a five-petal blossom seen from above with stamens
  cloud      256   a cel-shaded cumulus of petals (the sea of blossoms)
  sigil      256   ground seal: ring of ten petals, inner blossom, double rim, dots
  burst      256   radial spray of petals with a bright heart
  ribbon     128x256  a stream of petals: soft core, wave edge, tiny petals (beam texture, V along the length)
  blade      128   a razor petal (the sharp, thin petals that cut), pointing up
  motes      64    a few drifting petals and a sparkle
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(arr_v, arr_a, name):
    """arr_v: value 0..255 (grey), arr_a: alpha 0..1, both 2D at final size."""
    v = np.clip(arr_v, 0, 255).astype(np.uint8)
    a = np.clip(arr_a * 255.0, 0, 255).astype(np.uint8)
    im = Image.fromarray(np.dstack([v, v, v, a]), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)
    return im


def down(arr, w, h):
    im = Image.fromarray(np.clip(arr * 255, 0, 255).astype(np.uint8), "L").resize((w, h), Image.LANCZOS)
    return np.asarray(im, dtype=np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def noise(w, h, cell, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
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
    out, amp, tot = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octaves):
        out += amp * noise(w, h, max(1, base // (2 ** o)), seed + o * 17)
        tot += amp
        amp *= 0.5
    return out / tot


# ---------------------------------------------------------------- petal geometry
def petal_width(y):
    y = np.clip(y, 0, 1)
    return 0.62 * np.sqrt(y) * np.sqrt(np.clip(1 - y ** 3.2, 0, 1))


def petal_mask(x, y, narrow=1.0, notch=0.15):
    """x in [-1,1] across, y in [0,1] base -> tip. Hard mask (supersampled by the caller)."""
    w = petal_width(y) * narrow
    cut = 1 - notch * np.exp(-(x / 0.2) ** 2)
    return ((np.abs(x) < w) & (y >= 0) & (y < cut)).astype(np.float32)


def petal_shade(x, y, mask):
    """Grey value of a petal: darker mauve base, pale tip, rim light, fine veins."""
    v = 150 + 95 * np.clip(y, 0, 1) ** 1.4
    ang = np.arctan2(x, np.maximum(y, 1e-3))
    vein = np.zeros_like(x)
    for a0 in (-0.16, 0.0, 0.16):
        vein = np.maximum(vein, np.exp(-((ang - a0) / 0.018) ** 2))
    v = v * (1 - 0.22 * vein * smooth(0.05, 0.2, y) * (1 - smooth(0.55, 0.9, y)))
    w = np.maximum(petal_width(y), 1e-3)
    rim = smooth(0.78, 1.0, np.abs(x) / w)
    v = v + 40 * rim * smooth(0.2, 0.6, y)
    return v


def make_petal(n=128, narrow=1.0, notch=0.15, pad=0.04):
    s = n * SS
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x = (xx / (s - 1) * 2 - 1) * 0.78
    y = 1 - (yy / (s - 1)) * (1 + 2 * pad) + pad
    m = petal_mask(x, y, narrow, notch)
    v = petal_shade(x, y, m)
    a = down(m, n, n)
    vv = down(v / 255.0 * m, n, n) / np.maximum(a, 1e-3) * 255
    return np.where(a > 0.01, vv, 0), a


def petal_img(n, narrow=1.0, notch=0.15):
    v, a = make_petal(n, narrow, notch)
    g = np.clip(v, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([g, g, g, np.clip(a * 255, 0, 255).astype(np.uint8)]), "RGBA")


def paste(canvas, im, cx, cy, size, angle, alpha=1.0):
    p = im.resize((max(2, int(size)), max(2, int(size))), Image.LANCZOS).rotate(angle, resample=Image.BICUBIC, expand=True)
    if alpha < 1:
        r, g, b, a = p.split()
        p = Image.merge("RGBA", (r, g, b, a.point(lambda q: int(q * alpha))))
    canvas.alpha_composite(p, (int(cx - p.width / 2), int(cy - p.height / 2)))


def save_img(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


# ---------------------------------------------------------------- textures
def tex_petal():
    v, a = make_petal(128)
    save(v, a, "cherry_blossom_petal")


def tex_flower():
    n = 128
    s = n * SS
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    cx = (xx - s / 2) / (s / 2)
    cy = -(yy - s / 2) / (s / 2)
    A = np.zeros((s, s), np.float32)
    V = np.zeros((s, s), np.float32)
    for k in range(5):
        a0 = k * 2 * math.pi / 5 + math.pi / 2
        c, sn = math.cos(a0), math.sin(a0)
        # local petal frame: y along the petal direction, from the centre outward
        ly = (cx * c + cy * sn) / 0.94
        lx = (-cx * sn + cy * c) / 0.94 * 0.62 * 1.55 * 0.78
        ly2 = (ly - 0.04) / 0.96
        m = petal_mask(lx, ly2, 1.0, 0.16)
        v = petal_shade(lx, ly2, m)
        # draw order: later petals tuck under: blend by mask, darken a thin seam along the earlier petal
        seam = (A > 0.5) & (m > 0.5)
        v = np.where(seam, v * 0.92, v)
        V = np.where(m > 0.5, v, V)
        A = np.maximum(A, m)
    r = np.sqrt(cx ** 2 + cy ** 2)
    core = r < 0.13
    V = np.where(core, 105, V)
    A = np.maximum(A, core.astype(np.float32))
    img = Image.fromarray(np.zeros((s, s), np.uint8), "L")
    d = ImageDraw.Draw(img)
    for k in range(10):
        a0 = k * 2 * math.pi / 10 + 0.3
        r1 = 0.15 + 0.1 * (k % 2)
        r2 = 0.34 + 0.05 * (k % 3)
        p1 = (s / 2 + math.cos(a0) * r1 * s / 2, s / 2 - math.sin(a0) * r1 * s / 2)
        p2 = (s / 2 + math.cos(a0) * r2 * s / 2, s / 2 - math.sin(a0) * r2 * s / 2)
        d.line([p1, p2], fill=255, width=3 * SS // 2)
        d.ellipse([p2[0] - 2.2 * SS, p2[1] - 2.2 * SS, p2[0] + 2.2 * SS, p2[1] + 2.2 * SS], fill=255)
    st = np.asarray(img, dtype=np.float32) / 255.0
    V = np.where(st > 0.5, 255, V)
    A = np.maximum(A, st)
    a = down(A, n, n)
    v = down(V / 255.0 * A, n, n) / np.maximum(a, 1e-3) * 255
    save(np.where(a > 0.01, v, 0), a, "cherry_blossom_flower")


def tex_cloud():
    n = 256
    rng = np.random.default_rng(4107)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x = (xx / (n - 1)) * 2 - 1
    y = -((yy / (n - 1)) * 2 - 1)
    f = np.zeros((n, n), np.float32)
    for i in range(46):
        ang = rng.random() * 6.283
        rad = (rng.random() ** 0.6) * 0.5
        px, py = math.cos(ang) * rad, math.sin(ang) * rad * 0.8 - 0.02
        r = 0.12 + rng.random() * 0.17
        d2 = ((x - px) ** 2 + (y - py) ** 2) / (r * r)
        f = np.maximum(f, np.clip(1 - d2, 0, 1) ** 0.7)
    f = f + (fbm(n, n, 32, 55) - 0.5) * 0.28
    a = smooth(0.12, 0.34, f) * (1 - smooth(0.8, 1.0, np.sqrt(x * x + y * y)))
    # cel shading: light from the top-left, three soft steps, mauve underside
    gy, gx = np.gradient(np.asarray(Image.fromarray((np.clip(f, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4)), np.float32) / 255.0)
    light = np.clip(-gx * 0.7 + gy * 0.9, -1, 1) * 8
    lv = np.clip(0.55 + light * 0.5 + f * 0.25, 0, 1)
    step = np.floor(lv * 3) / 3 * 0.55 + lv * 0.45
    v = 120 + 135 * step
    # white speckles (loose petals) and fine petal-cluster texture
    sp = noise(n, n, 6, 91)
    v = np.where(sp > 0.8, 255, v)
    v = v * (0.9 + 0.1 * fbm(n, n, 8, 77))
    save(v, a * 0.92, "cherry_blossom_cloud")


def petal_poly(cx, cy, length, width, ang, steps=30, narrow=1.0):
    pts = []
    for i in range(steps + 1):
        yv = 0.97 * i / steps
        pts.append((petal_width(yv) * narrow * width, yv))
    pts.append((0, 0.86))
    for i in range(steps, -1, -1):
        yv = 0.97 * i / steps
        pts.append((-petal_width(yv) * narrow * width, yv))
    out = []
    for px, py in pts:
        lx, ly = px, py * length
        out.append((cx + lx * math.cos(ang) + ly * math.sin(ang), cy - (-lx * math.sin(ang) + ly * math.cos(ang))))
    return out


def tex_sigil():
    n = 256
    s = n * SS
    img = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(img)
    c = s / 2

    def circ(r, w, fill=255):
        d.ellipse([c - r * c, c - r * c, c + r * c, c + r * c], outline=fill, width=int(w * SS))

    circ(0.97, 1.6)
    circ(0.93, 3.0, 200)
    circ(0.63, 1.8, 230)
    circ(0.60, 1.0, 150)
    # ring of ten petals pointing inward (tips toward the centre)
    for k in range(10):
        ang = k * 2 * math.pi / 10
        bx, by = c + math.sin(ang) * 0.9 * c, c - math.cos(ang) * 0.9 * c
        # a petal whose base is on the outer rim and whose tip points to the centre
        poly = petal_poly(bx, by, 0.27 * s, 0.36 * s / 2 * 0.9, ang + math.pi)
        d.polygon(poly, fill=70)
        d.line(poly + [poly[0]], fill=255, width=int(2.6 * SS), joint="curve")
        vx, vy = bx + math.sin(ang + math.pi) * 0.17 * s, by - math.cos(ang + math.pi) * 0.17 * s
        d.line([(bx, by), (vx, vy)], fill=190, width=SS)
        ob = (c + math.sin(ang + math.pi / 10) * 0.955 * c, c - math.cos(ang + math.pi / 10) * 0.955 * c)
        d.ellipse([ob[0] - 2.5 * SS, ob[1] - 2.5 * SS, ob[0] + 2.5 * SS, ob[1] + 2.5 * SS], fill=255)
    # inner five-petal blossom
    for k in range(5):
        ang = k * 2 * math.pi / 5 + math.pi / 5
        poly = petal_poly(c, c, 0.5 * c, 0.5 * c * 0.8, ang)
        d.polygon(poly, fill=90)
        d.line(poly + [poly[0]], fill=255, width=int(2.2 * SS), joint="curve")
    for k in range(5):
        ang = k * 2 * math.pi / 5
        p = (c + math.sin(ang) * 0.58 * c, c - math.cos(ang) * 0.58 * c)
        d.ellipse([p[0] - 3 * SS, p[1] - 3 * SS, p[0] + 3 * SS, p[1] + 3 * SS], fill=255)
    d.ellipse([c - 0.07 * c, c - 0.07 * c, c + 0.07 * c, c + 0.07 * c], fill=255)
    arr = down(np.asarray(img, dtype=np.float32) / 255.0, n, n)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt(((xx - n / 2) / (n / 2)) ** 2 + ((yy - n / 2) / (n / 2)) ** 2)
    glow = np.clip(1 - r, 0, 1) ** 2.2 * 0.18
    a = np.clip(arr + glow, 0, 1) * (1 - smooth(0.985, 1.0, r))
    v = 150 + 105 * np.clip(arr * 1.2, 0, 1)
    save(v, a, "cherry_blossom_sigil")


def tex_burst():
    n = 256
    rng = np.random.default_rng(777)
    base = petal_img(96)
    canvas = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    c = n / 2
    for i in range(34):
        ang = rng.random() * 2 * math.pi
        rad = (0.14 + 0.8 * rng.random() ** 0.8) * c
        size = 12 + 26 * (1 - rad / c) * (0.5 + rng.random()) + 6
        px, py = c + math.cos(ang) * rad, c - math.sin(ang) * rad
        # petals tumble, loosely pointing outward
        paste(canvas, base, px, py, size, math.degrees(ang) - 90 + (rng.random() - 0.5) * 120, 0.55 + 0.45 * (1 - rad / c))
    a = np.asarray(canvas, dtype=np.float32)[..., 3] / 255.0
    v = np.asarray(canvas, dtype=np.float32)[..., 0]
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    ang = np.arctan2(yy - c, xx - c)
    rays = (0.5 + 0.5 * np.sin(ang * 14 + fbm(n, n, 64, 3) * 9)) ** 6 * np.clip(1 - r, 0, 1) ** 1.4
    core = np.clip(1 - r / 0.34, 0, 1) ** 1.6
    a2 = np.clip(a + 0.55 * rays + core, 0, 1) * (1 - smooth(0.9, 1.0, r))
    v2 = np.where(a > 0.3, v, 235) * 0.7 + 255 * np.clip(core + rays * 0.5, 0, 1) * 0.3 + 40
    save(v2, a2, "cherry_blossom_burst")


def tex_ribbon():
    w, h = 128, 256
    rng = np.random.default_rng(912)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = (xx / (w - 1)) * 2 - 1
    t = yy / h
    wave = 0.12 * np.sin(t * 2 * math.pi * 2) + 0.05 * np.sin(t * 2 * math.pi * 5 + 1.3)
    d = (u - wave) / (0.36 + 0.06 * np.sin(t * 2 * math.pi * 3))
    core = np.exp(-d * d * 2.0)
    soft = np.exp(-(u / 0.95) ** 2 * 3.2)
    fine = 0.75 + 0.25 * fbm(w, h, 16, 12)
    a = np.clip((core * 0.9 + soft * 0.28) * fine, 0, 1)
    v = 175 + 80 * core
    canvas = Image.fromarray(np.dstack([v, v, v, a * 255]).astype(np.uint8), "RGBA")
    pet = petal_img(48)
    for i in range(12):
        px = w / 2 + (rng.random() - 0.5) * w * 0.8
        py = (i + rng.random()) * h / 12
        paste(canvas, pet, px, py, 14 + rng.random() * 12, rng.random() * 360, 1.0)
    arr = np.asarray(canvas, dtype=np.float32)
    arr[..., 3] *= 1 - smooth(0.82, 1.0, np.abs(u))
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(os.path.join(OUT, "cherry_blossom_ribbon.png"), optimize=True)
    print("wrote cherry_blossom_ribbon", (w, h))


def tex_blade():
    n = 128
    s = n * SS
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x = (xx / (s - 1)) * 2 - 1
    y = 1 - (yy / (s - 1)) * 1.06 + 0.03
    bend = 0.12 * np.sin(np.clip(y, 0, 1) * math.pi)
    xb = x - bend
    wd = 0.17 * np.clip(np.sin(np.clip(y, 0, 1) ** 0.8 * math.pi), 0, 1) ** 0.9
    m = ((np.abs(xb) < wd) & (y > 0) & (y < 1)).astype(np.float32)
    edge = np.clip(1 - np.abs(xb) / np.maximum(wd, 1e-3), 0, 1)
    v = 170 + 85 * (1 - edge) ** 2 * 0 + 85 * smooth(0.0, 0.5, edge) + 30 * y
    a = down(m, n, n)
    vv = down(v / 255.0 * m, n, n) / np.maximum(a, 1e-3) * 255
    # soft halo around the blade so it reads as a glinting edge
    halo = np.exp(-((x - bend) / 0.28) ** 2) * np.clip(np.sin(np.clip(y, 0, 1) * math.pi), 0, 1) ** 0.7 * 0.28
    halo = down(halo.astype(np.float32), n, n)
    a2 = np.clip(np.maximum(a, halo), 0, 1)
    save(np.where(a > 0.05, vv, 235), a2, "cherry_blossom_blade")


def tex_motes():
    n = 64
    rng = np.random.default_rng(31)
    canvas = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    pet = petal_img(48)
    for i in range(6):
        paste(canvas, pet, 10 + rng.random() * 44, 10 + rng.random() * 44, 12 + rng.random() * 10, rng.random() * 360, 0.95)
    arr = np.asarray(canvas, dtype=np.float32)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    star = np.clip(1 - np.abs(xx - 32) / 1.8, 0, 1) * np.clip(1 - np.abs(yy - 32) / 14, 0, 1) + \
        np.clip(1 - np.abs(yy - 32) / 1.8, 0, 1) * np.clip(1 - np.abs(xx - 32) / 14, 0, 1)
    arr[..., :3] = np.where(star[..., None] > 0.1, 255, arr[..., :3])
    arr[..., 3] = np.clip(arr[..., 3] + star * 160, 0, 255)
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(os.path.join(OUT, "cherry_blossom_motes.png"), optimize=True)
    print("wrote cherry_blossom_motes", (n, n))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex_petal()
    tex_flower()
    tex_cloud()
    tex_sigil()
    tex_burst()
    tex_ribbon()
    tex_blade()
    tex_motes()

"""Generates the Eye Magic VFX textures (iris, lid, slit pupil, lens flare, tracking reticle, gaze rays, ground sigil ...).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/eye_*.png
White / grey with alpha so the vertex colour tints them; the iris keeps a dark pupil (opaque) so it reads with ALPHA blending
and vanishes with ADD blending.

    python3 -B tools/gen_eye_textures.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def grid(w, h=None):
    h = h or w
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def vnoise(w, h, cells, seed):
    rng = np.random.RandomState(seed)
    g = rng.rand(cells + 1, cells + 1).astype(np.float32)
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(img, dtype=np.float32) / 255.0


def fbm(w, h, seed, base=4, octs=4):
    out, amp, tot = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octs):
        out += amp * vnoise(w, h, base * 2 ** o, seed + o * 31)
        tot += amp
        amp *= 0.5
    return out / tot


def save(name, grey, alpha):
    g = np.clip(grey, 0, 1)
    a = np.clip(alpha, 0, 1)
    arr = np.dstack([g, g, g, a])
    Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name, arr.shape[1], "x", arr.shape[0])


def canvas(w, h=None):
    h = h or w
    return Image.new("L", (w * SS, h * SS), 0)


def finish(img, w, h=None, blur=0.0):
    h = h or w
    im = img.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im, dtype=np.float32) / 255.0


def almond_h(x, k=0.46):
    return k * np.clip(1 - x * x, 0, 1) ** 0.85


# ------------------------------------------------------------------------------------------------ iris
def iris():
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rng = np.random.RandomState(7)
    fib = np.zeros_like(r)
    for k in range(9, 70, 3):
        fib += rng.rand() * np.sin(th * k + rng.rand() * 6.28 + r * rng.uniform(-4, 4))
    fib = fib / 18 + 0.5
    n1 = fbm(n, n, 11, 6, 3)
    base = 0.30 + 0.55 * np.clip(fib, 0, 1) * (0.6 + 0.8 * n1)
    base *= 0.75 + 0.5 * smooth(0.25, 0.8, r)                     # brighter toward the rim
    base += 0.35 * np.exp(-((r - 0.42) / 0.035) ** 2)              # collarette
    base *= 1 - 0.75 * smooth(0.80, 0.97, r)                       # limbal darkening
    base += 0.25 * np.exp(-((r - 0.985) / 0.02) ** 2)              # thin bright edge line
    pupil = 1 - smooth(0.20, 0.25, r)
    base = base * (1 - pupil) + 0.03 * pupil
    base += 0.45 * np.exp(-((r - 0.27) / 0.025) ** 2) * (1 - pupil)  # pupil rim glow
    alpha = 1 - smooth(0.965, 1.0, r)
    save("eye_iris", base, alpha)


# ------------------------------------------------------------------------------------------------ lids
def lid_outline(n=256):
    x, y = grid(n)
    xs = x / 0.96
    hu = almond_h(xs, 0.42)
    hl = almond_h(xs, 0.36)
    inside = (np.abs(xs) <= 1)
    d_up = np.abs(-y - hu)
    d_lo = np.abs(y - hl)
    line = np.maximum(np.exp(-(d_up / 0.012) ** 2), np.exp(-(d_lo / 0.010) ** 2)) * inside
    halo = np.maximum(np.exp(-(d_up / 0.05) ** 2), np.exp(-(d_lo / 0.045) ** 2)) * inside * 0.35
    # a second, thinner lid crease above
    crease = np.exp(-(np.abs(-y - hu * 1.28 - 0.04) / 0.006) ** 2) * (np.abs(xs) < 0.8) * 0.7
    # lashes: small ticks on the upper lid, leaning outward
    lash = np.zeros_like(x)
    for i in range(-5, 6):
        if i == 0:
            continue
        lx = i * 0.16
        top = almond_h(np.array(lx), 0.42)
        ln = 0.06 + 0.05 * (1 - abs(i) / 6)
        # lean outward
        t = np.clip((-y - top) / ln, 0, 1)
        cx = lx + np.sign(lx) * 0.07 * t
        lash += np.exp(-((xs - cx) / 0.008) ** 2) * ((-y > top) & (-y < top + ln))
    # inner-corner caret
    caret = np.exp(-(np.abs(xs + 0.93 + 0.4 * np.abs(y)) / 0.012) ** 2) * (np.abs(y) < 0.12)
    g = np.clip(line + halo * 0.6 + crease + lash * 0.8 + caret, 0, 1)
    a = np.clip(line + halo + crease + lash * 0.9 + caret, 0, 1)
    save("eye_almond", 0.55 + 0.45 * g, a)


def sclera():
    n = 256
    x, y = grid(n)
    xs = x / 0.96
    hu, hl = almond_h(xs, 0.42), almond_h(xs, 0.36)
    inside = (np.abs(xs) <= 1) & (-y < hu) & (y < hl)
    edge = np.minimum(hu + y, hl - y)
    edge = np.where(inside, edge, 0)
    a = smooth(0.0, 0.03, edge)
    shade = 0.30 + 0.7 * smooth(0.0, 0.30, edge)                 # darker toward the lids
    veins = fbm(n, n, 5, 8, 3)
    shade = shade * (0.8 + 0.3 * veins)
    save("eye_sclera", shade, a * 0.96)


def slit():
    w, h = 128, 256
    x, y = grid(w, h)
    wid = 0.55 * np.clip(1 - np.abs(y) ** 1.6, 0, 1) ** 1.2
    d = np.abs(x) / np.maximum(wid, 1e-3)
    core = np.exp(-(d * 1.6) ** 2) * (np.abs(y) < 1)
    halo = np.exp(-(np.abs(x) / 0.5) ** 2) * np.exp(-(np.abs(y) / 0.8) ** 2) * 0.35
    save("eye_slit", np.clip(core + 0.2, 0, 1) * (core > 0.02) + halo, np.clip(core * 1.1 + halo, 0, 1))


# ------------------------------------------------------------------------------------------------ flare / rays / glint
def flare():
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    streak = np.exp(-(y / 0.018) ** 2) * np.exp(-np.abs(x) * 2.4)
    streak2 = np.exp(-(y / 0.06) ** 2) * np.exp(-np.abs(x) * 4.5) * 0.5
    vert = np.exp(-(x / 0.012) ** 2) * np.exp(-np.abs(y) * 5.0) * 0.6
    ring = np.exp(-((r - 0.62) / 0.012) ** 2) * 0.5 + np.exp(-((r - 0.66) / 0.05) ** 2) * 0.12
    core = np.exp(-(r / 0.06) ** 2) + 0.4 * np.exp(-(r / 0.18) ** 2)
    ghosts = np.zeros_like(r)
    for cx, rad, k in ((-0.45, 0.07, 0.35), (0.3, 0.05, 0.3), (0.58, 0.1, 0.22), (-0.72, 0.04, 0.4)):
        d = np.sqrt((x - cx) ** 2 + y * y)
        ghosts += k * (smooth(rad, rad * 0.55, d)) * 0.7 + k * np.exp(-((d - rad) / 0.008) ** 2)
    v = np.clip(streak + streak2 + vert + ring + core + ghosts, 0, 1.4)
    save("eye_flare", np.clip(v, 0, 1), np.clip(v * 1.1, 0, 1))


def rays():
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rng = np.random.RandomState(21)
    v = np.zeros_like(r)
    for i in range(46):
        a0 = rng.uniform(-math.pi, math.pi)
        wd = rng.uniform(0.012, 0.05)
        ln = rng.uniform(0.45, 1.0)
        dth = np.arctan2(np.sin(th - a0), np.cos(th - a0))
        w = wd * (1 - np.clip(r / ln, 0, 1) * 0.85) * (0.4 / np.maximum(r, 0.2) + 0.4)
        v += rng.uniform(0.4, 1.0) * np.exp(-(dth * r / np.maximum(w, 1e-3)) ** 2) * (1 - smooth(ln * 0.5, ln, r))
    v *= smooth(0.04, 0.12, r)
    v += 0.7 * np.exp(-(r / 0.08) ** 2)
    save("eye_rays", np.clip(v, 0, 1), np.clip(v, 0, 1) * (1 - smooth(0.92, 1.0, r)))


def glint():
    n = 64
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    a = np.exp(-(y / 0.05) ** 2) * np.exp(-np.abs(x) * 2.2) + np.exp(-(x / 0.05) ** 2) * np.exp(-np.abs(y) * 2.2)
    d1 = np.exp(-((x - y) / 0.04) ** 2) * np.exp(-r * 4.5) * 0.35 + np.exp(-((x + y) / 0.04) ** 2) * np.exp(-r * 4.5) * 0.35
    v = np.clip(a + d1 + 1.2 * np.exp(-(r / 0.12) ** 2), 0, 1)
    save("eye_glint", v, v * (1 - smooth(0.9, 1.0, r)))


# ------------------------------------------------------------------------------------------------ line art
def reticle():
    n = 256
    im = canvas(n)
    d = ImageDraw.Draw(im)
    S = n * SS
    c = S / 2

    def circ(rad, w, fill=255):
        d.ellipse([c - rad * c, c - rad * c, c + rad * c, c + rad * c], outline=fill, width=int(w * SS))
    circ(0.96, 2)
    circ(0.90, 1)
    circ(0.66, 1.5)
    for i in range(72):                                   # ticks
        a = math.tau * i / 72
        l = 0.07 if i % 6 == 0 else 0.035
        r0 = 0.90
        d.line([c + math.cos(a) * r0 * c, c + math.sin(a) * r0 * c, c + math.cos(a) * (r0 - l) * c, c + math.sin(a) * (r0 - l) * c],
               fill=255, width=int((2 if i % 6 == 0 else 1) * SS))
    for q in range(4):                                    # chevron brackets pointing inward
        a = math.pi / 4 + q * math.pi / 2
        for s in (-1, 1):
            a0 = a + s * 0.0
        r1, r2 = 0.60, 0.40
        tip = (c + math.cos(a) * r2 * c, c + math.sin(a) * r2 * c)
        l1 = (c + math.cos(a - 0.2) * r1 * c, c + math.sin(a - 0.2) * r1 * c)
        l2 = (c + math.cos(a + 0.2) * r1 * c, c + math.sin(a + 0.2) * r1 * c)
        d.line([l1, tip, l2], fill=255, width=int(2.5 * SS))
    for q in range(4):                                    # cross gaps
        a = q * math.pi / 2
        d.line([c + math.cos(a) * 0.22 * c, c + math.sin(a) * 0.22 * c, c + math.cos(a) * 0.50 * c, c + math.sin(a) * 0.50 * c],
               fill=200, width=int(1.5 * SS))
    circ(0.12, 1.5)
    # dotted inner ring
    for i in range(24):
        a = math.tau * i / 24
        rr = 0.78
        d.ellipse([c + math.cos(a) * rr * c - 2 * SS, c + math.sin(a) * rr * c - 2 * SS, c + math.cos(a) * rr * c + 2 * SS, c + math.sin(a) * rr * c + 2 * SS], fill=220)
    v = finish(im, n)
    blur = np.asarray(Image.fromarray((v * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3)), dtype=np.float32) / 255
    a = np.clip(v + blur * 0.6, 0, 1)
    save("eye_reticle", 0.6 + 0.4 * v, a)


def sigil():
    n = 256
    im = canvas(n)
    d = ImageDraw.Draw(im)
    S = n * SS
    c = S / 2

    def circ(rad, w, fill=255):
        d.ellipse([c - rad * c, c - rad * c, c + rad * c, c + rad * c], outline=fill, width=int(w * SS))
    circ(0.98, 2.5)
    circ(0.93, 1)
    circ(0.62, 1.5)
    circ(0.56, 1)
    circ(0.30, 2)
    circ(0.10, 1.5)
    # twelve small almond eyes between the outer ring and the second ring
    for i in range(12):
        a = math.tau * i / 12
        rr = 0.775
        ux, uy = math.cos(a), math.sin(a)       # radial
        tx, ty = -uy, ux                        # tangent
        pts_u, pts_l = [], []
        for k in range(15):
            s = k / 14 * 2 - 1
            hh = 0.055 * (1 - s * s) ** 0.9
            L = 0.095 * s
            pts_u.append((c + (ux * rr + tx * L + ux * hh) * c, c + (uy * rr + ty * L + uy * hh) * c))
            pts_l.append((c + (ux * rr + tx * L - ux * hh) * c, c + (uy * rr + ty * L - uy * hh) * c))
        d.line(pts_u + pts_l[::-1] + [pts_u[0]], fill=255, width=int(1.5 * SS))
        d.ellipse([c + ux * rr * c - 0.022 * c, c + uy * rr * c - 0.022 * c, c + ux * rr * c + 0.022 * c, c + uy * rr * c + 0.022 * c], fill=255)
    # two interlaced triangles
    for off in (0, math.pi / 3):
        pts = [(c + math.cos(off - math.pi / 2 + k * math.tau / 3) * 0.62 * c, c + math.sin(off - math.pi / 2 + k * math.tau / 3) * 0.62 * c) for k in range(3)]
        d.line(pts + [pts[0]], fill=230, width=int(1.5 * SS))
    # radial spokes
    for i in range(12):
        a = math.tau * i / 12 + math.pi / 12
        d.line([c + math.cos(a) * 0.32 * c, c + math.sin(a) * 0.32 * c, c + math.cos(a) * 0.56 * c, c + math.sin(a) * 0.56 * c], fill=180, width=int(1 * SS))
    v = finish(im, n)
    blur = np.asarray(Image.fromarray((v * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.5)), dtype=np.float32) / 255
    save("eye_sigil", 0.6 + 0.4 * v, np.clip(v + blur * 0.55, 0, 1))


# ------------------------------------------------------------------------------------------------ streaks / shards
def trail():
    w, h = 64, 256
    x, y = grid(w, h)
    yy = (np.arange(h, dtype=np.float32) + 0.5) / h
    pulse = 0.78 + 0.22 * np.sin(yy * math.tau * 4)[:, None]
    n = fbm(w, h, 3, 4, 3)
    core = np.exp(-(x / 0.07) ** 2)
    mid = np.exp(-(x / 0.25) ** 2) * 0.6 * (0.75 + 0.5 * n)
    halo = np.exp(-(np.abs(x) / 0.7) ** 2) * 0.25
    # thin spiral hint
    spir = np.exp(-((x - 0.38 * np.sin(yy * math.tau * 3)[:, None]) / 0.06) ** 2) * 0.5
    v = (core + mid + halo + spir) * pulse
    save("eye_trail", np.clip(v, 0, 1), np.clip(v, 0, 1) * (1 - smooth(0.85, 1.0, np.abs(x))))


def shard():
    w, h = 64, 128
    x, y = grid(w, h)
    t = (y + 1) / 2                                    # 0 top (tip) .. 1 bottom
    wid = 0.62 * np.sin(np.clip(t, 0, 1) ** 0.7 * math.pi) ** 0.9 * (1 - 0.0 * t)
    inside = np.abs(x) < wid
    facet = 0.55 + 0.45 * np.sign(x) * 0.5 + 0.2 * fbm(w, h, 9, 3, 3)
    edge = smooth(0, 0.18, wid - np.abs(x))
    mid = np.exp(-(x / 0.05) ** 2)
    g = np.clip(facet * 0.8 + 0.5 * mid + 0.4 * (1 - edge), 0, 1)
    save("eye_shard", g, inside * np.clip(edge * 2.5, 0, 1) * 0.95)


def curl():
    """The curled face-mark of the anime still: a tendril that loops into a hook with a small bead at the end (two mirrored
    copies, a rosy cel-shaded line with a soft glow)."""
    w = 256
    im = canvas(w)
    d = ImageDraw.Draw(im)
    S = w * SS
    for side in (-1, 1):
        pts = []
        for i in range(120):
            u = i / 119.0
            ang = -0.6 + u * 4.6
            r = (0.42 - 0.3 * u) * S * 0.5
            cx = S * (0.5 + side * 0.17)
            pts.append((cx + side * r * math.cos(ang) + side * u * S * 0.12, S * 0.5 + r * math.sin(ang) - (1 - u) * S * 0.06))
        d.line(pts, fill=255, width=int(S * 0.028), joint="curve")
        ex, ey = pts[-1]
        rr = S * 0.026
        d.ellipse([ex - rr, ey - rr, ex + rr, ey + rr], fill=255)
    line = finish(im, w, blur=0.0)
    halo = finish(im, w, blur=5.0)
    save("eye_curl", np.clip(0.75 + 0.25 * line, 0, 1), np.clip(line + halo * 0.7, 0, 1))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    iris(); lid_outline(); sclera(); slit(); flare(); rays(); glint(); reticle(); sigil(); trail(); shard(); curl()

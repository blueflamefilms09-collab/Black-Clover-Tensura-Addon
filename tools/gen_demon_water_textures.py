"""Generates the Demon Water Magic VFX textures (Hydra of Darkneros: scaled serpent heads with fanged maws, spiny crests, ink-black water).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/demon_water_*.png

    python3 -B tools/gen_demon_water_textures.py

Look reference (owner concept art, manga): pale scaled serpents with long spiny ridges, open maws full of fangs, drawn with heavy
black linework on a black ground, bubbles drifting up. Textures are grey-scale with alpha so the vertex colour tints them (ink-black
water, teal-green light); the dark linework is part of the grey values.
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


class Cv:
    """Supersampled grey + alpha canvas."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("LA", (w * SS, h * SS), (40, 0))
        self.d = ImageDraw.Draw(self.im)

    def P(self, pts):
        return [(x * self.w * SS, (1 - y) * self.h * SS) for x, y in pts]   # unit coords, y up

    def poly(self, pts, v=200, a=255):
        self.d.polygon(self.P(pts), fill=(int(v), int(a)))

    def line(self, pts, v=20, wd=0.01, a=255):
        px = self.P(pts)
        self.d.line(px, fill=(int(v), int(a)), width=max(1, int(wd * self.w * SS)), joint="curve")

    def ell(self, cx, cy, rx, ry, v=200, a=255):
        x0, y0 = self.P([(cx - rx, cy + ry)])[0]
        x1, y1 = self.P([(cx + rx, cy - ry)])[0]
        self.d.ellipse([x0, y0, x1, y1], fill=(int(v), int(a)))

    def arc(self, cx, cy, rx, ry, a0, a1, v=20, wd=0.01):
        x0, y0 = self.P([(cx - rx, cy + ry)])[0]
        x1, y1 = self.P([(cx + rx, cy - ry)])[0]
        self.d.arc([x0, y0, x1, y1], a0, a1, fill=(int(v), 255), width=max(1, int(wd * self.w * SS)))

    def array(self):
        g = np.asarray(self.im.resize((self.w, self.h), Image.LANCZOS), dtype=np.float32) / 255.0
        return g[..., 0], g[..., 1]


def save(name, grey, alpha):
    rgba = np.zeros(grey.shape + (4,), np.uint8)
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = np.clip(grey * 255, 0, 255).astype(np.uint8)
    rgba[..., 3] = np.clip(alpha * 255, 0, 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, "demon_water_%s.png" % name))


def noise(w, h, scale, seed):
    rng = np.random.default_rng(seed)
    n = rng.random((h, w)).astype(np.float32)
    im = Image.fromarray((n * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(scale))
    a = np.asarray(im, dtype=np.float32)
    a = (a - a.min()) / max(1e-6, a.max() - a.min())
    return a


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w, (y + 0.5) / h


# ------------------------------------------------------------------------------------------------ serpent head (tip up)
def head():
    W, H = 128, 256
    c = Cv(W, H)
    # spiny crest fins on both sides of the skull (the long ridges of the concept art)
    for s in (-1, 1):
        for i in range(7):
            y = 0.16 + i * 0.075
            x0 = 0.5 + s * (0.20 + 0.05 * math.sin(i * 0.5))
            tip = (0.5 + s * (0.46 - i * 0.012), y + 0.075 + 0.02 * i)
            c.poly([(x0, y), tip, (x0 - s * 0.01, y + 0.07)], 150, 255)
            c.line([(x0, y), tip, (x0 - s * 0.01, y + 0.07)], 15, 0.008)
    # neck + skull
    skull = [(0.34, 0.0), (0.66, 0.0), (0.70, 0.16), (0.76, 0.34), (0.70, 0.50), (0.60, 0.58), (0.40, 0.58), (0.30, 0.50), (0.24, 0.34), (0.30, 0.16)]
    c.poly(skull, 190)
    c.line(skull + [skull[0]], 12, 0.012)
    # scale hatching on the skull
    rnd = random.Random(7)
    for row in range(10):
        for col in range(6):
            x = 0.31 + col * 0.075 + (0.037 if row % 2 else 0)
            y = 0.04 + row * 0.052
            if abs(x - 0.5) > 0.21 - max(0, (0.12 - y)) * 0.3:
                continue
            c.arc(x, y, 0.034, 0.03, 190, 350, 70, 0.007)
    # brow ridges and the eyes: glowing slits
    for s in (-1, 1):
        c.poly([(0.5 + s * 0.06, 0.47), (0.5 + s * 0.20, 0.52), (0.5 + s * 0.17, 0.43)], 60)
        c.poly([(0.5 + s * 0.085, 0.475), (0.5 + s * 0.17, 0.495), (0.5 + s * 0.155, 0.455)], 255)
    # the two jaws, opened wide, with the dark maw between them
    maw = [(0.40, 0.56), (0.60, 0.56), (0.74, 0.88), (0.5, 0.80), (0.26, 0.88)]
    c.poly(maw, 8)
    for s in (-1, 1):
        jaw = [(0.5 + s * 0.10, 0.54), (0.5 + s * 0.25, 0.60), (0.5 + s * 0.36, 0.80), (0.5 + s * 0.30, 0.99), (0.5 + s * 0.20, 0.82), (0.5 + s * 0.12, 0.64)]
        c.poly(jaw, 175)
        c.line(jaw + [jaw[0]], 12, 0.012)
        # fangs along the inner edge of the jaw
        n = 7
        for i in range(n):
            t = i / (n - 1)
            bx = 0.5 + s * (0.12 + 0.17 * t)
            by = 0.64 + 0.30 * t
            ln = 0.07 + 0.06 * math.sin(math.pi * t)
            c.poly([(bx, by - 0.025), (bx, by + 0.025), (bx - s * ln * 0.95, by + ln * 0.15 + 0.01)], 252)
            c.line([(bx, by - 0.025), (bx - s * ln * 0.95, by + ln * 0.15 + 0.01), (bx, by + 0.025)], 12, 0.006)
        # the long curved fang at the front
        c.poly([(0.5 + s * 0.29, 0.97), (0.5 + s * 0.25, 0.84), (0.5 + s * 0.215, 0.62 + 0.05)], 255)
    # tongue
    c.poly([(0.47, 0.60), (0.53, 0.60), (0.55, 0.72), (0.5, 0.70), (0.45, 0.72)], 110)
    g, a = c.array()
    save("head", g, a)


# ------------------------------------------------------------------------------------------------ scaled body strip
def scales(glow):
    W, H = 128, 256
    c = Cv(W, H)
    rows, cols = 16, 4
    rh, cw = 1.0 / rows, 1.0 / cols
    # body silhouette: spiky margins, tileable in V
    for r in range(-1, rows + 1):
        for k in range(cols + 1):
            cx = (k + (0.5 if r % 2 else 0.0)) * cw * 0.84 + 0.08
            cy = 1 - (r + 0.5) * rh
            if cx < 0.02 or cx > 0.98:
                continue
            w2, h2 = cw * 0.50, rh * 0.95
            shield = [(cx - w2, cy + h2 * 0.45), (cx - w2 * 0.8, cy - h2 * 0.1), (cx, cy - h2 * 0.75), (cx + w2 * 0.8, cy - h2 * 0.1), (cx + w2, cy + h2 * 0.45), (cx, cy + h2 * 0.55)]
            v = 150 + 70 * ((r * 7 + k * 3) % 5) / 4
            if glow:
                c.line(shield + [shield[0]], 255, 0.012)
                c.ell(cx, cy + h2 * 0.1, 0.012, 0.01, 255)
            else:
                c.poly(shield, v)
                c.line(shield + [shield[0]], 14, 0.011)
                c.arc(cx, cy + h2 * 0.2, w2 * 0.55, h2 * 0.35, 20, 160, 235, 0.008)
    # spines along both margins
    for r in range(rows):
        for s in (0, 1):
            y = 1 - (r + 0.5) * rh
            x = 0.0 if s == 0 else 1.0
            sg = 1 if s == 0 else -1
            tri = [(x, y + rh * 0.28), (x + sg * 0.15, y - rh * 0.15), (x, y - rh * 0.32)]
            if glow:
                c.line(tri, 255, 0.012)
            else:
                c.poly(tri, 170)
                c.line(tri, 14, 0.011)
    g, a = c.array()
    # soft body shading: darker towards the margins so the strip reads as round
    X, Y = grid(W, H)
    shade = 1 - 0.55 * np.abs(X - 0.5) ** 1.5 * 2.2
    if glow:
        alpha = np.clip(a * g, 0, 1)
        # the band centre catches light
        alpha *= 0.55 + 0.45 * np.exp(-((X - 0.5) / 0.28) ** 2)
        save("scale_glow", np.ones_like(g), alpha)
    else:
        save("scales", g * np.clip(shade, 0.3, 1), a)


# ------------------------------------------------------------------------------------------------ circular maw sigil
def fangring():
    W = 256
    c = Cv(W, W)
    cx = cy = 0.5

    def ring(r0, r1, v=255):
        pts_o = [(cx + r1 * math.cos(t * math.tau / 180), cy + r1 * math.sin(t * math.tau / 180)) for t in range(181)]
        pts_i = [(cx + r0 * math.cos(t * math.tau / 180), cy + r0 * math.sin(t * math.tau / 180)) for t in range(180, -1, -1)]
        c.poly(pts_o + pts_i, v)

    ring(0.470, 0.488)
    ring(0.395, 0.405)
    ring(0.19, 0.205)
    n = 24
    for i in range(n):
        a = math.tau * i / n
        da = math.tau / n * 0.42
        # outer fangs point inwards from the rim, inner fangs outwards: a closing maw
        r_out, r_tip = 0.395, 0.275 + 0.03 * (i % 2)
        c.poly([(cx + r_out * math.cos(a - da), cy + r_out * math.sin(a - da)), (cx + r_out * math.cos(a + da), cy + r_out * math.sin(a + da)),
                (cx + r_tip * math.cos(a), cy + r_tip * math.sin(a))], 255)
        a2 = a + math.tau / n / 2
        c.poly([(cx + 0.205 * math.cos(a2 - da * 0.7), cy + 0.205 * math.sin(a2 - da * 0.7)), (cx + 0.205 * math.cos(a2 + da * 0.7), cy + 0.205 * math.sin(a2 + da * 0.7)),
                (cx + 0.27 * math.cos(a2), cy + 0.27 * math.sin(a2))], 230)
        # spines on the outer rim
        c.poly([(cx + 0.488 * math.cos(a - da * 0.6), cy + 0.488 * math.sin(a - da * 0.6)), (cx + 0.488 * math.cos(a + da * 0.6), cy + 0.488 * math.sin(a + da * 0.6)),
                (cx + 0.5 * math.cos(a), cy + 0.5 * math.sin(a))], 255)
    # serpent coils between the rings: S-curves around the circle
    for i in range(6):
        a0 = math.tau * i / 6
        pts = []
        for k in range(24):
            t = k / 23
            r = 0.35 + 0.025 * math.sin(t * math.tau * 2)
            a = a0 + t * math.tau / 6 * 0.9
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        c.line(pts, 255, 0.010, 200)
    g, a = c.array()
    X, Y = grid(W, W)
    r = np.hypot(X - 0.5, Y - 0.5)
    # a faint scaled water disc inside
    n1 = noise(W, W, 3, 4)
    disc = np.clip(1 - r / 0.19, 0, 1) ** 1.5 * (0.15 + 0.2 * n1)
    alpha = np.maximum(a * (0.85 + 0.15 * g), disc)
    save("fangring", np.ones_like(g), alpha)


# ------------------------------------------------------------------------------------------------ bubble
def bubble():
    W = 64
    X, Y = grid(W, W)
    x, y = (X - 0.5) * 2, (Y - 0.5) * 2
    r = np.hypot(x, y)
    rim = np.exp(-((r - 0.86) / 0.07) ** 2)
    body = np.clip(1 - r, 0, 1) ** 0.5 * 0.10
    # crescent highlight upper-left, fainter bounce light lower-right
    hl = np.exp(-(((x + 0.42) / 0.20) ** 2 + ((y + 0.42) / 0.10) ** 2)) * (r < 0.8)
    th = math.radians(45)
    xr, yr = x * math.cos(th) + y * math.sin(th), -x * math.sin(th) + y * math.cos(th)
    hl = np.exp(-(((xr + 0.60) / 0.17) ** 2 + ((yr) / 0.34) ** 2)) * (r < 0.84)
    bounce = np.exp(-(((x - 0.45) / 0.24) ** 2 + ((y - 0.45) / 0.09) ** 2)) * 0.5 * (r < 0.8)
    a = np.clip(rim * 0.85 + body + hl + bounce, 0, 1) * np.clip((1 - r) * 14, 0, 1)
    save("bubble", np.ones((W, W), np.float32), a)


# ------------------------------------------------------------------------------------------------ ground ripple rings
def ripple():
    W = 256
    X, Y = grid(W, W)
    x, y = (X - 0.5) * 2, (Y - 0.5) * 2
    r = np.hypot(x, y)
    ang = np.arctan2(y, x)
    n = noise(W, W, 6, 11)
    wob = 0.012 * np.sin(ang * 5 + 1) + 0.02 * (n - 0.5)
    a = np.zeros_like(r)
    for r0, wd, amp in ((0.93, 0.025, 1.0), (0.78, 0.018, 0.75), (0.64, 0.014, 0.55), (0.47, 0.011, 0.4), (0.30, 0.009, 0.3)):
        broken = 0.55 + 0.45 * np.sin(ang * 7 + r0 * 20)      # rings are interrupted
        a += amp * np.exp(-((r - r0 + wob) / wd) ** 2) * np.clip(broken * 1.6, 0, 1)
    a += 0.18 * np.exp(-((r - 0.97) / 0.06) ** 2)
    a *= np.clip((1 - r) * 12, 0, 1)
    save("ripple", np.ones_like(a), np.clip(a, 0, 1))


# ------------------------------------------------------------------------------------------------ ink pool with ragged rim
def pool():
    W = 256
    X, Y = grid(W, W)
    x, y = (X - 0.5) * 2, (Y - 0.5) * 2
    r = np.hypot(x, y)
    ang = np.arctan2(y, x)
    n1 = noise(W, W, 5, 21)
    rng = np.random.default_rng(5)
    edge = 0.80 + 0.045 * np.sin(ang * 9 + 0.7) + 0.04 * np.sin(ang * 17 + 2.0)
    # dripping tongues of ink
    for k in range(14):
        a0 = rng.random() * math.tau
        wd = 0.06 + rng.random() * 0.1
        da = np.angle(np.exp(1j * (ang - a0)))
        edge = edge + (0.1 + 0.09 * rng.random()) * np.exp(-(da / wd) ** 2)
    d = (r - edge)
    body = np.clip(-d * 40, 0, 1)
    # swirling surface: spiral ridges and curdled noise
    spiral = 0.5 + 0.5 * np.sin(ang * 3 + r * 16 + n1 * 4)
    ridge = np.exp(-((spiral - 0.5) / 0.12) ** 2)
    g = 0.30 + 0.35 * n1 + 0.30 * ridge * (r < edge - 0.05)
    # black tar rim, then a thin lit edge
    rim = np.exp(-((d + 0.035) / 0.03) ** 2)
    g = g * (1 - 0.75 * rim)
    g = np.clip(g + 0.6 * np.exp(-((d + 0.01) / 0.012) ** 2), 0, 1)
    save("pool", g, body * 0.97)


# ------------------------------------------------------------------------------------------------ ink splatter
def splash():
    W = 128
    c = Cv(W, W)
    rng = random.Random(33)
    c.ell(0.5, 0.5, 0.13, 0.12, 255)
    for i in range(15):
        a = math.tau * i / 15 + rng.random() * 0.3
        ln = 0.18 + rng.random() * 0.22
        wd = 0.035 + rng.random() * 0.03
        tip = (0.5 + ln * 2 * math.cos(a) * 0.95, 0.5 + ln * 2 * math.sin(a) * 0.95)
        base = (0.5 + 0.08 * math.cos(a), 0.5 + 0.08 * math.sin(a))
        px, py = -math.sin(a) * wd, math.cos(a) * wd
        mid = (0.5 + ln * 1.1 * math.cos(a), 0.5 + ln * 1.1 * math.sin(a))
        c.poly([(base[0] + px, base[1] + py), (mid[0] + px * 0.7, mid[1] + py * 0.7), tip, (mid[0] - px * 0.7, mid[1] - py * 0.7), (base[0] - px, base[1] - py)], 255)
        if rng.random() < 0.8:
            c.ell(0.5 + (ln * 2 + 0.06) * math.cos(a + 0.1), 0.5 + (ln * 2 + 0.06) * math.sin(a + 0.1), 0.014, 0.014, 255)
    for i in range(14):
        a = rng.random() * math.tau
        rr = 0.3 + rng.random() * 0.17
        c.ell(0.5 + rr * math.cos(a), 0.5 + rr * math.sin(a), 0.008 + rng.random() * 0.012, 0.008 + rng.random() * 0.012, 255)
    g, a = c.array()
    save("splash", np.ones_like(a), a)


# ------------------------------------------------------------------------------------------------ thorn / spine shard (tip up)
def spine():
    W, H = 64, 128
    c = Cv(W, H)
    sh = [(0.2, 0.0), (0.8, 0.0), (0.66, 0.35), (0.56, 0.7), (0.5, 1.0), (0.44, 0.7), (0.33, 0.35)]
    c.poly(sh, 230)
    c.poly([(0.5, 0.02), (0.78, 0.02), (0.62, 0.35), (0.54, 0.7), (0.5, 1.0)], 150)
    c.line(sh + [sh[0]], 20, 0.03)
    for t in (0.2, 0.4, 0.6):
        c.line([(0.5 - 0.2 * (1 - t), t), (0.5 + 0.2 * (1 - t), t - 0.06)], 40, 0.02)
    g, a = c.array()
    save("spine", g, a)


# ------------------------------------------------------------------------------------------------ flowing streak (afterimage, V along the flow)
def streak():
    W, H = 64, 256
    X, Y = grid(W, H)
    x = (X - 0.5) * 2
    n = noise(W, H, 4, 3)
    a = np.exp(-(x / (0.45 + 0.1 * np.sin(Y * 12))) ** 2)
    lines = 0.5 + 0.5 * np.sin((x + 0.2 * np.sin(Y * 9)) * 14 + n * 5)
    a = a * (0.45 + 0.55 * lines) * (0.7 + 0.3 * n)
    a *= np.clip(Y * 8, 0, 1) * np.clip((1 - Y) * 8, 0, 1)
    save("streak", np.ones_like(a), np.clip(a * 1.2, 0, 1))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    head()
    scales(False)
    scales(True)
    fangring()
    bubble()
    ripple()
    pool()
    splash()
    spine()
    streak()
    print("demon water textures written")

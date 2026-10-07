"""Generates Secre Swallowtail's Seal Magic VFX textures (cyan / light-blue neon, square-edged geometry, white speed lines).

    python3 -B tools/gen_seal_secre_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/secre_*.png (white / grey with alpha, tinted by the vertex colour).
Look reference: docs/attributes/art_reference/seal_eternal_prison.webp (clear blue glass cube, thin bright edges, white speed lines),
seal_inverse_release.webp (a white point with blocky right-angle lightning lines), seal_basic.webp (swirling blue ribbons), seal_circles_pack.jpg.
  secre_hex     256  hexagram circle: square-cut rim, nested squares, hexagram, corner nodes
  secre_rings   256  concentric rings with block ticks, broken bars and a square eye
  secre_star    256  eight-point star with an octagon frame and diamond nodes
  secre_circle  256  nested rotated squares inside a ring (a clean geometric array)
  secre_line    256x32  a straight neon line (white core, square ends), U along the line
  secre_node    64   a square node with a diamond flare
  secre_face    256  a cube face: bright thin frame, corner brackets, faint grid, a glass sheen
  secre_speed   256x16  a white speed line, pointed tail, hard head
  secre_plus    64   a plus-shaped glint
  secre_core    256  a white point with sharp rays and right-angle bolts
  secre_hoop    256x64  a ring band (rails, chevrons), tileable along U
  secre_flash   256  the breaking flash: shattered rays and shards
  secre_stream  64x256  a gentle stream (soft ribbon with bright motes), V along the flow
  secre_orb     64   a glowing orb with a crisp rim
"""
import math

import numpy as np

from gen_seal_textures import Cv


def square(cv, r, w, rot=0.0, heat=1.0, glow=0.0, ga=0.3, cx=0.0, cy=0.0):
    pts = [(cx + r * math.cos(rot + math.pi / 4 + k * math.pi / 2), cy + r * math.sin(rot + math.pi / 4 + k * math.pi / 2))
           for k in range(4)]
    cv.poly(pts, w, True, heat, glow, ga, flat=True)
    return pts


def tex_hex():
    cv = Cv(256, 256)
    cv.disc(0.94, soft=0.04, heat=0.0, gain=0.07)
    cv.ring(0.975, 0.018, 1.0, glow=0.03, ga=0.4)
    cv.ring(0.92, 0.008, 0.9)
    # square-cut rim blocks
    for i in range(48):
        a = 2 * math.pi * i / 48
        if i % 2 == 0:
            cv.seg((0.935 * math.cos(a), 0.935 * math.sin(a)), (0.965 * math.cos(a), 0.965 * math.sin(a)), 0.02, 0.9, flat=True)
    v = cv.star(0.86, 6, 2, 0.016, heat=1.0, glow=0.025, ga=0.35)
    cv.poly([(0.86 * math.cos(-math.pi / 2 + k * math.pi / 3), 0.86 * math.sin(-math.pi / 2 + k * math.pi / 3)) for k in range(6)], 0.008, heat=0.8)
    for (x, y) in v:
        square(cv, 0.035, 0.012, 0.0, 1.0, cx=x, cy=y)
        cv.disc(0.015, heat=1.0, cx=x, cy=y)
    square(cv, 0.70, 0.012, 0.0, 0.95, glow=0.02, ga=0.3)
    square(cv, 0.70, 0.010, math.pi / 4, 0.9)
    cv.ring(0.44, 0.012, 1.0)
    cv.ring(0.40, 0.006, 0.8, dashes=24, duty=0.55)
    cv.star(0.4, 6, 2, 0.008, rot=0.0, heat=0.9)
    square(cv, 0.17, 0.014, math.pi / 4, 1.0, glow=0.02, ga=0.35)
    cv.ticks(0.0, 0.34, 4, 0.006, 0.8)
    cv.disc(0.05, heat=1.0)
    cv.glow(0.12, amp=0.55)
    cv.save("secre_hex")


def tex_rings():
    cv = Cv(256, 256)
    cv.disc(0.92, soft=0.05, heat=0.0, gain=0.06)
    cv.ring(0.97, 0.014, 1.0, glow=0.035, ga=0.4)
    cv.ring(0.90, 0.022, 1.0, dashes=48, duty=0.7)
    for i in range(72):
        a = 2 * math.pi * i / 72
        r1 = 0.86 if i % 3 else 0.82
        cv.seg((0.78 * math.cos(a), 0.78 * math.sin(a)), (r1 * math.cos(a), r1 * math.sin(a)), 0.012, 0.85, flat=True)
    cv.ring(0.74, 0.008, 0.8)
    for k in range(4):
        a0 = k * math.pi / 2 + 0.12
        cv.ring(0.66, 0.05, 1.0, a0=a0, a1=a0 + 1.1, glow=0.02, ga=0.3)
        cv.ring(0.575, 0.012, 0.9, a0=a0 + 0.5, a1=a0 + 1.45)
    cv.ring(0.50, 0.007, 0.8)
    for k in range(6):
        a = k * math.pi / 3
        x, y = 0.5 * math.cos(a), 0.5 * math.sin(a)
        square(cv, 0.028, 0.010, a, 1.0, cx=x, cy=y)
    cv.ring(0.40, 0.020, 1.0, dashes=12, duty=0.7, phase=0.15)
    cv.ring(0.30, 0.008, 0.9)
    square(cv, 0.19, 0.013, 0.0, 1.0, glow=0.02, ga=0.3)
    cv.ring(0.10, 0.012, 1.0)
    cv.disc(0.045, heat=1.0)
    cv.glow(0.1, amp=0.5)
    cv.save("secre_rings")


def tex_star():
    cv = Cv(256, 256)
    cv.disc(0.92, soft=0.05, heat=0.0, gain=0.05)
    cv.ring(0.965, 0.014, 1.0, glow=0.03, ga=0.4)
    v = cv.star(0.90, 8, 3, 0.016, heat=1.0, glow=0.025, ga=0.35)
    cv.poly(v, 0.008, heat=0.8)
    cv.star(0.66, 8, 2, 0.010, heat=0.9)
    cv.star(0.66, 8, 2, 0.010, rot=-math.pi / 2 + math.pi / 8, heat=0.8)
    for (x, y) in v:
        a = math.atan2(y, x)
        cv.poly([(x + 0.07 * math.cos(a), y + 0.07 * math.sin(a)), (x - 0.05 * math.sin(a), y + 0.05 * math.cos(a)),
                 (x - 0.04 * math.cos(a), y - 0.04 * math.sin(a)), (x + 0.05 * math.sin(a), y - 0.05 * math.cos(a))], 0.012, heat=1.0)
    cv.ring(0.46, 0.012, 1.0)
    cv.ring(0.42, 0.006, 0.8, dashes=32, duty=0.5)
    for k in range(4):
        a = k * math.pi / 2
        cv.seg((0.14 * math.cos(a), 0.14 * math.sin(a)), (0.40 * math.cos(a), 0.40 * math.sin(a)), 0.016, 1.0, flat=True)
    square(cv, 0.12, 0.014, math.pi / 4, 1.0)
    cv.disc(0.05, heat=1.0)
    cv.glow(0.1, amp=0.5)
    cv.save("secre_star")


def tex_circle():
    cv = Cv(256, 256)
    cv.disc(0.92, soft=0.05, heat=0.0, gain=0.05)
    cv.ring(0.97, 0.020, 1.0, glow=0.035, ga=0.4)
    for k in range(6):
        square(cv, 0.96 - 0.09 * k, 0.012 if k % 2 == 0 else 0.007, k * math.pi / 12, 1.0 - 0.06 * k, glow=0.015 if k == 0 else 0.0, ga=0.3)
    cv.ring(0.9, 0.006, 0.8, dashes=60, duty=0.5)
    cv.ring(0.40, 0.012, 1.0)
    cv.ring(0.26, 0.008, 0.9, dashes=16, duty=0.6)
    for k in range(8):
        a = k * math.pi / 4
        cv.seg((0.47 * math.cos(a), 0.47 * math.sin(a)), (0.56 * math.cos(a), 0.56 * math.sin(a)), 0.014, 0.9, flat=True)
    cv.disc(0.06, heat=1.0)
    cv.glow(0.12, amp=0.5)
    cv.save("secre_circle")


def tex_line():
    cv = Cv(256, 32)
    sl, X, Y = cv.full()
    u = (X + 8.0) / 16.0
    core = (np.abs(Y) < 0.10).astype(np.float32)
    edge = np.clip(0.5 - (np.abs(Y) - 0.10) / cv.u, 0, 1)
    body = np.where(np.abs(Y) < 0.10, 1.0, edge)
    halo = np.exp(-((Y / 0.42) ** 2)) * 0.55
    pulse = 0.8 + 0.2 * np.sin(u * math.tau * 10)
    cap = np.clip((np.minimum(u, 1 - u)) * 80, 0, 1)
    cv.put(sl, np.clip(np.maximum(body, halo) * cap * pulse, 0, 1), np.clip(body * cap, 0, 1))
    cv.save("secre_line")


def tex_node():
    cv = Cv(64, 64)
    cv.glow(0.45, amp=0.5)
    sl, X, Y = cv.full()
    fl = np.exp(-(np.abs(X) / 0.05)) * np.clip(1 - np.abs(Y), 0, 1) ** 2 + np.exp(-(np.abs(Y) / 0.05)) * np.clip(1 - np.abs(X), 0, 1) ** 2
    cv.put(sl, fl * 0.8, 0.7)
    sq = np.maximum(np.abs(X), np.abs(Y))
    cv.put(sl, np.clip(0.5 - (sq - 0.2) / cv.u, 0, 1), 1.0)
    cv.put(sl, np.clip(0.5 - (np.abs(sq - 0.32) - 0.025) / cv.u, 0, 1), 0.9)
    cv.save("secre_node")


def tex_face():
    cv = Cv(256, 256)
    sl, X, Y = cv.full()
    m = np.maximum(np.abs(X), np.abs(Y))
    inner = np.clip((0.97 - m) / 0.97, 0, 1)
    fill = 0.10 + 0.18 * (1 - inner) ** 1.5
    frame = np.exp(-(((m - 0.955) / 0.05) ** 2)) * 0.9
    edge = np.clip(0.5 - (np.abs(m - 0.965) - 0.012) / cv.u, 0, 1)
    cv.put(sl, np.clip(fill + frame * 0.5, 0, 1) * (m < 1.0), 0.15)
    cv.put(sl, edge, 1.0)
    # faint grid
    for g in np.linspace(-0.75, 0.75, 4):
        cv.seg((g, -0.94), (g, 0.94), 0.005, 0.6, flat=True, gain=0.45)
        cv.seg((-0.94, g), (0.94, g), 0.005, 0.6, flat=True, gain=0.45)
    # corner brackets
    for sx in (-1, 1):
        for sy in (-1, 1):
            c = (0.9 * sx, 0.9 * sy)
            cv.seg(c, (0.9 * sx - 0.28 * sx, 0.9 * sy), 0.03, 1.0, flat=True)
            cv.seg(c, (0.9 * sx, 0.9 * sy - 0.28 * sy), 0.03, 1.0, flat=True)
            square(cv, 0.025, 0.01, 0.0, 1.0, cx=0.78 * sx, cy=0.78 * sy)
    # a diagonal glass sheen
    d = np.abs(X + Y * 0.9 + 0.35)
    cv.put(sl, np.clip(0.5 - (d - 0.07) / cv.u, 0, 1) * (m < 0.93) * 0.35, 0.5)
    d2 = np.abs(X + Y * 0.9 + 0.62)
    cv.put(sl, np.clip(0.5 - (d2 - 0.025) / cv.u, 0, 1) * (m < 0.93) * 0.3, 0.5)
    cv.save("secre_face")


def tex_speed():
    cv = Cv(256, 16)
    sl, X, Y = cv.full()
    u = (X + 16.0) / 32.0
    wd = 0.06 + 0.75 * u
    body = np.clip(1 - np.abs(Y) / wd, 0, 1) ** 0.8 * np.clip(u * 1.2, 0, 1) ** 1.2
    hard = (u > 0.985).astype(np.float32) * 0
    cv.put(sl, np.clip(body * 1.1, 0, 1) * np.clip((1 - u) * 90, 0, 1) + hard, np.clip(body, 0, 1))
    cv.save("secre_speed")


def tex_plus():
    cv = Cv(64, 64)
    cv.glow(0.45, amp=0.35)
    sl, X, Y = cv.full()
    ax, ay = np.abs(X), np.abs(Y)
    arm = np.maximum(np.clip(1 - np.maximum(ax / 0.8, ay / 0.2), 0, 1), np.clip(1 - np.maximum(ay / 0.8, ax / 0.2), 0, 1))
    shape = np.clip(arm * 4.0, 0, 1)
    cv.put(sl, shape, 1.0)
    cv.put(sl, np.clip(0.5 - (np.maximum(ax, ay) - 0.12) / cv.u, 0, 1), 1.0)
    cv.put(sl, np.exp(-((X * X + Y * Y) / 0.04)), 1.0)
    cv.save("secre_plus")


def tex_core():
    cv = Cv(256, 256)

    def fn(rho, ang, X, Y):
        out = np.exp(-(rho / 0.10) ** 2) * 1.3 + np.exp(-(rho / 0.30) ** 2) * 0.35
        rng = np.random.default_rng(31)
        for i in range(24):
            a = 2 * math.pi * i / 24 + (rng.random() - 0.5) * 0.16
            ln = 0.40 + 0.6 * rng.random() if i % 3 == 0 else 0.20 + 0.3 * rng.random()
            dang = np.abs((ang - a + math.pi) % (2 * math.pi) - math.pi)
            fall = np.clip(1 - rho / ln, 0, 1)
            out = np.maximum(out, np.exp(-((dang * np.maximum(rho, 0.02)) / (0.006 + 0.022 * fall)) ** 2) * fall ** 1.1)
        return out

    cv.polar(fn, heat=1.0)
    # right-angle bolts (blocky zigzag) leaving the core, like the inverse-release art
    rng = np.random.default_rng(5)
    for k in range(9):
        a = 2 * math.pi * k / 9 + 0.3
        x, y = 0.07 * math.cos(a), 0.07 * math.sin(a)
        horiz = (k % 2 == 0)
        for s in range(5):
            step = 0.05 + 0.09 * rng.random()
            sgn = 1 if (math.cos(a) if horiz else math.sin(a)) >= 0 else -1
            nx, ny = (x + sgn * step, y) if horiz else (x, y + sgn * step)
            cv.seg((x, y), (nx, ny), 0.012, 1.0, flat=True, glow=0.014, ga=0.3)
            x, y = nx, ny
            horiz = not horiz
            sgn2 = 1 if rng.random() < 0.5 else -1
            if s < 4:
                nx, ny = (x + sgn2 * step * 0.6, y) if horiz else (x, y + sgn2 * step * 0.6)
                cv.seg((x, y), (nx, ny), 0.012, 1.0, flat=True)
                x, y = nx, ny
                horiz = not horiz
    cv.save("secre_core")


def tex_hoop():
    cv = Cv(256, 64)
    cv.seg((-4.2, -0.9), (4.2, -0.9), 0.07, 1.0, glow=0.12, ga=0.35)
    cv.seg((-4.2, 0.9), (4.2, 0.9), 0.07, 1.0, glow=0.12, ga=0.35)
    cv.seg((-4.2, 0.0), (4.2, 0.0), 0.05, 0.9, glow=0.2, ga=0.45)
    n = 16
    for i in range(n):
        cx = -4 + (i + 0.5) * 8 / n
        cv.poly([(cx - 0.18, -0.55), (cx + 0.12, 0.0), (cx - 0.18, 0.55)], 0.06, closed=False, heat=1.0, flat=True)
        sx = -4 + i * 8 / n
        cv.seg((sx, -0.35), (sx, 0.35), 0.04, 0.8, flat=True)
    cv.save("secre_hoop")


def tex_flash():
    cv = Cv(256, 256)

    def fn(rho, ang, X, Y):
        out = np.exp(-(rho / 0.20) ** 2) * 1.2 + np.exp(-(rho / 0.55) ** 2) * 0.3
        rng = np.random.default_rng(77)
        for i in range(30):
            a = 2 * math.pi * i / 30 + (rng.random() - 0.5) * 0.15
            ln = 0.5 + 0.5 * rng.random()
            dang = np.abs((ang - a + math.pi) % (2 * math.pi) - math.pi)
            fall = np.clip(1 - rho / ln, 0, 1)
            out = np.maximum(out, np.exp(-((dang * np.maximum(rho, 0.02)) / (0.008 + 0.03 * fall)) ** 2) * fall ** 0.9)
        return out

    cv.polar(fn, heat=1.0)
    rng = np.random.default_rng(9)
    for i in range(26):
        a = rng.random() * math.tau
        d = 0.28 + 0.62 * rng.random()
        s = 0.03 + 0.07 * rng.random()
        cx, cy = d * math.cos(a), d * math.sin(a)
        pts = [(cx + s * math.cos(a + k * 2.0 + rng.random()), cy + s * math.sin(a + k * 2.0 + rng.random())) for k in range(3)]
        cv.fill(pts, 0.9, 0.85)
    cv.ring(0.7, 0.012, 1.0, glow=0.03, ga=0.3, dashes=18, duty=0.6)
    cv.save("secre_flash")


def tex_stream():
    cv = Cv(64, 256)
    sl, X, Y = cv.full()
    v = (Y + 1) / 2
    wob = np.sin(Y * 9.0) * 0.07
    xx = X - wob
    body = np.exp(-((xx / 0.16) ** 2) * 1.5)
    core = np.exp(-((xx / 0.05) ** 2) * 1.5) * (0.65 + 0.35 * np.sin(Y * 22.0))
    cv.put(sl, np.clip(body * 0.6 + core * 0.8, 0, 1), np.clip(core, 0, 1))
    rng = np.random.default_rng(13)
    for i in range(10):
        cv.disc(0.014 + 0.012 * rng.random(), heat=1.0, cx=(rng.random() - 0.5) * 0.36, cy=-0.95 + 1.9 * (i + rng.random() * 0.5) / 10)
    cv.save("secre_stream")


def tex_orb():
    cv = Cv(64, 64)
    sl, X, Y = cv.full()
    rho = np.hypot(X, Y)
    cv.put(sl, np.exp(-((rho / 0.55) ** 2) * 1.6) * 0.8, 0.2)
    cv.put(sl, np.clip(0.5 - (rho - 0.42) / cv.u, 0, 1) * 0.7, 0.6)
    cv.put(sl, np.clip(0.5 - (np.abs(rho - 0.42) - 0.035) / cv.u, 0, 1), 1.0)
    cv.put(sl, np.exp(-(((X + 0.1) ** 2 + (Y + 0.12) ** 2) / 0.02)), 1.0)
    cv.save("secre_orb")


def main():
    for f in (tex_hex, tex_rings, tex_star, tex_circle, tex_line, tex_node, tex_face, tex_speed, tex_plus, tex_core, tex_hoop, tex_flash, tex_stream, tex_orb):
        f()


if __name__ == "__main__":
    main()

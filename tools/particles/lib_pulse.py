"""Rings, bursts, circles, bolts, runes, slashes and swirls (the 'shape' particles)."""
import copy
import math

import numpy as np

from artkit import (Canvas, add_px, blank, cov, glow, hexc, lerp, over, ramp, sd_capsule, sd_disc, sd_ellipse, sd_poly, sd_ring, sd_star, smoothstep, solid, union, vnoise)
from registry import particle

WHITE = ramp("#8f9bbd", "#bcc7e6", "#e6ecfb", "#ffffff")
WARM = ramp("#a9946a", "#d9c492", "#f5e8c2", "#ffffff")


def _line(c, pts, w0, w1=None):
    s = np.full(c.x.shape, 9.0, np.float32)
    for k in range(len(pts) - 1):
        a = lerp(w0, w1 if w1 is not None else w0, k / max(1, len(pts) - 1))
        b = lerp(w0, w1 if w1 is not None else w0, (k + 1) / max(1, len(pts) - 1))
        s = np.minimum(s, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], a, b))
    return s


@particle("ring_pulse", "pulse", 8, group="pulse", px=32, tint=True, behaviour="A thin ring that leaves the spot and widens fast while it thins and fades: the shape of a pulse or a cast. Stays in place, additive.",
          size=[0.5, 2.4], life=[10, 16])
def ring_pulse(c, i, n, rng):
    t = i / float(n - 1)
    e = 1 - (1 - t) ** 2
    r = lerp(3.0, 14.8, e)
    w = lerp(2.4, 0.9, t)
    inten = np.clip(1.0 - np.abs(c.r - r) / (w * 0.5 + 0.9), 0, 1) * (1 - t) ** 0.9 * 1.15
    inner = np.exp(-((c.r / (r * 0.8 + 1)) ** 2)) * 0.18 * (1 - t)
    return glow(c, inten + inner, WHITE, levels=4, amax=250, floor=0.15)


@particle("halo_ring", "pulse", 6, group="pulse", px=32, tint=True, behaviour="A halo: a thick glowing ring with a thin echo inside and eight sparkles on it; holds its size and pulses, then dims. Stays in place, additive.",
          size=[1.2, 1.4], curve="hold", life=[22, 34], fade=[0.2, 0.45], anim="loop", loop=3, spin=0.0)
def halo_ring(c, i, n, rng):
    t = i / float(n)
    p = 1.0 + 0.06 * math.sin(t * 2 * math.pi)
    R = 11.0 * p
    main = np.clip(1.0 - np.abs(c.r - R) / 2.4, 0, 1)
    echo = np.clip(1.0 - np.abs(c.r - (R - 3.6)) / 0.9, 0, 1) * 0.55
    glo = np.exp(-(((c.r - R) / 4.0) ** 2)) * 0.4
    img = glow(c, main + echo + glo, WARM, levels=5, amax=245, floor=0.12)
    for k in range(8):
        a = k * math.pi / 4 + t * math.pi / 4
        px, py = int(c.n / 2 + R * math.cos(a)), int(c.n / 2 + R * math.sin(a))
        add_px(img, [(px, py), (px + 1, py), (px, py + 1)] if k % 2 == 0 else [(px, py)], "#ffffff", 255)
    return img


@particle("shock_ring", "pulse", 6, group="pulse", px=32, tint=True, behaviour="A hard shock ring: a ragged thick band that races outward and thins to a line; the hit of a stomp or a blast. Stays in place, additive.",
          size=[0.6, 2.6], life=[8, 14], fade=[0.0, 0.5])
def shock_ring(c, i, n, rng):
    t = i / float(n - 1)
    e = 1 - (1 - t) ** 2.4
    R = lerp(4.0, 14.5, e)
    th = lerp(4.6, 1.2, t)
    rag = 0.9 * np.sin(c.a * 9 + i * 1.3) * (1 - t) + 0.5 * np.sin(c.a * 17 - i)
    sd = np.abs(c.r - (R + rag)) - th * 0.5
    sh = np.clip(0.5 + (c.r - R) / (th * 1.3), 0, 0.999)
    img = solid(c, sd, sh, ramp("#7d88a8", "#aab6d4", "#dde5f6", "#ffffff"), outline=None)
    img[..., 3] *= (1 - 0.55 * t)
    inner = glow(c, np.exp(-(((c.r - R + 2) / 2.5) ** 2)) * 0.35 * (1 - t), WHITE, levels=3, amax=130)
    return over(inner, img)


@particle("impact_burst", "pulse", 6, group="pulse", px=32, tint=True, behaviour="A comic impact star: a ten-spike burst that flashes at full size and opens into a ring of spikes as it fades. Stays in place, additive.",
          size=[0.8, 2.2], life=[8, 14], fade=[0.0, 0.55], spin=0.04)
def impact_burst(c, i, n, rng):
    t = i / float(n - 1)
    R = lerp(7.0, 14.5, 1 - (1 - t) ** 2)
    sd = sd_star(c, 0, 0, 10, R, R * 0.46, ang=0.3 + 0.1 * i)
    if t > 0.3:
        hole = R * lerp(0.0, 0.72, (t - 0.3) / 0.7)
        sd = np.maximum(sd, -(c.r - hole))
    sh = np.clip(1.0 - c.r / (R + 1), 0, 0.999)
    img = solid(c, sd, sh, ramp("#9aa7cc", "#cdd7f0", "#f2f6ff", "#ffffff"), outline=None)
    img[..., 3] *= (1 - 0.5 * t)
    return img


@particle("circle_flash", "pulse", 8, group="pulse", px=32, tint=True, behaviour="A magic circle that snaps open: two rings, a hexagram and tick marks that turn a little while they flash and fade. Lies where it is spawned, additive.",
          size=[1.6, 1.9], life=[12, 20], curve="ease_out", fade=[0.0, 0.55], spin=0.0)
def circle_flash(c, i, n, rng):
    t = i / float(n - 1)
    b = 1.0 - 0.8 * t
    rot = (i * math.pi / 14) * 0.7
    R = 13.2
    s = np.minimum(sd_ring(np.sqrt(c.x ** 2 + c.y ** 2) - R, 1.0), sd_ring(np.sqrt(c.x ** 2 + c.y ** 2) - 9.2, 0.9))
    for off in (0.0, math.pi / 3):
        pts = [(9.2 * math.cos(rot + off + k * 2 * math.pi / 3 - math.pi / 2), 9.2 * math.sin(rot + off + k * 2 * math.pi / 3 - math.pi / 2)) for k in range(3)]
        for k in range(3):
            s = np.minimum(s, sd_capsule(c, pts[k][0], pts[k][1], pts[(k + 1) % 3][0], pts[(k + 1) % 3][1], 0.9))
    for k in range(12):
        a = rot * 0.5 + k * math.pi / 6
        s = np.minimum(s, sd_capsule(c, 13.2 * math.cos(a), 13.2 * math.sin(a), 15.0 * math.cos(a), 15.0 * math.sin(a), 0.9))
    inten = np.clip((0.75 - s) / 1.0, 0, 1) * b
    halo = np.exp(-(((c.r - 11.0) / 5.0) ** 2)) * 0.22 * b
    flash = np.exp(-((c.r / 6.0) ** 2)) * max(0.0, 1.0 - t * 2.4) * 0.9
    return glow(c, inten + halo + flash, WHITE, levels=5, amax=250, floor=0.1)


@particle("lightning_arc", "pulse", 6, group="storm", px=32, tint=True, behaviour="A jagged bolt that cracks across the sprite and re-rolls its path every frame (flicker), with a short branch and a pale halo. Stays in place, additive, very short.",
          size=[0.9, 0.8], life=[4, 8], curve="hold", fade=[0.0, 0.3], anim="loop", loop=1)
def lightning_arc(c, i, n, rng):
    rr = np.random.default_rng(900 + i * 7)
    k = 7
    pts = []
    flip = -1 if i % 2 else 1
    for s in range(k + 1):
        u = s / float(k)
        x = lerp(-13.5, 13.5, u) * flip
        y = lerp(-11.0, 11.0, u)
        if 0 < s < k:
            x += rr.uniform(-3.2, 3.2)
            y += rr.uniform(-3.2, 3.2)
        pts.append((x, y))
    sd = _line(c, pts, 1.5)
    bi = int(rr.integers(2, 5))
    bx, by = pts[bi]
    br = [(bx, by), (bx + rr.uniform(3, 6) * flip, by + rr.uniform(2, 6)), (bx + rr.uniform(5, 9) * flip, by + rr.uniform(5, 9))]
    sd2 = _line(c, br, 1.4, 0.3)
    s = np.minimum(sd, sd2)
    core = np.clip((0.55 - s) / 0.8, 0, 1)
    halo = np.clip((2.4 - s) / 2.6, 0, 1) * 0.42
    return glow(c, core + halo, ramp("#6c8be0", "#a6c0ff", "#e8f1ff", "#ffffff"), levels=4, amax=250, floor=0.18)


# ---------------------------------------------------------------- runes (eight glyphs)
def _rune_sol(c):
    s = sd_ring(c.r - 3.4, 1.4)
    s = np.minimum(s, sd_disc(c, 0, 0, 1.2))
    for k in range(8):
        a = k * math.pi / 4
        s = np.minimum(s, sd_capsule(c, 4.9 * math.cos(a), 4.9 * math.sin(a), 6.6 * math.cos(a), 6.6 * math.sin(a), 1.3))
    return s


def _rune_luna(c):
    big = sd_disc(c, 0, 0, 5.6)
    cut = sd_disc(c, 2.6, -0.8, 4.6)
    s = np.maximum(big, -cut)
    return np.minimum(s, sd_disc(c, 3.6, -3.8, 0.9))


def _rune_eye(c):
    a = sd_disc(c, 0, 6.6, 8.6)
    b = sd_disc(c, 0, -6.6, 8.6)
    lens = np.maximum(a, b)
    s = sd_ring(lens, 1.4)
    s = np.minimum(s, sd_disc(c, 0, 0, 1.8))
    return np.minimum(s, sd_capsule(c, 0, -6.4, 0, -5.0, 1.2))


def _rune_wave(c):
    s = np.full(c.x.shape, 9.0, np.float32)
    for row in (-3.6, 0.0, 3.6):
        pts = [(x, row + 1.4 * math.sin(x * 0.9)) for x in np.linspace(-6, 6, 14)]
        s = np.minimum(s, _line(c, pts, 1.4))
    return s


def _rune_flame(c):
    pts = [(0, -6.6), (3.4, -1.6), (4.6, 2.4), (2.6, 5.8), (0, 6.6), (-2.6, 5.8), (-4.6, 2.4), (-3.4, -0.6), (-1.2, -3.2), (0, -6.6)]
    s = _line(c, pts, 1.4)
    return np.minimum(s, sd_disc(c, 0, 3.0, 1.5))


def _rune_leaf(c):
    x, y = c.rot(-math.pi / 4)
    class _V:
        pass
    v = _V(); v.x, v.y = x, y
    lens = np.maximum(sd_disc(v, 0, 5.6, 8.6), sd_disc(v, 0, -5.6, 8.6))
    s = sd_ring(lens, 1.4)
    return np.minimum(s, sd_capsule(c, -4.4, 4.4, 4.2, -4.2, 1.1))


def _rune_bolt(c):
    return _line(c, [(2.2, -6.4), (-2.4, -0.4), (2.0, 0.6), (-2.6, 6.4)], 1.7)


def _rune_key(c):
    s = sd_ring(sd_disc(c, 0, -3.6, 2.4), 1.4)
    s = np.minimum(s, sd_capsule(c, 0, -1.2, 0, 6.2, 1.4))
    s = np.minimum(s, sd_capsule(c, 0, 3.4, 2.8, 3.4, 1.3))
    return np.minimum(s, sd_capsule(c, 0, 5.8, 2.8, 5.8, 1.3))


def _rune(builder):
    def draw(c, i, n, rng):
        b = [0.35, 0.75, 1.0, 1.0, 0.7, 0.3][i]
        sc = 1.0 + 0.04 * (i - 2)
        v = copy.copy(c)
        v.x, v.y, v.r = c.x / sc, c.y / sc, c.r / sc
        s = builder(v)
        core = np.clip((0.7 - s) / 1.0, 0, 1) * b
        halo = np.clip((3.0 - s) / 3.4, 0, 1) * 0.38 * b
        ring = np.clip(1.0 - np.abs(c.r - 7.3) / 0.7, 0, 1) * 0.35 * b * (np.sin(c.a * 6 + i) > 0.1)
        return glow(c, core + halo + ring, WHITE, levels=5, amax=250, floor=0.12)
    return draw


for _pid, _fn, _txt in (
        ("rune_sol", _rune_sol, "the sun glyph: a ringed dot with eight ticks"),
        ("rune_luna", _rune_luna, "the moon glyph: a crescent with a star"),
        ("rune_eye", _rune_eye, "the eye glyph: an almond with a pupil and a brow tick"),
        ("rune_wave", _rune_wave, "the water glyph: three waves"),
        ("rune_flame", _rune_flame, "the fire glyph: a triangle with a flame bar"),
        ("rune_leaf", _rune_leaf, "the life glyph: a leaf with a midrib"),
        ("rune_bolt", _rune_bolt, "the storm glyph: a zigzag bolt"),
        ("rune_key", _rune_key, "the key glyph: a looped key with two teeth")):
    particle(_pid, "rune", 6, tint=True, group="rune",
             behaviour="Rune glyph, %s. Fades in, glows steady, fades out while rising a hair; a faint ring of dashes around it. Stays in place, additive." % _txt)(_rune(_fn))


@particle("slash_streak", "combat", 6, group="combat", px=32, tint=True, behaviour="A sword slash: a tapering crescent that sweeps open over the first frames and then thins out; white edge, pale body. Stays in place, additive, very short.",
          size=[1.0, 1.5], life=[5, 9], curve="linear", fade=[0.0, 0.5], anim="age")
def slash_streak(c, i, n, rng):
    t = i / float(n - 1)
    sweep = min(1.0, (i + 1) / 3.0)
    tail = min(0.7, max(0.0, (i - 2) / 3.0))
    pts = []
    k = 14
    for s in range(k + 1):
        u = s / float(k)
        if u > sweep or u < tail * 0.8:
            continue
        a = math.radians(lerp(-155, -25, u))
        pts.append((22 * math.cos(a) + 1.0, 22 * math.sin(a) + 17.0, math.sin(math.pi * u)))
    s = np.full(c.x.shape, 9.0, np.float32)
    for q in range(len(pts) - 1):
        s = np.minimum(s, sd_capsule(c, pts[q][0], pts[q][1], pts[q + 1][0], pts[q + 1][1], 0.4 + 4.6 * pts[q][2] * (1 - 0.4 * tail), 0.4 + 4.6 * pts[q + 1][2] * (1 - 0.4 * tail)))
    inten = np.clip((0.8 - s) / 1.2, 0, 1)
    edge = np.clip((0.7 - np.abs(s + 1.6)) / 0.9, 0, 1) * 0.0
    halo = np.clip((3.5 - s) / 3.5, 0, 1) * 0.35
    return glow(c, inten + halo + edge, WHITE, levels=5, amax=250, floor=0.12)


@particle("wind_swirl", "combat", 8, group="air", px=32, tint=True, behaviour="A double-armed spiral of thin wind lines that turns while it opens up and fades. Stays in place, additive.",
          size=[0.9, 1.5], life=[14, 22], curve="ease_out", fade=[0.1, 0.55], anim="age", spin=0.12)
def wind_swirl(c, i, n, rng):
    t = i / float(n - 1)
    rot = t * math.pi * 0.8
    s = np.full(c.x.shape, 9.0, np.float32)
    for arm in range(2):
        pts = []
        for q in range(26):
            th = q / 25.0 * 2.5 * math.pi
            r = 1.5 + th * 1.7 * (0.8 + 0.4 * t)
            a = th + rot + arm * math.pi
            pts.append((r * math.cos(a), r * math.sin(a), q / 25.0))
        for q in range(25):
            s = np.minimum(s, sd_capsule(c, pts[q][0], pts[q][1], pts[q + 1][0], pts[q + 1][1], 0.5 + 1.6 * math.sin(math.pi * pts[q][2]), 0.5 + 1.6 * math.sin(math.pi * pts[q + 1][2])))
    inten = np.clip((0.75 - s) / 1.0, 0, 1) * (1 - 0.5 * t)
    return glow(c, inten, WHITE, levels=4, amax=235, floor=0.18)

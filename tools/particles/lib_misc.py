"""Tools, paper, ink, thread, music and the darker combat particles."""
import math

import numpy as np

from artkit import (Canvas, add_px, blank, cov, glow, hexc, lerp, lumpy, over, ramp, sd_capsule, sd_disc, sd_ellipse, sd_poly, sd_ring, smoothstep, solid, union, vnoise)
from registry import particle


def _box(c, hx, hy, ang=0.0, r=0.0):
    x, y = c.rot(ang)
    qx, qy = np.abs(x) - (hx - r), np.abs(y) - (hy - r)
    return np.sqrt(np.maximum(qx, 0) ** 2 + np.maximum(qy, 0) ** 2) + np.minimum(np.maximum(qx, qy), 0) - r


@particle("cog", "tool", 6, group="mechanism", behaviour="A brass cog with eight teeth and an axle hole that turns steadily; drifts down, translucent. The gear of clockwork and machines.",
          gravity=0.08, spin=0.0, size=[0.26, 0.2], life=[26, 44], anim="loop", loop=2)
def cog(c, i, n, rng):
    ang = i * (math.pi / 4) / n
    body = sd_disc(c, 0, 0, 4.3)
    teeth = np.full(c.x.shape, 9.0, np.float32)
    for k in range(8):
        a = ang + k * math.pi / 4
        x, y = c.rot(a)
        teeth = np.minimum(teeth, np.maximum(np.abs(y) - 1.35, np.abs(x - 5.0) - 1.3))
    sd = np.minimum(body, teeth)
    sd = np.maximum(sd, -(sd_disc(c, 0, 0, 1.6)))
    sh = np.clip(0.55 + 0.3 * (-(c.x + c.y) / 10.0) + 0.12 * np.sin(c.a * 8 + ang * 8), 0, 0.999)
    return solid(c, sd, sh, ramp("#5a3c1c", "#8a5e2c", "#c08a40", "#ecc070"), outline="#2e1d0c")


@particle("clock_tick", "tool", 6, group="mechanism", behaviour="A small brass clock face whose hand jumps one step each frame, with a brief ring pulse on every tick; hovers and fades, translucent. (The Time Magic effects are untouched; this is a plain tick glyph.)",
          gravity=-0.02, spin=0.0, drag=0.9, size=[0.3, 0.28], life=[22, 36], anim="loop", loop=3, curve="hold")
def clock_tick(c, i, n, rng):
    face = solid(c, c.r - 5.2, np.clip(0.8 - c.r / 9.0, 0, 0.999), ramp("#cdbd96", "#e6d9b8", "#f6eed6", "#fffbea"), outline="#6b4a22")
    rim = sd_ring(c.r - 5.9, 1.4)
    img = over(solid(c, rim, np.clip(0.5 - c.y / 14.0, 0, 0.999), ramp("#6b4a22", "#a77a38", "#d9a752", "#f3d48a"), outline=None), face)
    for k in range(4):
        a = k * math.pi / 2
        add_px(img, [(int(c.n / 2 - 0.5 + 4.1 * math.cos(a) + 0.5), int(c.n / 2 - 0.5 + 4.1 * math.sin(a) + 0.5))], "#6b4a22", 255)
    a = -math.pi / 2 + i * math.pi / 3
    hand = sd_capsule(c, 0, 0, 3.8 * math.cos(a), 3.8 * math.sin(a), 1.1)
    m = hand < 0.25
    img[m, :3] = hexc("#3a2410")
    img[m, 3] = 255
    pulse = glow(c, np.clip(1.0 - np.abs(c.r - (6.2 + 0.7 * (i % 2))) / 0.8, 0, 1) * (0.5 if i % 2 == 0 else 0.0), ramp("#f3d48a"), levels=2, amax=170, floor=0.1)
    return over(pulse, img)


@particle("card", "tool", 6, group="paper", behaviour="A playing card flipping through the air: front with a red diamond, back with a blue lattice; turns about its long axis, falls with a sway, translucent.",
          gravity=0.10, spin=0.0, sway=0.01, size=[0.3, 0.26], life=[30, 50])
def card(c, i, n, rng):
    ph = i * math.pi / 3.0
    wf = math.cos(ph)
    hx = max(0.9, 3.7 * abs(wf))
    sd = _box(c, hx, 5.2, 0.35, 0.8)
    front = wf > 0
    x, y = c.rot(0.35)
    if front:
        img = solid(c, sd, np.clip(0.8 - 0.15 * y / 5.0, 0, 0.999), ramp("#c9c9d6", "#e8e8f0", "#f8f8fc", "#ffffff"), outline="#40404e")
        if hx > 2.0:
            pip = (np.abs(x) / max(1.0, hx * 0.5) + np.abs(y) / 2.6) < 1.0
            pip &= sd < -0.8
            img[pip, :3] = hexc("#c8283c")
            img[pip, 3] = 255
    else:
        img = solid(c, sd, np.clip(0.5 + 0.2 * np.sin((x + y) * 1.3), 0, 0.999), ramp("#1c3a7a", "#2a55a8", "#3f74d0", "#6e9ae8"), outline="#102048")
        if hx > 2.0:
            lat = ((np.floor((x + 8) / 2.0) + np.floor((y + 8) / 2.0)) % 2 == 0) & (sd < -1.0)
            img[lat, :3] = hexc("#8fb4ff")
    return img


@particle("page", "tool", 6, group="paper", behaviour="A loose page: parchment with lines of script and a curling corner that flaps as it spirals down; slow, swaying, translucent.",
          gravity=0.05, sway=0.016, spin=0.06, size=[0.32, 0.28], life=[36, 62])
def page(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    sx = 0.55 + 0.45 * abs(math.cos(ph))
    ang = 0.3 * math.sin(ph) + 0.1
    sd = _box(c, 4.4 * sx, 5.6, ang, 0.6)
    x, y = c.rot(ang)
    img = solid(c, sd, np.clip(0.7 + 0.15 * np.sin(ph) - 0.12 * (y / 6.0), 0, 0.999), ramp("#b19b6c", "#d3c08c", "#ecdfb5", "#fff6d8"), outline="#6a5936")
    for row in range(5):
        yy = -3.2 + row * 1.6
        m = (np.abs(y - yy) < 0.4) & (np.abs(x) < 3.0 * sx) & (sd < -0.9) & (np.abs(x) < 3.2 * sx - (0.8 if row == 4 else 0))
        img[m, :3] = hexc("#6a5030")
        img[m, 3] = 255
    curl = (x > 1.2 * sx) & (y > 2.0) & ((x - 1.2 * sx) + (y - 2.0) > 3.6 - 1.4 * math.sin(ph) ** 2) & (sd < 0)
    img[curl, :3] = hexc("#8a7448")
    return img


def _splat(c, i, n, rmp, seed, drip_col):
    t = i / float(n - 1)
    r = lerp(1.6, 5.2, 1 - (1 - t) ** 2)
    nz = lumpy(c, np.random.default_rng(seed), 4, 2)
    ang_n = np.sin(c.a * 5 + seed) * 0.35 + np.sin(c.a * 9 + seed * 2) * 0.25
    sd = c.r - (r + (ang_n * 1.5 + nz * 1.1) * min(1.0, t * 2.2))
    rr = np.random.default_rng(seed + i)
    sat = np.full(c.x.shape, 9.0, np.float32)
    for k in range(5 if t > 0.2 else 0):
        a = rr.uniform(0, 2 * math.pi)
        d = r + rr.uniform(1.4, 3.0) * min(1.0, t * 2)
        sat = np.minimum(sat, sd_disc(c, d * math.cos(a), d * math.sin(a), rr.uniform(0.6, 1.2)))
    sd = np.minimum(sd, sat)
    if t > 0.6:
        sd = np.minimum(sd, sd_capsule(c, -1.0, r * 0.4, -1.0, r * 0.4 + 3.0 * (t - 0.5) * 2, 1.4, 0.9))
    sh = np.clip(0.4 + 0.5 * (-(c.x + c.y) / 10.0) + 0.1 * nz, 0, 0.999)
    img = solid(c, sd, sh, rmp, outline=None)
    hl = (np.abs(c.x + 1.6) < 0.7) & (np.abs(c.y + 1.6) < 0.7) & (sd < -0.6)
    img[hl, :3] = hexc(drip_col)
    return img


@particle("ink_splat", "tool", 6, group="ink", behaviour="A blot of black ink: starts as a dot, spreads into an irregular splat with satellite droplets and a drip, with a wet highlight; hardly moves, translucent, fades as it dries.",
          gravity=0.0, spin=0.0, drag=0.85, size=[0.2, 0.46], life=[20, 36], anim="age", curve="ease_out", fade=[0.0, 0.45])
def ink_splat(c, i, n, rng):
    return _splat(c, i, n, ramp("#05050c", "#0d0d1c", "#1a1a32", "#2c2c52"), 61, "#6a6fa8")


@particle("paint_splat", "tool", 6, group="ink", tint=True, behaviour="A splat of paint in a neutral light tone (the theme colour dyes it) with satellite drops and a drip; spreads and settles, translucent.",
          gravity=0.02, spin=0.0, drag=0.85, size=[0.22, 0.5], life=[20, 36], anim="age", curve="ease_out", fade=[0.0, 0.45])
def paint_splat(c, i, n, rng):
    return _splat(c, i, n, ramp("#aab1c4", "#cfd5e4", "#eef1f8", "#ffffff"), 83, "#ffffff")


@particle("thread", "tool", 6, group="thread", tint=True, behaviour="A loose thread: a thin curling strand with a bright edge that wriggles while it drifts; tintable, translucent.",
          gravity=0.03, sway=0.01, jitter=0.003, size=[0.34, 0.3], life=[28, 46], spin=0.03)
def thread(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    pts = []
    for k in range(18):
        s = k / 17.0
        x = lerp(-6.5, 6.5, s)
        y = 2.8 * math.sin(s * 2 * math.pi * 1.1 + ph) + 1.2 * math.sin(s * 9 + ph * 2) * (0.4 + 0.6 * s)
        pts.append((x, y))
    sd = np.full(c.x.shape, 9.0, np.float32)
    for k in range(17):
        sd = np.minimum(sd, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], 1.2, 1.2))
    sh = np.clip(0.55 + 0.4 * np.sin(c.x * 0.7 + ph), 0, 0.999)
    img = solid(c, sd, sh, ramp("#9aa0b4", "#c4c9d8", "#e8ebf4", "#ffffff"), outline=None)
    return img


@particle("music_note", "tool", 6, group="music", tint=True, behaviour="A music note (a single eighth on three frames, a beamed pair on three, picked at random per particle) that bobs while it floats up and fades; tintable, translucent.",
          gravity=-0.05, sway=0.012, spin=0.02, drag=0.96, anim="random", size=[0.3, 0.28], life=[26, 44], curve="hold", fade=[0.12, 0.4])
def music_note(c, i, n, rng):
    wob = 0.12 * math.sin(i * 1.7)
    rmp = ramp("#aeb6cc", "#d4daea", "#f2f4fa", "#ffffff")
    if i < 3:
        head = sd_ellipse(c, -1.6, 3.8, 2.7, 2.0, -0.45 + wob)
        stem = sd_capsule(c, 0.9, 3.2, 0.9, -5.6, 1.3)
        flag = np.minimum(sd_capsule(c, 0.9, -5.6, 3.8, -3.6, 1.3), sd_capsule(c, 3.8, -3.6, 3.4, -0.8, 1.2))
        sd = np.minimum(head, np.minimum(stem, flag))
    else:
        h1 = sd_ellipse(c, -3.8, 4.4, 2.5, 1.9, -0.45 + wob)
        h2 = sd_ellipse(c, 2.6, 2.4, 2.5, 1.9, -0.45 - wob)
        st1 = sd_capsule(c, -1.5, 3.9, -1.5, -4.2, 1.2)
        st2 = sd_capsule(c, 4.9, 1.9, 4.9, -6.2, 1.2)
        beam = sd_capsule(c, -1.5, -4.2, 4.9, -6.2, 1.9)
        sd = np.minimum(np.minimum(h1, h2), np.minimum(np.minimum(st1, st2), beam))
    return solid(c, sd, np.clip(0.55 + 0.4 * (-c.y / 9.0), 0, 0.999), rmp, outline="#6c7490")


@particle("chain_link", "tool", 6, group="mechanism", behaviour="An iron chain link that turns about its long axis: a full oval ring, a narrow ring, then the edge-on bar; dark steel with a bright edge. Falls with a spin, translucent, hits the ground.",
          gravity=0.45, spin=0.18, size=[0.26, 0.22], life=[20, 34], collide=True)
def chain_link(c, i, n, rng):
    ph = i * math.pi / n
    f = abs(math.cos(ph))
    b = max(1.1, 3.4 * f)
    ang = 0.6
    sd = sd_ellipse(c, 0, 0, 5.4, b, ang)
    if b > 1.8:
        ringv = sd_ring(sd, 2.0)
    else:
        ringv = sd
    x, y = c.rot(ang)
    return solid(c, ringv, np.clip(0.5 + 0.4 * (-y / 5.0) + 0.1 * np.sin(x), 0, 0.999), ramp("#34373f", "#575c6c", "#868ca2", "#c0c6d8"), outline="#12131a")


# ---------------------------------------------------------------- the darker ones
@particle("blood_drop", "water", 4, group="blood", behaviour="A stylised blood drop: a deep-red teardrop with a wet highlight that falls fast and, on its last frame, flattens into a small splat with two satellites. Opaque look, translucent render.",
          gravity=0.65, drag=0.99, size=[0.16, 0.16], life=[14, 26], anim="age", curve="hold", fade=[0.0, 0.2])
def blood_drop(c, i, n, rng):
    rmp = ramp("#3e050e", "#70101e", "#a21a2e", "#d23a4c")
    if i == n - 1:
        sd = np.minimum(sd_ellipse(c, 0, 4.0, 4.8, 1.4), np.minimum(sd_disc(c, -4.6, -0.6, 1.0), sd_disc(c, 4.6, 0.2, 0.9)))
        img = solid(c, sd, np.clip(0.5 + (-(c.y - 3) / 4.0), 0, 0.999), rmp, outline="#240309")
        return img
    s = 1.0 + 0.26 * i
    pts = [(0, -6.4 * s), (2.3, -0.4 * s), (2.9, 2.0), (1.5, 4.6), (0, 5.4), (-1.5, 4.6), (-2.9, 2.0), (-2.3, -0.4 * s)]
    sd = np.minimum(sd_poly(c, pts), sd_disc(c, 0, 2.2, 3.0 - 0.3 * i))
    img = solid(c, sd, np.clip(0.3 + 0.5 * np.where(c.x < 0.5, 1.0, 0.4) + 0.15 * (-c.y / 8.0), 0, 0.999), rmp, outline="#240309")
    add_px(img, [(int(c.n / 2 - 2), int(c.n / 2 + 1)), (int(c.n / 2 - 2), int(c.n / 2 + 2))], "#ffb0ba", 255)
    return img


@particle("poison_cloud", "tool", 6, group="poison", px=32, behaviour="A toxic cloud: lumpy green fog with rising bubbles and a sick yellow rim, billowing slowly; spreads while it thins, translucent but full-bright so it reads in the dark.",
          gravity=-0.02, drag=0.97, spin=0.01, size=[0.5, 1.0], life=[28, 48], curve="ease_out", fade=[0.2, 0.6], anim="age", light="full")
def poison_cloud(c, i, n, rng):
    t = i / float(n - 1)
    nz = vnoise(c.n, 3, np.random.default_rng(500 + i), 4)
    r = lerp(6.5, 13.0, t)
    shape = smoothstep(r + 3, r - 5, c.r + (nz - 0.5) * 9.0)
    v = np.clip(shape * (0.6 + 0.6 * vnoise(c.n, 5, np.random.default_rng(600 + i), 2)), 0, 1)
    rmp = ramp("#173a16", "#245c22", "#3a8a2e", "#6ac048", "#b6ee78")
    img = np.zeros((c.n, c.n, 4), np.float32)
    k = np.clip(np.floor((v * 0.8 + 0.1 * nz) * len(rmp)), 0, len(rmp) - 1).astype(int)
    img[..., :3] = rmp[k]
    q = np.floor(v * 5 + 0.5) / 5
    q[v < 0.12] = 0
    img[..., 3] = q * 215 * lerp(1.0, 0.7, t)
    rr = np.random.default_rng(700 + i)
    for _ in range(5):
        bx, by, br = rr.uniform(-7, 7), rr.uniform(-7, 7) - 3 * t, rr.uniform(1.0, 2.0)
        ring = np.abs(np.sqrt((c.x - bx) ** 2 + (c.y - by) ** 2) - br) < 0.6
        ring &= (img[..., 3] > 60)
        img[ring, :3] = hexc("#d6f9a0")
        img[ring, 3] = 235
    return img


@particle("shadow_tendril", "tool", 6, group="shadow", px=32, behaviour="A curling tendril of shadow that reaches up from the bottom and coils at the tip, with a violet edge light; sways and retracts, translucent, never lit.",
          gravity=-0.03, drag=0.93, sway=0.004, size=[0.6, 0.8], life=[20, 34], light="normal", blend="translucent", curve="hold", fade=[0.15, 0.4], anim="loop", loop=3)
def shadow_tendril(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    pts = []
    for k in range(20):
        s = k / 19.0
        y = lerp(14.0, -9.0, s)
        x = 3.5 * math.sin(s * 3.4 + ph) * s + 0.0
        if s > 0.8:
            a = (s - 0.8) / 0.2 * 2.6 + ph * 0.3
            x += 2.4 * math.sin(a) * 1.0
            y += 2.2 * (1 - math.cos(a))
        pts.append((x, y))
    sd = np.full(c.x.shape, 9.0, np.float32)
    for k in range(19):
        sd = np.minimum(sd, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], lerp(5.2, 0.9, k / 18.0), lerp(5.2, 0.9, (k + 1) / 18.0)))
    nz = lumpy(c, np.random.default_rng(800 + i), 4, 1)
    sd = sd + nz * 0.7
    img = solid(c, sd, np.clip(0.15 + 0.4 * np.clip((-sd) / 2.5, 0, 1), 0, 0.999), ramp("#07050c", "#120a20", "#1f1238", "#33205a"), outline="#6a45b8", outline_alpha=210)
    return img


@particle("lava_blob", "tool", 6, group="fire", behaviour="A bubble of molten rock: dark crust plates over a bright orange core with a yellow hot spot, swelling and cooling as it falls; lit by its own glow, translucent.",
          gravity=0.35, drag=0.97, spin=0.0, light="full", size=[0.24, 0.16], life=[18, 32], anim="loop", loop=3)
def lava_blob(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    r = 4.4 + 0.5 * math.sin(ph)
    nz = lumpy(c, np.random.default_rng(90 + i), 3, 2)
    sd = c.r - (r + nz * 1.0)
    core = np.clip(1.0 - c.r / (r + 0.5), 0, 1)
    crust = vnoise(c.n, 3, np.random.default_rng(120), 3)
    plate = crust > (0.5 + 0.1 * math.sin(ph))
    sh = np.where(plate, 0.18 + 0.2 * core, 0.5 + 0.49 * core)
    img = solid(c, sd, np.clip(sh, 0, 0.999), ramp("#1d0a06", "#4a1608", "#d8431a", "#ff8a1f", "#ffd24a"), outline="#2a0c06")
    halo = glow(c, np.exp(-((c.r / 5.6) ** 2)) * 0.45, ramp("#7a1a0c", "#d8431a", "#ff8a1f"), levels=3, amax=120)
    return over(halo, img)


@particle("electric_orb", "glow", 8, group="storm", tint=True, behaviour="A small orb of static with short jagged arcs that jump to new places every frame; hovers and jitters, additive, very bright.",
          gravity=0.0, jitter=0.012, drag=0.9, size=[0.26, 0.12], life=[8, 16], anim="loop", loop=1, curve="hold")
def electric_orb(c, i, n, rng):
    rr = np.random.default_rng(1000 + i * 13)
    s = np.full(c.x.shape, 9.0, np.float32)
    for k in range(3):
        a = rr.uniform(0, 2 * math.pi)
        pts = [(0.0, 0.0)]
        for q in range(1, 4):
            r = q * 2.7
            pts.append((r * math.cos(a + rr.uniform(-0.5, 0.5)), r * math.sin(a + rr.uniform(-0.5, 0.5))))
        for q in range(3):
            s = np.minimum(s, sd_capsule(c, pts[q][0], pts[q][1], pts[q + 1][0], pts[q + 1][1], 0.8, 0.5))
    core = np.exp(-((c.r / 2.2) ** 2)) * 1.1
    arcs = np.clip((0.5 - s) / 0.7, 0, 1)
    return glow(c, np.maximum(core, arcs) + np.exp(-((c.r / 4.8) ** 2)) * 0.25, ramp("#7da0e8", "#b4ccff", "#e8f1ff", "#ffffff"), levels=4, amax=250, floor=0.15)

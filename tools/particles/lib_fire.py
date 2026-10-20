"""Fire, heat, smoke and ash particles."""
import math

import numpy as np

from artkit import (Canvas, add_px, blank, cov, glow, hexc, lerp, lumpy, over, ramp, sd_capsule, sd_disc, sd_ellipse, sd_poly, smoothstep, solid, union, vnoise)
from registry import particle

EMBER_RAMP = ramp("#7a1a0c", "#d8431a", "#ff8a1f", "#ffd24a", "#fff6c8")
GLOW_FIRE = ramp("#5c0f08", "#b3300f", "#ff7a1c", "#ffc247", "#fff1b8")


@particle("ember", "fire", 6, group="fire", behaviour="A small glowing coal that flickers while it drifts up; gets hot-yellow in the middle, fades to red. Rises, light drag, additive, full-bright.",
          gravity=-0.12, size=[0.17, 0.05], life=[16, 30])
def ember(c, i, n, rng):
    rd = 2.5 + 0.55 * math.sin(i * 1.9) + (0.3 if i % 2 else 0)
    d = (np.abs(c.x + 0.2 * math.sin(i)) + np.abs(c.y)) - rd
    halo = glow(c, np.exp(-(c.r / 4.4) ** 2) * 0.55, GLOW_FIRE, levels=3, amax=150)
    core = solid(c, d, np.clip(1.0 - (np.abs(c.x) + np.abs(c.y)) / (rd + 0.6) + 0.12 * math.sin(i * 2.3), 0, 0.999), EMBER_RAMP, outline="#5c0f08", outline_alpha=210)
    img = over(halo, core)
    if i % 3 == 1:
        add_px(img, [(int(c.n / 2 + 3), int(c.n / 2 - 4))], "#ffc247", 230)
    if i % 3 == 2:
        add_px(img, [(int(c.n / 2 - 4), int(c.n / 2 - 2))], "#ff7a1c", 230)
    return img


@particle("cinder", "fire", 6, group="fire", behaviour="A burnt-out lump with glowing cracks that pulse; tumbles, falls slowly, translucent, lit by its own cracks.",
          gravity=0.18, spin=0.12, drag=0.96, blend="translucent", light="normal", size=[0.2, 0.12], life=[24, 40], fade=[0.04, 0.4])
def cinder(c, i, n, rng):
    ang = i * 0.5
    x, y = c.rot(ang)
    nz = lumpy(c, np.random.default_rng(7), 3, 2)
    d = np.sqrt(x * x + y * y) - (4.3 + 1.1 * nz)
    sh = np.clip(0.55 + 0.3 * (-(c.x + c.y) / 8.0) + 0.15 * nz, 0, 0.999)
    img = solid(c, d, sh, ramp("#1d1917", "#2d2623", "#40362f", "#5a4c42"), outline="#0d0a09")
    crack = vnoise(c.n, 4, np.random.default_rng(11), 2)
    pulse = 0.5 + 0.5 * math.sin(i * 1.1)
    thr = 0.60 - 0.1 * pulse
    mask = (d < 0) & (crack > thr) & (crack < thr + 0.07 + 0.05 * pulse)
    img[mask, :3] = hexc("#ff7a1c") if pulse < 0.6 else hexc("#ffc247")
    img[mask, 3] = 255
    return img


def _flame(c, i, n, rng, rmp, hot, seed):
    t = i / float(n)
    sway = 1.6 * math.sin(t * 2 * math.pi) + 0.6 * math.sin(t * 4 * math.pi + seed)
    top = -7.2 + 1.2 * math.sin(t * 2 * math.pi * 2 + seed)
    base = 6.2
    v = (c.y - top) / (base - top)                    # 0 at the tip, 1 at the base
    w = 4.3 * (np.clip(v, 0, 1) ** 0.62) * (1 - 0.30 * np.clip(v, 0, 1) ** 4)
    axis = sway * (1 - np.clip(v, 0, 1)) ** 2
    nz = lumpy(c, np.random.default_rng(seed + i), 4, 1)
    sd = np.abs(c.x - axis) - w * (1 + 0.12 * nz)
    sd = np.where((v < 0) | (v > 1.0), 1.0, sd)
    sd = np.maximum(sd, np.where(c.y > base - 1.2, np.sqrt((c.x * 0.8) ** 2 + (c.y - (base - 3.2)) ** 2) - 3.9, -9))
    core = np.clip(1.0 - np.abs(c.x - axis) / np.maximum(w, 0.8), 0, 1) * (0.35 + 0.65 * np.clip(v, 0, 1))
    shade = np.clip(core * 1.15 + hot, 0, 0.999)
    img = solid(c, sd, shade, rmp, outline=None)
    halo = glow(c, np.exp(-(((c.x - axis * 0.5) / 6.0) ** 2 + ((c.y + 0.5) / 8.5) ** 2)) * 0.5, rmp, levels=3, amax=120)
    return over(halo, img)


@particle("flame_tongue", "fire", 6, group="fire", behaviour="A tongue of fire (red edge, orange body, white-yellow heart) that licks upwards and sways; rises, shrinks to nothing.",
          gravity=-0.16, size=[0.28, 0.08], life=[12, 22], jitter=0.003)
def flame_tongue(c, i, n, rng):
    return _flame(c, i, n, rng, ramp("#7a1a0c", "#c8330f", "#ff7a1c", "#ffc247", "#fff6c8"), 0.0, 3)


@particle("flame_soul", "fire", 6, group="fire", tint=True, behaviour="The same tongue in pale spirit-fire (white body, tintable): a cold flame for souls, ghosts and blue fire; drifts up with a little sway.",
          gravity=-0.12, size=[0.28, 0.07], life=[14, 24], sway=0.004)
def flame_soul(c, i, n, rng):
    return _flame(c, i, n, rng, ramp("#8d98b8", "#b9c4e0", "#dfe6f7", "#f6f9ff", "#ffffff"), 0.1, 9)


@particle("spark", "fire", 4, group="fire", behaviour="A hot spark: a bright four-ray burst that shortens as it cools; flies fast, falls a little, drag.",
          gravity=0.10, drag=0.9, size=[0.17, 0.04], life=[8, 16], fade=[0.0, 0.5], anim="age", curve="linear")
def spark(c, i, n, rng):
    L = [6.5, 5.0, 3.6, 2.2][i]
    w = [1.6, 1.4, 1.2, 1.0][i]
    sd = union(sd_capsule(c, -L, 0, L, 0, w, 0.2), sd_capsule(c, 0, -L, 0, L, w, 0.2),
               sd_capsule(c, -L * 0.55, -L * 0.55, L * 0.55, L * 0.55, 0.9, 0.2), sd_capsule(c, -L * 0.55, L * 0.55, L * 0.55, -L * 0.55, 0.9, 0.2))
    halo = glow(c, np.exp(-(c.r / 3.2) ** 2) * 0.7, ramp("#ff7a1c", "#ffc247", "#fff6c8"), levels=3, amax=170)
    core = solid(c, sd, np.clip(1.0 - c.r / (L + 1), 0, 0.999), ramp("#ff9a2a", "#ffd24a", "#fff3b0", "#ffffff"), outline=None)
    return over(halo, core)


@particle("ash_flake", "fire", 6, group="ash", behaviour="A thin grey ash flake that tumbles and flips as it sinks; matte, no glow.",
          gravity=0.12, spin=0.10, drag=0.95, sway=0.010, blend="translucent", light="normal", size=[0.2, 0.15], life=[40, 70], fade=[0.05, 0.4], curve="hold")
def ash_flake(c, i, n, rng):
    ph = i / float(n) * math.pi
    b = 0.9 + 3.4 * abs(math.cos(ph))
    sd = sd_ellipse(c, 0, 0, 5.0, b, 0.5 + 0.5 * math.sin(i * 0.9))
    shade = np.clip(0.55 + 0.3 * np.sin(c.x * 0.5 + i) + 0.0 * c.y, 0, 0.999)
    img = solid(c, sd, shade, ramp("#34323a", "#58565e", "#85838b", "#b3b1b8"), outline="#1e1d22")
    return img


@particle("soot_puff", "fire", 6, group="ash", behaviour="A dark soot puff that swells and thins out while rising slowly; dithered lumpy edge, translucent, never glows.",
          gravity=-0.03, drag=0.96, blend="translucent", light="normal", size=[0.28, 0.62], life=[26, 44], curve="ease_out", fade=[0.08, 0.7], anim="age")
def soot_puff(c, i, n, rng):
    t = i / float(n - 1)
    nz = vnoise(c.n, 3, np.random.default_rng(21 + i), 3)
    r = lerp(3.4, 7.2, t)
    inten = smoothstep(r + 1.5, r - 3.0, c.r + (nz - 0.5) * 5.0) * lerp(0.95, 0.45, t)
    return glow(c, inten, ramp("#0c0b10", "#18171d", "#262430", "#3a3744"), levels=4, amax=235, floor=0.12)


@particle("smoke_wisp", "fire", 6, group="ash", tint=True, behaviour="A curling wisp of smoke (light grey, tintable); rises in an S shape that unwinds, translucent, fades.",
          gravity=-0.04, drag=0.97, sway=0.006, blend="translucent", light="normal", size=[0.24, 0.5], life=[28, 48], curve="ease_out", fade=[0.1, 0.65], anim="age")
def smoke_wisp(c, i, n, rng):
    t = i / float(n - 1)
    pts = []
    for k in range(13):
        s = k / 12.0
        y = 7.0 - s * 14.0
        x = math.sin(s * math.pi * 1.6 + t * 2.4) * (2.2 + 2.0 * s)
        pts.append((x, y))
    sd = np.full(c.x.shape, 9.0, np.float32)
    for k in range(len(pts) - 1):
        wd = lerp(2.8, 1.0, k / 12.0) * lerp(1.0, 0.7, t)
        sd = np.minimum(sd, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], wd, lerp(2.8, 1.0, (k + 1) / 12.0) * lerp(1.0, 0.7, t)))
    nz = vnoise(c.n, 4, np.random.default_rng(31 + i), 2)
    inten = np.clip((1.2 - sd) / 2.6, 0, 1) * (0.65 + 0.5 * nz) * lerp(1.0, 0.55, t)
    return glow(c, inten, ramp("#8d8c96", "#b4b3bd", "#d4d3db", "#f2f2f6"), levels=4, amax=230, floor=0.14)

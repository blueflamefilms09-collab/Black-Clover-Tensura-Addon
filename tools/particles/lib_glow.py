"""Light and spirit particles: motes, glints, stars, wisps, rays, nebula, void, souls."""
import math

import numpy as np

from artkit import (Canvas, add_px, blank, cov, glow, hexc, lerp, lumpy, over, ramp, sd_capsule, sd_disc, sd_ellipse, sd_poly, sd_star, smoothstep, solid, union, vnoise)
from registry import particle

WHITE = ramp("#8f9bbd", "#bcc7e6", "#e6ecfb", "#ffffff")        # greyscale-white ramps keep the colour tint clean
WARM = ramp("#a9946a", "#d9c492", "#f5e8c2", "#ffffff")


def _arms(c, lh, lv, w, ang=0.0):
    x, y = c.rot(ang)
    s = np.full(c.x.shape, 9.0, np.float32)

    class _C:  # a canvas view in the rotated frame
        pass
    v = _C()
    v.x, v.y = x, y
    s = np.minimum(sd_capsule(v, 0, 0, lh, 0, w, 0.15), sd_capsule(v, 0, 0, -lh, 0, w, 0.15))
    s = np.minimum(s, np.minimum(sd_capsule(v, 0, 0, 0, lv, w, 0.15), sd_capsule(v, 0, 0, 0, -lv, w, 0.15)))
    return s


@particle("mote", "glow", 6, group="light", tint=True, behaviour="The basic light mote: a soft round glow with a hard white core that breathes; drifts, slowly rising, additive, the theme colour tints it.",
          size=[0.20, 0.06], gravity=-0.03, life=[16, 28])
def mote(c, i, n, rng):
    p = 1.0 + 0.2 * math.sin(i / float(n) * 2 * math.pi)
    inten = np.exp(-((c.r / (3.3 * p)) ** 2)) * 1.05
    img = glow(c, inten, WHITE, levels=5, amax=235, floor=0.1)
    core = solid(c, np.sqrt((c.x ** 2 + c.y ** 2)) - 1.35 * p, np.full(c.x.shape, 0.999), ramp("#ffffff"), outline=None)
    return over(img, core)


@particle("mote_soft", "glow", 4, group="light", tint=True, behaviour="A large dim glow without a core: an air-filling haze mote; very slow, long fade in and out, additive.",
          size=[0.34, 0.5], gravity=-0.01, drag=0.97, life=[26, 44], fade=[0.3, 0.55], anim="age", curve="ease_out")
def mote_soft(c, i, n, rng):
    p = 1.0 + 0.1 * i
    inten = np.exp(-((c.r / (5.0 * p)) ** 2)) * 0.62
    return glow(c, inten, WHITE, levels=6, amax=165, floor=0.08)


@particle("glint", "glow", 6, group="light", tint=True, behaviour="A lens glint: a thin four-armed cross that swells and snaps shut, with a tilt; sits where it is spawned, additive.",
          size=[0.30, 0.28], gravity=0.0, drag=0.9, life=[10, 18], anim="age", curve="pop", fade=[0.05, 0.4])
def glint(c, i, n, rng):
    t = (i + 0.5) / float(n)
    L = 1.5 + 6.2 * math.sin(math.pi * t)
    s = _arms(c, L, L * 0.82, 1.7 * math.sin(math.pi * t) + 0.5, ang=0.18 * (t - 0.5))
    inten = np.clip((0.7 - s) / 1.0, 0, 1)
    img = glow(c, inten, WHITE, levels=4, amax=250, floor=0.2)
    halo = glow(c, np.exp(-((c.r / 3.0) ** 2)) * 0.55 * math.sin(math.pi * t), WHITE, levels=3, amax=150)
    return over(halo, img)


@particle("star4", "glow", 6, group="light", tint=True, behaviour="A solid four-point star that turns a quarter turn while it twinkles and shrinks; slow rise, additive.",
          size=[0.26, 0.05], gravity=-0.05, spin=0.0, life=[18, 30], anim="age", curve="linear", fade=[0.05, 0.4])
def star4(c, i, n, rng):
    t = i / float(n)
    R = 7.0 * (1.0 - 0.3 * t) * (1.0 + 0.08 * math.sin(i * 2.0))
    sd = sd_star(c, 0, 0, 4, R, R * 0.42, ang=t * math.pi / 4)
    body = solid(c, sd, np.clip(1.0 - c.r / (R + 1), 0, 0.999), ramp("#aeb9d8", "#d6def2", "#f0f4ff", "#ffffff"), outline=None)
    halo = glow(c, np.exp(-((c.r / (R * 0.55)) ** 2)) * 0.5, WHITE, levels=3, amax=140)
    return over(halo, body)


@particle("gem_sparkle", "glow", 6, group="light", tint=True, behaviour="A tiny cut gem with three shaded facets that flashes a cross glint at its brightest frame; hovers, additive.",
          size=[0.24, 0.1], gravity=0.0, drag=0.9, life=[14, 24], anim="age", curve="pop")
def gem_sparkle(c, i, n, rng):
    pts = [(0, -5.0), (4.0, -1.5), (0, 5.2), (-4.0, -1.5)]
    sd = sd_poly(c, pts)
    img = solid(c, sd, np.where(c.x < -0.3, 0.2, np.where(c.x > 0.3, 0.55, 0.9)) * np.ones_like(c.x), ramp("#9aa5c6", "#c9d3ee", "#eef2ff", "#ffffff"), outline="#7482a8", outline_alpha=200)
    t = (i + 0.5) / n
    g = math.sin(math.pi * t)
    arms = _arms(c, 1.0 + 5.5 * g, 1.0 + 5.5 * g, 1.0)
    cross = glow(c, np.clip((0.6 - arms) / 1.0, 0, 1) * g, WHITE, levels=4, amax=255, floor=0.25)
    return over(img, cross)


@particle("mana_mote", "glow", 8, group="light", tint=True, behaviour="A mote of raw mana: a glow with a tiny satellite circling it; the satellite leaves a faint ring. Rises gently, additive.",
          size=[0.22, 0.08], gravity=-0.06, life=[20, 34], sway=0.003)
def mana_mote(c, i, n, rng):
    a = i / float(n) * 2 * math.pi
    base = glow(c, np.exp(-((c.r / 2.6) ** 2)), WHITE, levels=4, amax=230)
    ring = glow(c, np.clip(1.0 - np.abs(c.r - 5.2) / 0.9, 0, 1) * 0.35, WHITE, levels=2, amax=120, floor=0.1)
    sx, sy = 5.2 * math.cos(a), 5.2 * math.sin(a)
    sat = glow(c, np.exp(-(((c.x - sx) ** 2 + (c.y - sy) ** 2) / 1.6)), WHITE, levels=4, amax=255, floor=0.15)
    return over(ring, base, sat)


@particle("aura_wisp", "glow", 6, group="light", tint=True, behaviour="A comet-shaped aura wisp: a round head and a tail that waves; rises and bends, additive.",
          size=[0.34, 0.1], gravity=-0.10, sway=0.008, life=[16, 28], swirl=0.04, curve="linear")
def aura_wisp(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    pts = [(math.sin(ph + k * 0.7) * (0.4 + 0.35 * k), -4.0 + k * 1.7) for k in range(6)]
    s = sd_disc(c, pts[0][0], pts[0][1], 2.5)
    for k in range(5):
        s = np.minimum(s, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], lerp(3.6, 0.5, k / 4.0), lerp(3.6, 0.5, (k + 1) / 4.0)))
    img = glow(c, np.clip((1.3 - s) / 2.8, 0, 1) * np.clip(1.05 - (c.y + 4) / 14.0, 0.25, 1), WHITE, levels=5, amax=235, floor=0.12)
    return img


@particle("spirit_firefly", "glow", 8, group="light", tint=True, behaviour="A spirit firefly: a small warm body whose glow blinks on and off in a slow rhythm and whose wings flick; wanders on a lazy path, additive.",
          size=[0.20, 0.16], gravity=-0.01, sway=0.012, jitter=0.006, drag=0.95, life=[30, 54], curve="hold", fade=[0.15, 0.35], anim="loop", loop=2)
def spirit_firefly(c, i, n, rng):
    b = [0.15, 0.4, 0.85, 1.0, 0.85, 0.5, 0.2, 0.1][i]
    halo = glow(c, np.exp(-((c.r / (2.2 + 3.2 * b)) ** 2)) * (0.25 + 0.7 * b), WARM, levels=4, amax=230)
    body = solid(c, sd_ellipse(c, 0, 0.5, 2.2, 1.4), np.full(c.x.shape, 0.4 + 0.59 * b), WARM, outline=None)
    img = over(halo, body)
    if i % 2 == 0:
        add_px(img, [(int(c.n / 2 - 2), int(c.n / 2 - 2)), (int(c.n / 2 + 1), int(c.n / 2 - 2))], "#ffffff", 150)
    return img


@particle("dream_star", "glow", 6, group="light", behaviour="A pastel five-point star with two tiny sparkles that trade places; floats up and sways, twinkles, additive. Native lavender-pink.",
          size=[0.24, 0.08], gravity=-0.05, sway=0.01, life=[24, 40], curve="pulse", fade=[0.2, 0.4])
def dream_star(c, i, n, rng):
    t = i / float(n)
    R = 5.2 * (0.85 + 0.15 * math.sin(t * 2 * math.pi))
    sd = sd_star(c, 0, 0, 5, R, R * 0.45, ang=0.2 * math.sin(t * 2 * math.pi))
    sh = np.clip(0.5 + 0.5 * (-c.y / R) * 0.7 + 0.2 * np.sin(c.x * 0.6), 0, 0.999)
    body = solid(c, sd, sh, ramp("#a38be0", "#c9a9ee", "#f0b9e8", "#ffe4f5"), outline="#7a64c4", outline_alpha=190)
    halo = glow(c, np.exp(-((c.r / 5.5) ** 2)) * 0.5, ramp("#8f78d8", "#c9a0ee", "#f6c4ee"), levels=3, amax=130)
    img = over(halo, body)
    a = t * 2 * math.pi
    add_px(img, [(int(c.n / 2 + 6 * math.cos(a)), int(c.n / 2 + 6 * math.sin(a))), (int(c.n / 2 - 6 * math.cos(a)), int(c.n / 2 - 6 * math.sin(a)))], "#ffffff", 235)
    return img


@particle("holy_ray", "glow", 6, group="light", px=32, tint=True, behaviour="A sunburst of holy rays: eight shafts that turn slowly and breathe around a white core; stays in place, additive, long soft fade.",
          size=[0.9, 1.4], gravity=0.0, drag=0.9, life=[14, 22], curve="ease_out", fade=[0.1, 0.6], anim="age")
def holy_ray(c, i, n, rng):
    t = i / float(n - 1)
    rot = t * math.pi / 8
    lobe = (0.5 + 0.5 * np.cos(8 * (c.a - rot))) ** 7
    lobe2 = (0.5 + 0.5 * np.cos(4 * (c.a - rot * 1.7 + 0.4))) ** 11
    inten = (lobe * np.exp(-c.r / (7.0 + 5.0 * t)) * 1.3 + lobe2 * np.exp(-c.r / 12.0) * 0.5 + np.exp(-((c.r / 3.0) ** 2))) * lerp(1.0, 0.7, t)
    return glow(c, inten, WARM, levels=5, amax=240, floor=0.12)


@particle("nebula_puff", "glow", 6, group="light", px=32, behaviour="A cloud of star dust in violet, blue and pink: three drifting lobes of noise with bright pin stars; swells and thins, slow spin, additive.",
          size=[0.5, 0.9], gravity=-0.01, drag=0.97, spin=0.02, life=[30, 52], curve="ease_out", fade=[0.2, 0.6], anim="loop", loop=4)
def nebula_puff(c, i, n, rng):
    nz = vnoise(c.n, 3, np.random.default_rng(100 + i), 4)
    nz2 = vnoise(c.n, 5, np.random.default_rng(200 + i), 3)
    r = c.r / 14.0
    shape = smoothstep(1.0, 0.1, r + (nz - 0.5) * 0.8)
    v = np.clip(shape * (0.45 + nz2 * 0.9), 0, 1)
    hue = np.clip(0.2 + 0.6 * (nz2 * 0.7 + (c.x + c.y) / 40.0), 0, 0.999)
    img = np.zeros((c.n, c.n, 4), np.float32)
    rmp = ramp("#241046", "#4a1f8f", "#6d3fd6", "#4b7bf2", "#ff6fc8", "#ffd9f4")
    k = np.clip(np.floor((0.25 * v + hue * 0.75) * len(rmp)), 0, len(rmp) - 1).astype(int)
    img[..., :3] = rmp[k]
    q = np.floor(v * 5 + 0.5) / 5
    q[v < 0.12] = 0
    img[..., 3] = q * 235
    rr = np.random.default_rng(300 + i)
    for _ in range(5):
        px, py = int(rr.integers(6, 26)), int(rr.integers(6, 26))
        if img[py, px, 3] > 80:
            add_px(img, [(px, py)], "#ffffff", 255)
    return img


@particle("void_speck", "glow", 4, group="light", behaviour="A speck of nothing: a black core with a thin violet rim that pulses; sucks slowly sideways, translucent so it stays dark.",
          gravity=0.0, blend="translucent", light="normal", size=[0.14, 0.06], swirl=0.12, life=[18, 30], curve="linear", fade=[0.1, 0.4], anim="loop", loop=3)
def void_speck(c, i, n, rng):
    r = 2.1 + 0.5 * math.sin(i * 1.6)
    rim = glow(c, np.clip(1.0 - np.abs(c.r - (r + 1.2)) / 1.4, 0, 1), ramp("#2a1450", "#5a2ca3", "#9d6bf0"), levels=3, amax=230)
    core = solid(c, c.r - r, np.full(c.x.shape, 0.0), ramp("#06040a"), outline=None)
    return over(rim, core)


@particle("soul_wisp", "glow", 8, group="light", tint=True, behaviour="A ghostly wisp with a round head, two dark eyes that blink and a wavy tail; floats up with a lazy sway, additive.",
          size=[0.30, 0.16], gravity=-0.07, sway=0.010, life=[24, 40], curve="hold", fade=[0.2, 0.4])
def soul_wisp(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    pts = [(math.sin(ph + k * 0.9) * (0.3 + 0.3 * k), -2.0 + k * 1.9) for k in range(5)]
    s = sd_disc(c, 0, -3.0, 3.1)
    for k in range(4):
        s = np.minimum(s, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], lerp(4.6, 0.8, k / 3.0), lerp(4.6, 0.8, (k + 1) / 3.0)))
    inten = np.clip((1.0 - s) / 2.2, 0, 1) * np.clip(1.1 - (c.y + 5) / 14.0, 0.2, 1)
    img = glow(c, inten, ramp("#9fb2cf", "#c6d4ea", "#e8f0fb", "#ffffff"), levels=5, amax=235, floor=0.12)
    if i != 4:
        add_px(img, [(int(c.n / 2 - 2), int(c.n / 2 - 4)), (int(c.n / 2 + 1), int(c.n / 2 - 4))], "#2a3350", 245)
    return img

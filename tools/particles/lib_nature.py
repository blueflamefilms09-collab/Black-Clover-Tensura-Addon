"""Shards, feathers, petals, leaves, seeds, spores, water, snow and earth particles."""
import math

import numpy as np

from artkit import (Canvas, add_px, blank, cov, glow, hexc, lerp, lumpy, over, ramp, sd_capsule, sd_disc, sd_ellipse, sd_poly, sd_ring, smoothstep, solid, union, vnoise)
from registry import particle


def _tf(pts, ang, sx=1.0, sy=1.0, ox=0.0, oy=0.0):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(ox + (x * sx) * ca - (y * sy) * sa, oy + (x * sx) * sa + (y * sy) * ca) for x, y in pts]


def _facets(c, pts, ang, rmp, outline, glint_frame=None, i=0, a_in=255):
    """A convex-ish polygon shaded in three facets by the side of the (rotated) long axis a pixel is on."""
    sd = sd_poly(c, pts)
    x, y = c.rot(ang)
    sh = np.where(x < -0.8, 0.12, np.where(x < 0.9, 0.62, 0.36)) + 0.28 * np.clip(-y / 8.0, 0, 1)
    img = solid(c, sd, np.clip(sh, 0, 0.999), rmp, outline=outline)
    if a_in < 255:
        img[(sd < 0), 3] = a_in
    return img, sd


# ---------------------------------------------------------------- shards
@particle("shard_ice", "shard", 6, group="shard", behaviour="A sliver of ice with three facets that spins end over end and flashes a glint on one frame; falls with a little bounce, translucent.",
          gravity=0.30, spin=0.24, size=[0.22, 0.16], life=[22, 38])
def shard_ice(c, i, n, rng):
    ang = i * math.pi / 3.0 + 0.4
    pts = _tf([(0, -6.8), (2.5, -1.2), (1.7, 6.2), (-1.5, 5.4), (-2.7, -0.4)], ang)
    img, sd = _facets(c, pts, ang, ramp("#4f7fc4", "#86bdee", "#c6eaff", "#ffffff"), "#33579a", i=i)
    if i == 2:
        add_px(img, [(int(c.n / 2 - 1), int(c.n / 2 - 3)), (int(c.n / 2 - 2), int(c.n / 2 - 3)), (int(c.n / 2 - 1), int(c.n / 2 - 4))], "#ffffff", 255)
    return img


@particle("shard_crystal", "shard", 6, group="shard", tint=True, behaviour="A faceted crystal chunk (six-sided, light neutral facets so the theme colour reads) that turns slowly; drifts down, glints, translucent.",
          gravity=0.16, spin=0.12, size=[0.24, 0.18], life=[26, 44])
def shard_crystal(c, i, n, rng):
    ang = i * math.pi / 6.0
    pts = _tf([(0, -6.4), (3.9, -3.2), (3.9, 2.8), (0, 6.2), (-3.9, 2.8), (-3.9, -3.2)], ang * 0.6)
    sd = sd_poly(c, pts)
    x, y = c.rot(ang * 0.6)
    face = np.where(y < -2.2, 0.95, np.where(x < 0.0, 0.42, 0.68))
    sh = np.clip(face - 0.1 * np.clip(y / 7.0, 0, 1), 0, 0.999)
    img = solid(c, sd, sh, ramp("#7a84a6", "#a6b0d0", "#d0d8ee", "#f4f7ff"), outline="#586288")
    img = img.copy()
    edge = (np.abs(x) < 0.45) & (sd < 0) & (y > -2.2)
    img[edge, :3] = hexc("#e6ecfb")
    if i % 3 == 1:
        add_px(img, [(int(c.n / 2 - 2), int(c.n / 2 - 3))], "#ffffff", 255)
    return img


@particle("shard_glass", "shard", 6, group="shard", tint=True, behaviour="A broken pane fragment: a see-through triangle with bright edges and a diagonal shine that slides across it; tumbles and falls, translucent.",
          gravity=0.34, spin=0.26, size=[0.24, 0.2], life=[20, 34])
def shard_glass(c, i, n, rng):
    ang = i * math.pi / 3.2 + 0.2
    pts = _tf([(-5.0, 4.0), (5.4, 1.2), (-1.0, -6.0)], ang)
    sd = sd_poly(c, pts)
    x, y = c.rot(ang)
    img = solid(c, sd, np.full(c.x.shape, 0.55), ramp("#a8b7d6", "#d9e4f6", "#ffffff"), outline="#eef4ff", outline_alpha=255)
    inside = sd < 0
    img[inside, 3] = 62
    edge = (sd > -1.2) & inside
    img[edge, 3] = 215
    img[edge, :3] = hexc("#f4f8ff")
    shine = inside & (np.abs((x + y * 0.4) - (-4 + i * 1.7)) < 0.9)
    img[shine, 3] = 235
    img[shine, :3] = hexc("#ffffff")
    return img


# ---------------------------------------------------------------- feathers
def _feather(c, i, n, rmp, spine, outline, curl=0.5, glow_rmp=None):
    ph = i / float(n) * 2 * math.pi
    ang = -0.9 + 0.7 * math.sin(ph) + i * 0.15
    sq = 0.4 + 0.6 * abs(math.cos(ph * 0.5 + 0.4))
    ca, sa = math.cos(ang), math.sin(ang)
    pts = []
    for k in range(15):
        s_ = k / 14.0
        px = lerp(-7.0, 7.0, s_)
        py = curl * 2.4 * math.sin(s_ * math.pi)
        pts.append((px * ca - py * sa, px * sa + py * ca))
    sd = np.full(c.x.shape, 9.0, np.float32)
    for k in range(14):
        s0, s1 = k / 14.0, (k + 1) / 14.0
        w0 = 6.4 * math.sin(math.pi * min(1.0, s0 * 1.04) ** 0.7) * sq
        w1 = 6.4 * math.sin(math.pi * min(1.0, s1 * 1.04) ** 0.7) * sq
        sd = np.minimum(sd, sd_capsule(c, pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], max(0.8, w0), max(0.8, w1)))
    u, v = c.rot(ang)
    sd = sd + 0.5 * np.sign(np.sin((u - v * 0.8) * 1.5)) * 0.5
    sh = np.clip(0.58 + 0.12 * np.sin((u - v * 0.8) * 1.5) + 0.08 * np.sign(v), 0, 0.999)
    img = solid(c, sd, sh, rmp, outline=outline)
    sp = sd_capsule(c, pts[0][0], pts[0][1], pts[-1][0], pts[-1][1], 0.9)
    m = (sp < 0.25) & (sd < 0)
    img[m, :3] = hexc(spine)
    img[m, 3] = 255
    if glow_rmp is not None:
        halo = glow(c, np.clip((2.8 - sd) / 3.2, 0, 1) * 0.55, glow_rmp, levels=3, amax=150)
        img = over(halo, img)
    return img


@particle("feather_white", "feather", 6, group="feather", tint=True, behaviour="A white feather that turns over as it sways down; flat shaded, a grey spine and finely notched barbs, slow fall.",
          size=[0.34, 0.3])
def feather_white(c, i, n, rng):
    return _feather(c, i, n, ramp("#a9a7b8", "#cbc9d8", "#e8e7f0", "#ffffff"), "#8a879b", "#7b788c", 0.4)


@particle("feather_black", "feather", 6, group="feather", behaviour="A black feather with a blue-violet sheen on the edge; same lazy sway and fall as the white one, never glows.",
          size=[0.34, 0.3])
def feather_black(c, i, n, rng):
    return _feather(c, i, n, ramp("#0d0c12", "#1b1a24", "#2b2a3a", "#4a4766"), "#5e5a82", "#04030a", 0.4)


@particle("feather_fire", "feather", 6, group="feather", behaviour="A feather of living fire (red edge, orange vane, yellow spine) that trails a soft glow; floats up with a flutter and burns out, additive.",
          gravity=-0.05, sway=0.012, light="full", blend="add", size=[0.36, 0.14], life=[28, 46], curve="linear", fade=[0.08, 0.5])
def feather_fire(c, i, n, rng):
    return _feather(c, i, n, ramp("#8f1a0a", "#d8431a", "#ff8a1f", "#ffd24a"), "#fff2b0", "#5c0f08", 0.5, glow_rmp=ramp("#7a1a0c", "#d8431a", "#ff8a1f"))


# ---------------------------------------------------------------- flora
def _flat_petal(c, i, n, rmp, outline, notch=False):
    ph = i / float(n) * 2 * math.pi
    ang = 0.5 + 0.9 * math.sin(ph * 0.5 + 0.5)
    sq = max(0.3, 0.35 + 0.65 * abs(math.cos(ph)))
    x, y = c.rot(ang)
    y = y / sq
    L, W = 5.8, 3.4
    u = (y + L) / (2 * L)                                # 0 at the base point, 1 at the tip
    w = W * np.sin(np.clip(u, 0, 1) ** 0.8 * math.pi * 0.92) ** 0.9
    sd = np.abs(x) - w
    sd = np.where((u < 0) | (u > 1), 1.0, sd)
    sh = np.clip(0.5 + 0.35 * (x / 4.0) + 0.2 * (-y / 8.0), 0, 0.999)
    img = solid(c, sd, sh, rmp, outline=outline)
    if notch:
        k = (u > 0.84) & (np.abs(x) < 0.8 * (u - 0.8) * 5.0)
        img[k, 3] = 0
    return img


@particle("petal", "flora", 6, group="flora", tint=True, behaviour="A soft petal in a neutral light tone so the theme colour dyes it; flips as it flutters down, a gentle sway, translucent.",
          size=[0.26, 0.22])
def petal(c, i, n, rng):
    return _flat_petal(c, i, n, ramp("#cdb8c4", "#e6d4dc", "#f8eef2", "#ffffff"), "#b69aa8")


@particle("petal_cherry", "flora", 6, group="flora", behaviour="A cherry blossom petal (pink, notched at the tip, darker at the base) that spins and sways down; the Cherry Blossom magic's own.",
          size=[0.26, 0.22], sway=0.016, life=[36, 62])
def petal_cherry(c, i, n, rng):
    return _flat_petal(c, i, n, ramp("#e0709c", "#f093b4", "#ffb6cf", "#ffe0ea"), "#c05882", notch=True)


def _leaf(c, i, n, rmp, outline, vein):
    ph = i / float(n) * 2 * math.pi
    ang = 0.8 + 1.0 * math.sin(ph * 0.5 + 0.3)
    sq = 0.4 + 0.6 * abs(math.cos(ph))
    x, y = c.rot(ang)
    y = y / max(0.3, sq)
    u = (y + 6.2) / 12.4
    w = 3.8 * np.maximum(np.sin(np.clip(u, 0, 1) * math.pi), 0) ** 0.75 * (1.1 - 0.35 * np.clip(u, 0, 1))
    sd = np.abs(x) - w
    sd = np.where((u < 0) | (u > 1), 1.0, sd)
    sh = np.clip(0.55 + 0.3 * np.where(x > 0, 1, -0.4) * np.clip(1 - np.abs(x) / 4.0, 0, 1), 0, 0.999)
    img = solid(c, sd, sh, rmp, outline=outline)
    mid = (np.abs(x) < 0.5) & (sd < 0)
    img[mid, :3] = hexc(vein)
    for t in (-3.0, 0.0, 3.0):
        vm = (np.abs(np.abs(x) - np.abs(y - t) * 0.8 - 0.3) < 0.35) & (sd < 0) & (np.abs(y - t) < 2.2) & (y > -5)
        img[vm, :3] = hexc(vein)
    stem = sd_capsule(type("V", (), dict(x=x, y=y))(), 0, 6.2, 0, 7.8, 0.9)
    m = (stem < 0.2) & (sd >= 0)
    img[m, :3] = hexc(outline)
    img[m, 3] = 255
    return img


@particle("leaf_green", "flora", 6, group="flora", behaviour="A green leaf with a pale midrib and veins; turns over as it tumbles down, quicker fall than a petal, translucent.",
          gravity=0.10, size=[0.30, 0.26])
def leaf_green(c, i, n, rng):
    return _leaf(c, i, n, ramp("#2c6428", "#46903c", "#6dbb55", "#a0dd78"), "#1d441b", "#cdeea0")


@particle("leaf_autumn", "flora", 6, group="flora", behaviour="An autumn leaf (red, orange and gold) with dark veins, same tumble as the green one but drier and slower.",
          gravity=0.08, size=[0.30, 0.26])
def leaf_autumn(c, i, n, rng):
    return _leaf(c, i, n, ramp("#8a2410", "#c0501a", "#e6841f", "#f6bc3c"), "#5e1a0b", "#6a1c0c")


@particle("seed", "flora", 6, group="flora", behaviour="A winged tree seed (maple key): a seed and one long wing that whirls around the seed like a helicopter blade while it sinks.",
          gravity=0.05, drag=0.9, spin=0.0, sway=0.004, size=[0.3, 0.26], life=[30, 52])
def seed(c, i, n, rng):
    ang = i / float(n) * math.pi * 2
    wing_pts = _tf([(0.5, -1.3), (3.6, -3.4), (7.0, -2.8), (7.8, 0.4), (4.0, 1.8), (0.5, 1.3)], ang)
    wing = sd_poly(c, wing_pts)
    body = sd_disc(c, 0.0, 0.0, 2.0)
    x, y = c.rot(ang)
    vein = np.abs(y + 1.2 - 0.18 * x) < 0.45
    img = solid(c, np.minimum(wing, body), np.where(body < wing, 0.2, np.clip(0.78 - 0.1 * y, 0, 0.999)) * np.ones_like(c.x),
                ramp("#6a4a28", "#a0763e", "#cfa764", "#ecd49a"), outline="#4a3018")
    m = vein & (wing < 0) & (body > 0.3) & (x > 1.5)
    img[m, :3] = hexc("#8a5f30")
    return img


@particle("spore", "flora", 6, group="flora", behaviour="A drifting spore: a pale yellow-green puff with a few dust specks orbiting it; floats up and wobbles, additive, soft.",
          gravity=-0.03, sway=0.01, jitter=0.004, light="full", blend="add", size=[0.22, 0.12], life=[28, 48], curve="hold", fade=[0.2, 0.4])
def spore(c, i, n, rng):
    ph = i / float(n) * 2 * math.pi
    base = glow(c, np.exp(-((c.r / 2.9) ** 2)) * 1.05, ramp("#5f7d2a", "#8fb43c", "#c8e866", "#f4ffb8"), levels=4, amax=225)
    img = base
    for k in range(4):
        a = ph + k * math.pi / 2
        add_px(img, [(int(c.n / 2 + 4.6 * math.cos(a)), int(c.n / 2 + 4.6 * math.sin(a)))], "#d6f08a", 220)
    return img


# ---------------------------------------------------------------- water
@particle("bubble", "water", 6, group="water", behaviour="A soap bubble: a thin blue ring with a bright glint, wobbling and growing slightly; the last frame is the pop (a ring of droplets). Rises slowly, translucent.",
          gravity=-0.06, sway=0.008, size=[0.2, 0.3], life=[22, 40], curve="linear", fade=[0.05, 0.12], anim="age")
def bubble(c, i, n, rng):
    if i == n - 1:
        img = blank(c.n)
        for k in range(8):
            a = k * math.pi / 4 + 0.2
            add_px(img, [(int(c.n / 2 + 6 * math.cos(a)), int(c.n / 2 + 6 * math.sin(a)))], "#bfe4ff", 230)
        for k in range(8):
            a = k * math.pi / 4 + 0.6
            add_px(img, [(int(c.n / 2 + 3.4 * math.cos(a)), int(c.n / 2 + 3.4 * math.sin(a)))], "#e8f6ff", 160)
        return img
    r = 4.4 + 0.25 * i + (0.5 if i % 2 else 0.0)
    ry = r - (0.6 if i % 2 else 0.0)
    sd = sd_ellipse(c, 0, 0, r, ry)
    ringv = sd_ring(sd, 1.1)
    img = solid(c, ringv, np.clip(0.5 - c.y / 12.0 + 0.25 * np.sign(-c.x - c.y) * 0.5, 0, 0.999), ramp("#4f86c6", "#8cc2f0", "#cfeaff", "#ffffff"), outline=None)
    fill = glow(c, np.where(sd < 0, 0.18, 0.0), ramp("#9fd0f8"), levels=1, amax=70, floor=0.05)
    img = over(fill, img)
    add_px(img, [(int(c.n / 2 - 3), int(c.n / 2 - 3)), (int(c.n / 2 - 2), int(c.n / 2 - 4)), (int(c.n / 2 - 3), int(c.n / 2 - 2))], "#ffffff", 255)
    return img


@particle("droplet", "water", 4, group="water", behaviour="A water drop: a teardrop that stretches as it falls and, on the last frame, flattens into a splash ring with two satellites. Falls fast, translucent.",
          gravity=0.7, drag=0.99, size=[0.16, 0.16], life=[14, 26], anim="age", curve="hold", fade=[0.0, 0.2])
def droplet(c, i, n, rng):
    rmp = ramp("#2c62ac", "#5898de", "#9bcfff", "#e8f7ff")
    if i == n - 1:
        sd = np.minimum(sd_ellipse(c, 0, 4.0, 5.2, 1.5), np.minimum(sd_disc(c, -5.0, -0.5, 1.1), sd_disc(c, 5.0, 0.0, 1.0)))
        return solid(c, sd, np.clip(0.55 + (-(c.y - 3) / 4.0), 0, 0.999), rmp, outline="#1f4a86")
    s = 1.0 + 0.28 * i
    pts = [(0, -6.4 * s), (2.4, -0.4 * s), (3.0, 2.0), (1.6, 4.6), (0, 5.4), (-1.6, 4.6), (-3.0, 2.0), (-2.4, -0.4 * s)]
    sd = sd_poly(c, pts)
    sd = np.minimum(sd, sd_disc(c, 0, 2.2, 3.1 - 0.35 * i))
    return solid(c, sd, np.clip(0.35 + 0.45 * np.where(c.x < 0.5, 1.0, 0.4) + 0.2 * (-c.y / 8.0), 0, 0.999), rmp, outline="#1f4a86")


@particle("water_splash", "water", 6, group="water", px=32, tint=True, behaviour="A splash crown: nine arcs of water that fly out and up from the spot with a bead at each tip, then fall back; a thin ring on the ground; stays at its spot, translucent, short.",
          gravity=0.0, size=[0.8, 1.5], life=[10, 18], anim="age", curve="ease_out", fade=[0.0, 0.5])
def water_splash(c, i, n, rng):
    t = i / float(n - 1)
    rmp = ramp("#5f86c8", "#9cc0ee", "#d9ebff", "#ffffff")
    s = np.full(c.x.shape, 9.0, np.float32)
    base_y = 9.0
    for k in range(9):
        a = (k + 0.5) / 9.0 * math.pi
        dirx = math.cos(a)
        reach = 11.5 * (0.6 + 0.4 * abs(math.sin(a * 2.0 + 0.7)))
        peak = 12.0 * (0.55 + 0.45 * abs(math.sin(a * 1.7)))
        for q, (lag, rad) in enumerate(((0.0, 1.5), (0.16, 1.1), (0.3, 0.8))):
            u = max(0.0, min(1.0, t * 1.15 - lag))
            x = dirx * reach * u
            y = base_y - (peak * 4.0 * u * (1.0 - u) * (1.0 - 0.15 * t)) - 1.0
            s = np.minimum(s, sd_disc(c, x, y, rad * (1 - 0.35 * t)))
    a_r = lerp(3.0, 13.5, t)
    b_r = lerp(0.9, 2.8, t)
    qv = np.sqrt((c.x / a_r) ** 2 + ((c.y - (base_y + 1.5)) / b_r) ** 2)
    ring = (np.abs(qv - 1.0) * b_r * 0.9) - 0.55
    s = np.minimum(s, ring)
    img = solid(c, s, np.clip(0.55 + (-(c.y - 4.0) / 18.0), 0, 0.999), rmp, outline="#35579a", outline_alpha=190)
    img[..., 3] *= (1 - 0.5 * t)
    return img


@particle("snowflake", "water", 6, group="water", tint=True, behaviour="A six-armed snow crystal that turns slowly while it sinks, with a little sway; crisp white, translucent.",
          gravity=0.05, drag=0.95, sway=0.012, spin=0.04, size=[0.24, 0.2], life=[40, 70], curve="hold", fade=[0.08, 0.3])
def snowflake(c, i, n, rng):
    rot = i * math.pi / 18.0
    s = np.full(c.x.shape, 9.0, np.float32)
    for k in range(6):
        a = rot + k * math.pi / 3
        ca, sa = math.cos(a), math.sin(a)
        s = np.minimum(s, sd_capsule(c, 0, 0, 6.4 * ca, 6.4 * sa, 1.1))
        for off, ln in ((3.0, 2.3), (4.8, 1.6)):
            for sg in (-1, 1):
                a2 = a + sg * math.pi / 3.2
                bx, by = off * ca, off * sa
                s = np.minimum(s, sd_capsule(c, bx, by, bx + ln * math.cos(a2), by + ln * math.sin(a2), 0.9))
    s = np.minimum(s, sd_disc(c, 0, 0, 1.3))
    return solid(c, s, np.clip(1.0 - c.r / 8.0, 0, 0.999), ramp("#a9bfe0", "#d7e6fb", "#ffffff"), outline=None)


# ---------------------------------------------------------------- earth
@particle("sand_grain", "earth", 4, group="earth", behaviour="A pinch of sand grains that tumble: five to six single-texel specks in warm tans that swap places each frame; falls fast, collides, translucent.",
          size=[0.16, 0.12], gravity=0.5, life=[16, 28], spin=0.0, anim="loop", loop=2)
def sand_grain(c, i, n, rng):
    img = blank(c.n)
    rr = np.random.default_rng(40 + i)
    cols = ["#a8854a", "#c9a560", "#e6c887", "#f4e1a8"]
    for k in range(7):
        x = int(rr.integers(4, 12))
        y = int(rr.integers(4, 12))
        add_px(img, [(x, y)], cols[int(rr.integers(0, 4))], 255)
        if k % 2 == 0:
            add_px(img, [(x + 1, y)], cols[1], 255)
        if k % 3 == 0:
            add_px(img, [(x, y + 1)], cols[0], 255)
    return img


@particle("earth_chip", "earth", 6, group="earth", behaviour="A chunk of dirt and rock with a light top and dark underside that tumbles; heavy, bounces on the ground, opaque.",
          size=[0.2, 0.15], gravity=0.7, spin=0.3, life=[16, 28])
def earth_chip(c, i, n, rng):
    ang = i * math.pi / 3.0
    pts = _tf([(-4.8, -0.8), (-1.2, -5.0), (4.6, -2.0), (3.4, 3.2), (-2.4, 4.6)], ang)
    sd = sd_poly(c, pts)
    x, y = c.rot(ang)
    sh = np.clip(0.55 - 0.5 * y / 8.0 + 0.1 * np.sin(x * 1.4), 0, 0.999)
    img = solid(c, sd, sh, ramp("#4a3626", "#6d5038", "#8f6c4c", "#b08a63"), outline="#2a1e14")
    return img


@particle("bone_chip", "earth", 6, group="earth", behaviour="A sliver of bone: an off-white splinter with a knobbed end that tumbles; falls and bounces, opaque.",
          size=[0.22, 0.16], gravity=0.55, spin=0.28, life=[18, 32])
def bone_chip(c, i, n, rng):
    ang = i * math.pi / 3.0 + 0.3
    x, y = c.rot(ang)
    class V: pass
    v = V(); v.x, v.y = x, y
    shaft = sd_capsule(v, -4.4, 0, 4.2, 0, 2.3, 1.7)
    knob = np.minimum(sd_disc(v, -5.0, -1.2, 1.5), sd_disc(v, -5.0, 1.2, 1.5))
    sd = np.minimum(shaft, knob)
    sh = np.clip(0.7 - 0.3 * y / 3.0, 0, 0.999)
    return solid(c, sd, sh, ramp("#a79d86", "#cdc5b0", "#ebe4d2", "#ffffff"), outline="#6a604a")

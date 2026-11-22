"""Software mock-up of Goku's boss VFX (arc/goku/GokuFx.java, 4.107.0 richFx), drawn WITHOUT the game.

It replays the emitters of GokuFx / GokuBoss / GokuMoves tick by tick (same counts, spreads, radii, colours, call rates), ages the particles like vanilla dust
(10-40 ticks, stationary) and the themed sparks (short, fast), and draws them as additive soft sprites in front of a blocky stand-in figure.
It is NOT Minecraft: real dust sprites, lighting and the client fresnel aura shell (GsAuraLayer, drawn here as a faint outline) look different.
Script-made mock-up, reported as such (rule 15); nothing here was made in an app.

    python3 tools/goku_vfx_preview/preview.py [out_dir]      # default docs/previews/goku_vfx
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "previews", "goku_vfx")
os.makedirs(OUT, exist_ok=True)
W = H = 420

FORMS = [  # idx, name, rgb, electric, hair
    (1, "Super Saiyan", 0xFFD020, False, (255, 220, 60)),
    (2, "Super Saiyan 2", 0xFFE840, True, (255, 230, 70)),
    (3, "Super Saiyan 3", 0xFFC820, True, (255, 210, 50)),
    (4, "Super Saiyan God", 0xFF3030, False, (230, 40, 40)),
    (5, "Super Saiyan Blue", 0x40B0FF, False, (80, 170, 255)),
    (6, "Blue + Kaio-ken", 0xFF4060, True, (90, 160, 255)),
    (7, "Ultra Instinct Sign", 0xB0C8FF, False, (190, 200, 220)),
    (8, "Perfected Ultra Instinct", 0xE8F0FF, True, (225, 232, 245)),
    (9, "True Ultra Instinct", 0xFFFFFF, True, (245, 245, 250)),
    (10, "Mastered Ultra Instinct", 0xEAF0FF, True, (240, 244, 255)),
]
STRENGTH = lambda step: 0.0 if step <= 0 else min(1.0, 0.28 + 0.08 * step)


def rgbt(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def mix(c, to, t):
    a, b = rgbt(c), rgbt(to)
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


class P:
    __slots__ = ("p", "v", "age", "life", "col", "size", "kind", "drag")

    def __init__(s, p, v, life, col, size, kind, drag=1.0):
        s.p, s.v, s.age, s.life, s.col, s.size, s.kind, s.drag = list(p), list(v), 0, life, col, size, kind, drag


class World:
    def __init__(s, seed=1):
        s.r = random.Random(seed)
        s.ps = []
        s.t = 0
        s.sent = 0

    def send(s, kind, col, size, pos, count, d=(0, 0, 0), speed=0.0, life=None, drag=1.0):
        r = s.r
        for _ in range(max(1, count)):
            p = (pos[0] + r.gauss(0, 1) * d[0], pos[1] + r.gauss(0, 1) * d[1], pos[2] + r.gauss(0, 1) * d[2])
            v = (r.gauss(0, 1) * speed, r.gauss(0, 1) * speed, r.gauss(0, 1) * speed)
            if kind == "dust":
                lf = int(8.0 / (r.random() * 0.8 + 0.2))
            else:
                lf = life or 14
            s.ps.append(P(p, v, lf, col, size, kind, drag))
        s.sent += 1

    def step(s):
        s.t += 1
        keep = []
        for q in s.ps:
            q.age += 1
            if q.age >= q.life:
                continue
            for i in range(3):
                q.p[i] += q.v[i]
                q.v[i] *= 0.91 if q.kind != "dust" else 1.0
            keep.append(q)
        s.ps = keep


# ---- the emitters (mirror GokuFx.java; counts are the budget-granted counts, the cap is 2000 a tick so nothing is cut here) -----------------------------------------
def dust(w, rgb, size, pos, n, d=(0, 0, 0), speed=0.0):
    w.send("dust", rgbt(rgb) if isinstance(rgb, int) else rgb, size, pos, n, d, speed)


def spray_endrod(w, pos, n, d, speed):
    w.send("spark", (235, 245, 255), 0.7, pos, n, d, speed, 22, 0.9)


def spray_electric(w, pos, n, d, speed):
    w.send("spark", (190, 200, 255), 0.55, pos, n, d, speed, 9)


def spray_crit(w, pos, n, d, speed):
    w.send("spark", (255, 240, 170), 0.6, pos, n, d, speed, 12)


def flash(w, pos, size=7.0):
    w.send("flash", (255, 255, 255), size, pos, 1, life=6)


def boom(w, pos, size=1.0):
    w.send("boom", (255, 255, 255), size, pos, 1, life=14)


def ring_points(w, cx, cy, cz, radius, rgb, pts, size):
    off = (w.t % 40) * 0.157
    for i in range(pts):
        a = off + i * math.tau / pts
        dust(w, rgb, size, (cx + math.cos(a) * radius, cy, cz + math.sin(a) * radius), 1)


def aura(w, c, rgb, electric, rich=True, h=1.9, wd=0.6):
    x, y, z = c
    if rich:
        dust(w, mix(rgb, 0xFFFFFF, 0.6), 0.9, (x, y + h * .5, z), 4, (wd * .25, h * .35, wd * .25))
        spray_endrod(w, (x, y + 0.2, z), 4, (wd * .7, .1, wd * .7), 0.09)
        dust(w, mix(rgb, 0xFFFFFF, 0.25), 1.8, (x, y + h * 1.05, z), 3, (wd * .35, .25, wd * .35))
        ring_points(w, x, y + .08, z, (0.9 + 0.35 * math.sin(w.t * 0.35)) * wd * 1.4, mix(rgb, 0xFFFFFF, 0.35), 6, 1.0)
    dust(w, rgb, 1.5, (x, y + h * .5, z), 6, (wd * .6, h * .45, wd * .6))
    if electric:
        spray_electric(w, (x, y + h * .5, z), 2, (wd * .7, h * .5, wd * .7), 0.2)


def ring(w, c, radius, rgb, pts, rich=True):
    if rich and radius > 1.0:
        ring_points(w, c[0], c[1] + .15, c[2], radius * .6, mix(rgb, 0xFFFFFF, 0.55), max(4, pts // 2), 1.1)
        spray_endrod(w, (c[0], c[1] + .2, c[2]), 3, (radius * .5, .1, radius * .5), 0.12)
    for i in range(pts):
        a = i * math.tau / pts
        dust(w, rgb, 1.4, (c[0] + math.cos(a) * radius, c[1] + .1, c[2] + math.sin(a) * radius), 1)


def gather(w, at, radius, rgb, n):
    dust(w, rgb, 1.2, at, n, (radius, radius * .6, radius))


def burst(w, at, rgb, n):
    dust(w, rgb, 2.0, at, n, (.5, .5, .5), 0.2)
    spray_endrod(w, at, max(1, n // 3), (.4, .4, .4), 0.15)


def beam(w, a, b, rgb, rich=True, pts=24):
    v = [b[i] - a[i] for i in range(3)]
    for i in range(1, pts + 1):
        t = i / (pts + 1)
        dust(w, rgb, 2.2, [a[k] + v[k] * t for k in range(3)], 2, (.15, .15, .15))
    if rich:
        for i in range(pts // 2):
            t = (i + .5) / (pts // 2)
            dust(w, mix(rgb, 0xFFFFFF, .8), 1.3, [a[k] + v[k] * t for k in range(3)], 1, (.03, .03, .03))
        spray_electric(w, [a[k] + v[k] * .5 for k in range(3)], 3, (abs(v[0]) * .4 + .2, abs(v[1]) * .4 + .2, abs(v[2]) * .4 + .2), 0.3)


def helix(w, base, height, radius, rgb, turn):
    n = 12
    for i in range(n):
        t = (i + .5) / n
        a = turn * 0.45 + t * math.pi * 4 + (i % 2) * math.pi
        dust(w, mix(rgb, 0xFFFFFF, .3), 1.5, (base[0] + math.cos(a) * radius, base[1] + t * height, base[2] + math.sin(a) * radius), 1)


def shockwave(w, at, rgb, radius):
    for i in range(10):
        dust(w, mix(rgb, 0xFFFFFF, .5), 2.4, (at[0], at[1] + i * 1.1, at[2]), 2, (.25, .1, .25))
    flash(w, (at[0], at[1] + 1.0, at[2]))
    boom(w, (at[0], at[1] + .3, at[2]))
    ring_points(w, at[0], at[1] + .1, at[2], radius * 1.3, mix(rgb, 0xFFFFFF, .2), 16, 1.6)


def trail(w, a, b, rgb):
    for i in range(8):
        t = (i + .5) / 8
        dust(w, mix(rgb, 0xFFFFFF, .5), 1.2, [a[k] + (b[k] - a[k]) * t for k in range(3)], 1, (.04, .04, .04))


def true_hit(w, c):
    dust(w, 0xFFFFFF, 1.7, c, 5, (.3, .4, .3), 0.02)
    spray_crit(w, c, 5, (.35, .45, .35), 0.35)
    spray_electric(w, c, 2, (.3, .4, .3), 0.3)


def shimmer(w, c, n):
    spray_endrod(w, (c[0], c[1] + .95, c[2]), n, (.5, .8, .5), 0.05)


# ---- the camera and the stand-in figure ------------------------------------------------------------------------------------------------------------------------
class Cam:
    def __init__(s, eye, look, fov=55):
        s.eye = np.array(eye, float)
        f = np.array(look, float) - s.eye
        s.f = f / np.linalg.norm(f)
        s.r = np.cross(s.f, [0, 1, 0])
        s.r /= np.linalg.norm(s.r)
        s.u = np.cross(s.r, s.f)
        s.k = (H / 2) / math.tan(math.radians(fov) / 2)

    def proj(s, p):
        d = np.array(p, float) - s.eye
        z = d @ s.f
        if z < 0.2:
            return None
        return (W / 2 + (d @ s.r) / z * s.k, H / 2 - (d @ s.u) / z * s.k, z)


def cube(draw, cam, c, size, col):
    cx, cy, cz = c
    sx, sy, sz = [s / 2 for s in size]
    pts = [(cx + dx * sx, cy + dy * sy, cz + dz * sz) for dx in (-1, 1) for dy in (-1, 1) for dz in (-1, 1)]
    faces = [(0, 1, 3, 2, .62), (4, 5, 7, 6, .95), (0, 1, 5, 4, .5), (2, 3, 7, 6, 1.0), (0, 2, 6, 4, .72), (1, 3, 7, 5, .82)]
    pr = [cam.proj(p) for p in pts]
    if any(p is None for p in pr):
        return
    order = sorted(faces, key=lambda f: -sum(pr[i][2] for i in f[:4]))
    for a, b, c2, d, sh in order[3:]:
        draw.polygon([pr[a][:2], pr[b][:2], pr[c2][:2], pr[d][:2]], fill=tuple(int(v * sh) for v in col))


def figure(img, cam, pos, hair, orange=True, glow=None):
    d = ImageDraw.Draw(img)
    x, y, z = pos
    gi = (230, 120, 20) if orange else (40, 40, 60)
    parts = [((x - .15, y + .4, z), (.24, .8, .24), (30, 40, 120)), ((x + .15, y + .4, z), (.24, .8, .24), (30, 40, 120)),
             ((x, y + 1.15, z), (.56, .7, .3), gi), ((x - .42, y + 1.2, z), (.2, .7, .22), gi), ((x + .42, y + 1.2, z), (.2, .7, .22), gi),
             ((x, y + 1.72, z), (.4, .4, .4), (235, 190, 150))]
    for c, s, col in parts:
        cube(d, cam, c, s, col)
    for k, (dx, dy, tall) in enumerate([(-.18, 0, .4), (0, .05, .55), (.18, 0, .4), (-.1, .0, .45), (.1, .05, .5)]):   # hair
        cube(d, cam, (x + dx, y + 1.98 + dy + tall / 2 - .1, z + (k % 2) * .05 - .02), (.14, tall, .14), hair)
    if glow is not None:                                                    # the client fresnel shell (GsAuraLayer): a faint outline
        for c, s, col in parts:
            cube(d, cam, c, (s[0] * 1.12, s[1] * 1.06, s[2] * 1.12), glow)


def draw_particles(img, cam, ps, extra=1.0):
    acc = np.zeros((H, W, 3), np.float32)
    xs = np.arange(W)[None, :]
    ys = np.arange(H)[:, None]
    for q in sorted(ps, key=lambda q: -np.linalg.norm(np.array(q.p) - cam.eye)):
        pr = cam.proj(q.p)
        if pr is None:
            continue
        sx, sy, z = pr
        fade = 1.0 - max(0.0, (q.age / q.life - 0.55) / 0.45)
        if q.kind == "dust":
            r = max(1.2, 0.225 * q.size * cam.k / z * .5)
            a = 0.85 * fade
        elif q.kind == "spark":
            r = max(1.0, 0.12 * q.size * cam.k / z * .5)
            a = 0.9 * fade
        elif q.kind == "flash":
            t = q.age / q.life
            r = 0.7 * q.size * cam.k / z * (0.4 + t)
            a = 0.9 * (1 - t)
        else:  # boom: expanding ring
            t = q.age / q.life
            rr = (0.5 + t * 6) * cam.k / z
            x0, x1, y0, y1 = int(max(0, sx - rr - 3)), int(min(W, sx + rr + 3)), int(max(0, sy - rr * .35 - 3)), int(min(H, sy + rr * .35 + 3))
            if x1 <= x0 or y1 <= y0:
                continue
            dd = np.sqrt(((xs[:, x0:x1] - sx) / rr) ** 2 + ((ys[y0:y1] - sy) / (rr * .35)) ** 2)
            acc[y0:y1, x0:x1] += (np.exp(-((dd - 1) ** 2) * 60) * 0.7 * (1 - t))[..., None] * np.array([1, 1, 1], np.float32)
            continue
        x0, x1, y0, y1 = int(max(0, sx - r * 3)), int(min(W, sx + r * 3 + 1)), int(max(0, sy - r * 3)), int(min(H, sy + r * 3 + 1))
        if x1 <= x0 or y1 <= y0:
            continue
        d2 = ((xs[:, x0:x1] - sx) ** 2 + (ys[y0:y1] - sy) ** 2) / (r * r)
        core = np.exp(-d2 * 1.2) * a
        halo = np.exp(-d2 * 0.18) * a * 0.22
        col = np.array(q.col, np.float32) / 255.0
        acc[y0:y1, x0:x1] += (core + halo)[..., None] * col * extra
    base = np.asarray(img.convert("RGB"), np.float32) / 255.0
    out = 1 - (1 - base) * np.exp(-acc)           # soft additive: never clips harshly
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))


def backdrop():
    a = np.zeros((H, W, 3), np.float32)
    for yy in range(H):
        t = yy / H
        a[yy, :] = np.array([10 + 14 * t, 12 + 16 * t, 24 + 22 * t])
    img = Image.fromarray(a.astype(np.uint8))
    d = ImageDraw.Draw(img)
    d.polygon([(0, H * .78), (W, H * .72), (W, H), (0, H)], fill=(34, 42, 30))
    return img


def label(img, text, sub=None):
    d = ImageDraw.Draw(img)
    try:
        f = ImageFont.truetype("DejaVuSans-Bold.ttf", 15)
        f2 = ImageFont.truetype("DejaVuSans.ttf", 11)
    except Exception:
        f = f2 = ImageFont.load_default()
    d.rectangle([0, 0, W, 24], fill=(0, 0, 0))
    d.text((8, 4), text, fill=(255, 255, 255), font=f)
    if sub:
        d.text((8, H - 16), sub, fill=(200, 200, 200), font=f2)
    return img


def frame(w, cam, hair, pos=(0, 0, 0), glow=None, title="", sub=None, orange=True, extra=1.0):
    img = backdrop()
    figure(img, cam, pos, hair, orange, glow)
    img = draw_particles(img, cam, w.ps, extra)
    return label(img, title, sub)


def sheet(frames, cols, path, gif=None, dur=60):
    rows = (len(frames) + cols - 1) // cols
    s = Image.new("RGB", (cols * W, rows * H), (0, 0, 0))
    for i, f in enumerate(frames):
        s.paste(f, ((i % cols) * W, (i // cols) * H))
    s.save(path)
    if gif:
        gif[0].save(path.replace(".png", ".gif"), save_all=True, append_images=gif[1:], duration=dur, loop=0, optimize=True)


GOKU = (0, 0, 0)
CAM = Cam((5.2, 2.0, 6.4), (0, 1.15, 0))
CAM_WIDE = Cam((7.5, 3.0, 11.5), (0, 1.5, 0))
CENTRE = (0, 0, 0)


def steady_form(idx, name, rgb, electric, hair, ticks=90, rich=True):
    w = World(idx)
    for _ in range(ticks):
        if w.t % 4 == 0:
            aura(w, CENTRE, rgb, electric, rich)
        if idx >= 7 and w.t % 20 == 0:
            shimmer(w, CENTRE, 4)
        if idx == 10 and w.t % 20 == 0:
            ring(w, CENTRE, 1.6, rgb, 10, rich)
        w.step()
    glow = tuple(int(c * STRENGTH(idx) * .55) for c in rgbt(rgb))
    return frame(w, CAM, hair, glow=glow, title="%d  %s" % (idx, name), sub="aura every 4 ticks%s" % ("  + shimmer" if idx >= 7 else ""))


def scene_forms(rich=True):
    fr = [steady_form(i, n, c, e, h, rich=rich) for i, n, c, e, h in FORMS]
    sheet(fr, 5, os.path.join(OUT, "goku_forms_aura%s.png" % ("" if rich else "_old")))
    return fr


def scene_powerup(idx=1, name="Super Saiyan"):
    rgb, hair = FORMS[idx - 1][2], FORMS[idx - 1][4]
    w = World(5)
    frames, gif = [], []
    wind = 40
    for left in range(wind, -1, -1):
        mid = (0, 0.95, 0)
        if left % 4 == 0:
            gather(w, mid, 1.6, rgb, 8)
        if left % 2 == 0:
            helix(w, CENTRE, 2.5, 1.1, rgb, left)
        if left % 10 == 0:
            ring(w, CENTRE, 2.0 + (wind - left) * 0.1, rgb, 14)
        if left == wind // 2:
            burst(w, mid, rgb, 16)
            shockwave(w, CENTRE, rgb, 3.0)
        if left == 0:
            burst(w, mid, rgb, 20)
            ring(w, CENTRE, 6.0, rgb, 20)
            shockwave(w, CENTRE, rgb, 6.0)
        w.step()
        t = wind - left
        if t % 2 == 0:
            f = frame(w, CAM_WIDE, hair if t > wind // 2 else (30, 30, 30), title="Power-up to %s  tick %d/%d" % (name, t, wind), sub="gather + helix + rings; shockwave at the middle and at the end")
            gif.append(f)
            if t in (0, 10, 20, 24, 30, 40):
                frames.append(f)
    for k in range(6):
        if k % 2 == 0:
            aura(w, CENTRE, rgb, False)
        w.step()
    f = frame(w, CAM_WIDE, hair, title="Power-up to %s  after" % name)
    gif.append(f)
    frames.append(f)
    sheet(frames, 4, os.path.join(OUT, "goku_powerup_%s.png" % name.lower().replace(" ", "_")), gif)


def scene_kamehameha():
    rgb = 0x5EC8FF
    w = World(9)
    frames, gif = [], []
    total, target = 40, (14.0, 1.0, 0.0)
    cam = Cam((4.0, 3.0, 12.0), (5, 1.3, 0))
    for left in range(total, -1, -1):
        hands = (0.8, 1.25, 0.0)
        gather(w, hands, 0.5 + (total - left) * 0.02, 0x5EC8FF, 6)
        if left % 9 == 0:
            beam(w, (0, 1.7, 0), target, 0x9FD8FF, rich=True)
        w.step()
        t = total - left
        if t % 2 == 0:
            gif.append(frame(w, cam, (255, 220, 60), pos=(0, 0, 0), title="Kamehameha: charge (tick %d)" % t, sub="motes gather in the hands; a thin telegraph line every 9 ticks"))
        if t in (0, 20, 40):
            frames.append(gif[-1] if t else frame(w, cam, (255, 220, 60), title="Kamehameha: charge (tick 0)"))
    for k in range(10):
        beam(w, (0.8, 1.25, 0), (40, 1.25, 0), rgb) if k == 0 else None
        if k == 0:
            burst(w, (22, 1.25, 0), rgb, 10)
        w.step()
        f = frame(w, cam, (255, 220, 60), title="Kamehameha: fired (tick +%d)" % k, sub="beam shell (24 points x2) + white core + sparks; burst where it ends")
        gif.append(f)
        if k in (1, 5):
            frames.append(f)
    sheet(frames, 3, os.path.join(OUT, "goku_kamehameha.png"), gif)


def scene_spirit_bomb():
    rgb = 0x8CD8FF
    w = World(11)
    frames, gif = [], []
    total = 80
    cam = Cam((7.0, 4.0, 14.0), (0, 4.0, 0))
    spot = (6.0, 0, 3.0)
    for left in range(total, -1, -1):
        grow = 0.6 + 2.4 * (total - left) / total
        gather(w, (0, 1.9 + 2.5, 0), grow, rgb, 8)
        if left % 12 == 0:
            ring(w, spot, 10.0, rgb, 16)
        w.step()
        t = total - left
        if t % 2 == 0:
            f = frame(w, cam, (255, 220, 60), title="Spirit Bomb: charge (tick %d/%d)" % (t, total), sub="gathering motes above his head + a 10-block ring where it will land", extra=1.0)
            gif.append(f)
            if t in (0, 20, 40, 60, 80):
                frames.append(f)
    burst(w, (spot[0], spot[1] + 1, spot[2]), rgb, 24)
    ring(w, spot, 10.0, rgb, 24)
    for k in range(8):
        w.step()
    f = frame(w, cam, (255, 220, 60), title="Spirit Bomb: it lands", sub="burst + ring of 24 at the spot")
    gif.append(f)
    frames.append(f)
    sheet(frames, 3, os.path.join(OUT, "goku_spirit_bomb.png"), gif)


def scene_punch():
    rgb = FORMS[4][2]
    w = World(13)
    frames, gif = [], []
    cam = Cam((2.5, 1.9, 6.5), (1.4, 1.2, 0))
    fist, victim = (0.6, 1.3, 0.0), (2.4, 1.2, 0.0)
    for t in range(18):
        if t == 4:
            trail(w, fist, victim, rgb)
            burst(w, victim, rgb, 5)
        if t == 5:
            true_hit(w, victim)
        w.step()
        img = backdrop()
        figure(img, cam, (0, 0, 0), (80, 170, 255))
        figure(img, cam, (2.4, 0, 0), (40, 40, 40), orange=False)
        img = draw_particles(img, cam, w.ps)
        f = label(img, "Punch + true-damage flash  (tick %d)" % t, "trail (8 points), impact burst, then the white-blue true-hit flash on the victim")
        gif.append(f)
        if t in (3, 5, 7, 11):
            frames.append(f)
    sheet(frames, 4, os.path.join(OUT, "goku_punch_true_hit.png"), gif)


def scene_misc():
    fr = []
    w = World(21)    # Kaio-ken
    for _ in range(90):
        if w.t % 4 == 0:
            aura(w, CENTRE, 0xE03020, True)
        w.step()
    fr.append(frame(w, CAM, (30, 30, 30), glow=(110, 30, 20), title="Kaio-ken (red aura, base to Super Saiyan 3)"))
    w = World(22)    # counter flare / DxD shrug
    ring(w, CENTRE, 4.0, 0xFFD020, 16)
    for _ in range(6):
        w.step()
    fr.append(frame(w, CAM_WIDE, (255, 220, 60), title="Ki flare (shrugs off a debuff): ring in his form colour"))
    w = World(23)    # form bar refill
    ring(w, CENTRE, 3.0, 0x40B0FF, 16)
    for _ in range(4):
        w.step()
    fr.append(frame(w, CAM_WIDE, (80, 170, 255), title="Form bar refilled (ring of 16)"))
    w = World(24)    # ultra instinct dodge afterimage
    shimmer(w, CENTRE, 12)
    burst(w, (0, 1.1, 0), 0x8CD8FF, 12)
    for _ in range(4):
        w.step()
    fr.append(frame(w, CAM, (230, 235, 245), title="Ultra Instinct dodge afterimage (shimmer 12)"))
    sheet(fr, 4, os.path.join(OUT, "goku_misc.png"))


if __name__ == "__main__":
    scene_forms(True)
    scene_forms(False)
    scene_powerup(1, "Super Saiyan")
    scene_powerup(5, "Super Saiyan Blue")
    scene_kamehameha()
    scene_spirit_bomb()
    scene_punch()
    scene_misc()
    print("written to", os.path.abspath(OUT))

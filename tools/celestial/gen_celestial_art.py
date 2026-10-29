"""Celestial Spirit Magic: the particle / sprite textures of the magic circles (white on transparent, tinted in the game), one per sign.

    python3 -B tools/celestial/gen_celestial_art.py

Writes assets/nusmp/textures/particle/
    celestial_circle.png    256 x 256  the zodiac wheel: two rings of ticks, twelve constellation cells, a dodecagram, a small centre circle (turns slowly)
    celestial_ring.png      256 x 256  the rune ring: broken arcs with 36 rune marks (turns against the wheel)
    celestial_glint.png     128 x 128  a four-point star flare
    celestial_shaft.png      64 x 256  a shaft of starlight (soft across, fading at both ends)
    celestial_door.png      128 x 256  the light in the Gate's doorway: an arched door of light with streaks
    celestial_sign_<id>.png  64 x 64   the constellation of each spirit (and of the gate key and the king) with a faint ring, for the middle of a circle
Everything is drawn 4x and brought down with LANCZOS, with a soft bloom added under the lines. Deterministic.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from celestial_data import CONSTELLATIONS, SPIRITS, out_path  # noqa: E402

SS = 4


def canvas(w, h):
    return Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))


def finish(im, w, h, bloom=0.0):
    small = im.resize((w, h), Image.LANCZOS)
    if bloom > 0:
        glow = small.filter(ImageFilter.GaussianBlur(bloom))
        a = np.asarray(small, np.float32), np.asarray(glow, np.float32)
        out = np.clip(a[0] + a[1] * 0.75, 0, 255)
        out[..., :3] = 255
        return Image.fromarray(out.astype(np.uint8), "RGBA")
    return small


def line(d, pts, width, alpha=255):
    d.line([(x * SS, y * SS) for x, y in pts], fill=(255, 255, 255, alpha), width=max(1, int(width * SS)), joint="curve")


def circle(d, c, r, width, alpha=255):
    d.ellipse([(c[0] - r) * SS, (c[1] - r) * SS, (c[0] + r) * SS, (c[1] + r) * SS], outline=(255, 255, 255, alpha), width=max(1, int(width * SS)))


def disc(d, c, r, alpha=255):
    d.ellipse([(c[0] - r) * SS, (c[1] - r) * SS, (c[0] + r) * SS, (c[1] + r) * SS], fill=(255, 255, 255, alpha))


def arc(d, c, r, a0, a1, width, alpha=255):
    d.arc([(c[0] - r) * SS, (c[1] - r) * SS, (c[0] + r) * SS, (c[1] + r) * SS], a0, a1, fill=(255, 255, 255, alpha), width=max(1, int(width * SS)))


def constellation(d, name, ox, oy, size, star=1.0, lw=0.7, alpha=255, rot=0.0):
    """Draws the constellation `name` into the square (ox, oy, size); rot turns it about the square's centre (radians)."""
    stars, edges = CONSTELLATIONS[name]
    cx, cy = ox + size / 2, oy + size / 2

    def at(p):
        x, y = (p[0] - 0.5) * size, (p[1] - 0.5) * size
        c, s = math.cos(rot), math.sin(rot)
        return cx + x * c - y * s, cy + x * s + y * c

    for a, b in edges:
        line(d, [at(stars[a]), at(stars[b])], lw, int(alpha * 0.75))
    for i, p in enumerate(stars):
        q = at(p)
        disc(d, q, star * (1.25 if i % 3 == 0 else 0.85), alpha)
        if i % 3 == 0:                                                     # a small cross flare on every third star
            line(d, [(q[0] - star * 2.4, q[1]), (q[0] + star * 2.4, q[1])], 0.4, int(alpha * 0.6))
            line(d, [(q[0], q[1] - star * 2.4), (q[0], q[1] + star * 2.4)], 0.4, int(alpha * 0.6))


def wheel():
    w = h = 256
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = (128, 128)
    circle(d, c, 124, 1.6); circle(d, c, 119, 0.7, 200)
    for i in range(72):                                                    # ticks between the two outer rings
        a = i * math.tau / 72
        r0, r1 = 119.5, 123.5 if i % 6 else 123.5
        long = i % 6 == 0
        line(d, [(c[0] + math.cos(a) * (r0 - (2.5 if long else 0)), c[1] + math.sin(a) * (r0 - (2.5 if long else 0))), (c[0] + math.cos(a) * r1, c[1] + math.sin(a) * r1)], 0.7 if not long else 1.1)
    circle(d, c, 96, 1.2); circle(d, c, 66, 1.2); circle(d, c, 63, 0.6, 190)
    names = [k[0] for k in SPIRITS if k[2] == "gold"]
    for i, nm in enumerate(names):                                         # twelve cells between r66 and r96, each with its constellation, upright toward the outside
        a = i * math.tau / 12 - math.pi / 2
        mid = 81.0
        x, y = c[0] + math.cos(a) * mid, c[1] + math.sin(a) * mid
        constellation(d, nm, x - 14, y - 14, 28, star=0.9, lw=0.5, rot=a + math.pi / 2)
        a2 = a + math.pi / 12                                              # the cell dividers
        line(d, [(c[0] + math.cos(a2) * 66, c[1] + math.sin(a2) * 66), (c[0] + math.cos(a2) * 96, c[1] + math.sin(a2) * 96)], 0.7, 220)
    for i in range(12):                                                    # the dodecagram {12/5} inside r 60
        a, b = i * math.tau / 12, ((i * 5) % 12) * math.tau / 12 + 0.0
        line(d, [(c[0] + math.cos(a) * 60, c[1] + math.sin(a) * 60), (c[0] + math.cos(b + math.tau / 12 * 5 * 0) * 60, c[1] + math.sin(b) * 60)], 0.5, 120)
    for i in range(12):                                                    # and a plain twelve-point star in front
        a = i * math.tau / 12
        a_next = (i + 1) * math.tau / 12
        mid = (a + a_next) / 2
        line(d, [(c[0] + math.cos(a) * 58, c[1] + math.sin(a) * 58), (c[0] + math.cos(mid) * 30, c[1] + math.sin(mid) * 30), (c[0] + math.cos(a_next) * 58, c[1] + math.sin(a_next) * 58)], 0.8, 200)
    circle(d, c, 16, 1.0); circle(d, c, 10, 0.6, 200); disc(d, c, 3.0)
    return finish(im, w, h, bloom=1.4)


def runering():
    w = h = 256
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    c = (128, 128)
    rng = np.random.RandomState(404)
    for (r, wd, gaps) in ((112, 1.5, 6), (104, 0.7, 9)):
        seg = math.tau / gaps
        for g in range(gaps):
            a0 = math.degrees(g * seg + 0.08)
            a1 = math.degrees((g + 1) * seg - 0.08)
            arc(d, c, r, a0, a1, wd)
    for i in range(36):                                                    # 36 rune marks: a stem with two or three hooks, between r 91 and r 101
        a = i * math.tau / 36
        ca, sa = math.cos(a), math.sin(a)
        tx, ty = -sa, ca
        r0, r1 = 91.0, 101.0
        line(d, [(c[0] + ca * r0, c[1] + sa * r0), (c[0] + ca * r1, c[1] + sa * r1)], 0.9)
        for _ in range(rng.randint(2, 4)):
            t = rng.uniform(0.15, 0.95)
            rr = r0 + (r1 - r0) * t
            side = rng.choice([-1, 1])
            ln = rng.uniform(2.0, 4.4)
            slant = rng.uniform(-0.6, 0.6)
            p0 = (c[0] + ca * rr, c[1] + sa * rr)
            p1 = (p0[0] + tx * side * ln + ca * slant * ln, p0[1] + ty * side * ln + sa * slant * ln)
            line(d, [p0, p1], 0.8)
    circle(d, c, 88, 0.8, 200)
    for i in range(12):                                                    # twelve small stars on r 88
        a = i * math.tau / 12 + math.pi / 12
        disc(d, (c[0] + math.cos(a) * 88, c[1] + math.sin(a) * 88), 1.7)
    return finish(im, w, h, bloom=1.2)


def glint():
    w = h = 128
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = (xx - 63.5) / 64.0, (yy - 63.5) / 64.0
    r = np.sqrt(dx * dx + dy * dy)
    core = np.exp(-(r * 9.0) ** 2) * 1.0 + np.exp(-(r * 3.2) ** 2) * 0.45
    cross = np.exp(-(np.abs(dx) * 38) ** 2) * np.exp(-(np.abs(dy) * 1.8)) * 0.9 + np.exp(-(np.abs(dy) * 38) ** 2) * np.exp(-(np.abs(dx) * 1.8)) * 0.9
    diag = (np.exp(-(np.abs(dx - dy) * 30) ** 2) + np.exp(-(np.abs(dx + dy) * 30) ** 2)) * np.exp(-r * 4.5) * 0.35
    a = np.clip(core + cross + diag, 0, 1)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = 255
    out[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def shaft():
    w, h = 64, 256
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx = (xx - 31.5) / 32.0
    across = np.exp(-(dx * 2.6) ** 2)
    along = np.clip(np.minimum(yy / 40.0, (h - 1 - yy) / 90.0), 0, 1) ** 1.3
    rng = np.random.RandomState(11)
    streak = 0.75 + 0.25 * np.repeat(rng.rand(1, w), h, axis=0)
    a = np.clip(across * along * streak, 0, 1)
    core = np.exp(-(dx * 9.0) ** 2) * along * 0.6
    a = np.clip(a * 0.8 + core, 0, 1)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = 255
    out[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def door():
    w, h = 128, 256
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    cx = 63.5
    # an arched opening: a rectangle with a half-round top
    half = 54.0
    top = 74.0                                                             # where the round top begins
    inside = np.zeros((h, w), np.float32)
    rect = (np.abs(xx - cx) <= half) & (yy >= top) & (yy <= h - 6)
    rr = np.sqrt((xx - cx) ** 2 + (yy - top) ** 2)
    arch = (rr <= half) & (yy < top)
    inside = (rect | arch).astype(np.float32)
    dist = np.where(rect | arch, np.minimum(half - np.abs(xx - cx), np.where(yy < top, half - rr, 1e9)), 0)
    edge = np.clip(dist / 14.0, 0, 1)
    glow = np.exp(-(np.abs(xx - cx) / 38.0) ** 2) * np.clip((yy - 20) / 120.0, 0, 1)
    rng = np.random.RandomState(5)
    streak = np.repeat(rng.rand(h, 1), w, axis=1) * 0.25 + 0.75
    a = inside * (0.35 + 0.65 * glow) * edge ** 0.6 * streak
    a = np.clip(a * 1.25, 0, 1)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = 255
    out[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def sign(name):
    w = h = 64
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    circle(d, (32, 32), 30.5, 0.9, 150)
    constellation(d, name, 8, 8, 48, star=1.7, lw=0.9)
    return finish(im, w, h, bloom=1.0)


def main():
    w = wheel()
    wheel().save(out_path("textures", "particle", "celestial_circle.png"))
    runering().save(out_path("textures", "particle", "celestial_ring.png"))
    glint().save(out_path("textures", "particle", "celestial_glint.png"))
    shaft().save(out_path("textures", "particle", "celestial_shaft.png"))
    door().save(out_path("textures", "particle", "celestial_door.png"))
    for k in SPIRITS:
        sign(k[0]).save(out_path("textures", "particle", "celestial_sign_%s.png" % k[0]))
    sign("spirit_king_gate").save(out_path("textures", "particle", "celestial_sign_spirit_king_gate.png"))
    sign("king").save(out_path("textures", "particle", "celestial_sign_king.png"))
    print("celestial art written")


if __name__ == "__main__":
    main()

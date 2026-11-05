"""Generates the Nen (Hunter x Hunter) VFX textures, deterministic (Pillow + numpy, no external images).

    python3 -B tools/gen_nen_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/nen_*.png   (white + alpha, the vertex colour tints them; RGB is 0 under a transparent pixel)

Look: aura in the anime is not a puff, it is tongues of light that leave the skin and lick upwards, a hot white core inside a coloured body, and thin streamers that
curl away. So the set is built from flame tongues, ribbons, a few crisp sigils and one hex lattice (the Nen type wheel):
  nen_flame_0..3  128x256  one aura tongue (base at the bottom, wavering tip on top, a bright core line inside, vertical striations)
  nen_ribbon      64x256   a flowing streamer, soft edges, tapered at both ends (trails, helix streamers, strings)
  nen_core        128      hot core: gaussian body, a bright centre and a faint outer ring (orbs, bullets, fist)
  nen_burst       256      starburst: 14 tapered rays of different length, a core and a thin ring (Ren burst, impact)
  nen_ring        256      ground ring: a crisp main ring, a fainter inner ring, tick marks and a soft outer glow
  nen_hex         256      hex lattice sigil: two counter hexagons, spokes, vertex nodes (stance, domain, mark)
  nen_sphere      256      a lattice sphere seen from outside: fresnel rim, latitude and longitude lines (En)
  nen_eye         256x128  a slit eye with a lens outline and horizontal flare (Gyo)
  nen_bolt        64x256   a crackling electric rope with side forks (Transmutation)
  nen_link        64x128   two chain links of aura, one face-on, one edge-on (Conjuration)
  nen_needle      32x256   a long needle: bright tip on top, an eye at the bottom (Manipulation)
  nen_gum         64x256   a glossy stretching rope with bulges (Bungee Gum)
  nen_shard       64       a faceted splinter (armour shatter)
  nen_crack       256      ground cracks radiating from the middle (Enhancement impact)
  nen_spark       64       a four-point spark with a long horizontal streak
  nen_mark        128      a target mark: ring, three ticks and a small eye (Manipulation, Gyo)
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 2


def grid(w, h):
    ys, xs = np.mgrid[0:h * SS, 0:w * SS].astype(np.float32)
    return (xs + 0.5) / (w * SS), (ys + 0.5) / (h * SS)


def vnoise(w, h, cell, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(w // cell)), max(1, int(h // cell))
    g = rng.random((gh + 2, gw + 2)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, base, seed, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += amp * vnoise(w, h, max(1, base // (2 ** o)), seed + o * 17)
        tot += amp
        amp *= 0.5
    return out / tot


def sm(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def seg_dist(px, py, ax, ay, bx, by):
    """Distance from points to the segment a-b (broadcast)."""
    dx, dy = bx - ax, by - ay
    l2 = max(dx * dx + dy * dy, 1e-9)
    t = np.clip(((px - ax) * dx + (py - ay) * dy) / l2, 0, 1)
    return np.hypot(px - (ax + t * dx), py - (ay + t * dy))


def save(name, alpha, gray=None, w=None, h=None):
    """alpha / gray are at supersampled resolution; the file is w x h."""
    ah, aw = alpha.shape
    h = h or ah // SS
    w = w or aw // SS
    gray = np.ones_like(alpha) if gray is None else gray
    a = np.clip(alpha, 0, 1).reshape(h, SS, w, SS).mean((1, 3))
    ga = (np.clip(gray, 0, 1) * np.clip(alpha, 0, 1)).reshape(h, SS, w, SS).mean((1, 3))
    g = np.where(a > 1e-5, ga / np.maximum(a, 1e-5), 0)
    a8 = (a * 255 + 0.5).astype(np.uint8)
    g8 = (np.clip(g, 0, 1) * 255 + 0.5).astype(np.uint8)
    g8 = np.where(a8 > 0, g8, 0).astype(np.uint8)                         # no colour under a transparent pixel (the safe-additive rule)
    rgba = np.dstack([g8, g8, g8, a8])
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)


# ------------------------------------------------------------------ flame tongues
def flame(seed, lean, wob, phase):
    w, h = 128, 256
    u, v = grid(w, h)
    t = 1 - v                                                              # 0 at the base, 1 at the tip
    n = fbm(w * SS, h * SS, 28, seed)
    n2 = fbm(w * SS, h * SS, 10, seed + 5)
    uw = u + (n - 0.5) * 0.10 * t                                          # the tongue licks sideways as it climbs
    c = 0.5 + lean * 0.2 * t ** 1.7 + wob * 0.07 * np.sin(t * 6.5 + phase) * t
    hw = 0.47 * (1 - t) ** 0.62 * (0.82 + 0.18 * np.sin(t * 9 + phase * 2)) + 0.012
    d = np.abs(uw - c) / hw
    body = sm(1.0, 0.2, d)
    core = np.exp(-(((uw - c) / (hw * 0.4)) ** 2))
    strip = 0.72 + 0.28 * np.sin((uw - c) * 70 + n2 * 6)                   # vertical striations
    a = body * (0.5 + 0.5 * core) * (0.74 + 0.26 * n2) * (0.86 + 0.14 * strip)
    a *= sm(0.0, 0.04, t) * (1 - sm(0.9, 1.0, t))
    return np.clip(a * 1.15, 0, 1)


def ribbon():
    w, h = 64, 256
    u, v = grid(w, h)
    n = fbm(w * SS, h * SS, 20, 91)
    x = (u - 0.5 + (n - 0.5) * 0.12) * 2
    a = np.exp(-(x / 0.42) ** 2) * (0.55 + 0.45 * np.exp(-(x / 0.12) ** 2))
    a *= sm(0, 0.12, v) * (1 - sm(0.78, 1.0, v)) * (0.8 + 0.2 * n)
    return np.clip(a, 0, 1)


def core():
    w = 128
    u, v = grid(w, w)
    r = np.hypot(u - 0.5, v - 0.5) * 2
    a = np.exp(-(r / 0.38) ** 2) * 0.8 + np.exp(-(r / 0.12) ** 2) * 0.6 + 0.22 * np.exp(-(((r - 0.78) / 0.05) ** 2))
    return np.clip(a * (1 - sm(0.9, 1.0, r)), 0, 1)


def burst():
    w = 256
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    a = np.zeros_like(r)
    rng = np.random.default_rng(14)
    for k in range(14):
        th = k / 14 * math.tau + rng.random() * 0.2
        ln = 0.55 + 0.4 * rng.random()
        da = np.abs(((ang - th + math.pi) % math.tau) - math.pi)
        wid = 0.06 * (1 - np.clip(r / ln, 0, 1)) + 0.004
        a = np.maximum(a, np.exp(-((da * r / wid) ** 2)) * (1 - sm(0.1, ln, r)) * sm(0.02, 0.1, r))
    a += np.exp(-(r / 0.16) ** 2) * 0.9 + 0.35 * np.exp(-(((r - 0.72) / 0.025) ** 2))
    return np.clip(a * (1 - sm(0.92, 1.0, r)), 0, 1)


def ring():
    w = 256
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    a = np.exp(-(((r - 0.9) / 0.018) ** 2)) + 0.4 * np.exp(-(((r - 0.8) / 0.012) ** 2)) + 0.25 * np.exp(-(((r - 0.9) / 0.07) ** 2))
    tick = (np.cos(ang * 72) > 0.7).astype(np.float32) * np.exp(-(((r - 0.86) / 0.022) ** 2)) * 0.7
    tick2 = (np.cos(ang * 12) > 0.9).astype(np.float32) * np.exp(-(((r - 0.72) / 0.03) ** 2)) * 0.8
    return np.clip((a + tick + tick2) * (1 - sm(0.97, 1.0, r)), 0, 1)


def hexring(x, y, R, rot, wid):
    """Outline of a regular hexagon of circumradius R rotated by rot (distance based)."""
    d = np.full_like(x, 9.0)
    for k in range(6):
        a0, a1 = rot + k * math.pi / 3, rot + (k + 1) * math.pi / 3
        d = np.minimum(d, seg_dist(x, y, R * math.cos(a0), R * math.sin(a0), R * math.cos(a1), R * math.sin(a1)))
    return np.exp(-((d / wid) ** 2))


def hexsigil():
    w = 256
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r = np.hypot(x, y)
    a = hexring(x, y, 0.9, 0.0, 0.012) + 0.7 * hexring(x, y, 0.9, math.pi / 6, 0.009) + 0.5 * hexring(x, y, 0.55, math.pi / 6, 0.009)
    for k in range(6):
        ang = k * math.pi / 3
        a += 0.45 * np.exp(-((seg_dist(x, y, 0.2 * math.cos(ang), 0.2 * math.sin(ang), 0.9 * math.cos(ang), 0.9 * math.sin(ang)) / 0.007) ** 2))
        nx, ny = 0.9 * math.cos(ang), 0.9 * math.sin(ang)
        a += 0.9 * np.exp(-(((np.hypot(x - nx, y - ny)) / 0.035) ** 2))
    a += 0.8 * np.exp(-(((r - 0.2) / 0.012) ** 2)) + 0.3 * np.exp(-(r / 0.1) ** 2)
    return np.clip(a * (1 - sm(0.96, 1.0, r)), 0, 1)


def sphere():
    w = 256
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r = np.hypot(x, y)
    inside = r < 0.97
    z = np.sqrt(np.clip(1 - (r / 0.97) ** 2, 0, 1))
    rim = (1 - z) ** 2.4
    a = rim * 0.85 * inside
    lat = np.zeros_like(r)
    for k in range(-3, 4):                                                 # latitude lines: y = k/4 * radius, drawn as horizontal ellipses seen edge on
        lat = np.maximum(lat, np.exp(-(((y / 0.97 - k / 4) / 0.012) ** 2)))
    lon = np.zeros_like(r)
    for k in range(6):                                                     # meridians: ellipses of different tilt
        ph = k / 6 * math.pi
        xm = np.sin(ph) * np.sqrt(np.clip(1 - (y / 0.97) ** 2, 0, 1)) * 0.97
        lon = np.maximum(lon, np.exp(-(((np.abs(x) - np.abs(xm)) / 0.012) ** 2)))
    a += (lat * 0.5 + lon * 0.4) * inside * (0.4 + 0.6 * z)
    a += 0.06 * inside
    return np.clip(a, 0, 1)


def eye():
    w, h = 256, 128
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2 * 0.5
    lens = np.abs(y) - 0.2 * (1 - np.abs(x) ** 1.4)
    a = np.exp(-((lens / 0.012) ** 2)) * (np.abs(x) < 0.92) * 0.9
    fill = sm(0.02, -0.06, lens) * (np.abs(x) < 0.9) * 0.28
    pupil = np.exp(-((x / 0.055) ** 2)) * np.exp(-((y / 0.17) ** 2)) * (lens < 0)
    flare = np.exp(-((y / 0.012) ** 2)) * np.exp(-(np.abs(x) / 0.8) ** 2) * 0.9
    glow = np.exp(-(x ** 2 / 0.22 + (y ** 2) / 0.02)) * 0.5
    return np.clip(a + fill + pupil + flare + glow, 0, 1)


def bolt():
    w, h = 64, 256
    u, v = grid(w, h)
    n = fbm(w * SS, h * SS, 8, 31)
    x = (u - 0.5) * 2
    core_ = np.exp(-((x / 0.07) ** 2))
    glow = np.exp(-((x / 0.34) ** 2)) * (0.35 + 0.35 * n)
    a = core_ * 0.95 + glow
    rng = np.random.default_rng(7)
    for _ in range(9):                                                     # side forks
        y0 = rng.random()
        side = rng.choice([-1, 1])
        ln = 0.18 + 0.2 * rng.random()
        bx, by = side * ln * 1.4, y0 + 0.07
        a += 0.55 * np.exp(-((seg_dist(x, v, 0, y0, bx * 0.5, by) / 0.012) ** 2))
    a *= sm(0, 0.04, v) * (1 - sm(0.96, 1.0, v))
    return np.clip(a, 0, 1)


def link():
    w, h = 64, 128
    u, v = grid(w, h)
    x, y = (u - 0.5) * 2, v * 2                                            # y 0..2: first link 0..1 (face-on oval), second 1..2 (edge-on bar)
    yy = y - 0.5
    ov = np.hypot(x / 0.6, yy / 0.46)
    face = np.exp(-(((ov - 1) / 0.13) ** 2)) * (y < 1.0)
    bar = np.exp(-((np.abs(x) - 0.0) / 0.09) ** 2) * sm(1.04, 1.12, y) * (1 - sm(1.88, 1.96, y))
    rim = np.exp(-(((np.abs(x) - 0.2) / 0.05) ** 2)) * sm(1.04, 1.12, y) * (1 - sm(1.88, 1.96, y)) * 0.6
    sheen = np.exp(-(((x + 0.3) / 0.1) ** 2)) * np.exp(-(((yy + 0.1) / 0.2) ** 2)) * (y < 1.0) * 0.5
    return np.clip(face + bar + rim + sheen + 0.0, 0, 1)


def needle():
    w, h = 32, 256
    u, v = grid(w, h)
    x = (u - 0.5) * 2
    shaft_w = 0.07 + 0.05 * v
    a = np.exp(-((x / shaft_w) ** 2)) * sm(0.0, 0.07, v) * 0.9
    tip = np.exp(-((x / 0.35) ** 2)) * np.exp(-((v / 0.05) ** 2)) * 0.9
    eye_ = np.exp(-((((np.hypot(x / 0.34, (v - 0.94) / 0.045)) - 1) / 0.25) ** 2)) * 0.9
    halo = np.exp(-((x / 0.5) ** 2)) * 0.18 * (1 - v * 0.4)
    return np.clip(a + tip + eye_ + halo, 0, 1)


def gum():
    w, h = 64, 256
    u, v = grid(w, h)
    x = (u - 0.5) * 2
    bulge = 0.5 + 0.18 * np.sin(v * math.tau * 3) + 0.1 * np.sin(v * math.tau * 7 + 1)
    d = np.abs(x) / bulge
    body = sm(1.0, 0.82, d)
    z = np.sqrt(np.clip(1 - d ** 2, 0, 1))
    spec = np.exp(-(((x + 0.18) / 0.07) ** 2)) * 0.8
    a = body * (0.5 + 0.5 * z) + spec * body
    a *= sm(0, 0.05, v) * (1 - sm(0.95, 1.0, v))
    return np.clip(a, 0, 1)


def shard():
    w = 64
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    m = np.zeros_like(x)
    pts = [(0.0, -0.95), (0.45, -0.1), (0.2, 0.9), (-0.25, 0.55), (-0.5, -0.05)]
    # polygon fill by winding test
    inside = np.zeros(x.shape, bool)
    for i in range(len(pts)):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % len(pts)]
        cond = ((y0 > y) != (y1 > y)) & (x < (x1 - x0) * (y - y0) / (y1 - y0 + 1e-9) + x0)
        inside ^= cond
    d = np.full_like(x, 9.0)
    for i in range(len(pts)):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % len(pts)]
        d = np.minimum(d, seg_dist(x, y, x0, y0, x1, y1))
    facet = 0.55 + 0.45 * (x * 0.6 - y * 0.3 > 0.05)
    m = inside * facet * 0.8 + np.exp(-((d / 0.03) ** 2)) * 0.9
    return np.clip(m, 0, 1)


def crack():
    w = 256
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r = np.hypot(x, y)
    a = np.zeros_like(r)
    rng = np.random.default_rng(23)
    for k in range(9):                                                     # each crack: a wandering polyline outwards, with one fork
        ang = k / 9 * math.tau + rng.random() * 0.4
        px, py = 0.03 * math.cos(ang), 0.03 * math.sin(ang)
        for s in range(9):
            ang += (rng.random() - 0.5) * 0.7
            step = 0.09 + 0.05 * rng.random()
            nx, ny = px + step * math.cos(ang), py + step * math.sin(ang)
            wid = 0.016 * (1 - s / 10) + 0.004
            a = np.maximum(a, np.exp(-((seg_dist(x, y, px, py, nx, ny) / wid) ** 2)) * (1 - s / 11))
            if s == 4:
                fa = ang + (0.7 if k % 2 else -0.7)
                a = np.maximum(a, np.exp(-((seg_dist(x, y, nx, ny, nx + 0.22 * math.cos(fa), ny + 0.22 * math.sin(fa)) / 0.006) ** 2)) * 0.7)
            px, py = nx, ny
    a += np.exp(-(r / 0.14) ** 2) * 0.7
    return np.clip(a * (1 - sm(0.9, 1.0, r)), 0, 1)


def spark():
    w = 64
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    a = np.exp(-((y / 0.04) ** 2)) * np.exp(-(np.abs(x) / 0.85) ** 2) + np.exp(-((x / 0.05) ** 2)) * np.exp(-(np.abs(y) / 0.5) ** 2) * 0.8
    a += np.exp(-(np.hypot(x, y) / 0.16) ** 2)
    return np.clip(a * (1 - sm(0.92, 1.0, np.hypot(x, y))), 0, 1)


def mark():
    w = 128
    u, v = grid(w, w)
    x, y = (u - 0.5) * 2, (v - 0.5) * 2
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    a = np.exp(-(((r - 0.82) / 0.025) ** 2)) * (0.5 + 0.5 * (np.cos(ang * 3) > -0.2))
    for k in range(3):
        th = k * math.tau / 3 + math.pi / 2
        a += np.exp(-((seg_dist(x, y, 0.6 * math.cos(th), 0.6 * math.sin(th), 1.0 * math.cos(th), 1.0 * math.sin(th)) / 0.03) ** 2)) * 0.9
    lens = np.abs(y) - 0.2 * (1 - np.abs(x / 0.5) ** 1.4)
    a += np.exp(-((lens / 0.02) ** 2)) * (np.abs(x) < 0.5) * 0.9 + np.exp(-(np.hypot(x / 0.07, y / 0.16))) * 0.9 * (np.abs(x) < 0.5)
    a += 0.25 * np.exp(-(r / 0.5) ** 2)
    return np.clip(a * (1 - sm(0.94, 1.0, r)), 0, 1)


def main():
    for i, (seed, lean, wob, ph) in enumerate([(3, 0.0, 0.8, 0.0), (11, 1.0, 0.6, 1.7), (23, -1.0, 0.7, 3.1), (37, 0.4, 1.0, 4.6)]):
        save("nen_flame_%d" % i, flame(seed, lean, wob, ph))
    save("nen_ribbon", ribbon())
    save("nen_core", core())
    save("nen_burst", burst())
    save("nen_ring", ring())
    save("nen_hex", hexsigil())
    save("nen_sphere", sphere())
    save("nen_eye", eye())
    save("nen_bolt", bolt())
    save("nen_link", link())
    save("nen_needle", needle())
    save("nen_gum", gum())
    save("nen_shard", shard())
    save("nen_crack", crack())
    save("nen_spark", spark())
    save("nen_mark", mark())
    print("wrote", len([f for f in os.listdir(OUT) if f.startswith("nen_")]), "nen textures to", os.path.abspath(OUT))


if __name__ == "__main__":
    main()

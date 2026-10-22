"""The hand-painted textures of the Spirit of Space (docs/magic/spirit_space.md). Deterministic (every painter is seeded by its name), numpy + Pillow only.

The spirit is a TEAR IN REALITY: deep indigo / black with violet and cyan star points, thin pale linework, glow only through the additive (SafeAdditive) passes.

    python3 -B tools/gen_spirit_space_art.py            writes textures/entity/spirit_space/*.png (the scrolling starfield layers, the dark bases, the fins, the portal)
                                                         and the geo sheets textures/entity/spirit_space_{orb,wisp}[_lod1|_lod2|_fx].png via tools/gen_spirit_space_geo.py

Layer textures (all tile in both directions, so a UV scroll never shows a seam; the stars carry NO colour under a fully transparent texel, the safe additive rule):
    stars_far / stars_mid / stars_near   three starfields of growing star size, drawn as three shells that scroll at different speeds (the parallax of the face)
    core / body                          the opaque dark bases of the orb and of the wisp's body
    fin                                  the cosmic fins (a gradient with veins and star dots)
    portal_ring / portal_disc            the portal's rim and its starfield window
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from spirit_space_lib import rgb, smoothstep  # noqa: E402

ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity")
OUT = os.path.join(ROOT, "spirit_space")

PAL = dict(void="#05031A", ink="#0A0626", indigo="#120A3C", deep="#1E1260", violet="#6A3DFF", violet_l="#9C7BFF", lilac="#C9B8FF", cyan="#4FE3FF", cyan_d="#1FA8D8",
           pale="#EAF6FF", magenta="#C75CFF", gold="#FFE7A8")
C = {k: rgb(v) / 255.0 for k, v in PAL.items()}


def seed_of(name):
    return sum((i + 1) * ord(c) for i, c in enumerate(name)) * 7919 % (2 ** 31)


# ------------------------------------------------------------------------------------------------------------------ tileable noise
def pnoise(rng, w, h, n):
    """Periodic value noise: an n x n lattice that wraps, smoothly interpolated, so the result tiles."""
    g = rng.random((n, n)).astype(np.float32)
    ys = np.linspace(0, n, h, endpoint=False)
    xs = np.linspace(0, n, w, endpoint=False)
    y0 = ys.astype(int)[:, None]
    x0 = xs.astype(int)[None, :]
    fy = (ys - y0[:, 0])[:, None]
    fx = (xs - x0[0, :])[None, :]
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    y1 = (y0 + 1) % n
    x1 = (x0 + 1) % n
    a = g[y0, x0] * (1 - fx) + g[y0, x1] * fx
    b = g[y1, x0] * (1 - fx) + g[y1, x1] * fx
    return a * (1 - fy) + b * fy


def pfbm(rng, w, h, base=3, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += pnoise(rng, w, h, base * 2 ** o) * amp
        tot += amp
        amp *= 0.5
    return out / tot


class Sky:
    """A premultiplied RGBA float canvas that wraps: stamps near an edge continue on the other side, so the texture tiles."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = np.zeros((h, w, 4), np.float32)

    def add(self, x, y, color, a):
        self.px[y % self.h, x % self.w, :3] += color * a
        self.px[y % self.h, x % self.w, 3] += a

    def star(self, cx, cy, color, size, ray=0, rng=None):
        """A soft round star of radius `size` (gaussian), optionally with a 4-point cross of length `ray` pixels."""
        r = int(math.ceil(size * 2.2))
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                d2 = (dx * dx + dy * dy) / max(1e-6, size * size)
                v = math.exp(-d2 * 1.4)
                if v > 0.03:
                    self.add(cx + dx, cy + dy, color, v)
        for k in range(1, ray + 1):
            v = (1.0 - k / (ray + 1.0)) ** 1.6 * 0.9
            for sx, sy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                self.add(cx + sx * k, cy + sy * k, color, v)

    def done(self, alpha_scale=1.0):
        a = np.clip(self.px[..., 3], 0, 1)
        out = np.zeros((self.h, self.w, 4), np.float32)
        safe = np.maximum(self.px[..., 3], 1e-6)[..., None]
        out[..., :3] = np.clip(self.px[..., :3] / safe, 0, 1)
        out[..., 3] = a * alpha_scale
        out[a <= 0.004] = 0
        return out


def save_png(name, arr, folder=OUT):
    os.makedirs(folder, exist_ok=True)
    a = np.clip(arr, 0, 1)
    img = (a * 255.0 + 0.5).astype(np.uint8)
    img[img[..., 3] == 0] = 0                                                   # nothing under a fully transparent texel (the safe additive rule)
    Image.fromarray(img, "RGBA").save(os.path.join(folder, name + ".png"), optimize=True)


# ------------------------------------------------------------------------------------------------------------------ the starfields
STAR_COLORS = [C["pale"], C["pale"], C["cyan"], C["violet_l"], C["lilac"], C["gold"], C["magenta"]]
STAR_WEIGHTS = np.array([4, 3, 4, 4, 2, 1, 1], np.float32)
STAR_WEIGHTS /= STAR_WEIGHTS.sum()


def starfield(name, size, count, size_range, ray_range, ray_chance, dim, nebula=0.0, seed=0):
    rng = np.random.default_rng(seed or seed_of(name))
    sky = Sky(size, size)
    if nebula > 0:                                                              # soft cloud banks under the stars: violet and cyan, tileable
        n1 = pfbm(rng, size, size, 3, 4)
        n2 = pfbm(rng, size, size, 4, 3)
        cloud = smoothstep(n1, 0.46, 0.80)
        for col, mask, k in ((C["violet"], cloud, 0.55), (C["cyan_d"], smoothstep(n2, 0.55, 0.85) * cloud, 0.35), (C["magenta"], smoothstep(n2, 0.70, 0.95) * 0.5, 0.25)):
            a = mask * k * nebula
            sky.px[..., :3] += col * a[..., None]
            sky.px[..., 3] += a
    for i in range(count):
        cx, cy = int(rng.integers(0, size)), int(rng.integers(0, size))
        col = STAR_COLORS[int(rng.choice(len(STAR_COLORS), p=STAR_WEIGHTS))]
        s = float(rng.uniform(*size_range))
        ray = int(rng.integers(ray_range[0], ray_range[1] + 1)) if rng.random() < ray_chance else 0
        sky.star(cx, cy, col * float(rng.uniform(dim[0], dim[1])), s, ray)
    return sky.done()


def make_layers():
    save_png("stars_far", starfield("stars_far", 128, 150, (0.35, 0.6), (0, 0), 0.0, (0.55, 1.0), nebula=0.0))
    save_png("stars_mid", starfield("stars_mid", 128, 46, (0.5, 0.95), (1, 2), 0.18, (0.7, 1.0), nebula=0.0))
    save_png("stars_near", starfield("stars_near", 128, 13, (0.9, 1.5), (3, 6), 0.7, (0.85, 1.0), nebula=0.9))


# ------------------------------------------------------------------------------------------------------------------ dark bases
def dark_base(name, w, h, bands=False):
    rng = np.random.default_rng(seed_of(name))
    n = pfbm(rng, w, h, 3, 4)
    n2 = pfbm(rng, w, h, 6, 3)
    out = np.zeros((h, w, 4), np.float32)
    base = C["ink"] + (C["deep"] - C["ink"]) * smoothstep(n, 0.2, 0.75)[..., None]
    base = base + (C["violet"] * 0.5 - base) * (smoothstep(n2, 0.5, 0.9) * 0.45)[..., None]
    if bands:                                                                   # the wisp's body: faint rings across the length (v) and a darker back (u), like the plates of a serpent
        yy = np.arange(h, dtype=np.float32)[:, None]
        xx = np.arange(w, dtype=np.float32)[None, :]
        ring = np.clip(1.0 - np.abs(((yy % 16) - 0.5) / 16.0 * 2 - 1.0) * 6.0, 0, 1) * 0.0
        line = (np.abs((yy % 16) - 1.0) < 0.6).astype(np.float32)
        belly = smoothstep(np.cos((xx / w) * math.tau), 0.5, -0.9)              # a paler stripe down the underside
        base = base + (C["deep"] * 1.0 - base) * (line * 0.5)[..., None] * np.ones((1, w, 1))
        base = base + (C["violet"] * 0.35 - base) * (belly * 0.4)[..., None]
        base = base + (C["cyan"] * 0.25 - base) * ((line * belly) * 0.55)[..., None]
    out[..., :3] = np.clip(base, 0, 1)
    out[..., 3] = 1.0
    return out


def make_bases():
    save_png("core", dark_base("core", 128, 128))
    save_png("body", dark_base("body", 128, 128, bands=True))


# ------------------------------------------------------------------------------------------------------------------ fins and the portal
def fin_texture():
    """The cosmic fin: u runs across the fin (0 at the root, 1 at the edge), v along it. Opaque at the root, gone at the edge, with spars, veins and star dots."""
    w, h = 64, 64
    rng = np.random.default_rng(seed_of("fin"))
    xx = np.linspace(0, 1, w, dtype=np.float32)[None, :]
    yy = np.linspace(0, 1, h, endpoint=False, dtype=np.float32)[:, None]
    root = (1.0 - smoothstep(xx, 0.05, 1.0)) ** 0.8 * np.ones((h, 1), np.float32)
    n = pfbm(rng, w, h, 4, 3)
    base = C["violet"][None, None, :] * (1 - xx[..., None]) + C["cyan"][None, None, :] * xx[..., None]
    a = root * (0.35 + 0.65 * smoothstep(n, 0.25, 0.8))
    spar = np.zeros((h, w), np.float32)
    for k in range(5):                                                          # thin spars fanning from the root to the edge
        t = (k + 0.5) / 5
        line = np.exp(-(((yy - t) * 1.0) ** 2) * 900 * np.ones((1, w), np.float32)) * (1 - xx * 0.7)
        spar = np.maximum(spar, line)
    px = np.zeros((h, w, 4), np.float32)
    col = base * (0.55 + 0.3 * n[..., None])
    col = col * (1 - spar[..., None] * 0.6) + C["lilac"] * (spar[..., None] * 0.6)
    px[..., :3] = col
    px[..., 3] = np.clip(a * (1 - spar * 0.2) + spar * root * 0.9, 0, 1)
    sky = Sky(w, h)
    for i in range(14):
        sky.star(int(rng.integers(0, w)), int(rng.integers(0, h)), C["pale"], 0.55)
    s = sky.done()
    out = px.copy()
    m = s[..., 3:4]
    out[..., :3] = out[..., :3] * (1 - m) + s[..., :3] * m
    out[..., 3] = np.clip(px[..., 3] + s[..., 3] * 0.7 * root, 0, 1)
    save_png("fin", out)


def portal_textures():
    # the rim: a 128 x 32 band, u around the ring: a bright core line, jagged rift edges either side, rune ticks
    w, h = 128, 32
    rng = np.random.default_rng(seed_of("portal_ring"))
    yy = np.linspace(-1, 1, h, dtype=np.float32)[:, None]
    xx = np.arange(w, dtype=np.float32)[None, :]
    jag = pnoise(rng, w, 8, 16)
    jag = np.repeat(jag, h // 8, axis=0)[:h]
    edge = 0.34 + 0.38 * (jag - 0.5) * 2.0
    core = np.exp(-(yy * yy) / (2 * 0.07 ** 2)) * np.ones((1, w), np.float32)
    glass = np.clip(1.0 - np.abs(np.abs(yy) - edge) / 0.06, 0, 1) * (np.abs(yy) < 0.95)
    body = smoothstep(edge - np.abs(yy), 0.0, 0.25) * 0.4
    tick = (((xx % 8) < 1.2) & (np.abs(yy) > 0.45) & (np.abs(yy) < 0.8)).astype(np.float32) * 0.55
    a = np.clip(core + glass * 0.9 + body + tick, 0, 1)
    col = C["violet_l"] * body[..., None] + C["cyan"] * glass[..., None] + C["pale"] * core[..., None] + C["lilac"] * tick[..., None]
    col = col / np.maximum(1e-6, (body + glass + core + tick))[..., None]
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = np.clip(col, 0, 1)
    out[..., 3] = a
    save_png("portal_ring", out)
    # the disc: a starfield window with a slow spiral of nebula, bright at the rim, dark in the middle (u across the disc through the middle, v round)
    s = 128
    sky = Sky(s, s)
    srng = np.random.default_rng(seed_of("portal_disc"))
    sky.px[..., :3] += (C["ink"] * 0.9)[None, None, :] * 0.85                         # the window is dark: a deep indigo that the stars sit in
    sky.px[..., 3] += 0.85
    yy2, xx2 = np.mgrid[0:s, 0:s].astype(np.float32)
    n = pfbm(srng, s, s, 3, 4)
    cloud = smoothstep(n, 0.4, 0.8)
    sky.px[..., :3] += (C["violet"] * 0.7)[None, None, :] * (cloud * 0.6)[..., None]
    sky.px[..., 3] += cloud * 0.6
    sky.px[..., :3] += (C["cyan_d"] * 0.8)[None, None, :] * (smoothstep(n, 0.62, 0.9) * 0.5)[..., None]
    sky.px[..., 3] += smoothstep(n, 0.62, 0.9) * 0.5
    for i in range(70):
        sky.star(int(srng.integers(0, s)), int(srng.integers(0, s)), STAR_COLORS[int(srng.integers(0, len(STAR_COLORS)))], float(srng.uniform(0.4, 0.9)), 2 if srng.random() < 0.15 else 0)
    save_png("portal_disc", sky.done())


def halo():
    """A smooth violet halo: a round gradient (the whole 64 x 64 is light, fading to nothing at the edge)."""
    n = 128
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    d = np.hypot(xx - n / 2 + 0.5, yy - n / 2 + 0.5) / (n / 2)
    a = np.clip(1.0 - d, 0, 1) ** 3.0
    out = np.zeros((n, n, 4), np.float32)
    out[..., :3] = (C["violet"] * 0.8 + C["cyan"] * 0.2)[None, None, :]
    out[..., 3] = a * 0.8
    save_png("halo", out)


def main():
    halo()
    make_layers()
    make_bases()
    fin_texture()
    portal_textures()
    print("wrote the layer textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()

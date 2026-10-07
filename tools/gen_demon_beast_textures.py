"""Generates the Demon Beast Magic VFX textures: src/main/resources/assets/nusmp/textures/particle/demon_beast_*.png

Look reference (owner art, docs/attributes/art_reference/pack_demon_beast.webp): a ragged, grainy, stippled crimson beast-shaped aura with
horn / ear spikes standing behind the caster, and white double rings of glowing rune glyphs in front of the outstretched hand.
Deterministic (fixed seeds), glyphs are drawn from strokes (no fonts).   python3 -B tools/gen_demon_beast_textures.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 3


def save(arr, name):
    arr = np.clip(arr, 0, 1)
    Image.fromarray((arr * 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "demon_beast_" + name + ".png"))


def noise(n, seed, octaves=5, base=4):
    rng = np.random.RandomState(seed)
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        g = base * (2 ** o)
        f = rng.rand(g + 1, g + 1)
        im = Image.fromarray((f * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC)
        out += amp * np.asarray(im, dtype=float) / 255
        tot += amp
        amp *= 0.55
    out /= tot
    return (out - out.min()) / (out.max() - out.min() + 1e-9)


def grain(n, seed, p):
    return (np.random.RandomState(seed).rand(n, n) < p).astype(float)


def rgba(v, a, tint=(1, 1, 1)):
    h, w = a.shape
    o = np.zeros((h, w, 4))
    for i in range(3):
        o[..., i] = v * tint[i]
    o[..., 3] = a
    return o


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), dtype=float) / 255


def poly_mask(size, polys):
    im = Image.new("L", (size * SS, size * SS), 0)
    d = ImageDraw.Draw(im)
    for p in polys:
        d.polygon([(x * SS, y * SS) for x, y in p], fill=255)
    return np.asarray(im.resize((size, size), Image.LANCZOS), dtype=float) / 255


# ------------------------------------------------------------------------------------------------ beast aura
def beast_aura(seed, n=256, horns=True):
    """Crimson stippled beast-shaped flame: a hunched silhouette with two ear / horn spikes and ragged licks, grainy like the still."""
    rng = np.random.RandomState(seed)
    y, x = np.mgrid[0:n, 0:n] / n
    # body: a tall blob, wider at the bottom, tapering up into a head with two spikes
    body = np.exp(-(((x - 0.5) / (0.30 + 0.12 * y)) ** 2 + ((y - 0.62) / 0.36) ** 2) ** 1.2)
    head = np.exp(-(((x - 0.5) / 0.15) ** 2 + ((y - 0.30) / 0.17) ** 2) ** 1.4)
    shape = np.maximum(body, head)
    if horns:
        for sx in (-1, 1):
            bx = 0.5 + sx * 0.10
            tip = (0.5 + sx * 0.20, 0.03)
            shape = np.maximum(shape, poly_mask(n, [[(bx * n - 9, 0.34 * n), (bx * n + 9, 0.34 * n), (tip[0] * n, tip[1] * n)]]))
    # licks: ragged flame tongues from the edge, by noise-warped threshold
    nz = noise(n, seed, 6, 3)
    warp = noise(n, seed + 1, 4, 5)
    edge = shape + (nz - 0.5) * 0.85 + (warp - 0.5) * 0.35 - 0.38
    mask = np.clip(edge * 5.0, 0, 1)
    # stipple: dense grain inside, sparse at the rim, like a printed halftone
    mottle = noise(n, seed + 2, 5, 6)
    dens = np.clip(mask * (0.55 + 0.6 * mottle), 0, 1)
    g = (rng.rand(n, n) < dens).astype(float)
    g = np.maximum(g * 0.9, mask * 0.30)
    dark = np.clip(0.30 + 0.7 * mottle * (1 - 0.5 * mask), 0, 1)       # darker pockets inside, lighter rim
    a = np.clip(g, 0, 1) * np.clip(mask * 1.5, 0, 1)
    v = np.clip(0.45 + 0.55 * (1 - dark) + 0.25 * (1 - mask), 0, 1)
    return rgba(v, a)


def smoke_puff(seed, n=128):
    nz = noise(n, seed, 5, 3)
    y, x = np.mgrid[0:n, 0:n] / n
    r = np.sqrt((x - .5) ** 2 + (y - .5) ** 2) * 2
    m = np.clip(1 - r, 0, 1) ** 0.8 * (0.35 + nz)
    m = np.clip((m - 0.25) * 2.2, 0, 1)
    g = (np.random.RandomState(seed).rand(n, n) < np.clip(m * 1.2, 0, 1)).astype(float)
    return rgba(0.55 + 0.45 * nz, np.maximum(g * 0.85, m * 0.35) * np.clip(1 - r, 0, 1) ** 0.3)


# ------------------------------------------------------------------------------------------------ rune glyphs
def glyph(d, cx, cy, h, rng, w):
    """One random angular rune (stroke based) centred at cx, cy of height h; mirrors the still's alien script."""
    pts = [(cx + rng.uniform(-0.45, 0.45) * h, cy + rng.uniform(-0.5, 0.5) * h) for _ in range(rng.randint(3, 5))]
    for i in range(len(pts) - 1):
        d.line([pts[i], pts[i + 1]], fill=255, width=w)
    s = rng.randint(0, 3)
    if s == 0:
        d.line([(cx, cy - h / 2), (cx, cy + h / 2)], fill=255, width=w)
    elif s == 1:
        d.arc([cx - h * .3, cy - h * .3, cx + h * .3, cy + h * .3], rng.randint(0, 180), rng.randint(200, 340), fill=255, width=w)
    else:
        r = max(2, w)
        d.ellipse([cx - r, cy + h * .4 - r, cx + r, cy + h * .4 + r], fill=255)


def rune_ring(n=512):
    """Two concentric white rings of rune glyphs between thin double circles, a soft halo: the circle in front of the hand."""
    S = n * 2
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    rng = np.random.RandomState(77)
    c = S / 2
    rings = [(0.93, 0.80, 38), (0.70, 0.57, 26)]
    for ro, ri, cnt in rings:
        for r in (ro, ri):
            d.ellipse([c - r * c, c - r * c, c + r * c, c + r * c], outline=255, width=5)
        rm = (ro + ri) / 2 * c
        gh = (ro - ri) * c * 0.62
        for i in range(cnt):
            a = 2 * math.pi * i / cnt
            gx, gy = c + math.cos(a) * rm, c + math.sin(a) * rm
            tmp = Image.new("L", (int(gh * 2.6), int(gh * 2.6)), 0)
            td = ImageDraw.Draw(tmp)
            glyph(td, tmp.width / 2, tmp.height / 2, gh, rng, 4)
            tmp = tmp.rotate(-math.degrees(a) - 90 + 180, resample=Image.BICUBIC)
            im.paste(255, (int(gx - tmp.width / 2), int(gy - tmp.height / 2)), tmp)
    core = np.asarray(im.resize((n, n), Image.LANCZOS), dtype=float) / 255
    halo = blur(core, 5) * 1.6 + blur(core, 14) * 1.2
    a = np.clip(core + halo * 0.7, 0, 1)
    v = np.clip(0.8 + 0.2 * core, 0, 1)
    return rgba(v, a)


def rune_band(w=512, h=32):
    S = SS
    im = Image.new("L", (w * S, h * S), 0)
    d = ImageDraw.Draw(im)
    rng = np.random.RandomState(31)
    d.line([(0, 3 * S), (w * S, 3 * S)], fill=255, width=2 * S)
    d.line([(0, (h - 3) * S), (w * S, (h - 3) * S)], fill=255, width=2 * S)
    cnt = 16
    for i in range(cnt):
        tmp = Image.new("L", (28 * S, 28 * S), 0)
        glyph(ImageDraw.Draw(tmp), 14 * S, 14 * S, 17 * S, rng, 2 * S)
        im.paste(255, (int((i + 0.5) * w / cnt * S - 14 * S), int(h * S / 2 - 14 * S)), tmp)
    a = np.asarray(im.resize((w, h), Image.LANCZOS), dtype=float) / 255
    a = np.clip(a + blur(a, 1.5) * 0.8, 0, 1)
    return rgba(np.ones_like(a), a)


# ------------------------------------------------------------------------------------------------ claws, fangs, eye, paw, streak
def claw_marks(n=256):
    """Three parallel tapering claw slashes, curved, with ragged torn edges (drawn bottom-left to top-right)."""
    polys = []
    for k, off in enumerate((-0.12, 0.0, 0.12)):
        L, R = [], []
        steps = 40
        for i in range(steps + 1):
            t = i / steps
            x = 0.12 + 0.76 * t
            y = 0.82 - 0.64 * t + 0.22 * math.sin(t * math.pi) * 0.9 + off * (1 - 0.3 * t)
            w = 0.045 * math.sin(math.pi * t) ** 0.6 * (1.0 - 0.15 * k)
            L.append((x * n - w * n * 0.4, y * n - w * n)); R.append((x * n + w * n * 0.4, y * n + w * n))
        polys.append(L + R[::-1])
    m = poly_mask(n, polys)
    nz = noise(n, 5, 5, 8)
    m = np.clip(m * (0.6 + nz * 0.8) * 1.5 - 0.1, 0, 1)
    core = blur(m, 1.2)
    halo = blur(m, 7)
    a = np.clip(core + halo * 0.6, 0, 1)
    return rgba(np.clip(0.6 + core * 0.4, 0, 1), a)


def fang(n=128):
    """A curved horn / fang shard, tip at the top, with a bright edge and a dark grained base."""
    y = np.linspace(0, 1, n)[:, None]
    x = np.linspace(0, 1, n)[None, :]
    cx = 0.5 + 0.12 * (1 - y) ** 1.6
    wid = 0.20 * y ** 0.9
    d = np.abs(x - cx) / (wid + 1e-4)
    m = np.clip((1 - d) * 4, 0, 1) * (y > 0.02)
    edge = np.clip(1 - np.abs(d - 0.8) * 5, 0, 1) * m
    nz = noise(n, 9, 4, 6)
    v = np.clip(0.55 + 0.25 * nz + edge * 0.5 + (1 - y) * 0.2, 0, 1)
    return rgba(v, m)


def beast_eye(n=128):
    """A slit-pupil predator eye with a hot halo and a glint."""
    y, x = np.mgrid[0:n, 0:n] / n
    dx, dy = (x - .5) * 2, (y - .5) * 2
    lid = np.clip(1 - np.abs(dy) / (0.42 * np.clip(1 - dx * dx, 0, 1) ** 0.7 + 1e-3), 0, 1)
    lid = np.clip(lid * 4, 0, 1) * (np.abs(dx) < 1)
    slit = np.clip(1 - np.abs(dx) / (0.10 * np.clip(1 - dy * dy / 0.5, 0, 1) + 1e-3), 0, 1)
    slit = np.clip(slit * 3, 0, 1) * (np.abs(dy) < 0.7)
    r = np.sqrt(dx * dx + dy * dy)
    halo = np.clip(1 - r, 0, 1) ** 2.5
    v = np.clip(lid * (1 - slit * 0.92), 0, 1)
    glint = np.exp(-(((dx + 0.25) ** 2 + (dy + 0.12) ** 2) / 0.004))
    a = np.clip(v * 0.95 + halo * 0.55 + glint, 0, 1)
    return rgba(np.clip(0.55 + v * 0.45 + glint, 0, 1), a)


def paw(n=128):
    polys = []
    def ell(cx, cy, rx, ry, rot=0.0):
        p = []
        for i in range(24):
            t = 2 * math.pi * i / 24
            ex, ey = rx * math.cos(t), ry * math.sin(t)
            p.append((cx + ex * math.cos(rot) - ey * math.sin(rot), cy + ex * math.sin(rot) + ey * math.cos(rot)))
        return p
    polys.append(ell(.5 * n, .66 * n, .22 * n, .17 * n))
    for cx, cy, rot in ((.18, .45, -.5), (.38, .27, -.15), (.62, .27, .15), (.82, .45, .5)):
        polys.append(ell(cx * n, cy * n, .08 * n, .11 * n, rot))
    m = poly_mask(n, polys)
    nz = noise(n, 14, 4, 5)
    m = np.clip(m * (0.75 + 0.5 * nz), 0, 1)
    a = np.clip(blur(m, 1) + blur(m, 5) * 0.4, 0, 1)
    return rgba(np.clip(0.6 + 0.4 * nz, 0, 1), a)


def flame_streak(n=128):
    """Ragged lick streak: U across, V along (bright at the head V=0, tearing off at the tail), grainy."""
    y, x = np.mgrid[0:n, 0:n] / n
    nz = noise(n, 21, 5, 4)
    wob = (noise(n, 22, 3, 3) - 0.5) * 0.3
    w = (1 - y) ** 0.7 * 0.45 * (0.7 + 0.6 * nz)
    d = np.abs(x - 0.5 - wob * y) / (w + 1e-3)
    m = np.clip(1 - d, 0, 1) ** 0.8
    core = np.clip(1 - d * 2.2, 0, 1)
    g = (np.random.RandomState(3).rand(n, n) < np.clip(m * 1.3, 0, 1)).astype(float)
    a = np.clip(np.maximum(g * 0.8, m * 0.45) * np.clip(1 - y, 0, 1) ** 0.4, 0, 1)
    return rgba(np.clip(0.45 + core * 0.55, 0, 1), a)


def ember(n=64):
    y, x = np.mgrid[0:n, 0:n] / n
    r = np.sqrt((x - .5) ** 2 + (y - .5) ** 2) * 2
    a = np.clip(1 - r, 0, 1) ** 2
    cross = np.exp(-((x - .5) / 0.025) ** 2) * np.clip(1 - np.abs(y - .5) * 2, 0, 1) ** 1.5 + np.exp(-((y - .5) / 0.025) ** 2) * np.clip(1 - np.abs(x - .5) * 2, 0, 1) ** 1.5
    return rgba(np.ones((n, n)), np.clip(a + cross * 0.7, 0, 1))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    save(beast_aura(11), "aura")
    save(beast_aura(23, horns=False), "aura_b")
    save(smoke_puff(5), "puff")
    save(rune_ring(), "rune_ring")
    save(rune_band(), "rune_band")
    save(claw_marks(), "claws")
    save(fang(), "fang")
    save(beast_eye(), "eye")
    save(paw(), "paw")
    save(flame_streak(), "streak")
    save(ember(), "ember")
    print("demon_beast textures written to", os.path.abspath(OUT))

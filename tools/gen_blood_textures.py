"""Generates the Blood Magic VFX textures (blood_*.png) for BloodLayer.

Deterministic (fixed seeds), Pillow + numpy only. Output: src/main/resources/assets/nusmp/textures/particle/blood_*.png

    python3 -B tools/gen_blood_textures.py

Look reference (owner still): a giant translucent red sphere with a pink glowing rim, full of white fibre-like veins and a few dark
clots; dozens of thin dark-red threads arcing from the caster on the ground up to it; black ink-like tendrils pooling at the feet.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


def save(name, rgba):
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name)


def noise(h, w, cells, rng, octaves=4):
    """Layered value noise in [0,1]."""
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = cells * (2 ** o)
        g = rng.random((c + 1, c + 1)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32) / 255
        tot += amp
        amp *= 0.5
    return out / tot


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def grid(n):
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    return (x + 0.5) / n * 2 - 1, (y + 0.5) / n * 2 - 1


def blur(a, r):
    return np.asarray(Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255


def rgba(rgb, alpha):
    h, w = alpha.shape
    out = np.zeros((h, w, 4), np.float32)
    for i in range(3):
        out[..., i] = rgb[i] if np.isscalar(rgb[i]) else rgb[i]
    out[..., 3] = alpha * 255
    return out


# ------------------------------------------------------------------------------------------------ sphere
def gen_sphere():
    """The giant blood cell: translucent red body, pink rim glow, white capillary fibres, dark clots. Full colour."""
    n, ss = 256, 2
    rng = np.random.default_rng(101)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    body = smooth(0.93, 0.86, r)
    rim = np.exp(-((r - 0.9) / 0.045) ** 2)
    halo = np.exp(-np.maximum(r - 0.9, 0) / 0.05) * (r > 0.88)
    nz = noise(n, n, 5, rng)
    fib = Image.new("L", (n * ss, n * ss), 0)
    d = ImageDraw.Draw(fib)
    for _ in range(900):
        a = rng.random() * math.tau
        rr = math.sqrt(rng.random()) * 0.88
        px, py = rr * math.cos(a), rr * math.sin(a)
        ang = rng.random() * math.tau
        pts = [(px, py)]
        for _s in range(int(4 + rng.random() * 7)):
            ang += (rng.random() - 0.5) * 1.5
            px += math.cos(ang) * 0.022
            py += math.sin(ang) * 0.022
            if px * px + py * py > 0.84:
                break
            pts.append((px, py))
        if len(pts) > 2:
            d.line([((q[0] + 1) / 2 * n * ss, (q[1] + 1) / 2 * n * ss) for q in pts], fill=int(150 + rng.random() * 105), width=ss)
    fibre = np.asarray(fib.resize((n, n), Image.LANCZOS), np.float32) / 255
    fibre = np.minimum(1, fibre * 1.5) * smooth(0.9, 0.78, r)
    clots = np.zeros((n, n), np.float32)
    for _ in range(9):
        a = rng.random() * math.tau
        rr = math.sqrt(rng.random()) * 0.65
        cx, cy = rr * math.cos(a), rr * math.sin(a)
        s = 0.03 + rng.random() * 0.05
        clots = np.maximum(clots, np.exp(-(((x - cx) / s) ** 2 + ((y - cy) / (s * 0.7)) ** 2) * 1.2) * (0.6 + 0.4 * nz))
    shade = 0.8 + 0.4 * (r ** 2) + 0.25 * (nz - 0.5)
    col = np.zeros((n, n, 3), np.float32)
    base = np.array([238, 52, 62], np.float32)
    pink = np.array([255, 150, 190], np.float32)
    dark = np.array([110, 8, 22], np.float32)
    for i in range(3):
        c = base[i] * shade
        c = c * (1 - rim * 0.8) + pink[i] * rim * 0.8
        c = c * (1 - fibre * 0.85) + 255 * fibre * 0.85
        c = c * (1 - clots * 0.9) + dark[i] * clots * 0.9
        c = c * (1 - halo * 0.6) + pink[i] * halo * 0.6
        col[..., i] = c
    alpha = np.clip(body * (0.58 + 0.12 * nz + 0.35 * r ** 3) + fibre * 0.4 + clots * 0.35 + rim * 0.7 + halo * 0.5, 0, 1)
    alpha *= smooth(1.0, 0.93, r)
    out = np.zeros((n, n, 4), np.float32)
    out[..., :3] = col
    out[..., 3] = alpha * 255
    save("blood_sphere", out)


def gen_fibre():
    """Loose fibre web (white, alpha): a second veined layer that counter-rotates inside the sphere."""
    n, ss = 256, 2
    rng = np.random.default_rng(102)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    im = Image.new("L", (n * ss, n * ss), 0)
    d = ImageDraw.Draw(im)
    for _ in range(420):
        a = rng.random() * math.tau
        rr = math.sqrt(rng.random()) * 0.9
        px, py = rr * math.cos(a), rr * math.sin(a)
        ang = rng.random() * math.tau
        pts = [(px, py)]
        for _s in range(int(6 + rng.random() * 14)):
            ang += (rng.random() - 0.5) * 1.1
            px += math.cos(ang) * 0.02
            py += math.sin(ang) * 0.02
            pts.append((px, py))
        d.line([((q[0] + 1) / 2 * n * ss, (q[1] + 1) / 2 * n * ss) for q in pts], fill=int(120 + rng.random() * 135), width=ss)
    a = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255
    a = np.minimum(1, a * 1.4) * smooth(0.98, 0.8, r)
    a = np.maximum(a, blur(a, 2) * 0.6)
    save("blood_fibre", rgba((255, 235, 238), a))


# ------------------------------------------------------------------------------------------------ threads
def gen_thread():
    """Thin dark-red thread, 32 x 256 (V runs along the length), with tiny beads. Colour is the point."""
    w, h = 32, 256
    rng = np.random.default_rng(103)
    v = np.mgrid[0:h, 0:w][1].astype(np.float32)
    u = (v + 0.5) / w * 2 - 1
    t = (np.mgrid[0:h, 0:w][0].astype(np.float32) + 0.5) / h
    bead = 1 + 0.9 * np.exp(-((((t * 7) % 1) - 0.5) / 0.08) ** 2) * (rng.random() * 0 + 1)
    core = np.exp(-(u / (0.2 * bead)) ** 2)
    halo = np.exp(-(u / 0.55) ** 2) * 0.35
    a = np.clip(core + halo, 0, 1)
    out = np.zeros((h, w, 4), np.float32)
    out[..., 0] = 170 + 85 * core
    out[..., 1] = 14 + 60 * core
    out[..., 2] = 24 + 60 * core
    out[..., 3] = a * 255
    save("blood_thread", out)


def gen_streak():
    """Blood comet for the projectile, 64 x 256: pointed head at the top (V=0), ragged gooey tail fading to the bottom. Grey + alpha."""
    w, h = 64, 256
    rng = np.random.default_rng(104)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    t = (y + 0.5) / h
    u = (x + 0.5) / w * 2 - 1
    wid = np.where(t < 0.18, np.sin(np.minimum(t / 0.18, 1) * math.pi / 2) ** 0.7 * 0.55, 0.55 * np.maximum(1 - (t - 0.18) / 0.82, 0) ** 0.8 + 0.06)
    wob = (noise(h, w, 6, rng, 3) - 0.5) * 0.35 * t
    edge = np.abs(u - wob) / np.maximum(wid, 0.02)
    a = smooth(1.0, 0.7, edge) * (1 - t ** 1.6)
    spray = (noise(h, w, 10, rng, 3) > 0.62).astype(np.float32) * smooth(0.9, 0.3, np.abs(u)) * t * (1 - t) * 1.6
    a = np.clip(np.maximum(a, spray * 0.7), 0, 1)
    shade = np.clip(0.55 + 0.45 * (1 - np.abs(u) / np.maximum(wid, 0.05)) + 0.3 * np.exp(-((u + 0.18) / 0.08) ** 2) * (t < 0.5), 0, 1)
    g = 120 + 135 * shade
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = g[..., None]
    out[..., 3] = a * 255
    save("blood_streak", out)


# ------------------------------------------------------------------------------------------------ droplets / splats
def gen_droplet():
    """Glossy teardrop, tip up, 64 px. Grey so the colour tints it."""
    n = 64
    x, y = grid(n)
    yy = (y + 0.9) / 1.8                       # 0 at the tip, 1 at the base
    wid = np.where(yy < 0.0, 0, np.where(yy < 0.55, 0.62 * np.maximum(yy / 0.55, 0) ** 1.6, 0.62 * np.sqrt(np.maximum(0, 1 - ((yy - 0.55) / 0.45) ** 2))))
    inside = smooth(1.0, 0.8, np.abs(x) / np.maximum(wid, 0.01)) * (yy > 0) * (yy < 1)
    hl = np.exp(-(((x + 0.2) / 0.1) ** 2 + ((y - 0.3) / 0.2) ** 2))
    g = 130 + 70 * (1 - np.abs(x) / np.maximum(wid, 0.05)) + 90 * hl
    out = np.zeros((n, n, 4), np.float32)
    out[..., :3] = np.clip(g, 0, 255)[..., None]
    out[..., 3] = inside * 255
    save("blood_droplet", out)


def gen_splat():
    """Splash: ragged blob, radial streaks, satellite drops. 256 px, grey + alpha."""
    n = 256
    rng = np.random.default_rng(105)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    k = 40
    spikes = np.zeros_like(r)
    ang = rng.random(k) * math.tau
    ln = 0.3 + rng.random(k) * 0.55
    wd = 0.02 + rng.random(k) * 0.05
    for i in range(k):
        dth = np.abs(((th - ang[i] + math.pi) % math.tau) - math.pi)
        spikes = np.maximum(spikes, np.exp(-(dth * r / wd[i]) ** 2) * smooth(ln[i], ln[i] * 0.4, r) * (r > 0.05))
    blobr = 0.3 + 0.1 * (noise(n, n, 6, rng, 3) - 0.5) * 2
    blob = smooth(blobr + 0.04, blobr - 0.06, r)
    a = np.maximum(blob, spikes)
    for _ in range(26):
        s = rng.random() * math.tau
        rr = 0.5 + rng.random() * 0.42
        sz = 0.012 + rng.random() * 0.03
        a = np.maximum(a, np.exp(-(((x - rr * math.cos(s)) ** 2 + (y - rr * math.sin(s)) ** 2) / sz ** 2) * 1.5))
    a = np.clip(a * 1.1, 0, 1)
    shade = 150 + 80 * smooth(0.35, 0.0, r) + 40 * (noise(n, n, 12, rng, 3) - 0.5)
    out = np.zeros((n, n, 4), np.float32)
    out[..., :3] = np.clip(shade, 0, 255)[..., None]
    out[..., 3] = a * 255
    save("blood_splat", out)


def gen_clot():
    """Dark clump with glossy edge, 64 px (used as heavy debris). Grey + alpha."""
    n = 64
    rng = np.random.default_rng(106)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    rr = 0.62 + 0.18 * (noise(n, n, 4, rng, 3) - 0.5) * 2
    a = smooth(rr, rr - 0.12, r)
    g = 90 + 120 * np.exp(-(((x + 0.25) / 0.15) ** 2 + ((y + 0.25) / 0.12) ** 2)) + 50 * (1 - r)
    out = np.zeros((n, n, 4), np.float32)
    out[..., :3] = np.clip(g, 0, 255)[..., None]
    out[..., 3] = a * 255
    save("blood_clot", out)


# ------------------------------------------------------------------------------------------------ ground
def gen_tendril():
    """Black ink pool with curling tendrils reaching out of it (the shadow at the caster's feet). Near-black + alpha, dark red sheen."""
    n, ss = 256, 2
    rng = np.random.default_rng(107)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    pool_r = 0.34 + 0.07 * np.sin(th * 5 + 1) + 0.04 * np.sin(th * 11)
    pool = smooth(pool_r + 0.03, pool_r - 0.03, r)
    im = Image.new("L", (n * ss, n * ss), 0)
    d = ImageDraw.Draw(im)

    def tendril(a0, length, w0, depth):
        px, py = 0.25 * math.cos(a0), 0.25 * math.sin(a0)
        ang = a0
        curl = (rng.random() - 0.5) * 0.35
        steps = 34
        for s in range(steps):
            t = s / steps
            ang += curl + (rng.random() - 0.5) * 0.18
            step = length / steps
            nx, ny = px + math.cos(ang) * step, py + math.sin(ang) * step
            w = max(1, w0 * (1 - t) ** 0.9 * n * ss / 2)
            d.line([((px + 1) / 2 * n * ss, (py + 1) / 2 * n * ss), ((nx + 1) / 2 * n * ss, (ny + 1) / 2 * n * ss)], fill=255, width=int(w) + 1)
            if depth > 0 and s in (12, 22) and rng.random() < 0.8:
                tendril(ang + (rng.random() - 0.5) * 1.7, length * 0.35, w0 * 0.5, depth - 1)
            px, py = nx, ny

    for i in range(14):
        tendril(math.tau * i / 14 + rng.random() * 0.3, 0.45 + rng.random() * 0.25, 0.034, 1)
    t = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255
    a = np.clip(np.maximum(pool, t), 0, 1) * smooth(1.0, 0.9, r)
    sheen = np.exp(-((r - 0.25) / 0.12) ** 2) * pool * (0.4 + 0.6 * noise(n, n, 8, rng, 3))
    out = np.zeros((n, n, 4), np.float32)
    out[..., 0] = 14 + 120 * sheen
    out[..., 1] = 3 + 6 * sheen
    out[..., 2] = 8 + 22 * sheen
    out[..., 3] = a * 255
    save("blood_tendril", out)


def gen_sigil():
    """Vein ring sigil for the ground, 256 px: double ring, capillary network between, ticks and cells. White + alpha."""
    n, ss = 256, 2
    rng = np.random.default_rng(108)
    im = Image.new("L", (n * ss, n * ss), 0)
    d = ImageDraw.Draw(im)
    c = n * ss / 2

    def circ(rad, w, fill=255):
        R = rad * c
        d.ellipse([c - R, c - R, c + R, c + R], outline=fill, width=w)

    circ(0.96, 3)
    circ(0.9, 2, 200)
    circ(0.62, 3)
    circ(0.56, 1, 170)
    for i in range(48):
        a = math.tau * i / 48
        r0, r1 = (0.9, 0.96) if i % 4 else (0.84, 0.96)
        d.line([(c + math.cos(a) * r0 * c, c + math.sin(a) * r0 * c), (c + math.cos(a) * r1 * c, c + math.sin(a) * r1 * c)], fill=230, width=2)
    for _ in range(70):                          # capillaries bridging the two bands
        a = rng.random() * math.tau
        r = 0.62 + rng.random() * 0.28
        pts = []
        ang = a
        for s in range(int(5 + rng.random() * 8)):
            pts.append((c + math.cos(ang) * r * c, c + math.sin(ang) * r * c))
            r += (rng.random() - 0.4) * 0.03
            ang += (rng.random() - 0.5) * 0.05
            r = min(0.9, max(0.62, r))
        if len(pts) > 1:
            d.line(pts, fill=int(110 + rng.random() * 120), width=1)
    for i in range(8):                           # large cells on the inner band
        a = math.tau * (i + 0.5) / 8
        rr = 0.76
        R = 0.05 * c
        d.ellipse([c + math.cos(a) * rr * c - R, c + math.sin(a) * rr * c - R, c + math.cos(a) * rr * c + R, c + math.sin(a) * rr * c + R], outline=255, width=2)
    for i in range(8):                           # inner star of arterial lines
        a = math.tau * i / 8
        d.line([(c + math.cos(a) * 0.12 * c, c + math.sin(a) * 0.12 * c), (c + math.cos(a) * 0.56 * c, c + math.sin(a) * 0.56 * c)], fill=190, width=2)
    a = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255
    a = np.clip(a + blur(a, 2.5) * 0.7, 0, 1)
    save("blood_sigil", rgba((255, 255, 255), a))


def gen_flare():
    """Hot core flash: soft glow with a four-point cross and a ring halo. 128 px, white + alpha."""
    n = 128
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    core = np.exp(-(r / 0.16) ** 2)
    glow = np.exp(-(r / 0.45) ** 2) * 0.55
    cross = (np.exp(-(x / 0.03) ** 2) * np.exp(-np.abs(y) * 2.2) + np.exp(-(y / 0.03) ** 2) * np.exp(-np.abs(x) * 2.2)) * 0.9
    ring = np.exp(-((r - 0.78) / 0.04) ** 2) * 0.35
    a = np.clip(core + glow + cross + ring, 0, 1) * smooth(1.0, 0.85, r)
    save("blood_flare", rgba((255, 255, 255), a))


if __name__ == "__main__":
    gen_sphere()
    gen_fibre()
    gen_thread()
    gen_streak()
    gen_droplet()
    gen_splat()
    gen_clot()
    gen_tendril()
    gen_sigil()
    gen_flare()

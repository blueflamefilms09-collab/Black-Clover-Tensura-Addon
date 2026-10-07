"""Generates the Key Magic VFX textures (ornate golden keys, keyholes, ethereal doors, spatial rifts, sigils).

Deterministic (fixed seeds, no fonts). White / grey with alpha, so the vertex colour tints them gold.
Output: src/main/resources/assets/nusmp/textures/particle/key_*.png

    python3 -B tools/gen_key_textures.py

Sprites: key_key (ornate key, bow at the bottom, bit at the top), key_keyhole, key_door (arched ethereal door),
key_sigil (ground sigil of keyholes), key_ring (shock ring), key_rift (spatial slit), key_glint (lens flare),
key_shard (golden fragment), key_streak (beam trail with chevrons).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def canvas(w, h):
    return Image.new("L", (w * SS, h * SS), 0)


def poly(d, pts, v=255):
    d.polygon([(x * SS, y * SS) for x, y in pts], fill=v)


def ell(d, cx, cy, rx, ry, v=255):
    d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=v)


def rect(d, x0, y0, x1, y1, v=255):
    d.rectangle([x0 * SS, y0 * SS, x1 * SS, y1 * SS], fill=v)


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), dtype=np.float32) / 255.0


def arr(img, w, h):
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float32) / 255.0


def noise(w, h, seed, octaves=4, base=4):
    rng = np.random.RandomState(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = base * (2 ** o)
        g = rng.rand(n + 1, n + 1).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, dtype=np.float32) / 255.0
        tot += amp
        amp *= 0.5
    return out / tot


def metal(mask, w, h, seed, bevel=2.2, light=(-0.6, -0.8), grain=0.08):
    """Brushed, bevelled metal shading of a binary mask: returns (grey, alpha)."""
    m = mask
    soft = blur(m, bevel)
    gy, gx = np.gradient(soft)
    k = 9.0 / bevel
    shade = 0.72 + (gx * light[0] + gy * light[1]) * k
    inner = blur(m, bevel * 3.0)
    shade += 0.18 * (inner - 0.5)
    shade += (noise(w, h, seed, 4, 8) - 0.5) * grain * 2
    edge = np.clip(m - blur(m, 1.0), 0, 1)
    shade += edge * 0.5
    shade = np.clip(shade, 0.25, 1.0)
    return shade * m, m


def save(name, grey, alpha):
    h, w = grey.shape
    g = (np.clip(grey, 0, 1) * 255).astype(np.uint8)
    a = (np.clip(alpha, 0, 1) * 255).astype(np.uint8)
    rgba = np.stack([g, g, g, a], axis=-1)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name, w, h)


def glow_field(w, h, cx, cy, sx, sy, p=2.0):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    r = np.sqrt(((x - cx) / sx) ** 2 + ((y - cy) / sy) ** 2)
    return np.clip(1 - r, 0, 1) ** p


# ------------------------------------------------------------------------------------------------ the ornate key (128 x 256)
def tex_key():
    W, H = 128, 256
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    cx = 64
    # bow: ring with a trefoil of lobes and a gem hole
    ell(d, cx, 200, 38, 38)
    for a in (-90, 30, 150):
        ell(d, cx + 40 * math.cos(math.radians(a)), 200 + 40 * math.sin(math.radians(a)), 15, 15)
    ell(d, cx, 200, 20, 20, 0)
    for a in (-90, 30, 150):
        ell(d, cx + 38 * math.cos(math.radians(a)), 200 + 38 * math.sin(math.radians(a)), 6, 6, 0)
    # neck and shaft with collars and a bead
    poly(d, [(cx - 14, 164), (cx + 14, 164), (cx + 7, 150), (cx - 7, 150)])
    rect(d, cx - 6, 40, cx + 6, 152)
    for y0, hw in ((140, 15), (122, 12)):
        rect(d, cx - hw, y0, cx + hw, y0 + 7)
    ell(d, cx, 100, 11, 15)
    ell(d, cx, 84, 8, 4)
    # bit: toothed plate to the right, with a fleur on the left of the tip
    poly(d, [(cx + 5, 40), (cx + 34, 40), (cx + 34, 52), (cx + 22, 52), (cx + 22, 58), (cx + 40, 58), (cx + 40, 72), (cx + 20, 72), (cx + 20, 80), (cx + 5, 80)])
    poly(d, [(cx - 5, 40), (cx - 5, 26), (cx, 12), (cx + 5, 26), (cx + 5, 40)])
    ell(d, cx, 14, 8, 8)
    ell(d, cx - 12, 30, 5, 5)
    ell(d, cx + 12, 26, 4, 4)
    mask = arr(c, W, H)
    grey, alpha = metal(mask, W, H, 11)
    # engraved line inside the bow and along the shaft
    c2 = canvas(W, H)
    d2 = ImageDraw.Draw(c2)
    d2.ellipse([(cx - 30) * SS, (200 - 30) * SS, (cx + 30) * SS, (200 + 30) * SS], outline=255, width=SS)
    d2.line([(cx * SS, 60 * SS), (cx * SS, 146 * SS)], fill=255, width=SS)
    eng = arr(c2, W, H) * mask
    grey = grey * (1 - 0.45 * eng)
    # gem glint on the bow
    gem = glow_field(W, H, cx, 200, 9, 9, 1.0)
    grey = np.clip(grey + gem * 0.5, 0, 1)
    save("key_key", grey, alpha)


# ------------------------------------------------------------------------------------------------ keyhole (128)
def tex_keyhole():
    W = H = 128
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    ell(d, 64, 54, 20, 20)
    poly(d, [(50, 68), (78, 68), (86, 112), (42, 112)])
    mask = arr(c, W, H)
    halo = blur(mask, 7)
    halo2 = blur(mask, 2.4)
    rim = np.clip(halo2 - mask * 0.0, 0, 1)
    ring = np.zeros((H, W), np.float32)
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    r = np.sqrt((x - 64) ** 2 + (y - 64) ** 2)
    ring = np.exp(-((r - 58) / 2.2) ** 2) * 0.8 + np.exp(-((r - 50) / 1.2) ** 2) * 0.5
    core = 1 - mask  # the hole is dark void, the glow wraps it
    val = np.clip(rim * 1.0 * core + halo * 0.5 * core + ring * 0.9, 0, 1)
    # inner light leaking out of the hole (white-hot centre)
    leak = blur(mask, 3) * 0.55
    grey = np.clip(val + leak * mask, 0, 1)
    alpha = np.clip(val * 1.1 + mask * 0.9, 0, 1)
    save("key_keyhole", np.maximum(grey, mask * 0.9), alpha)


# ------------------------------------------------------------------------------------------------ ethereal door (128 x 256)
def tex_door():
    W, H = 128, 256
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    # outer arched frame
    ell(d, 64, 76, 56, 62)
    rect(d, 8, 76, 120, 250)
    frame = arr(c, W, H)
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    ell(d, 64, 78, 46, 52)
    rect(d, 18, 78, 110, 240)
    panel = arr(c, W, H)
    ring_mask = np.clip(frame - panel, 0, 1)
    grey, alpha = metal(ring_mask, W, H, 5, bevel=2.5)
    # panel: faint glowing veil with a vertical gradient and panelled mouldings
    yy = np.linspace(0, 1, H, dtype=np.float32)[:, None] * np.ones((1, W), np.float32)
    veil = (0.35 + 0.5 * yy) * panel
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    for box in ((28, 56, 62, 130), (66, 56, 100, 130), (28, 140, 62, 226), (66, 140, 100, 226)):
        d.rectangle([box[0] * SS, box[1] * SS, box[2] * SS, box[3] * SS], outline=255, width=SS)
    mould = arr(c, W, H) * panel
    veil = veil * (1 - 0.35 * mould) + mould * 0.45
    veil += (noise(W, H, 21, 4, 4) - 0.5) * 0.18 * panel
    # keyhole in the middle of the leaf
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    ell(d, 64, 134, 7, 7)
    poly(d, [(60, 138), (68, 138), (71, 156), (57, 156)])
    kh = arr(c, W, H)
    veil = np.clip(veil + blur(kh, 4) * 0.9, 0, 1) * (1 - 0.5 * kh) + kh * 0.0
    out_g = np.where(ring_mask > 0.05, grey, veil)
    out_a = np.clip(ring_mask + panel * (0.38 + 0.4 * yy), 0, 1)
    # soft outer halo
    halo = blur(frame, 6) * 0.35
    out_g = np.clip(out_g + halo * (1 - frame), 0, 1)
    out_a = np.clip(out_a + halo, 0, 1)
    save("key_door", out_g, out_a)


# ------------------------------------------------------------------------------------------------ ground sigil (256)
def tex_sigil():
    W = H = 256
    cx = cy = 128
    c = canvas(W, H)
    d = ImageDraw.Draw(c)

    def circ(r, w):
        d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=255, width=int(w * SS))

    circ(124, 3)
    circ(116, 1.2)
    circ(86, 2)
    circ(80, 1)
    circ(44, 2.2)
    # key-bit teeth between the two outer rings
    for i in range(48):
        a = 2 * math.pi * i / 48
        r0, r1 = 117, 117 + (5 if i % 2 else 2)
        d.line([((cx + r0 * math.cos(a)) * SS, (cy + r0 * math.sin(a)) * SS), ((cx + r1 * math.cos(a)) * SS, (cy + r1 * math.sin(a)) * SS)], fill=255, width=SS)
    # six keyholes around the middle ring
    for i in range(6):
        a = 2 * math.pi * i / 6 - math.pi / 2
        ux, uy = math.cos(a), math.sin(a)
        px, py = -uy, ux
        bx, by = cx + 99 * ux, cy + 99 * uy
        ell(d, bx, by, 6.5, 6.5)
        tip = (bx + 20 * ux, by + 20 * uy)
        poly(d, [(bx + 4 * px, by + 4 * py), (bx - 4 * px, by - 4 * py), (tip[0] - 7 * px, tip[1] - 7 * py), (tip[0] + 7 * px, tip[1] + 7 * py)])
        # inward chevron between keyholes
        a2 = a + math.pi / 6
        q = [(cx + 84 * math.cos(a2), cy + 84 * math.sin(a2)), (cx + 66 * math.cos(a2 + 0.07), cy + 66 * math.sin(a2 + 0.07)), (cx + 66 * math.cos(a2 - 0.07), cy + 66 * math.sin(a2 - 0.07))]
        poly(d, q)
    # hexagram of thin lines inside
    for k in range(2):
        pts = [(cx + 80 * math.cos(math.pi / 6 + k * math.pi / 3 + j * 2 * math.pi / 3), cy + 80 * math.sin(math.pi / 6 + k * math.pi / 3 + j * 2 * math.pi / 3)) for j in range(3)]
        d.line([(x * SS, y * SS) for x, y in pts + [pts[0]]], fill=255, width=SS)
    # centre keyhole
    ell(d, cx, cy - 6, 14, 14)
    poly(d, [(cx - 8, cy + 4), (cx + 8, cy + 4), (cx + 13, cy + 30), (cx - 13, cy + 30)])
    line = arr(c, W, H)
    glow = blur(line, 3) * 0.6
    grey = np.clip(line * (0.82 + 0.18 * noise(W, H, 4, 4, 8)) + glow, 0, 1)
    alpha = np.clip(line + glow * 0.9, 0, 1)
    # faint disc fill so the ground reads
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    r = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
    fill = np.clip(1 - r / 124, 0, 1) ** 0.6 * (r < 122) * 0.12
    grey = np.clip(grey + fill, 0, 1)
    alpha = np.clip(alpha + fill, 0, 1)
    save("key_sigil", grey, alpha)


# ------------------------------------------------------------------------------------------------ shock ring (256)
def tex_ring():
    W = H = 256
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    r = np.sqrt((x - 128) ** 2 + (y - 128) ** 2)
    ang = np.arctan2(y - 128, x - 128)
    n = noise(W, H, 8, 4, 6)
    wob = (n - 0.5) * 6
    band = np.exp(-((r - 112 + wob) / 3.2) ** 2)
    fine = np.exp(-((r - 104 + wob) / 1.2) ** 2) * 0.6
    spikes = (np.abs(np.sin(ang * 24)) ** 12) * np.exp(-((r - 118) / 6.0) ** 2) * 0.9
    halo = np.exp(-((r - 110) / 14.0) ** 2) * 0.35
    v = np.clip(band + fine + spikes + halo, 0, 1)
    save("key_ring", np.clip(v * 1.1, 0, 1), v)


# ------------------------------------------------------------------------------------------------ spatial rift (128 x 256)
def tex_rift():
    W, H = 128, 256
    rng = np.random.RandomState(31)
    # jagged centre line
    ys = np.linspace(0, H - 1, 40)
    xs = 64 + np.cumsum((rng.rand(40) - 0.5) * 10) * 0.9
    xs -= (xs - 64).mean()
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    pts_l, pts_r = [], []
    for i, (xx, yy) in enumerate(zip(xs, ys)):
        t = yy / (H - 1)
        hw = 16 * math.sin(math.pi * t) ** 0.8 + 0.8
        pts_l.append(((xx - hw), yy))
        pts_r.append(((xx + hw), yy))
    poly(d, pts_l + pts_r[::-1])
    mask = arr(c, W, H)
    inner = blur(mask, 2.5)
    edge = np.clip(blur(mask, 1.6) - mask * 0.55, 0, 1)
    halo = blur(mask, 9)
    cracks = np.zeros((H, W), np.float32)
    c2 = canvas(W, H)
    d2 = ImageDraw.Draw(c2)
    for k in range(9):
        i = rng.randint(4, 36)
        x0, y0 = xs[i], ys[i]
        a = rng.choice([-1, 1]) * (0.5 + rng.rand() * 1.0) + math.pi / 2 * rng.choice([0, 1])
        pts = [(x0, y0)]
        for j in range(5):
            a += (rng.rand() - 0.5) * 1.0
            pts.append((pts[-1][0] + 9 * math.cos(a), pts[-1][1] + 9 * math.sin(a)))
        d2.line([(x * SS, y * SS) for x, y in pts], fill=255, width=SS)
    cracks = arr(c2, W, H) * np.clip(1 - mask, 0, 1)
    cracks = blur(cracks, 0.7)
    # interior: bright white-gold light, the edge is hottest
    grey = np.clip(0.7 * inner + edge * 1.2 + halo * 0.5 + cracks * 0.7, 0, 1)
    alpha = np.clip(mask + edge + halo * 0.7 + cracks * 0.8, 0, 1)
    save("key_rift", grey, alpha)


# ------------------------------------------------------------------------------------------------ lens glint (128)
def tex_glint():
    W = H = 128
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    dx, dy = (x - 64) / 64.0, (y - 64) / 64.0
    r = np.sqrt(dx * dx + dy * dy) + 1e-4
    core = np.exp(-(r / 0.09) ** 2)
    halo = np.exp(-(r / 0.28) ** 2) * 0.55
    spike_v = np.exp(-(np.abs(dx) / 0.025)) * np.clip(1 - np.abs(dy), 0, 1) ** 2.4
    spike_h = np.exp(-(np.abs(dy) / 0.025)) * np.clip(1 - np.abs(dx), 0, 1) ** 2.4
    d1 = np.abs(dx - dy) / 1.414
    d2 = np.abs(dx + dy) / 1.414
    diag = (np.exp(-d1 / 0.02) + np.exp(-d2 / 0.02)) * np.clip(1 - r, 0, 1) ** 4 * 0.45
    ring = np.exp(-((r - 0.62) / 0.025) ** 2) * 0.35
    v = np.clip(core + halo + spike_v * 0.9 + spike_h * 0.9 + diag + ring, 0, 1)
    v *= np.clip(1.15 - r, 0, 1) ** 0.5
    save("key_glint", v, v)


# ------------------------------------------------------------------------------------------------ golden shard (64)
def tex_shard():
    W = H = 64
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    poly(d, [(32, 3), (44, 24), (38, 61), (28, 46), (20, 26)])
    mask = arr(c, W, H)
    grey, alpha = metal(mask, W, H, 3, bevel=1.6, grain=0.12)
    c = canvas(W, H)
    d = ImageDraw.Draw(c)
    d.line([(32 * SS, 6 * SS), (31 * SS, 52 * SS)], fill=255, width=SS)
    d.line([(32 * SS, 24 * SS), (42 * SS, 26 * SS)], fill=255, width=SS)
    facet = arr(c, W, H) * mask
    grey = np.clip(grey + facet * 0.35, 0, 1)
    save("key_shard", grey, alpha)


# ------------------------------------------------------------------------------------------------ beam streak (64 x 256)
def tex_streak():
    W, H = 64, 256
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    dx = np.abs(x - 32) / 32.0
    t = y / H  # 0 = head (top), 1 = tail
    body = np.exp(-(dx / (0.18 + 0.5 * t)) ** 2)
    core = np.exp(-(dx / 0.07) ** 2)
    # chevrons of light travelling along the beam
    chev = np.clip(np.sin((t * 10 + dx * 2.2) * math.pi * 2), 0, 1) ** 6 * np.exp(-(dx / 0.7) ** 2) * 0.45
    n = noise(W, H, 15, 3, 4)
    v = (body * (0.55 + 0.45 * n) + core * 0.8 + chev) * (1 - t) ** 0.8
    v = np.clip(v, 0, 1)
    save("key_streak", v, v)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    tex_key()
    tex_keyhole()
    tex_door()
    tex_sigil()
    tex_ring()
    tex_rift()
    tex_glint()
    tex_shard()
    tex_streak()

"""Sprite makers shared by the particle texture generators of the 1.93.0 rebuild of Shakudo, Scale, Spike, Nail and Hair Magic
(tools/gen_shakudo_textures.py, gen_scale_textures.py, gen_spike_textures.py, gen_nail_textures.py, gen_hair_textures.py).

Every sprite is grey + alpha (the vertex colour tints it), square unless stated, drawn at 4x and reduced. Deterministic (fixed seeds).
Output: src/main/resources/assets/nusmp/textures/particle/<name>.png (read by VfxTextures.byName and by the armour accents).
"""
import math
import os
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def canvas(w, h=None):
    h = h or w
    return Image.new("L", (w * SS, h * SS), 0)


def to_rgba(grey, alpha=None):
    """grey: PIL 'L' (supersampled) -> RGBA at the final size; alpha defaults to the grey itself."""
    w, h = grey.size[0] // SS, grey.size[1] // SS
    g = np.asarray(grey.resize((w, h), Image.LANCZOS), np.float32) / 255
    a = g if alpha is None else np.asarray(alpha.resize((w, h), Image.LANCZOS), np.float32) / 255
    rgb = np.clip(g, 0, 1) * 255
    return Image.fromarray(np.dstack([rgb, rgb, rgb, np.clip(a, 0, 1) * 255]).astype(np.uint8), "RGBA")


def mask_rgba(mask, shade):
    """mask: PIL L (alpha), shade: PIL L (grey), both supersampled."""
    return to_rgba(shade, mask)


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0 + 1e-9), 0, 1)
    return t * t * (3 - 2 * t)


def blur(im, r):
    return im.filter(ImageFilter.GaussianBlur(r * SS))


def arr_rgba(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def glow_blob(size, power=2.2, core=0.9):
    x, y = grid(size, size)
    r = np.sqrt(x * x + y * y)
    a = np.clip(1 - r, 0, 1) ** power
    g = np.clip(a * 1.0 + core * np.exp(-r * r * 18), 0, 1)
    return arr_rgba(np.dstack([g * 255, g * 255, g * 255, a * 255]))


def ring_sprite(size, r0=0.78, r1=0.96, soft=0.05, detail=True, seed=1):
    x, y = grid(size, size)
    r = np.sqrt(x * x + y * y)
    a = smooth(r0 - soft, r0, r) * (1 - smooth(r1, r1 + soft, r))
    if detail:
        rng = np.random.default_rng(seed)
        th = np.arctan2(y, x)
        a = a * (0.7 + 0.3 * np.sin(th * 24 + rng.uniform(0, 6)) ** 2)
    g = np.clip(0.55 + 0.45 * a, 0, 1)
    return arr_rgba(np.dstack([g * 255, g * 255, g * 255, a * 255]))


def flare_sprite(size, arms=4, length=0.95):
    x, y = grid(size, size)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    star = np.zeros_like(r)
    for k in range(arms):
        ang = math.pi * k / arms
        d = np.abs(np.sin(th - ang)) * r
        along = np.abs(np.cos(th - ang)) * r
        star = np.maximum(star, np.exp(-(d / 0.035) ** 2) * np.clip(1 - along / length, 0, 1) ** 1.5)
    core = np.exp(-r * r * 22)
    a = np.clip(star + core, 0, 1)
    return arr_rgba(np.dstack([a * 255, a * 255, a * 255, a * 255]))


def streak_sprite(w, h, tail_fade=True):
    """A long soft speed-line sprite: bright thin core across, fading toward the tail (top)."""
    x, y = grid(w, h)
    across = np.exp(-(x / 0.28) ** 2) * 0.8 + np.exp(-(x / 0.07) ** 2) * 0.9
    along = smooth(-1.0, 0.7, y) if tail_fade else np.ones_like(y)
    tip = 1 - smooth(0.85, 1.0, y)
    a = np.clip(across * along * tip, 0, 1)
    return arr_rgba(np.dstack([a * 255, a * 255, a * 255, a * 255]))


def poly_sprite(size, pts, seed, shade_light=(-0.6, -0.8), base=0.64, spread=0.3, outline=0.05, cuts=5):
    """A faceted cel-shaded polygon: flat-shaded triangle facets fanned from a hub, dark outline and creases. pts in 0..1."""
    rng = np.random.default_rng(seed)
    S = size * SS
    msk = Image.new("L", (S, S), 0)
    im = Image.new("L", (S, S), 0)
    P = [(x * S, y * S) for x, y in pts]
    ImageDraw.Draw(msk).polygon(P, fill=255)
    cx = sum(p[0] for p in P) / len(P) + rng.uniform(-0.04, 0.04) * S
    cy = sum(p[1] for p in P) / len(P) + rng.uniform(-0.04, 0.04) * S
    d = ImageDraw.Draw(im)
    n = len(P)
    lines = []
    for i in range(n):
        a, b = P[i], P[(i + 1) % n]
        mx, my = (a[0] + b[0]) / 2 - cx, (a[1] + b[1]) / 2 - cy
        ln = math.hypot(mx, my) + 1e-6
        sh = base + spread * ((mx / ln) * shade_light[0] + (my / ln) * shade_light[1]) + rng.uniform(-0.05, 0.05)
        d.polygon([(cx, cy), a, b], fill=int(np.clip(sh, 0, 1) * 255))
        lines.append(((cx, cy), a))
    for _ in range(cuts):
        a = P[rng.integers(n)]
        t = rng.uniform(0.3, 0.75)
        lines.append(((cx + (a[0] - cx) * t, cy + (a[1] - cy) * t), (cx + (a[0] - cx) * 0.12, cy + (a[1] - cy) * 0.12)))
    ln = Image.new("L", (S, S), 0)
    ld = ImageDraw.Draw(ln)
    for a, b in lines:
        ld.line([a, b], fill=255, width=max(2, int(S * outline * 0.3)))
    ld.line(P + [P[0]], fill=255, width=max(3, int(S * outline)), joint="curve")
    shade = np.asarray(im, np.float32) * (1 - np.asarray(ln, np.float32) / 255 * 0.9)
    return to_rgba(Image.fromarray(shade.astype(np.uint8)), msk)


def crack_sprite(size, seed, branches=9):
    rng = np.random.default_rng(seed)
    S = size * SS
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2

    def grow(x, y, ang, length, width, depth):
        if length < S * 0.03 or depth > 4:
            return
        pts = [(x, y)]
        steps = 6
        for _ in range(steps):
            ang += rng.uniform(-0.35, 0.35)
            x += math.cos(ang) * length / steps
            y += math.sin(ang) * length / steps
            pts.append((x, y))
        d.line(pts, fill=255, width=max(1, int(width)), joint="curve")
        if rng.random() < 0.7:
            grow(x, y, ang + rng.uniform(0.4, 0.9), length * 0.55, width * 0.7, depth + 1)
        if rng.random() < 0.5:
            grow(pts[2][0], pts[2][1], ang - rng.uniform(0.5, 1.0), length * 0.5, width * 0.7, depth + 1)
    for i in range(branches):
        grow(c, c, 2 * math.pi * i / branches + rng.uniform(-0.2, 0.2), S * rng.uniform(0.28, 0.46), S * 0.014, 0)
    g = blur(im, 0.5)
    glo = blur(im, 3.0)
    a = np.clip(np.asarray(g, np.float32) / 255 + np.asarray(glo, np.float32) / 255 * 0.6, 0, 1)
    return to_rgba(Image.fromarray((a * 255).astype(np.uint8)))


def polar_sigil(size, motif, seed, rings=(0.97, 0.92, 0.62, 0.58, 0.3), ticks=48, motif_count=12):
    """A rune circle: concentric rings, a tick band and a ring of motifs ('scroll', 'scallop', 'spike', 'nail', 'curl', 'plate')."""
    rng = np.random.default_rng(seed)
    S = size * SS
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    for i, r in enumerate(rings):
        w = max(2, int(S * (0.012 if i % 2 == 0 else 0.006)))
        d.ellipse([c - r * c, c - r * c, c + r * c, c + r * c], outline=255 if i % 2 == 0 else 180, width=w)
    r0, r1 = 0.92 * c, 0.97 * c
    for k in range(ticks):
        a = 2 * math.pi * k / ticks
        rr = r0 if k % 4 else r0 - 0.03 * c
        d.line([(c + math.cos(a) * rr, c + math.sin(a) * rr), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=200, width=max(1, int(S * 0.004)))
    rm = 0.76 * c
    for k in range(motif_count):
        a = 2 * math.pi * k / motif_count
        px, py = c + math.cos(a) * rm, c + math.sin(a) * rm
        ux, uy = math.cos(a), math.sin(a)
        vx, vy = -uy, ux
        s = 0.1 * c
        if motif == "spike":
            d.polygon([(px + ux * s * 1.4, py + uy * s * 1.4), (px - vx * s * 0.6 - ux * s * 0.8, py - vy * s * 0.6 - uy * s * 0.8),
                       (px + vx * s * 0.6 - ux * s * 0.8, py + vy * s * 0.6 - uy * s * 0.8)], fill=255)
        elif motif == "nail":
            d.line([(px - ux * s, py - uy * s), (px + ux * s * 1.3, py + uy * s * 1.3)], fill=255, width=max(2, int(S * 0.008)))
            d.line([(px - ux * s - vx * s * 0.55, py - uy * s - vy * s * 0.55), (px - ux * s + vx * s * 0.55, py - uy * s + vy * s * 0.55)], fill=255, width=max(2, int(S * 0.012)))
        elif motif == "scallop":
            for j in (-1, 0, 1):
                qx, qy = px + vx * s * 0.7 * j, py + vy * s * 0.7 * j
                d.arc([qx - s * 0.5, qy - s * 0.5, qx + s * 0.5, qy + s * 0.5], start=math.degrees(a) - 10, end=math.degrees(a) + 190, fill=255, width=max(2, int(S * 0.007)))
        elif motif == "curl":
            pts = []
            for t in range(24):
                tt = t / 23.0
                ang = a + tt * 5.0 * (1 if k % 2 == 0 else -1)
                rad = s * 1.1 * (1 - tt)
                pts.append((px + math.cos(ang) * rad, py + math.sin(ang) * rad))
            d.line(pts, fill=255, width=max(2, int(S * 0.007)), joint="curve")
        elif motif == "plate":
            d.polygon([(px + ux * s - vx * s * 0.8, py + uy * s - vy * s * 0.8), (px + ux * s + vx * s * 0.8, py + uy * s + vy * s * 0.8),
                       (px - ux * s + vx * s * 0.8, py - uy * s + vy * s * 0.8), (px - ux * s - vx * s * 0.8, py - uy * s - vy * s * 0.8)], outline=255, width=max(2, int(S * 0.007)))
            d.ellipse([px - s * 0.18, py - s * 0.18, px + s * 0.18, py + s * 0.18], fill=255)
        else:                                    # scroll: a small spiral with a tail
            pts = []
            for t in range(20):
                tt = t / 19.0
                ang = a + math.pi + tt * 4.2
                rad = s * 0.9 * (1 - tt * 0.8)
                pts.append((px + math.cos(ang) * rad, py + math.sin(ang) * rad))
            d.line(pts, fill=255, width=max(2, int(S * 0.007)), joint="curve")
    # an inner star of lines
    pts = int(rng.integers(5, 9))
    for k in range(pts):
        a0 = 2 * math.pi * k / pts + rng.uniform(0, 0.05)
        a1 = a0 + 2 * math.pi * (pts // 2) / pts
        d.line([(c + math.cos(a0) * 0.3 * c, c + math.sin(a0) * 0.3 * c), (c + math.cos(a1) * 0.3 * c, c + math.sin(a1) * 0.3 * c)], fill=210, width=max(2, int(S * 0.005)))
    soft = blur(im, 1.2)
    a = np.clip(np.asarray(im, np.float32) / 255 + np.asarray(soft, np.float32) / 255 * 0.5, 0, 1)
    return to_rgba(Image.fromarray((a * 255).astype(np.uint8)))


def spike_sprite(size, seed, lean=0.0, facets=True):
    """A tapered spike pointing up with a split facet and a dark outline (pivot at the bottom centre)."""
    S = size * SS
    msk = Image.new("L", (S, S), 0)
    im = Image.new("L", (S, S), 0)
    bx0, bx1, by, tx, ty = 0.26 * S, 0.74 * S, 0.97 * S, (0.5 + lean) * S, 0.03 * S
    ImageDraw.Draw(msk).polygon([(bx0, by), (bx1, by), (tx, ty)], fill=255)
    d = ImageDraw.Draw(im)
    d.polygon([(bx0, by), ((bx0 + bx1) / 2, by), (tx, ty)], fill=190)
    d.polygon([((bx0 + bx1) / 2, by), (bx1, by), (tx, ty)], fill=95)
    ln = Image.new("L", (S, S), 0)
    ImageDraw.Draw(ln).line([(bx0, by), (tx, ty), (bx1, by), (bx0, by)], fill=255, width=int(S * 0.035), joint="curve")
    ImageDraw.Draw(ln).line([((bx0 + bx1) / 2, by), (tx, ty)], fill=255, width=int(S * 0.012))
    shade = np.asarray(im, np.float32) * (1 - np.asarray(ln, np.float32) / 255 * 0.9)
    return to_rgba(Image.fromarray(shade.astype(np.uint8)), msk)


def nail_sprite(size, seed):
    """A nail pointing up: round head at the bottom, a long shaft, a sharp point."""
    S = size * SS
    msk = Image.new("L", (S, S), 0)
    im = Image.new("L", (S, S), 0)
    dm, di = ImageDraw.Draw(msk), ImageDraw.Draw(im)
    cx = S * 0.5
    dm.rectangle([cx - S * 0.045, S * 0.12, cx + S * 0.045, S * 0.84], fill=255)
    dm.polygon([(cx - S * 0.045, S * 0.12), (cx + S * 0.045, S * 0.12), (cx, S * 0.01)], fill=255)
    dm.ellipse([cx - S * 0.19, S * 0.82, cx + S * 0.19, S * 0.98], fill=255)
    di.rectangle([cx - S * 0.045, S * 0.12, cx, S * 0.84], fill=235)
    di.rectangle([cx, S * 0.12, cx + S * 0.045, S * 0.84], fill=130)
    di.polygon([(cx - S * 0.045, S * 0.12), (cx, S * 0.12), (cx, S * 0.01)], fill=240)
    di.polygon([(cx, S * 0.12), (cx + S * 0.045, S * 0.12), (cx, S * 0.01)], fill=140)
    di.ellipse([cx - S * 0.19, S * 0.82, cx + S * 0.19, S * 0.98], fill=170)
    di.ellipse([cx - S * 0.13, S * 0.85, cx + S * 0.07, S * 0.93], fill=250)
    ln = Image.new("L", (S, S), 0)
    dl = ImageDraw.Draw(ln)
    dl.line([(cx - S * 0.045, S * 0.84), (cx - S * 0.045, S * 0.12), (cx, S * 0.01), (cx + S * 0.045, S * 0.12), (cx + S * 0.045, S * 0.84)], fill=255, width=int(S * 0.016))
    dl.ellipse([cx - S * 0.19, S * 0.82, cx + S * 0.19, S * 0.98], outline=255, width=int(S * 0.02))
    shade = np.asarray(im, np.float32) * (1 - np.asarray(ln, np.float32) / 255 * 0.9)
    return to_rgba(Image.fromarray(shade.astype(np.uint8)), msk)


def scale_sprite(size, seed):
    """One fish / dragon scale: a rounded shield with fan ridges, a lit rim and an ink line."""
    x, y = grid(size, size)
    u, v = x, y * 0.9 + 0.1
    r = np.sqrt(u * u * 0.9 + np.clip(v, 0, None) ** 2)
    top = (v < 0) & (v > -0.85) & (np.abs(u) < 0.86 * (1 + v / 0.85) ** 0.8)
    mask = ((r < 0.92) & (v >= 0)) | top
    th = np.arctan2(np.clip(v, 0, None), u)
    ridges = 0.5 + 0.5 * np.cos(th * 14)
    shade = 0.45 + 0.35 * ridges * smooth(0.1, 0.9, r) + 0.2 * smooth(0.55, 0.9, r)
    shade = np.where(r > 0.84, 1.0, shade)                                  # lit rim
    shade = np.where(r > 0.9, 0.2, shade)                                   # ink line
    a = mask.astype(np.float32)
    return arr_rgba(np.dstack([shade * 255, shade * 255, shade * 255, a * 255]))


def lock_sprite(size, seed, curl=1.0):
    """A hanging lock of hair: an S-curved ribbon made of several strands, narrowing to a point (pivot at the top)."""
    rng = np.random.default_rng(seed)
    S = size * SS
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    strands = 7
    for s in range(strands):
        off = (s - (strands - 1) / 2) / strands * 0.22
        pts = []
        for t in range(40):
            tt = t / 39.0
            cxx = 0.5 + off * (1 - tt * 0.85) + 0.16 * curl * math.sin(tt * 5.0 + s * 0.15)
            pts.append((cxx * S, (0.04 + tt * 0.92) * S))
        d.line(pts, fill=int(140 + 110 * rng.random()), width=max(2, int(S * (0.02 + 0.012 * rng.random()))), joint="curve")
    g = np.asarray(blur(im, 0.4), np.float32) / 255
    a = np.clip(g * 1.6, 0, 1)
    return to_rgba(Image.fromarray((g * 255).astype(np.uint8)), Image.fromarray((a * 255).astype(np.uint8)))


def flake_sprite(size, seed):
    """A flake of gold leaf: an irregular curled quadrilateral with a crinkle."""
    rng = np.random.default_rng(seed)
    pts = [(0.5 + 0.42 * math.cos(a) * rng.uniform(0.6, 1.0), 0.5 + 0.28 * math.sin(a) * rng.uniform(0.6, 1.0)) for a in np.linspace(0, 2 * math.pi, 7)[:-1]]
    return poly_sprite(size, pts, seed, base=0.7, spread=0.3, outline=0.035, cuts=3)


def ripple_sprite(size, seed):
    """Concentric wave arcs (scale magic's water-like shimmer)."""
    x, y = grid(size, size)
    r = np.sqrt(x * x + y * y)
    a = np.zeros_like(r)
    for k, rr in enumerate((0.35, 0.58, 0.8, 0.95)):
        a = np.maximum(a, np.exp(-((r - rr) / 0.025) ** 2) * (1.0 - 0.18 * k) * (0.6 + 0.4 * np.cos(np.arctan2(y, x) * (3 + k) + k)))
    return arr_rgba(np.dstack([a * 255, a * 255, a * 255, np.clip(a, 0, 1) * 255]))


def plate_sprite(size, seed):
    """A riveted inlaid plate (rounded rectangle, bevel, border inlay, corner rivets, a diamond)."""
    S = size * SS
    msk = Image.new("L", (S, S), 0)
    im = Image.new("L", (S, S), 0)
    ImageDraw.Draw(msk).rounded_rectangle([S * 0.1, S * 0.04, S * 0.9, S * 0.96], radius=S * 0.05, fill=255)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([S * 0.1, S * 0.04, S * 0.9, S * 0.96], radius=S * 0.05, fill=120)
    d.rounded_rectangle([S * 0.14, S * 0.08, S * 0.86, S * 0.92], radius=S * 0.04, outline=250, width=int(S * 0.012))
    d.rounded_rectangle([S * 0.19, S * 0.13, S * 0.81, S * 0.87], radius=S * 0.03, outline=190, width=int(S * 0.008))
    cx, cy = S * 0.5, S * 0.5
    d.polygon([(cx, cy - S * 0.22), (cx + S * 0.15, cy), (cx, cy + S * 0.22), (cx - S * 0.15, cy)], outline=250, width=int(S * 0.012))
    for sx in (0.2, 0.8):
        for sy in (0.14, 0.86):
            d.ellipse([S * sx - S * 0.025, S * sy - S * 0.025, S * sx + S * 0.025, S * sy + S * 0.025], fill=255)
    ln = Image.new("L", (S, S), 0)
    ImageDraw.Draw(ln).rounded_rectangle([S * 0.1, S * 0.04, S * 0.9, S * 0.96], radius=S * 0.05, outline=255, width=int(S * 0.03))
    shade = np.asarray(im, np.float32) * (1 - np.asarray(ln, np.float32) / 255 * 0.9)
    return to_rgba(Image.fromarray(shade.astype(np.uint8)), msk)


def mote_sprite(size):
    x, y = grid(size, size)
    r = np.sqrt(x * x + y * y)
    a = np.exp(-(r / 0.38) ** 2)
    return arr_rgba(np.dstack([a * 255, a * 255, a * 255, a * 255]))

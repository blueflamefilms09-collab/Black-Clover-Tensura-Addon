"""Generates the Copper Magic VFX textures (Red Shine Mace and friends).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/copper_*.png

    python3 -B tools/gen_copper_textures.py

Look reference (owner still, pack_copper): a caster thrusts a long dark polearm; at its tip burns a ragged yellow-orange blaze;
the target is a brown faceted slab of rock with thick black cel-shading lines; vertical speed lines and a shaking blur hang in
the air in front of cold grey stone. So: faceted cel-shaded metal with dark linework, a hot yellow-orange blaze, long thin
speed streaks, rivets and hammered plates. Sprites are grey with alpha (vertex colour tints them) except the blaze core which
keeps a warm tone through the tint.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(arr_or_im, name):
    im = arr_or_im
    if isinstance(im, np.ndarray):
        im = Image.fromarray(np.clip(im, 0, 255).astype(np.uint8), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def rgba(grey, alpha):
    g = np.clip(grey, 0, 1) * 255
    a = np.clip(alpha, 0, 1) * 255
    return np.dstack([g, g, g, a])


def value_noise(w, h, cells, rng, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cw, ch = max(2, cells[0] * 2 ** o), max(2, cells[1] * 2 ** o)
        small = rng.random((ch + 1, cw + 1)).astype(np.float32)
        im = Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, np.float32) / 255 * amp
        tot += amp
        amp *= 0.5
    return out / tot


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def blur(arr, r):
    return np.asarray(Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255


def periodic(th, rng, terms=7, maxk=11):
    """Seamless function of the angle (sum of sines), range about -1..1."""
    out = np.zeros_like(th)
    for _ in range(terms):
        k = int(rng.integers(2, maxk))
        out += np.sin(th * k + rng.uniform(0, 6.28)) * rng.uniform(0.4, 1.0) / math.sqrt(k)
    return out / 1.6


# ---------------------------------------------------------------------------------------------- faceted cel-shaded solids
def faceted(points, size, seed, light=(-0.6, -0.8), outline=0.07, base=0.62, spread=0.34, inner=6):
    """A convex-ish polygon split into triangle facets from an off-centre hub, each facet flat shaded by its facing the light,
    thick dark outline and a few inner crease lines (like the hand-inked rock in the reference). Returns RGBA array."""
    rng = np.random.default_rng(seed)
    S = size * SS
    im = Image.new("L", (S, S), 0)
    msk = Image.new("L", (S, S), 0)
    pts = [(x * S, y * S) for x, y in points]
    ImageDraw.Draw(msk).polygon(pts, fill=255)
    cx = sum(p[0] for p in pts) / len(pts) + rng.uniform(-0.05, 0.05) * S
    cy = sum(p[1] for p in pts) / len(pts) + rng.uniform(-0.05, 0.05) * S
    d = ImageDraw.Draw(im)
    n = len(pts)
    lines = []
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        mx, my = (a[0] + b[0]) / 2 - cx, (a[1] + b[1]) / 2 - cy
        ln = math.hypot(mx, my) + 1e-6
        facing = (mx / ln) * light[0] + (my / ln) * light[1]
        shade = base + spread * facing + rng.uniform(-0.05, 0.05)
        d.polygon([(cx, cy), a, b], fill=int(np.clip(shade, 0, 1) * 255))
        lines.append(((cx, cy), a))
    # secondary creases
    for _ in range(inner):
        a = pts[rng.integers(n)]
        t = rng.uniform(0.25, 0.7)
        e = (cx + (a[0] - cx) * t + rng.uniform(-0.04, 0.04) * S, cy + (a[1] - cy) * t + rng.uniform(-0.04, 0.04) * S)
        lines.append((e, (cx + (a[0] - cx) * 0.1, cy + (a[1] - cy) * 0.1)))
    arr = np.asarray(im, np.float32) / 255
    # rim light on the lit side: brighten pixels near the edge facing the light
    m = np.asarray(msk, np.float32) / 255
    # draw dark linework
    ln = Image.new("L", (S, S), 0)
    ld = ImageDraw.Draw(ln)
    for a, b in lines:
        ld.line([a, b], fill=255, width=max(2, int(S * outline * 0.28)))
    ld.line(pts + [pts[0]], fill=255, width=max(3, int(S * outline)), joint="curve")
    lnm = np.asarray(ln, np.float32) / 255
    shaded = arr * (1 - lnm * 0.92)
    # soft inner highlight band
    tex = value_noise(S, S, (6, 6), rng, 3)
    shaded = shaded * (0.9 + 0.2 * tex)
    out = Image.fromarray((np.dstack([shaded, shaded, shaded, m]) * 255).astype(np.uint8), "RGBA")
    out = out.resize((size, size), Image.LANCZOS)
    return out


# ---------------------------------------------------------------------------------------------- textures
def tex_mace_head():
    # a heavy, blocky mace head: a faceted slab with a socket stub on the left (reads as the brown slab in the still)
    pts = [(0.10, 0.45), (0.22, 0.20), (0.50, 0.08), (0.82, 0.18), (0.94, 0.42), (0.88, 0.72), (0.62, 0.92), (0.30, 0.86), (0.12, 0.68)]
    im = faceted(pts, 256, 11, inner=9)
    # rivets
    d = ImageDraw.Draw(im)
    for x, y in [(0.30, 0.34), (0.70, 0.30), (0.74, 0.66), (0.36, 0.72), (0.52, 0.5)]:
        r = 7
        d.ellipse([x * 256 - r, y * 256 - r, x * 256 + r, y * 256 + r], fill=(235, 235, 235, 255), outline=(10, 10, 10, 255), width=2)
        d.ellipse([x * 256 - 2, y * 256 - 5, x * 256 + 2, y * 256 - 1], fill=(255, 255, 255, 255))
    save(im, "copper_mace_head")


def tex_shard():
    pts = [(0.50, 0.02), (0.78, 0.30), (0.70, 0.62), (0.56, 0.98), (0.34, 0.66), (0.26, 0.30)]
    save(faceted(pts, 128, 5, outline=0.09, inner=3), "copper_shard")
    pts2 = [(0.08, 0.50), (0.40, 0.14), (0.92, 0.36), (0.74, 0.80), (0.28, 0.86)]
    save(faceted(pts2, 128, 23, outline=0.09, inner=3), "copper_chunk")


def tex_blaze():
    """The ragged yellow-orange blaze at the weapon tip: hot core, swirling licks, hard anime edge. Grey scale with alpha."""
    rng = np.random.default_rng(7)
    W = 256
    x, y = grid(W, W)
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    lick = value_noise(W, W, (4, 4), rng, 4)
    pk = periodic(th, rng, 9, 14)
    edge = 0.58 + 0.17 * pk + 0.10 * (lick - 0.5) * 2
    # flame licks: sharpen the peaks so tongues stream out of the blob
    edge = np.where(pk > 0.25, edge + (pk - 0.25) * 0.22, edge)
    swirl = 0.5 + 0.5 * np.sin(th * 3 + r * 7 + lick * 6)
    body = smooth(edge, edge - 0.10, r)
    hard = (body > 0.5).astype(np.float32)
    body = 0.3 * body + 0.7 * hard
    heat = np.clip((1 - r / np.maximum(edge, 0.2)), 0, 1)
    g = 0.62 + 0.38 * np.sqrt(heat)
    g = g * (0.9 + 0.1 * swirl)
    # inner dark swirl line (like the ring drawn inside the blob in the still)
    ring = np.exp(-((r - 0.24 - 0.04 * np.sin(th * 2)) / 0.03) ** 2)
    g = g - 0.34 * ring * (heat > 0.1)
    # inner fold lines
    fold = np.exp(-((np.sin(th * 2 + r * 9) ) / 0.12) ** 2) * (r < 0.5) * (r > 0.3)
    g = g - 0.18 * fold
    halo = np.exp(-(r / 0.85) ** 2) * 0.35
    a = np.maximum(body, halo)
    save(rgba(g, a), "copper_blaze")


def tex_streak():
    """Speed lines: many thin vertical streaks of different length (U across, V along the travel). Tapers to both ends."""
    rng = np.random.default_rng(3)
    w, h = 128, 256
    arr = np.zeros((h, w), np.float32)
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    for _ in range(26):
        cx = rng.uniform(0.04, 0.96) * w
        wd = rng.uniform(0.6, 2.2)
        y0, ln = rng.uniform(0, 0.7), rng.uniform(0.25, 0.9)
        prof = np.clip((y - y0) / ln, 0, 1)
        prof = np.abs(np.sin(prof * math.pi)) ** 0.7 * (y >= y0) * (y <= y0 + ln)
        xs = np.arange(w, dtype=np.float32)
        col = np.exp(-((xs - cx) / wd) ** 2)
        arr = np.maximum(arr, prof[:, None] * col[None, :] * rng.uniform(0.55, 1.0))
    # central bright core lane
    xs = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    core = np.exp(-(xs / 0.22) ** 2)[None, :] * (0.35 + 0.65 * np.sin(y * math.pi))[:, None]
    arr = np.maximum(arr, core * 0.75)
    save(rgba(0.55 + 0.45 * arr, arr), "copper_streak")


def tex_shaft():
    """Polearm shaft ribbon: long dark/bright metal pole with ring collars and a hot centre line (U across, V along)."""
    w, h = 64, 256
    xs = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    ys = (np.arange(h, dtype=np.float32) + 0.5) / h
    pole = smooth(0.30, 0.22, np.abs(xs))[None, :] * np.ones((h, 1), np.float32)
    # cylinder shading: bright on the left-of-centre, dark rim
    shade = 0.55 + 0.45 * np.cos((xs + 0.18) * 2.2)
    g = np.broadcast_to(shade[None, :], (h, w)).copy()
    rim = smooth(0.22, 0.30, np.abs(xs))[None, :] * np.ones((h, 1))
    g = g * (1 - rim * 0.9)
    a = pole.copy()
    # collars
    for cy in (0.18, 0.50, 0.82):
        band = np.exp(-((ys - cy) / 0.018) ** 2)
        wide = smooth(0.56, 0.46, np.abs(xs))[None, :] * band[:, None]
        g = np.maximum(g * (1 - wide), wide * (0.8 + 0.2 * np.cos(xs * 3))[None, :])
        a = np.maximum(a, wide)
        edge = np.exp(-((ys - cy) / 0.006) ** 2)[:, None] * wide
        g = g * (1 - edge * 0.8)
    # fine grain
    rng = np.random.default_rng(31)
    g = g * (0.94 + 0.06 * rng.random((h, w)))
    save(rgba(g, a), "copper_shaft")


def tex_ring():
    """Hammered copper ring band for the ground: dents, two engraved lines and rivets. U wraps around, V across the band."""
    rng = np.random.default_rng(13)
    w, h = 512, 64
    ys = (np.arange(h, dtype=np.float32) + 0.5) / h * 2 - 1
    band = smooth(1.0, 0.82, np.abs(ys))[:, None] * np.ones((1, w), np.float32)
    n = value_noise(w, h, (40, 3), rng, 4)
    dents = np.sin(n * 25) * 0.5 + 0.5
    g = 0.62 + 0.28 * (n - 0.5) * 2 + 0.1 * dents
    g = g + 0.25 * np.exp(-((ys + 0.55) / 0.16) ** 2)[:, None]    # highlight
    g = g - 0.35 * np.exp(-((np.abs(ys) - 0.5) / 0.045) ** 2)[:, None]  # engraved lines
    g = g * (1 - 0.5 * smooth(0.78, 0.98, np.abs(ys)))[:, None] if False else g * (1 - 0.5 * smooth(0.78, 0.98, np.abs(ys)))[:, None]
    img = rgba(g, band)
    im = Image.fromarray((img).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    for i in range(16):
        cx = (i + 0.5) * w / 16
        d.ellipse([cx - 5, h / 2 - 5, cx + 5, h / 2 + 5], fill=(240, 240, 240, 255), outline=(20, 20, 20, 255), width=2)
        d.ellipse([cx - 2, h / 2 - 4, cx + 1, h / 2 - 1], fill=(255, 255, 255, 255))
    save(im, "copper_ring")


def tex_sigil():
    """Ground plate sigil: toothed outer gear, concentric engraved rings, a six point polygon star and rivets. Grey + alpha."""
    S = 256 * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2

    def circ(r, **k):
        d.ellipse([c - r, c - r, c + r, c + r], **k)
    # gear teeth
    teeth = 24
    pts = []
    R0, R1 = 0.96 * c, 0.86 * c
    for i in range(teeth * 2):
        a0 = math.pi * 2 * i / (teeth * 2)
        a1 = math.pi * 2 * (i + 1) / (teeth * 2)
        r = R0 if i % 2 == 0 else R1
        pts += [(c + math.cos(a0) * r, c + math.sin(a0) * r), (c + math.cos(a1) * r, c + math.sin(a1) * r)]
    d.polygon(pts, fill=(205, 205, 205, 235))
    circ(0.84 * c, fill=(120, 120, 120, 150))
    circ(0.80 * c, outline=(255, 255, 255, 255), width=int(S * 0.012))
    circ(0.62 * c, outline=(235, 235, 235, 255), width=int(S * 0.02))
    circ(0.58 * c, outline=(30, 30, 30, 255), width=int(S * 0.006))
    circ(0.30 * c, outline=(255, 255, 255, 255), width=int(S * 0.018))
    # polygon star {6/2} twice, mace-head diamonds
    for rot, rr, col in ((0, 0.60, (255, 255, 255, 255)), (math.pi / 6, 0.52, (190, 190, 190, 255))):
        for k in range(2):
            tri = [(c + math.cos(rot + math.pi * 2 * (i / 3 + k / 6) - math.pi / 2) * rr * c,
                    c + math.sin(rot + math.pi * 2 * (i / 3 + k / 6) - math.pi / 2) * rr * c) for i in range(3)]
            d.polygon(tri + [tri[0]], outline=col, width=int(S * 0.008))
    # radial bars between rings
    for i in range(12):
        a = math.pi * 2 * i / 12 + math.pi / 12
        d.line([(c + math.cos(a) * 0.64 * c, c + math.sin(a) * 0.64 * c), (c + math.cos(a) * 0.78 * c, c + math.sin(a) * 0.78 * c)],
               fill=(255, 255, 255, 255), width=int(S * 0.014))
    # rivets on the ring
    for i in range(12):
        a = math.pi * 2 * i / 12
        x, y = c + math.cos(a) * 0.70 * c, c + math.sin(a) * 0.70 * c
        r = S * 0.016
        d.ellipse([x - r, y - r, x + r, y + r], fill=(250, 250, 250, 255), outline=(25, 25, 25, 255), width=int(S * 0.004))
    # central mace-head diamond
    dm = [(c, c - 0.2 * c), (c + 0.16 * c, c), (c, c + 0.2 * c), (c - 0.16 * c, c)]
    d.polygon(dm, fill=(255, 255, 255, 255), outline=(25, 25, 25, 255))
    d.line([dm[0], dm[2]], fill=(25, 25, 25, 255), width=int(S * 0.004))
    d.line([dm[1], dm[3]], fill=(25, 25, 25, 255), width=int(S * 0.004))
    save(im.resize((256, 256), Image.LANCZOS), "copper_sigil")


def tex_crack():
    """Radial ground fracture from a point, jagged forks, dark in the middle bright on the edges (ALPHA or ADD)."""
    rng = np.random.default_rng(19)
    S = 256 * SS
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2

    def branch(x, y, ang, length, width, depth):
        if length < S * 0.02 or depth > 3:
            return
        px, py = x, y
        steps = max(3, int(length / (S * 0.03)))
        for _ in range(steps):
            ang += rng.uniform(-0.45, 0.45)
            st = length / steps
            nx, ny = px + math.cos(ang) * st, py + math.sin(ang) * st
            d.line([(px, py), (nx, ny)], fill=255, width=max(2, int(width)))
            if rng.random() < 0.28:
                branch(nx, ny, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0), length * 0.45, width * 0.6, depth + 1)
            px, py = nx, ny
            width *= 0.93
    for i in range(9):
        branch(c, c, math.pi * 2 * i / 9 + rng.uniform(-0.2, 0.2), S * rng.uniform(0.32, 0.47), S * 0.017, 0)
    a = np.asarray(im, np.float32) / 255
    wide = np.asarray(im.filter(ImageFilter.GaussianBlur(S * 0.012)), np.float32) / 255
    a = np.clip(a * 1.0 + wide * 0.8, 0, 1)
    a = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).resize((256, 256), Image.LANCZOS), np.float32) / 255
    x, y = grid(256, 256)
    fall = smooth(1.0, 0.7, np.hypot(x, y))
    core = np.exp(-(np.hypot(x, y) / 0.12) ** 2)
    save(rgba(0.7 + 0.3 * a, np.clip(a * fall + core * 0.7, 0, 1)), "copper_crack")


def tex_flare():
    """Four-point lens star with a hard bright core and a diagonal small pair, for impact flashes and glints."""
    W = 256
    x, y = grid(W, W)
    r = np.hypot(x, y)
    h = np.exp(-(np.abs(y) / 0.025)) * np.exp(-np.abs(x) * 2.8)
    v = np.exp(-(np.abs(x) / 0.025)) * np.exp(-np.abs(y) * 2.8)
    u, w = (x + y) / 1.414, (x - y) / 1.414
    d1 = np.exp(-(np.abs(w) / 0.03)) * np.exp(-np.abs(u) * 5)
    d2 = np.exp(-(np.abs(u) / 0.03)) * np.exp(-np.abs(w) * 5)
    core = np.exp(-(r / 0.09) ** 2)
    halo = np.exp(-(r / 0.35) ** 2) * 0.45
    a = np.clip(h + v + 0.45 * (d1 + d2) + core + halo, 0, 1)
    save(rgba(0.7 + 0.3 * a, a), "copper_flare")


def tex_flecks():
    """Hot hammered metal flecks and sparks: small bright slivers of varied sizes, for rising motes and spark showers."""
    rng = np.random.default_rng(41)
    S = 256 * SS
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    for _ in range(46):
        x, y = rng.uniform(0.06, 0.94) * S, rng.uniform(0.06, 0.94) * S
        a = rng.uniform(0, math.pi)
        ln = rng.uniform(0.02, 0.085) * S
        wd = ln * rng.uniform(0.18, 0.4)
        dx, dy, px, py = math.cos(a) * ln, math.sin(a) * ln, -math.sin(a) * wd, math.cos(a) * wd
        d.polygon([(x - dx, y - dy), (x + px, y + py), (x + dx, y + dy), (x - px, y - py)], fill=int(rng.uniform(170, 255)))
    img = im.resize((256, 256), Image.LANCZOS)
    a = np.asarray(img, np.float32) / 255
    glow = blur(a, 2.2) * 0.55
    al = np.clip(a + glow, 0, 1)
    save(rgba(0.75 + 0.25 * a, al), "copper_flecks")


def tex_plate():
    """A big hammered copper plate / shield card with bevel, rivets in the corners and a diagonal sheen (ALPHA, wall panels)."""
    rng = np.random.default_rng(53)
    W = 256
    x, y = grid(W, W)
    n = value_noise(W, W, (14, 14), rng, 4)
    g = 0.58 + 0.3 * (n - 0.5) * 2
    g += 0.22 * np.exp(-(((x + y) * 0.7 + 0.2) / 0.18) ** 2)
    box = np.maximum(np.abs(x), np.abs(y))
    bevel = smooth(0.92, 0.80, box)
    g = g * (0.55 + 0.45 * bevel) + 0.25 * np.exp(-((box - 0.86) / 0.025) ** 2)
    a = smooth(0.99, 0.94, box)
    im = Image.fromarray(rgba(g, a).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    for sx in (-1, 1):
        for sy in (-1, 1):
            cx, cy = W / 2 + sx * 0.76 * W / 2, W / 2 + sy * 0.76 * W / 2
            d.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(240, 240, 240, 255), outline=(15, 15, 15, 255), width=3)
            d.ellipse([cx - 3, cy - 6, cx + 1, cy - 2], fill=(255, 255, 255, 255))
    d.rectangle([4, 4, W - 5, W - 5], outline=(20, 20, 20, 255), width=3)
    save(im, "copper_plate")


def tex_slam_ring():
    """Hard thin shock ring with torn dust-line edge, for the expanding impact rings (U/V both used as a billboard)."""
    rng = np.random.default_rng(61)
    W = 256
    x, y = grid(W, W)
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    rr = 0.80 + 0.06 * periodic(th, rng, 8, 16)
    line = np.exp(-((r - rr) / 0.018) ** 2)
    soft = np.exp(-((r - rr + 0.04) / 0.10) ** 2) * 0.55
    inner = np.exp(-((r - rr * 0.86) / 0.008) ** 2) * 0.6
    ticks = (np.sin(th * 48) > 0.2) * np.exp(-((r - rr - 0.07) / 0.03) ** 2) * 0.8
    a = np.clip(line + soft + inner + ticks, 0, 1) * smooth(1.0, 0.95, r)
    save(rgba(0.7 + 0.3 * line, a), "copper_slam_ring")


if __name__ == "__main__":
    tex_mace_head()
    tex_shard()
    tex_blaze()
    tex_streak()
    tex_shaft()
    tex_ring()
    tex_sigil()
    tex_crack()
    tex_flare()
    tex_flecks()
    tex_plate()
    tex_slam_ring()

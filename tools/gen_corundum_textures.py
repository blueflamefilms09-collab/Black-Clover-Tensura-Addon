"""Generates the Corundum Magic VFX textures (deterministic, no fonts, no external images).

    python3 -B tools/gen_corundum_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/corundum_*.png

Look reference (owner art: the Ideal Closer stills): corundum reads as flat-shaded, angular, faceted pale lavender-grey stone, every
facet a single flat tone with a thin dark outline drawn round it, a mottled grain over the lit faces, hard planar highlights on the
upper-left edges and deep blue-grey shadow in the lower right; the owner spec adds ruby / sapphire gem shaders (red and blue gems).
Texture convention (same as the rest of textures/particle): white / greyscale with alpha so the vertex colour tints them (stone
lavender, ruby red, sapphire blue). The baked outlines are dark luminance, so they stay dark under any tint.

Sprites: facet (faceted boulder), shard (hex prism crystal, tip up), gem (brilliant cut gem, top view), star (six ray asterism flare),
band (tileable faceted stone band), hex (glowing hexagon rings), sigil (hexagram ground sigil), streak (comet streak with facet
flecks), crack (broken ground cracks), burst (starburst of stone spikes), chip (small stone chip).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
LIGHT = np.array([-0.45, -0.6, 0.66], dtype=np.float32)
LIGHT /= np.linalg.norm(LIGHT)
INK = 26  # outline luminance


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grey(lum, alpha):
    l = np.clip(lum, 0, 255)
    a = np.clip(alpha, 0, 1) * 255
    return Image.fromarray(np.dstack([l, l, l, a]).astype(np.uint8), "RGBA")


def noise(w, h, cells, seed):
    rng = np.random.default_rng(seed)
    g = (rng.random((cells, cells)) * 255).astype(np.uint8)
    return np.asarray(Image.fromarray(g).resize((w, h), Image.BICUBIC), dtype=np.float32) / 255.0


def fbm(w, h, seed, base=6, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + noise(w, h, base * (2 ** o), seed + o * 11) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def voronoi(w, h, pts, wrapx=False):
    """Nearest cell id, distance to nearest, distance to second nearest (pixels)."""
    p = np.array(pts, dtype=np.float32)
    if wrapx:
        p = np.concatenate([p, p + [w, 0], p - [w, 0]])
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt((xx[None] - p[:, 0, None, None]) ** 2 + (yy[None] - p[:, 1, None, None]) ** 2)
    order = np.argsort(d, axis=0)
    d1 = np.take_along_axis(d, order[:1], 0)[0]
    d2 = np.take_along_axis(d, order[1:2], 0)[0]
    return order[0] % len(pts), d1, d2


def facet_lum(ids, n, seed, d1, d2, lw, grain_seed, lo=70, hi=235):
    """Flat shaded facets: one tone per cell from a random normal, dark outline at the cell borders, mottled grain."""
    rng = np.random.default_rng(seed)
    nrm = rng.normal(size=(n, 3)).astype(np.float32)
    nrm[:, 2] = np.abs(nrm[:, 2]) * 0.8 + 0.5
    nrm /= np.linalg.norm(nrm, axis=1)[:, None]
    dot = np.clip(nrm @ LIGHT, 0, 1)
    dot = (dot - dot.min()) / max(1e-6, dot.max() - dot.min())
    tone = lo + (hi - lo) * dot
    h, w = ids.shape
    lum = tone[ids]
    grain = fbm(w, h, grain_seed, 8, 4)
    lum = lum * (0.88 + 0.24 * grain)
    edge = np.clip((d2 - d1) / lw, 0, 1)
    edge = edge * edge * (3 - 2 * edge)
    return lum * edge + INK * (1 - edge), edge


def down(im, size):
    return im.resize(size, Image.LANCZOS)


# ------------------------------------------------------------------------------------------------ sprites
def make_facet(S=256, ss=2):
    W = S * ss
    rng = np.random.default_rng(3)
    n = 10
    ang = np.sort(rng.random(n)) * 2 * math.pi
    ang = (np.arange(n) + rng.random(n) * 0.6) / n * 2 * math.pi
    rad = 0.36 + 0.12 * rng.random(n)
    poly = [(W / 2 + math.cos(a) * r * W, W / 2 + math.sin(a) * r * W) for a, r in zip(ang, rad)]
    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).polygon(poly, fill=255)
    mask = np.asarray(m, dtype=np.float32) / 255
    pts = [(W / 2 + (rng.random() - 0.5) * W * 0.8, W / 2 + (rng.random() - 0.5) * W * 0.8) for _ in range(16)]
    ids, d1, d2 = voronoi(W, W, pts)
    lum, edge = facet_lum(ids, 16, 5, d1, d2, 2.4 * ss, 9)
    # silhouette outline: dark band just inside the mask edge
    mi = Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(2 * ss + 1))
    inner = np.asarray(mi, dtype=np.float32) / 255
    lum = lum * inner + INK * (1 - inner)
    save(down(grey(lum, mask), (S, S)), "corundum_facet")


def make_shard(w=128, h=256, ss=4):
    W, H = w * ss, h * ss
    im = Image.new("L", (W, H), 0)
    al = Image.new("L", (W, H), 0)
    d, da = ImageDraw.Draw(im), ImageDraw.Draw(al)
    P = lambda x, y: (x * ss, y * ss)
    apex = (64, 5)
    top = [(26, 78), (50, 88), (78, 88), (102, 78)]
    bot = [(26, 246), (50, 254), (78, 254), (102, 246)]
    faces = [
        ([apex, top[0], top[1]], 150), ([apex, top[1], top[2]], 232), ([apex, top[2], top[3]], 112),
        ([top[0], top[1], bot[1], bot[0]], 128), ([top[1], top[2], bot[2], bot[1]], 214), ([top[2], top[3], bot[3], bot[2]], 92),
    ]
    sil = [apex, top[0], bot[0], bot[1], bot[2], bot[3], top[3]]
    da.polygon([P(*p) for p in sil], fill=255)
    for pts, lum in faces:
        d.polygon([P(*p) for p in pts], fill=lum)
    for pts, _ in faces:
        d.line([P(*p) for p in pts + [pts[0]]], fill=INK, width=3 * ss, joint="curve")
    # hard highlight strip and an internal fracture line
    d.polygon([P(55, 100), P(61, 100), P(60, 236), P(54, 240)], fill=250)
    d.line([P(70, 130), P(64, 162), P(72, 190), P(66, 222)], fill=INK + 20, width=1 * ss)
    a = np.asarray(al, dtype=np.float32) / 255
    l = np.asarray(im, dtype=np.float32)
    vert = np.linspace(1.0, 0.82, H)[:, None]
    l = l * vert * (0.9 + 0.2 * fbm(W, H, 21, 6, 4))
    save(down(grey(l, a), (w, h)), "corundum_shard")


def make_gem(S=128, ss=4):
    W = S * ss
    im = Image.new("L", (W, W), 0)
    al = Image.new("L", (W, W), 0)
    d, da = ImageDraw.Draw(im), ImageDraw.Draw(al)
    c = W / 2
    R, r = 0.47 * W, 0.22 * W
    O = [(c + math.cos(math.radians(45 * i - 90)) * R, c + math.sin(math.radians(45 * i - 90)) * R) for i in range(8)]
    T = [(c + math.cos(math.radians(45 * i - 90 + 22.5)) * r, c + math.sin(math.radians(45 * i - 90 + 22.5)) * r) for i in range(8)]
    da.polygon(O, fill=255)

    def lum_of(p, base):
        a = math.atan2(p[1] - c, p[0] - c)
        return base + 70 * math.cos(a - math.radians(-130))
    for i in range(8):
        o0, o1, tt, tp = O[i], O[(i + 1) % 8], T[i], T[i - 1]
        up = ((o0[0] + o1[0] + tt[0]) / 3, (o0[1] + o1[1] + tt[1]) / 3)
        lo = ((o0[0] + tp[0] + tt[0]) / 3, (o0[1] + tp[1] + tt[1]) / 3)
        d.polygon([o0, o1, tt], fill=int(np.clip(lum_of(up, 140), 40, 250)))
        d.polygon([o0, tp, tt], fill=int(np.clip(lum_of(lo, 110) + (22 if i % 2 else -10), 40, 250)))
    d.polygon(T, fill=205)
    d.polygon([(c - 0.1 * W, c - 0.12 * W), (c + 0.02 * W, c - 0.14 * W), (c - 0.04 * W, c - 0.02 * W)], fill=252)
    for i in range(8):
        d.line([O[i], T[i], O[(i + 1) % 8]], fill=INK + 14, width=ss)
        d.line([O[i], O[(i + 1) % 8]], fill=INK, width=2 * ss)
    d.line(T + [T[0]], fill=INK + 10, width=ss)
    l = np.asarray(im, dtype=np.float32)
    save(down(grey(l, np.asarray(al, dtype=np.float32) / 255), (S, S)), "corundum_gem")


def make_star(S=256):
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    x, y = (xx - S / 2 + 0.5) / (S / 2), (yy - S / 2 + 0.5) / (S / 2)
    r = np.sqrt(x * x + y * y) + 1e-6
    th = np.arctan2(y, x)
    out = np.zeros_like(r)
    for k in range(6):  # six sapphire asterism rays, each both ways along the axis
        a = k * math.pi / 3 + math.pi / 6
        perp = np.abs(-x * math.sin(a) + y * math.cos(a))
        along = np.abs(x * math.cos(a) + y * math.sin(a))
        w = 0.012 + 0.03 * np.exp(-along * 3.2)
        out += np.exp(-(perp / w) ** 2) * np.clip(1 - along, 0, 1) ** 1.7
    for k in range(6):  # short secondary rays between
        a = k * math.pi / 3
        perp = np.abs(-x * math.sin(a) + y * math.cos(a))
        along = np.abs(x * math.cos(a) + y * math.sin(a))
        out += 0.45 * np.exp(-(perp / 0.014) ** 2) * np.clip(1 - along * 2.2, 0, 1) ** 2
    core = np.exp(-(r / 0.09) ** 2) * 1.3 + np.exp(-(r / 0.26) ** 2) * 0.35
    v = np.clip(out + core, 0, 1.4)
    v = np.clip(v, 0, 1) * np.clip(1 - r, 0, 1) ** 0.5
    save(grey(np.full_like(v, 255), v), "corundum_star")


def make_band(w=256, h=64, ss=3):
    W, H = w * ss, h * ss
    rng = np.random.default_rng(14)
    pts = [(rng.random() * W, 6 * ss + rng.random() * (H - 12 * ss)) for _ in range(15)]
    ids, d1, d2 = voronoi(W, H, pts, wrapx=True)
    lum, edge = facet_lum(ids, 15, 8, d1, d2, 2.2 * ss, 31, 80, 230)
    # jagged top / bottom silhouette: tiled polyline of random heights
    k = 16
    hts = rng.random(k + 1) * 9 * ss
    hts[-1] = hts[0]
    hb = rng.random(k + 1) * 9 * ss
    hb[-1] = hb[0]
    xs = np.arange(W) / W * k
    top = np.interp(xs, np.arange(k + 1), hts)
    bot = H - np.interp(xs, np.arange(k + 1), hb)
    yy = np.arange(H)[:, None]
    mask = ((yy >= top[None]) & (yy <= bot[None])).astype(np.float32)
    m = Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(2 * ss + 1))
    inner = np.asarray(m, dtype=np.float32) / 255
    lum = lum * inner + INK * (1 - inner)
    save(down(grey(lum, mask), (w, h)), "corundum_band")


def hexpts(cx, cy, r, rot=0.0):
    return [(cx + math.cos(rot + math.pi / 3 * i) * r, cy + math.sin(rot + math.pi / 3 * i) * r) for i in range(6)]


def glow_lines(S, draw_fn, blur=5, ss=2):
    W = S * ss
    im = Image.new("L", (W, W), 0)
    draw_fn(ImageDraw.Draw(im), W)
    sharp = np.asarray(im, dtype=np.float32) / 255
    soft = np.asarray(im.filter(ImageFilter.GaussianBlur(blur * ss)), dtype=np.float32) / 255
    v = np.clip(sharp * 0.95 + soft * 1.8, 0, 1)
    return np.asarray(Image.fromarray((v * 255).astype(np.uint8)).resize((S, S), Image.LANCZOS), dtype=np.float32) / 255


def make_hex(S=256):
    def fn(d, W):
        c = W / 2
        for r, wd in ((0.46, 7), (0.40, 3), (0.30, 2)):
            pts = hexpts(c, c, r * W)
            d.line(pts + [pts[0]], fill=255, width=wd * 2, joint="curve")
        outer, mid = hexpts(c, c, 0.46 * W), hexpts(c, c, 0.30 * W)
        for p, q in zip(outer, mid):
            d.line([p, q], fill=200, width=3)
        for p in outer:
            d.ellipse([p[0] - 9, p[1] - 9, p[0] + 9, p[1] + 9], fill=255)
    v = glow_lines(S, fn, 4)
    save(grey(np.full_like(v, 255), v), "corundum_hex")


def make_sigil(S=256):
    def fn(d, W):
        c = W / 2
        d.ellipse([c - 0.47 * W, c - 0.47 * W, c + 0.47 * W, c + 0.47 * W], outline=255, width=6)
        d.ellipse([c - 0.43 * W, c - 0.43 * W, c + 0.43 * W, c + 0.43 * W], outline=190, width=2)
        for rot in (-math.pi / 2, -math.pi / 2 + math.pi / 3):
            tri = [(c + math.cos(rot + 2 * math.pi / 3 * i) * 0.42 * W, c + math.sin(rot + 2 * math.pi / 3 * i) * 0.42 * W) for i in range(3)]
            d.line(tri + [tri[0]], fill=255, width=5, joint="curve")
        inner = hexpts(c, c, 0.21 * W)
        d.line(inner + [inner[0]], fill=230, width=4, joint="curve")
        inner2 = hexpts(c, c, 0.12 * W, math.pi / 6)
        d.line(inner2 + [inner2[0]], fill=200, width=3, joint="curve")
        for i, p in enumerate(hexpts(c, c, 0.42 * W, -math.pi / 2)):  # cut gems on the six points
            rr = 0.035 * W
            d.polygon([(p[0], p[1] - rr * 1.4), (p[0] + rr, p[1]), (p[0], p[1] + rr * 1.4), (p[0] - rr, p[1])], fill=255)
        for i in range(12):  # tick marks between the rings
            a = math.pi / 6 * i
            d.line([(c + math.cos(a) * 0.43 * W, c + math.sin(a) * 0.43 * W), (c + math.cos(a) * 0.47 * W, c + math.sin(a) * 0.47 * W)], fill=210, width=3)
        d.ellipse([c - 0.03 * W, c - 0.03 * W, c + 0.03 * W, c + 0.03 * W], fill=255)
    v = glow_lines(S, fn, 3)
    save(grey(np.full_like(v, 255), v), "corundum_sigil")


def make_streak(w=64, h=256):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x = (xx - w / 2 + 0.5) / (w / 2)
    t = yy / (h - 1)
    ends = np.clip(np.minimum(t, 1 - t) * 7, 0, 1)
    core = np.exp(-(x / 0.10) ** 2) + 0.35 * np.exp(-(x / 0.42) ** 2)
    v = core * ends
    rng = np.random.default_rng(23)
    for i in range(9):  # facet flecks: small angular diamonds sitting beside the core
        cy = (i + 0.5 + (rng.random() - 0.5) * 0.5) / 9 * h
        cx = w / 2 + (rng.random() - 0.5) * w * 0.7
        sz = 4 + rng.random() * 5
        dd = np.abs(xx - cx) / sz * 1.2 + np.abs(yy - cy) / (sz * 2.2)
        v = v + np.clip(1 - dd, 0, 1) ** 0.7 * 0.9 * ends
    v = np.clip(v, 0, 1)
    save(grey(np.full_like(v, 255), v), "corundum_streak")


def make_crack(S=256, ss=2):
    W = S * ss
    rng = np.random.default_rng(41)
    pts = [(W / 2 + (rng.random() - 0.5) * W * 0.95, W / 2 + (rng.random() - 0.5) * W * 0.95) for _ in range(34)]
    ids, d1, d2 = voronoi(W, W, pts)
    line = np.clip(1 - (d2 - d1) / (1.6 * ss), 0, 1)
    gap = fbm(W, W, 51, 5, 3)
    line = line * (gap > 0.46)
    yy, xx = np.mgrid[0:W, 0:W].astype(np.float32)
    r = np.sqrt((xx - W / 2) ** 2 + (yy - W / 2) ** 2) / (W / 2)
    img = Image.fromarray((line * 255).astype(np.uint8))
    d = ImageDraw.Draw(img)
    for k in range(9):  # long radial fractures from the centre
        a = rng.random() * 2 * math.pi
        L = 0.55 + 0.4 * rng.random()
        pts2 = [(W / 2, W / 2)]
        cur = a
        for s in range(1, 8):
            cur += (rng.random() - 0.5) * 0.5
            rr = L * s / 7 * W / 2
            pts2.append((W / 2 + math.cos(cur) * rr, W / 2 + math.sin(cur) * rr))
        d.line(pts2, fill=255, width=int(2.4 * ss * (1.2 - 0.4 * rng.random())), joint="curve")
    v = np.asarray(img, dtype=np.float32) / 255 * np.clip(1.05 - r, 0, 1) ** 0.8 * np.clip(r * 14, 0, 1)
    save(down(grey(np.full_like(v, 255), v), (S, S)), "corundum_crack")


def make_burst(S=256, ss=3):
    W = S * ss
    rng = np.random.default_rng(57)
    im = Image.new("L", (W, W), 0)
    al = Image.new("L", (W, W), 0)
    d, da = ImageDraw.Draw(im), ImageDraw.Draw(al)
    c = W / 2
    n = 13
    for i in range(n):
        a = (i + (rng.random() - 0.5) * 0.5) / n * 2 * math.pi
        L = (0.2 + 0.28 * rng.random()) * W
        if i % 3 == 0:
            L = 0.47 * W
        wd = (0.035 + 0.03 * rng.random()) * W
        tip = (c + math.cos(a) * L, c + math.sin(a) * L)
        l = (c + math.cos(a - 1.5708) * wd, c + math.sin(a - 1.5708) * wd)
        r = (c + math.cos(a + 1.5708) * wd, c + math.sin(a + 1.5708) * wd)
        mid = (c + math.cos(a) * L * 0.14, c + math.sin(a) * L * 0.14)
        da.polygon([l, tip, r], fill=255)
        lit = 225 if math.cos(a - math.radians(-130)) > 0 else 120
        d.polygon([l, tip, mid], fill=lit)
        d.polygon([r, tip, mid], fill=max(70, lit - 85))
        d.line([l, tip, r, l], fill=INK, width=int(2.2 * ss))
        d.line([mid, tip], fill=INK + 20, width=ss)
    hexp = hexpts(c, c, 0.075 * W, 0.3)
    da.polygon(hexp, fill=255)
    d.polygon(hexp, fill=235)
    d.line(hexp + [hexp[0]], fill=INK, width=2 * ss)
    l = np.asarray(im, dtype=np.float32) * (0.92 + 0.16 * fbm(W, W, 61, 8, 3))
    save(down(grey(l, np.asarray(al, dtype=np.float32) / 255), (S, S)), "corundum_burst")


def make_chip(S=64, ss=6):
    W = S * ss
    im = Image.new("L", (W, W), 0)
    al = Image.new("L", (W, W), 0)
    d, da = ImageDraw.Draw(im), ImageDraw.Draw(al)
    p = lambda x, y: (x * W / 64, y * W / 64)
    poly = [p(8, 30), p(26, 6), p(52, 14), p(58, 42), p(34, 58), p(14, 50)]
    da.polygon(poly, fill=255)
    cen = p(30, 32)
    shades = [225, 170, 235, 110, 85, 150]
    for i in range(6):
        d.polygon([poly[i], poly[(i + 1) % 6], cen], fill=shades[i])
    for i in range(6):
        d.line([poly[i], cen], fill=INK + 25, width=ss)
        d.line([poly[i], poly[(i + 1) % 6]], fill=INK, width=2 * ss)
    save(down(grey(np.asarray(im, dtype=np.float32), np.asarray(al, dtype=np.float32) / 255), (S, S)), "corundum_chip")


if __name__ == "__main__":
    make_facet()
    make_shard()
    make_gem()
    make_star()
    make_band()
    make_hex()
    make_sigil()
    make_streak()
    make_crack()
    make_burst()
    make_chip()

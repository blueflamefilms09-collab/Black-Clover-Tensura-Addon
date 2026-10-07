"""Generates the Butoh Magic VFX textures (butoh_*.png in textures/particle).

Look reference (owner art, pack_butoh): a hooded dancer in a pale feather-scale cloak with navy scalloped hems; two big teal-jade
blades with a chain-link ornament down the middle and a flat grey-blue tip; long ribbons of iridescent oil-slick water (teal, violet,
green, rose sheen) cutting across a near-black cave; small leaf-shaped ink flecks flying off the cuts.

White / grey sprites are tinted by the vertex colour; butoh_sheen and butoh_fleck carry their colour on purpose.
Deterministic, no fonts.   python3 -B tools/gen_butoh_textures.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def rng(seed):
    return np.random.default_rng(seed)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def noise(h, w, cells_y, cells_x, seed, wrap_x=False):
    """Value noise, bicubic-ish via smooth upsample."""
    g = rng(seed).random((cells_y + 2, cells_x + 2))
    if wrap_x:
        g[:, -1] = g[:, 0]
        g[:, -2] = g[:, 1] if cells_x > 2 else g[:, -2]
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(im, dtype=np.float32) / 255.0


def fbm(h, w, base, seed, octaves=4, wrap_x=False):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = base * (2 ** o)
        out += amp * noise(h, w, c, c, seed + o * 17, wrap_x)
        tot += amp
        amp *= 0.5
    return out / tot


def save(name, rgba):
    arr = np.clip(rgba, 0, 1)
    im = Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, "butoh_%s.png" % name))
    print("butoh_%s.png %dx%d" % (name, im.width, im.height))


def grey(a, level=None):
    """alpha mask (h,w) -> RGBA white/grey with alpha. level = optional (h,w) brightness."""
    h, w = a.shape
    out = np.zeros((h, w, 4), np.float32)
    lv = np.ones_like(a) if level is None else level
    out[..., 0] = out[..., 1] = out[..., 2] = lv
    out[..., 3] = a
    return out


def pil_mask(w, h, fn, blur=0.0):
    """Supersampled draw: fn(draw, scale) paints white on black; returns float (h,w)."""
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im, dtype=np.float32) / 255.0


# ----------------------------------------------------------------------------------------------------- blade 64x256
def blade():
    w, h = 64, 256

    def d(dr, s):
        # flat grey-blue blade: straight edges, a long angled tip at the top, squared tail
        dr.polygon([(w * 0.5 * s, 2 * s), (w * 0.9 * s, 70 * s), (w * 0.9 * s, (h - 6) * s), (w * 0.1 * s, (h - 6) * s), (w * 0.1 * s, 70 * s)], fill=255)
    m = pil_mask(w, h, d)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    cx = (xs - w / 2) / (w * 0.4)
    # bevel: bright ridge in the middle, darker toward the edges, a thin hard rim light
    edge = np.abs(cx)
    lv = 0.55 + 0.35 * np.clip(1 - edge, 0, 1) ** 1.5
    # chain-link ornament: stacked ovals with a hollow centre, down the blade
    orn = np.zeros((h, w), np.float32)
    y0, step = 84.0, 24.0
    for k in range(int((h - 100) / step)):
        cy = y0 + k * step
        dy = (ys - cy) / 13.0
        dx = (xs - w / 2) / 9.0
        r = np.sqrt(dx * dx + dy * dy)
        ring = smooth(0.62, 0.78, r) * (1 - smooth(0.95, 1.12, r))
        orn = np.maximum(orn, ring)
    lv = lv - 0.45 * orn + 0.25 * np.maximum(0, np.roll(orn, -2, 1) - orn)
    # fuller line along the blade and tip edge highlight
    lv += 0.18 * np.exp(-((cx) * 5) ** 2) * (ys > 70)
    tipedge = np.exp(-((np.abs(cx) - np.clip((ys - 2) / 68.0, 0, 1)) * 14) ** 2) * (ys < 72)
    lv += 0.35 * tipedge
    lv += (fbm(h, w, 6, 11, 3) - 0.5) * 0.12
    save("blade", grey(m, np.clip(lv, 0, 1)))


# ----------------------------------------------------------------------------------------------------- sheen 128x256
def sheen():
    """Iridescent oil-slick ribbon: U across, V along. Rainbow bands drift with a noise warp, soft transparent edges."""
    w, h = 128, 256
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xs / (w - 1)
    v = ys / (h - 1)
    warp = fbm(h, w, 4, 3, 3)
    wob = 0.06 * np.sin(v * 9 + warp * 5)
    cu = (u - 0.5 + wob)
    prof = np.exp(-(cu / 0.30) ** 4)                       # ribbon body
    core = np.exp(-(cu / 0.12) ** 2)
    # thin-film phase: depends on thickness (profile) and a warped coordinate along the ribbon
    phase = 1.6 * prof + 0.9 * warp + 0.55 * v * 3 + 1.2 * np.abs(cu)
    r = 0.5 + 0.5 * np.sin(phase * 6.28 + 0.0)
    g = 0.5 + 0.5 * np.sin(phase * 6.28 + 2.1)
    b = 0.5 + 0.5 * np.sin(phase * 6.28 + 4.2)
    # bias toward the reference palette: teal / jade, violet, rose
    teal = np.stack([0.30, 0.85, 0.80])
    viol = np.stack([0.66, 0.45, 0.95])
    rgb = np.stack([r, g, b], -1)
    mixk = (0.5 + 0.5 * np.sin(phase * 6.28 * 0.5))[..., None]
    pal = teal * (1 - mixk) + viol * mixk
    rgb = 0.5 * rgb + 0.5 * pal
    rgb = rgb * (0.55 + 0.6 * prof[..., None]) + 0.4 * core[..., None]
    streak = fbm(h, w, 3, 5, 2)
    a = prof * (0.55 + 0.45 * smooth(0.2, 0.7, streak)) * smooth(0.0, 0.06, v) * (1 - smooth(0.94, 1.0, v)) * 0.95
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = rgb
    out[..., 3] = a
    save("sheen", out)


# ----------------------------------------------------------------------------------------------------- ripple 256 (seigaiha disc)
def ripple():
    n = 256
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    cx = cy = (n - 1) / 2
    dx, dy = xs - cx, ys - cy
    r = np.sqrt(dx * dx + dy * dy) / (n / 2)
    ang = np.arctan2(dy, dx)
    # outer double ring
    ring = np.exp(-((r - 0.96) / 0.018) ** 2) + 0.55 * np.exp(-((r - 0.89) / 0.012) ** 2)
    # seigaiha: concentric scallops tiled in rings of arcs between r .25 and .84
    sc = np.zeros((n, n), np.float32)
    for (rr, cnt, off) in ((0.80, 20, 0.0), (0.66, 16, 0.5), (0.52, 12, 0.0), (0.38, 8, 0.5)):
        seg = 2 * math.pi / cnt
        a = (ang / seg + off) % 1.0 - 0.5
        # distance to the local scallop centre on the ring
        px = rr * np.cos((np.floor(ang / seg + off) + 0.5 - off) * seg)
        py = rr * np.sin((np.floor(ang / seg + off) + 0.5 - off) * seg)
        qx, qy = dx / (n / 2) - px, dy / (n / 2) - py
        q = np.sqrt(qx * qx + qy * qy)
        rad = 0.20
        for k, wt in ((1.0, 1.0), (0.72, 0.8), (0.44, 0.6)):
            sc = np.maximum(sc, wt * np.exp(-((q - rad * k) / 0.011) ** 2) * (r < rr + 0.02) * (r > 0.1))
    core = np.exp(-(r / 0.07) ** 2) + 0.5 * np.exp(-((r - 0.13) / 0.01) ** 2)
    m = np.clip(ring + sc * 0.85 + core, 0, 1)
    # faint fill so the ground reads as water under the lines
    fill = smooth(0.95, 0.2, r) * 0.10 * (0.6 + 0.8 * fbm(n, n, 4, 5, 3))
    a = np.clip(m + fill, 0, 1) * (r < 1.0)
    lv = 0.78 + 0.22 * m
    save("ripple", grey(a, lv))


# ----------------------------------------------------------------------------------------------------- scales 256x64 (ring band)
def scales():
    """Feather-scale cloak hem: rows of scallops, dark scalloped outline; U wraps seamlessly 8 times."""
    w, h = 256, 64
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    out_a = np.zeros((h, w), np.float32)
    lv = np.full((h, w), 0.8, np.float32)
    cols = 8
    cw = w / cols
    for row, (cyc, off) in enumerate(((10, 0.0), (28, 0.5), (46, 0.0))):
        u = ((xs / cw + off) % 1.0 - 0.5) * cw
        dy = ys - cyc
        r = np.sqrt(u * u + dy * dy)
        inside = (r < cw * 0.5) & (dy > -cw * 0.1)
        line = np.exp(-((r - cw * 0.46) / 1.1) ** 2) * (dy > -3)
        line2 = np.exp(-((r - cw * 0.30) / 0.9) ** 2) * (dy > -2)
        out_a = np.maximum(out_a, np.where(inside, 0.35 + 0.15 * (1 - r / (cw * 0.5)), 0))
        out_a = np.maximum(out_a, line * 0.95)
        out_a = np.maximum(out_a, line2 * 0.6)
        lv = np.where(inside, 0.85 - 0.2 * line, lv)
    edge = smooth(0, 0.18, ys / h) * (1 - smooth(0.84, 1.0, ys / h))
    out_a *= edge
    save("scales", grey(np.clip(out_a, 0, 1), lv))


# ----------------------------------------------------------------------------------------------------- links 256x64
def links():
    w, h = 256, 64
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    a = np.zeros((h, w), np.float32)
    lv = np.zeros((h, w), np.float32)
    n = 8
    pitch = w / n
    for k in range(n):
        cx = (k + 0.5) * pitch
        flat = (k % 2 == 0)
        # alternating links: one seen full-on (oval ring), the next edge-on (thin bar), like a chain
        dx = (xs - cx)
        dy = (ys - h / 2)
        if flat:
            r = np.sqrt((dx / (pitch * 0.62)) ** 2 + (dy / (h * 0.40)) ** 2)
            ring = smooth(0.62, 0.74, r) * (1 - smooth(0.96, 1.05, r))
            a = np.maximum(a, ring)
            lv = np.maximum(lv, ring * (0.65 + 0.35 * np.clip(-dy / 20 + 0.5, 0, 1)))
        else:
            bar = smooth(h * 0.12, h * 0.07, np.abs(dy)) * smooth(pitch * 0.78, pitch * 0.62, np.abs(dx))
            a = np.maximum(a, bar)
            lv = np.maximum(lv, bar * (0.7 + 0.3 * np.clip(-dy / 6 + 0.5, 0, 1)))
    save("links", grey(np.clip(a, 0, 1), np.clip(lv, 0.4, 1)))


# ----------------------------------------------------------------------------------------------------- fleck 64 (ink / jade leaf)
def fleck():
    n = 64

    def d(dr, s):
        pts = []
        for i in range(0, 41):
            t = i / 40.0
            x = 32 + 0 * t
            pts.append(((32 + 11 * math.sin(math.pi * t)) * s, (6 + 52 * t) * s))
        for i in range(40, -1, -1):
            t = i / 40.0
            pts.append(((32 - 11 * math.sin(math.pi * t) * (0.8)) * s, (6 + 52 * t) * s))
        dr.polygon(pts, fill=255)
    m = pil_mask(n, n, d, 0.5)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    lv = 0.6 + 0.4 * np.clip(1 - np.abs(xs - 32) / 12, 0, 1) * (1 - ys / n)
    out = np.zeros((n, n, 4), np.float32)
    out[..., 0] = lv * 0.55
    out[..., 1] = lv * 1.0
    out[..., 2] = lv * 0.92
    out[..., 3] = m
    save("fleck", out)


# ----------------------------------------------------------------------------------------------------- splash 256 (radial burst of crescent drops)
def splash():
    n = 256
    r_ = rng(77)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    cx = cy = (n - 1) / 2
    dx, dy = xs - cx, ys - cy
    r = np.sqrt(dx * dx + dy * dy) / (n / 2)
    ang = np.arctan2(dy, dx)
    a = np.zeros((n, n), np.float32)
    for i in range(22):
        ai = 2 * math.pi * i / 22 + (r_.random() - 0.5) * 0.12
        r0 = 0.12 + 0.18 * r_.random()
        r1 = 0.55 + 0.42 * r_.random()
        da = (ang - ai + math.pi) % (2 * math.pi) - math.pi
        # tapered streak with a drop head at the end, slightly curved
        t = np.clip((r - r0) / (r1 - r0), 0, 1)
        curve = 0.10 * t * t
        wd = 0.012 + 0.03 * t * (1 - t) * 4 * 0.35
        d_ = np.abs(da - curve) * r
        streak = np.exp(-(d_ / np.maximum(wd, 0.004)) ** 2) * ((r > r0) & (r < r1)) * (0.4 + 0.6 * t)
        head = np.exp(-(((r - r1) / 0.028) ** 2 + (d_ / 0.022) ** 2))
        a = np.maximum(a, np.maximum(streak, head * 1.1))
    a += 0.8 * np.exp(-(r / 0.06) ** 2)
    a *= (0.8 + 0.4 * fbm(n, n, 8, 3, 2))
    save("splash", grey(np.clip(a, 0, 1), 0.7 + 0.3 * np.clip(a, 0, 1)))


# ----------------------------------------------------------------------------------------------------- slash 256x64 (crescent streak)
def slash():
    w, h = 256, 64
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xs / (w - 1)
    v = (ys - h / 2) / (h / 2)
    # thickest toward the lead (u -> 1), hard bright edge on one side, soft streaks trailing
    body = np.exp(-(v / (0.10 + 0.5 * u)) ** 2)
    hard = np.exp(-((v + 0.12 * u * 4 - (0.34 * u)) / 0.06) ** 2) * u
    lines = (0.5 + 0.5 * np.sin(v * 30 + fbm(h, w, 4, 9, 2) * 6)) * 0.25 * (1 - u) * body
    a = np.clip(body * (0.35 + 0.65 * u ** 1.4) + hard * 0.9 + lines, 0, 1) * smooth(0, 0.05, u) * (1 - smooth(0.97, 1, u))
    save("slash", grey(a, 0.75 + 0.25 * np.clip(hard + body * u, 0, 1)))


# ----------------------------------------------------------------------------------------------------- flash 128 (feather star)
def flash():
    n = 128
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    dx, dy = (xs - 63.5) / 64, (ys - 63.5) / 64
    r = np.sqrt(dx * dx + dy * dy) + 1e-4
    ang = np.arctan2(dy, dx)
    rays = np.zeros_like(r)
    for k, (cnt, ln, wd) in enumerate(((4, 1.0, 0.020), (4, 0.62, 0.030), (8, 0.42, 0.02))):
        base = math.pi / 4 * (k == 1)
        da = (ang - base) % (2 * math.pi / cnt)
        da = np.minimum(da, 2 * math.pi / cnt - da)
        # feather-shaped: wide near the middle, needle-pointed far
        w_ = wd * (1 - r / ln).clip(0, 1) ** 0.7 + 0.002
        rays = np.maximum(rays, np.exp(-((da * r) / w_) ** 2) * (1 - smooth(0.0, ln, r)) ** 0.8)
    halo = np.exp(-(r / 0.22) ** 2)
    a = np.clip(rays + halo, 0, 1)
    save("flash", grey(a, 0.75 + 0.25 * halo))


if __name__ == "__main__":
    blade()
    sheen()
    ripple()
    scales()
    links()
    fleck()
    splash()
    slash()
    flash()

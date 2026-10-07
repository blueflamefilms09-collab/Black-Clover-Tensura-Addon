"""Textures for the Gel Magic VFX (vfx/client/layer/GelLayer). Deterministic and procedural: no fonts, no input images.

    python3 -B tools/gen_gel_textures.py [name ...]        # no names = all

Writes src/main/resources/assets/nusmp/textures/particle/gel_*.png.

Look (Black Clover, Gel Magic; the Sticky Salamander spell): a semi-translucent, thick, jiggling jelly that refracts light. Mint and
teal glass-like blobs with a darker, denser rim, a glossy window highlight, a bright caustic net floating inside, little trapped
bubbles, drips and sticky strands that stretch and bead.

How the textures are read: every body sprite is a grey-scale jelly (ALPHA = silhouette and density, denser and more opaque at the rim,
light and clear in the middle, grey = shading) that the layer draws ALPHA in the body tint, and every body sprite has a twin
`<name>_hi` (white, ALPHA = light only: rim light, window highlight, caustic net, bubble glints) that the layer draws ADDITIVE on top.
Small sprites (drops, bubbles, strand, wall, ring, glint) have the light baked in.

  gel_blob           256 a jelly sphere (body)                    gel_blob_hi         its light
  gel_puddle         256 a lobed puddle seen from above (body)     gel_puddle_hi       its ridge light and caustic net
  gel_splat          256 an impact splat: arms with bulb ends      gel_splat_hi        its light
  gel_salamander     256 x 128 a gel salamander seen from above, head to the right        gel_salamander_hi
  gel_ring           256 a wobbling jelly shock ring with a bright ridge and outer beads (rings, ripples)
  gel_strand         64 x 256 a sticky strand (base bulb at the bottom, beads, tip bulb at the top)
  gel_drops          256 x 128 atlas of four 64 x 128 cells: teardrop, round bead, falling drop, double drop
  gel_bubbles        256 atlas (2 x 2 cells of 128): big bubble, pair, cluster of five, foam of nine
  gel_wall           512 x 128 tileable jelly curtain: a thick scalloped meniscus at the foot, slow vertical streaks, bubbles
  gel_glint          128 a four-pointed jelly sparkle with a soft core
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4  # supersampling for clean edges


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


# ------------------------------------------------------------------------------------------------ numeric helpers
def rgba(grey, alpha):
    g, a = np.clip(grey, 0, 1), np.clip(alpha, 0, 1)
    return Image.fromarray(np.dstack([g * 255, g * 255, g * 255, a * 255]).astype(np.uint8), "RGBA")


def white(alpha):
    return rgba(np.ones_like(alpha), alpha)


def vnoise2(w, h, cx, cy, seed):
    """Tileable value noise 0..1 with its own cell size per axis (smoothstep interpolation)."""
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cx), max(1, h // cy)
    g = rng.random((gh, gw)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    x1, y1 = (x0 + 1) % gw, (y0 + 1) % gh
    x0, y0 = x0 % gw, y0 % gh
    a = g[y0, x0] * (1 - tx) + g[y0, x1] * tx
    b = g[y1, x0] * (1 - tx) + g[y1, x1] * tx
    return a * (1 - ty) + b * ty


def fbm2(w, h, cx, cy, seed, octaves=4):
    n, amp, tot = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octaves):
        n += vnoise2(w, h, max(1, cx >> o), max(1, cy >> o), seed + 17 * o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def sstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def blur(a, sigma, wrap_x=False, wrap_y=False):
    """Gaussian blur through the FFT (wraps where asked, zero-padded elsewhere)."""
    if sigma <= 0.01:
        return a
    p = int(sigma * 3.2) + 2
    py, px = 0 if wrap_y else p, 0 if wrap_x else p
    b = np.pad(a, ((py, py), (px, px)))
    h, w = b.shape
    fy, fx = np.fft.fftfreq(h)[:, None], np.fft.rfftfreq(w)[None, :]
    g = np.exp(-2.0 * (np.pi * sigma) ** 2 * (fx * fx + fy * fy))
    r = np.fft.irfft2(np.fft.rfft2(b) * g, s=(h, w))
    return r[py:h - py, px:w - px].astype(np.float32)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx + 0.5, yy + 0.5


class Mask:
    """A supersampled single-channel canvas; value 255 = full."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("L", (w * SS, h * SS), 0)
        self.d = ImageDraw.Draw(self.im)

    def poly(self, pts, v=255):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=int(v))

    def disc(self, c, r, v=255):
        self.d.ellipse([(c[0] - r) * SS, (c[1] - r) * SS, (c[0] + r) * SS, (c[1] + r) * SS], fill=int(v))

    def ellipse(self, c, rx, ry, v=255):
        self.d.ellipse([(c[0] - rx) * SS, (c[1] - ry) * SS, (c[0] + rx) * SS, (c[1] + ry) * SS], fill=int(v))

    def taper(self, p0, p1, w0, w1, v=255):
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dy) or 1e-6
        nx, ny = -dy / L, dx / L
        self.poly([(p0[0] + nx * w0 / 2, p0[1] + ny * w0 / 2), (p1[0] + nx * w1 / 2, p1[1] + ny * w1 / 2),
                   (p1[0] - nx * w1 / 2, p1[1] - ny * w1 / 2), (p0[0] - nx * w0 / 2, p0[1] - ny * w0 / 2)], v)
        self.disc(p0, w0 / 2, v)
        self.disc(p1, w1 / 2, v)

    def arr(self):
        return np.asarray(self.im.resize((self.w, self.h), Image.BOX), np.float32) / 255.0


# ------------------------------------------------------------------------------------------------ the jelly look
def soft_ellipse(w, h, cx, cy, rx, ry, rot=0.0, power=2.0):
    x, y = grid(w, h)
    c, s = math.cos(rot), math.sin(rot)
    dx, dy = x - cx, y - cy
    u, v = (dx * c + dy * s) / rx, (-dx * s + dy * c) / ry
    return np.clip(1 - (u * u + v * v), 0, 1) ** power


def rim_light(m, sigma, lx, ly):
    """Edge light: bright where the silhouette's outward normal faces the light (lx, ly; image coordinates, y down)."""
    b = blur(m, sigma)
    gy, gx = np.gradient(b)
    gm = np.hypot(gx, gy)
    nl = -gx * lx - gy * ly
    return np.clip(nl / (gm.max() + 1e-6), 0, 1)


def caustic_net(w, h, n, seed, width=0.07):
    """A thin bright net of Voronoi edges: the light a jelly focuses inside itself."""
    rng = np.random.default_rng(seed)
    pts = rng.random((n, 2)) * np.array([w, h])
    x, y = grid(w, h)
    d = np.stack([np.hypot(x - px, y - py) for px, py in pts])
    d.sort(axis=0)
    e = (d[1] - d[0]) / math.sqrt(w * h / n)
    return np.exp(-(e / width) ** 2).astype(np.float32)


def bubble_glints(w, h, items):
    """Little trapped bubbles: a faint ring with a bright dot. items = [(cx, cy, r)] in pixels."""
    x, y = grid(w, h)
    out = np.zeros((h, w), np.float32)
    for cx, cy, r in items:
        d = np.hypot(x - cx, y - cy)
        ring = np.exp(-((d - r) / 0.9) ** 2)
        lit = 0.4 + 0.6 * np.clip(((x - cx) * -0.6 + (y - cy) * -0.8) / (r + 1e-6), 0, 1)
        dot = np.exp(-(np.hypot(x - (cx - r * 0.38), y - (cy - r * 0.4)) / max(0.8, r * 0.22)) ** 2)
        out += 0.55 * ring * lit + 0.9 * dot
    return np.clip(out, 0, 1)


def gel(m, seed, sig, bubbles=(), spec=(), net=0.32, net_n=14, rim=1.0, edge=0.52):
    """Shade a float silhouette mask like jelly. Returns (body_grey, body_alpha, hi_alpha) arrays.
    sig = the blur radius (px) that stands for the thickness of the jelly; spec = [(cx, cy, rx, ry, rot, strength)] window highlights."""
    h, w = m.shape
    thick = sstep(0.0, 0.98, blur(m, sig) / max(1e-6, blur(m, sig).max()))
    n1 = fbm2(w, h, max(4, w // 6), max(4, h // 6), seed, 4)
    bounce = rim_light(m, 5.0, 0.55, 0.83)                       # light coming through the jelly: the lower rim glows
    grey = 0.40 + 0.34 * thick ** 0.9 + 0.20 * blur(bounce, 3.0) + 0.12 * (n1 - 0.5)
    alpha = m * (0.34 + edge * (1 - thick) ** 1.25 + 0.10 * (n1 - 0.5))
    # a darker, denser skin right at the edge
    skin = np.clip(m - blur(m, 2.2), 0, 1)
    grey = grey - 0.30 * skin
    alpha = alpha + 0.30 * skin * m

    lit = np.clip(rim_light(m, 1.8, -0.6, -0.8) * 1.0 + 0.55 * rim_light(m, 6.0, -0.6, -0.8), 0, 1) * rim
    lit2 = 0.45 * rim_light(m, 2.2, 0.55, 0.83)
    cn = caustic_net(w, h, net_n, seed + 5) * net * thick
    cn2 = caustic_net(w, h, int(net_n * 0.6), seed + 9, 0.10) * net * 0.5 * thick
    hi = lit + lit2 + cn + cn2
    for cx, cy, rx, ry, rot, k in spec:
        hi = hi + k * soft_ellipse(w, h, cx, cy, rx, ry, rot, 1.6)
    hi = hi + bubble_glints(w, h, bubbles) * 0.8
    hi = np.clip(hi, 0, 1) * np.clip(blur(m, 1.2) * 1.4, 0, 1)
    # the trapped bubbles also darken the body a little around their rim
    return np.clip(grey, 0, 1), np.clip(alpha, 0, 0.97), hi


def save_pair(name, m, seed, sig, **kw):
    g, a, hi = gel(m, seed, sig, **kw)
    save(rgba(g, a), name)
    save(white(hi), name + "_hi")


def baked(m, seed, sig, **kw):
    """A single sprite with the light added into the grey (small sprites)."""
    g, a, hi = gel(m, seed, sig, **kw)
    return rgba(np.clip(g + hi * 0.9, 0, 1), np.clip(a + hi * 0.5 * (m > 0.02), 0, 1))


# ------------------------------------------------------------------------------------------------ the sprites
def tex_blob():
    S = 256
    m = Mask(S, S)
    rng = np.random.default_rng(11)
    ph = rng.random(6) * math.tau
    pts = []
    for i in range(240):
        a = math.tau * i / 240
        r = 0.425 * S * (1 + 0.030 * math.sin(2 * a + ph[0]) + 0.022 * math.sin(3 * a + ph[1]) + 0.012 * math.sin(5 * a + ph[2]))
        # slightly flattened foot and a hint of a shoulder at the top: reads as a heavy blob, not a ball
        r *= 1 + 0.035 * max(0.0, math.sin(a)) - 0.0 * math.cos(a)
        pts.append((S / 2 + r * math.cos(a), S / 2 + 0.03 * S + r * 0.93 * math.sin(a)))
    m.poly(pts)
    spec = [(S * 0.33, S * 0.29, S * 0.16, S * 0.07, -0.62, 1.0), (S * 0.24, S * 0.43, S * 0.035, S * 0.035, 0, 0.9),
            (S * 0.70, S * 0.72, S * 0.10, S * 0.025, -0.78, 0.45)]
    bub = [(S * 0.62, S * 0.40, 7), (S * 0.69, S * 0.47, 4), (S * 0.45, S * 0.64, 9), (S * 0.56, S * 0.70, 5), (S * 0.33, S * 0.58, 4)]
    save_pair("gel_blob", m.arr(), 21, 30, bubbles=bub, spec=spec, net=0.30, net_n=24)


def tex_puddle():
    S = 256
    rng = np.random.default_rng(31)
    m = Mask(S, S)
    ph = rng.random(8) * math.tau
    pts = []
    for i in range(300):
        a = math.tau * i / 300
        f = 0.78 + 0.075 * math.sin(2 * a + ph[0]) + 0.06 * math.sin(3 * a + ph[1]) + 0.04 * math.sin(4 * a + ph[2]) \
            + 0.03 * math.sin(6 * a + ph[3]) + 0.018 * math.sin(9 * a + ph[4])
        pts.append((S / 2 + S * 0.40 * f * math.cos(a), S / 2 + S * 0.40 * f * math.sin(a)))
    m.poly(pts)
    for k in range(9):                                                   # satellite droplets thrown round the edge
        a = rng.random() * math.tau
        r = S * (0.40 + 0.055 * rng.random())
        m.disc((S / 2 + r * math.cos(a), S / 2 + r * math.sin(a)), 3 + 6 * rng.random())
    bub = [(S * 0.40, S * 0.45, 8), (S * 0.62, S * 0.36, 5), (S * 0.55, S * 0.66, 10), (S * 0.30, S * 0.62, 5), (S * 0.70, S * 0.58, 6)]
    spec = [(S * 0.38, S * 0.26, S * 0.12, S * 0.03, -0.35, 0.7), (S * 0.66, S * 0.76, S * 0.09, S * 0.02, -0.4, 0.4)]
    marr = m.arr()
    g, a, hi = gel(marr, 33, 16, bubbles=bub, spec=spec, net=0.55, net_n=18, edge=0.55)
    # concentric ripples settling in the pool, faint
    x, y = grid(S, S)
    r = np.hypot(x - S / 2, y - S / 2) / (S * 0.5)
    rip = (0.5 + 0.5 * np.sin(r * 34.0)) * np.exp(-((r - 0.45) / 0.25) ** 2) * 0.22
    hi = np.clip(hi + rip * marr, 0, 1)
    save(rgba(g, a), "gel_puddle")
    save(white(hi), "gel_puddle_hi")


def tex_splat():
    S = 256
    rng = np.random.default_rng(41)
    m = Mask(S, S)
    c = (S / 2, S / 2)
    m.ellipse(c, S * 0.19, S * 0.18)
    n = 11
    for i in range(n):
        a = math.tau * (i + 0.5 * rng.random()) / n
        L = S * (0.30 + 0.17 * rng.random() * (1 if i % 2 == 0 else 0.7))
        w0 = S * (0.12 + 0.03 * rng.random())
        p0 = (c[0] + math.cos(a) * S * 0.08, c[1] + math.sin(a) * S * 0.08)
        p1 = (c[0] + math.cos(a) * L, c[1] + math.sin(a) * L)
        m.taper(p0, p1, w0, S * 0.025)
        m.disc(p1, S * (0.025 + 0.02 * rng.random()))                    # the bulb at the tip of every arm
        if rng.random() < 0.6:                                          # a droplet flung beyond it
            q = (c[0] + math.cos(a + 0.05) * (L + S * 0.05), c[1] + math.sin(a + 0.05) * (L + S * 0.05))
            if max(abs(q[0] - S / 2), abs(q[1] - S / 2)) < S * 0.47:
                m.disc(q, 3 + 3 * rng.random())
    for k in range(7):
        a = rng.random() * math.tau
        r = S * (0.32 + 0.12 * rng.random())
        q = (c[0] + math.cos(a) * r, c[1] + math.sin(a) * r)
        if max(abs(q[0] - S / 2), abs(q[1] - S / 2)) < S * 0.47:
            m.disc(q, 2 + 3 * rng.random())
    bub = [(S * 0.45, S * 0.44, 6), (S * 0.57, S * 0.55, 8), (S * 0.50, S * 0.40, 4)]
    spec = [(S * 0.42, S * 0.40, S * 0.07, S * 0.03, -0.7, 1.0)]
    save_pair("gel_splat", m.arr(), 43, 14, bubbles=bub, spec=spec, net=0.4, net_n=12)


def salamander_mask(W, H):
    m = Mask(W, H)
    rng = np.random.default_rng(51)
    # spine, tail (t = 0) to head (t = 1)
    def spine(t):
        x = W * (0.05 + 0.84 * t)
        y = H * 0.5 + H * 0.115 * math.sin(t * math.tau * 1.15 + 0.5) * (1 - 0.35 * t)
        return x, y

    def radius(t):
        if t < 0.42:
            k = t / 0.42
            return 1.5 + (H * 0.105 - 1.5) * (k ** 1.35)
        if t < 0.78:
            return H * (0.105 + 0.016 * math.sin((t - 0.42) / 0.36 * math.pi))
        if t < 0.86:
            return H * (0.105 - 0.024 * (t - 0.78) / 0.08)
        return H * (0.082 + 0.016 * math.sin((t - 0.86) / 0.14 * math.pi) - 0.03 * ((t - 0.86) / 0.14) ** 2)

    N = 420
    for i in range(N + 1):
        t = i / N
        x, y = spine(t)
        m.disc((x, y), radius(t))
    hx, hy = spine(0.93)
    m.ellipse((hx, hy), H * 0.14, H * 0.10)                              # the head, wider than the neck
    sx, sy = spine(1.0)
    m.disc((sx, sy), H * 0.05)                                          # the snout
    # legs: (t on the spine, side, reach)
    for t, side, fwd in [(0.74, -1, 1), (0.74, 1, 1), (0.40, -1, -1), (0.40, 1, -1)]:
        x, y = spine(t)
        up = (x - W * 0.012 * fwd, y + side * H * 0.06)
        knee = (x + fwd * W * 0.02, y + side * H * 0.22)
        foot = (x + fwd * W * 0.055, y + side * H * 0.33)
        m.taper(up, knee, H * 0.09, H * 0.065)
        m.taper(knee, foot, H * 0.065, H * 0.05)
        for k in (-1, 0, 1):                                            # three toes
            tip = (foot[0] + fwd * W * 0.022 + k * W * 0.012, foot[1] + side * H * 0.075 + k * fwd * side * H * 0.01 * 0)
            tip = (foot[0] + fwd * W * 0.02 + k * W * 0.022 * fwd, foot[1] + side * H * 0.06)
            m.taper(foot, tip, H * 0.035, H * 0.016)
    return m.arr(), spine, radius


def tex_salamander():
    W, H = 256, 128
    marr, spine, radius = salamander_mask(W, H)
    # spine beads and back spots, eyes
    glints = []
    for i in range(10):
        t = 0.12 + 0.72 * i / 9
        x, y = spine(t)
        glints.append((x, y, 1.8 + 3.0 * radius(t) / (H * 0.105) * 0.5))
    ex, ey = spine(0.945)
    eyes = [(ex, ey - H * 0.06, 3.0), (ex, ey + H * 0.06, 3.0)]
    spec = [(spine(0.60)[0], spine(0.60)[1] - H * 0.06, W * 0.12, H * 0.025, -0.1, 0.9),
            (spine(0.87)[0], spine(0.87)[1] - H * 0.06, W * 0.04, H * 0.02, -0.3, 0.8)]
    g, a, hi = gel(marr, 53, 8, bubbles=glints, spec=spec, net=0.5, net_n=22, edge=0.5)
    x, y = grid(W, H)
    for ex_, ey_, r in eyes:                                             # the eyes: dark wet beads with a glint
        d = np.hypot(x - ex_, y - ey_)
        g = np.where(d < r, 0.1, g)
        a = np.where(d < r, np.maximum(a, 0.95), a)
        hi = np.maximum(hi, 0.95 * np.exp(-(np.hypot(x - (ex_ - 1.0), y - (ey_ - 1.2)) / 1.3) ** 2))
    save(rgba(g, a), "gel_salamander")
    save(white(hi), "gel_salamander_hi")


def tex_ring():
    S = 256
    x, y = grid(S, S)
    dx, dy = x - S / 2, y - S / 2
    r = np.hypot(dx, dy) / (S / 2)
    th = np.arctan2(dy, dx)
    rng = np.random.default_rng(61)
    ph = rng.random(5) * math.tau
    wob = 0.012 * np.sin(5 * th + ph[0]) + 0.009 * np.sin(8 * th + ph[1]) + 0.006 * np.sin(13 * th + ph[2])
    rr = r - wob
    c0 = 0.80
    ridge = np.exp(-((rr - c0) / 0.028) ** 2)                            # the bright bulging ridge
    belly = np.exp(-((rr - c0) / 0.075) ** 2)                            # the translucent body of the wave
    wake = (0.5 + 0.5 * np.sin((c0 - rr) * 120.0)) * np.exp(-np.clip(c0 - rr, 0, 9) / 0.16) * (rr < c0) * 0.35
    lit = 0.65 + 0.35 * np.clip((dx * -0.6 + dy * -0.8) / (S / 2 * 0.8) * 1.2, -1, 1)
    foam = fbm2(S, S, 24, 24, 63, 3)
    g = 0.45 * belly + 0.75 * ridge * lit + 0.30 * wake
    a = 0.55 * belly + 0.9 * ridge + 0.45 * wake * (foam + 0.2)
    # beads thrown just outside the ridge
    m = Mask(S, S)
    for i in range(22):
        ang = rng.random() * math.tau
        rad = (c0 + 0.045 + 0.05 * rng.random()) * S / 2
        m.disc((S / 2 + rad * math.cos(ang), S / 2 + rad * math.sin(ang)), 1.6 + 3.2 * rng.random() ** 2)
    beads = m.arr()
    g = g + 0.8 * beads
    a = a + 0.9 * beads
    edge = 1 - sstep(0.93, 0.995, r)
    save(rgba(np.clip(g, 0, 1) * edge, np.clip(a, 0, 1) * edge), "gel_ring")


def tex_strand():
    W, H = 64, 256
    m = Mask(W, H)
    for i in range(500):
        u = i / 499.0                                                   # 0 = bottom (the caster's end), 1 = top
        y = H * (1 - u)
        r = 3.2 + 14.5 * math.exp(-(u / 0.07) ** 2) + 5.5 * math.exp(-((u - 0.30) / 0.028) ** 2) + 4.5 * math.exp(-((u - 0.52) / 0.022) ** 2) \
            + 6.0 * math.exp(-((u - 0.74) / 0.026) ** 2) + 9.0 * math.exp(-((u - 0.965) / 0.03) ** 2)
        r += 0.7 * math.sin(u * 40)
        x = W / 2 + 1.6 * math.sin(u * 9.0)
        m.disc((x, y), r)
    spec = [(W * 0.42, H * 0.955, 4, 7, 0, 0.9), (W * 0.40, H * 0.06, 5, 9, 0, 0.9)]
    save(baked(m.arr(), 71, 6, bubbles=[(W * 0.5, H * 0.94, 3)], spec=spec, net=0.3, net_n=8), "gel_strand")


def tex_drops():
    W, H = 256, 128
    m = Mask(W, H)
    # cell 0: a teardrop, tip up
    cx = 32
    pts = []
    for i in range(120):
        a = math.tau * i / 120
        pts.append((cx + 19 * math.cos(a), 84 + 19 * math.sin(a)))
    m.poly(pts)
    m.poly([(cx - 17.5, 76), (cx + 17.5, 76), (cx + 2.0, 12), (cx - 2.0, 12)])
    m.disc((cx, 13), 2.0)
    # cell 1: a round bead with a short neck
    cx = 96
    m.disc((cx, 68), 22)
    m.taper((cx, 50), (cx, 26), 14, 3)
    # cell 2: a long falling drop (a thin neck below the head)
    cx = 160
    m.ellipse((cx, 92), 16, 20)
    m.taper((cx, 80), (cx, 10), 14, 2.5)
    m.disc((cx, 120), 3.5)
    # cell 3: a double drop, a small one about to pinch off above a big one
    cx = 224
    m.disc((cx, 86), 20)
    m.taper((cx, 70), (cx, 52), 12, 3)
    m.disc((cx, 38), 10)
    m.disc((cx, 14), 4)
    marr = m.arr()
    g, a, hi = gel(marr, 81, 7, bubbles=[(24, 90, 3), (88, 72, 4), (152, 96, 3), (216, 90, 4)],
                   spec=[(24, 76, 4, 9, 0.35, 0.95), (88, 60, 4.5, 9, 0.35, 0.95), (152, 86, 4, 8, 0.35, 0.95), (216, 80, 4.5, 9, 0.35, 0.95), (221, 34, 2.6, 4, 0.3, 0.9)],
                   net=0.18, net_n=10)
    save(rgba(np.clip(g + 0.9 * hi, 0, 1), np.clip(a + 0.5 * hi * (marr > 0.02), 0, 1)), "gel_drops")


def tex_bubbles():
    S = 256
    x, y = grid(S, S)
    g = np.zeros((S, S), np.float32)
    a = np.zeros((S, S), np.float32)
    cells = [
        [(64, 66, 46)],
        [(48, 80, 38), (92, 44, 20)],
        [(40, 48, 22), (86, 40, 14), (60, 84, 18), (98, 78, 11), (28, 100, 9)],
        [(34, 34, 13), (64, 28, 9), (92, 38, 11), (46, 62, 15), (80, 68, 12), (28, 90, 9), (58, 98, 13), (96, 98, 8), (104, 66, 7)],
    ]
    for ci, items in enumerate(cells):
        ox, oy = (ci % 2) * 128, (ci // 2) * 128
        for bx, by, r in items:
            cx, cy = ox + bx, oy + by
            d = np.hypot(x - cx, y - cy)
            inside = (np.abs(x - cx) < r + 3) & (np.abs(y - cy) < r + 3)
            ang_l = np.clip(((x - cx) * -0.62 + (y - cy) * -0.78) / (r + 1e-6), -1, 1)
            ring = np.exp(-((d - r) / 1.15) ** 2) * (0.45 + 0.55 * np.clip(ang_l * 0.5 + 0.5, 0, 1) ** 1.4)
            ring2 = np.exp(-((d - r * 0.93) / 0.9) ** 2) * np.clip(-ang_l, 0, 1) * 0.55      # the lower inner rim: light leaving the bubble
            fill = 0.10 * np.clip(1 - (d / r) ** 2, 0, 1) * (d < r)
            hl = np.exp(-(((x - (cx - r * 0.42)) / (r * 0.22)) ** 2 + ((y - (cy - r * 0.46)) / (r * 0.10)) ** 2)) * 1.0
            hl2 = np.exp(-(np.hypot(x - (cx + r * 0.38), y - (cy + r * 0.45)) / max(1.0, r * 0.1)) ** 2) * 0.55
            v = np.clip(ring + ring2 + hl + hl2, 0, 1) * inside
            g = np.maximum(g, np.clip(0.55 * ring + ring2 + hl + hl2 + 0.4 * fill / 0.1 * 0.1, 0, 1) * inside)
            a = np.maximum(a, np.clip(v + fill * inside, 0, 1))
    save(rgba(np.clip(0.55 + g * 0.45, 0, 1), a), "gel_bubbles")


def tex_wall():
    W, H = 512, 128
    x, y = grid(W, H)
    u, v = x / W, y / H
    streak = fbm2(W, H, 16, 128, 91, 4)                                  # long vertical flows, tileable in x
    streak2 = fbm2(W, H, 6, 64, 93, 3)
    lobes = 0.0
    for k, (cyc, amp, ph) in enumerate([(14, 0.040, 0.3), (23, 0.020, 1.7), (37, 0.010, 4.1)]):
        lobes = lobes + amp * np.sin(math.tau * cyc * u + ph)
    foot = 0.80 + lobes                                                   # the scalloped top edge of the thick meniscus
    meniscus = sstep(foot - 0.02, foot + 0.05, v)                         # 0 above, 1 in the thick foot (image bottom)
    body = 0.30 + 0.34 * streak + 0.14 * (streak2 - 0.5)
    top_fade = sstep(0.0, 0.16, v)                                        # the curtain thins out toward its top
    a = body * top_fade * (1 - 0.35 * meniscus) + meniscus * (0.78 + 0.18 * streak)
    g = 0.46 + 0.30 * streak + 0.30 * meniscus * (0.5 + 0.5 * np.sin(math.tau * 14 * u + 0.3 + 5 * (v - foot)))
    # bright flow lines in the curtain and a lit lip along the meniscus
    flow = np.exp(-((streak - 0.58) / 0.045) ** 2) * top_fade * (1 - meniscus) * 0.7
    lip = np.exp(-((v - foot - 0.01) / 0.018) ** 2) * (0.6 + 0.4 * np.sin(math.tau * 14 * u + 1.2) )
    # trapped bubbles in the curtain
    rng = np.random.default_rng(97)
    bub = np.zeros((H, W), np.float32)
    for i in range(34):
        cx, cy, r = rng.random() * W, 14 + rng.random() * 98, 2.0 + 6.5 * rng.random() ** 2
        for ox in (-W, 0, W):
            d = np.hypot(x - (cx + ox), y - cy)
            ring = np.exp(-((d - r) / 0.9) ** 2) * (0.5 + 0.5 * np.clip(((x - cx - ox) * -0.6 + (y - cy) * -0.8) / r, 0, 1))
            dot = np.exp(-(np.hypot(x - (cx + ox - r * 0.4), y - (cy - r * 0.42)) / max(0.8, r * 0.22)) ** 2)
            bub = np.maximum(bub, 0.6 * ring + 0.9 * dot)
    g = g + 0.55 * flow + 0.7 * lip + bub * 0.7
    a = a + 0.4 * flow + 0.5 * lip + bub * 0.55
    # soft top edge, no hard cut at the bottom
    a = a * sstep(0.0, 0.05, v) * (1 - sstep(0.985, 1.0, v))
    save(rgba(np.clip(g, 0, 1), np.clip(a, 0, 1)), "gel_wall")


def tex_glint():
    S = 128
    x, y = grid(S, S)
    dx, dy = x - S / 2, y - S / 2
    r = np.hypot(dx, dy)
    core = np.exp(-(r / 7.0) ** 2) + 0.35 * np.exp(-(r / 16.0) ** 2)
    v = np.exp(-(dx / 1.8) ** 2) * np.exp(-(np.abs(dy) / 40.0) ** 1.2)
    hz = np.exp(-(dy / 1.8) ** 2) * np.exp(-(np.abs(dx) / 40.0) ** 1.2)
    rd = (dx + dy) / 1.414
    cd = (dx - dy) / 1.414
    d1 = np.exp(-(rd / 1.5) ** 2) * np.exp(-(np.abs(cd) / 14.0) ** 1.4) * 0.5
    d2 = np.exp(-(cd / 1.5) ** 2) * np.exp(-(np.abs(rd) / 14.0) ** 1.4) * 0.5
    s = np.clip(core + v + hz + d1 + d2, 0, 1) * (1 - sstep(0.80, 1.0, r / (S / 2)))
    save(white(s), "gel_glint")


ALL = {
    "gel_blob": tex_blob, "gel_puddle": tex_puddle, "gel_splat": tex_splat, "gel_salamander": tex_salamander, "gel_ring": tex_ring,
    "gel_strand": tex_strand, "gel_drops": tex_drops, "gel_bubbles": tex_bubbles, "gel_wall": tex_wall, "gel_glint": tex_glint,
}

if __name__ == "__main__":
    names = sys.argv[1:] or list(ALL)
    for n in names:
        ALL[n]()

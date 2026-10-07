"""Textures for the Curse Magic VFX (vfx/client/layer/CurseLayer). Deterministic and procedural: no fonts, no input images.

    python3 -B tools/gen_curse_textures.py [name ...]        # no names = all

Writes src/main/resources/assets/nusmp/textures/particle/curse_*.png.

Look (Black Clover, Curse Magic: curses descend from Megicula): glowing, jagged black runes; thorned black marks that crawl and
glow deep purple; ash; veins of corruption cracking the ground; soul wisps drained out of the victim. High contrast: black
bodies, violet-white light.

How the textures are read (this is the trick of the whole set): ALPHA is the silhouette and GREY is the glow. The layer draws a
texture twice, once ALPHA in near-black (the cursed mass, with its soft dark halo) and once ADD in violet (only the bright
strokes, cracks and rims light up). One texture, two passes: a black body with light in it. Where the grey is the point
(flash, ring, wisp) the texture is only drawn additive.

  curse_glyphs   4 x 4 atlas of 16 jagged runes (one billboard each; the layer cuts the cells out with UV rectangles)
  curse_band     512 x 64 tileable rune ribbon between two serrated rails (rings round the bolt, walls)
  curse_sigil    256 ground sigil: serrated rails, a ring of runes, a saw ring, a hexagram of blades, an inner ring
  curse_brand    256 the curse brand: five thorned blades in a pinwheel inside a broken, barbed ring (the signature mark)
  curse_veins    256 corruption spreading over the ground: a ragged black blight with glowing branching cracks
  curse_wall     512 x 128 tileable wall: a crown of black thorns over a smoky mass, glowing cracks, a row of runes at its foot
  curse_bolt     64 x 256 (tail at the bottom, head at the top) the hex needle: barbed shaft, jagged core, crackle, tail strands
  curse_tendril  64 x 256 (base at the bottom, tip at the top) a thorned, sinuous black tendril with a glowing seam
  curse_smoke    256 ragged ash smoke puff with rim light
  curse_flakes   128 a cloud of ash flakes and embers
  curse_flash    128 jagged starburst flash (hot core, uneven spikes, four long needles, fine sparks)
  curse_corona   256 an eclipse: hollow centre, hard bright limb, jagged streamers (additive, over a black body)
  curse_ring     256 jagged shock ring: a bright line with spikes, a hatched inner shock band
  curse_wisp     64 x 128 a drained soul: teardrop head with hollow eyes and a flowing tail
  curse_shards   256 atlas (2 x 2 cells of 128): tall thorn, fang cluster, obsidian dagger, hooked barb
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


def vnoise(w, h, cell, seed):
    """Tileable value noise 0..1 (smoothstep interpolation)."""
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
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


def fbm(w, h, cell, seed, octaves=5):
    n, amp, tot = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octaves):
        n += vnoise(w, h, max(1, cell >> o), seed + 17 * o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def sstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def sample(a, x, y):
    """Bilinear sample (wrapping) of array a at float pixel coordinates."""
    h, w = a.shape
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    tx, ty = x - x0, y - y0
    x1, y1 = (x0 + 1) % w, (y0 + 1) % h
    x0, y0 = x0 % w, y0 % h
    return (a[y0, x0] * (1 - tx) + a[y0, x1] * tx) * (1 - ty) + (a[y1, x0] * (1 - tx) + a[y1, x1] * tx) * ty


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


# ------------------------------------------------------------------------------------------------ drawing
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

    def blade(self, p0, p1, hw, at=0.35, v=255):
        """A kite: pointed at both ends, widest at `at` of its length."""
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dy) or 1e-6
        nx, ny = -dy / L, dx / L
        m = (p0[0] + dx * at, p0[1] + dy * at)
        self.poly([p0, (m[0] + nx * hw, m[1] + ny * hw), p1, (m[0] - nx * hw, m[1] - ny * hw)], v)

    def taper(self, p0, p1, w0, w1, v=255):
        """A trapezoid from p0 (full width w0) to p1 (width w1)."""
        dx, dy = p1[0] - p0[0], p1[1] - p0[1]
        L = math.hypot(dx, dy) or 1e-6
        nx, ny = -dy / L, dx / L
        self.poly([(p0[0] + nx * w0 / 2, p0[1] + ny * w0 / 2), (p1[0] + nx * w1 / 2, p1[1] + ny * w1 / 2),
                   (p1[0] - nx * w1 / 2, p1[1] - ny * w1 / 2), (p0[0] - nx * w0 / 2, p0[1] - ny * w0 / 2)], v)

    def path(self, pts, w0, w1=None, v=255):
        """A polyline whose width tapers from w0 (first point) to w1 (last), with round joints."""
        w1 = w0 if w1 is None else w1
        lens = [0.0]
        for i in range(1, len(pts)):
            lens.append(lens[-1] + math.hypot(pts[i][0] - pts[i - 1][0], pts[i][1] - pts[i - 1][1]))
        total = lens[-1] or 1.0
        ws = [w0 + (w1 - w0) * l / total for l in lens]
        for i in range(len(pts) - 1):
            p, q = pts[i], pts[i + 1]
            dx, dy = q[0] - p[0], q[1] - p[1]
            L = math.hypot(dx, dy) or 1e-6
            nx, ny = -dy / L, dx / L
            hp, hq = ws[i] / 2, ws[i + 1] / 2
            self.poly([(p[0] + nx * hp, p[1] + ny * hp), (q[0] + nx * hq, q[1] + ny * hq),
                       (q[0] - nx * hq, q[1] - ny * hq), (p[0] - nx * hp, p[1] - ny * hp)], v)
            self.disc(q, hq, v)
        self.disc(pts[0], ws[0] / 2, v)

    def arc(self, c, r, a0, a1, w, v=255):
        n = max(4, int(abs(a1 - a0) * r / 2.0))
        pts = [(c[0] + math.cos(a0 + (a1 - a0) * i / n) * r, c[1] + math.sin(a0 + (a1 - a0) * i / n) * r) for i in range(n + 1)]
        self.path(pts, w, w, v)

    def arr(self):
        return np.asarray(self.im.resize((self.w, self.h), Image.LANCZOS), np.float32) / 255.0


def jag(rng, p0, p1, n, amp):
    """A jagged line from p0 to p1 with n pieces."""
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L, dx / L
    pts = [p0]
    for i in range(1, n):
        t = i / n
        o = rng.uniform(-amp, amp)
        pts.append((p0[0] + dx * t + nx * o, p0[1] + dy * t + ny * o))
    pts.append(p1)
    return pts


def finish(core, body=None, fill=None, s1=1.3, s2=4.5, g1=0.6, g2=0.32, wrap_x=False, wrap_y=False, body_alpha=0.92):
    """core = bright strokes, body = the black silhouette, fill = a faint dark fill. -> RGBA (alpha = silhouette + halo, grey = glow)."""
    h1, h2 = blur(core, s1, wrap_x, wrap_y), blur(core, s2, wrap_x, wrap_y)
    grey = np.clip(core + g1 * h1 + g2 * h2, 0, 1)
    parts = [core, h1 * 1.15, h2 * 0.95]
    if body is not None:
        parts.append(body * body_alpha)
    if fill is not None:
        parts.append(fill)
    return rgba(grey, np.maximum.reduce(parts))


# ------------------------------------------------------------------------------------------------ the runes
_DELTAS = [(-1, -1), (1, -1), (-1, 1), (1, 1), (-1, -2), (1, -2), (-1, 2), (1, 2), (0, 2), (0, -2), (1, 0), (-1, 0)]
_GLYPHS = {}


def glyph_edges(idx):
    """The strokes of jagged rune number idx: [((x0, y0), (x1, y1), weight)] in a 0..1 box (y down). Deterministic."""
    if idx in _GLYPHS:
        return _GLYPHS[idx]
    rng = np.random.default_rng(7100 + idx * 37)
    xs, ys = (0.16, 0.5, 0.84), (0.04, 0.27, 0.5, 0.73, 0.96)
    staff = rng.random() < 0.72
    E, U = [], []
    if staff:
        E.append(((1, 0), (1, 4)))
        U += [(1, 0), (1, 1), (1, 2), (1, 3), (1, 4)]
    else:
        pat = int(rng.integers(0, 3))
        if pat == 0:
            E += [((0, 0), (2, 2)), ((2, 2), (0, 4))]
            U += [(0, 0), (2, 2), (0, 4)]
        elif pat == 1:
            E += [((2, 0), (0, 2)), ((0, 2), (2, 4))]
            U += [(2, 0), (0, 2), (2, 4)]
        else:
            E += [((0, 0), (0, 4)), ((0, 2), (2, 0))]
            U += [(0, 0), (0, 2), (0, 4), (2, 0)]
    want, tries = int(rng.integers(2, 5)) + (0 if staff else 1), 0
    while want > 0 and tries < 80:
        tries += 1
        a = U[int(rng.integers(len(U)))]
        d = _DELTAS[int(rng.integers(len(_DELTAS)))]
        b = (a[0] + d[0], a[1] + d[1])
        if not (0 <= b[0] <= 2 and 0 <= b[1] <= 4) or (a, b) in E or (b, a) in E:
            continue
        if staff and a[0] == 1 and b[0] == 1:
            continue
        E.append((a, b))
        U.append(b)
        want -= 1
    jit = {}

    def pt(n):
        if n not in jit:
            jit[n] = (xs[n[0]] + rng.uniform(-0.03, 0.03), ys[n[1]] + rng.uniform(-0.02, 0.02))
        return jit[n]

    out = []
    for k, (a, b) in enumerate(E):
        p0, p1 = pt(a), pt(b)
        out.append((p0, p1, 1.0))
        if k > 0 and rng.random() < 0.5:                       # a barb at the end of the stroke
            ang = math.atan2(p1[1] - p0[1], p1[0] - p0[0]) + (1 if rng.random() < 0.5 else -1) * rng.uniform(0.7, 1.2)
            out.append((p1, (p1[0] + math.cos(ang) * 0.17, p1[1] + math.sin(ang) * 0.17), 0.7))
    _GLYPHS[idx] = out
    return out


def paint_glyph(mask, edges, cx, cy, w, h, rot, hw, v=255):
    ca, sa = math.cos(rot), math.sin(rot)

    def T(p):
        x, y = (p[0] - 0.5) * w, (p[1] - 0.5) * h
        return (cx + x * ca - y * sa, cy + x * sa + y * ca)

    for p0, p1, wgt in edges:
        mask.blade(T(p0), T(p1), hw * wgt, 0.32, v)


# ------------------------------------------------------------------------------------------------ textures
def tex_glyphs():
    S, cell = 256, 64
    core, body = Mask(S, S), Mask(S, S)
    for k in range(16):
        cx, cy = (k % 4) * cell + cell / 2, (k // 4) * cell + cell / 2
        e = glyph_edges(k)
        paint_glyph(body, e, cx, cy, 30, 46, 0, 3.8)
        paint_glyph(core, e, cx, cy, 30, 46, 0, 1.55)
    save(finish(core.arr(), body.arr()), "curse_glyphs")


def tex_band():
    W, H = 512, 64
    core, body = Mask(W, H), Mask(W, H)
    for y, d in ((5.0, 1), (H - 5.0, -1)):                    # the two serrated rails
        core.path([(0, y), (W, y)], 1.7)
        body.path([(0, y), (W, y)], 5.0)
        for x in range(0, W, 8):
            core.poly([(x, y), (x + 5, y), (x + 2.5, y + d * 6)])
            body.poly([(x - 1.5, y - d * 1.5), (x + 6.5, y - d * 1.5), (x + 2.5, y + d * 8)])
    for i in range(8):                                        # eight runes per tile
        e = glyph_edges((i * 5 + 3) % 16)
        paint_glyph(body, e, 32 + i * 64, 32, 22, 34, 0, 3.4)
        paint_glyph(core, e, 32 + i * 64, 32, 22, 34, 0, 1.45)
    for i in range(9):                                        # a diamond between the runes (the ones on the seam wrap)
        x = i * 64
        for m, hw, hh in ((core, 2.6, 6.5), (body, 4.4, 9.5)):
            m.poly([(x, 32 - hh), (x + hw, 32), (x, 32 + hh), (x - hw, 32)])
    fill = Mask(W, H)                                          # a dark translucent ribbon behind the runes
    fill.poly([(0, 5), (W, 5), (W, H - 5), (0, H - 5)], 170)
    save(finish(core.arr(), body.arr(), fill.arr(), wrap_x=True), "curse_band")


def tex_sigil(S=256):
    c = S / 2.0
    R = c - 2.0
    core, body, fill = Mask(S, S), Mask(S, S), Mask(S, S)

    def P(r, a):
        return (c + math.cos(a) * r * R, c + math.sin(a) * r * R)

    fill.disc((c, c), 0.985 * R, 118)                           # a faint dark ground under everything
    fill.arc((c, c), 0.795 * R, 0, math.tau, 0.19 * R, 205)   # a darker band behind the runes

    def rail(r, n, gap, w, spike):
        step = math.tau / n
        for i in range(n):
            a0, a1 = i * step + gap / 2, (i + 1) * step - gap / 2
            core.arc((c, c), r * R, a0, a1, w)
            body.arc((c, c), r * R, a0, a1, w * 2.6)
            if spike:
                for a in (a0, a1):
                    core.blade(P(r - 0.012, a), P(r + spike, a), 1.5, 0.3)
                    body.blade(P(r - 0.012, a), P(r + spike, a), 3.2, 0.3)

    rail(0.965, 18, 0.10, 1.9, 0.034)                          # outer rail: broken, with a thorn at every break
    rail(0.915, 36, 0.07, 1.25, 0.0)
    for i in range(120):                                       # fine ticks between the two rails
        a = math.tau * i / 120
        core.path([P(0.928, a), P(0.945 if i % 5 else 0.952, a)], 0.9)
    n = 22
    for i in range(n):                                         # the rune ring
        a = math.tau * i / n + 0.05
        cx, cy = P(0.80, a)
        e = glyph_edges((i * 7 + 3) % 16)
        paint_glyph(body, e, cx, cy, 0.125 * R, 0.165 * R, a + math.pi / 2, 3.0)
        paint_glyph(core, e, cx, cy, 0.125 * R, 0.165 * R, a + math.pi / 2, 1.2)
    for r in (0.715, 0.685):                                   # the saw ring: two rails with teeth between them
        core.arc((c, c), r * R, 0, math.tau, 1.3)
        body.arc((c, c), r * R, 0, math.tau, 3.4)
    for i in range(48):
        a0, a1 = math.tau * i / 48, math.tau * (i + 0.5) / 48
        for m, k in ((core, 1.0), (body, 1.6)):
            m.poly([P(0.69, a0), P(0.69, a1), P(0.712 if i % 2 else 0.70, (a0 + a1) / 2)])
    for tri in range(2):                                       # hexagram of blades
        pts = [P(0.655, math.radians(-90 + tri * 60 + k * 120)) for k in range(3)]
        for k in range(3):
            core.blade(pts[k], pts[(k + 1) % 3], 2.0, 0.5)
            body.blade(pts[k], pts[(k + 1) % 3], 5.0, 0.5)
    for k in range(6):
        p = P(0.655, math.radians(-90 + k * 60))
        core.disc(p, 3.0)
        body.disc(p, 6.0)
    core.arc((c, c), 0.34 * R, 0, math.tau, 1.4)               # inner ring with six spokes
    body.arc((c, c), 0.34 * R, 0, math.tau, 3.6)
    core.arc((c, c), 0.31 * R, 0, math.tau, 0.9)
    for k in range(6):
        a = math.radians(-90 + k * 60 + 30)
        core.blade(P(0.34, a), P(0.46, a), 1.7, 0.4)
        body.blade(P(0.34, a), P(0.46, a), 3.6, 0.4)
        a2 = math.radians(-90 + k * 60)
        core.blade(P(0.34, a2), P(0.205, a2), 1.5, 0.4)
        body.blade(P(0.34, a2), P(0.205, a2), 3.2, 0.4)
    save(finish(core.arr(), body.arr(), fill.arr()), "curse_sigil")


def tex_brand(S=256):
    c = S / 2.0
    R = c - 3.0
    rng = np.random.default_rng(6502)
    core, body = Mask(S, S), Mask(S, S)

    def P(r, a):
        return (c + math.cos(a) * r, c + math.sin(a) * r)

    N = 5
    for k in range(N):                                         # five thorned blades in a pinwheel
        a0 = math.tau * k / N + 0.2
        pts = []
        for i in range(31):
            s = i / 30
            ang = a0 + s * 1.25 + 0.10 * math.sin(s * 9 + k)
            pts.append(P((0.11 + 0.62 * s ** 0.9) * R, ang))
        body.path(pts, 23, 1.6)
        core.path(pts, 7.0, 0.8)
        for si, sc in ((7, 0.95), (12, 0.8), (17, 0.65), (22, 0.5)):
            p, q = pts[si], pts[si + 1]
            ang = math.atan2(q[1] - p[1], q[0] - p[0]) - 0.95
            e = (p[0] + math.cos(ang) * 0.21 * R * sc, p[1] + math.sin(ang) * 0.21 * R * sc)
            body.taper(p, e, 11.0 * sc, 0.6)
            core.taper(p, e, 3.6 * sc, 0.5)
        core.disc(pts[-1], 2.6)
        body.disc(pts[-1], 4.8)
        a_mid = a0 + 0.62                                      # a jagged crack in the gap behind each arm
        cr = jag(rng, P(0.15 * R, a_mid + 0.75), P(0.60 * R, a_mid + 0.95), 7, 0.045 * R)
        core.path(cr, 1.5, 0.6)
    n = 10                                                     # the broken, barbed outer ring
    for i in range(n):
        a0, a1 = math.tau * i / n + 0.09, math.tau * (i + 1) / n - 0.09
        core.arc((c, c), 0.865 * R, a0, a1, 2.0)
        body.arc((c, c), 0.865 * R, a0, a1, 5.4)
        for a, sgn in ((a0, 1), (a1, 1)):
            core.blade(P(0.865 * R, a), P(0.865 * R - 0.075 * R, a + (0.10 if a == a0 else -0.10)), 1.5, 0.3)
            body.blade(P(0.865 * R, a), P(0.865 * R - 0.075 * R, a + (0.10 if a == a0 else -0.10)), 3.4, 0.3)
        am = (a0 + a1) / 2
        core.blade(P(0.865 * R, am), P(0.985 * R, am), 2.1, 0.3)
        body.blade(P(0.865 * R, am), P(0.985 * R, am), 4.6, 0.3)
    core.arc((c, c), 0.095 * R, 0, math.tau, 2.4)
    body.arc((c, c), 0.095 * R, 0, math.tau, 6.0)
    core.disc((c, c), 3.4)
    save(finish(core.arr(), body.arr()), "curse_brand")


def tex_veins(S=256):
    c = S / 2.0
    R = c - 3.0
    rng = np.random.default_rng(8214)
    core, body = Mask(S, S), Mask(S, S)

    def branch(p, ang, length, w, depth):
        pts, cur = [p], p
        steps = max(3, int(length / 8))
        for i in range(steps):
            ang += rng.normal(0, 0.30)
            cur = (cur[0] + math.cos(ang) * length / steps, cur[1] + math.sin(ang) * length / steps)
            if math.hypot(cur[0] - c, cur[1] - c) > R * 0.97:
                break
            pts.append(cur)
            if depth < 3 and i > 0 and rng.random() < 0.30:
                branch(cur, ang + (1 if rng.random() < 0.5 else -1) * rng.uniform(0.5, 1.0), length * rng.uniform(0.35, 0.6), w * 0.62, depth + 1)
        if len(pts) > 1:
            body.path(pts, w * 2.6, w * 0.8)
            core.path(pts, w, w * 0.35)

    for k in range(9):
        branch((c, c), math.tau * k / 9 + rng.uniform(-0.2, 0.2), R * rng.uniform(0.8, 0.97), 3.3, 0)
    core.disc((c, c), 6.0)
    body.disc((c, c), 13.0)
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xx - c, yy - c) / R
    n1, n2 = fbm(S, S, 48, 8301), fbm(S, S, 24, 8317)
    mass = sstep(0.0, 0.34, (1.0 - r) * 1.15 + (n1 - 0.5) * 0.85 + (n2 - 0.5) * 0.25 - 0.05)    # ragged black blight
    cr, br = core.arr(), body.arr()
    glow = np.clip(cr + 0.5 * blur(cr, 1.6) + 0.35 * blur(cr, 5.0) + 0.06 * mass, 0, 1)
    alpha = np.maximum.reduce([mass * 0.86, br * 0.95, cr, blur(cr, 5.0) * 0.9])
    save(rgba(glow, alpha), "curse_veins")


def tex_wall():
    W, H = 512, 128
    rng = np.random.default_rng(9103)
    core, body = Mask(W, H), Mask(W, H)
    n = 19
    for i in range(n):                                         # the crown of black thorns (drawn twice across the seam)
        x = (i + 0.5) / n * W + rng.uniform(-9, 9)
        hw, tip_y, lean = rng.uniform(10, 18), rng.uniform(3, 50), rng.uniform(-9, 9)
        for ox in (-W, 0, W):
            base_l, base_r, tip = (x + ox - hw, H * 0.66), (x + ox + hw, H * 0.66), (x + ox + lean, tip_y)
            body.poly([base_l, (x + ox - hw * 0.35 + lean * 0.3, tip_y + (H * 0.66 - tip_y) * 0.5), tip,
                       (x + ox + hw * 0.55 + lean * 0.3, tip_y + (H * 0.66 - tip_y) * 0.45), base_r])
            core.path([(x + ox + hw * 0.55, H * 0.64), (x + ox + hw * 0.5 + lean * 0.35, tip_y + (H * 0.66 - tip_y) * 0.5), (x + ox + lean, tip_y + 2)], 2.0, 0.5)
    for i in range(16):                                        # glowing cracks climbing from the foot
        x = 10 + (i + rng.uniform(0.1, 0.9)) * (W - 20) / 16
        top = rng.uniform(46, 104)
        pts = jag(rng, (x, H + 2), (x + rng.uniform(-14, 14), top), 7, 4.5)
        core.path(pts, 2.2, 0.6)
        if rng.random() < 0.6:
            k = int(rng.integers(2, 5))
            br = jag(rng, pts[k], (pts[k][0] + rng.choice([-1, 1]) * rng.uniform(10, 22), pts[k][1] - rng.uniform(8, 16)), 3, 3)
            core.path(br, 1.4, 0.4)
    for y in (H * 0.715, H * 0.965):                           # rails and a row of runes along the foot
        core.path([(0, y), (W, y)], 1.4)
        body.path([(0, y), (W, y)], 4.0)
    for i in range(8):
        e = glyph_edges((i * 3 + 1) % 16)
        paint_glyph(body, e, 32 + i * 64, H * 0.84, 20, 26, 0, 3.0)
        paint_glyph(core, e, 32 + i * 64, H * 0.84, 20, 26, 0, 1.3)
    cr, br = core.arr(), body.arr()
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    v = yy / H
    mist = fbm(W, H, 32, 9150)
    mass = sstep(0.30, 0.66, v + (mist - 0.5) * 0.55) * (0.62 + 0.38 * v)
    glow = np.clip(cr + 0.55 * blur(cr, 1.4, True) + 0.3 * blur(cr, 4.5, True) + 0.07 * mass, 0, 1)
    alpha = np.maximum.reduce([mass * 0.82, br * 0.94, cr, blur(cr, 4.5, True) * 0.9])
    save(rgba(glow, alpha), "curse_wall")


def tex_bolt():
    L, Wd = 256, 64                                            # drawn lying down (tail left, head right), then stood up
    rng = np.random.default_rng(3303)
    core, body = Mask(L, Wd), Mask(L, Wd)

    def yc(x):
        return 32 + 1.6 * math.sin(x / 19.0)

    def hw(x):
        if x < 188:
            return 0.6 + 12.5 * (x / 188.0) ** 1.5
        return 13.1 * max(0.0, 1 - (x - 188) / 68.0) ** 0.85

    top = [(x, yc(x) - hw(x)) for x in range(0, 256, 4)] + [(255, yc(255))]
    bot = [(x, yc(x) + hw(x)) for x in range(252, -1, -4)]
    body.poly(top + bot)
    for x0 in (110, 138, 166):                                 # reversed barbs, like a harpoon
        for sgn in (-1, 1):
            base = (x0, yc(x0) + sgn * hw(x0) * 0.7)
            tip = (x0 - 38, yc(x0) + sgn * (hw(x0) + 16))
            body.taper(base, tip, 9.0, 0.5)
            core.taper(base, tip, 1.6, 0.4)
    zig = [(10.0, 32.0)]
    for x in range(19, 250, 9):                                # the jagged core
        zig.append((float(x), yc(x) + (2.8 if len(zig) % 2 else -2.8) * (0.5 + 0.5 * x / 250) + rng.uniform(-0.8, 0.8)))
    zig.append((254.0, yc(254)))
    core.path(zig, 2.2, 3.6)
    core.disc((236, yc(236)), 5.5)
    for _ in range(6):                                         # crackle off the shaft
        x = rng.uniform(80, 200)
        sgn = 1 if rng.random() < 0.5 else -1
        a = sgn * rng.uniform(0.4, 0.9)
        p0 = (x, yc(x) + sgn * hw(x) * 0.8)
        p1 = (x - 30 * math.cos(a) * 0.8, p0[1] + sgn * 20)
        core.path(jag(rng, p0, p1, 4, 2.5), 1.5, 0.5)
    for off in (-9, -4, 6, 11):                                # strands streaming off the tail
        x1 = rng.uniform(60, 105)
        core.path(jag(rng, (0, 32 + off * 0.3), (x1, 32 + off), 5, 1.2), 1.1, 0.6)
    cr, br = core.arr(), body.arr()
    ramp = (0.22 + 0.78 * (np.arange(L, dtype=np.float32) / (L - 1)) ** 1.25)[None, :]
    cr = cr * ramp
    h1, h2 = blur(cr, 1.5), blur(cr, 5.0)
    grey = np.clip(cr + 0.6 * h1 + 0.35 * h2 + 0.12 * br * ramp, 0, 1)
    alpha = np.maximum.reduce([br * 0.93, cr, h1 * 1.1, h2 * 0.9])
    save(rgba(np.rot90(grey, 1), np.rot90(alpha, 1)), "curse_bolt")


def tex_tendril():
    L, Wd = 256, 64                                            # base left, tip right, then stood up
    rng = np.random.default_rng(4410)
    core, body = Mask(L, Wd), Mask(L, Wd)

    def yc(x):
        return 32 + 11 * math.sin(x / 21.0 + 0.4) * (1 - x / 290.0)

    spine = [(float(x), yc(x)) for x in range(0, 252, 3)]
    body.path(spine, 26, 1.6)
    core.path(spine[2:], 4.4, 0.9)
    for i in range(7, len(spine) - 6, 5):                      # thorns, alternating sides, pointing toward the tip
        p, q = spine[i], spine[i + 1]
        sgn = 1 if (i // 5) % 2 else -1
        ang = math.atan2(q[1] - p[1], q[0] - p[0]) + sgn * (0.95 + rng.uniform(-0.2, 0.2))
        s = 1.0 - i / len(spine) * 0.7
        e = (p[0] + math.cos(ang) * 22 * s, p[1] + math.sin(ang) * 22 * s)
        body.taper(p, e, 9.0 * s, 0.5)
        core.taper(p, e, 2.6 * s, 0.4)
    cr, br = core.arr(), body.arr()
    ramp = (0.35 + 0.65 * (np.arange(L, dtype=np.float32) / (L - 1)) ** 0.9)[None, :]
    cr = cr * ramp
    h1, h2 = blur(cr, 1.4), blur(cr, 4.0)
    grey = np.clip(cr + 0.6 * h1 + 0.3 * h2 + 0.08 * br, 0, 1)
    alpha = np.maximum.reduce([br * 0.94, cr, h1 * 1.1, h2 * 0.9])
    save(rgba(np.rot90(grey, 1), np.rot90(alpha, 1)), "curse_tendril")


def tex_smoke(S=256):
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    c = (S - 1) / 2.0
    r = np.hypot(xx - c, yy - c) / c
    w1, w2 = fbm(S, S, 64, 5201, 4), fbm(S, S, 64, 5217, 4)
    base = fbm(S, S, 32, 5230, 6)
    d = sample(base, xx + (w1 - 0.5) * 80, yy + (w2 - 0.5) * 80)
    shape = np.clip(1 - r / 0.94, 0, 1) ** 0.65
    dens = d * 1.3 * (0.30 + shape) - 0.14
    alpha = sstep(0.0, 0.22, dens) * (1 - sstep(0.80, 1.0, r))
    rim = 1 - sstep(0.0, 0.34, dens)                           # thin smoke at the edge catches the light
    speck = (vnoise(S, S, 3, 5240) > 0.965).astype(np.float32) * sstep(0.1, 0.4, dens)
    grey = np.clip((0.07 + 0.9 * rim) * (alpha > 0.02) + speck * 0.9, 0, 1) * np.clip(alpha * 2.5, 0, 1)
    save(rgba(grey, alpha * 0.96), "curse_smoke")


def tex_flakes(S=128):
    rng = np.random.default_rng(1207)
    m, e = Mask(S, S), Mask(S, S)
    shade = Image.new("L", (S * SS, S * SS), 0)
    ds = ImageDraw.Draw(shade)
    pos = []
    while len(pos) < 52:
        p = (rng.uniform(8, S - 8), rng.uniform(8, S - 8))
        if all(math.hypot(p[0] - q[0], p[1] - q[1]) > 8.5 for q in pos):
            pos.append(p)
        if len(pos) < 52 and rng.random() < 0.02:
            break
    for i, p in enumerate(pos):
        n = int(rng.integers(3, 6))
        rad = rng.uniform(2.0, 5.5) * (1.4 if i % 9 == 0 else 1.0)
        a0 = rng.uniform(0, math.tau)
        pts = []
        for k in range(n):
            a = a0 + math.tau * k / n + rng.uniform(-0.4, 0.4)
            rr = rad * rng.uniform(0.55, 1.15)
            pts.append((p[0] + math.cos(a) * rr * 1.25, p[1] + math.sin(a) * rr * 0.8))
        m.poly(pts)
        ds.polygon([(x * SS, y * SS) for x, y in pts], fill=int(rng.uniform(110, 255)))
    for i in range(0, len(pos), 4):                            # a few embers
        e.disc(pos[i], 1.5)
    ma, ea = m.arr(), e.arr()
    sh = np.asarray(shade.resize((S, S), Image.LANCZOS), np.float32) / 255.0
    h = blur(ea, 3.0)
    save(rgba(np.clip(sh * 0.85 + ea + h * 0.8, 0, 1), np.maximum.reduce([ma * 0.95, ea, h * 0.9])), "curse_flakes")


def tex_flash(S=128):
    rng = np.random.default_rng(2718)
    c = S / 2.0
    R = c - 2.0
    core = Mask(S, S)
    n = 15
    for i in range(n):
        a = math.tau * i / n + rng.uniform(-0.16, 0.16)
        length = R * rng.uniform(0.40, 0.86)
        core.blade((c, c), (c + math.cos(a) * length, c + math.sin(a) * length), rng.uniform(1.8, 3.8), 0.14)
    for k in range(4):                                         # four long needles
        a = math.pi / 2 * k + 0.35
        core.blade((c, c), (c + math.cos(a) * R, c + math.sin(a) * R), 2.3, 0.1)
    for i in range(24):                                        # fine sparks between the spikes
        a = rng.uniform(0, math.tau)
        length = R * rng.uniform(0.18, 0.42)
        core.blade((c + math.cos(a) * 3, c + math.sin(a) * 3), (c + math.cos(a) * length, c + math.sin(a) * length), 1.1, 0.2)
    core.disc((c, c), 0.10 * R)
    cr = core.arr()
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    rr = np.hypot(xx - c, yy - c) / R
    radial = np.exp(-(rr / 0.17) ** 2)
    h1, h2 = blur(cr, 2.0), blur(cr, 7.0)
    grey = np.clip(cr + 0.7 * h1 + 0.5 * radial, 0, 1)
    alpha = np.maximum.reduce([cr, h1 * 1.2, h2 * 0.8, radial * 0.95]) * (1 - sstep(0.85, 1.0, rr))
    save(rgba(grey, alpha), "curse_flash")


def tex_corona(S=256):
    """An eclipse: a hollow, black centre ringed by a hard bright limb and jagged streamers (additive, over a black body)."""
    rng = np.random.default_rng(4242)
    c = (S - 1) / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xx - c, yy - c) / c
    th = np.arctan2(yy - c, xx - c)
    limb = 0.40
    reach_n = np.zeros_like(th)
    for k, amp in ((3, 1.0), (5, 0.9), (8, 0.8), (13, 0.7), (21, 0.55), (34, 0.4)):
        reach_n += amp * np.cos(k * th + rng.uniform(0, math.tau))
    reach_n = (reach_n / 4.35) * 0.5 + 0.5
    reach = limb + 0.10 + 0.50 * reach_n ** 1.6
    along = np.clip(1 - (r - limb) / np.maximum(reach - limb, 1e-3), 0, 1)
    rays = np.zeros_like(th)
    for k in (29, 47, 61):
        rays += np.clip(np.cos(k * th + rng.uniform(0, math.tau) + 2.5 * vnoise(S, S, 16, 4300 + k)) * 1.8 - 0.55, 0, 1)
    streamers = along ** 1.5 * np.clip(0.25 + rays * 0.6, 0, 1) * (r > limb)
    glow = np.exp(-np.maximum(r - limb, 0) / 0.065) * 0.85
    limb_line = np.exp(-(((r - limb - 0.012) / 0.011) ** 2))
    hole = sstep(limb - 0.012, limb + 0.004, r)
    fl = Mask(S, S)
    n = 26
    for i in range(n):                                         # jagged flares, a few of them long
        a = math.tau * i / n + rng.uniform(-0.12, 0.12)
        reach_px = rng.uniform(0.10, 0.22) if i % 3 else rng.uniform(0.36, 0.58)
        r0, r1 = (limb - 0.012) * c, (limb + reach_px) * c
        fl.blade((c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1),
                 rng.uniform(1.8, 3.6) * (1.5 if reach_px > 0.3 else 1.0), 0.22)
    flares = fl.arr()
    intensity = np.clip(glow * hole + streamers * 0.45 + flares * 0.95 + blur(flares, 2.5) * 0.4 + limb_line, 0, 1) * (1 - sstep(0.82, 1.0, r))
    save(rgba(intensity, np.clip(intensity * 1.1, 0, 1) * hole), "curse_corona")


def tex_ring(S=256):
    rng = np.random.default_rng(1611)
    c = S / 2.0
    R = c - 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xx - c, yy - c) / R
    th = np.arctan2(yy - c, xx - c)
    r0 = 0.80
    line = np.exp(-((r - r0) / 0.011) ** 2)
    band = np.where(r < r0, np.exp(-(r0 - r) / 0.10), np.exp(-((r - r0) / 0.025) ** 2)) * 0.62
    hatch = 0.55 + 0.45 * np.clip(np.sin(th * 46 + 6 * vnoise(S, S, 8, 1620)) * 0.7 + 0.5 + (vnoise(S, S, 4, 1621) - 0.5), 0, 1)
    inner = np.exp(-((r - 0.58) / 0.006) ** 2) * 0.5 * (np.sin(th * 24) > -0.2)
    sp = Mask(S, S)
    n = 44
    for i in range(n):
        a = math.tau * (i + rng.uniform(-0.3, 0.3)) / n
        h = rng.uniform(0.04, 0.17) * (1.5 if i % 7 == 0 else 1.0)
        w = rng.uniform(0.012, 0.03)
        pts = [((r0 - 0.012) * R, a - w), ((r0 + h) * R, a), ((r0 - 0.012) * R, a + w)]
        sp.poly([(c + math.cos(t) * q, c + math.sin(t) * q) for q, t in pts])
    spikes = sp.arr()
    intensity = np.clip(line * 1.0 + band * hatch + spikes * 0.95 + inner, 0, 1) * (1 - sstep(0.96, 1.0, r))
    save(rgba(intensity, np.clip(intensity * 1.12, 0, 1)), "curse_ring")


def tex_wisp():
    W, H = 64, 128
    rng = np.random.default_rng(3301)
    m = Mask(W, H)
    m.disc((32, 30), 12.5)                                     # an elongated skull of mist
    m.poly([(21, 28), (43, 28), (40, 46), (34, 56), (30, 56), (24, 46)])
    m.disc((32, 24), 13.0)
    m.path([(32 + 4 * math.sin(i / 8.0) * (i / 70.0), 48 + i * 1.18) for i in range(0, 69)], 20, 0.8)     # the body, tapering to a point
    m.path([(24, 50), (12, 72), (18, 96), (12, 118)], 8, 0.4)  # tattered strands
    m.path([(41, 50), (54, 70), (46, 94), (53, 116)], 7, 0.4)
    m.path([(29, 54), (24, 86), (28, 120)], 5, 0.3)
    eyes = Mask(W, H)
    eyes.poly([(20, 24), (30, 28), (29, 34), (21, 32)])        # slanted, sunken eyes
    eyes.poly([(44, 24), (34, 28), (35, 34), (43, 32)])
    eyes.poly([(31, 36), (33, 36), (33.5, 40), (30.5, 40)])    # a nose notch
    eyes.path([(26, 46), (28.5, 44.5), (30, 47), (32, 44.5), (34, 47), (35.5, 44.5), (38, 46)], 1.5)   # a jagged mouth
    body, hole = m.arr(), eyes.arr()
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    n = vnoise(W, H, 8, 3301) * 0.55 + vnoise(W, H, 4, 3302) * 0.45
    fall = np.clip(1 - (yy - 34) / 94.0, 0, 1)
    ragged = sstep(0.28, 0.5, n + 0.55 * fall - 0.15 + 0.25)             # the tail is eaten away into tatters
    a = np.clip(body * (0.30 + 0.70 * fall ** 0.8) * np.where(yy > 48, ragged, 1.0), 0, 1) * (1 - hole)
    g = np.clip((0.50 + 0.5 * fall) * (0.75 + 0.45 * n) * body, 0, 1) * (1 - hole)
    edge = np.clip(blur(body, 1.4) - body * 0.8, 0, 1)
    eye_rim = np.clip(blur(hole, 1.6) - hole * 0.9, 0, 1)
    save(rgba(np.clip(g + edge * 0.35 + eye_rim * 0.9, 0, 1), np.clip(a + edge * 0.3 + eye_rim * 0.7, 0, 1)), "curse_wisp")


def tex_shards():
    S, cs = 256, 128
    mk = lambda: Image.new("L", (S * SS, S * SS), 0)
    sil, shade, lines = mk(), mk(), mk()
    ds, dh, dl = ImageDraw.Draw(sil), ImageDraw.Draw(shade), ImageDraw.Draw(lines)
    rng = np.random.default_rng(1717)

    def P(cell, x, y):
        return ((cell % 2 * cs + x) * SS, (cell // 2 * cs + y) * SS)

    def facet(cell, pts, g):
        q = [P(cell, *p) for p in pts]
        ds.polygon(q, fill=255)
        dh.polygon(q, fill=int(g))

    def line(cell, pts, w, v=255):
        dl.line([P(cell, *p) for p in pts], fill=int(v), width=max(1, int(w * SS)), joint="curve")

    def thorn(cell, x, w, h, lean, y0=126):
        tip = (x + lean, y0 - h)
        bl, br = (x - w / 2, y0), (x + w / 2, y0)
        ridge = [(x, y0), (x + lean * 0.25, y0 - h * 0.35), (x + lean * 0.6, y0 - h * 0.7), tip]
        left = [bl, (x - w * 0.42 + lean * 0.2, y0 - h * 0.4), (x - w * 0.2 + lean * 0.6, y0 - h * 0.78), tip]
        right = [br, (x + w * 0.4 + lean * 0.2, y0 - h * 0.42), (x + w * 0.18 + lean * 0.6, y0 - h * 0.78), tip]
        facet(cell, left + ridge[::-1][1:-1], 46)
        facet(cell, right + ridge[::-1][1:-1], 84)
        line(cell, ridge, 1.7, 235)
        line(cell, right, 1.2, 150)
        for f in (0.22, 0.45, 0.68):                           # fracture glints across the thorn
            yy = y0 - h * f
            xr = x + lean * f * 0.7
            line(cell, [(xr - w * 0.32 * (1 - f), yy + 3), (xr - w * 0.1, yy - 2), (xr + w * 0.12, yy + 2), (xr + w * 0.34 * (1 - f), yy - 3)], 1.1, 200)

    thorn(0, 62, 36, 120, 8)
    thorn(1, 34, 24, 84, -6)
    thorn(1, 66, 34, 118, 4)
    thorn(1, 98, 22, 70, 10)
    left = [(66, 6), (52, 38), (40, 70), (46, 92), (38, 100), (44, 122), (64, 122)]                # a jagged dagger of obsidian
    right = [(66, 6), (78, 40), (86, 70), (82, 76), (92, 112), (80, 122), (64, 122)]
    facet(2, left, 46)
    facet(2, right, 92)
    line(2, [(66, 6), (65, 60), (64, 122)], 1.7, 245)
    line(2, left[:-1], 1.4, 215)
    line(2, right[:-1], 1.4, 215)
    line(2, jag(rng, (56, 100), (64, 70), 4, 3), 1.1, 190)
    line(2, jag(rng, (74, 60), (66, 36), 3, 2.5), 1.0, 170)
    spine = [(34 + 38 * math.sin(s * 1.5) * 0.0 + s * 60, 122 - 98 * math.sin(s * 1.45) ** 1.1) for s in np.linspace(0, 1, 18)]   # hooked barb
    hook = [(x + 0, y) for x, y in spine]
    for i in range(len(hook) - 1):
        t = i / (len(hook) - 1)
        wd = 20 * (1 - t) ** 0.8 + 1.0
        (x0, y0), (x1, y1) = hook[i], hook[i + 1]
        L = math.hypot(x1 - x0, y1 - y0) or 1
        nx, ny = -(y1 - y0) / L, (x1 - x0) / L
        facet(3, [(x0, y0), (x1, y1), (x1 + nx * wd * 0.5, y1 + ny * wd * 0.5), (x0 + nx * wd * 0.6, y0 + ny * wd * 0.6)], 84)
        facet(3, [(x0, y0), (x1, y1), (x1 - nx * wd * 0.5, y1 - ny * wd * 0.5), (x0 - nx * wd * 0.6, y0 - ny * wd * 0.6)], 44)
    line(3, hook, 1.6, 240)
    sa = np.asarray(sil.resize((S, S), Image.LANCZOS), np.float32) / 255.0
    sh = np.asarray(shade.resize((S, S), Image.LANCZOS), np.float32) / 255.0
    la = np.asarray(lines.resize((S, S), Image.LANCZOS), np.float32) / 255.0
    h1, h2 = blur(la, 1.3), blur(sa, 3.0)
    grey = np.clip(sh * 0.75 * sa + la + 0.5 * h1, 0, 1)
    alpha = np.maximum.reduce([sa, h1 * 1.1, h2 * 0.35])
    save(rgba(grey, alpha), "curse_shards")


def tex_spiral(S=256):
    """The Mark of Megicula's Curse as the anime draws it: one thick soft-edged spiral arm (a rose-like coil of about two turns),
    its edge fuzzy like ink bled into skin, its inside speckled with lighter flecks. ALPHA = the arm, GREY = the flecks."""
    c = S / 2.0
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    dx, dy = xx - c, yy - c
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    wob = 1.0 + 0.07 * np.sin(th * 3 + 0.6) + 0.04 * np.sin(th * 5 + 2.0)
    turns, r0, r1 = 2.15, 0.07 * S, 0.37 * S
    # distance (in radius) from the nearest spiral arm: r = r0 + (r1-r0) * (th + 2*pi*k) / (2*pi*turns)
    pitch = (r1 - r0) / turns
    ph = (r / wob - r0) / pitch - th / math.tau
    d = np.abs(ph - np.round(ph)) * pitch                      # distance across the arm, in px
    k = np.round(ph)
    inside = (r / wob > r0 * 0.4) & (k >= 0) & (k < turns + 0.35)
    wid = pitch * (0.30 + 0.05 * fbm(S, S, 48, 11, 3))
    fuzz = (fbm(S, S, 6, 21, 4) - 0.5) * pitch * 0.34
    arm = sstep(wid + pitch * 0.12, wid - pitch * 0.06, d + fuzz) * inside
    cap = sstep(r0 * 0.3, r0 * 1.0, r)                          # round the inner tip
    arm = arm * np.where(k == 0, cap, 1.0)
    alpha = np.clip(blur(arm.astype(np.float32), 2.2) * 1.15, 0, 1)
    flecks = sstep(0.56, 0.78, fbm(S, S, 5, 33, 4)) * 0.8 + sstep(0.62, 0.9, fbm(S, S, 12, 44, 3)) * 0.5
    core = sstep(wid * 0.95, 0.0, d + fuzz * 0.5) * 0.45 + 0.25
    grey = np.clip(blur(arm.astype(np.float32), 3.0) * (0.22 + flecks * core * 0.9), 0, 1)
    save(rgba(grey, alpha), "curse_spiral")


ALL = {"glyphs": tex_glyphs, "band": tex_band, "sigil": tex_sigil, "brand": tex_brand, "veins": tex_veins, "wall": tex_wall,
       "bolt": tex_bolt, "tendril": tex_tendril, "smoke": tex_smoke, "flakes": tex_flakes, "flash": tex_flash, "ring": tex_ring,
       "wisp": tex_wisp, "shards": tex_shards, "corona": tex_corona, "spiral": tex_spiral}

if __name__ == "__main__":
    names = [a.replace("curse_", "") for a in sys.argv[1:]] or list(ALL)
    for n in names:
        ALL[n]()

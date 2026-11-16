"""Skill icons v3, the painter (Pillow + numpy only, deterministic: the same inputs give the same bytes).

House style (docs/skill_icons_study.md): the icons are painted at 128 px (4 x the in-game 32 px) and brought down to 32 px, so the symbol is shaded and has
clean edges instead of flat blocks: a lit bevel (light from the top left, warm lights, cool shadows), a dark outline, a coloured rim glow, a soft aura behind
the symbol and a dark vignetted background. The frame is then drawn pixel exact on the 32 px grid, its colour says the class of the skill (Tensura's own
convention: Unique cyan, Extra gold; Ultimate royal gold with jewels and a violet inner line).

A symbol is a function  symbol(c)  that adds parts to a Ctx in a 0..100 unit space (x right, y down, centre 50,50). Parts are shaded in order; the union of the
shaded parts gets the outline. Animated icons (Ultimates) are the same picture with a pulsing aura, a glint that sweeps over the symbol, two orbiting sparkles and a
shimmer that circles the frame; every effect is periodic, so frame 0 and frame N look like neighbours and the loop is seamless.
"""
import math

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

S = 128            # working canvas
SS = 2             # supersample of the masks
OUT = 32           # in-game icon size
U = S / 100.0      # one unit in pixels


# ------------------------------------------------------------------------------------------------ colour helpers
def rgb(c):
    if isinstance(c, str):
        c = c.lstrip("#")
        return np.array([int(c[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)
    if isinstance(c, np.ndarray):
        return c.astype(np.float32)
    return np.array(c[:3], dtype=np.float32)


def mix(a, b, t):
    return rgb(a) * (1 - t) + rgb(b) * t


def lighten(c, t):
    return mix(c, (255, 244, 220), t)


def darken(c, t):
    return mix(c, (14, 8, 30), t)


# ------------------------------------------------------------------------------------------------ masks
def _arr(im):
    return np.asarray(im, dtype=np.float32) / 255.0


def _blank():
    return Image.new("L", (S * SS, S * SS), 0)


def _fin(im):
    return _arr(im.resize((S, S), Image.BOX))


def P(pts):
    return [(x * U * SS, y * U * SS) for x, y in pts]


def blur(m, r):
    if r <= 0:
        return m
    im = Image.fromarray((np.clip(m, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return _arr(im)


def soften(m, r=1.2):
    """Rounds corners (blur then a sharp threshold), keeps the area."""
    b = blur(m, r)
    return np.clip((b - 0.5) * 6.0 + 0.5, 0, 1)


def dilate(m, r):
    """Round dilation by r pixels."""
    b = blur(m, r * 0.7)
    return np.clip((b - 0.06) * 8.0, 0, 1)


def shift(m, dx, dy):
    return np.roll(np.roll(m, int(round(dy)), axis=0), int(round(dx)), axis=1)


def poly(pts, mirror=False, round_=1.0):
    im = _blank()
    d = ImageDraw.Draw(im)
    d.polygon(P(pts), fill=255)
    if mirror:
        d.polygon(P([(100 - x, y) for x, y in pts]), fill=255)
    m = _fin(im)
    return soften(m, round_) if round_ else m


def circle(cx, cy, r, ry=None):
    ry = r if ry is None else ry
    im = _blank()
    ImageDraw.Draw(im).ellipse([(cx - r) * U * SS, (cy - ry) * U * SS, (cx + r) * U * SS, (cy + ry) * U * SS], fill=255)
    return _fin(im)


def ring(cx, cy, r, w, a0=None, a1=None):
    """A ring of outer radius r and thickness w; a0..a1 (degrees, 0 = right, clockwise) make it an arc."""
    im = _blank()
    d = ImageDraw.Draw(im)
    box = [(cx - r) * U * SS, (cy - r) * U * SS, (cx + r) * U * SS, (cy + r) * U * SS]
    if a0 is None:
        d.ellipse(box, fill=255)
        r2 = r - w
        d.ellipse([(cx - r2) * U * SS, (cy - r2) * U * SS, (cx + r2) * U * SS, (cy + r2) * U * SS], fill=0)
    else:
        d.arc(box, a0, a1, fill=255, width=max(1, int(w * U * SS)))
    return _fin(im)


def line(pts, w, caps=True):
    im = _blank()
    d = ImageDraw.Draw(im)
    wp = max(1, int(w * U * SS))
    pp = P(pts)
    d.line(pp, fill=255, width=wp, joint="curve")
    if caps:
        for x, y in (pp[0], pp[-1]):
            d.ellipse([x - wp / 2, y - wp / 2, x + wp / 2, y + wp / 2], fill=255)
    return _fin(im)


def taper(p0, p1, w0, w1, curve=0.0, n=14):
    """A stroke that goes from width w0 at p0 to w1 at p1, bent sideways by curve (units)."""
    (x0, y0), (x1, y1) = p0, p1
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L, dx / L
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        bend = curve * math.sin(math.pi * t)
        cx, cy = x0 + dx * t + nx * bend, y0 + dy * t + ny * bend
        # tangent
        t2 = min(1.0, t + 0.02)
        bend2 = curve * math.sin(math.pi * t2)
        tx, ty = (x0 + dx * t2 + nx * bend2) - cx, (y0 + dy * t2 + ny * bend2) - cy
        tl = math.hypot(tx, ty) or 1.0
        px, py = -ty / tl, tx / tl
        w = (w0 + (w1 - w0) * t) / 2
        left.append((cx + px * w, cy + py * w))
        right.append((cx - px * w, cy - py * w))
    return poly(left + right[::-1], round_=0.8)


def spline(pts, n=10):
    """Catmull-Rom through pts (list of (x, y)) -> a denser list."""
    if len(pts) < 3:
        return list(pts)
    p = [pts[0]] + list(pts) + [pts[-1]]
    out = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for k in range(n):
            t = k / n
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2
                                    + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
    out.append(pts[-1])
    return out


def star(cx, cy, r_out, r_in, n=4, rot=-90):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        r = r_out if i % 2 == 0 else r_in
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return poly(pts, round_=0.0)


def grid():
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    return xx, yy


# ------------------------------------------------------------------------------------------------ shading
_L = np.array([-0.55, -0.62, 0.56], dtype=np.float32)
_L /= np.linalg.norm(_L)


def shade_rgb(mask, base, bevel=3.2, spec=0.35, grad=None, ambient=0.28, k=1.0):
    """Colour of a bevelled, lit surface for the given mask (the alpha is the mask itself)."""
    h = blur(mask, bevel * (S / 128.0))
    gy, gx = np.gradient(h)
    kk = bevel * 2.6 * k
    nx, ny = -gx * kk, -gy * kk
    nz = np.ones_like(nx)
    ln = np.sqrt(nx * nx + ny * ny + nz * nz)
    lam = np.clip((nx * _L[0] + ny * _L[1] + nz * _L[2]) / ln, 0, 1)
    t = np.clip(ambient + (1 - ambient) * lam * 1.15, 0, 1)
    yy, xx = grid()
    # a soft vertical light fall-off: the top of the symbol is brighter
    t = np.clip(t + 0.12 * (0.5 - yy / S), 0, 1)
    if grad is not None:
        c0, c1 = rgb(grad[0]), rgb(grad[1])
        v = np.clip((yy - S * 0.15) / (S * 0.7), 0, 1)[..., None]
        base_img = c0 * (1 - v) + c1 * v
    else:
        base_img = np.broadcast_to(rgb(base), (S, S, 3))
    dark = darken(base_img, 0.55) * np.array([0.9, 0.9, 1.1], dtype=np.float32)
    light = lighten(base_img, 0.45)
    t3 = t[..., None]
    lo = np.clip(t3 * 2.0, 0, 1)
    hi = np.clip(t3 * 2.0 - 1.0, 0, 1)
    col = dark * (1 - lo) + base_img * lo
    col = col * (1 - hi) + light * hi
    if spec > 0:
        sp = np.clip(lam - 0.86, 0, 1) / 0.14
        col = col + sp[..., None] * spec * (np.array([255, 250, 235], dtype=np.float32) - col)
    return np.clip(col, 0, 255)


class Ctx:
    """The canvas of one icon; symbols call part / flat / glow / add on it."""

    def __init__(self, pal):
        self.pal = pal           # dict: a (main), b (second), c (accent), glow, bg0, bg1
        self.items = []

    # a shaded part
    def part(self, mask, color=None, bevel=3.2, spec=0.35, grad=None, outline=True, k=1.0):
        self.items.append(("part", mask, color if color is not None else self.pal["a"], bevel, spec, grad, outline, k))

    # a flat tint of the mask over what is below (details: eyes, lines, runes)
    def flat(self, mask, color, alpha=1.0, outline=False):
        self.items.append(("flat", mask, color, alpha, outline))

    # an additive light (glows, flames' cores, eyes)
    def add(self, mask, color, alpha=1.0):
        self.items.append(("add", mask, color, alpha))

    # a flat tint inside the previous shaded parts only
    def cut(self, mask, color=(10, 6, 20), alpha=0.85):
        self.items.append(("cut", mask, color, alpha))

    def dot(self, x, y, r, color, alpha=1.0, glow=True):
        m = circle(x, y, r)
        self.flat(m, color, alpha)
        if glow:
            self.add(blur(m, r * U * 0.9), color, 0.55)


def _over(canvas, col, m):
    """canvas = [rgb(S,S,3), alpha(S,S)] premultiplied-free; col is (S,S,3) or (3,), m the coverage."""
    rgbc, al = canvas
    m3 = m[..., None]
    rgbc[:] = rgbc * (1 - m3) + col * m3
    al[:] = al + m * (1 - al)


def bg_layers(pal, seed):
    """Dark vignetted background with a lit centre and a few faint rays."""
    yy, xx = grid()
    cx, cy = S * 0.5, S * 0.46
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (S * 0.62)
    t = np.clip(1 - d, 0, 1) ** 1.4
    c0, c1 = rgb(pal["bg0"]), rgb(pal["bg1"])
    img = c1 + (c0 - c1) * t[..., None]
    # faint rays
    ang = np.arctan2(yy - cy, xx - cx)
    rays = 0.5 + 0.5 * np.cos(ang * 7 + seed * 0.7)
    img = img * (1 + 0.07 * rays[..., None] * t[..., None])
    # corner darkening
    v = np.clip(d - 0.55, 0, 1)
    img = img * (1 - 0.35 * v[..., None])
    # low amplitude deterministic grain, in 4 px blocks so it survives the downscale as texture
    rng = np.random.RandomState(seed & 0x7FFFFFFF)
    g = rng.rand(S // 4, S // 4).astype(np.float32)
    g = np.kron(g, np.ones((4, 4), dtype=np.float32))
    img = img * (1 + 0.05 * (g[..., None] - 0.5))
    return np.clip(img, 0, 255)


def paint(items, pal, bgimg, aura=0.5, glint=None, sparkles=(), shimmer_outline=True):
    """Composite one frame at 128 px. glint = (position, width) of a sweeping band (or None); sparkles = [(x, y, r)] in units."""
    canvas = [bgimg.copy(), np.ones((S, S), dtype=np.float32)]
    # union of the outlined parts
    union = np.zeros((S, S), dtype=np.float32)
    for it in items:
        if it[0] == "part" and it[6]:
            union = np.maximum(union, it[1])
        elif it[0] == "flat" and it[4]:
            union = np.maximum(union, it[1])
    # aura behind the symbol
    glow = rgb(pal["glow"])
    ab = blur(union, 11)
    ab2 = blur(union, 26)
    yy, xx = grid()
    rad = np.clip(1 - np.sqrt((xx - S / 2) ** 2 + (yy - S / 2) ** 2) / (S * 0.5), 0, 1) ** 1.6
    _over(canvas, np.broadcast_to(glow, (S, S, 3)), np.clip((ab * 0.55 + ab2 * 0.6 + rad * 0.25) * aura, 0, 0.85))
    # rim glow + dark outline
    rim = np.clip(dilate(union, 3.6) - dilate(union, 1.6), 0, 1)
    _over(canvas, np.broadcast_to(lighten(glow, 0.1), (S, S, 3)), rim * 0.25 * aura)
    ol = dilate(union, 1.9)
    _over(canvas, np.broadcast_to(darken(pal["bg1"], 0.7), (S, S, 3)), ol * 0.96)
    # drop shadow below the symbol for depth
    sh = shift(blur(union, 2.5), 2, 3)
    canvas[0][:] = canvas[0] * (1 - 0.28 * sh[..., None] * (1 - union[..., None]))
    # parts in order, each casting a soft shadow on what is under it
    under = np.zeros((S, S), dtype=np.float32)
    for it in items:
        kind = it[0]
        if kind == "part":
            _, m, col, bevel, spec, grad, _o, k = it
            if under.max() > 0:
                cast = shift(blur(m, 2.0), 2, 2) * under
                canvas[0][:] = canvas[0] * (1 - 0.40 * cast[..., None])
            sc = shade_rgb(m, col, bevel, spec, grad, k=k)
            _over(canvas, sc, m)
            if _o:
                # a thin dark rim inside the part separates it from the parts it overlaps (a cartoon line, like Tensura's outlined sprites)
                inner_edge = m * (1 - np.clip(1 - dilate(1 - m, 1.3), 0, 1))
                _over(canvas, darken(sc, 0.62), inner_edge * 0.55)
            under = np.maximum(under, m)
        elif kind == "flat":
            _, m, col, alpha, _o = it
            _over(canvas, rgb(col), m * alpha)
        elif kind == "add":
            _, m, col, alpha = it
            canvas[0][:] = np.clip(canvas[0] + rgb(col) * m[..., None] * alpha, 0, 255)
        elif kind == "cut":
            _, m, col, alpha = it
            _over(canvas, rgb(col), m * under * alpha)
    # glint sweeping across the symbol
    if glint is not None:
        pos, wid = glint
        u = (xx * 0.8 + yy * 0.6) / S * 100.0
        band = np.clip(1 - np.abs(u - pos) / wid, 0, 1) ** 1.5
        canvas[0][:] = np.clip(canvas[0] + band[..., None] * union[..., None] * np.array([255, 245, 225], dtype=np.float32) * 0.55, 0, 255)
    for (x, y, r) in sparkles:
        st = star(x, y, r, r * 0.22, 4)
        sg = blur(st, 2.0)
        canvas[0][:] = np.clip(canvas[0] + sg[..., None] * rgb(lighten(glow, 0.7)) * 0.7 + st[..., None] * 90, 0, 255)
    return canvas[0]


def to_32(img128):
    """128 -> 32, a box filter (keeps the outline thin and clean) plus a light unsharp."""
    im = Image.fromarray(np.clip(img128, 0, 255).astype(np.uint8), "RGB").resize((OUT, OUT), Image.BOX)
    return im.filter(ImageFilter.UnsharpMask(radius=0.7, percent=55, threshold=2))


# ------------------------------------------------------------------------------------------------ frames (pixel exact, 32 px)
FRAMES = {
    # outer, light, mid, dark, inner  (the ring is 2 px: its first px light, its second px mid; the lower right edge dark)
    "unique": dict(outer=(6, 22, 34), light=(150, 246, 240), mid=(32, 176, 190), dark=(12, 96, 120), inner=(6, 40, 56), corner=(235, 255, 252)),
    "extra": dict(outer=(40, 18, 4), light=(255, 226, 120), mid=(236, 160, 40), dark=(160, 88, 16), inner=(52, 28, 6), corner=(255, 246, 200)),
    "ultimate": dict(outer=(26, 8, 40), light=(255, 236, 150), mid=(226, 170, 60), dark=(128, 64, 140), inner=(60, 16, 86), corner=(255, 255, 255)),
}


def frame_mask():
    """Which pixels of the 32 px canvas belong to the frame (the 3 px border)."""
    yy, xx = np.mgrid[0:OUT, 0:OUT]
    e = np.minimum(np.minimum(xx, OUT - 1 - xx), np.minimum(yy, OUT - 1 - yy))
    return e


def draw_frame(img, cls, accent, shimmer=None):
    """Draws the class frame onto the 32 px RGB image (numpy float array HxWx3) and returns RGBA uint8. shimmer = position 0..1 round the ring or None."""
    f = FRAMES[cls]
    out = np.zeros((OUT, OUT, 4), dtype=np.float32)
    out[..., :3] = img
    out[..., 3] = 255
    e = frame_mask()
    yy, xx = np.mgrid[0:OUT, 0:OUT]
    # rounded corners: the four corner pixels are transparent, like Tensura's icons
    corner = ((xx == 0) | (xx == OUT - 1)) & ((yy == 0) | (yy == OUT - 1))
    # inner shadow: the first inner ring of art is darkened
    inner = e == 3
    out[inner, :3] = out[inner, :3] * 0.72 + rgb(f["inner"]) * 0.28
    ring1 = e == 1
    ring2 = e == 2
    topleft = (yy <= xx) & (yy + xx <= OUT - 1)       # top triangle ... refine below
    lit = (xx + yy) < (OUT - 1)                       # upper-left half of the diagonal
    out[e == 0, :3] = rgb(f["outer"])
    for m, key_lit, key_dark in ((ring1, "light", "mid"), (ring2, "mid", "dark")):
        c_lit, c_dark = rgb(f[key_lit]), rgb(f[key_dark])
        sel = m & lit
        out[sel, :3] = c_lit
        sel = m & ~lit
        out[sel, :3] = c_dark
    if cls == "ultimate":
        # violet inner line, accent jewels in the four corners
        out[ring2, :3] = np.where(lit[ring2][:, None], rgb(f["mid"]) * 0.6 + rgb(f["inner"]) * 0.4, rgb(f["inner"]))
        acc = rgb(accent)
        for (cx, cy) in ((1, 1), (OUT - 2, 1), (1, OUT - 2), (OUT - 2, OUT - 2)):
            out[cy, cx, :3] = lighten(acc, 0.5)
            for (ox, oy) in ((1, 0), (0, 1)):
                px, py = cx + (ox if cx < 16 else -ox), cy + (oy if cy < 16 else -oy)
                out[py, px, :3] = acc
    # a bright pixel at each corner of the ring (the stud)
    for (cx, cy) in ((1, 1), (OUT - 2, 1), (1, OUT - 2), (OUT - 2, OUT - 2)):
        if cls != "ultimate":
            out[cy, cx, :3] = rgb(f["corner"])
    if shimmer is not None:
        ang = (np.arctan2(yy - (OUT - 1) / 2, xx - (OUT - 1) / 2) / (2 * math.pi)) % 1.0
        dd = np.abs(ang - shimmer)
        dd = np.minimum(dd, 1 - dd)
        boost = np.clip(1 - dd / 0.10, 0, 1) ** 1.4
        sel = (e == 1) | (e == 2)
        out[sel, :3] = out[sel, :3] + (255 - out[sel, :3]) * (boost[sel][:, None] * 0.75)
    out[corner, 3] = 0
    return np.clip(out, 0, 255).astype(np.uint8)


def render(symbol, pal, cls, seed, t=None, animate=False):
    """One 32 px RGBA frame. t is the loop phase in [0, 1) (only used when animate)."""
    c = Ctx(pal)
    symbol(c)
    bgimg = bg_layers(pal, seed)
    aura, glint, sparkles, shimmer = 0.55, None, (), None
    if animate and t is not None:
        w = 2 * math.pi * t
        aura = 0.55 + 0.22 * math.sin(w)
        pos = -30.0 + 175.0 * t              # leaves the canvas on both ends -> seamless
        glint = (pos, 15.0)
        sparkles = []
        for i, (ph, rad) in enumerate(((0.0, 41.0), (0.5, 34.0))):
            a = w + ph * 2 * math.pi + i
            r = 6.0 + 2.5 * math.sin(2 * w + i * 2)
            sparkles.append((50 + rad * math.cos(a) * 1.0, 48 + rad * math.sin(a) * 0.9, r))
        shimmer = t
    img = paint(c.items, pal, bgimg, aura, glint, sparkles)
    small = np.asarray(to_32(img), dtype=np.float32)
    return Image.fromarray(draw_frame(small, cls, pal["glow"], shimmer), "RGBA")

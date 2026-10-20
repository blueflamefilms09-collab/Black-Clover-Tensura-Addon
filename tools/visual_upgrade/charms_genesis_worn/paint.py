"""paint: hand-authored-looking details painted onto the skin and the glow map of a visual upgrade model, after the UV layout.

Each `deco_*` runs over the UV groups of one part (a group is a rectangle of the skin: an unrolled loft, or one face direction) through mesh_lib's Atlas.paint_in, so the
detail follows the form: engraved rune bands round a collar, filigree scrolls on a plate, an enamel inlay, glowing veins in a gem, a lit core. The skin is float32 RGBA
0..255 (rows run down), the glow map is float32 RGBA 0..255 where the alpha is the light intensity.
"""
import math
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

_FONT = {}


def font(size):
    f = _FONT.get(size)
    if f is None:
        try:
            f = ImageFont.load_default(size=size)
        except Exception:
            f = ImageFont.load_default()
        _FONT[size] = f
    return f


def rgb_of(c):
    if isinstance(c, str):
        c = int(c, 16)
    if isinstance(c, int):
        return ((c >> 16) & 255, (c >> 8) & 255, c & 255)
    return tuple(int(x) for x in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _mask(w, h, draw_fn, scale=3, blur=0.5):
    """An anti-aliased float mask (h, w) 0..1: draw_fn(ImageDraw, S) draws on a canvas S times larger."""
    im = Image.new("L", (w * scale, h * scale), 0)
    draw_fn(ImageDraw.Draw(im), scale)
    im = im.resize((w, h), Image.LANCZOS)
    if blur > 0:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im, np.float32) / 255.0


def _apply_engrave(img, mask, dark=0.55, rim=0.18):
    """Darkens the skin where the mask is (an engraved groove) and lights a thin rim beside it."""
    m = mask[..., None]
    img[..., :3] *= 1 - dark * m
    shifted = np.roll(np.roll(mask, 1, axis=0), 1, axis=1)
    edge = np.clip(shifted - mask, 0, 1)[..., None]
    img[..., :3] += 255 * rim * edge * 0.8


def _put_glow(glow, mask, rgb, strength=1.0, halo=1.0):
    m = mask
    if halo > 0:
        hl = np.asarray(Image.fromarray((np.clip(m, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(halo)), np.float32) / 255.0
        a = np.clip(np.maximum(m, hl * 0.75) * strength, 0, 1)
    else:
        a = np.clip(m * strength, 0, 1)
    core = np.array(rgb, np.float32)
    white = core + (255 - core) * 0.35 * np.clip(m, 0, 1)[..., None]
    new = np.concatenate([white, (a * 255)[..., None]], axis=2)
    use = (new[..., 3] > glow[..., 3])[..., None]
    glow[...] = np.where(use, new, glow)


def groups_of(part, min_w=8, min_h=4):
    return [g for g in getattr(part, "groups", []) if g.rect is not None and g.rect[2] >= min_w and g.rect[3] >= min_h]


# ------------------------------------------------------------------ glyphs
_STROKES = [
    ((0.5, 0.05), (0.5, 0.95)), ((0.1, 0.5), (0.9, 0.5)), ((0.15, 0.1), (0.85, 0.9)), ((0.85, 0.1), (0.15, 0.9)), ((0.2, 0.1), (0.5, 0.9)), ((0.8, 0.1), (0.5, 0.9)),
    ((0.2, 0.1), (0.8, 0.1)), ((0.2, 0.9), (0.8, 0.9)), ((0.2, 0.1), (0.2, 0.9)), ((0.8, 0.1), (0.8, 0.9)), ((0.5, 0.05), (0.15, 0.5)), ((0.5, 0.05), (0.85, 0.5)),
    ((0.15, 0.5), (0.5, 0.95)), ((0.85, 0.5), (0.5, 0.95)),
]


def _glyph(draw, x0, y0, gw, gh, rng, width, S):
    kind = int(rng.integers(0, 9))
    if kind == 8:                                           # a small ring with a dot
        r = min(gw, gh) * 0.32
        cx, cy = x0 + gw / 2, y0 + gh / 2
        draw.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=255, width=max(1, int(width * S)))
        draw.ellipse([(cx - width) * S, (cy - width) * S, (cx + width) * S, (cy + width) * S], fill=255)
        return
    n = int(rng.integers(2, 5))
    idx = rng.choice(len(_STROKES), size=n, replace=False)
    for i in idx:
        (ax, ay), (bx, by) = _STROKES[int(i)]
        draw.line([(x0 + ax * gw) * S, (y0 + ay * gh) * S, (x0 + bx * gw) * S, (y0 + by * gh) * S], fill=255, width=max(1, int(width * S)))


def deco_glyphs(vm, part, rgb, text=None, rows=1, strength=1.0, seed=0, glow=True, engrave=0.5, min_w=14, min_h=6, vertical=False, density=1.0):
    """Rune / lettering bands. Every large enough group of the part gets `rows` rows of glyphs (real letters of `text` now and then), engraved in the skin and,
    with `glow`, lit in the glow map. vertical: the rows run down the group (a band whose length is the texture's y)."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 977 + grp.rect[0] * 31 + grp.rect[1]) & 0xFFFFFFFF)
        if vertical:
            cw, ch = h, w
        else:
            cw, ch = w, h
        rowh = max(5, int(ch / max(1, rows)))
        gh = max(4.0, rowh * 0.72)
        gw = gh * 0.78
        lw = max(0.6, gh / 9.0)

        def drawer(d, S):
            for r in range(rows):
                y0 = r * rowh + (rowh - gh) / 2
                x = 1.0
                k = 0
                while x + gw < cw - 1:
                    if rng.random() < density:
                        if text and rng.random() < 0.28:
                            ch_ = text[int(rng.integers(0, len(text)))]
                            if ch_.strip():
                                d.text((x * S, (y0 - 1) * S), ch_.upper(), fill=255, font=font(int(gh * S * 0.95)))
                        else:
                            _glyph(d, x, y0, gw, gh, rng, lw, S)
                    x += gw * 1.35
                    k += 1
                if rowh >= 8:
                    d.line([0, (y0 - 1) * S, cw * S, (y0 - 1) * S], fill=255, width=max(1, int(0.7 * S)))
                    d.line([0, (y0 + gh + 1) * S, cw * S, (y0 + gh + 1) * S], fill=255, width=max(1, int(0.7 * S)))

        m = _mask(cw, ch, drawer)
        if vertical:
            m = m.T
        _apply_engrave(img, m, dark=engrave)
        if glow:
            _put_glow(gl, m, rgb, strength, halo=0.9)

    for g in groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_fill_glow(vm, part, rgb, strength=1.0, core=0.35, tint_skin=0.55, facets=True, seed=0):
    """An emissive part (a gem, a core): the skin takes the colour (a faceted crystal look) and the glow map lights every texel, brightest toward the middle of each rectangle."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed + grp.rect[0] * 13) & 0xFFFFFFFF)
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        cx, cy = (w - 1) / 2, (h - 1) / 2
        rad = np.clip(np.hypot((xx - cx) / max(cx, 1), (yy - cy) / max(cy, 1)) / 1.2, 0, 1)
        fac = np.ones((h, w), np.float32)
        if facets:
            ang = rng.uniform(0, math.pi)
            d = (xx * math.cos(ang) + yy * math.sin(ang)) / max(w + h, 1) * 9.0
            fac = 0.68 + 0.32 * (np.floor(d) % 2) + 0.12 * np.sin(d * math.pi)
        base = np.array(rgb, np.float32)
        col = base[None, None, :] * (0.5 + 0.7 * fac[..., None] * (1 - 0.4 * rad[..., None]))
        img[..., :3] = img[..., :3] * (1 - tint_skin) + np.clip(col, 0, 255) * tint_skin
        a = np.clip((1.0 - 0.55 * rad) * fac * strength, 0, 1)
        white = base + (255 - base) * core * (1 - rad[..., None])
        new = np.concatenate([white, (a * 255)[..., None]], axis=2)
        gl[...] = np.where((new[..., 3] > gl[..., 3])[..., None], new, gl)

    for g in groups_of(part, 1, 1):
        vm.atlas.paint_in(g, run)


def deco_veins(vm, part, rgb, n=5, strength=0.9, seed=0, width=1.1, min_w=10, min_h=10, dark=0.35):
    """Branching energy veins: random walks from the rectangle's edge toward the middle, lit in the glow map and cut into the skin."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 131 + grp.rect[0] * 7 + grp.rect[1] * 3) & 0xFFFFFFFF)

        def drawer(d, S):
            for _ in range(n):
                side = int(rng.integers(0, 4))
                if side == 0:
                    x, y = rng.uniform(0, w), 0.0
                elif side == 1:
                    x, y = rng.uniform(0, w), float(h)
                elif side == 2:
                    x, y = 0.0, rng.uniform(0, h)
                else:
                    x, y = float(w), rng.uniform(0, h)
                ang = math.atan2(h / 2 - y, w / 2 - x) + rng.uniform(-0.6, 0.6)
                steps = int(max(w, h) * 0.9)
                for s in range(steps):
                    ang += rng.uniform(-0.45, 0.45)
                    nx, ny = x + math.cos(ang) * 1.6, y + math.sin(ang) * 1.6
                    d.line([x * S, y * S, nx * S, ny * S], fill=255, width=max(1, int(width * S)))
                    x, y = nx, ny
                    if rng.random() < 0.07:                                         # a branch
                        a2 = ang + rng.choice([-1, 1]) * rng.uniform(0.6, 1.2)
                        bx, by = x, y
                        for _k in range(int(rng.integers(4, 12))):
                            a2 += rng.uniform(-0.4, 0.4)
                            nbx, nby = bx + math.cos(a2) * 1.5, by + math.sin(a2) * 1.5
                            d.line([bx * S, by * S, nbx * S, nby * S], fill=255, width=max(1, int(width * 0.7 * S)))
                            bx, by = nbx, nby
                    if not (-2 < x < w + 2 and -2 < y < h + 2):
                        break

        m = _mask(w, h, drawer)
        img[..., :3] *= 1 - dark * m[..., None]
        _put_glow(gl, m, rgb, strength, halo=1.2)

    for g in groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_filigree(vm, part, rgb, seed=0, density=1.0, min_w=12, min_h=12, engrave=0.35, lit=None):
    """Scrollwork: mirrored sine curls, dots and a border line, in the part's metal (a raised look: light rim on the dark side)."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 53 + grp.rect[0] * 5) & 0xFFFFFFFF)

        def drawer(d, S):
            d.rectangle([1.5 * S, 1.5 * S, (w - 2.5) * S, (h - 2.5) * S], outline=255, width=max(1, int(0.8 * S)))
            ncurl = int(max(2, (w + h) / 14 * density))
            for _ in range(ncurl):
                cx, cy = rng.uniform(4, w - 4), rng.uniform(4, h - 4)
                r0 = rng.uniform(1.0, 3.2)
                turn = rng.choice([-1, 1])
                a = rng.uniform(0, 6.28)
                pts = []
                for k in range(26):
                    rr = r0 * (1 + k * 0.17)
                    pts.append(((cx + math.cos(a + turn * k * 0.42) * rr) * S, (cy + math.sin(a + turn * k * 0.42) * rr) * S))
                d.line(pts, fill=255, width=max(1, int(0.8 * S)), joint="curve")
                d.ellipse([(cx - 0.8) * S, (cy - 0.8) * S, (cx + 0.8) * S, (cy + 0.8) * S], fill=255)

        m = _mask(w, h, drawer)
        _apply_engrave(img, m, dark=engrave, rim=0.25)
        if lit is not None:
            _put_glow(gl, m * 0.6, lit, 0.55, halo=0.6)

    for g in groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_enamel(vm, part, a, b, cells=3, seed=0, min_w=8, min_h=8):
    """A cloisonne inlay: the rectangle split by thin metal walls into coloured cells in two enamel colours with a glassy highlight."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 17 + grp.rect[0]) & 0xFFFFFFFF)
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        gx = max(2, int(w / max(4, w / cells)))
        gy = max(2, int(h / max(4, h / cells)))
        ci = (xx * gx / w).astype(np.int32)
        cj = (yy * gy / h).astype(np.int32)
        pick = rng.random((gy + 1, gx + 1)) > 0.5
        col = np.where(pick[cj, ci][..., None], np.array(a, np.float32), np.array(b, np.float32))
        wall = ((xx * gx / w) % 1.0 < 0.06) | ((yy * gy / h) % 1.0 < 0.06)
        spec = np.exp(-(((xx % (w / gx)) / (w / gx) - 0.3) ** 2 + ((yy % (h / gy)) / (h / gy) - 0.3) ** 2) / 0.04)[..., None] * 60
        out = np.where(wall[..., None], img[..., :3] * 0.9 + 25, np.clip(col + spec, 0, 255))
        img[..., :3] = out

    for g in groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_stars(vm, part, rgb, n=40, seed=0, strength=0.9):
    """Pin-point stars inside a part (a night-sky inlay): bright dots and a few four-point glints in the glow map and the skin."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 11 + grp.rect[0]) & 0xFFFFFFFF)
        cnt = max(3, int(n * (w * h) / 4096))

        def drawer(d, S):
            for _ in range(cnt):
                x, y = rng.uniform(0, w), rng.uniform(0, h)
                r = rng.uniform(0.35, 0.9)
                d.ellipse([(x - r) * S, (y - r) * S, (x + r) * S, (y + r) * S], fill=255)
                if rng.random() < 0.22:
                    d.line([(x - 2.2) * S, y * S, (x + 2.2) * S, y * S], fill=200, width=max(1, int(0.5 * S)))
                    d.line([x * S, (y - 2.2) * S, x * S, (y + 2.2) * S], fill=200, width=max(1, int(0.5 * S)))

        m = _mask(w, h, drawer, blur=0.3)
        img[..., :3] = img[..., :3] * (1 - 0.5 * m[..., None]) + np.array(rgb, np.float32) * 0.5 * m[..., None]
        _put_glow(gl, m, rgb, strength, halo=0.8)

    for g in groups_of(part, 6, 6):
        vm.atlas.paint_in(g, run)


def deco_tint(vm, part, rgb, amount=0.35):
    """Multiplies the part's skin toward a colour (verdigris, soot, rust ...)."""
    def run(img, gl, grp):
        img[..., :3] = img[..., :3] * (1 - amount) + np.array(rgb, np.float32) * amount * (img[..., :3].mean(axis=2, keepdims=True) / 140.0)

    for g in groups_of(part, 1, 1):
        vm.atlas.paint_in(g, run)


def deco_rim_glow(vm, part, rgb, strength=0.8, width=1.4, min_w=6, min_h=6):
    """A lit line round the edge of every rectangle (the bevel of a glowing plate)."""
    def run(img, gl, grp):
        h, w = img.shape[:2]

        def drawer(d, S):
            d.rectangle([1 * S, 1 * S, (w - 2) * S, (h - 2) * S], outline=255, width=max(1, int(width * S)))

        m = _mask(w, h, drawer)
        _put_glow(gl, m, rgb, strength, halo=1.0)

    for g in groups_of(part, min_w, min_h):
        vm.atlas.paint_in(g, run)


def deco_grain(vm, part, amount=0.12, seed=0):
    """Hammered / pitted surface: a fine bump pattern that makes a flat metal read as hand worked."""
    def run(img, gl, grp):
        h, w = img.shape[:2]
        rng = np.random.default_rng((zlib.crc32(part.name.encode()) + seed * 3 + grp.rect[0] * 3) & 0xFFFFFFFF)
        n = rng.random((max(2, h // 3 + 2), max(2, w // 3 + 2))).astype(np.float32)
        im = np.asarray(Image.fromarray((n * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), np.float32) / 255.0
        img[..., :3] *= (1 + amount * (im - 0.5) * 2)[..., None]

    for g in groups_of(part, 4, 4):
        vm.atlas.paint_in(g, run)

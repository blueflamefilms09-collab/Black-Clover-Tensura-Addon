"""Shared kit of the Recombination "anime look" generators (tools/recomb/gen_recomb_anime.py): a GeoLite model whose faces are painted one by one at a
chosen number of texels per model pixel (per-face uv, hidden faces left out), and the face painters of a town of houses turned into a golem: plastered
timber-frame walls, ashlar stone, brick, slate roofs with moss, arched windows with warm or dark panes, doors, chimneys, leaves, tower stone, glowing eyes.

    from recomb_kit import Sheet, Anim, wave, keys, S
    sh = Sheet("recombanime", "golem", tex_w=1536, seed=7)
    sh.bone("root", pivot=(0, 0, 0))
    sh.cube("root", (-10, 0, -10), (20, 12, 20), base=("brick", {}), faces={"north": ("timber", {"windows": [(10, 6, 5, 7, "lit")]})}, hide=("bottom",))
    sh.save(Anim())

File frame (tools/geo_builder.py): model pixels (1/16 block), y up, feet at the origin, front on -z, +x the model's left. Faces are painted as seen from outside, upright.
Every texture is deterministic (a seed per model and per cube); run `python3 -B tools/recomb/gen_recomb_anime.py` to redo them. Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
from geo_builder import Model, Anim, ROOT  # noqa: E402,F401
from transmutation_geo_kit import wave, keys, S, vnoise  # noqa: E402,F401

SS = 2                                    # supersampling of the shapes (box filtered down)

# ------------------------------------------------------------------------------------------------ palette
PLASTER = (224, 208, 172)
PLASTER_D = (196, 176, 140)
TIMBER = (82, 52, 34)
TIMBER_L = (124, 84, 52)
STONE = (158, 152, 142)
STONE_D = (112, 108, 102)
STONE_L = (194, 188, 174)
MORTAR = (206, 194, 172)
BRICK = (178, 88, 58)
BRICK_D = (132, 62, 44)
BRICK_L = (212, 124, 82)
SLATE = (76, 90, 112)
SLATE_L = (116, 134, 158)
SLATE_D = (46, 56, 74)
TILE = (170, 74, 52)
TILE_L = (206, 112, 76)
TILE_D = (112, 46, 38)
MOSS = (96, 142, 58)
MOSS_L = (146, 188, 84)
MOSS_D = (60, 96, 44)
LIT_TOP = (255, 226, 140)
LIT_BOT = (255, 148, 58)
DARK_TOP = (30, 42, 58)
DARK_BOT = (66, 88, 112)
EYE_CORE = (246, 255, 176)
EYE_RIM = (150, 214, 42)
IRON = (54, 56, 66)
IRON_L = (112, 116, 130)


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def shade(c, k):
    return tuple(int(max(0, min(255, c[i] * k))) for i in range(3))


# ------------------------------------------------------------------------------------------------ the canvas of one face
class Cv:
    """One face: PIL images at SS x the texel size for the colour and the glow map, with coordinates in model pixels (x right, y down, as seen)."""

    def __init__(self, w, h, K, rng):
        self.w, self.h, self.K, self.rng = w, h, K, rng
        self.Wt, self.Ht = max(1, int(math.ceil(w * K - 1e-6))), max(1, int(math.ceil(h * K - 1e-6)))
        self.im = Image.new("RGB", (self.Wt * SS, self.Ht * SS), (0, 0, 0))
        self.gl = Image.new("RGB", (self.Wt * SS, self.Ht * SS), (0, 0, 0))
        self.d = ImageDraw.Draw(self.im)
        self.g = ImageDraw.Draw(self.gl)
        self.k = K * SS

    def p(self, v):
        return v * self.k

    def rect(self, x0, y0, x1, y1, fill, glow=None):
        q = [self.p(x0), self.p(y0), max(self.p(x0), self.p(x1) - 1), max(self.p(y0), self.p(y1) - 1)]
        self.d.rectangle(q, fill=fill)
        if glow is not None:
            self.g.rectangle(q, fill=glow)

    def poly(self, pts, fill, glow=None):
        q = [(self.p(x), self.p(y)) for x, y in pts]
        self.d.polygon(q, fill=fill)
        if glow is not None:
            self.g.polygon(q, fill=glow)

    def ellipse(self, cx, cy, rx, ry, fill, glow=None):
        if rx <= 0 or ry <= 0:
            return
        b = [self.p(cx - rx), self.p(cy - ry), self.p(cx + rx), self.p(cy + ry)]
        self.d.ellipse(b, fill=fill)
        if glow is not None:
            self.g.ellipse(b, fill=glow)

    def line(self, x0, y0, x1, y1, width, fill, glow=None):
        q = [self.p(x0), self.p(y0), self.p(x1), self.p(y1)]
        self.d.line(q, fill=fill, width=max(1, int(round(self.p(width)))))
        if glow is not None:
            self.g.line(q, fill=glow, width=max(1, int(round(self.p(width)))))

    def out(self):
        im = self.im.resize((self.Wt, self.Ht), Image.BOX)
        gl = self.gl.resize((self.Wt, self.Ht), Image.BOX)
        return np.asarray(im).astype(np.float32), np.asarray(gl).astype(np.float32)


def grain(rgb, rng, K, amount=0.10, cell=2.0):
    """Tonal variation: smooth value noise (features of about 'cell' model pixels) plus a little texel noise."""
    H, W = rgb.shape[:2]
    n = vnoise(H, W, rng, max(1.0, cell * K)) * 0.7 + rng.random((H, W)).astype(np.float32) * 0.3
    return rgb * (1.0 + (n[..., None] - 0.5) * 2.0 * amount)


def finish(rgb, K, ao=0.16, top=0.10, edge=1.2):
    """Soft shading baked into the face: lighter at the top, darker at the bottom and along the edges (ambient occlusion)."""
    H, W = rgb.shape[:2]
    yy, xx = np.mgrid[0:H, 0:W]
    fy = (yy + 0.5) / H
    k = 1.0 + top * (0.5 - fy) * 2.0
    e = int(max(1, round(edge * K)))
    dist = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)).astype(np.float32)
    k = k * (1.0 - ao * np.clip(1.0 - dist / max(1, e * 2.0), 0, 1) ** 1.6)
    return rgb * k[..., None]


def moss(rgb, rng, K, where="top", amount=0.5, cell=2.6):
    """Moss growing from the top and/or bottom edge: soft green blobs with a light fringe."""
    H, W = rgb.shape[:2]
    yy = (np.mgrid[0:H, 0:W][0] + 0.5) / H
    n = vnoise(H, W, rng, max(1.0, cell * K))
    w = np.zeros((H, W), np.float32)
    if where in ("top", "both"):
        w = np.maximum(w, np.clip(1.0 - yy / 0.35, 0, 1))
    if where in ("bottom", "both"):
        w = np.maximum(w, np.clip((yy - 0.72) / 0.28, 0, 1))
    m = (n * 0.75 + w * 0.7) > (1.15 - amount * 0.6)
    m &= w > 0.02
    tone = vnoise(H, W, rng, max(1.0, 1.4 * K))
    col = np.zeros((H, W, 3), np.float32)
    for i in range(3):
        col[..., i] = np.where(tone > 0.62, MOSS_L[i], np.where(tone < 0.30, MOSS_D[i], MOSS[i]))
    out = rgb.copy()
    out[m] = col[m]
    return out


# ------------------------------------------------------------------------------------------------ parts drawn on walls
def window(cv, cx, cy, w, h, style="lit", arch=True, shutters=False, sill=True):
    """An arched window: stone surround, a pane that is warm (lit, also written to the glow map) or dark with a sheen, muntins, a sill and optional shutters. (cx, cy) is the centre."""
    x0, x1, y0, y1 = cx - w / 2, cx + w / 2, cy - h / 2, cy + h / 2
    sur = 0.9
    if shutters:
        for sx in (-1, 1):
            xs = x0 - 1.7 if sx < 0 else x1 + 0.3
            cv.rect(xs, y0 + (w / 2 if arch else 0), xs + 1.4, y1, (96, 120, 78))
            cv.line(xs + 0.7, y0 + (w / 2 if arch else 0) + 0.3, xs + 0.7, y1 - 0.3, 0.3, (70, 90, 58))
    rad = w / 2
    if arch:
        cv.rect(x0 - sur, y0 + rad, x1 + sur, y1 + sur * 0.3, STONE_L)
        cv.ellipse(cx, y0 + rad, rad + sur, rad + sur, STONE_L)
    else:
        cv.rect(x0 - sur, y0 - sur, x1 + sur, y1 + sur * 0.3, STONE_L)
    if style == "lit":
        top, bot = LIT_TOP, LIT_BOT
    else:
        top, bot = DARK_TOP, DARK_BOT
    steps = max(4, int(h * cv.K))
    for i in range(steps):
        t = i / (steps - 1)
        ya, yb = y0 + h * i / steps, y0 + h * (i + 1) / steps
        col = mix(top, bot, t)
        gcol = shade(col, 0.95) if style == "lit" else None
        if arch and ya < y0 + rad:
            dy = (y0 + rad) - (ya + yb) / 2
            hw = math.sqrt(max(0.0, rad * rad - dy * dy)) if dy > 0 else rad
            cv.rect(cx - hw, ya, cx + hw, yb + 0.05, col, gcol)
        else:
            cv.rect(x0, ya, x1, yb + 0.05, col, gcol)
    mun = (60, 40, 28) if style == "lit" else (28, 30, 38)
    cv.rect(cx - 0.18, y0 + (0.2 if arch else 0), cx + 0.18, y1, mun, (0, 0, 0))
    ym = y0 + h * 0.58
    cv.rect(x0, ym - 0.18, x1, ym + 0.18, mun, (0, 0, 0))
    if style != "lit":                                              # a sheen across a dark pane
        cv.poly([(x0 + 0.2, y1 - 0.2), (x0 + w * 0.55, y0 + h * 0.3), (x0 + w * 0.75, y0 + h * 0.3), (x0 + w * 0.4, y1 - 0.2)], (92, 120, 150))
    if sill:
        cv.rect(x0 - 1.2, y1, x1 + 1.2, y1 + 0.8, STONE_L)
        cv.rect(x0 - 1.2, y1 + 0.7, x1 + 1.2, y1 + 1.05, STONE_D)


def door(cv, cx, w, h, style="oak"):
    """An arched oak door standing on the bottom edge of the face with iron straps, a ring and a stone frame."""
    y1 = cv.h
    y0 = y1 - h
    rad = w / 2
    cv.rect(cx - w / 2 - 1.0, y0 + rad, cx + w / 2 + 1.0, y1, STONE_L)
    cv.ellipse(cx, y0 + rad, rad + 1.0, rad + 1.0, STONE_L)
    cv.rect(cx - w / 2, y0 + rad, cx + w / 2, y1, (112, 74, 44))
    cv.ellipse(cx, y0 + rad, rad, rad, (112, 74, 44))
    n = max(3, int(w / 1.4))
    for i in range(n + 1):
        x = cx - w / 2 + w * i / n
        cv.line(x, y0 + rad * 0.5, x, y1, 0.2, (72, 46, 28))
    for yy in (y0 + h * 0.38, y0 + h * 0.76):
        cv.rect(cx - w / 2, yy - 0.35, cx + w / 2, yy + 0.35, IRON)
        cv.rect(cx - w / 2, yy - 0.35, cx + w / 2, yy - 0.15, IRON_L)
    cv.ellipse(cx + w * 0.22, y0 + h * 0.58, 0.55, 0.55, (214, 176, 70), (80, 52, 12))
    cv.ellipse(cx + w * 0.22, y0 + h * 0.58, 0.28, 0.28, (80, 52, 20))


# ------------------------------------------------------------------------------------------------ the face painters (kind, options)
def _noise_fill(cv, base, rng, amount=0.08):
    arr = np.zeros((cv.Ht, cv.Wt, 3), np.float32)
    arr[:] = np.array(base, np.float32)
    return grain(arr, rng, cv.K, amount, 3.0)


def paint_timber(w, h, K, rng, o):
    """A plastered wall in a timber frame: posts, rails, braces, then windows [(cx, cy, w, h, style)] and a door (cx, w, h)."""
    cv = Cv(w, h, K, rng)
    plaster = o.get("plaster", PLASTER)
    cv.rect(0, 0, w, h, plaster)
    t = o.get("beam", 1.5)
    bay = o.get("bay", 13.0)
    nb = max(1, int(round(w / bay)))
    xs = [w * i / nb for i in range(nb + 1)]
    rails = o.get("rails", [h * 0.5] if h > 14 else [])
    for i in range(nb):                                              # braces in bays that hold no window
        if not o.get("braces", True):
            break
        bx0, bx1 = xs[i], xs[i + 1]
        busy = any(abs(wx - (bx0 + bx1) / 2) < (bx1 - bx0) / 2 for (wx, wy, ww, wh, ws) in o.get("windows", []))
        if busy or (bx1 - bx0) < 6:
            continue
        ys = [0] + list(rails) + [h]
        for j in range(len(ys) - 1):
            ya, yb = ys[j], ys[j + 1]
            if yb - ya < 5:
                continue
            flip = (i + j) % 2
            xa, xb = (bx0, bx1) if not flip else (bx1, bx0)
            cv.line(xa, ya, xb, yb, t * 0.62, TIMBER)
    for x in xs:
        cv.rect(x - t / 2, 0, x + t / 2, h, TIMBER)
        cv.rect(x - t / 2, 0, x - t / 2 + 0.28, h, TIMBER_L)
    for y in [0] + list(rails) + [h]:
        cv.rect(0, y - t / 2, w, y + t / 2, TIMBER)
        cv.rect(0, y - t / 2, w, y - t / 2 + 0.28, TIMBER_L)
    for (cx, cy, ww, wh, st) in o.get("windows", []):
        window(cv, cx, cy, ww, wh, st, shutters=o.get("shutters", False))
    dr = o.get("door")
    if dr:
        door(cv, *dr)
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.07, 2.4)
    rgb = finish(rgb, K, ao=0.18, top=0.10)
    if o.get("moss"):
        rgb = moss(rgb, rng, K, o["moss"], 0.4)
    return rgb, gl


def paint_stone(w, h, K, rng, o):
    """Ashlar: rows of blocks of uneven length, mortar joints, light and dark blocks, optional windows, door, moss."""
    cv = Cv(w, h, K, rng)
    base = o.get("base", STONE)
    cv.rect(0, 0, w, h, MORTAR)
    rh = o.get("row", 4.0)
    nr = max(1, int(round(h / rh)))
    for r in range(nr):
        ya, yb = h * r / nr, h * (r + 1) / nr
        x = -float(rng.random()) * 4.0
        while x < w:
            bw = o.get("blk", 7.0) * (0.7 + float(rng.random()) * 0.7)
            tone = float(rng.random())
            col = mix(shade(base, 0.82), shade(base, 1.14), tone)
            cv.rect(max(0, x) + 0.18, ya + 0.18, min(w, x + bw) - 0.18, yb - 0.18, col)
            cv.rect(max(0, x) + 0.18, ya + 0.18, min(w, x + bw) - 0.18, ya + 0.5, shade(col, 1.12))
            cv.rect(max(0, x) + 0.18, yb - 0.5, min(w, x + bw) - 0.18, yb - 0.18, shade(col, 0.82))
            x += bw
    for (cx, cy, ww, wh, st) in o.get("windows", []):
        window(cv, cx, cy, ww, wh, st)
    dr = o.get("door")
    if dr:
        door(cv, *dr)
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.07, 2.0)
    rgb = finish(rgb, K, ao=0.2, top=0.12)
    if o.get("moss"):
        rgb = moss(rgb, rng, K, o["moss"], o.get("moss_amount", 0.5))
    return rgb, gl


def paint_brick(w, h, K, rng, o):
    """Running-bond brick: warm terracotta bricks with a lit upper edge and a shaded lower edge, pale mortar; soft shading over the whole face (the golem's fists)."""
    cv = Cv(w, h, K, rng)
    bw, bh = o.get("bw", 6.0), o.get("bh", 3.0)
    cv.rect(0, 0, w, h, MORTAR)
    nr = max(1, int(round(h / bh)))
    base = o.get("base", BRICK)
    for r in range(nr):
        ya, yb = h * r / nr, h * (r + 1) / nr
        off = (bw / 2) if r % 2 else 0.0
        nb = int(w / bw) + 2
        for i in range(-1, nb):
            xa, xb = off + i * bw, off + (i + 1) * bw
            if xb <= 0 or xa >= w:
                continue
            tone = float(rng.random())
            col = mix(shade(base, 0.8), shade(base, 1.18), tone)
            if rng.random() < 0.12:
                col = mix(col, STONE_D, 0.5)
            xa2, xb2 = max(0, xa) + 0.16, min(w, xb) - 0.16
            cv.rect(xa2, ya + 0.16, xb2, yb - 0.16, col)
            cv.rect(xa2, ya + 0.16, xb2, ya + 0.16 + 0.42, shade(col, 1.18))
            cv.rect(xa2, yb - 0.16 - 0.42, xb2, yb - 0.16, shade(col, 0.76))
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.06, 1.6)
    if o.get("soft"):                                                # a soft light from the upper left over the whole block
        H, W = rgb.shape[:2]
        yy, xx = np.mgrid[0:H, 0:W]
        lit = np.exp(-(((xx / W - 0.3) / 0.6) ** 2 + ((yy / H - 0.25) / 0.6) ** 2))
        rgb = rgb * (0.82 + 0.32 * lit)[..., None]
    rgb = finish(rgb, K, ao=o.get("ao", 0.22), top=0.10)
    if o.get("moss"):
        rgb = moss(rgb, rng, K, o["moss"], 0.35)
    return rgb, gl


def paint_slate(w, h, K, rng, o):
    """Roof shingles in scalloped rows (slate or clay tile), a ridge line, moss patches and a few tufts. 'across' = the rows run along the face's height (for a slab seen from its top)."""
    pal = {"slate": (SLATE, SLATE_L, SLATE_D), "tile": (TILE, TILE_L, TILE_D)}[o.get("tone", "slate")]
    A, L, D = pal
    if o.get("across"):
        rgb, gl = paint_slate(h, w, K, rng, {k: v for k, v in o.items() if k != "across"})
        return np.ascontiguousarray(np.transpose(rgb, (1, 0, 2))), np.ascontiguousarray(np.transpose(gl, (1, 0, 2)))
    cv = Cv(w, h, K, rng)
    cv.rect(0, 0, w, h, shade(D, 0.8))
    rh = o.get("row", 3.0)
    sw = o.get("sw", 3.0)
    nr = int(math.ceil(h / rh)) + 1
    for r in range(nr):
        y0 = r * rh
        off = (sw / 2) if r % 2 else 0.0
        nn = int(w / sw) + 2
        for i in range(-1, nn):
            cx = off + i * sw + sw / 2
            tone = float(rng.random())
            col = mix(shade(A, 0.82), shade(A, 1.16), tone)
            cv.rect(cx - sw / 2 + 0.1, y0, cx + sw / 2 - 0.1, y0 + rh * 0.55, col)
            cv.ellipse(cx, y0 + rh * 0.55, sw / 2 - 0.1, rh * 0.5, col)
            cv.ellipse(cx, y0 + rh * 0.45, sw / 2 - 0.5, rh * 0.28, shade(col, 1.14))      # the lit crown of the shingle
            cv.ellipse(cx, y0 + rh * 1.0, sw / 2 - 0.25, rh * 0.16, shade(col, 0.66))      # its shadow on the row below
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.07, 1.8)
    rgb = finish(rgb, K, ao=0.16, top=0.08)
    if o.get("moss"):
        rgb = moss(rgb, rng, K, o["moss"], o.get("moss_amount", 0.55))
    if o.get("ridge"):                                               # the ridge cap along the top edge
        n = int(max(1, o.get("ridge_px", 1.4) * K))
        rgb[:n, :] = np.array(shade(A, 0.7), np.float32)
        rgb[n:n + max(1, n // 3), :] = np.array(shade(L, 1.0), np.float32)
    return rgb, gl


def paint_grass(w, h, K, rng, o):
    """A flat top with grass and moss over stone: for terraces and for the tops of roofs."""
    cv = Cv(w, h, K, rng)
    base = o.get("under", STONE)
    cv.rect(0, 0, w, h, shade(base, 0.9))
    rgb, gl = cv.out()
    H, W = rgb.shape[:2]
    n = vnoise(H, W, rng, max(1.0, 2.4 * K))
    n2 = vnoise(H, W, rng, max(1.0, 1.0 * K))
    cover = o.get("cover", 0.8)
    m = n > (1.0 - cover) * 0.9
    col = np.zeros((H, W, 3), np.float32)
    for i in range(3):
        col[..., i] = np.where(n2 > 0.66, MOSS_L[i], np.where(n2 < 0.3, MOSS_D[i], MOSS[i]))
    rgb = np.where(m[..., None], col, rgb)
    for _ in range(int(w * h / 18)):                                 # tufts and tiny flowers
        x, y = int(rng.integers(0, W)), int(rng.integers(0, H))
        if m[y, x]:
            rgb[y, x] = (255, 236, 150) if rng.random() < 0.1 else MOSS_L
    rgb = finish(rgb, K, ao=0.12, top=0.0)
    return rgb, gl


def paint_leaves(w, h, K, rng, o):
    """Foliage: clumps of three greens with lit spots, dithered edges."""
    cv = Cv(w, h, K, rng)
    cv.rect(0, 0, w, h, MOSS_D)
    rgb, gl = cv.out()
    H, W = rgb.shape[:2]
    n = vnoise(H, W, rng, max(1.0, 2.2 * K))
    n2 = vnoise(H, W, rng, max(1.0, 1.0 * K))
    pal = o.get("pal", (MOSS_D, MOSS, MOSS_L))
    out = np.zeros((H, W, 3), np.float32)
    for i in range(3):
        out[..., i] = np.where(n > 0.62, pal[2][i], np.where(n > 0.38, pal[1][i], pal[0][i]))
    out = np.where(((n2 > 0.8))[..., None], np.array([186, 216, 108], np.float32), out)
    out = finish(out, K, ao=0.3, top=0.1, edge=1.6)
    return out, gl


def paint_wood(w, h, K, rng, o):
    """Vertical oak boards with grain, nail heads and a dark gap between boards."""
    cv = Cv(w, h, K, rng)
    base = o.get("base", (122, 82, 50))
    bw = o.get("bw", 3.0)
    cv.rect(0, 0, w, h, shade(base, 0.45))
    n = max(1, int(round(w / bw)))
    for i in range(n):
        xa, xb = w * i / n, w * (i + 1) / n
        col = mix(shade(base, 0.8), shade(base, 1.15), float(rng.random()))
        cv.rect(xa + 0.15, 0, xb - 0.15, h, col)
        for _ in range(3):
            gx = xa + 0.4 + float(rng.random()) * max(0.1, (xb - xa - 0.8))
            cv.line(gx, 0, gx + float(rng.random()) * 0.3 - 0.15, h, 0.12, shade(col, 0.78))
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.06, 2.0)
    rgb = finish(rgb, K, ao=0.2, top=0.08)
    return rgb, gl


def paint_tower(w, h, K, rng, o):
    """Tower stone: small pale blocks, an arrow slit or two that glow warm, a darker foot."""
    sl = o.get("slits", [])
    oo = dict(o)
    oo["blk"] = o.get("blk", 4.0)
    oo["row"] = o.get("row", 3.0)
    oo["base"] = o.get("base", (176, 170, 160))
    oo.pop("slits", None)
    cv_rgb, cv_gl = paint_stone(w, h, K, rng, oo)
    if sl:
        cv = Cv(w, h, K, rng)
        for (cx, cy, sw, sh) in sl:
            cv.rect(cx - sw / 2 - 0.5, cy - sh / 2 - 0.5, cx + sw / 2 + 0.5, cy + sh / 2 + 0.5, (66, 62, 60))
            cv.rect(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2, (255, 190, 90), (230, 150, 50))
        add, gadd = cv.out()
        mask = add.sum(axis=2) > 0
        cv_rgb = np.where(mask[..., None], add, cv_rgb)
        cv_gl = np.maximum(cv_gl, gadd)
    return cv_rgb, cv_gl


def paint_visor(w, h, K, rng, o):
    """The golem's face: dark weathered stone with two slanted slit eyes, yellow-green, glowing (the glow map)."""
    cv = Cv(w, h, K, rng)
    cv.rect(0, 0, w, h, (58, 56, 60))
    ey = h * o.get("ey", 0.5)
    ex = w * o.get("ex", 0.27)
    ew, eh = w * o.get("ew", 0.2), h * o.get("eh", 0.17)
    for sx in (-1, 1):
        cx = w / 2 + sx * ex
        pts = [(cx - sx * ew * 0.5, ey - eh * 0.2), (cx + sx * ew * 0.55, ey - eh * 0.75), (cx + sx * ew * 0.5, ey + eh * 0.35), (cx - sx * ew * 0.5, ey + eh * 0.55)]
        grow = [(x + (x - cx) * 0.55, y + (y - ey) * 0.9) for x, y in pts]
        cv.poly(grow, (88, 120, 30), (70, 110, 20))
        cv.poly(pts, EYE_RIM, (170, 230, 50))
        inner = [(cx + (x - cx) * 0.62, ey + (y - ey) * 0.55) for x, y in pts]
        cv.poly(inner, EYE_CORE, (255, 255, 200))
    cv.rect(w * 0.12, h * 0.78, w * 0.88, h * 0.82, (36, 34, 38))                          # mouth seam
    rgb, gl = cv.out()
    rgb = grain(rgb, rng, K, 0.1, 2.0)
    rgb = finish(rgb, K, ao=0.24, top=0.1)
    return rgb, gl


def paint_iron(w, h, K, rng, o):
    """Iron strap: dark metal with rivets."""
    cv = Cv(w, h, K, rng)
    cv.rect(0, 0, w, h, IRON)
    cv.rect(0, 0, w, max(0.4, h * 0.12), IRON_L)
    for i in range(max(2, int(w / 3))):
        cv.ellipse(1.2 + i * 3.0, h / 2, 0.45, 0.45, IRON_L)
    rgb, gl = cv.out()
    return grain(rgb, rng, K, 0.07, 1.5), gl


def paint_lamp(w, h, K, rng, o):
    """A lantern pane: a lit warm block (glows)."""
    cv = Cv(w, h, K, rng)
    cv.rect(0, 0, w, h, (255, 190, 90), (255, 170, 70))
    cv.rect(w * 0.15, h * 0.15, w * 0.85, h * 0.85, (255, 236, 170), (255, 220, 140))
    rgb, gl = cv.out()
    return rgb, gl


def paint_chimney(w, h, K, rng, o):
    """Chimney brick with soot toward the top."""
    rgb, gl = paint_brick(w, h, K, rng, {"bw": 4.0, "bh": 2.2, "base": (150, 84, 64), "ao": 0.2})
    H = rgb.shape[0]
    soot = np.clip(1.0 - np.arange(H) / (H * 0.35), 0, 1)[:, None, None]
    return rgb * (1.0 - 0.55 * soot), gl


KINDS = {"timber": paint_timber, "stone": paint_stone, "brick": paint_brick, "slate": paint_slate, "grass": paint_grass, "leaves": paint_leaves, "wood": paint_wood,
         "tower": paint_tower, "visor": paint_visor, "iron": paint_iron, "lamp": paint_lamp, "chimney": paint_chimney}

FACE_KEYS = {"west": "east", "north": "north", "east": "west", "south": "south", "top": "up", "bottom": "down"}          # physical side -> the key of a Bedrock per-face uv
FACE_ORDER = ("north", "south", "west", "east", "top", "bottom")


# ------------------------------------------------------------------------------------------------ a model whose faces are painted one by one
class Sheet:
    def __init__(self, key, name, tex_w=1536, K=3.0, seed=1):
        self.m = Model(key, name, tex_w, tex_w)
        self.tw, self.K, self.seed = tex_w, K, seed
        self.items = []

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def cube(self, bone, o, s, base=("stone", {}), faces=None, hide=(), rot=None, pv=None, K=None):
        """o = the corner with the smallest x, y, z; s = (w, h, d); base = (kind, options) of every face; faces = {side: (kind, options)} overrides; hide = sides not drawn."""
        c = self.m.cube(bone, origin=o, size=s, uv=(0, 0), rotation=rot, pivot=pv)
        self.items.append(dict(c=c, s=tuple(s), base=base, faces=faces or {}, hide=tuple(hide), K=K or self.K, idx=len(self.items)))
        return c

    def count(self):
        return self.m.cube_count()

    @staticmethod
    def face_size(side, s):
        w, h, d = s
        return {"north": (w, h), "south": (w, h), "west": (d, h), "east": (d, h), "top": (w, d), "bottom": (w, d)}[side]

    def pack(self):
        rects = []
        for it in self.items:
            for side in FACE_ORDER:
                if side in it["hide"]:
                    continue
                fw, fh = self.face_size(side, it["s"])
                Wt, Ht = int(math.ceil(fw * it["K"] - 1e-6)), int(math.ceil(fh * it["K"] - 1e-6))
                rects.append((Ht + 2, Wt + 2, it["idx"], side))
        rects.sort(key=lambda r: (-r[0], -r[1]))
        x = y = rowh = 0
        place = {}
        for (rh, rw, idx, side) in rects:
            if rw > self.tw:
                raise SystemExit("face wider than the sheet")
            if x + rw > self.tw:
                x, y, rowh = 0, y + rowh, 0
            place[(idx, side)] = (x + 1, y + 1)
            x += rw
            rowh = max(rowh, rh)
        return place, y + rowh

    def paint(self):
        place, height = self.pack()
        th = int(math.ceil(height / 8.0) * 8)
        self.m.tex_h = th
        img = np.zeros((th, self.tw, 4), np.float32)
        glow = np.zeros((th, self.tw, 4), np.float32)
        for it in self.items:
            c = it["c"]
            uvd = {}
            for side in FACE_ORDER:
                if side in it["hide"]:
                    continue
                fw, fh = self.face_size(side, it["s"])
                kind, opt = it["faces"].get(side, it["base"])
                rng = np.random.default_rng(self.seed * 100003 + it["idx"] * 131 + FACE_ORDER.index(side) * 17)
                rgb, gl = KINDS[kind](fw, fh, it["K"], rng, dict(opt))
                Ht, Wt = rgb.shape[:2]
                x0, y0 = place[(it["idx"], side)]
                rgb = np.clip(rgb, 0, 255)
                gl = np.clip(gl, 0, 255)
                img[y0:y0 + Ht, x0:x0 + Wt, :3] = rgb
                img[y0:y0 + Ht, x0:x0 + Wt, 3] = 255
                glow[y0:y0 + Ht, x0:x0 + Wt, :3] = gl
                glow[y0:y0 + Ht, x0:x0 + Wt, 3] = 255
                for dst, src_slice in ((img, rgb), (glow, gl)):          # one texel of bleed round the face so a sample on the edge never reads a neighbour
                    a = 255
                    dst[y0 - 1, x0:x0 + Wt, :3] = src_slice[0]
                    dst[y0 + Ht, x0:x0 + Wt, :3] = src_slice[-1]
                    dst[y0:y0 + Ht, x0 - 1, :3] = src_slice[:, 0]
                    dst[y0:y0 + Ht, x0 + Wt, :3] = src_slice[:, -1]
                    dst[y0 - 1, x0:x0 + Wt, 3] = a
                    dst[y0 + Ht, x0:x0 + Wt, 3] = a
                    dst[y0:y0 + Ht, x0 - 1, 3] = a
                    dst[y0:y0 + Ht, x0 + Wt, 3] = a
                uvd[FACE_KEYS[side]] = {"uv": [x0, y0], "uv_size": [Wt, Ht]}
            c["uv"] = uvd
        return Image.fromarray(img.astype(np.uint8), "RGBA"), Image.fromarray(glow.astype(np.uint8), "RGBA")

    def save(self, anim=None):
        im, gl = self.paint()
        self.m.tex_w, self.m.tex_h = self.tw, im.size[1]
        self.m.save(anim)
        self.m.save_texture(im, gl)
        print("wrote", self.m.id, "cubes", self.m.cube_count(), "tex", im.size[0], "x", im.size[1], "clips", sorted(anim.clips) if anim and anim.clips else "-")

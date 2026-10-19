"""Why: the shared code of the abyssal boss looks (docs/labyrinth/abyssal_bosses.md): one small library that every look file (tools/abyssal/look_*.py) is written against, so that
twenty-two creatures can be built, painted and animated by scripts that can be re-run (the owner's rule for textures) and that all end in the repo's GeoLite pipeline (tools/geo_builder.py
writes the format, client/geo/* draws it).

What it gives a look file:
  * Creature     a GeoLite model whose cubes are painted by *materials* (functions that draw on a face canvas) and packed into one atlas automatically: box UV in the vanilla layout,
                 a density of 1 or 2 texels per model pixel, mirrored twins sharing one rectangle, an emissive glow map painted at the same time.
  * Humanoid     the vanilla PLAYER model as bones (head, body, arms, legs) textured by a real 64x64 player skin (box UV of the vanilla skin layout, a glow skin next to it) plus
                 separate GeoLite EXTRAS (crown, phylactery, wings, staff ...) hung on named anchor bones, so that the same skin and the same extras can later be handed to the player-model renderer.
  * AnimRig      keyframes for the clip names the pose clock plays (idle walk run attack cast cast_big hurt death), written from a motion function plus the secondary motion (sway) of
                 every bone that asked for it.
Frame of the files: pixels (16 = one block), y up, origin at the feet, the front on -z, +x is the model's left (docs/asset_pipeline.md). Run python as `python3 -B`.
"""
import json
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, TOOLS)
import geo_builder  # noqa: E402

ROOT = geo_builder.ROOT                      # src/main/resources/assets/nusmp
KEY = "abyssal"
TAU = math.tau


# ------------------------------------------------------------------------------------------------ colours
def rgb(c):
    """'#rrggbb', '#rgb' or a tuple -> (r, g, b)."""
    if isinstance(c, str):
        c = c.lstrip("#")
        if len(c) == 3:
            c = "".join(ch * 2 for ch in c)
        return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))
    return tuple(int(v) for v in c[:3])


def mix(a, b, k):
    a, b = rgb(a), rgb(b)
    return tuple(int(round(a[i] * (1 - k) + b[i] * k)) for i in range(3))


def shade(c, k):
    """k < 1 darker, > 1 lighter (shadows lean blue-violet, lights warm: the kit's rule)."""
    r, g, b = rgb(c)
    if k < 1:
        return (int(r * k * 0.98), int(g * k * 0.98), int(min(255, b * k * 1.06)))
    k2 = k - 1
    return (int(min(255, r + (255 - r) * k2 * 0.9)), int(min(255, g + (255 - g) * k2 * 0.8)), int(min(255, b + (255 - b) * k2 * 0.6)))


# ------------------------------------------------------------------------------------------------ canvas
class Cv:
    """A face of a cube (or of a skin part) to draw on: img and glow are uint8 views (h, w, 4) into the atlas, so drawing here paints the real texture. (x, y) is the texel from the
    top-left of the face as seen from outside. `face` is right (-x) front (-z) left (+x) back (+z) top bottom; `rng` is seeded from the cube, so a rebuild gives the same pixels."""

    def __init__(self, img, glow, face, seed):
        self.img, self.glow, self.face = img, glow, face
        self.h, self.w = img.shape[0], img.shape[1]
        self.rng = np.random.RandomState(seed & 0x7FFFFFFF)

    def noise(self, scale=2):
        """Smooth value noise 0..1, (h, w)."""
        gh, gw = self.h // scale + 3, self.w // scale + 3
        g = self.rng.rand(gh, gw)
        ys, xs = np.mgrid[0:self.h, 0:self.w]
        fy, fx = ys / scale, xs / scale
        y0, x0 = fy.astype(int), fx.astype(int)
        ty, tx = fy - y0, fx - x0
        a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
        b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
        return a * (1 - ty) + b * ty

    def set_rgb(self, arr, alpha=255):
        a = np.clip(arr, 0, 255).astype(np.uint8)
        self.img[..., :3] = a
        self.img[..., 3] = alpha

    def px(self, x, y, c, g=None, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.img[y, x, :3] = rgb(c)
            self.img[y, x, 3] = a
            if g is not None:
                self.glow[y, x, :3] = rgb(g)
                self.glow[y, x, 3] = 255

    def glowpx(self, x, y, g, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.glow[y, x, :3] = rgb(g)
            self.glow[y, x, 3] = a

    def rect(self, x, y, w, h, c, g=None, a=255):
        x0, y0, x1, y1 = max(0, int(x)), max(0, int(y)), min(self.w, int(x + w)), min(self.h, int(y + h))
        if x1 <= x0 or y1 <= y0:
            return
        self.img[y0:y1, x0:x1, :3] = rgb(c)
        self.img[y0:y1, x0:x1, 3] = a
        if g is not None:
            self.glow[y0:y1, x0:x1, :3] = rgb(g)
            self.glow[y0:y1, x0:x1, 3] = 255

    def clear(self, x, y, w, h):
        x0, y0, x1, y1 = max(0, int(x)), max(0, int(y)), min(self.w, int(x + w)), min(self.h, int(y + h))
        if x1 > x0 and y1 > y0:
            self.img[y0:y1, x0:x1] = 0
            self.glow[y0:y1, x0:x1] = 0

    def grect(self, x, y, w, h, g, a=255):
        x0, y0, x1, y1 = max(0, int(x)), max(0, int(y)), min(self.w, int(x + w)), min(self.h, int(y + h))
        if x1 > x0 and y1 > y0:
            self.glow[y0:y1, x0:x1, :3] = rgb(g)
            self.glow[y0:y1, x0:x1, 3] = a

    def line(self, x0, y0, x1, y1, c, g=None):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            t = i / max(1, n)
            self.px(int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), c, g)

    def disc(self, cx, cy, r, c, g=None):
        for yy in range(int(cy - r - 1), int(cy + r + 2)):
            for xx in range(int(cx - r - 1), int(cx + r + 2)):
                if (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r + 0.3:
                    self.px(xx, yy, c, g)

    def get(self, x, y):
        return tuple(int(v) for v in self.img[y, x, :3])

    def scale_rgb(self, k):
        """Multiplies the colour of every opaque texel by k (a quick light/dark pass)."""
        m = self.img[..., 3] > 0
        v = self.img[..., :3].astype(float) * k
        self.img[..., :3][m] = np.clip(v[m], 0, 255).astype(np.uint8)


# ------------------------------------------------------------------------------------------------ materials (functions of a Cv)
def flat(color, var=0.08, scale=2):
    def f(cv):
        n = cv.noise(scale)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (1 + var * (n[..., None] * 2 - 1)))
    return f


def banded(color, color2, rows=4, var=0.06):
    """Horizontal bands of two tones (fur, wrappings, layered cloth)."""
    def f(cv):
        n = cv.noise(2)
        ys = (np.arange(cv.h)[:, None] // max(1, rows)) % 2
        base = np.where(ys[..., None] == 0, np.array(rgb(color), float), np.array(rgb(color2), float))
        cv.set_rgb(base * (1 + var * (n[..., None] * 2 - 1)))
    return f


def bone(color="#d9d2b6", crack=0.05):
    def f(cv):
        n = cv.noise(2)
        base = np.array(rgb(color), float)[None, None, :] * (0.88 + 0.22 * n[..., None])
        cv.set_rgb(base)
        dark = shade(color, 0.55)
        m = cv.rng.rand(cv.h, cv.w) < crack
        for y, x in zip(*np.nonzero(m)):
            for k in range(cv.rng.randint(2, 5)):
                cv.px(x + k // 2, y + k, dark)
    return f


def cloth(color, fold=3, tatter=0, trim=None, trim_rows=1, var=0.06):
    """Hanging cloth: vertical folds, optional ragged hem on the side faces (transparent texels) and a trim line at the bottom."""
    def f(cv):
        n = cv.noise(2)
        xs = np.arange(cv.w)[None, :]
        fo = 0.9 + 0.1 * np.sin(xs * TAU / max(2, fold * 2) + cv.rng.rand() * 6)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (fo * (1 + var * (n * 2 - 1)))[..., None])
        if trim and cv.face not in ("top", "bottom"):
            cv.rect(0, cv.h - trim_rows - (1 if tatter else 0), cv.w, trim_rows, trim)
        if tatter and cv.face not in ("top", "bottom"):
            for x in range(cv.w):
                cut = cv.rng.randint(0, tatter + 1)
                if cut:
                    cv.clear(x, cv.h - cut, 1, cut)
    return f


def scales(color, size=3, edge=0.62):
    def f(cv):
        n = cv.noise(3)
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        row = ys // size
        cx = (xs + (row % 2) * (size // 2 + (size % 2))) % size
        cy = ys % size
        k = 1.0 + 0.16 * (1 - cy / max(1, size - 1)) - 0.22 * (cy / max(1, size - 1)) ** 2
        k = np.where((cx == 0) | (cy == size - 1), edge, k)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (k * (0.92 + 0.16 * n))[..., None])
    return f


def plate(color, rim=None, rivets=True):
    def f(cv):
        n = cv.noise(3)
        base = np.array(rgb(color), float)[None, None, :] * (0.92 + 0.14 * n[..., None])
        cv.set_rgb(base)
        hi, lo = shade(color, 1.35), shade(color, 0.55)
        if cv.w > 2 and cv.h > 2:
            cv.rect(0, 0, cv.w, 1, hi)
            cv.rect(0, 0, 1, cv.h, hi)
            cv.rect(0, cv.h - 1, cv.w, 1, lo)
            cv.rect(cv.w - 1, 0, 1, cv.h, lo)
            if rim:
                cv.rect(1, 1, cv.w - 2, 1, rim)
        if rivets and cv.w >= 6 and cv.h >= 6:
            for x, y in ((2, 2), (cv.w - 3, 2), (2, cv.h - 3), (cv.w - 3, cv.h - 3)):
                cv.px(x, y, lo)
    return f


def flesh(color, vein=None, vein_density=0.02):
    def f(cv):
        n = cv.noise(3)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (0.86 + 0.28 * n[..., None]))
        if vein:
            m = cv.rng.rand(cv.h, cv.w) < vein_density
            for y, x in zip(*np.nonzero(m)):
                for k in range(cv.rng.randint(3, 8)):
                    cv.px(x + (k % 2), y + k, vein)
    return f


def chitin(color, spot=None, band=3):
    def f(cv):
        n = cv.noise(2)
        ys = np.arange(cv.h)[:, None]
        k = 1.0 - 0.18 * ((ys % band) == (band - 1))
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (k * (0.9 + 0.2 * n))[..., None])
        if spot:
            m = cv.rng.rand(cv.h, cv.w) < 0.04
            for y, x in zip(*np.nonzero(m)):
                cv.px(x, y, spot)
    return f


def fur(color, streak=None):
    def f(cv):
        n = cv.noise(2)
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        st = 0.85 + 0.3 * ((np.sin(xs * 1.7 + (ys // 3) * 2.1 + cv.rng.rand() * 6) + 1) / 2)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (st * (0.9 + 0.2 * n))[..., None])
        if streak:
            m = cv.rng.rand(cv.h, cv.w) < 0.05
            for y, x in zip(*np.nonzero(m)):
                cv.px(x, y, streak)
    return f


def membrane(color, vein=None, ragged=False):
    """A wing's skin: a soft gradient, a few darker ribs on the broad faces, a lighter lower edge. Small faces stay plain (a rib every three texels reads as noise)."""
    vein = vein or shade(color, 0.6)

    def f(cv):
        n = cv.noise(4)
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        g = 0.9 + 0.2 * (xs / max(1, cv.w - 1))
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (g * (0.93 + 0.14 * n))[..., None])
        if cv.face in ("front", "back") and cv.w >= 8 and cv.h >= 9:
            for i in range(2, cv.w, max(5, cv.w // 4)):
                cv.line(i, 0, i + 1, cv.h - 1, vein)
        if cv.face in ("front", "back") and cv.h >= 9:
            cv.rect(0, cv.h - 1, cv.w, 1, shade(color, 1.35))
        if ragged and cv.face in ("front", "back"):
            for x in range(0, cv.w, 2):
                cut = cv.rng.randint(0, max(2, cv.h // 3))
                if cut:
                    cv.clear(x, cv.h - cut, 2, cut)
    return f


def feathers(color, tip=None, glow=None, size=3):
    """Rows of overlapping feathers: each row a shade lighter and its lower edge a shade darker; `tip` colours the last two rows (burnt tips, gold edges) and `glow` lights them."""
    def f(cv):
        n = cv.noise(2)
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        row = ys // size
        cy = ys % size
        k = 1.0 + 0.1 * (row % 2) - 0.16 * (cy == size - 1) + 0.08 * (cy == 0)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (k * (0.92 + 0.16 * n))[..., None])
        if tip and cv.face in ("front", "back"):
            for y in range(max(0, cv.h - 2), cv.h):
                for x in range(cv.w):
                    cv.px(x, y, tip, glow)
    return f


def brain(color, groove=None):
    groove = rgb(groove or shade(color, 0.6))

    def f(cv):
        n = cv.noise(2)
        n2 = cv.noise(4)
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        ridge = np.sin((xs * 0.8 + n2 * 9 + ys * 0.5) * 1.3)
        k = 0.8 + 0.3 * (ridge * 0.5 + 0.5)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * (k * (0.92 + 0.16 * n))[..., None])
        m = ridge < -0.82
        cv.img[..., :3][m] = np.array(groove, np.uint8)
    return f


def metal(color, edge=None):
    def f(cv):
        ys = np.arange(cv.h)[:, None]
        k = 1.12 - 0.35 * (ys / max(1, cv.h - 1)) + 0.05 * cv.noise(2)
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * k[..., None] * np.ones((1, cv.w, 1)))
        if edge and cv.w > 2:
            cv.rect(0, 0, cv.w, 1, edge)
    return f


def glass(color, alpha=120):
    """Pane-like: mostly transparent, a few opaque rim and highlight texels (the renderer's alpha test keeps those)."""
    def f(cv):
        cv.img[...] = 0
        hi = shade(color, 1.5)
        cv.rect(0, 0, cv.w, 1, color)
        cv.rect(0, cv.h - 1, cv.w, 1, color)
        cv.rect(0, 0, 1, cv.h, color)
        cv.rect(cv.w - 1, 0, 1, cv.h, color)
        for i in range(0, cv.h, 3):
            cv.px(1 + (i // 3) % max(1, cv.w - 2), i, hi)
    return f


def glowfill(color, soft=0.3):
    """Emissive only (no diffuse texel): flames, light, ghostly aura. The glow pass draws it additive."""
    def f(cv):
        n = cv.noise(2)
        cv.img[...] = 0
        k = 1 - soft + soft * n
        cv.glow[..., :3] = np.clip(np.array(rgb(color), float)[None, None, :] * k[..., None], 0, 255).astype(np.uint8)
        cv.glow[..., 3] = 255
    return f


def wisp(color, power=0.75):
    """Ghostly light: emissive only, bright in the middle of the strip and fading to nothing at both ends, with a ragged edge (the additive pass makes dark texels invisible)."""
    def f(cv):
        cv.img[...] = 0
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        prof = np.sin(np.pi * (ys + 0.5) / cv.h) ** 1.3
        n = cv.noise(2)
        k = np.clip(prof * (0.45 + 0.9 * n) * power, 0, 1)
        if cv.face in ("top", "bottom"):
            k = k * 0.0
        cv.glow[..., :3] = np.clip(np.array(rgb(color), float)[None, None, :] * k[..., None], 0, 255).astype(np.uint8)
        cv.glow[..., 3] = 255
    return f


def halo(color, power=0.7):
    """A soft glow: brightest in the middle of every face and fading to nothing at its edge, so a box of it reads as a round bloom, not a block."""
    def f(cv):
        cv.img[...] = 0
        ys, xs = np.mgrid[0:cv.h, 0:cv.w]
        cx, cy = (cv.w - 1) / 2, (cv.h - 1) / 2
        r = np.sqrt(((xs - cx) / max(0.5, cv.w / 2)) ** 2 + ((ys - cy) / max(0.5, cv.h / 2)) ** 2)
        k = np.clip(1 - r, 0, 1) ** 1.4 * power * (0.8 + 0.4 * cv.noise(2))
        cv.glow[..., :3] = np.clip(np.array(rgb(color), float)[None, None, :] * k[..., None], 0, 255).astype(np.uint8)
        cv.glow[..., 3] = 255
    return f


def emissive(color, glow=None, soft=0.25):
    """Solid and bright in both passes (a gem, an ember, a lit eye)."""
    def f(cv):
        n = cv.noise(2)
        k = 1 - soft + soft * n
        cv.set_rgb(np.array(rgb(color), float)[None, None, :] * k[..., None])
        cv.glow[..., :3] = np.clip(np.array(rgb(glow or color), float)[None, None, :] * k[..., None], 0, 255).astype(np.uint8)
        cv.glow[..., 3] = 255
    return f


def eyeball(iris, pupil="#101010", white="#e8e4d8", vein="#a03030", glow_iris=True):
    def f(cv):
        flat(white, 0.05)(cv)
        cx, cy = cv.w / 2 - 0.5, cv.h / 2 - 0.5
        r = max(1.2, min(cv.w, cv.h) * 0.36)
        for yy in range(cv.h):
            for xx in range(cv.w):
                d = math.hypot(xx - cx, yy - cy)
                if d <= r:
                    cv.px(xx, yy, iris, iris if glow_iris else None)
                elif d <= r + 1.2 and vein:
                    pass
        pr = max(0.8, r * 0.45)
        for yy in range(cv.h):
            for xx in range(cv.w):
                if abs(xx - cx) <= pr * 0.55 and abs(yy - cy) <= pr * 1.6:
                    cv.px(xx, yy, pupil, (0, 0, 0))
        if vein:
            for i in range(4):
                a = cv.rng.rand() * TAU
                cv.line(int(cx + math.cos(a) * r), int(cy + math.sin(a) * r), int(cx + math.cos(a) * (r + 3)), int(cy + math.sin(a) * (r + 3)), vein)
    return f


def over(base, *decals):
    """A painter: the base material, then each decal (a function of a Cv) in order."""
    def f(cv):
        base(cv)
        for d in decals:
            d(cv)
    return f


def on(faces, fn):
    """A decal that only runs on the named face(s): on('front', lambda cv: ...)."""
    fs = (faces,) if isinstance(faces, str) else tuple(faces)

    def f(cv):
        if cv.face in fs:
            fn(cv)
    return f


def stripe(color, y, h=1, g=None, faces=("front", "back", "left", "right")):
    return on(faces, lambda cv: cv.rect(0, y, cv.w, h, color, g))


def glow_stripe(color, y, h=1, faces=("front", "back", "left", "right")):
    return on(faces, lambda cv: cv.grect(0, y, cv.w, h, color))


# ------------------------------------------------------------------------------------------------ the atlas packer
def pack(items, width, reserved=None):
    """Skyline packing. items = {key: (w, h)}; reserved = (w, h) kept free at the top-left. Returns ({key: (x, y)}, height)."""
    heights = np.zeros(width, dtype=int)
    if reserved:
        heights[:reserved[0]] = reserved[1]
    out = {}
    for key, (w, h) in sorted(items.items(), key=lambda kv: (-kv[1][1], -kv[1][0], str(kv[0]))):
        if w > width:
            raise ValueError("a cube is wider than the atlas: %s %s > %d" % (key, w, width))
        best_x, best_y = 0, None
        for x in range(0, width - w + 1):
            y = int(heights[x:x + w].max())
            if best_y is None or y < best_y:
                best_x, best_y = x, y
        out[key] = (best_x, best_y)
        heights[best_x:best_x + w] = best_y + h
    return out, int(heights.max())


def face_rects(u, v, w, h, d):
    """Texel rectangles (x, y, w, h) of the six faces of the vanilla box layout."""
    return {"right": (u, v + d, d, h), "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
            "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


def uv_dict(u, v, w, h, d):
    """The per-face uv object of a geo cube for the vanilla box layout (the loader's names: east = the model's -x side, west = +x; the bottom is stored flipped like the box)."""
    r = face_rects(u, v, w, h, d)

    def e(name):
        x, y, fw, fh = r[name]
        return {"uv": [x, y], "uv_size": [fw, fh]}
    return {"east": e("right"), "north": e("front"), "west": e("left"), "south": e("back"), "up": e("top"), "down": e("bottom")}


def seed_of(*parts):
    h = 2166136261
    for p in parts:
        for ch in str(p):
            h = ((h ^ ord(ch)) * 16777619) & 0xFFFFFFFF
    return h


# ------------------------------------------------------------------------------------------------ the creature
class Creature:
    """One GeoLite model under construction. Bones first, then boxes with a paint (a material, or over(material, decals...)); finish() packs the atlas, paints, and writes the files."""

    def __init__(self, look, phase, tex_w=512, name=None):
        self.look, self.phase = look, phase
        self.name = name or "%s_p%d" % (look, phase)
        self.tex_w = tex_w
        self.m = geo_builder.Model(KEY, self.name, tex_w=tex_w, tex_h=64)
        self.reqs = []                 # painted cubes, in order
        self.reserved = None
        self.fixed = []                # (cube, uv rect) with a fixed place (the player skin)
        self.n = 0
        self.skin = None
        self.skin_ids = set()

    # ---- bones
    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def pivot(self, bone):
        return self.m.bones[bone]["pivot"]

    # ---- boxes
    def box(self, bone, origin, size, paint, k=None, inflate=0.0, rot=None, pivot=None, mirror=False, key=None):
        sx, sy, sz = (max(0.5, float(s)) for s in size)
        if k is None:
            k = 2 if max(sx, sy, sz) >= 5 else 1
        while 2 * (sx + sz) * k > self.tex_w and k > 1:
            k -= 1
        c = self.m.cube(bone, origin=origin, size=(sx, sy, sz), uv=(0, 0), inflate=inflate, rotation=rot, pivot=pivot, mirror=mirror)
        self.n += 1
        d, w, h = max(1, round(sz * k)), max(1, round(sx * k)), max(1, round(sy * k))
        self.reqs.append({"cube": c, "dims": (w, h, d), "paint": paint, "key": key or ("c%d" % self.n), "seed": seed_of(self.name, bone, self.n), "owner": True})
        return c

    def pair(self, bone_l, bone_r, origin, size, paint, k=None, inflate=0.0, rot=None, pivot=None):
        """A box on the +x bone and its mirror image on the -x bone; they share one texture rectangle (the right one is the mirrored cube)."""
        sx, sy, sz = size
        key = "p%d" % (self.n + 1)
        cl = self.box(bone_l, origin, size, paint, k=k, inflate=inflate, rot=rot, pivot=pivot, key=key)
        ox, oy, oz = origin
        mo = (-(ox + sx), oy, oz)
        mrot = None if rot is None else (rot[0], -rot[1], -rot[2])
        mp = None if pivot is None else (-pivot[0], pivot[1], pivot[2])
        c = self.m.cube(bone_r, origin=mo, size=(max(0.5, sx), max(0.5, sy), max(0.5, sz)), uv=(0, 0), inflate=inflate, rotation=mrot, pivot=mp, mirror=True)
        r = self.reqs[-1]
        self.reqs.append({"cube": c, "dims": r["dims"], "paint": paint, "key": key, "seed": r["seed"], "owner": False})
        return cl, c

    def raw(self, bone, origin, size, uv, inflate=0.0, mirror=False):
        """A box with a fixed uv corner (the player skin's vanilla layout), no packing."""
        d, w, h = size[2], size[0], size[1]
        c = self.m.cube(bone, origin=origin, size=size, uv=(0, 0), inflate=inflate, mirror=mirror)
        c["uv"] = uv_dict(uv[0], uv[1], w, h, d)
        self.skin_ids.add(id(c))
        return c

    # ---- chains (tentacles, necks, tails)
    def chain(self, name, parent, start, axis, lengths, widths, paint, depth=None, curl=(0, 0, 0), k=None, tip_paint=None):
        """Bones name_0 .. name_n-1, each a cube of lengths[i] along `axis` ('-y', '+y', '-z', '+z', '+x', '-x') from its pivot; widths[i] is the cross size. Each segment is a child of the
        previous one with `curl` degrees of rest bend. Returns the bone names."""
        sign = -1 if axis[0] == "-" else 1
        ax = axis[1]
        names = []
        px, py, pz = start
        par = parent
        for i, (ln, wd) in enumerate(zip(lengths, widths)):
            dp = depth[i] if depth else wd
            bn = "%s_%d" % (name, i)
            self.bone(bn, parent=par, pivot=(px, py, pz), rotation=curl if i else None)
            if ax == "y":
                size = (wd, ln, dp)
                org = (px - wd / 2, py - ln if sign < 0 else py, pz - dp / 2)
                npos = (px, py + sign * ln, pz)
            elif ax == "z":
                size = (wd, dp, ln)
                org = (px - wd / 2, py - dp / 2, pz - ln if sign < 0 else pz)
                npos = (px, py, pz + sign * ln)
            else:
                size = (ln, dp, wd)
                org = (px - ln if sign < 0 else px, py - dp / 2, pz - wd / 2)
                npos = (px + sign * ln, py, pz)
            self.box(bn, org, size, tip_paint if (tip_paint and i == len(lengths) - 1) else paint, k=k)
            names.append(bn)
            px, py, pz = npos
            par = bn
        return names

    # ---- finish
    def finish(self, reserved=None, skin=None):
        items = {}
        for r in self.reqs:
            if r["owner"]:
                w, h, d = r["dims"]
                items[r["key"]] = (2 * (w + d), h + d)
        pos, height = pack(items, self.tex_w, reserved)
        th = max(16, ((height + 15) // 16) * 16)
        if reserved:
            th = max(th, reserved[1])
        img = np.zeros((th, self.tex_w, 4), np.uint8)
        glow = np.zeros((th, self.tex_w, 4), np.uint8)
        if skin is not None:
            img[:64, :64] = skin[0]
            glow[:64, :64] = skin[1]
        for r in self.reqs:
            w, h, d = r["dims"]
            u, v = pos[r["key"]]
            r["cube"]["uv"] = uv_dict(u, v, w, h, d)
            if r["owner"]:
                for face, (x, y, fw, fh) in face_rects(u, v, w, h, d).items():
                    cv = Cv(img[y:y + fh, x:x + fw], glow[y:y + fh, x:x + fw], face, r["seed"] + seed_of(face) % 977)
                    r["paint"](cv)
        self.m.tex_h = th
        return img, glow

    def save(self, img, glow, animation=None, shared_anim=None):
        from PIL import Image
        m = self.m
        os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        with open(os.path.join(ROOT, "geo", "entity", m.id + ".geo.json"), "w") as f:
            json.dump(_round(m.geo()), f, separators=(",", ":"))
        Image.fromarray(img, "RGBA").save(os.path.join(ROOT, "textures", "entity", m.id + ".png"), optimize=True)
        Image.fromarray(glow, "RGBA").save(os.path.join(ROOT, "textures", "entity", m.id + "_glow.png"), optimize=True)


def _round(o):
    if isinstance(o, float):
        return round(o, 3)
    if isinstance(o, list):
        return [_round(v) for v in o]
    if isinstance(o, dict):
        return {k: _round(v) for k, v in o.items()}
    return o


# ------------------------------------------------------------------------------------------------ the player model
SKIN_PARTS = {"head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
              "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4), "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
              "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4), "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4)}
ARM_PARTS = ("rarm", "rsleeve", "larm", "lsleeve")
ALL_FACES = ("right", "front", "left", "back", "top", "bottom")


class Skin:
    """A 64x64 player skin and its glow skin, drawn with the same materials and decals as the geo cubes (a face of a part is a Cv). Opaque base layer, optional second layer."""

    def __init__(self, slim=False, seed=1):
        self.slim = slim
        self.img = np.zeros((64, 64, 4), np.uint8)
        self.glow = np.zeros((64, 64, 4), np.uint8)
        self.seed = seed

    def dims(self, part):
        u, v, w, h, d = SKIN_PARTS[part]
        if self.slim and part in ARM_PARTS:
            w = 3
        return u, v, w, h, d

    def face(self, part, face):
        u, v, w, h, d = self.dims(part)
        x, y, fw, fh = face_rects(u, v, w, h, d)[face]
        return Cv(self.img[y:y + fh, x:x + fw], self.glow[y:y + fh, x:x + fw], face, seed_of(self.seed, part, face))

    def paint(self, part, paint, faces=ALL_FACES):
        for f in faces:
            paint(self.face(part, f))

    def clear_part(self, part):
        for f in ALL_FACES:
            cv = self.face(part, f)
            cv.img[...] = 0
            cv.glow[...] = 0

    def opaque_base(self):
        """The base layer must be opaque (vanilla draws it with no alpha test on the player model): any texel left empty becomes black."""
        for part in ("head", "body", "rarm", "larm", "rleg", "lleg"):
            for f in ALL_FACES:
                cv = self.face(part, f)
                m = cv.img[..., 3] == 0
                cv.img[m] = (8, 6, 10, 255)

    def save_skin(self, name):
        """The skin where the player-model kit reads it: textures/entity/playermodel/<name>.png and <name>_glow.png."""
        from PIL import Image
        os.makedirs(os.path.join(ROOT, "textures", "entity", "playermodel"), exist_ok=True)
        Image.fromarray(self.img, "RGBA").save(os.path.join(ROOT, "textures", "entity", "playermodel", name + ".png"), optimize=True)
        Image.fromarray(self.glow, "RGBA").save(os.path.join(ROOT, "textures", "entity", "playermodel", name + "_glow.png"), optimize=True)


class Humanoid(Creature):
    """The vanilla player model as GeoLite bones with a real 64x64 skin (box UV of the vanilla layout) in the corner of the atlas, and room next to it for EXTRAS: any cube added with box()
    to a bone named after the part it hangs on (head, body, arm_r, arm_l, leg_r, leg_l, root, or a child bone of those).

    Bones: root > body (pivot at the hips) > head (neck), arm_r, arm_l (shoulders); root > leg_r, leg_l (hips). The player's right is -x."""
    ANCHORS = ("root", "body", "head", "arm_r", "arm_l", "leg_r", "leg_l")

    def __init__(self, look, phase, skin, tex_w=256):
        super().__init__(look, phase, tex_w=tex_w)
        self.skin = skin
        self.extras = []
        s = 3 if skin.slim else 4
        self.bone("root", pivot=(0, 0, 0))
        self.bone("body", "root", pivot=(0, 12, 0))
        self.bone("head", "body", pivot=(0, 24, 0))
        self.bone("arm_r", "body", pivot=(-5, 22, 0))
        self.bone("arm_l", "body", pivot=(5, 22, 0))
        self.bone("leg_r", "root", pivot=(-1.9, 12, 0))
        self.bone("leg_l", "root", pivot=(1.9, 12, 0))
        R = self.raw
        R("head", (-4, 24, -4), (8, 8, 8), SKIN_PARTS["head"][:2])
        R("body", (-4, 12, -2), (8, 12, 4), SKIN_PARTS["body"][:2])
        R("arm_r", (-4 - s, 10, -2), (s, 12, 4), SKIN_PARTS["rarm"][:2])
        R("arm_l", (4, 10, -2), (s, 12, 4), SKIN_PARTS["larm"][:2])
        R("leg_r", (-3.9, 0, -2), (4, 12, 4), SKIN_PARTS["rleg"][:2])
        R("leg_l", (-0.1, 0, -2), (4, 12, 4), SKIN_PARTS["lleg"][:2])
        R("head", (-4, 24, -4), (8, 8, 8), SKIN_PARTS["hat"][:2], inflate=0.5)
        R("body", (-4, 12, -2), (8, 12, 4), SKIN_PARTS["jacket"][:2], inflate=0.25)
        R("arm_r", (-4 - s, 10, -2), (s, 12, 4), SKIN_PARTS["rsleeve"][:2], inflate=0.25)
        R("arm_l", (4, 10, -2), (s, 12, 4), SKIN_PARTS["lsleeve"][:2], inflate=0.25)
        R("leg_r", (-3.9, 0, -2), (4, 12, 4), SKIN_PARTS["rpants"][:2], inflate=0.25)
        R("leg_l", (-0.1, 0, -2), (4, 12, 4), SKIN_PARTS["lpants"][:2], inflate=0.25)

    def note_extra(self, ident, anchor, bones):
        """Lists an extra for the look table: its id, the part it hangs on, the bones (of the extras model) that belong to it."""
        self.extras.append({"id": ident, "anchor": anchor, "bones": list(bones)})

    def finish(self, **kw):
        return super().finish(reserved=(64, 64), skin=(self.skin.img, self.skin.glow))

    PIVOTS = {"head": (0, 24, 0), "body": (0, 24, 0), "arm_r": (-5, 22, 0), "arm_l": (5, 22, 0), "leg_r": (-1.9, 12, 0), "leg_l": (1.9, 12, 0)}

    def save_extras(self, img, glow, name):
        """The EXTRAS alone, in the player-model kit's format (docs/playermodel.md): a GeoLite file whose ROOT bones are named x_head, x_body, x_arm_r, x_arm_l, x_leg_r, x_leg_l (each drawn under that part of
        the player model, with the file's absolute humanoid coordinates), every extra a child of the root of the part it hangs on (the bones of my composite whose parent is `root` or `body` go under
        x_body), and the cubes that sit straight on a part (pauldrons, spikes) in that root's own cubes. The atlas is the composite's with the skin corner blanked. Written as <name> (geo/entity,
        textures/entity). Returns (cube count, the names of every bone of the file)."""
        from PIL import Image
        m = self.m
        remap = {"root": "x_body", "body": "x_body", "head": "x_head", "arm_r": "x_arm_r", "arm_l": "x_arm_l", "leg_r": "x_leg_r", "leg_l": "x_leg_l"}
        roots = {}
        bones = []
        for n in m.order:
            b = m.bones[n]
            cubes = [c for c in b["cubes"] if id(c) not in self.skin_ids]
            if n in remap:                                               # a part of the player body: only its extra cubes survive, on the root of its anchor
                if cubes:
                    r = roots.setdefault(remap[n], {"name": remap[n], "pivot": list(self.PIVOTS["body" if remap[n] == "x_body" else remap[n][2:]]), "cubes": []})
                    r["cubes"] += cubes
                continue
            nb = dict(b)
            nb["cubes"] = cubes
            par = b.get("parent")
            nb["parent"] = remap.get(par, par)
            if par in remap:
                roots.setdefault(remap[par], {"name": remap[par], "pivot": list(self.PIVOTS["body" if remap[par] == "x_body" else remap[par][2:]]), "cubes": []})
            bones.append(nb)
        out = list(roots.values()) + bones
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + name, "texture_width": m.tex_w, "texture_height": m.tex_h, "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0]},
            "bones": out}]}
        with open(os.path.join(ROOT, "geo", "entity", name + ".geo.json"), "w") as f:
            json.dump(_round(geo), f, separators=(",", ":"))
        i2, g2 = img.copy(), glow.copy()
        i2[:64, :64] = 0
        g2[:64, :64] = 0
        Image.fromarray(i2, "RGBA").save(os.path.join(ROOT, "textures", "entity", name + ".png"), optimize=True)
        Image.fromarray(g2, "RGBA").save(os.path.join(ROOT, "textures", "entity", name + "_glow.png"), optimize=True)
        return sum(len(b["cubes"]) for b in out), [b["name"] for b in out]


# ------------------------------------------------------------------------------------------------ animation
CLIP_LENGTH = {"idle": 3.2, "walk": 1.1, "run": 0.62, "attack": 0.9, "cast": 1.2, "cast_big": 2.4, "hurt": 0.45, "death": 2.6}
CLIP_LOOP = {"idle": True, "walk": True, "run": True, "attack": False, "cast": True, "cast_big": False, "hurt": False, "death": False}
SWAY_GAIN = {"idle": 1.0, "walk": 1.4, "run": 1.9, "attack": 1.4, "cast": 1.3, "cast_big": 1.2, "hurt": 0.6, "death": 0.25}


class AnimRig:
    """The clips of one look (the three phases share one animation file, so a bone that only phase 3 has simply has a track the other models ignore).

    motion(clip, t, L) -> {bone: {"rot": (x, y, z), "pos": (x, y, z), "scale": (x, y, z)}}   the poses the look writes by hand (any key may be missing)
    sway(bone, axis, amp, cyc, phase, kind)  secondary motion added in every clip: amp degrees (or pixels for kind "pos"), cyc cycles per 3.2 s, scaled by the clip's gain."""

    def __init__(self, motion, step=None):
        self.motion = motion
        self.sways = {}
        self.bones = set()
        self.step = step or {}

    def sway(self, bone, axis=0, amp=3.0, cyc=1, phase=0.0, kind="rot", gain=1.0):
        item = (kind, axis, amp, cyc, round(phase, 4), gain)
        lst = self.sways.setdefault(bone, [])
        if item not in lst:                                     # the three phases register the same bones again: once is enough
            lst.append(item)
        self.bones.add(bone)

    def sway_chain(self, names, axis=0, amp=4.0, cyc=1, lag=0.12, kind="rot", axis2=None, amp2=None):
        """A travelling wave down a chain (tentacle, neck, tail): each segment lags the one before it."""
        for i, n in enumerate(names):
            self.sway(n, axis, amp, cyc, -lag * i, kind)
            if axis2 is not None:
                self.sway(n, axis2, amp2 if amp2 is not None else amp, cyc, -lag * i + 0.25, kind)

    def _sample(self, clip, t, L):
        out = {}
        for b, items in self.motion(clip, t, L).items():
            out[b] = {k: list(v) for k, v in items.items()}
        gain = SWAY_GAIN[clip]
        loop = CLIP_LOOP[clip]
        for b, lst in self.sways.items():
            for kind, axis, amp, cyc, phase, g in lst:
                n = max(1, round(cyc * L / 3.2)) if loop else cyc * L / 3.2
                env = 1.0 if loop else math.sin(math.pi * min(1.0, t / L)) ** 0.5
                if clip == "death":
                    env = max(0.0, 1.0 - t / L)
                d = out.setdefault(b, {})
                if kind == "spin":                                  # a continuous turn: `amp` whole turns per 3.2 s (at least one per clip, so a loop closes)
                    v = d.setdefault("rot", [0.0, 0.0, 0.0])
                    if loop:
                        v[axis] += 360.0 * max(1, round(amp * L / 3.2)) * t / L
                    continue
                val = amp * gain * g * math.sin(TAU * (n * t / L + phase)) * env
                key = {"rot": "rot", "pos": "pos", "scale": "scale"}[kind]
                v = d.setdefault(key, [1.0, 1.0, 1.0] if kind == "scale" else [0.0, 0.0, 0.0])
                if kind == "scale":
                    for i in range(3):
                        v[i] += val if (axis == 3 or axis == i) else 0.0     # axis 3 = all three
                else:
                    v[axis] += val
        return out

    def write(self, anim_name, clips=None, only=None, filename=None):
        """Writes abyssal_<anim_name>.animation.json. `only` = the bone names that exist in at least one phase: tracks for any other bone are dropped (a look's motion function may name bones that only a later phase has)."""
        a = geo_builder.Anim()
        for clip in (clips or CLIP_LENGTH):
            L = CLIP_LENGTH[clip]
            a.clip(clip, length=L, loop=CLIP_LOOP[clip])
            step = self.step.get(clip, 0.2 if clip == "idle" else 0.125)
            n = max(2, int(round(L / step)))
            times = [L * i / n for i in range(n + 1)]
            samples = [self._sample(clip, t, L) for t in times]
            if CLIP_LOOP[clip]:
                samples[-1] = samples[0]
            bones = set()
            for s in samples:
                bones |= set(s)
            for b in sorted(bones):
                if only is not None and b not in only:
                    continue
                for kind, setter in (("rot", a.rot), ("pos", a.pos), ("scale", a.scale)):
                    frames = {}
                    for t, s in zip(times, samples):
                        v = s.get(b, {}).get(kind)
                        frames[t] = tuple(round(x, 2) for x in v) if v is not None else ((1, 1, 1) if kind == "scale" else (0, 0, 0))
                    if all(f == ((1, 1, 1) if kind == "scale" else (0, 0, 0)) for f in frames.values()):
                        continue
                    setter(clip, b, frames)
        os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
        path = os.path.join(ROOT, "animations", "entity", (filename or KEY + "_" + anim_name) + ".animation.json")
        with open(path, "w") as f:
            json.dump(a.data(), f, separators=(",", ":"))
        return a


def lerp(a, b, k):
    return a + (b - a) * k


def smooth(k):
    k = max(0.0, min(1.0, k))
    return k * k * (3 - 2 * k)


def pulse(t, L, n=1, phase=0.0):
    return 0.5 + 0.5 * math.sin(TAU * (n * t / L + phase))


def keys(t, pts):
    """Piecewise-smooth interpolation of (time, value) points (value may be a tuple)."""
    if t <= pts[0][0]:
        return pts[0][1]
    for (t0, v0), (t1, v1) in zip(pts, pts[1:]):
        if t <= t1:
            k = smooth((t - t0) / max(1e-6, t1 - t0))
            if isinstance(v0, (tuple, list)):
                return tuple(lerp(a, b, k) for a, b in zip(v0, v1))
            return lerp(v0, v1, k)
    return pts[-1][1]


# ------------------------------------------------------------------------------------------------ shapes
def rotpt(v, r):
    """A point turned the way a geo cube's `rotation` turns it (file frame, y up): Rx first, then Ry, then Rz, right-handed, degrees."""
    x, y, z = v
    a, b, c = (math.radians(q) for q in r)
    y, z = y * math.cos(a) - z * math.sin(a), y * math.sin(a) + z * math.cos(a)
    x, z = x * math.cos(b) + z * math.sin(b), -x * math.sin(b) + z * math.cos(b)
    x, y = x * math.cos(c) - y * math.sin(c), x * math.sin(c) + y * math.cos(c)
    return (x, y, z)


def add(p, q):
    return tuple(a + b for a, b in zip(p, q))


def beam(c, bone, start, length, w, d, rot, paint, k=None, along="+y"):
    """A cube whose pivot is `start` and that runs `length` along +y (or -y) from it, turned by rot about that pivot. Returns the far end, so beams can be chained into horns, fingers and spikes."""
    sx, sy, sz = start
    sign = -1 if along == "-y" else 1
    org = (sx - w / 2, sy - length if sign < 0 else sy, sz - d / 2)
    c.box(bone, org, (w, length, d), paint, k=k, rot=rot if any(rot) else None, pivot=start)
    return add(start, rotpt((0, sign * length, 0), rot))


def curve(c, bone, start, lengths, widths, rots, paint, depth=None, k=None, along="+y"):
    """A bent horn / claw / tentacle tip: beams chained end to start; rots[i] is the (rx, ry, rz) of segment i (cumulative turns are the caller's to write)."""
    p = start
    for i, ln in enumerate(lengths):
        p = beam(c, bone, p, ln, widths[i], depth[i] if depth else widths[i], rots[i], paint, k=k, along=along)
    return p


def ring(c, bone, y, h, hw, hd, t, paint, k=1):
    """A hollow rectangular band (a crown, a collar, a belt) around the origin: half width hw, half depth hd, wall thickness t."""
    c.box(bone, (-hw, y, -hd - t), (2 * hw, h, t), paint, k=k)
    c.box(bone, (-hw, y, hd), (2 * hw, h, t), paint, k=k)
    c.box(bone, (-hw - t, y, -hd), (t, h, 2 * hd), paint, k=k)
    c.box(bone, (hw, y, -hd), (t, h, 2 * hd), paint, k=k)


def spike(c, bone, base, h, w, paint, rot=(0, 0, 0), n=3, taper=0.55, k=1):
    """A tapering spike of n stacked beams (horn, thorn, crown point) standing on `base`, turned by rot about it."""
    p = base
    ln = h / n
    wd = w
    for i in range(n):
        p = beam(c, bone, p, ln, wd, wd, rot, paint, k=k)
        wd *= taper
    return p


def humanoid_motion(extra=None, stride=26, arm_swing=20, hover=0.0, lean=0.0, slouch=0.0, cast_arms=(-150, -150), attack_hand="r", legs_still=False, bob=0.5):
    """The poses of the player-shaped body for every clip, plus an optional extra(clip, t, L) that adds to them (a look's own gestures). Angles in degrees, file frame (see rotpt):
    +x swings a hanging limb forward, +z swings it towards +x."""
    ca = (abs(cast_arms[0]), abs(cast_arms[1]))                                    # +x raises an arm forward and up: a cast always lifts the arms

    def motion(clip, t, L):
        P = {}

        def put(b, kind, v):
            d = P.setdefault(b, {})
            cur = d.get(kind, [0.0, 0.0, 0.0] if kind != "scale" else [1.0, 1.0, 1.0])
            d[kind] = [a * b2 for a, b2 in zip(cur, v)] if kind == "scale" else [a + b2 for a, b2 in zip(cur, v)]
        ph = TAU * t / L
        if clip == "idle":
            put("body", "rot", (1.2 * math.sin(ph) + lean, 0, 0))
            put("head", "rot", (1.5 * math.sin(ph + 1) - slouch, 2.5 * math.sin(ph), 0))
            put("arm_r", "rot", (2.5 * math.sin(ph + 0.5), 0, -3 + 1.5 * math.sin(ph)))
            put("arm_l", "rot", (-2.5 * math.sin(ph + 0.5), 0, 3 - 1.5 * math.sin(ph)))
            put("root", "pos", (0, hover + bob * 0.6 * math.sin(2 * ph), 0))
        elif clip in ("walk", "run"):
            a = stride * (1.0 if clip == "walk" else 1.45)
            sw = math.sin(ph)
            if not legs_still:
                put("leg_l", "rot", (a * sw, 0, 0))
                put("leg_r", "rot", (-a * sw, 0, 0))
            put("arm_l", "rot", (-arm_swing * sw * (1 if clip == "walk" else 1.5), 0, 4))
            put("arm_r", "rot", (arm_swing * sw * (1 if clip == "walk" else 1.5), 0, -4))
            put("body", "rot", (lean + (3 if clip == "walk" else 12), 4 * sw, 0))
            put("head", "rot", (-(3 if clip == "walk" else 8) - slouch, -4 * sw, 0))
            put("root", "pos", (0, hover + bob * abs(math.sin(ph)) * 1.4, 0))
        elif clip == "attack":
            k = t / L
            ang = keys(k, [(0.0, 0.0), (0.24, 165.0), (0.42, 35.0), (0.7, 8.0), (1.0, 0.0)])
            dip = keys(k, [(0.0, 0.0), (0.24, -8.0), (0.42, 14.0), (0.7, 3.0), (1.0, 0.0)])
            arm = "arm_r" if attack_hand == "r" else "arm_l"
            other = "arm_l" if attack_hand == "r" else "arm_r"
            put(arm, "rot", (ang, 0, -6 if attack_hand == "r" else 6))
            put(other, "rot", (ang * 0.3, 0, 0))
            put("body", "rot", (dip + lean, (-1 if attack_hand == "r" else 1) * dip * 0.8, 0))
            put("head", "rot", (-dip * 0.5, 0, 0))
            put("root", "pos", (0, hover, -1.5 * max(0.0, dip) / 14.0))
        elif clip == "cast":
            s = 0.5 + 0.5 * math.sin(ph)
            put("arm_r", "rot", (ca[0] * 0.55 - 8 * s, 0, -14))
            put("arm_l", "rot", (ca[1] * 0.55 - 8 * s, 0, 14))
            put("head", "rot", (-10 + 3 * s, 0, 0))
            put("body", "rot", (-4, 0, 0))
            put("root", "pos", (0, hover + 0.8 * s, 0))
        elif clip == "cast_big":
            k = t / L
            up = keys(k, [(0.0, 0.0), (0.35, 1.0), (0.8, 1.15), (0.9, -0.1), (1.0, 0.0)])
            put("arm_r", "rot", (ca[0] * up, 0, -22 * up))
            put("arm_l", "rot", (ca[1] * up, 0, 22 * up))
            put("head", "rot", (-22 * up, 0, 0))
            put("body", "rot", (-12 * up, 0, 0))
            put("root", "pos", (0, hover + 3.0 * max(0.0, up), 0))
        elif clip == "hurt":
            k = t / L
            h = math.sin(math.pi * k)
            put("body", "rot", (-14 * h, 0, 0))
            put("head", "rot", (-18 * h, 0, 0))
            put("arm_r", "rot", (-25 * h, 0, -10 * h))
            put("arm_l", "rot", (-25 * h, 0, 10 * h))
            put("root", "pos", (0, hover, 1.2 * h))
        elif clip == "death":
            k = t / L
            f = smooth(k / 0.7)
            put("root", "rot", (95 * f, 0, 0))                                      # falls over backwards
            put("root", "pos", (0, hover * (1 - f), 0))
            put("arm_r", "rot", (-120 * f, 0, -30 * f))
            put("arm_l", "rot", (-100 * f, 0, 30 * f))
            put("head", "rot", (-20 * f, 0, 0))
        if extra:
            for b, items in extra(clip, t, L).items():
                for kind, v in items.items():
                    put(b, kind, v)
        return P
    return motion


def bat_wing(c, parent, name, pivot, side, span, bone_paint, mem_paint, lift=18.0, droop=34.0, depth=0.55, sweep=34.0):
    """A leathery wing for one side (side -1 = the model's right, +1 = left): a root bone `name` (shoulder: the arm and the inner membrane) and `name`2 (the wrist: the outer arm, the three
    fingers and the outer membrane), so the two halves can flap one after the other. span = the length of the whole wing in pixels. Returns the two bone names. The rest pose is spread out
    to the side and a little up, the membrane hanging below the leading edge in strips with a scalloped trailing edge."""
    px, py, pz = pivot
    c.bone(name, parent, pivot=pivot, rotation=(0, -side * sweep, 0))
    inner, outer = span * 0.42, span * 0.58
    # the inner arm
    ay = math.radians(lift)
    ex, ey = px + side * inner * math.cos(ay), py + inner * math.sin(ay)
    c.box(name, (min(px, ex) - 0.2, py - 0.6, pz - 0.6), (abs(ex - px) + 0.4, 1.2, 1.2), bone_paint, k=1, rot=(0, 0, side * lift), pivot=pivot)
    c.bone(name + "2", name, pivot=(ex, ey, pz))
    c.box(name + "2", (min(ex, ex + side * outer * 0.8) - 0.2, ey - 0.5, pz - 0.5), (outer * 0.8 + 0.4, 1.0, 1.0), bone_paint, k=1, rot=(0, 0, side * (lift - 8)), pivot=(ex, ey, pz))
    wx, wy = ex + side * outer * 0.8 * math.cos(math.radians(lift - 8)), ey + outer * 0.8 * math.sin(math.radians(lift - 8))
    for ang, ln in ((24, outer * 0.62), (52, outer * 0.74), (80, outer * 0.62)):
        c.box(name + "2", (wx - 0.4, wy - ln, pz - 0.4), (0.8, ln, 0.8), bone_paint, k=1, rot=(0, 0, side * ang), pivot=(wx, wy, pz))
    # the membrane in strips from the arm line down to a scalloped edge
    rows = 7
    for r in range(rows):
        y_top = py + (inner * math.sin(ay)) * (1 - r / rows) - r * (droop / rows) * 0.9
        width = inner + outer * (1 - (r / rows) ** 1.4) * 0.95
        x0 = px + side * 0.8
        x1 = px + side * width
        c.box(name if r < 3 else name + "2", (min(x0, x1), y_top - droop / rows, pz - depth / 2), (abs(x1 - x0), droop / rows + 0.2, depth), mem_paint, k=1)
    return [name, name + "2"]


def feather_wing(c, parent, name, pivot, side, span, paint, paint2=None, rows=5, height=26.0, tilt=14.0, sweep=32.0, raise_=30.0):
    """An angel's wing for one side: a bone arm along the top and `rows` tiers of feathers of falling length, the longest at the bottom; `name`2 is the outer half so it can fold."""
    px, py, pz = pivot
    paint2 = paint2 or paint
    c.bone(name, parent, pivot=pivot, rotation=(0, -side * sweep, side * raise_))
    c.bone(name + "2", name, pivot=(px + side * span * 0.45, py + span * 0.1, pz))
    for r in range(rows):
        ln = span * (0.5 + 0.5 * r / max(1, rows - 1))
        y = py + span * 0.1 - r * (height / rows)
        bn = name if r < 2 else name + "2"
        x0 = px + side * 1.0
        x1 = px + side * ln
        c.box(bn, (min(x0, x1), y - height / rows, pz - 0.35), (abs(x1 - x0), height / rows + 0.3, 0.7), paint if r % 2 == 0 else paint2, k=1, rot=(0, 0, side * (tilt * (1 - r / rows))), pivot=pivot)
    c.box(name, (min(px, px + side * span * 0.55), py + span * 0.1 - 0.7, pz - 0.7), (span * 0.55, 1.4, 1.4), paint2, k=1)
    return [name, name + "2"]


def tilt_rot(dx, dz, tilt):
    """The (rx, ry, rz) that tip a cube standing along +y over towards the horizontal direction (dx, dz) by `tilt` degrees (a horn leaning outward), for beam() and spike()."""
    n = math.hypot(dx, dz) or 1.0
    dx, dz = dx / n, dz / n
    s = math.sin(math.radians(tilt))
    a = math.asin(max(-1, min(1, dz * s)))
    b = -math.asin(max(-1, min(1, dx * s / max(1e-6, math.cos(a)))))
    return (math.degrees(a), 0.0, math.degrees(b))


def horn(c, bone, base, dx, dz, lengths, widths, tilts, paint, k=1):
    """A curved horn: segments leaning outward towards (dx, dz) by the successive angles `tilts` (degrees from vertical), end to start. Returns the tip."""
    p = base
    for ln, wd, tl in zip(lengths, widths, tilts):
        p = beam(c, bone, p, ln, wd, wd, tilt_rot(dx, dz, tl), paint, k=k)
    return p


def tail(c, parent, name, start, lengths, widths, paint, axis="+z", curl=(-6, 0, 0), tip=None):
    """A tail chain from `start` backwards (+z) with a rest curl per segment; tip = a paint for the last segment."""
    return c.chain(name, parent, start, axis, lengths, widths, paint, curl=curl, tip_paint=tip)

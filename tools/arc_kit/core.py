"""Why: the pixel-art primitives every part of the art kit paints with: a per-face texture packer, a small pixel canvas, hard-edged palettes and the
material painters (cloth, leather, plate, chain, fur, bone, bandage, scales, hair, skin, feathers) plus the keyframe helpers of the clips.

These are the techniques of the Lucius generator (tools/zogratis_lucius_kit.py), copied and extended here so the kit owns its code and the Zogratis
files stay untouched. Nothing in this module writes files by itself.

    pk = Pack(512, 512, dens=2)                              # 2 texels per model pixel on every face unless a box says otherwise
    pk.box(m, "torso", (-4, 11, -2.5), (8, 12, 5), paint)    # paint(face, i, j, fw, fh) -> (rgba | None, glow rgba | None)
    pk.finish()

Faces: "top" "bottom" "front" (-z) "back" (+z) "left" (+x, the model's left) "right" (-x). With box uv a face is laid out exactly like vanilla, so i = 0 is
the x-min edge of the front / top / bottom, the x-max edge of the back, the BACK edge of the right face and the FRONT edge of the left face; j = 0 is the
top edge of the side faces and the BACK edge of top / bottom. On screen (front view) the viewer's left is the model's RIGHT (-x): i = 0 of the front face.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)
from spirit_geo import Sheet, nz, fz, hsh, rgba  # noqa: E402

SIN, COS, TAU = math.sin, math.cos, 2 * math.pi
FACES = ("top", "bottom", "front", "back", "left", "right")
GEO_FACE = {"front": "north", "back": "south", "right": "east", "left": "west", "top": "up", "bottom": "down"}
SIDES = ("front", "back", "left", "right")
# shading baked into the palette index of a face (pixel art: the face decides the tone, not a multiplier): the top is lit, the bottom is dark
FACE_T = {"top": 0.14, "bottom": -0.30, "front": 0.02, "back": -0.14, "left": -0.16, "right": -0.07}
BAYER = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))
TINY = 0.01


# ------------------------------------------------------------------------------------------------------------- colour helpers
def A3(c):
    return np.array(c[:3], np.float32)


def clamp(v, lo=0.0, hi=1.0):
    return lo if v < lo else hi if v > hi else v


def smooth(a, b, x):
    t = clamp((x - a) / (b - a)) if b != a else (1.0 if x >= a else 0.0)
    return t * t * (3 - 2 * t)


def ramp(cols, t):
    """Smooth colour along a list of colours, t in 0..1."""
    t = clamp(t) * (len(cols) - 1)
    i = min(int(t), len(cols) - 2)
    f = t - i
    return A3(cols[i]) * (1 - f) + A3(cols[i + 1]) * f


def pal(cols, t, i=0, j=0, dither=0.0):
    """Hard-edged colour: the palette entry t (0..1) falls on, optionally ordered-dithered (the crisp pixel-art way to a band edge)."""
    n = len(cols)
    t = t + (((BAYER[j & 3][i & 3] + 0.5) / 16.0) - 0.5) * dither
    return cols[min(n - 1, max(0, int(clamp(t) * n)))]


def mix(a, b, t):
    return tuple(a[k] * (1 - t) + b[k] * t for k in range(3))


def px(c, a=255):
    return rgba(c, a)


def r5(v):
    """Round to the nearest half pixel."""
    return round(v * 2) / 2.0


def lerp(a, b, t):
    return a + (b - a) * t


# ------------------------------------------------------------------------------------------------------------- the packer
class Pack(Sheet):
    """Boxes are collected, then every face gets its own texel rectangle (sorted tallest first) and is painted at finish(): a box of size (X, Y, Z) model
    pixels takes d*X x d*Y texels on its front / back, d*Z x d*Y on its sides and d*X x d*Z on its top / bottom, d = the density of that face (texels
    per pixel). The cubes are written with an explicit per-face uv; a face that is not asked for has no uv, so the renderer does not draw it.
    Boxes with the same `key` (and the same size / density / faces) share one painted rectangle (feathers, tufts, spokes); twins are mirrored copies."""

    def __init__(self, w, h, dens=2):
        super().__init__(w, h)
        self.dens = dens
        self.q = []
        self.links = []
        self.templates = {}
        self.placed = []

    def _dd(self, dens):
        if isinstance(dens, dict):
            return {f: int(dens.get(f, self.dens)) for f in FACES}
        return {f: int(dens or self.dens) for f in FACES}

    def box(self, m, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None, mirror=False, faces=None, dens=None, key=None):
        g = 1.0 / self.dens
        size = tuple(max(g, round(v / g) * g) for v in size)
        faces = tuple(faces) if faces else FACES
        c = m.cube(bone, tuple(origin), size, uv=(0, 0), inflate=inflate, rotation=rotation, pivot=pivot, mirror=mirror)
        sig = (size, tuple(sorted(self._dd(dens).items())), faces)
        if key is not None:
            tk = (key, sig)
            if tk in self.templates:
                self.links.append((c, self.templates[tk]))
                return c
            self.templates[tk] = c
        self.q.append((c, size, self._dd(dens), faces, paint))
        return c

    def twin(self, m, bone, c):
        """The mirrored copy of a cube on the other side of the model (x -> -x), sharing its painted rectangles."""
        sx = c["size"][0]
        o = c["origin"]
        rot = c.get("rotation")
        piv = c.get("pivot")
        t = m.cube(bone, (-(o[0] + sx), o[1], o[2]), tuple(c["size"]), uv=(0, 0), inflate=c.get("inflate", 0.0),
                   rotation=None if not rot else (rot[0], -rot[1], -rot[2]), pivot=None if not rot else (-piv[0], piv[1], piv[2]), mirror=True)
        self.links.append((t, c))
        return t

    @staticmethod
    def face_dims(face, size, d):
        X, Y, Z = size
        if face in ("front", "back"):
            return int(round(X * d)), int(round(Y * d))
        if face in ("left", "right"):
            return int(round(Z * d)), int(round(Y * d))
        return int(round(X * d)), int(round(Z * d))

    def finish(self):
        jobs = []
        for qi, (c, size, dd, faces, paint) in enumerate(self.q):
            for f in faces:
                fw, fh = self.face_dims(f, size, dd[f])
                jobs.append((qi, f, fw, fh))
        jobs.sort(key=lambda t: (-t[3], -t[2], t[0]))
        rect = {}
        for (qi, f, fw, fh) in jobs:
            u, v = self.alloc(fw, fh)
            rect[(qi, f)] = (u, v, fw, fh)
        for qi, (c, size, dd, faces, paint) in enumerate(self.q):
            uv = {}
            for f in faces:
                u, v, fw, fh = rect[(qi, f)]
                uv[GEO_FACE[f]] = {"uv": [u, v], "uv_size": [fw, fh]}
            c["uv"] = uv
        for (c, src) in self.links:
            c["uv"] = json.loads(json.dumps(src["uv"]))
        self.placed = [(qi, rect) for qi in range(len(self.q))]
        self.paint_all(rect)

    def paint_all(self, rect=None):
        if rect is None:
            rect = self.placed[0][1] if self.placed else {}
        self.img[:] = 0
        self.glow[:] = 0
        for qi, (c, size, dd, faces, paint) in enumerate(self.q):
            for f in faces:
                u, v, fw, fh = rect[(qi, f)]
                for j in range(fh):
                    for i in range(fw):
                        cc, g = paint(f, i, j, fw, fh)
                        self._put(u + i, v + j, cc, g)

    def height_used(self):
        return self.y + self.row


# ------------------------------------------------------------------------------------------------------------- a pixel canvas
class Canvas:
    """A w x h pixel picture with a colour layer and a glow layer, drawn with a few primitives (inclusive pixel coordinates); `at(i, j)` reads a texel."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.c = np.zeros((h, w, 4), np.float32)
        self.g = np.zeros((h, w, 4), np.float32)

    def put(self, x, y, col, a=255, glow=None, ga=255):
        x, y = int(x), int(y)
        if 0 <= x < self.w and 0 <= y < self.h:
            if col is not None:
                self.c[y, x] = (col[0], col[1], col[2], a)
            if glow is not None:
                self.g[y, x] = (glow[0], glow[1], glow[2], ga)

    def clear(self, x, y):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.c[y, x] = 0
            self.g[y, x] = 0

    def has(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h and self.c[int(y), int(x), 3] > 0

    def rect(self, x0, y0, x1, y1, col, a=255, glow=None, ga=255):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.put(x, y, col, a, glow, ga)

    def line(self, x0, y0, x1, y1, col, a=255, glow=None, ga=255, width=1):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for k in range(n + 1):
            t = k / max(1, n)
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for oy in range(width):
                for ox in range(width):
                    self.put(round(x) + ox, round(y) + oy, col, a, glow, ga)

    def poly(self, pts, col, a=255, glow=None, ga=255):
        """Fill a polygon (pixel centres inside it, even-odd)."""
        ys = [p[1] for p in pts]
        for y in range(max(0, int(math.floor(min(ys)))), min(self.h, int(math.ceil(max(ys))) + 1)):
            yc = y + 0.5
            xs = []
            for k in range(len(pts)):
                (xa, ya), (xb, yb) = pts[k], pts[(k + 1) % len(pts)]
                if (ya <= yc < yb) or (yb <= yc < ya):
                    xs.append(xa + (yc - ya) / (yb - ya) * (xb - xa))
            xs.sort()
            for k in range(0, len(xs) - 1, 2):
                for x in range(int(math.floor(xs[k] - 0.5 + 1)), int(math.floor(xs[k + 1] - 0.5)) + 1):
                    self.put(x, y, col, a, glow, ga)

    def ellipse(self, cx, cy, rx, ry, col, a=255, glow=None, ga=255):
        for y in range(int(cy - ry - 1), int(cy + ry + 2)):
            for x in range(int(cx - rx - 1), int(cx + rx + 2)):
                if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1.0:
                    self.put(x, y, col, a, glow, ga)

    def at(self, i, j):
        if not (0 <= i < self.w and 0 <= j < self.h):
            return None, None
        c, g = self.c[j, i], self.g[j, i]
        return ((int(c[0]), int(c[1]), int(c[2]), int(c[3])) if c[3] > 0 else None,
                (int(g[0]), int(g[1]), int(g[2]), int(g[3])) if g[3] > 0 else None)

    def image(self, scale=1):
        from PIL import Image
        im = Image.fromarray(np.clip(self.c, 0, 255).astype(np.uint8), "RGBA")
        return im.resize((self.w * scale, self.h * scale), Image.NEAREST) if scale != 1 else im


def canvas_paint(canvas, face_name, fallback):
    """A painter that reads `canvas` on one face (the texel at i, j) and calls `fallback` on every other face (and on transparent canvas texels when
    the canvas is drawn over a base: pass `fallback` as the base painter and leave the canvas texels you want to keep as they are)."""
    def p(face, i, j, fw, fh):
        if face == face_name:
            c, g = canvas.at(i, j)
            if c is not None or g is not None:
                if c is None:
                    base = fallback(face, i, j, fw, fh)
                    return base[0], g
                return c, g
        return fallback(face, i, j, fw, fh)
    return p


def decal_paint(decals, fallback):
    """canvas_paint for several faces at once: decals = {face: Canvas}. A transparent texel of a decal shows the base painter."""
    def p(face, i, j, fw, fh):
        cv = decals.get(face)
        if cv is not None:
            c, g = cv.at(i, j)
            if c is not None or g is not None:
                if c is None:
                    base = fallback(face, i, j, fw, fh)
                    return base[0], g
                return c, g
        return fallback(face, i, j, fw, fh)
    return p


def canvas_plate(canvas):
    """A painter for a thin plate: the canvas on the front face and, mirrored, on the back face (so it reads right from behind); other faces stay clear."""
    def p(face, i, j, fw, fh):
        if face == "front":
            return canvas.at(i, j)
        if face == "back":
            return canvas.at(fw - 1 - i, j)
        return None, None
    return p


def over(base, top, faces=None):
    """The painter `top` over `base`: a texel where top returns no colour shows base (the glow of top is kept). faces limits where top applies."""
    def p(face, i, j, fw, fh):
        if faces is None or face in faces:
            c, g = top(face, i, j, fw, fh)
            if c is not None:
                return c, g
            if g is not None:
                return base(face, i, j, fw, fh)[0], g
        return base(face, i, j, fw, fh)
    return p


def band(top, base, j0=0.0, j1=1.0, faces=SIDES):
    """base everywhere except rows from fraction j0 to j1 of the face height, which show `top` (fractions 0..1 from the top edge of the face)."""
    def p(face, i, j, fw, fh):
        if face in faces and j0 * fh <= j < j1 * fh:
            return top(face, i, j, fw, fh)
        return base(face, i, j, fw, fh)
    return p


def band_px(top, base, from_top=None, from_bottom=None, rows=None, faces=SIDES):
    """base, with the first `from_top` texel rows (or the last `from_bottom`, or rows=(a, b) as texel rows) painted by `top`."""
    def p(face, i, j, fw, fh):
        if face in faces:
            if from_top is not None and j < from_top:
                return top(face, i, j, fw, fh)
            if from_bottom is not None and j >= fh - from_bottom:
                return top(face, i, j, fw, fh)
            if rows is not None and rows[0] <= j < rows[1]:
                return top(face, i, j, fw, fh)
        return base(face, i, j, fw, fh)
    return p


def only(painter, faces):
    """A painter that is transparent on every face outside `faces`."""
    def p(face, i, j, fw, fh):
        if face in faces:
            return painter(face, i, j, fw, fh)
        return None, None
    return p


# ------------------------------------------------------------------------------------------------------------- strand / folds
def strand_id(across, along, salt, width=2.0, wander=1.0):
    x = across / width + wander * SIN(along * 0.13 + across * 0.31 + salt) / width * 2 + 0.4 * SIN(along * 0.37 + salt * 2.3)
    sid = math.floor(x)
    return sid, x - sid


def edge_cut(face, i, j, fw, fh, edges, depth, salt, width=2.0, taper=1.5):
    """True when the texel is cut off by a ragged, spiky edge: edges is a string of 'b' (bottom), 't' (top), 'l' / 'r' (the i = 0 / i = fw - 1 side);
    depth = how many texels the strands reach in; every strand has its own length and a pointed tip."""
    for e in edges:
        if e in "bt":
            d = (fh - 1 - j) if e == "b" else j
            sid, frac = strand_id(i, 0, salt + (3 if e == "t" else 0), width, 0.0)
        else:
            d = (fw - 1 - i) if e == "r" else i
            sid, frac = strand_id(j, 0, salt + (5 if e == "l" else 7), width, 0.0)
        ln = depth * (0.25 + 0.75 * hsh(sid, salt, 11))
        ln -= abs(frac - 0.5) * taper * 2.0
        if d < depth - ln:
            return True
    return False


# ------------------------------------------------------------------------------------------------------------- material painters
def fabric(cols, salt=0, period=8.0, amp=0.26, tilt=0.0, patch=None, patch_thr=0.2, hem=0, hem_cols=None, lit=0.10, weave=0.025, dither=0.045, seam=True,
           cut="", cut_depth=4, noise=0.16, bias=0.0, cut_width=2.0, cut_taper=1.5):
    """Cloth in a hard-edged palette: vertical pleats (sinusoidal folds quantised into the palette bands), a darker lower edge, a faint weave, and optionally
    dark blotches (patch = a darker palette, patch_thr = how much of the surface they take), a hem band of `hem` texel rows, a ragged edge (cut = edges)."""
    n = len(cols)

    def p(face, i, j, fw, fh):
        if cut and face in SIDES and edge_cut(face, i, j, fw, fh, cut, cut_depth, salt, cut_width, cut_taper):
            return None, None
        v = j / max(1, fh - 1)
        f = SIN((i + tilt * j + 2.5 * SIN(j * 0.12 + salt)) * TAU / period + salt * 1.3)
        t = 0.52 + bias + amp * f * (0.7 + 0.3 * SIN(j * 0.07 + salt * 2)) - lit * v + nz(i * 0.35, j * 0.35, salt) * noise + FACE_T[face] * 0.5
        t += weave if (i + j) % 2 == 0 else -weave
        c = pal(cols, t, i, j, dither)
        if patch is not None and face in SIDES:
            b = nz(i * 0.16 + 3, j * 0.13, salt + 21) + 0.5 * nz(i * 0.4, j * 0.4, salt + 5)
            if b > patch_thr:
                c = pal(patch, t * 0.9 + 0.05, i, j, dither)
        if hem and j >= fh - hem and face in SIDES:
            c = pal(hem_cols or cols, 0.12 + 0.3 * (j == fh - hem) + 0.1 * hsh(i // 2, salt, 4), i, j, 0.0)
        if seam and j == 0 and face in SIDES:
            c = cols[0]
        return px(c), None
    return p


def leather(cols, salt=0, dither=0.05, seam=True, noise=0.16, bias=0.0):
    """Leather: fine grain, no pleats, a darker top and bottom edge and a faint stitch line one texel inside the lower edge."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.56 + bias + noise * nz(i * 0.5, j * 0.5, salt) + 0.07 * fz(i, j, salt) - 0.12 * v + FACE_T[face] * 0.5
        if seam and face in SIDES:
            if j == 0:
                t = 0.0
            elif j == fh - 2 and i % 3 != 1 and fh > 5:
                t += 0.22
            elif j == fh - 1:
                t = 0.05
        return px(pal(cols, t, i, j, dither)), None
    return p


def hair_paint(salt=1, cols=((8, 8, 12), (18, 18, 28), (34, 36, 56), (66, 72, 106)), strand=2.0, sheen=0.2):
    """Hair: strands of 2 to 3 texels, each with its own tone from the middle of the palette; a narrow dark gap every few strands, a pale sheen along the middle of some of
    them, a little darker at the tips. The darkest tone is only used for the gaps and the roots, so a big hair surface stays calm."""
    def p(face, i, j, fw, fh):
        sid, frac = strand_id(i, j, salt, strand * 1.3, 1.4)
        h = hsh(sid, salt, 3)
        v = j / max(1, fh - 1)
        t = 0.44 + 0.26 * h - 0.10 * v + FACE_T[face] * 0.5
        if frac < 0.16 and hsh(sid, salt, 9) > 0.35:
            t -= 0.22
        elif 0.45 < frac < 0.72 and h > 0.45:
            t += sheen * (0.4 + 0.5 * h) * (1 - 0.5 * v)
        return px(pal(cols, t, i, j, 0.02)), None
    return p


def skin_paint(cols=((176, 136, 140), (214, 176, 172), (238, 208, 198), (250, 230, 220)), salt=0):
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.62 + 0.05 * nz(i * 0.3, j * 0.3, salt) - 0.18 * v + FACE_T[face] * 0.6
        return px(pal(cols, t, i, j, 0.0)), None
    return p


def solid(c, g=None, noise=0.0, salt=0):
    c = A3(c)

    def p(face, i, j, fw, fh):
        k = 1.0 + FACE_T[face] * 0.5
        return px(c * k + fz(i, j, salt) * noise), (px(g[:3], g[3]) if g else None)
    return p


def metal(cols, salt=0, glow=None, band=True, emboss=True, rim=True, lift=0.0):
    """Gold or steel in a hard-edged palette: lit top rows, dark bottom rows, a bright rim line, engraved dashes; glow = (r, g, b, a) for the glow layer."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.78 - 0.55 * v + FACE_T[face] * 0.5 + 0.06 * fz(i, j, salt) + lift
        if face in ("top",):
            t = 0.92
        if rim and (i == 0 or j == 0) and face in SIDES and fw > 3 and fh > 3:
            t += 0.15
        if rim and (i == fw - 1 or j == fh - 1) and face in SIDES and fw > 3 and fh > 3:
            t -= 0.2
        if emboss and fw >= 6 and fh >= 6 and face in SIDES and ((i % 6 == 2 and 2 <= j < fh - 2) or (j % 6 == 2 and 2 <= i < fw - 2)) and hsh(i // 6, j // 6, salt) > 0.55:
            t -= 0.18
        g = px(glow[:3], glow[3] * (0.55 + 0.45 * (1 - v))) if glow else None
        return px(pal(cols, t, i, j, 0.04)), g
    return p


def plate(cols, salt=0, rivets=True, glow=None, trim=None):
    """Plate armour: a lit upper edge, a dark lower edge, a brushed grain, a rim and rivets in the corners; trim = a second palette for a raised border."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.70 - 0.42 * v + FACE_T[face] * 0.5 + 0.05 * fz(i, j, salt) + 0.03 * ((i + j) % 2)
        c = pal(cols, t, i, j, 0.03)
        if face in SIDES and fw > 4 and fh > 4:
            if trim is not None and (i < 2 or j < 2 or i >= fw - 2 or j >= fh - 2):
                c = pal(trim, 0.75 - 0.4 * v + (0.15 if (i == 0 or j == 0) else 0.0), i, j, 0.0)
            elif i == 0 or j == 0:
                c = pal(cols, min(1.0, t + 0.25))
            elif i == fw - 1 or j == fh - 1:
                c = pal(cols, max(0.0, t - 0.3))
            if rivets and fw > 7 and fh > 7 and (i in (2, fw - 3)) and (j in (2, fh - 3)):
                c = pal(cols, 0.95)
        g = px(glow[:3], glow[3]) if glow and face in SIDES and (i == 1 or j == 1) and fw > 6 else None
        return px(c), g
    return p


def chain_paint(cols, salt=0):
    """Chain mail: a staggered ring pattern, each ring a lit top-left and a dark bottom-right texel."""
    def p(face, i, j, fw, fh):
        r = (j // 2) % 2
        a, b = (i + r * 2) % 4, j % 2
        t = 0.55 + 0.1 * nz(i * 0.4, j * 0.4, salt) + FACE_T[face] * 0.4
        if a in (0, 1) and b == 0:
            t += 0.25
        elif a in (2, 3) and b == 1:
            t -= 0.25
        elif a == 3 or (a == 1 and b == 1):
            t -= 0.1
        return px(pal(cols, t, i, j, 0.02)), None
    return p


def bone_paint(cols, salt=0):
    """Bone: pale, with a few darker cracks and a ridge line."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.72 - 0.3 * v + 0.14 * nz(i * 0.4, j * 0.3, salt) + FACE_T[face] * 0.5
        if hsh(i // 3, j // 5, salt) > 0.86 and (i + j) % 3 == 0:
            t -= 0.4
        return px(pal(cols, t, i, j, 0.04)), None
    return p


def bandage_paint(cols, salt=0, slope=1.0, gap=5.0):
    """Wrapped bandage cloth: diagonal wraps of the same width, each with a darker line where it overlaps the next and a lit upper edge; no noise, so the wraps read as
    bands (the slope is texels of shift per row, so the bands wind round the part)."""
    def p(face, i, j, fw, fh):
        k = (i + slope * j * 0.7) / gap
        f = k - math.floor(k)
        t = 0.60 + FACE_T[face] * 0.4
        if f < 0.14:
            t -= 0.34
        elif f > 0.82:
            t += 0.16
        elif f > 0.5:
            t -= 0.04
        return px(pal(cols, t, i, j, 0.0)), None
    return p


def scale_paint(cols, salt=0, size=3):
    """Overlapping scales: rows of rounded scales, each lit on top and dark under its lower edge."""
    def p(face, i, j, fw, fh):
        r = j // size
        x = (i + (size // 2 if r % 2 else 0)) % (size * 2 // 2 + 1)
        u, v = x - size / 2.0, (j % size) - 0.5
        d = math.hypot(u, v * 1.2)
        t = 0.66 + 0.08 * nz(i * 0.5, j * 0.5, salt) + FACE_T[face] * 0.4 - 0.1 * (j % size) / size
        if (j % size) == size - 1:
            t -= 0.35
        elif (j % size) == 0:
            t += 0.12
        return px(pal(cols, t, i, j, 0.03)), None
    return p


def fur(cols, salt=1, flow="y", root=0.5, strand=2.0, wander=1.0, lift=0.0, cut=None, cut_depth=5, taper=1.5, dither=0.06, tip=0.3, faces_cut=SIDES + ("top",)):
    """Fur in a hard-edged palette: strands that wander, each with its own tone, a dark gap between neighbours, dark at the root, pale at the tip.
    flow 'y': strands run down the face (along = j); 'x': along i (the top of a shoulder slab). cut: edges string for edge_cut (ragged spiky tips)."""
    def p(face, i, j, fw, fh):
        if cut and face in faces_cut and edge_cut(face, i, j, fw, fh, cut, cut_depth, salt, strand, taper):
            return None, None
        across, along = (i, j) if flow == "y" else (j, i)
        nal = fh if flow == "y" else fw
        sid, frac = strand_id(across, along, salt, strand, wander)
        h = hsh(sid, salt, 3)
        v = along / max(1, nal - 1)
        t = 0.55 + 0.28 * h + 0.06 * SIN(along * 0.35 + h * 9) + lift + tip * v * v * 0.4
        if frac < 0.3:
            t -= 0.13
        elif frac > 0.78:
            t += 0.05
        if hsh(sid, along // 3, salt + 17) > 0.9:
            t += 0.18
        t += nz(i * 0.2, j * 0.2, salt) * 0.2 - root * (1 - v) ** 2 * 0.55 + FACE_T[face] * 0.6
        return px(pal(cols, t, i, j, dither)), None
    return p


def feather(cols, tip_glow=None, salt=0, length_axis="j", shaft=True, round_tip=True, tip_frac=0.22, dither=0.05, barbs=True):
    """A feather plate face: a rounded tip, a lighter shaft down the middle, slanted barb lines, pale at the tip; transparent outside the vane.
    length_axis 'j': the feather runs along j (the tip at j = fh - 1); tip_glow (r, g, b, a) puts a glow on the last tip_frac."""
    def p(face, i, j, fw, fh):
        if length_axis == "j":
            a, b, na, nb = i, j, fw, fh
        else:
            a, b, na, nb = j, i, fh, fw
        cx = (na - 1) / 2.0
        u = (a - cx) / max(0.5, na / 2.0)
        s = b / max(1, nb - 1)
        w = 1.0 - 0.18 * s
        if round_tip and s > 0.7:
            w *= math.sqrt(max(0.0, 1.0 - ((s - 0.7) / 0.3) ** 2 * 0.95))
        if abs(u) > w + 0.02:
            return None, None
        t = 0.62 + 0.13 * s - 0.28 * abs(u) ** 1.5
        if barbs:
            t += 0.07 * (1 if ((b + int(a * 0.8 * (1 if u > 0 else -1))) % 4 < 1) else 0)
            t -= 0.07 * (1 if ((b * 2 + int(abs(a - cx) * 1.5)) % 7 == 0) else 0)
        if shaft and abs(u) < 0.14:
            t += 0.22
        if abs(u) > w - 0.12:
            t -= 0.1
        t += FACE_T[face] * 0.3 + 0.05 * fz(i, j, salt)
        g = None
        if tip_glow and s > 1 - tip_frac:
            g = px(tip_glow[:3], tip_glow[3] * ((s - (1 - tip_frac)) / tip_frac) ** 1.2)
        return px(pal(cols, t, i, j, dither)), g
    return p


def clump(cols, salt=0, flip=False, strands=3.4, root=0.6, curl=0.0, tip_cols=None, glow=None, dither=0.05, split=0.0):
    """One clump of fur / hair as a flat plate face: a flame shape that is widest a quarter of the way down and ends in a point, strands that fan out from
    the root and meet at the tip, dark roots and pale tips. flip: the root is at the bottom row (a standing clump). curl bends the tip sideways (a fraction
    of the width); split cuts a notch into the tip. Transparent outside the shape."""
    def p(face, i, j, fw, fh):
        s = (fh - 1 - j) / max(1, fh - 1) if flip else j / max(1, fh - 1)
        cx = (fw - 1) / 2.0 + curl * fw * 0.5 * s * s
        grow = smooth(0.0, 0.3, s)
        w = (0.55 + 0.45 * grow) * (1.0 - smooth(0.3, 1.0, s) ** 1.35)
        half = max(0.5, (fw / 2.0) * w)
        u = (i - cx) / half
        if abs(u) > 1.0 + 1e-6 and not (abs(i - cx) <= 0.5 and s < 1.0):
            return None, None
        if split and s > 0.72 and abs(i - cx) < 0.6 and s < 0.9:
            return None, None
        k = (u + 1.0) * 0.5 * strands
        sid = math.floor(k)
        frac = k - sid
        h = hsh(sid, salt, 3)
        t = 0.5 + 0.3 * h + 0.35 * s * (1 - 0.4 * h) - root * (1 - s) ** 1.6 * 0.9
        if frac < 0.22 or abs(u) > 0.86:
            t -= 0.14
        elif 0.45 < frac < 0.62:
            t += 0.07
        t += FACE_T[face] * 0.3
        cc = (tip_cols if (tip_cols and s > 0.82) else cols)
        g = None
        if glow and s > 0.7:
            g = px(glow[:3], glow[3] * ((s - 0.7) / 0.3) ** 1.3)
        return px(pal(cc, t, i, j, dither)), g
    return p


def blade_paint(cols, edge=None, glow=None, salt=0, fuller=True, edge_side="front"):
    """A blade or any long flat steel part: a bright edge line, a fuller (groove) down the middle, a darker spine; `edge` = (r, g, b) tints the edge,
    `glow` = (r, g, b, a) glows along the edge. The length runs along j (the tip at the bottom row j = fh - 1 is NOT assumed: rotate the cube instead)."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        u = (i + 0.5) / max(1, fw)
        t = 0.62 + FACE_T[face] * 0.5 + 0.05 * fz(i, j, salt)
        if face in ("front", "back") and fw >= 3:
            if u < 0.18 or u > 0.82:
                t += 0.3
            elif fuller and 0.4 < u < 0.6:
                t -= 0.16
            else:
                t += 0.04
        c = pal(cols, t, i, j, 0.02)
        g = None
        if edge is not None and face in ("front", "back") and (u < 0.16 or u > 0.84):
            c = tuple(edge)
        if glow is not None and face in ("front", "back") and (u < 0.2 or u > 0.8):
            g = px(glow[:3], glow[3])
        elif glow is not None and face in ("left", "right", "top", "bottom"):
            g = px(glow[:3], glow[3] * 0.7)
        return px(c), g
    return p


# ------------------------------------------------------------------------------------------------------------- animation helpers
def cyc(a, clip, bone, kind, L, fn, n=1, steps=16):
    """A looping track: fn(theta) -> (x, y, z) sampled steps times over the clip length L, with n repeats of theta in 0..TAU."""
    frames = {}
    for k in range(steps + 1):
        frames[L * k / steps] = tuple(round(v, 3) for v in fn(TAU * n * k / steps))
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def swing(base, amp, ph=0.0):
    """base + amp * (sin(theta + ph) - sin(ph)) per axis: equal to base at theta = 0, so a loop starts on the neutral pose."""
    return lambda th: tuple(base[k] + amp[k] * (SIN(th + ph) - SIN(ph)) for k in range(3))


def catmull(pts, t):
    """Catmull-Rom interpolation through (time, (x, y, z)) keys, clamped at both ends."""
    if t <= pts[0][0]:
        return tuple(pts[0][1])
    if t >= pts[-1][0]:
        return tuple(pts[-1][1])
    k = 0
    while k + 2 < len(pts) and t >= pts[k + 1][0]:
        k += 1
    (t1, p1), (t2, p2) = pts[k], pts[k + 1]
    p0 = pts[k - 1][1] if k > 0 else p1
    p3 = pts[k + 2][1] if k + 2 < len(pts) else p2
    u = (t - t1) / (t2 - t1)
    out = []
    for d in range(3):
        m1 = 0.5 * (p2[d] - p0[d])
        m2 = 0.5 * (p3[d] - p1[d])
        u2, u3 = u * u, u * u * u
        out.append((2 * u3 - 3 * u2 + 1) * p1[d] + (u3 - 2 * u2 + u) * m1 + (-2 * u3 + 3 * u2) * p2[d] + (u3 - u2) * m2)
    return tuple(out)

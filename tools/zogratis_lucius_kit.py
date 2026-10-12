"""Shared kit of tools/gen_zogratis_lucius.py: a per-face texture packer, a small pixel canvas, material painters and animation helpers for the
GeoLite pipeline (tools/geo_builder.py -> client.geo.GeoDraw). Nothing here writes files by itself.

    pk = Pack(512, 512, dens=2)                              # 2 texels per model pixel on every face unless a box says otherwise
    pk.box(m, "torso", (-4, 11, -2.5), (8, 12, 5), paint)    # paint(face, i, j, fw, fh) -> (rgba | None, glow rgba | None) like tools/spirit_geo.Sheet
    pk.box(..., dens={"front": 4}, faces=("front", "left"), key="tuft")   # a 4x front face, only two faces drawn, a uv shared by every box of that key
    pk.finish()

Faces: "top" "bottom" "front" (-z) "back" (+z) "left" (+x, the model's left) "right" (-x). With box uv a face is laid out exactly like vanilla, so i = 0 is
the x-min edge of the front / top / bottom, the x-max edge of the back, the BACK edge of the right face and the FRONT edge of the left face; j = 0 is the
top edge of the side faces and the BACK edge of top / bottom (the same orientation as tools/gen_dungeon_mobs.py).
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from spirit_geo import Sheet, nz, fz, hsh, rgba  # noqa: E402

SIN, COS, TAU = math.sin, math.cos, 2 * math.pi
FACES = ("top", "bottom", "front", "back", "left", "right")
GEO_FACE = {"front": "north", "back": "south", "right": "east", "left": "west", "top": "up", "bottom": "down"}
SIDES = ("front", "back", "left", "right")
# shading baked into the palette index of a face (pixel art: the face decides the tone, not a multiplier): the top is lit, the bottom is dark
FACE_T = {"top": 0.13, "bottom": -0.30, "front": 0.0, "back": -0.10, "left": -0.03, "right": -0.03}
BAYER = ((0, 8, 2, 10), (12, 4, 14, 6), (3, 11, 1, 9), (15, 7, 13, 5))


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


def canvas_plate(canvas):
    """A painter for a thin plate: the canvas on the front face and, mirrored, on the back face (so it reads right from behind); other faces stay clear."""
    def p(face, i, j, fw, fh):
        if face == "front":
            return canvas.at(i, j)
        if face == "back":
            return canvas.at(fw - 1 - i, j)
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
def fabric(cols, salt=0, period=8.0, amp=0.26, tilt=0.0, patch=None, patch_thr=0.2, hem=0, hem_cols=None, lit=0.10, weave=0.025, dither=0.045, seam=True):
    """Cloth in a hard-edged palette: vertical pleats (sinusoidal folds quantised into the palette bands), a darker lower edge, a faint weave, and optionally
    dark blotches (patch = a darker palette, patch_thr = how much of the surface they take)."""
    n = len(cols)

    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        f = SIN((i + tilt * j + 2.5 * SIN(j * 0.12 + salt)) * TAU / period + salt * 1.3)
        t = 0.52 + amp * f * (0.7 + 0.3 * SIN(j * 0.07 + salt * 2)) - lit * v + nz(i * 0.35, j * 0.35, salt) * 0.16 + FACE_T[face] * 0.5
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


def hair(salt=1, cols=((8, 8, 12), (18, 18, 28), (34, 36, 56), (66, 72, 106)), strand=2.0, sheen=0.3):
    def p(face, i, j, fw, fh):
        sid, frac = strand_id(i, j, salt, strand, 1.4)
        h = hsh(sid, salt, 3)
        v = j / max(1, fh - 1)
        t = 0.25 + 0.3 * h - 0.14 * v + FACE_T[face] * 0.5
        if frac < 0.3:
            t -= 0.16
        elif 0.45 < frac < 0.7 and h > 0.4:
            t += sheen * (0.5 + 0.5 * h) * (1 - 0.5 * v)
        return px(pal(cols, t, i, j, 0.04)), None
    return p


def skin(cols=((176, 136, 140), (214, 176, 172), (238, 208, 198), (250, 230, 220)), salt=0):
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.62 + 0.1 * nz(i * 0.3, j * 0.3, salt) - 0.18 * v + FACE_T[face] * 0.6
        return px(pal(cols, t, i, j, 0.05)), None
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


def feather(cols, tip_glow=None, salt=0, length_axis="j", shaft=True, round_tip=True, tip_frac=0.22, dither=0.05, barbs=True):
    """A feather plate face: a rounded tip, a lighter shaft down the middle, slanted barb lines, pale at the tip; transparent outside the vane.
    length_axis 'j': the feather runs along j (the tip at j = fh - 1); tip_glow (r, g, b, a) puts a glow on the last tip_frac."""
    def p(face, i, j, fw, fh):
        if length_axis == "j":
            a, b, na, nb = i, j, fw, fh
        else:
            a, b, na, nb = j, i, fh, fw
        cx = (na - 1) / 2.0
        u = (a - cx) / max(0.5, na / 2.0)                    # -1 .. 1 across the vane
        s = b / max(1, nb - 1)                               # 0 root .. 1 tip
        w = 1.0 - 0.18 * s                                   # the vane narrows a little towards the tip
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


def keys(a, clip, bone, kind, pts, fps=20, smooth_=True):
    """Keyframes from (time, (x, y, z)) pairs, smoothed with a Catmull-Rom spline sampled `fps` times a second (the loader interpolates linearly)."""
    pts = sorted((float(t), tuple(float(x) for x in v)) for t, v in pts)
    if smooth_ and len(pts) > 2:
        t0, t1 = pts[0][0], pts[-1][0]
        n = max(2, int(round((t1 - t0) * fps)))
        times = sorted({round(t0 + (t1 - t0) * k / n, 4) for k in range(n + 1)} | {round(t, 4) for t, _ in pts})
        frames = {t: tuple(round(v, 3) for v in catmull(pts, t)) for t in times}
    else:
        frames = {t: tuple(round(v, 3) for v in x) for t, x in pts}
    if kind == "scale":                                                      # the spline may undershoot: a scale must never reach zero or turn negative (mirrored quads)
        frames = {t: tuple(max(0.01, v) for v in x) for t, x in frames.items()}
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, frames)


def add(v, d):
    return tuple(v[k] + d[k] for k in range(3))


def spin(a, clip, bone, turns=1.0, axis=1, steps=4):
    """A bone turning about one axis for the whole clip: whole turns, so a loop is seamless."""
    L = a.clips[clip]["animation_length"]
    fr = {}
    for k in range(steps + 1):
        v = [0.0, 0.0, 0.0]
        v[axis] = 360.0 * turns * k / steps
        fr[L * k / steps] = tuple(v)
    a.rot(clip, bone, fr)


def clump(cols, salt=0, flip=False, strands=3.4, root=0.6, curl=0.0, tip_cols=None, glow=None, dither=0.05, split=0.0):
    """One clump of fur / hair / feathery tuft as a flat plate face: a flame shape that is widest a quarter of the way down and ends in a point, strands that
    fan out from the root and meet at the tip, dark roots and pale tips. flip: the root is at the bottom row (a standing clump). curl bends the tip
    sideways (a fraction of the width); split cuts a notch into the tip. Transparent outside the shape."""
    def p(face, i, j, fw, fh):
        s = (fh - 1 - j) / max(1, fh - 1) if flip else j / max(1, fh - 1)          # 0 at the root, 1 at the tip
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

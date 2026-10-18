"""The framework of tools/elite/gen_plushies.py: a plushie is a handful of cubes (a big head, a small body, stubby limbs and a few mob-specific features) packed onto ONE small palette texture.

    Plush         the builder: cube() adds a box and paints its faces, emit_model() writes the block model json, render() draws a small preview
    Tex           a 64x64 texture with a shelf packer; every cube gets a Minecraft-style box layout (top, bottom, west, north, east, south)

Coordinates are model units (1/16 block): x east, y up, z south; the plushie's FRONT is north (-z), so its face is painted on the north faces. Faces are painted as seen from outside
(origin top-left, u to the right, v down); the same convention is used by emit_model() and render(), so what the preview shows is what the game draws.

    face    u direction         v direction
    north   -x                  -y
    south   +x                  -y
    east    -z                  -y
    west    +z                  -y
    up      +x                  +z
    down    +x                  -z
"""
import math
import os

import numpy as np
from PIL import Image

FACES = ("north", "south", "east", "west", "up", "down")
TS = 64


def clamp8(v):
    return int(max(0, min(255, round(v))))


def hexc(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def shade(c, k):
    return (clamp8(c[0] * k), clamp8(c[1] * k), clamp8(c[2] * k))


def mix(a, b, t):
    return tuple(clamp8(a[i] * (1 - t) + b[i] * t) for i in range(3))


def hash2(x, y, salt=0):
    h = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFF) / 65535.0


class Cube:
    def __init__(self, name, size, pos, rect_origin, rot=None, tag=""):
        self.name, self.size, self.pos, self.rot, self.tag = name, tuple(size), tuple(pos), rot, tag
        w, h, d = self.size
        u, v = rect_origin
        # Minecraft box layout: rects as (x, y, w, h) in texture pixels
        self.rects = {
            "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
            "west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
        }

    def box(self):
        (x, y, z), (w, h, d) = self.pos, self.size
        return (x, y, z, x + w, y + h, z + d)


class Tex:
    def __init__(self, size=TS):
        self.size = size
        self.im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.px = self.im.load()
        self.x = self.y = self.row = 0

    def alloc(self, w, h):
        if self.x + w > self.size:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        if self.y + h > self.size:
            raise RuntimeError("plush texture full (%d x %d)" % (self.size, self.size))
        o = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return o

    def used(self):
        return int((np.asarray(self.im)[..., 3] > 0).sum())


class Plush:
    """Builds one plushie. Defaults give the usual sitting plush; every mob overrides what makes it different."""

    def __init__(self, pid, base=(200, 200, 200), translucent=False, ts=TS):
        self.id, self.base, self.translucent = pid, base, translucent
        self.tex = Tex(ts)
        self.cubes = []
        self.named = {}

    # ---------------------------------------------------------------- cubes
    def cube(self, name, size, pos, color=None, rot=None, noise=7, edge=0.88, faces=None, tag=""):
        """Adds a box and fills every face with `color` (default the plush's base colour): a little noise like felt, the top lighter, the bottom and the rim darker.
        rot = (axis, angle, (ox, oy, oz)) rotates the element (angles in 22.5 steps)."""
        w, h, d = [int(round(s)) for s in size]
        o = self.tex.alloc(2 * d + 2 * w, d + h)
        c = Cube(name, (w, h, d), pos, o, rot, tag)
        col = color or self.base
        for f, (rx, ry, rw, rh) in c.rects.items():
            k = {"up": 1.12, "down": 0.72, "north": 1.0, "south": 0.9, "east": 0.94, "west": 0.94}[f]
            for j in range(rh):
                for i in range(rw):
                    n = (hash2(rx + i, ry + j, len(self.cubes) + 1) - 0.5) * 2 * noise
                    rim = edge if (i == 0 or j == 0 or i == rw - 1 or j == rh - 1) and rw > 2 and rh > 2 else 1.0
                    base = shade(col, k * rim)
                    self.tex.px[rx + i, ry + j] = (clamp8(base[0] + n), clamp8(base[1] + n), clamp8(base[2] + n), 255)
        self.cubes.append(c)
        self.named[name] = c
        if faces:
            for f, fn in faces.items():
                fn(self, c, f)
        return c

    # ---------------------------------------------------------------- face painting (coordinates as seen from outside, origin top-left of the face)
    def put(self, c, face, x, y, color, a=255):
        rx, ry, rw, rh = c.rects[face]
        if 0 <= x < rw and 0 <= y < rh and color is not None:
            self.tex.px[rx + x, ry + y] = (color[0], color[1], color[2], a)

    def rect(self, c, face, x, y, w, h, color):
        for j in range(h):
            for i in range(w):
                self.put(c, face, x + i, y + j, color)

    def dots(self, c, face, pts, color):
        for (x, y) in pts:
            self.put(c, face, x, y, color)

    def grid(self, c, face, rows, pal, x0=0, y0=0):
        """Paints an ASCII picture at (x0, y0) of the face; '.' leaves the pixel alone."""
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch != ".":
                    self.put(c, face, x0 + i, y0 + j, pal[ch])

    def face_size(self, c, face):
        return c.rects[face][2], c.rects[face][3]

    def eyes(self, head, style="dot", color=(24, 20, 30), y=3, gap=None, hi=True, size=1, lid=None):
        """Eyes on the north face of `head` (a Cube), symmetric about the middle. style: dot, tall, wide, angry, happy, slit, x, glow."""
        w, h = self.face_size(head, "north")
        gap = gap if gap is not None else max(2, w // 2 - 1)
        lx = (w - gap) // 2 - size
        rx = w - lx - size
        for (ex, sgn) in ((lx, -1), (rx, 1)):
            if style == "dot":
                self.rect(head, "north", ex, y, size, size, color)
                if hi:
                    self.put(head, "north", ex + (size - 1 if sgn < 0 else 0), y, (255, 255, 255))
            elif style == "tall":
                self.rect(head, "north", ex, y, size, size + 1, color)
                if hi:
                    self.put(head, "north", ex + (size - 1 if sgn < 0 else 0), y, (255, 255, 255))
            elif style == "wide":
                self.rect(head, "north", ex - 1 if sgn < 0 else ex, y, size + 1, size, color)
                if hi:
                    self.put(head, "north", ex + (0 if sgn < 0 else 1), y, (255, 255, 255))
            elif style == "angry":
                self.rect(head, "north", ex, y, size + 1, size, color)
                self.put(head, "north", ex + (size + 1 if sgn > 0 else -1), y - 1, color)
                self.put(head, "north", ex + (size if sgn < 0 else 0), y - 1, color)
            elif style == "happy":
                self.put(head, "north", ex, y + 1, color)
                self.put(head, "north", ex + 1, y, color)
                self.put(head, "north", ex + 2, y + 1, color)
            elif style == "slit":
                self.rect(head, "north", ex, y, size, size + 2, color)
            elif style == "x":
                for k in range(3):
                    self.put(head, "north", ex + k, y + k, color)
                    self.put(head, "north", ex + 2 - k, y + k, color)
            elif style == "glow":
                self.rect(head, "north", ex, y, size + 1, size, color)
                self.put(head, "north", ex, y - 1, shade(color, 0.6))
            if lid is not None:
                self.rect(head, "north", ex, y - 1, size + (1 if style in ("wide", "angry", "glow") else 0), 1, lid)

    def mouth(self, head, style="smile", color=(60, 30, 40), y=5, tongue=None):
        w, h = self.face_size(head, "north")
        cx = w // 2
        if style == "smile":
            self.dots(head, "north", [(cx - 1, y), (cx, y)], color)
            self.dots(head, "north", [(cx - 2, y - 1), (cx + 1, y - 1)], color)
        elif style == "line":
            self.rect(head, "north", cx - 1, y, 2, 1, color)
        elif style == "fangs":
            self.rect(head, "north", cx - 2, y, 4, 1, color)
            self.dots(head, "north", [(cx - 2, y + 1), (cx + 1, y + 1)], (250, 250, 250))
        elif style == "open":
            self.rect(head, "north", cx - 1, y, 2, 2, color)
            if tongue:
                self.put(head, "north", cx - 1, y + 1, tongue)
                self.put(head, "north", cx, y + 1, tongue)
        elif style == "cat":
            self.dots(head, "north", [(cx, y), (cx - 1, y + 1), (cx + 1, y + 1)], color)

    def blush(self, head, color=(255, 150, 160), y=4, a=140):
        w, h = self.face_size(head, "north")
        for x in (1, w - 3):
            self.rect(head, "north", x, y, 2, 1, mix(self.base, color, a / 255))

    # ---------------------------------------------------------------- output
    def model_json(self, display=True):
        els = []
        for c in self.cubes:
            x0, y0, z0, x1, y1, z1 = c.box()
            faces = {}
            for f in FACES:
                rx, ry, rw, rh = c.rects[f]
                k = 16.0 / self.tex.size
                faces[f] = {"uv": [round(rx * k, 4), round(ry * k, 4), round((rx + rw) * k, 4), round((ry + rh) * k, 4)], "texture": "#0"}
            e = {"name": c.name, "from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
            if c.rot:
                axis, ang, org = c.rot
                e["rotation"] = {"angle": ang, "axis": axis, "origin": list(org)}
            els.append(e)
        m = {"credit": "generated by tools/elite/gen_plushies.py", "texture_size": [self.tex.size, self.tex.size], "textures": {"0": "nusmp:block/plush/" + self.id, "particle": "nusmp:block/plush/" + self.id}}
        if self.translucent:
            m["render_type"] = "minecraft:translucent"
        else:
            m["render_type"] = "minecraft:cutout"
        m["elements"] = els
        if display:
            m["display"] = {
                "gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.62, 0.62, 0.62]},
                "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
                "fixed": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [0.75, 0.75, 0.75]},
                "thirdperson_righthand": {"rotation": [75, 315, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                "firstperson_righthand": {"rotation": [0, 315, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
            }
        return m

    def bounds(self):
        lo, hi = [99.0] * 3, [-99.0] * 3
        for c in self.cubes:
            b = c.box()
            if c.rot:
                axis, ang, org = c.rot
                pts = rot_corners(b, axis, ang, org)
            else:
                pts = [(b[i], b[j], b[k]) for i in (0, 3) for j in (1, 4) for k in (2, 5)]
            for p in pts:
                for a in range(3):
                    lo[a], hi[a] = min(lo[a], p[a]), max(hi[a], p[a])
        return lo, hi


def rot_corners(b, axis, ang, org):
    x0, y0, z0, x1, y1, z1 = b
    pts = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    return [rot_point(p, axis, ang, org) for p in pts]


def rot_point(p, axis, ang, org):
    a = math.radians(ang)
    x, y, z = p[0] - org[0], p[1] - org[1], p[2] - org[2]
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + org[0], y + org[1], z + org[2])


# ======================================================================= preview renderer
def face_quad(b, f):
    x0, y0, z0, x1, y1, z1 = b
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[f]


NORMALS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
LIGHT = {"up": 1.0, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6, "down": 0.5}


def render(plush, yaw=-32.0, pitch=24.0, size=192, scale=None, bg=(70, 76, 96, 255)):
    """An orthographic z-buffered preview of the plush (nearest-neighbour texture, simple face shading). yaw 0 looks at the front (north face)."""
    scale = scale or size / 24.0
    tex = np.asarray(plush.tex.im, np.uint8)
    img = np.zeros((size, size, 4), np.uint8)
    img[:] = bg
    zbuf = np.full((size, size), 1e9)
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def rotate(x, y, z):
        x1, z1 = x * cy + z * sy, -x * sy + z * cy                 # yaw about y
        y2, z2 = y * cp + z1 * sp, -y * sp + z1 * cp               # pitch about x: looking down on the plush
        return x1, y2, z2

    def project(p):
        x1, y2, z2 = rotate(p[0] - 8, p[1] - 6, p[2] - 8)
        return (size / 2 - x1 * scale, size * 0.58 - y2 * scale, z2)          # the camera looks south from the north: the screen's right is -x

    quads = []
    for c in plush.cubes:
        b = c.box()
        for f in FACES:
            pts = face_quad(b, f)
            n = NORMALS[f]
            if c.rot:
                axis, ang, org = c.rot
                pts = [rot_point(p, axis, ang, org) for p in pts]
                n = rot_point(n, axis, ang, (0, 0, 0))
            if rotate(*n)[2] >= -1e-6:                                   # facing away from the camera
                continue
            proj = [project(p) for p in pts]
            rx, ry, rw, rh = c.rects[f]
            quads.append((proj, (rx, ry, rw, rh), LIGHT[f] if not c.rot else 0.75 + 0.25 * n[1]))
    for proj, (rx, ry, rw, rh), lit in quads:
        xs = [p[0] for p in proj]
        ys = [p[1] for p in proj]
        for py in range(max(0, int(min(ys))), min(size, int(max(ys)) + 1)):
            for px in range(max(0, int(min(xs))), min(size, int(max(xs)) + 1)):
                uv = inv_bilinear(proj, px + 0.5, py + 0.5)
                if uv is None:
                    continue
                u, v = uv
                z = (1 - u) * (1 - v) * proj[0][2] + u * (1 - v) * proj[1][2] + u * v * proj[2][2] + (1 - u) * v * proj[3][2]
                if z >= zbuf[py, px]:
                    continue
                tx = min(rx + rw - 1, rx + int(u * rw))
                ty = min(ry + rh - 1, ry + int(v * rh))
                t = tex[ty, tx]
                if t[3] < 150:
                    continue
                zbuf[py, px] = z
                img[py, px] = (int(t[0] * (0.55 + 0.45 * lit)), int(t[1] * (0.55 + 0.45 * lit)), int(t[2] * (0.55 + 0.45 * lit)), 255)
    return Image.fromarray(img, "RGBA")


def inv_bilinear(q, px, py):
    """The (u, v) of the point inside the quad q = [TL, TR, BR, BL] (screen coords), by Newton iteration from the centre; None when outside."""
    p00, p10, p11, p01 = [(a[0], a[1]) for a in q]
    u = v = 0.5
    for _ in range(8):
        fx = (1 - u) * (1 - v) * p00[0] + u * (1 - v) * p10[0] + u * v * p11[0] + (1 - u) * v * p01[0] - px
        fy = (1 - u) * (1 - v) * p00[1] + u * (1 - v) * p10[1] + u * v * p11[1] + (1 - u) * v * p01[1] - py
        dxu = -(1 - v) * p00[0] + (1 - v) * p10[0] + v * p11[0] - v * p01[0]
        dyu = -(1 - v) * p00[1] + (1 - v) * p10[1] + v * p11[1] - v * p01[1]
        dxv = -(1 - u) * p00[0] - u * p10[0] + u * p11[0] + (1 - u) * p01[0]
        dyv = -(1 - u) * p00[1] - u * p10[1] + u * p11[1] + (1 - u) * p01[1]
        det = dxu * dyv - dxv * dyu
        if abs(det) < 1e-9:
            return None
        u -= (fx * dyv - fy * dxv) / det
        v -= (dxu * fy - dyu * fx) / det
    if -1e-3 <= u <= 1 + 1e-3 and -1e-3 <= v <= 1 + 1e-3:
        return min(1.0, max(0.0, u)), min(1.0, max(0.0, v))
    return None

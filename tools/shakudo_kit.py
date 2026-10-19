"""Shared GeoLite helpers for the 1.93.0 rebuild of Shakudo, Scale, Spike, Nail and Hair Magic
(tools/gen_shakudo_geo.py, gen_scale_geo.py, gen_spike_geo.py, gen_nail_geo.py, gen_hair_geo.py).

A Kit is one geo model plus its texture atlas and glow map:
  * cubes are added with Kit.box / Kit.pair; the box UV is packed into the atlas automatically (shelf packing) and the six faces are
    painted by a material (a function that returns the face colour and the face glow), so the texture can always be redone;
  * Kit.pair builds a right-hand cube and its mirrored left-hand twin (the mirror flag of the geo format, one painting for both);
  * Kit.save(anim) writes assets/nusmp/geo/entity/<key>_<name>.geo.json, animations/entity/..., textures/entity/<key>_<name>.png + _glow.png.

File frame (see geo_builder): pixels, y up, feet at y = 0, the front on -z, the player's right on -x. The humanoid frame used by every armour:
head 8x8x8 at (-4, 24, -4); body 8x12x4 at (-4, 12, -2); right arm at x -8..-4 (pivot -5, 22), left arm x 4..8 (pivot 5, 22); legs 4x12x4 at
(-4 / 0, 0, -2) (pivots -1.9 / 1.9, 12). Armour pieces are authored in that frame with inflate or a margin so they sit over the skin and over a
slim arm (3 wide) as well as a wide one.

Run python as `python3 -B`. Needs numpy and Pillow. Deterministic (fixed seeds).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np
from PIL import Image

from geo_builder import Model, Anim, ROOT          # noqa: F401  (re-exported for the generators)

FACE_LIGHT = {"top": 1.14, "north": 1.0, "east": 0.86, "west": 0.86, "south": 0.74, "bottom": 0.56}


# ================================================================================================ colour helpers
def rgb(h):
    """0xRRGGBB (or '#rrggbb') -> float array 0..1."""
    if isinstance(h, str):
        h = int(h.lstrip("#"), 16)
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], np.float32) / 255.0


def lerp(a, b, t):
    t = np.asarray(t, np.float32)
    if t.ndim == 2:
        t = t[..., None]
    return a * (1 - t) + b * t


def vnoise(w, h, rng, cells):
    """Smooth value noise 0..1 (about one random value per `cells` pixels)."""
    gw, gh = max(2, int(w / cells) + 2), max(2, int(h / cells) + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((max(1, w), max(1, h)), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0 + 1e-9), 0, 1)
    return t * t * (3 - 2 * t)


def hash2(x, y, seed=0):
    """Cheap deterministic per-cell random 0..1."""
    v = np.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453
    return (v - np.floor(v)).astype(np.float32)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w]
    return xx.astype(np.float32), yy.astype(np.float32)


# ================================================================================================ materials
# A material is  f(face, w, h, rng) -> (colour h x w x 3 in 0..1, glow h x w x 3 in 0..1)

def finish(col, glow, face, bevel=0.12):
    """Face light and a one pixel bevel (lit top/left rim, dark bottom/right rim), applied to every material."""
    h, w = col.shape[:2]
    col = col * FACE_LIGHT.get(face, 1.0)
    if w >= 3 and h >= 3 and bevel > 0:
        col[0, :, :] = col[0, :, :] * (1 + bevel * 1.6)
        col[:, 0, :] = col[:, 0, :] * (1 + bevel)
        col[h - 1, :, :] = col[h - 1, :, :] * (1 - bevel * 1.6)
        col[:, w - 1, :] = col[:, w - 1, :] * (1 - bevel)
    return np.clip(col, 0, 1), np.clip(glow, 0, 1)


def solid(color, glow=None, bevel=0.0):
    c, g = rgb(color), (rgb(glow) if glow is not None else None)

    def mat(face, w, h, rng):
        col = np.tile(c, (h, w, 1)).astype(np.float32)
        gl = np.tile(g, (h, w, 1)).astype(np.float32) if g is not None else np.zeros((h, w, 3), np.float32)
        return finish(col, gl, face, bevel) if bevel else (col, gl)
    return mat


def shakudo(dark=0x211A27, mid=0x3A2D43, gold=0xE6B450, inlay=True, sheen=0x5A4470):
    """Shakudo: the black-purple patinated copper-gold alloy of Japanese sword furniture with fine gold inlay lines (nunome-zogan)."""
    d, m, g, sh = rgb(dark), rgb(mid), rgb(gold), rgb(sheen)

    def mat(face, w, h, rng):
        broad = vnoise(w, h, rng, 5)
        fine = vnoise(w, h, rng, 1.6)
        col = lerp(d, m, broad) * (0.86 + 0.28 * fine)[..., None]
        yy = (np.arange(h, dtype=np.float32) / max(1, h - 1))[:, None, None]
        col = col + sh * 0.10 * (1 - yy) * broad[..., None]                 # a faint violet sheen from the top
        gl = np.zeros((h, w, 3), np.float32)
        if inlay and w >= 5 and h >= 5:
            line = np.zeros((h, w), np.float32)
            line[1, 1:w - 1] = 1; line[h - 2, 1:w - 1] = 1; line[1:h - 1, 1] = 1; line[1:h - 1, w - 2] = 1     # the inlaid border
            if w >= 9 and h >= 9:
                cx, cy = (w - 1) / 2, (h - 1) / 2
                xx, yy2 = grid(w, h)
                diam = np.abs(np.abs(xx - cx) / max(1, cx) + np.abs(yy2 - cy) / max(1, cy) - 0.55) < (0.9 / max(w, h)) * 2.2
                line = np.maximum(line, diam.astype(np.float32) * (np.minimum(xx, w - 1 - xx) > 2) * (np.minimum(yy2, h - 1 - yy2) > 2))
                if w >= 12:
                    line[int(cy), 3:w - 3] = np.maximum(line[int(cy), 3:w - 3], (np.arange(3, w - 3) % 3 != 0).astype(np.float32))
            col = lerp(col, g * 0.95, line * 0.95)
            gl = g[None, None, :] * line[..., None] * 0.6
        elif inlay and w >= 3 and h >= 3:
            line = np.zeros((h, w), np.float32)
            line[h // 2, :] = 1
            col = lerp(col, g * 0.9, line * 0.9)
            gl = g[None, None, :] * line[..., None] * 0.6
        return finish(col, gl, face)
    return mat


def gilt(gold=0xD4A445, deep=0x6E4A1C):
    """Polished gold fittings (crest, rivets, edge trim)."""
    g, d = rgb(gold), rgb(deep)

    def mat(face, w, h, rng):
        broad = vnoise(w, h, rng, 4)
        col = lerp(d, g, 0.35 + 0.65 * broad) * 1.0
        gl = col * 0.12
        return finish(col, gl, face, 0.2)
    return mat


def scales(deep=0x0F4C55, mid=0x2E9C9A, rim=0xA8F2E0, size=4, glow_rate=0.14):
    """Overlapping fish / dragon scales, rows offset by half a scale, a bright lower rim, an ink line under each scale."""
    dp, md, rm = rgb(deep), rgb(mid), rgb(rim)

    def mat(face, w, h, rng):
        s = size
        xx, yy = grid(w, h)
        row = np.floor(yy / s)
        off = (row % 2) * (s / 2.0)
        cell = np.floor((xx + off) / s)
        u = ((xx + off) % s) / s * 2 - 1                       # -1..1 across the scale
        v = (yy % s) / s                                       # 0 top .. 1 bottom
        arc = np.sqrt(u * u * 0.85 + (v * 1.15 - 0.15) ** 2)   # the rounded lower edge
        tone = hash2(cell, row, 3)
        base = lerp(dp, md, 0.25 + 0.5 * tone * (0.5 + 0.5 * v))
        col = base * (0.62 + 0.55 * v)[..., None]
        edge = smooth(0.78, 1.0, arc)
        col = lerp(col, rm * 0.9, (edge * smooth(0.45, 0.9, v) * 0.55))
        col = lerp(col, dp * 0.35, smooth(0.96, 1.12, arc) * 0.85)
        glow = rm[None, None, :] * (edge * (hash2(cell, row, 9) < glow_rate) * 0.55)[..., None]
        return finish(col, glow, face, 0.05)
    return mat


def steel_spike(dark=0x3A404C, light=0xB9C6D8, tip=0xE6F2FF, glow_tip=0.0):
    """Brushed gun-metal with a split facet; with glow_tip the bottom of the face glows (for the last box of a spike)."""
    d, l, t = rgb(dark), rgb(light), rgb(tip)

    def mat(face, w, h, rng):
        brush = vnoise(w, h, rng, 1.0) * 0.5 + vnoise(w, 1, rng, 1.0)[0][None, :] * 0.5
        yy = (np.arange(h, dtype=np.float32) / max(1, h - 1))[:, None]
        xx = (np.arange(w, dtype=np.float32) / max(1, w - 1))[None, :]
        facet = np.where(xx < 0.5, 0.75, 1.05) * np.ones((h, w), np.float32)
        base = 0.28 + 0.42 * facet * (0.6 + 0.4 * brush)
        col = lerp(d, l, base)
        col = lerp(col, t, smooth(0.7, 1.0, yy) * 0.35)
        gl = np.zeros((h, w, 3), np.float32)
        if glow_tip > 0:
            k = smooth(0.35, 1.0, yy) * np.ones((h, w), np.float32)
            gl = t[None, None, :] * (k * glow_tip)[..., None]
            col = lerp(col, t, k * 0.5)
        return finish(col, gl, face, 0.1)
    return mat


def iron_studs(dark=0x2C2724, rust=0x6B3A22, head=0xB8AFA0, pitch=4, ember=0xFF8A3A, heat=0.14):
    """A dark iron plate studded with round nail heads."""
    d, r, h0, e = rgb(dark), rgb(rust), rgb(head), rgb(ember)

    def mat(face, w, h, rng):
        n = vnoise(w, h, rng, 3)
        col = lerp(d, r, smooth(0.55, 0.95, n) * 0.6) * (0.8 + 0.4 * vnoise(w, h, rng, 1.2))[..., None]
        xx, yy = grid(w, h)
        px = (xx % pitch) - (pitch - 1) / 2.0
        py = (yy % pitch) - (pitch - 1) / 2.0
        rr = np.sqrt(px * px + py * py)
        stud = (rr < pitch * 0.38).astype(np.float32)
        shade = np.clip(0.6 + 0.5 * (-px - py) / pitch, 0.3, 1.2)
        col = lerp(col, h0 * shade[..., None], stud)
        gl = e[None, None, :] * (stud * heat * (hash2(np.floor(xx / pitch), np.floor(yy / pitch), 5) < 0.5))[..., None]
        return finish(col, gl, face)
    return mat


def nail_steel(dark=0x5C5750, light=0xE4DCCB, glow=None):
    """The long shaft of a driven nail: pale steel with a lit ridge along its length."""
    d, l = rgb(dark), rgb(light)
    g = rgb(glow) if glow is not None else None

    def mat(face, w, h, rng):
        xx, _ = grid(w, h)
        ridge = np.clip(1 - np.abs(xx - (w - 1) * 0.3) / max(1.0, w * 0.35), 0, 1)
        n = vnoise(w, h, rng, 1.5)
        col = lerp(d, l, 0.3 + 0.55 * ridge * (0.7 + 0.3 * n))
        gl = np.zeros((h, w, 3), np.float32)
        if g is not None:
            yy = (np.arange(h, dtype=np.float32) / max(1, h - 1))[:, None]
            gl = g[None, None, :] * (smooth(0.5, 1.0, yy) * 0.7 * np.ones((h, w)))[..., None]
        return finish(col, gl, face, 0.08)
    return mat


def keratin(base=0xD8C9A8, edge=0x8A7658, glow=0xFFB070):
    """Fingernail / claw: pale layered keratin with growth ridges and a warm tip."""
    b, e, g = rgb(base), rgb(edge), rgb(glow)

    def mat(face, w, h, rng):
        _, yy = grid(w, h)
        ridge = 0.5 + 0.5 * np.sin(yy * 1.7)
        n = vnoise(w, h, rng, 2)
        col = lerp(e, b, 0.45 + 0.4 * ridge * 0.4 + 0.3 * n)
        t = smooth(0.55, 1.0, (yy / max(1, h - 1)))
        gl = g[None, None, :] * (t * 0.5)[..., None]
        return finish(col, gl, face, 0.1)
    return mat


def hair(dark=0xC0305F, mid=0xFF6E9A, light=0xFFC4DA, tip_glow=0xFFB0D0, flow=0.22):
    """Strands: long vertical streaks of different widths and lightness, a lit gloss band, glowing tips on some of them."""
    d, m, l, g = rgb(dark), rgb(mid), rgb(light), rgb(tip_glow)

    def mat(face, w, h, rng):
        xx, yy = grid(w, h)
        shift = np.sin(yy * flow + xx * 0.9) * 0.6
        sid = np.floor(xx + shift)
        strand = hash2(sid, 0, 11)
        wid = 0.5 + 0.5 * hash2(sid, 1, 4)
        across = np.abs(((xx + shift) % 1.0) - 0.5) * 2
        body = lerp(d, m, 0.25 + 0.7 * strand)
        body = lerp(body, l, np.clip(0.55 * (1 - across * (1.2 - 0.5 * wid)) * smooth(0.55, 1.0, strand), 0, 1))
        gloss = np.exp(-((yy / max(1, h - 1) - 0.32) ** 2) / 0.02) * 0.28
        col = body * (0.82 + 0.3 * vnoise(w, h, rng, 6))[..., None] + l * gloss[..., None] * 0.5
        t = smooth(0.62, 1.0, yy / max(1, h - 1))
        gl = g[None, None, :] * (t * (strand > 0.45) * 0.55)[..., None]
        return finish(col, gl, face, 0.04)
    return mat


def emissive(color, base=0x1A1020):
    """A lit surface: the colour in the texture and fully in the glow map (eyes, runes, cores)."""
    c, b = rgb(color), rgb(base)

    def mat(face, w, h, rng):
        xx, yy = grid(w, h)
        r = np.sqrt(((xx - (w - 1) / 2) / max(1.0, w / 2)) ** 2 + ((yy - (h - 1) / 2) / max(1.0, h / 2)) ** 2)
        k = np.clip(1.25 - r, 0, 1)
        col = lerp(b, c, k)
        gl = c[None, None, :] * k[..., None]
        return np.clip(col, 0, 1), gl
    return mat


def stone(dark=0x4A4750, light=0x8A8794, crack=0x23212A):
    d, l, c = rgb(dark), rgb(light), rgb(crack)

    def mat(face, w, h, rng):
        n = vnoise(w, h, rng, 3)
        col = lerp(d, l, n) * (0.85 + 0.3 * vnoise(w, h, rng, 1.2))[..., None]
        cr = (vnoise(w, h, rng, 2.5) > 0.78).astype(np.float32)
        col = lerp(col, c, cr * 0.6)
        return finish(col, np.zeros((h, w, 3), np.float32), face)
    return mat


# ================================================================================================ the kit
class Kit:
    def __init__(self, key, name, tw=256, th=256, seed=1):
        self.key, self.name, self.tw, self.th = key, name, tw, th
        self.m = Model(key, name, tw, th)
        self.img = np.zeros((th, tw, 4), np.float32)
        self.glow = np.zeros((th, tw, 4), np.float32)
        self.rng = np.random.default_rng(seed)
        self._x = self._y = self._rowh = 0
        self.boxes = 0

    # ---------------------------------------------------------------- structure
    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def _alloc(self, sx, sy, sz):
        W, H = 2 * (sx + sz), sz + sy
        if self._x + W > self.tw:
            self._x, self._y, self._rowh = 0, self._y + self._rowh, 0
        if self._y + H > self.th or W > self.tw:
            raise SystemExit("atlas %dx%d full at box %d (size %s)" % (self.tw, self.th, self.boxes, (sx, sy, sz)))
        u, v = self._x, self._y
        self._x += W
        self._rowh = max(self._rowh, H)
        return u, v

    def _paint(self, u, v, size, mat):
        sx, sy, sz = size
        faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy), "north": (u + sz, v + sz, sx, sy),
                 "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
        for face, (fu, fv, fw, fh) in faces.items():
            if fw <= 0 or fh <= 0:
                continue
            col, gl = mat(face, int(fw), int(fh), self.rng)
            self.img[fv:fv + fh, fu:fu + fw, :3] = col
            self.img[fv:fv + fh, fu:fu + fw, 3] = 1.0
            self.glow[fv:fv + fh, fu:fu + fw, :3] = gl
            self.glow[fv:fv + fh, fu:fu + fw, 3] = np.where(gl.max(axis=2) > 0.02, 1.0, 0.0)

    def box(self, bone, origin, size, mat, inflate=0.0, rotation=None, pivot=None):
        """One cube with its own freshly painted box UV."""
        size = tuple(int(s) for s in size)
        u, v = self._alloc(*size)
        self._paint(u, v, size, mat)
        self.boxes += 1
        return self.m.cube(bone, origin=tuple(origin), size=size, uv=(u, v), inflate=inflate, rotation=rotation, pivot=pivot)

    def pair(self, right, left, origin, size, mat, inflate=0.0, rotation=None, pivot=None):
        """A cube on the player's right (-x) bone and its mirrored twin on the left bone, sharing one painting."""
        size = tuple(int(s) for s in size)
        u, v = self._alloc(*size)
        self._paint(u, v, size, mat)
        self.boxes += 1
        self.m.cube(right, origin=tuple(origin), size=size, uv=(u, v), inflate=inflate, rotation=rotation, pivot=pivot)
        o = (-(origin[0] + size[0]), origin[1], origin[2])
        rot = None if rotation is None else (rotation[0], -rotation[1], -rotation[2])
        pv = None if pivot is None else (-pivot[0], pivot[1], pivot[2])
        self.m.cube(left, origin=o, size=size, uv=(u, v), inflate=inflate, rotation=rot, pivot=pv, mirror=True)

    def pair_bones(self, base, parent=None, pivot=(0, 0, 0)):
        """Declares <base>_r and <base>_l bones (left mirrored in x) and returns their names."""
        r, l = base + "_r", base + "_l"
        self.bone(r, parent=(parent + "_r") if parent else None, pivot=pivot)
        self.bone(l, parent=(parent + "_l") if parent else None, pivot=(-pivot[0], pivot[1], pivot[2]))
        return r, l

    # ---------------------------------------------------------------- output
    def save(self, anim=None):
        self.m.save(anim)
        rgba = self.img.copy()
        rgba[..., :3] = np.clip(rgba[..., :3], 0, 1)
        im = Image.fromarray((rgba * 255 + 0.5).astype(np.uint8), "RGBA")
        gl = self.glow.copy()
        gim = Image.fromarray((np.dstack([gl[..., :3] * gl[..., 3:4], gl[..., 3:4]]) * 255 + 0.5).astype(np.uint8), "RGBA")
        self.m.save_texture(im, gim)
        print("%s_%s: %d cubes, %d bones, atlas %dx%d (used to y=%d)" % (self.key, self.name, self.m.cube_count(), len(self.m.bones), self.tw, self.th,
                                                                      self._y + self._rowh))


# ================================================================================================ animation helpers
def sway(anim, clip, bone, axis, amp, period, phase=0.0, steps=8):
    """A closed sine rotation of one bone (degrees) over the clip: axis 0/1/2 = x/y/z."""
    frames = {}
    for i in range(steps + 1):
        t = period * i / steps
        v = [0.0, 0.0, 0.0]
        v[axis] = amp * math.sin(2 * math.pi * (i / steps) + phase)
        frames[t] = tuple(v)
    anim.rot(clip, bone, frames)


def bob(anim, clip, bone, amp, period, phase=0.0, steps=8):
    frames = {}
    for i in range(steps + 1):
        frames[period * i / steps] = (0.0, amp * math.sin(2 * math.pi * (i / steps) + phase), 0.0)
    anim.pos(clip, bone, frames)


# ================================================================================================ shape helpers (tapered spikes, nails, chains)
def spike(k, parent, name, base, rot, length, thick, mat, tip_mat=None, segs=3):
    """A tapered spike: a bone at `base` turned by `rot` (degrees, file frame) holding `segs` stacked boxes along its +y that narrow to the tip.
    thick is the base width in pixels (an int >= segs); the last box uses tip_mat (a glowing tip). Returns the bone name."""
    k.bone(name, parent=parent, pivot=tuple(base), rotation=tuple(rot))
    thick = int(max(thick, segs))
    seg_len = max(1, int(round(length / segs)))
    y = 0
    for i in range(segs):
        t = max(1, int(round(thick * (1.0 - i / float(segs)))))
        h = seg_len if i < segs - 1 else max(1, int(length) - y)
        m = tip_mat if (tip_mat is not None and i == segs - 1) else mat
        k.box(name, (base[0] - t / 2.0, base[1] + y, base[2] - t / 2.0), (t, h, t), m)
        y += h
    return name


def nail(k, parent, name, base, rot, length, head_mat, shaft_mat, head=3):
    """A driven nail pointing along the bone's +y: a flat round-ish head on the surface and a thin shaft growing out of it."""
    k.bone(name, parent=parent, pivot=tuple(base), rotation=tuple(rot))
    k.box(name, (base[0] - head / 2.0, base[1], base[2] - head / 2.0), (head, 1, head), head_mat)
    k.box(name, (base[0] - 0.5, base[1] + 1, base[2] - 0.5), (1, int(length), 1), shaft_mat)
    return name


def chain_bones(k, parent, prefix, count, pivot, step):
    """`count` bones in a chain, each child of the last; the first pivots at `pivot`, every next one `step` (dx, dy, dz) further. Returns the names."""
    names = []
    p = list(pivot)
    last = parent
    for i in range(count):
        n = "%s%d" % (prefix, i)
        k.bone(n, parent=last, pivot=tuple(p))
        names.append(n)
        last = n
        p = [p[0] + step[0], p[1] + step[1], p[2] + step[2]]
    return names


def wave(anim, clip, names, axis, amp, period, phase_step=0.7, growth=1.0, steps=8):
    """A travelling wave down a chain of bones (each lags the one before by phase_step; amplitudes multiply by `growth`)."""
    a = amp
    for i, n in enumerate(names):
        sway(anim, clip, n, axis, a, period, -phase_step * i, steps)
        a *= growth

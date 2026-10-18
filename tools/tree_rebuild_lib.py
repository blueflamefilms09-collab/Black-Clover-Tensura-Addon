"""Shared library of the nature-magic rebuild (Tree, Vine, Plant, Poison Plant, Mud, Mucus): GeoLite atlas builder with procedural pixel-art materials
(bark, wood rings, leaves, vines, petals, thorns, toxic flesh, mud, gel, glowing cores), geometry helpers (mirrored side cubes, jointed limb chains, stepped
spikes, leaf clusters, rings) and animation helpers. The six generators gen_<key>_rebuild_geo.py import this file.

    from tree_rebuild_lib import *
    A = Atlas("tree", "helm", 64, 64, seed=3, mats=MATS)
    A.bone("head", pivot=(0, 24, 0))
    A.cube("head", (-5, 31, -5), (10, 3, 10), "bark")
    A.save()

File frame (same as tools/geo_builder.py): pixels, y up, feet at the origin, the front on -z, +x the model's left (the player's LEFT arm is +x, the right arm -x).
Human reference: head x +-4, y 24..32, z +-4; body x +-4, y 12..24, z +-2; arms x +-4..8, y 12..24 (pivot +-5, 22, 0); legs x 0..4 each side, y 0..12 (pivot +-1.9, 12, 0).
Box UV: every cube gets its own rectangle (shelf packed) and is painted face by face (tops lit, bottoms dark), the glow map holds only the glowing parts.
Run python as `python3 -B`.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
from geo_builder import Model, Anim, ROOT  # noqa: E402

SHADE = {"top": 1.12, "bottom": 0.60, "north": 1.0, "south": 0.78, "west": 0.88, "east": 0.94}


def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


# ====================================================================================================================
# noise (vectorised, deterministic)
# ====================================================================================================================
def _hash(ix, iy, seed):
    n = (ix.astype(np.int64) * 374761393 + iy.astype(np.int64) * 668265263 + int(seed) * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = n ^ (n >> 16)
    return (n & 0xFFFF).astype(np.float64) / 65535.0


def vnoise(X, Y, seed):
    """Value noise in 0..1 at float coordinates X, Y (arrays)."""
    x0, y0 = np.floor(X), np.floor(Y)
    fx, fy = X - x0, Y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = _hash(x0, y0, seed), _hash(x0 + 1, y0, seed)
    c, d = _hash(x0, y0 + 1, seed), _hash(x0 + 1, y0 + 1, seed)
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


def fbm(X, Y, seed, octaves=3):
    v, amp, tot = 0.0, 1.0, 0.0
    for o in range(octaves):
        v = v + amp * vnoise(X * (2 ** o), Y * (2 ** o), seed + o * 17)
        tot += amp
        amp *= 0.5
    return v / tot


# ====================================================================================================================
# material painters: each returns (t, outline, glow, alpha_mul, special)  t in 0..1 (light level), outline/glow in 0..1 masks
# ====================================================================================================================
def _grid(w, h):
    X, Y = np.meshgrid(np.arange(w, dtype=np.float64) + 0.5, np.arange(h, dtype=np.float64) + 0.5)
    return X, Y


def paint_bark(w, h, seed, P):
    X, Y = _grid(w, h)
    n = 0.62 * vnoise(X * 0.75, Y * 0.14, seed) + 0.38 * vnoise(X * 2.0, Y * 0.45, seed + 7)
    t = 0.16 + 0.78 * n
    outline = (n < 0.2).astype(np.float64)
    knot = vnoise(X * 0.35, Y * 0.35, seed + 31)
    kk = (knot > 0.82)
    t = np.where(kk, 0.18, t)
    outline = np.where(kk, 1.0, outline)
    glow = ((n < 0.15) * P.get("seam", 0) / 255.0).astype(np.float64)
    return t, outline, glow, 1.0


def paint_ring(w, h, seed, P):
    X, Y = _grid(w, h)
    cx, cy = (w - 1) / 2.0 + 0.7 * math.sin(seed), (h - 1) / 2.0 + 0.5 * math.cos(seed)
    d = np.sqrt(((X - 0.5 - cx)) ** 2 + ((Y - 0.5 - cy)) ** 2)
    band = 0.5 + 0.5 * np.sin(d * 2.2 + vnoise(X * 0.5, Y * 0.5, seed) * 2.0)
    t = 0.3 + 0.55 * band
    outline = (d > max(w, h) * 0.5 - 0.6).astype(np.float64)
    glow = (band > 0.82) * (P.get("ring_glow", 170) / 255.0) * np.clip(1.2 - d / (max(w, h) * 0.7), 0, 1)
    return t, outline, glow, 1.0


def paint_leaf(w, h, seed, P):
    X, Y = _grid(w, h)
    cx = (w - 1) / 2.0 + 0.5
    ax = np.abs(X - cx)
    n = fbm(X * 0.6, Y * 0.6, seed, 2)
    t = 0.42 + 0.28 * n + 0.22 * (1 - ax / max(1.0, w / 2.0))
    vein = (ax < 0.55) | (np.abs(((Y - ax * 1.1) % 3.0) - 1.5) < 0.4)
    t = np.where(vein, t + 0.32, t)
    outline = ((X < 1) | (X > w - 1) | (Y > h - 1)).astype(np.float64) * (1 if min(w, h) >= 4 else 0)
    glow = (vein * (P.get("vein_glow", 0) / 255.0)).astype(np.float64)
    return t, outline, glow, 1.0


def paint_vine(w, h, seed, P):
    X, Y = _grid(w, h)
    ph = (X * 1.0 + Y * 0.85) * (math.pi / 2.2)
    band = 0.5 + 0.5 * np.sin(ph)
    n = vnoise(X * 0.6, Y * 0.6, seed)
    t = 0.16 + 0.6 * band + 0.22 * n
    outline = (band < 0.14).astype(np.float64)
    thorn = _hash(np.floor(X / 2.0), np.floor(Y / 3.0), seed + 5) > 0.93
    t = np.where(thorn, 0.97, t)
    glow = (thorn * (P.get("thorn_glow", 0) / 255.0)).astype(np.float64)
    return t, outline, glow, 1.0


def paint_mud(w, h, seed, P):
    X, Y = _grid(w, h)
    n = fbm(X * 0.34, Y * 0.30, seed, 3)
    drip = vnoise(X * 1.3, Y * 0.07, seed + 9)
    t = 0.12 + 0.7 * n - 0.16 * (drip < 0.3)
    spec = (fbm(X * 0.8 + 11, Y * 0.8 + 3, seed + 21, 2) > 0.8) & (n > 0.55)
    t = np.where(spec, 0.99, t)
    crack = np.abs(fbm(X * 0.25, Y * 0.25, seed + 4, 2) - 0.5) < 0.035
    outline = crack.astype(np.float64)
    glow = (crack * (P.get("seam", 0) / 255.0)).astype(np.float64)
    return t, outline, glow, 1.0


def paint_gel(w, h, seed, P):
    X, Y = _grid(w, h)
    n = fbm(X * 0.4, Y * 0.4, seed, 2)
    t = 0.38 + 0.4 * n
    edge = np.minimum(np.minimum(X, w - X), np.minimum(Y, h - Y))
    rim = np.clip(1.0 - edge / 1.6, 0, 1) if min(w, h) >= 4 else np.zeros_like(X)
    t = t + 0.35 * rim
    # bubbles: bright rings with a clear middle
    rng = np.random.RandomState(seed + 3)
    alpha = np.full((h, w), 1.0)
    nb = max(1, int(w * h / 90))
    for _ in range(nb):
        bx, by, br = rng.uniform(0, w), rng.uniform(0, h), rng.uniform(1.1, 2.4)
        d = np.sqrt((X - bx) ** 2 + (Y - by) ** 2)
        ringm = np.abs(d - br) < 0.55
        t = np.where(ringm, 0.98, t)
        alpha = np.where(d < br - 0.4, alpha * 0.55, alpha)
    outline = np.zeros_like(X)
    glow = (rim * P.get("rim_glow", 0) / 255.0) * 1.0
    return t, outline, glow, alpha


def paint_toxic(w, h, seed, P):
    X, Y = _grid(w, h)
    n = fbm(X * 0.45, Y * 0.45, seed, 3)
    t = 0.15 + 0.7 * n
    spot = _hash(np.floor(X / 2.0), np.floor(Y / 2.0), seed + 13) > 0.86
    t = np.where(spot, 0.95, t)
    vein = np.abs(fbm(X * 0.3, Y * 0.3, seed + 2, 2) - 0.5) < 0.04
    outline = vein.astype(np.float64)
    glow = ((vein | spot) * P.get("vein_glow", 0) / 255.0).astype(np.float64)
    return t, outline, glow, 1.0


def paint_petal(w, h, seed, P):
    X, Y = _grid(w, h)
    cx, cy = w / 2.0, h / 2.0
    d = np.sqrt(((X - cx) / max(1.0, w / 2.0)) ** 2 + ((Y - cy) / max(1.0, h / 2.0)) ** 2)
    n = vnoise(X * 0.5, Y * 0.5, seed)
    t = 0.38 + 0.5 * np.clip(d, 0, 1) + 0.12 * n
    vein = (np.abs(((X - cx) * 1.1) % 2.6 - 1.3) < 0.3) & (d < 0.8)
    t = np.where(vein, t - 0.14, t)
    outline = ((d > 1.02) & (min(w, h) >= 4)).astype(np.float64)
    glow = ((1.0 - np.clip(d, 0, 1)) * (P.get("heart_glow", 0) / 255.0)).astype(np.float64)
    return t, outline, glow, 1.0


def paint_core(w, h, seed, P):
    X, Y = _grid(w, h)
    cx, cy = w / 2.0, h / 2.0
    r = np.sqrt(((X - cx) / max(1.0, w / 2.0)) ** 2 + ((Y - cy) / max(1.0, h / 2.0)) ** 2)
    t = np.clip(1.0 - r * 0.85, 0, 1) * 0.95 + 0.04
    glow = np.clip(1.15 - r, 0, 1) * 0.95
    return t, np.zeros_like(X), glow, 1.0


def paint_thorn(w, h, seed, P):
    X, Y = _grid(w, h)
    n = vnoise(X * 0.5, Y * 0.5, seed)
    t = 0.55 + 0.3 * n - 0.4 * (Y / max(1.0, h))
    outline = ((X < 1) | (X > w - 1)).astype(np.float64) * (1 if w >= 4 else 0)
    return t, outline, np.zeros_like(X), 1.0


def paint_plain(w, h, seed, P):
    X, Y = _grid(w, h)
    t = 0.4 + 0.4 * vnoise(X * 0.7, Y * 0.7, seed)
    outline = ((X < 1) | (Y < 1)).astype(np.float64) * (1 if min(w, h) >= 5 else 0)
    glow = np.full((h, w), P.get("flat_glow", 0) / 255.0)
    return t, outline, glow, 1.0


PAINTERS = {"bark": paint_bark, "ring": paint_ring, "leaf": paint_leaf, "vine": paint_vine, "mud": paint_mud, "gel": paint_gel, "toxic": paint_toxic,
            "petal": paint_petal, "core": paint_core, "thorn": paint_thorn, "plain": paint_plain}


def render_face(P, w, h, shade, seed):
    """Pixels of one face for material P: (rgb uint8 (h,w,3), alpha uint8 (h,w), glow rgb (h,w,3), glow alpha (h,w))."""
    t, outline, glow, amul = PAINTERS[P["kind"]](w, h, seed, P)
    t = t + (shade - 0.9) * 0.5
    ramp = np.array(P["ramp"], dtype=np.float64)
    lvl = np.where(t < 0.24, 0, np.where(t < 0.5, 1, np.where(t < 0.8, 2, 3)))
    rgb = ramp[1 + lvl]
    ok = np.asarray(outline) > 0.5
    k = 0.78 + 0.22 * shade
    rgb = np.where(ok[..., None], (ramp[0] * k)[None, None, :], rgb)
    alpha = np.full((h, w), float(P.get("alpha", 255)))
    alpha = alpha * np.asarray(amul)
    ga = np.clip(np.asarray(glow) * 255.0, 0, 255)
    gc = np.array(P.get("glowc", (255, 255, 255)), dtype=np.float64)
    grgb = np.broadcast_to(gc, (h, w, 3)).copy()
    # the hottest pixels of a glowing material are whitened
    grgb = np.where((ga > 200)[..., None], grgb * 0.6 + 255 * 0.4, grgb)
    return rgb.astype(np.uint8), np.clip(alpha, 0, 255).astype(np.uint8), grgb.astype(np.uint8), ga.astype(np.uint8)


# ====================================================================================================================
# Atlas: a Model with a shelf packer and the face painter
# ====================================================================================================================
class Atlas:
    def __init__(self, key, name, w, h, seed=1, mats=None):
        self.key = key
        self.m = Model(key, name, w, h)
        self.w, self.h = w, h
        self.x = self.y = self.rowh = 0
        self.jobs = []
        self.shared = {}
        self.seed = seed
        self.mats = mats or {}
        self.anim = Anim()
        self.count = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def _alloc(self, cw, ch):
        if self.x + cw > self.w:
            self.x, self.y, self.rowh = 0, self.y + self.rowh, 0
        if self.y + ch > self.h:
            raise RuntimeError("atlas %s is full (%dx%d)" % (self.m.id, self.w, self.h))
        u, v = self.x, self.y
        self.x += cw
        self.rowh = max(self.rowh, ch)
        return u, v

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, share=None, pivot=None):
        """One painted cube; sizes are whole pixels. rot = (rx, ry, rz) turns it about its own centre (or `pivot`). share = a tag: cubes with the
        same tag, size and material reuse one uv rectangle."""
        sx, sy, sz = [int(s) for s in size]
        if mat not in self.mats:
            raise KeyError("unknown material " + mat)
        key = (share, sx, sy, sz, mat) if share else None
        if key and key in self.shared:
            u, v = self.shared[key]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, mat, len(self.jobs)))
            if key:
                self.shared[key] = (u, v)
        pv = None
        if rot:
            pv = list(pivot) if pivot is not None else [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        self.count += 1
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=pv)

    def side(self, bone, sg, origin, size, mat, rot=None, inflate=0.0, share=None):
        """A cube designed for the +x side, mirrored to the -x side when sg == -1 (the player's right is -x)."""
        x0 = origin[0] if sg > 0 else -(origin[0] + size[0])
        r = (rot[0], sg * rot[1], sg * rot[2]) if rot else None
        return self.cube(bone, (x0, origin[1], origin[2]), size, mat, inflate=inflate, rot=r, share=share)

    # ------------------------------------------------------------------ jointed chains
    def limb(self, bone, parent, base, tilt, segs, w0, w1, mat, depth=None, mat2=None, share=None, curl=(0, 0, 0)):
        """A chain of segments growing along +y from `base`; each segment is its own bone. The first is turned by tilt = (rx, ry, rz) degrees, every
        next one by `curl` relative to the previous, so a chain bends. segs = a list of lengths; widths taper w0 -> w1. Cube coordinates of a bone
        are in its own un-turned frame: the tip of segment k is at self.tips[name] (use it to hang leaves or flowers on a tip). Returns the bone names."""
        names, p = [], parent
        pivot = list(base)
        n = len(segs)
        if not hasattr(self, "tips"):
            self.tips = {}
        for k, ln in enumerate(segs):
            nm = "%s_%d" % (bone, k)
            self.bone(nm, p, tuple(pivot), rotation=tuple(tilt if k == 0 else curl))
            f = k / float(max(1, n - 1)) if n > 1 else 0.0
            w = max(1, int(round(w0 + (w1 - w0) * f)))
            d = w if depth is None else max(1, int(depth))
            self.cube(nm, (pivot[0] - w / 2.0, pivot[1], pivot[2] - d / 2.0), (w, int(ln), d), mat2 if (mat2 and k % 2) else mat,
                      share=("%s%d" % (share, k)) if share else None)
            self.tips[nm] = (pivot[0], pivot[1] + ln, pivot[2])
            names.append(nm)
            p = nm
            pivot = [pivot[0], pivot[1] + ln, pivot[2]]
        return names

    def spike(self, bone, base, axis, length, width, mat, layers=3, share=None, tipw=1):
        """A stepped taper from `base` along axis ('+y', '-z' ...), `width` wide at the foot."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        for k in range(layers):
            f = k / float(layers)
            w = max(tipw, int(round(width * (1.0 - f) ** 0.9)))
            lo = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            o, s = [0, 0, 0], [w, w, w]
            for i in range(3):
                o[i] = base[i] - w / 2.0
            o[a] = lo
            s[a] = seg
            self.cube(bone, tuple(o), tuple(s), mat, share=("%s%d" % (share, k)) if share else None)

    def leaf(self, bone, center, size, mat, rot=None, share=None):
        """A flat leaf (w, h, 1) centred on `center`."""
        w, h = size
        return self.cube(bone, (center[0] - w / 2.0, center[1] - h / 2.0, center[2] - 0.5), (w, h, 1), mat, rot=rot, share=share)

    def ring(self, bone, center, radius, n, size, mat, y_rot=True, share=None):
        """n cubes of `size` around a horizontal circle of `radius` (each turned to face outwards)."""
        for k in range(n):
            a = k * 360.0 / n
            x, z = center[0] + radius * math.cos(math.radians(a)), center[2] + radius * math.sin(math.radians(a))
            self.cube(bone, (x - size[0] / 2.0, center[1] - size[1] / 2.0, z - size[2] / 2.0), size, mat, rot=(0, -a + 90, 0) if y_rot else None,
                      share=share)

    # ------------------------------------------------------------------ painting and saving
    def paint(self):
        img, glow = self.m.paint()
        ip, gp = np.array(img), np.array(glow)
        for (u, v, sx, sy, sz, mat, idx) in self.jobs:
            P = self.mats[mat]
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                if w <= 0 or h <= 0:
                    continue
                rgb, a, grgb, ga = render_face(P, w, h, SHADE[kind], self.seed * 101 + idx * 7 + list(SHADE).index(kind))
                ip[y:y + h, x:x + w, :3] = rgb
                ip[y:y + h, x:x + w, 3] = a
                gp[y:y + h, x:x + w, :3] = grgb
                gp[y:y + h, x:x + w, 3] = ga
        from PIL import Image
        return Image.fromarray(ip, "RGBA"), Image.fromarray(gp, "RGBA")

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        print("%-30s %3d cubes  %dx%d  atlas used to y=%d" % (self.m.id, self.m.cube_count(), self.w, self.h, self.y + self.rowh))
        return self.m.id


# ====================================================================================================================
# animation helpers
# ====================================================================================================================
def loop(clip_len, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop."""
    return {round(clip_len * k / steps, 2): f(k / float(steps)) for k in range(steps + 1)}


def sway(clip_len, amp, phase=0.0, steps=8, axis=2, base=(0, 0, 0)):
    """A rotation that swings about one axis (0 x, 1 y, 2 z) around `base`."""
    def f(p):
        v = list(base)
        v[axis] = base[axis] + amp * math.sin((p + phase) * 2 * math.pi)
        return tuple(v)
    return loop(clip_len, f, steps)


def spin(clip_len, axis=1, turns=1):
    steps = 4 * turns
    return {round(clip_len * k / steps, 2): tuple(90.0 * k if i == axis else 0 for i in range(3)) for k in range(steps + 1)}


def swell(clip_len, amp, phase=0.0, steps=8):
    return loop(clip_len, lambda p: (1 + amp * math.sin((p + phase) * 2 * math.pi),) * 3, steps)


# ====================================================================================================================
# the skin-layout overlay texture (a 64x64 player skin: torso, arms, legs, head) painted with the same materials
# ====================================================================================================================
class Skin:
    """textures/aura/<key>_skin.png (+ _glow): laid out like a vanilla skin; only the painted rectangles are opaque."""

    def __init__(self, key, mats, seed=5):
        from PIL import Image
        self.key, self.mats, self.seed = key, mats, seed
        self.ip = np.zeros((64, 64, 4), dtype=np.uint8)
        self.gp = np.zeros((64, 64, 4), dtype=np.uint8)
        self.n = 0

    @staticmethod
    def faces(u, v, sx, sy, sz):
        return {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}

    BOXES = {"torso": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4), "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4),
             "head": (0, 0, 8, 8, 8)}

    def put(self, box, kinds, rect, mat):
        """Paint the rect (col, row, w, h) (in each named face's own pixels) of the box's faces."""
        f = self.faces(*self.BOXES[box])
        P = self.mats[mat]
        for kind in kinds:
            x, y, w, h = f[kind]
            c0, r0, cw, rh = rect
            cw, rh = min(cw, w - c0), min(rh, h - r0)
            if cw <= 0 or rh <= 0:
                continue
            self.n += 1
            rgb, a, grgb, ga = render_face(P, cw, rh, SHADE[kind], self.seed * 31 + self.n * 5)
            self.ip[y + r0:y + r0 + rh, x + c0:x + c0 + cw, :3] = rgb
            self.ip[y + r0:y + r0 + rh, x + c0:x + c0 + cw, 3] = a
            self.gp[y + r0:y + r0 + rh, x + c0:x + c0 + cw, :3] = grgb
            self.gp[y + r0:y + r0 + rh, x + c0:x + c0 + cw, 3] = ga

    def save(self):
        from PIL import Image
        out = os.path.join(ROOT, "textures", "aura")
        os.makedirs(out, exist_ok=True)
        Image.fromarray(self.ip, "RGBA").save(os.path.join(out, self.key + "_skin.png"))
        Image.fromarray(self.gp, "RGBA").save(os.path.join(out, self.key + "_skin_glow.png"))
        print("%-30s skin layout 64x64, %d rectangles" % ("aura/" + self.key + "_skin", self.n))


# ====================================================================================================================
# shared shape helpers
# ====================================================================================================================
def leaves(A, bone, center, n, spread, size=(4, 4), mats=("leaf", "leaf_dark"), seed=0, up=0.0):
    """A cluster of n flat leaves around `center` (in the bone's own un-turned frame), each turned at its own angle."""
    for k in range(n):
        a = k * 137.5 + seed * 40
        r = spread * (0.35 + 0.65 * ((k * 0.618 + seed * 0.3) % 1.0))
        x, z = center[0] + r * math.cos(math.radians(a)), center[2] + r * math.sin(math.radians(a))
        y = center[1] + up * ((k * 0.37) % 1.0) + ((k % 3) - 1) * 0.8
        A.leaf(bone, (x, y, z), size, mats[k % len(mats)], rot=(20 * ((k % 3) - 1), a % 180, 25 * (((k + 1) % 3) - 1)),
               share="lf%d%d%d" % (k % 2, len(mats), size[0]))


def lean(dx, dz, phi):
    """The bone rotation (rx, ry, rz) that leans a +y limb by phi degrees towards the horizontal direction (dx, dz) (+x the model's left, +z its back)."""
    l = math.hypot(dx, dz) or 1.0
    return (round(-phi * dz / l, 2), 0, round(-phi * dx / l, 2))


def fan(A, bone, base, n, radius, a0, a1, size, mats, plane="xy", share=None):
    """A fan of n flat leaves around `base`: leaf k stands at an angle between a0 and a1 degrees from straight up (positive towards +x), radius out."""
    for k in range(n):
        th = a0 + (a1 - a0) * (k / float(max(1, n - 1)))
        x, y = base[0] + radius * math.sin(math.radians(th)), base[1] + radius * math.cos(math.radians(th))
        A.leaf(bone, (x, y, base[2]), size, mats[k % len(mats)], rot=(0, 0, -th), share=(share + str(k % len(mats))) if share else None)


def chain_z(A, bone, parent, start, n, seg, w0, w1, h0, h1, mat, yaw=0.0, mat2=None, share=None):
    """A chain along -z (the model's front) of n segments from `start` = (x, y, z): each a bone turned by `yaw` degrees about y relative to the previous one, so it can slither.
    Segment k is (w, h, seg) wide / tall / long, tapering w0->w1 and h0->h1. Returns the bone names."""
    names, p = [], parent
    z = start[2]
    for k in range(n):
        f = k / float(max(1, n - 1))
        w, h = max(1, int(round(w0 + (w1 - w0) * f))), max(1, int(round(h0 + (h1 - h0) * f)))
        nm = "%s_%d" % (bone, k)
        A.bone(nm, p, (start[0], start[1], z), rotation=(0, yaw if k else 0, 0))
        A.cube(nm, (start[0] - w / 2.0, start[1] - h / 2.0, z - seg), (w, h, seg), mat2 if (mat2 and k % 2) else mat, share=("%s%d" % (share, k)) if share else None)
        names.append(nm)
        p = nm
        z -= seg
    return names


def run_check(ids):
    """Runs tools/geo_check.py on the given model ids."""
    import subprocess
    r = subprocess.run([sys.executable, "-B", os.path.join(os.path.dirname(os.path.abspath(__file__)), "geo_check.py")] + list(ids))
    if r.returncode != 0:
        raise SystemExit("geo_check failed")

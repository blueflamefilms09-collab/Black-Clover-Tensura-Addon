"""Shared library of the 1.93.0 magic rebuild generators (Smoke, Snow, Wing, Vortex, Air, Ash): geo pieces, painted atlases, particle textures.

    import gen_air_rebuild_lib as L

Three parts, all deterministic (seeded numpy / Pillow, no fonts, no network), so every PNG and geo file can be redone:

* material sheets   L.Sheet(...)         a 64x64 seamless RGBA colour pattern plus a glow pattern, built from periodic noise (smoke billows,
                                           snow grains, feather barbs, wind swirls, air streamlines, ash and embers). Faces of the cubes crop the sheet.
* geo models        L.Piece(key, name)   a GeoLite model (tools/geo_builder.py) whose cubes are painted face by face from a Sheet, with box uv, a glow
                                           map, helpers for the shapes the magics need (puffs, feathers, spikes, rings, tubes between two points) and keyframes.
* particle textures L.particle(...)      helpers that draw the white / grey sprites the VFX layers tint (soft clouds, flakes, feathers, spirals, rings, streaks).

File frame of the geo models (tools/geo_builder.py): pixels, y up, feet at the origin, the model's front on -z, +x is the model's left (so the
character's RIGHT arm sits at x -8..-4 and the right leg at x -4..0). Rotations of a cube are degrees, applied X then Y then Z about the cube's centre.
Run python as `python3 -B`.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np                       # noqa: E402
from PIL import Image, ImageDraw, ImageFilter   # noqa: E402

from geo_builder import Model, Anim, ROOT   # noqa: E402

PARTICLE_DIR = os.path.join(ROOT, "textures", "particle")


# =====================================================================================================================
# noise
# =====================================================================================================================
def _periodic_noise(n, cells, rng):
    """n x n float array in 0..1, seamless, value noise with `cells` lattice cells per side (smoothstep interpolation)."""
    lat = rng.random((cells, cells))
    xs = np.linspace(0, cells, n, endpoint=False)
    x0 = np.floor(xs).astype(int)
    f = xs - x0
    f = f * f * (3 - 2 * f)
    x1 = (x0 + 1) % cells
    a = lat[np.ix_(x0, x0)]
    b = lat[np.ix_(x0, x1)]
    c = lat[np.ix_(x1, x0)]
    d = lat[np.ix_(x1, x1)]
    fx = f[None, :]
    fy = f[:, None]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def fbm(n, seed, base=4, octaves=4, gain=0.5):
    """Seamless fractal noise n x n in 0..1."""
    rng = np.random.default_rng(seed)
    out = np.zeros((n, n))
    amp, tot, cells = 1.0, 0.0, base
    for _ in range(octaves):
        out += amp * _periodic_noise(n, cells, rng)
        tot += amp
        amp *= gain
        cells *= 2
    out /= tot
    lo, hi = out.min(), out.max()
    return (out - lo) / max(1e-6, hi - lo)


def warp(field, other, amount):
    """Domain-warps `field` by `other` (both n x n, 0..1): swirls and billows."""
    n = field.shape[0]
    ys, xs = np.mgrid[0:n, 0:n]
    dx = ((other - 0.5) * amount).astype(float)
    dy = ((np.roll(other, 7, axis=0) - 0.5) * amount).astype(float)
    return field[((ys + dy).astype(int)) % n, ((xs + dx).astype(int)) % n]


def ridge(field):
    """1 on the thin lines where the noise crosses 0.5 (cracks, seams, veins)."""
    return 1.0 - np.abs(field - 0.5) * 2.0


def lerp_col(c0, c1, t):
    return tuple(int(round(c0[i] + (c1[i] - c0[i]) * t)) for i in range(3))


def ramp(stops, t):
    """Colour at t (0..1) on a list of RGB stops spread evenly."""
    t = min(max(t, 0.0), 1.0) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    return lerp_col(stops[i], stops[i + 1], t - i)


# =====================================================================================================================
# material sheets
# =====================================================================================================================
class Sheet:
    """A 64x64 seamless material: rgb (n,n,3) uint8, alpha (n,n) uint8, glow (n,n) float 0..1 and the glow colour."""
    N = 64

    def __init__(self, rgb, alpha, glow, glow_col):
        self.rgb, self.alpha, self.glow, self.glow_col = rgb, alpha, glow, glow_col


def _colorize(v, stops):
    n = v.shape[0]
    out = np.zeros((n, n, 3), np.uint8)
    t = np.clip(v, 0, 1) * (len(stops) - 1)
    i = np.minimum(t.astype(int), len(stops) - 2)
    f = (t - i)[..., None]
    arr = np.array(stops, float)
    out[:] = (arr[i] * (1 - f) + arr[i + 1] * f).astype(np.uint8)
    return out


def sheet_smoke(seed, stops, ember=(255, 140, 70), ember_amt=0.5, density=0.72):
    """Billowing smoke: warped fbm lit from above, dark fissures, a few ember specks glowing in the dark."""
    n = Sheet.N
    a = fbm(n, seed, 3, 4)
    b = fbm(n, seed + 1, 4, 3)
    v = warp(a, b, 14)
    v = 0.18 + 0.72 * np.clip(v * 1.1 - 0.05, 0, 1)
    rgb = _colorize(v, stops)
    spec = fbm(n, seed + 2, 8, 2)
    glow = np.where((spec > 0.88) & (v < 0.5), ember_amt, 0.0)
    glow += np.where(ridge(warp(b, a, 10)) > 0.93, 0.25 * ember_amt, 0.0)
    return Sheet(rgb, np.full((n, n), 255, np.uint8), np.clip(glow, 0, 1), ember)


def sheet_snow(seed, stops, glint=(190, 235, 255), vein=(150, 200, 235)):
    """Packed snow: soft grain, pale blue hollows, thin ice veins, sparkles."""
    n = Sheet.N
    g = fbm(n, seed, 6, 3)
    g2 = fbm(n, seed + 1, 3, 3)
    v = np.clip(0.55 * g + 0.45 * g2, 0, 1)
    v = 0.35 + 0.65 * v
    rgb = _colorize(v, stops)
    r = ridge(warp(g2, g, 9))
    vein_m = r > 0.955
    rgb[vein_m] = np.array(vein, np.uint8)
    rng = np.random.default_rng(seed + 5)
    sp = rng.random((n, n))
    spark = sp > 0.985
    glow = np.where(spark, 0.9, 0.0) + np.where(vein_m, 0.35, 0.0)
    rgb[spark] = np.array((255, 255, 255), np.uint8)
    return Sheet(rgb, np.full((n, n), 255, np.uint8), np.clip(glow, 0, 1), glint)


def sheet_feather(seed, stops, shaft=(255, 236, 190), glow_col=(255, 232, 170)):
    """Feather barbs: fine diagonal barbs either side of a vertical rachis, tinted at the edges (the image runs top = base, bottom = tip)."""
    n = Sheet.N
    ys, xs = np.mgrid[0:n, 0:n]
    cx = n / 2.0
    side = np.where(xs < cx, -1, 1)
    off = np.abs(xs - cx)
    barb = 0.5 + 0.5 * np.sin((ys + off * 0.9 * side * -1) * 1.35 + fbm(n, seed, 4, 2) * 3.0)
    edge = np.clip(off / (n / 2.0), 0, 1)
    v = 0.30 + 0.45 * barb + 0.25 * (1 - edge)
    v += (fbm(n, seed + 3, 8, 2) - 0.5) * 0.18
    rgb = _colorize(np.clip(v, 0, 1), stops)
    rach = off < 1.6
    rgb[rach] = np.array(shaft, np.uint8)
    glow = np.where(rach, 0.55, 0.0) + np.where(edge > 0.86, 0.18, 0.0)
    return Sheet(rgb, np.full((n, n), 255, np.uint8), np.clip(glow, 0, 1), glow_col)


def sheet_swirl(seed, stops, glow_col=(190, 255, 255), arms=3):
    """Spiral arms: the wind-wrapped plate of the vortex magic (bright arms, deep teal between)."""
    n = Sheet.N
    ys, xs = np.mgrid[0:n, 0:n]
    dx, dy = xs - n / 2.0 + 0.5, ys - n / 2.0 + 0.5
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    w = fbm(n, seed, 4, 3)
    s = 0.5 + 0.5 * np.sin(arms * th + r * 0.34 + (w - 0.5) * 2.2)
    v = np.clip(0.2 + 0.8 * s ** 1.6 + (w - 0.5) * 0.15, 0, 1)
    rgb = _colorize(v, stops)
    glow = np.where(s > 0.9, 0.7, 0.0)
    return Sheet(rgb, np.full((n, n), 255, np.uint8), glow, glow_col)


def sheet_air(seed, stops, glow_col=(210, 250, 255), alpha=(70, 185)):
    """Compressed air: horizontal streamlines in a glassy pale cyan, a white sheen, translucent (the piece is drawn in the translucent layer)."""
    n = Sheet.N
    ys, xs = np.mgrid[0:n, 0:n]
    w = fbm(n, seed, 3, 3)
    flow = 0.5 + 0.5 * np.sin((ys + (w - 0.5) * 12) * 0.55)
    v = np.clip(0.25 + 0.55 * flow + (fbm(n, seed + 2, 6, 2) - 0.5) * 0.3, 0, 1)
    rgb = _colorize(v, stops)
    a = (alpha[0] + (alpha[1] - alpha[0]) * v).astype(np.uint8)
    glow = np.where(flow > 0.93, 0.6, 0.0)
    return Sheet(rgb, a, glow, glow_col)


def sheet_ash(seed, stops, ember=(255, 110, 40), crack_amt=0.9):
    """Burnt ash: grey-black speckle, pale ash drifts, ember-lit cracks."""
    n = Sheet.N
    g = fbm(n, seed, 5, 4)
    h = fbm(n, seed + 1, 3, 3)
    v = np.clip(0.2 + 0.7 * g * (0.6 + 0.4 * h), 0, 1)
    rgb = _colorize(v, stops)
    r = ridge(warp(h, g, 12))
    crack = r > 0.935
    dark = np.array(stops[0], np.uint8)
    rgb[crack] = (dark * 0.5).astype(np.uint8)
    rng = np.random.default_rng(seed + 7)
    spk = rng.random((n, n)) > 0.992
    glow = np.where(crack, crack_amt, 0.0) + np.where(spk, 0.7, 0.0)
    return Sheet(rgb, np.full((n, n), 255, np.uint8), np.clip(glow, 0, 1), ember)


def sheet_solid(rgb, glow=0.0, glow_col=(255, 255, 255), alpha=255, noise=0.06, seed=1):
    """A flat colour with a touch of grain (trim metal, cores, lenses)."""
    n = Sheet.N
    g = fbm(n, seed, 8, 2) - 0.5
    arr = np.zeros((n, n, 3), float)
    for i in range(3):
        arr[..., i] = rgb[i] * (1 + g * noise * 2)
    return Sheet(np.clip(arr, 0, 255).astype(np.uint8), np.full((n, n), alpha, np.uint8), np.full((n, n), glow), glow_col)


SHADE = {"top": 1.14, "bottom": 0.58, "north": 1.0, "south": 0.74, "west": 0.86, "east": 0.93}


# =====================================================================================================================
# geo piece
# =====================================================================================================================
def orient_y(dx, dy, dz):
    """Cube rotation (rx, ry, rz) in degrees that turns a cube's local +y axis to the direction (dx, dy, dz) (the file frame, see the module doc)."""
    d = math.sqrt(dx * dx + dy * dy + dz * dz) or 1.0
    dx, dy, dz = dx / d, dy / d, dz / d
    rx = math.degrees(math.asin(max(-1.0, min(1.0, dz))))
    c = math.cos(math.radians(rx))
    rz = math.degrees(math.atan2(-dx * (1 if c > 1e-6 else 0), dy)) if c > 1e-6 else 0.0
    return (round(rx, 2), 0, round(rz, 2))


class Piece:
    """One GeoLite model with a shelf-packed, face-painted atlas. cube() takes whole-pixel sizes (box uv); the material is a name in self.mats."""

    def __init__(self, key, name, w=128, h=128, seed=1, mats=None):
        self.m = Model(key, name, w, h)
        self.key, self.name, self.w, self.h = key, name, w, h
        self.x = self.y = self.rowh = 0
        self.jobs = []
        self.shared = {}
        self.mats = mats if mats is not None else {}
        self.anim = Anim()
        self.rng = random.Random(seed)
        self.nprng = np.random.default_rng(seed)

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def _alloc(self, cw, ch):
        if self.x + cw > self.w:
            self.x, self.y, self.rowh = 0, self.y + self.rowh, 0
        if self.x + cw > self.w:
            raise RuntimeError("cube %dx%d is wider than the atlas %s_%s (%d)" % (cw, ch, self.key, self.name, self.w))
        u, v = self.x, self.y
        self.x += cw
        self.rowh = max(self.rowh, ch)
        return u, v

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, share=None, pivot=None):
        """A painted cube. size is rounded to whole pixels (minimum 1). rot turns it about its own centre unless a pivot is given."""
        sx, sy, sz = [max(1, int(round(s))) for s in size]
        k = (share, sx, sy, sz, mat) if share else None
        if k and k in self.shared:
            u, v = self.shared[k]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, mat))
            if k:
                self.shared[k] = (u, v)
        pv = None
        if rot and any(rot):
            pv = list(pivot) if pivot is not None else [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=list(rot) if (rot and any(rot)) else None, pivot=pv)

    # ------------------------------------------------------------------ shapes
    def puff(self, bone, c, r, mat, share=None, squash=1.0, turn=0):
        """A round-looking lump at c = (x, y, z) of radius r pixels, built only of upright cubes (a fat cross of three cubes, four for the bigger ones)
        so it reads as a soft blocky cloud, never as a jagged crystal. `turn` is reserved. Returns the cube count."""
        r = max(1.0, r)
        s = 2 * r
        sy = s * squash
        k = 0.62
        n = 0
        sh = share or None

        def box(sz, tag, rot=None):
            nonlocal n
            o = (c[0] - sz[0] / 2.0, c[1] - sz[1] / 2.0, c[2] - sz[2] / 2.0)
            self.cube(bone, o, sz, mat, rot=rot, share=(sh + tag) if sh else None)
            n += 1
        if r < 2.1:
            box((s, sy, s), "a")
            return n
        box((s * 0.84, sy * 0.84, s * 0.84), "a")
        box((s, sy * k, s * k), "b")
        box((s * k, sy, s * k), "c")
        if r >= 3.0:
            box((s * k, sy * k, s), "d")
        return n

    def bar(self, bone, a, b, thick, mat, thick2=None, share=None):
        """A thin cube from point a to point b (local y axis along the segment). thick = width and depth; returns nothing."""
        dx, dy, dz = b[0] - a[0], b[1] - a[1], b[2] - a[2]
        ln = math.sqrt(dx * dx + dy * dy + dz * dz)
        if ln < 0.5:
            return
        mid = ((a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0)
        t = max(1, int(round(thick)))
        L = max(1, int(round(ln)))
        o = (mid[0] - t / 2.0, mid[1] - L / 2.0, mid[2] - t / 2.0)
        self.cube(bone, o, (t, L, t2(thick2, t)), mat, rot=orient_y(dx, dy, dz), share=share)

    def chain(self, bone, pts, thick, mat, taper=0.0, share=None):
        """Bars between consecutive points; the thickness shrinks by `taper` (0..1) over the whole chain."""
        n = len(pts) - 1
        for i in range(n):
            th = max(1.0, thick * (1.0 - taper * i / max(1, n)))
            self.bar(bone, pts[i], pts[i + 1], th, mat, share=(share + str(i)) if share else None)

    def spike(self, bone, base, axis, length, width, mat, layers=3, share=None):
        """A stepped taper from `base` along axis ('+y', '-z' ...) : a crystal / thorn / icicle made of stacked cubes shrinking to the tip."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        for k in range(layers):
            w = max(1, int(round(width * (1.0 - k / float(layers)) ** 0.9)))
            lo = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            o = [base[i] - w / 2.0 for i in range(3)]
            s = [w, w, w]
            o[a] = lo
            s[a] = seg
            self.cube(bone, tuple(o), tuple(s), mat, share=(share + str(k)) if share else None)

    def ring(self, bone, c, radius, thick, n, mat, axis="y", tilt=(0, 0, 0), share=None, depth=None):
        """n cubes round a circle (axis 'y' = a horizontal ring): the cheap torus of the wind and the halos."""
        t = max(1, int(round(thick)))
        d = max(1, int(round(depth if depth is not None else thick)))
        ln = max(1, int(round(2 * math.pi * radius / n * 1.15)))
        for i in range(n):
            a = 2 * math.pi * i / n
            px, pz = math.cos(a) * radius, math.sin(a) * radius
            if axis == "y":
                o = (c[0] + px - ln / 2.0, c[1] - t / 2.0, c[2] + pz - d / 2.0)
                self.cube(bone, o, (ln, t, d), mat, rot=(0, round(-math.degrees(a) + 90, 2), 0), share=share)
            elif axis == "z":                                   # a ring standing in the xy plane (a halo behind the back)
                o = (c[0] + px - ln / 2.0, c[1] + pz - t / 2.0, c[2] - d / 2.0)
                self.cube(bone, o, (ln, t, d), mat, rot=(0, 0, round(math.degrees(a), 2)), share=share)
            else:                                               # axis 'x': a ring in the yz plane
                o = (c[0] - d / 2.0, c[1] + px - t / 2.0, c[2] + pz - ln / 2.0)
                self.cube(bone, o, (d, t, ln), mat, rot=(round(-math.degrees(a), 2), 0, 0), share=share)

    # ------------------------------------------------------------------ painting
    def fit(self):
        """The atlas grows downwards while cubes are added; at save time its height becomes the next power of two that holds them."""
        used = self.y + self.rowh
        h = 8
        while h < used:
            h *= 2
        self.h = h
        self.m.tex_h = h

    def paint(self):
        self.fit()
        img = np.zeros((self.h, self.w, 4), np.uint8)
        gl = np.zeros((self.h, self.w, 4), np.uint8)
        for (u, v, sx, sy, sz, mat) in self.jobs:
            sh = self.mats[mat]
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self._face(img, gl, x, y, w, h, sh, SHADE[kind])
        return Image.fromarray(img, "RGBA"), Image.fromarray(gl, "RGBA")

    def _face(self, img, gl, x0, y0, w, h, sh, shade):
        if w <= 0 or h <= 0:
            return
        n = Sheet.N
        ox, oy = self.rng.randrange(n), self.rng.randrange(n)
        ys = (np.arange(h) + oy) % n
        xs = (np.arange(w) + ox) % n
        rgb = sh.rgb[np.ix_(ys, xs)].astype(float)
        a = sh.alpha[np.ix_(ys, xs)]
        g = sh.glow[np.ix_(ys, xs)]
        k = shade
        rgb = rgb * k
        if w >= 5 and h >= 5:                                   # a lit edge top-left, a shaded edge bottom-right
            rgb[0, :, :] *= 1.08
            rgb[:, 0, :] *= 1.05
            rgb[-1, :, :] *= 0.88
            rgb[:, -1, :] *= 0.9
        img[y0:y0 + h, x0:x0 + w, :3] = np.clip(rgb, 0, 255).astype(np.uint8)
        img[y0:y0 + h, x0:x0 + w, 3] = a
        ga = np.clip(g * 255 * (0.55 + 0.45 * shade), 0, 255).astype(np.uint8)
        gl[y0:y0 + h, x0:x0 + w, 0] = sh.glow_col[0]
        gl[y0:y0 + h, x0:x0 + w, 1] = sh.glow_col[1]
        gl[y0:y0 + h, x0:x0 + w, 2] = sh.glow_col[2]
        gl[y0:y0 + h, x0:x0 + w, 3] = ga

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        used = self.y + self.rowh
        print("%-26s %3d cubes  %dx%d  atlas rows used %d" % (self.m.id, self.m.cube_count(), self.w, self.h, used))
        return self.m.id


def t2(v, default):
    return default if v is None else max(1, int(round(v)))


def loop(length, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop (first == last)."""
    return {round(length * k / steps, 3): f(k / float(steps)) for k in range(steps + 1)}


def sine(length, amp, phase=0.0, axis=1, steps=8, base=0.0):
    return loop(length, lambda p: tuple((base + amp * math.sin((p + phase) * 2 * math.pi)) if i == axis else 0 for i in range(3)), steps)


def sine3(length, amps, phase=0.0, steps=8):
    return loop(length, lambda p: tuple(amps[i] * math.sin((p + phase) * 2 * math.pi) for i in range(3)), steps)


def spin(length, axis=1, steps=4, turns=1):
    return {round(length * k / steps, 3): tuple((360.0 * turns * k / steps) if i == axis else 0 for i in range(3)) for k in range(steps + 1)}


# =====================================================================================================================
# particle textures (white / grey sprites with alpha: the vertex colour tints them)
# =====================================================================================================================
SS = 2


def _canvas(w, h):
    return np.zeros((h * SS, w * SS), float)


def _finish(arr, w, h, name, tint=(255, 255, 255), colour=None, gamma=1.0):
    """arr (h*SS, w*SS) float 0..1 -> RGBA PNG with white (or `tint`) colour and arr as alpha. `colour` is an optional (h, w, 3) array."""
    im = Image.fromarray((np.clip(arr, 0, 1) ** gamma * 255).astype(np.uint8), "L").resize((w, h), Image.LANCZOS)
    a = np.array(im)
    rgba = np.zeros((h, w, 4), np.uint8)
    if colour is None:
        rgba[..., 0], rgba[..., 1], rgba[..., 2] = tint
    else:
        rgba[..., :3] = colour
    rgba[..., 3] = a
    os.makedirs(PARTICLE_DIR, exist_ok=True)
    Image.fromarray(rgba, "RGBA").save(os.path.join(PARTICLE_DIR, name + ".png"), optimize=True)
    print("wrote", name, (w, h))


def grid(w, h):
    ys, xs = np.mgrid[0:h * SS, 0:w * SS]
    return (xs + 0.5) / SS, (ys + 0.5) / SS


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def particle_cloud(name, seed, size=128, lobes=7, hard=0.0, tint=(255, 255, 255), detail=0.35):
    """A soft billowing cloud sprite: overlapping lobes, noise-eaten edge, bright core. hard raises the contrast of the edge."""
    w = h = size
    X, Y = grid(w, h)
    rng = np.random.default_rng(seed)
    cx, cy = w / 2.0, h / 2.0
    dens = np.zeros_like(X)
    for i in range(lobes):
        a = rng.uniform(0, 2 * math.pi)
        d = rng.uniform(0, 0.24) * w
        r = rng.uniform(0.17, 0.30) * w
        px, py = cx + math.cos(a) * d, cy + math.sin(a) * d
        dist = np.hypot(X - px, Y - py) / r
        dens = np.maximum(dens, 1 - smoothstep(0.35, 1.0, dist))
    big = fbm(256, seed + 3, 4, 4)
    nz = Image.fromarray((big * 255).astype(np.uint8)).resize((w * SS, h * SS), Image.BICUBIC)
    nz = np.array(nz) / 255.0
    dens = np.clip(dens * (1 - detail + detail * 2 * nz) , 0, 1)
    edge = np.hypot(X - cx, Y - cy) / (w * 0.5)
    dens *= 1 - smoothstep(0.78, 1.0, edge)
    if hard:
        dens = smoothstep(hard, 1.0, dens)
    _finish(dens * 0.92, w, h, name, tint)


def particle_soft(name, size=64, power=2.2, tint=(255, 255, 255)):
    w = h = size
    X, Y = grid(w, h)
    r = np.hypot(X - w / 2.0, Y - h / 2.0) / (w / 2.0)
    _finish((1 - np.clip(r, 0, 1)) ** power, w, h, name, tint)


def particle_ring(name, size=256, inner=0.78, outer=0.97, soft=0.06, ticks=0, seed=1, wobble=0.0, tint=(255, 255, 255)):
    """A thin ring (inner / outer radius as a fraction of the half size), optional tick marks and a noise wobble."""
    w = h = size
    X, Y = grid(w, h)
    r = np.hypot(X - w / 2.0, Y - h / 2.0) / (w / 2.0)
    th = np.arctan2(Y - h / 2.0, X - w / 2.0)
    if wobble:
        r = r + wobble * (fbm(128, seed, 6, 3)[((th + math.pi) / (2 * math.pi) * 127).astype(int) % 128, 0] - 0.5)
    m = smoothstep(inner - soft, inner, r) * (1 - smoothstep(outer, outer + soft, r))
    if ticks:
        t = (np.cos(th * ticks) > 0.55) & (r > inner - 0.07) & (r < inner)
        m = np.maximum(m, t * 0.8 * smoothstep(inner - 0.08, inner - 0.04, r))
    _finish(m, w, h, name, tint)


def particle_streak(name, w=64, h=256, seed=1, core=0.16, wisp=0.5, tint=(255, 255, 255)):
    """A speed streak / wisp: bright thin core that fades to both ends, a noisy halo (u across, v along; top = tail)."""
    X, Y = grid(w, h)
    u = (X - w / 2.0) / (w / 2.0)
    v = Y / h
    n = fbm(256, seed, 4, 3)
    nz = np.array(Image.fromarray((n * 255).astype(np.uint8)).resize((w * SS, h * SS), Image.BICUBIC)) / 255.0
    width = core + wisp * (0.25 + 0.75 * v) * (0.6 + 0.8 * nz)
    m = np.clip(1 - np.abs(u) / np.maximum(width, 1e-3), 0, 1) ** 1.6
    m *= smoothstep(0.0, 0.25, v) * (1 - 0.15 * v)
    _finish(m, w, h, name, tint)


def particle_shape(name, draw_fn, size=128, blur=0.6, glow=0.25, tint=(255, 255, 255)):
    """draw_fn(ImageDraw, size) draws white shapes on black at SS x resolution; a soft glow halo is added under them."""
    big = Image.new("L", (size * SS, size * SS), 0)
    d = ImageDraw.Draw(big)
    draw_fn(d, size * SS)
    sharp = big.filter(ImageFilter.GaussianBlur(blur * SS))
    halo = big.filter(ImageFilter.GaussianBlur(size * SS * 0.03)) if glow else None
    a = np.array(sharp, float) / 255.0
    if halo is not None:
        a = np.maximum(a, np.array(halo, float) / 255.0 * glow * 3)
    _finish(a, size, size, name, tint)


def polar_poly(cx, cy, pts):
    return [(cx + r * math.cos(a), cy + r * math.sin(a)) for r, a in pts]

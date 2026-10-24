"""
FIRE remake (new round), PREVIEW ONLY: the quad / mesh collector `Out` used by the effect builders in fire_new_effects.py.
It produces the same kind of data the real layers hand to VfxVertexBuffer (camera-relative quads with a texture, a blend and a colour) plus, for direction C,
authored 3D meshes (the sdw3 mesh text format) drawn as toon-shaded flat quads.
"""
import math
import os

import numpy as np

import fire_new_raster as R
from fire_new_raster import UP, V, norm

ROOT = R.pv.ROOT
MESH_DIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "fire_new", "meshes")

# toon palette (0..1)
GOLD = np.array([1.0, 0.74, 0.20])
CORE = np.array([1.0, 0.94, 0.62])
TONE_LIT = np.array([1.0, 0.78, 0.24])
TONE_SHADE = np.array([0.62, 0.10, 0.06])
RIM = np.array([1.0, 0.86, 0.38])
SHADE = np.array([0.60, 0.33, 0.30])
RED = np.array([226, 58, 16]) / 255.0
HOTW = np.array([255, 248, 214]) / 255.0


def rgba(r, g, b, a=1.0):
    return (r / 255.0, g / 255.0, b / 255.0, a)


WHITE = (1.0, 1.0, 1.0, 1.0)
GLOW = (1.0, 0.54, 0.16, 1.0)
HOTC = (1.0, 0.93, 0.62, 1.0)


def tint(c, a):
    return (c[0], c[1], c[2], c[3] * a)


# ------------------------------------------------------------------------------------------------ meshes (the sdw3 text format)
_MESH = {}


class Mesh:
    def __init__(self, name, quads):
        self.name = name
        self.q = quads                                 # (layer, pts (4,3) px, normal (3), rgb 0..255)

    def counts(self):
        return len(self.q)


def mesh(name):
    if name not in _MESH:
        quads = []
        with open(os.path.join(MESH_DIR, name + ".txt"), encoding="utf-8") as fh:
            for line in fh:
                if not line.startswith("q "):
                    continue
                v = line.split()
                layer = int(v[1])
                nums = [float(x) for x in v[2:]]
                pts = np.array([[nums[k * 5], nums[k * 5 + 1], nums[k * 5 + 2]] for k in range(4)])
                quads.append((layer, pts, np.array(nums[20:23]), np.array(nums[23:26])))
        _MESH[name] = Mesh(name, quads)
    return _MESH[name]


def basis(yaxis, xhint):
    """the sdw3 painter's frame: the mesh +y axis goes to `yaxis`, the mesh +x axis ends up as `xhint` as nearly as the y axis allows (z = x cross y)"""
    y = norm(np.asarray(yaxis, float))
    xh = np.asarray(xhint, float)
    x = xh - y * np.dot(xh, y)
    if np.linalg.norm(x) < 1e-4:
        x = np.cross(y, V(1, 0, 0)) if abs(y[0]) < 0.9 else np.cross(y, UP)
    x = norm(x)
    z = np.cross(x, y)
    return x, y, z


class Out:
    def __init__(self, cam, target, age, dur, seed=12345):
        self.cam, self.target = np.array(cam, float), np.array(target, float)
        self.f, self.r, self.u = R.pv.basis(self.cam, self.target)
        self.age, self.dur, self.seed = age, dur, seed
        self.q = []                                    # textured quads
        self.m = []                                    # opaque mesh quads (toon coloured)
        self.light = norm(-0.38 * self.r + 0.82 * self.u - 0.55 * self.f)      # key light: from above-left, toward the camera

    # -- counts
    def verts(self):
        return 4 * (len(self.q) + len(self.m))

    # -- textured primitives -----------------------------------------------------------------------------------
    def _emit(self, pts, uv, tex, col, blend):
        pts = np.array([np.asarray(p, float) - self.cam for p in pts])
        col = [col] * 4 if np.ndim(col[0]) == 0 else list(col)
        self.q.append({"t": tex, "b": blend, "o": 1 if blend == "ADD" else 0, "d": float(np.linalg.norm(pts.mean(axis=0))), "p": pts, "uv": uv, "c": np.array(col)})

    def plane(self, c, ax, ay, hw, hh, tex, col=WHITE, blend="ALPHA", u=(0.0, 1.0), flip=False, vv=(0.0, 1.0)):
        c, ax, ay = np.asarray(c, float), np.asarray(ax, float), np.asarray(ay, float)
        u0, u1 = (u[1], u[0]) if flip else u
        pts = [c - ax * hw + ay * hh, c + ax * hw + ay * hh, c + ax * hw - ay * hh, c - ax * hw - ay * hh]
        self._emit(pts, [(u0, vv[0]), (u1, vv[0]), (u1, vv[1]), (u0, vv[1])], tex, col, blend)

    def bb(self, c, w, h, tex, roll=0.0, col=WHITE, blend="ALPHA", flip=False):
        ca, sa = math.cos(roll), math.sin(roll)
        self.plane(c, self.r * ca + self.u * sa, -self.r * sa + self.u * ca, w / 2, h / 2, tex, col, blend, flip=flip)

    def dirbb(self, c, d, w, h, tex, col=WHITE, blend="ALPHA", flip=False):
        """a camera-facing card whose texture +x points along the world direction d (as seen from the camera)"""
        dv = np.asarray(d, float)
        dv = dv - self.f * np.dot(dv, self.f)
        ax = norm(dv) if np.linalg.norm(dv) > 1e-6 else self.r
        if np.dot(ax, self.r) < 0:                                          # pointing left on screen: mirror instead of turning the card upside down
            ax, flip = -ax, not flip
        ay = -norm(np.cross(self.f, ax))
        self.plane(c, ax, ay, w / 2, h / 2, tex, col, blend, flip=flip)

    def strip(self, a, b, width, tex, col=WHITE, blend="ALPHA", u=(0.0, 1.0), flip=False, cols=None):
        """camera-facing strip from a to b; the texture +x runs from a to b"""
        a, b = np.asarray(a, float), np.asarray(b, float)
        d = b - a
        ln = np.linalg.norm(d)
        if ln < 1e-6:
            return
        ax = d / ln
        mid = (a + b) / 2
        ay = norm(np.cross(ax, mid - self.cam))
        u0, u1 = (u[1], u[0]) if flip else u
        hw = width / 2
        pts = [a + ay * hw, b + ay * hw, b - ay * hw, a - ay * hw]
        cc = cols if cols else [col] * 4
        self._emit(pts, [(u0, 0), (u1, 0), (u1, 1), (u0, 1)], tex, cc, blend)

    def upright(self, base, w, h, tex, col=WHITE, blend="ALPHA", flip=False, lean=0.0):
        """a flame standing on `base`: faces the camera about the vertical axis, texture bottom on the ground"""
        r = np.array([self.r[0], 0.0, self.r[2]])
        r = norm(r) if np.linalg.norm(r) > 1e-6 else V(1, 0, 0)
        b = np.asarray(base, float)
        top = b + UP * h + r * lean
        pts = [top - r * w / 2, top + r * w / 2, b + r * w / 2, b - r * w / 2]
        u0, u1 = (1, 0) if flip else (0, 1)
        self._emit(pts, [(u0, 0), (u1, 0), (u1, 1), (u0, 1)], tex, col, blend)

    def ground(self, c, rad, tex, spin=0.0, col=WHITE, blend="ALPHA", ry=None):
        ca, sa = math.cos(spin), math.sin(spin)
        c = np.asarray(c, float)
        ax, ay = V(ca, 0, sa), V(-sa, 0, ca)
        self.plane(c, ax, ay, rad, rad if ry is None else ry, tex, col, blend)

    # -- meshes ------------------------------------------------------------------------------------------------------
    def mesh(self, m, origin, yaxis, xhint=UP, scale=(1 / 16, 1 / 16, 1 / 16), cool=0.0, hot=0.0, alpha=1.0, layers=(0, 2), toon=True):
        """draw an authored mesh: the +y axis to `yaxis`, +x to `xhint`, scale in blocks per mesh pixel (sx, sy, sz).  Colour per quad = the heat tint of the mesh, then
        3 toon tones from a fixed key light, the emissive core where the quad faces the camera, a bright rim where it grazes the silhouette (all decided per quad)."""
        if isinstance(m, str):
            m = mesh(m)
        x, y, z = basis(yaxis, xhint)
        S = np.array(scale, float)
        o = np.asarray(origin, float)
        Rm = np.stack([x, y, z], axis=1)                                   # columns
        for layer, pts, n, rgb in m.q:
            if layer not in layers:
                continue
            wp = o + (pts * S) @ Rm.T
            wn = Rm @ (n / np.maximum(S, 1e-9))
            wn = norm(wn)
            ctr = wp.mean(axis=0)
            base = rgb / 255.0
            if layer == 2:
                col = base                                                 # emissive ornaments are flat
            else:
                if cool:
                    base = base * (1 - cool) + RED * cool
                if hot:
                    base = base * (1 - hot) + HOTW * hot
                vdir = norm(self.cam - ctr)
                d = float(np.dot(wn, vdir))
                f = abs(d)
                nf = wn if d >= 0 else -wn
                lam = float(np.dot(nf, self.light))
                if toon:
                    # three flat tones from the key light (lit / mid / shade, hue shifting yellow -> red), the emissive core where the quad faces the viewer, a bright rim where it grazes
                    if lam > 0.30:
                        col = base * 0.68 + TONE_LIT * 0.32
                    elif lam > -0.10:
                        col = base
                    else:
                        col = base * 0.45 + TONE_SHADE * 0.55
                    if f > 0.93 and lam > -0.10:
                        col = col * 0.40 + CORE * 0.60                                                         # the emissive core
                    elif f < 0.27:
                        col = col * 0.12 + RIM * 0.88                                                          # rim light
                else:
                    col = base
            self.m.append({"p": wp - self.cam, "rgb": np.clip(col, 0, 1), "a": alpha, "d": float(np.linalg.norm(ctr - self.cam))})

    # -- finishing ---------------------------------------------------------------------------------------------------
    def finish(self):
        self.q.sort(key=lambda q: (q["o"], -q["d"]))
        return self.q, self.m


def draw(out, rast):
    q, m = out.finish()
    for mq in m:
        rast.flat(mq["p"], mq["rgb"], mq["a"])
    for t in q:
        rast.textured(t["p"], t["uv"], t["c"], t["t"], t["b"])

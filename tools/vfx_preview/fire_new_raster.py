"""
FIRE remake (new round), PREVIEW ONLY: the software rasteriser and the quad / mesh collector shared by tools/vfx_preview/fire_new_preview.py.

Differences to the repo's own preview.py raster (which stays untouched and is used for the BEFORE column through the real Java layers):
  * bilinear, premultiplied texture sampling with clamp (the cel textures have hard edges that nearest sampling would stair-step at 2x),
  * a depth buffer: opaque 3D toon meshes (direction C) write it, alpha / additive quads are depth-TESTED against it (as the game does),
  * flat-coloured mesh quads (the toon colour is decided per quad, like the per-quad tint the sdw3 mesh loader hands to the vertex consumer),
  * supersampling (render at SSx, box-filter down).
Honest limits: painter's order inside the alpha / additive passes (no per-pixel sort), no bloom, no world light, never seen in a game.
"""
import math

import numpy as np
from PIL import Image

import preview as pv

UP = np.array([0.0, 1.0, 0.0])


def V(*a):
    return np.array(a, float)


def norm(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def hsh(seed, i, salt=0):
    x = math.sin(i * 12.9898 + salt * 78.233 + seed * 0.001) * 43758.5453
    return x - math.floor(x)


def ease_out(t):
    t = min(1.0, max(0.0, t))
    return 1 - (1 - t) ** 3


def ease_in(t):
    t = min(1.0, max(0.0, t))
    return t ** 3


def ease_back(t):
    t = min(1.0, max(0.0, t))
    c1, c3 = 1.70158, 2.70158
    return 1 + c3 * (t - 1) ** 3 + c1 * (t - 1) ** 2


def clamp(x, a=0.0, b=1.0):
    return max(a, min(b, x))


def lerp(a, b, t):
    return a + (b - a) * t


# ------------------------------------------------------------------------------------------------ textures
_TEX = {}


def load_dir(prefix, folder):
    import os
    for fn in sorted(os.listdir(folder)):
        if fn.endswith(".png") and fn != "tex_sheet.png":
            a = np.asarray(Image.open(os.path.join(folder, fn)).convert("RGBA"), dtype=np.float32) / 255.0
            a[..., :3] *= a[..., 3:4]                                   # premultiplied
            _TEX[prefix + fn[:-4]] = a


def tex(name):
    if name in _TEX:
        return _TEX[name]
    a = pv.texture(name).copy()                                          # the repo textures of the BEFORE column (straight alpha -> premultiplied)
    a[..., :3] *= a[..., 3:4]
    _TEX[name] = a
    return a


def sample(t, U, Vv):
    th, tw = t.shape[:2]
    x = np.clip(U * tw - 0.5, 0, tw - 1.001)
    y = np.clip(Vv * th - 0.5, 0, th - 1.001)
    x0, y0 = x.astype(np.int32), y.astype(np.int32)
    fx, fy = (x - x0)[..., None], (y - y0)[..., None]
    a = t[y0, x0] * (1 - fx) + t[y0, x0 + 1] * fx
    b = t[y0 + 1, x0] * (1 - fx) + t[y0 + 1, x0 + 1] * fx
    return a * (1 - fy) + b * fy


# ------------------------------------------------------------------------------------------------ raster
class Raster:
    def __init__(self, w, h, cam, target, scene, fov=54.0):
        self.W, self.H, self.fov = w, h, fov
        self.cam, self.target = np.array(cam, float), np.array(target, float)
        self.f, self.r, self.u = pv.basis(self.cam, self.target)
        self.k = (h / 2) / math.tan(math.radians(fov / 2))
        old = (pv.W, pv.H)
        pv.W, pv.H = w, h
        ofov, pv.FOV = pv.FOV, fov
        self.img = pv.background(scene, cam, target).copy()
        img = pv.draw_figures(self.img, scene, cam, target)
        pv.W, pv.H = old
        pv.FOV = ofov
        self.img = np.array(img, np.float32)
        self.depth = np.full((h, w), 1e9, np.float32)
        self.yy, self.xx = np.mgrid[0:h, 0:w].astype(np.float32)

    # ---- projection
    def proj(self, pts):
        z = pts @ self.f
        sx = self.W / 2 + (pts @ self.r) / z * self.k
        sy = self.H / 2 - (pts @ self.u) / z * self.k
        return sx, sy, z

    def _tris(self, pts, extra=None):
        """yield (minx, maxx, miny, maxy, a, b, c, mask, iz, w_) per triangle of the quad (0,1,2) (0,2,3)"""
        z = pts @ self.f
        if np.any(z < 0.05):
            return
        sx = self.W / 2 + (pts @ self.r) / z * self.k
        sy = self.H / 2 - (pts @ self.u) / z * self.k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, min(x0, x1, x2) - 1)), int(min(self.W - 1, max(x0, x1, x2) + 1)) + 1
            miny, maxy = int(max(0, min(y0, y1, y2) - 1)), int(min(self.H - 1, max(y0, y1, y2) + 1)) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = self.xx[miny:maxy, minx:maxx] + 0.5
            Y = self.yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            m = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not m.any():
                continue
            iz = np.array([1 / z[i] for i in tri])
            w_ = a * iz[0] + b * iz[1] + c * iz[2]
            yield tri, minx, maxx, miny, maxy, a, b, c, m, iz, w_

    # ---- opaque mesh quad (flat colour, depth write)
    def flat(self, pts, rgb, alpha=1.0):
        for tri, minx, maxx, miny, maxy, a, b, c, m, iz, w_ in self._tris(pts):
            zpix = 1.0 / np.maximum(w_, 1e-9)
            reg_d = self.depth[miny:maxy, minx:maxx]
            ok = m & (zpix < reg_d)
            if not ok.any():
                continue
            region = self.img[miny:maxy, minx:maxx]
            region[ok] = region[ok] * (1 - alpha) + np.array(rgb, np.float32) * alpha
            if alpha >= 0.999:
                reg_d[ok] = zpix[ok]

    # ---- textured quad (alpha / add), depth tested, no depth write
    def textured(self, pts, uv, col, texname, blend, flat_uv=False):
        t = tex(texname)
        uv = np.asarray(uv, float)
        col = np.asarray(col, float)
        for tri, minx, maxx, miny, maxy, a, b, c, m, iz, w_ in self._tris(pts):
            def interp(vals):
                return (a * vals[0] * iz[0] + b * vals[1] * iz[1] + c * vals[2] * iz[2]) / w_
            U = interp([uv[i, 0] for i in tri])
            Vv = interp([uv[i, 1] for i in tri])
            vc = np.stack([interp([col[i, ch] for i in tri]) for ch in range(4)], axis=-1)
            zpix = 1.0 / np.maximum(w_, 1e-9)
            ok = m & (zpix < self.depth[miny:maxy, minx:maxx] + 1e-4)
            if not ok.any():
                continue
            s = sample(t, np.clip(U, 0, 1), np.clip(Vv, 0, 1))               # premultiplied rgb, straight alpha in [..., 3]
            al = s[..., 3:4] * vc[..., 3:4]
            rgb = s[..., :3] * vc[..., :3] * vc[..., 3:4]                     # premultiplied * tint * quad alpha
            okm = ok[..., None]
            region = self.img[miny:maxy, minx:maxx]
            if blend in ("ALPHA", "WATER"):
                region[:] = np.where(okm, region * (1 - al) + rgb, region)
            else:                                                             # ADD / FIRE
                region[:] = np.where(okm, region + rgb, region)

    def result(self, tw, th):
        im = Image.fromarray((np.clip(self.img, 0, 1) * 255 + 0.5).astype(np.uint8))
        return im.resize((tw, th), Image.LANCZOS)

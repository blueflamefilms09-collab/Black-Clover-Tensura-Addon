#!/usr/bin/env python3
"""
WATER remake, PHASE A = PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

For every water shape three directions are built and rasterised next to the CURRENT look (produced by the real layer class through the repo's Java preview):

    A  anime cel         quads (the same data the real layers hand to VfxVertexBuffer), 3-band cel textures, white highlight, cyan outline      tools/gen_water_new_textures.py
    B  volumetric glass  quads, translucent glass sheets, baked caustics, fresnel rim, bubbles                                                   tools/gen_water_new_textures.py
    C  real 3D toon      real mesh geometry (dragon head with jaw and teeth, wave segments, spheres, domes, horns, rings, feathers), authored in the
                         repo's mesh vocabulary (tools/gen_sdw3_models.py `Mesh`), toon shaded per quad on the CPU: 3 tones by the light, one hard specular,
                         rim light, emissive core as an additive layer                                                                            tools/gen_water_new_meshes.py

    python tools/gen_water_new_textures.py
    python tools/gen_water_new_meshes.py
    python tools/vfx_preview/water_new_preview.py        # sheets + compare + budget json (needs JDK 21, numpy, Pillow, like preview.py)

Output: docs/previews/vfx_remake/water_new/water_new_{A,B,C}.png, water_compare.png, water_budget.json.
Honest limits: headless software rasteriser, painter's-order sort for the sprites and a z-buffer for the meshes, no bloom, no world light, no terrain, never seen in a game.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402  (the repo's own preview tool)
import mc_wind_preview as mw                                                # noqa: E402  (only its quad collector `Out` is reused)
from mc_wind_preview import Out, V, norm, ease_out                          # noqa: E402

ROOT = pv.ROOT
OUTDIR = os.path.join(ROOT, "docs", "previews", "vfx_remake", "water_new")
TEXDIR = os.path.join(OUTDIR, "tex")
MESHDIR = os.path.join(OUTDIR, "meshes")
CW, CH = 560, 420
UP = np.array([0.0, 1.0, 0.0])
SS = int(os.environ.get("WATER_SS", "2"))                                   # mesh supersampling (anti-aliasing)


def clamp(x, a=0.0, b=1.0):
    return min(b, max(a, x))


def rgba(r, g, b, a=1.0):
    return (int(round(clamp(a) * 255)) << 24) | (int(r) << 16) | (int(g) << 8) | int(b)


def W(a=1.0):
    return rgba(255, 255, 255, a)


def ease_back(t):
    t = clamp(t)
    c1 = 1.70158
    return 1 + (c1 + 1) * (t - 1) ** 3 + c1 * (t - 1) ** 2


def ease_in(t):
    return clamp(t) ** 3


def hs(seed, i, salt):
    return mw.hsh(seed, i, salt)


# ================================================================================================ sprite collector (A and B)
class QO(Out):
    def strip2(self, a, b, width, tex, color=0xFFFFFFFF, tile=1.0, uoff=0.0, flip=False, blend="ALPHA"):
        """camera-facing strip from a to b, texture +x runs a -> b, texture top kept 'up' on screen"""
        a, b = np.asarray(a, float), np.asarray(b, float)
        d = b - a
        ln = np.linalg.norm(d)
        if ln < 1e-6:
            return
        ax = d / ln
        mid = (a + b) / 2
        ay = norm(np.cross(ax, mid - self.cam))
        if ay[1] < 0:
            ay = -ay
        self.plane(mid, ax, ay, ln / 2, width / 2, tex, color, flip, u=(uoff, uoff + tile), blend=blend)

    def ground(self, c, r, tex, color=0xFFFFFFFF, rot=0.0, blend="ALPHA"):
        ax = V(math.cos(rot), 0, math.sin(rot))
        ay = V(-math.sin(rot), 0, math.cos(rot))
        self.plane(c, ax, ay, r, r, tex, color, blend=blend)

    def upright(self, c, w, h, tex, color=0xFFFFFFFF, blend="ALPHA", flip=False):
        """base centre c, faces the camera round the vertical axis"""
        rr = V(self.r[0], 0, self.r[2])
        rr = norm(rr) if np.linalg.norm(rr) > 1e-6 else V(1, 0, 0)
        self.plane(c + UP * h / 2, rr, UP, w / 2, h / 2, tex, color, flip, blend=blend)

    def vplane(self, c, yaw, w, h, tex, color=0xFFFFFFFF, blend="ALPHA", flip=False):
        """a vertical plane with its base centre at c, turned by yaw round the vertical axis"""
        self.plane(c + UP * h / 2, V(math.cos(yaw), 0, math.sin(yaw)), UP, w / 2, h / 2, tex, color, flip, blend=blend)

    def dplane(self, c, ax, ay, w, h, tex, color=0xFFFFFFFF, blend="ALPHA", flip=False):
        self.plane(c, norm(ax), norm(ay), w / 2, h / 2, tex, color, flip, blend=blend)


# ================================================================================================ mesh pieces (C)
class MeshData:
    def __init__(self, path):
        L, P, N, T = [], [], [], []
        with open(path, encoding="utf-8") as f:
            for line in f:
                if not line.startswith("q "):
                    continue
                t = line.split()
                L.append(int(t[1]))
                P.append(np.array(t[2:22], float).reshape(4, 5)[:, :3])
                N.append([float(x) for x in t[22:25]])
                T.append([float(x) for x in t[25:28]])
        self.L, self.P, self.N, self.T = np.array(L), np.array(P), np.array(N), np.array(T) / 255.0
        self.n = len(L)

    def count(self, layer):
        return int((self.L == layer).sum())


MESHES = {}


def mesh(name):
    if name not in MESHES:
        MESHES[name] = MeshData(os.path.join(MESHDIR, name + ".txt"))
    return MESHES[name]


def nm(base, lod):
    """the lo variant of a piece when the LOD asks for it and the file exists"""
    if lod and os.path.isfile(os.path.join(MESHDIR, base + "_lo.txt")):
        return base + "_lo"
    return base


def basis_y(y, zhint):
    Y = norm(np.asarray(y, float))
    Z = np.asarray(zhint, float) - Y * np.dot(zhint, Y)
    if np.linalg.norm(Z) < 1e-4:
        Z = np.array([1.0, 0, 0]) - Y * Y[0]
        if np.linalg.norm(Z) < 1e-4:
            Z = np.array([0, 0, 1.0])
    Z = norm(Z)
    return np.cross(Y, Z), Y, Z


def basis_x(y, xhint):
    Y = norm(np.asarray(y, float))
    X = np.asarray(xhint, float) - Y * np.dot(xhint, Y)
    if np.linalg.norm(X) < 1e-4:
        X = np.array([1.0, 0, 0]) - Y * Y[0]
        if np.linalg.norm(X) < 1e-4:
            X = np.array([0, 0, 1.0])
    X = norm(X)
    return X, Y, np.cross(X, Y)


class CL:
    """the mesh instances of one frame: what the drawer would hand to the vertex consumer"""

    def __init__(self):
        self.items = []

    def add(self, name, pos, y, zhint=None, xhint=None, scale=1.0, alpha=1.0, glow=1.0, pal="water", foam_alpha=1.0):
        sc = scale if isinstance(scale, (tuple, list)) else (scale, scale, scale)
        if xhint is not None:
            X, Y, Z = basis_x(y, xhint)
        else:
            X, Y, Z = basis_y(y, zhint if zhint is not None else UP)
        R = np.stack([X * sc[0], Y * sc[1], Z * sc[2]], axis=1) / 16.0      # columns: mesh x, y, z -> world
        self.items.append(dict(name=name, R=R, t=np.asarray(pos, float), alpha=alpha, glow=glow, pal=pal, foam_alpha=foam_alpha))

    def counts(self):
        c = [0, 0, 0]
        for it in self.items:
            m = mesh(it["name"])
            for l in range(3):
                c[l] += m.count(l)
        return c


TONES = {"deep": [(18, 58, 128), (30, 92, 178), (84, 160, 232)], "water": [(30, 84, 164), (58, 150, 236), (140, 220, 255)], "foam": [(150, 190, 225), (214, 236, 252), (255, 255, 255)]}
RIM = np.array([210, 244, 255]) / 255.0
GLOWC = np.array([150, 235, 255]) / 255.0


def render_meshes(items, cam, target, ss=SS):
    """z-buffered software rasteriser: per-quad toon colour (3 tones by the normal against the sun, hard specular, rim), opaque pass, translucent pass far to near,
    additive glow pass depth-tested against the opaque pass. Returns premultiplied colour (H, W, 3), coverage (H, W), glow (H, W, 3) at the preview size."""
    Wd, Hd = pv.W, pv.H
    w, h = Wd * ss, Hd * ss
    f, r, u = pv.basis(cam, target)
    k = (h / 2) / math.tan(math.radians(pv.FOV / 2))
    SUN = norm(-0.55 * r + 0.8 * UP - 0.3 * f)                           # lit from the upper left, on the viewer's side (the drawer has the camera at hand)
    C = np.zeros((h, w, 3), np.float32)
    A = np.zeros((h, w), np.float32)
    G = np.zeros((h, w, 3), np.float32)
    ZB = np.zeros((h, w), np.float32)                                   # inverse depth, bigger = nearer
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    recs = []
    for it in items:
        m = mesh(it["name"])
        R = it["R"]
        Pw = m.P @ R.T + it["t"]                                         # (N, 4, 3)
        Rin = np.linalg.inv(R)
        Nw = m.N @ Rin
        Nw /= np.maximum(np.linalg.norm(Nw, axis=1, keepdims=True), 1e-9)
        cen = Pw.mean(axis=1)
        Vv = cam - cen
        Vv /= np.maximum(np.linalg.norm(Vv, axis=1, keepdims=True), 1e-9)
        ndv = np.sum(Nw * Vv, axis=1)
        Nw = np.where(ndv[:, None] < 0, -Nw, Nw)                         # two-sided lighting
        ndv = np.abs(ndv)
        hl = 0.5 + 0.5 * (Nw @ SUN)
        tone = np.where(hl > 0.86, 2, np.where(hl > 0.58, 1, 0))
        Hh = SUN[None, :] + Vv
        Hh /= np.maximum(np.linalg.norm(Hh, axis=1, keepdims=True), 1e-9)
        spec = np.sum(Nw * Hh, axis=1) > 0.993
        rim = ndv < 0.30
        for qi in range(m.n):
            lay = m.L[qi]
            if lay == 1:
                col = GLOWC * it["glow"]
                al = 1.0
            else:
                pal = TONES["foam" if lay == 2 else it["pal"]]
                col = np.array(pal[tone[qi]], np.float32) / 255.0 * m.T[qi]
                if spec[qi]:
                    col = col * 0.15 + 0.85
                elif rim[qi]:
                    col = col * 0.45 + RIM * 0.55
                al = it["foam_alpha"] if lay == 2 and it["alpha"] < 0.95 else it["alpha"]
            pc = Pw[qi] - cam
            z = pc @ f
            if np.any(z < 0.05):
                continue
            recs.append((lay, al, col, pc, z, float(np.mean(z))))

    def project(pc, z):
        return w / 2 + (pc @ r) / z * k, h / 2 - (pc @ u) / z * k

    def tris(pc, z):
        sx, sy = project(pc, z)
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, math.floor(min(x0, x1, x2)))), int(min(w - 1, math.ceil(max(x0, x1, x2)))) + 1
            miny, maxy = int(max(0, math.floor(min(y0, y1, y2)))), int(min(h - 1, math.ceil(max(y0, y1, y2)))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            X = xx[miny:maxy, minx:maxx] + 0.5
            Y = yy[miny:maxy, minx:maxx] + 0.5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            msk = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not msk.any():
                continue
            iz = a / z[tri[0]] + b / z[tri[1]] + c / z[tri[2]]
            yield (slice(miny, maxy), slice(minx, maxx)), msk, iz.astype(np.float32)

    opaque = [q for q in recs if q[0] != 1 and q[1] >= 0.9]
    trans = sorted([q for q in recs if q[0] != 1 and q[1] < 0.9], key=lambda q: -q[5])
    glow = [q for q in recs if q[0] == 1]
    for lay, al, col, pc, z, _ in opaque:
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl])
            ZB[sl] = np.where(ok, iz, ZB[sl])
            C[sl] = np.where(ok[..., None], (col * al)[None, None, :], C[sl])
            A[sl] = np.where(ok, al, A[sl])
    for lay, al, col, pc, z, _ in trans:
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl])
            C[sl] = np.where(ok[..., None], C[sl] * (1 - al) + col[None, None, :] * al, C[sl])
            A[sl] = np.where(ok, A[sl] * (1 - al) + al, A[sl])
    for lay, al, col, pc, z, _ in glow:
        for sl, msk, iz in tris(pc, z):
            ok = msk & (iz > ZB[sl] * 0.995)
            G[sl] = np.where(ok[..., None], np.maximum(G[sl], col[None, None, :] * 0.9), G[sl])

    def down(a):
        return a.reshape(Hd, ss, Wd, ss, *a.shape[2:]).mean(axis=(1, 3)) if a.ndim == 2 else a.reshape(Hd, ss, Wd, ss, a.shape[2]).mean(axis=(1, 3))
    return down(C), down(A), down(G)


# ================================================================================================ the shapes
def frame_of(F, T):
    d = norm(np.asarray(T, float) - np.asarray(F, float))
    s = norm(np.cross(d, UP)) if abs(d[1]) < 0.95 else V(1, 0, 0)
    u2 = norm(np.cross(s, d))
    return d, s, u2


def bubbles_arc(o, c, p, t, n, tex, size, seed, col, vy=(1.6, 3.4), vh=(1.0, 3.2), g=2.8):
    for i in range(n):
        a = hs(seed, i, 1) * math.tau
        v = (vh[0] + (vh[1] - vh[0]) * hs(seed, i, 2)) * p
        vyy = (vy[0] + (vy[1] - vy[0]) * hs(seed, i, 3)) * p
        tt = t * 1.2
        q = c + V(math.cos(a) * v * tt * 0.7, 0.1 + vyy * tt - g * tt * tt * p, math.sin(a) * v * tt * 0.7)
        o.bb(q, size * p * (1 - 0.4 * t), size * p * (1 - 0.4 * t), tex, 0, col)


# ------------------------------------------------------------------------------------------------ ROAR  (WATERFX_CAST; canon: Roar_of_the_Sea_Dragon)
class RoarSt:
    def __init__(self, o, F, T, p):
        self.F, self.T, self.p, self.o = F, T, p, o
        self.d, self.s, self.u2 = frame_of(F, T)
        self.L = float(np.linalg.norm(T - F))
        age, dur = o.age, o.dur
        self.charge = clamp(age / (0.25 * dur))
        self.run = clamp((age - 0.12 * dur) / (0.7 * dur))
        self.head = ease_out(self.run)
        self.tail = max(0.0, self.head - 0.4)
        self.fade = clamp((dur - age) / 6.0)
        self.open = ease_back(self.charge) * (1 - 0.4 * clamp((age - 0.4 * dur) / (0.3 * dur)))

    def pt(self, sg):
        o, p = self.o, self.p
        env = clamp((self.head - sg) / 0.22)
        amp = 0.42 * p * env
        return (self.F + self.d * (self.L * sg) + self.s * (amp * math.sin(sg * self.L * 0.85 - o.age * 0.45))
                + self.u2 * (0.14 * p * env * math.sin(sg * self.L * 0.55 - o.age * 0.3)))

    def tang(self, sg):
        a, b = self.pt(max(0.0, sg - 0.03)), self.pt(sg + 0.001)
        return norm(b - a)


def roar_A(o, F, T, p):
    st = RoarSt(o, F, T, p)
    if st.head <= 0.001:
        o.ground(F + UP * 0.05, 0.6 * p * st.open, "a_ring", W(0.9 * st.fade), -o.age * 0.07)
        return
    hp = st.pt(st.head)
    t = st.tang(st.head)
    n = 8
    pts = [st.pt(st.tail + (st.head - st.tail) * i / n) for i in range(n + 1)]
    for i in range(n):
        g = (i + 0.5) / n
        wd = (0.35 + 1.0 * g) * p
        o.strip2(pts[i], pts[i + 1], wd, "a_body", W(0.95 * st.fade), tile=0.45, uoff=-o.age * 0.02 + i * 0.45)
    o.strip2(hp - t * 0.1 * p, hp + t * 3.0 * p, 2.3 * p, "a_head", W(st.fade))
    for i in range(8):                                                   # drops shed from the body
        sg = st.tail + (st.head - st.tail) * hs(o.seed, i, 4)
        life = (o.age * 0.06 + hs(o.seed, i, 5)) % 1.0
        q = st.pt(sg) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.5 * math.sin(life * math.pi) * p - 0.35 * life * life * p + 0.2 * p)
        o.bb(q, 0.24 * p, 0.28 * p, "a_drop", 0, W(math.sin(life * math.pi) * st.fade))
    o.ground(F + UP * 0.05, 0.6 * p * st.open, "a_ring", W(0.9 * st.fade * (1 - st.run)), -o.age * 0.07)
    if st.head > 0.88:
        k = (st.head - 0.88) / 0.12
        o.upright(T + V(0, -0.1, 0), 2.4 * p * k, 1.3 * p * k, "a_crown", W((1 - 0.5 * k) * st.fade))
        o.ground(T + UP * 0.08, (0.5 + 1.1 * k) * p, "a_ring", W((1 - k) * 0.9 * st.fade), o.age * 0.05)


def roar_B(o, F, T, p):
    st = RoarSt(o, F, T, p)
    if st.head <= 0.001:
        o.ground(F + UP * 0.05, 0.6 * p * st.open, "b_ripple", W(0.9 * st.fade), -o.age * 0.07)
        return
    hp = st.pt(st.head)
    t = st.tang(st.head)
    n = 8
    pts = [st.pt(st.tail + (st.head - st.tail) * i / n) for i in range(n + 1)]
    for lay, (wm, sp, al) in enumerate(((1.0, 0.10, 0.62), (0.62, -0.16, 0.7), (0.34, 0.22, 0.8))):   # three sheets scrolling against each other
        for i in range(n):
            g = (i + 0.5) / n
            wd = (0.38 + 1.0 * g) * p * wm
            o.strip2(pts[i], pts[i + 1], wd, "b_body", W(al * st.fade), tile=0.45, uoff=o.age * sp + lay * 0.3 + i * 0.45)
    o.strip2(hp - t * 0.15 * p, hp + t * 3.3 * p, 2.55 * p, "b_head", W(0.4 * st.fade))        # a second, larger copy behind: thickness
    o.strip2(hp - t * 0.1 * p, hp + t * 3.0 * p, 2.3 * p, "b_head", W(0.95 * st.fade))
    o.bb(hp + t * 2.8 * p + UP * 0.05 * p, 0.55 * p, 0.55 * p, "b_glint", o.age * 0.1, W(0.9 * st.fade), blend="ADD")
    for i in range(12):                                                  # bubbles in the wake
        sg = st.tail + (st.head - st.tail) * hs(o.seed, i, 4) * 0.9
        life = (o.age * 0.07 + hs(o.seed, i, 5)) % 1.0
        q = st.pt(sg) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.8 * p + st.u2 * (0.4 * life * p)
        sz = (0.2 + 0.15 * hs(o.seed, i, 7)) * p
        o.bb(q, sz, sz, "b_bubble", 0, W(math.sin(life * math.pi) * 0.9 * st.fade))
    o.ground(F + UP * 0.05, 0.6 * p * st.open, "b_ripple", W(0.9 * st.fade * (1 - st.run)), -o.age * 0.07)
    if st.head > 0.88:
        k = (st.head - 0.88) / 0.12
        o.upright(T + V(0, -0.1, 0), 2.6 * p * k, 1.3 * p * k, "b_crown", W((1 - 0.5 * k) * st.fade))
        o.ground(T + UP * 0.08, (0.5 + 1.1 * k) * p, "b_ripple", W((1 - k) * 0.9 * st.fade), o.age * 0.05)


def roar_C(o, c, F, T, p, lod=0):
    st = RoarSt(o, F, T, p)
    if st.head <= 0.001:
        o.ground(F + UP * 0.05, 0.7 * p * st.open, "glow", rgba(120, 220, 255, 0.8 * st.fade), 0, blend="ADD")
        return
    hp = st.pt(st.head)
    t = st.tang(st.head)
    kh = 0.95 * p
    side = st.s
    up_ = norm(np.cross(side, t))
    if up_[1] < 0:
        up_ = -up_
    X, Y, Z = basis_y(t, up_)
    origin = hp + Y * (8 * kh / 16)
    jaw = (0.35 + 0.30 * st.open) * (0.9 + 0.1 * math.sin(o.age * 0.5))
    c.add(nm("dragon_head_upper", lod), origin, Y, Z, scale=kh)
    Yj = Y * math.cos(jaw) - Z * math.sin(jaw)
    Zj = Z * math.cos(jaw) + Y * math.sin(jaw)
    c.add(nm("dragon_head_lower", lod), origin + (Y * 2 - Z * 9) * kh / 16, Yj, Zj, scale=kh)
    # body: pieces from the neck back to the tail, each lying along the path
    land = clamp((st.head - 0.88) / 0.12)                                # at the landing the body breaks into the crown
    nseg = max(1, int(round((3 if lod else 5) * (1 - 0.8 * land))))
    gaps = [st.head - (st.head - st.tail) * (i / nseg) for i in range(nseg + 1)]
    for i in range(nseg):
        a, b = st.pt(gaps[i]), st.pt(gaps[i + 1])
        mid = (a + b) / 2
        dv = a - b
        ln = float(np.linalg.norm(dv))
        if ln < 1e-4:
            continue
        g = (i + 0.5) / nseg
        ks = kh * (1.0 - 0.55 * g)
        c.add(nm("dragon_seg_b" if i % 2 == 0 else "dragon_seg", lod), mid, dv, UP, scale=(ks, ln / (14 / 16.0) * 1.0, ks))
    tp = st.pt(st.tail)
    tdir = norm(tp - st.pt(st.tail + 0.06))
    c.add(nm("dragon_tail", lod), tp, tdir, UP, scale=kh * 0.42)
    # drops shed (gems)
    for i in range(2 if lod else 3):
        sg = st.tail + (st.head - st.tail) * hs(o.seed, i, 4)
        life = (o.age * 0.06 + hs(o.seed, i, 5)) % 1.0
        q = st.pt(sg) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.5 * math.sin(life * math.pi) * p - 0.35 * life * life * p + 0.35 * p)
        c.add("gem", q, UP, V(0, 0, 1), scale=16 * (0.2 * p * math.sin(life * math.pi) * st.fade + 0.01))
    o.ground(F + UP * 0.05, 0.7 * p * st.open, "glow", rgba(120, 220, 255, 0.8 * st.fade * (1 - st.run)), 0, blend="ADD")
    if st.head > 0.88:
        k = (st.head - 0.88) / 0.12
        n = 3 if lod else 4
        for i in range(n):
            a = i * math.tau / n + 0.3
            radial = V(math.cos(a), 0, math.sin(a))
            c.add(nm("horn", lod), T + radial * 0.35 * p * k + V(0, -0.05, 0), UP, xhint=radial, scale=(0.8 * p, 0.7 * p * k, 0.8 * p))
        c.add(nm("ripple", 1), T + UP * 0.05, UP, V(0, 0, 1), scale=((0.5 + 1.1 * k) * p, 0.5 * p * (1 - k), (0.5 + 1.1 * k) * p))
    # additive extras (sprites): the glow in the maw and the caster's sigil flash
    o.bb(hp + t * 1.3 * p, 0.9 * p, 0.9 * p, "glow", 0, rgba(120, 220, 255, 0.5 * st.fade), blend="ADD")


# ------------------------------------------------------------------------------------------------ LANCE  (WATERFX_CAST, Aqua Javelin; canon: Aqua_Javelin_Rhya + the lance sheet)
class LanceSt:
    def __init__(self, o, F, T, p):
        self.F, self.T, self.p, self.o = F, T, p, o
        self.d, self.s, self.u2 = frame_of(F, T)
        self.L = float(np.linalg.norm(T - F))
        dur, age = o.dur, o.age
        self.run = clamp((age - 0.1 * dur) / (0.7 * dur))
        self.head = ease_out(self.run)
        self.fade = clamp((dur - age) / 4.0)
        self.hp = F + self.d * (self.L * self.head)


def lance_A(o, F, T, p):
    st = LanceSt(o, F, T, p)
    o.ground(F + UP * 0.05, 0.5 * p * clamp(o.age / 4), "a_ring", W(0.9 * st.fade * (1 - st.run)), -o.age * 0.1)
    if st.head <= 0.001:
        return
    hp, d = st.hp, st.d
    o.strip2(hp - d * 5.2 * p, hp + d * 0.6 * p, 1.35 * p, "a_lance", W(st.fade))
    for k in range(2):                                                   # two wisps winding round the shaft
        prev = None
        for i in range(7):
            g = i / 6
            ang = g * 9 - o.age * 0.45 + k * math.pi
            q = hp - d * (4.6 * p * (1 - g)) + st.s * math.cos(ang) * 0.5 * p * (1 - 0.4 * g) + st.u2 * math.sin(ang) * 0.5 * p * (1 - 0.4 * g)
            if prev is not None:
                o.strip2(prev, q, 0.2 * p, "a_body", W(0.9 * st.fade))
            prev = q
    for i in range(6):
        life = (o.age * 0.08 + hs(o.seed, i, 5)) % 1.0
        q = hp - d * (hs(o.seed, i, 4) * 4.5 * p) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.3 * p - 0.3 * life * p)
        o.bb(q, 0.2 * p, 0.24 * p, "a_drop", 0, W(math.sin(life * math.pi) * st.fade))
    if st.head > 0.9:
        k = (st.head - 0.9) / 0.1
        o.upright(T + V(0, -0.1, 0), 1.8 * p * k, 1.0 * p * k, "a_crown", W((1 - 0.5 * k) * st.fade))


def lance_B(o, F, T, p):
    st = LanceSt(o, F, T, p)
    o.ground(F + UP * 0.05, 0.5 * p * clamp(o.age / 4), "b_ripple", W(0.9 * st.fade * (1 - st.run)), -o.age * 0.1)
    if st.head <= 0.001:
        return
    hp, d = st.hp, st.d
    o.strip2(hp - d * 5.4 * p, hp + d * 0.7 * p, 1.6 * p, "b_lance", W(0.45 * st.fade))
    o.strip2(hp - d * 5.2 * p, hp + d * 0.6 * p, 1.35 * p, "b_lance", W(0.95 * st.fade), uoff=0.0)
    o.strip2(hp - d * 5.0 * p, hp + d * 0.2 * p, 0.45 * p, "b_flow", W(0.9 * st.fade), tile=1.0, uoff=-o.age * 0.25)
    o.bb(hp + d * 0.55 * p, 0.6 * p, 0.6 * p, "b_glint", o.age * 0.2, W(0.9 * st.fade), blend="ADD")
    for i in range(9):
        life = (o.age * 0.09 + hs(o.seed, i, 5)) % 1.0
        q = hp - d * (hs(o.seed, i, 4) * 4.8 * p) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.8 * p + st.u2 * (0.25 * life * p)
        sz = (0.16 + 0.12 * hs(o.seed, i, 7)) * p
        o.bb(q, sz, sz, "b_bubble", 0, W(math.sin(life * math.pi) * 0.9 * st.fade))
    if st.head > 0.9:
        k = (st.head - 0.9) / 0.1
        o.upright(T + V(0, -0.1, 0), 1.9 * p * k, 1.0 * p * k, "b_crown", W((1 - 0.5 * k) * st.fade))


def lance_C(o, c, F, T, p, lod=0):
    st = LanceSt(o, F, T, p)
    o.ground(F + UP * 0.05, 0.5 * p * clamp(o.age / 4), "glow", rgba(120, 220, 255, 0.7 * st.fade * (1 - st.run)), 0, blend="ADD")
    if st.head <= 0.001:
        return
    hp, d = st.hp, st.d
    k = 1.15 * p * (0.85 + 0.15 * st.fade)
    c.add(nm("lance", lod), hp - d * 2.6 * p, d, UP, scale=(k, k, k))
    for i in range(2 if lod else 3):
        ang = o.age * 0.5 + i * math.tau / 3
        rad = st.s * math.cos(ang) + st.u2 * math.sin(ang)
        c.add(nm("wisp", lod), hp - d * 1.6 * p + rad * 0.45 * p, d, rad, scale=(0.9 * p, 1.5 * p, 0.9 * p), alpha=0.8)
    for i in range(2 if lod else 4):
        life = (o.age * 0.08 + hs(o.seed, i, 5)) % 1.0
        q = hp - d * (hs(o.seed, i, 4) * 4.5 * p) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.3 * p - 0.3 * life * p)
        c.add("gem", q, d * -1 + UP * 0.2, UP, scale=16 * (0.2 * p * math.sin(life * math.pi) * st.fade + 0.01))
    o.bb(hp + d * 2.2 * p, 0.8 * p, 0.8 * p, "glow", 0, rgba(140, 230, 255, 0.55 * st.fade), blend="ADD")
    if st.head > 0.9:
        kk = (st.head - 0.9) / 0.1
        n = 3 if lod else 5
        for i in range(n):
            a = i * math.tau / n
            radial = V(math.cos(a), 0, math.sin(a))
            c.add(nm("horn", lod), T + radial * 0.25 * p * kk + V(0, -0.05, 0), UP, xhint=radial, scale=(0.6 * p, 0.5 * p * kk, 0.6 * p))


# ------------------------------------------------------------------------------------------------ CRADLE  (WATERFX_ZONE; canon: Cradle_of_the_Sea_Dragon + Sea_Dragon's_Waterball)
def zone_state(o, p):
    age = o.age
    fade = clamp(min(age / 8.0, (o.dur - age) / 12.0))
    open_ = ease_back(clamp(age / 10.0))
    return fade, open_


def cradle_A(o, F, T, p):
    fade, op = zone_state(o, p)
    R = p
    cen = F + UP * R * 0.95
    o.ground(F + UP * 0.05, R * 1.1 * op, "a_ring", W(0.8 * fade), o.age * 0.02)
    o.ground(F + UP * 0.07, R * 0.8 * op, "a_ring", W(0.7 * fade), -o.age * 0.03)
    o.bb(cen, 2 * R * 0.97 * op, 2 * R * 0.97 * op, "a_sphere", 0, W(0.78 * fade))
    for i in range(5):                                                   # the chain of bubbles beside it (still)
        a = o.age * 0.03 + 0.4
        q = cen + o.r * (R * (1.05 + 0.34 * i)) + o.u * (-0.25 * R * i + 0.2 * R)
        sz = R * (0.38 - 0.05 * i)
        o.bb(q, sz, sz, "a_blob", 0, W(0.9 * fade))
    for i in range(4):
        ang = o.age * 0.06 + i * math.tau / 4
        q = cen + V(math.cos(ang) * R * 1.15, 0.3 * R * math.sin(ang * 2), math.sin(ang) * R * 1.15)
        o.bb(q, 0.3 * R, 0.3 * R, "a_blob", 0, W(0.9 * fade))


def cradle_B(o, F, T, p):
    fade, op = zone_state(o, p)
    R = p
    cen = F + UP * R * 0.95
    o.ground(F + UP * 0.05, R * 1.1 * op, "b_ripple", W(0.8 * fade), o.age * 0.02)
    o.bb(cen, 2 * R * 0.97 * op, 2 * R * 0.97 * op, "b_sphere", 0, W(0.8 * fade))
    for i in range(2):                                                   # crossed sheets carrying the caustics, turning against each other
        yaw = o.age * 0.04 * (1 if i == 0 else -1.4) + i * math.pi / 2
        o.vplane(F + UP * (R * 0.05), yaw, 2 * R * 0.9 * op, 1.9 * R * op, "b_sphere", W(0.28 * fade))
    for i in range(8):
        life = (o.age * 0.03 + hs(o.seed, i, 5)) % 1.0
        ang = hs(o.seed, i, 4) * math.tau
        rr = R * 0.8 * hs(o.seed, i, 3)
        q = F + V(math.cos(ang) * rr, 0.2 + life * 1.8 * R, math.sin(ang) * rr)
        sz = (0.18 + 0.2 * hs(o.seed, i, 7)) * R * 0.6
        o.bb(q, sz, sz, "b_bubble", 0, W(math.sin(life * math.pi) * 0.95 * fade))
    o.bb(cen + o.r * (-R * 0.45) + o.u * (R * 0.55), 0.3 * R, 0.3 * R, "b_glint", 0.3, W(0.8 * fade), blend="ADD")


def cradle_C(o, c, F, T, p, lod=0):
    fade, op = zone_state(o, p)
    R = p
    cen = F + UP * R * 0.95
    c.add(nm("sphere", lod), cen, UP, V(0, 0, 1), scale=16 * R * 0.97 * op, alpha=0.5, foam_alpha=0.95)
    for i in range(2 if lod else 3):                                     # whirl bands round the sphere, each turning on its own axis
        ang = o.age * (0.05 + 0.02 * i) + i * 2.1
        axis = norm(V(math.cos(ang) * 0.5, 1.0, math.sin(ang) * 0.5) * (1 if i != 1 else 1) + V(0.3 * i, 0, -0.3 * i))
        c.add(nm("ripple", 1), cen, axis, V(0, 0, 1), scale=(R * 1.03 * op, R * 0.55 * op, R * 1.03 * op))
    nb = 2 if lod else 5
    for i in range(nb):                                                  # the bubble chain
        q = cen + o.r * (R * (1.15 + 0.3 * i)) + o.u * (-0.22 * R * i + 0.15 * R)
        c.add("orb_xs", q, UP, V(0, 0, 1), scale=16 * R * (0.17 - 0.02 * i), alpha=0.9)
    c.add(nm("ripple", 1), F + UP * 0.05, UP, V(0, 0, 1), scale=(R * 1.0 * op, 0.5 * R, R * 1.0 * op))
    o.bb(cen + o.r * (-R * 0.42) + o.u * (R * 0.55), 0.35 * R, 0.35 * R, "glow", 0, rgba(200, 240, 255, 0.5 * fade), blend="ADD")


# ------------------------------------------------------------------------------------------------ NEST  (WATERFX_ZONE; canon: Nest_of_the_Sea_Dragon)
def nest_A(o, F, T, p):
    fade, op = zone_state(o, p)
    R = p
    o.ground(F + UP * 0.05, R * 1.15 * op, "a_ring", W(0.9 * fade), o.age * 0.03)
    for yaw, wm, al in ((0.0, 1.0, 0.9), (0.9, 0.78, 0.45), (-0.9, 0.78, 0.45)):
        dd = V(math.cos(yaw + o.age * 0.012), 0, math.sin(yaw + o.age * 0.012))
        if wm == 1.0:
            o.upright(F, 2 * R * op, R * 1.02 * op, "a_dome", W(al * fade))
        else:
            o.vplane(F, math.atan2(o.r[2], o.r[0]) + yaw, 2 * R * wm * op, R * 0.98 * op, "a_dome", W(al * fade))
    for i in range(6):
        ang = o.age * 0.05 + i * math.tau / 6
        q = F + V(math.cos(ang) * R * 1.1, 0.15 * R + 0.12 * R * math.sin(o.age * 0.2 + i), math.sin(ang) * R * 1.1)
        o.bb(q, 0.26 * R, 0.26 * R, "a_blob", 0, W(0.95 * fade))


def nest_B(o, F, T, p):
    fade, op = zone_state(o, p)
    R = p
    o.ground(F + UP * 0.05, R * 1.15 * op, "b_ripple", W(0.8 * fade), o.age * 0.03)
    base = math.atan2(o.r[2], o.r[0])
    for i, (wy, sp, w_) in enumerate(((0.0, 0.0, 2.0), (0.7, 1.0, 1.7), (-0.7, -1.4, 1.5), (1.3, 0.6, 1.2))):
        yaw = base + wy + o.age * 0.02 * sp
        o.vplane(F, yaw, w_ * R * op, R * (1.02 - 0.05 * i) * op, "b_dome", W((0.75 - 0.1 * i) * fade))
    for i in range(8):
        life = (o.age * 0.03 + hs(o.seed, i, 5)) % 1.0
        ang = hs(o.seed, i, 4) * math.tau
        rr = R * 0.85 * hs(o.seed, i, 3)
        q = F + V(math.cos(ang) * rr, 0.1 + life * 1.1 * R, math.sin(ang) * rr)
        o.bb(q, 0.22 * R * 0.8, 0.22 * R * 0.8, "b_bubble", 0, W(math.sin(life * math.pi) * fade))


def nest_C(o, c, F, T, p, lod=0):
    fade, op = zone_state(o, p)
    R = p
    spin = o.age * 0.05
    c.add(nm("dome", lod), F, UP, V(math.cos(spin), 0, math.sin(spin)), scale=(R * op, R * op, R * op), alpha=1.0, foam_alpha=1.0, pal="deep")
    c.add(nm("ripple", 1), F + UP * 0.05, UP, V(0, 0, 1), scale=(R * 1.0 * op, 0.6 * R, R * 1.0 * op))
    for i in range(3 if lod else 6):
        ang = o.age * 0.05 + i * math.tau / (3 if lod else 6)
        q = F + V(math.cos(ang) * R * 1.1, 0.2 * R + 0.1 * R * math.sin(o.age * 0.2 + i), math.sin(ang) * R * 1.1)
        c.add("orb_xs", q, UP, V(0, 0, 1), scale=16 * 0.13 * R, alpha=0.95)


# ------------------------------------------------------------------------------------------------ BURST  (WATERFX_BURST; canon: Surging_Waves, Sea_Dragon_God's_Tail, splash sprites)
def burst_t(o):
    t = o.age / o.dur
    return t, 1 - ease_in(t), clamp(1 - o.age / 6.0)


def burst_A(o, F, T, p):
    t, fade, flash = burst_t(o)
    if flash > 0:
        o.bb(F + UP * 0.9 * p, 4.0 * p * (0.5 + 0.5 * (1 - flash)), 4.0 * p * (0.5 + 0.5 * (1 - flash)), "a_flash", o.age * 0.6, W(flash * 0.9), blend="ADD")
    k = clamp(t * 1.4)
    h = (0.5 + 1.1 * math.sin(k * math.pi)) * p
    rise = ease_out(clamp(t * 1.6))
    for yaw in (0.0, math.pi / 2):
        o.vplane(F, math.atan2(o.r[2], o.r[0]) + (0.55 if yaw == 0.0 else -0.55), (2.0 + 2.4 * rise) * p, h * 1.6, "a_crown", W(0.95 * fade))
    for i in range(2):
        lt = clamp(t * 1.5 - i * 0.18)
        if lt > 0:
            o.ground(F + UP * (0.07 + 0.01 * i), (0.4 + 2.6 * ease_out(lt)) * p, "a_ring", W((1 - lt) * 0.95), o.age * 0.05 + i)
    bubbles_arc(o, F, p, t, 8, "a_blob", 0.3, o.seed, W(fade))


def burst_B(o, F, T, p):
    t, fade, flash = burst_t(o)
    if flash > 0:
        o.bb(F + UP * 0.6 * p, 2.6 * p * (0.6 + 0.4 * (1 - flash)), 2.6 * p * (0.6 + 0.4 * (1 - flash)), "glow", 0, W(flash * 0.8), blend="ADD")
    k = clamp(t * 1.4)
    h = (0.5 + 1.1 * math.sin(k * math.pi)) * p
    rise = ease_out(clamp(t * 1.6))
    for yaw in (0.0, math.pi / 2):
        o.vplane(F, math.atan2(o.r[2], o.r[0]) + (0.55 if yaw == 0.0 else -0.55), (2.0 + 2.4 * rise) * p, h * 1.6, "b_crown", W(0.9 * fade))
    for i in range(2):
        lt = clamp(t * 1.5 - i * 0.18)
        if lt > 0:
            o.ground(F + UP * (0.07 + 0.01 * i), (0.4 + 2.6 * ease_out(lt)) * p, "b_ripple", W((1 - lt) * 0.9), o.age * 0.05 + i)
    bubbles_arc(o, F, p, t, 10, "b_bubble", 0.3, o.seed, W(fade))
    o.bb(F + UP * 0.8 * p, 0.6 * p, 0.6 * p, "b_glint", o.age * 0.1, W(flash), blend="ADD")


def burst_C(o, c, F, T, p, lod=0):
    t, fade, flash = burst_t(o)
    if flash > 0:
        o.bb(F + UP * 0.6 * p, 2.6 * p * (0.6 + 0.4 * (1 - flash)), 2.6 * p * (0.6 + 0.4 * (1 - flash)), "glow", 0, W(flash * 0.8), blend="ADD")
    k = clamp(t * 1.4)
    n = 4 if lod else 6
    reach = (0.5 + 1.1 * ease_out(clamp(t * 1.4))) * p
    hgt = (0.35 + 1.25 * math.sin(k * math.pi)) * p
    for i in range(n):
        a = i * math.tau / n + 0.35
        radial = V(math.cos(a), 0, math.sin(a))
        c.add(nm("horn", lod), F + radial * reach + V(0, 0.02, 0), UP, xhint=radial, scale=(1.5 * p, max(0.02, hgt * 0.6), 1.5 * p))
    for i in range(2):
        lt = clamp(t * 1.5 - i * 0.2)
        if lt > 0:
            c.add(nm("ripple", 1), F + UP * 0.04, UP, V(0, 0, 1), scale=((0.4 + 2.2 * ease_out(lt)) * p, 0.9 * p * (1 - lt), (0.4 + 2.2 * ease_out(lt)) * p))
    for i in range(3 if lod else 6):
        a = hs(o.seed, i, 1) * math.tau
        v = (1.0 + 2.2 * hs(o.seed, i, 2)) * p
        vy = (1.6 + 2.0 * hs(o.seed, i, 3)) * p
        tt = t * 1.2
        q = F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.8 * tt * tt * p, math.sin(a) * v * tt * 0.7)
        vel = V(math.cos(a) * v * 0.7, vy - 5.6 * tt * p, math.sin(a) * v * 0.7)
        c.add("gem", q, vel, UP, scale=16 * 0.2 * p * (1 - 0.4 * t) * (0.4 + 0.6 * fade))


# ------------------------------------------------------------------------------------------------ SPLASH  (WATER_SPLASH; canon: the splash sprites of the pack)
def splash_A(o, F, T, p):
    t = o.age / o.dur
    fade = 1 - ease_in(t)
    rise = ease_out(clamp(t * 2.0))
    o.upright(F, (1.8 + 1.2 * rise) * p, (0.4 + 1.1 * math.sin(clamp(t * 1.5) * math.pi)) * p, "a_crown", W(fade))
    o.ground(F + UP * 0.05, (0.3 + 1.6 * ease_out(t)) * p, "a_ring", W((1 - t) * 0.9), o.age * 0.05)
    bubbles_arc(o, F, p, t, 6, "a_blob", 0.22, o.seed + 5, W(fade), vy=(1.2, 2.6), vh=(0.6, 1.6))


def splash_B(o, F, T, p):
    t = o.age / o.dur
    fade = 1 - ease_in(t)
    rise = ease_out(clamp(t * 2.0))
    o.upright(F, (1.8 + 1.2 * rise) * p, (0.4 + 1.1 * math.sin(clamp(t * 1.5) * math.pi)) * p, "b_crown", W(0.92 * fade))
    o.ground(F + UP * 0.05, (0.3 + 1.6 * ease_out(t)) * p, "b_ripple", W((1 - t) * 0.9), o.age * 0.05)
    bubbles_arc(o, F, p, t, 7, "b_bubble", 0.24, o.seed + 5, W(fade), vy=(1.2, 2.6), vh=(0.6, 1.6))


def splash_C(o, c, F, T, p, lod=0):
    t = o.age / o.dur
    fade = 1 - ease_in(t)
    k = clamp(t * 1.5)
    n = 3 if lod else 5
    reach = (0.25 + 0.7 * ease_out(clamp(t * 1.6))) * p
    hgt = (0.25 + 0.95 * math.sin(k * math.pi)) * p
    for i in range(n):
        a = i * math.tau / n + 0.5
        radial = V(math.cos(a), 0, math.sin(a))
        c.add(nm("horn", lod), F + radial * reach, UP, xhint=radial, scale=(1.1 * p, max(0.02, hgt * 0.6), 1.1 * p))
    c.add(nm("ripple", 1), F + UP * 0.04, UP, V(0, 0, 1), scale=((0.3 + 1.5 * ease_out(t)) * p, 0.7 * p * (1 - t), (0.3 + 1.5 * ease_out(t)) * p))
    for i in range(2 if lod else 4):
        a = hs(o.seed + 5, i, 1) * math.tau
        v = (0.6 + 1.0 * hs(o.seed + 5, i, 2)) * p
        vy = (1.2 + 1.4 * hs(o.seed + 5, i, 3)) * p
        tt = t * 1.2
        q = F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.8 * tt * tt * p, math.sin(a) * v * tt * 0.7)
        vel = V(math.cos(a) * v * 0.7, vy - 5.6 * tt * p, math.sin(a) * v * 0.7)
        c.add("gem", q, vel, UP, scale=16 * 0.17 * p * (1 - 0.4 * t) * (0.4 + 0.6 * fade))


# ------------------------------------------------------------------------------------------------ RING  (WATER_RING; canon: the foam ring under the Nest, Sea_Dragon_God's_Vortex)
def ring_r(o, p, k):
    lt = clamp(o.age / o.dur * 1.3 - k * 0.15)
    return lt, (0.5 + 3.0 * ease_out(lt)) * p


def ring_A(o, F, T, p):
    for k in range(3):
        lt, r = ring_r(o, p, k)
        if 0 < lt < 1:
            o.ground(F + UP * (0.05 + 0.01 * k), r, "a_ring", W((1 - lt) * 0.95), o.age * (0.05 if k % 2 else -0.05) + k)


def ring_B(o, F, T, p):
    for k in range(3):
        lt, r = ring_r(o, p, k)
        if 0 < lt < 1:
            o.ground(F + UP * (0.05 + 0.01 * k), r, "b_ripple", W((1 - lt) * 0.9), o.age * (0.05 if k % 2 else -0.05) + k)
    for i in range(3):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        a = hs(o.seed, i, 4) * math.tau
        o.bb(F + V(math.cos(a) * 0.8 * p, 0.1 + life * 0.8 * p, math.sin(a) * 0.8 * p), 0.2 * p, 0.2 * p, "b_bubble", 0, W(math.sin(life * math.pi)))


def ring_C(o, c, F, T, p, lod=0):
    n = 2 if lod else 3
    for k in range(n):
        lt, r = ring_r(o, p, k)
        if 0 < lt < 1:
            c.add(nm("ripple", 1), F + UP * (0.02 + 0.01 * k), UP, V(math.cos(o.age * 0.05 + k), 0, math.sin(o.age * 0.05 + k)), scale=(r, 0.5 * p * (1 - lt) ** 0.7, r))


# ------------------------------------------------------------------------------------------------ VALKYRIE add-on  (new, optional; canon: Valkyrie_Dress / Saint_Valkyrie_Dress)
def valk_state(o):
    return 1.0, 0.5 + 0.5 * math.sin(o.age * 0.12)


def valk_A(o, F, T, p):
    fl = 0.1 * math.sin(o.age * 0.12)
    for sgn in (-1, 1):
        c = F + V(sgn * 0.2, 1.3, -0.2)
        ax = V(sgn * 1.0, 0.12 + fl, -0.1)
        ay = V(-sgn * 0.12, 1.0, 0.0)
        w_ = 2.7 * p
        o.dplane(c + norm(ax) * (w_ / 2 - 0.25) + norm(ay) * (w_ / 2 - 0.25), ax, ay, w_, w_, "a_wing", W(0.9), flip=(sgn < 0))
    o.ground(F + V(0, 2.28, 0), 0.5, "a_ring", W(0.9), o.age * 0.06)
    for i in range(7):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        side = -1 if i % 2 else 1
        q = F + V(side * (0.9 + 0.8 * hs(o.seed, i, 6)), 1.9 - 1.7 * life, -0.3 + 0.2 * hs(o.seed, i, 8))
        o.bb(q, 0.16, 0.2, "a_drop", 0, W(math.sin(life * math.pi)))


def valk_B(o, F, T, p):
    fl = 0.1 * math.sin(o.age * 0.12)
    for sgn in (-1, 1):
        c = F + V(sgn * 0.2, 1.3, -0.2)
        ax = V(sgn * 1.0, 0.12 + fl, -0.1)
        ay = V(-sgn * 0.12, 1.0, 0.0)
        w_ = 2.7 * p
        o.dplane(c + norm(ax) * (w_ / 2 - 0.25) + norm(ay) * (w_ / 2 - 0.25), ax, ay, w_, w_, "b_wing", W(0.92), flip=(sgn < 0))
    o.ground(F + V(0, 2.28, 0), 0.5, "b_ripple", W(0.9), o.age * 0.06)
    for i in range(8):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        side = -1 if i % 2 else 1
        q = F + V(side * (0.9 + 0.8 * hs(o.seed, i, 6)), 1.9 - 1.7 * life, -0.3 + 0.2 * hs(o.seed, i, 8))
        sz = 0.12 + 0.1 * hs(o.seed, i, 7)
        o.bb(q, sz, sz, "b_bubble", 0, W(math.sin(life * math.pi)))
    o.bb(F + V(0, 2.28, 0), 0.5, 0.5, "b_glint", o.age * 0.05, W(0.7), blend="ADD")


def valk_C(o, c, F, T, p, lod=0):
    fl = 0.12 * math.sin(o.age * 0.12)
    nf = 2 if lod else 3
    for sgn in (-1, 1):
        root = F + V(sgn * 0.2, 1.4, -0.2)
        for i in range(nf):
            ang = math.radians(8 + i * (66 / max(1, nf - 1)) + fl * 60)
            d = V(sgn * math.cos(ang), math.sin(ang), -0.35)
            c.add(nm("feather", lod), root, d, V(0, 0, 1), scale=(1.6 * p, (1.15 - 0.15 * i) * p, 1.2 * p), alpha=0.92)
    c.add(nm("ripple", 1), F + V(0, 2.25, 0), UP, V(math.cos(o.age * 0.06), 0, math.sin(o.age * 0.06)), scale=(0.5 / 1.0, 0.5, 0.5))
    for i in range(2 if lod else 4):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        side = -1 if i % 2 else 1
        q = F + V(side * (0.9 + 0.8 * hs(o.seed, i, 6)), 1.9 - 1.7 * life, -0.3 + 0.2 * hs(o.seed, i, 8))
        c.add("gem", q, -UP, UP, scale=16 * (0.15 * math.sin(life * math.pi) + 0.01))
    o.bb(F + V(0, 2.28, 0), 0.7, 0.7, "glow", 0, rgba(180, 235, 255, 0.5), blend="ADD")


# ================================================================================================ scene table
SCN = V
SCENES = [
    dict(key="roar", title="ROAR  Sea Dragon's Roar", shape="WATERFX_CAST (also WATER_DRAGON, unused)", layer="WaterFxLayer", jshape="WATERFX_CAST", F=SCN(0, 1.5, 0), T=SCN(0, 1.5, 12), p=1.2, dur=24,
         cam=(5.6, 2.0, 6.6), target=(0, 1.55, 7.4), ages=[4, 8, 14], mid=1, figures=[[0, 0, -0.5, 1.8], [-1.5, 0, 8.5, 1.8]], cap=100, live=51, gen=(roar_A, roar_B, roar_C),
         still="Roar_of_the_Sea_Dragon (head with open jaw and fangs, banded body); Tainted_Culvert_Wyrm_Rhya as the second head reference"),
    dict(key="lance", title="LANCE  Aqua Javelin", shape="WATERFX_CAST (also WATER_JAVELIN, unused)", layer="WaterFxLayer", jshape="WATERFX_CAST", F=SCN(0, 1.5, 0), T=SCN(0, 1.5, 14), p=0.8, dur=14,
         cam=(4.2, 1.9, 8.2), target=(0, 1.5, 9.6), ages=[3, 6, 10], mid=1, figures=[[0, 0, -0.5, 1.8]], cap=100, live=51, gen=(lance_A, lance_B, lance_C),
         still="Aqua_Javelin_Rhya (long lance with spiral wisps) + the dark lance concept sheet (ee700f44)"),
    dict(key="cradle", title="CRADLE  Sea Dragon's Cradle (zone, sphere)", shape="WATERFX_ZONE (also WATER_CRADLE, unused)", layer="WaterFxLayer", jshape="WATERFX_ZONE", F=SCN(0, 0, 6), T=SCN(0, 1, 6), p=2.2, dur=80,
         cam=(-5.6, 2.4, 1.4), target=(0, 1.7, 6.0), ages=[4, 20, 50], mid=1, figures=[[0, 0, 6, 1.8], [0.9, 0, 5.3, 1.8]], cap=100, live=39, gen=(cradle_A, cradle_B, cradle_C),
         still="Cradle_of_the_Sea_Dragon (a huge pale sphere, white hot-spot, bubble chain) + Sea_Dragon's_Waterball (outlined spheres, lighter blotches)"),
    dict(key="nest", title="NEST  Sea Dragon's Nest (zone, dome)", shape="WATERFX_ZONE (also WATER_NEST, unused)", layer="WaterFxLayer", jshape="WATERFX_ZONE", F=SCN(0, 0, 6), T=SCN(0, 1, 6), p=2.4, dur=80,
         cam=(-5.6, 2.2, 1.4), target=(0, 1.2, 6.0), ages=[4, 20, 50], mid=1, figures=[[0, 0, 6, 1.8]], cap=100, live=39, gen=(nest_A, nest_B, nest_C),
         still="Nest_of_the_Sea_Dragon (a dome with three spiral eddies and a white foam rim); Aqua_Curtain_Rhya as the veined-glass alternative"),
    dict(key="burst", title="BURST  impact (crown + ripples)", shape="WATERFX_BURST (also WATER_BURST, unused)", layer="WaterFxLayer", jshape="WATERFX_BURST", F=SCN(0, 0, 6), T=SCN(0, 1, 6), p=1.4, dur=28,
         cam=(-4.6, 2.2, 2.0), target=(0, 0.9, 6.0), ages=[3, 8, 16], mid=1, figures=[[0, 0, 0.5, 1.8]], cap=100, live=30, gen=(burst_A, burst_B, burst_C),
         still="Sea_Dragon_God's_Tail (manga blast with a crown of water) + Surging_Waves + the water-splash sprite sheet (gif Water 03/04/12) + the Avatar-style sheet (crown, column)"),
    dict(key="splash", title="SPLASH  Mist Step / pages", shape="WATER_SPLASH", layer="WaterMagicLayer", jshape="WATER_SPLASH", F=SCN(0, 0, 4), T=SCN(0, 0.8, 4), p=1.2, dur=20,
         cam=(-4.5, 2.2, 0.5), target=(0, 0.8, 4.0), ages=[2, 6, 12], mid=1, figures=[[0, 0, 0, 1.8]], cap=100, live=23, gen=(splash_A, splash_B, splash_C),
         still="the splash sprite sheet (gif: Water 03 / 04 / 12 crowns) + Water Tutorial (lighter-blue rim, droplets)"),
    dict(key="ring", title="RING  Mist Veil / basic arts", shape="WATER_RING", layer="WaterMagicLayer", jshape="WATER_RING", F=SCN(0, 0, 4), T=SCN(0, 1, 4), p=1.2, dur=30,
         cam=(-4.5, 3.0, 0.5), target=(0, 0.3, 4.0), ages=[4, 12, 22], mid=1, figures=[[0, 0, 0, 1.8]], cap=100, live=100, gen=(ring_A, ring_B, ring_C),
         still="the foam ring at the foot of Nest_of_the_Sea_Dragon + Sea_Dragon_God's_Vortex (manga rings) + the gif Water 09 arc"),
    dict(key="valk", title="VALKYRIE  add-on (wings, halo, drips)", shape="new optional; today WATER_DRESS (unused) + the 3D armour", layer="WaterSpellLayer", jshape="WATER_DRESS", F=SCN(0, 0, 0), T=SCN(0, 1, 0), p=1.0, dur=600,
         cam=(2.0, 1.9, -5.4), target=(0, 1.3, 0), ages=[10, 40, 100], mid=1, figures=[[0, 0, 0, 1.8]], cap=100, live=74, gen=(valk_A, valk_B, valk_C),
         still="Valkyrie_Dress (translucent crystal wings, bubbles) + Saint_Valkyrie_Dress (halo, feather wings)"),
]


# ================================================================================================ render
def load_textures():
    for fn in sorted(os.listdir(TEXDIR)):
        if fn.endswith(".png"):
            pv._tex_cache["mc/" + fn[:-4]] = np.asarray(Image.open(os.path.join(TEXDIR, fn)).convert("RGBA"), dtype=np.float32) / 255.0


def make(sc, ver, age, lod=0, seed=12345):
    o = QO(sc["cam"], sc["target"], age, sc["dur"], seed)
    cl = CL()
    F, T, p = sc["F"], sc["T"], sc["p"]
    if ver == "C":
        sc["gen"][2](o, cl, F, T, p, lod)
    else:
        sc["gen"]["AB".index(ver)](o, F, T, p)
    return o.finish(), cl


def peak(sc, ver, lod=0):
    pk = dict(quads=0, mesh=0, layers=[0, 0, 0], sprites=0, total=0)
    for age in range(0, min(sc["dur"], 120) + 1):
        q, cl = make(sc, ver, age, lod)
        c = cl.counts()
        mq = sum(c)
        pk["sprites"] = max(pk["sprites"], len(q))
        pk["mesh"] = max(pk["mesh"], mq)
        pk["total"] = max(pk["total"], len(q) + mq)
        if mq >= sum(pk["layers"]):
            pk["layers"] = c
    return pk


def tile_for(sc, ver, age, label, sub="", lod=0):
    q, cl = make(sc, ver, age, lod)
    cam, target = sc["cam"], sc["target"]
    sd = dict(sky=[0.30, 0.34, 0.44], figures=sc["figures"])
    img = pv.background(sd, cam, target)
    img = pv.draw_figures(img, sd, cam, target)
    if cl.items:
        Cb, Ab, Gb = render_meshes(cl.items, np.array(cam, float), np.array(target, float))
        img = img * (1 - Ab[..., None]) + Cb
        img = img + Gb
    img = pv.raster(img, q, cam, target)
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, CW, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, CH - 13), sub, fill=(255, 235, 150))
    return t


def before_scene(sc):
    return {"layer": sc["layer"], "shape": sc["jshape"], "from": list(map(float, sc["F"])), "to": list(map(float, sc["T"])), "power": sc["p"], "duration": sc["dur"],
            "cam": list(sc["cam"]), "target": list(sc["target"]), "ages": sc["ages"], "figures": sc["figures"], "sky": [0.30, 0.34, 0.44]}


CLOSEUPS = [("roar", (3.4, 1.9, 7.4), (0, 1.55, 8.4), 9, "ROAR head"), ("burst", (-4.6, 1.9, 4.2), (0, 0.8, 6.0), 8, "BURST crown"), ("nest", (-4.0, 1.6, 3.4), (0, 1.0, 6.0), 20, "NEST dome")]


def closeups():
    """3 hero shapes x A / B / C at 2x the size (the same tick as the compare sheet)"""
    cw, ch = pv.W, pv.H
    pv.W, pv.H = 760, 560
    global CW, CH
    ocw, och = CW, CH
    CW, CH = 760, 560
    sheet = Image.new("RGB", (760 * 3, 560 * len(CLOSEUPS)), (16, 18, 22))
    for r, (key, cam, tgt, age, label) in enumerate(CLOSEUPS):
        sc = dict([s for s in SCENES if s["key"] == key][0])
        sc["cam"], sc["target"] = cam, tgt
        for c, v in enumerate("ABC"):
            t = tile_for(sc, v, age, f"{label}  {dict(A='A anime cel', B='B volumetric glass', C='C real 3D toon')[v]}  t={age}")
            sheet.paste(t, (c * 760, r * 560))
    sheet.save(os.path.join(OUTDIR, "water_closeups.png"))
    pv.W, pv.H = cw, ch
    CW, CH = ocw, och


def main():
    only = [a for a in sys.argv[1:] if not a.startswith("-")]
    quick = "--quick" in sys.argv
    pv.W, pv.H = CW, CH
    load_textures()
    os.makedirs(pv.BUILD, exist_ok=True)
    scenes = [s for s in SCENES if not only or s["key"] in only]
    classes = pv.build([s["layer"] for s in scenes])
    budget = {}
    before = {}
    for sc in scenes:
        sd = before_scene(sc)
        data = pv.run_java(sd, classes, os.path.join(pv.BUILD, "wn_before_" + sc["key"] + ".json"))
        before[sc["key"]] = (sd, data)
        budget[sc["key"]] = {"title": sc["title"], "shape": sc["shape"], "cap_quads": sc["cap"], "today_live_quads": sc["live"],
                             "today_peak_in_preview": max(len(fr["quads"]) for fr in data["frames"])}
    names = {"A": "A anime cel", "B": "B volumetric glass", "C": "C real 3D toon"}
    sheets = {v: [] for v in "ABC"}
    cmp_rows = []
    for sc in scenes:
        rows = {v: [] for v in "ABC"}
        sd, data = before[sc["key"]]
        for v in "ABC":
            pk = peak(sc, v, 0)
            entry = {"peak_total_quads": pk["total"], "peak_sprite_quads": pk["sprites"], "peak_mesh_quads": pk["mesh"], "mesh_layers_0_1_2": pk["layers"]}
            if v == "C":
                pk1 = peak(sc, v, 1)
                entry["lod1_peak_total_quads"] = pk1["total"]
                entry["lod1_peak_mesh_quads"] = pk1["mesh"]
            budget[sc["key"]][v] = entry
            for age in sc["ages"]:
                rows[v].append(tile_for(sc, v, age, f"{sc['key']} {names[v]}  t={age}/{sc['dur']}", ""))
            sheets[v].append((sc, rows[v]))
        mid = sc["mid"]
        fr = data["frames"][mid]
        btile = Image.fromarray((np.clip(pv.draw_figures(pv.background(dict(sky=[0.30, 0.34, 0.44]), sc["cam"], sc["target"]), dict(figures=sc["figures"]), sc["cam"], sc["target"]), 0, 1) * 255).astype(np.uint8))
        img = np.asarray(btile, np.float32) / 255.0
        img = pv.raster(img, fr["quads"], sc["cam"], sc["target"])
        btile = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
        d = ImageDraw.Draw(btile)
        d.rectangle([0, 0, CW, 14], fill=(14, 16, 20))
        d.text((5, 2), f"BEFORE (current)  {sc['key']}  t={fr['age']:.0f}  {len(fr['quads'])} quads", fill=(255, 255, 255))
        cmp_rows.append((sc, [btile] + [rows[v][mid] for v in "ABC"]))
        b = budget[sc["key"]]
        print(sc["key"], "before", b["today_peak_in_preview"], {v: (b[v]["peak_total_quads"], b[v]["peak_mesh_quads"]) for v in "ABC"}, "C lod1", b["C"]["lod1_peak_total_quads"], flush=True)
    sfx = "_" + "_".join(only) if only else ""
    for v in "ABC":
        maxc = max(len(r[1]) for r in sheets[v])
        sheet = Image.new("RGB", (CW * maxc, CH * len(sheets[v]) + 30), (16, 18, 22))
        ImageDraw.Draw(sheet).text((8, 8), f"WATER  {names[v]}   (software render, no bloom, not seen in game)", fill=(255, 255, 255))
        for i, (sc, tiles) in enumerate(sheets[v]):
            for j, t in enumerate(tiles):
                sheet.paste(t, (j * CW, 30 + i * CH))
        sheet.save(os.path.join(OUTDIR, f"water_new_{v}{sfx}.png"))
    cmp = Image.new("RGB", (CW * 4, CH * len(cmp_rows) + 30), (16, 18, 22))
    ImageDraw.Draw(cmp).text((8, 8), "WATER  BEFORE | A anime cel | B volumetric glass | C real 3D toon   (same camera, same tick; software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, (sc, tiles) in enumerate(cmp_rows):
        for j, t in enumerate(tiles):
            cmp.paste(t, (j * CW, 30 + i * CH))
    cmp.save(os.path.join(OUTDIR, f"water_compare{sfx}.png"))
    if not only:
        with open(os.path.join(OUTDIR, "water_budget.json"), "w") as f:
            json.dump(budget, f, indent=1)
        closeups()


if __name__ == "__main__":
    main()

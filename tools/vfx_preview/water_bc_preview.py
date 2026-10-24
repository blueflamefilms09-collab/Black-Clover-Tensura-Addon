#!/usr/bin/env python3
"""
WATER remake, hybrid "BC" = PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

BC = the real 3D toon meshes of C as the SOLID CORE of every effect, wrapped / trailed / filled by the translucent glass of B (body sheen shells, flow ribbons, bubbles, glints, mist,
splash crowns).  The same eight shapes, cameras and ticks as water_compare.png, so the columns are directly comparable.

    python tools/gen_water_new_textures.py     # the 26 sprites of A and B (unchanged)
    python tools/gen_water_bc_textures.py      # + bc_shell, bc_mist, bc_sheen, bc_ribbon
    python tools/gen_water_new_meshes.py       # the C pieces (unchanged)
    python tools/gen_water_bc_meshes.py        # + the bc_* cores
    python tools/vfx_preview/water_bc_preview.py [shape ...]     (numpy + Pillow; the BEFORE, B and C columns are cut out of water_compare.png, made by water_new_preview.py)

Output (docs/previews/vfx_remake/water_new/): water_bc_compare.png, water_bc_sheet.png, water_bc_closeups.png, water_bc_lod.png, water_bc_budget.json.

How a frame is put together (what the game would do): meshes first (z-buffered, toon shaded per quad on the CPU, translucent hulls blended far to near without writing depth), then the glow
layer additively, then the glass SPRITES painter-sorted but DEPTH-TESTED against the mesh depth (so a ribbon behind the body is hidden by it, one in front of it covers it, like depth test on /
depth write off in the game).  Honest limits: software render, no bloom, no world light, translucent mesh hulls and glass sprites are not sorted against each other, never seen in a game.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import preview as pv                                                        # noqa: E402
import water_new_preview as wn                                              # noqa: E402  (the A / B / C generators and the shared helpers)
from water_new_preview import (QO, CL, RoarSt, LanceSt, basis_y, nm, mesh, clamp, rgba, W, ease_out, ease_in, hs, zone_state, burst_t, ring_r, norm, V, UP, SCENES)   # noqa: E402

OUTDIR = wn.OUTDIR
CW, CH = wn.CW, wn.CH
SS = wn.SS

# ================================================================================================ mesh pass with depth
TONES = dict(wn.TONES)
TONES["glass"] = [(118, 188, 238), (168, 222, 252), (236, 249, 255)]
TONES["tongue"] = [(64, 134, 212), (118, 194, 246), (206, 237, 255)]
TONES["core"] = [(14, 46, 104), (26, 84, 166), (84, 162, 232)]
RIM = wn.RIM
GLOWC = wn.GLOWC


class CL2(CL):
    def add(self, name, pos, y, zhint=None, xhint=None, scale=1.0, alpha=1.0, glow=1.0, pal="water", foam_alpha=1.0, fres=False):
        super().add(name, pos, y, zhint=zhint, xhint=xhint, scale=scale, alpha=alpha, glow=glow, pal=pal, foam_alpha=foam_alpha)
        self.items[-1]["fres"] = fres


def render_meshes(items, cam, target, ss=SS):
    """like water_new_preview.render_meshes, plus: a depth buffer is handed back (nearest solid), glass hulls get a fresnel alpha (clear in the middle, denser at the grazing edge),
    a softer rim tint.  Returns premultiplied colour (H, W, 3), coverage (H, W), glow (H, W, 3), inverse depth (H, W)."""
    Wd, Hd = pv.W, pv.H
    w, h = Wd * ss, Hd * ss
    f, r, u = pv.basis(cam, target)
    k = (h / 2) / math.tan(math.radians(pv.FOV / 2))
    SUN = norm(-0.55 * r + 0.8 * UP - 0.3 * f)
    C = np.zeros((h, w, 3), np.float32)
    A = np.zeros((h, w), np.float32)
    G = np.zeros((h, w, 3), np.float32)
    ZB = np.zeros((h, w), np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    recs = []
    for it in items:
        m = mesh(it["name"])
        R = it["R"]
        Pw = m.P @ R.T + it["t"]
        Rin = np.linalg.inv(R)
        Nw = m.N @ Rin
        Nw /= np.maximum(np.linalg.norm(Nw, axis=1, keepdims=True), 1e-9)
        cen = Pw.mean(axis=1)
        Vv = cam - cen
        Vv /= np.maximum(np.linalg.norm(Vv, axis=1, keepdims=True), 1e-9)
        ndv = np.sum(Nw * Vv, axis=1)
        Nw = np.where(ndv[:, None] < 0, -Nw, Nw)
        ndv = np.abs(ndv)
        hl = 0.5 + 0.5 * (Nw @ SUN)
        tone = np.where(hl > 0.86, 2, np.where(hl > 0.58, 1, 0))
        Hh = SUN[None, :] + Vv
        Hh /= np.maximum(np.linalg.norm(Hh, axis=1, keepdims=True), 1e-9)
        spec = np.sum(Nw * Hh, axis=1) > 0.9985
        rim = ndv < 0.22
        for qi in range(m.n):
            lay = m.L[qi]
            if lay == 1:
                col, al = GLOWC * it["glow"], 1.0
            else:
                pal = TONES["foam" if lay == 2 else it["pal"]]
                col = np.array(pal[tone[qi]], np.float32) / 255.0 * m.T[qi]
                if spec[qi]:
                    col = col * 0.25 + 0.75
                elif rim[qi]:
                    col = col * 0.6 + RIM * 0.4
                al = it["foam_alpha"] if lay == 2 and it["alpha"] < 0.95 else it["alpha"]
                if it.get("fres") and lay != 2:
                    al = min(1.0, al * (0.30 + 0.70 * (1 - ndv[qi]) ** 1.5) * 1.55)
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
        return a.reshape(Hd, ss, Wd, ss, *a.shape[2:]).mean(axis=(1, 3))
    zb = ZB.reshape(Hd, ss, Wd, ss).max(axis=(1, 3))
    return down(C), down(A), down(G), zb


def raster(img, quads, cam, target, ZB):
    """pv.raster plus a depth test against the mesh pass (sprites never write depth)"""
    f, r, u = pv.basis(cam, target)
    Wd, Hd = pv.W, pv.H
    k = (Hd / 2) / math.tan(math.radians(pv.FOV / 2))
    order = sorted(range(len(quads)), key=lambda i: quads[i]["o"])
    yy, xx = np.mgrid[0:Hd, 0:Wd].astype(np.float32)
    for qi in order:
        q = quads[qi]
        tex = pv.texture(q["t"])
        th, tw = tex.shape[:2]
        v = np.array(q["v"], dtype=np.float64)
        pts, uv = v[:, :3], v[:, 3:5]
        col = np.array([[((c >> 16) & 255) / 255, ((c >> 8) & 255) / 255, (c & 255) / 255, ((c >> 24) & 255) / 255] for c in v[:, 5].astype(np.int64)])
        z = pts @ f
        if np.any(z < 0.05):
            continue
        sx = Wd / 2 + (pts @ r) / z * k
        sy = Hd / 2 - (pts @ u) / z * k
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]
            y0, y1, y2 = sy[list(tri)]
            minx, maxx = int(max(0, min(x0, x1, x2))), int(min(Wd - 1, max(x0, x1, x2))) + 1
            miny, maxy = int(max(0, min(y0, y1, y2))), int(min(Hd - 1, max(y0, y1, y2))) + 1
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
            m = (a >= -1e-4) & (b >= -1e-4) & (c >= -1e-4)
            if not m.any():
                continue
            iz = np.array([1 / z[i] for i in tri])
            w_ = a * iz[0] + b * iz[1] + c * iz[2]
            m &= w_ > ZB[miny:maxy, minx:maxx] * 0.985                 # in front of (or level with) the nearest solid piece
            if not m.any():
                continue

            def interp(vals):
                return (a * vals[0] * iz[0] + b * vals[1] * iz[1] + c * vals[2] * iz[2]) / w_
            U = interp([uv[i, 0] for i in tri])
            Vv = interp([uv[i, 1] for i in tri])
            ui = (np.mod(U, 1.0) * tw).astype(int).clip(0, tw - 1)
            vi = (np.mod(Vv, 1.0) * th).astype(int).clip(0, th - 1)
            vc = np.stack([interp([col[i, ch] for i in tri]) for ch in range(4)], axis=-1)
            s = tex[vi, ui] * vc
            al = (s[..., 3] * m)[..., None]
            region = img[miny:maxy, minx:maxx]
            if q["b"] in ("ALPHA", "WATER"):
                region[:] = region * (1 - al) + s[..., :3] * al
            else:
                region += s[..., :3] * al
    return img


def load_textures():
    wn.load_textures()                                                       # also picks up tex/bc_*.png (same folder)


# ================================================================================================ glass helpers (B's layers)
def toward_cam(o, pos, k):
    d = o.cam - np.asarray(pos, float)
    n = np.linalg.norm(d)
    return d / n * k if n > 1e-6 else np.zeros(3)


def sheet(o, a, b, width, tex, alpha, off, tile=0.45, uoff=0.0, flip=False):
    """a camera-facing glass strip from a to b, pushed `off` blocks toward (+) or away from (-) the camera"""
    mid = (np.asarray(a, float) + np.asarray(b, float)) / 2
    sh = toward_cam(o, mid, off)
    o.strip2(np.asarray(a, float) + sh, np.asarray(b, float) + sh, width, tex, W(alpha), tile=tile, uoff=uoff, flip=flip)


def mist(o, pos, size, alpha, roll=0.0):
    o.bb(pos, size, size, "bc_mist", roll, W(alpha))


def bubble(o, pos, size, alpha):
    o.bb(pos, size, size, "b_bubble", 0, W(alpha))


# ================================================================================================ the eight BC shapes
def roar_BC(o, c, F, T, p, lod=0):
    st = RoarSt(o, F, T, p)
    if st.head <= 0.001:
        o.ground(F + UP * 0.05, 0.7 * p * st.open, "b_ripple", W(0.8 * st.fade), -o.age * 0.07)
        o.ground(F + UP * 0.06, 0.7 * p * st.open, "glow", rgba(120, 220, 255, 0.7 * st.fade), 0, blend="ADD")
        return
    hp = st.pt(st.head)
    t = st.tang(st.head)
    kh = 0.95 * p
    up_ = norm(np.cross(st.s, t))
    if up_[1] < 0:
        up_ = -up_
    X, Y, Z = basis_y(t, up_)
    origin = hp + Y * (8 * kh / 16)
    jaw = (0.35 + 0.30 * st.open) * (0.9 + 0.1 * math.sin(o.age * 0.5))
    c.add(nm("bc_head_upper", lod), origin, Y, Z, scale=kh, pal="core")
    c.add(nm("bc_head_hull", lod), origin, Y, Z, scale=kh * 1.0, alpha=0.32, pal="glass", fres=True)
    Yj = Y * math.cos(jaw) - Z * math.sin(jaw)
    Zj = Z * math.cos(jaw) + Y * math.sin(jaw)
    c.add(nm("bc_head_lower", lod), origin + (Y * 2 - Z * 9) * kh / 16, Yj, Zj, scale=kh, pal="core")
    land = clamp((st.head - 0.88) / 0.12)
    nseg = max(1, int(round((3 if lod else 4) * (1 - 0.8 * land))))
    gaps = [st.head - 0.05 * p - (st.head - st.tail) * (i / nseg) for i in range(nseg + 1)]
    for i in range(nseg):
        a, b = st.pt(gaps[i]), st.pt(gaps[i + 1])
        mid = (a + b) / 2
        dv = a - b
        ln = float(np.linalg.norm(dv))
        if ln < 1e-4:
            continue
        g = (i + 0.5) / nseg
        ks = kh * (0.97 - 0.52 * g)
        c.add(nm("bc_seg_b" if i % 2 == 0 else "bc_seg", lod), mid, dv, UP, scale=(ks, ln * 1.08, ks), pal="core")
    tp = st.pt(st.tail)
    c.add(nm("bc_tail", lod), tp, norm(tp - st.pt(st.tail + 0.06)), UP, scale=kh * 0.42, pal="core")

    # ---- glass wrap (B): sheen shells on the body, flow ribbons, mist over the neck join, bubbles, drops, glints
    nst = 4 if lod else 6
    sp = [st.tail + (st.head - st.tail) * i / nst for i in range(nst + 1)]
    pts = [st.pt(s) for s in sp]
    rad = [0.47 * kh * (0.97 - 0.52 * (1 - s_i / nst)) for s_i in range(nst + 1)]
    fade = st.fade
    for i in range(nst):                                                       # the glass: a wide pale envelope behind the core, a clear shell in front of it (the core is seen through it)
        r_ = (rad[i] + rad[i + 1]) / 2
        ext = (pts[i + 1] - pts[i]) * 0.3                                      # the strips overlap, so their rectangular ends do not show as seams
        sheet(o, pts[i] - ext, pts[i + 1] + ext, 2 * r_ * 2.0, "b_body", 0.26 * fade, -0.3 * r_, uoff=o.age * 0.020 + i * 0.45, tile=0.6)
        sheet(o, pts[i] - ext, pts[i + 1] + ext, 2 * r_ * 1.55, "b_body", 0.36 * fade, 1.05 * r_, uoff=-o.age * 0.03 + i * 0.7 + 0.3, tile=0.6)
    for k in range(1 if lod else 2):                                           # flow ribbons winding round the body
        prev = None
        nrb = 8
        for j in range(nrb + 1):
            sg = st.tail + (st.head - 0.02 - st.tail) * j / nrb
            gg = j / nrb
            ang = sg * st.L * 1.25 - o.age * 0.42 + k * math.pi
            rr = 0.47 * kh * (0.97 - 0.52 * (1 - gg)) * 1.28
            q = st.pt(sg) + st.s * math.cos(ang) * rr + st.u2 * math.sin(ang) * rr * 0.8
            if prev is not None:
                o.strip2(prev, q, 0.15 * p, "bc_ribbon", W(0.85 * fade))
            prev = q
    for i in range(2 if lod else 5):                                           # mist at the neck join and along the wake
        sg = st.head - (0.03 + 0.07 * i) * (1 if i < 2 else 3)
        sg = clamp(sg, st.tail, st.head)
        mist(o, st.pt(sg) + st.u2 * (0.25 * p) + st.s * (hs(o.seed, i, 9) - 0.5) * 0.5 * p, (0.9 + 0.3 * hs(o.seed, i, 8)) * p, 0.34 * fade, i * 1.3 + o.age * 0.02)
    for i in range(5 if lod else 10):
        sg = st.tail + (st.head - st.tail) * hs(o.seed, i, 4) * 0.9
        life = (o.age * 0.07 + hs(o.seed, i, 5)) % 1.0
        q = st.pt(sg) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.8 * p + st.u2 * (0.4 * life * p + 0.15 * p)
        bubble(o, q, (0.2 + 0.15 * hs(o.seed, i, 7)) * p, math.sin(life * math.pi) * 0.9 * fade)
    for i in range(2 if lod else 4):
        sg = st.tail + (st.head - st.tail) * hs(o.seed, i, 4)
        life = (o.age * 0.06 + hs(o.seed, i, 5)) % 1.0
        q = st.pt(sg) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.5 * math.sin(life * math.pi) * p - 0.35 * life * life * p + 0.35 * p)
        o.bb(q, 0.22 * p, 0.28 * p, "b_drop", 0, W(math.sin(life * math.pi) * fade))
    sheet(o, hp - t * 0.1 * p, hp + t * 2.6 * p, 1.30 * p, "bc_shell", 0.45 * fade, 0.7 * kh, tile=1.0)                      # a soft glass lens over the skull (round edges, fresnel rim)
    sx = 1 if np.dot(X, o.cam - origin) > 0 else -1
    o.bb(origin - Y * 0.0, 3.4 * p, 3.4 * p, "glow", 0, rgba(130, 215, 255, 0.28 * fade), blend="ADD")
    eye = origin + (X * sx * 9.0 + Y * 11.0 + Z * 3.5) * kh / 16
    o.bb(eye, 0.45 * p, 0.45 * p, "b_glint", o.age * 0.1, W(0.85 * fade), blend="ADD")
    o.bb(hp + t * 1.3 * p, 0.9 * p, 0.9 * p, "glow", 0, rgba(120, 220, 255, 0.45 * fade), blend="ADD")
    o.ground(F + UP * 0.05, 0.7 * p * st.open, "b_ripple", W(0.8 * fade * (1 - st.run)), -o.age * 0.07)
    if st.head > 0.88:
        k = (st.head - 0.88) / 0.12
        n = 3 if lod else 4
        for i in range(n):
            a = i * math.tau / n + 0.3
            radial = V(math.cos(a), 0, math.sin(a))
            c.add(nm("bc_horn", lod), T + radial * 0.35 * p * k + V(0, -0.05, 0), UP, xhint=radial, scale=(0.8 * p, 0.7 * p * k, 0.8 * p), alpha=0.92, foam_alpha=0.92, pal="water")
        o.upright(T + V(0, -0.1, 0), 2.6 * p * k, 1.3 * p * k, "b_crown", W(0.55 * (1 - 0.5 * k) * fade))
        c.add(nm("bc_ripple", lod), T + UP * 0.05, UP, V(0, 0, 1), scale=((0.5 + 1.1 * k) * p, 0.8 * p * (1 - k), (0.5 + 1.1 * k) * p), alpha=0.6, foam_alpha=0.7, pal="glass", fres=True)
        o.ground(T + UP * 0.08, (0.5 + 1.1 * k) * p, "b_ripple", W((1 - k) * 0.7 * fade), o.age * 0.05)


def lance_BC(o, c, F, T, p, lod=0):
    st = LanceSt(o, F, T, p)
    o.ground(F + UP * 0.05, 0.5 * p * clamp(o.age / 4), "b_ripple", W(0.8 * st.fade * (1 - st.run)), -o.age * 0.1)
    if st.head <= 0.001:
        return
    hp, d = st.hp, st.d
    k = 1.15 * p * (0.85 + 0.15 * st.fade)
    c.add(nm("bc_lance", lod), hp - d * 2.6 * p, d, UP, scale=(k, k, k), pal="core")
    # glass: a wide shell behind the spear, flow ribbons winding round it, a sheen strip over it
    o.strip2(hp - d * 5.5 * p, hp + d * 0.8 * p, 1.75 * p, "b_lance", W(0.40 * st.fade))
    sheet(o, hp - d * 4.6 * p, hp - d * 0.8 * p, 0.62 * p, "bc_sheen", 0.30 * st.fade, 0.4 * p, uoff=-o.age * 0.05)
    for kk in range(1 if lod else 2):
        prev = None
        nrb = 7 if lod else 10
        for i in range(nrb + 1):
            g = i / nrb
            ang = g * 9 - o.age * 0.45 + kk * math.pi
            q = hp - d * (4.9 * p * (1 - g) - 0.2 * p) + st.s * math.cos(ang) * 0.5 * p * (1 - 0.35 * g) + st.u2 * math.sin(ang) * 0.5 * p * (1 - 0.35 * g)
            if prev is not None:
                o.strip2(prev, q, 0.17 * p, "bc_ribbon", W(0.85 * st.fade))
            prev = q
    for i in range(2 if lod else 3):
        mist(o, hp - d * (3.6 + 1.3 * i) * p + st.u2 * 0.12 * p, 0.85 * p, 0.32 * st.fade, i)
    for i in range(4 if lod else 7):
        life = (o.age * 0.09 + hs(o.seed, i, 5)) % 1.0
        q = hp - d * (hs(o.seed, i, 4) * 4.8 * p) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.8 * p + st.u2 * (0.25 * life * p)
        bubble(o, q, (0.16 + 0.12 * hs(o.seed, i, 7)) * p, math.sin(life * math.pi) * 0.9 * st.fade)
    for i in range(1 if lod else 3):
        life = (o.age * 0.08 + hs(o.seed, i, 5)) % 1.0
        q = hp - d * (hs(o.seed, i, 4) * 4.5 * p) + st.s * (hs(o.seed, i, 6) - 0.5) * 0.9 * p + st.u2 * (0.3 * p - 0.3 * life * p)
        o.bb(q, 0.18 * p, 0.22 * p, "b_drop", 0, W(math.sin(life * math.pi) * st.fade))
    o.bb(hp + d * 0.62 * p, 0.7 * p, 0.7 * p, "b_glint", o.age * 0.2, W(0.9 * st.fade), blend="ADD")
    o.bb(hp + d * 2.2 * p, 0.8 * p, 0.8 * p, "glow", 0, rgba(140, 230, 255, 0.4 * st.fade), blend="ADD")
    if st.head > 0.9:
        kk = (st.head - 0.9) / 0.1
        n = 3
        for i in range(n):
            a = i * math.tau / n
            radial = V(math.cos(a), 0, math.sin(a))
            c.add(nm("bc_horn", lod), T + radial * 0.25 * p * kk + V(0, -0.05, 0), UP, xhint=radial, scale=(0.6 * p, 0.5 * p * kk, 0.6 * p), alpha=0.92, foam_alpha=0.92)
        o.upright(T + V(0, -0.1, 0), 1.9 * p * kk, 1.0 * p * kk, "b_crown", W(0.55 * (1 - 0.5 * kk)))


def cradle_BC(o, c, F, T, p, lod=0):
    fade, op = zone_state(o, p)
    R = p
    cen = F + UP * R * 0.95
    c.add(nm("bc_sphere", lod), cen, UP, V(0, 0, 1), scale=16 * R * 0.97 * op, alpha=0.40, pal="glass", fres=True, foam_alpha=0.9)
    for i in range(2 if lod else 3):                                          # whirl bands: solid foam-blue rings turning on their own axes
        ang = o.age * (0.05 + 0.02 * i) + i * 2.1
        axis = norm(V(math.cos(ang) * 0.5, 1.0, math.sin(ang) * 0.5) + V(0.3 * i, 0, -0.3 * i))
        c.add(nm("bc_ripple", 1), cen, axis, V(0, 0, 1), scale=(R * 1.0 * op, R * 1.3 * op, R * 1.0 * op), alpha=0.7, foam_alpha=0.8, pal="glass", fres=True)
    c.add(nm("bc_ripple", 1), F + UP * 0.05, UP, V(0, 0, 1), scale=(R * 1.0 * op, 0.9 * R, R * 1.0 * op), alpha=0.6, foam_alpha=0.7, pal="glass", fres=True)
    # glass fill: the rim shell + a faint caustic sphere + two crossed caustic sheets + bubbles inside + the chain beside it
    o.ground(F + UP * 0.05, R * 1.1 * op, "b_ripple", W(0.7 * fade), o.age * 0.02)
    o.bb(cen, 2 * R * 0.99 * op, 2 * R * 0.99 * op, "bc_shell", 0, W(0.95 * fade))
    o.bb(cen, 2 * R * 0.93 * op, 2 * R * 0.93 * op, "b_sphere", 0, W(0.20 * fade))
    if not lod:
        for i in range(2):
            yaw = o.age * 0.04 * (1 if i == 0 else -1.4) + i * math.pi / 2
            o.vplane(F + UP * (R * 0.05), yaw, 2 * R * 0.9 * op, 1.9 * R * op, "b_sphere", W(0.16 * fade))
    for i in range(5 if lod else 9):
        life = (o.age * 0.03 + hs(o.seed, i, 5)) % 1.0
        ang = hs(o.seed, i, 4) * math.tau
        rr = R * 0.8 * hs(o.seed, i, 3)
        q = F + V(math.cos(ang) * rr, 0.2 + life * 1.8 * R, math.sin(ang) * rr)
        bubble(o, q, (0.18 + 0.2 * hs(o.seed, i, 7)) * R * 0.6, math.sin(life * math.pi) * 0.95 * fade)
    for i in range(3 if lod else 5):
        q = cen + o.r * (R * (1.15 + 0.3 * i)) + o.u * (-0.22 * R * i + 0.15 * R)
        bubble(o, q, R * (0.36 - 0.05 * i), 0.95 * fade)
    for i in range(2 if lod else 3):
        mist(o, F + V(math.cos(i * 2.1 + o.age * 0.02) * R * 0.8, 0.12, math.sin(i * 2.1 + o.age * 0.02) * R * 0.8), 1.3 * R, 0.30 * fade, i)
    o.bb(cen + o.r * (-R * 0.42) + o.u * (R * 0.55), 0.35 * R, 0.35 * R, "b_glint", 0.3, W(0.8 * fade), blend="ADD")


def nest_BC(o, c, F, T, p, lod=0):
    fade, op = zone_state(o, p)
    R = p
    spin = o.age * 0.05
    c.add(nm("bc_dome", lod), F, UP, V(math.cos(spin), 0, math.sin(spin)), scale=(R * op, R * op, R * op), alpha=0.50 * min(1.0, fade * 1.4), foam_alpha=0.8, pal="core", fres=True)
    c.add(nm("bc_ripple", 1), F + UP * 0.05, UP, V(0, 0, 1), scale=(R * 1.12 * op, 0.9 * R, R * 1.12 * op), alpha=0.6, foam_alpha=0.7, pal="glass", fres=True)
    o.ground(F + UP * 0.05, R * 1.2 * op, "b_ripple", W(0.65 * fade), o.age * 0.03)
    base = math.atan2(o.r[2], o.r[0])
    for i, (wy, sp, w_, al) in enumerate(((0.0, 0.0, 2.0, 0.34), (0.8, 1.0, 1.7, 0.28), (-0.8, -1.4, 1.5, 0.28))[:2 if lod else 3]):
        yaw = base + wy + o.age * 0.02 * sp
        o.vplane(F, yaw, w_ * R * op, R * (1.02 - 0.05 * i) * op, "b_dome", W(al * fade))
    for i in range(5 if lod else 9):
        life = (o.age * 0.03 + hs(o.seed, i, 5)) % 1.0
        ang = hs(o.seed, i, 4) * math.tau
        rr = R * 0.85 * hs(o.seed, i, 3)
        q = F + V(math.cos(ang) * rr, 0.1 + life * 1.1 * R, math.sin(ang) * rr)
        bubble(o, q, 0.22 * R * 0.8, math.sin(life * math.pi) * fade)
    for i in range(2 if lod else 4):
        a = o.age * 0.03 + i * math.tau / 4
        mist(o, F + V(math.cos(a) * R * 1.05, 0.15, math.sin(a) * R * 1.05), 0.9 * R, 0.30 * fade, i)
    for i in range(3 if lod else 5):                                          # beads of foam round the rim (glass bubbles)
        ang = o.age * 0.05 + i * math.tau / (3 if lod else 5)
        bubble(o, F + V(math.cos(ang) * R * 1.1, 0.2 * R + 0.1 * R * math.sin(o.age * 0.2 + i), math.sin(ang) * R * 1.1), 0.26 * R, 0.95 * fade)
    o.bb(F + o.r * (-R * 0.4) + o.u * (R * 0.7) + UP * 0.1, 0.35 * R, 0.35 * R, "b_glint", 0.2, W(0.7 * fade), blend="ADD")
    if not lod:                                                                # the three spiral eddies of the still, as swirl sprites lying on the camera side of the dome
        phi = math.atan2(o.cam[2] - F[2], o.cam[0] - F[0])
        for lon, lat, sz in ((-0.62, 28, 0.55), (0.0, 50, 0.42), (0.62, 28, 0.55)):
            la = math.radians(lat)
            pos = F + V(math.cos(la) * math.cos(phi + lon), math.sin(la), math.cos(la) * math.sin(phi + lon)) * (R * op * 1.03)
            o.bb(pos, sz * R * op, sz * R * op, "b_swirl", o.age * 0.06 * (1 if lon >= 0 else -1), W(0.62 * fade))


HORN_TIP = (23.2, 27.0)                                                        # model px of bc_horn's tip (x outward, y up), see gen_water_bc_meshes.horn_tip


def crown(o, c, F, p, t, lod, n, reach, hgt, wid, base, foam=0.92, horn_alpha=0.92):
    """horns of the C core with the B crown between them; a droplet at every tip"""
    for i in range(n):
        a = i * math.tau / n + base
        radial = V(math.cos(a), 0, math.sin(a))
        sy = max(0.02, hgt * 0.6)
        c.add(nm("bc_horn", lod), F + radial * reach + V(0, 0.02, 0), UP, xhint=radial, scale=(wid * p, sy, wid * p), alpha=0.78, foam_alpha=foam, pal="tongue", fres=True)
        if hgt > 0.35 * p:
            tip = F + radial * (reach + HORN_TIP[0] * wid * p / 16) + V(0, 0.02 + HORN_TIP[1] * sy / 16 + 0.05 * p, 0)
            bubble(o, tip, 0.2 * p, 0.95)


def burst_BC(o, c, F, T, p, lod=0):
    t, fade, flash = burst_t(o)
    if flash > 0:
        o.bb(F + UP * 0.6 * p, 2.6 * p * (0.6 + 0.4 * (1 - flash)), 2.6 * p * (0.6 + 0.4 * (1 - flash)), "glow", 0, W(flash * 0.8), blend="ADD")
    k = clamp(t * 1.4)
    n = 4 if lod else 6
    reach = (0.5 + 1.1 * ease_out(clamp(t * 1.4))) * p
    hgt = (0.35 + 1.25 * math.sin(k * math.pi)) * p
    crown(o, c, F, p, t, lod, n, reach, hgt, 0.95, 0.35)
    rise = ease_out(clamp(t * 1.6))
    h = (0.5 + 1.1 * math.sin(k * math.pi)) * p
    base = math.atan2(o.r[2], o.r[0])
    for yaw in ((0.55,) if lod else (0.55, -0.55)):                              # the glass crown that fills the gaps between the horns (translucent, behind the horns)
        o.vplane(F, base + yaw, (2.0 + 2.4 * rise) * p, h * 1.6, "b_crown", W(0.62 * fade))
    for i in range(2):
        lt = clamp(t * 1.5 - i * 0.2)
        if lt > 0:
            rr = (0.4 + 2.2 * ease_out(lt)) * p
            c.add(nm("bc_ripple", lod), F + UP * 0.04, UP, V(0, 0, 1), scale=(rr, 1.0 * p * (1 - lt), rr), alpha=0.6, foam_alpha=0.7, pal="glass", fres=True)
            o.ground(F + UP * (0.07 + 0.01 * i), (0.4 + 2.6 * ease_out(lt)) * p, "b_ripple", W((1 - lt) * 0.7), o.age * 0.05 + i)
    for i in range(3 if lod else 6):
        a = hs(o.seed, i, 1) * math.tau
        v = (1.0 + 2.2 * hs(o.seed, i, 2)) * p
        vy = (1.6 + 2.0 * hs(o.seed, i, 3)) * p
        tt = t * 1.2
        q = F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.8 * tt * tt * p, math.sin(a) * v * tt * 0.7)
        o.bb(q, 0.2 * p * (1 - 0.4 * t), 0.26 * p * (1 - 0.4 * t), "b_drop", 0, W(0.4 + 0.6 * fade))
    wn.bubbles_arc(o, F, p, t, 4 if lod else 8, "b_bubble", 0.28, o.seed, W(fade))
    for i in range(2 if lod else 4):
        a = i * 1.7 + 0.4
        mist(o, F + V(math.cos(a) * reach * 0.8, 0.25 * p + 0.5 * p * t, math.sin(a) * reach * 0.8), 1.1 * p, 0.32 * fade, i)
    o.bb(F + UP * 0.8 * p, 0.6 * p, 0.6 * p, "b_glint", o.age * 0.1, W(flash), blend="ADD")


def splash_BC(o, c, F, T, p, lod=0):
    t = o.age / o.dur
    fade = 1 - ease_in(t)
    k = clamp(t * 1.5)
    n = 3 if lod else 5
    reach = (0.25 + 0.7 * ease_out(clamp(t * 1.6))) * p
    hgt = (0.25 + 0.95 * math.sin(k * math.pi)) * p
    crown(o, c, F, p, t, lod, n, reach, hgt, 0.75, 0.5)
    rise = ease_out(clamp(t * 2.0))
    o.upright(F, (1.8 + 1.2 * rise) * p, (0.4 + 1.1 * math.sin(clamp(t * 1.5) * math.pi)) * p, "b_crown", W(0.6 * fade))
    rr = (0.3 + 1.5 * ease_out(t)) * p
    c.add(nm("bc_ripple", lod), F + UP * 0.04, UP, V(0, 0, 1), scale=(rr, 0.8 * p * (1 - t), rr), alpha=0.6, foam_alpha=0.7, pal="glass", fres=True)
    o.ground(F + UP * 0.05, (0.3 + 1.6 * ease_out(t)) * p, "b_ripple", W((1 - t) * 0.7), o.age * 0.05)
    for i in range(2 if lod else 4):
        a = hs(o.seed + 5, i, 1) * math.tau
        v = (0.6 + 1.0 * hs(o.seed + 5, i, 2)) * p
        vy = (1.2 + 1.4 * hs(o.seed + 5, i, 3)) * p
        tt = t * 1.2
        q = F + V(math.cos(a) * v * tt * 0.7, 0.1 + vy * tt - 2.8 * tt * tt * p, math.sin(a) * v * tt * 0.7)
        o.bb(q, 0.17 * p, 0.22 * p, "b_drop", 0, W(0.4 + 0.6 * fade))
    wn.bubbles_arc(o, F, p, t, 3 if lod else 5, "b_bubble", 0.22, o.seed + 5, W(fade), vy=(1.2, 2.6), vh=(0.6, 1.6))
    mist(o, F + UP * 0.25 * p, 1.6 * p, 0.3 * fade)


def ring_BC(o, c, F, T, p, lod=0):
    n = 2 if lod else 3
    for kk in range(n):
        lt, r = ring_r(o, p, kk)
        if 0 < lt < 1:
            c.add(nm("bc_ripple", lod), F + UP * (0.02 + 0.01 * kk), UP, V(math.cos(o.age * 0.05 + kk), 0, math.sin(o.age * 0.05 + kk)), scale=(r, 0.9 * p * (1 - lt) ** 0.7, r), alpha=0.55, foam_alpha=0.7, pal="glass", fres=True)
            o.ground(F + UP * (0.05 + 0.01 * kk), r, "b_ripple", W((1 - lt) * 0.78), o.age * (0.05 if kk % 2 else -0.05) + kk)
    for i in range(2 if lod else 4):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        a = hs(o.seed, i, 4) * math.tau
        bubble(o, F + V(math.cos(a) * 0.8 * p, 0.1 + life * 0.8 * p, math.sin(a) * 0.8 * p), 0.2 * p, math.sin(life * math.pi))


def valk_BC(o, c, F, T, p, lod=0):
    fl = 0.1 * math.sin(o.age * 0.12)
    sc = 0.62 * p
    for sgn, nmw in ((1, "bc_wing_r"), (-1, "bc_wing_l")):
        c.add(nm(nmw, lod), F + V(0, 0.32 + fl * 0.4, 0.0), UP, V(0, 0, 1), scale=(sc, sc * (1 + fl), sc), alpha=0.85, foam_alpha=0.85, pal="tongue", fres=True)
        root = F + V(sgn * 3 * sc / 16, 0.32 + 18 * sc / 16, -3.5 * sc / 16 - 0.05)
        ax = V(sgn * 1.0, 0.30 + fl, 0.0)
        ay = V(-sgn * 0.30, 1.0, 0.0)
        w_ = 2.2 * p
        o.dplane(root + norm(ax) * (w_ / 2 - 0.2) + norm(ay) * (w_ / 2 - 0.2), ax, ay, w_, w_, "b_wing", W(0.62), flip=(sgn < 0))
    c.add(nm("bc_ripple", 1), F + V(0, 2.25, 0), UP, V(math.cos(o.age * 0.06), 0, math.sin(o.age * 0.06)), scale=(0.5, 0.5, 0.5), alpha=0.9, foam_alpha=0.9)
    o.bb(F + V(0, 2.28, 0), 0.7, 0.7, "glow", 0, rgba(180, 235, 255, 0.5), blend="ADD")
    o.bb(F + V(0, 2.28, 0), 0.5, 0.5, "b_glint", o.age * 0.05, W(0.7), blend="ADD")
    for i in range(2 if lod else 4):
        side = -1 if i % 2 else 1
        mist(o, F + V(side * (0.35 + 0.15 * i), 1.35 + 0.1 * i, -0.4), 0.7, 0.28, i)
    for i in range(3 if lod else 8):
        life = (o.age * 0.05 + hs(o.seed, i, 5)) % 1.0
        side = -1 if i % 2 else 1
        q = F + V(side * (0.9 + 0.8 * hs(o.seed, i, 6)), 1.9 - 1.7 * life, -0.3 + 0.2 * hs(o.seed, i, 8))
        if i % 2:
            o.bb(q, 0.14, 0.2, "b_drop", 0, W(math.sin(life * math.pi)))
        else:
            bubble(o, q, 0.12 + 0.1 * hs(o.seed, i, 7), math.sin(life * math.pi))


GENS = dict(roar=roar_BC, lance=lance_BC, cradle=cradle_BC, nest=nest_BC, burst=burst_BC, splash=splash_BC, ring=ring_BC, valk=valk_BC)


# ================================================================================================ render
def make_bc(sc, age, lod=0, seed=12345):
    o = QO(sc["cam"], sc["target"], age, sc["dur"], seed)
    cl = CL2()
    GENS[sc["key"]](o, cl, sc["F"], sc["T"], sc["p"], lod)
    return o.finish(), cl


def tile_bc(sc, age, label, sub="", lod=0, fallback=False):
    cam, target = sc["cam"], sc["target"]
    sd = dict(sky=[0.30, 0.34, 0.44], figures=sc["figures"])
    img = pv.background(sd, cam, target)
    img = pv.draw_figures(img, sd, cam, target)
    if fallback:
        q, cl = wn.make(sc, "A", age)
        img = pv.raster(img, q, cam, target)
    else:
        q, cl = make_bc(sc, age, lod)
        zb = np.zeros((pv.H, pv.W), np.float32)
        if cl.items:
            Cb, Ab, Gb, zb = render_meshes(cl.items, np.array(cam, float), np.array(target, float))
            img = img * (1 - Ab[..., None]) + Cb
            img = img + Gb
        img = raster(img, q, cam, target, zb)
    t = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    d = ImageDraw.Draw(t)
    d.rectangle([0, 0, pv.W, 14], fill=(14, 16, 20))
    d.text((5, 2), label, fill=(255, 255, 255))
    if sub:
        d.text((5, pv.H - 13), sub, fill=(255, 235, 150))
    return t


def peak_bc(sc, lod=0):
    pk = dict(sprites=0, mesh=0, total=0, layers=[0, 0, 0])
    for age in range(0, min(sc["dur"], 120) + 1):
        q, cl = make_bc(sc, age, lod)
        c = cl.counts()
        mq = sum(c)
        pk["sprites"] = max(pk["sprites"], len(q))
        pk["mesh"] = max(pk["mesh"], mq)
        pk["total"] = max(pk["total"], len(q) + mq)
        if mq >= sum(pk["layers"]):
            pk["layers"] = c
    return pk


def crop_old():
    """BEFORE | A | B | C tiles of water_compare.png (same camera, same tick; produced by water_new_preview.py)"""
    im = Image.open(os.path.join(OUTDIR, "water_compare.png")).convert("RGB")
    rows = []
    for i in range(len(SCENES)):
        y = 30 + i * CH
        rows.append([im.crop((j * CW, y, (j + 1) * CW, y + CH)) for j in range(4)])
    return rows


CLOSEUPS = [("roar", (3.4, 1.9, 7.4), (0, 1.55, 8.4), 9, "ROAR head"), ("nest", (-4.0, 1.6, 3.4), (0, 1.0, 6.0), 20, "NEST dome"), ("burst", (-5.4, 2.3, 2.8), (0, 0.9, 6.0), 8, "BURST crown")]


def main():
    only = [a for a in sys.argv[1:] if not a.startswith("-")]
    pv.W, pv.H = CW, CH
    load_textures()
    scenes = [s for s in SCENES if not only or s["key"] in only]
    sfx = "_" + "_".join(only) if only else ""
    old = crop_old()
    budget = {}
    cmp_rows, sheet_rows = [], []
    for sc in scenes:
        idx = [s["key"] for s in SCENES].index(sc["key"])
        pk0, pk1 = peak_bc(sc, 0), peak_bc(sc, 1)
        pa = wn.peak(sc, "A", 0)
        budget[sc["key"]] = {"title": sc["title"], "today_live_quads": sc["live"], "BC_lod0": {"total": pk0["total"], "mesh": pk0["mesh"], "sprites": pk0["sprites"], "mesh_layers_0_1_2": pk0["layers"]},
                             "BC_lod1": {"total": pk1["total"], "mesh": pk1["mesh"], "sprites": pk1["sprites"]}, "A_fallback": {"total": pa["total"]}}
        print(sc["key"], "BC", pk0["mesh"], "+", pk0["sprites"], "lod1", pk1["mesh"], "+", pk1["sprites"], "A", pa["total"], flush=True)
        tiles = [tile_bc(sc, age, f"{sc['key']} BC hybrid  t={age}/{sc['dur']}") for age in sc["ages"]]
        sheet_rows.append(tiles)
        o = old[idx]
        cmp_rows.append([o[0], o[2], o[3], tiles[sc["mid"]]])
    sheet = Image.new("RGB", (CW * 3, CH * len(sheet_rows) + 30), (16, 18, 22))
    ImageDraw.Draw(sheet).text((8, 8), "WATER  BC hybrid (C 3D toon core + B glass wrap)   3 ticks per shape   (software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, ts in enumerate(sheet_rows):
        for j, t in enumerate(ts):
            sheet.paste(t, (j * CW, 30 + i * CH))
    sheet.save(os.path.join(OUTDIR, f"water_bc_sheet{sfx}.png"))
    cmp = Image.new("RGB", (CW * 4, CH * len(cmp_rows) + 30), (16, 18, 22))
    ImageDraw.Draw(cmp).text((8, 8), "WATER  BEFORE | B volumetric glass | C real 3D toon | BC hybrid   (same camera, same tick; software render, no bloom, not seen in game)", fill=(255, 255, 255))
    for i, ts in enumerate(cmp_rows):
        for j, t in enumerate(ts):
            cmp.paste(t, (j * CW, 30 + i * CH))
    cmp.save(os.path.join(OUTDIR, f"water_bc_compare{sfx}.png"))
    if not only:
        with open(os.path.join(OUTDIR, "water_bc_budget.json"), "w") as f:
            json.dump(budget, f, indent=1)
        closeups()
        lod_sheet()


def closeups():
    """roar head, nest, burst crown at 2x the compare tile size: B | C | BC would triple the size, so the row is BC only, next to the old C of the same view"""
    pv.W, pv.H = CW * 2, CH * 2
    sheet = Image.new("RGB", (pv.W * 3, pv.H), (16, 18, 22))
    for c, (key, cam, tgt, age, label) in enumerate(CLOSEUPS):
        sc = dict([s for s in SCENES if s["key"] == key][0])
        sc["cam"], sc["target"] = cam, tgt
        sheet.paste(tile_bc(sc, age, f"{label}  BC hybrid  t={age}  (2x)"), (c * pv.W, 0))
    sheet.save(os.path.join(OUTDIR, "water_bc_closeups.png"))
    pv.W, pv.H = CW, CH


def lod_sheet():
    """LOD0 | LOD1 | A sprite fall-back at the same tick, four shapes"""
    keys = ["roar", "nest", "burst", "valk"]
    sheet = Image.new("RGB", (CW * 3, CH * len(keys)), (16, 18, 22))
    for i, key in enumerate(keys):
        sc = [s for s in SCENES if s["key"] == key][0]
        age = sc["ages"][sc["mid"]]
        for j, (lod, fb, lab) in enumerate(((0, False, "BC LOD0 (near)"), (1, False, "BC LOD1 (far / crowded)"), (0, True, "A sprite fall-back (over the frame ceiling)"))):
            sheet.paste(tile_bc(sc, age, f"{key}  {lab}  t={age}", lod=lod, fallback=fb), (j * CW, i * CH))
    sheet.save(os.path.join(OUTDIR, "water_bc_lod.png"))


if __name__ == "__main__":
    main()

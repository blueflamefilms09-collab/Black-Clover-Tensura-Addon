#!/usr/bin/env python3
"""
VALKYRIE ARMOUR v2 = PREVIEW ONLY.  Nothing here is registered in the game; no Java class, no asset, no lang key.

Software render (numpy + Pillow) of the redo of the Valkyrie armour (tools/gen_valkyrie_armor_v2.py): soft draped veil wings, rounded petal frills, water-spiral legs, and the SAINT look (feathered bird wings,
halo), on the block-figure stand-in of tools/mode_preview/preview.py (same boxes; hair, eyes and raised twin tails added), posed like the player model.

    python tools/gen_valkyrie_armor_v2.py
    python tools/vfx_preview/valkyrie_v2_preview.py [ref|turn|vs|flutter|fit|lod|pieces|saint|fp ...]       (no argument: everything)

Output (docs/previews/vfx_remake/valkyrie/): valkyrie_ref_crop.png, valkyrie_wings_front_back_side.png, valkyrie_vs_ref.png, valkyrie_flutter.png, valkyrie_player_fit.png, valkyrie_lod.png,
valkyrie_pieces.png, valkyrie_saint.png, valkyrie_first_person.png.

How a frame is put together (what the layer would do): plates, the stand-in and the alpha-tested petal CARDS (layer 3) z-buffered and toon shaded (3 tones from the quad normal against a light from the upper
left of the camera) with the card texture; the translucent VEIL quads (layer 4: wings, sleeve veils, water strands, book pages) painter-sorted far to near, texture colour and alpha times a fresnel term, no
depth write; then the additive layers (1 flat, 5 textured glow: rims, veins, belt, halo) depth tested against the opaque parts only.  Wings flutter per vertex (weight = distance from the hinge / wing length) with a
travelling wave along the veil.  Honest limits: software render, no bloom, no world light or lightmap, additive drawn as plain addition, the textures are procedural (Pillow) 64 x 64 sheets, the
bubbles / foam / background are flat 2D drawing, never seen in a game, GPU cost not measured.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
ROOT = os.path.abspath(os.path.join(TOOLS, ".."))
sys.path.insert(0, TOOLS)
sys.path.insert(0, HERE)
import gen_valkyrie_armor_v2 as g2                                              # noqa: E402
from valkyrie_preview import rx, ry, rz, tr, about, apply, mirror, basis, bg_plain, bg_ref, label, font   # noqa: E402  (v1 helpers, unchanged)

BASE = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
OUT = BASE
REF = os.path.join(BASE, "ref", "valkyrie_ref_130.png")
REF_SAINT = os.path.join(BASE, "ref", "saint_valkyrie_dress.png")
FOV = 34.0

TONES = {"plate": [(88, 160, 214), (150, 214, 242), (236, 252, 255)],
         "skin": [(176, 124, 96), (226, 170, 138), (250, 214, 186)],
         "suit": [(14, 22, 38), (22, 36, 58), (40, 60, 90)],
         "hair": [(26, 24, 38), (50, 46, 66), (90, 88, 112)],
         "gold": [(176, 138, 56), (226, 188, 92), (255, 236, 160)],
         "dark": [(10, 10, 18), (16, 16, 26), (30, 30, 44)],
         "frost": [(150, 190, 225), (222, 242, 255), (255, 255, 255)],
         "deep": [(26, 74, 156), (44, 116, 206), (110, 178, 240)],
         "scale": [(40, 112, 180), (76, 170, 226), (176, 230, 252)],
         "glass": [(50, 128, 215), (110, 190, 245), (214, 244, 255)]}
RIM = np.array([205, 246, 255], np.float32) / 255
GLOWC = np.array([200, 255, 255], np.float32) / 255
LIGHT = [0.9, 1.0, 1.08]


def v3(*a): return np.array(a, np.float64)


_tex = {}


def tex(name):
    if name not in _tex:
        _tex[name] = np.asarray(Image.open(os.path.join(BASE, "tex", "valk2_%s.png" % name)).convert("RGBA"), np.float32) / 255
    return _tex[name]


WRAP = ("scales",)


def sample(t, U, V, wrap=False):
    th, tw = t.shape[:2]
    if wrap:
        U, V = U % 1.0, V % 1.0
    x = np.clip(U, 0, 0.9999) * tw - 0.5
    y = np.clip(V, 0, 0.9999) * th - 0.5
    x0 = np.clip(np.floor(x).astype(int), 0, tw - 1)
    y0 = np.clip(np.floor(y).astype(int), 0, th - 1)
    x1, y1 = np.clip(x0 + 1, 0, tw - 1), np.clip(y0 + 1, 0, th - 1)
    fx, fy = (x - x0)[..., None], (y - y0)[..., None]
    return (t[y0, x0] * (1 - fx) * (1 - fy) + t[y0, x1] * fx * (1 - fy) + t[y1, x0] * (1 - fx) * fy + t[y1, x1] * fx * fy)


# ================================================================================================ pose / skeleton
class Pose:
    def __init__(self, **kw):
        self.hover = 0.0
        self.lean = 0.0
        self.yaw_body = 0.0
        self.yaw = 0.0
        self.pitch = 0.0
        self.arm_l = (0.0, 0.0)          # (forward, outward) degrees
        self.arm_r = (0.0, 0.0)
        self.leg_l = 0.0
        self.leg_r = 0.0
        self.leg_abd_l = 0.0
        self.leg_abd_r = 0.0
        self.flap = 0.0
        self.bend = 0.0
        self.sweep = 0.0
        self.wave = 0.0                  # phase of the travelling wave along the veils
        self.wave_amp = 0.0
        self.tails = False               # the twin tails of the stand-in raised
        self.__dict__.update(kw)


def bones(p):
    root = tr(0, p.hover, 0) @ ry(p.yaw_body) @ about((0, 12, 0), rx(p.lean))
    B = {"body": root, "root": root, "head": root @ about((0, 24, 0), ry(p.yaw) @ rx(p.pitch))}
    for s, key, arm in ((1, "l", p.arm_l), (-1, "r", p.arm_r)):
        B["arm_" + key] = root @ about((6 * s, 22, 0), rz(s * arm[1]) @ rx(-arm[0]))
    base = tr(0, p.hover, 0) @ ry(p.yaw_body)
    B["leg_l"] = base @ about((2, 12, 0), rz(p.leg_abd_l) @ rx(p.leg_l))
    B["leg_r"] = base @ about((-2, 12, 0), rz(-p.leg_abd_r) @ rx(p.leg_r))
    return B


def wing_deform(pts, uv, hinge, length, s, p, B, scale=1.0):
    d = pts - hinge
    w = np.clip(np.linalg.norm(d, axis=1) / length, 0, 1)
    ang = np.radians(-s * (p.flap + p.bend * w ** 1.4) * scale)
    c, sn = np.cos(ang), np.sin(ang)
    x = hinge[0] + d[:, 0] * c - d[:, 1] * sn
    y = hinge[1] + d[:, 0] * sn + d[:, 1] * c
    z = hinge[2] + d[:, 2]
    sw = np.radians(p.sweep * w * scale)
    cs, ss_ = np.cos(sw), np.sin(sw)
    dy, dz = y - hinge[1], z - hinge[2]
    y2 = hinge[1] + dy * cs - dz * ss_
    z2 = hinge[2] + dy * ss_ + dz * cs
    z2 = z2 + p.wave_amp * w * np.sin(2 * math.pi * 1.3 * uv[:, 1] - p.wave) * (0.25 + 0.75 * uv[:, 0])
    x = x + 0.4 * p.wave_amp * w * np.sin(2 * math.pi * 1.1 * uv[:, 1] - p.wave + 1.0) * uv[:, 0]
    return apply(B["root"], np.c_[x, y2, z2])


# ================================================================================================ geometry containers
class Quads:
    def __init__(self):
        self.P, self.UV, self.K, self.TX, self.T, self.A, self.Z = [], [], [], [], [], [], []

    def add(self, pts, uv, kind, tx, tint, alpha=1.0):
        self.P.append(pts)
        self.UV.append(uv)
        self.K.append(kind)
        self.TX.append(tx)
        self.T.append(tint)
        self.A.append(alpha)

    def __len__(self): return len(self.P)


KIND = {0: "plate", 1: "add", 3: "card", 4: "veil", 5: "glow", 6: "frost", 7: "deep", 8: "scale", 9: "glass"}


def mesh_to_quads(out, mesh, texmap, fn, only_kinds=None, tint_gold=False):
    if not mesh.q:
        return
    allp = np.array([[v[:3] for v in qd[1]] for qd in mesh.q], float).reshape(-1, 3)
    uvs = np.array([[v[3:5] for v in qd[1]] for qd in mesh.q], float).reshape(-1, 2)
    wp = fn(allp, uvs).reshape(-1, 4, 3)
    uu = uvs.reshape(-1, 4, 2)
    for k, qd in enumerate(mesh.q):
        kind = KIND[qd[0]]
        rgb = np.array(qd[3], np.float32) / 255
        pal = "gold" if (kind == "plate" and rgb[2] < 0.55) else "plate"
        out.add(wp[k], uu[k], kind if kind != "plate" else pal, texmap.get(qd[0]), rgb)


def obox(out, c, ax, ay, hw, hh, ht, kind, M, tint=(255, 255, 255)):
    ax, ay = np.array(ax, float), np.array(ay, float)
    ax /= np.linalg.norm(ax)
    ay /= np.linalg.norm(ay)
    az = np.cross(ax, ay)
    pts = {}
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                pts[(sx, sy, sz)] = np.array(c, float) + ax * sx * hw + ay * sy * hh + az * sz * ht
    faces = [((1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)), ((-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, -1, -1)), ((-1, 1, -1), (-1, 1, 1), (1, 1, 1), (1, 1, -1)),
             ((-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)), ((-1, -1, -1), (-1, 1, -1), (1, 1, -1), (1, -1, -1)), ((1, -1, 1), (1, 1, 1), (-1, 1, 1), (-1, -1, 1))]
    for f in faces:
        p = apply(M, np.array([pts[k] for k in f]))
        out.add(p, np.zeros((4, 2)), kind, None, np.array(tint, np.float32) / 255)


def box(out, mn, mx, kind, M, tint=(255, 255, 255)):
    c = [(mn[i] + mx[i]) / 2 for i in range(3)]
    obox(out, c, (1, 0, 0), (0, 1, 0), (mx[0] - mn[0]) / 2, (mx[1] - mn[1]) / 2, (mx[2] - mn[2]) / 2, kind, M, tint)


def figure(out, B, p, legs=True):
    box(out, (-4, 12, -2), (4, 24, 2), "suit", B["body"])
    box(out, (-4, 24, -4), (4, 32, 4), "skin", B["head"])
    box(out, (-4.4, 29, -4.4), (4.4, 32.4, 4.4), "hair", B["head"])
    box(out, (-4.4, 25, -4.4), (4.4, 29, -3.0), "hair", B["head"])
    box(out, (-4.4, 28.2, 4.0), (4.4, 29.6, 4.5), "hair", B["head"])
    for sx in (-1, 1):
        box(out, (sx * 2.4 - 0.6, 26.4, 4.02), (sx * 2.4 + 0.6, 27.6, 4.3), "dark", B["head"])
        if p.tails:                                                                     # twin tails flying up and out like the still
            for j in range(3):
                c = (sx * (5.2 + 1.4 * j), 28.5 + 3.2 * j, 0.0)
                obox(out, c, (1, 0, 0), (0, 1, 0), 0.8, 2.0, 1.1, "hair", B["head"])
        else:
            box(out, (sx * 4.5 - 0.5, 20, -1.8), (sx * 4.5 + 0.5, 28, 1.8), "hair", B["head"])
    for s, key in ((1, "l"), (-1, "r")):
        x0, x1 = (4, 8) if s > 0 else (-8, -4)
        box(out, (x0, 12, -2), (x1, 24, 2), "skin", B["arm_" + key])
        lx0, lx1 = (0, 4) if s > 0 else (-4, 0)
        if legs:
            box(out, (lx0, 0, -2), (lx1, 12, 2), "suit", B["leg_" + key])


def armour(out, B, p, lod, look="VALKYRIE", only=None, side=None):
    ms = g2.build_all()
    for name, fn, bone, copies, tx, looks in g2.PIECES:
        if look not in looks:
            continue
        if only and name not in only:
            continue
        m = ms[(name, lod)]
        for c in range(copies):
            s = 1 if c == 0 else -1
            if side is not None and s != side:
                continue
            if bone in ("arm", "leg"):
                M = B["%s_%s" % (bone, "l" if s > 0 else "r")]
            elif bone in ("head", "body"):
                M = B[bone]
            else:
                M = None
            if M is not None:
                def f(pts, uv, M=M, s=s):
                    return apply(M, pts if s > 0 else mirror(pts))
            else:
                info = g2.BONES[bone]
                H = np.array(info["hinge"], float)
                Ls = info["length"]
                scale = -0.7 if bone == "wing_hinge_low" else 1.0

                def f(pts, uv, s=s, H=H, Ls=Ls, scale=scale):
                    q = pts if s > 0 else mirror(pts)
                    h = H if s > 0 else np.array([-H[0], H[1], H[2]])
                    return wing_deform(q, uv, h, Ls, s, p, B, scale)
            mesh_to_quads(out, m, {int(k): v for k, v in tx.items()}, f)
    return out


# ================================================================================================ rasteriser
def render(quads, eye, target, size, bg, ss=2, fov=FOV, front=None):
    """quads in px (the model space); the camera is in blocks."""
    Wd, Hd = size
    w, h = Wd * ss, Hd * ss
    f, r, u = basis(eye, target)
    k = (h / 2) / math.tan(math.radians(fov / 2))
    SUN = -0.55 * r + 0.72 * u - 0.3 * f + 0.25 * np.array([0, 1, 0])
    SUN = SUN / np.linalg.norm(SUN)
    img = np.asarray(bg.resize((w, h), Image.BILINEAR).convert("RGB"), np.float32).copy() / 255
    ZB = np.zeros((h, w), np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    recs = []
    for i in range(len(quads)):
        P = quads.P[i] / 16.0
        n = np.cross(P[2] - P[0], P[3] - P[1])
        ln = np.linalg.norm(n)
        if ln < 1e-12:
            n = np.cross(P[1] - P[0], P[3] - P[0])
            ln = np.linalg.norm(n)
            if ln < 1e-12:
                continue
        n = n / ln
        cen = P.mean(axis=0)
        Vv = eye - cen
        Vv = Vv / np.linalg.norm(Vv)
        ndv = float(n @ Vv)
        if ndv < 0:
            n = -n
        ndv = abs(ndv)
        pc = P - eye
        z = pc @ f
        if np.any(z < 0.3):
            continue
        hl = 0.5 + 0.5 * float(n @ SUN)
        tone = 2 if hl > 0.86 else 1 if hl > 0.55 else 0
        recs.append((quads.K[i], quads.TX[i], quads.T[i], quads.UV[i], pc, z, tone, ndv, float(z.mean())))

    def tris(pc, z, uv):
        sx = w / 2 + (pc @ r) / z * k
        sy = h / 2 - (pc @ u) / z * k
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
            msk = (a >= -2e-3) & (b >= -2e-3) & (c >= -2e-3)
            if not msk.any():
                continue
            wa, wb, wc = a / z[tri[0]], b / z[tri[1]], c / z[tri[2]]
            iz = wa + wb + wc
            U = (wa * uv[tri[0], 0] + wb * uv[tri[1], 0] + wc * uv[tri[2], 0]) / iz
            V = (wa * uv[tri[0], 1] + wb * uv[tri[1], 1] + wc * uv[tri[2], 1]) / iz
            yield (slice(miny, maxy), slice(minx, maxx)), msk, iz.astype(np.float32), U, V

    flat = ("plate", "skin", "suit", "hair", "gold", "dark", "frost", "deep", "scale")
    for kind, tx, tint, uv, pc, z, tone, ndv, zc in recs:                                  # opaque: plates, stand-in, cut-out cards
        if kind in flat:
            pal = np.array(TONES[kind][tone], np.float32) / 255
            col = pal * (0.7 + 0.3 * tint) if kind in ("plate", "gold") else pal
            if ndv < 0.22 and kind in ("plate", "frost", "deep", "scale"):
                col = col * 0.6 + RIM * 0.4
            for sl, msk, iz, U, V in tris(pc, z, uv):
                ok = msk & (iz > ZB[sl])
                ZB[sl] = np.where(ok, iz, ZB[sl])
                img[sl] = np.where(ok[..., None], col[None, None, :], img[sl])
        elif kind == "card":
            t = tex(tx)
            for sl, msk, iz, U, V in tris(pc, z, uv):
                px = sample(t, U, V, tx in WRAP)
                ok = msk & (iz > ZB[sl]) & (px[..., 3] > 0.5)
                col = np.clip(px[..., :3] * tint[None, None, :] * LIGHT[tone], 0, 1)
                if ndv < 0.2:
                    col = col * 0.8 + RIM * 0.2
                ZB[sl] = np.where(ok, iz, ZB[sl])
                img[sl] = np.where(ok[..., None], col, img[sl])
    veil = sorted([q for q in recs if q[0] in ("veil", "glass")], key=lambda q: -q[8])
    for kind, tx, tint, uv, pc, z, tone, ndv, zc in veil:                                  # translucent, far to near
        if kind == "glass":
            gc = np.array(TONES["glass"][tone], np.float32) / 255
            ga = float(np.clip(0.72 + 0.3 * (1 - ndv), 0.7, 0.95))
            for sl, msk, iz, U, V in tris(pc, z, uv):
                a3 = (ga * (msk & (iz > ZB[sl])))[..., None]
                img[sl] = img[sl] * (1 - a3) + (gc * (0.7 + 0.3 * ndv) + (RIM * 0.3 if ndv < 0.25 else 0))[None, None, :] * a3
            continue
        t = tex(tx)
        for sl, msk, iz, U, V in tris(pc, z, uv):
            px = sample(t, U, V)
            al = np.clip(px[..., 3] * (0.82 + 0.45 * (1 - ndv)), 0, 0.97)
            ok = msk & (iz > ZB[sl])
            col = np.clip(px[..., :3] * LIGHT[tone] * (1.0 + (0.12 if ndv < 0.25 else 0.0)), 0, 1)
            a3 = (al * ok)[..., None]
            img[sl] = img[sl] * (1 - a3) + col * a3
    for kind, tx, tint, uv, pc, z, tone, ndv, zc in recs:                                  # emissive, additive
        if kind not in ("add", "glow"):
            continue
        t = tex(tx) if kind == "glow" and tx else None
        for sl, msk, iz, U, V in tris(pc, z, uv):
            ok = msk & (iz > ZB[sl] * 0.995)
            if t is not None:
                px = sample(t, U, V)
                add_ = px[..., :3] * px[..., 3:4] * 0.8 * tint[None, None, :]
            else:
                add_ = (GLOWC * (0.55 + 0.45 * ndv) * 0.85)[None, None, :]
            img[sl] = np.where(ok[..., None], np.clip(img[sl] + add_, 0, 1), img[sl])
    if front is not None:
        img = front(img, w, h, ZB, (eye, f, r, u, k))
    out = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).resize((Wd, Hd), Image.LANCZOS)
    return out, (eye, f, r, u, k / ss, Wd, Hd)


def project(cam, p):
    eye, f, r, u, k, Wd, Hd = cam
    pc = np.asarray(p, float) - eye
    z = pc @ f
    return Wd / 2 + (pc @ r) / z * k, Hd / 2 - (pc @ u) / z * k, z


def cam_of(eye, target, size, fov=FOV):
    f, r, u = basis(eye, target)
    k = (size[1] / 2) / math.tan(math.radians(fov / 2))
    return (eye, f, r, u, k, size[0], size[1])


# ================================================================================================ bubbles, foam
def stamp_bubble(img, cx, cy, rad, strength=1.0):
    """a translucent sphere: faint body, bright fresnel rim, a window glint top-left and a soft caustic bottom-right (img float (h, w, 3), pixel units)."""
    h, w = img.shape[:2]
    x0, x1 = int(max(0, cx - rad - 2)), int(min(w, cx + rad + 3))
    y0, y1 = int(max(0, cy - rad - 2)), int(min(h, cy + rad + 3))
    if x0 >= x1 or y0 >= y1:
        return
    Y, X = np.mgrid[y0:y1, x0:x1].astype(np.float32)
    dx, dy = (X + 0.5 - cx) / rad, (Y + 0.5 - cy) / rad
    rr = np.sqrt(dx * dx + dy * dy)
    inside = rr <= 1.0
    body = 0.07 + 0.5 * np.clip(rr, 0, 1) ** 6
    rim = np.exp(-((rr - 0.94) / 0.05) ** 2) * 0.55
    gl = np.exp(-(((dx + 0.38) ** 2) * 14 + ((dy + 0.42) ** 2) * 22)) * 0.9
    ca = np.exp(-(((dx - 0.4) ** 2) * 9 + ((dy - 0.42) ** 2) * 9)) * 0.28
    a = np.clip((body + rim) * strength, 0, 0.9) * inside
    col = np.array([0.82, 1.0, 0.98], np.float32)
    reg = img[y0:y1, x0:x1]
    reg[:] = reg * (1 - a[..., None]) + col * a[..., None]
    reg[:] = np.clip(reg + (gl + ca)[..., None] * inside[..., None] * 0.9, 0, 1)


def bubble_field(seed, n, centre, spread, rmin=0.025, rmax=0.13):
    rng = np.random.default_rng(seed)
    out = []
    for i in range(n):
        p = np.array(centre) + rng.normal(0, 1, 3) * np.array(spread)
        out.append((p, float(rng.uniform(rmin, rmax) ** 1.0)))
    return out


def stamp_bubbles(img, cam, field, scale):
    for p, rad in field:
        x, y, z = project(cam, p)
        stamp_bubble(img, x * scale, y * scale, max(2.0, rad / z * cam[4] * scale))


def foam_clusters(img, cam, scale, specs, seed=1):
    """white water puffs (flat 2D stand-in for the existing Water FX): specs = [(world point, radius in blocks)]."""
    rng = np.random.default_rng(seed)
    h, w = img.shape[:2]
    lay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(lay)
    for p, rad in specs:
        x, y, z = project(cam, p)
        x, y = x * scale, y * scale
        r = rad / z * cam[4] * scale
        for j in range(22):
            ox, oy = rng.normal(0, r * 0.7), rng.normal(-r * 0.1, r * 0.45)
            rr = r * rng.uniform(0.16, 0.42)
            c = int(rng.uniform(225, 255))
            d.ellipse((x + ox - rr, y + oy - rr, x + ox + rr, y + oy + rr), fill=(c, 255, 252, int(rng.uniform(150, 230))))
    lay = lay.filter(ImageFilter.GaussianBlur(max(1, h * 0.0022)))
    base = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8)).convert("RGBA")
    base.alpha_composite(lay)
    img[:] = np.asarray(base.convert("RGB"), np.float32) / 255


# ================================================================================================ scenes
POSES = {
    "stand": Pose(),
    "cast": Pose(arm_l=(8, 150), arm_r=(8, 150), pitch=-8, flap=4, bend=6, wave_amp=0.6),
    "ref": Pose(hover=2.5, lean=-3, yaw_body=-8, yaw=8, pitch=-14, arm_l=(14, 148), arm_r=(10, 152), leg_l=10, leg_r=-14, leg_abd_l=8, leg_abd_r=7, flap=3, bend=5, wave_amp=0.55, wave=1.0, tails=True),
    "point": Pose(arm_r=(88, 4), arm_l=(0, 6), flap=2, bend=3),
    "sneak": Pose(lean=26, pitch=-26, leg_l=-10, leg_r=14, arm_l=(-16, 4), arm_r=(10, 4), flap=2, bend=3),
    "walk": Pose(arm_l=(-32, 3), arm_r=(32, 3), leg_l=30, leg_r=-30, flap=3, bend=4, wave_amp=0.4),
}


def orbit(angle, dist=7.2, height=1.6, look_y=1.55):
    a = math.radians(angle)
    return v3(math.sin(a) * dist, height, math.cos(a) * dist), v3(0, look_y, 0)


def scene(pose, lod, eye, target, size, bg, look="VALKYRIE", ss=2, fig=True, arm=True, only=None, front=None):
    q = Quads()
    B = bones(pose)
    if fig:
        figure(q, B, pose)
    if arm:
        armour(q, B, pose, lod, look=look, only=only)
    img, cam = render(q, eye, target, size, bg, ss=ss, front=front)
    return img, cam, q


def count_armour(q):
    return sum(1 for k in q.K if k in ("plate", "gold", "card", "veil", "glow", "add"))


def turnaround(look="VALKYRIE", tag="valkyrie_wings_front_back_side.png"):
    pose = Pose(flap=2, bend=4, wave_amp=0.5, wave=0.6)
    names = ["front", "front-left 45", "left side", "back-left 135", "back", "back-right 225", "right side", "front-right 315"]
    tiles = []
    for i, nm in enumerate(names):
        eye, tgt = orbit(i * 45)
        img, cam, q = scene(pose, 0, eye, tgt, (300, 440), bg_plain((300, 440)), look=look)
        label(img, nm)
        tiles.append(img)
    sheet = Image.new("RGB", (1200, 880), (10, 14, 18))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 4) * 300, (i // 4) * 440))
    p = os.path.join(OUT, tag)
    sheet.save(p)
    return p


def fit_sheet():
    rows = [("standing", POSES["stand"]), ("casting, arms raised", POSES["cast"]), ("one arm raised (lance pose)", POSES["point"]), ("sneaking", POSES["sneak"]), ("walking", POSES["walk"])]
    views = [("front", 0), ("three-quarter", 40), ("side", 90), ("back", 180)]
    TW, TH = 240, 340
    sheet = Image.new("RGB", (TW * 4, TH * len(rows)), (10, 14, 18))
    for r_, (rn, pose) in enumerate(rows):
        for c_, (vn, ang) in enumerate(views):
            eye, tgt = orbit(ang, 7.2, 1.6, 1.5)
            img, cam, q = scene(pose, 0, eye, tgt, (TW, TH), bg_plain((TW, TH)))
            label(img, "%s / %s" % (rn, vn), sz=12)
            sheet.paste(img, (c_ * TW, r_ * TH))
    p = os.path.join(OUT, "valkyrie_player_fit.png")
    sheet.save(p)
    return p


def flutter_sheet():
    keys = [("A  closed -12 / tips -16", Pose(flap=-12, bend=-16, sweep=5, wave_amp=0.7, wave=0.0)), ("B  -5 / -7", Pose(flap=-5, bend=-7, sweep=2, wave_amp=0.7, wave=1.3)), ("C  rest", Pose(wave_amp=0.7, wave=2.6)),
            ("D  +7 / +10", Pose(flap=7, bend=10, sweep=-2, wave_amp=0.7, wave=3.9)), ("E  open +14 / +22", Pose(flap=14, bend=22, sweep=-5, wave_amp=0.7, wave=5.2))]
    TW, TH = 280, 400
    sheet = Image.new("RGB", (TW * 5, TH * 2), (10, 14, 18))
    for i, (n, p) in enumerate(keys):
        for j, (ang, nm) in enumerate(((180, "back"), (35, "3/4 front"))):
            eye, tgt = orbit(ang, 7.2, 1.7, 1.55)
            img, cam, q = scene(p, 0, eye, tgt, (TW, TH), bg_plain((TW, TH)))
            label(img, "%s (%s)" % (n, nm), sz=12)
            sheet.paste(img, (i * TW, j * TH))
    pth = os.path.join(OUT, "valkyrie_flutter.png")
    sheet.save(pth)
    return pth


def lod_sheet():
    TW, TH = 300, 440
    sheet = Image.new("RGB", (TW * 3, TH * 2), (10, 14, 18))
    for lod, nm in enumerate(("NEAR", "MID", "FAR")):
        for j, ang in enumerate((35, 180)):
            eye, tgt = orbit(ang, 7.2, 1.6, 1.55)
            img, cam, q = scene(Pose(flap=2, bend=3, wave_amp=0.4), lod, eye, tgt, (TW, TH), bg_plain((TW, TH)))
            label(img, "%s  %d armour quads (+ grimoire)" % (nm, count_armour(q)), sz=12)
            sheet.paste(img, (lod * TW, j * TH))
    p = os.path.join(OUT, "valkyrie_lod.png")
    sheet.save(p)
    return p


def pieces_sheet():
    names = [p[0] for p in g2.PIECES if "VALKYRIE" in p[5]]
    TW, TH = 300, 300
    cols = 4
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (TW * cols, TH * rows), (10, 14, 18))
    for i, nm in enumerate(names):
        q = Quads()
        ms = g2.build_all()
        info = next(p for p in g2.PIECES if p[0] == nm)
        mesh_to_quads(q, ms[(nm, 0)], {int(k): v for k, v in info[4].items()}, lambda p, uv: p)
        allp = np.concatenate(q.P)
        c = (allp.max(axis=0) + allp.min(axis=0)) / 2
        ext = float(np.max(allp.max(axis=0) - allp.min(axis=0)))
        d = v3(0.35, 0.25, 1.0)
        eye = (c + d / np.linalg.norm(d) * ext * 2.4) / 16
        img, cam = render(q, eye, c / 16, (TW, TH), bg_plain((TW, TH)), ss=2)
        label(img, "%s  %d quads (near, left copy)" % (nm, len(ms[(nm, 0)].q)), sz=12)
        sheet.paste(img, ((i % cols) * TW, (i // cols) * TH))
    p = os.path.join(OUT, "valkyrie_pieces.png")
    sheet.save(p)
    return p


# ---- the reference study with the crops
def ref_crop():
    ref = Image.open(REF).convert("RGB")
    S = 4
    big = ref.resize((ref.width * S, ref.height * S), Image.LANCZOS)
    crops = [("wing, viewer left: a thin smooth blade, sharp tip, bright rim line, teal body, soft inner edge", (40, 0, 100, 70)),
             ("wing, viewer right: wider, stepped / frilled trailing edge, bright rim, runs into the sleeve", (110, 0, 175, 75)),
             ("tiara: ~5 slim thorns on a band; hair loops up", (75, 8, 125, 50)),
             ("bodice: layered rounded frosty feathers; book = white soft-edged sheet", (70, 35, 140, 85)),
             ("legs: twisted ropes of water, a crown of fingers at the top, foam", (60, 70, 140, 144))]
    cw = 300
    sheet = Image.new("RGB", (big.width + cw * 2 + 12, max(big.height, 3 * 232) + 4), (14, 18, 22))
    sheet.paste(big, (0, 0))
    d = ImageDraw.Draw(sheet)
    f = font(12)
    y = 2
    for i, (txt, box_) in enumerate(crops):
        c = ref.crop(box_)
        sc = min((cw - 4) / c.width, 200 / c.height)
        c = c.resize((int(c.width * sc), int(c.height * sc)), Image.BICUBIC)
        x = big.width + 6 + (i % 2) * (cw + 2)
        yy = 2 + (i // 2) * 232
        sheet.paste(c, (x, yy))
        words, line, ty = txt.split(), "", yy + c.height + 4
        for wd in words:
            if d.textlength(line + " " + wd, font=f) > cw - 6:
                d.text((x, ty), line, fill=(235, 245, 240), font=f)
                ty += 14
                line = wd
            else:
                line = (line + " " + wd).strip()
        d.text((x, ty), line, fill=(235, 245, 240), font=f)
        bx = [b * S for b in box_]
        d.rectangle(bx, outline=(255, 226, 90), width=2)
        d.text((bx[0] + 3, bx[1] + 2), str(i + 1), fill=(255, 226, 90), font=font(15))
        d.text((x + 3, yy + 2), str(i + 1), fill=(255, 226, 90), font=font(15))
    d.text((8, 6), "reference: the anime still (Valkyrie_Dress.webp, 200x144 x4) and its five studied crops", fill=(255, 255, 255), font=font(14))
    p = os.path.join(OUT, "valkyrie_ref_crop.png")
    sheet.save(p)
    return p


def ref_scene(look="VALKYRIE", pose=None, size=(1000, 720), seed=4):
    pose = pose or POSES["ref"]
    eye = v3(-2.0, 0.8, 5.3)
    tgt = v3(0.0, 1.58, 0.0)
    bg = bg_ref(size, seed)
    cam0 = cam_of(eye, tgt, size)
    back = bubble_field(7, 22, (0.0, 1.5, -0.9), (1.6, 1.0, 0.5))
    near = bubble_field(11, 20, (0.0, 1.5, 1.3), (1.5, 0.9, 0.4))
    sc = 2
    arr = np.asarray(bg.resize((size[0] * sc, size[1] * sc), Image.BILINEAR), np.float32) / 255
    arr = arr.copy()
    stamp_bubbles(arr, cam0, back, sc)
    bg2 = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8)).resize(size, Image.LANCZOS)

    def front(img, w, h, ZB, c):
        cam_s = cam_of(eye, tgt, (w, h))
        foam_clusters(img, cam_s, 1, [(v3(-0.75, -0.08, 0.2), 0.42), (v3(0.7, -0.12, 0.25), 0.36), (v3(0.0, -0.3, 0.35), 0.5), (v3(-1.25, 0.25, -0.2), 0.33), (v3(1.25, 0.2, -0.1), 0.28),
                                      (v3(-1.5, -0.1, 0.0), 0.4), (v3(1.6, -0.05, 0.1), 0.34)], seed=3)
        stamp_bubbles(img, cam_s, near, 1)
        return img
    img, cam, q = scene(pose, 0, eye, tgt, size, bg2, look=look, front=front)
    return img, q


def vs_ref(tag="valkyrie_vs_ref.png"):
    ref = Image.open(REF).convert("RGB")
    size = (1000, 720)
    refbig = ref.resize(size, Image.LANCZOS)
    img, q = ref_scene()
    sheet = Image.new("RGB", (size[0] * 2 + 6, size[1] + 24), (12, 12, 14))
    sheet.paste(refbig, (0, 24))
    sheet.paste(img, (size[0] + 6, 24))
    label(sheet, "REFERENCE (anime still, Valkyrie Dress)", (8, 3), 14)
    label(sheet, "NEW v2: veil wings + petal frills + water legs; same pose / camera idea (software render; bubbles, foam, background drawn flat)", (size[0] + 14, 3), 14)
    p = os.path.join(OUT, tag)
    sheet.save(p)
    return p


# ---- the Saint look
def saint_sheet():
    ref = Image.open(REF_SAINT).convert("RGB")
    size = (1000, 820)
    sref = ref.resize((size[0], int(size[0] * ref.height / ref.width)), Image.LANCZOS)
    pose = Pose(arm_l=(10, 40), arm_r=(12, 28), leg_l=-6, leg_r=10, pitch=-4, hover=2, flap=0, bend=0)
    eye, tgt = orbit(25, 8.6, 1.5, 1.7)
    bg = bg_plain((1000, int(1000 * ref.height / ref.width)), (22, 40, 70), (150, 190, 215))
    img, cam, q = scene(pose, 0, eye, tgt, (1000, sref.height), bg, look="SAINT")
    tiles = []
    for nm, ang in (("front", 0), ("three-quarter", 40), ("side", 90), ("back", 180)):
        e2, t2 = orbit(ang, 9.4, 1.6, 1.7)
        im2, c2, q2 = scene(Pose(), 0, e2, t2, (250, 330), bg_plain((250, 330), (22, 40, 70), (150, 190, 215)), look="SAINT")
        label(im2, "SAINT %s" % nm, sz=12)
        tiles.append(im2)
    W2 = 1000 * 2 + 6
    sheet = Image.new("RGB", (W2, 24 + sref.height + 336), (12, 12, 14))
    sheet.paste(sref, (0, 24))
    sheet.paste(img, (1006, 24))
    label(sheet, "REFERENCE: Saint_Valkyrie_Dress.webp (manga page: feathered wings, halo, winged helm)", (8, 3), 14)
    label(sheet, "NEW v2 SAINT look: feather wings (bone + 3 rows of feather cards), halo, winged helm, gold sash; plates and frills shared with the base look", (1014, 3), 14)
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * 252, 24 + sref.height + 4))
    p = os.path.join(OUT, "valkyrie_saint.png")
    sheet.save(p)
    return p


# ---- first person: which pieces a hand hook would draw
def first_person():
    """what the local player would see: only the right arm, its sleeve, shoulder cap and wrist cuff (a hand-render hook would draw exactly this; the body layers are not drawn in first person)."""
    pose = Pose(arm_r=(72, 16), flap=0)
    eye = v3(0.0, 1.62, -0.12)
    tgt = v3(-0.12, 1.42, 1.0)
    size = (640, 360)
    bg = bg_plain(size, (30, 70, 80), (70, 130, 120))
    B = bones(pose)
    q = Quads()
    box(q, (-8, 12, -2), (-4, 24, 2), "skin", B["arm_r"])
    armour(q, B, pose, 0, only=("cuff", "shoulder", "arm_veil", "sleeve"), side=-1)
    img, cam = render(q, eye, tgt, size, bg, ss=2, fov=75)
    label(img, "first person: the right arm only (sleeve, shoulder cap, cuff, sleeve veil), as a hand-render hook would draw it", sz=12)
    p = os.path.join(OUT, "valkyrie_first_person.png")
    img.save(p)
    return p


def main():
    os.makedirs(OUT, exist_ok=True)
    todo = sys.argv[1:] or ["ref", "turn", "vs", "flutter", "fit", "lod", "pieces", "saint", "fp"]
    fn = {"ref": ref_crop, "turn": turnaround, "vs": vs_ref, "flutter": flutter_sheet, "fit": fit_sheet, "lod": lod_sheet, "pieces": pieces_sheet, "saint": saint_sheet, "fp": first_person}
    for t in todo:
        print("wrote", fn[t]())


if __name__ == "__main__":
    main()

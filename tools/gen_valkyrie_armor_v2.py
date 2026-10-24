#!/usr/bin/env python3
"""
VALKYRIE ARMOUR v2 (the redo after "try again") = PREVIEW ONLY (nothing is registered in the game, nothing goes to src/ or assets/).

v1 (tools/gen_valkyrie_armor_meshes.py, kept, previews in docs/previews/vfx_remake/valkyrie/v1/) built the wings as stiff faceted crystal slabs and the frills as thorns.  v2 follows the anime still
(docs/previews/vfx_remake/valkyrie/ref/valkyrie_ref_130.png = Valkyrie_Dress.webp) much more closely:

  * the wings are a SOFT DRAPED VEIL: one thin curved membrane per wing (a smooth grid, a gentle static ripple, a scalloped trailing edge, a second frill layer over the outer half, a bright leading rim,
    two glowing veins) with the gradient white-cyan core -> teal edge and the transparency painted in a small TEXTURE (the mesh carries real UVs: u across the veil, v along it), flowing out of the shoulder
    together with a sleeve veil that hangs off each arm
  * the bodice, skirt, shoulder caps and wrist cuffs are tiers of ROUNDED petal cards (one textured card with a rounded alpha mask, two quads per petal so it bends), not thorns
  * the legs are spiral strands of water with a crown of flame fingers at the hip, as in the still (pale rope-like columns), not boots
  * a second selectable look, the SAINT Valkyrie Dress (Saint_Valkyrie_Dress.webp): big feathered bird wings, a halo, a winged helm, a gold sash

Mesh text format = the `q` lines of tools/gen_sdw3_models.py (the `Mesh` class).  Layers (new in v2, 0..2 as before):
  0 plate: flat toon shaded solid        1 emissive flat (additive)        2 unused
  3 CARD: textured, alpha-tested (cut-out), drawn like entityCutoutNoCull      4 VEIL: textured, translucent, no culling (entityTranslucent)      5 GLOW: textured, additive
The texture of a layer 3/4/5 quad is per piece (pieces.json "tex").  Units: model pixels (1/16 block), REST pose of the vanilla player model: y up, feet y = 0, front = +z; the LEFT (+x) copy of a
paired piece is written, the layer draws the mirror.  LODs: no suffix = NEAR, `_mid`, `_lo` = FAR.

    python tools/gen_valkyrie_armor_v2.py   ->  docs/previews/vfx_remake/valkyrie/{meshes/valk2_*.txt, tex/*.png, pieces_v2.json, counts_v2.txt}
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh, add, sub, mul, cross, dot, norm, mean                # noqa: E402
from gen_valkyrie_armor_meshes import lead_curve, hsh, ring, loft, leaf, slab          # noqa: E402  (v1 helpers, unchanged)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
BASE = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie")
OUT = os.path.join(BASE, "meshes")
TEXD = os.path.join(BASE, "tex")

W = (255, 255, 255)
P = (232, 248, 255)
A = (200, 236, 250)
D = (150, 210, 238)
G = (210, 255, 255)
VEIN = (120, 205, 215)
RIMC = (170, 235, 245)
GOLD = (236, 200, 104)

BONES = {"wing_hinge": {"hinge": (4.6, 21.5, -2.2), "length": 32.0}, "wing_hinge_low": {"hinge": (2.4, 15.5, -2.6), "length": 15.0}, "saint_hinge": {"hinge": (4.6, 21.5, -2.4), "length": 34.0}}


def smooth(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


# ============================================================================================ textures (64 x 64 RGBA)
def tex_veil():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64
            if u < 0.5:
                t = u / 0.5
                c = np.array([196, 248, 252]) * (1 - t) + np.array([84, 212, 226]) * t
            else:
                t = (u - 0.5) / 0.5
                c = np.array([84, 212, 226]) * (1 - t) + np.array([20, 140, 170]) * t
            c = c / 255.0
            glow = math.exp(-((u - 0.2) / 0.12) ** 2)
            c = c * (1 - 0.35 * glow) + 0.35 * glow
            rim = math.exp(-(u / 0.045) ** 2)
            c = c * (1 - rim) + rim
            for vu in (0.30, 0.58):
                vein = math.exp(-((u - vu - 0.015 * math.sin(v * 14)) / 0.02) ** 2)
                c = c * (1 - 0.35 * vein) + 0.35 * vein
            a = 0.93 - 0.42 * smooth(0.35, 1.0, u) + 0.07 * rim + 0.05 * math.sin(v * 31 + u * 7)
            img[y, x, :3] = np.clip(c, 0, 1)
            img[y, x, 3] = min(1.0, max(0.05, a))
    return img


def petal_mask(u, v):
    if v < 0.34:
        hw = 0.5
    else:
        k = (v - 0.34) / 0.66
        hw = 0.5 * math.sqrt(max(0.0, 1 - k * k))
    return abs(u - 0.5) <= hw, hw


def tex_petal():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64
            inside, hw = petal_mask(u, v)
            if not inside:
                continue
            c = np.array([248, 255, 255]) * (1 - v) + np.array([112, 212, 238]) * v
            c = c / 255.0
            edge = min(hw - abs(u - 0.5), 1 - v) if v > 0.34 else min(0.5 - abs(u - 0.5), 1.0)
            if edge < 0.045:
                c = np.minimum(1.0, c * 0.7 + 0.35)
            if abs(u - 0.5) < 0.018 and v < 0.85:
                c = np.minimum(1.0, c * 0.92 + 0.12)
            if v > 0.15 and (abs((u - 0.5)) * 1.8 + 0.12 * v) % 0.18 < 0.02:
                c = c * 0.93
            img[y, x] = (*np.clip(c, 0, 1), 1.0)
    return img


def tex_water():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64
            a = 1 - abs(2 * u - 1) ** 2.2
            c = np.array([214, 252, 255]) * a + np.array([84, 196, 236]) * (1 - a)
            c = c / 255.0
            sp = math.sin(v * 40 + u * 9) * math.sin(v * 17 - u * 5)
            c = np.minimum(1.0, c + 0.12 * max(0, sp))
            img[y, x] = (*np.clip(c, 0, 1), min(1.0, 0.18 + 0.85 * a ** 0.8))
    return img


def tex_flame():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, 1 - (y + 0.5) / 64        # v = 0 at the base (bottom of the image) .. 1 at the tip
            hw = 0.5 * (1 - v) ** 0.75
            if abs(u - 0.5) > hw:
                continue
            c = np.array([245, 255, 255]) * (1 - v) + np.array([140, 222, 244]) * v
            img[y, x] = (*(c / 255.0), 1.0)
    return img


def tex_feather():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64             # v = 0 root .. 1 tip
            if v < 0.62:
                hw = 0.5 * (0.55 + 0.45 * smooth(0.0, 0.45, v))
            else:
                k = (v - 0.62) / 0.38
                hw = 0.5 * math.sqrt(max(0.0, 1 - k * k))
            if abs(u - 0.5) > hw:
                continue
            c = np.array([252, 253, 255]) * 1.0
            t = abs(u - 0.5) / max(hw, 1e-3)
            c = c * (1 - 0.22 * t ** 2) + np.array([150, 206, 240]) * 0.22 * t ** 2
            if abs(u - 0.5) < 0.016:
                c = np.array([255, 255, 255])
            barb = ((v * 1.0 + abs(u - 0.5) * 0.9) * 22) % 1.0
            if barb < 0.12 and abs(u - 0.5) > 0.03:
                c = c * 0.9
            if v < 0.12:
                c = c * 0.7 + np.array([236, 200, 104]) * 0.3                      # a hint of gold at the quill
            if hw - abs(u - 0.5) < 0.02:
                c = np.minimum(255, c * 0.8 + 60)
            img[y, x] = (*np.clip(c / 255.0, 0, 1), 1.0)
    return img


def tex_glow():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64
            f = math.exp(-(((u - 0.5) * 3.2) ** 2 + ((v - 0.5) * 1.4) ** 2) * 2.2)
            img[y, x] = (0.80, 1.0, 1.0, f)
    return img


def tex_book():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5) / 64, (y + 0.5) / 64
            e = min(u, 1 - u, v, 1 - v)
            soft = smooth(0.0, 0.12, e)
            c = np.array([246, 255, 255]) / 255.0
            if 0.14 < v < 0.86 and 0.1 < u < 0.9 and (int(v * 64) % 5 == 0):
                c = c * 0.93
            img[y, x] = (*c, 0.25 + 0.75 * soft)
    return img


def tex_halo():
    img = np.zeros((64, 64, 4), np.float32)
    for y in range(64):
        for x in range(64):
            u = (x + 0.5) / 64
            f = math.exp(-((u - 0.5) / 0.2) ** 2)
            img[y, x] = (1.0, 0.95, 0.74, f)
    return img


TEXTURES = {"veil": tex_veil, "petal": tex_petal, "water": tex_water, "flame": tex_flame, "feather": tex_feather, "glow": tex_glow, "book": tex_book, "halo": tex_halo}


def write_textures():
    os.makedirs(TEXD, exist_ok=True)
    for n, fn in TEXTURES.items():
        Image.fromarray((np.clip(fn(), 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(TEXD, "valk2_%s.png" % n))


# ============================================================================================ primitives
def card(m, layer, a, b, c, d, uv, rgb=W):
    """a textured quad a(top-left) b(top-right) c(bottom-right) d(bottom-left); uv = (u0, v0, u1, v1)."""
    u0, v0, u1, v1 = uv
    m.quad(layer, [a, b, c, d], [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], rgb)


def frill(m, y, hx, hz, n, L, f0, f1, wscale, rot=0.0, cx=0.0, cz=0.0, up=False, rgbs=(W, P), bulge=0.0, droop=0.0):
    """one tier of rounded petals round an elliptical ring at height y: each petal is a textured card of two quads (a top row hugging the surface at f0 degrees from vertical and a bottom row
    flared to f1), so a tier reads as a soft ruffle of overlapping frosty petals.  up=True makes the petals rise (a collar)."""
    pts = []
    for k in range(n):
        a = rot + 2 * math.pi * k / n
        pts.append((cx + hx * math.cos(a), cz + hz * math.sin(a), a))
    sgn = 1.0 if up else -1.0
    for k in range(n):
        x, z, a = pts[k]
        nx, nz, _ = pts[(k + 1) % n]
        wd = math.hypot(nx - x, nz - z) * wscale
        o = norm((math.cos(a) / hx, 0.0, math.sin(a) / hz))
        t = norm((-o[2], 0.0, o[0]))
        top = (x, y, z)
        r0, r1 = math.radians(f0), math.radians(f1)
        L0, L1 = L * 0.5, L * 0.5
        mid = (top[0] + o[0] * L0 * math.sin(r0) + o[0] * bulge, top[1] + sgn * (L0 * math.cos(r0)) - droop * 0.2, top[2] + o[2] * L0 * math.sin(r0) + o[2] * bulge)
        bot = (mid[0] + o[0] * L1 * math.sin(r1), mid[1] + sgn * (L1 * math.cos(r1)) - droop, mid[2] + o[2] * L1 * math.sin(r1))
        tint = rgbs[k % len(rgbs)]
        card(m, 3, add(top, mul(t, -wd * 0.45)), add(top, mul(t, wd * 0.45)), add(mid, mul(t, wd * 0.5)), add(mid, mul(t, -wd * 0.5)), (0, 0, 1, 0.5), tint)
        card(m, 3, add(mid, mul(t, -wd * 0.5)), add(mid, mul(t, wd * 0.5)), add(bot, mul(t, wd * 0.46)), add(bot, mul(t, -wd * 0.46)), (0, 0.5, 1, 1), tint)


# ============================================================================================ the soft veil wings
def veil_wing(name, s, H, L, th0, th1, lean, wmax, tilt, ncol, nrow, scallops, ripple, down=False, frill_sheet=True, veins=True, seed=0):
    """A soft draped translucent wing.  The leading edge is a smooth curve from the hinge H; the membrane hangs off it toward the body as a grid ncol x nrow (width: quick rise from the root, a long
    taper to a needle tip, scalloped trailing edge); a gentle static ripple; a second frill sheet over the outer half (offset in front of the first, its own scallop phase); a bright rim strip along
    the leading edge and two glowing veins (layer 5).  UV: u = 0 at the leading edge .. 1 at the trailing edge, v = 0 at the root .. 1 at the tip (the veil texture)."""
    m = Mesh(name)
    tp = math.radians(tilt)
    cols = []
    for i in range(ncol + 1):
        u = i / ncol
        p, d = lead_curve(s, H, L, th0, th1, lean, u)
        if down:
            d = (d[0], -d[1], d[2])
            p = (p[0], 2 * H[1] - p[1], p[2])
        n0 = (0.0, math.sin(tp), math.cos(tp))
        n = norm(sub(n0, mul(d, dot(n0, d))))
        a = norm(cross(n, d))
        if a[0] * s > 0:
            a = mul(a, -1)
        n = norm(cross(d, a)) if dot(cross(d, a), n0) > 0 else norm(cross(a, d))
        g = ((1 - u) ** 0.8) * (0.42 + 0.58 * smooth(0.0, 0.3, u))
        sc = 1.0 - 0.2 * smooth(0.2, 0.5, u) * (0.5 - 0.5 * math.cos(2 * math.pi * scallops * u))
        w = wmax * g * sc * (1.0 if u < 0.985 else 0.0)
        cols.append((u, p, d, a, n, w))

    def pos(i, r, off=0.0, phase=0.0):
        u, p, d, a, n, w = cols[i]
        rip = ripple * math.sin(2 * math.pi * 1.7 * u + 1.2 + phase) * (0.25 + 0.75 * r)
        return add(add(p, mul(a, w * r)), mul(n, rip + off))

    for i in range(ncol):
        for j in range(nrow):
            r0, r1 = j / nrow, (j + 1) / nrow
            u0, u1 = cols[i][0], cols[i + 1][0]
            m.quad(4, [pos(i, r0), pos(i + 1, r0), pos(i + 1, r1), pos(i, r1)], [(r0, u0), (r0, u1), (r1, u1), (r1, u0)], W)
    if frill_sheet:                                                            # a second, shorter layer over the outer half, scalloped out of phase
        r_in = 0.5
        for i in range(ncol):
            u0, u1 = cols[i][0], cols[i + 1][0]
            if u0 < 0.1 or u1 > 0.97:
                continue
            e0 = 1.0 + 0.07 * math.sin(2 * math.pi * (scallops + 1) * u0 + 1.6)
            e1 = 1.0 + 0.07 * math.sin(2 * math.pi * (scallops + 1) * u1 + 1.6)
            q0, q1 = [pos(i, r_in, 0.55, 0.9), pos(i + 1, r_in, 0.55, 0.9)], [pos(i, e0, 0.55, 0.9), pos(i + 1, e1, 0.55, 0.9)]
            m.quad(4, [q0[0], q0[1], q1[1], q1[0]], [(0.5, u0), (0.5, u1), (min(1.0, e1 * 0.98), u1), (min(1.0, e0 * 0.98), u0)], W)
    for i in range(ncol):                                                      # the rim: a thin bright strip along the leading edge
        u0, u1 = cols[i][0], cols[i + 1][0]
        a0, a1 = cols[i][3], cols[i + 1][3]
        m.quad(5, [add(cols[i][1], mul(a0, -0.25)), add(cols[i + 1][1], mul(a1, -0.25)), add(cols[i + 1][1], mul(a1, 0.8)), add(cols[i][1], mul(a0, 0.8))], [(0.5, 0.3), (0.5, 0.3), (0.5, 0.7), (0.5, 0.7)], RIMC)
    if veins:
        for r in (0.33, 0.62):
            for i in range(0, ncol - 2, 2):
                p0, p1 = pos(i, r, 0.12), pos(min(ncol, i + 2), r, 0.12)
                a0, a1 = cols[i][3], cols[min(ncol, i + 2)][3]
                m.quad(5, [sub(p0, mul(a0, 0.16)), add(p0, mul(a0, 0.16)), add(p1, mul(a1, 0.16)), sub(p1, mul(a1, 0.16))], [(0.5, 0.5)] * 4, VEIN)
    return m


def wing_main(lod):
    ncol, nrow, sc = {0: (14, 3, 4), 1: (8, 2, 3), 2: (5, 1, 2)}[lod]
    return veil_wing("valk2_wing_main" + ["", "_mid", "_lo"][lod], 1.0, BONES["wing_hinge"]["hinge"], 32.0, 26.0, 46.0, 0.07, 13.0, 12.0, ncol, nrow, sc, 0.65 if lod < 2 else 0.0,
                     frill_sheet=lod == 0, veins=lod < 2, seed=3)


def wing_low(lod):
    ncol, nrow, sc = {0: (8, 2, 2), 1: (5, 1, 2), 2: (3, 1, 1)}[lod]
    return veil_wing("valk2_wing_low" + ["", "_mid", "_lo"][lod], 1.0, BONES["wing_hinge_low"]["hinge"], 15.0, 14.0, 34.0, 0.12, 4.6, 8.0, ncol, nrow, sc, 0.4 if lod < 2 else 0.0,
                     frill_sheet=False, veins=False, seed=11, down=True)


def arm_veil(lod):
    """the sleeve veil: a draped sheet hanging off the outer-back edge of the left arm (pivot (6, 22, 0)), widening toward the wrist, scalloped hem, so with the arms raised it runs into the wing root."""
    m = Mesh("valk2_arm_veil" + ["", "_mid", "_lo"][lod])
    nc, nr = {0: (5, 3), 1: (3, 2), 2: (2, 1)}[lod]
    def pos(i, r):
        t = i / nc
        y = 24.6 - 13.2 * t
        wv = (1.2 + 5.2 * t) * (1.0 - 0.14 * (0.5 - 0.5 * math.cos(2 * math.pi * 3 * t)) * smooth(0.2, 0.6, t))
        x = 8.1 + r * wv
        z = -0.4 - r * wv * 0.5 + 0.35 * math.sin(2 * math.pi * 1.4 * t + 0.6) * r
        return (x, y, z)
    for i in range(nc):
        for j in range(nr):
            r0, r1 = j / nr, (j + 1) / nr
            m.quad(4, [pos(i, r0), pos(i + 1, r0), pos(i + 1, r1), pos(i, r1)], [(r0, i / nc), (r0, (i + 1) / nc), (r1, (i + 1) / nc), (r1, i / nc)], W)
    return m


def sleeve(lod):
    """the translucent aqua sleeve of the left arm (pivot (6, 22, 0)): a six-sided tube from the shoulder to the wrist, slightly flared, veil texture."""
    m = Mesh("valk2_sleeve" + ["", "_mid", "_lo"][lod])
    n = {0: 6, 1: 5, 2: 4}[lod]
    rows = {0: 2, 1: 1, 2: 1}[lod]
    ys = [23.6, 19.0, 14.6][: rows + 1] if rows == 2 else [23.6, 14.6]
    secs = [ring(y, 2.75 + 0.18 * i, 2.75 + 0.18 * i, n, cx=6.0, rot=math.pi / n) for i, y in enumerate(ys)]
    for i in range(len(secs) - 1):
        for j in range(n):
            k = (j + 1) % n
            m.quad(4, [secs[i][j], secs[i][k], secs[i + 1][k], secs[i + 1][j]], [(0.18, i / (len(secs) - 1)), (0.42, i / (len(secs) - 1)), (0.42, (i + 1) / (len(secs) - 1)), (0.18, (i + 1) / (len(secs) - 1))], W)
    return m


def crown(lod):
    """the tiara: a slim band round the brow and slender ice thorns (the middle ones tallest, 5 to 7 in front like the still), two feather ornaments over the ears."""
    m = Mesh("valk2_crown" + ["", "_mid", "_lo"][lod])
    yb = 30.7
    n = 8 if lod < 2 else 6
    loft(m, 0, [ring(yb - 0.7, 4.5, 4.5, n, rot=math.pi / n), ring(yb + 0.8, 4.3, 4.3, n, rot=math.pi / n)], A)
    hs = {0: [3.2, 5.0, 7.0, 9.5, 7.0, 5.0, 3.2], 1: [4.5, 7.5, 9.5, 7.5, 4.5], 2: [5.5, 9.5, 5.5]}[lod]
    k = len(hs)
    for i, h in enumerate(hs):
        a = math.pi / 2 - math.radians(-52 + 104 * i / (k - 1))
        c, s_ = math.cos(a), math.sin(a)
        base = (4.1 * c, yb + 0.6, 4.1 * s_)
        leaf(m, 0, base, norm((c * 0.18, 1.0, s_ * 0.18)), (c, 0, s_), h, 0.75, 0.55, W)
    if lod < 2:
        for sx in (1, -1):
            for j, (L, yy) in enumerate(((6.5, 27.8), (5.0, 29.0))):
                p0 = (sx * 4.5, yy, -0.8 - 0.7 * j)
                d = norm((sx * 0.5, 0.45, -0.75))
                nr = norm(cross(d, (0, 1, 0)))
                tip = add(p0, mul(d, L))
                card(m, 3, add(p0, mul(nr, -0.9)), add(p0, mul(nr, 0.9)), add(tip, mul(nr, 0.6)), add(tip, mul(nr, -0.6)), (0, 0, 1, 1), W)
    return m


def bodice(lod):
    m = Mesh("valk2_bodice" + ["", "_mid", "_lo"][lod])
    ys = [24.5, 20.5, 16.0, 12.7]
    hxz = [(4.55, 2.6), (4.8, 2.8), (4.3, 2.55), (4.4, 2.6)]
    n = 8 if lod == 0 else 6
    secs = [ring(y, hx, hz, n, rot=math.pi / n) for y, (hx, hz) in zip(ys, hxz)]
    if lod == 2:
        secs = [secs[0], secs[1], secs[3]]
    loft(m, 0, secs, A)
    loft(m, 1, [ring(13.7, 4.45, 2.65, n, rot=math.pi / n), ring(12.9, 4.45, 2.65, n, rot=math.pi / n)], G)
    tiers = {0: [(23.2, 4.95, 3.0, 9, 4.2, 8, 24), (19.9, 5.15, 3.2, 9, 4.8, 10, 30), (16.7, 4.9, 3.05, 9, 5.0, 12, 34)], 1: [(22.6, 5.0, 3.0, 7, 4.6, 10, 28), (18.4, 5.1, 3.15, 7, 5.6, 12, 34)], 2: [(19.5, 5.1, 3.15, 6, 6.5, 12, 34)]}[lod]
    for i, (y, hx, hz, nn, L, f0, f1) in enumerate(tiers):
        frill(m, y, hx, hz, nn, L, f0, f1, 1.45, rot=0.35 * i, rgbs=(W, P, W) if i % 2 == 0 else (P, W, A))
    if lod < 2:
        frill(m, 24.0, 3.1, 2.0, 6 if lod == 0 else 5, 3.2, 8, 30, 1.5, up=True, rgbs=(W, P))
    return m


def skirt(lod):
    m = Mesh("valk2_skirt" + ["", "_mid", "_lo"][lod])
    tiers = {0: [(13.3, 4.9, 3.1, 9, 5.8, 18, 42), (12.3, 5.3, 3.5, 10, 6.4, 28, 52), (11.4, 5.7, 3.9, 11, 5.8, 38, 62)], 1: [(13.0, 5.0, 3.2, 8, 6.2, 22, 46), (11.8, 5.6, 3.8, 9, 6.0, 36, 60)], 2: [(12.6, 5.3, 3.5, 7, 6.6, 30, 55)]}[lod]
    for i, (y, hx, hz, nn, L, f0, f1) in enumerate(tiers):
        frill(m, y, hx, hz, nn, L, f0, f1, 1.5, rot=0.3 * i, rgbs=(W, P, A) if i % 2 == 0 else (P, A, W))
    return m


def shoulder(lod):
    """the frilled shoulder cap of the left arm (pivot (6, 22, 0)): tiers of petals round the top of the arm, flared out like the layered sleeves of the still."""
    m = Mesh("valk2_shoulder" + ["", "_mid", "_lo"][lod])
    tiers = {0: [(24.4, 3.5, 3.3, 7, 4.0, 14, 40), (23.0, 3.9, 3.7, 8, 4.4, 26, 52)], 1: [(24.2, 3.7, 3.5, 6, 4.4, 20, 46)], 2: [(24.0, 3.7, 3.5, 5, 4.4, 24, 48)]}[lod]
    for i, (y, hx, hz, nn, L, f0, f1) in enumerate(tiers):
        frill(m, y, hx, hz, nn, L, f0, f1, 1.5, rot=0.4 * i, cx=6.0, rgbs=(W, P))
    return m


def cuff(lod):
    m = Mesh("valk2_cuff" + ["", "_mid", "_lo"][lod])
    nn = {0: 6, 1: 5, 2: 4}[lod]
    frill(m, 14.2, 2.7, 2.7, nn, 3.6, 16, 46, 1.5, cx=6.0, rgbs=(W, P))
    return m


def leg(lod):
    """the water column of the left leg (pivot (2, 12, 0)): spiral strands of water round the leg, a crown of flame fingers at the hip, foam fins at the foot."""
    m = Mesh("valk2_leg" + ["", "_mid", "_lo"][lod])
    strands, seg = {0: (3, 7), 1: (2, 5), 2: (2, 3)}[lod]
    cx = 2.0
    for k in range(strands):
        ph = 2 * math.pi * k / strands
        for i in range(seg):
            t0, t1 = i / seg, (i + 1) / seg
            def hp(t):
                a = ph + 2 * math.pi * 1.25 * t
                r = 3.1 + 0.45 * math.sin(t * 5 + k) + 0.35 * t
                return (cx + r * math.cos(a), 11.6 - 11.0 * t, r * math.sin(a) * 1.0)
            p0, p1 = hp(t0), hp(t1)
            up = (0, 1.15, 0)
            m.quad(4, [add(p0, up), add(p1, up), sub(p1, up), sub(p0, up)], [(0, 0), (0, 1), (1, 1), (1, 0)], W)
    ns = {0: 6, 1: 5, 2: 4}[lod]
    secs = [ring(11.8, 2.9, 2.9, ns, cx=cx, rot=math.pi / ns), ring(6.0, 3.15, 3.15, ns, cx=cx, rot=math.pi / ns), ring(0.6, 3.0, 3.0, ns, cx=cx, rot=math.pi / ns)]
    if lod > 0:
        secs = [secs[0], secs[2]]
    for i in range(len(secs) - 1):
        for j in range(ns):
            k = (j + 1) % ns
            t0, t1 = i / (len(secs) - 1), (i + 1) / (len(secs) - 1)
            m.quad(4, [secs[i][j], secs[i][k], secs[i + 1][k], secs[i + 1][j]], [(0.12, t0), (0.2, t0), (0.2, t1), (0.12, t1)], P)
    nf = {0: 6, 1: 4, 2: 0}[lod]
    for k in range(nf):
        a = 2 * math.pi * (k + 0.5) / nf
        c, s_ = math.cos(a), math.sin(a)
        base = (cx + 2.7 * c, 11.0, 2.7 * s_)
        tip = (cx + 3.5 * c, 16.2 + 1.2 * (k % 2), 3.6 * s_)
        t = norm((-s_, 0, c))
        card(m, 3, add(base, mul(t, -1.2)), add(base, mul(t, 1.2)), add(tip, mul(t, 0.05)), add(tip, mul(t, -0.05)), (0, 0, 1, 1), W)      # flame finger (cut-out card, flame texture)
    return m


def grimoire(lod):
    """Noelle's grimoire floating open at the left hip: two bright pages in a shallow V, a dim cover behind, a soft glow plate."""
    m = Mesh("valk2_grimoire" + ["", "_mid", "_lo"][lod])
    c = (9.0, 11.2, 3.6)
    ax = norm((0.62, 0.0, 0.78))
    ay = norm((-0.28, 0.93, 0.24))
    az = norm(cross(ax, ay))
    p00 = add(add(c, mul(ax, -5.0)), mul(ay, -4.2))
    pm0 = add(add(c, mul(az, -0.6)), mul(ay, -4.2))
    p10 = add(add(c, mul(ax, 5.0)), mul(ay, -4.2))
    p01, pm1, p11 = add(p00, mul(ay, 8.4)), add(pm0, mul(ay, 8.4)), add(p10, mul(ay, 8.4))
    m.quad(4, [p01, pm1, pm0, p00], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    m.quad(4, [pm1, p11, p10, pm0], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    big = 1.5
    m.quad(5, [add(p01, add(mul(ax, -big), mul(ay, big))), add(p11, add(mul(ax, big), mul(ay, big))), add(p10, add(mul(ax, big), mul(ay, -big))), add(p00, add(mul(ax, -big), mul(ay, -big)))], [(0, 0), (1, 0), (1, 1), (0, 1)], G)
    if lod == 0:
        slab(m, 0, add(c, mul(az, -1.0)), ax, ay, 5.2, 4.5, 0.25, D)
    return m


# ---- the Saint Valkyrie Dress
def feather_wing(name, s, H, bone, feathers, lod_scale=1.0):
    """a bird wing of feather cards: an arm bone shoulder -> elbow -> wrist and feathers (each two quads, textured, rounded tip) hung on it: primaries fanned from the wrist, secondaries along the forearm,
    covert rows over the bone.  feathers = [(t along bone 0..2, angle degrees from the bone direction, length, width), ...]"""
    m = Mesh(name)
    th = [math.radians(a) for a in (62, 40)]
    sh = H
    el = add(sh, (s * math.sin(th[0]) * 13, math.cos(th[0]) * 13, -1.6))
    wr = add(el, (s * math.sin(th[1]) * 11, math.cos(th[1]) * 11, -1.2))
    def on(t):
        if t <= 1:
            return add(sh, mul(sub(el, sh), t)), norm(sub(el, sh))
        return add(el, mul(sub(wr, el), t - 1)), norm(sub(wr, el))
    for k, (t, ang, ln, wd) in enumerate(feathers):
        p, d = on(t)
        # rotate d about the z axis by -ang*s (clockwise for the +x wing) so the feathers trail on the side of the body
        rot = math.radians(-ang * s)
        dd = norm((d[0] * math.cos(rot) - d[1] * math.sin(rot), d[0] * math.sin(rot) + d[1] * math.cos(rot), d[2] - 0.05 - 0.002 * ln))
        nr = norm(cross(dd, (0, 0, 1)))
        mid = add(p, mul(dd, ln * 0.5))
        tip = add(mid, mul(norm(add(dd, (0, -0.18, -0.04))), ln * 0.5))
        card(m, 3, add(p, mul(nr, -wd * 0.4)), add(p, mul(nr, wd * 0.4)), add(mid, mul(nr, wd * 0.5)), add(mid, mul(nr, -wd * 0.5)), (0, 0, 1, 0.5), W if k % 2 == 0 else P)
        card(m, 3, add(mid, mul(nr, -wd * 0.5)), add(mid, mul(nr, wd * 0.5)), add(tip, mul(nr, wd * 0.45)), add(tip, mul(nr, -wd * 0.45)), (0, 0.5, 1, 1), W if k % 2 == 0 else P)
    # the bone: a thin strip so the wing has a spine (flat plate)
    for a_, b_ in ((sh, el), (el, wr)):
        m.quad(0, [add(a_, (0, 0, 0.5)), add(b_, (0, 0, 0.5)), add(b_, (0, 0, -0.5)), add(a_, (0, 0, -0.5))], [(0, 0)] * 4, A)
    return m


def saint_wing(lod):
    prim = [(1.0 + 0.1 * i, 14 + 4.5 * i, 31 - 1.6 * i, 5.2) for i in range(9)]                       # fanned from the wrist, the first the most upright
    prim = [(1.0 + 0.12 * i, 8 + 7.5 * i, 26 - 1.4 * i, 5.0) for i in range(9)]
    sec = [(0.3 + 0.11 * i, 76 + 3 * i, 18 - 0.8 * i, 4.6) for i in range(7)]
    cov = [(0.15 + 0.2 * i, 84, 9.0, 4.2) for i in range(8)]
    if lod == 1:
        prim, sec, cov = prim[::2] + [prim[-1]], sec[::2], cov[::2]
    if lod == 2:
        prim, sec, cov = prim[::3] + [prim[-1]], sec[::3], []
    return feather_wing("valk2_saint_wing" + ["", "_mid", "_lo"][lod], 1.0, BONES["saint_hinge"]["hinge"], "saint_hinge", prim + sec + cov)


def saint_halo(lod):
    m = Mesh("valk2_saint_halo" + ["", "_mid", "_lo"][lod])
    n = {0: 16, 1: 10, 2: 6}[lod]
    R, y = 6.6, 35.5
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        def pt(a, k):
            r = R + k * 0.55
            return (r * math.cos(a), y + 1.0 * math.sin(a) * 0.0, r * math.sin(a) * 0.95 - 0.8)
        m.quad(5, [pt(a0, -1), pt(a1, -1), pt(a1, 1), pt(a0, 1)], [(0, 0), (0, 1), (1, 1), (1, 0)], W)
    return m


def saint_crown(lod):
    """the winged helm of the Saint dress: a small crown, and two swept-back feather blades on each side of the head."""
    m = Mesh("valk2_saint_crown" + ["", "_mid", "_lo"][lod])
    yb = 30.7
    n = 8 if lod < 2 else 6
    loft(m, 0, [ring(yb - 0.7, 4.5, 4.5, n, rot=math.pi / n), ring(yb + 0.8, 4.3, 4.3, n, rot=math.pi / n)], P)
    for i, h in enumerate([2.6, 4.2, 5.4, 4.2, 2.6] if lod == 0 else [4.0, 5.4, 4.0]):
        k = 5 if lod == 0 else 3
        a = math.pi / 2 - math.radians(-40 + 80 * i / (k - 1))
        c, s_ = math.cos(a), math.sin(a)
        leaf(m, 0, (4.1 * c, yb + 0.6, 4.1 * s_), norm((c * 0.15, 1.0, s_ * 0.15)), (c, 0, s_), h, 0.5, 0.4, W)
    if lod < 2:
        for sx in (1, -1):
            for j, (L, yy, wd) in enumerate(((10.0, 31.0, 1.8), (8.0, 29.4, 1.6), (6.0, 27.8, 1.4))):
                p0 = (sx * 4.6, yy, 0.4 - 1.1 * j)
                d = norm((sx * 0.62, 0.38, -0.68))
                nr = norm(cross(d, (0, 1, 0)))
                tip = add(p0, mul(d, L))
                card(m, 3, add(p0, mul(nr, -wd * 0.5)), add(p0, mul(nr, wd * 0.5)), add(tip, mul(nr, wd * 0.4)), add(tip, mul(nr, -wd * 0.4)), (0, 0, 1, 1), W)
    return m


def saint_sash(lod):
    m = Mesh("valk2_saint_sash" + ["", "_mid", "_lo"][lod])
    n = 8 if lod == 0 else 6
    loft(m, 0, [ring(17.4, 4.95, 2.95, n, rot=math.pi / n), ring(15.6, 4.95, 2.95, n, rot=math.pi / n)], GOLD)
    if lod < 2:
        slab(m, 0, (0.0, 16.5, 3.2), (1, 0, 0), (0, 1, 0), 1.0, 1.0, 0.35, GOLD)
    return m


# ============================================================================================ build
# name, builder, bone, copies, tex per layer, looks
PIECES = [
    ("wing_main", wing_main, "wing_hinge", 2, {4: "veil", 5: "glow"}, ("VALKYRIE",)),
    ("wing_low", wing_low, "wing_hinge_low", 2, {4: "veil", 5: "glow"}, ("VALKYRIE",)),
    ("arm_veil", arm_veil, "arm", 2, {4: "veil"}, ("VALKYRIE",)),
    ("sleeve", sleeve, "arm", 2, {4: "veil"}, ("VALKYRIE", "SAINT")),
    ("crown", crown, "head", 1, {3: "petal"}, ("VALKYRIE",)),
    ("bodice", bodice, "body", 1, {3: "petal"}, ("VALKYRIE", "SAINT")),
    ("skirt", skirt, "body", 1, {3: "petal"}, ("VALKYRIE", "SAINT")),
    ("shoulder", shoulder, "arm", 2, {3: "petal"}, ("VALKYRIE", "SAINT")),
    ("cuff", cuff, "arm", 2, {3: "petal"}, ("VALKYRIE", "SAINT")),
    ("leg", leg, "leg", 2, {3: "flame", 4: "water"}, ("VALKYRIE", "SAINT")),
    ("grimoire", grimoire, "body", 1, {4: "book", 5: "glow"}, ("VALKYRIE", "SAINT")),
    ("saint_wing", saint_wing, "saint_hinge", 2, {3: "feather"}, ("SAINT",)),
    ("saint_halo", saint_halo, "head", 1, {5: "halo"}, ("SAINT",)),
    ("saint_crown", saint_crown, "head", 1, {3: "feather"}, ("SAINT",)),
    ("saint_sash", saint_sash, "body", 1, {}, ("SAINT",)),
]
NAMES = ["", "_mid", "_lo"]
LV = ["near", "mid", "far"]


def build_all():
    out = {}
    for name, fn, *_ in PIECES:
        for lod in range(3):
            out[(name, lod)] = fn(lod)
    return out


def totals(ms, look):
    res = []
    for lod in range(3):
        n = 0
        for name, fn, bone, copies, tx, looks in PIECES:
            if look in looks:
                n += len(ms[(name, lod)].q) * copies
        res.append(n)
    return res


def main():
    os.makedirs(OUT, exist_ok=True)
    write_textures()
    ms = build_all()
    lines = ["Valkyrie armour v2 (preview only): quads per piece and level (LEFT copy written; x copies = what the layer draws); layers 0 plate, 1 emissive, 3 card, 4 veil, 5 glow", "",
             "%-12s %-5s %6s %5s %4s %5s %5s %5s %8s  looks" % ("piece", "level", "quads", "L0", "L1", "L3", "L4", "L5", "x copies")]
    man = {"units": "px (1/16 block), vanilla player REST pose, y up, feet y=0, front +z", "bones": BONES, "pieces": []}
    for name, fn, bone, copies, tx, looks in PIECES:
        ent = {"name": name, "bone": bone, "copies": copies, "tex": {str(k): v for k, v in tx.items()}, "looks": list(looks), "levels": {}}
        for lod in range(3):
            m = ms[(name, lod)]
            c = {k: 0 for k in range(6)}
            for qd in m.q:
                c[qd[0]] += 1
            fn_ = "valk2_%s%s.txt" % (name, NAMES[lod])
            with open(os.path.join(OUT, fn_), "w", encoding="utf-8", newline="\n") as f:
                f.write(m.text())
            lines.append("%-12s %-5s %6d %5d %4d %5d %5d %5d %8d  %s" % (name, LV[lod], len(m.q), c[0], c[1], c[3], c[4], c[5], len(m.q) * copies, "/".join(looks)))
            ent["levels"][LV[lod]] = {"file": fn_, "quads": len(m.q)}
        man["pieces"].append(ent)
    lines += ["", "TOTAL quads per wearer (both copies), by look"]
    man["totals"] = {}
    for look in ("VALKYRIE", "SAINT"):
        t = totals(ms, look)
        man["totals"][look] = {LV[i]: {"quads": t[i], "vertices": t[i] * 4} for i in range(3)}
        lines.append("%-9s near %4d (%5d verts)  mid %4d (%5d)  far %4d (%5d)" % (look, t[0], t[0] * 4, t[1], t[1] * 4, t[2], t[2] * 4))
    with open(os.path.join(OUT, "counts_v2.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(BASE, "pieces_v2.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(man, f, indent=1)
    print("\n".join(lines))


if __name__ == "__main__":
    main()

"""vupreview: software preview (no game, no GPU) of the relic models, old and visual upgrade, with the passes the game draws.

An orthographic z-buffered rasteriser on top of tools/relics/preview_worn.py (the figure, the frames, the triangle filler). For a model with a `skin` record it draws

    pass 1  the skin (lit)             pass 2  the glow map (additive)        pass 3  the nebula of the STAR quads (additive, scrolled by the clock)
    pass 4  the HALO quads over the shared fx sheet (additive, tinted by the colour slot of the quad)

and for a library model the shared atlas tinted by the palette (emissive quads added on top), exactly the two paths of client/relics/worn/lib/WornLibDraw. Animation groups
run at the clock `t` (ticks) with the same maths as WornAnim. Not a pixel-exact render: no perspective, no light map, no bloom.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import metrics as MT  # noqa: E402
RELICS = os.path.abspath(os.path.join(HERE, "..", "..", "relics"))
sys.path.insert(0, RELICS)
import preview_worn as PW  # noqa: E402

TEXROOT = os.path.join(MT.ASSETS, "textures")
ATLAS = np.asarray(Image.open(PW.ATLAS).convert("RGBA"), np.float32) / 255
_CACHE = {}
DEG = math.pi / 180.0
EMIT, SHELL, HOT, GLOWMAP, STAR, HALO = 1, 2, 4, 8, 16, 32


def tex(path, premultiply=False):
    key = (path, premultiply)
    if key not in _CACHE:
        im = np.asarray(Image.open(os.path.join(TEXROOT, path)).convert("RGBA"), np.float32) / 255
        if premultiply:
            im = np.concatenate([im[..., :3] * im[..., 3:4], im[..., 3:4]], axis=2)
        _CACHE[key] = im
    return _CACHE[key]


def parse_model(path):
    """(anchor, header dict, groups {idx: (name, kind, px,py,pz,a,b,c)}, quads [(g, slot, flags, P, UV, N)])."""
    anchor, head, groups, quads = "body", {}, {}, []
    for line in open(path):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "anchor":
            anchor = v[1]
        elif v[0] in ("skin", "glowtex", "startex", "lod"):
            head[v[0]] = v[1]
        elif v[0] == "grp":
            groups[int(v[1])] = (v[2], v[3], tuple(float(x) for x in v[4:10]))
        elif v[0] == "q":
            g, slot, flags = int(v[1]), int(v[2]), int(v[3])
            P, UV, N = [], [], []
            for k in range(4):
                o = 4 + k * 8
                P.append([float(x) for x in v[o:o + 3]])
                UV.append([float(x) for x in v[o + 3:o + 5]])
                N.append([float(x) for x in v[o + 5:o + 8]])
            quads.append((g, slot, flags, np.array(P), np.array(UV), np.array(N)))
    return anchor, head, groups, quads


def rx_(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry_(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz_(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def group_xf(g, t, limb=0.0):
    """(R 3x3, scale, translation) of a group at the clock t, as WornAnim.eval."""
    if g is None:
        return np.eye(3), 1.0, np.zeros(3), None
    name, kind, (px, py, pz, a, b, c) = g
    R, s, tr = np.eye(3), 1.0, np.zeros(3)
    if kind == "bob":
        tr = np.array([0, a * math.sin(t * b + c), 0])
    elif kind in ("spin", "orbit"):
        R = ry_(t * a * DEG)
    elif kind == "swing":
        R = rx_((a * limb + b * math.sin(t * 0.09)) * DEG)
    elif kind == "sway":
        R = rz_(a * math.sin(t * b + c) * DEG)
    elif kind == "pulse":
        s = 1 + a * math.sin(t * b + c)
    elif kind == "tumble":
        R = rx_(t * a * DEG) @ ry_(t * c * DEG) @ rz_(t * b * DEG)
    return R, s, tr, np.array([px, py, pz])


def anim_quads(quads, groups, t):
    """The quads with their group motion applied: [(slot, flags, P, UV, N)]."""
    out = []
    cache = {}
    for (g, slot, flags, P, UV, N) in quads:
        if g not in cache:
            cache[g] = group_xf(groups.get(g), t)
        R, s, tr, pv = cache[g]
        if pv is not None:
            P = (P - pv) * s @ R.T + pv + tr
            N = N @ R.T
        out.append((slot, flags, P, UV, N))
    return out


def render(pieces, palette, yaw, pitch, crop, scale, size, t=0.0, figure=True, passes=("skin", "glow", "star", "halo"), bg=0.0):
    """pieces = [(path, anchor or None, mirror)]. Returns float RGB (h, w, 3)."""
    x0, x1, y0, y1 = crop
    W, H = size
    R = PW.rot(yaw, pitch)
    cxm, cym = (x0 + x1) / 2, (y0 + y1) / 2
    img = np.zeros((H, W, 3), np.float32) + np.array([0.20, 0.21, 0.27], np.float32) * (1.0 - bg)
    img *= (0.8 + 0.4 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    zb = np.full((H, W), 1e9, np.float32)

    def proj(Pm):
        Q = Pm @ R.T
        return np.stack([W / 2 + (Q[:, 0] - cxm) * scale, H / 2 + (Q[:, 1] - cym) * scale, Q[:, 2] * scale], axis=1)

    def draw(Pm, UV, N, texture, tint, mode, bias=0.0):
        S = proj(Pm)
        Nv = N @ R.T
        if mode == "add" and float(Nv[:, 2].mean()) > 0.05:                  # RenderType.eyes culls back faces
            return
        for tri in ((0, 1, 2), (0, 2, 3)):
            ii = list(tri)
            PW._raster(img, zb, S[ii], UV[ii], Nv[ii], texture, tint, mode, bias)

    if figure:
        for b in PW.figure_boxes():
            for (Pq, n) in PW.box_quads(b[:6]):
                draw(Pq, np.zeros((4, 2)), np.tile(n, (4, 1)), None, np.array(b[6]), "solid")
    cols = PW.slot_colors(*palette)
    adds = []
    for (path, anchor, mirror) in pieces:
        a, head, groups, quads = parse_model(path)
        a = anchor or a
        off = np.array(PW.ANCHORS[a], float)
        aq = anim_quads(quads, groups, t)
        custom = "skin" in head
        skin = tex(head["skin"]) if custom else ATLAS
        for (slot, flags, P, UV, N) in aq:
            Pm, Nm = P.copy(), N.copy()
            if mirror:
                Pm[:, 0] *= -1
                Nm[:, 0] *= -1
            Pm = Pm + off
            if custom:
                if flags & (STAR | HALO):
                    adds.append((Pm, UV, Nm, flags, slot, head))
                    continue
                if "skin" in passes:
                    draw(Pm, UV, Nm, skin, np.ones(3, np.float32), "solid")
                if flags & GLOWMAP:
                    adds.append((Pm, UV, Nm, flags, slot, head))
            else:
                draw(Pm, UV, Nm, skin, cols[slot], "solid")
                if flags & EMIT:
                    adds.append((Pm, UV, Nm, flags, slot, head))
    for (Pm, UV, Nm, flags, slot, head) in adds:
        if "skin" not in head:
            draw(Pm, UV, Nm, ATLAS, cols[slot] * 0.9, "add", bias=0.5)
            continue
        if flags & HALO:
            if "halo" in passes:
                draw(Pm, UV, Nm, tex("visual_upgrade/vu_fx.png", True), cols[slot], "add", bias=0.6)
        elif flags & STAR:
            if "star" in passes and "startex" in head:
                UVs = UV + np.array([t * 0.0021, t * 0.0013])
                draw(Pm, UVs % 1.0, Nm, tex(head["startex"], True), np.ones(3, np.float32), "add", bias=0.6)
        elif flags & GLOWMAP:
            if "glow" in passes and "glowtex" in head:
                draw(Pm, UV, Nm, tex(head["glowtex"], True), np.ones(3, np.float32) * 0.95, "add", bias=0.4)
    return np.clip(img, 0, 1)


def tile(pieces, palette, view, zoom, panel, t=0.0, figure=True, passes=("skin", "glow", "star", "halo"), ss=2):
    yaw, pitch = view
    crop = zoom
    w, h = crop[1] - crop[0], crop[3] - crop[2]
    scale = min((panel[0] - 6) / w, (panel[1] - 6) / h)
    im = render(pieces, palette, yaw, pitch, crop, scale * ss, (panel[0] * ss, panel[1] * ss), t, figure, passes)
    return im.reshape(panel[1], ss, panel[0], ss, 3).mean(axis=(1, 3))


def sheet(rows, out, panel=(300, 300), title=None):
    """rows = [[(label, ndarray tile), ...], ...] -> one PNG."""
    ncol = max(len(r) for r in rows)
    H = len(rows) * (panel[1] + 14) + (18 if title else 0)
    img = Image.new("RGB", (ncol * panel[0], H), (10, 10, 14))
    d = ImageDraw.Draw(img)
    y = 0
    if title:
        d.text((6, 3), title, fill=(255, 255, 255))
        y = 18
    for r in rows:
        for i, (label, t) in enumerate(r):
            img.paste(Image.fromarray((np.clip(t, 0, 1) * 255).astype(np.uint8)), (i * panel[0], y + 12))
            d.text((i * panel[0] + 4, y), label, fill=(220, 220, 230))
        y += panel[1] + 14
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    img.save(out)
    return out

"""Software preview of the worn relic models beside a Steve-like figure (no game, no GPU).

    python3 tools/relics/preview_worn.py <model[@anchor][~] ...> [--palette main,trim,glow] [--out sheet.png] [--views front,three,side,back] [--zoom head|body|arm|leg|all]
    python3 tools/relics/preview_worn.py --relic tiny_duck_crown            (the pieces and palette come from the catalog and the survey)
    python3 tools/relics/preview_worn.py --sheet                              (a contact sheet of every look model with its default palette)

An orthographic z-buffered rasteriser with the game's rules for these models: the file's layer space (y down, front is -z), the atlas tinted by the
colour slots, one directional light on the smoothed normals, and the emissive quads drawn additive on top (what RenderType.eyes does). The figure is
plain boxes with a face so the proportions read; animation groups are shown at rest.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
WORN = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "relic_worn")
ATLAS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "relic_worn", "atlas.png")

ANCHORS = {"head": (0, 0, 0), "body": (0, 0, 0), "rarm": (-5, 2, 0), "larm": (5, 2, 0), "rleg": (-1.9, 12, 0), "lleg": (1.9, 12, 0)}
VIEWS = {"front": (0, 0), "three": (35, 12), "side": (90, 0), "back": (180, 0), "top": (0, 70)}
ZOOMS = {"head": (-9.5, 9.5, -17, 6), "body": (-11, 11, -3, 24), "arm": (-14, 6, -2, 20), "leg": (-9, 9, 6, 28), "all": (-13, 13, -17, 30), "wide": (-15, 15, -20, 30)}


def parse(path):
    anchor, quads, groups = "head", [], {}
    for line in open(path):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "anchor":
            anchor = v[1]
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
    return anchor, quads, groups


def rgb(h):
    return np.array([(h >> 16) & 255, (h >> 8) & 255, h & 255], np.float64) / 255.0


def slot_colors(main, trim, glow):
    m, t, g = rgb(main), rgb(trim), rgb(glow)
    return [m, t, g, m * 0.38, (t + g) / 2, np.ones(3)]


def rot(yaw, pitch):
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return Rx @ Ry


LIGHT = np.array([-0.35, -0.8, -0.5])
LIGHT = LIGHT / np.linalg.norm(LIGHT)
LIGHT2 = np.array([0.6, -0.2, 0.4])
LIGHT2 = LIGHT2 / np.linalg.norm(LIGHT2)


def shade(n):
    return 0.40 + 0.55 * np.clip(n @ LIGHT, 0, 1) + 0.15 * np.clip(n @ LIGHT2, 0, 1)


def figure_boxes():
    """(x0,y0,z0,x1,y1,z1, rgb) of the Steve-like figure in layer space."""
    skin, shirt, pants, shoe, hair = (0.80, 0.60, 0.46), (0.22, 0.52, 0.55), (0.25, 0.26, 0.55), (0.30, 0.30, 0.32), (0.25, 0.17, 0.10)
    return [(-4, -8, -4, 4, 0, 4, skin, "head"), (-4, 0, -2, 4, 12, 2, shirt, ""), (-8, 0, -2, -4, 12, 2, skin, ""), (4, 0, -2, 8, 12, 2, skin, ""),
            (-3.9, 12, -2, 0, 24, 2, pants, ""), (0, 12, -2, 3.9, 24, 2, pants, ""), (-3.9, 22, -2, 0, 24, 2, shoe, ""), (0, 22, -2, 3.9, 24, 2, shoe, ""),
            (-4.05, -8.05, -4.05, 4.05, -6.0, 4.05, hair, "")]


def box_quads(b):
    x0, y0, z0, x1, y1, z1 = b
    faces = [((x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (0, 0, -1)), ((x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1), (0, 0, 1)),
             ((x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (-1, 0, 0)), ((x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0), (1, 0, 0)),
             ((x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0), (0, -1, 0)), ((x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (0, 1, 0))]
    return [(np.array(f[:4], float), np.array(f[4], float)) for f in faces]


def _raster(img, zb, P, UV, N, tex, tint, mode, bias):
    H, W = zb.shape
    x0, x1 = int(max(0, math.floor(P[:, 0].min()))), int(min(W - 1, math.ceil(P[:, 0].max())))
    y0, y1 = int(max(0, math.floor(P[:, 1].min()))), int(min(H - 1, math.ceil(P[:, 1].max())))
    if x0 > x1 or y0 > y1:
        return
    yy, xx = np.mgrid[y0:y1 + 1, x0:x1 + 1].astype(np.float32) + 0.5
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = P
    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
    if abs(den) < 1e-9:
        return
    w0 = ((by - cy) * (xx - cx) + (cx - bx) * (yy - cy)) / den
    w1 = ((cy - ay) * (xx - cx) + (ax - cx) * (yy - cy)) / den
    w2 = 1 - w0 - w1
    inside = (w0 >= -1e-3) & (w1 >= -1e-3) & (w2 >= -1e-3)
    if not inside.any():
        return
    z = w0 * az + w1 * bz + w2 * cz
    sub, reg = zb[y0:y1 + 1, x0:x1 + 1], img[y0:y1 + 1, x0:x1 + 1]
    m = inside & (z < sub + bias if mode == "add" else z < sub)
    if not m.any():
        return
    if tex is not None:
        u = (w0 * UV[0, 0] + w1 * UV[1, 0] + w2 * UV[2, 0]) * tex.shape[1]
        v = (w0 * UV[0, 1] + w1 * UV[1, 1] + w2 * UV[2, 1]) * tex.shape[0]
        s = tex[np.clip(v.astype(int), 0, tex.shape[0] - 1), np.clip(u.astype(int), 0, tex.shape[1] - 1)][..., :3]
    else:
        s = np.ones(m.shape + (3,), np.float32)
    if mode == "solid":
        n = w0[..., None] * N[0] + w1[..., None] * N[1] + w2[..., None] * N[2]
        n = n / np.maximum(np.linalg.norm(n, axis=-1, keepdims=True), 1e-6)
        sh = shade(n)[..., None]
        col = s * tint * sh
        reg[m] = col[m]
        sub[m] = z[m]
    else:
        reg[m] += (s * tint)[m]


def render(pieces, palette, yaw, pitch, crop, scale, size, atlas, figure=True, t=0.0):
    """pieces = [(file, anchor, mirror)]. Returns float RGB (h, w, 3)."""
    x0, x1, y0, y1 = crop
    W, H = size
    R = rot(yaw, pitch)
    cxm, cym = (x0 + x1) / 2, (y0 + y1) / 2
    img = np.zeros((H, W, 3), np.float32) + np.array([0.33, 0.36, 0.43], np.float32)
    img *= (0.85 + 0.3 * np.linspace(1, 0, H, dtype=np.float32))[:, None, None]
    zb = np.full((H, W), 1e9, np.float32)

    def proj(Pm):
        Q = Pm @ R.T
        return np.stack([W / 2 + (Q[:, 0] - cxm) * scale, H / 2 + (Q[:, 1] - cym) * scale, Q[:, 2] * scale], axis=1), Q

    def tri_draw(Pm, UV, N, tex, tint, mode, cull=True, bias=0.0):
        S, Q = proj(Pm)
        Nv = N @ R.T
        for tri in ((0, 1, 2), (0, 2, 3)):
            ii = list(tri)
            _raster(img, zb, S[ii], UV[ii], Nv[ii], tex, tint, mode, bias)

    if figure:
        for b in figure_boxes():
            for (Pq, n) in box_quads(b[:6]):
                tri_draw(Pq, np.zeros((4, 2)), np.tile(n, (4, 1)), None, np.array(b[6]), "solid")
        # a face on the front of the head
        for (ex, col) in ((-2.0, (0.1, 0.35, 0.4)), (1.0, (0.1, 0.35, 0.4))):
            Pq = np.array([[ex, -5, -4.06], [ex + 1.0, -5, -4.06], [ex + 1.0, -4, -4.06], [ex, -4, -4.06]])
            tri_draw(Pq, np.zeros((4, 2)), np.tile([0, 0, -1.0], (4, 1)), None, np.array(col), "solid")
    cols = slot_colors(*palette)
    emit = []
    for (fname, anchor, mirror) in pieces:
        a, quads, groups = parse(fname)
        a = anchor or a
        off = np.array(ANCHORS[a], float)
        for (g, slot, flags, P, UV, N) in quads:
            Pm, Nm = P.copy(), N.copy()
            if mirror:
                Pm[:, 0] *= -1
                Nm[:, 0] *= -1
            Pm = Pm + off
            tint = cols[slot]
            tri_draw(Pm, UV, Nm, atlas, tint, "solid")
            if flags & 1:
                emit.append((Pm, UV, Nm, tint))
    for (Pm, UV, Nm, tint) in emit:
        tri_draw(Pm, UV, Nm, atlas, tint * 0.9, "add", bias=0.5)
    img = np.clip(img, 0, 1)
    return img


def sheet_for(pieces, palette, views, zoom, panel=(300, 330), label="", figure=True):
    atlas = np.asarray(Image.open(ATLAS).convert("RGBA"), np.float32) / 255
    crop = ZOOMS[zoom]
    w, h = crop[1] - crop[0], crop[3] - crop[2]
    scale = min((panel[0] - 8) / w, (panel[1] - 20) / h)
    tiles = []
    for v in views:
        yaw, pitch = VIEWS[v]
        ss = 2
        im = render(pieces, palette, yaw, pitch, crop, scale * ss, (panel[0] * ss, panel[1] * ss), atlas, figure)
        im = im.reshape(panel[1], ss, panel[0], ss, 3).mean(axis=(1, 3))
        tiles.append(im)
    row = np.concatenate(tiles, axis=1)
    out = Image.fromarray((row * 255).astype(np.uint8))
    d = ImageDraw.Draw(out)
    d.text((6, 4), label, fill=(255, 255, 255))
    return out


def parse_pieces(specs):
    out = []
    for s in specs:
        mirror = s.endswith("~")
        s = s.rstrip("~")
        name, _, anchor = s.partition("@")
        out.append((os.path.join(WORN, name + ".txt"), anchor or None, mirror))
    return out


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("pieces", nargs="*")
    ap.add_argument("--palette", default="8C8C9C,D8C070,FFE9A0")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "relics_preview", "worn.png"))
    ap.add_argument("--views", default="front,three,side")
    ap.add_argument("--zoom", default="head")
    ap.add_argument("--nofigure", action="store_true")
    a = ap.parse_args(argv)
    pal = tuple(int(x, 16) for x in a.palette.split(","))
    im = sheet_for(parse_pieces(a.pieces), pal, a.views.split(","), a.zoom, label=" ".join(a.pieces), figure=not a.nofigure)
    os.makedirs(os.path.dirname(os.path.abspath(a.out)), exist_ok=True)
    im.save(a.out)
    print("wrote", a.out)


if __name__ == "__main__":
    main(sys.argv[1:])

#!/usr/bin/env python3
"""A tiny software renderer for the preview PNGs of docs/body_parts_pass.md (no game, no GPU): reads an entity mesh (entity_meshes/<id>.txt, the EntityMeshData format) and its skin,
puts a player-shaped proxy of boxes next to it and draws side / back / three-quarter views with a z-buffer and a headlight. Used by tools/gen_saiyan_monkey_tail.py's preview step and by
tools/audit/preview_body_parts.py.

  python3 tools/audit/mesh_preview.py <mesh id> <out.png> [--skin <skin id>]
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")


def read_mesh(eid):
    """-> list of (verts[4x3], normals[4x3], uvs[4x2]) in pixels (a part's rotations are not applied: the rest pose)"""
    quads = []
    with open(os.path.join(RES, "entity_meshes", eid + ".txt")) as fh:
        for line in fh:
            v = line.split()
            if not v or v[0] != "qn" or v[1] != "0":
                continue
            f = [float(x) for x in v[2:]]
            P = np.array([f[8 * i:8 * i + 3] for i in range(4)])
            UV = np.array([f[8 * i + 3:8 * i + 5] for i in range(4)])
            N = np.array([f[8 * i + 5:8 * i + 8] for i in range(4)])
            quads.append((P, N, UV))
    return quads


def player_boxes():
    """(centre, size, rgb) of a plain player: head 8, body 8x12x4, arms and legs 4x12x4 (pixels, feet at y 0, front +z)"""
    skin, shirt, trousers = (214, 166, 130), (70, 110, 170), (60, 60, 110)
    return [((0, 28, 0), (8, 8, 8), skin), ((0, 18, 0), (8, 12, 4), shirt), ((6, 18, 0), (4, 12, 4), skin), ((-6, 18, 0), (4, 12, 4), skin),
            ((2, 6, 0), (4, 12, 4), trousers), ((-2, 6, 0), (4, 12, 4), trousers)]


def box_quads(c, s, rgb):
    cx, cy, cz = c
    sx, sy, sz = (x / 2 for x in s)
    out = []
    for axis, sign in ((0, 1), (0, -1), (1, 1), (1, -1), (2, 1), (2, -1)):
        a, b = [i for i in range(3) if i != axis]
        n = np.zeros(3)
        n[axis] = sign
        P = []
        for ua, ub in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
            p = np.array([cx, cy, cz], float)
            p[axis] += sign * (sx, sy, sz)[axis]
            p[a] += ua * (sx, sy, sz)[a]
            p[b] += ub * (sx, sy, sz)[b]
            P.append(p)
        out.append((np.array(P), np.array([n] * 4), rgb))
    return out


def render(items, yaw, pitch, W, H, scale, centre, bg=(236, 238, 242)):
    """items: (verts, normals, colour or (texture array, uvs, texscale)). yaw 0 looks at the front (+z), 180 at the back."""
    img = np.zeros((H, W, 3), float)
    img[:] = bg
    zbuf = np.full((H, W), 1e9)
    cy_, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def cam(p):
        p = np.asarray(p, float) - centre
        x = p[0] * cy_ - p[2] * sy_
        z = p[0] * sy_ + p[2] * cy_
        y = p[1] * cp - z * sp
        z = p[1] * sp + z * cp
        return np.array([W / 2 - x * scale, H / 2 - y * scale, z])           # screen x is mirrored so +x (the model's left) is on the viewer's right when looking at the front

    def camn(n):
        n = np.asarray(n, float)
        x = n[0] * cy_ - n[2] * sy_
        z = n[0] * sy_ + n[2] * cy_
        y = n[1] * cp - z * sp
        z = n[1] * sp + z * cp
        return np.array([-x, y, z])

    for P, N, col in items:
        S = [cam(p) for p in P]
        nrm = camn(np.mean(N, axis=0))
        n_len = np.linalg.norm(nrm)
        shade = 0.55 + 0.45 * abs(nrm[2] / n_len) if n_len > 1e-9 else 0.8
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = (S[i] for i in tri)
            x0, x1 = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(W - 1, math.ceil(max(a[0], b[0], c[0]))))
            y0, y1 = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(H - 1, math.ceil(max(a[1], b[1], c[1]))))
            if x1 < x0 or y1 < y0:
                continue
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            w0 = ((b[1] - c[1]) * (xs - c[0]) + (c[0] - b[0]) * (ys - c[1])) / den
            w1 = ((c[1] - a[1]) * (xs - c[0]) + (a[0] - c[0]) * (ys - c[1])) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -0.001) & (w1 >= -0.001) & (w2 >= -0.001)
            z = w0 * a[2] + w1 * b[2] + w2 * c[2]
            sub = zbuf[y0:y1 + 1, x0:x1 + 1]
            ok = inside & (z < sub)
            if not ok.any():
                continue
            if isinstance(col, tuple) and len(col) == 3 and not hasattr(col[0], "shape"):
                rgb = np.array(col, float) * shade
                img[y0:y1 + 1, x0:x1 + 1][ok] = rgb
                sub[ok] = z[ok]
            else:
                tex, uvs, ts = col
                uvt = [uvs[i] for i in tri]
                u = (w0 * uvt[0][0] + w1 * uvt[1][0] + w2 * uvt[2][0]) * ts
                v = (w0 * uvt[0][1] + w1 * uvt[1][1] + w2 * uvt[2][1]) * ts
                ui = np.clip(u.astype(int), 0, tex.shape[1] - 1)
                vi = np.clip(v.astype(int), 0, tex.shape[0] - 1)
                px = tex[vi, ui]
                ok &= px[..., 3] > 127
                if not ok.any():
                    continue
                img[y0:y1 + 1, x0:x1 + 1][ok] = px[..., :3][ok] * shade
                sub[ok] = z[ok]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")


def mesh_items(eid, skin_id=None, tint=(255, 255, 255)):
    quads = read_mesh(eid)
    skin = np.array(Image.open(os.path.join(RES, "textures", "entity", "boss_mesh", (skin_id or eid) + ".png")).convert("RGBA"))
    ts = skin.shape[1] / 128.0
    tex = skin.copy()
    tex[..., :3] = (tex[..., :3].astype(float) * np.array(tint) / 255.0).astype(np.uint8)
    return [(P, N, (tex, UV, ts)) for P, N, UV in quads]


def sheet(eid, out, skin_id=None, tint=(255, 255, 255), title=None, views=((90, 8), (0, 8), (40, 14)), W=300, H=420, scale=7.5, centre=(0, 17, -8)):
    """side (the player faces right), back, back three-quarter"""
    items = [(P, N, rgb) for c, s, rgb in player_boxes() for P, N, rgb in box_quads(c, s, rgb)] + mesh_items(eid, skin_id, tint)
    ims = [render(items, yaw, pitch, W, H, scale, np.array(centre, float)) for yaw, pitch in views]
    out_im = Image.new("RGB", (W * len(ims), H + 22), (255, 255, 255))
    for i, im in enumerate(ims):
        out_im.paste(im, (W * i, 22))
    ImageDraw.Draw(out_im).text((6, 5), title or eid, fill=(20, 20, 20))
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    out_im.save(out, optimize=True)
    return out


if __name__ == "__main__":
    a = [x for x in sys.argv[1:] if not x.startswith("--")]
    skin = sys.argv[sys.argv.index("--skin") + 1] if "--skin" in sys.argv else None
    if skin:
        a = [x for x in a if x != skin]
    print(sheet(a[0], a[1], skin))

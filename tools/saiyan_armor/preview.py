"""Software preview of the Saiyan alloy armour as the game draws it: the worn model files (relic_worn/saiyan_alloy_*.txt) with their baked skins on a plain stand-in body, from the front, the side
and the back, solid pass plus the additive glow-map pass. No Minecraft, no Blender: numpy + Pillow.

    python3 -B tools/saiyan_armor/preview.py [--src DIR] [--out docs/saiyan_armor/previews]

--src DIR reads models from DIR and skins from DIR/skins (a Blender trial run); the default is the mod's assets.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")

# world frame of the player (y up, front -z, the player's right = -x), pixels; anchor pivots
PIVOT = {"head": (0, 24, 0), "body": (0, 24, 0), "rarm": (-5, 22, 0), "larm": (5, 22, 0), "rleg": (-1.9, 12, 0), "lleg": (1.9, 12, 0)}
SETS = {   # the catalog's item lines: item -> [(model, anchor or None, mirrored)]
    "saiyan_alloy_scouter": [("saiyan_alloy_scouter", None, False)],
    "saiyan_alloy_battle_armor": [("saiyan_alloy_chest", None, False), ("saiyan_alloy_pauldron", "rarm", False), ("saiyan_alloy_pauldron", "larm", True),
                                  ("saiyan_alloy_vambrace", "rarm", False), ("saiyan_alloy_vambrace", "larm", True)],
    "saiyan_alloy_waist_guard": [("saiyan_alloy_skirt", None, False)],
    "saiyan_alloy_greaves": [("saiyan_alloy_greave", "rleg", False), ("saiyan_alloy_greave", "lleg", True)],
}


def read_model(path):
    anchor, skin, glow, quads = "body", None, None, []
    for raw in open(path, encoding="utf8"):
        v = raw.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "anchor":
            anchor = v[1]
        elif v[0] == "skin":
            skin = v[1]
        elif v[0] == "glowtex":
            glow = v[1]
        elif v[0] == "q":
            n = [float(x) for x in v[4:]]
            quads.append((int(v[3]), np.array(n).reshape(4, 8)))
    return anchor, skin, glow, quads


def load_tex(src_skins, rel, texdir):
    p = os.path.join(src_skins, os.path.basename(rel)) if src_skins else os.path.join(texdir, rel)
    return np.asarray(Image.open(p).convert("RGBA"), np.float32) / 255.0


def to_world(p, anchor, mirror):
    ox, oy, oz = PIVOT[anchor]
    x = -p[:, 0] if mirror else p[:, 0]
    return np.stack([x + ox, oy - p[:, 1], p[:, 2] + oz], axis=1)


def body_boxes():
    """Stand-in body: (name, lo, hi, rgb)."""
    sk, sh, pa = (200, 150, 112), (60, 70, 120), (50, 52, 70)
    return [("head", (-4, 24, -4), (4, 32, 4), sk), ("body", (-4, 12, -2), (4, 24, 2), sh), ("rarm", (-8, 12, -2), (-4, 24, 2), sk), ("larm", (4, 12, -2), (8, 24, 2), sk),
            ("rleg", (-4, 0, -2), (0, 12, 2), pa), ("lleg", (0, 0, -2), (4, 12, 2), pa)]


def box_quads(lo, hi):
    (x0, y0, z0), (x1, y1, z1) = lo, hi
    v = np.array([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)], float)
    f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (2, 3, 7, 6), (1, 2, 6, 5), (0, 4, 7, 3)]
    return [v[list(i)] for i in f]


def view_basis(view):
    """screen x, screen y(up), depth (bigger = farther) as rows of a 3x3."""
    if view == "front":
        return np.array([[1, 0, 0], [0, 1, 0], [0, 0, 1]], float)
    if view == "back":
        return np.array([[-1, 0, 0], [0, 1, 0], [0, 0, -1]], float)
    return np.array([[0, 0, -1], [0, 1, 0], [-1, 0, 0]], float)      # the player's left side: front to the right


LIGHT = np.array([-0.35, 0.55, -0.75])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def raster(img, zbuf, B, scale, cx, cy, P, UV, N, tex, additive=False, flat=None, cull=True):
    """Rasterises one quad (two triangles) with a texture or a flat colour."""
    S = P @ B.T
    sx = cx + S[:, 0] * scale
    sy = cy - S[:, 1] * scale
    dz = S[:, 2]
    nrm = N.mean(axis=0)
    nl = np.linalg.norm(nrm)
    nrm = nrm / nl if nl > 1e-6 else nrm
    shade = 0.5 + 0.5 * max(0.0, float(nrm @ LIGHT))
    for tri in ((0, 1, 2), (0, 2, 3)):
        a, b, c = tri
        xs, ys = sx[[a, b, c]], sy[[a, b, c]]
        x0, x1 = int(max(0, math.floor(xs.min()))), int(min(img.shape[1] - 1, math.ceil(xs.max())))
        y0, y1 = int(max(0, math.floor(ys.min()))), int(min(img.shape[0] - 1, math.ceil(ys.max())))
        if x1 < x0 or y1 < y0:
            continue
        den = (ys[1] - ys[2]) * (xs[0] - xs[2]) + (xs[2] - xs[1]) * (ys[0] - ys[2])
        if abs(den) < 1e-9:
            continue
        gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
        l0 = ((ys[1] - ys[2]) * (gx - xs[2]) + (xs[2] - xs[1]) * (gy - ys[2])) / den
        l1 = ((ys[2] - ys[0]) * (gx - xs[2]) + (xs[0] - xs[2]) * (gy - ys[2])) / den
        l2 = 1 - l0 - l1
        m = (l0 >= -0.001) & (l1 >= -0.001) & (l2 >= -0.001)
        if not m.any():
            continue
        z = l0 * dz[a] + l1 * dz[b] + l2 * dz[c]
        sub = zbuf[y0:y1 + 1, x0:x1 + 1]
        ok = m & (z < sub + (0.02 if additive else 0.0))
        if not ok.any():
            continue
        if flat is not None:
            col = np.zeros(ok.shape + (4,), np.float32)
            col[..., :3] = np.array(flat, np.float32) / 255.0 * shade
            col[..., 3] = 1.0
        else:
            u = l0 * UV[a, 0] + l1 * UV[b, 0] + l2 * UV[c, 0]
            v = l0 * UV[a, 1] + l1 * UV[b, 1] + l2 * UV[c, 1]
            H, W = tex.shape[:2]
            col = tex[np.clip((v * H).astype(int), 0, H - 1), np.clip((u * W).astype(int), 0, W - 1)].copy()
            if not additive:
                col[..., :3] *= shade
        reg = img[y0:y1 + 1, x0:x1 + 1]
        if additive:
            reg[ok] = np.minimum(1.0, reg[ok] + col[ok][:, :3] * col[ok][:, 3:4] * 1.0)
        else:
            sel = ok & (col[..., 3] > 0.5)
            reg[sel] = col[sel][:, :3]
            sub[sel] = z[sel]


def render(item, models_dir, skins, view, scale=14.0, size=(300, 560), focus=None):
    img = np.zeros((size[1], size[0], 3), np.float32) + np.array([0.13, 0.14, 0.18], np.float32)
    zbuf = np.full((size[1], size[0]), 1e9, np.float32)
    B = view_basis(view)
    f = B @ np.array(focus if focus else (0.0, 16.0, 0.0), float)
    cx, cy = size[0] / 2 - f[0] * scale, size[1] / 2 + f[1] * scale
    for name, lo, hi, col in body_boxes():
        for q in box_quads(lo, hi):
            n = np.cross(q[1] - q[0], q[2] - q[0])
            nn = np.tile(n / (np.linalg.norm(n) + 1e-9), (4, 1))
            raster(img, zbuf, B, scale, cx, cy, q, np.zeros((4, 2)), nn, None, flat=col)
    glows = []
    pieces = SETS[item] if isinstance(item, str) else item
    for model, anchor, mirror in pieces:
        a0, skin, glow, quads = read_model(os.path.join(models_dir, model + ".txt"))
        a = anchor or a0
        tex = load_tex(skins, skin, os.path.join(ASSETS, "textures"))
        gtex = load_tex(skins, glow, os.path.join(ASSETS, "textures")) if glow else None
        for flags, q in quads:
            P = to_world(q[:, 0:3], a, mirror)
            N = q[:, 5:8].copy()
            N[:, 1] *= -1
            if mirror:
                N[:, 0] *= -1
            uv = q[:, 3:5]
            if mirror:
                P, uv, N = P[[0, 3, 2, 1]], uv[[0, 3, 2, 1]], N[[0, 3, 2, 1]]            # the writer's corner order for a mirrored piece
            raster(img, zbuf, B, scale, cx, cy, P, uv, N, tex)
            if flags & 8 and gtex is not None:
                glows.append((P, uv, N, gtex))
    for P, uv, N, gtex in glows:
        raster(img, zbuf, B, scale, cx, cy, P, uv, N, gtex, additive=True)
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def sheet(models_dir, skins, out):
    cols = []
    for view in ("front", "side", "back"):
        cols.append(render(list(SETS.keys())[0] and sum((SETS[k] for k in SETS), []), models_dir, skins, view))
    w = sum(c.width for c in cols)
    im = Image.new("RGB", (w, cols[0].height + 26), (20, 22, 28))
    x = 0
    d = ImageDraw.Draw(im)
    for c, t in zip(cols, ("front", "side (left)", "back")):
        im.paste(c, (x, 26))
        d.text((x + 8, 8), t, fill=(220, 224, 240))
        x += c.width
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out)
    return im


def pieces_sheet(models_dir, skins, out):
    """Close views: every item on its own part of the body, front and a three-quarter turn is approximated by the side view."""
    tiles = []
    specs = [("saiyan_alloy_scouter", 40.0, (0, 28, 0), (320, 320)), ("saiyan_alloy_battle_armor", 22.0, (0, 18, 0), (460, 420)), ("saiyan_alloy_waist_guard", 30.0, (0, 9, 0), (400, 300)),
             ("saiyan_alloy_greaves", 30.0, (0, 6, 0), (400, 300))]
    for item, scale, focus, size in specs:
        row = [render(item, models_dir, skins, v, scale=scale, size=size, focus=focus) for v in ("front", "side", "back")]
        tiles.append(row)
    W = max(sum(i.width for i in r) for r in tiles)
    H = sum(r[0].height for r in tiles)
    im = Image.new("RGB", (W, H), (20, 22, 28))
    y = 0
    for r in tiles:
        x = 0
        for i in r:
            im.paste(i, (x, y))
            x += i.width
        y += r[0].height
    os.makedirs(os.path.dirname(out), exist_ok=True)
    im.save(out)
    return im


def main():
    src = None
    out = os.path.join(ROOT, "docs", "saiyan_armor", "previews")
    if "--src" in sys.argv:
        src = os.path.abspath(sys.argv[sys.argv.index("--src") + 1])
    if "--out" in sys.argv:
        out = os.path.abspath(sys.argv[sys.argv.index("--out") + 1])
    models = src or os.path.join(ASSETS, "relic_worn")
    skins = os.path.join(src, "skins") if src else None
    sheet(models, skins, os.path.join(out, "saiyan_alloy_set.png"))
    pieces_sheet(models, skins, os.path.join(out, "saiyan_alloy_pieces.png"))
    print("wrote previews to", out)


if __name__ == "__main__":
    main()

"""Software preview of the grimoire revamp (docs/grimoire_revamp.md): classic vs revamped book, closed and open, plus the glow halo / rune ring.

A small z-buffer rasteriser over the quads the mod itself builds (tools/grimoire_revamp/DumpPlans.java writes them), with the item colour tints, flat lighting by normal,
glowing quads full-bright and the art-pack overlays alpha-blended. It is NOT the game: no Minecraft lighting, no item-atlas filtering, no bloom, no particles.

    python3 -B tools/grimoire_revamp/preview.py PLANS.json OUT_DIR [look ...]
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
TEX = os.path.join(ASSETS, "textures", "item", "grimoire_book")
HALO = os.path.join(ASSETS, "textures", "entity", "grimoire_revamp_halo.png")
RUNES = os.path.join(ASSETS, "textures", "particle", "magic_runes")
_cache = {}


def tex(path):
    if path not in _cache:
        _cache[path] = np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255.0 if os.path.isfile(path) else None
    return _cache[path]


def rgb(v):
    return np.array([(v >> 16) & 255, (v >> 8) & 255, v & 255], np.float32) / 255.0 if v >= 0 else np.ones(3, np.float32)


def rot(yaw, pitch):
    cy, sy, cp, sp = math.cos(yaw), math.sin(yaw), math.cos(pitch), math.sin(pitch)
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rx = np.array([[1, 0, 0], [0, cp, -sp], [0, sp, cp]])
    return rx @ ry


def render(quads, tints, size, yaw, pitch, extra=(), scale=None, bg=(34, 30, 44)):
    """quads: [texture, tint, emissive, pos(12), uv(8)]; extra: [(image array, z, half, spin, rgb, alpha)] drawn last (halo / ring, model units, centre (8, 8))."""
    R = rot(math.radians(yaw), math.radians(pitch))
    img = np.zeros((size, size, 3), np.float32)
    img[:] = np.array(bg, np.float32) / 255.0
    zb = np.full((size, size), 1e9, np.float32)
    s = scale or size / 20.0
    light = np.array([-0.35, 0.8, 0.5])
    light /= np.linalg.norm(light)
    items = []
    for q in quads:
        items.append(("q", q))
    for e in extra:
        items.append(("x", e))
    for kind, it in items:
        if kind == "q":
            name, tint, emis, pos, uv = it
            p = np.array(pos, np.float32).reshape(4, 3) - 8.0
            arr = tex(os.path.join(TEX, name + ".png"))
            if arr is None:
                continue
            col_t = rgb(tint) if tint >= 0 else np.ones(3, np.float32)
            uvs = np.array(uv, np.float32).reshape(4, 2) / 16.0
            n = np.cross(p[1] - p[0], p[2] - p[0]) if np.linalg.norm(np.cross(p[1] - p[0], p[2] - p[0])) > 1e-9 else np.cross(p[2] - p[1], p[3] - p[1])
        else:
            arr, z, half, spin, col, alpha = it
            c, sn = math.cos(math.radians(spin)), math.sin(math.radians(spin))
            corners = [(-1, -1), (1, -1), (1, 1), (-1, 1)]
            p = np.array([[half * (sx * c - sy * sn), half * (sx * sn + sy * c), z - 8.0] for sx, sy in corners], np.float32)
            uvs = np.array([[0, 1], [1, 1], [1, 0], [0, 0]], np.float32)
            col_t, emis = col, 1
            n = np.array([0, 0, -1.0])
        nn = np.linalg.norm(n)
        if nn < 1e-9:
            continue
        n = n / nn
        w = p @ R.T                                         # camera space: x right, y up, z towards the viewer
        nw = R @ n
        if kind == "q" and nw[2] < -0.02 and not (name.startswith("revamp") and False):
            pass
        shade = 1.0 if emis else 0.52 + 0.48 * max(0.0, float(np.dot(R @ n, R @ light)))
        sx_ = size / 2 + w[:, 0] * s
        sy_ = size / 2 - w[:, 1] * s
        dep = -w[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c3 = tri
            x0, x1 = int(max(0, math.floor(min(sx_[[a, b, c3]])))), int(min(size - 1, math.ceil(max(sx_[[a, b, c3]]))))
            y0, y1 = int(max(0, math.floor(min(sy_[[a, b, c3]])))), int(min(size - 1, math.ceil(max(sy_[[a, b, c3]]))))
            if x1 < x0 or y1 < y0:
                continue
            xs, ys = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            d = (sy_[b] - sy_[c3]) * (sx_[a] - sx_[c3]) + (sx_[c3] - sx_[b]) * (sy_[a] - sy_[c3])
            if abs(d) < 1e-9:
                continue
            l1 = ((sy_[b] - sy_[c3]) * (xs - sx_[c3]) + (sx_[c3] - sx_[b]) * (ys - sy_[c3])) / d
            l2 = ((sy_[c3] - sy_[a]) * (xs - sx_[c3]) + (sx_[a] - sx_[c3]) * (ys - sy_[c3])) / d
            l3 = 1 - l1 - l2
            m = (l1 >= -1e-4) & (l2 >= -1e-4) & (l3 >= -1e-4)
            if not m.any():
                continue
            z = l1 * dep[a] + l2 * dep[b] + l3 * dep[c3]
            u = l1 * uvs[a, 0] + l2 * uvs[b, 0] + l3 * uvs[c3, 0]
            v = l1 * uvs[a, 1] + l2 * uvs[b, 1] + l3 * uvs[c3, 1]
            H, W = arr.shape[:2]
            ti = np.clip((u * W).astype(int), 0, W - 1)
            tj = np.clip((v * H).astype(int), 0, H - 1)
            t = arr[tj, ti]
            alpha_px = t[..., 3] * (alpha if kind == "x" else 1.0)
            if kind == "x":
                col = t[..., :3] * col_t
            else:
                col = t[..., :3] * col_t * shade
            ys_i, xs_i = np.where(m)
            zz = z[ys_i, xs_i]
            gy, gx = ys_i + y0, xs_i + x0
            ok = (alpha_px[ys_i, xs_i] > 0.04) & ((zz < zb[gy, gx] + (0.02 if kind == "q" else 0.0)) if kind == "q" else (zz < zb[gy, gx] + 1e-3))
            if kind == "x":
                ok &= alpha_px[ys_i, xs_i] > 0.01
            if not ok.any():
                continue
            ys_i, xs_i, zz, gy, gx = ys_i[ok], xs_i[ok], zz[ok], gy[ok], gx[ok]
            a_ = alpha_px[ys_i, xs_i][:, None]
            cc = col[ys_i, xs_i]
            if kind == "q":
                a_ = np.where(a_ > 0.97, 1.0, a_)
            img[gy, gx] = img[gy, gx] * (1 - a_) + cc * a_
            if kind == "q":
                zb[gy, gx] = np.where(a_[:, 0] > 0.5, zz, zb[gy, gx])
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def rune_for(magic):
    for suffix in ("_rune_ring.png", ".png"):
        p = os.path.join(RUNES, magic.lower() + suffix)
        if os.path.isfile(p):
            return tex(p)
    return tex(os.path.join(RUNES, "anti_magic.png"))


def main():
    plans = json.load(open(sys.argv[1]))
    out = sys.argv[2]
    only = set(sys.argv[3:])
    os.makedirs(out, exist_ok=True)
    size = 360
    by = {}
    for p in plans:
        by[(p["look"], p["held"], p["open"])] = p
    looks = sorted({p["look"] for p in plans})
    sheet_rows = []
    for look in looks:
        if only and look not in only:
            continue
        cells = []
        labels = []
        for held, open_, yaw, pitch, name in ((False, 0, 38, -22, "closed, front 3/4"), (False, 0, 200, -18, "closed, back 3/4"), (True, 3, 180, 18, "open, reader side"), (True, 3, 0, 14, "open, onlooker side")):
            p = by[(look, held, open_)]
            row = []
            for variant in ("classic", "revamp"):
                quads = [[q[0], p["tint"][q[1]] if q[1] >= 0 else -1, q[2], q[3], q[4]] for q in p[variant]]
                row.append(render(quads, p["tint"], size, yaw, pitch))
            cells.append(row)
            labels.append(name)
        w = size * len(cells)
        img = Image.new("RGB", (w, size * 2 + 56), (22, 20, 30))
        d = ImageDraw.Draw(img)
        d.text((8, 4), "%s   top: classic (%d quads closed)   bottom: revamp (%d quads closed)" % (look, len(by[(look, False, 0)]["classic"]), len(by[(look, False, 0)]["revamp"])), fill=(235, 225, 190))
        for i, row in enumerate(cells):
            d.text((i * size + 8, 22), labels[i], fill=(190, 190, 205))
            img.paste(row[0], (i * size, 40))
            img.paste(row[1], (i * size, 40 + size))
        img.save(os.path.join(out, "books_%s.png" % look))
        sheet_rows.append(img)
        print("wrote", look)
    # the effect layer: open book with the halo and the rune ring, idle and on a cast flare (software approximation)
    look = "wind_four_leaf" if "wind_four_leaf" in looks else looks[0]
    p = by[(look, True, 3)]
    quads = [[q[0], p["tint"][q[1]] if q[1] >= 0 else -1, q[2], q[3], q[4]] for q in p["revamp"]]
    glow = rgb(p["glow"])
    halo, ring = tex(HALO), rune_for(p["magic"])
    cells = []
    for title, fl, st in (("classic (no effect layer)", None, 0), ("revamp idle", 0.0, 0), ("revamp, cast flare", 1.0, 1.0)):
        if fl is None:
            qs = [[q[0], p["tint"][q[1]] if q[1] >= 0 else -1, q[2], q[3], q[4]] for q in p["classic"]]
            cells.append((title, render(qs, p["tint"], 420, 180, 22)))
            continue
        extra = [(halo, 7.3, 9.5 + 2.5 * fl * st, 0, glow, min(1, 0.16 + 0.35 * fl * st)),
                 (ring, 6.9, 6.6 + 2.2 * fl * st, 20, glow, min(1, 0.30 + 0.55 * fl * st)),
                 (ring, 6.85, (6.6 + 2.2 * fl * st) * 0.62, -28, glow, min(1, (0.30 + 0.55 * fl * st) * 0.8))]
        cells.append((title, render(quads, p["tint"], 420, 180, 22, extra=extra)))
    img = Image.new("RGB", (420 * 3, 420 + 24), (22, 20, 30))
    d = ImageDraw.Draw(img)
    for i, (title, im) in enumerate(cells):
        d.text((i * 420 + 8, 6), "%s  (%s, software approximation, no particles)" % (title, look), fill=(235, 225, 190))
        img.paste(im, (i * 420, 24))
    img.save(os.path.join(out, "effects_%s.png" % look))
    print("wrote effects")


if __name__ == "__main__":
    main()

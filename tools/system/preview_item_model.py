#!/usr/bin/env python3
"""Why: the Shadow Core's held model is a vanilla "elements" model (cuboids, some turned), which nobody can look at without starting the game. This script reads the real
model file and its texture and rasterises it (a z-buffer, nearest-texel skin, a simple light from above and the side, the full-bright elements unshaded and additive-ish)
from four sides and from above, on a dark and on a daylight background, next to the 16x16 inventory icon enlarged. It follows the game's element rules (the faces'
uv mapping, a turn about one axis through an origin) closely enough to judge shape, colour and glow; it is NOT the game's renderer (no ambient occlusion, no real lighting).

    python tools/system/preview_item_model.py [--model shadow_core] [--out build/system_preview/shadow_core.png]
"""
import argparse
import json
import math
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
W = H = 360


def load(model):
    m = json.load(open(os.path.join(ASSETS, "models", "item", model + "_3d.json")))
    tex = {}
    for k, v in m["textures"].items():
        tex[k] = np.asarray(Image.open(os.path.join(ASSETS, "textures", v.split(":")[1] + ".png")).convert("RGBA"), np.float32) / 255
    return m, tex


def rot(point, el):
    r = el.get("rotation")
    if not r:
        return np.array(point, float)
    o = np.array(r["origin"], float)
    a = math.radians(r["angle"])
    c, s = math.cos(a), math.sin(a)
    x, y, z = np.array(point, float) - o
    if r["axis"] == "y":
        x, z = c * x + s * z, -s * x + c * z
    elif r["axis"] == "x":
        y, z = c * y - s * z, s * y + c * z
    else:
        x, y = c * x - s * y, s * x + c * y
    return np.array([x, y, z]) + o


def faces(el):
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    # vertex order = the game's: (top-left, bottom-left, bottom-right, top-right) of the face as seen from outside
    quad = {
        "north": [(x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)],
        "south": [(x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)],
        "east": [(x1, y1, z1), (x1, y0, z1), (x1, y0, z0), (x1, y1, z0)],
        "west": [(x0, y1, z0), (x0, y0, z0), (x0, y0, z1), (x0, y1, z1)],
        "up": [(x0, y1, z0), (x0, y1, z1), (x1, y1, z1), (x1, y1, z0)],
        "down": [(x0, y0, z1), (x0, y0, z0), (x1, y0, z0), (x1, y0, z1)],
    }
    for name, f in el["faces"].items():
        yield name, [rot(v, el) for v in quad[name]], f


def build(model):
    quads = []
    for el in model["elements"]:
        glow = el.get("neoforge_data", {}).get("block_light", 0) > 0
        for name, pts, f in faces(el):
            u1, v1, u2, v2 = f["uv"]
            uv = [(u1, v1), (u1, v2), (u2, v2), (u2, v1)]
            n = np.cross(pts[1] - pts[0], pts[2] - pts[1])
            n = n / (np.linalg.norm(n) + 1e-9)
            quads.append((pts, uv, n, glow and f.get("shade", True) is False, f["texture"].lstrip("#")))
    return quads


def camera(yaw, pitch, dist=34.0):
    t = np.array([8.0, 8.0, 8.0])
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    eye = t + dist * np.array([sy * cp, sp, cy * cp])
    f = (t - eye) / np.linalg.norm(t - eye)
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    return eye, f, r, np.cross(r, f)


def render(quads, tex, yaw, pitch, bg_top, bg_bottom):
    eye, f, r, u = camera(yaw, pitch)
    k = (H / 2) / math.tan(math.radians(20))
    img = np.zeros((H, W, 3), np.float32)
    for y in range(H):
        img[y, :] = np.array(bg_top, np.float32) * (1 - y / H) + np.array(bg_bottom, np.float32) * (y / H)
    zbuf = np.full((H, W), 1e9, np.float32)
    light = np.array([0.35, 0.9, -0.45])
    light /= np.linalg.norm(light)
    for pts, uv, n, unshaded, tname in quads:
        d = [np.array(p) - eye for p in pts]
        z = [x @ f for x in d]
        if min(z) < 1:
            continue
        # back-face cull
        if n @ (np.array(pts[0]) - eye) > 0:
            continue
        sc = [(W / 2 + (x @ r) / zz * k, H / 2 - (x @ u) / zz * k) for x, zz in zip(d, z)]
        shade = 1.0 if unshaded else 0.50 + 0.50 * max(0.0, float(n @ light))
        tx = tex[tname]
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [sc[i] for i in tri]
            minx, maxx = int(max(0, math.floor(min(a[0], b[0], c[0])))), int(min(W - 1, math.ceil(max(a[0], b[0], c[0]))))
            miny, maxy = int(max(0, math.floor(min(a[1], b[1], c[1])))), int(min(H - 1, math.ceil(max(a[1], b[1], c[1]))))
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            for py in range(miny, maxy + 1):
                for px in range(minx, maxx + 1):
                    w0 = ((b[1] - c[1]) * (px + 0.5 - c[0]) + (c[0] - b[0]) * (py + 0.5 - c[1])) / den
                    w1 = ((c[1] - a[1]) * (px + 0.5 - c[0]) + (a[0] - c[0]) * (py + 0.5 - c[1])) / den
                    w2 = 1 - w0 - w1
                    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                        continue
                    zi = 1.0 / (w0 / z[tri[0]] + w1 / z[tri[1]] + w2 / z[tri[2]])
                    if zi >= zbuf[py, px]:
                        continue
                    pu = (w0 * uv[tri[0]][0] / z[tri[0]] + w1 * uv[tri[1]][0] / z[tri[1]] + w2 * uv[tri[2]][0] / z[tri[2]]) * zi
                    pv = (w0 * uv[tri[0]][1] / z[tri[0]] + w1 * uv[tri[1]][1] / z[tri[1]] + w2 * uv[tri[2]][1] / z[tri[2]]) * zi
                    th, tw = tx.shape[:2]
                    sx = min(tw - 1, max(0, int(pu / 16 * tw)))
                    sy = min(th - 1, max(0, int(pv / 16 * th)))
                    texel = tx[sy, sx]
                    if texel[3] < 0.1:
                        continue
                    zbuf[py, px] = zi
                    img[py, px] = texel[:3] * 255 * shade * texel[3] + img[py, px] * (1 - texel[3])
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="shadow_core")
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "system_preview", "shadow_core.png"))
    args = ap.parse_args()
    model, tex = load(args.model)
    quads = build(model)
    views = [(35, 22), (125, 22), (215, 22), (35, 68)]
    dark, light = ((14, 10, 28), (30, 22, 56)), ((150, 170, 200), (214, 224, 238))
    sheet = Image.new("RGB", (W * 4 + 150, H * 2), (10, 10, 14))
    for row, (top, bottom) in enumerate((dark, light)):
        for i, (yaw, pitch) in enumerate(views):
            sheet.paste(render(quads, tex, yaw, pitch, top, bottom), (i * W, row * H))
    icon = Image.open(os.path.join(ASSETS, "textures", "item", args.model + ".png")).convert("RGBA")
    for row, bg in enumerate(((20, 14, 36), (198, 198, 198))):
        tile = Image.new("RGBA", (128, 128), bg + (255,))
        tile.alpha_composite(icon.resize((128, 128), Image.NEAREST))
        sheet.paste(tile.convert("RGB"), (W * 4 + 11, row * H + 20))
    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    sheet.save(args.out)
    print("wrote", args.out)


if __name__ == "__main__":
    main()

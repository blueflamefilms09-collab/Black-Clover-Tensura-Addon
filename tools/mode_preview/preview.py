"""Headless preview of the transformation armour overlays (0.38).

Compiles the REAL ModeArmorLayer against small stubs (stubs/, plus the VFX preview's Mth / ResourceLocation stubs and JOML),
runs it for every mode with the player renderer's transform, and rasterises the quads with the real textures next to a
block-figure player, from the front and the back.

    python tools/mode_preview/preview.py   ->  build/mode_preview/modes.png
Needs JDK 21, numpy, Pillow, and tools/vfx_preview/lib/joml-1.10.5.jar (the VFX preview downloads it).
"""
import json
import math
import os
import shutil
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "build", "mode_preview")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
JOML = os.path.join(ROOT, "tools", "vfx_preview", "lib", "joml-1.10.5.jar")
W, H = 300, 340


def build():
    cls = os.path.join(OUT, "classes")
    shutil.rmtree(cls, ignore_errors=True)
    files = []
    for base in (os.path.join(HERE, "stubs"),):
        for d, _, fs in os.walk(base):
            files += [os.path.join(d, f) for f in fs if f.endswith(".java")]
    vstub = os.path.join(ROOT, "tools", "vfx_preview", "stubs", "net", "minecraft")
    files += [os.path.join(vstub, "util", "Mth.java"), os.path.join(vstub, "resources", "ResourceLocation.java")]
    files.append(os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "mode", "ModeArmorLayer.java"))
    files.append(os.path.join(HERE, "Preview.java"))
    subprocess.run(["javac", "-nowarn", "-cp", JOML, "-d", cls] + files, check=True)
    return cls


_tex = {}


def tex(path):
    if path not in _tex:
        _tex[path] = np.asarray(Image.open(os.path.join(TEX, path)).convert("RGBA"), dtype=np.float32) / 255
    return _tex[path]


def boxes():
    v = []
    for mn, mx, col in [((-0.25, 0.75, -0.125), (0.25, 1.5, 0.125), (0.25, 0.55, 0.65)),
                        ((-0.25, 1.5, -0.25), (0.25, 2.0, 0.25), (0.75, 0.6, 0.45)),
                        ((-0.25, 0.0, -0.125), (0.0, 0.75, 0.125), (0.2, 0.25, 0.55)),
                        ((0.0, 0.0, -0.125), (0.25, 0.75, 0.125), (0.2, 0.25, 0.5)),
                        ((-0.5, 0.75, -0.125), (-0.25, 1.5, 0.125), (0.75, 0.6, 0.45)),
                        ((0.25, 0.75, -0.125), (0.5, 1.5, 0.125), (0.75, 0.6, 0.45))]:
        x0, y0, z0 = mn; x1, y1, z1 = mx
        for p in ([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)],
                  [(x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (x0, y0, z1)], [(x1, y0, z0), (x1, y1, z0), (x1, y1, z1), (x1, y0, z1)],
                  [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]):
            v.append((np.array(p, float), col))
    return v


def render(quads, eye, target):
    img = np.ones((H, W, 3), np.float32) * np.array([0.52, 0.64, 0.8])
    zbuf = np.full((H, W), 1e9, np.float32)
    f = np.array(target, float) - eye; f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    foc = W / (2 * math.tan(math.radians(28)))
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)

    def raster(p, uv, shade_rgb, t, kind, alpha):
        d = p - eye
        z = d @ f
        if (z < 0.05).any():
            return
        sx, sy = W / 2 + (d @ r) * foc / z, H / 2 - (d @ u) * foc / z
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, x1, x2 = sx[list(tri)]; y0, y1, y2 = sy[list(tri)]
            den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
            if abs(den) < 1e-9:
                continue
            minx, maxx = int(max(0, min(x0, x1, x2))), int(min(W - 1, max(x0, x1, x2))) + 1
            miny, maxy = int(max(0, min(y0, y1, y2))), int(min(H - 1, max(y0, y1, y2))) + 1
            if minx >= maxx or miny >= maxy:
                continue
            X = xx[miny:maxy, minx:maxx] + .5; Y = yy[miny:maxy, minx:maxx] + .5
            a = ((y1 - y2) * (X - x2) + (x2 - x1) * (Y - y2)) / den
            b = ((y2 - y0) * (X - x2) + (x0 - x2) * (Y - y2)) / den
            c = 1 - a - b
            m = (a >= 0) & (b >= 0) & (c >= 0)
            Z = a * z[tri[0]] + b * z[tri[1]] + c * z[tri[2]]
            reg = img[miny:maxy, minx:maxx]
            zb = zbuf[miny:maxy, minx:maxx]
            if t is None:
                vis = m & (Z < zb)
                reg[vis] = shade_rgb
                zb[vis] = Z[vis]
                continue
            U = a * uv[tri[0], 0] + b * uv[tri[1], 0] + c * uv[tri[2], 0]
            V = a * uv[tri[0], 1] + b * uv[tri[1], 1] + c * uv[tri[2], 1]
            th, tw = t.shape[:2]
            px = t[(np.clip(V, 0, .9999) * th).astype(int), (np.clip(U, 0, .9999) * tw).astype(int)]
            al = px[..., 3] * alpha
            vis = m & (Z < zb) & (al > 0.1 if kind == "cutout" else al > 0.01)
            if kind == "cutout":
                reg[vis] = px[..., :3][vis] * 0.9
                zb[vis] = Z[vis]
            elif kind == "translucent":
                reg[vis] = reg[vis] * (1 - al[vis, None]) + px[..., :3][vis] * al[vis, None]
            else:                                   # eyes: additive
                reg[vis] = np.clip(reg[vis] + px[..., :3][vis] * al[vis, None], 0, 1)

    for p, col in boxes():
        raster(p, None, np.array(col, np.float32), None, None, 1)
    order = {"cutout": 0, "translucent": 1, "eyes": 2}
    for q in sorted(quads, key=lambda q: order[q["k"]]):
        v = np.array(q["v"], float)
        raster(v[:, :3], v[:, 3:5], None, tex(q["t"]), q["k"], q["a"] / 255)
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    cls = build()
    out = os.path.join(OUT, "modes.json")
    subprocess.run(["java", "-cp", cls + os.pathsep + JOML, "Preview", out, sys.argv[1] if len(sys.argv) > 1 else "40"], check=True)
    modes = json.load(open(out))
    views = [("front", np.array([-1.6, 1.9, 3.4]), np.array([0, 1.15, 0])), ("back", np.array([1.8, 2.1, -3.3]), np.array([0, 1.2, 0])),
             ("right side", np.array([-3.8, 1.6, 0.2]), np.array([0, 1.1, 0.2]))]
    sheet = Image.new("RGB", (W * len(modes), (H + 16) * len(views)), (20, 20, 24))
    for i, (name, quads) in enumerate(modes.items()):
        for j, (label, eye, look) in enumerate(views):
            tile = Image.fromarray((np.clip(render(quads, eye, look), 0, 1) * 255).astype(np.uint8))
            ImageDraw.Draw(tile).text((6, 4), f"{name} ({label}) {len(quads)} quads", fill=(10, 10, 10))
            sheet.paste(tile, (i * W, j * (H + 16)))
    path = os.path.join(OUT, "modes.png")
    sheet.save(path)
    print("wrote", path)


if __name__ == "__main__":
    main()

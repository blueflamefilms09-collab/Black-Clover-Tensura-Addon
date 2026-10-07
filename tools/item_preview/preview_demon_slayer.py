"""
Headless preview of the 0.48 Genesis Demon-Slayer's 3D item model (client/DemonSlayerMesh.java, the real in-game mesh).

    python tools/item_preview/preview_demon_slayer.py

Compiles DemonSlayerMesh with MeshDump.java (plain javac, no Minecraft needed), dumps its quads for a few states, and rasterises
them with a z-buffer: nearest-texel textures, cutout alpha, item-style lighting, the clover alpha-blended, glows additive. The
void (in game: the crimson parallax shader) is drawn as a screen-space crimson star field, close to the shader's look.
Output: build/item_preview/demon_slayer.png (front, three-quarter, back, the bloom at resonance 4, mid-fracture).
"""
import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
MESH = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "client", "DemonSlayerMesh.java")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures")
OUT = os.path.join(ROOT, "build", "item_preview")
W = H = 360


def build():
    classes = os.path.join(OUT, "classes")
    os.makedirs(classes, exist_ok=True)
    subprocess.run(["javac", "-nowarn", "-d", classes, MESH, os.path.join(HERE, "MeshDump.java")], check=True)
    return classes


def dump(classes, res, fracture, time):
    out = subprocess.run(["java", "-cp", classes, "MeshDump", str(res), str(fracture), str(time)], check=True, capture_output=True, text=True).stdout
    quads = []
    for line in out.splitlines():
        v = line.split()
        layer = int(v[0])
        pts = np.array([float(x) for x in v[1:21]]).reshape(4, 5)
        n = np.array([float(x) for x in v[21:24]])
        quads.append((layer, pts, n, int(v[24])))
    return quads


def tex(name):
    return np.asarray(Image.open(os.path.join(TEX, name)).convert("RGBA"), np.float32) / 255


TEXTURES = {0: "entity/demon_slayer_sword.png", 2: "entity/demon_slayer_bloom.png", 3: "entity/demon_slayer_bloom_glow.png", 4: "particle/glow.png"}


def view(yaw, pitch, dist=1.9):
    target = np.array([0.5, 0.5, 0.5])
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    eye = target + dist * np.array([sy * cp, sp, cy * cp])
    f = target - eye; f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0]); r /= np.linalg.norm(r)
    u = np.cross(r, f)
    return eye, f, r, u


def project(p, cam):
    eye, f, r, u = cam
    d = p - eye
    z = d @ f
    k = (H / 2) / math.tan(math.radians(25))
    return np.stack([W / 2 + (d @ r) / z * k, H / 2 - (d @ u) / z * k, z], axis=-1)


def starfield():
    rng = np.random.default_rng(7)
    img = np.zeros((H, W, 3), np.float32)
    img[...] = [0.05, 0.0, 0.02]
    for _ in range(900):
        x, y = rng.integers(0, W), rng.integers(0, H)
        c = np.array([rng.uniform(0.5, 1.0), rng.uniform(0.02, 0.15), rng.uniform(0.05, 0.25)]) * rng.uniform(0.3, 1)
        img[y, x] += c
        if rng.random() < 0.2 and x + 1 < W:
            img[y, x + 1] += c * 0.5
    return np.clip(img, 0, 1)


def render(quads, cam):
    img = np.zeros((H, W, 3), np.float32) + np.array([0.33, 0.35, 0.42])
    zbuf = np.full((H, W), np.inf, np.float32)
    stars = starfield()
    light = np.array([0.2, 1.0, -0.7]); light /= np.linalg.norm(light)
    light2 = np.array([-0.2, 1.0, 0.7]); light2 /= np.linalg.norm(light2)
    order = [0, 1, 2, 3, 4]
    for layer in order:
        for (ly, pts, n, argb) in quads:
            if ly != layer:
                continue
            P = project(pts[:, :3], cam)
            if np.any(P[:, 2] <= 0.01):
                continue
            # back-face cull like the game (body and glows cull; the void type does not)
            area = (P[1, 0] - P[0, 0]) * (P[2, 1] - P[0, 1]) - (P[2, 0] - P[0, 0]) * (P[1, 1] - P[0, 1])
            if layer in (0, 3, 4) and area > 0:          # screen y is down, so counter-clockwise has negative area
                continue
            col = np.array([(argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255], np.float32) / 255
            shade = 0.45 + 0.35 * max(0, n @ light) + 0.2 * max(0, n @ light2)
            for tri in ((0, 1, 2), (0, 2, 3)):
                raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], layer, col, shade, stars)
    return img


def raster(img, zbuf, P, UV, layer, col, shade, stars):
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
    inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
    if not inside.any():
        return
    iz = w0 / az + w1 / bz + w2 / cz                                  # perspective-correct interpolation
    z = 1 / iz
    u = (w0 * UV[0, 0] / az + w1 * UV[1, 0] / bz + w2 * UV[2, 0] / cz) * z
    v = (w0 * UV[0, 1] / az + w1 * UV[1, 1] / bz + w2 * UV[2, 1] / cz) * z
    sub = zbuf[y0:y1 + 1, x0:x1 + 1]
    region = img[y0:y1 + 1, x0:x1 + 1]
    if layer == 1:
        m = inside & (z < sub - 1e-5)
        region[m] = stars[y0:y1 + 1, x0:x1 + 1][m]
        sub[m] = z[m]
        return
    t = tex(TEXTURES[layer])
    th, tw = t.shape[:2]
    if layer == 0:
        tu, tv = np.clip((u / 64 * tw).astype(int), 0, tw - 1), np.clip((v / 64 * th).astype(int), 0, th - 1)
    else:
        tu, tv = np.clip((u * tw).astype(int), 0, tw - 1), np.clip((v * th).astype(int), 0, th - 1)
    s = t[tv, tu]
    if layer == 0:
        m = inside & (s[..., 3] > 0.1) & (z < sub - 1e-5)
        region[m] = s[..., :3][m] * shade
        sub[m] = z[m]
    elif layer == 2:
        m = inside & (z < sub + 1e-3)
        a = (s[..., 3] * col[3])[..., None]
        region[m] = (region * (1 - a) + s[..., :3] * col[:3] * shade * a)[m]
    else:
        m = inside & (z < sub + 1e-3)
        region[m] = np.clip(region + s[..., :3] * s[..., 3:4] * col[:3], 0, 1)[m]


def main():
    classes = build()
    shots = [("front", 0, 5, 0, 0.0), ("three-quarter", 35, 20, 0, 0.0), ("back", 180, 5, 0, 0.0), ("bloom (resonance 4)", 20, 10, 4, 0.0),
             ("Black Meteorite fracture", 25, 10, 2, 1.0)]
    tiles = []
    for (label, yaw, pitch, res, fr) in shots:
        quads = dump(classes, res, fr, 37.0)
        img = render(quads, view(yaw, pitch))
        tiles.append(Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGB"))
        print(label, len(quads), "quads")
    sheet = Image.new("RGB", (W * len(tiles), H))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * W, 0))
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, "demon_slayer.png")
    sheet.save(path)
    print("wrote", path)


if __name__ == "__main__":
    sys.exit(main())

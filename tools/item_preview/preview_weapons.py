"""
Headless preview of the 0.50 3D weapons (the mesh files WeaponRenderer draws, from tools/gen_weapon_models.py).

    python tools/item_preview/preview_weapons.py [ids...]

Reads assets/nusmp/weapon_meshes/<id>.txt, places it the way the renderer does (onto the old sprite's diagonal), and rasterises
it with a z-buffer: nearest-texel skin, cutout alpha, item-style lighting, the glow layer additive, the void as a crimson star
field (in game: the Demon-Slayer's parallax shader). Each row: the old icon, front, three-quarter, edge-on, back.
Output: build/item_preview/weapons.png
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
OUT = os.path.join(ROOT, "build", "item_preview")
W = H = 220


def load(item_id):
    place, glow, quads = None, None, []
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt")):
        v = line.split()
        if not v or v[0] == "#":
            continue
        if v[0] == "place":
            place = [float(x) for x in v[1:]]
        elif v[0] == "glow":
            glow = (int(v[1], 16), float(v[2]), float(v[3]), float(v[4]))
        elif v[0] == "q":
            pts = np.array([float(x) for x in v[2:22]]).reshape(4, 5)
            quads.append((int(v[1]), pts, np.array([float(x) for x in v[22:25]])))
    ox, oy, ang, sc, ymin = place
    a = math.radians(ang - 90)
    R = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1]])
    out = []
    for (layer, pts, n) in quads:
        p = pts.copy()
        xyz = (p[:, :3] - [0, ymin, 0]) * sc
        p[:, :3] = xyz @ R.T + [ox, oy, 0.5]
        out.append((layer, p, R @ n))
    return out, glow


def tex(path):
    return np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255 if os.path.exists(path) else None


def view(yaw, pitch, dist=1.9):
    target = np.array([0.5, 0.5, 0.5])
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    eye = target + dist * np.array([sy * cp, sp, cy * cp])
    f = target - eye
    f /= np.linalg.norm(f)
    r = np.cross(f, [0, 1, 0])
    r /= np.linalg.norm(r)
    return eye, f, r, np.cross(r, f)


def project(p, cam):
    eye, f, r, u = cam
    d = p - eye
    z = d @ f
    k = (H / 2) / math.tan(math.radians(25))
    return np.stack([W / 2 + (d @ r) / z * k, H / 2 - (d @ u) / z * k, z], axis=-1)


rng = np.random.default_rng(7)
STARS = np.zeros((H, W, 3), np.float32) + [0.05, 0, 0.02]
for _ in range(500):
    x, y = rng.integers(0, W), rng.integers(0, H)
    STARS[y, x] += np.array([rng.uniform(0.5, 1), rng.uniform(0.02, 0.15), rng.uniform(0.05, 0.25)]) * rng.uniform(0.3, 1)
STARS = np.clip(STARS, 0, 1)


def render(quads, skin, glowtex, glow, cam):
    img = np.zeros((H, W, 3), np.float32) + [0.33, 0.35, 0.42]
    zbuf = np.full((H, W), np.inf, np.float32)
    l1 = np.array([0.2, 1.0, -0.7]); l1 /= np.linalg.norm(l1)
    l2 = np.array([-0.2, 1.0, 0.7]); l2 /= np.linalg.norm(l2)
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else None
    for layer in (0, 1, 2):
        if layer == 2 and (glowtex is None or tint is None):
            continue
        for (ly, pts, n) in quads:
            if ly != layer:
                continue
            P = project(pts[:, :3], cam)
            if np.any(P[:, 2] <= 0.01):
                continue
            area = (P[1, 0] - P[0, 0]) * (P[2, 1] - P[0, 1]) - (P[2, 0] - P[0, 0]) * (P[1, 1] - P[0, 1])
            if layer != 1 and area > 0:
                continue
            sh = 0.45 + 0.35 * max(0, n @ l1) + 0.2 * max(0, n @ l2)
            for tri in ((0, 1, 2), (0, 2, 3)):
                raster(img, zbuf, P[list(tri)], pts[list(tri), 3:5], layer, sh, skin if layer == 0 else glowtex, tint)
    return np.clip(img, 0, 1)


def raster(img, zbuf, P, UV, layer, sh, t, tint):
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
    z = 1 / (w0 / az + w1 / bz + w2 / cz)
    u = (w0 * UV[0, 0] / az + w1 * UV[1, 0] / bz + w2 * UV[2, 0] / cz) * z
    v = (w0 * UV[0, 1] / az + w1 * UV[1, 1] / bz + w2 * UV[2, 1] / cz) * z
    sub, region = zbuf[y0:y1 + 1, x0:x1 + 1], img[y0:y1 + 1, x0:x1 + 1]
    if layer == 1:
        m = inside & (z < sub - 1e-5)
        region[m] = STARS[y0:y1 + 1, x0:x1 + 1][m]
        sub[m] = z[m]
        return
    th, tw = t.shape[:2]
    s = t[np.clip(v.astype(int), 0, th - 1), np.clip(u.astype(int), 0, tw - 1)]
    if layer == 0:
        m = inside & (s[..., 3] > 0.1) & (z < sub - 1e-5)
        region[m] = s[..., :3][m] * sh
        sub[m] = z[m]
    else:
        m = inside & (z < sub + 1e-3)
        region[m] += (s[..., :3] * s[..., 3:4] * tint)[m]


def icon(item_id):
    im = Image.open(os.path.join(ASSETS, "textures", "item", item_id + ".png")).convert("RGBA").resize((W, H), Image.NEAREST)
    bg = Image.new("RGBA", (W, H), (84, 89, 107, 255))
    return np.asarray(Image.alpha_composite(bg, im).convert("RGB"), np.float32) / 255


def main(ids):
    if not ids:
        ids = sorted(f[:-4] for f in os.listdir(os.path.join(ASSETS, "weapon_meshes")) if f.endswith(".txt"))
    rows = []
    for item_id in ids:
        quads, glow = load(item_id)
        d = os.path.join(ASSETS, "textures", "entity", "weapon")
        skin, glowtex = tex(os.path.join(d, item_id + ".png")), tex(os.path.join(d, item_id + "_glow.png"))
        tiles = [icon(item_id)] + [render(quads, skin, glowtex, glow, view(yw, p)) for (yw, p) in ((0, 0), (35, 20), (88, 5), (180, 0))]
        rows.append(np.concatenate(tiles, 1))
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, "weapons.png")
    Image.fromarray((np.concatenate(rows, 0) * 255).astype(np.uint8)).save(out)
    print("wrote", out)


if __name__ == "__main__":
    main(sys.argv[1:])

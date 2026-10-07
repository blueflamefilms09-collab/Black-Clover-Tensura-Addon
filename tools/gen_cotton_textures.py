"""0.52: skins for Charmy's cotton sheep and cotton cloud entities (client/CottonSheepModel.java, client/CottonCloudModel.java).

    python tools/gen_cotton_textures.py

  textures/entity/cotton_sheep.png   128x128: a tall upright fluffy sheep like the anime's (cream curls, a pale long face with
                                     round pale-blue eyes, curled horns, a chef's hat and a blue neckerchief for the cooks)
  textures/entity/cotton_cloud.png   64x64:   the cotton cloud's puffs
The part tables below MUST match the models (u, v = texOffs; w, h, d = box size).
"""
import os

import numpy as np
from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity")

SHEEP = {
    "leg": (0, 0, 6, 8, 6), "arm": (30, 0, 6, 12, 6), "hips": (0, 18, 13, 8, 9), "belly": (0, 36, 15, 8, 10), "chest": (0, 55, 14, 7, 9),
    "head": (64, 0, 8, 9, 8), "crown": (64, 18, 9, 3, 9), "muzzle": (64, 31, 4, 4, 3), "ear": (64, 39, 5, 2, 1), "horn": (64, 43, 3, 3, 3),
    "hat_base": (64, 50, 8, 6, 8), "hat_puff": (64, 65, 10, 5, 10), "tie": (96, 0, 3, 6, 1), "band": (64, 82, 10, 2, 9), "hoof": (96, 24, 5, 2, 5),
}
CLOUD = {"puff_big": (0, 0, 12, 6, 12), "puff_small": (0, 20, 8, 5, 8)}

WOOL, WOOL_SHADE, SKIN = (246, 240, 222), (212, 202, 176), (228, 218, 198)


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "left": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "right": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def cells(rng, w, h, n):
    """Voronoi lumps: returns (shade 0..1, edge 0..1) per pixel, curly wool."""
    pts = np.stack([rng.uniform(0, w, n), rng.uniform(0, h, n)], 1)
    yy, xx = np.mgrid[0:h, 0:w] + 0.5
    d = np.sqrt((xx[..., None] - pts[:, 0]) ** 2 + (yy[..., None] - pts[:, 1]) ** 2)
    d.sort(axis=2)
    return np.clip(d[..., 0] / 2.6, 0, 1), np.clip(1 - (d[..., 1] - d[..., 0]) * 1.6, 0, 1)


def paint(img, part, table, fn, rng):
    for face, (fu, fv, fw, fh) in faces(*table[part]).items():
        sh, ed = cells(rng, fw, fh, max(3, fw * fh // 5))
        for j in range(fh):
            for i in range(fw):
                c = fn(face, i, j, fw, fh, sh[j, i], ed[j, i])
                if c is not None:
                    img[fv + j, fu + i] = (*np.clip(c, 0, 255), 255)


def wool(rgb=WOOL, shade=WOOL_SHADE):
    def f(face, i, j, fw, fh, sh, ed):
        c = np.array(rgb, np.float32) * (1 - sh * 0.45) + np.array(shade, np.float32) * sh * 0.45
        if ed > 0.5:
            c = c * 0.86 + np.array(shade, np.float32) * 0.14                       # the groove between two curls
        if face == "bottom":
            c = c * 0.88
        return c
    return f


def sheep():
    img = np.zeros((128, 128, 4), np.float32)
    rng = np.random.default_rng(5230)
    for part in ("hips", "belly", "chest", "crown"):
        paint(img, part, SHEEP, wool(), rng)

    def leg(face, i, j, fw, fh, sh, ed):
        if j >= 6 and face not in ("top", "bottom"):
            return np.array((150, 140, 128), np.float32)                           # the hooves
        return wool()(face, i, j, fw, fh, sh, ed)
    paint(img, "leg", SHEEP, leg, rng)

    def arm(face, i, j, fw, fh, sh, ed):
        if j >= 9 and face not in ("top", "bottom"):
            return np.array(SKIN, np.float32) * (0.95 if (i + j) % 3 == 0 else 1.0)     # hands
        return wool()(face, i, j, fw, fh, sh, ed)
    paint(img, "arm", SHEEP, arm, rng)

    def head(face, i, j, fw, fh, sh, ed):
        if face == "front":
            if i == 0 or i == fw - 1 or j == 0:
                return wool()(face, i, j, fw, fh, sh, ed)                           # wool framing the face
            c = np.array(SKIN, np.float32)
            if j in (3, 4) and (i in (1, 2) or i in (5, 6)):
                if j == 3:
                    return np.array((236, 242, 240), np.float32) if i not in (2, 5) else np.array((150, 196, 210), np.float32)   # the big pale eyes
                return np.array((150, 196, 210), np.float32) if i in (1, 6) else np.array((70, 82, 90), np.float32)            # irises / pupils
            if j == 2 and (i in (1, 2) or i in (5, 6)):
                return np.array((120, 106, 92), np.float32)                         # heavy lids
            if j == 1 and (i in (1, 2, 5, 6)):
                return np.array((200, 188, 166), np.float32)                        # soft brows
            return c
        return wool()(face, i, j, fw, fh, sh, ed)
    paint(img, "head", SHEEP, head, rng)

    def muzzle(face, i, j, fw, fh, sh, ed):
        c = np.array((236, 226, 208), np.float32)
        if face == "front" and j == 2 and i in (1, 2):
            c = np.array((110, 94, 88), np.float32)                                 # nostrils
        if face == "front" and j == 3:
            c = np.array((190, 170, 150), np.float32)                               # a gentle mouth line
        return c
    paint(img, "muzzle", SHEEP, muzzle, rng)
    paint(img, "ear", SHEEP, lambda face, i, j, fw, fh, sh, ed: np.array((236, 200, 188) if face == "front" else SKIN, np.float32), rng)

    def horn(face, i, j, fw, fh, sh, ed):
        c = np.array((196, 180, 150), np.float32)
        if (i + j) % 2 == 0:
            c = c * 0.86                                                            # ridges
        return c
    paint(img, "horn", SHEEP, horn, rng)

    def hat(face, i, j, fw, fh, sh, ed):
        c = np.array((250, 250, 252), np.float32)
        if face in ("front", "back", "left", "right") and i % 2 == 1:
            c = c * 0.93                                                            # pleats
        return c
    paint(img, "hat_base", SHEEP, hat, rng)
    paint(img, "hat_puff", SHEEP, wool((252, 252, 254), (226, 228, 236)), rng)
    paint(img, "tie", SHEEP, lambda face, i, j, fw, fh, sh, ed: np.array((70, 110, 178), np.float32) * (0.85 if j % 2 else 1.0), rng)
    paint(img, "band", SHEEP, lambda face, i, j, fw, fh, sh, ed: np.array((74, 114, 182), np.float32), rng)
    paint(img, "hoof", SHEEP, lambda face, i, j, fw, fh, sh, ed: np.array((140, 130, 118), np.float32), rng)
    return img


def cloud():
    img = np.zeros((64, 64, 4), np.float32)
    rng = np.random.default_rng(5231)
    for part in CLOUD:
        for face, (fu, fv, fw, fh) in faces(*CLOUD[part]).items():
            sh, ed = cells(rng, fw, fh, max(3, fw * fh // 6))
            for j in range(fh):
                for i in range(fw):
                    c = np.array((252, 252, 255), np.float32) * (1 - sh[j, i] * 0.06) + np.array((214, 228, 248), np.float32) * sh[j, i] * 0.12
                    if face == "bottom":
                        c = c * 0.9 + np.array((190, 206, 236), np.float32) * 0.1             # a soft blue underside
                    if rng.random() < 0.02:
                        c = np.array((255, 255, 255), np.float32)                          # a glint
                    img[fv + j, fu + i] = (*np.clip(c, 0, 255), 255)
    return img


def main():
    os.makedirs(ASSETS, exist_ok=True)
    Image.fromarray(sheep().astype(np.uint8), "RGBA").save(os.path.join(ASSETS, "cotton_sheep.png"), optimize=True)
    Image.fromarray(cloud().astype(np.uint8), "RGBA").save(os.path.join(ASSETS, "cotton_cloud.png"), optimize=True)
    print("wrote cotton_sheep, cotton_cloud")


if __name__ == "__main__":
    main()

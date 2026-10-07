"""0.52: skins for Zagred's Grimoire Daemons (client/GrimoireDaemonModel.java). Deterministic, procedural.

    python tools/gen_daemon_textures.py

  textures/entity/grimoire_daemon_{lesser,greater,arch}.png        128x128 skin (the part table below MUST match GrimoireDaemonModel)
  textures/entity/grimoire_daemon_{lesser,greater,arch}_glow.png   its emissive layer (eyes, leaf emblems, page text, rune trim)
The rule stones of "Overwrite" use the arch skin (its slab part).
"""
import math
import os

import numpy as np
from PIL import Image

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity")
S = 128

# name: (u, v, w, h, d), the same numbers as the model's texOffs / addBox sizes
PARTS = {
    "robe_top": (0, 0, 8, 8, 6), "robe_mid": (0, 16, 10, 8, 7), "robe_low": (0, 32, 12, 8, 8), "head": (0, 50, 7, 7, 7),
    "hood_tip": (40, 0, 2, 4, 2), "arm": (48, 0, 3, 9, 3), "horn": (40, 8, 2, 5, 2),
    "cover": (64, 0, 6, 12, 1), "pages": (64, 16, 10, 10, 1), "wing": (64, 32, 14, 22, 1), "slab": (0, 70, 8, 16, 3),
}

TIERS = {
    # robe, trim, hood, glow, book cover, leaves
    "lesser": dict(robe=(74, 72, 80), trim=(110, 104, 110), hood=(34, 32, 40), glow=(255, 190, 90), cover=(48, 46, 54), leaves=1, wing=None),
    "greater": dict(robe=(56, 16, 24), trim=(150, 40, 30), hood=(22, 8, 12), glow=(255, 110, 50), cover=(70, 14, 20), leaves=3, wing=None),
    "arch": dict(robe=(40, 28, 70), trim=(214, 170, 70), hood=(18, 12, 34), glow=(190, 150, 255), cover=(34, 20, 60), leaves=5, wing=(52, 30, 84)),
}


def rng_for(*k):
    return np.random.default_rng(abs(hash(k)) % (2 ** 32))


def leaf_points(n, cx, cy, r):
    if n == 1:
        return [(cx, cy)]
    return [(cx + math.cos(-math.pi / 2 + i * 2 * math.pi / n) * r, cy + math.sin(-math.pi / 2 + i * 2 * math.pi / n) * r) for i in range(n)]


def build(tier, pal):
    img = np.zeros((S, S, 4), np.float32)
    glow = np.zeros((S, S, 4), np.float32)
    r = np.random.default_rng(5200 + len(tier))

    def faces(u, v, w, h, d):
        return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "left": (u, v + d, d, h), "front": (u + d, v + d, w, h),
                "right": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

    def fill(name, fn):
        for face, (fu, fv, fw, fh) in faces(*PARTS[name]).items():
            for j in range(fh):
                for i in range(fw):
                    res = fn(face, i, j, fw, fh)
                    if res is None:
                        continue
                    c, g = res
                    if c is not None:
                        img[fv + j, fu + i] = c
                    if g is not None:
                        glow[fv + j, fu + i] = g

    def noisy(rgb, k=14):
        return np.array(list(rgb) + [255], np.float32) + np.concatenate([r.normal(0, k, 3), [0]])

    def robe(rgb, hem=False, ruins=0.0):
        def f(face, i, j, fw, fh):
            if face in ("top", "bottom"):
                return noisy(np.array(rgb) * 0.7), None
            c = noisy(rgb)
            if j % 4 == 0:
                c[:3] *= 0.82                                     # folds
            if face in ("front", "back") and i in (fw // 2,):
                c[:3] *= 1.15
            if hem and j >= fh - 2 and r.random() < 0.55:
                c[3] = 0                                          # a ragged, tattered hem
            if ruins and r.random() < ruins:
                c[3] = 0
            g = None
            if face in ("front", "back") and j == 1 and i % 2 == 0:
                g = np.array(list(pal["glow"]) + [150], np.float32)          # rune trim at the shoulder line
                c = np.array(list(pal["trim"]) + [255], np.float32)
            return c, g
        return f

    fill("robe_top", robe(pal["robe"]))
    fill("robe_mid", robe(pal["robe"], ruins=0.04))
    fill("robe_low", robe(pal["robe"], hem=True))

    def head(face, i, j, fw, fh):
        c = noisy(pal["hood"], 8)
        g = None
        if face == "front":
            if j in (3,) and i in (1, 2, 4, 5):
                return np.array(list(pal["glow"]) + [255], np.float32), np.array(list(pal["glow"]) + [255], np.float32)   # eyes
            if 2 <= j <= 5 and 1 <= i <= 5:
                c = noisy(np.array(pal["hood"]) * 0.45, 5)                                                              # the dark under the hood
        return c, g
    fill("head", head)
    fill("hood_tip", lambda face, i, j, fw, fh: (noisy(pal["hood"], 8), None))
    fill("arm", lambda face, i, j, fw, fh: (noisy(np.array(pal["robe"]) * (0.8 if j > 6 else 1.0), 10), None) if j < 7 else (noisy((120, 112, 104), 10), None))
    fill("horn", lambda face, i, j, fw, fh: (noisy((200, 184, 160) if tier != "arch" else (190, 170, 120), 10) * np.array([1, 1, 1, 1]), None))

    def cover(face, i, j, fw, fh):
        c = noisy(pal["cover"], 8)
        g = None
        if i in (0, fw - 1) or j in (0, fh - 1):
            c = np.array(list(pal["trim"]) + [255], np.float32)           # a gilt edge
        if face == "front":
            for (px, py) in leaf_points(pal["leaves"], fw / 2, fh / 2, 3.0 if pal["leaves"] > 1 else 0):
                if abs(i + 0.5 - px) < 0.9 and abs(j + 0.5 - py) < 0.9:
                    c = np.array(list(pal["glow"]) + [255], np.float32)
                    g = np.array(list(pal["glow"]) + [255], np.float32)
        return c, g
    fill("cover", cover)

    def pages(face, i, j, fw, fh):
        c = noisy((226, 214, 188), 8)
        g = None
        if face in ("front", "back") and 1 <= i <= fw - 2 and j % 2 == 1 and 1 <= j <= fh - 2 and r.random() < 0.8:
            c = noisy(np.array(pal["glow"]) * 0.7, 10)
            g = np.array(list(pal["glow"]) + [230], np.float32)           # glowing text
        return c, g
    fill("pages", pages)

    def wing(face, i, j, fw, fh):
        base = pal["wing"] or (30, 20, 50)
        c = noisy(base, 8)
        if i % 4 == 0 or j % 6 == 0:
            c = np.array(list(pal["trim"]) + [255], np.float32)           # gold veins
        if r.random() < 0.07 and j > fh * 0.6:
            c[3] = 0                                                      # a torn edge
        return c, (np.array(list(pal["glow"]) + [120], np.float32) if (i + j) % 9 == 0 and face in ("front", "back") else None)
    fill("wing", wing)

    def slab(face, i, j, fw, fh):
        c = noisy((52, 44, 76), 9)
        g = None
        if i in (0, fw - 1) or j in (0, fh - 1):
            c = np.array([120, 100, 180, 255], np.float32)
        if face in ("front", "back") and 1 <= i <= fw - 2 and 2 <= j <= fh - 3:
            if (i * 7 + j * 3) % 5 in (0, 1) and (j % 3 != 0):
                c = np.array([190, 150, 255, 255], np.float32)
                g = np.array([190, 150, 255, 255], np.float32)            # the glyph that is the rule
        return c, g
    fill("slab", slab)
    return img, glow


def main():
    os.makedirs(ASSETS, exist_ok=True)
    for tier, pal in TIERS.items():
        img, glow = build(tier, pal)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ASSETS, f"grimoire_daemon_{tier}.png"), optimize=True)
        Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ASSETS, f"grimoire_daemon_{tier}_glow.png"), optimize=True)
        print("wrote grimoire_daemon_" + tier)


if __name__ == "__main__":
    main()

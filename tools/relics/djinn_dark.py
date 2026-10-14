"""djinn_dark: the Dark Djinn's extras and the 2D art of the unique / dark looks (called by gen_djinn_unique.py).

A Dark Djinn is the same Djinn with its metal turned black and its trim to gold (references 60, 61, 62 of the owner's collage): black plate and
cloth, gold edges, a halo of black thorns edged in gold, an eye crest on the brow whose pupil keeps the Djinn's own accent colour, black rukh
feathers along the wings, black veins on the skin, a dark red-violet aura. The palette and the halo / eye builders live in djinn_unique*.py; this
module adds the rukh feathers and paints the textures:
    <id>_u_rune.png   the unique body-art (a pattern per Djinn), additive
    <id>_d_rune.png   the dark body-art (gold and accent lines), additive
    <id>_d_art.png    black veins and eye marks on the skin, alpha blended over the model
    <id>_d_bust.png   the dark Lesser Summon's bust
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                      # noqa: E402
import djinn_unique as du                  # noqa: E402
import djinn_unique_back as dub            # noqa: E402
import djinn_icons as di                   # noqa: E402

GOLD = (222, 178, 74)
AURA = (0x5A, 0x14, 0x6E)                  # the dark aura's base: a red-violet (the Java side mixes in a little of the Djinn's colour)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# ================================================================ mesh extras
def dark_extras(k):
    """Black rukh feathers hanging from the wings (two on the inner segment, four on the outer one), each edged in gold: the black wings of ref 60 on
    top of the Djinn's own wing shape."""
    for sgn in (1, -1):
        for j, (s, a, L, w) in enumerate(((0.3, -66, 7.5, 2.0), (0.75, -74, 6.5, 1.8))):
            dub.wf(k, "in", f"rukh{j}", sgn, s * du.INNER, a, L, w, 0.5, -8, "plate", 4, du.pf_feather, bz=-0.3)
        for j, (s, a, L, w) in enumerate(((0.05, -46, 14.0, 2.4), (0.25, -58, 13.0, 2.3), (0.45, -68, 11.5, 2.2), (0.68, -78, 9.0, 2.0))):
            dub.wf(k, "out", f"rukh{j}", sgn, s * du.OUTER, a, L, w, 0.5, -10, "plate", 4, du.pf_feather, bz=-0.3)
            if j % 2 == 0:
                dub.wf(k, "out", f"gold{j}", sgn, s * du.OUTER, a, L * 0.9, 0.35, 0.4, -10, "trim", 3, du.pf_taper, bz=0.12)


# ================================================================ 2D skins
BOXES = di.BOXES
faces = di.faces


def _grid(w, h):
    return np.meshgrid(np.arange(w, dtype=np.float32), np.arange(h, dtype=np.float32))


def pattern(name, w, h, face, box):
    """A 0..1 mask for a face of w x h px: the Djinn's own body-art."""
    X, Y = _grid(w, h)
    cx, cy = (w - 1) / 2, (h - 1) / 2
    m = np.zeros((h, w), np.float32)
    if name == "BAAL":                                           # lightning: a zig-zag line down each face
        zz = cx + np.where((Y % 6) < 3, (Y % 3) - 1.0, 1.0 - (Y % 3)) * 1.0
        m = (np.abs(X - zz) < 0.62).astype(np.float32)
    elif name == "AMON":                                         # flames licking up from the lower edge
        edge = h * (0.38 + 0.34 * (0.5 + 0.5 * np.sin(X * 1.9 + h)))
        m = ((Y > h - edge) & (np.abs(np.sin(X * 1.9 + h)) > 0.55) & (Y % 2 == 0)).astype(np.float32)
        m = np.maximum(m, ((Y == h - 1)).astype(np.float32) * 0.6)
    elif name == "VINEA":                                        # waves: rows of sine lines
        m = (np.abs((Y % 4) - 2 - 1.1 * np.sin(X * 1.0 + (Y // 4) * 1.7)) < 0.6).astype(np.float32)
        m *= ((Y // 4) % 2 == 0)
    elif name == "FOCALOR":                                      # swirls: concentric arcs round a point
        r = np.hypot(X - cx, Y - cy)
        m = ((np.sin(r * 1.5 + np.arctan2(Y - cy, X - cx)) > 0.78) & (r < max(w, h) * 0.9)).astype(np.float32)
    elif name == "ZAGAN":                                        # vines: a wandering line with leaf dots
        vx = cx + 1.6 * np.sin(Y * 0.8)
        m = (np.abs(X - vx) < 0.55).astype(np.float32)
        m = np.maximum(m, ((np.abs(X - vx - 1.8) < 0.6) & (Y % 5 == 1)).astype(np.float32))
        m = np.maximum(m, ((np.abs(X - vx + 1.8) < 0.6) & (Y % 5 == 3)).astype(np.float32))
    elif name == "ZEPAR":                                        # cracks: stepped angular lines
        step = (Y // 3) % 2
        m = ((np.abs(X - (cx + (step * 2 - 1) * 1.5)) < 0.6) | ((Y % 6 == 0) & (np.abs(X - cx) < 2.2))).astype(np.float32)
    elif name == "FURFUR":                                       # sound: rings spreading from the left edge
        r = np.hypot(X, Y - cy)
        m = (((r % 4.0) < 0.9) & (r < h * 0.95)).astype(np.float32)
    elif name == "MARCHOSIAS":                                   # claw marks: slashes
        m = (((np.abs(((X + Y * 0.8) % 4.5) - 1.0) < 0.55) & (np.abs(X - cx) < w * 0.45))).astype(np.float32)
    elif name == "SITRI":                                        # sunrays from the middle
        ang = np.arctan2(Y - cy, X - cx)
        r = np.hypot(X - cx, Y - cy)
        m = ((np.abs(np.sin(ang * 4.0)) > 0.94) & (r > 1.2)).astype(np.float32)
    elif name == "BIFRON":                                       # forks: a Y that repeats down the face
        k = Y % 8
        trunk = (np.abs(X - cx) < 0.55) & (k < 4)
        fork = (np.abs(np.abs(X - cx) - (k - 3)) < 0.6) & (k >= 4)
        m = (trunk | fork).astype(np.float32)
    elif name == "PAIMON":                                       # little crescents
        gx, gy = (X % 6) - 2.5, (Y % 6) - 2.5
        m = ((np.hypot(gx, gy) < 2.1) & (np.hypot(gx - 1.0, gy) > 1.7) & ((X // 6 + Y // 6) % 2 == 0)).astype(np.float32)
    elif name == "VASSAGO":                                      # scales
        gx, gy = (X % 4) - 1.5, (Y % 4) - 1.5 + 2 * ((X // 4) % 2)
        m = ((np.hypot(gx, gy) > 1.5) & (np.hypot(gx, gy) < 2.2)).astype(np.float32)
    return m


def _paste(px, bx, face, mask, col, strength=1.0):
    x, y, w, h = faces(bx)[face]
    for j in range(h):
        for i in range(w):
            if mask[j, i] > 0.05:
                a = int(min(255, 255 * mask[j, i] * strength))
                if a > px[x + i, y + j][3]:
                    px[x + i, y + j] = col + (a,)


def make_rune(dj, variant, assets):
    """The body-art overlay: eyes, brow mark and chest sigil as in djinn_icons.make_skin, plus the Djinn's own pattern on every face."""
    c, g = di.rgb(dj["color"]), di.rgb(dj["glow"])
    dark = variant == "d"
    main = mix(g, c, 0.2) if not dark else mix(GOLD, g, 0.15)
    hot = g if not dark else mix(g, (255, 255, 255), 0.35)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    for bx in BOXES:
        for face in ("front", "back", "left", "right"):
            if bx == "head" and face == "front":
                continue
            x, y, w, h = faces(bx)[face]
            msk = pattern(dj["name"], w, h, face, bx)
            if bx == "head":
                msk = msk * 0.55
            _paste(px, bx, face, msk, main, 0.85 if bx != "head" else 0.7)
    hx, hy = faces("head")["front"][0], faces("head")["front"][1]
    for xx in (1, 2, 5, 6):
        px[hx + xx, hy + 4] = (255, 255, 255, 255) if xx in (2, 5) else hot + (255,)
    px[hx + 3, hy + 1] = hot + (255,)
    px[hx + 4, hy + 1] = hot + (255,)
    if dark:
        for xx in (2, 5):
            px[hx + xx, hy + 2] = hot + (200,)
    x, y, w, h = faces("body")["front"]
    cx, cy = x + w // 2, y + 5
    for a in range(0, 360, 30):
        px[cx + int(round(2.4 * math.cos(math.radians(a)))), cy + int(round(2.4 * math.sin(math.radians(a))))] = hot + (255,)
    px[cx, cy] = (255, 255, 255, 255)
    out = os.path.join(assets, "textures", "entity", "djinn")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, f"{dj['id']}_{variant}_rune.png"))


def make_dark_art(dj, assets):
    """Black veins spreading over the body, the arms and the legs, black marks under the eyes (ref 60: the half-black face) and at the temples;
    alpha blended over the skin (entityTranslucent), so the skin shows between the veins."""
    rng = np.random.default_rng(sum(map(ord, dj["id"])) * 31)
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = im.load()
    BLK, GLD = (10, 8, 14), (176, 138, 60)

    def put(x, y, col, a):
        if 0 <= x < 64 and 0 <= y < 64 and a > px[x, y][3]:
            px[x, y] = col + (int(a),)
    for bx in BOXES:
        for face in ("front", "back", "left", "right"):
            x, y, w, h = faces(bx)[face]
            for v in range(max(2, w // 2)):
                cur = x + int(rng.integers(0, w))
                for j in range(h):
                    cur = max(x, min(x + w - 1, cur + int(rng.integers(-1, 2))))
                    put(cur, y + j, BLK, 215 if (j % 7) else 120)
                    if j % 5 == 2:
                        put(cur + 1, y + j, BLK, 150)
            if bx != "head":
                for j in range(2, h, 5):
                    put(x + int(rng.integers(0, w)), y + j, GLD, 150)
    hx, hy = faces("head")["front"][0], faces("head")["front"][1]
    for sgn_x in (1, 6):
        for j in range(5, 8):
            put(hx + sgn_x, hy + j, BLK, 235)
    for xx in range(1, 7):
        put(hx + xx, hy + 3, BLK, 150)
    for xx in (0, 7):
        for j in range(2, 6):
            put(hx + xx, hy + j, BLK, 180)
    for face in ("left", "right"):
        x, y, w, h = faces("head")[face]
        for j in range(3, 7):
            put(x + w // 2, y + j, BLK, 220)
    out = os.path.join(assets, "textures", "entity", "djinn")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, f"{dj['id']}_d_art.png"))


def make_dark_bust(dj, assets):
    """The Lesser Summon's bust for the dark look: djinn_icons.make_bust drawn with a black body and gold horns, plus an eye crest in the accent."""
    c = (28, 24, 38)
    d2 = dict(dj, id=dj["id"] + "_d", color=(c[0] << 16) | (c[1] << 8) | c[2], glow=(GOLD[0] << 16) | (GOLD[1] << 8) | GOLD[2])
    di.make_bust(d2, assets)
    path = os.path.join(assets, "textures", "entity", "djinn", dj["id"] + "_d_bust.png")
    im = Image.open(path).convert("RGBA")
    d = ImageDraw.Draw(im)
    g = di.rgb(dj["glow"])
    d.polygon([(16, 7), (11, 9), (8, 8), (13, 6)], fill=(12, 10, 16, 255))
    d.polygon([(16, 7), (21, 9), (24, 8), (19, 6)], fill=(12, 10, 16, 255))
    d.ellipse([14, 7, 18, 10], fill=(240, 232, 210, 255), outline=GOLD + (255,))
    d.rectangle([15, 8, 17, 9], fill=g + (255,))
    d.ellipse([8, 0, 24, 4], outline=GOLD + (255,))
    for sg in (-1, 1):
        d.polygon([(16 + sg * 8, 21), (16 + sg * 15, 12), (16 + sg * 14, 24), (16 + sg * 10, 26)], fill=(14, 12, 20, 180))
    im.save(path)


def make_skins(dj, variant, assets):
    make_rune(dj, variant, assets)
    if variant == "d":
        make_dark_art(dj, assets)
        make_dark_bust(dj, assets)

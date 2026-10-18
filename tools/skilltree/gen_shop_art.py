#!/usr/bin/env python3
"""Why: the Black Market screen has two pictures that are not drawn with rectangles: the old witch's portrait and the cobwebs in the corners of her panel (the owner asked for a
"hidden, dusty shop run by an old witch", and the screen's whole look is that room). Both are generated, not hand-painted, so they can be rebuilt and checked:

  * assets/nusmp/textures/gui/black_market/cobwebs.png   96x96, four 48x48 corner webs (top-left, top-right, bottom-left, bottom-right), pale threads at low alpha, drawn
    deterministically (radial threads and sagging rings, a few loose strands); no randomness except a fixed seed;
  * assets/nusmp/textures/gui/black_market/witch.png     40x42, the witch's bust: her REAL model (tools/arc/char_black_market_witch.py, built by tools/gen_arc_characters.py)
    drawn by the repo's own headless renderer (tools/render_preview, the same code the game's GeoDraw runs), cut out of the renderer's sky by a flood fill of the smooth gradient, and
    reduced to the size the screen draws it at. Using the model keeps the portrait in step with the 3D figure by construction: change her look and run this again.

    python3 -B tools/skilltree/gen_shop_art.py                    # both (the portrait needs a JDK 21 and the render harness, see tools/render_preview/preview.py)
    python3 -B tools/skilltree/gen_shop_art.py --webs-only        # only the cobwebs (pure Pillow)
    python3 -B tools/skilltree/gen_shop_art.py --portrait-from render.png   # use a finished cut-out render (RGBA, 340x360) instead of calling the harness

Needs Python 3 with numpy and Pillow. Deterministic: the same inputs give the same bytes.
"""
import json
import math
import os
import random
import subprocess
import sys
import tempfile
from collections import deque

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "black_market")
RENDER = os.path.join(ROOT, "tools", "render_preview", "preview.py")

PORTRAIT_W, PORTRAIT_H = 40, 42
TILE_W, TILE_H = 340, 404                  # the render tile; the top and bottom strips carry the harness's labels and are cropped away
CROP = (0, 24, TILE_W, 24 + round(TILE_W * PORTRAIT_H / PORTRAIT_W))   # the portrait's own aspect, below the top-left label and above the bottom-left one
SHEET_OFFSET = (6, 36)                     # where the harness puts the tile in its sheet (margin 6, header 30 + margin 6)
VIEW = {"name": "portrait", "yaw": 14, "pitch": 4, "target": [-0.03, 1.45, 0], "dist": 2.9}


# ------------------------------------------------------------------------------------------------------------------------ the cobwebs
def web_corner(size=48):
    """One web hanging in the top-left corner of a size x size tile: threads fan out from the corner, rings sag between them, a few loose strands dangle."""
    rng = random.Random(1887)
    img = np.zeros((size, size, 4), np.uint8)
    thread = (222, 214, 236)

    def put(x, y, a):
        if 0 <= x < size and 0 <= y < size:
            old = img[y, x, 3]
            if a > old:
                img[y, x] = (thread[0], thread[1], thread[2], a)

    def line(x0, y0, x1, y1, a0, a1):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(n + 1):
            t = i / max(1, n)
            put(int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), int(a0 + (a1 - a0) * t))

    angles = [13, 31, 50, 69, 85]                                          # degrees from the top edge towards the left edge (not hugging the edges)
    reach = [size - 2, size - 6, size - 2, size - 7, size - 3]
    for a, r in zip(angles, reach):
        rad = math.radians(a)
        line(0, 0, math.cos(rad) * r, math.sin(rad) * r, 120, 24)          # the radial threads fade with distance
    for ring, r in enumerate((8, 16, 24, 33, 42)):
        for k in range(len(angles) - 1):
            if ring >= 3 and rng.random() < 0.38:                          # the outer rings are torn here and there
                continue
            a0, a1 = math.radians(angles[k]), math.radians(angles[k + 1])
            p0 = (math.cos(a0) * r, math.sin(a0) * r)
            p1 = (math.cos(a1) * r, math.sin(a1) * r)
            mid = ((p0[0] + p1[0]) / 2 * 0.80, (p0[1] + p1[1]) / 2 * 0.80)  # the thread sags towards the corner
            prev = p0
            alpha = int(96 - ring * 14)
            for i in range(1, 9):
                t = i / 8.0
                x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * mid[0] + t * t * p1[0]
                y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * mid[1] + t * t * p1[1]
                line(prev[0], prev[1], x, y, alpha, alpha)
                prev = (x, y)
    for _ in range(4):                                                     # loose strands that hang off the web
        a = math.radians(rng.uniform(4, 86))
        r0 = rng.uniform(26, 40)
        x0, y0 = math.cos(a) * r0, math.sin(a) * r0
        line(x0, y0, x0 + rng.uniform(-2, 5), y0 + rng.uniform(3, 8), 70, 12)
    return Image.fromarray(img, "RGBA")


def webs_sheet():
    tl = web_corner(48)
    sheet = Image.new("RGBA", (96, 96), (0, 0, 0, 0))
    sheet.paste(tl, (0, 0))
    sheet.paste(tl.transpose(Image.FLIP_LEFT_RIGHT), (48, 0))
    sheet.paste(tl.transpose(Image.FLIP_TOP_BOTTOM), (0, 48))
    sheet.paste(tl.transpose(Image.ROTATE_180), (48, 48))
    return sheet


# ------------------------------------------------------------------------------------------------------------------------ the portrait
def cut_sky(sheet_path, tol=7):
    """The tile of the harness's sheet, cropped to the portrait window, with the sky (a smooth gradient reached from the border) made transparent."""
    sheet = Image.open(sheet_path).convert("RGB")
    ox, oy = SHEET_OFFSET
    a = np.asarray(sheet.crop((ox, oy, ox + TILE_W, oy + TILE_H))).astype(np.int32)[CROP[1]:CROP[3], CROP[0]:CROP[2]]
    h, w, _ = a.shape
    sky = np.zeros((h, w), bool)
    q = deque()
    for x in range(w):
        for y in (0, h - 1):
            sky[y, x] = True
            q.append((y, x))
    for y in range(h):
        for x in (0, w - 1):
            if not sky[y, x]:
                sky[y, x] = True
                q.append((y, x))
    while q:
        y, x = q.popleft()
        c = a[y, x]
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and not sky[ny, nx] and np.abs(a[ny, nx] - c).sum() <= tol:
                sky[ny, nx] = True
                q.append((ny, nx))
    # specks of sky the fill could not reach (inside a loop of the staff, between hair and brim) are cleaned by a small opening of the mask
    alpha = np.where(sky, 0, 255).astype(np.uint8)
    return Image.fromarray(np.dstack([a.astype(np.uint8), alpha]), "RGBA")


def drop_specks(alpha, smallest):
    """Removes the islands of fewer than `smallest` pixels (4-connected) from an alpha plane: the loose tatters of a ragged hem that would only read as dirt at this size."""
    h, w = alpha.shape
    seen = np.zeros((h, w), bool)
    out = alpha.copy()
    for y0 in range(h):
        for x0 in range(w):
            if seen[y0, x0] or alpha[y0, x0] <= 0.5:
                continue
            comp = [(y0, x0)]
            seen[y0, x0] = True
            i = 0
            while i < len(comp):
                y, x = comp[i]
                i += 1
                for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx] and alpha[ny, nx] > 0.5:
                        seen[ny, nx] = True
                        comp.append((ny, nx))
            if len(comp) < smallest:
                for y, x in comp:
                    out[y, x] = 0.0
    return out


def reduce(cut):
    """The cut-out at the size the screen draws it: area-averaged in premultiplied alpha (no dark fringe), lifted and sharpened so the face reads at 40x42 on the dark panel,
    and given a one-pixel dark outline, the way the sprites of this mod are drawn."""
    from PIL import ImageFilter
    arr = np.asarray(cut.convert("RGBA")).astype(np.float64) / 255.0
    pre = arr[..., :3] * arr[..., 3:4]
    img = Image.fromarray((np.dstack([pre, arr[..., 3:4]]) * 255 + 0.5).astype(np.uint8), "RGBA").resize((PORTRAIT_W, PORTRAIT_H), Image.BOX)
    p = np.asarray(img).astype(np.float64) / 255.0
    a = p[..., 3]
    rgb = np.where(a[..., None] > 0.02, p[..., :3] / np.maximum(a[..., None], 1e-6), 0.0)
    rgb = np.clip(rgb, 0, 1) ** 0.74                                         # the preview light is darker than the panel's: lift the shadows
    lum = (rgb * np.array([0.2126, 0.7152, 0.0722])).sum(axis=2, keepdims=True)
    rgb = np.clip(lum + (rgb - lum) * 1.22, 0, 1)                            # a little more colour: plum, amber and olive are what make her
    rgb_img = Image.fromarray((rgb * 255 + 0.5).astype(np.uint8), "RGB").filter(ImageFilter.UnsharpMask(radius=0.9, percent=70, threshold=1))
    rgb = np.asarray(rgb_img).astype(np.float64) / 255.0
    alpha = np.clip((a - 0.12) / 0.55, 0, 1)                                 # edge pixels that are mostly sky become clean, not smeared
    alpha = drop_specks(alpha, 4)
    inside = alpha > 0.5
    grown = inside.copy()
    grown[1:, :] |= inside[:-1, :]
    grown[:-1, :] |= inside[1:, :]
    grown[:, 1:] |= inside[:, :-1]
    grown[:, :-1] |= inside[:, 1:]
    edge = grown & ~inside
    out = np.dstack([rgb, alpha])
    out[edge] = (0.05, 0.03, 0.08, 0.92)                                     # the outline
    return Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")


def render_portrait():
    """Runs the repo's render harness on a one-view scene of the witch and returns the path of its sheet."""
    build = os.path.join(ROOT, "build", "shop_art")
    os.makedirs(build, exist_ok=True)
    scene = {"shop_portrait": {"kind": "geo", "model": "arc_black_market_witch", "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": [0],
                               "tile": [TILE_W, TILE_H], "floor": False, "figure": False, "expect": [], "anim": "idle", "views": [VIEW]}}
    scenes = os.path.join(build, "scenes.json")
    with open(scenes, "w") as f:
        json.dump(scene, f, indent=1)
    env = dict(os.environ, RENDER_PREVIEW_BUILD=build)
    subprocess.run([sys.executable, "-B", RENDER, "--ssaa", "3", "--scenes", scenes, "shop_portrait"], check=True, env=env)
    return os.path.join(build, "shop_portrait.png")


def main(argv):
    os.makedirs(OUT, exist_ok=True)
    webs_only = "--webs-only" in argv
    webs_sheet().save(os.path.join(OUT, "cobwebs.png"))
    print("wrote cobwebs.png")
    if webs_only:
        return 0
    if "--portrait-from" in argv:
        cut = Image.open(argv[argv.index("--portrait-from") + 1]).convert("RGBA")
    else:
        cut = cut_sky(render_portrait())
    reduce(cut).save(os.path.join(OUT, "witch.png"))
    print("wrote witch.png")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

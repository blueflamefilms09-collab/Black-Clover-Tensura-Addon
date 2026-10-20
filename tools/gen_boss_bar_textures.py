#!/usr/bin/env python3
"""
Textures and style data of the custom boss bars (client/gui/bossbar/Custom*, docs/boss_bars.md). Original art, deterministic: running it again
rewrites byte-identical files.

    assets/nusmp/textures/gui/boss_bar/frame_<style>.png   512x268 atlas per style: three layers of 512x84 (= the 256x42 widget at 2x)
                                                           y=0 back (track, name banner, socket disc), y=92 frame (opaque ornaments, window and
                                                           socket cut out), y=184 glow (white + alpha, tinted by the game and drawn additively)
    assets/nusmp/textures/gui/boss_bar/frame_<style>.png.mcmeta   blur on (the 2x art is drawn at half size)
    assets/nusmp/textures/gui/boss_bar/emblems.png          256x160, 32x32 emblems, 8 per row (+ .mcmeta)
    assets/nusmp/textures/gui/boss_bar/fillfx.png           64x(16 x 13) tileable overlays of the fill: one row per effect of styles.FX
    assets/nusmp/textures/gui/boss_bar/chains.png           64x16 tileable chains, drawn over the bar while the boss cannot be hurt
    assets/nusmp/boss_bars/<style>.json                     the data of each style (with --json; the committed files are the source of truth, edit them by hand)
    assets/nusmp/boss_bars/_index.json                      style ids, emblem names, effect names, the layout numbers (a JUnit test compares them with the Java side)

    python3 tools/gen_boss_bar_textures.py            # textures + _index.json
    python3 tools/gen_boss_bar_textures.py --json     # also (re)write the style json files
    python3 tools/gen_boss_bar_textures.py --only holy,evil
"""
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "boss_bar_art"))

import emblems as EM          # noqa: E402
import lib                    # noqa: E402
import motifs as MO           # noqa: E402
from styles import STYLES, FX  # noqa: E402

ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
TEX = os.path.join(ASSETS, "textures", "gui", "boss_bar")
DATA = os.path.join(ASSETS, "boss_bars")

LAYER_H = lib.H * lib.OUT_SCALE                    # 84
LAYER_Y = [0, 92, 184]                              # back, frame, glow
ATLAS_W, ATLAS_H = lib.W * lib.OUT_SCALE, 268
FX_ROW_H, FX_W = 16, 64
MCMETA_BLUR = '{\n  "texture": {\n    "blur": true\n  }\n}\n'


def pal_of(style):
    return {k: lib.rgb(v) for k, v in style["pal"].items()}


def write(path, data, mode="w"):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, mode) as f:
        f.write(data)


# ================================================================ frames
def frame_atlas(style):
    x = MO.Ctx(pal_of(style), style["seed"] * 1000 + sum(map(ord, style["id"])))
    MO.base(x)
    for m in style["motifs"]:
        MO.MOTIFS[m](x)
    MO.finish(x)
    atlas = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    for y, layer in zip(LAYER_Y, (x.back, x.front, x.glow)):
        atlas.alpha_composite(layer.final(), (0, y))
    return atlas


# ================================================================ fill effects (white + alpha, 64x16 tiles, tileable)
def fx_tile(name):
    rnd = random.Random(sum(map(ord, name)) * 31)
    im = Image.new("RGBA", (FX_W, FX_ROW_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    a = np.zeros((FX_ROW_H, FX_W), float)

    def put(x, y, v):
        for dx in (-FX_W, 0, FX_W):
            xx = int(round(x + dx))
            if 0 <= xx < FX_W and 0 <= y < FX_ROW_H:
                a[int(y), xx] = max(a[int(y), xx], v)

    def blob(cx, cy, rx, ry, v):
        for yy in range(FX_ROW_H):
            for xx in range(-8, FX_W + 8):
                dd = ((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2
                if dd < 1:
                    put(xx, yy, v * (1 - dd) ** 1.2)

    if name == "shimmer":
        for yy in range(FX_ROW_H):
            for xx in range(FX_W):
                u = ((xx + yy * 0.9) % FX_W) / FX_W
                a[yy, xx] = max(0, 1 - abs(u - 0.5) * 7) ** 1.5 * 0.9
    elif name == "drip":
        for _ in range(7):
            x0, ln = rnd.randint(0, FX_W - 1), rnd.randint(5, 15)
            for yy in range(ln):
                put(x0, yy, 0.8 - yy / ln * 0.3)
                if yy < 3:
                    put(x0 + 1, yy, 0.4)
            blob(x0, ln, 1.4, 1.8, 0.9)
    elif name == "embers":
        for _ in range(26):
            blob(rnd.uniform(0, FX_W), rnd.uniform(1, 15), rnd.uniform(0.8, 1.8), rnd.uniform(0.8, 1.8), rnd.uniform(0.5, 1.0))
    elif name == "wave":
        for k, (ph, amp) in enumerate(((0, 2.4), (1.7, 1.8))):
            for xx in range(FX_W):
                y = 8 + math.sin((xx / FX_W) * 2 * math.pi * 2 + ph) * amp + (k * 3 - 2)
                put(xx, y, 0.8)
                put(xx, y + 1, 0.4)
    elif name == "sparks":
        for _ in range(5):
            x0 = rnd.randint(0, FX_W - 1)
            y = rnd.randint(0, 3)
            while y < FX_ROW_H:
                put(x0, y, 0.95)
                put(x0 + 1, y, 0.5)
                x0 += rnd.choice((-1, 1))
                y += 1
    elif name == "frost":
        for _ in range(9):
            cx, cy, s = rnd.uniform(0, FX_W), rnd.uniform(2, 14), rnd.uniform(1.5, 3.5)
            for t in range(-int(s), int(s) + 1):
                put(cx + t, cy, 0.8 - abs(t) / (s + 1) * 0.5)
                put(cx, cy + t, 0.8 - abs(t) / (s + 1) * 0.5)
        for _ in range(30):
            put(rnd.randint(0, FX_W - 1), rnd.randint(0, FX_ROW_H - 1), 0.35)
    elif name == "souls":
        for _ in range(6):
            blob(rnd.uniform(0, FX_W), rnd.uniform(4, 12), rnd.uniform(1.6, 2.6), rnd.uniform(3.5, 6), rnd.uniform(0.5, 0.85))
    elif name == "leaves":
        for _ in range(8):
            cx, cy = rnd.uniform(0, FX_W), rnd.uniform(3, 13)
            ang = rnd.uniform(0, math.pi)
            for t in np.linspace(-1, 1, 14):
                put(cx + math.cos(ang) * t * 3.2, cy + math.sin(ang) * t * 3.2, 0.85 - abs(t) * 0.3)
            blob(cx, cy, 1.2, 1.2, 0.6)
    elif name == "static":
        for yy in range(FX_ROW_H):
            for xx in range(FX_W):
                a[yy, xx] = rnd.random() ** 3 * 0.9
    elif name == "glitch":
        for _ in range(8):
            y0, x0, ln = rnd.randint(0, FX_ROW_H - 1), rnd.randint(0, FX_W - 1), rnd.randint(5, 22)
            for t in range(ln):
                put(x0 + t, y0, 0.85)
    elif name == "veins":
        for _ in range(4):
            x0, y0 = rnd.uniform(0, FX_W), rnd.uniform(0, FX_ROW_H)
            ang = rnd.uniform(0, 6.28)
            for _ in range(22):
                put(x0, y0, 0.8)
                ang += rnd.uniform(-0.6, 0.6)
                x0 += math.cos(ang)
                y0 = min(FX_ROW_H - 1, max(0, y0 + math.sin(ang)))
    elif name == "runes":
        for k in range(4):
            cx, cy = 8 + k * 16, 8
            for _ in range(3):
                x1, y1 = cx + rnd.randint(-3, 3), cy + rnd.randint(-5, 5)
                n = 8
                for t in range(n + 1):
                    put(cx + (x1 - cx) * t / n, cy + (y1 - cy) * t / n, 0.8)
    elif name == "shield":
        for yy in range(FX_ROW_H):
            for xx in range(FX_W):
                hx = (xx % 8) - 4
                hy = (yy % 6) - 3
                edge = max(abs(hy) * 1.15, abs(hx) * 0.58 + abs(hy) * 0.58)
                a[yy, xx] = 0.8 if edge > 2.6 else 0.12
    elif name == "chains":
        big = Image.new("L", (FX_W * 4, FX_ROW_H * 4), 0)
        bd = ImageDraw.Draw(big)
        for k in range(FX_W // 16):
            for cx, rx in ((k * 16 + 4, 5.2), (k * 16 + 12, 2.4)):
                for dx in (-FX_W, 0, FX_W):
                    bd.ellipse([(cx + dx - rx) * 4, (8 - 3.6) * 4, (cx + dx + rx) * 4, (8 + 3.6) * 4], outline=255, width=6)
            bd.line([(k * 16 + 7, 8 * 4 / 4), (k * 16 + 9.5, 8)], fill=0)
        small = np.array(big.resize((FX_W, FX_ROW_H), Image.BOX), float) / 255.0
        a = np.clip(small * 1.6, 0, 1)
    arr = np.zeros((FX_ROW_H, FX_W, 4), np.uint8)
    arr[..., :3] = 255
    arr[..., 3] = np.clip(a * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


# ================================================================ json
def style_json(s):
    p = s["pal"]
    return {
        "id": s["id"], "family": s["family"], "emblem": s["emblem"], "fx": s["fx"], "entrance": s["entrance"], "death": s["death"],
        "colors": {
            "fill_top": s["fill"][0].upper(), "fill_bottom": s["fill"][1].upper(), "glow": p["gl"].upper(), "accent": p["ac"].upper(), "ghost": "#FFFFFF",
            "name": s["name"].upper(), "title": s["title"].upper(), "shadow": p["dk"].upper(), "frame": "#FFFFFF", "shield": s["shield"].upper(),
            "invulnerable": "#9CD8FF", "cast": p["ac"].upper(), "lair": s["lair"].upper(), "marker": p["hi"].upper(),
        },
        "shimmer": {"speed": 1.0, "strength": 0.5},
    }


def index_json():
    return {
        "version": 1,
        "styles": [s["id"] for s in STYLES],
        "emblems": EM.NAMES,
        "fx": FX,
        "layout": {
            "width": lib.W, "height": lib.H, "window": list(lib.WIN), "socket": list(lib.SOCK), "plate": list(lib.PLATE),
            "atlas_w": ATLAS_W, "atlas_h": ATLAS_H, "layer_y": LAYER_Y, "layer_h": LAYER_H,
            "emblem_cols": EM.COLS, "emblem_rows": EM.ROWS, "emblem_cell": 32,
            "fx_w": FX_W, "fx_row_h": FX_ROW_H,
        },
    }


def main(argv):
    only = None
    if "--only" in argv:
        only = set(argv[argv.index("--only") + 1].split(","))
    want_json = "--json" in argv
    for s in STYLES:
        if only and s["id"] not in only:
            continue
        atlas = frame_atlas(s)
        path = os.path.join(TEX, "frame_%s.png" % s["id"])
        os.makedirs(TEX, exist_ok=True)
        lib.save_png(atlas, path)
        write(path + ".mcmeta", MCMETA_BLUR)
        if want_json:
            write(os.path.join(DATA, s["id"] + ".json"), json.dumps(style_json(s), indent=2) + "\n")
    if not only:
        # emblems: each is drawn with the palette of the first style that uses it
        sheet = Image.new("RGBA", (EM.COLS * 32, EM.ROWS * 32), (0, 0, 0, 0))
        owner = {}
        for s in STYLES:
            owner.setdefault(s["emblem"], s)
        for i, name in enumerate(EM.NAMES):
            st = owner.get(name) or STYLES[0]
            sheet.alpha_composite(EM.draw(name, pal_of(st)), ((i % EM.COLS) * 32, (i // EM.COLS) * 32))
        lib.save_png(sheet, os.path.join(TEX, "emblems.png"))
        write(os.path.join(TEX, "emblems.png.mcmeta"), MCMETA_BLUR)
        fxs = Image.new("RGBA", (FX_W, FX_ROW_H * len(FX)), (0, 0, 0, 0))
        for i, name in enumerate(FX):
            fxs.alpha_composite(fx_tile(name), (0, i * FX_ROW_H))
        lib.save_png(fxs, os.path.join(TEX, "fillfx.png"))
        lib.save_png(fx_tile("chains"), os.path.join(TEX, "chains.png"))
        write(os.path.join(DATA, "_index.json"), json.dumps(index_json(), indent=2) + "\n")
    print("done: %d styles" % (len(STYLES) if not only else len(only)))


if __name__ == "__main__":
    main(sys.argv[1:])

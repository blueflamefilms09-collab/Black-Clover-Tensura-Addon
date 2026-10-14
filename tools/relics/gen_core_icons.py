#!/usr/bin/env python3
"""The 16x16 icons of the Spirit Cores and Devil Cores (relics/socket) and their item models. Idempotent.

    python3 tools/relics/gen_core_icons.py            write 15 PNGs + 15 models
    python3 tools/relics/gen_core_icons.py --sheet out.png   also an 8x scaled contact sheet

What it writes (assets/nusmp):
    textures/item/spirit_core_<element>.png, devil_core_<devil>.png     16x16, drawn from the colours in the Java catalog
    models/item/<same>.json                                             parent item/generated
The colours are PARSED from relics/socket/SocketCatalog.java and SocketElement.java, so the Java table stays the single source: a new socket added
there gets its icon the next time this runs. A Spirit Core is a round glass orb with a bright core and a glint; a Devil Core is a cut gem with a
dark rim and a slit of the devil's colour, so the two read apart at a glance in a hotbar.
"""
import argparse
import json
import math
import os
import re

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
SOCK = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "relics", "socket")


def table():
    el = open(os.path.join(SOCK, "SocketElement.java")).read()
    rgb = {m.group(1): int(m.group(2), 16) for m in re.finditer(r"^\s{4}([A-Z]+)\(0x([0-9A-Fa-f]{6}),", el, re.M)}
    cat = open(os.path.join(SOCK, "SocketCatalog.java")).read()
    out = []
    for m in re.finditer(r'spirit\("(\w+)", "[^"]+", SocketElement\.(\w+),', cat):
        out.append(("spirit_core_" + m.group(1), rgb[m.group(2)], False))
    for m in re.finditer(r'devil\("(\w+)", "[^"]+", SocketElement\.\w+, 0x([0-9A-Fa-f]{6}),', cat):
        out.append(("devil_core_" + m.group(1), int(m.group(2), 16), True))
    if len(out) != 15:
        raise SystemExit("expected 15 cores, parsed %d" % len(out))
    return out


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(rgb, k):
    return tuple(max(0, min(255, int(c * k))) for c in rgb)


def orb(rgb):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    base = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d > 6.6:
                continue
            if d > 5.6:
                px[x, y] = shade(base, 0.35) + (255,)
                continue
            t = d / 5.6
            lit = mix((255, 255, 255), base, min(1.0, 0.35 + t * 0.9))
            lit = shade(lit, 1.0 - 0.35 * max(0.0, (y - cy) / 6.0))      # darker toward the bottom
            px[x, y] = lit + (255,)
    for (x, y) in ((5, 4), (6, 4), (5, 5)):                              # the glint
        px[x, y] = (255, 255, 255, 255)
    return img


def gem(rgb):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    base = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
    dark = shade(base, 0.22)
    for y in range(16):
        half = 7 - abs(y - 7.5) * 0.9              # a diamond, wider in the middle
        for x in range(16):
            dx = abs(x - 7.5)
            if dx > half:
                continue
            if dx > half - 1.1 or y in (1, 14):
                px[x, y] = shade(dark, 0.7) + (255,)
            else:
                k = 0.45 + 0.5 * (1 - dx / max(half, 1.0))
                px[x, y] = shade(mix((20, 8, 28), base, 0.55), k) + (255,)
    for y in range(4, 12):                           # the slit
        px[7, y] = base + (255,)
        px[8, y] = shade(base, 1.2) + (255,)
    px[7, 7] = px[8, 7] = (255, 255, 255, 255)
    for (x, y) in ((4, 5), (5, 4)):
        px[x, y] = shade(base, 1.25) + (255,)
    return img


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sheet")
    args = ap.parse_args()
    tdir = os.path.join(ASSETS, "textures", "item")
    mdir = os.path.join(ASSETS, "models", "item")
    os.makedirs(tdir, exist_ok=True)
    os.makedirs(mdir, exist_ok=True)
    sheet = Image.new("RGBA", (16 * 8 * 5, 16 * 8 * 3), (40, 40, 48, 255))
    for i, (name, rgb, devil) in enumerate(table()):
        img = gem(rgb) if devil else orb(rgb)
        img.save(os.path.join(tdir, name + ".png"), optimize=True)
        with open(os.path.join(mdir, name + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + name}}, f, indent=2)
            f.write("\n")
        big = img.resize((128, 128), Image.NEAREST)
        sheet.paste(big, ((i % 5) * 128, (i // 5) * 128), big)
    if args.sheet:
        sheet.save(args.sheet)
    print("wrote 15 core icons and models")


if __name__ == "__main__":
    main()

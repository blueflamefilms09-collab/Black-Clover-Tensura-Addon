#!/usr/bin/env python3
"""Generates the assets of the Lockdown Barrier (1.107.0): the amber wall of a locked room.

The trial hall's Magic Barrier (tools/gen_labyrinth_trial_assets.py) is a cyan hexagon lattice; this is its sister in amber, with a different pattern so the two read
apart at a glance (a trial hall seals itself with cyan hexagons; a room that locked behind the party seals with amber diamonds and chevrons that crawl upwards):
a 16x16 translucent sheet of four frames, a block model (cube_all, translucent), a blockstate and an item model. Deterministic (no randomness), so
`python3 -B tools/gen_lockdown_assets.py --check` can verify the committed files.

Usage: python3 -B tools/gen_lockdown_assets.py [--check]
"""
import io
import json
import math
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
N = 16
FRAMES = 4
AMBER = (255, 176, 40)
AMBER_HOT = (255, 226, 140)
EMBER = (255, 96, 24)


def sheet_frames():
    """Four frames of an amber sheet: a diamond lattice (period 8, so cells tile), nodes at the crossings, and chevrons of heat that climb the sheet."""
    out = Image.new("RGBA", (N, N * FRAMES))
    for f in range(FRAMES):
        img = Image.new("RGBA", (N, N), AMBER + (84,))
        d = ImageDraw.Draw(img)
        for ox in (-8, 0, 8):
            for oy in (-8, 0, 8):
                diamond = [(ox + 4, oy + 0), (ox + 8, oy + 4), (ox + 4, oy + 8), (ox + 0, oy + 4)]
                d.line(diamond + [diamond[0]], fill=AMBER_HOT + (210,), width=1)
        px = img.load()
        for y in range(N):
            for x in range(N):
                # a chevron band that climbs one row per frame-step: the sheet looks like it is rising
                band = (y + abs(x - 8) // 2 + f * 4) % 16
                if band < 2:
                    r, g, b, a = px[x, y]
                    px[x, y] = (min(255, r + 30), min(255, g + 40), min(255, b + 40), min(255, a + 70))
        for ox in (0, 8):
            for oy in (0, 8):
                for vx, vy in ((4, 0), (8, 4), (4, 8), (0, 4)):
                    x, y = (vx + ox) % N, (vy + oy) % N
                    px[x, y] = EMBER + (238,)                                    # the nodes of the lattice glow hotter
        # a one-pixel frame so two neighbouring sheets read as separate wall segments when seen from the side
        for i in range(N):
            for (x, y) in ((i, 0), (i, N - 1), (0, i), (N - 1, i)):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, min(255, a + 40))
        out.paste(img, (0, f * N))
    return out


def png_bytes(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return buf.getvalue()


def json_bytes(obj):
    return (json.dumps(obj, indent=2) + "\n").encode()


def build():
    files = {}
    files["textures/block/lockdown_barrier.png"] = png_bytes(sheet_frames())
    files["textures/block/lockdown_barrier.png.mcmeta"] = json_bytes({"animation": {"frametime": 6}})
    files["models/block/lockdown_barrier.json"] = json_bytes({"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": "nusmp:block/lockdown_barrier"}})
    files["blockstates/lockdown_barrier.json"] = json_bytes({"variants": {"": {"model": "nusmp:block/lockdown_barrier"}}})
    files["models/item/lockdown_barrier.json"] = json_bytes({"parent": "nusmp:block/lockdown_barrier"})
    return files


def main(argv):
    files = build()
    check = "--check" in argv
    bad = 0
    for rel, data in sorted(files.items()):
        path = os.path.join(ASSETS, rel)
        if check:
            try:
                same = open(path, "rb").read() == data
            except FileNotFoundError:
                same = False
            if not same:
                bad += 1
                print("DIFFERS: " + rel)
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as f:
                f.write(data)
    if check:
        print("%d files checked, %d differ" % (len(files), bad))
        return 1 if bad else 0
    print("%d files written under %s" % (len(files), os.path.normpath(ASSETS)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

#!/usr/bin/env python3
"""Why: the Beast Will core (item nusmp:beast_will_core, 3.45.0) needs an inventory icon. This is a TEMPORARY flat icon (a glowing orb with a faint ring and
motes, drawn procedurally so it can be regenerated and tuned); the 3D in-hand model of rule 7 is not part of 3.45.0. Writes
src/main/resources/assets/nusmp/textures/item/beast_will_core.png (16x16) and models/item/beast_will_core.json (a plain generated item model).
Run from the repo root: python tools/beastwill/gen_core_icon.py"""
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "item", "beast_will_core.png")
MODEL = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item", "beast_will_core.json")

N = 16
C = (N - 1) / 2.0
R = 5.6


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def main():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    px = im.load()
    rim, mid, core = (46, 22, 92), (126, 84, 214), (236, 214, 255)
    for y in range(N):
        for x in range(N):
            dx, dy = x - C, y - C
            d = math.hypot(dx, dy)
            if d <= R:
                # lit from the upper left: the brightest point sits off centre
                hx, hy = x - (C - 1.6), y - (C - 1.6)
                lit = max(0.0, 1.0 - math.hypot(hx, hy) / (R * 1.35))
                shade = 1.0 - d / R
                col = mix(rim, mid, min(1.0, shade * 1.4))
                col = mix(col, core, lit ** 1.6)
                px[x, y] = col + (255,)
            elif d <= R + 1.6:
                a = int(120 * (1.0 - (d - R) / 1.6))          # the soft halo
                px[x, y] = (150, 110, 240, max(0, a))
    # a ring of dark pixels round the orb keeps it readable on any background
    for y in range(N):
        for x in range(N):
            if px[x, y][3] == 255:
                continue
            for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + ox, y + oy
                if 0 <= nx < N and 0 <= ny < N and px[nx, ny][3] == 255 and math.hypot(x - C, y - C) <= R + 1.1:
                    px[x, y] = (24, 10, 52, 255)
    # three claw marks across the lower right of the orb: the beast's mark
    for k, (x0, y0) in enumerate(((6, 9), (8, 7), (10, 5))):
        for s in range(3):
            x, y = x0 + s, y0 + s
            if 0 <= x < N and 0 <= y < N and px[x, y][3] == 255:
                px[x, y] = mix(px[x, y][:3], (255, 238, 200), 0.55) + (255,)
    # two motes
    for x, y, col in ((2, 2, (255, 240, 255, 255)), (13, 3, (200, 170, 255, 230)), (3, 13, (200, 170, 255, 200))):
        if px[x, y][3] < 40:
            px[x, y] = col
    os.makedirs(os.path.dirname(TEX), exist_ok=True)
    im.save(TEX)
    os.makedirs(os.path.dirname(MODEL), exist_ok=True)
    with open(MODEL, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/beast_will_core"}}, f, indent=2)
        f.write("\n")
    print("wrote", TEX, "and", MODEL)


if __name__ == "__main__":
    main()

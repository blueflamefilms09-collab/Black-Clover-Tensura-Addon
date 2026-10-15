#!/usr/bin/env python3
"""
1.35.0: the assets of the Labyrinth's two portal blocks (docs/labyrinth/c_gate.md):

  Labyrinth Gate       nusmp:labyrinth_gate   a translucent full cube whose texture is an animated swirl, deep blue and violet with white
                                              sparks winding inward (the floating gate of the Overworld arch; walk into it)
  Labyrinth Exit Gate  nusmp:labyrinth_exit   the same swirl in golden-white, winding outward (the way back, on the arrival pad)

    python3 tools/gen_labyrinth_gate_textures.py            writes everything
    python3 tools/gen_labyrinth_gate_textures.py --check    verifies the written files (sizes, frame count, JSON) and exits 1 on a problem

Writes assets/nusmp/textures/block/{labyrinth_gate,labyrinth_exit}.png (16 x 128: eight 16x16 frames) with their .png.mcmeta (frametime 3,
interpolated), the blockstates, the block models (cube_all with render_type translucent) and the item models (the block model is the icon).
Only Pillow and the standard library. Idempotent: running it again rewrites the same bytes (the sparks are seeded).
"""
import argparse
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nusmp")
SIZE, FRAMES, ARMS = 16, 8, 3

GATES = {
    # name: (core colour, mid colour, rim colour, spark colour, seed, direction: -1 winds inward, +1 outward)
    "labyrinth_gate": ((14, 10, 64), (96, 52, 210), (36, 20, 120), (255, 255, 255), 2026035, -1),
    "labyrinth_exit": ((255, 248, 214), (236, 178, 64), (190, 120, 40), (255, 255, 255), 2026036, +1),
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def swirl_sheet(core, mid, rim, spark, seed, direction):
    """16 x (16 * FRAMES): a three-armed swirl turning one arm per loop (seamless), opaque in the middle, clearer at the rim, with sparks that pulse."""
    sheet = Image.new("RGBA", (SIZE, SIZE * FRAMES), (0, 0, 0, 0))
    px = sheet.load()
    rng = random.Random(seed)
    sparks = [(rng.uniform(0.25, 0.85), rng.uniform(0, 2 * math.pi), rng.uniform(0, 2 * math.pi)) for _ in range(6)]
    for f in range(FRAMES):
        phase = 2 * math.pi * f / FRAMES
        for y in range(SIZE):
            for x in range(SIZE):
                dx, dy = (x + 0.5 - SIZE / 2) / (SIZE / 2), (y + 0.5 - SIZE / 2) / (SIZE / 2)
                r = math.hypot(dx, dy)
                theta = math.atan2(dy, dx)
                # the arm pattern: rotates by one arm per loop, and winds in (or out) as r changes
                s = 0.5 + 0.5 * math.sin(ARMS * theta + direction * 7.0 * r + phase)
                s = s * s                                                    # thinner, brighter arms
                glow = max(0.0, 1.0 - r) ** 3                                # the bright eye
                base = lerp(rim, mid, s) if r > 0.55 else lerp(core, mid, s)
                col = lerp(base, spark, min(1.0, glow * 1.2))
                alpha = 255 if r < 0.5 else int(round(255 - (min(r, 1.3) - 0.5) / 0.8 * 150))
                alpha = max(90, min(255, alpha + int(60 * s * (1.0 if r < 1.0 else 0.5))))
                px[x, y + f * SIZE] = (col[0], col[1], col[2], alpha)
        # sparks: fixed points on the swirl that brighten and fade once per loop (integer frequency, so the loop closes)
        for (sr, st, sp) in sparks:
            b = 0.5 + 0.5 * math.sin(phase + sp)
            ang = st + direction * phase / ARMS
            sx = int(SIZE / 2 + math.cos(ang) * sr * SIZE / 2)
            sy = int(SIZE / 2 + math.sin(ang) * sr * SIZE / 2)
            if 0 <= sx < SIZE and 0 <= sy < SIZE:
                old = px[sx, sy + f * SIZE]
                px[sx, sy + f * SIZE] = (lerp(old[:3], spark, b)[0], lerp(old[:3], spark, b)[1], lerp(old[:3], spark, b)[2], max(old[3], int(200 * b)))
    return sheet


def write_all():
    tex = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    for name, (core, mid, rim, spark, seed, direction) in GATES.items():
        swirl_sheet(core, mid, rim, spark, seed, direction).save(os.path.join(tex, name + ".png"), optimize=True)
        write_json(os.path.join(tex, name + ".png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"": {"model": "nusmp:block/" + name}}})
        write_json(os.path.join(ASSETS, "models", "block", name + ".json"),
                   {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": "nusmp:block/" + name}})
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "nusmp:block/" + name})
    print("wrote the Labyrinth gate assets:", ", ".join(GATES))


def check():
    bad = []
    for name in GATES:
        png = os.path.join(ASSETS, "textures", "block", name + ".png")
        try:
            im = Image.open(png)
            if im.size != (SIZE, SIZE * FRAMES) or im.mode != "RGBA":
                bad.append("%s: %s %s, want (%d, %d) RGBA" % (png, im.size, im.mode, SIZE, SIZE * FRAMES))
        except OSError as e:
            bad.append("%s: %s" % (png, e))
        for rel in ("textures/block/%s.png.mcmeta", "blockstates/%s.json", "models/block/%s.json", "models/item/%s.json"):
            path = os.path.join(ASSETS, rel % name)
            try:
                with open(path) as f:
                    obj = json.load(f)
            except (OSError, ValueError) as e:
                bad.append("%s: %s" % (path, e))
                continue
            if rel.startswith("models/block") and obj.get("render_type") != "minecraft:translucent":
                bad.append(path + ": the block model must declare render_type minecraft:translucent")
            if rel.endswith("mcmeta") and obj.get("animation", {}).get("frametime") != 3:
                bad.append(path + ": frametime must be 3")
    for line in bad:
        print("FAIL", line)
    print("check: %s" % ("ok" if not bad else "%d problem(s)" % len(bad)))
    return 1 if bad else 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--check", action="store_true", help="verify the written assets instead of writing them")
    args = ap.parse_args()
    sys.exit(check() if args.check else (write_all() or 0))

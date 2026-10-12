#!/usr/bin/env python3
"""
Assets of the Golden City zone of the Variable Core (docs/living_dungeon.md, "The Golden City"): the one new block of the biosphere, the
golden leaf of its giant trees (texture, blockstate, block model, item model). Idempotent: running it again rewrites the same files.

    python3 tools/gen_nexus_golden_assets.py
"""
import json
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "nusmp")

# the gold of the Golden City, dark to bright, in the order the leaf is shaded
SHADES = [(122, 84, 16), (168, 118, 24), (206, 156, 36), (236, 190, 62), (252, 222, 112), (255, 244, 176)]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def golden_leaves(seed=1021):
    """A leaf block: clusters of small leaves in four golds, dark gaps between them where the light comes through, a few bright dewdrops."""
    n = 16
    rng = random.Random(seed)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    shade = [[0] * n for _ in range(n)]
    # leaf clusters: each is a short diagonal stroke of two or three pixels that wraps round the tile, so the block tiles
    for _ in range(46):
        x, y = rng.randrange(n), rng.randrange(n)
        base = rng.choice((1, 2, 2, 3, 3))
        dx, dy = rng.choice(((1, 0), (0, 1), (1, 1), (1, -1)))
        for i in range(rng.choice((2, 3, 3))):
            cx, cy = (x + dx * i) % n, (y + dy * i) % n
            shade[cy][cx] = max(shade[cy][cx], base + (1 if i == 1 and base < 4 else 0))
    for y in range(n):
        for x in range(n):
            s = shade[y][x]
            if s == 0:
                if rng.random() < 0.45:
                    continue                                          # a gap: see-through
                s = 1
            col = SHADES[min(s, len(SHADES) - 1)]
            j = rng.randint(-8, 8)
            px[x, y] = (max(0, min(255, col[0] + j)), max(0, min(255, col[1] + j)), max(0, min(255, col[2] + j // 2)), 255)
    for _ in range(5):                                                 # dewdrops of light
        x, y = rng.randrange(n), rng.randrange(n)
        if px[x, y][3]:
            px[x, y] = SHADES[-1] + (255,)
    return img


def main():
    tex = os.path.join(ASSETS, "textures", "block")
    os.makedirs(tex, exist_ok=True)
    golden_leaves().save(os.path.join(tex, "golden_leaves.png"))
    write_json(os.path.join(ASSETS, "blockstates", "golden_leaves.json"), {"variants": {"": {"model": "nusmp:block/golden_leaves"}}})
    write_json(os.path.join(ASSETS, "models", "block", "golden_leaves.json"), {
        "parent": "minecraft:block/cube_all",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"all": "nusmp:block/golden_leaves"},
    })
    write_json(os.path.join(ASSETS, "models", "item", "golden_leaves.json"), {"parent": "nusmp:block/golden_leaves"})
    print("wrote the golden leaves (texture, blockstate, block model, item model)")


if __name__ == "__main__":
    main()

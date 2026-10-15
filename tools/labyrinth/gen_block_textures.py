#!/usr/bin/env python3
"""
Textures, blockstates and models of the Labyrinth masonry (docs/labyrinth/a1_foundation.md):
labyrinth_bricks, labyrinth_tiles, labyrinth_pillar (top/side), labyrinth_glyph, labyrinth_lantern, magicule_vein.
Idempotent and seeded: running it again rewrites the same bytes.

    python3 tools/labyrinth/gen_block_textures.py          # write everything
    python3 tools/labyrinth/gen_block_textures.py --check  # only verify the files exist and are 16x16 RGBA
"""
import json
import math
import os
import random
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.normpath(os.path.join(HERE, "..", "..", "src", "main", "resources"))
ASSETS = os.path.join(RES, "assets", "nusmp")
DATA = os.path.join(RES, "data")
N = 16

# the stone: a dark blue-black with a faint warm gold in the seams
STONE = (26, 28, 44)
STONE_LIGHT = (38, 41, 62)
STONE_DARK = (16, 17, 30)
SEAM = (12, 12, 22)
GOLD = (142, 112, 54)
GOLD_BRIGHT = (196, 160, 82)
CYAN = (92, 236, 255)
CYAN_CORE = (214, 252, 255)
CYAN_DIM = (40, 120, 150)
FLAME = (255, 214, 120)
FLAME_CORE = (255, 250, 220)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def save(img, name):
    p = os.path.join(ASSETS, "textures", "block", name + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p, optimize=True)
    return p


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def mix(a, b, t):
    return clamp(tuple(a[i] + (b[i] - a[i]) * t for i in range(3)))


def stone_base(seed, speckle=0.18):
    """The raw rock: dark blue-black grain with a few lighter flecks."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (N, N))
    px = img.load()
    for y in range(N):
        for x in range(N):
            g = rng.random()
            c = mix(STONE_DARK, STONE_LIGHT, g * 0.6 + 0.2)
            if rng.random() < speckle:
                c = mix(c, STONE_LIGHT, 0.6)
            px[x, y] = c + (255,)
    return img


def bricks(seed, rows=((0, 7), (8, 15)), cols=8, offset=4):
    """Running-bond bricks with a dark mortar line and a gold fleck here and there along the seams."""
    rng = random.Random(seed)
    img = stone_base(seed)
    px = img.load()
    for ri, (y0, y1) in enumerate(rows):
        shift = offset if ri % 2 else 0
        for y in range(y0, y1 + 1):
            for x in range(N):
                if y == y1 or (x + shift) % cols == cols - 1:
                    px[x, y] = SEAM + (255,)
                    if rng.random() < 0.10:
                        px[x, y] = GOLD + (255,)
                elif y == y0:
                    px[x, y] = mix(px[x, y][:3], STONE_LIGHT, 0.35) + (255,)  # a highlight on the top edge of each brick
    return img


def tiles(seed):
    """Four smaller tiles per face with a gold-flecked cross seam: the floor stone."""
    rng = random.Random(seed)
    img = stone_base(seed, speckle=0.10)
    px = img.load()
    for y in range(N):
        for x in range(N):
            seam = (x % 8 == 7) or (y % 8 == 7)
            if seam:
                px[x, y] = (SEAM if rng.random() > 0.12 else GOLD) + (255,)
            elif x % 8 == 0 or y % 8 == 0:
                px[x, y] = mix(px[x, y][:3], STONE_LIGHT, 0.3) + (255,)
    # one gold corner stud per tile
    for cx, cy in ((3, 3), (11, 3), (3, 11), (11, 11)):
        px[cx, cy] = GOLD_BRIGHT + (255,)
    return img


def pillar_side(seed):
    """Vertical flutes on the column: light and dark stripes, a gold line down one flute."""
    img = stone_base(seed, speckle=0.05)
    px = img.load()
    for y in range(N):
        for x in range(N):
            phase = x % 4
            if phase == 0:
                px[x, y] = mix(px[x, y][:3], SEAM, 0.8) + (255,)
            elif phase == 1:
                px[x, y] = mix(px[x, y][:3], STONE_LIGHT, 0.5) + (255,)
        px[6, y] = (GOLD if y % 5 else GOLD_BRIGHT) + (255,)
    return img


def pillar_top(seed):
    """A capped top: a gold ring around a darker core."""
    img = stone_base(seed, speckle=0.05)
    px = img.load()
    for y in range(N):
        for x in range(N):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 6.5:
                px[x, y] = SEAM + (255,)
            elif d > 5.5:
                px[x, y] = GOLD + (255,)
            elif d < 2.5:
                px[x, y] = mix(px[x, y][:3], STONE_DARK, 0.6) + (255,)
    return img


def glyph(seed):
    """A brick face with a glowing cyan sigil: a diamond with a bar, the builders' mark."""
    img = bricks(seed)
    px = img.load()
    for y in range(N):
        for x in range(N):
            d = abs(x - 7.5) + abs(y - 7.5)
            if d <= 2.0:
                px[x, y] = CYAN_CORE + (255,)
            elif d <= 3.5:
                px[x, y] = CYAN + (255,)
            elif d <= 4.5:
                px[x, y] = mix(px[x, y][:3], CYAN_DIM, 0.6) + (255,)
    for x in range(3, 13):
        px[x, 3] = CYAN + (255,)
        px[x, 12] = CYAN + (255,)
    for y in (4, 5, 10, 11):
        px[3, y] = CYAN_DIM + (255,)
        px[12, y] = CYAN_DIM + (255,)
    return img


def lantern(seed):
    """A caged mana flame: an iron-dark frame, gold corner rivets, a warm bright core."""
    img = stone_base(seed, speckle=0.05)
    px = img.load()
    for y in range(N):
        for x in range(N):
            edge = x in (0, 15) or y in (0, 15)
            bar = x in (5, 10) or y in (5, 10)
            if edge:
                px[x, y] = SEAM + (255,)
            elif bar:
                px[x, y] = (GOLD if (x + y) % 3 else GOLD_BRIGHT) + (255,)
            else:
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 2.0:
                    px[x, y] = FLAME_CORE + (255,)
                elif d < 4.0:
                    px[x, y] = mix(FLAME, FLAME_CORE, (4.0 - d) / 2.0) + (255,)
                elif d < 6.0:
                    px[x, y] = mix(px[x, y][:3], FLAME, (6.0 - d) / 2.0 * 0.7) + (255,)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = GOLD_BRIGHT + (255,)
    return img


def vein(seed):
    """Dark stone with bright cyan-white veins: three random walks that branch, with a soft halo."""
    rng = random.Random(seed)
    img = stone_base(seed, speckle=0.08)
    px = img.load()
    core = set()
    for start in ((0, rng.randrange(N)), (rng.randrange(N), 0), (N - 1, rng.randrange(N))):
        x, y = start
        dx, dy = (1, 0) if x == 0 else (0, 1) if y == 0 else (-1, 0)
        for _ in range(13):
            core.add((x % N, y % N))
            if rng.random() < 0.3:
                dx, dy = (dy, dx) if rng.random() < 0.5 else (-dy, -dx)
                if dx == 0 and dy == 0:
                    dx = 1
            x, y = x + dx, y + dy
            if rng.random() < 0.08:
                core.add(((x + rng.choice((-1, 1))) % N, y % N))
    for y in range(N):
        for x in range(N):
            if (x, y) in core:
                px[x, y] = (CYAN_CORE if rng.random() < 0.5 else CYAN) + (255,)
            else:
                near = any(((x + ox) % N, (y + oy) % N) in core for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if near:
                    px[x, y] = mix(px[x, y][:3], CYAN_DIM, 0.45) + (255,)
    return img


TEXTURES = {
    "labyrinth_bricks": lambda: bricks(1935101),
    "labyrinth_tiles": lambda: tiles(1935102),
    "labyrinth_pillar_side": lambda: pillar_side(1935103),
    "labyrinth_pillar_top": lambda: pillar_top(1935104),
    "labyrinth_glyph": lambda: glyph(1935105),
    "labyrinth_lantern": lambda: lantern(1935106),
    "magicule_vein": lambda: vein(1935107),
}

BLOCKS = ["labyrinth_bricks", "labyrinth_tiles", "labyrinth_pillar", "labyrinth_glyph", "labyrinth_lantern", "magicule_vein"]


def write_models():
    for b in BLOCKS:
        if b == "labyrinth_pillar":
            write_json(os.path.join(ASSETS, "models", "block", b + ".json"),
                       {"parent": "minecraft:block/cube_column",
                        "textures": {"end": "nusmp:block/labyrinth_pillar_top", "side": "nusmp:block/labyrinth_pillar_side"}})
            write_json(os.path.join(ASSETS, "models", "block", b + "_horizontal.json"),
                       {"parent": "minecraft:block/cube_column_horizontal",
                        "textures": {"end": "nusmp:block/labyrinth_pillar_top", "side": "nusmp:block/labyrinth_pillar_side"}})
            write_json(os.path.join(ASSETS, "blockstates", b + ".json"),
                       {"variants": {"axis=y": {"model": "nusmp:block/labyrinth_pillar"},
                                     "axis=z": {"model": "nusmp:block/labyrinth_pillar_horizontal", "x": 90},
                                     "axis=x": {"model": "nusmp:block/labyrinth_pillar_horizontal", "x": 90, "y": 90}}})
        else:
            write_json(os.path.join(ASSETS, "models", "block", b + ".json"),
                       {"parent": "minecraft:block/cube_all", "textures": {"all": "nusmp:block/" + b}})
            write_json(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {"": {"model": "nusmp:block/" + b}}})
        write_json(os.path.join(ASSETS, "models", "item", b + ".json"), {"parent": "nusmp:block/" + b})
        write_json(os.path.join(DATA, "nusmp", "loot_table", "blocks", b + ".json"),
                   {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
                                                          "entries": [{"type": "minecraft:item", "name": "nusmp:" + b}],
                                                          "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def write_tags():
    """The pickaxe tag (requiresCorrectToolForDrops needs it or nothing drops) and the vein target tag the vein processor reads."""
    p = os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json")
    values = []
    if os.path.exists(p):  # keep what other generators put there
        with open(p) as f:
            values = list(json.load(f).get("values", []))
    for b in BLOCKS:
        if "nusmp:" + b not in values:
            values.append("nusmp:" + b)
    write_json(p, {"replace": False, "values": values})


def check():
    bad = 0
    for name in TEXTURES:
        p = os.path.join(ASSETS, "textures", "block", name + ".png")
        if not os.path.exists(p):
            print("missing", p)
            bad += 1
            continue
        im = Image.open(p)
        if im.size != (N, N) or im.mode != "RGBA":
            print("bad size/mode", p, im.size, im.mode)
            bad += 1
    for b in BLOCKS:
        for p in (os.path.join(ASSETS, "blockstates", b + ".json"), os.path.join(ASSETS, "models", "block", b + ".json"),
                  os.path.join(ASSETS, "models", "item", b + ".json"), os.path.join(DATA, "nusmp", "loot_table", "blocks", b + ".json")):
            if not os.path.exists(p):
                print("missing", p)
                bad += 1
    print("%d problem(s)" % bad)
    return bad == 0


def main():
    if "--check" in sys.argv:
        return 0 if check() else 1
    for name, fn in TEXTURES.items():
        print("wrote", save(fn(), name))
    write_models()
    write_tags()
    return 0


if __name__ == "__main__":
    sys.exit(main())

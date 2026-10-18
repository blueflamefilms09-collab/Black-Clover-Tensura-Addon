"""tools/elite/gen_world_blocks.py: writes the textures, models, blockstates, item models, loot tables, tags and the recipe of the elite world blocks and items.

    python3 -B tools/elite/gen_world_blocks.py        # idempotent: rewrites every file it owns; touches nothing else

Why a script and not hand-drawn files: the five stages of the Magic Ore growth (Small Bud, Medium Bud, Large Bud, Cluster, Mother Rock) must look like ONE crystal growing, so the four
bud textures are drawn by the same shard routine at growing sizes and the four charge states of the Mother Rock by the same vein routine at growing brightness; a new tweak of the palette
then changes all of them together. The hand items (the Magicule Gauge, the Calamity Core) are real 3D element models (vanilla JSON elements, each cube a flat patch of one small palette
texture) with their own display transforms, and a flat sprite for their icons is not needed because the GUI draws the 3D model too. Pure python with Pillow and deterministic.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

from world_art import RAMPS, noise, ramp_color  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp")
NS = "nusmp"


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def save_png(img, *parts):
    path = os.path.join(ASSETS, "textures", *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ============================================================================================ crystal sprites
def shard(px, cx, base, w, h, lean=0.0, ramp="crystal"):
    """One crystal column: wide at the base, a point at the top, lit from the left."""
    for r in range(h):
        y = base - r
        if y < 0 or y > 15:
            continue
        width = max(1, round(w * (1.0 - (r / float(h)) ** 1.4)))
        centre = cx + lean * r / float(h)
        x0 = int(round(centre - width / 2.0))
        for i in range(width):
            x = x0 + i
            if x < 0 or x > 15:
                continue
            t = 0.35 + 0.55 * (1.0 - i / max(1.0, width - 1.0)) if width > 1 else 0.8
            if i == 0 and width > 2:
                t = 0.97
            if r == h - 1:
                t = 1.0
            if i == width - 1 and width > 2:
                t = 0.18
            if r < 2:
                t -= 0.15
            px[x, y] = ramp_color(ramp, t) + (255,)


def bud(stage):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    layouts = {
        0: [(8, 15, 3, 5, 0.0)],
        1: [(7, 15, 4, 8, 0.0), (11, 15, 3, 5, 1.0)],
        2: [(8, 15, 5, 11, 0.0), (4, 15, 4, 8, -1.0), (12, 15, 3, 6, 1.0)],
        3: [(8, 15, 5, 14, 0.0), (4, 15, 4, 11, -2.0), (12, 15, 4, 10, 2.0), (2, 15, 3, 7, -1.0), (14, 15, 3, 8, 1.0)],
    }
    for cx, base, w, h, lean in layouts[stage]:
        shard(px, cx, base, w, h, lean)
    if stage == 3:                                  # the cluster sits on a little rock
        for x in range(1, 15):
            for y in (14, 15):
                if px[x, y][3] == 0 or noise(x, y, 5) > 0.55:
                    px[x, y] = ramp_color("stone", 0.25 + 0.5 * noise(x, y, 9)) + (255,)
    return img


def mother(charge):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = noise(x, y, 31)
            base = ramp_color("stone", 0.10 + 0.45 * n + 0.12 * noise(x // 2, y // 2, 4))
            tint = ramp_color("crystal", 0.05 + 0.25 * n)
            px[x, y] = tuple(int(base[k] * 0.7 + tint[k] * 0.3) for k in range(3)) + (255,)
    for i in range(11):                             # embedded crystal facets, 2 or 3 px blocks
        cx, cy = int(noise(i, 1, 70) * 12) + 1, int(noise(i, 2, 70) * 12) + 1
        size = 2 + (i % 2)
        for dx in range(size):
            for dy in range(size):
                x, y = cx + dx, cy + dy
                if 0 <= x < 16 and 0 <= y < 16:
                    t = 0.25 + 0.5 * (1.0 - dx / float(size)) * (1.0 - dy * 0.3) + 0.12 * charge
                    px[x, y] = ramp_color("crystal", t) + (255,)
    veins = {0: 0, 1: 10, 2: 22, 3: 38}[charge]
    lit = {0: 0.5, 1: 0.7, 2: 0.85, 3: 1.0}[charge]
    x, y = 3, 0
    for k in range(veins):                          # glowing veins: a drunk walk down the face, brighter with the charge
        px[x % 16, y % 16] = ramp_color("crystal", lit) + (255,)
        step = int(noise(k, charge, 90) * 3)
        if step == 0:
            x += 1
        elif step == 1:
            y += 1
        else:
            x -= 1
            y += 1
        if y > 15:
            x, y = int(noise(k, 7, 91) * 14) + 1, 0
    if charge == 3:
        for (x, y) in ((4, 4), (11, 5), (7, 10), (12, 12), (3, 12)):
            px[x, y] = (236, 220, 255, 255)
    return img


# ============================================================================================ item sprites (the icons of the plain item sprites are the 3D models; these are for the shard)
def shard_item():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    shard(px, 8, 14, 7, 13, 1.0)
    shard(px, 5, 14, 3, 6, -1.0)
    for (x, y) in ((11, 3), (4, 5)):
        px[x, y] = (236, 220, 255, 255)
    return img


# ============================================================================================ the palette of the 3D hand items
PATCHES = [  # name -> rgb, laid out 4 by 4, each patch 4 x 4 px
    ("brass_dark", RAMPS["trim"][0]), ("brass", RAMPS["trim"][1]), ("brass_light", RAMPS["trim"][2]), ("brass_shine", RAMPS["trim"][3]),
    ("dark", RAMPS["dark"][1]), ("violet_dark", RAMPS["crystal"][0]), ("violet", RAMPS["crystal"][2]), ("violet_light", RAMPS["crystal"][3]),
    ("glow", (236, 214, 255)), ("bone", RAMPS["bone"][2]), ("leather", (84, 56, 36)), ("stone", RAMPS["stone"][2]),
    ("stone_dark", RAMPS["stone"][1]), ("rune", (255, 160, 70)), ("black", (10, 8, 14)), ("white", (250, 250, 255)),
]


def palette_image():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    px = img.load()
    for i, (_, rgb) in enumerate(PATCHES):
        cx, cy = (i % 4) * 4, (i // 4) * 4
        for y in range(4):
            for x in range(4):
                n = (noise(cx + x, cy + y, 3) - 0.5) * 14
                px[cx + x, cy + y] = tuple(max(0, min(255, int(c + n))) for c in rgb) + (255,)
    return img


def patch_uv(name):
    i = [n for n, _ in PATCHES].index(name)
    cx, cy = (i % 4) * 4, (i // 4) * 4
    return [cx + 0.5, cy + 0.5, cx + 3.5, cy + 3.5]


def element(frm, to, patch, rotation=None, shade=True):
    uv = patch_uv(patch)
    faces = {d: {"uv": uv, "texture": "#p"} for d in ("north", "east", "south", "west", "up", "down")}
    e = {"from": list(frm), "to": list(to), "faces": faces}
    if rotation:
        e["rotation"] = rotation
    if not shade:
        e["shade"] = False
    return e


DISPLAY = {
    "gui": {"rotation": [28, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
    "thirdperson_righthand": {"rotation": [60, 45, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [60, 45, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [10, 40, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [10, 40, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
}


def gauge_model():
    els = [
        element((7, 0, 7), (9, 5, 9), "leather"),
        element((6, 4, 6), (10, 6, 10), "brass_dark"),
        element((4, 6, 6), (12, 14, 10), "brass"),
        element((3, 7, 6), (13, 13, 10), "brass"),
        element((5, 5, 6), (11, 15, 10), "brass"),
        element((5, 7, 5.5), (11, 13, 6.2), "dark"),
        element((4.5, 6.5, 5.2), (11.5, 13.5, 5.6), "brass_shine", shade=False),
        element((7.5, 8.5, 5.0), (8.5, 12.5, 5.5), "violet_light", rotation={"angle": 22.5, "axis": "z", "origin": [8, 9, 5]}, shade=False),
        element((7, 15, 7), (9, 18, 9), "violet", shade=False),
        element((7.4, 15.4, 7.4), (8.6, 17.4, 8.6), "glow", shade=False),
        element((6, 12, 5.0), (7, 13, 5.5), "glow", shade=False),
        element((9, 12, 5.0), (10, 13, 5.5), "glow", shade=False),
        element((6, 8, 5.0), (7, 9, 5.5), "glow", shade=False),
        element((9, 8, 5.0), (10, 9, 5.5), "glow", shade=False),
    ]
    return {"textures": {"p": NS + ":item/world_palette", "particle": NS + ":item/world_palette"}, "elements": els, "display": DISPLAY}


def core_model():
    els = [element((6, 6, 6), (10, 10, 10), "glow", shade=False)]
    for x in (3, 11):
        for y in (3, 11):
            for z in (3, 11):
                els.append(element((x, y, z), (x + 2, y + 2, z + 2), "violet"))
    for a, b in ((3, 3), (3, 12), (12, 3), (12, 12)):
        els.append(element((a, 5, b), (a + 1, 11, b + 1), "violet_dark"))
        els.append(element((5, a, b), (11, a + 1, b + 1), "violet_dark"))
        els.append(element((a, b, 5), (a + 1, b + 1, 11), "violet_dark"))
    els.append(element((7, 13, 7), (9, 15, 9), "rune", shade=False))
    els.append(element((7, 1, 7), (9, 3, 9), "rune", shade=False))
    els.append(element((4, 7.5, 4), (12, 8.5, 12), "bone", rotation={"angle": 45, "axis": "y", "origin": [8, 8, 8]}))
    return {"textures": {"p": NS + ":item/world_palette", "particle": NS + ":item/world_palette"}, "elements": els, "display": DISPLAY}


# ============================================================================================ blocks, loot, tags
BUD_IDS = ["magic_bud_small", "magic_bud_medium", "magic_bud_large", "magic_crystal_cluster"]
FACING = {"down": {"x": 180}, "east": {"x": 90, "y": 90}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "up": {}, "west": {"x": 90, "y": 270}}
SILK = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def loot(block, lo, hi):
    return {"type": "minecraft:block", "random_sequence": NS + ":blocks/" + block, "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": NS + ":" + block, "conditions": [SILK]},
        {"type": "minecraft:item", "name": NS + ":magicule_shard", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}, {"function": "minecraft:explosion_decay"}]}]}]}]}


def main():
    # ---- textures
    for i, name in enumerate(BUD_IDS):
        save_png(bud(i), "block", name + ".png")
    for c in range(4):
        save_png(mother(c), "block", "mother_rock_%d.png" % c)
    save_png(shard_item(), "item", "magicule_shard.png")
    save_png(palette_image(), "item", "world_palette.png")

    # ---- blockstates and models of the crystals (the vanilla amethyst cluster pattern)
    for name in BUD_IDS:
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": {"facing=" + f: dict({"model": NS + ":block/" + name}, **rot) for f, rot in FACING.items()}})
        write_json(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": NS + ":block/" + name}})
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":block/" + name}})
    write_json(os.path.join(ASSETS, "blockstates", "mother_rock.json"), {"variants": {"charge=%d" % c: {"model": NS + ":block/mother_rock_%d" % c} for c in range(4)}})
    for c in range(4):
        write_json(os.path.join(ASSETS, "models", "block", "mother_rock_%d.json" % c), {"parent": "minecraft:block/cube_all", "textures": {"all": NS + ":block/mother_rock_%d" % c}})
    write_json(os.path.join(ASSETS, "models", "item", "mother_rock.json"), {"parent": NS + ":block/mother_rock_3"})

    # ---- the items
    write_json(os.path.join(ASSETS, "models", "item", "magicule_shard.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": NS + ":item/magicule_shard"}})
    gm = gauge_model()
    gm["parent"] = "minecraft:block/block"
    write_json(os.path.join(ASSETS, "models", "item", "magicule_gauge.json"), gm)
    cm = core_model()
    cm["parent"] = "minecraft:block/block"
    write_json(os.path.join(ASSETS, "models", "item", "calamity_core.json"), cm)

    # ---- loot (silk touch gives the block itself, otherwise Magicule Shards; the Tensura crystal bonus is added by WorldEvents)
    for name, lo, hi in (("magic_bud_small", 1, 1), ("magic_bud_medium", 1, 2), ("magic_bud_large", 2, 3), ("magic_crystal_cluster", 3, 5), ("mother_rock", 6, 9)):
        write_json(os.path.join(DATA, "loot_table", "blocks", name + ".json"), loot(name, lo, hi))

    # ---- tags (the pickaxe tag is the one existing file this area edits: see docs/elite/world.md)
    values = ["%s:%s" % (NS, n) for n in BUD_IDS + ["mother_rock"]]
    write_json(os.path.join(DATA, "tags", "block", "magic_crystals.json"), {"replace": False, "values": values})
    write_json(os.path.join(DATA, "tags", "item", "magic_crystals.json"), {"replace": False, "values": values})

    # ---- the Magicule Gauge recipe
    write_json(os.path.join(DATA, "recipe", "magicule_gauge.json"), {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"I": {"item": "minecraft:copper_ingot"}, "C": {"item": "minecraft:compass"}, "S": {"item": NS + ":magicule_shard"}},
        "pattern": [" S ", "ICI", " I "], "result": {"id": NS + ":magicule_gauge", "count": 1}})
    print("wrote the textures, models, blockstates, loot tables, tags and the gauge recipe")


if __name__ == "__main__":
    main()

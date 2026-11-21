#!/usr/bin/env python3
"""
Infinity Stones, Phase B: the 16x16 inventory icons and item models of the Nano Gauntlet and Loki's Scepter, and the 16x16 textures of the Vormir Altar block.

    python3 tools/relics/infinity_b_icons.py

Writes textures/item/<id>.png, models/item/<id>.json (builtin/entity, draws the in-hand look) and models/item/<id>_icon.json (the flat icon),
and textures/block/vormir_altar_top.png and _side.png. Painted from ASCII pixel maps like wave27_icons.py; idempotent. Not made in Blender: these are
flat sprites, which the owner's rules keep as the inventory icon.
"""
import json
import os
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "anime_multiverse")
sys.path.insert(0, HERE)
import wave27_icons as w  # reuse render() and hexc()

ICONS = []


def icon(iid, grade, palette, rows):
    ICONS.append((iid, grade, {k: w.hexc(v) for k, v in palette.items()}, rows))


# Nano Gauntlet: a dark grey-blue armoured glove with red and gold nanotech seams and a ring of six small stone sockets across the back.
icon("the_nano_gauntlet", "GOD", {
    "d": 0x2A2E3C, "D": 0x1A1C26, "r": 0xC8362E, "g": 0xFFB43A, "G": 0xB07A18, "s": 0x4A5068,
    "m": 0xE8D84A, "p": 0xC060F0, "b": 0x4A8CFF, "o": 0xF08A2A, "e": 0x4AD07A, "R": 0xF04A4A,
}, [
    "................",
    "...ss.ss.ss.....",
    "..sddsddsddss...",
    "..sddsddsddds...",
    "..sdddddddddds..",
    "..sdmdpdbdodds..",
    "..sdddddddddds..",
    "..sdRdedgddds...",
    "..sdddddddds....",
    "..rrrrrrrrrr....",
    "..gGgGgGgGgG....",
    "..DdddddddddD...",
    "..DrdrdrdrdD....",
    "..DDDDDDDDDD....",
])

# Loki's Scepter: a golden spear-staff, slim shaft with a leaf-shaped blade and the blue Mind Stone glowing in the curl just under the blade.
icon("lokis_scepter", "GOD", {
    "g": 0xE0B44A, "G": 0xA67C24, "h": 0xFFE58A, "b": 0x3A8BFF, "B": 0xBFE0FF, "k": 0x3A2C10,
}, [
    "..............hg",
    ".............hgg",
    "............hggG",
    "...........hggG.",
    "..........gBbgG.",
    ".........gbBbgG.",
    "........GgbbgG..",
    "........gGgGG...",
    ".......gG.G.....",
    "......gG........",
    ".....gG.........",
    "....gG..........",
    "...gG...........",
    "..kG............",
    ".kG.............",
])


def block_textures():
    base = (0x3A, 0x2A, 0x26)
    # side: dark carved stone with orange rune seams; top: slab with a glowing orange soul-ring
    side = Image.new("RGBA", (16, 16), base + (255,))
    top = Image.new("RGBA", (16, 16), base + (255,))
    sp, tp = side.load(), top.load()
    for y in range(16):
        for x in range(16):
            n = ((x * 7 + y * 13) % 5) * 4
            c = (base[0] + n, base[1] + n, base[2] + n, 255)
            sp[x, y] = c
            tp[x, y] = c
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                sp[x, y] = (0x1E, 0x16, 0x14, 255)
                tp[x, y] = (0x1E, 0x16, 0x14, 255)
    for x in range(3, 13):  # side rune band
        sp[x, 5] = (0xE8, 0x7A, 0x1E, 255)
        sp[x, 10] = (0xE8, 0x7A, 0x1E, 255)
    for y in (6, 7, 8, 9):
        sp[4, y] = (0xE8, 0x7A, 0x1E, 255)
        sp[11, y] = (0xE8, 0x7A, 0x1E, 255)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 4.2 <= d <= 5.4:
                tp[x, y] = (0xF0, 0x8A, 0x2A, 255)
            elif d < 1.8:
                tp[x, y] = (0xFF, 0xD0, 0x60, 255)
    return side, top


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    models = os.path.join(ASSETS, "models", "item")
    os.makedirs(tex, exist_ok=True)
    for iid, grade, pal, rows in ICONS:
        w.render(iid, grade, pal, rows).save(os.path.join(tex, iid + ".png"))
        write_json(os.path.join(models, iid + ".json"), {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "anime_multiverse:item/" + iid}})
        write_json(os.path.join(models, iid + "_icon.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "anime_multiverse:item/" + iid}})
    side, top = block_textures()
    bt = os.path.join(ASSETS, "textures", "block")
    os.makedirs(bt, exist_ok=True)
    side.save(os.path.join(bt, "vormir_altar_side.png"))
    top.save(os.path.join(bt, "vormir_altar_top.png"))
    write_json(os.path.join(ASSETS, "models", "block", "vormir_altar.json"),
               {"parent": "minecraft:block/cube_column", "textures": {"end": "anime_multiverse:block/vormir_altar_top", "side": "anime_multiverse:block/vormir_altar_side"}})
    write_json(os.path.join(ASSETS, "models", "item", "vormir_altar.json"), {"parent": "anime_multiverse:block/vormir_altar"})
    write_json(os.path.join(ASSETS, "blockstates", "vormir_altar.json"), {"variants": {"": {"model": "anime_multiverse:block/vormir_altar"}}})
    print("wrote %d icons, the altar block assets" % len(ICONS))


if __name__ == "__main__":
    main()

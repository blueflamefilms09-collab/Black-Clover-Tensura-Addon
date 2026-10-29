#!/usr/bin/env python3
"""Writes the item models of the Hub (docs/elite/hub.md): the Dungeon Map and the two NPC spawn eggs.

    python tools/elite/gen_hub_models.py

The Dungeon Map follows the pattern of the (removed) Skill Book model: the model file uses the `neoforge:separate_transforms` loader, so the item is a real 3D model in
the hand (a rolled map on two wooden rollers with four knobs, built from a few cuboids) and keeps its flat sprite as the inventory, ground and item-frame icon. The hand transforms are the Skill
Book's (a flat sheet held face-on reads the same way).
"""
import json
import os
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "models", "item")


def el(name, frm, to, faces):
    return {"name": name, "from": frm, "to": to, "faces": faces}


def face(tex, uv):
    return {"uv": uv, "texture": tex}


def build_3d(display):
    elements = [el("sheet", [3, 2, 7.6], [13, 14, 8.4], {
        "south": face("#map", [0, 0, 16, 16]), "north": face("#map", [16, 0, 0, 16]),
        "east": face("#paper", [3, 3, 4, 13]), "west": face("#paper", [3, 3, 4, 13]),
        "up": face("#paper", [3, 3, 13, 4]), "down": face("#paper", [3, 3, 13, 4])})]

    def roller(name, y):
        faces = {k: face("#wood", [0, 0, 16, 2]) for k in ("north", "south", "east", "west")}
        faces.update({k: face("#wood", [0, 0, 16, 3]) for k in ("up", "down")})
        return el(name, [2.2, y, 7.2], [13.8, y + 1.4, 8.8], faces)

    def knob(name, x0, x1, y0, y1):
        return el(name, [x0, y0, 7.4], [x1, y1, 8.6], {k: face("#wood", [0, 0, 3, 3]) for k in ("north", "south", "east", "west", "up", "down")})

    elements += [roller("top_roller", 13.4), roller("bottom_roller", 1.2),
                 knob("knob_top_left", 1.4, 2.2, 13.2, 15.0), knob("knob_top_right", 13.8, 14.6, 13.2, 15.0),
                 knob("knob_bottom_left", 1.4, 2.2, 1.0, 2.8), knob("knob_bottom_right", 13.8, 14.6, 1.0, 2.8)]
    return {"textures": {"map": "nusmp:item/dungeon_map", "paper": "nusmp:item/dungeon_map", "wood": "minecraft:block/dark_oak_planks", "particle": "nusmp:item/dungeon_map"},
            "elements": elements, "display": display}


def write(name, obj):
    with open(os.path.join(OUT, name), "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    with open(os.path.join(OUT, "dungeon_map_3d.json")) as f:   # the display block was copied from the (removed) Skill Book model; it now lives in this file
        display = json.load(f)["display"]
    write("dungeon_map_3d.json", build_3d(display))
    write("dungeon_map_2d.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/dungeon_map"}})
    write("dungeon_map.json", {"loader": "neoforge:separate_transforms", "gui_light": "front", "textures": {"particle": "nusmp:item/dungeon_map"},
                               "base": {"parent": "nusmp:item/dungeon_map_3d"},
                               "perspectives": {"gui": {"parent": "nusmp:item/dungeon_map_2d"}, "ground": {"parent": "nusmp:item/dungeon_map_2d"}, "fixed": {"parent": "nusmp:item/dungeon_map_2d"}}})
    for n in ("quest_giver_npc_spawn_egg", "merchant_npc_spawn_egg"):
        write(n + ".json", {"parent": "minecraft:item/template_spawn_egg"})
    print("wrote the Hub item models into", OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())

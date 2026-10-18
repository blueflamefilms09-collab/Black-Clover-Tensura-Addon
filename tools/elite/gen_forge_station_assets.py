"""Writes the JSON assets of the four forge stations: blockstates (lit / not lit), the two block models per tier, the item models, the loot tables, and the item models
of the three upgrade kits.

    python3 -B tools/elite/gen_forge_station_assets.py

The textures come from gen_forge_station_textures.py. The model is a small forge: a body, a work-surface slab on top (the "anvil" plate), and a short chimney at the back
corner; the lit variant swaps in the glowing side and top textures. Crafting recipes of the stations and kits are hand-written (data/nusmp/recipe/forge_*.json) because
they are the cost of the upgrade path and are meant to be edited.
"""
import json
import os

RES = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources")
TIERS = ["basic", "magisteel", "mythril", "dragonforge"]


def write(path, doc):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", newline="\n") as f:
        json.dump(doc, f, indent=2)
        f.write("\n")


def model(tier, lit):
    sfx = "_lit" if lit else ""
    tex = "nusmp:block/forge_station_%s_" % tier
    return {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": tex + "plate",
            "side": tex + "side" + sfx,
            "plate": tex + "plate",
            "top": tex + "top" + sfx,
            "bottom": tex + "bottom",
        },
        "elements": [
            {"from": [1, 0, 1], "to": [15, 9, 15],
             "faces": {"down": {"texture": "#bottom", "cullface": "down"}, "north": {"texture": "#side"}, "south": {"texture": "#side"},
                       "west": {"texture": "#side"}, "east": {"texture": "#side"}}},
            {"from": [0, 9, 0], "to": [16, 13, 16],
             "faces": {"down": {"texture": "#bottom"}, "up": {"texture": "#top"},
                       "north": {"texture": "#plate", "uv": [0, 0, 16, 4]}, "south": {"texture": "#plate", "uv": [0, 0, 16, 4]},
                       "west": {"texture": "#plate", "uv": [0, 0, 16, 4]}, "east": {"texture": "#plate", "uv": [0, 0, 16, 4]}}},
            {"from": [10, 13, 10], "to": [14, 16, 14],
             "faces": {"north": {"texture": "#plate", "uv": [0, 0, 4, 3]}, "south": {"texture": "#plate", "uv": [0, 0, 4, 3]},
                       "west": {"texture": "#plate", "uv": [0, 0, 4, 3]}, "east": {"texture": "#plate", "uv": [0, 0, 4, 3]},
                       "up": {"texture": "#bottom"}}},
        ],
    }


def build():
    for t in TIERS:
        name = "forge_station_" + t
        write("assets/nusmp/blockstates/%s.json" % name, {"variants": {
            "lit=false": {"model": "nusmp:block/" + name},
            "lit=true": {"model": "nusmp:block/%s_lit" % name}}})
        write("assets/nusmp/models/block/%s.json" % name, model(t, False))
        write("assets/nusmp/models/block/%s_lit.json" % name, model(t, True))
        write("assets/nusmp/models/item/%s.json" % name, {"parent": "nusmp:block/" + name})
        write("data/nusmp/loot_table/blocks/%s.json" % name, {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": "nusmp:" + name}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        if t != "basic":
            write("assets/nusmp/models/item/forge_upgrade_kit_%s.json" % t, {
                "parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/forge_upgrade_kit_%s" % t}})
    print("wrote the assets of", len(TIERS), "stations")


if __name__ == "__main__":
    build()

"""Celestial Spirit Magic: the small data files (JSON) that go with the code.

    python3 -B tools/celestial/gen_celestial_data.py

Writes under src/main/resources/data/
  nusmp/dimension_type/spirit_world.json, nusmp/dimension/spirit_world.json   the Spirit World: a void under the star sky of the End, night for ever
  nusmp/curios/slots/celestial_key.json, nusmp/curios/entities/celestial_key.json, curios/tags/item/celestial_key.json     the Celestial Key slot of Curios (12 places, the 17 keys)
  nusmp/loot_modifiers/celestial_keys.json                                      the global loot modifier that adds keys to the dungeon's chests
and makes sure data/neoforge/loot_modifiers/global_loot_modifiers.json lists nusmp:celestial_keys (idempotent).
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from celestial_data import KEYS, ROOT  # noqa: E402

DATA = os.path.join(ROOT, "src", "main", "resources", "data")


def write(rel, obj):
    p = os.path.join(DATA, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", newline="\n") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


def main():
    write("nusmp/dimension_type/spirit_world.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False, "ambient_light": 0.35, "fixed_time": 18000,
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": False, "respawn_anchor_works": False, "has_raids": False,
        "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:the_end"})
    write("nusmp/dimension/spirit_world.json", {"type": "nusmp:spirit_world", "generator": {"type": "minecraft:flat", "settings": {
        "biome": "minecraft:the_void", "layers": [], "lakes": False, "features": False, "structure_overrides": []}}})
    write("nusmp/curios/slots/celestial_key.json", {"size": 12, "order": 11, "icon": "curios:slot/empty_curio_slot", "validators": ["curios:tag"]})
    write("nusmp/curios/entities/celestial_key.json", {"entities": ["player"], "slots": ["celestial_key"]})
    write("curios/tags/item/celestial_key.json", {"replace": False, "values": ["nusmp:" + k[0] + "_key" for k in KEYS]})
    write("nusmp/loot_modifiers/celestial_keys.json", {"type": "nusmp:celestial_keys", "conditions": [
        {"condition": "minecraft:location_check", "predicate": {"dimension": "nusmp:mega_dungeon"}}]})
    gp = os.path.join(DATA, "neoforge", "loot_modifiers", "global_loot_modifiers.json")
    g = json.load(open(gp))
    if "nusmp:celestial_keys" not in g["entries"]:
        g["entries"].append("nusmp:celestial_keys")
        with open(gp, "w", newline="\n") as f:
            f.write(json.dumps(g, indent=2) + "\n")
    print("celestial data written")


if __name__ == "__main__":
    main()

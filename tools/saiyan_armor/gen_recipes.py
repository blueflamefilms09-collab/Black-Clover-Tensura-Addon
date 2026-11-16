"""Writes the crafting recipes of the Saiyan alloy set (data/anime_multiverse/recipe/saiyan_alloy_*.json). Idempotent.

    python3 -B tools/saiyan_armor/gen_recipes.py

Ingot: 3 iron + 1 gold + 1 copper -> 2 ingots, or 1 netherite scrap + 1 iron + 1 gold -> 2 ingots. Pieces use the vanilla shapes (helmet 5, chestplate 8, leggings 7, boots 4 ingots; the scouter
has a glass pane lens). There is no smithing recipe on purpose: a netherite piece is better than this set, so an upgrade from it would be a downgrade.
"""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "anime_multiverse", "recipe")
INGOT = {"item": "anime_multiverse:saiyan_alloy_ingot"}


def shaped(name, pattern, extra=None):
    key = {"A": INGOT}
    key.update(extra or {})
    return name, {"type": "minecraft:crafting_shaped", "category": "equipment", "key": key, "pattern": pattern, "result": {"id": "anime_multiverse:" + name, "count": 1}}


def shapeless(name, items, count):
    return name, {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": i} for i in items],
                  "result": {"id": "anime_multiverse:saiyan_alloy_ingot", "count": count}}


RECIPES = [
    shapeless("saiyan_alloy_ingot_from_iron", ["minecraft:iron_ingot"] * 3 + ["minecraft:gold_ingot", "minecraft:copper_ingot"], 2),
    shapeless("saiyan_alloy_ingot_from_scrap", ["minecraft:netherite_scrap", "minecraft:iron_ingot", "minecraft:gold_ingot"], 2),
    shaped("saiyan_alloy_scouter", ["AAA", "AGA"], {"G": {"item": "minecraft:glass_pane"}}),
    shaped("saiyan_alloy_battle_armor", ["A A", "AAA", "AAA"]),
    shaped("saiyan_alloy_waist_guard", ["AAA", "A A", "A A"]),
    shaped("saiyan_alloy_greaves", ["A A", "A A"]),
]

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, body in RECIPES:
        with open(os.path.join(OUT, name + ".json"), "w", encoding="utf8", newline="\n") as f:
            json.dump(body, f, indent=2)
            f.write("\n")
    print("wrote %d recipes" % len(RECIPES))

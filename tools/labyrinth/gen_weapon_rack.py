"""The weapon rack (1.107.0, docs/labyrinth/weapon_racks.md): writes every data and asset file of the rack, reproducibly.

    python3 -B tools/labyrinth/gen_weapon_rack.py

Writes (all under src/main/resources, plus the lang fragment):
  assets/nusmp/textures/block/weapon_rack.png      16x16: wood on the top half, riveted iron on the bottom half (a fixed seed)
  assets/nusmp/models/block/weapon_rack.json       the frame: two posts and feet, a top rail with three hooks, a lower plank, a back brace (26 pixels tall)
  assets/nusmp/models/item/weapon_rack.json        the inventory model (the block model)
  assets/nusmp/blockstates/weapon_rack.json        the four facings
  data/nusmp/loot_table/blocks/weapon_rack.json    the block drops itself
  data/nusmp/loot_table/racks/<family>.json        the six themed loot tables of the loot-filled racks (family -> RackFamilies)
  data/nusmp/recipe/weapon_rack.json               the crafting recipe
  data/minecraft/tags/block/mineable/axe.json      the rack is mined with an axe
  tools/lang/weapon_racks.json                     the lang fragment
"""
import json
import os
import random

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as f:
        f.write(text)


def write_json(path, obj):
    write(path, json.dumps(obj, indent=2) + "\n")


# ---------------------------------------------------------------- the texture
def make_texture():
    from PIL import Image
    rnd = random.Random(1107)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(8):                                    # wood: dark oak planks, horizontal grain
        streak = rnd.randint(-8, 8)
        for x in range(16):
            n = rnd.randint(-6, 6)
            base = 84 + streak + n
            px[x, y] = (base + 10, int(base * 0.70) + 6, int(base * 0.42) + 4, 255)
    for x in range(16):
        r, g, b, a = px[x, 0]
        px[x, 0] = (r + 14, g + 10, b + 6, 255)           # lit top edge
        r, g, b, a = px[x, 7]
        px[x, 7] = (r - 18, g - 14, b - 10, 255)          # shaded bottom edge
    for y in range(8, 16):                                # iron: dark steel with rivets
        for x in range(16):
            n = rnd.randint(-5, 5)
            base = 104 + n - (y - 8) * 2
            px[x, y] = (base, base + 2, base + 8, 255)
    for x in range(16):
        px[x, 8] = (150, 152, 160, 255)                   # bright edge
        px[x, 15] = (62, 63, 70, 255)                     # dark edge
    for (x, y) in ((2, 11), (13, 11), (7, 12)):           # rivets
        px[x, y] = (170, 172, 180, 255)
        px[x + 1 if x < 15 else x, y] = (70, 71, 78, 255)
    path = os.path.join(RES, "assets", "nusmp", "textures", "block", "weapon_rack.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ---------------------------------------------------------------- the model
WOOD, IRON = (0, 0, 16, 8), (0, 8, 16, 16)


def face_uv(region, w, h):
    x0, y0, x1, y1 = region
    return [x0, y0, min(x1, x0 + max(1, round(w))), min(y1, y0 + max(1, round(h)))]


def element(frm, to, region):
    dx, dy, dz = to[0] - frm[0], to[1] - frm[1], to[2] - frm[2]
    faces = {
        "north": ("#t", dx, dy), "south": ("#t", dx, dy), "east": ("#t", dz, dy), "west": ("#t", dz, dy),
        "up": ("#t", dx, dz), "down": ("#t", dx, dz),
    }
    return {"from": list(frm), "to": list(to),
            "faces": {k: {"uv": face_uv(region, w, h), "texture": tex} for k, (tex, w, h) in faces.items()}}


def make_model():
    els = [
        element((0, 0, 7), (2, 26, 9), WOOD),            # left post
        element((14, 0, 7), (16, 26, 9), WOOD),          # right post
        element((0, 0, 3), (2, 2, 13), WOOD),            # left foot
        element((14, 0, 3), (16, 2, 13), WOOD),          # right foot
        element((2, 23, 7), (14, 26, 9), IRON),          # top rail
        element((2, 12, 7.5), (14, 13.5, 8.5), WOOD),    # back brace
        element((2, 6, 4), (14, 8, 12), WOOD),           # lower plank (the shelf)
        element((2.7, 20, 8.6), (3.7, 23, 9.4), IRON),   # hooks under the rail, at the three weapon slots (x = 3.2, 8, 12.8)
        element((7.5, 20, 8.6), (8.5, 20 + 3, 9.4), IRON),
        element((12.3, 20, 8.6), (13.3, 23, 9.4), IRON),
    ]
    model = {"textures": {"t": "nusmp:block/weapon_rack", "particle": "nusmp:block/weapon_rack"},
             "elements": els}
    write_json(os.path.join(RES, "assets", "nusmp", "models", "block", "weapon_rack.json"), model)
    write_json(os.path.join(RES, "assets", "nusmp", "models", "item", "weapon_rack.json"),
               {"parent": "nusmp:block/weapon_rack",
                "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0], "scale": [0.45, 0.45, 0.45]},
                            "ground": {"translation": [0, 3, 0], "scale": [0.3, 0.3, 0.3]},
                            "fixed": {"rotation": [0, 180, 0], "translation": [0, -3, 0], "scale": [0.5, 0.5, 0.5]},
                            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.35, 0.35, 0.35]}}})
    write_json(os.path.join(RES, "assets", "nusmp", "blockstates", "weapon_rack.json"),
               {"variants": {
                   "facing=south": {"model": "nusmp:block/weapon_rack"},
                   "facing=west": {"model": "nusmp:block/weapon_rack", "y": 90},
                   "facing=north": {"model": "nusmp:block/weapon_rack", "y": 180},
                   "facing=east": {"model": "nusmp:block/weapon_rack", "y": 270}}})


# ---------------------------------------------------------------- the loot
def worn(lo=0.25, hi=0.85):
    return {"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": lo, "max": hi}, "add": False}


def enchanted(lo, hi):
    return {"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": lo, "max": hi}, "options": "#minecraft:on_random_loot"}


def entry(item, weight, *functions):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if functions:
        e["functions"] = list(functions)
    return e


FAMILIES = {
    "plain": [entry("minecraft:iron_sword", 6, worn()), entry("minecraft:iron_axe", 4, worn()), entry("minecraft:stone_sword", 4, worn()), entry("minecraft:bow", 4, worn()),
              entry("minecraft:shield", 4, worn()), entry("minecraft:iron_helmet", 4, worn()), entry("minecraft:iron_chestplate", 3, worn()), entry("minecraft:chainmail_helmet", 3, worn()),
              entry("minecraft:leather_chestplate", 3, worn()), entry("minecraft:diamond_sword", 1, worn(0.4, 0.9))],
    "armoury": [entry("minecraft:iron_sword", 6, worn()), entry("minecraft:iron_axe", 5, worn()), entry("minecraft:iron_pickaxe", 2, worn()), entry("minecraft:crossbow", 3, worn()),
                entry("minecraft:shield", 4, worn()), entry("minecraft:iron_helmet", 4, worn()), entry("minecraft:iron_chestplate", 4, worn()), entry("minecraft:iron_leggings", 3, worn()),
                entry("minecraft:diamond_sword", 2, worn(0.4, 0.9)), entry("minecraft:diamond_axe", 1, worn(0.4, 0.9)), entry("minecraft:netherite_axe", 1, worn(0.5, 0.9))],
    "crypt": [entry("minecraft:stone_sword", 5, worn(0.5, 0.95)), entry("minecraft:iron_sword", 5, worn(0.5, 0.95)), entry("minecraft:stone_axe", 3, worn(0.5, 0.95)), entry("minecraft:bow", 4, worn(0.5, 0.95)),
              entry("minecraft:wither_skeleton_skull", 2), entry("minecraft:skeleton_skull", 3), entry("minecraft:chainmail_chestplate", 3, worn(0.5, 0.95)),
              entry("minecraft:iron_helmet", 3, worn(0.5, 0.95)), entry("minecraft:shield", 2, worn(0.5, 0.95)), entry("minecraft:netherite_sword", 1, worn(0.6, 0.95))],
    "sacred": [entry("minecraft:golden_sword", 6, worn(0.1, 0.6)), entry("minecraft:golden_helmet", 4, worn(0.1, 0.6)), entry("minecraft:golden_chestplate", 4, worn(0.1, 0.6)),
               entry("minecraft:golden_leggings", 3, worn(0.1, 0.6)), entry("minecraft:shield", 4, worn(0.1, 0.5)), entry("minecraft:iron_sword", 3, worn(0.1, 0.5), enchanted(10, 20)),
               entry("minecraft:diamond_sword", 2, worn(0.1, 0.5), enchanted(14, 26)), entry("minecraft:diamond_helmet", 1, worn(0.1, 0.5), enchanted(14, 26))],
    "arcane": [entry("minecraft:iron_sword", 4, enchanted(14, 28)), entry("minecraft:bow", 4, enchanted(14, 28)), entry("minecraft:crossbow", 3, enchanted(14, 28)),
               entry("minecraft:turtle_helmet", 3, worn()), entry("minecraft:chainmail_helmet", 3, enchanted(10, 22)), entry("minecraft:golden_helmet", 3, enchanted(10, 22)),
               entry("minecraft:shield", 3, enchanted(10, 22)), entry("minecraft:diamond_sword", 2, enchanted(18, 30)), entry("minecraft:trident", 1, worn(0.3, 0.8), enchanted(16, 30))],
    "wild": [entry("minecraft:wooden_axe", 4, worn()), entry("minecraft:stone_axe", 5, worn()), entry("minecraft:stone_sword", 4, worn()), entry("minecraft:bow", 5, worn()),
             entry("minecraft:crossbow", 3, worn()), entry("minecraft:leather_helmet", 4, worn()), entry("minecraft:leather_chestplate", 4, worn()), entry("minecraft:turtle_helmet", 2, worn()),
             entry("minecraft:shield", 3, worn()), entry("minecraft:iron_axe", 2, worn())],
}


def make_loot():
    for fam, entries in FAMILIES.items():
        write_json(os.path.join(RES, "data", "nusmp", "loot_table", "racks", fam + ".json"),
                   {"type": "minecraft:chest",
                    "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 5}, "bonus_rolls": 0, "entries": entries}]})
    write_json(os.path.join(RES, "data", "nusmp", "loot_table", "blocks", "weapon_rack.json"),
               {"type": "minecraft:block",
                "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "nusmp:weapon_rack"}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def make_recipe_tags_lang():
    write_json(os.path.join(RES, "data", "nusmp", "recipe", "weapon_rack.json"),
               {"type": "minecraft:crafting_shaped", "category": "misc",
                "key": {"S": {"item": "minecraft:stick"}, "I": {"item": "minecraft:iron_ingot"}, "P": {"tag": "minecraft:planks"}},
                "pattern": ["SIS", "SPS", "PPP"],
                "result": {"id": "nusmp:weapon_rack", "count": 1}})
    write_json(os.path.join(RES, "data", "minecraft", "tags", "block", "mineable", "axe.json"), {"values": ["nusmp:weapon_rack"]})
    write_json(os.path.join(ROOT, "tools", "lang", "weapon_racks.json"), {
        "block.nusmp.weapon_rack": "Weapon Rack",
        "nusmp.weapon_rack.decor": "These are only for show.",
    })


if __name__ == "__main__":
    make_texture()
    make_model()
    make_loot()
    make_recipe_tags_lang()
    print("weapon rack: wrote texture, model, blockstate, 6 loot tables, block drop, recipe, axe tag, lang fragment")

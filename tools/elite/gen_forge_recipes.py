"""Generates the forge recipes (data/nusmp/elite/forge/recipes/*.json) and their index from the table below.

    python3 -B tools/elite/gen_forge_recipes.py

Why a script: 40+ recipes share a handful of rules (minigame sequences per category and tier, premium extras, the engraving flag), so the table is the single
place the numbers live and the JSON files are its output. The numbers are this mod's own design (the original addon's source was not available); see
docs/elite/forge.md. Item ids: vanilla ones; Tensura ones that other files of this mod already rely on (tensura:low_magisteel_ingot, high_magisteel_ingot,
mithril_ingot, adamantite_ingot, hihiirokane_ingot, magic_stone, the three magic crystals, daemon_essence); nusmp weapons that exist as items of this mod.
Run it again after editing the table; it rewrites every file (a file not in the table is left alone).
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "data", "nusmp", "elite", "forge", "recipes")

# minigame sequences by (category, tier): (type, difficulty)
H, F, R, T = "hammer_timing", "magicule_flow", "rune_alignment", "temperature_forge"
SEQ = {
    ("weapon", 0): [(H, 1)],
    ("weapon", 1): [(H, 2), (T, 1)],
    ("weapon", 2): [(H, 3), (T, 3), (F, 2)],
    ("weapon", 3): [(H, 4), (T, 4), (R, 3), (F, 4)],
    ("armor", 0): [(H, 1), (T, 1)],
    ("armor", 1): [(H, 2), (T, 2)],
    ("armor", 2): [(H, 3), (T, 3), (R, 2)],
    ("armor", 3): [(H, 4), (T, 4), (R, 3)],
    ("tool", 0): [(H, 1)],
    ("tool", 1): [(H, 2), (F, 1)],
    ("tool", 2): [(H, 3), (T, 2), (F, 2)],
    ("tool", 3): [(H, 4), (T, 3), (F, 3)],
    ("rune", 0): [(R, 1), (F, 1)],
    ("rune", 1): [(R, 2), (F, 2)],
    ("rune", 2): [(R, 3), (F, 3), (H, 2)],
    ("rune", 3): [(R, 4), (F, 4), (T, 3), (H, 3)],
}

GOLD = ("minecraft:gold_ingot", 2, 2)
DIAMOND = ("minecraft:diamond", 1, 3)
MED = ("tensura:medium_quality_magic_crystal", 1, 4)
HIGH = ("tensura:high_quality_magic_crystal", 1, 6)
HIHI = ("tensura:hihiirokane_ingot", 1, 8)
AMETHYST = ("minecraft:amethyst_shard", 4, 2)

# name, tier, category, output item, output count, [(item, count)], [premium (item, count, bonus)], base quality, engrave
R_ = []


def add(name, tier, cat, out, inputs, premium=(), base=12, engrave=None, count=1):
    if engrave is None:
        engrave = cat in ("weapon", "armor", "rune")
    R_.append((name, tier, cat, out, count, inputs, list(premium), base, engrave))


# ---------------------------------------------------------------- tier 0: Basic station (iron, leather-grade, magic stones)
add("iron_sword", 0, "weapon", "minecraft:iron_sword", [("minecraft:iron_ingot", 2), ("minecraft:stick", 1)], [GOLD], 16)
add("iron_axe", 0, "weapon", "minecraft:iron_axe", [("minecraft:iron_ingot", 3), ("minecraft:stick", 2)], [GOLD], 15)
add("golden_sword", 0, "weapon", "minecraft:golden_sword", [("minecraft:gold_ingot", 2), ("minecraft:stick", 1)], [DIAMOND], 18)
add("bow", 0, "weapon", "minecraft:bow", [("minecraft:stick", 3), ("minecraft:string", 3), ("minecraft:iron_ingot", 1)], [GOLD], 14)
add("crossbow", 0, "weapon", "minecraft:crossbow", [("minecraft:iron_ingot", 2), ("minecraft:string", 3), ("minecraft:tripwire_hook", 1), ("minecraft:stick", 3)], [GOLD], 13)
add("iron_pickaxe", 0, "tool", "minecraft:iron_pickaxe", [("minecraft:iron_ingot", 3), ("minecraft:stick", 2)], [GOLD], 16)
add("iron_shovel", 0, "tool", "minecraft:iron_shovel", [("minecraft:iron_ingot", 1), ("minecraft:stick", 2)], [GOLD], 16)
add("iron_hoe", 0, "tool", "minecraft:iron_hoe", [("minecraft:iron_ingot", 2), ("minecraft:stick", 2)], [GOLD], 16)
add("iron_helmet", 0, "armor", "minecraft:iron_helmet", [("minecraft:iron_ingot", 5)], [GOLD], 15)
add("iron_chestplate", 0, "armor", "minecraft:iron_chestplate", [("minecraft:iron_ingot", 8)], [GOLD], 14)
add("iron_leggings", 0, "armor", "minecraft:iron_leggings", [("minecraft:iron_ingot", 7)], [GOLD], 14)
add("iron_boots", 0, "armor", "minecraft:iron_boots", [("minecraft:iron_ingot", 4)], [GOLD], 15)
add("chainmail_chestplate", 0, "armor", "minecraft:chainmail_chestplate", [("minecraft:iron_ingot", 6), ("minecraft:string", 4)], [GOLD], 13)
add("shield", 0, "armor", "minecraft:shield", [("minecraft:iron_ingot", 1), ("minecraft:oak_planks", 6)], [GOLD], 15)
add("magic_tool_sword", 0, "rune", "nusmp:magic_tool_sword", [("minecraft:iron_ingot", 2), ("minecraft:stick", 1), ("tensura:magic_stone", 2), ("tensura:low_quality_magic_crystal", 1)], [MED], 14)
add("magic_tool_spear", 0, "rune", "nusmp:magic_tool_spear", [("minecraft:iron_ingot", 1), ("minecraft:stick", 2), ("tensura:magic_stone", 2), ("tensura:low_quality_magic_crystal", 1)], [MED], 14)
add("magic_tool_bow", 0, "rune", "nusmp:magic_tool_bow", [("minecraft:stick", 3), ("minecraft:string", 3), ("tensura:magic_stone", 2), ("tensura:low_quality_magic_crystal", 1)], [MED], 14)

# ---------------------------------------------------------------- tier 1: Magisteel station (diamond, low magisteel)
add("diamond_sword", 1, "weapon", "minecraft:diamond_sword", [("minecraft:diamond", 2), ("minecraft:stick", 1), ("tensura:low_magisteel_ingot", 1)], [MED], 14)
add("diamond_axe", 1, "weapon", "minecraft:diamond_axe", [("minecraft:diamond", 3), ("minecraft:stick", 2), ("tensura:low_magisteel_ingot", 1)], [MED], 13)
add("diamond_pickaxe", 1, "tool", "minecraft:diamond_pickaxe", [("minecraft:diamond", 3), ("minecraft:stick", 2), ("tensura:low_magisteel_ingot", 1)], [MED], 14)
add("diamond_shovel", 1, "tool", "minecraft:diamond_shovel", [("minecraft:diamond", 1), ("minecraft:stick", 2), ("tensura:low_magisteel_ingot", 1)], [MED], 14)
add("diamond_helmet", 1, "armor", "minecraft:diamond_helmet", [("minecraft:diamond", 5), ("tensura:low_magisteel_ingot", 1)], [MED], 13)
add("diamond_chestplate", 1, "armor", "minecraft:diamond_chestplate", [("minecraft:diamond", 8), ("tensura:low_magisteel_ingot", 2)], [MED], 12)
add("diamond_leggings", 1, "armor", "minecraft:diamond_leggings", [("minecraft:diamond", 7), ("tensura:low_magisteel_ingot", 2)], [MED], 12)
add("diamond_boots", 1, "armor", "minecraft:diamond_boots", [("minecraft:diamond", 4), ("tensura:low_magisteel_ingot", 1)], [MED], 13)
add("permafrost_sabre", 1, "weapon", "nusmp:permafrost_sabre", [("tensura:low_magisteel_ingot", 4), ("minecraft:packed_ice", 2), ("minecraft:diamond", 1), ("minecraft:stick", 1)], [MED], 12)
add("cyclone_rapier", 1, "weapon", "nusmp:cyclone_rapier", [("tensura:low_magisteel_ingot", 4), ("minecraft:phantom_membrane", 2), ("minecraft:diamond", 1), ("minecraft:stick", 1)], [MED], 12)
add("spell_forged_rapier", 1, "rune", "nusmp:spell_forged_rapier", [("tensura:low_magisteel_ingot", 3), ("minecraft:diamond", 2), ("minecraft:amethyst_shard", 4), ("tensura:medium_quality_magic_crystal", 1)], [HIGH], 12)

# ---------------------------------------------------------------- tier 2: Mythril station (netherite, mithril, high magisteel)
add("netherite_sword", 2, "weapon", "minecraft:netherite_sword", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 1), ("tensura:mithril_ingot", 2), ("minecraft:stick", 1)], [HIGH], 12)
add("netherite_axe", 2, "weapon", "minecraft:netherite_axe", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 2), ("tensura:mithril_ingot", 2), ("minecraft:stick", 2)], [HIGH], 11)
add("netherite_pickaxe", 2, "tool", "minecraft:netherite_pickaxe", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 2), ("tensura:mithril_ingot", 2), ("minecraft:stick", 2)], [HIGH], 12)
add("netherite_helmet", 2, "armor", "minecraft:netherite_helmet", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 3), ("tensura:mithril_ingot", 4)], [HIGH], 10)
add("netherite_chestplate", 2, "armor", "minecraft:netherite_chestplate", [("minecraft:netherite_ingot", 2), ("minecraft:diamond", 4), ("tensura:mithril_ingot", 6)], [HIGH], 9)
add("netherite_leggings", 2, "armor", "minecraft:netherite_leggings", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 4), ("tensura:mithril_ingot", 5)], [HIGH], 9)
add("netherite_boots", 2, "armor", "minecraft:netherite_boots", [("minecraft:netherite_ingot", 1), ("minecraft:diamond", 2), ("tensura:mithril_ingot", 3)], [HIGH], 10)
add("trident", 2, "weapon", "minecraft:trident", [("minecraft:heart_of_the_sea", 1), ("minecraft:prismarine_shard", 8), ("tensura:mithril_ingot", 3), ("minecraft:diamond", 1)], [HIGH], 10)
add("magebane_longsword", 2, "rune", "nusmp:magebane_longsword", [("tensura:mithril_ingot", 5), ("tensura:high_magisteel_ingot", 2), ("minecraft:amethyst_shard", 4), ("minecraft:diamond", 1)], [HIGH], 10)
add("prism_blade", 2, "rune", "nusmp:prism_blade", [("tensura:mithril_ingot", 4), ("minecraft:prismarine_crystals", 8), ("minecraft:amethyst_shard", 6), ("tensura:high_magisteel_ingot", 1)], [HIGH], 10)
add("severing_greatsword", 2, "weapon", "nusmp:severing_greatsword", [("tensura:high_magisteel_ingot", 6), ("tensura:mithril_ingot", 4), ("minecraft:diamond_block", 1)], [HIGH], 9)
add("magebane_shield", 2, "armor", "nusmp:magebane_shield", [("tensura:mithril_ingot", 4), ("minecraft:shield", 1), ("minecraft:amethyst_shard", 4)], [HIGH], 10)

# ---------------------------------------------------------------- tier 3: Dragonforge station (adamantite, hihiirokane)
add("mace", 3, "weapon", "minecraft:mace", [("minecraft:heavy_core", 1), ("minecraft:breeze_rod", 2), ("tensura:adamantite_ingot", 2), ("tensura:hihiirokane_ingot", 1)], [HIGH], 10)
add("thundercleave", 3, "weapon", "nusmp:thundercleave", [("tensura:adamantite_ingot", 4), ("minecraft:lightning_rod", 3), ("tensura:mithril_ingot", 2), ("minecraft:netherite_ingot", 1)], [HIHI], 9)
add("bloodbound_katana", 3, "weapon", "nusmp:bloodbound_katana", [("tensura:adamantite_ingot", 4), ("tensura:hihiirokane_ingot", 1), ("tensura:daemon_essence", 2), ("minecraft:netherite_ingot", 1)], [HIGH], 9)
add("rimeheart_runeblade", 3, "rune", "nusmp:rimeheart_runeblade", [("tensura:adamantite_ingot", 4), ("minecraft:blue_ice", 6), ("tensura:hihiirokane_ingot", 1), ("tensura:high_quality_magic_crystal", 2)], [MED], 8)
add("doom_hammer", 3, "weapon", "nusmp:doom_hammer", [("tensura:adamantite_ingot", 5), ("minecraft:netherite_ingot", 2), ("minecraft:obsidian", 8), ("tensura:daemon_essence", 1)], [HIHI], 8)
add("netherite_chestplate_dragonforged", 3, "armor", "minecraft:netherite_chestplate", [("minecraft:netherite_ingot", 3), ("tensura:adamantite_ingot", 4), ("tensura:hihiirokane_ingot", 1)], [HIGH], 8)
add("netherite_helmet_dragonforged", 3, "armor", "minecraft:netherite_helmet", [("minecraft:netherite_ingot", 2), ("tensura:adamantite_ingot", 2), ("tensura:hihiirokane_ingot", 1)], [HIGH], 8)
add("netherite_pickaxe_dragonforged", 3, "tool", "minecraft:netherite_pickaxe", [("minecraft:netherite_ingot", 2), ("tensura:adamantite_ingot", 2), ("minecraft:stick", 2)], [HIHI], 9)


def build():
    os.makedirs(OUT, exist_ok=True)
    names = set()
    for name, tier, cat, out, count, inputs, premium, base, engrave in R_:
        assert name not in names, name
        names.add(name)
        assert 1 <= len(inputs) <= 6 and len(premium) <= 4 and 0 <= base <= 30
        steps = SEQ[(cat, tier)]
        doc = {
            "tier": tier,
            "category": cat,
            "output": {"item": out, "count": count},
            "inputs": [{"item": i, "count": c} for i, c in inputs],
            "premium": [{"item": i, "count": c, "bonus": b} for i, c, b in premium],
            "minigames": [{"type": t, "difficulty": d} for t, d in steps],
            "base_quality": base,
            "engrave": engrave,
        }
        with open(os.path.join(OUT, name + ".json"), "w", newline="\n") as f:
            json.dump(doc, f, indent=2)
            f.write("\n")
    print("wrote", len(names), "recipes to", os.path.normpath(OUT))


if __name__ == "__main__":
    build()

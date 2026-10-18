#!/usr/bin/env python3
"""Why: the contract templates of the Nations system (data/nusmp/elite/nations/contracts/*.json) are written by this table so the numbers of the five mission tiers stay consistent
(a tier-4 job always pays more than a tier-3 one) and a new template is one line. Run from the repo root: python3 tools/elite/gen_nation_contracts.py
Rewards are copper coins (the wallet unit), nation XP and reputation. Everything here is checkable from a server-side game event: kill, mine, craft, discover biomes, hand in items.
Only vanilla ids and vanilla tags are used so the pack works with nothing else installed; add your own JSON next to these for modded targets."""
import json
import os

COINS = {1: 200, 2: 600, 3: 1500, 4: 3500, 5: 8000}
XP = {1: 8, 2: 20, 3: 45, 4: 90, 5: 180}
REP = {1: 5, 2: 10, 3: 15, 4: 20, 5: 30}
MIN_LEVEL = {1: 1, 2: 3, 3: 5, 4: 7, 5: 9}
TIME = {1: 0, 2: 0, 3: 0, 4: 180, 5: 240}   # minutes; 0 = no limit

# (id, type, target, count, tier, title)
T = [
    # --- kill
    ("slay_zombies", "kill_mob", "minecraft:zombie", 20, 1, "Clear the Shambling Dead"),
    ("slay_skeletons", "kill_mob", "minecraft:skeleton", 15, 1, "Rattle the Bone Archers"),
    ("slay_spiders", "kill_mob", "minecraft:spider", 15, 1, "Spiders in the Cellar"),
    ("slay_creepers", "kill_mob", "minecraft:creeper", 8, 1, "Defuse the Creepers"),
    ("cull_undead", "kill_mob", "#minecraft:undead", 40, 2, "Cull the Undead Tide"),
    ("slay_witches", "kill_mob", "minecraft:witch", 4, 2, "Hex the Hedge Witches"),
    ("slay_endermen", "kill_mob", "minecraft:enderman", 8, 2, "Hunt the Tall Ones"),
    ("slay_drowned", "kill_mob", "minecraft:drowned", 15, 2, "Pull the Drowned from the Shore"),
    ("slay_raiders", "kill_mob", "#minecraft:raiders", 25, 3, "Break the Raiders"),
    ("slay_blazes", "kill_mob", "minecraft:blaze", 10, 3, "Quench the Blazes"),
    ("slay_wither_skeletons", "kill_mob", "minecraft:wither_skeleton", 8, 3, "Fortress Patrol"),
    ("slay_phantoms", "kill_mob", "minecraft:phantom", 10, 3, "Clear the Night Sky"),
    ("slay_piglin_brutes", "kill_mob", "minecraft:piglin_brute", 6, 4, "Bastion Breakers"),
    ("slay_ravagers", "kill_mob", "minecraft:ravager", 3, 4, "Fell the Ravagers"),
    ("slay_ghasts", "kill_mob", "minecraft:ghast", 4, 4, "Tears of the Nether"),
    ("slay_evokers", "kill_mob", "minecraft:evoker", 3, 4, "Silence the Evokers"),
    ("slay_elder_guardian", "kill_mob", "minecraft:elder_guardian", 1, 5, "The Drowned Throne"),
    ("slay_wither", "kill_mob", "minecraft:wither", 1, 5, "End the Wither"),
    ("slay_warden", "kill_mob", "minecraft:warden", 1, 5, "Silence the Warden"),
    # --- mine
    ("mine_stone", "mine_block", "minecraft:stone", 128, 1, "Quarry Work"),
    ("mine_logs", "mine_block", "#minecraft:logs", 64, 1, "Timber for the Palisade"),
    ("mine_coal", "mine_block", "#minecraft:coal_ores", 32, 1, "Coal for the Forges"),
    ("mine_iron", "mine_block", "#minecraft:iron_ores", 24, 2, "Iron for the Armoury"),
    ("mine_gold", "mine_block", "#minecraft:gold_ores", 16, 2, "Gold for the Treasury"),
    ("mine_redstone", "mine_block", "#minecraft:redstone_ores", 32, 2, "Redstone for the Engineers"),
    ("mine_diamonds", "mine_block", "#minecraft:diamond_ores", 8, 3, "Diamonds for the Crown"),
    ("mine_emeralds", "mine_block", "#minecraft:emerald_ores", 6, 3, "Emeralds for the Envoys"),
    ("mine_obsidian", "mine_block", "minecraft:obsidian", 16, 3, "Obsidian for the Gate"),
    ("mine_debris", "mine_block", "minecraft:ancient_debris", 4, 4, "Ancient Debris"),
    # --- craft
    ("craft_torches", "craft_item", "minecraft:torch", 64, 1, "Light the Streets"),
    ("craft_bread", "craft_item", "minecraft:bread", 16, 1, "Bread for the Garrison"),
    ("craft_bows", "craft_item", "minecraft:bow", 3, 2, "Arm the Archers"),
    ("craft_iron_swords", "craft_item", "minecraft:iron_sword", 3, 2, "Iron Blades"),
    ("craft_brewing_stands", "craft_item", "minecraft:brewing_stand", 2, 2, "Equip the Apothecary"),
    ("craft_golden_apples", "craft_item", "minecraft:golden_apple", 3, 3, "Field Rations of the Elite"),
    ("craft_diamond_swords", "craft_item", "minecraft:diamond_sword", 2, 3, "Blades for the Captains"),
    ("craft_enchanting_table", "craft_item", "minecraft:enchanting_table", 1, 3, "A Table for the Court Mage"),
    ("craft_beacon", "craft_item", "minecraft:beacon", 1, 5, "Raise the Beacon"),
    # --- explore
    ("explore_three_biomes", "explore_biome", "", 3, 1, "Walk the Borders"),
    ("explore_six_biomes", "explore_biome", "", 6, 2, "Survey the Realm"),
    ("explore_ten_biomes", "explore_biome", "", 10, 4, "Cartographer's Commission"),
    ("explore_forests", "explore_biome", "#minecraft:is_forest", 2, 1, "Scout the Forests"),
    ("explore_oceans", "explore_biome", "#minecraft:is_ocean", 2, 2, "Chart the Coast"),
    ("explore_nether", "explore_biome", "#minecraft:is_nether", 3, 3, "Map the Nether Wastes"),
    ("explore_badlands", "explore_biome", "#minecraft:is_badlands", 1, 2, "Mesa Expedition"),
    # --- deliver
    ("deliver_cobblestone", "deliver_item", "minecraft:cobblestone", 64, 1, "Stone for the Walls"),
    ("deliver_wheat", "deliver_item", "minecraft:wheat", 64, 1, "Grain for the Granary"),
    ("deliver_string", "deliver_item", "minecraft:string", 48, 1, "String for the Bowyers"),
    ("deliver_leather", "deliver_item", "minecraft:leather", 24, 1, "Hides for the Tannery"),
    ("deliver_iron", "deliver_item", "minecraft:iron_ingot", 32, 2, "Ingots for the Smiths"),
    ("deliver_gold", "deliver_item", "minecraft:gold_ingot", 24, 2, "Tribute in Gold"),
    ("deliver_gunpowder", "deliver_item", "minecraft:gunpowder", 32, 2, "Powder for the Sappers"),
    ("deliver_diamonds", "deliver_item", "minecraft:diamond", 8, 3, "Diamonds for the Vault"),
    ("deliver_emeralds", "deliver_item", "minecraft:emerald", 16, 3, "Emeralds for the Envoys"),
    ("deliver_blaze_rods", "deliver_item", "minecraft:blaze_rod", 8, 3, "Rods for the Alchemists"),
    ("deliver_ender_pearls", "deliver_item", "minecraft:ender_pearl", 12, 3, "Pearls for the Gatewrights"),
    ("deliver_netherite_scrap", "deliver_item", "minecraft:netherite_scrap", 4, 4, "Scrap for the Royal Armoury"),
    ("deliver_nether_star", "deliver_item", "minecraft:nether_star", 1, 5, "A Star for the Crown"),
]


def main():
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "data", "nusmp", "elite", "nations", "contracts")
    os.makedirs(out, exist_ok=True)
    for cid, typ, target, count, tier, title in T:
        scale = 1.0 + (min(count, 128) ** 0.5) * 0.03
        doc = {
            "type": typ,
            "tier": tier,
            "count": count,
            "title": title,
            "reward": {"coins": int(round(COINS[tier] * scale / 10.0)) * 10, "xp": int(round(XP[tier] * scale)), "rep": REP[tier]},
            "weight": {1: 12, 2: 10, 3: 8, 4: 5, 5: 2}[tier],
            "min_level": MIN_LEVEL[tier],
        }
        if target:
            doc["target"] = target
        if TIME[tier]:
            doc["time_limit_minutes"] = TIME[tier]
        with open(os.path.join(out, cid + ".json"), "w", encoding="utf-8") as f:
            json.dump(doc, f, indent=2)
            f.write("\n")
    print("wrote", len(T), "contract templates to", os.path.normpath(out))


if __name__ == "__main__":
    main()

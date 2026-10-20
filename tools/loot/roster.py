"""The roster of the functional anime loot of 1.99.0: the ONE place where the 80 items are written down. Everything else is generated from it:

    python3 -B tools/loot/gen_data.py        data/nusmp/loot/roster.json (what the game reads), the recipes, the item tag, the item models, tools/lang/squad_loot.json
    python3 -B tools/gen_loot_icons.py       the 16 x 16 icons of foods, materials and armour, the four armour layer textures, the effect icon
    python3 -B tools/gen_weapon_models.py loot    the eight 3D weapons and the eight 3D trinkets (tools/gen_loot_models.py)

Kinds: food (an ordinary meal or tonic), elixir (restores magicules or aura), material (a monster part), crafted (a material made from other things), ammo (bone arrows),
weapon, armour, trinket. Money is in bronze coins (100 bronze = 1 silver, as the Hub shop counts). Hunger: foods only restore it; no effect below is a hunger effect, and a test
(LootRosterTest) fails if one ever is.
"""

FAMILIES = ["goblin", "ogre", "undead", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect"]

# ---------------------------------------------------------------- materials: (id, name, family, value, tier, chance, minBand, lore)
MATERIALS = [
    ("goblin_ear", "Goblin Ear", "goblin", 6, 1, 0.16, 0, "A clipped goblin ear, kept as proof of the kill."),
    ("goblin_tooth", "Goblin Tooth", "goblin", 8, 1, 0.12, 0, "Small, yellow and sharper than it looks."),
    ("orc_tusk", "Orc Tusk", "ogre", 18, 2, 0.10, 0, "A heavy curved tusk, good ivory for a smith."),
    ("troll_hide", "Troll Hide", "ogre", 32, 2, 0.07, 1, "Thick grey hide that closes its own cuts."),
    ("bone_shard", "Bone Shard", "undead", 4, 1, 0.20, 0, "A splinter of old bone, hard enough for an arrowhead."),
    ("ghoul_claw", "Ghoul Claw", "undead", 22, 2, 0.08, 0, "A black hooked claw from something that dug its way out."),
    ("lich_dust", "Lich Dust", "undead", 46, 3, 0.05, 1, "Grey dust that hums with leftover magicules."),
    ("wraith_essence", "Wraith Essence", "undead", 64, 3, 0.035, 2, "A cold thread of a spirit that has not finished leaving."),
    ("slime_gel", "Slime Gel", "slime", 5, 1, 0.20, 0, "Clear, wobbling and safe to cook."),
    ("gel_core", "Gel Core", "slime", 26, 2, 0.06, 1, "The firm bead at the centre of a slime."),
    ("rusted_cog", "Rusted Cog", "construct", 12, 1, 0.14, 0, "A toothed wheel from a construct's workings."),
    ("golem_core", "Golem Core", "construct", 84, 3, 0.03, 1, "The warm stone that tells a golem to stand up."),
    ("cursed_cloth", "Cursed Cloth", "cultist", 20, 2, 0.10, 0, "A rag from a cultist's robe that never quite stays still."),
    ("cultist_sigil", "Cultist Sigil", "cultist", 38, 2, 0.05, 1, "A tin badge stamped with a ring of small runes."),
    ("imp_horn", "Imp Horn", "demon", 28, 2, 0.09, 0, "A small red horn, warm to the touch."),
    ("shadow_core_shard", "Shadow Core Shard", "demon", 92, 3, 0.03, 2, "A shard of a shadow's heart. The System's Shadow Core works in the same recipes."),
    ("spirit_wisp", "Spirit Wisp", "elemental", 42, 2, 0.06, 0, "A drifting spark of nature spirit, held in a glass-like skin."),
    ("ember_heart", "Ember Heart", "elemental", 58, 3, 0.035, 1, "A coal that is never quite out."),
    ("beast_fang", "Beast Fang", "beast", 14, 1, 0.14, 0, "A long fang from a dungeon beast."),
    ("drake_scale", "Drake Scale", "beast", 52, 3, 0.05, 1, "A hand-sized scale, tougher than most steel plate."),
    ("tide_pearl", "Tide Pearl", "aquatic", 36, 2, 0.06, 0, "A pearl that stays wet."),
    ("kraken_ink", "Kraken Ink", "aquatic", 40, 2, 0.05, 1, "Blue-black ink in a sealed gland."),
    ("chitin_plate", "Chitin Plate", "insect", 16, 1, 0.14, 0, "A curved plate of insect shell."),
    ("venom_sac", "Venom Sac", "insect", 24, 2, 0.08, 0, "A tight gland of slow venom. Handle with a glove."),
]

# ---------------------------------------------------------------- crafted: (id, name, value, lore)
CRAFTED = [
    ("shadowsteel_ingot", "Shadowsteel Ingot", 140, "Iron folded with a shard of shadow: dark, light and cold."),
]
AMMO = [
    ("bone_arrow", "Bone Arrow", 1, "An arrow with a bone-shard head. Hits a little harder than flint."),
]

# ---------------------------------------------------------------- foods: (id, name, kind, nutrition, saturation, effects, mana, aura, drops, value, lore, eat_seconds)
# effects: (effect id, seconds, amplifier); drops: (family or "any", chance, minBand)
FOODS = [
    ("dungeon_ration_stew", "Dungeon Ration Stew", "food", 8, 0.8, [("minecraft:regeneration", 10, 0)], 0, 0, [("any", 0.05, 0)], 20, "Thick, salty and exactly what a delver needs after a long floor.", 1.6),
    ("goblin_ear_jerky", "Goblin-Ear Jerky", "food", 4, 0.5, [("minecraft:speed", 30, 0)], 0, 0, [("goblin", 0.06, 0)], 10, "Dried, peppered and chewy. Tastes better than it sounds.", 1.2),
    ("adventurers_hardtack", "Adventurer's Hardtack", "food", 5, 0.4, [("minecraft:absorption", 60, 0)], 0, 0, [("any", 0.05, 0)], 8, "Keeps for a year and breaks teeth for two.", 1.2),
    ("slime_jelly_pudding", "Slime Jelly Pudding", "food", 4, 0.6, [("minecraft:regeneration", 12, 0), ("minecraft:absorption", 30, 0)], 0, 0, [("slime", 0.07, 0)], 14, "Wobbling, sweet and perfectly clear.", 1.4),
    ("mandrake_stew", "Mandrake Stew", "food", 7, 0.8, [("minecraft:strength", 60, 0)], 0, 0, [("elemental", 0.05, 0)], 24, "The root screams only if you hurry the pot.", 1.6),
    ("walking_mushroom_skewer", "Walking Mushroom Skewer", "food", 5, 0.6, [("minecraft:speed", 45, 0)], 0, 0, [("insect", 0.05, 0)], 12, "Caught on a floor with fungal gardens, grilled before it can wander off.", 1.2),
    ("orc_haunch_roast", "Orc-Haunch Roast", "food", 10, 1.0, [("minecraft:strength", 90, 0)], 0, 0, [("ogre", 0.07, 0)], 30, "A big roast for a big appetite.", 1.8),
    ("wyvern_steak", "Wyvern Steak", "food", 12, 1.2, [("minecraft:resistance", 60, 0)], 0, 0, [("beast", 0.05, 1)], 60, "Rare, red inside and worth the climb.", 1.8),
    ("bone_broth", "Bone Broth", "food", 5, 0.5, [("minecraft:regeneration", 15, 0)], 0, 0, [("undead", 0.07, 0)], 12, "Simmered a full day from clean bones.", 1.4),
    ("kraken_fritter", "Kraken Fritter", "food", 6, 0.6, [("minecraft:water_breathing", 120, 0), ("minecraft:speed", 20, 0)], 0, 0, [("aquatic", 0.06, 0)], 26, "Golden outside, tender inside, salty like the deep.", 1.4),
    ("dryad_honey_cake", "Dryad Honey Cake", "food", 6, 0.7, [("minecraft:absorption", 90, 0), ("minecraft:regeneration", 6, 0)], 0, 0, [("beast", 0.04, 1)], 34, "Soft, sweet and warm from the grove.", 1.4),
    ("frostbite_sorbet", "Frostbite Sorbet", "food", 3, 0.4, [("minecraft:fire_resistance", 90, 0)], 0, 0, [("elemental", 0.05, 0)], 18, "So cold it fogs the bowl.", 1.2),
    ("ember_chili", "Ember Chili", "food", 6, 0.6, [("minecraft:strength", 45, 0), ("minecraft:fire_resistance", 45, 0)], 0, 0, [("demon", 0.05, 0)], 22, "One spoon and the floor feels cooler.", 1.6),
    ("cave_mushroom_soup", "Cave Mushroom Soup", "food", 6, 0.7, [("minecraft:night_vision", 120, 0), ("minecraft:regeneration", 5, 0)], 0, 0, [("insect", 0.05, 0), ("slime", 0.03, 0)], 16, "Glows faintly. So does the eater's grin.", 1.4),
    ("falna_cutlet", "Falna Cutlet", "food", 9, 0.9, [("minecraft:strength", 60, 0), ("minecraft:resistance", 30, 0)], 0, 0, [("ogre", 0.04, 1)], 48, "A breaded cutlet that the guild cooks serve to those who come back.", 1.8),
    ("hearth_bread", "Hearth Bread", "food", 5, 0.6, [("minecraft:absorption", 45, 0)], 0, 0, [("any", 0.05, 0)], 9, "Baked at the Hub on a hearth that never goes out.", 1.2),
    ("delvers_trail_mix", "Delver's Trail Mix", "food", 3, 0.3, [("minecraft:speed", 20, 0), ("minecraft:haste", 30, 0)], 0, 0, [("any", 0.05, 0)], 9, "Nuts, dried berries and a few things nobody asks about.", 0.9),
    ("golem_stone_biscuit", "Golem-Stone Biscuit", "food", 4, 0.4, [("minecraft:resistance", 45, 0)], 0, 0, [("construct", 0.06, 0)], 12, "Hard as its name. Dip it.", 1.2),
    ("spirit_tea", "Spirit Tea", "food", 2, 0.2, [("nusmp:mana_surge", 90, 0), ("minecraft:regeneration", 4, 0)], 0, 0, [("elemental", 0.05, 0), ("cultist", 0.03, 0)], 28, "Steeped in a wisp's glow.", 1.4),
    ("mana_elixir", "Mana Elixir", "elixir", 1, 0.0, [("nusmp:mana_surge", 20, 0)], 25, 0, [("any", 0.04, 0), ("undead", 0.03, 0)], 40, "Restores a quarter of your magicules.", 1.0),
    ("greater_mana_elixir", "Greater Mana Elixir", "elixir", 1, 0.0, [("nusmp:mana_surge", 40, 0)], 50, 0, [("any", 0.02, 2), ("cultist", 0.03, 2)], 110, "Restores half of your magicules.", 1.0),
    ("aura_tonic", "Aura Tonic", "elixir", 1, 0.0, [("minecraft:absorption", 20, 0)], 0, 40, [("construct", 0.03, 1), ("ogre", 0.03, 1)], 60, "Restores two fifths of your aura.", 1.0),
    ("wisp_draught", "Wisp Draught", "elixir", 1, 0.0, [("minecraft:speed", 40, 0)], 15, 0, [("elemental", 0.04, 0)], 34, "A little magicule and a lot of lightness.", 1.0),
    ("starlit_nectar", "Starlit Nectar", "elixir", 1, 0.0, [("minecraft:regeneration", 10, 0)], 40, 0, [("elemental", 0.015, 3), ("cultist", 0.015, 3)], 180, "Restores two fifths of your magicules and mends a little.", 1.0),
    ("sages_elixir", "Sage's Elixir", "elixir", 1, 0.0, [("nusmp:mana_surge", 60, 0)], 70, 30, [("any", 0.01, 3)], 320, "Restores most of your magicules and some aura. Take it slowly.", 1.2),
    ("healers_vial", "Healer's Vial", "food", 1, 0.0, [("minecraft:instant_health", 1, 0), ("minecraft:regeneration", 8, 0)], 0, 0, [("any", 0.05, 0)], 36, "A mouthful of bright red restoration.", 1.0),
    ("fortitude_flask", "Fortitude Flask", "food", 1, 0.0, [("minecraft:absorption", 60, 1)], 0, 0, [("any", 0.03, 1), ("slime", 0.03, 1)], 46, "Two extra hearts of padding for a minute.", 1.0),
    ("swiftfoot_tonic", "Swiftfoot Tonic", "food", 1, 0.0, [("minecraft:speed", 45, 1), ("minecraft:jump_boost", 45, 0)], 0, 0, [("beast", 0.03, 0), ("insect", 0.03, 0)], 38, "Light feet and a spring in the step.", 1.0),
    ("berserkers_draught", "Berserker's Draught", "food", 1, 0.0, [("minecraft:strength", 30, 1)], 0, 0, [("demon", 0.025, 2), ("ogre", 0.025, 2)], 70, "Hits harder for half a minute, and tastes of iron.", 1.0),
    ("bulwark_tonic", "Bulwark Tonic", "food", 1, 0.0, [("minecraft:resistance", 30, 1), ("minecraft:absorption", 30, 0)], 0, 0, [("construct", 0.025, 2), ("ogre", 0.025, 2)], 70, "A wall in a bottle.", 1.0),
]

# ---------------------------------------------------------------- gear
# weapons: (id, name, type, speed, durability, value, primary stat, family, chance, minBand, lore, engravings)
WEAPONS = [
    ("goblin_fang_dagger", "Goblin-Fang Dagger", "dagger", 2.2, 320, 60, "goblin", 0.020, 0, "Two goblin teeth for a point and a bad temper for an edge."),
    ("delvers_longsword", "Delver's Longsword", "longsword", 1.6, 520, 90, "any", 0.015, 0, "The plain blade of a hundred nameless delvers."),
    ("bone_lance", "Bone Lance", "spear", 1.3, 420, 110, "undead", 0.018, 0, "A shaft tipped with a long splinter of grave bone."),
    ("orc_cleaver", "Orc Cleaver", "battleaxe", 1.0, 560, 140, "ogre", 0.018, 0, "Heavy enough to ring a bell with the flat."),
    ("shadowsteel_katana", "Shadowsteel Katana", "katana", 1.8, 600, 260, "demon", 0.012, 1, "Folded with a shard of shadow; it drinks the light along its edge."),
    ("drakefang_greatsword", "Drakefang Greatsword", "greatsword", 1.0, 700, 300, "beast", 0.012, 1, "A slab of steel set with drake fang and scale."),
    ("wisp_staff", "Wisp Staff", "staff", 1.4, 360, 180, "elemental", 0.015, 0, "A twisted staff that holds a spirit wisp like a lantern."),
    ("golem_maul", "Golem Maul", "hammer", 0.9, 640, 220, "construct", 0.015, 0, "A stone head on an iron haft, still faintly warm."),
]
WEAPON_ENGRAVINGS = {
    "goblin_fang_dagger": [("swift", 1), ("severance", 1)],
    "delvers_longsword": [("sturdy", 1), ("severance", 1)],
    "bone_lance": [("soul_eater", 1), ("enervation", 1), ("barrier_piercing", 1)],
    "orc_cleaver": [("crushing", 2), ("sturdy", 1)],
    "shadowsteel_katana": [("severance", 2), ("swift", 1), ("barrier_piercing", 1)],
    "drakefang_greatsword": [("elemental_boost", 1), ("crushing", 1), ("severance", 1)],
    "wisp_staff": [("magic_weapon", 1), ("magicule_absorption", 1), ("swift", 1)],
    "golem_maul": [("crushing", 2), ("sturdy", 2), ("magic_interference", 1)],
}
# armour: (id, name, set, piece, defense, toughness, durability factor, value, lore); the set's material is registered once
ARMOUR_SETS = {
    "drakescale": {"name": "Drake-Scale", "repair": "drake_scale", "toughness": 1.0, "knockback": 0.0, "enchant": 12, "family": "beast", "chance": 0.010, "minBand": 1},
    "shadowsteel": {"name": "Shadowsteel", "repair": "shadowsteel_ingot", "toughness": 2.0, "knockback": 0.05, "enchant": 16, "family": "demon", "chance": 0.008, "minBand": 2},
}
ARMOUR = [
    ("drakescale_helm", "Drake-Scale Helm", "drakescale", "head", 2, 90, "Overlapping scales over a leather cap."),
    ("drakescale_vest", "Drake-Scale Vest", "drakescale", "chest", 6, 180, "A vest of drake scales sewn in rows."),
    ("drakescale_greaves", "Drake-Scale Greaves", "drakescale", "legs", 5, 140, "Scale-plated leggings that do not rattle."),
    ("drakescale_boots", "Drake-Scale Boots", "drakescale", "feet", 2, 80, "Scale-capped boots with chitin soles."),
    ("shadowsteel_helm", "Shadowsteel Helm", "shadowsteel", "head", 3, 200, "A dark helm with a slit that seems deeper than it should."),
    ("shadowsteel_cuirass", "Shadowsteel Cuirass", "shadowsteel", "chest", 8, 380, "A black cuirass that gives back no reflection."),
    ("shadowsteel_greaves", "Shadowsteel Greaves", "shadowsteel", "legs", 6, 300, "Light, dark and cold to the knee."),
    ("shadowsteel_sabatons", "Shadowsteel Sabatons", "shadowsteel", "feet", 3, 190, "Footsteps in these make no echo."),
]
# trinkets: (id, name, curios slot, primary stat, value, family, chance, minBand, lore)
TRINKETS = [
    ("goblin_ear_ward", "Goblin Ear Ward", "charm", "max_health", 70, "goblin", 0.010, 0, "Three ears on a cord. Goblins are said to leave the wearer alone; they do not."),
    ("orc_tusk_ring", "Orc Tusk Ring", "ring", "crit_chance", 90, "ogre", 0.010, 0, "A ring carved from a tusk and bound in iron."),
    ("lich_dust_pendant", "Lich Dust Pendant", "necklace", "mana", 150, "undead", 0.008, 1, "A glass drop of lich dust on a chain."),
    ("wisp_lantern", "Wisp Lantern", "charm", "mana_regen", 130, "elemental", 0.008, 0, "A small lantern that keeps a wisp company."),
    ("golem_core_belt", "Golem Core Belt", "belt", "armor", 160, "construct", 0.008, 1, "A wide belt buckled round a golem core."),
    ("wraith_cloak", "Wraith Cloak", "back", "move_speed", 190, "undead", 0.006, 2, "A cloak of cursed cloth that moves a half-step ahead of you."),
    ("cultist_sigil_ring", "Cultist Sigil Ring", "ring", "mana", 110, "cultist", 0.008, 1, "A ring stamped with a cultist's runes."),
    ("tide_pearl_amulet", "Tide Pearl Amulet", "necklace", "max_health", 120, "aquatic", 0.008, 0, "A pearl on a silver wire; it is always cool."),
]

# ---------------------------------------------------------------- recipes
# each: (name, type, pattern/ingredients, result id, result count). Ingredients are item ids or #tags. Shapes use a key map.
RECIPES = [
    # materials into trinkets
    ("goblin_ear_ward", "shapeless", ["nusmp:goblin_ear", "nusmp:goblin_ear", "nusmp:goblin_ear", "minecraft:string", "minecraft:iron_nugget"], "nusmp:goblin_ear_ward", 1),
    ("orc_tusk_ring", "shaped", (" T ", "I I", " I "), {"T": "nusmp:orc_tusk", "I": "minecraft:iron_ingot"}, "nusmp:orc_tusk_ring", 1),
    ("lich_dust_pendant", "shaped", (" S ", "G G", " L "), {"S": "minecraft:string", "G": "minecraft:gold_nugget", "L": "nusmp:lich_dust"}, "nusmp:lich_dust_pendant", 1),
    ("wisp_lantern", "shaped", (" I ", "GWG", " I "), {"I": "minecraft:iron_nugget", "G": "minecraft:glass_pane", "W": "nusmp:spirit_wisp"}, "nusmp:wisp_lantern", 1),
    ("golem_core_belt", "shaped", ("LLL", "HCH", "LLL"), {"L": "minecraft:leather", "H": "nusmp:troll_hide", "C": "nusmp:golem_core"}, "nusmp:golem_core_belt", 1),
    ("wraith_cloak", "shaped", ("WCW", "CCC", "C C"), {"W": "nusmp:wraith_essence", "C": "nusmp:cursed_cloth"}, "nusmp:wraith_cloak", 1),
    ("cultist_sigil_ring", "shaped", (" S ", "G G", " G "), {"S": "nusmp:cultist_sigil", "G": "minecraft:gold_ingot"}, "nusmp:cultist_sigil_ring", 1),
    ("tide_pearl_amulet", "shaped", (" W ", "IPI", " I "), {"W": "minecraft:string", "I": "minecraft:iron_nugget", "P": "nusmp:tide_pearl"}, "nusmp:tide_pearl_amulet", 1),
    # materials into weapons
    ("goblin_fang_dagger", "shaped", ("T", "T", "S"), {"T": "nusmp:goblin_tooth", "S": "minecraft:stick"}, "nusmp:goblin_fang_dagger", 1),
    ("delvers_longsword", "shaped", ("I", "I", "G"), {"I": "minecraft:iron_ingot", "G": "nusmp:ghoul_claw"}, "nusmp:delvers_longsword", 1),
    ("bone_lance", "shaped", ("  B", " S ", "S  "), {"B": "nusmp:bone_shard", "S": "minecraft:bone"}, "nusmp:bone_lance", 1),
    ("orc_cleaver", "shaped", ("TIT", "TS ", " S "), {"T": "nusmp:orc_tusk", "I": "minecraft:iron_ingot", "S": "minecraft:stick"}, "nusmp:orc_cleaver", 1),
    ("shadowsteel_katana", "shaped", (" SH", " S ", "I  "), {"S": "nusmp:shadowsteel_ingot", "H": "nusmp:imp_horn", "I": "minecraft:stick"}, "nusmp:shadowsteel_katana", 1),
    ("drakefang_greatsword", "shaped", (" FD", "FDF", "IF "), {"F": "nusmp:beast_fang", "D": "nusmp:drake_scale", "I": "minecraft:iron_ingot"}, "nusmp:drakefang_greatsword", 1),
    ("wisp_staff", "shaped", (" W ", " S ", " S "), {"W": "nusmp:spirit_wisp", "S": "minecraft:stick"}, "nusmp:wisp_staff", 1),
    ("golem_maul", "shaped", ("GCG", " S ", " S "), {"G": "nusmp:rusted_cog", "C": "nusmp:golem_core", "S": "minecraft:iron_ingot"}, "nusmp:golem_maul", 1),
    # armour
    ("drakescale_helm", "shaped", ("DDD", "L L"), {"D": "nusmp:drake_scale", "L": "minecraft:leather"}, "nusmp:drakescale_helm", 1),
    ("drakescale_vest", "shaped", ("D D", "DLD", "DDD"), {"D": "nusmp:drake_scale", "L": "minecraft:leather"}, "nusmp:drakescale_vest", 1),
    ("drakescale_greaves", "shaped", ("DLD", "D D", "L L"), {"D": "nusmp:drake_scale", "L": "minecraft:leather"}, "nusmp:drakescale_greaves", 1),
    ("drakescale_boots", "shaped", ("D D", "C C"), {"D": "nusmp:drake_scale", "C": "nusmp:chitin_plate"}, "nusmp:drakescale_boots", 1),
    ("shadowsteel_helm", "shaped", ("SSS", "S S"), {"S": "nusmp:shadowsteel_ingot"}, "nusmp:shadowsteel_helm", 1),
    ("shadowsteel_cuirass", "shaped", ("S S", "SSS", "SSS"), {"S": "nusmp:shadowsteel_ingot"}, "nusmp:shadowsteel_cuirass", 1),
    ("shadowsteel_greaves", "shaped", ("SSS", "S S", "S S"), {"S": "nusmp:shadowsteel_ingot"}, "nusmp:shadowsteel_greaves", 1),
    ("shadowsteel_sabatons", "shaped", ("S S", "S S"), {"S": "nusmp:shadowsteel_ingot"}, "nusmp:shadowsteel_sabatons", 1),
    # crafted materials and ammo
    ("shadowsteel_ingot", "shapeless", ["#nusmp:loot/shadow_cores", "minecraft:iron_ingot"], "nusmp:shadowsteel_ingot", 1),
    ("bone_arrow", "shaped", ("B", "S", "F"), {"B": "nusmp:bone_shard", "S": "minecraft:stick", "F": "minecraft:feather"}, "nusmp:bone_arrow", 4),
    # foods and elixirs
    ("mana_elixir", "shapeless", ["nusmp:lich_dust", "nusmp:lich_dust", "minecraft:glass_bottle", "minecraft:lapis_lazuli"], "nusmp:mana_elixir", 1),
    ("slime_jelly_pudding", "shapeless", ["nusmp:slime_gel", "nusmp:slime_gel", "minecraft:sugar", "minecraft:egg"], "nusmp:slime_jelly_pudding", 2),
    ("bone_broth", "shapeless", ["nusmp:bone_shard", "nusmp:bone_shard", "minecraft:bowl"], "nusmp:bone_broth", 1),
    ("ember_chili", "shapeless", ["nusmp:ember_heart", "minecraft:bowl", "minecraft:red_mushroom", "minecraft:carrot"], "nusmp:ember_chili", 2),
    ("spirit_tea", "shapeless", ["nusmp:spirit_wisp", "minecraft:glass_bottle", "minecraft:sugar"], "nusmp:spirit_tea", 1),
    ("kraken_fritter", "shapeless", ["nusmp:kraken_ink", "minecraft:wheat", "minecraft:egg"], "nusmp:kraken_fritter", 2),
    ("fortitude_flask", "shapeless", ["nusmp:gel_core", "minecraft:glass_bottle", "minecraft:golden_carrot"], "nusmp:fortitude_flask", 1),
    ("healers_vial", "shapeless", ["nusmp:imp_horn", "minecraft:glass_bottle", "minecraft:glistering_melon_slice"], "nusmp:healers_vial", 1),
    ("swiftfoot_tonic", "shapeless", ["nusmp:beast_fang", "minecraft:glass_bottle", "minecraft:sugar", "minecraft:rabbit_foot"], "nusmp:swiftfoot_tonic", 1),
    ("berserkers_draught", "shapeless", ["nusmp:troll_hide", "nusmp:imp_horn", "minecraft:glass_bottle", "minecraft:blaze_powder"], "nusmp:berserkers_draught", 1),
    ("orc_haunch_roast", "shapeless", ["nusmp:orc_tusk", "minecraft:cooked_beef", "minecraft:bowl"], "nusmp:orc_haunch_roast", 2),
]

# materials that are used by code, not (only) by a recipe: id -> a short description. The data test reads this list: every material has a recipe or an entry here.
CODE_USES = {
    "shadow_core_shard": "Anvil: tempers a piece of loot gear with one extra stat line (once).",
    "golem_core": "Anvil: fully repairs any damaged tool, weapon or armour.",
    "venom_sac": "Anvil: coats a weapon so its next twenty strikes poison.",
}
# every material and food can be sold to a Hub Merchant (sneak + use on the Merchant): MaterialSell. The price is the item's own `value`.

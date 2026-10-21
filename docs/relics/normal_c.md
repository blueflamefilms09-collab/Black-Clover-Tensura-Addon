# Wave 105 (1.111.0, key normal_c): 121 Normal relics

Owner request: balance the grade pyramid by ADDING relics (Normal about 300); nothing is regraded, moved or removed. This wave adds **121 Normal** relics with a utility theme: charms, tokens, seals, guild / academy / kingdom badges, pouches, lamps, compasses, keys, scroll cases and trade-tool charms (the smith's hammer, the healer's kit, the cook's ladle, the miner's lamp, the scout's cloak), the basic HEAD / CHEST / FEET gear of the worlds, and trinkets made from monster drops (goblin teeth, orc tusks, lizardman scales, wolf fangs), plus 4 plain melee weapons (the limit was 12). Spread over 35 worlds: Attack on Titan, Black Clover, Bleach, Delicious in Dungeon, Demon Slayer, Dragon Ball, Dungeons and Dragons, Fairy Tail, Fate/stay night, Frieren: Beyond Journey's End, Fullmetal Alchemist, Goblin Slayer, Hunter x Hunter, Is It Wrong to Try to Pick Up Girls in a Dungeon?, Jujutsu Kaisen, KonoSuba, Made in Abyss, Magi: The Labyrinth of Magic, Mashle: Magic and Muscles, Monster Hunter, Mushoku Tensei, My Hero Academia, Naruto, One Piece, Overlord, Re:Zero, Solo Leveling, Spice and Wolf, Sword Art Online, That Time I Got Reincarnated as a Slime, The Eminence in Shadow, The Rising of the Shield Hero, The Seven Deadly Sins, Tower of God, Yu-Gi-Oh!.

Status: **NEW** (all 121). **REPLACED**: none. **KEPT**: every existing relic, id, grade and file. Built from `tools/relics/new_normal_c/` (`rows.py` gear, `weapons.py` weapons, `gen.py`, `models.py`, `presets.py`).

Normal grade: one or two small passives or one simple act (1% magicules); weapons 20-30 damage. Every act is a shape of `Wave31Acts`; nothing touches the food bar. Weapons carry Tensura engravings and are real 3D meshes with the old-style sprite as inventory icon, plus Better Combat files.

| id | name | grade | slot | world | passive | acts |
|---|---|---|---|---|---|---|
| `corps_foot_soldiers_haori` | Corps Foot Soldier's Haori | Normal | BACK | Demon Slayer | +0.04 attack speed | none |
| `deckhands_oilskin_cape` | Deckhand's Oilskin Cape | Normal | BACK | One Piece | *0.1 swim speed | none |
| `dire_wolf_pelt_mantle` | Dire Wolf Pelt Mantle | Normal | BACK | That Time I Got Reincarnated as a Slime | 12% less cold damage | none |
| `hunter_exam_travellers_poncho` | Hunter Exam Traveller's Poncho | Normal | BACK | Hunter x Hunter | *0.08 sneaking speed | none |
| `job_board_wanderers_duster` | Job Board Wanderer's Duster | Normal | BACK | Fairy Tail | *0.02 movement speed, +1 max health | none |
| `priestella_gondoliers_cape` | Priestella Gondolier's Cape | Normal | BACK | Re:Zero | 10% less cold damage | none |
| `rangers_moss_scout_cloak` | Ranger's Moss Scout Cloak | Normal | BACK | Dungeons and Dragons | *0.1 sneaking speed | none |
| `silver_rank_plain_cloak` | Silver Rank Plain Cloak | Normal | BACK | Goblin Slayer | 10% less proj damage | none |
| `squad_trainees_mantle` | Squad Trainee's Mantle | Normal | BACK | Black Clover | +1 armour | none |
| `wayfaring_pedlars_wagon_cape` | Wayfaring Pedlar's Wagon Cape | Normal | BACK | Spice and Wolf | 10% less cold damage | none |
| `cave_raiders_rope_belt` | Cave Raider's Rope Belt | Normal | BELT | Made in Abyss | +3 safe fall distance | none |
| `curse_tool_sash` | Curse Tool Sash | Normal | BELT | Jujutsu Kaisen | 12% less dark damage | none |
| `delvers_ration_belt` | Delver's Ration Belt | Normal | BELT | Delicious in Dungeon | +1 luck | none |
| `dwargon_smiths_tool_belt` | Dwargon Smith's Tool Belt | Normal | BELT | That Time I Got Reincarnated as a Slime | +0.5 attack damage | none |
| `gathering_hunters_belt` | Gathering Hunter's Belt | Normal | BELT | Monster Hunter | +2 max health | none |
| `hunter_guild_utility_belt` | Hunter Guild Utility Belt | Normal | BELT | Solo Leveling | *0.02 movement speed, +1 safe fall distance | none |
| `kunai_holster_belt` | Kunai Holster Belt | Normal | BELT | Naruto | +0.05 attack speed | none |
| `marine_cutlass_baldric` | Marine Cutlass Baldric | Normal | BELT | One Piece | +0.05 knockback resistance, +0.03 attack speed | none |
| `orc_hide_utility_belt` | Orc Hide Utility Belt | Normal | BELT | That Time I Got Reincarnated as a Slime | +0.08 knockback resistance | none |
| `regulars_shinsu_pouch_belt` | Regular's Shinsu Pouch Belt | Normal | BELT | Tower of God | +0.04 jump | none |
| `spell_component_belt` | Spell Component Belt | Normal | BELT | Dungeons and Dragons | **0.03 magicules | none |
| `turtle_school_weighted_belt` | Turtle School Weighted Belt | Normal | BELT | Dragon Ball | +0.08 knockback resistance | none |
| `boar_hat_tavern_token` | Boar Hat Tavern Token | Normal | CHARM | The Seven Deadly Sins | +2 max health | none |
| `capsule_case_charm` | Capsule Case Charm | Normal | CHARM | Dragon Ball | none | Pop the Capsule (90 s, 1%) |
| `card_sleeve_locket` | Card Sleeve Locket | Normal | CHARM | Yu-Gi-Oh! | +0.04 attack speed | none |
| `chunin_exam_scroll_case_charm` | Chunin Exam Scroll Case Charm | Normal | CHARM | Naruto | none | Quick Swap (70 s, 1%) |
| `cracked_mana_crystal` | Cracked Mana Crystal | Normal | CHARM | Solo Leveling | **0.03 magicules | none |
| `crimson_tassel_of_axel` | Crimson Tassel of Axel | Normal | CHARM | KonoSuba | none | Tiny Pop (60 s, 1%) |
| `delvers_brass_lamp` | Delver's Brass Lamp | Normal | CHARM | Made in Abyss | none | Lamp Glow (60 s, 1%) |
| `dungeon_cooks_ladle_charm` | Dungeon Cook's Ladle Charm | Normal | CHARM | Delicious in Dungeon | none | Hot Broth Sip (100 s, 1%) |
| `dwargon_smiths_hammer_charm` | Dwargon Smith's Hammer Charm | Normal | CHARM | That Time I Got Reincarnated as a Slime | +0.5 attack damage | none |
| `dwargon_vault_key_charm` | Dwargon Vault Key Charm | Normal | CHARM | That Time I Got Reincarnated as a Slime | +1 luck | none |
| `goblin_slayers_tooth_tally` | Goblin Slayer's Tooth Tally | Normal | CHARM | Goblin Slayer | +0.5 attack damage | none |
| `grand_line_berry_purse_charm` | Grand Line Berry Purse Charm | Normal | CHARM | One Piece | +1 luck | none |
| `half_senzu_bean_charm` | Half Senzu Bean Charm | Normal | CHARM | Dragon Ball | none | Half Bean (120 s, 1%) |
| `hell_butterfly_pin` | Hell Butterfly Pin | Normal | CHARM | Bleach | none | Messenger Flutter (80 s, 1%) |
| `hobgoblin_warband_token` | Hobgoblin Warband Token | Normal | CHARM | That Time I Got Reincarnated as a Slime | none | Warband Call (90 s, 1%) |
| `hotbar_potion_pouch` | Hotbar Potion Pouch | Normal | CHARM | Sword Art Online | none | Quaff Potion (100 s, 1%) |
| `hunter_exam_number_tag` | Hunter Exam Number Tag | Normal | CHARM | Hunter x Hunter | +1 luck | none |
| `jujutsu_high_warding_strip` | Jujutsu High Warding Strip | Normal | CHARM | Jujutsu Kaisen | 10% less curse damage | none |
| `lacrima_spark_lamp` | Lacrima Spark Lamp | Normal | CHARM | Fairy Tail | none | Lacrima Crackle (60 s, 1%) |
| `lizardman_marsh_scale_charm` | Lizardman Marsh Scale Charm | Normal | CHARM | That Time I Got Reincarnated as a Slime | +1 armour | none |
| `orario_delvers_compass` | Orario Delver's Compass | Normal | CHARM | Is It Wrong to Try to Pick Up Girls in a Dungeon? | none | Find Bearings (80 s, 1%) |
| `orc_disaster_tusk_charm` | Orc Disaster Tusk Charm | Normal | CHARM | That Time I Got Reincarnated as a Slime | +0.08 knockback resistance | none |
| `priests_poultice_kit_charm` | Priest's Poultice Kit Charm | Normal | CHARM | Frieren: Beyond Journey's End | none | Quick Poultice (90 s, 1%) |
| `roxys_water_pearl` | Roxy's Water Pearl | Normal | CHARM | Mushoku Tensei | *0.1 swim speed | none |
| `shadow_garden_cipher_coin` | Shadow Garden Cipher Coin | Normal | CHARM | The Eminence in Shadow | *0.05 sneaking speed | none |
| `shinobi_smoke_pellet_pouch` | Shinobi Smoke Pellet Pouch | Normal | CHARM | Naruto | none | Smoke Burst (60 s, 1%) |
| `soul_candy_pill_case` | Soul Candy Pill Case | Normal | CHARM | Bleach | none | Soul Candy (90 s, 1%) |
| `spare_mana_gem` | Spare Mana Gem | Normal | CHARM | Fate/stay night | **0.03 magicules | none |
| `starwolf_fang_pendant` | Starwolf Fang Pendant | Normal | CHARM | That Time I Got Reincarnated as a Slime | *0.03 movement speed | none |
| `state_alchemists_pocket_watch` | State Alchemist's Pocket Watch | Normal | CHARM | Fullmetal Alchemist | *0.02 movement speed | none |
| `survey_corps_signal_flare_charm` | Survey Corps Signal Flare Charm | Normal | CHARM | Attack on Titan | none | Flare Mark (80 s, 1%) |
| `wisteria_incense_sachet` | Wisteria Incense Sachet | Normal | CHARM | Demon Slayer | none | Wisteria Whiff (90 s, 1%) |
| `axel_adventurers_leather_coat` | Axel Adventurer's Leather Coat | Normal | CHEST | KonoSuba | +1 armour, +1 luck | none |
| `exam_examinees_waistcoat` | Exam Examinee's Waistcoat | Normal | CHEST | Hunter x Hunter | +1 armour, *0.02 movement speed | none |
| `gate_raiders_padded_coat` | Gate Raider's Padded Coat | Normal | CHEST | Solo Leveling | +1 armour, +2 max health | none |
| `genin_flak_vest` | Genin Flak Vest | Normal | CHEST | Naruto | +2 armour | none |
| `kou_soldiers_lamellar` | Kou Soldier's Lamellar | Normal | CHEST | Magi: The Labyrinth of Magic | +2 armour | none |
| `lizardman_scale_cuirass` | Lizardman Scale Cuirass | Normal | CHEST | That Time I Got Reincarnated as a Slime | +1 armour, *0.02 movement speed | none |
| `magic_knight_recruits_coat` | Magic Knight Recruit's Coat | Normal | CHEST | Black Clover | +1 armour, +1 max health | none |
| `marine_recruits_uniform` | Marine Recruit's Uniform | Normal | CHEST | One Piece | +1 armour, +0.03 knockback resistance | none |
| `orc_warband_scale_mail` | Orc Warband Scale Mail | Normal | CHEST | That Time I Got Reincarnated as a Slime | +2 armour | none |
| `priestella_guards_tabard` | Priestella Guard's Tabard | Normal | CHEST | Re:Zero | +1 armour, 10% less cold damage | none |
| `soul_reaper_shihakusho` | Soul Reaper Shihakusho | Normal | CHEST | Bleach | *0.02 movement speed, 10% less dark damage | none |
| `survey_corps_cadet_jacket` | Survey Corps Cadet Jacket | Normal | CHEST | Attack on Titan | +2 armour | none |
| `corps_straw_zori` | Corps Straw Zori | Normal | FEET | Demon Slayer | *0.08 sneaking speed | none |
| `delvers_rope_soled_boots` | Delver's Rope-Soled Boots | Normal | FEET | Made in Abyss | +3 safe fall distance | none |
| `dwargon_miners_steel_toe_boots` | Dwargon Miner's Steel-Toe Boots | Normal | FEET | That Time I Got Reincarnated as a Slime | +1 armour, +1 safe fall distance | none |
| `exam_marathon_shoes` | Exam Marathon Shoes | Normal | FEET | Hunter x Hunter | *0.03 movement speed | none |
| `kou_soldiers_marching_sandals` | Kou Soldier's Marching Sandals | Normal | FEET | Magi: The Labyrinth of Magic | *0.03 movement speed | none |
| `lizardman_marsh_greaves` | Lizardman Marsh Greaves | Normal | FEET | That Time I Got Reincarnated as a Slime | *0.1 swim speed | none |
| `magic_knight_recruits_boots` | Magic Knight Recruit's Boots | Normal | FEET | Black Clover | *0.02 movement speed, +1 safe fall distance | none |
| `marine_recruits_boots` | Marine Recruit's Boots | Normal | FEET | One Piece | +2 safe fall distance | none |
| `orc_hide_greaves` | Orc Hide Greaves | Normal | FEET | That Time I Got Reincarnated as a Slime | +0.08 knockback resistance | none |
| `shinobi_sandals` | Shinobi Sandals | Normal | FEET | Naruto | *0.03 movement speed | none |
| `soul_reaper_waraji` | Soul Reaper Waraji | Normal | FEET | Bleach | *0.08 sneaking speed | none |
| `survey_corps_cadet_boots` | Survey Corps Cadet Boots | Normal | FEET | Attack on Titan | +3 safe fall distance | none |
| `training_ground_sneakers` | Training Ground Sneakers | Normal | FEET | My Hero Academia | +0.04 jump | none |
| `archers_finger_guards` | Archer's Finger Guards | Normal | HANDS | Dungeons and Dragons | +0.3 reach | none |
| `beast_tamers_falconry_glove` | Beast Tamer's Falconry Glove | Normal | HANDS | Hunter x Hunter | +0.3 reach | none |
| `clerics_bandage_wraps` | Cleric's Bandage Wraps | Normal | HANDS | Frieren: Beyond Journey's End | +2 max health | none |
| `cooks_oven_mitts` | Cook's Oven Mitts | Normal | HANDS | Delicious in Dungeon | 15% less fire damage | none |
| `cradle_diggers_gloves` | Cradle Digger's Gloves | Normal | HANDS | Made in Abyss | +2 safe fall distance | none |
| `deckhands_rope_gloves` | Deckhand's Rope Gloves | Normal | HANDS | One Piece | *0.1 swim speed | none |
| `dwargon_forge_gloves` | Dwargon Forge Gloves | Normal | HANDS | That Time I Got Reincarnated as a Slime | 15% less fire damage | none |
| `genin_arm_guards` | Genin Arm Guards | Normal | HANDS | Naruto | +1 armour | none |
| `guild_skinners_gloves` | Guild Skinner's Gloves | Normal | HANDS | Monster Hunter | +1 armour | none |
| `sword_saint_trainees_grip_wrap` | Sword Saint Trainee's Grip Wrap | Normal | HANDS | Re:Zero | +0.04 attack speed | none |
| `thiefs_lockpick_gloves` | Thief's Lockpick Gloves | Normal | HANDS | Magi: The Labyrinth of Magic | *0.08 sneaking speed | none |
| `tournament_fist_wraps` | Tournament Fist Wraps | Normal | HANDS | Dragon Ball | +0.5 attack damage | none |
| `axel_apprentice_wizards_hat` | Axel Apprentice Wizard's Hat | Normal | HEAD | KonoSuba | **0.03 magicules | none |
| `corps_fox_mask_replica` | Corps Fox Mask Replica | Normal | HEAD | Demon Slayer | 10% less curse damage, +1 luck | none |
| `dungeon_cooks_bandana` | Dungeon Cook's Bandana | Normal | HEAD | Delicious in Dungeon | 12% less fire damage | none |
| `exam_hall_bandana` | Exam Hall Bandana | Normal | HEAD | Hunter x Hunter | +1 luck | none |
| `falmuth_footmans_kettle_helm` | Falmuth Footman's Kettle Helm | Normal | HEAD | The Rising of the Shield Hero | +2 armour | none |
| `gate_raiders_rank_helmet` | Gate Raider's Rank Helmet | Normal | HEAD | Solo Leveling | +1 armour, +0.03 knockback resistance | none |
| `hero_course_goggles` | Hero Course Goggles | Normal | HEAD | My Hero Academia | +0.04 jump | none |
| `hollow_mask_fragment` | Hollow Mask Fragment | Normal | HEAD | Bleach | 10% less holy damage | none |
| `leaf_village_forehead_protector` | Leaf Village Forehead Protector | Normal | HEAD | Naruto | +1 armour, +1 max health | none |
| `magnoshutatt_student_cap` | Magnoshutatt Student Cap | Normal | HEAD | Magi: The Labyrinth of Magic | **0.03 magicules | none |
| `marine_recruits_cap` | Marine Recruit's Cap | Normal | HEAD | One Piece | +2 max health | none |
| `survey_corps_cadet_cap` | Survey Corps Cadet Cap | Normal | HEAD | Attack on Titan | +0.05 knockback resistance | none |
| `ahjin_guild_pin` | Ahjin Guild Pin | Normal | NECK | Solo Leveling | +2 max health | none |
| `blumund_adventurer_tag` | Blumund Adventurer Tag | Normal | NECK | That Time I Got Reincarnated as a Slime | +1 armour | none |
| `clover_kingdom_citizen_seal` | Clover Kingdom Citizen Seal | Normal | NECK | Black Clover | +1 luck | none |
| `copper_adventurer_plate_of_e_rantel` | Copper Adventurer Plate of E-Rantel | Normal | NECK | Overlord | +2 safe fall distance | none |
| `dwargon_merchant_guild_seal` | Dwargon Merchant Guild Seal | Normal | NECK | That Time I Got Reincarnated as a Slime | +1 luck | none |
| `easton_academy_crest` | Easton Academy Crest | Normal | NECK | Mashle: Magic and Muscles | +1 armour | none |
| `harpers_brooch_pin` | Harper's Brooch Pin | Normal | NECK | Dungeons and Dragons | *0.05 sneaking speed | none |
| `hero_course_badge` | Hero Course Badge | Normal | NECK | My Hero Academia | +2 max health | none |
| `lugunica_royal_guard_crest` | Lugunica Royal Guard Crest | Normal | NECK | Re:Zero | +0.05 knockback resistance | none |
| `magnoshutatt_academy_medal` | Magnoshutatt Academy Medal | Normal | NECK | Magi: The Labyrinth of Magic | **0.03 magicules | none |
| `marine_recruit_badge` | Marine Recruit Badge | Normal | NECK | One Piece | +0.05 knockback resistance | none |
| `seireitei_academy_badge` | Seireitei Academy Badge | Normal | NECK | Bleach | +2 safe fall distance | none |
| `tempest_federation_citizen_medal` | Tempest Federation Citizen Medal | Normal | NECK | That Time I Got Reincarnated as a Slime | +2 max health | none |
| `wings_of_freedom_pin` | Wings of Freedom Pin | Normal | NECK | Attack on Titan | +2 max health | none |
| `orc_tusk_cleaver` | Orc Tusk Cleaver | Normal | MAIN_HAND (shortsword, 23 dmg, 1.6/s) | That Time I Got Reincarnated as a Slime | +0.05 knockback resistance | Tusk Chop (25 s, 1%) (engravings: crushing) |
| `direwolf_fang_spear` | Direwolf Fang Spear | Normal | MAIN_HAND (spear, 25.5 dmg, 1.3/s) | That Time I Got Reincarnated as a Slime | +0.4 reach | Fang Thrust (30 s, 1%) (engravings: swift) |
| `delvers_pickaxe` | Delver's Pickaxe | Normal | MAIN_HAND (handaxe, 24 dmg, 1.4/s) | Made in Abyss | +0.5 attack damage | Pick Strike (30 s, 1%) (engravings: severance) |
| `dungeon_cooks_ladle` | Dungeon Cook's Ladle | Normal | MAIN_HAND (hammer, 27.5 dmg, 1.1/s) | Delicious in Dungeon | +2 max health | Ladle Bonk (35 s, 1%) (engravings: sturdy) |

## Weapon models

* `orc_tusk_cleaver`: A curved, creamy ivory tusk blade with a ridged edge, a wrapped sinew grip, a small iron cap and a loop of cord at the pommel.
* `direwolf_fang_spear`: A long ash pole with a curved ivory fang head, a wrap of grey fur under the head, a leather-bound grip and an iron butt cap.
* `delvers_pickaxe`: A short oak haft with a long steel pick on one side and a small flat hammer face on the other, bound in rope and capped with iron.
* `dungeon_cooks_ladle`: A long wooden handle with an iron bowl at the end, a hanging hook on the haft, a leather strap and a dented rim.

## Files and hooks

* New: `relics/wave/Wave105.java` (defs, `IDS`, self-test `relic.normal_c`), `tools/lang/normal_c.json`, sprites and item models (`textures/item`, `models/item`), worn ornaments `relic_worn/uq_<id>.txt` (117), 4 weapon meshes and skins, 4 Better Combat files, `Wave105DefsTest`.
* Appended to shared files (append-only; other builders touch the same files, the lead regenerates them after merging): `RelicCatalog.java` (two `guard(...)` lines), `relic_worn/catalog.txt` (117 `relic` lines, from `gen_worn_all.py`), the Curios tag files, both `weapon_classes.json` (4 rows), `tools/bettercombat/manifest.json`, `tools/relics/display_presets_manifest.json`, and one hook in `tools/relics/gen_relic_waves.py` (`all_rows()` adds the 117 rows so the worn library builds their ornaments).
* Not touched: version, CHANGELOG, README, LIBRARIES, `en_us.json`, `.github`, the dungeon road table (`relic_themes.json`, a missing entry falls back to the id hash).
* Loot: reachable at their grade through the existing grade pools (`RelicLoot`); no new loot table.

## Not verified (nothing was run in a game)

* How the 117 worn looks and 4 hand models look in game (only the mesh previews of the weapons were rendered); the looks of Normal gear are deliberately simple.
* The in-game self-test `relic.normal_c`, the act effects and their balance in play, the Better Combat swings of the 4 weapons.
* The client-class tests (`RelicHandCoverageTest`, `RelicRenderCoverageTest`, `WornCoverageTest`) need Minecraft client classes the offline shim lacks; their logic was checked through the catalog lines and files instead. `RelicUniqueLooksTest` only reads waves 31-80; the same checks (unique sprite, ornament, palette) were run on this wave by script.
* `gen_worn_all.py` ends with the visual-upgrade step; check that it did not rewrite shared textures.
* Names of Wave 104 (built in parallel) are not visible to this wave; every name here is tied to a franchise place, guild or creature to avoid generic clashes.
* Grade pyramid note: no existing relic was reported as mis-graded by this wave.

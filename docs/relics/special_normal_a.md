# Wave 103 (1.111.0, key special_normal_a): 25 Special and 50 Normal relics

Owner request: balance the grade pyramid by ADDING relics (Normal about 300, Special about 141); nothing is regraded, moved or removed. This wave adds **50 Normal** and **25 Special** relics: everyday adventurer gear and trinkets (packs, lamps, charms, cloaks, boots, caps, belts, gloves, coats) and 8 plain weapons, from Tensura, Black Clover, Magi, DanMachi, Fairy Tail, Shield Hero, Dungeons and Dragons, Solo Leveling, Re:Zero, KonoSuba, Overlord, Sword Art Online, Fate, Naruto, Fullmetal Alchemist, One Piece, Hunter x Hunter, Dragon Ball, Bleach, Frieren, Delicious in Dungeon, Yu-Gi-Oh!, The Seven Deadly Sins and original ones.

Status: **NEW** (all 75). **REPLACED**: none. **KEPT**: every existing relic, id, grade and file. Built from `tools/relics/new_special_normal_a/` (`rows.py` gear, `weapons.py` weapons, `gen.py`, `models.py`, `presets.py`).

Normal grade: one or two small passives and at most one cheap act (1% magicules); weapons 20-30 damage. Special grade: two or three small passives, one or two acts (2-3%); weapons 35-55 damage. Every act is a shape of `Wave31Acts`; nothing touches the food bar. Weapons carry Tensura engravings and are real 3D meshes with the old-style sprite as inventory icon, plus Better Combat files.

| id | name | grade | slot | world | passive | acts |
|---|---|---|---|---|---|---|
| `adventurers_hooded_cloak` | Adventurer's Hooded Cloak | Normal | BACK | Dungeons and Dragons | *0.1 sneaking speed, 10% less cold damage | none |
| `great_jura_travelling_cloak` | Great Jura Travelling Cloak | Normal | BACK | That Time I Got Reincarnated as a Slime | *0.03 movement speed, 10% less cold damage | none |
| `supporters_rucksack` | Supporter's Rucksack | Normal | BACK | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +0.05 knockback resistance, +2 max health | none |
| `alchemists_chalk_pouch` | Alchemist's Chalk Pouch | Normal | BELT | Fullmetal Alchemist | none | Quick Circle (100 s, 1%) |
| `duelists_deck_box_belt` | Duelist's Deck Box Belt | Normal | BELT | Yu-Gi-Oh! | +0.05 attack speed, +1 luck | none |
| `gate_scavengers_satchel` | Gate Scavenger's Satchel | Normal | BELT | Solo Leveling | +1 luck, +2 max health | none |
| `medicine_peddlers_satchel` | Medicine Peddler's Satchel | Normal | BELT | The Rising of the Shield Hero | none | Herb Poultice (100 s, 1%) |
| `nazarick_pages_sash` | Nazarick Page's Sash | Normal | BELT | Overlord | +2 max health, +0.05 knockback resistance | none |
| `street_thiefs_sash` | Street Thief's Sash | Normal | BELT | Magi: The Labyrinth of Magic | *0.03 movement speed, *0.1 sneaking speed | none |
| `beatrices_doll_charm` | Beatrice's Doll Charm | Normal | CHARM | Re:Zero | +1 luck | Doll's Warning (90 s, 1%) |
| `caravan_water_flask` | Caravan Water Flask | Normal | CHARM | Magi: The Labyrinth of Magic | none | Cool Mouthful (75 s, 1%) |
| `elf_village_herb_pouch` | Elf Village Herb Pouch | Normal | CHARM | Frieren: Beyond Journey's End | none | Calming Tea (100 s, 1%) |
| `everburning_torch_charm` | Everburning Torch Charm | Normal | CHARM | Dungeons and Dragons | 10% less fire damage | Torch Flick (45 s, 1%) |
| `familia_emblem_pin` | Familia Emblem Pin | Normal | CHARM | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +2 max health | Hearth Thought (120 s, 1%) |
| `filo_feather_charm` | Filo Feather Charm | Normal | CHARM | The Rising of the Shield Hero | none | Quick Stride (60 s, 1%) |
| `four_leaf_squires_pin` | Four-Leaf Squire's Pin | Normal | CHARM | Black Clover | +1 luck | none |
| `happys_fish_charm` | Happy's Fish Charm | Normal | CHARM | Fairy Tail | +0.04 jump, +1 luck | none |
| `kobold_miners_lamp` | Kobold Miner's Lamp | Normal | CHARM | That Time I Got Reincarnated as a Slime | none | Steady Light (60 s, 1%) |
| `little_slime_charm` | Little Slime Charm | Normal | CHARM | That Time I Got Reincarnated as a Slime | none | Soft Squeeze (90 s, 1%) |
| `wanderers_waystone` | Wanderer's Waystone | Normal | CHARM | Original | *0.02 movement speed | Rub the Stone (80 s, 1%) |
| `academy_students_black_robe` | Academy Student's Black Robe | Normal | CHEST | Bleach | +2 max health, 10% less dark damage | none |
| `chaldea_standard_mystic_code` | Chaldea Standard Mystic Code | Normal | CHEST | Fate/stay night | +1 armour, 10% less curse damage | none |
| `dungeon_cooks_apron` | Dungeon Cook's Apron | Normal | CHEST | Delicious in Dungeon | +1 armour, 15% less fire damage | none |
| `goblin_village_hide_vest` | Goblin Village Hide Vest | Normal | CHEST | That Time I Got Reincarnated as a Slime | +1 armour, +2 max health | none |
| `mansion_servants_apron` | Mansion Servant's Apron | Normal | CHEST | Re:Zero | +1 armour, +2 max health | none |
| `starter_town_leather_tunic` | Starter Town Leather Tunic | Normal | CHEST | Sword Art Online | +1 armour, *0.02 movement speed | none |
| `studded_leather_jerkin` | Studded Leather Jerkin | Normal | CHEST | Dungeons and Dragons | +2 armour | none |
| `association_rubber_boots` | Association Rubber Boots | Normal | FEET | Solo Leveling | +3 safe fall distance, +0.05 knockback resistance | none |
| `babel_street_sandals` | Babel Street Sandals | Normal | FEET | Is It Wrong to Try to Pick Up Girls in a Dungeon? | *0.1 sneaking speed, +2 safe fall distance | none |
| `hage_village_work_boots` | Hage Village Work Boots | Normal | FEET | Black Clover | *0.03 movement speed, +2 safe fall distance | none |
| `pilgrims_walking_boots` | Pilgrim's Walking Boots | Normal | FEET | Original | *0.04 movement speed | none |
| `bandaged_training_wraps` | Bandaged Training Wraps | Normal | HANDS | Black Clover | +0.5 attack damage, +0.05 knockback resistance | none |
| `e_rank_hunters_work_gloves` | E-Rank Hunter's Work Gloves | Normal | HANDS | Solo Leveling | +0.5 attack damage, +0.03 attack speed | none |
| `guild_mark_armband` | Guild Mark Armband | Normal | HANDS | Fairy Tail | none | Rally Cry (90 s, 1%) |
| `ring_of_minor_protection` | Ring of Minor Protection | Normal | HANDS | Dungeons and Dragons | +1 armour, 10% less magic damage | none |
| `rope_climbers_gloves` | Rope Climber's Gloves | Normal | HANDS | Original | +0.5 reach, +2 safe fall distance | none |
| `weighted_training_wristbands` | Weighted Training Wristbands | Normal | HANDS | Dragon Ball | +0.5 attack damage, +0.05 knockback resistance | none |
| `dwargon_smiths_leather_cap` | Dwargon Smith's Leather Cap | Normal | HEAD | That Time I Got Reincarnated as a Slime | 15% less fire damage, +1 armour | none |
| `east_blue_sailors_bandana` | East Blue Sailor's Bandana | Normal | HEAD | One Piece | *0.1 swim speed, +0.03 attack speed | none |
| `leaf_genin_forehead_protector` | Leaf Genin Forehead Protector | Normal | HEAD | Naruto | +1 armour, +0.03 attack speed | none |
| `melromarc_guards_coif` | Melromarc Guard's Coif | Normal | HEAD | The Rising of the Shield Hero | +1 armour, +0.05 knockback resistance | none |
| `novice_magic_knights_cap` | Novice Magic Knight's Cap | Normal | HEAD | Black Clover | **0.03 magicules, +1 luck | none |
| `axel_adventurers_scarf` | Axel Adventurer's Scarf | Normal | NECK | KonoSuba | +1 luck, 10% less fire damage | none |
| `hunter_exam_number_plate` | Hunter Exam Number Plate | Normal | NECK | Hunter x Hunter | +1 luck, +2 max health | none |
| `sindria_traders_scarf` | Sindria Trader's Scarf | Normal | NECK | Magi: The Labyrinth of Magic | *0.1 swim speed, 10% less fire damage | none |
| `d_rank_raiders_mantle` | D-Rank Raider's Mantle | Special | BACK | Solo Leveling | +2 armour, +4 max health, skill danger sense | Ready Stance (80 s, 2%) |
| `desert_dancers_veil_cape` | Desert Dancer's Veil Cape | Special | BACK | Magi: The Labyrinth of Magic | *0.05 movement speed, *0.1 sneaking speed, 20% less fire damage | Veil Step (50 s, 2%) |
| `adventurers_mana_flask_belt` | Adventurer's Mana-Flask Belt | Special | BELT | KonoSuba | **0.08 magicules, +2 max health | Mana Sip (100 s, 2%) |
| `guild_veterans_sash` | Guild Veteran's Sash | Special | BELT | Mushoku Tensei | +4 max health, +1 armour, +0.1 knockback resistance | Old Campaigner (80 s, 2%) |
| `compass_of_true_north` | Compass of True North | Special | CHARM | Dungeons and Dragons | *0.04 movement speed, +1 luck | True Heading (70 s, 2%) |
| `hi_potion_vial_bandolier` | Hi-Potion Vial Bandolier | Special | CHARM | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +2 max health | Hi-Potion (100 s, 2%); Quick Potion (120 s, 2%, sneak) |
| `quiet_mages_flower_locket` | Quiet Mage's Flower Locket | Special | CHARM | Frieren: Beyond Journey's End | 25% less magic damage, **0.05 magicules, +1 luck | Quiet Focus (90 s, 2%) |
| `spirit_whisper_bell` | Spirit Whisper Bell | Special | CHARM | That Time I Got Reincarnated as a Slime | +2 luck, skill magic sense | Ring the Bell (80 s, 2%) |
| `elven_chain_shirt` | Elven Chain Shirt | Special | CHEST | Dungeons and Dragons | +3 armour, *0.1 sneaking speed | Ring Ward (80 s, 2%) |
| `golden_dawn_recruits_coat` | Golden Dawn Recruit's Coat | Special | CHEST | Black Clover | +2 armour, +4 max health, +0.05 attack speed | Golden Rush (70 s, 2%) |
| `tortoise_shell_cuirass` | Tortoise-Shell Cuirass | Special | CHEST | The Rising of the Shield Hero | +3 armour, +0.1 knockback resistance, 15% less expl damage | Shell Up (90 s, 3%) |
| `beastkin_hunters_boots` | Beastkin Hunter's Boots | Special | FEET | That Time I Got Reincarnated as a Slime | *0.06 movement speed, *0.1 sneaking speed, +3 safe fall distance | Pounce (40 s, 2%) |
| `boots_of_the_winding_path` | Boots of the Winding Path | Special | FEET | Dungeons and Dragons | *0.05 movement speed, +4 safe fall distance, standing slow falling | Short Cut (60 s, 2%) |
| `kingdom_couriers_boots` | Kingdom Courier's Boots | Special | FEET | Re:Zero | *0.06 movement speed, +0.04 jump | Courier's Sprint (70 s, 2%) |
| `dragon_slayer_apprentice_bracers` | Dragon Slayer Apprentice Bracers | Special | HANDS | Fairy Tail | +1 attack damage, 25% less fire damage | Warm Fist (50 s, 2%) |
| `gate_raider_grip_gloves` | Gate-Raider Grip Gloves | Special | HANDS | Solo Leveling | +1 attack damage, +0.06 attack speed, +0.5 reach | Hard Grip (60 s, 2%) |
| `mana_skin_wrap` | Mana Skin Wrap | Special | HANDS | Black Clover | +2 armour, +1 attack damage, **0.04 magicules | Mana Skin (70 s, 2%) |
| `brumund_knight_captains_helm` | Brumund Knight-Captain's Helm | Special | HEAD | That Time I Got Reincarnated as a Slime | +2 armour, +0.1 knockback resistance, 15% less proj damage | Hold the Line (80 s, 2%) |
| `s_class_trial_hood` | S-Class Trial Hood | Special | HEAD | Fairy Tail | +4 max health, skill danger sense | Steady Nerve (90 s, 2%) |
| `squad_vice_captains_circlet` | Squad Vice-Captain's Circlet | Special | HEAD | Black Clover | **0.06 magicules, +1 luck, +1 armour | Command Voice (90 s, 2%) |
| `familia_falna_locket` | Familia Falna Locket | Special | NECK | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +4 max health, +0.05 attack speed, +1 luck | Goddess's Pulse (100 s, 2%) |
| `silver_guild_pendant` | Silver Guild Pendant | Special | NECK | Fairy Tail | **0.06 magicules, 20% less magic damage | Mark of Rank (90 s, 2%) |
| `apprentices_training_sword` | Apprentice's Training Sword | Normal | MAIN_HAND (longsword, 25 dmg, 1.6/s) | Black Clover | +2 max health | Practice Cut (25 s, 1%) (engravings: sturdy) |
| `gate_scavengers_knife` | Gate Scavenger's Knife | Normal | MAIN_HAND (dagger, 20 dmg, 2.2/s) | Solo Leveling | *0.05 sneaking speed | Quick Stab (20 s, 1%) (engravings: swift) |
| `adventurers_handaxe` | Adventurer's Handaxe | Normal | MAIN_HAND (handaxe, 24 dmg, 1.4/s) | Dungeons and Dragons | +0.5 attack damage | Throw Axe (30 s, 1%) (engravings: crushing) |
| `lizardman_scouts_spear` | Lizardman Scout's Spear | Normal | MAIN_HAND (spear, 25.5 dmg, 1.3/s) | That Time I Got Reincarnated as a Slime | +0.5 reach | Marsh Thrust (30 s, 1%) (engravings: swift) |
| `hedge_wizards_oak_staff` | Hedge Wizard's Oak Staff | Normal | MAIN_HAND (staff, 24.5 dmg, 1.4/s) | Magi: The Labyrinth of Magic | **0.04 magicules | Hedge Spark (25 s, 1%) (engravings: magic_weapon) |
| `silvered_shortsword` | Silvered Shortsword | Special | MAIN_HAND (shortsword, 41 dmg, 1.8/s) | Dungeons and Dragons | +1 attack damage, 15% less dark damage | Silver Flash (30 s, 2%); Warding Cut (60 s, 2%, sneak) (engravings: holy_weapon, swift) |
| `hakurou_school_practice_katana` | Hakurou School Practice Katana | Special | MAIN_HAND (katana, 44 dmg, 1.7/s) | That Time I Got Reincarnated as a Slime | +0.1 attack speed, +1 luck | Iaido Cut (35 s, 2%); Calm Mind (70 s, 2%, sneak) (engravings: severance, swift) |
| `familia_smiths_mace` | Familia Smith's Mace | Special | MAIN_HAND (hammer, 50 dmg, 1.1/s) | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +1 attack damage, +0.1 knockback resistance | Forge Bash (35 s, 2%); Anvil Guard (60 s, 2%, sneak) (engravings: crushing, sturdy) |

## Weapon models

* `apprentices_training_sword`: A plain steel longsword with a rounded tip, a leather-wrapped grip, a simple brass crossguard and a nicked pommel.
* `gate_scavengers_knife`: A short dark blade with a faint blue edge, a black rubber grip with finger grooves and a loop pommel.
* `adventurers_handaxe`: A broad steel axe head on a short oak haft, bound with leather straps and with a small beard under the blade.
* `lizardman_scouts_spear`: A long green-stained pole with a leaf-shaped bronze head, a fringe of reed cord below it and a wrapped grip.
* `hedge_wizards_oak_staff`: A gnarled oak staff with a knot at the top cradling a small green crystal and a cord of leaves tied below it.
* `silvered_shortsword`: A broad, short blade of mirror-bright silver with a blue-wrapped grip, a plain oval guard and a pommel with a small star-shaped engraving.
* `hakurou_school_practice_katana`: A slightly curved steel katana with a clean temper line, a round black tsuba, a navy cord grip and a small white pommel cap.
* `familia_smiths_mace`: A flanged iron mace with a dark steel head, copper bands, a leather-bound haft and a small anvil stamped on the pommel.

## Files and hooks

* New: `relics/wave/Wave103.java` (defs, `IDS`, self-test `relic.special_normal_a`), `tools/lang/special_normal_a.json`, sprites and item models (`textures/item`, `models/item`), worn ornaments `relic_worn/uq_<id>.txt` (67), 8 weapon meshes and skins, 8 Better Combat files, `Wave103DefsTest`.
* Appended to shared files: `RelicCatalog.java` (two `guard(...)` lines), `relic_worn/catalog.txt` (67 `relic` lines, from `gen_worn_all.py`), the five Curios tag files, both `weapon_classes.json` (8 rows), `tools/bettercombat/manifest.json`, `tools/relics/display_presets_manifest.json`, two `weapon_meshes_sz` copies (dagger, katana), and one hook in `tools/relics/gen_relic_waves.py` (`all_rows()` adds the 67 rows so the worn library builds their ornaments).
* Not touched: version, CHANGELOG, README, LIBRARIES, `en_us.json`, `.github`, the dungeon road table (`relic_themes.json`, a missing entry falls back to the id hash).
* Loot: reachable at their grade through the existing grade pools (`RelicLoot`); no new loot table.

## Not verified (nothing was run in a game)

* How the 67 worn looks and 8 hand models look in game (only the mesh previews were rendered); the looks of Normal gear are deliberately simple.
* The in-game self-test `relic.special_normal_a`, the act effects and their balance in play, the Better Combat swings of the 8 weapons.
* `RelicHandCoverageTest`, `RelicRenderCoverageTest.everyWornRelicHasAWornModelInTheCatalog` and `WornCoverageTest` need Minecraft client classes the offline shim lacks; their logic was checked through the catalog lines and files instead. `RelicUniqueLooksTest` only reads waves 31-80; the same checks (unique sprite, ornament, palette) were run on this wave by script.
* `gen_worn_all.py` ends with the visual-upgrade step, which fails in this checkout (`metrics` module); its rewritten textures were reverted, the catalog and ornaments were kept.
* Grade pyramid note: no existing relic was reported as mis-graded by this wave.

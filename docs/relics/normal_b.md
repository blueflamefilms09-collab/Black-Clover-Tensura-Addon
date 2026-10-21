# Wave 104 (1.111.0, key normal_b): 120 Normal relics

Owner request: balance the grade pyramid by ADDING relics (Normal about 300, Special about 141); nothing is regraded, moved or removed. This wave adds **120 Normal** and **0 Special** relics (the pyramid is built from the bottom up): everyday adventurer gear and trinkets (charms, rings, caps, coats, cloaks, boots, belts, gloves, pendants) and 6 plain weapons, from Tensura, Black Clover, Magi, DanMachi, Fairy Tail, Shield Hero, Dungeons and Dragons, Solo Leveling, Re:Zero, KonoSuba, Overlord, Sword Art Online, Fate, Naruto, Fullmetal Alchemist, One Piece, Hunter x Hunter, Dragon Ball, Bleach, Frieren, Delicious in Dungeon, Yu-Gi-Oh!, The Seven Deadly Sins, Mushoku Tensei, Mob Psycho 100, My Hero Academia, Demon Slayer, Jujutsu Kaisen, Spice and Wolf, Slayers and original ones.

Status: **NEW** (all 120). **REPLACED**: none. **KEPT**: every existing relic, id, grade and file. Built from `tools/relics/new_normal_b/` (`rows.py` gear, `weapons.py` weapons, `gen.py`, `models.py`, `presets.py`).

Normal grade: one small passive (sometimes two tiny ones) or one simple act (1% magicules); weapons 20-30 damage. There is no RING or OFF_HAND slot in the mod: rings are worn on the HANDS slot with the CHARM look. Every act is a shape of `Wave31Acts`; nothing touches the food bar. Weapons carry Tensura engravings and are real 3D meshes with the old-style sprite as inventory icon, plus Better Combat files.

| id | name | grade | slot | world | passive | acts |
|---|---|---|---|---|---|---|
| `bedroll_and_pack` | Bedroll and Pack | Normal | BACK | Dungeons and Dragons | +2 max health | none |
| `hage_village_patchwork_cloak` | Hage Village Patchwork Cloak | Normal | BACK | Black Clover | 12% less cold damage | none |
| `hero_students_short_cape` | Hero Student's Short Cape | Normal | BACK | My Hero Academia | +2 max health | none |
| `leaf_village_messenger_satchel` | Leaf Village Messenger Satchel | Normal | BACK | Naruto | *0.03 movement speed | none |
| `magus_apprentices_cloak` | Magus Apprentice's Cloak | Normal | BACK | Fate/stay night | **0.03 magicules | none |
| `navigators_waterproof_cloak` | Navigator's Waterproof Cloak | Normal | BACK | One Piece | *0.1 swim speed | none |
| `rain_catchers_reed_cloak` | Rain-Catcher's Reed Cloak | Normal | BACK | Original | 10% less cold damage, *0.05 swim speed | none |
| `sealed_forest_wanderers_cape` | Sealed Forest Wanderer's Cape | Normal | BACK | That Time I Got Reincarnated as a Slime | *0.1 sneaking speed | none |
| `state_alchemists_long_coat` | State Alchemist's Long Coat | Normal | BACK | Fullmetal Alchemist | +1 armour | none |
| `travellers_roadside_cape` | Traveller's Roadside Cape | Normal | BACK | The Seven Deadly Sins | *0.03 movement speed | none |
| `adventurers_money_pouch_belt` | Adventurer's Money Pouch Belt | Normal | BELT | KonoSuba | +1 luck | none |
| `adventurers_potion_holster` | Adventurer's Potion Holster | Normal | BELT | Is It Wrong to Try to Pick Up Girls in a Dungeon? | none | Quick Draught (100 s, 1%) |
| `hunters_utility_belt` | Hunter's Utility Belt | Normal | BELT | Solo Leveling | +1 luck | none |
| `kouhas_silk_sash` | Kouha's Silk Sash | Normal | BELT | Magi: The Labyrinth of Magic | *0.03 movement speed | none |
| `kunai_holster_strap` | Kunai Holster Strap | Normal | BELT | Naruto | +0.03 attack speed | none |
| `lucys_key_ring_belt` | Lucy's Key Ring Belt | Normal | BELT | Fairy Tail | **0.03 magicules | none |
| `ogre_village_rope_belt` | Ogre Village Rope Belt | Normal | BELT | That Time I Got Reincarnated as a Slime | +2 max health | none |
| `pilgrims_knotted_rope_cord` | Pilgrim's Knotted Rope Cord | Normal | BELT | Original | +2 safe fall distance | none |
| `servants_contract_belt` | Servant's Contract Belt | Normal | BELT | Fate/stay night | +1 armour | none |
| `straw_hat_crew_sash` | Straw Hat Crew Sash | Normal | BELT | One Piece | +2 max health | none |
| `witch_cult_prayer_cord` | Witch Cult Prayer Cord | Normal | BELT | Re:Zero | 12% less dark damage | none |
| `zanpakuto_sheath_sash` | Zanpakuto Sheath Sash | Normal | BELT | Bleach | +0.5 attack damage | none |
| `aincrad_teleport_crystal_chip` | Aincrad Teleport Crystal Chip | Normal | CHARM | Sword Art Online | +0.04 jump | none |
| `aladdins_flute_replica` | Aladdin's Flute Replica | Normal | CHARM | Magi: The Labyrinth of Magic | none | Wishful Note (60 s, 1%) |
| `boar_hat_tavern_token` | Boar Hat Tavern Token | Normal | CHARM | The Seven Deadly Sins | none | Tavern Cheer (90 s, 1%) |
| `capsule_corp_capsule_case` | Capsule Corp Capsule Case | Normal | CHARM | Dragon Ball | none | First Aid Capsule (100 s, 1%) |
| `command_spell_ink_vial` | Command Spell Ink Vial | Normal | CHARM | Fate/stay night | none | Crest Drop (75 s, 1%) |
| `cooked_mandrake_sprig` | Cooked Mandrake Sprig | Normal | CHARM | Delicious in Dungeon | none | Soothing Sniff (90 s, 1%) |
| `cursed_talisman_strip` | Cursed Talisman Strip | Normal | CHARM | Jujutsu Kaisen | 12% less curse damage | none |
| `dungeon_key_fragment` | Dungeon Key Fragment | Normal | CHARM | Solo Leveling | +1 luck | none |
| `elder_lich_apprentice_bone_charm` | Elder Lich Apprentice Bone Charm | Normal | CHARM | Overlord | 12% less wither damage | none |
| `explosion_fan_club_pin` | Explosion Fan Club Pin | Normal | CHARM | KonoSuba | 12% less expl damage | none |
| `final_selection_wisteria_charm` | Final Selection Wisteria Charm | Normal | CHARM | Demon Slayer | 10% less dark damage | none |
| `grimoire_page_scrap` | Grimoire Page Scrap | Normal | CHARM | Black Clover | **0.03 magicules | none |
| `hearth_warm_pocket_stone` | Hearth-Warm Pocket Stone | Normal | CHARM | Original | 15% less cold damage | none |
| `hero_course_pass_tag` | Hero Course Pass Tag | Normal | CHARM | My Hero Academia | +2 max health | none |
| `himmels_statue_pebble` | Himmel's Statue Pebble | Normal | CHARM | Frieren: Beyond Journey's End | *0.03 movement speed | none |
| `linas_fireball_tassel` | Lina's Fireball Tassel | Normal | CHARM | Slayers | none | Tassel Flick (60 s, 1%) |
| `log_pose_keychain` | Log Pose Keychain | Normal | CHARM | One Piece | none | Check the Pose (80 s, 1%) |
| `merchants_weighing_coin` | Merchant's Weighing Coin | Normal | CHARM | Spice and Wolf | +1 luck | none |
| `millennium_puzzle_replica` | Millennium Puzzle Replica | Normal | CHARM | Yu-Gi-Oh! | +1 luck | none |
| `nen_water_divination_cup` | Nen Water Divination Cup | Normal | CHARM | Hunter x Hunter | **0.03 magicules | none |
| `ninja_tool_pouch_charm` | Ninja Tool Pouch Charm | Normal | CHARM | Naruto | +0.03 attack speed | none |
| `roxys_water_drop_charm` | Roxy's Water Drop Charm | Normal | CHARM | Mushoku Tensei | 12% less fire damage | none |
| `soul_reaper_academy_badge` | Soul Reaper Academy Badge | Normal | CHARM | Bleach | +1 armour | none |
| `subarus_track_jacket_zipper` | Subaru's Track Jacket Zipper | Normal | CHARM | Re:Zero | none | Zip Up (120 s, 1%) |
| `telekinesis_club_badge` | Telekinesis Club Badge | Normal | CHARM | Mob Psycho 100 | none | Mild Shove (60 s, 1%) |
| `wave_fragment_sliver` | Wave Fragment Sliver | Normal | CHARM | The Rising of the Shield Hero | none | Hum of the Wave (90 s, 1%) |
| `adventurers_cooking_smock` | Adventurer's Cooking Smock | Normal | CHEST | Delicious in Dungeon | +2 max health, 10% less fire damage | none |
| `chunin_flak_vest` | Chunin Flak Vest | Normal | CHEST | Naruto | +2 armour | none |
| `corps_uniform_shirt` | Corps Uniform Shirt | Normal | CHEST | Demon Slayer | +1 armour, +2 max health | none |
| `guild_mages_travelling_vest` | Guild Mage's Travelling Vest | Normal | CHEST | Fairy Tail | +2 max health | none |
| `gym_uniform_of_the_hero_course` | Gym Uniform of the Hero Course | Normal | CHEST | My Hero Academia | *0.03 movement speed | none |
| `hunter_candidates_travel_coat` | Hunter Candidate's Travel Coat | Normal | CHEST | Hunter x Hunter | 12% less cold damage | none |
| `jujutsu_high_uniform_jacket` | Jujutsu High Uniform Jacket | Normal | CHEST | Jujutsu Kaisen | 10% less curse damage, +1 armour | none |
| `lightweight_player_coat` | Lightweight Player Coat | Normal | CHEST | Sword Art Online | *0.03 movement speed, +1 armour | none |
| `magic_knight_training_jacket` | Magic Knight Training Jacket | Normal | CHEST | Black Clover | +1 armour, +2 max health | none |
| `merchants_fur_lined_coat` | Merchant's Fur-Lined Coat | Normal | CHEST | Spice and Wolf | 15% less cold damage | none |
| `padded_gambeson` | Padded Gambeson | Normal | CHEST | Dungeons and Dragons | +1 armour, +0.05 knockback resistance | none |
| `pirates_striped_shirt` | Pirate's Striped Shirt | Normal | CHEST | One Piece | *0.1 swim speed | none |
| `rain_slick_oilskin_jacket` | Rain-Slick Oilskin Jacket | Normal | CHEST | Original | 10% less cold damage, *0.05 swim speed | none |
| `wandering_mages_mantle_cuirass` | Wandering Mage's Mantle Cuirass | Normal | CHEST | Slayers | **0.03 magicules | none |
| `dwarven_road_hobnail_boots` | Dwarven Road Hobnail Boots | Normal | FEET | That Time I Got Reincarnated as a Slime | +0.05 knockback resistance, +1 armour | none |
| `elven_travellers_soft_boots` | Elven Traveller's Soft Boots | Normal | FEET | Frieren: Beyond Journey's End | *0.04 movement speed | none |
| `hero_course_sneakers` | Hero Course Sneakers | Normal | FEET | My Hero Academia | +0.04 jump | none |
| `marsh_walker_waders` | Marsh-Walker Waders | Normal | FEET | Original | *0.1 swim speed | none |
| `martial_artists_training_boots` | Martial Artist's Training Boots | Normal | FEET | Dragon Ball | *0.03 movement speed | none |
| `sailors_deck_shoes` | Sailor's Deck Shoes | Normal | FEET | One Piece | *0.1 swim speed, +2 safe fall distance | none |
| `servants_light_boots` | Servant's Light Boots | Normal | FEET | Fate/stay night | *0.1 sneaking speed | none |
| `shinobi_sandals` | Shinobi Sandals | Normal | FEET | Naruto | *0.1 sneaking speed, +2 safe fall distance | none |
| `squires_running_shoes` | Squire's Running Shoes | Normal | FEET | Black Clover | *0.03 movement speed | none |
| `straw_sandals_of_the_mountain` | Straw Sandals of the Mountain | Normal | FEET | Demon Slayer | *0.03 movement speed, +2 safe fall distance | none |
| `student_dormitory_slippers` | Student Dormitory Slippers | Normal | FEET | Jujutsu Kaisen | +1 luck | none |
| `whale_island_hiking_boots` | Whale Island Hiking Boots | Normal | FEET | Hunter x Hunter | +0.04 jump, +2 safe fall distance | none |
| `breathing_practice_wristlets` | Breathing Practice Wristlets | Normal | HANDS | Demon Slayer | none | Steady Breath (90 s, 1%) |
| `climbers_chalked_gloves` | Climber's Chalked Gloves | Normal | HANDS | Original | +2 safe fall distance, +0.5 reach | none |
| `duel_disk_wrist_strap` | Duel Disk Wrist Strap | Normal | HANDS | Yu-Gi-Oh! | +0.03 attack speed | none |
| `espers_plain_mitts` | Esper's Plain Mitts | Normal | HANDS | Mob Psycho 100 | +0.05 knockback resistance | none |
| `ring_of_feather_falling_common` | Ring of Feather Falling (Common) | Normal | HANDS | Dungeons and Dragons | +3 safe fall distance | none |
| `ring_of_minor_mana_stillness` | Ring of Minor Mana Stillness | Normal | HANDS | Frieren: Beyond Journey's End | *0.1 sneaking speed | none |
| `ring_of_the_third_class_squire` | Ring of the Third-Class Squire | Normal | HANDS | Black Clover | +1 luck | none |
| `ring_of_the_wandering_sorceress` | Ring of the Wandering Sorceress | Normal | HANDS | Slayers | **0.03 magicules | none |
| `rubber_grip_gloves` | Rubber Grip Gloves | Normal | HANDS | One Piece | +0.5 reach | none |
| `shadow_soldier_signet_ring` | Shadow Soldier Signet Ring | Normal | HANDS | Solo Leveling | **0.03 magicules | none |
| `sin_of_patience_ring` | Sin of Patience Ring | Normal | HANDS | The Seven Deadly Sins | none | Patient Moment (100 s, 1%) |
| `slave_crest_hand_wrap` | Slave Crest Hand Wrap | Normal | HANDS | The Rising of the Shield Hero | +0.5 attack damage | none |
| `swordsmans_grip_gloves` | Swordsman's Grip Gloves | Normal | HANDS | Mushoku Tensei | +0.5 attack damage, +0.03 attack speed | none |
| `young_alchemists_chalk_gloves` | Young Alchemist's Chalk Gloves | Normal | HANDS | Fullmetal Alchemist | none | Palm Clap (75 s, 1%) |
| `adventurers_guild_bandana` | Adventurer's Guild Bandana | Normal | HEAD | Mushoku Tensei | +1 luck | none |
| `demon_slayer_corps_hairpin` | Demon Slayer Corps Hairpin | Normal | HEAD | Demon Slayer | +0.03 attack speed | none |
| `hero_costume_visor` | Hero Costume Visor | Normal | HEAD | My Hero Academia | +1 armour, +0.05 knockback resistance | none |
| `hollow_mask_fragment` | Hollow Mask Fragment | Normal | HEAD | Bleach | 12% less dark damage | none |
| `holy_knight_recruits_helm` | Holy Knight Recruit's Helm | Normal | HEAD | The Seven Deadly Sins | +1 armour, +0.05 knockback resistance | none |
| `hunter_exam_goggles` | Hunter Exam Goggles | Normal | HEAD | Hunter x Hunter | standing night vision | none |
| `lantern_keepers_cap` | Lantern-Keeper's Cap | Normal | HEAD | Original | none | Cap Lamp (90 s, 1%) |
| `marine_recruit_cap` | Marine Recruit Cap | Normal | HEAD | One Piece | +1 armour | none |
| `rogues_black_cowl` | Rogue's Black Cowl | Normal | HEAD | Dungeons and Dragons | *0.1 sneaking speed | none |
| `sand_village_travellers_hat` | Sand Village Traveller's Hat | Normal | HEAD | Naruto | 12% less fire damage | none |
| `sorceress_silver_tiara` | Sorceress' Silver Tiara | Normal | HEAD | Slayers | **0.03 magicules | none |
| `travellers_mage_cap` | Traveller's Mage Cap | Normal | HEAD | Frieren: Beyond Journey's End | **0.03 magicules | none |
| `village_militia_kettle_helm` | Village Militia Kettle Helm | Normal | HEAD | The Rising of the Shield Hero | +1 armour | none |
| `wizards_apprentice_pointed_hat` | Wizard's Apprentice Pointed Hat | Normal | HEAD | Fairy Tail | **0.03 magicules | none |
| `celestial_spirit_key_pendant` | Celestial Spirit Key Pendant | Normal | NECK | Fairy Tail | **0.03 magicules | none |
| `demon_blood_talisman_chip` | Demon Blood Talisman Chip | Normal | NECK | Slayers | 12% less fire damage | none |
| `djinn_vessel_replica_amulet` | Djinn Vessel Replica Amulet | Normal | NECK | Magi: The Labyrinth of Magic | **0.03 magicules | none |
| `four_leaf_clover_locket` | Four-Leaf Clover Locket | Normal | NECK | Black Clover | +1 luck | none |
| `four_star_ball_pendant` | Four-Star Ball Pendant | Normal | NECK | Dragon Ball | +1 luck | none |
| `goddess_blessing_medallion` | Goddess' Blessing Medallion | Normal | NECK | Is It Wrong to Try to Pick Up Girls in a Dungeon? | +2 max health | none |
| `guardians_gold_coin_necklace` | Guardian's Gold Coin Necklace | Normal | NECK | Overlord | +1 armour | none |
| `prayer_bead_necklace` | Prayer Bead Necklace | Normal | NECK | Jujutsu Kaisen | 10% less curse damage | none |
| `rimurus_slime_pendant` | Rimuru's Slime Pendant | Normal | NECK | That Time I Got Reincarnated as a Slime | +2 max health | none |
| `spirit_mediums_salt_pouch` | Spirit Medium's Salt Pouch | Normal | NECK | Mob Psycho 100 | 12% less dark damage | none |
| `spirit_tortoise_scale_charm` | Spirit Tortoise Scale Charm | Normal | NECK | The Rising of the Shield Hero | +0.05 knockback resistance, +1 armour | none |
| `wolf_tooth_pendant` | Wolf-Tooth Pendant | Normal | NECK | Spice and Wolf | +1 luck | none |
| `selection_hopefuls_bokken` | Selection Hopeful's Bokken | Normal | MAIN_HAND (katana, 24.5 dmg, 1.7/s) | Demon Slayer | +0.03 attack speed | Oak Cut (25 s, 1%) (engravings: swift) |
| `genin_academy_kunai` | Genin Academy Kunai | Normal | MAIN_HAND (dagger, 20 dmg, 2.2/s) | Naruto | *0.05 sneaking speed | Kunai Throw (20 s, 1%) (engravings: swift) |
| `east_blue_cutlass` | East Blue Cutlass | Normal | MAIN_HAND (sabre, 23.5 dmg, 1.7/s) | One Piece | *0.05 swim speed | Deck Slash (25 s, 1%) (engravings: sturdy) |
| `axel_frontier_club` | Axel Frontier Club | Normal | MAIN_HAND (club, 25.5 dmg, 1.3/s) | KonoSuba | +0.05 knockback resistance | Frontier Bash (30 s, 1%) (engravings: crushing) |
| `apprentice_birch_wand` | Apprentice Birch Wand | Normal | MAIN_HAND (wand, 20.5 dmg, 1.5/s) | Frieren: Beyond Journey's End | **0.03 magicules | Petal Spark (20 s, 1%) (engravings: magic_weapon) |
| `hunter_exam_machete` | Hunter Exam Machete | Normal | MAIN_HAND (cleaver, 24 dmg, 1.6/s) | Hunter x Hunter | +0.5 attack damage | Brush Cut (25 s, 1%) (engravings: sturdy) |

## Weapon models

* `selection_hopefuls_bokken`: A slightly curved oak blade with a darker grain, a round black tsuba, a rope-wrapped grip and a worn pommel cap.
* `genin_academy_kunai`: A leaf-shaped dark steel blade, a short wrapped grip and a large ring pommel with a loop of cloth cord.
* `east_blue_cutlass`: A broad curved single-edged blade, a sweeping brass basket guard, a dark wood grip and a flat brass pommel.
* `axel_frontier_club`: A thick tapering oak club with iron nails driven in around the head, leather straps on the grip and an iron butt cap.
* `apprentice_birch_wand`: A pale slim birch rod with a small green bud-shaped crystal at the tip, a wrapped grip and a ring of leaves carved near the base.
* `hunter_exam_machete`: A wide single-edged blade with a clipped tip, a green cord-wrapped handle with a lanyard hole and a short black guard.

## Files and hooks

* New: `relics/wave/Wave104.java` (defs, `IDS`, self-test `relic.normal_b`), `tools/lang/normal_b.json`, sprites and item models (`textures/item`, `models/item`), worn ornaments `relic_worn/uq_<id>.txt` (114), 6 weapon meshes and skins, 6 Better Combat files, `Wave104DefsTest`.
* Appended to shared files: `RelicCatalog.java` (two `guard(...)` lines), `relic_worn/catalog.txt` (114 `relic` lines, from `gen_worn_all.py`), the five Curios tag files, both `weapon_classes.json` (6 rows), `tools/bettercombat/manifest.json`, `tools/relics/display_presets_manifest.json`, two `weapon_meshes_sz` copies (the dagger and the katana), and one hook in `tools/relics/gen_relic_waves.py` (`all_rows()` adds the 114 rows so the worn library builds their ornaments).
* Not touched: version, CHANGELOG, README, LIBRARIES, `en_us.json`, `.github`, the dungeon road table (`relic_themes.json`, a missing entry falls back to the id hash).
* Loot: reachable at their grade through the existing grade pools (`RelicLoot`); no new loot table.

## Not verified (nothing was run in a game)

* How the 114 worn looks and 6 hand models look in game (only the mesh previews were rendered); the looks of Normal gear are deliberately simple.
* The in-game self-test `relic.normal_b`, the act effects and their balance in play, the Better Combat swings of the 6 weapons.
* `RelicHandCoverageTest`, `RelicRenderCoverageTest.everyWornRelicHasAWornModelInTheCatalog` and `WornCoverageTest` need Minecraft client classes the offline shim lacks; their logic was checked through the catalog lines and files instead. `RelicUniqueLooksTest` only reads waves 31-80; the same checks (unique sprite, ornament, palette) were run on this wave by script.
* `gen_worn_all.py` ends with the visual-upgrade step, which fails in this checkout (`metrics` module); its rewritten textures were reverted, the catalog and ornaments were kept.
* Grade pyramid note: no existing relic was reported as mis-graded by this wave.

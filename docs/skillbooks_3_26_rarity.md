# Skill books 3.26.0: rarity tiers and the franchise blacklist (builder RARITY)

This is what sections 1, 2 and 5 and the RARITY line of section 6 of `docs/skillbooks_3_26.md` became in code. Nothing here is verified in a running game (see "What was not seen in a game").

## What was built, in plain words

- Every Skill Book line has one of five rarities: common, uncommon, rare, epic, mythic. It is data in `skill_books.json`, generated from `tools/lines/rarity_table.py`.
- The book NAME is coloured by rarity (white, green, blue, purple, orange). Epic and mythic books glint. The tooltip has a "Rarity: ..." row, and mythic books a red "Mythic" tag. Reading a book plays a rarity coloured burst and the challenge-complete toast sound.
- Three doors hand out books, each with its own rule (below). Luck is the player's `Attributes.LUCK`, Looting is the level on the weapon of the killing blow; each is read ONCE per drop.
- The franchises in `blockedFranchises` (default `tensura,black_clover,magi`) are never DROPPED, never PICKED for a swap and never STOCKED by the Black Market. Books already held still read; commands and the creative tab are unchanged.
- `rarityTiers = false` gives back the 3.25 drops exactly (proven by a replay test).

Not included: the ten Evolution skills (the lead fills `NEW_SKILLS` in `tools/lines/rarity_table.py` and regenerates), the 3D book look and the `nusmp:rarity` model files (the LOOK builder; the property value `BookLook.rarityValue(bookId)` = `(rank+1)/6` is ready for it), the lang merge (the fragment is `tools/lang/skillbooks_rarity.json`), version bump, changelog.

## NEW / REPLACED / KEPT

| Part | Status | Detail |
| --- | --- | --- |
| `skilltree/rarity/BookRarity`, `RarityRules`, `RarityConfig`, `BlockedFranchises`, `BossThemes`, `BookFacts` | NEW | pure rules, no Minecraft type (BookFacts only reads the shipped table) |
| `tools/lines/rarity_table.py`, `rarity` and `theme` per line in `skill_books.json` | NEW | `gen_book_index.py` appends the two fields; a table without them reads as common, no theme |
| Component `nusmp:skill_book_rarity`, `SkillItems.rarityOf` | NEW | appended after the existing components; a stack without it reads its line's table rarity |
| Name colour of a Skill Book | REPLACED | was the line's colour; now the rarity colour. The "Teaches" row keeps the line colour |
| Glint of a Skill Book | REPLACED | was "every book"; now epic and mythic only. `LegacyLook.enabled()` (the LOOK builder's client setting) brings the old rule back |
| Refusal text "You have already comprehended this path." | REPLACED | key `message.nusmp.skillbook.already_comprehended`. The old key `message.nusmp.skillbook.already_owned` stays in the lang file, unused. The book is still NOT consumed |
| Unlock burst of a reading | EXTENDED | the same effect count; the colour is the rarity's and the power grows with the rank. The old call `UnlockFx.play(player, routeId, rgb, announce)` is kept |
| Monster drop of a book | REPLACED when `rarityTiers` is on | chance `rarityBaseChance` (0.1%) times a Luck and Looting factor, then a rarity is drawn. Off: the 3.25 rule (0.4%, any line) |
| Guardian drop | REPLACED when `rarityTiers` is on | ONE themed book, guaranteed (`guardianBookChance` 1.0), rarity by depth. Off: the 3.25 rule (30%, any line, 1 to 3 SP Books) |
| Rift chest drop | REPLACED when `rarityTiers` is on | mythic only (epic when none is left), chance times a small Luck factor. Off: the 3.25 rule |
| SP Books from monsters and Guardians | KEPT (3.25 rule, tiers off) | with the tiers on none drop (the SP Books are gone with the Skill Points; `LootDrops` already made nothing of them) |
| `LootRules.decide`, `LootDrops.materialize(Plan..)`, `routeBook`, `RiftBookLootModifier.add(..)` 5-argument form, `BossBookDrops.process(..)` 4-argument form, `SkillBookLootModifier.add(..)` 4-argument form | KEPT | the old entry points delegate with the config read once |
| `RouteTable.Snap` 2-argument constructor, `LineIndex.Line` 11- and 5-argument constructors, `BookLook.Entry` 6-argument constructor, `LootVictims.Site` 2-argument constructor, `UnlockFx.play` 4-argument | KEPT | for old callers and tests |
| Black Market stock and restock | CHANGED | `ShopGoods.routeIds()` leaves out blocked lines. Held books, commands, the creative tab: untouched |
| Swap of an old removed-magic book (`BookUse.replace`) | CHANGED | never hands out a blocked line, and (tiers on) never a mythic one |

## The numbers

Over the 254 lines of 3.25 (`python3 tools/lines/rarity_table.py --report`):

| Rarity | Lines | Share | Target |
| --- | --- | --- | --- |
| common | 76 | 29.9% | 30% |
| uncommon | 71 | 28.0% | 28% |
| rare | 61 | 24.0% | 24% |
| epic | 30 | 11.8% | 12% |
| mythic | 16 | 6.3% | 6% (10 to 22 lines) |

With the default blacklist applied (Tensura, Black Clover, Magi = 75 lines hidden) the open pool is common 69, uncommon 46, rare 36, epic 17, mythic 11. Five mythic lines are in blocked franchises: `storm_dragon_veldora`, `dragon_nova_milim`, `wizard_king_sovereign`, `devil_union_vessel`, `solomon_wisdom_sage`. A server owner who empties the list gets all 16 in the rifts.

How a line gets its rarity (in this order): `NEW_SKILLS` (the lead's fixed Evolution skills; ids that do not exist yet are ignored), `EXPLICIT` (hand-written: 4 hand-built classics, 15 mythic monarch/god/sovereign lines plus the Black Market flagship, 16 epic story lines), then a COMPUTED score from the strongest finisher of the route file, cut at the fixed thresholds 1.61 / 1.79 / 2.023 (uncommon / rare / epic). The computed path never gives mythic. The thresholds are constants, not quantiles, so adding a line later never moves an existing one.

## How each door decides

All three doors need a player kill in the Mega-Dungeon (the same gates as 3.25). Luck = `Attributes.LUCK` of the killer (the opener for a chest), Looting = the level of the Looting enchantment on the weapon of the killing blow (the damage source's weapon, else the main hand). NaN, infinity and negatives read as 0.

1. **Tier 1, ordinary Abyssal monsters** (`SkillBookLootModifier`, a global loot modifier). Chance of a book = `rarityBaseChance` (0.001) times `min(10, 1 + 0.25 * looting + 0.05 * luck)`. Then the rarity is drawn from the weights common 600, uncommon 300, rare 80, epic 10, mythic 0, each times `min(20, 1 + 0.5 * luck * rank)` (common never changes with Luck). At Luck 0 an epic is 1% of the books, about 0.001% of the kills. A line of that rarity is then picked by `loot_weight` through the per-rarity table, with the pity rule (`BookPity`) of the killer. A rarity with no line that can drop steps down one rarity at a time.
2. **Tier 2, the decennial Guardians** (`BossBookDrops`, the only door that serves a Guardian; the loot modifier refuses it, so one death never pays twice). ONE themed book at `guardianBookChance` (1.0 = guaranteed, no dice value spent). Rarity by depth (the sign of the floor does not matter): floors 10 and 20 rare; 30 and 40 rare with a 20% chance of epic; 50 and deeper epic. The book is a DETERMINISTIC pick from (floor, the sector's Region Theme, rarity): the candidate list is sorted by id, the start is a SplitMix64 / FNV-1a seed of those three values (no world seed, so every server agrees), themed candidates (the theme's backgrounds, see `BossThemes`) are preferred and the whole rarity is the fall-back. A killer who owns the chosen book gets the next unowned one (the pity walk), widening to the whole rarity when every themed book is owned. Never blocked, never mythic.
3. **Tier 3, rift chests** (`RiftBookLootModifier`, the relic vault tables inside a pocket cell). The listed chance (table times `riftBookScale`) is multiplied by `min(3, 1 + 0.1 * luck)` of the opener. The book is a MYTHIC line the opener does not own (weighted by `loot_weight`, up to `pityRerolls` tries); when none is left, an epic one. Mythic comes ONLY from here (plus the Black Market flagship, the command and the creative tab).

The 27 Region Themes of the game (the contract says 28; the enum `RegionTheme` has 27) all have an entry in `BossThemes`, each mapped to one to three of the 15 backgrounds of the book table. A unit test iterates `RegionTheme.values()`, so a new theme without an entry fails the build.

## Settings (`nusmp-skilltree-loot-server.toml`, section `[rarity]`, all SERVER, plain `define`, no list values)

| Key | Default | Meaning |
| --- | --- | --- |
| `rarityTiers` | `true` | `false` restores the 3.25 drops exactly |
| `guardianBookChance` | `1.0` (0 to 1) | chance that a Guardian drops its themed book |
| `rarityBaseChance` | `0.001` (0 to 1) | chance per kill of a book from an ordinary Abyssal monster, before Luck and Looting |
| `hideBlockedFranchises` | `true` | the blacklist is on |
| `blockedFranchises` | `tensura,black_clover,magi` | comma, semicolon or space separated franchise keys; bad entries are ignored, at most 64, lower-cased |

The blacklist reads the setting as a STRING on purpose (a list value in a NeoForge config throws while the file is loading). The weight tables are cached per registry version AND per setting text, so editing the setting is seen on the next drop, with one rebuild.

## Worst-case cost

- Server: no per-tick work anywhere. Everything runs per kill, per chest or per reading.
  - Tier 1 drop: a config snapshot (one small record), two attribute/enchantment reads (only while the tiers are on), one chance roll, and only on a success one rarity draw and one binary search in a cached table. A miss costs one dice value.
  - Guardian death (about one per boss floor): one sort of at most about 254 small records and a few list filters. Bounded by the table size.
  - Rift chest: nothing for a table that is not listed, outside a pocket or with chance 0 (the Luck is read only after those checks).
  - `RouteTable` builds six weight tables of 254 entries once per registry version or per setting change (never per drop). `BlockedFranchises.current()` keeps its last parse, so a drop never parses a string.
  - At most ONE book per kill or chest (plus none from the SP Books). Entities: at most one item drop per death.
- Network: no new packet. The reading burst is the same as before: at most 5 VFX sends per reading to nearby players, the read cooldown is 20 ticks, and the pending-effect queue is capped at 512.
- Client draw: nothing new per frame; the glint and name colour are read when the tooltip or the item is drawn.
- Per-player state: the pity memory is the existing per-UUID `BookPity`, dropped on logout and on server stop.

## Tests

Pure JUnit (all pass here): `BookRarityTest`, `RarityRulesTest` (200,000 even draws and a seeded run, Luck factor, Looting, clamps, NaN and negatives), `BossThemesTest` (every `RegionTheme` covered, determinism over 1000 seeds and every theme, never blocked or mythic, step down, pity walk), `BlockedFranchisesTest` (parser and facade, junk, cap), `RarityTableTest` (reads `skill_books.json`: valid rarity and theme per line, spread bands, 10 to 22 mythic, every hand-written id exists and matches, route files and lines agree, every door keeps a pool under the default blacklist), `LootRarityRulesTest` (tiers off replay of 3.25 with the same dice count over thousands of cases, tiers on gates, chance edges, Guardian guarantee), `RouteTableRarityTest` (per-rarity partition, blacklist zeroing, one rebuild per setting change), `LineIndexRarityTest` (old table, damaged values, old constructors), `RarityLangTest`, and `SkillBookLangTest` now also reads every `tools/lang/skillbooks_*.json` fragment.

In-game self-tests (registered by `LinesLoot.init`, for the CI server smoke test): see the next section.

### Self-test prefixes for the smoke list

- `skillbook.rarity.drops` (tables partition the weights, monster picks never mythic or blocked, Guardian picks deterministic, tiers-off replay, rift modifier gives mythic or the epic fallback)
- `skillbook.rarity.read` (a stack wears its line's rarity, the component wins, the glint, read / refuse / keep, swaps never mythic or blocked)
- `skillbook.blocked` (with the default blacklist forced for the test and restored afterwards: no pick path names a blocked line, the shop stocks none, a held blocked book still reads, one rebuild per setting change)

## What was not seen in a game

- None of the three self-tests has run in a game or on a server; they compile against the real classes (type-checked) and the pure parts are covered by JUnit.
- The name colours, the glint, the tooltip, the burst colour and the toast sound were not looked at or heard.
- The Looting read (`DamageSource.getWeaponItem` and the enchantment registry lookup) and `Attributes.LUCK` were not exercised against a real Luck potion or a Looting sword; a failed read counts as "no bonus", never as an error.
- The Guardian's `Site.themeId` comes from `sector.theme.id()`; a sector with no theme falls back to the whole rarity.
- Tier 1 at the default 0.1% is rare by design; the first real feel of it needs a play session.
- `BlockedFranchises.setSource` is installed in `LinesLoot.init`; before that (and in every unit test) nothing is blocked.

## Notes for the lead

- `NEW_SKILLS` in `tools/lines/rarity_table.py` is empty. Fill it with the ten Evolution skills' rarities from the design doc, then run `python3 tools/lines/gen_book_index.py`. The spread test has bands (common 26 to 34, uncommon 24 to 32, rare 20 to 28, epic 8 to 16, mythic 3.5 to 9 percent, and 10 to 22 mythic lines) that the ten new lines will not break.
- New lines also need a `theme`: `gen_book_index.py` takes it from the rows' `L.background` or `HAND_THEMES`; `RarityTableTest` fails on a line without one.
- Lang: merge `tools/lang/skillbooks_rarity.json` into `en_us.json` (8 keys). Until then the in-game fallbacks show the same English text.
- `BookLook.rarityValue(bookId)` is the value for the item model property `nusmp:rarity` (`(rank+1)/6`, 0 for unknown); the LOOK builder registers the property and writes the overrides.
- `LegacyLook.set(boolean)` is for the LOOK builder's client config; common code only reads it.
- Smoke list: add the three prefixes above.

## The rarity table

### Mythic (16 lines)

- **black_clover** (blocked by default): `devil_union_vessel`, `wizard_king_sovereign`
- **danmachi**: `freya_charm`
- **exiled_heavy_knight**: `smoldering_fang_of_insanity`
- **god_of_high_school**: `goh_monkey_king`, `goh_zeus_charyeok`
- **magi** (blocked by default): `solomon_wisdom_sage`
- **omniscient_readers_viewpoint**: `constellation_patronage`, `omniscient_viewpoint`
- **solo_leveling**: `monarchs_domain`, `shadow_extraction`, `white_flame_sovereign`
- **tbate**: `asura_ascension`
- **tensura** (blocked by default): `dragon_nova_milim`, `storm_dragon_veldora`
- **tower_of_god**: `tog_zahard_decree`

### Epic (30 lines)

- **black_clover** (blocked by default): `elven_rite_bearer`, `golden_dawn_prodigy`, `grimoire_page_scholar`
- **god_of_high_school**: `goh_charyeok_vessel`, `goh_hephaestus_charyeok`, `goh_nyuh_weapon_forms`
- **magi** (blocked by default): `dark_rukh_fallen_magi`, `extreme_magic_adept`, `flame_djinn_swordsman`, `frost_djinn_blade`, `multi_vessel_warlord`, `seven_seas_swordmaster`
- **omniscient_readers_viewpoint**: `breaking_the_sky_sword`, `demon_king_of_salvation`
- **solo_leveling**: `beast_monarch`, `frost_monarch`, `iron_body_monarch`, `kamish_wrath`, `plague_monarch`
- **tbate**: `aether_bow`, `aether_destruction`, `aether_step`, `dragon_will`, `hoarfrost_art`, `thunder_art`
- **tensura** (blocked by default): `crimson_sovereign`, `primordial_black_diablo`, `primordial_red_destruction`, `velzard_frost_dragon`
- **tower_of_god**: `tog_jahad_royal`

### Rare (61 lines)

- **black_clover** (blocked by default): `cauldron_witch_alchemist`, `crimson_lion_blazeguard`, `green_mantis_ambusher`, `heart_kingdom_lifebinder`, `magic_tool_artificer`, `silver_eagle_marksman`, `spirit_dive_rider`, `temple_delver`
- **danmachi**: `hephaistos_forge`, `hestia_knife`, `rea_laevateinn`
- **exiled_heavy_knight**: `boss_pattern_reader`, `cheese_strategy`, `greatsword_doctrine`, `hidden_class_berserker`, `min_max_build`, `oath_of_the_heavy_armor`, `siege_calculation`
- **god_of_high_school**: `goh_jumeok_fist`, `goh_ki_cultivation`
- **magi** (blocked by default): `borg_barrier_adept`, `flute_magi_prodigy`, `magnostadt_staff_wizard`, `metal_vessel_conqueror`, `rukh_caller_priest`, `shadow_djinn_assassin`, `stone_djinn_bulwark`, `thunder_djinn_duelist`, `tundra_harpoon_hunter`
- **omniscient_readers_viewpoint**: `abyssal_black_flame`, `jihye_twin_blades`, `sangah_war_room`, `scenario_clearer`
- **solo_leveling**: `beru_ant_king`, `bloodlust_pressure`, `hunter_mage`, `kasaka_venom_fang`
- **tbate**: `aevum_hourglass`, `beast_will`, `bonded_beast`, `core_refinement`, `dwarven_forge`, `ember_flame_art`, `goldsmith_radiance`, `mana_beast_hunter`, `scythe_reaper`, `sword_saint_style`, `vritra_blood_rite`
- **tensura** (blocked by default): `beastkin_lord_carrion`, `flame_spirit_ifrit`, `hellflare_flame_lord`, `hinata_moonlit_blade`, `labyrinth_maker_ramiris`, `ogre_lord_might`, `slime_body_shapeshift`, `vampire_queen_night`
- **tower_of_god**: `tog_agnis_ice`, `tog_headon_channel`, `tog_irregular`, `tog_spear_bearer`, `tog_wolhaiguk_moon`

### Uncommon (71 lines)

- **black_clover** (blocked by default): `anti_magic_blade_dancer`, `black_bulls_vanguard`, `blue_rose_cavalier`, `coral_peacock_trickster`, `dark_triad_hexer`, `magic_broom_aerialist`, `magic_knight_duelist`, `mana_skin_brawler`, `purple_orca_tidecaller`, `spade_beastblood_hunter`
- **danmachi**: `braver_command`, `crozzo_magic_sword`, `little_rookie`, `takemikazuchi_bushi`
- **exiled_heavy_knight**: `animation_cancel`, `beta_tester_insight`, `cursed_equipment_mastery`, `grinding_protocol`, `lady_luck`, `new_game_plus_memory`, `sequence_break_route`, `shield_throw_art`, `wall_clip_glitch`
- **god_of_high_school**: `goh_apollo_charyeok`, `goh_ares_charyeok`, `goh_artemis_charyeok`, `goh_athena_charyeok`, `goh_hades_charyeok`, `goh_heracles_labors`, `goh_kendo`, `goh_mighty_style`, `goh_poseidon`, `goh_tournament_contender`
- **magi** (blocked by default): `fanalis_brawler`, `fog_troupe_bandit`, `household_vessel_wielder`, `imperial_fan_dancer`, `legionary_phalanx_shield`, `magic_carpet_rider`, `magoi_siphoner`
- **omniscient_readers_viewpoint**: `black_heavenly_demon_sword`, `fourth_wall`, `heewon_righteous_knight`, `sea_of_stories`, `storyteller`, `way_of_the_wind`
- **solo_leveling**: `goliath_colossal_fist`, `hunter_ranger`, `igris_blood_red`, `raid_captain_command`, `shadow_exchange`
- **tbate**: `djinn_mirage`, `elenoir_wind_weaver`, `lance_shadowblade`, `lance_vanguard`, `mana_rotation`, `realmheart_physique`
- **tensura** (blocked by default): `dryad_world_tree`, `great_sage_analysis`, `guardian_absolute_defense`, `hakurou_blade_way`, `puppet_master_clayman`, `shadow_ninja_souei`, `shuna_holy_weaving`, `spatial_manipulation`
- **tower_of_god**: `tog_black_march`, `tog_faerie`, `tog_fisherman`, `tog_floor_guard`, `tog_ranker_authority`, `tog_suspendium_arms`

### Common (76 lines)

- **black_clover** (blocked by default): `diamond_crystal_bulwark`, `mana_zone_sentinel`
- **danmachi**: `airiel_wind_maiden`, `amazon_berserker`, `argonaut_charge`, `bell_firebolt`, `boar_warrior_might`, `cinder_ella`, `dian_cecht_healer`, `dungeon_supporter`, `dwarf_guardian`, `endure_ability`, `hestia_falna`, `hunter_ability`, `liaris_freese`, `luminous_wind`, `monster_tamer`, `werewolf_gale_kick`, `wynn_fimbulvetr`
- **exiled_heavy_knight**: `aggro_manipulation`, `basic_swordsmanship`, `consumable_optimization`, `damage_rotation`, `debuff_stack_doctrine`, `elemental_resist_meta`, `invincibility_frames`, `loot_duplication_glitch`, `overweight_build`, `party_support_rotation`, `perfect_parry_timing`, `save_point_anchor`
- **god_of_high_school**: `goh_capoeira`, `goh_hapkido`, `goh_hermes_charyeok`, `goh_judo`, `goh_muay_thai`, `goh_pankration_grappler`, `goh_taekwondo`, `goh_tiger_crane_kungfu`
- **magi** (blocked by default): `dungeon_conqueror_captain`, `tide_djinn_dancer`
- **omniscient_readers_viewpoint**: `beast_communion`, `bookmark`, `character_list`, `dokkaebi_bundle`, `prisoner_golden_headband`, `probability_stories`, `regressor_hundred_lives`, `secretive_plotter`, `steel_bulwark`, `stigma_bearer`, `tyrant_dark_mirror`
- **solo_leveling**: `double_dungeon_survivor`, `hunter_healer`, `hunter_sword_dance`, `iron_shadow_guard`, `quicksilver_rush`, `rulers_authority`, `stealth_skill`, `system_daily_quest`
- **tbate**: `lance_bulwark`
- **tensura** (blocked by default): `orc_lord_famine`, `predator_gluttony`, `storm_wolf_ranga`
- **tower_of_god**: `tog_beast_tamer`, `tog_fug_assassin`, `tog_guide`, `tog_inventory`, `tog_light_bearer`, `tog_lighthouse_keeper`, `tog_regular_climber`, `tog_reverse_flow_control`, `tog_scout`, `tog_shinsu_reinforcement`, `tog_shinsu_shooting`, `tog_wave_controller`


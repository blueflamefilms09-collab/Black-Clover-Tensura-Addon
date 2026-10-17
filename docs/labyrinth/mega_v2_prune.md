# Removing the old dungeon (1.58.0, builder PRUNE)

The owner's words: "remove the old dungeon completely but keep the blocks if the new one uses them." This file is the map that was made before
anything was cut (section 2), the result (sections 3 to 5), what the cut changed in the code that stayed (section 6), the config, commands, quests
and old-world behaviour (sections 7 to 10), and the honest list of what could not be verified (section 12). It follows the contract in
`docs/labyrinth/mega_v2.md` section 4.

History is untouched. `docs/labyrinth.md` and `docs/living_dungeon.md` only got a note at the top; the 1.35.0 design docs
(`docs/labyrinth/a1_foundation.md` to `f3_vital.md`), every older `mega_*.md` and `CHANGELOG.md` say what they said. Nothing in `labyrinth/mega/**`
was redesigned: five files had a reference to a removed thing fixed (section 6.4, exact). The Time Magic VFX, `arc/**` and the lang file were not touched.

## 1. What "the old dungeon" was

1. **The Spire layout of 1.35.0.** The floating Spire in the dimension `nusmp:labyrinth`: structure set `labyrinth_spires`, structure
   `labyrinth_spire`, thirteen pools `nusmp:labyrinth/*`, thirty templates, the biome-palette and vein processors, the generator code
   `labyrinth/gen`, the routing `mega.layout = "spire"` and the switch `MegaConfig.LAYOUT`.
2. **The Living Dungeon (1.17.0 to 1.23.0).** The cave dimension `nusmp:living_dungeon` and everything that fed it: tiers, atmosphere, the
   ecology and its saves, the Golden City, the Variable Core (zones, vaults, collapse, navigation), the sentient rooms and the immune system, the
   Maw, the wild food web, the two floor arenas (Goliath's pit, the Charybdis basin), `DungeonConfig`, `/multiverse dungeon ...` and
   `labyrinth.engine = "legacy"`.
3. **The legacy gates and maws.** The Dungeon Gate arches (structure `nusmp:dungeon_gate`), the Dungeon Maw, `MegaConfig.LEGACY_GATES`, and the
   parts of `labyrinth/gate` that only served them.

What stays is what the Mega-Dungeon, the Hub, the Gateway, the Guard, the D&D layer, the economy, quests, relics, charter grimoires, bosses, the
encounter tables, the templates, the arcs or a test still read. Where there was doubt the object stayed registered and is listed as a **kept
orphan** (section 5.2).

## 2. The map (made before the cut)

**How it was made.** A static reference graph of every Java file of `src/main` and `src/test` (class to class through imports, the same package,
wildcard imports and fully qualified names; comments and string literals stripped), plus greps over `src/main/resources`, `tools`, `.github`,
`docs/quests.md` and the quest specs, plus a scan of the block palette of every `.nbt` template. A class of the old dungeon was **kept** when live
code outside it reaches it (the keep-closure); the three hubs that tie everything together (`NUSMP`, `NUCreativeTab`, `MultiverseCommands`) were
not counted as a reason to keep, they were edited. The graph was re-run after the cut: every one of the 77 classes left in `dungeon/**` is reached
from live code or from a registration (section 5.1).

| Object | Decision | Who uses it / why |
| --- | --- | --- |
| Spire: structure set `labyrinth_spires`, structure `labyrinth_spire`, 13 pools `labyrinth/*`, 30 templates `structure/labyrinth/**`, biome tag `has_structure/labyrinth_spire`, structure tag `labyrinth_spire` | REMOVE | Only the Spire's dimension generated them. No kept pool, template or processor names any of them (`LabyrinthDataTest`, and the template scan below). |
| Spire: processors `labyrinth_palette`, `labyrinth_interior`, `magicule_veins`, block tags `labyrinth/swap_floor/pillar/wood/accent` and `labyrinth/veinable`, data map `labyrinth_palette` | REMOVE | Read only by `BiomePaletteProcessor` / `MagiculeVeinProcessor`, which are removed. The processors the Mega-Dungeon uses (`nusmp:mod_compat`, `nusmp:region_theme`) are untouched. |
| Spire: `labyrinth/gen` (`BiomeMirror`, `BiomePaletteProcessor`, `GenRegistry`, `LabyrinthPalette`, `MagiculeVeinProcessor`, `PaletteSwap`, `VeinMath`) and their 2 tests | REMOVE | The Spire's generator. `GenRegistry` registered only those processor types. |
| `labyrinth/gen/StateCarry` | NEW | The one method of `BiomePaletteProcessor` that live code needs (`ModCompatProcessor` copies block-state properties by name). Extracted unchanged in behaviour so the processor could go. |
| `labyrinth/gen/LabyrinthLocate`, `LabyrinthCommand`, `LabyrinthSystem` | KEEP, slimmed | `/multiverse labyrinth locate / enter / leave / here` still work for the Gateway; `LabyrinthSystem` lost only `GenRegistry.init`. |
| Dimension `nusmp:labyrinth`: `dimension/labyrinth.json`, `dimension_type/labyrinth.json`, biome `labyrinth_sky`, the client effects `nusmp:labyrinth` | KEEP-ORPHAN (inert) | A world that ever loaded the dimension names it in `level.dat`; a missing dimension type there could make the whole save fail to decode. Its generator has `structure_overrides: []` now, so nothing generates. `Labyrinth.DIMENSION` is still compared by `Labyrinth.in`, so the Guard, the economy, the gloom and the void rescue still hold an old Spire sky. |
| `MegaConfig.LAYOUT` (+ `LAYOUT_MEGA`, `LAYOUT_SPIRE`, `DEFAULT_LAYOUT`), `Labyrinth.layoutIsMega()`, `Labyrinth.engineIsLabyrinth()`, `Labyrinth.ENGINE_*`, `Labyrinth.BIOME`, `Labyrinth.SPIRE_TAG` | REMOVE | The Mega-Dungeon is the only layout; every branch is now the Mega-Dungeon branch. |
| Living Dungeon core: `LivingDungeon`, `DungeonTier`, `DungeonAtmosphere`, `DungeonAmbience`, `DungeonSave`, `EcosystemData`, `DungeonEcology`, `DungeonConfig`, `DungeonCommand`, `DungeonHooks`, `Harvest`, `CarcassBlock`, `LegacySeal`, `RadiationShield`, `RadiationCount` (15 of the 25 files removed from `dungeon/`; the Maw files and `DungeonBlocks` are in their own rows) | REMOVE | Only the removed dungeon asked "is this the Living Dungeon" (`LivingDungeon.in`). Callers that did were edited (section 6). |
| Maw: `DungeonMaw`, `MawReward`, `MawCredit`, `MawFit`, `MawFruitMath`, `MawManifest`, `MawShapes`, `MawStyle`, `MawText` | REMOVE | The legacy way in. Blocks and items below. |
| `dungeon/city` (Golden City, 12 files, 4 tests) | REMOVE | Zone of the Living Dungeon only. |
| `dungeon/nexus` core (23), `nexus/zone` (55: Golden City site, Monarch's Vault, Tempest Mirage, Underworld Gate, Hive, Gravity Warp), `nexus/collapse` (25), `nexus/vault` (27), `nexus/nav` (19), 5 of `nexus/relic` (`Relics`, the facade the vault, zones and immune system used; `RelicSlot`, `RelicBlocks`, `ArtifactBlock`, `ManaGateBlock`: the pedestal and mana gate) | REMOVE | The Variable Core and everything it built or collapsed. `DungeonCollapseEvent` was the event the floor bosses posted and the Djinn Seal code listened to: replaced by a direct call (section 6.3). |
| `dungeon/sentient` (31 of 36 files): rooms, born mobs, the immune system, the Juggernaut, thresholds, ash-and-stone drops | REMOVE | The sentient layer of the Living Dungeon. |
| `dungeon/sentient/Dice`, `StoneTable` | KEEP | The economy rolls every drop with them: `EconomyDrops`, `EconomyMath`, `FactionLoot`, `FactionDrops`, the whole `mega/gear` package and the tests `EconomyMathTest`, `FactionLootTest`. |
| `dungeon/sentient/SentientItems`, `MaterialItem`, `MaterialTable` | KEEP | The twelve synthesis materials are registered items named by the tag `synthesis_materials`, the quest chapter `gauntlet` (direwolf pelt, disciple sigil) and three advancements (devil horn). |
| `dungeon/wild` (7 of 8 files): `Dweller`, `Roster`, `WildDriver`, `WildRules`, `WildSpawn`, `Appetite`, `HuntPlan` | REMOVE | The food web of the Living Dungeon. |
| `dungeon/wild/Wildlife` | KEEP, rewritten | `EconomyDrops` asks it for the tier of a mob by the entity tags `nusmp:dungeon/tier1..3`, `legendary`, `excluded`. The decision itself moved into the new pure `TierRule`. |
| Entity tags `tags/entity_type/dungeon/` `tier1`, `tier2`, `tier3`, `legendary`, `excluded` | KEEP | `Wildlife` (the economy's tiers) and `FactionDataTest` read them. |
| Entity tag `tags/entity_type/dungeon/grazers` | KEEP-ORPHAN | No game code reads it; the Mega-Dungeon's `FactionDataTest` (`everyCreatureOfTheToughnessTiersHasAFactionUnlessItIsAGrazer`) does. It was removed first by mistake and put back when the test run caught it. |
| Entity tags `dungeon/aquatic`, `avian`, `born_catacombs`, `born_underworld`, `born_waterways`, `juggernaut_catacombs`, `juggernaut_underworld`, `juggernaut_waterways` | REMOVE | Read only by the born-mob and immune-system code of the Living Dungeon. |
| `dungeon/DungeonEntities`, `DungeonCreature`, `DungeonGoals`, `DungeonSlimeEntity`, `LivingArmorEntity`, `DirewolfEntity`, `BasiliskEntity` | KEEP, cut | The creatures are in the entity tags of the Mega-Dungeon (`tags/entity_type/mega/construct`, `beast`, `slime`), in `data/nusmp/mega/encounters/*.json`, in the quest chapter `gauntlet` (kill counts) and in the renderers; `FloorWardenEntity` extends `LivingArmorEntity`. The ecology (hunger-driven breeding, carcasses, the `Dressed` flag) is gone from them. |
| `dungeon/DungeonGuard`, `DungeonGuardRules` | KEEP, cut | The Guard holds the whole Mega-Dungeon and the arena of the Final Hour (mixins `LevelGuardMixin` and `LevelChunkGuardMixin`, `SpellRuntime`, `TreeRestore`, `UnderworldMatter`, the Wave relics, `GatekeeperDoorBlock`, ...). Lost: the Living Dungeon branch, the immune system, the fixture exemption. |
| `dungeon/DungeonFood`, `MealTable`, `MealPerks`, `MealBuffs`, `MealSkills`, `MealAttunement`, `SkillLease`, `SkillsData`, `Cooldowns`, `DungeonEffects` | KEEP | The cooking system and its effects: the economy's meat (`EconomyDrops`, `FactionDrops`, `EcoSelfTests`), `ManaLaw`, `CharterMirror`, `Lucius`, the crucible self-tests, quests ("Delicious in Dungeon"). |
| `dungeon/floor/goliath` and `charybdis`: the arenas (`GoliathPitPlan/Site/Gates/Ways`, `PitRng`, `CharybdisBasinPlan/Site`, `CharybdisGates`, `CharybdisWays`, `CharybdisRng`, `CharybdisSurface`) | REMOVE | The pit and the basin were Living Dungeon floors. |
| `dungeon/floor/goliath` and `charybdis`: the bosses (`GoliathBoss`, `CharybdisSpawnBoss`, moves, math, tables, text, VFX, entities, `MagiculeScaleEntity`, `Swim`, `ParryRules`, `CharybdisMinions`) and the commands `/multiverse boss goliath` and `/multiverse boss charybdis` | KEEP | Bosses stay (brief). Advancements `bosses/goliath`, `bosses/charybdis_spawn`, `bosses/all`, quests, the entity tag `boss`, the renderers. With their arenas gone they are called by command or spawn egg; their pay-out (the Djinn Seal source) works wherever `Labyrinth.in` is true. |
| `dungeon/nexus/relic` (30 files: the six Treasury relics: Glutton's Chalice, Mantle of the Scavenger, Band of the Spatial Anchor, Devil-Bound Sheath, Gambler's Grimoire Page, Amulet of the Juggernaut's Blindness) | KEEP | Relics stay (brief): `RelicItems` in the creative tab and `WeaponRenderer`, the Curios tag `dungeon_relic`, `Worn` in Lucius's capabilities, `RelicEvents` on the bus. Their handlers lost the ties to the removed immune system and radiation. |
| Blocks `dungeon_gate`, `dungeon_maw`, `maw_root`, `dungeon_mycelium`, `carcass`, `aurelian_altar`, `descent_gate`, `illusion_stone`, `illusion_deepslate`, `nutrient_bloom`, `warp_trap`, `artifact_block`, `mana_gate`, `golden_leaves` (14) | REMOVE | None of them appears in the block palette of any of the 124 templates left (`structure/mega/**`, `hub`, `mega_gateway`), in a pool, a processor, a loot table, a recipe, a tag, a quest or an advancement. This is the check behind "keep the blocks if the new one uses them": the new dungeon uses none of them. |
| Items `dungeon_gate`, `dungeon_maw`, `maw_root`, `dungeon_mycelium`, `aurelian_altar`, `aurelian_core`, `descent_gate`, `illusion_stone`, `illusion_deepslate`, `nutrient_bloom`, `nutrient_nectar`, `warp_trap`, `artifact_block`, `mana_gate`, `golden_leaves`, `maw_fruit`, `ki_resonance_lantern`, `anti_bird_compass`, `venom_gland` (19) | REMOVE | Obtainable only in the removed dungeon (the lantern and the compass only found its Variable Core). Referenced by nothing that stays. |
| Blocks of `labyrinth/*` (`labyrinth_gate`, `labyrinth_exit`, `gatekeeper_door`, `guardian_altar`, `sealed_archway`, `ward_rune`, `spawn_glyph`, `pitfall_tile`, `snuffer_glyph`, `sigil_plate`, `riddle_stone`, `relic_pedestal`, `peak_altar`, ...) and `labyrinth/gate` (`GateBlock`, `ExitGateBlock`, `GateRegistry`, `GateRules`, `LabyrinthTravel`, `LabyrinthEvents`) | KEEP | In the templates of the Mega-Dungeon (the palette scan lists them) and the exit gate and the Gatekeeper door are rooms of the new dungeon. |
| Structure `nusmp:dungeon_gate`, structure set `dungeon_gates`, pool `gate/base`, templates `gate/ruin_a`, `ruin_b`, biome tag `has_structure/dungeon_gate` | REMOVE | The legacy arches. |
| Structure **tag** `nusmp:dungeon_gate` | KEEP, now only the Gateway | `Labyrinth.GATE_TAG` and `LabyrinthLocate` ask it where the way in is. Renaming it would touch `labyrinth/mega/**`; the file now lists `nusmp:mega_gateway` only. |
| `MegaConfig.LEGACY_GATES` and its check in `MegaTravel` | REMOVE | Nothing is left to refuse. |
| Dimension `nusmp:living_dungeon`: dimension, dimension_type, biome `dungeon_depths`, effects id `nusmp:living_dungeon` | KEEP-ORPHAN (inert) | Same reason as the Spire's. In addition the dimension type of the **Mega-Dungeon** names `"effects": "nusmp:living_dungeon"`, so `DungeonSky.EFFECTS_ID` and its `registerEffects` stay (the rest of `DungeonSky`, the mood layer, went). |
| Data attachments `nusmp:ecosystem`, `nusmp:dweller` | REMOVE | Only the removed ecology read them. See the old-world section for what a save with them does. |
| Data components `nusmp:fuel`, `nusmp:flame`, `nusmp:target` (`NavComponents`) | REMOVE | Only the lantern and compass carried them; their items are removed with them. |

### 2.1 The template scan

`python3 -I` over every `.nbt` under `data/nusmp/structure` (a small NBT reader, palette names only) after the cut: 124 templates, the nusmp blocks
that appear are `gatekeeper_door`, `guardian_altar`, `labyrinth_exit`, `labyrinth_gate`, `pitfall_tile`, `relic_pedestal`, `riddle_stone`,
`sealed_archway`, `sigil_plate`, `snuffer_glyph`, `spawn_glyph`, `ward_rune`. None of the 14 removed blocks is among them. (Before the cut: 156
templates; the 32 that went are the 30 under `structure/labyrinth/**` and the 2 under `structure/gate/`.)

## 3. What was removed, in numbers

| Package | Main files | Test files | Notable classes |
| --- | ---: | ---: | --- |
| `dungeon` (root) | 25 | 10 | `LivingDungeon`, `DungeonEcology`, `DungeonConfig`, `DungeonCommand`, `DungeonMaw`, `MawReward`, `DungeonBlocks`, `CarcassBlock`, `DungeonTier`, `DungeonAtmosphere`, `RadiationShield` |
| `dungeon/city` | 12 | 4 | `CityBuilder`, `CityPlan`, `CityBlocks`, `AurelianCore` |
| `dungeon/nexus` | 23 | 11 | `NexusBuilder`, `NexusZones`, `NexusCommand`, `DungeonCollapseEvent`, `SpatialDistortion` |
| `dungeon/nexus/zone` | 55 | 25 | `MonarchBoss`, `MonarchDriver`, `HiveDriver`, `GravityWarp`, `GoldenCitySite`, `TempestMirageSite`, `UnderworldGateSite` |
| `dungeon/nexus/collapse` | 25 | 12 | `CollapseManager`, `CollapseRunner`, `CollapseCommand` |
| `dungeon/nexus/vault` | 27 | 6 | `VaultDirector`, `NexusBlocks`, `DescentGateBlock`, `IllusionBlock`, `WarpTrapBlock` |
| `dungeon/nexus/nav` | 19 | 6 | `LanternItem`, `AntiBirdCompassItem`, `NavComponents`, `NavTicker`, `AvianDrops` |
| `dungeon/nexus/relic` | 5 | 0 | `RelicBlocks`, `ArtifactBlock`, `ManaGateBlock`, `RelicSlot`, `Relics` |
| `dungeon/sentient` | 31 | 14 | `DungeonRoomManager`, `ImmuneSystem`, `WallBirth`, `BornDriver`, `JuggernautSpawner`, `SentientCommand` |
| `dungeon/wild` | 7 | 5 | `WildDriver`, `Dweller`, `Roster`, `WildRules` |
| `dungeon/floor/goliath` | 5 | 2 | `GoliathPitSite`, `GoliathPitPlan`, `GoliathPitGates` |
| `dungeon/floor/charybdis` | 6 | 2 | `CharybdisBasinSite`, `CharybdisBasinPlan`, `CharybdisGates` |
| `labyrinth/gen` | 7 | 2 | `BiomePaletteProcessor`, `MagiculeVeinProcessor`, `GenRegistry`, `BiomeMirror` |
| `client/nexus` | 1 | 0 | `NavClient` |
| **Total** | **248** | **99** | `dungeon/**` went from 316 main files to 77 (76 old + the new `TierRule`) |

Resources and tools removed: 201 asset files (14 blockstates, 39 block models, 57 item models, 35 block textures, 42 item textures, 9 weapon
textures, 5 weapon meshes), 81 data files (section 4), and 8 generator scripts (`tools/gen_dungeon_city_assets.py`,
`gen_nexus_golden_assets.py`, `gen_nexus_vault_assets.py`, `gen_nexus_nav_assets.py`, `gen_nexus_relic_assets.py`, `gen_dungeon_reward_assets.py`,
`tools/labyrinth/gen_spire.py`, `tools/labyrinth/gen_gate.py`). `tools/gen_dungeon_assets.py` lost the parts that drew the removed objects.
Unused lang entries are left (harmless, as the brief says).

## 4. The data files removed

* `data/minecraft/tags/block/mineable/axe.json` (it listed only `nusmp:maw_root`).
* `data/nusmp/advancement/dungeon/` `descent`, `labyrinth`, `living_dungeon`, `maw`, `maw_item` (the generator no longer writes them).
* `data/nusmp/data_maps/worldgen/biome/labyrinth_palette.json`, `data/nusmp/recipe/ki_resonance_lantern.json`.
* `data/nusmp/structure/gate/` `ruin_a`, `ruin_b`; `structure/labyrinth/`: `base`, `ascent_1..4/` (`hall_n`, `gallery_n`, `shrine_n`: 12), `caps/` (4),
  `crown/spire_top`, `peak/boss_peak`, `side/` (9), `vault/treasury`, `warden/hall` (30 in all).
* `data/nusmp/tags/block/` `illusion_stones`, `labyrinth/swap_accent`, `swap_floor`, `swap_pillar`, `swap_wood`, `labyrinth/veinable`.
* `data/nusmp/tags/entity_type/dungeon/` `aquatic`, `avian`, `born_catacombs`, `born_underworld`, `born_waterways`, `juggernaut_catacombs`,
  `juggernaut_underworld`, `juggernaut_waterways` (`tier1..3`, `legendary`, `excluded` stay: `Wildlife` reads them; `grazers` stays: `FactionDataTest` reads it).
* `data/nusmp/tags/item/` `butchering_blades`, `lantern_cores`, `lantern_fuel`.
* `data/nusmp/tags/worldgen/biome/has_structure/` `dungeon_gate`, `labyrinth_spire`; `tags/worldgen/structure/labyrinth_spire`.
* `data/nusmp/worldgen/processor_list/` `labyrinth_interior`, `labyrinth_palette`, `magicule_veins`.
* `data/nusmp/worldgen/structure/` `dungeon_gate`, `labyrinth_spire`; `structure_set/` `dungeon_gates`, `labyrinth_spires`.
* `data/nusmp/worldgen/template_pool/` `gate/base`; `labyrinth/` `ascent_1..4`, `base`, `caps`, `crown`, `peak`, `side`, `side_end`, `vault`, `warden`, `wildcard`.

Data files **edited** (not removed): `dimension/labyrinth.json` (`structure_overrides` is `[]`), `tags/worldgen/structure/dungeon_gate.json` (only
`nusmp:mega_gateway`), and the advancements `arcs/dungeon_arc`, `dungeon/archway`, `charybdis`, `entered`, `goliath`, `labyrinth_event`, `meat`,
`traps`, `warden` (regenerated, section 9). `assets/nusmp/weapon_classes.json` lost the lantern and compass lines.

## 5. What stayed

### 5.1 Kept, and the reason

The table in section 2 gives the reason per group. The check after the cut: the graph was run again on the finished tree; of the 77 classes left in
`dungeon/**`, 64 are reached from live code outside `dungeon/` and the other 13 only through a registration (`NUSMP`, `NUCreativeTab`,
`MultiverseCommands`): `CharybdisCommand`, `GoliathCommand`, `RelicCommand`, `RelicEvents` and the handlers it wires (`AmuletHandler`,
`MantleHandler`, `SheathHandler`, `RegenMuffle`, `MantleRules`, `RelicRules`), and `SentientItems` with `MaterialItem` and `MaterialTable`. None is
unreachable.

### 5.2 Kept orphans (still registered or present, nothing in the new dungeon needs them)

| What | Why it stays |
| --- | --- |
| Dimensions `nusmp:labyrinth` and `nusmp:living_dungeon` (dimension, dimension type, biome files; the client effects) | Old saves name them. They are data only and generate nothing. |
| The six Treasury relics and their handlers | Relics stay by the brief. The Treasury room that gave them is gone, so they are now obtainable by `/multiverse dungeon relic give` and the creative tab only. Adding them to the Mega-Dungeon's vault tables is a design call for the lead. |
| The twelve synthesis materials | Tag, quests and advancements name them; the sentient drop table that produced them is gone, so they too come by command and creative tab until the economy adds them to a table. `MaterialTable.materialFor` stays and is tested. |
| `GoliathRise.triggers`, `TRIGGER_RANGE`, `TRIGGER_HEIGHT` (who wakes the Goliath in its pit), `GoliathText.fallen` and `CharybdisText.fallen` (the lines that promised a pit or basin would come down), `GoliathText.woken` | Pure helpers of the removed arenas. The three tests that pin them (`GoliathRiseTest`, `GoliathTextTest`, `CharybdisTextTest`) stay. Harmless; a later clean-up can remove them with their tests. |
| `DigestTable.WARD_AHEAD` | The radiation ward the Glutton's Chalice cast (`RadiationShield`) is gone; the constant has no reader and no test. |
| `HubKeys.LEGACY_GATE_USED` (written by `HubTravel`) and `LabyrinthTravel.LEGACY_GATE_USED` (the same spelling, `nusmp_dungeon_gate`) | The stamp the removed Dungeon Maw read. `HubKeysTest` pins that `LabyrinthTravel` spells it; nothing reads it now. Kept so no file under `labyrinth/mega/**` had to change. |
| `HouseholdRules.isSealBoss` and `DarkRules.isShardZone` still name `MONARCH_VAULT` | The Blood-Red Commander (the Monarch's Vault boss, removed with the Nexus zones) was the fourth Djinn Seal source and a Dark Rukh Shard source. Both are pure rules with tests; no caller can pass that zone any more. There are three Seal sources now (Goliath, the Charybdis Spawn, the Devil Avatar). Moving the fourth to a Mega-Dungeon boss is a design call for the lead. |
| `LightItems` names `ki_resonance_lantern` (D&D light table) and `WeaponClassesTest` uses its id as sample text | The item is gone; a name in a list matches nothing. Left because the D&D layer is out of scope and a test pins the list. |
| Lang entries of every removed block, item, advancement and message | The brief says to leave unused lang keys. |
| `tools/labyrinth/gen_mega_gateway.py` docstring still mentions `dungeon_gates` | It is history in a comment, in a file other builders own. |

## 6. The code that stayed and had to be cut

### 6.1 The Guard and the creatures

* `DungeonGuard.on(level)` is `ZogratisArena.in(level)` (always) or `Labyrinth.in(level)` and the `unbreakable` switch of the **labyrinth** config.
  The Living Dungeon branch, the `ImmuneSystem` threat feed, `SentientMode` and the `isFixture` exemption are gone; `onExplosion` just clears the
  affected blocks. `DungeonGuardRules.exempt / isProtected / refusesChange` lost their `fixture` argument and `DungeonGuardRulesTest` was updated.
* `DungeonCreature`: no `dressed` flag, no `dress()`, no abstract `weight()` / `carcassKind()`, no ecosystem part in `feed`, no `die()` override
  (carcasses), no `Scavenge` goal. `feed(int)` is public and kept (meals and slimes use it). The "Dressed" NBT key is no longer written or read.
  `DungeonSlimeEntity.metabolize()` grows a slime from hunger directly (no ecosystem biomass). The living armour, direwolf and basilisk lost their
  two overrides.
* `Wildlife` reads the five entity tags and nothing else; the rule that turns tag membership into a tier is the new pure `TierRule` with
  `TierRuleTest`.

### 6.2 Bosses

* `GoliathBoss`: no pit or Nexus code. `spawnRising(ServerLevel, BlockPos seal, float yaw)`. When it falls inside a Labyrinth dimension it
  broadcasts `GoliathText.slain` (a plain "The Goliath has fallen to X."; the old `fallen` line said its pit would come down) and calls `DjinnSources.floorBossFell(level, arena, "GOLIATH_PIT", killer)`. The pit plan's radius moved into
  `GoliathTable.ARENA_R = 28`.
* `CharybdisSpawnBoss.onFightEnd` calls a new `reportFall` (broadcast `CharybdisText.slain`, a plain "X has slain the Charybdis Spawn!"; the old `fallen` line sent the players to the relics of a
  basin that comes apart; then `DjinnSources.floorBossFell(..., "CHARYBDIS_BASIN", ...)`, inside a Labyrinth dimension). `CharybdisTable` gained `ARENA_R = 28`, `ARENA_OUTER = 32`, `ARENA_HEIGHT = 24`, `ARENA_HOVER = 5`.
  `CharybdisMinions` lost the orphan sweep and gained `onServerStopping` (clears its statics; it was one of the basin site's stopping hooks).
* `DevilAvatarBoss.reportFall()` does the same for the Avatar (`"UNDERWORLD_GATE"`), guarded by `fallReported`; the NBT key `AFell` is unchanged.
* `/multiverse boss goliath`, `/multiverse boss goliath rise` and `/multiverse boss charybdis` stay (the two command classes were restored after the
  first cut); the Goliath appears where the executor stands (or climbs out of the floor under them), the Charybdis Spawn at the executor with the arena there.
* The strings `"GOLIATH_PIT"`, `"CHARYBDIS_BASIN"`, `"UNDERWORLD_GATE"` are **ledger keys** of the Djinn seal sources and are written into saves; they
  keep their old spelling on purpose.

### 6.3 Relics, the Djinn, the Treasury

* `DungeonCollapseEvent` is gone. `DjinnSources.onCollapse(event)` is replaced by `floorBossFell(ServerLevel, BlockPos, String zone, ServerPlayer)`
  (the three call sites above, each inside a try/catch); the listener line in `DjinnEvents` is removed; `DarkDjinn`'s two "inside a collapse" flags are
  `false`. The Blood-Red Commander was the fourth caller and is gone (section 5.2).
* `Relic` lost `treasury()` / `legacyTreasury()` / `pool()` (the Treasury's loot hooks); `RelicSlot` (the pedestal's enum) is removed with the
  pedestal block; `GamblerPage.backfire` and `ChaliceHandler.shield` no longer call the immune system, the collapse or `RadiationShield`.
* The Wave relics (13 files in `relics/wave`) asked `LivingDungeon.in(level)` where they now ask `Labyrinth.in(level)`: their protections now hold in
  the Mega-Dungeon (before, they held only in the removed caves). The Vizier card of `Wave30Deck` names the Gateway where it named the nearest Dungeon Gate or Spire.

### 6.4 The five forced edits in `labyrinth/mega/**` (exact)

1. `system/ShadowMinions.java`: removed `import ...dungeon.nexus.zone.MonarchBoss;` and the term `|| MonarchBoss.isSoldier(e)` in `isServant`'s test
   (the Monarch's soldiers no longer exist).
2. `MegaConfig.java`: removed `LAYOUT`, `LEGACY_GATES`, `LAYOUT_MEGA`, `LAYOUT_SPIRE`, `DEFAULT_LAYOUT`, their two `define` calls, and rewrote the class
   Javadoc.
3. `travel/MegaTravel.java`: removed the `oldGate` parameter of `enterSafely` / `enterNow`, the `LEGACY_GATES` refusal, the `atGateway` helper and the
   imports `Labyrinth` and `StructureStart`; Javadoc text.
4. `gen/ExpansionQueue.java`: `if (Labyrinth.layoutIsMega() && ticks % 2 == 0)` became `if (ticks % 2 == 0)`.
5. `safe/ModCompatProcessor.java`: the import and the two uses of `BiomePaletteProcessor.carryProperties` now name `StateCarry.carryProperties`.

### 6.5 The way in and out

* `Labyrinth` keeps `DIMENSION`, `MEGA_DIMENSION`, `SANCTUARY_DIMENSION`, `MEGA_GATEWAY_TAG`, `GATE_TAG`, `in`, `inMega`, `inSanctuary` and gains
  `REMOVED_DUNGEON_DIMENSION` / `inRemovedDungeon`. Gone: `SPIRE_TAG`, `BIOME`, `ENGINE_LABYRINTH`, `ENGINE_LEGACY`, `layoutIsMega()`, `engineIsLabyrinth()`.
* `LabyrinthTravel.enter` is the Hub when the Hub is on, else `MegaTravel.enter`. `evacuate(player)` (new) carries a player found in
  `nusmp:living_dungeon` back to where the Maw took them from (the persistent key `nusmp_dungeon_origin`, kept on purpose) or to their bed or the world
  spawn, then says so. `LabyrinthEvents.onPlayerTick` calls it in its ten-tick check. `GateRules.cooldownHolds(now, stamp)` replaced the two-stamp
  version (`GateRulesTest` updated). `LabyrinthTravel.LEGACY_GATE_USED` stays as a constant (section 5.2).
* `NUSMP.java`: only deletions of registrations, plus one added line, `CharybdisMinions::onServerStopping` (the replacement of the basin site's
  stopping hook). Exact lines are in the report to the lead.

## 7. Config

Removed:

| File | Key | Default |
| --- | --- | --- |
| `nusmp-dungeon-server.toml` (the whole file; `DungeonConfig` is no longer registered) | `dungeon.enabled`, `hazards`, `broadcast`, `life`, `noLoot`, `unbreakable`, `mawThreshold` (600.0), `mawMinutes` (20), `mawCooldownMinutes` (30), `regrowDays` (2.0), `simRadius` (3), `sentient`, `roomPopulation` (1.0), `roomRebirthMinutes` (30), `maxBornPerPlayer` (28), `juggernaut`, `juggernautThreat` (100.0), `juggernautCooldownMinutes` (10), `stoneDrops` (1.0) | 19 keys, booleans true |
| `nusmp-labyrinth-server.toml` | `labyrinth.engine` (`"labyrinth"`) | the switch between the Spire and the caves |
| `nusmp-labyrinth-mega-server.toml` | `mega.layout` (`"mega"`), `mega.legacyGates` (true) | the switch to the Spire, the old arches |

An old config file that still has these keys is not an error: keys a spec does not define are ignored (the spec's correction step may also drop them from the file; not tried here). The labyrinth key
`labyrinth.unbreakable` stays; it is the one the Guard now reads. `nusmp-dungeon-server.toml` is left on disk in old worlds, unread.

No config key was added.

## 8. Commands

* Removed: the whole `/multiverse dungeon ...` tree of the Living Dungeon (`enter`, `exit`, `maw open/close`, `status`, `ecosystem`, `spawn`, `census`,
  and the sub-trees of the city, nexus, vault, collapse and sentient builders). To summon the four creatures use `/summon` with their ids.
* Kept: `/multiverse labyrinth locate | enter | leave | here` (`palette` is gone with the Spire), `/multiverse mega ...`, `/multiverse boss goliath`
  and `charybdis`.
* Kept under the same name: `/multiverse dungeon relic ...` (the Treasury relics). `RelicCommand.addTo(node)` could not go on `/multiverse relic`
  (the Wave relics own that word), so `MultiverseCommands` builds a small `dungeon` node for it:
  `var dungeonNode = Commands.literal("dungeon"); RelicCommand.addTo(dungeonNode); root.then(dungeonNode);`

## 9. Quests and advancements

The specs were edited and regenerated with the repo's own generators (`gen_advancements.py`, `gen_quests.py`, `gen_docs.py`; a second run changes
nothing; `validate_snbt.py`: 22 chapters, 333 quests, 464 tasks, 0 errors, 0 warnings).

* Advancements removed: `dungeon/living_dungeon` (Into the Living Dungeon), `dungeon/maw`, `dungeon/maw_item`, `dungeon/descent`, `dungeon/labyrinth` (the
  Spire's). Their children now hang off `root`, `dungeon/entered` or `dungeon/mega`.
* `dungeon/entered` ("Dungeon Delver") keeps **all its criteria** (the old doors `a0_..d3_` and the Mega-Dungeon ones), because players' saved progress
  is stored by criterion name. Only the quest tasks and descriptions changed. `NusmpEvents` key `dungeon.entered` is no longer fired by anything
  (it stays in the criteria and in `AdvancementDataTest.EVENT_KEYS`); `dungeon.maw` is gone from the test and the docs.
* Quests removed: "The Maw Opens", "Down the Living Dungeon" (arc 3), the chapter `dungeon_labyrinth`'s "The Maw", "Into the Living Dungeon",
  "Dungeon Maw", "Descent Gate". Arc 3 now has Rumours, Delicious in Dungeon, Pit Guardian and the final. Arc 8's gate task asks for the dimension
  `nusmp:mega_dungeon`.
* Lang: four existing keys need their text replaced (the lead merges them; `merge_lang.py` never overwrites): see `$S/arc/frag/prune.lang.json`
  (`advancement.nusmp.bosses.goliath.desc`, `advancement.nusmp.dungeon.entered.desc`, `advancement.nusmp.dungeon.goliath.desc`,
  `advancement.nusmp.dungeon.labyrinth_event.title`).

## 10. Old worlds

What the owner accepted ("unknown blocks become air, unknown entities vanish") and what was done so that nothing else breaks:

* **Blocks.** The 14 removed blocks become air when their chunk loads (Minecraft's block registry answers air for an unknown name). A Dungeon Gate
  arch or Maw that stands in the Overworld loses its portal block and root blocks; the stone around stays. The structure start in the chunk is
  logged as unknown and ignored; `/locate` no longer finds it.
* **Items.** The 19 removed items (lantern, compass, Maw Fruit, Variable Core blocks ...) vanish from inventories and chests with a log line.
* **Players inside `nusmp:living_dungeon`.** The dimension stays registered so the world decodes; on the first ten-tick check the player is carried
  out (`LabyrinthTravel.evacuate`). `nusmp:labyrinth` (the Spire's sky) is still a Labyrinth dimension for the Guard and the void rescue; `/multiverse
  labyrinth leave` and the exit gate lead out of it.
* **Saved data.** `nusmp_dungeon.dat`, `nusmp_golden_city.dat`, `nusmp_nexus.dat`, `nusmp_collapse.dat`, `nusmp_gates.dat`, `nusmp_vaults.dat` and
  `nusmp_rooms.dat` in the Overworld's `data/` folder are no longer read. They can be deleted by hand; nothing deletes them.
* **Entities.** No entity type was removed: the creatures and both floor bosses keep their ids. A creature that carried the `nusmp:dweller` attachment
  loses it (see section 12).
* **Untouched.** The Mega-Dungeon (`nusmp:mega_dungeon`), the Hub (`nusmp:sanctuary`), the Gateway, player data, the economy ledgers, the Djinn
  ledgers and the relic ledgers are not changed.

## 11. Tools, CI and tests

* `tools/ci/server_smoke.py` and `tools/ci/mock_server.py`: the `locate-old-gate` check and the mocked answer for `nusmp:dungeon_gate` are gone; the
  `locate-gateway` check stays. `.github/workflows/smoke.yml` named no old-dungeon check (`SMOKE_EXPECT` lists only Mega-Dungeon phases), so it is
  unchanged.
* Generators that also drew the removed lantern and compass were trimmed so that running them cannot bring the deleted assets back:
  `tools/gen_nexus_relic_icons.py` and `tools/gen_nexus_relic_models.py` (their section for the lantern and for the compass, the registration lines and
  the doc text). Run in a scratch tree, the trimmed scripts reproduce the sprites, models, meshes and entity textures of the four relics that stay
  byte for byte (the Devil-Bound Sheath's item model differs from the file in the tree before and after the cut alike: it was tuned by hand later).
  `tools/relics/weapon_classes.json` (the copy of the runtime table), `tools/bettercombat/manifest.json` (regenerated with its own
  `inventory.py`; its `--check` is clean) and `tools/relics/display_presets_manifest.json` lost their two entries for the lantern and the compass.
* Tests deleted with their classes: 99 files (table in section 3). Tests rewritten: `LabyrinthDataTest` (now checks the Gateway pools, templates,
  processors and start pool, and that the Spire and the old arch stay removed), `DungeonGuardRulesTest`, `GateRulesTest`, `RelicEnumTest`,
  `AdvancementDataTest.EVENT_KEYS`. Tests added: `TierRuleTest`, and one constants test each in `CharybdisTableTest` and `GoliathTableTest` for the
  arena numbers that moved.

## 12. Verified and not verified

Run here:

* The whole-tree type check (`tc_full.sh`, main and test sources against the API stubs): 1317 errors at the start, 1177 after, **0 new** (the
  comparison key is file + message + source line; the drop is the stub noise of deleted files). The remaining errors are the known stub artifacts and
  third-party classes the dump does not describe.
* A static scan of every kept asset model and blockstate: 0 unresolved model parents, textures or models (also 0 before the cut).
* The quest generators (idempotent) and `validate_snbt.py`.
* `py_compile` of every Python file edited; `tools/bettercombat/inventory.py --check` and `tools/relics/display_presets.py --check` are clean.
* **Every pure test class** (the 393 test classes whose imports reach no Minecraft or NeoForge class) with the real JUnit 5.10.2 console launcher:
  3441 tests, 3441 passed, 0 failed. This run found two breakages of mine that the type check cannot see, both fixed: the Mega-Dungeon's
  `FactionDataTest` reads the entity tag `dungeon/grazers` by a path it builds at run time (the file was put back, section 5.2), and `HubKeysTest` pins
  that `LabyrinthTravel` spells the legacy gate stamp (the constant was put back, section 5.2). Two more failures of the first run were my own
  invocation (the main resources were not on the class path) and went away when they were.
* The repo's local shim runner (`alltests.sh`) was started too, on a machine loaded by many builders, and stopped by the 30-minute limit of a
  background job after 261 of 432 classes: 193 passed (1593 tests), 42 did not compile under the shim (it knows only a few assertions: no
  `assertThrows`, `fail`, `assertArrayEquals`, and the int overloads of `assertEquals` are ambiguous; some of them also reach Minecraft classes), and 26
  failed only because the runner starts in `src/test/java`, so a data test that reads `src/main/resources/...` by a relative path cannot find its file
  (23 of them pass under the real launcher, which starts in the project root; the other 3 are grimoire tests that reach game classes and run only in CI). I did not run the shim on the untouched tree, so this is not a before and
  after comparison; the real-JUnit run above is the result to trust.
* A mutation check of `TierRuleTest`: three mutants of `TierRule` (no veto, legendary as 2, tier 2 as 1) are all caught.
* The 41 test classes that import Minecraft or NeoForge classes were not run (the CI build runs them); the five of them that read repository files
  read nothing that was removed (grimoire textures, avatar textures, the lang file, `WikiBooks`).

Not run, and assumptions that could be wrong (nothing of this was started in a game):

* That NeoForge 21.1.259 **skips** a data attachment whose type is no longer registered (`nusmp:dweller` on entities, `nusmp:ecosystem`) instead of
  failing to load the entity or chunk. I believe it logs and skips; I did not read the loader in this environment.
* That a world whose `level.dat` names `nusmp:labyrinth` and `nusmp:living_dungeon` decodes with the inert dimension files present, and that the
  generator of `nusmp:labyrinth` with `structure_overrides: []` and no structure set generates nothing.
* That an unknown structure start in old chunk data is only logged.
* That the evacuation really lands a player home (the teleport call is the one `LabyrinthTravel.leave` has always used; this branch never ran).
* The extended relic-wave protections (they now count the Mega-Dungeon as the place they hold) change balance in a way nobody has played.
* The Treasury relics and the synthesis materials are now reachable only by command or the creative tab (section 5.2); whether that is wanted is the
  owner's call.
* `RelicEnumTest`, which I edited (the Treasury tests went with `Relic.treasury()`), is one of the 41 classes that reach game classes (`Relic` imports
  `Item`), so it has not been executed anywhere yet; it type-checks and its remaining assertions are the unchanged ones plus one that is a subset of
  the removed one.
* That unknown blocks, items and structure starts in old saves are handled the way the game handles any removed mod content (air, an empty stack, a
  logged error). This is the standard behaviour and the owner accepted it; it was not tried here.
* That the NeoForge config loader accepts an old file that still holds the removed keys (see section 7).
* The messages of the two floor bosses no longer promise a collapse, but the fight lines still say "pit" and "basin" in places (flavour text of the
  arenas they were written for); a rewording is cosmetic.

## 13. Sources

None. Everything here was derived from this repository; no external page was read.

# Elite world: bosses, world events and the magicule world engine (1.83.0, `elite/world`)

Written without a compiler for the whole project and without the game. The pure rules were compiled and unit tested with the JUnit shim (`msf_jt.sh`), the Minecraft-facing classes
were type-checked against the API stubs (`tc_wt.sh`; the only errors left are the stub artefacts "X is abstract; cannot be instantiated" and "does not override deserializeNBT in
INBTSerializable", a negative control was seen to fail), the models were rendered with `tools/render_preview/preview.py`, nothing ran in the game. **The original addon's source is not
available**: every design, number, trigger and word below is this mod's own. The inventory names are the owner's.

Inventory: "Bosses, World Events & Magicule Mechanics. Calamity System: boss phases, calamity core, and spawn triggers. Boss Entities: GraveboundColossus, LichBoss, GaiaSpikeEntity,
VoidBoltProjectile. Magicule World Engine: chunk magicule storage and sync, crystal growth stages (Magic Ore growth: Small Bud, Medium Bud, Large Bud, Cluster, Mother Rock), mother rock
growth, ore conversion, and mob replacement handlers."

## 1. NEW / EXISTS / REPLACE (CLAUDE.md rule 5)

| Part | What the mod already had | Verdict | Why |
|---|---|---|---|
| Chunk magicule storage and sync | Tensura keeps its own per-chunk magicule (`tensura.storage.chunk.ChunkStorage`: current/max/regeneration, used for players' magicule regeneration). Nothing in the mod stored a 0..1 world density. | **NEW** (next to Tensura's, not a bridge) | Tensura's number is an absolute amount for regeneration with its own modifiers; the engine needs a normalised density with biome, crystals, events and depth. Reading Tensura's storage by an API I cannot compile against is a risk with no gain; the two coexist. |
| Crystal growth (5 stages) and the Mother Rock | `magicule_vein` (a decorative Labyrinth block), Tensura's magic crystals (items) and `tensura:magic_ore`. No growing crystal. | **NEW** | The five blocks are new ids; nothing replaced. |
| Ore conversion | none | **NEW** | Uses Tensura's own `tensura:magic_ore`, `deepslate_magic_ore`, `silver_ore`, `deepslate_silver_ore` (they exist in the API dump) so no ore of our own was added. Targets are looked up by id at run time and skipped when absent. |
| Mob replacement handler | `HubRules`/`HubSpawnPolicy` cancel hostile spawns in the Sanctuary; `FoundryEncounters` replace spawns in the Mega-Dungeon. Neither touches the Overworld. | **NEW** | The Hub rules stay and are respected (the engine never acts in the Sanctuary). |
| Calamity system (director, triggers, phases, announcements, core) | none | **NEW** | |
| `LichBoss` | There is NO Lich mob in the Java code. The Lich content that exists is the Mega-Dungeon Foundry mobs `foundry_lich_of_the_silent_library` and `foundry_elder_lich_archivist_of_ruin` (data-driven dungeon mobs, `FoundryMobEntity`) and the item `lich_wand`. | **NEW**, decision: nothing to reuse or extend | A Foundry mob is a 1-of-400 data row of a single entity type with a role in dungeon encounters, with no phases; a world boss with phylacteries cannot be one. They stay untouched and unrelated. |
| `GraveboundColossus` | none (the dungeon has Goliath, a different boss) | **NEW** | |
| `GaiaSpikeEntity`, `VoidBoltProjectile` | none (`grep void_bolt|gaia` found nothing) | **NEW** | `VoidBoltProjectile.launch` is the call the Void skills use; no Void skill was changed. |
| `PhylacteryEntity` (fifth entity) | none | **NEW** | The inventory says the Lich has a phylactery; it needs a body the players can break. |
| Boss framework | `ZogratisBoss`/`ArcBoss` (devils, Gates, Charter, arenas), `DndBoss` (a layer on the Labyrinth Guardian, dungeon only) | **EXISTS, not reused as a base, reused as a standard** | See section 6. |
| Boss bar, boss rules, damage budget, `BalanceLaw` | all exist | **EXISTS, reused** | `BossBarSync.update` (generic overload), `BudgetedBoss`/`BossBudget` (so `BossIdentity` makes the boss rules apply: no regeneration in combat, no Unique/Ultimate skills), `BalanceLaw.cap`. |
| Rukh particles | `RukhFx`, `RukhParticles` | **EXISTS, reused** | Every effect of the bosses, the spike and the bolt is Light or Dark Rukh and nothing else (the owner's rule). |

No REPLACEMENT: nothing was removed or swapped. Existing files edited: see section 12.

## 2. The density model (`rules/MagiculeModel`, pure)

The stored value of a chunk column is a number in 0..1 that is MOVED toward a target, never set:

`target = clamp01( 0.10 (dimension floor) + 0.30 * biomeFactor + min(0.25, 0.01 * crystalScore) + min(0.30, motherBonus) + event )`

* **biomeFactor** 0..1 from `data/nusmp/elite/world/biome_magicule.json` (substring of the biome id, first match wins, default 0.3): deep dark 1.0, mushroom 0.95, `tensura`/`labyrinth` 0.9, lush caves 0.85, dripstone 0.8 ... plains 0.3, ocean 0.1, desert 0.05.
* **crystalScore** is kept by the crystal blocks' `onPlace`/`onRemove`: small bud 1, medium 2, large 3, cluster 5 (a Mother Rock counts through its bonus).
* **motherBonus** = `0.06 * rocks + 0.02 * sum of charges` (cap 0.30), the rocks being the Mother Rocks the chunk remembers by position (a small set, validated against the real blocks at every update).
* **event** is a signed bump in -0.5..+0.8 that decays to 0 by `eventDecayPerMinute` (0.01): a rising boss adds +0.15, a fallen calamity -0.35 (it spent the magicule), a broken Mother Rock -0.06, a matured one +0.05.
* **Depth is not stored**, it is added at the read: `effective = clamp01(stored + 0.20 * min(1, (seaLevel - y) / 96))`. A chunk is a column and the deep part of it is richer than its surface.
* **Movement**: `step` moves the density toward the target by at most `risePerMinute` (0.02, so a rich chunk needs most of an hour) or `fallPerMinute` (0.06), never overshooting; a chunk that was never updated answers its natural baseline (biome only) and the first update starts the chunk AT its target; a chunk nobody visits keeps its value and catches up in one step.
* **Bands** (HUD/commands): thin < 0.15 <= normal < 0.40 <= rich < 0.65 <= dense < 0.85 <= overflowing.
* **Storage**: a NeoForge chunk data attachment `nusmp:chunk_magicule` (`ChunkMagicule`: density, event, crystal score, Mother Rock positions, last update, ore budget). A chunk with no attachment costs nothing. Only chunks that are loaded and within `radiusChunks` (3) of a player are updated, every `updateTicks` (100), at most 256 chunks per level per pass.
* **Sync** (`MagiculeSync`, optional payload `nusmp:elite_magicule`, 15 bytes): per player every `syncTicks` (40) it is checked and sent only when the chunk changed or a value moved by >= 3/255 (own) or 5/255 (a cell of the 3x3 map), plus a refresh every 60 s; a player in a level where the engine is off gets one "off" packet. The client keeps it in `MagiculeClientCache` (no client classes, so the item tooltip can read it).
* **Hook**: `EliteHooks.chunkMagicule` (owned here) answers `MagiculeApi.effectiveAt(player)` (0 where the engine is off).
* **HUD**: holding `nusmp:magicule_gauge` in either hand draws a panel (bar, percentage, band, and the 3x3 chunk map) from `client/elite/world/MagiculeHud` (a GUI layer above all). Using the gauge prints the exact reading as an action bar message; its tooltip names the band.
* **Commands**: `/multiverse elite magicule get` (everyone), `set <0..1> [radius]` and `bump <delta> [radius]` (operators; `set` forces the density now and sets the event term so the target holds it there while the event fades).

## 3. Crystal growth (`rules/CrystalStage`, `CrystalGrowth`, `block/*`)

Five blocks in the owner's order: `magic_bud_small`, `magic_bud_medium`, `magic_bud_large`, `magic_crystal_cluster`, `mother_rock`. The first four are `AmethystClusterBlock`s (directional, waterloggable, the vanilla shapes by size, light 3/6/9/12) with random ticks; the Mother Rock is a full block with `charge` 0..3 (light 6..15).

| Step | Needs (density) | Chance per random tick (a block ticks about once a minute) | Extra |
|---|---|---|---|
| small -> medium | >= 0.15 | `base * (0.25 + 0.75 * k) * 1.0` | `k = (density - need) / (1 - need)`, `base = growthChance` (0.25) |
| medium -> large | >= 0.30 | same, x 0.9 | |
| large -> cluster | >= 0.45 | same, x 0.75 | |
| cluster -> Mother Rock | >= 0.85 | `motherChance` (0.04) x (0.5..1) | at least 3 other crystals within 3 blocks (a grove), fewer than `motherRocksPerChunk` (2) rocks in the chunk |
| Mother Rock charge +1 | any | `0.10 + 0.40 * density`, to 3 | |
| Mother Rock seeds a Small Bud | charge >= 1 | `seedChance (0.15) * (0.5 + 0.5 d) * (0.5 + 0.25 charge)` | spends one charge; on a free air/water face next to a solid block within `seedRadius` (4), never where more than 3 crystals stand within 2 blocks |

Drops (`data/nusmp/loot_table/blocks/*.json`): silk touch gives the block; otherwise Magicule Shards: small 1, medium 1-2, large 2-3, cluster 3-5, Mother Rock 6-9. `WorldEvents.onBlockDrops` adds Tensura's crystals when Tensura is there (cluster 25% a low-quality crystal, Mother Rock without silk 60% a medium-quality one). All five are in `#nusmp:magic_crystals` (block and item tag) and mined with a pickaxe. Growth never runs in a protected level.

**The ritual**: a Calamity Core used on a Mother Rock with charge >= 2 announces that core's calamity (the rock's charge goes to 0, the core is spent).

## 4. Ore conversion (`OreConverter`, `rules/OreConversion`)

In a chunk whose stored density is >= `densityThreshold` (0.70) the engine, one pass in three, asks each section's palette (`maybeHas`) whether an ore of the map exists and converts AT MOST ONE ore per pass, under the chunk's budget: `perInterval` (1) per window of `intervalTicks` (2400) and `perChunkLifetime` (24) in the chunk's whole life. The map (`data/nusmp/elite/world/ore_conversion.json`, files of all packs are merged, a later file replaces an ore) has: coal/copper/iron/lapis/redstone -> `tensura:magic_ore` (>= 0.70), the deepslate versions -> `tensura:deepslate_magic_ore`, gold -> silver (2) or magic ore (1) at >= 0.80, deepslate gold/diamond/emerald -> deepslate silver / magic ore at >= 0.85, diamond/emerald -> magic ore at >= 0.90. Targets are picked by weight among those registered. A converted block gets a small Light Rukh burst. Never in a protected level (`WorldGuard`).

## 5. Mob replacement (`MobTouchHandler`, `rules/MobTouch`)

On a NATURAL `FinalizeSpawnEvent` (never command, egg, spawner, structure) of a vanilla monster in `mob_touch.json`'s `eligible` list, in an allowed level, the chance is `chance(density) = min(maxChance, chance * (1 + 3 * (d - thr) / (1 - thr)))` (0.06 at the threshold 0.60, capped at 0.25). The tier is 1 from 0.60, 2 from 0.80, 3 from 0.92. A touched mob is **the same entity type** (so every other rule about it holds) with tier stats (health x1.5/2.2/3.2, damage x1.25/1.55/2.0, speed x1.03/1.06/1.10, armor +2/+4/+6), a name "Magicule-touched Zombie II" (shown on aim), a glowing outline (config `glowMarker`), a Dark Rukh aura near players, Magicule Shards in its drops (up to 1/2/3 rolls at 60%), a chance of a Tensura crystal at tier >= 2, and x1.5/2.2/3.0 experience. "Replaced" is therefore a stronger variant of the same mob, not another entity type (stated honestly: the data format has no `to`). Never in the Dungeon dimensions, the Sanctuary or the arenas. The calamity helpers carry `nusmp_calamity_add` and drop nothing.

## 6. The calamity system

**Why not `ZogratisBoss`/`ArcBoss`/`DndBoss` as the base.** `ZogratisBoss` is made for the devil family (power percentages, the Gates, Charter Grimoires, arenas that reset, a refill), `ArcBoss` for story characters registered in the roster (spawn eggs, floors, story unlocks, an `ArcSelfTests` check that every arc boss type is in the roster), `DndBoss` is a layer on the Labyrinth Guardian and only exists in the dungeon. A calamity is an open-world event. `CalamityBoss` is a lean `Monster` that takes from the mod what is shared: `BudgetedBoss` + `BossBudget` (the per-second damage cap, and through `BossIdentity` the boss rules), `BossBarSync.update(Entity, ServerBossEvent, ...)` (animated bar in the calamity's colours, phase and rage), `BalanceLaw.cap` for every hit on a player, `RukhFx` for every effect, `MoveScheduler`/`CalamityPhases` (pure, tested) for the fight.

**Director** (`rules/CalamityDirector` pure, `calamity/CalamityManager` glue, `calamity/CalamityData` saved data): every `checkTicks` (100) it observes each player (density where they stand, Mother Rocks within 3 chunks, night 13000-23000, moon phase) and asks the pure director. Triggers per calamity (`data/nusmp/elite/world/calamities/<id>.json`): `sustained_magicule` (density >= x for y seconds; the timer drains twice as fast when the condition breaks), `mother_rocks` (>= n rocks near and density >= x for y seconds), `night` (night, density, rocks, optional moon phase, a small chance per check), `ritual`, `command`. Rules: nothing wakes while another calamity is alive or announced (`maxActive` 1), each calamity has its own cooldown, a calamity names its dimensions, a weighted choice among the ready ones. A decision becomes an OMEN (announced, then a countdown 10/5/3/2/1 s on the action bar), then the RISE at a dry surface place 28-44 blocks from the player in a loaded chunk, with a visual-only lightning bolt. Saved: last wake times, living bosses, pending omens (a restart resumes the omen). A calamity that nobody (spectators excluded) is within 128 blocks of for 10 minutes leaves (`Vanish`), and one whose chunk is unloaded and that was not seen for an hour is dropped from the list of living calamities (it rejoins the list when it loads back), so a calamity that was fled from can never block the director for ever.

**Shipped calamities** (the mod's own numbers; all in the JSON, a datapack can retune or add):

| id | entity | triggers | cooldown | warning |
|---|---|---|---|---|
| `gravebound_colossus` | `nusmp:gravebound_colossus` | density >= 0.92 for 25 min; 3 Mother Rocks with density >= 0.75 for 5 min; ritual; command | 180 min | 20 s |
| `lich_calamity` | `nusmp:lich_boss` | a new-moon night (moon phase 4) with density >= 0.8 and a Mother Rock, 2% per check; density >= 0.95 for 40 min; ritual; command | 240 min | 25 s |

**Phases**: thresholds `[0.66, 0.33]` (three phases). A blow can never skip a phase (`CalamityPhases.clampDamage` cuts the damage in `getDamageAfterMagicAbsorb`, the last step before the health is lowered, so the health stops ON the line whatever the armor does; a health within 0.01% of a line counts as on it, so floats cannot leave the boss stuck a hair above; `/kill` and the void are not cut, the damage budget spreads them); crossing a line is a 50-tick invulnerable transformation (the `transform` clip, a Rukh burst, the phase's opening). Each phase tightens the boss: cooldown x1.0 / 0.78 / 0.55, damage x1.0 / 1.3 / 1.6, speed x1.0 / 1.06 / 1.12, helper cap 2 / 5 / 8. No phase heals; nothing restores health.

**Fight structure**: a move is a telegraph (the boss stands still and draws the SHAPE of the blow on the ground with Rukh for the whole wind-up), the blow, a recovery. Players are never held: damage, knockback, and a short Slowness II (the Lich's cursed ring).

**GraveboundColossus** (2.4 x 5.4, 2400 health, attack 14, armor 10, speed 0.24; Light Rukh, GeoLite model):

| Move | Phase | Wind-up | Cooldown | Range | Blow |
|---|---|---|---|---|---|
| swing | 1 | 22 | 40 | 0-7.5 | 110 degree cone, 1.0 x attack |
| slam | 1 | 34 | 140 | 0-10 | ring r 7, 1.3 x; spikes at r 4 and r 8 |
| quake_line | 1 | 30 | 170 | 4-26 | 12 Gaia spikes in a line, 0.9 x each |
| raise_dead | 2 | 50 | 560 | 0-45 | summons skeletons/zombies up to the phase's cap |
| charge | 2 | 28 | 230 | 8-30 | 14 ticks dash along the aim, 1.6 x to each caught |
| grave_rend | 3 | 42 | 210 | 0-13 | 110 degree cone, 2.2 x, spikes along it |
| collapse | 3 | 74 | 640 | 0-34 | three waves breaking the arena floor, 2.4 x to everyone ON THE GROUND within 14 (jump or fly) |

Phase 2 and 3 openings call the dead (3 and 5 helpers within the cap).

**LichBoss** (0.9 x 2.5, 1800 health, attack 10, armor 4, speed 0.26; Dark Rukh):

| Move | Phase | Wind-up | Cooldown | Range | Blow |
|---|---|---|---|---|---|
| bolt | 1 | 18 | 36 | 3-30 | one void bolt, 0.9 x |
| volley | 1 | 26 | 130 | 4-30 | fan of 3 / 5 / 7 bolts by phase, 0.8 x each |
| curse_ring | 1 | 36 | 180 | 0-24 | ring r 3.4 where you stood, 1.2 x and Slowness II 2.5 s |
| blink | 1 | 12 | 170 | 0-5.5 | teleports 9+ blocks from you (defensive) |
| raise_skeletons | 1 | 46 | 520 | 0-45 | skeletons/strays up to the cap |
| void_rain | 2 | 44 | 260 | 0-36 | 7 bolts fall from the sky on marked spots, 0.9 x each |
| death_nova | 3 | 58 | 440 | 0-30 | wave r 12, 2.6 x, STOPPED BY COVER (a block between the Lich's eyes and yours) |

Phase 2 raises 3 phylacteries (100 health each, times the multipliers) and shields the Lich (a blow does 3%); each broken one is announced; the last one shatters the shield, the Lich staggers 80 ticks and takes +30%. Phase 3 raises 2 more (70 health each) and shields again. A shield with no phylactery left (lost, unloaded) breaks by itself after 3 s. Phylacteries are linked to the Lich by a Dark Rukh stream, drop nothing, and vanish 5 s after their Lich is gone.

**GaiaSpikeEntity**: a ground hazard: for `delay` ticks the spot glows with Light Rukh (the warning), then it bursts, throws up and hits whoever stands there once, stays 24 ticks, and removes itself. Not saved. A `CalamityBoss` owner scales the damage (phase, multipliers, hit cap, records); any other owner is limited by `BalanceLaw.cap`. `GaiaSpikeEntity.raise(level, pos, owner, delay, damage)`.

**VoidBoltProjectile**: straight flight, no gravity, a Dark Rukh trail, pierce N targets, ends on a block or after 90 ticks, never hurts its owner/pets/the calamity family. `VoidBoltProjectile.launch(level, owner, from, dir, speed, damage, pierce)` is the entry for bosses AND for the Void skills (a skill passes the damage it already scaled with `BalanceLaw.damage`; the projectile applies `BalanceLaw.cap`). No Void skill was changed.

**Calamity Core** (`nusmp:calamity_core`, custom data `calamity=<id>`): dropped by the boss with 10-16 Magicule Shards and 1-2 Tensura high-quality crystals; the ritual item above. `/multiverse elite calamity core <id>` gives one.

**Announcements** (`elite/api/CalamityEvents`, game bus, server thread, none cancellable): `Omen`, `Rise`, `PhaseChange`, `Defeat`, `Vanish`, all extending `CalamityEvents.Base` (`calamityId()`, `displayName()`, `level()`, `pos()`, `text()` = the English line of the data file, for voice). Chat uses `nusmp.elite.calamity.<id>.<omen|spawn|phase|defeat>` with the file's English as fallback; `broadcast` (true) sends them to everyone.

**Reports to the records** (built in parallel; NOT imported here; the events live in `elite/api` and are fired on the game bus):

| Event | Fired | Fields |
|---|---|---|
| `CalamityDamageEvent` | after every hit between a player and a calamity (both directions) | `calamityId()`, `boss()` (and `bossId()`), `player()` (attacker when `toBoss()`, victim otherwise), `amount()` (health really lost, > 0, after mitigation, the phase guard and the damage budget), `toBoss()`, `phase()` (0-based), `fightTicks()`; damage by a player's tamed/projectile source is credited to the owner |
| `CalamityKillEvent` | once, when a calamity dies | `calamityId()`, `boss()`, `killer()` (null if not a player), `participants()` (UUID to damage dealt), `onlinePlayers()`, `fightTicks()`, `phaseCount()`, `noDamageTaken()` (flawless: no player lost health to the boss itself), `soloist()` |

## 7. Models, textures, items (how they are drawn)

The five entities are **GeoLite** models (`client/geo`, built by `tools/elite/gen_world_models.py` with `tools/geo_builder.py` and the painting kit `tools/elite/world_art.py`), not arc-kit characters: the arc kit paints named humanoid characters (face, hair, outfit, IK arms); a golem of grave soil, a robed skeleton with orbiting crystals, a vessel, a spike and a bolt are not that. One renderer (`client/elite/world/GeoWorldRenderer`) draws all of them from a small `Look` (clip, clock, lift, glow, shell). Bosses have the clips `idle walk windup cast strike transform death`, a glow layer (eyes, runes, core), the Lich's shield is an additive shell. Previews: `python3 -B tools/render_preview/preview.py elite_lich_cast` (scenes in `tools/render_preview/scenes.d/elite_*.json`, written by the generator). Block textures and the JSON models are written by `tools/elite/gen_world_blocks.py`: the four buds share one shard routine so they read as one crystal growing, the Mother Rock has four charge textures with brighter veins. The gauge and the core are real 3D element models (vanilla JSON cubes with a 16-patch palette texture and hand/GUI transforms). `WeaponRenderer` was not touched (its list is in an existing file and these are not weapons; no weapon was added, so rule 6 has nothing to do here).

## 8. Config `nusmp-elite-world-server.toml`

`enabled` (true, master switch); `[magicule]` `dimensions` (["minecraft:overworld"]), `radiusChunks` 3, `updateTicks` 100, `syncTicks` 40, `risePerMinute` 0.02, `fallPerMinute` 0.06, `eventDecayPerMinute` 0.01; `[crystals]` `growth` true, `growthChance` 0.25, `seedChance` 0.15, `motherChance` 0.04, `motherRocksPerChunk` 2, `seedRadius` 4; `[oreConversion]` `enabled` true, `densityThreshold` 0.70, `intervalTicks` 2400, `perInterval` 1, `perChunkLifetime` 24, `[mobReplacement]` `enabled` true, `densityThreshold` 0.60, `chance` 0.06, `maxChance` 0.25, `glowMarker` true; `[calamities]` `enabled` true, `autoSpawn` true, `ritual` true, `broadcast` true, `checkTicks` 100, `maxActive` 1, `minDistance` 28, `maxDistance` 44, `healthMultiplier` 1.0, `damageMultiplier` 1.0. With `enabled = false` nothing here acts (blocks and entities stay registered so a world that has them loads).

## 9. Protection

`WorldGuard.allowed(level)` is the one gate: switch on, the dimension listed, and not protected. Protected = `DungeonGuard.on(level)`, every Labyrinth dimension (the Mega-Dungeon, the Spire), the Sanctuary (the Hub), the removed Living Dungeon, the Zogratis and story arenas. Crystal growth, ore conversion, touched mobs, the director and the density all stop there, whatever the config lists.

## 10. Tests

* Pure JUnit (`src/test/java/com/newuniverse/nusmp/elite/world`): `MagiculeModelTest` (9), `CrystalGrowthTest` (9), `OreConversionTest` (9: map, picking, budget, mob chance, tiers, table), `CalamityRulesTest` (15: data, director triggers/cooldown/busy/dimension/night/weights, phases, no skipped phase, tuning, scheduler), `WorldAssetsTest` (5: blockstates, models, textures, loot, geo models and clips, lang keys of the code, shipped calamities). 47 pass; mutation checks (removing the phase clamp, the density threshold, the lifetime cap, the fall rate, the busy rule) each failed the matching test.
* In-game self-tests (registered in `labyrinth.mega.SelfTests`, run by `/multiverse mega selftest elite.world.<name>` and by the CI smoke): `density`, `crystal` (small -> medium -> large -> cluster -> Mother Rock -> a seeded bud, no growth at density 0.05, scores, the rock list, the thinning on break), `conversion` (iron ore -> `tensura:magic_ore`, the budget, the threshold), `touch` (tier 2 stats, the refused protected levels), `calamity` (the shipped files, entities exist, the director wakes each on its first trigger), `bosses` (Colossus 3 phases, no skipped phase, invulnerable transform, helpers, damage/kill/announce events; Lich shield with 3 phylacteries and its break), `entities` (spike, bolt, omen -> rise -> listed alive). They skip (pass with the reason) when the engine is switched off.

## 11. Not verified (be honest)

Nothing ran in the game. Type-checked only: every Minecraft/NeoForge class (blocks, entities, events, networking, the HUD layer, commands, attachments, saved data). Assumptions to watch on the first real run: that `onPlace`/`onRemove` behave as documented for same-block state changes (guarded with `is(this)`); that a chunk data attachment mutated in place is saved after `setUnsaved(true)`; that the optional payload registrar of the world engine coexists with the other registrars of the mod (the mod already registers several the same way); the preview renderer is a re-implementation of `GeoDraw` (the JSON element models of the two hand items were not rendered at all); boss balance (health 2400/1800, damage multipliers, cooldowns) is designed on paper against the existing damage budget (6% of max health per second) and `pvpHitCapPercent`, not played; the self-test `bosses` drives bosses by hand-ticking them (`tickCount` is raised by the test because the level does that before `tick()`).

## 12. Files and edits

New: `elite/world/**` (registry, config, data, api, guard, engine, sync, ore converter, mob handler, blocks, items, entities, calamity), `elite/api/{CalamityEvents,CalamityDamageEvent,CalamityKillEvent}.java`, `client/elite/world/**`, `tools/elite/{world_art,gen_world_models,gen_world_blocks}.py`, `tools/elite/world_lang.json`, `tools/render_preview/scenes.d/elite_*.json`, assets (blockstates, models, textures, geo, animations), data (`elite/world/*.json`, `loot_table/blocks`, tags, recipe), tests, this doc.
Edits to existing files: (1) `elite/EliteSystem.java`: one line in the world section, `com.newuniverse.nusmp.elite.world.WorldRegistry.init(modBus, container);`. (2) `data/minecraft/tags/block/mineable/pickaxe.json`: five ids appended. Nothing else (no version, CHANGELOG, README, LIBRARIES, gradle, mods.toml or en_us.json edit; the `EliteHooks.chunkMagicule` field is assigned from `WorldRegistry`, its declaration is untouched).

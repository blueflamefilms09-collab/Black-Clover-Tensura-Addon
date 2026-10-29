# Performance pass 1.99.0: rendering, event handlers and continuous mechanics

Owner request: "adding high-quality models and complex mechanics can cause lag. Review and optimise all the code: rendering logic, event handlers and continuous ticking mechanics must run as
efficiently as possible without frame drops or server lag." The rule for this pass: **behaviour must not change** (same results, same visuals, same saves), only the cost goes down; **add the
new, keep the old**; nothing was removed.

Nothing can be run in this environment (no game, no profiler). Every figure below is therefore a **static cost model** (what an expression allocates and how often it runs, from the source and
from the real asset statistics), not a measurement. The pass ships the tool that turns the models into measurements on the owner's machine: the probe (`/multiverse perf`, `/nusmp perf`).

## 1. The short version

* The server side was already throttled almost everywhere (a modulo or a stagger by entity id in the first line, a dimension test before any scan, dirty-flag syncs). 381 event handlers, 248
  `tick()`/`aiStep()` methods and 239 client `render()` methods were read by rule and the unusual ones by hand (section 6). **No server tick handler needed a rewrite**; the gains on the server are
  small and are listed honestly. What the server got is safety nets (budgets) and a probe.
* The real cost was on the **client, in the draw routines every model and effect goes through**: `GeoDraw` (the draw routine of every animated model: auras, props, Arc / Zogratis / Foundry /
  Abyssal bodies, player-model extras) allocated about **1.2 KB per cube per pass** on every frame; `VfxVertexBuffer` allocated a key plus up to eight vectors **per quad**; the spec-driven `FxRenderer`
  built a location per piece per frame and up to four vectors per vertex; the Foundry and Abyssal renderers built several strings and locations **per mob per frame**. All of that is now
  allocation free or cached, with the same floats reaching the GPU (proved bit for bit, section 5).
* **Levels of detail and budgets** where there were none: distance level of detail, culling and a vertex budget for the VFX layers and FX pieces (`vfxQuality`), a distance rule for player auras, a
  per-tick budget for effect packets and for the continuous particle trails. At the default settings nothing near the camera changes.
* **Config** `config/nusmp-performance-common.toml` and `config/nusmp-performance-client.toml`; **probe** with 40 wrapped handlers, 6 event-wide sections and one section per VFX layer.

## 2. NEW / REPLACED / KEPT

| | What |
|---|---|
| **NEW** | package `perf`: `PerfBudget`, `PerfBudgets`, `PerfProbe`, `RollingStat`, `PerfMath`, `DiffSend`, `BoundedCache`, `RlCache`, `PerfEpoch`, `VfxQuality`, `PerfSettings`, `PerfConfig`, `PerfBoot`, `PerfCommands`, `PerfEvents`, `PerfParticles`, `PerfSelfTests` |
| **NEW** | `client/perf/PerfClient` (client config, `/nusmp perf`, client probe sections), `vfx/client/VfxStats` |
| **NEW** | config files `nusmp-performance-common.toml` and `nusmp-performance-client.toml`; commands `/multiverse perf on\|off\|report\|reset\|settings` and `/nusmp perf on\|off\|report\|reset\|settings` |
| **NEW** | self-test `perf.core`; JUnit `PerfMathTest`, `PerfBudgetTest`, `DiffSendTest`, `PerfProbeTest`, `BoundedCacheTest`, `VfxQualityTest`, `PerfSourceScanTest`, `GeoDrawCacheTest` |
| **NEW** | `tools/perf/inventory.py` (regenerates the tables of section 7), `tools/lang/performance.json` |
| **REPLACED** (same result, cheaper) | the per-quad key and vector chains of `VfxVertexBuffer`; the per-cube arrays, per-bone quaternion and per-track arrays of `GeoDraw` / `GeoAnim`; the per-frame strings and locations of `FoundryRenderer`, `AbyssalRenderer`, `FxRenderer`, `AuraLayer`, `GrimoireFloatClient`, `RelicTextures.armorGlow`, `AuraRender.tex`, `ThreadTrapRenderer`, `ModeArmorLayer`, `CuriosLink.id`; the two vectors per vertex of `WeaponRenderer.emit` |
| **ADDED to** (a new rule next to the old behaviour, default = old behaviour near the camera) | `VfxManager.render` (level of detail, cull, frame budget), `FxRenderer.render` (cull), `PlayerAuraLayer` (aura LOD), `VfxSpawn.send` (effect packets per tick), 5 trail emitters (`PerfParticles`), `Relics` look lifetime and the Elsdocia look period (now config keys with the old numbers as defaults) |
| **KEPT** | every feature, effect, registered id, enum constant, saved data and packet; the Time Magic VFX (`TimeMagicLayer`, its textures and generator, the Time calls of `TimeBook`: not edited, and exempt from the level of detail and the culling); every public method signature (the probe wrappers keep the old name, signature and annotations; the original body is the same method under the name `<old>0`) |

## 3. How to see which part is slow

| Command | Who | What |
|---|---|---|
| `/multiverse perf on` / `off` | operator (level 2) | switches the probe (starts clean when switched on) |
| `/multiverse perf report` | operator | the ten most expensive **server** sections: calls, average, "now" (last 100 calls), maximum, ms of work per second; then the budgets (asked / granted / dropped / busiest tick) |
| `/multiverse perf reset`, `settings` | operator | clears the numbers; prints the active settings |
| `/nusmp perf on` / `off` / `report` / `reset` / `settings` | any player (client command) | the same for the **client** sections, plus what the effect system did last frame (`VfxStats`: running, drawn, thinned, culled, over budget, vertices) |

Or set `probe.enabled = true` in the config. The probe is **off by default**; when off a wrapped handler costs one read of a static volatile flag (`PerfProbe.begin()` answers 0, `end` returns on 0).

Section names: `s.` = server thread, `c.` = client thread.

| Wide sections (every mod's listeners on the event, `HIGHEST` to `LOWEST`) | |
|---|---|
| `s.event.server_tick`, `s.event.player_tick`, `s.event.level_tick`, `s.event.incoming_damage` | server |
| `c.event.client_tick`, `c.event.render_after_particles` | client (the render stage where every VFX layer, FX piece, zone and floating grimoire draws) |

Narrow sections (the handler or renderer itself): server `s.curios.passives`, `s.engravings.weapon`, `s.engravings.armor`, `s.elsdocia.scan`, `s.mana.heritage`, `s.mana.law`, `s.mana.zone`,
`s.djinn.engine`, `s.relic.worn_sync`, `s.relic.worn_sync_mob`, `s.relic.holder_sync`, `s.relic.mob_pack`, `s.relic.scan` (the Curios-reading relic look), `s.book.grimoire`, `s.book.grimoire_guard`,
`s.book.grimoire_slot`, `s.book.paint_studio`, `s.book.attribute_player`, `s.book.spell_runtime`, `s.book.dream_world`, `s.multiverse.sync`, `s.discovery.tracker`, `s.song.ticker`,
`s.mega.system_player`, `s.mega.puzzle`, `s.mega.mechanics`, `s.mega.floor_tracker`, `s.mega.floor_info`, `s.mega.system_sync`, `s.mega.gen`, `s.boss.bar_sync`, `s.create.traps`, `s.time_stop.server`,
`s.antimagic.nihility`, `s.wizard.library`, `s.corpse.tick`, `s.avatar.ai_entity`, `s.vfx.send`; client `c.vfx.tick`, `c.vfx.spawn`, `c.vfx.render`, `c.vfx.layer.<LayerClass>` (one per VFX layer, while
the probe is on), `c.fx.render`, `c.aura.layer`, `c.relic.worn_layer`, `c.relic.humanoid_layer`, `c.relic.scan`, `c.djinn.armor_layer`, `c.mode.armor_layer`, `c.foundry.render`.

## 4. Config

`config/nusmp-performance-common.toml` (both sides) and `config/nusmp-performance-client.toml` (this machine). The defaults reproduce the pre-1.99 behaviour; every guard says what 0 means.

| Key | Default | Meaning |
|---|---|---|
| `vfx.vfxQuality` (common) | `high` | `high`: the look the mod was designed with (64 effects at once, full detail to 96 blocks, 3/4 to 176, 1/2 to 256, none beyond; the vertex budget of a frame never binds). `medium`: 40 effects, detail 3/4 and from 64 blocks lower, 14000 vertices per frame, drawn to 160 blocks. `low`: 24 effects, half detail, 6000 vertices per frame, drawn to 96 blocks |
| `vfx.vfxQuality` (client) | `auto` | `auto` follows the common file; `low` / `medium` / `high` overrides it on this machine |
| `aura.auraLodNear` / `auraLodFar` / `auraMidCount` | 48 / 96 / 2 | every running aura of a player is painted to `auraLodNear` blocks; up to `auraLodFar` only the `auraMidCount` most recent; beyond none |
| `budgets.maxParticlesPerTick` | 2000 | server particles the budgeted continuous emitters (`PerfParticles`) may send per tick; 0 = unlimited |
| `budgets.maxEffectPacketsPerTick` | 160 | effect (VFX) packets of `VfxSpawn.send` per tick; the rest of that tick is dropped (the effect is not shown); 0 = unlimited |
| `budgets.clientParticlesPerTick` (client) | 400 | the client's allowance for budgeted ambient emitters (`PerfBudgets.clientParticles`) |
| `scans.entityScanInterval` | 5 | period (ticks) of the non-urgent scans that follow the config (today: Elsdocia's inventory look; the default is its old period) |
| `scans.relicScanInterval` | 10 | ticks a look at what a wearer carries stays good (`Relics.ttl()`; it was the constant `Relics.TTL`) |
| `scans.packetRefreshTicks` | 200 | ticks after which an unchanged mirrored state may be sent again anyway (`DiffSend`); 0 = never repeat |
| `probe.enabled` (both) | `false` | switch the probe on at start |

## 5. What was changed, before / after, with the cost model

Allocation sizes below are HotSpot 64-bit with compressed references: `float[3]` 32 B, `float[2]` 24 B, `float[n][]` header 16 B + 4 B per reference (rounded to 8), `Vector3f` 24 B,
`Quaternionf` 32 B, a two-reference record 24 B. CPU figures assume 10 to 20 ns for a short-lived allocation (bump, header, zeroing, later young collection) and are an **upper bound**: escape
analysis removes some of these when everything inlines; it cannot remove one that is stored in a map or passed to a large method (the per-quad key; the corner tables of a cube).
Asset facts (from `src/main/resources`): 1908 geo models; cubes per model mean 34, median 17, p90 85, p99 150, max 368; Foundry models (400) mean 61 cubes, max 126; Abyssal bosses (66) mean 120, max 340;
Arc characters (124) mean 68; one VFX activation is capped at 400 vertices (100 quads), 64 activations at once.

| # | Item (files) | Before | After | Estimated saving |
|---|---|---|---|---|
| P1 | **VfxVertexBuffer** (`vfx/client/VfxVertexBuffer.java`) | per quad: `new Key(texture, blend)` (24 B, hashed, compared) into a `LinkedHashMap.computeIfAbsent`; the shapes built 4 to 8 `Vector3f` per quad (`billboard` 8, `ring`/`arc`/`plane` 4, `beam` 8): **120 to 216 B per quad**; the batch arrays were rebuilt every frame | batch found by an identity shortcut (same texture and blend as the last quad) or a texture table with one slot per blend; shapes in plain floats with the same operation order; batches and arrays kept between frames; budget tested before the arithmetic | worst case 6400 quads per frame: 0.77 to 1.4 MB per frame (46 to 83 MB/s at 60 fps) of garbage gone; busy scene (600 quads) 72 to 130 KB per frame; CPU about 60 to 100 ns per quad (hash, equals, allocation) = 0.4 to 0.6 ms per frame at the worst case |
| P2 | **VfxManager / quality** (`VfxManager`, `VfxRenderContext`, `perf/VfxQuality`) | every running effect drawn at full detail at any distance; no frame budget; cap 64 | per effect: detail by distance (1, 3/4, 1/2, culled), the particle setting times that; frame budget (nearest first, drawing order unchanged); cap follows the quality; Time Magic exempt | `high`: nothing near changes, effects farther than 96 blocks get 3/4 or 1/2 of the segments, beyond 256 are skipped; `medium`: about 25% fewer vertices overall and 40 effects; `low`: about 50% fewer, 24 effects, 6000 vertices per frame. Layer cost scales with segments, so a crowd scene with half its effects past 96 blocks saves about a quarter of the VFX CPU on `high` |
| P3 | **FxRenderer** (`vfx/client/FxRenderer.java`) | per piece per frame: `"textures/fx/" + name + ".png"` and a location (about 150 B); per sprite vertex one `Vector3f` (24 B), per ribbon vertex four (96 B) | texture location cached per name; scratch vector per piece; effects whose reach is entirely past the far limit are skipped; cap follows the quality | a 400-vertex effect: 10 KB (sprites) to 38 KB (ribbons) per frame; 48 effects: up to 1.8 MB per frame |
| P4 | **GeoDraw / GeoAnim / GeoModelData** (`client/geo/*`) | per cube per pass: 45 objects, **1,264 B** (`float[8][]`, 8 corner arrays, 6 face corner lists, 6 uv tables with 4 pairs each); per turned bone one `Quaternionf` (32 B); per animated track one `float[3]` (32 B) | corners and normalised uv tables computed once per cube (`Cube#prepareDraw`); one scratch quaternion; tracks sampled into three scratch arrays (`Track#sampleInto`) | per frame: a 61-cube Foundry mob 77 KB per pass (about 154 KB with the glow pass); an Abyssal boss (120 cubes) 300 KB; 20 Foundry mobs in view about 3 MB per frame (185 MB/s at 60 fps). CPU: 0.45 to 0.9 us per cube, so about 1 to 2 ms per frame for 20 Foundry mobs; this is the largest single item |
| P5 | **FoundryRenderer** (`client/labyrinth/foundry/`) | per mob per frame: `id + "|" + anim`, `animations().toString()`, `model().toString() + animations()`, and in the entity renderer `GeoSpec.of(...)` for the texture (four strings and four locations): **about 900 B, 0.7 us** | one `Kit` per mob id (spec, clip reader, stride) rebuilt only when the index changes the animation class; texture cached per id | 100 visible mobs: 90 KB and 70 us per frame |
| P6 | **AbyssalRenderer** (`client/remodel/`) | per Guardian per frame: `spec(look, phase)` twice (8 concatenations and 8 locations, about 1.2 KB) plus a 3-string cache key | spec cached per look and phase (looks are unique objects), clip reader keyed by the animation file | 1.2 KB and about 1 us per Guardian per frame |
| P7 | **Aura sprites and layer** (`AuraRender.tex`, `AuraLayer.card`, `PlayerAuraLayer`) | `AuraRender.tex(path)` built a string and a location per call; `PlantAura`, `PoisonPlantAura`, `VineAura`, `MucusAura`, `TreeAuraFx` call it 3 times per paint, per player, per frame; the VFX spell card built an icon and a badge location per frame; every aura of every player was painted at any distance | `tex` answered from `RlCache`; card icons / badges cached per (magic, variant); aura LOD: all auras to 48 blocks, the 2 most recent to 96, none beyond | 450 B and 0.45 us per such aura per frame; a distant crowd skips its painters (vanilla stops drawing players near 64 blocks, so 48 to 96 is the only band that changes) |
| P8 | **Relic worn layer, mode layer** (`RelicWornLayer.Boxes`, `RelicTextures.armorGlow`, `ModeArmorLayer`) | per box quad two arrays (`float[12]`, `float[8]`, 112 B, 6 quads per box, boxes per Curios relic); armour glow location built per wearer per frame; library key lower-cased and concatenated per frame | unrolled vertices; glow location cached; key cached per mode | 670 B per box per frame; 200 B per armour relic wearer per frame |
| P9 | **GrimoireFloatClient.runeTexture** | per floating grimoire per frame: lower-case, 2 concatenations, a location and a **resource-manager question** (walks the packs that hold the `nusmp` namespace) | answer kept per magic, thrown away on a resource reload (`PerfEpoch`, bumped by the existing `VfxDefinitions` reload listener) | one pack lookup (microseconds) and about 250 B per book per frame |
| P10 | **Small client items** (`ThreadTrapRenderer`, `GuiScale.onLayerPost`, `RelicLayerGuard.onPre`) | a location per trap per frame; a set removal per HUD layer per frame; a lock plus a weak-map lookup per player per frame | constant; early exit on the empty set; last-renderer shortcut | below 1 us each; removes the last per-frame allocations of those paths |
| P11 | **Server small items** (`CuriosLink.id`, `Relics.ttl`, `ElsdociaEvents`, `TimeStop.onEntityTick`, `MobRelics.onTick`) | 28 strings and locations per player per second (5.6 KB, 7 us); `FROZEN.containsKey` and a config read for every entity of every level every tick | ids cached; empty-map exit; modulo before the config read; the two look periods are config keys with the old numbers | about 7 us per player per second; about 10 ns per entity per tick (2000 entities: 0.4 ms per second). Honest: small |
| P12 | **Budgets** (`VfxSpawn.send`, `PerfParticles` in 5 trail emitters: `MorrisMoves`, `LuciusFx`, `VanicaMoves`, `RestEvents`, `ThemeAmbient`) | unlimited: a boss that loops a spell over a crowd can send hundreds of effect packets in one tick to everyone within 96 blocks (400 sends x 10 players = 4000 packets, about 240 KB, in one tick) | 160 effect packets and 2000 trail particles per tick (config); the rest of that tick dropped and counted | a safety net: bounds the worst tick to 1600 packets; no change in a normal tick (the busiest realistic tick sends tens) |
| P13 | **Probe wrappers and wide sections** | no way to see the cost | 40 handlers / renderers wrapped (`<old>` calls `<old>0` between `PerfProbe.begin` / `end`), 6 wide sections, per-layer VFX sections | off: a flag read per wrapped call and eight small listener calls per tick; on: about 40 ns per section call |
| P14 | **WeaponRenderer** (`client/WeaponRenderer.java`, `emit` and the void pass) | per weapon vertex per pass two `new Vector3f()` (48 B); a body of 800 vertices plus glow, emissive and void passes allocated 60 to 100 KB per frame per held weapon | two static scratch vectors (render thread); the same `transformPosition` and `transform(...).normalize()` calls in the same order | up to about 100 KB and 3,000 short-lived objects per frame per visible weapon; output identical |

The arithmetic of P1, P3 and P4 is **bit for bit** the old arithmetic: scratch harnesses (not committed: they need JOML) compared the old vector chains with the new float code over 100,000 random
inputs for the five shapes, and the old per-cube formulas with the new tables over 100,000 random cubes (`GeoDrawCacheTest` repeats the cube comparison over every cube of every geo file of the mod, and
the track sampling over every track of every animation file).

## 6. Schedulers and continuous mechanics, by hand

These are the long-running systems the brief named. "Dimension test first" means the first line returns when no player is in the system's dimension.

| System | Frequency | Per-call work | Scales with | Verdict |
|---|---|---|---|---|
| Puzzle Director (`labyrinth/mega/puzzle`) | server tick; scan every 10 ticks (`now % 10 == 3`) | dimension test; scan per dungeon player; per active puzzle: players in area, `update` | players in the dungeon, active puzzles | OK, probe `s.mega.puzzle` |
| Mechanics Director | server tick; scan every 10 ticks | same shape as the Puzzle Director | same | OK, probe `s.mega.mechanics` |
| Dungeon Master (`enc/*`) | server tick; sweep every 100 ticks, hunt every 20, 400 tracked at most, 40 hunts per pass | other builder's lane | | not touched (section 9) |
| System `FloorInfo` | once a second (`% 20 == 7`) per dungeon player | signature set compared before any message; message throttled | dungeon players | OK, probe `s.mega.floor_info` |
| `FloorTracker` | every `trackEveryTicks` (config, 10), hard-lock guard every 20 | dungeon dimension test, then a pass over dungeon players | dungeon players | OK, probe `s.mega.floor_tracker` |
| `SystemSync` / `HubSync` | dirty flags; at most one send per 10 ticks per player | send only when dirty | dirty players | OK (this is the pattern `DiffSend` generalises) |
| Create trap runtime (`compat/create`) | every 2 ticks (10: refresh of near traps, 40: arm one) | inert without Create; live traps near players only | live traps | OK, probe `s.create.traps` |
| `ThemeAmbient`, `HubRules`, `PartyGuard`, `StairTravel`, `ExpansionTrigger`, `FlowAir` | every 40, 100, 20, 4, 20, 40 ticks | dimension test first | players of the dimension | OK; the ambient puff is now budgeted |
| Elsdocia scan | every 5 ticks per player (now `entityScanInterval`) | 41 slots, one `instanceof` each | players | OK, probe `s.elsdocia.scan` |
| Relic scans (`Relics.locatedList`, `CuriosLink`) | cached per entity for 10 ticks (now `relicScanInterval`); Curios passives once a second per player | reflection through cached `Method` handles; lists | wearers in view (client), players (server) | OK; ids cached; probes `s.relic.scan`, `c.relic.scan`, `s.curios.passives` |
| `PlayerAuras` sync | event driven (`set` / `clear` / start tracking) | one payload per change | auras started | OK, nothing continuous |
| Boss controllers (`BossBarSync`, `BossBuffEngine`, `BossRulesHooks`) | server tick, return at once with no bar / boss | style sent once per watcher | active bars | OK, probe `s.boss.bar_sync` |
| `ManaLaw` | once a second per player | zone test, corruption | players | OK, probe `s.mana.law`; `ManaZoneManager` `s.mana.zone` |
| Avatar AI (`AvatarAiEvents`) | entity tick, `instanceof AvatarEntity` first | the handler for avatars only | avatars | OK, probe `s.avatar.ai_entity` |
| Corpses (`CorpseHooks`) | server tick; `Corpses.tick`, `Pacts.tick` over live corpses / pacts | loops over the live ones | corpses | OK, probe `s.corpse.tick` |
| Safe rooms (`RestEvents`) | every 10 ticks per resting player (`CHECK_INTERVAL`), state test first | channel check, one hostile scan, sparkle (now budgeted) | resting players | OK |
| Entity handlers on `EntityTickEvent` (`TimeStop`, `MobRelics`, `WornSync`, `AvatarAiEvents`, `RelicGradeFx`) | per entity per tick | see section 7 | all entities | made cheaper (P11) |

## 7. Inventory (generated)

Four tables: A event handlers of the hot events, B `tick()` / `aiStep()` of entities, block entities, goals and schedulers, C client `render()` methods, D the VFX layers.
"Frequency" is read from the throttle in the first lines of the body (a modulo, a mask), else the default of the event; "Work per call" are the kinds of call found in the body; "Verdict":
**FIXED** = changed by this pass, **OK** = read by rule or by hand (the note says why), **WATCH** = nobody has read it yet (there should be none). `[probe ...]` = wrapped in that probe section.
Regenerate with `python3 tools/perf/inventory.py --write`.

<!-- inventory:begin -->
<!-- generated by tools/perf/inventory.py; do not edit by hand: change FIXED / REVIEWED in the script and run it again -->

#### A. Event handlers of the hot events (381 rows, 0 marked WATCH)

| Handler | Event | Frequency | Work per call | Scales with | Verdict |
|---|---|---|---|---|---|
| `antimagic/AntiMagic.java#migrate` | PlayerTickEvent.Post | every 100 ticks (staggered by id) | strings, skills | per player | OK: throttled |
| `blackclover/GrimoireAcceptance.java#onPlayerTick` | PlayerTickEvent.Post | every 100 ticks | trivial | per player | OK: throttled |
| `blackclover/GrimoireGuard.java#onPlayerTick` | PlayerTickEvent.Post | every 100 ticks | skills, loop | per player | OK: throttled [probe s.book.grimoire_guard] |
| `blackclover/GrimoireSlot.java#onPlayerTick` | PlayerTickEvent.Post | every tick | inventory, packets, loop | per player | OK: slot filled: one stack compare per tick (the change detector of the hip sync); slot empty: one pass over 37 slots with an instanceof each [probe s.book.grimoire_slot] |
| `book/GrimoireBook.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks (staggered by id) | skills, loop | per player | OK: throttled [probe s.book.grimoire] |
| `book/GrimoireSummon.java#onPlayerTick` | PlayerTickEvent.Post | every 60 ticks | trivial | per player | OK: throttled |
| `book/MirrorWorks.java#onPlayerTick` | PlayerTickEvent.Post | every tick | loop | per player | OK: light |
| `book/PaintStudio.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | entity scan, inventory, effects, loop | per player | OK: throttled [probe s.book.paint_studio] |
| `book/SpiritBond.java#onPlayerTick` | PlayerTickEvent.Post | every 200 ticks | trivial | per player | OK: throttled |
| `book/TimeBook.java#passiveTick` | PlayerTickEvent.Post | every tick | skills | per player | OK: one skill-storage lookup per player per tick; Time Magic is left as it is (owner rule 3) |
| `book/ext/AttributeEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates [probe s.book.attribute_player] |
| `butoh/ButohEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: ButohDance.tick returns after two map reads unless a dance session exists |
| `celestial/CelestialEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `charter/CharterEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: CharterMirror.pass returns at once except one tick in twenty per player (staggered by id) |
| `core/discovery/DiscoveryTracker.java#onTick` | PlayerTickEvent.Post | every tick | curios, loop | per player | OK: already staggered per player: inventory scan every 20 ticks, skills and Curios every 40, each on the player id phase [probe s.discovery.tracker] |
| `corpse/CorpseHooks.java#onPlayerTick` | PlayerTickEvent.Post | every 40 ticks | trivial | per player | OK: throttled |
| `dungeon/MealSkills.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `dungeon/nexus/relic/RelicEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 5 ticks | trivial | per player | OK: throttled |
| `elite/hub/QuestEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 200 ticks | trivial | per player | OK: throttled |
| `entity/zogratis/LuciusBossEventHandler.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: light |
| `heroweapon/bond/HeroBondEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 4 ticks | trivial | per player | OK: throttled |
| `heroweapon/gem/GemEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `item/ArmorEngravings.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | inventory, registry, loop | per player | OK: throttled [probe s.engravings.armor] |
| `item/CuriosLink.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | attributes, loop | per player | FIXED: modifier ids cached [probe s.curios.passives] |
| `item/ElsdociaEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | FIXED: period follows entityScanInterval |
| `item/MagicGear.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | effects | per player | OK: throttled |
| `item/WeaponEngravings.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | inventory, registry, loop | per player | OK: throttled [probe s.engravings.weapon] |
| `labyrinth/dnd/economy/DungeonHunger.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | effects | per player | OK: throttled |
| `labyrinth/gate/LabyrinthEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `labyrinth/mega/gen/MegaGenRegistry.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: MegaTravel.rememberLanding returns after a modulo outside the dungeon |
| `labyrinth/mega/safe/RegenThrottle.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `labyrinth/mega/safe/RestEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `labyrinth/mega/system/SystemEvents.java#onPlayerTick` | PlayerTickEvent.Post | every RECONCILE_EVERY ticks | trivial | per player | OK: SystemQuests.tickPlayer: every 5 ticks, dimension test first [probe s.mega.system_player] |
| `mana/Heritage.java#onPlayerTick` | PlayerTickEvent.Post | every 40 ticks | strings, attributes, loop | per player | OK: throttled [probe s.mana.heritage] |
| `mana/ManaLaw.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | effects | per player | OK: throttled [probe s.mana.law] |
| `mana/zone/GrandZones.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `multiverse/Ceremony.java#onPlayerTick` | PlayerTickEvent.Post | every 100 ticks | trivial | per player | OK: throttled |
| `multiverse/MasteryPages.java#onPlayerTick` | PlayerTickEvent.Post | every 40 ticks | trivial | per player | OK: throttled |
| `multiverse/MultiverseSync.java#onPlayerTick` | PlayerTickEvent.Post | every 40 ticks | packets | per player | OK: throttled [probe s.multiverse.sync] |
| `multiverse/RaceBuffs.java#onPlayerTick` | PlayerTickEvent.Post | every 40 ticks | attributes, loop | per player | OK: throttled |
| `multiverse/Squads.java#onPlayerTick` | PlayerTickEvent.Post | every 100 ticks | strings | per player | OK: throttled |
| `multiverse/WizardKing.java#onPlayerTick` | PlayerTickEvent.Post | every 200 ticks (staggered by id) | skills | per player | OK: throttled |
| `perf/PerfEvents.java#(lambda)` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `perf/PerfEvents.java#(lambda)` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `quests/QuestWorldEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | locations | per player | OK: throttled |
| `relics/RelicEngine.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/RelicHolderSync.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | packets | per player | OK: throttled [probe s.relic.holder_sync] |
| `relics/WornSync.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks (staggered by id) | trivial | per player | OK: throttled [probe s.relic.worn_sync] |
| `relics/djinn/DjinnEngine.java#onPlayerTick` | PlayerTickEvent.Post | every 4 ticks | trivial | per player | OK: throttled [probe s.djinn.engine] |
| `relics/djinn/weapon/DjinnWeaponEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 4 ticks | trivial | per player | OK: throttled |
| `relics/order/OrderEvents.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/socket/SocketEngine.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave0Hooks.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | inventory, curios, effects, loop | per player | OK: throttled |
| `relics/wave/Wave1.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks | attributes, effects | per player | OK: throttled |
| `relics/wave/Wave10Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/wave/Wave11Events.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks | effects | per player | OK: throttled |
| `relics/wave/Wave12Hooks.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: light |
| `relics/wave/Wave13Events.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks | trivial | per player | OK: throttled |
| `relics/wave/Wave14Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: light |
| `relics/wave/Wave15Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/wave/Wave16Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | loop | per player | OK: throttled |
| `relics/wave/Wave17Events.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave18Events.java#onPlayerTick` | PlayerTickEvent.Post | every 2 ticks | trivial | per player | OK: throttled |
| `relics/wave/Wave19Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave20Events.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks | effects, loop | per player | OK: throttled |
| `relics/wave/Wave21Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: light |
| `relics/wave/Wave22Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | effects | per player | OK: light |
| `relics/wave/Wave23Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `relics/wave/Wave24Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave25Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `relics/wave/Wave26Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/wave/Wave27Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: cheap gate, delegates |
| `relics/wave/Wave28Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | trivial | per player | OK: light |
| `relics/wave/Wave29Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave2Events.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave30Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | loop | per player | OK: light |
| `relics/wave/Wave3Events.java#onPlayerTick` | PlayerTickEvent.Post | every 4 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave5Hooks.java#onPlayerTick` | PlayerTickEvent.Post | every 5 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave6Events.java#onPlayerTick` | PlayerTickEvent.Post | every 10 ticks (staggered by id) | trivial | per player | OK: throttled |
| `relics/wave/Wave7Events.java#onPlayerTick` | PlayerTickEvent.Post | every 2 ticks (staggered by id) | effects, loop | per player | OK: throttled |
| `relics/wave/Wave8Events.java#onPlayerTick` | PlayerTickEvent.Post | every tick | effects, loop | per player | OK: light |
| `relics/wave/Wave9Events.java#onPlayerTick` | PlayerTickEvent.Post | every 4 ticks | trivial | per player | OK: throttled |
| `skill/SkillEvents.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: throttled |
| `skill/SkillEvolution.java#onPlayerTick` | PlayerTickEvent.Post | every 100 ticks | skills, loop | per player | OK: throttled |
| `song/SongTicker.java#onPlayerTick` | PlayerTickEvent.Post | every 20 ticks | trivial | per player | OK: reads one int when no song mode is on [probe s.song.ticker] |
| `wizard/WizardSystem.java#onPlayerTick` | PlayerTickEvent.Post | every 2400 ticks | trivial | per player | OK: throttled |
| `antimagic/NihilityZone.java#onServerTick` | ServerTickEvent.Post | every tick | entity scan, effects, loop | global (loops players of its dimension) | OK: iterates live zones only (none almost always); one entity scan per zone per pass [probe s.antimagic.nihility] |
| `arc/arena/ArcArenaDirector.java#onServerTick` | ServerTickEvent.Post | every STEP ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `arc/hub/ArcHubNpcs.java#onTick` | ServerTickEvent.Post | every EVERY ticks | player list | global (loops players of its dimension) | OK: throttled |
| `arc/hub/ChronicleBuilder.java#onTick` | ServerTickEvent.Post | every EVERY ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `arc/world/ArcFloorBosses.java#onTick` | ServerTickEvent.Post | every 20 ticks | loop | global (loops players of its dimension) | OK: throttled |
| `arena/ArenaDirector.java#onServerTick` | ServerTickEvent.Post | every STEP ticks | loop | global (loops players of its dimension) | OK: throttled |
| `avatar/AvatarSystem.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `avatar/ritual/SoulForgeSystem.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `blackclover/TimeStop.java#onServerTick` | ServerTickEvent.Post | every tick | loop | global (loops players of its dimension) | OK: light [probe s.time_stop.server] |
| `blackclover/TimedModifiers.java#onServerTick` | ServerTickEvent.Post | every tick | attributes, loop | global (loops players of its dimension) | OK: light |
| `book/DreamWorld.java#onServerTick` | ServerTickEvent.Post | every tick | packets, loop | global (loops players of its dimension) | OK: light [probe s.book.dream_world] |
| `book/ForbiddenMagic.java#onServerTick` | ServerTickEvent.Post | every 20 ticks | player list, loop | global (loops players of its dimension) | OK: throttled |
| `book/SpellRuntime.java#onServerTick` | ServerTickEvent.Post | every tick | blocks, loop | global (loops players of its dimension) | OK: walks only the live bolts and zones (empty almost always) [probe s.book.spell_runtime] |
| `book/TreeRestore.java#onServerTick` | ServerTickEvent.Post | every 20 ticks | loop | global (loops players of its dimension) | OK: throttled |
| `book/UnderworldMatter.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | loop | global (loops players of its dimension) | OK: throttled |
| `book/ext/AttributeEvents.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `boss/BossBuffEngine.java#onServerTick` | ServerTickEvent.Post | every tick | loop | global (loops players of its dimension) | OK: light |
| `boss/bar/BossBarSync.java#onServerTick` | ServerTickEvent.Post | every tick | player list, packets, loop | global (loops players of its dimension) | OK: returns at once with no boss bar; sends only to players not yet told [probe s.boss.bar_sync] |
| `boss/rules/BossRulesHooks.java#onTick` | ServerTickEvent.Post | every SWEEP_TICKS ticks | loop | global (loops players of its dimension) | OK: throttled |
| `celestial/SpiritWorld.java#onTick` | ServerTickEvent.Post | every 20 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `compat/create/CreateTrapDirector.java#onServerTick` | ServerTickEvent.Post | every 2 ticks | player list, strings, loop | global (loops players of its dimension) | OK: inert without Create; every 2 ticks; refresh of near traps every 10 ticks [probe s.create.traps] |
| `corpse/CorpseHooks.java#onTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates [probe s.corpse.tick] |
| `dungeon/nexus/relic/RelicEvents.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `elite/hub/QuestEvents.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `fairytail/FtWorld.java#onServerTick` | ServerTickEvent.Post | every tick | loop | global (loops players of its dimension) | OK: light |
| `heroweapon/bond/HeroBondEvents.java#onServerTick` | ServerTickEvent.Post | every 20 ticks | player list | global (loops players of its dimension) | OK: throttled |
| `heroweapon/curse/CurseEvents.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | player list, loop | global (loops players of its dimension) | OK: throttled |
| `heroweapon/forms/FormsSystem.java#(lambda)` | ServerTickEvent.Post | every RECONCILE_TICKS ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/ExpansionTrigger.java#onServerTick` | ServerTickEvent.Post | every EVERY ticks | player list, loop | global (loops players of its dimension) | OK: every 20 ticks, only while a sector waits |
| `labyrinth/mega/admin/AdminRegistry.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `labyrinth/mega/crucible/CrucibleVault.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | player list, loop | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/enc/EncounterDirector.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `labyrinth/mega/enc/WandererDirector.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `labyrinth/mega/floors/FloorTracker.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: every TRACK_EVERY_TICKS (config, 10), dungeon dimension test first [probe s.mega.floor_tracker] |
| `labyrinth/mega/flow/FlowAir.java#onServerTick` | ServerTickEvent.Post | every PERIOD_TICKS ticks | player list, loop | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/gen/MegaGenRegistry.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates [probe s.mega.gen] |
| `labyrinth/mega/hub/HubRules.java#onServerTick` | ServerTickEvent.Post | every SWEEP_EVERY ticks | player list, loop | global (loops players of its dimension) | OK: every 100 ticks, Sanctuary only |
| `labyrinth/mega/lock/LockRegistry.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `labyrinth/mega/mechanics/MechanicsDirector.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: returns when there is no dungeon; scan every 10 ticks; active mechanics only [probe s.mega.mechanics] |
| `labyrinth/mega/party/PartyGuard.java#onServerTick` | ServerTickEvent.Post | every EVERY ticks | player list, loop | global (loops players of its dimension) | OK: every 20 ticks, dungeon only |
| `labyrinth/mega/puzzle/PuzzleDirector.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: returns when there is no dungeon; scan every 10 ticks; active puzzles only [probe s.mega.puzzle] |
| `labyrinth/mega/rift/PocketEvents.java#onTick` | ServerTickEvent.Post | every 20 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/rift/RiftService.java#onServerTick` | ServerTickEvent.Post | every 20 ticks (staggered by id) | player list, loop | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/system/FloorInfoRegistry.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `labyrinth/mega/system/SystemEvents.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/theme/ThemeAmbient.java#onServerTick` | ServerTickEvent.Post | every 40 ticks | player list, packets, loop | global (loops players of its dimension) | OK: throttled |
| `labyrinth/mega/travel/StairTravel.java#onServerTick` | ServerTickEvent.Post | every EVERY ticks | player list, loop | global (loops players of its dimension) | OK: every 4 ticks, dungeon only |
| `mana/zone/ManaZoneManager.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates [probe s.mana.zone] |
| `multiverse/WorldSites.java#onServerTick` | ServerTickEvent.Post | every tick | loop | global (loops players of its dimension) | OK: cheap gate, delegates |
| `perf/PerfBoot.java#(lambda)` | ServerTickEvent.Post | every REFRESH_EVERY ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `perf/PerfEvents.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `perf/PerfEvents.java#(lambda)` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/RelicEngine.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/djinn/DjinnSources.java#onServerTick` | ServerTickEvent.Post | every 200 ticks | player list, inventory, loop | global (loops players of its dimension) | OK: throttled |
| `relics/order/OrderEvents.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave10Events.java#onServerTick` | ServerTickEvent.Post | every 4 ticks | loop | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave14Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave15Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave16Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave17Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave18Events.java#onServerTick` | ServerTickEvent.Post | every 20 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave19Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave21Events.java#onServerTick` | ServerTickEvent.Post | every 4 ticks | entity scan, loop | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave25Events.java#onServerTick` | ServerTickEvent.Post | every 4 ticks | loop | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave26Events.java#onServerTick` | ServerTickEvent.Post | every 20 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave29Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave2Events.java#onServerTick` | ServerTickEvent.Post | every tick | loop | global (loops players of its dimension) | OK: light |
| `relics/wave/Wave30Events.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `relics/wave/Wave3Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave4Handlers.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave6Events.java#onServerTick` | ServerTickEvent.Post | every tick | trivial | global (loops players of its dimension) | OK: cheap gate, delegates |
| `relics/wave/Wave9Events.java#onServerTick` | ServerTickEvent.Post | every 10 ticks | trivial | global (loops players of its dimension) | OK: throttled |
| `shadow/UniteWatcher.java#onServerTick` | ServerTickEvent.Post | every tick | player list, loop | global (loops players of its dimension) | OK: loops online players, a few field reads each |
| `soul/PaladinEvents.java#onTick` | ServerTickEvent.Post | every DRIVE_EVERY ticks | loop | global (loops players of its dimension) | OK: throttled |
| `wizard/WizardLibrary.java#onServerTick` | ServerTickEvent.Post | every 2 ticks | entity scan, player list, loop | global (loops players of its dimension) | OK: throttled [probe s.wizard.library] |
| `elite/hub/HubNpcSpawner.java#onLevelTick` | LevelTickEvent.Post | every 100 ticks | trivial | per level | OK: throttled |
| `perf/PerfEvents.java#(lambda)` | LevelTickEvent.Post | every tick | trivial | per level | OK: cheap gate, delegates |
| `perf/PerfEvents.java#(lambda)` | LevelTickEvent.Post | every tick | trivial | per level | OK: cheap gate, delegates |
| `avatar/ai/AvatarAiEvents.java#onEntityTick` | EntityTickEvent.Post | every tick | trivial | per entity, all levels | OK: cheap gate, delegates [probe s.avatar.ai_entity] |
| `blackclover/TimeStop.java#onEntityTick` | EntityTickEvent.Pre | every tick | trivial | per entity, all levels | FIXED: empty-map early exit |
| `client/relics/RelicGradeFx.java#onEntityTick` | EntityTickEvent.Post | every 3 ticks | trivial | per entity, all levels | OK: throttled |
| `relics/MobRelics.java#onTick` | EntityTickEvent.Post | every 20 ticks (staggered by id) | registry, locations, strings, attributes, effects, loop | per entity, all levels | FIXED: modulo before the config read [probe s.relic.mob_pack] |
| `relics/WornSync.java#onEntityTick` | EntityTickEvent.Post | every 20 ticks (staggered by id) | trivial | per entity, all levels | OK: one tick in twenty, exits for any mob that never carried a relic [probe s.relic.worn_sync_mob] |
| `client/ElsdociaGlitterClient.java#onTick` | ClientTickEvent.Post | every client tick | player list, particles, loop | client only | OK: light |
| `client/SwordDrawClient.java#onTick` | ClientTickEvent.Post | every client tick | loop | client only | OK: light |
| `client/avatar/PlayerAvatarClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/butoh/DanceClient.java#onClientTick` | ClientTickEvent.Post | every client tick | loop | client only | OK: light |
| `client/elite/hub/HubClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/grimoire/GrimoireFloatClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/grimoire/GrimoireShelfClient.java#onTick` | ClientTickEvent.Post | every client tick | loop | client only | OK: cheap gate, delegates |
| `client/grimoire/GrimoireSlotClient.java#(lambda)` | ClientTickEvent.Post | every client tick | packets, loop | client only | OK: cheap gate, delegates |
| `client/heroweapon/HeroKeysClient.java#onClientTick` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/heroweapon/curse/CurseClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/heroweapon/forms/FormsClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/manazone/ManaZoneClient.java#onClientTick` | ClientTickEvent.Post | every client tick | loop | client only | OK: light |
| `client/mega/system/FloorCardClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/mega/system/SystemClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/multiverse/MultiverseStatusClient.java#(lambda)` | ClientTickEvent.Post | every client tick | loop | client only | OK: cheap gate, delegates |
| `client/nexus/RelicKeys.java#onClientTick` | ClientTickEvent.Post | every client tick | packets, loop | client only | OK: cheap gate, delegates |
| `client/perf/PerfClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/perf/PerfClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/relics/RelicActKeys.java#onClientTick` | ClientTickEvent.Post | every client tick | packets, loop | client only | OK: light |
| `client/relics/RelicGradeFx.java#onClientTick` | ClientTickEvent.Post | every 8 ticks | player list, loop | client only | OK: throttled |
| `client/relics/djinn/DjinnKeys.java#onClientTick` | ClientTickEvent.Post | every client tick | packets, loop | client only | OK: light |
| `client/relics/fx/RelicFxClient.java#onClientTick` | ClientTickEvent.Post | every client tick | player list, curios, loop | client only | OK: budgeted: players within range and a particle cap per tick (WornBudget) |
| `client/relics/item/SocketFx.java#onClientTick` | ClientTickEvent.Post | every client tick | player list, inventory, loop | client only | OK: budgeted: MAX_HOLDERS and MAX_PARTICLES_PER_TICK; the quality is re-read every 200 ticks |
| `client/skilltree/ShopClient.java#onTick` | ClientTickEvent.Post | every 20 ticks | trivial | client only | OK: throttled |
| `client/song/SongClient.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/ui/GuiScale.java#(lambda)` | ClientTickEvent.Post | every client tick | loop | client only | OK: cheap gate, delegates |
| `client/ui/theme/PageUnlockOverlay.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `vfx/client/VfxClientEvents.java#(lambda)` | ClientTickEvent.Post | every client tick | trivial | client only | OK: cheap gate, delegates |
| `client/grimoire/GrimoireFloatClient.java#onRender` | RenderLevelStageEvent | every frame (several stages) | loop | per frame | OK: light |
| `client/grimoire/GrimoireShelfClient.java#onRender` | RenderLevelStageEvent | every frame (several stages) | loop | per frame | OK: light |
| `client/manazone/ManaZoneClient.java#onRenderStage` | RenderLevelStageEvent | every frame (several stages) | loop | per frame | OK: light |
| `client/perf/PerfClient.java#(lambda)` | RenderLevelStageEvent | every frame (several stages) | trivial | per frame | OK: cheap gate, delegates |
| `client/perf/PerfClient.java#(lambda)` | RenderLevelStageEvent | every frame (several stages) | trivial | per frame | OK: cheap gate, delegates |
| `client/soul/PaladinClient.java#(lambda)` | RenderLevelStageEvent | every frame (several stages) | trivial | per frame | OK: cheap gate, delegates |
| `client/soul/PaladinOverlay.java#onRender` | RenderLevelStageEvent | every frame (several stages) | loop | per frame | OK: light |
| `vfx/client/VfxClientEvents.java#onRenderStage` | RenderLevelStageEvent | every frame (several stages) | trivial | per frame | OK: cheap gate, delegates |
| `client/gui/bossbar/ModBossBarOverlay.java#onLayer` | RenderGuiLayerEvent.Pre | every 20 ticks | trivial | per frame x HUD layer | OK: throttled |
| `client/manazone/ManaZoneClient.java#onGuiLayer` | RenderGuiLayerEvent.Pre | every frame | trivial | per frame x HUD layer | OK: light |
| `client/ui/GuiScale.java#onLayerPre` | RenderGuiLayerEvent.Pre | every frame | trivial | per frame x HUD layer | OK: light |
| `client/ui/GuiScale.java#onLayerPost` | RenderGuiLayerEvent.Post | every frame | trivial | per frame x HUD layer | FIXED: nothing-pushed early exit |
| `arc/midnight/RadesThralls.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `arc/world/ArcFloorBosses.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `arena/ArenaSpawnEvents.java#onJoin` | EntityJoinLevelEvent | on an entity joining | player list, registry, loop | per spawn | OK: event driven (real work only for its own targets) |
| `boss/BossBuffEngine.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `boss/rules/BossRulesHooks.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `compat/create/ContraptionGuard.java#onEntityJoin` | EntityJoinLevelEvent | on an entity joining | registry, strings | per spawn | OK: event driven (real work only for its own targets) |
| `corpse/CorpseHooks.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `dungeon/DungeonGuard.java#onEntityJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `dungeon/nexus/relic/Attunement.java#onItemJoin` | EntityJoinLevelEvent | on an entity joining | strings | per spawn | OK: event driven |
| `dungeon/nexus/relic/RelicEvents.java#onItemJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `entity/zogratis/LuciusArcHooks.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `heroweapon/bond/HeroLoanEvents.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/dnd/vital/HpBuffer.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/crucible/SpiritRules.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/eco/TurfWars.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/enc/DepthMobs.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/enc/EncounterDirector.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/enc/WandererDirector.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/guard/GuardRegistry.java#onEntityJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `labyrinth/mega/system/SystemEvents.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/RelicEngine.java#onSummonLoad` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/djinn/weapon/DjinnWeaponEvents.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/order/OrderEvents.java#onEntityJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/wave/Wave1.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/wave/Wave10Events.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/wave/Wave11Events.java#onJoin` | EntityJoinLevelEvent | on an entity joining | inventory, loop | per spawn | OK: event driven (real work only for its own targets) |
| `relics/wave/Wave19Events.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/wave/Wave30Events.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `relics/wave/Wave9Events.java#onJoin` | EntityJoinLevelEvent | on an entity joining | strings | per spawn | OK: event driven |
| `soul/PaladinEvents.java#onJoin` | EntityJoinLevelEvent | on an entity joining | trivial | per spawn | OK: event driven |
| `NUSMP.java#(lambda)` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `avatar/AvatarSystem.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `avatar/ai/AvatarAiEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `avatar/ritual/SoulForgeSystem.java#(lambda)` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `blackclover/TimeStop.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/AirBook.java#cushion` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `book/ArmourState.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/AshSpells.java#witherThorns` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `book/EyeArts.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/KotodamaWords.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `book/MirrorWorks.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/PaintStudio.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `book/SlashBook.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `book/SmokeBook.java#dodge` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `book/SnowBook.java#frostThorns` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/SpiritBond.java#onHurt` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/WingBook.java#featherFall` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `book/ext/AttributeEvents.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `boss/BossEngravings.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `boss/rules/BossRulesHooks.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `butoh/ButohEvents.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `corpse/CorpseHooks.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/floor/charybdis/CharybdisMinions.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/Attunement.java#onHurt` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/ChaliceHandler.java#onDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/MantleHandler.java#onOutgoing` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/RelicEvents.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/RelicEvents.java#onIncomingDamageLast` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `dungeon/nexus/relic/SheathHandler.java#onDamageLast` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `entity/zogratis/LuciusArcHooks.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `entity/zogratis/LuciusBossEventHandler.java#onIncomingLate` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `entity/zogratis/SoulPaladins.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `entity/zogratis/SpellSeal.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/rules/HeroRuleEvents.java#onPhysicalDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/skill/HeroCombat.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/weapon/shield/ShieldRuntime.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/weapon/whip/WhipCommand.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `item/BossRelics.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | strings, packets, loop | per hit | OK: event driven |
| `item/BroochPinArts.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `item/MagebaneShieldArts.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `item/MagicGear.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/dnd/vital/VitalStrike.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/enc/NoiseEvents.java#onDamage` | LivingIncomingDamageEvent | on a hit | player list | per hit | OK: event driven |
| `labyrinth/mega/flow/FlowEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/gear/GearEvents.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/mechanics/MechanicsEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/system/SystemEvents.java#onFriendlyFire` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `mana/zone/ManaZoneManager.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | loop | per hit | OK: event driven |
| `relics/RelicEngine.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | loop | per hit | OK: event driven |
| `relics/djinn/DjinnEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/djinn/DjinnMagic.java#onOutgoing` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/djinn/DjinnMagic.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/djinn/weapon/DjinnWeaponEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/scaling/AuraEvents.java#onHit` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/socket/SocketEngine.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | loop | per hit | OK: event driven |
| `relics/wave/Wave0Hooks.java#onIncoming` | LivingIncomingDamageEvent | on a hit | packets, effects | per hit | OK: event driven |
| `relics/wave/Wave1.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave10Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave10Events.java#onIncomingLate` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave11Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave12Hooks.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave13Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | attributes | per hit | OK: event driven |
| `relics/wave/Wave14Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave15Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave16Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave17Events.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave18Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave19Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave20Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave21Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | entity scan, loop | per hit | OK: event driven (real work only for its own targets) |
| `relics/wave/Wave22Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave23Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave24Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave25Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | strings, effects | per hit | OK: event driven |
| `relics/wave/Wave26Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave27Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave28Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave29Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave2Events.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave30Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave3Belt.java#fortitude` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave3Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave4Handlers.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Dragon.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Hemlock.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Hooks.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Huntress.java#onOutgoing` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Souls.java#onOutgoing` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave6Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave71Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave7Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave8Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave9Events.java#onIncoming` | LivingIncomingDamageEvent | on a hit | effects | per hit | OK: event driven |
| `shadow/UniteEvents.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | packets | per hit | OK: event driven |
| `soul/PaladinEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `thread/RougeFateEvents.java#onIncoming` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `thread/ThreadSpells.java#onIncomingDamage` | LivingIncomingDamageEvent | on a hit | trivial | per hit | OK: event driven |
| `avatar/ai/AvatarAiEvents.java#onDamagePost` | LivingDamageEvent.Post | on a hit | loop | per hit | OK: event driven |
| `book/DreamWorld.java#onDamage` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `book/ext/AttributeEvents.java#onDamaged` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `charter/CharterEvents.java#onDamaged` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `core/discovery/DiscoveryTracker.java#onDamaged` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `dungeon/MealAttunement.java#onDamage` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/curse/CurseEvents.java#onDamage` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/gem/GemEvents.java#onHurt` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/rules/HeroRuleEvents.java#onShockDamage` | LivingDamageEvent.Pre | on a hit | trivial | per hit | OK: event driven |
| `heroweapon/skill/HeroCombat.java#onDamageDone` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/safe/RestEvents.java#onDamage` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `labyrinth/mega/system/SystemEvents.java#onPlayerHurt` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/pair/PairHooks.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/socket/SocketEngine.java#onDamaged` | LivingDamageEvent.Post | on a hit | loop | per hit | OK: event driven |
| `relics/wave/Wave0Hooks.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave10Events.java#onPost` | LivingDamageEvent.Post | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave12Hooks.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave14Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave15Events.java#onDamagePre` | LivingDamageEvent.Pre | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave15Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | strings | per hit | OK: event driven |
| `relics/wave/Wave19Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave21Events.java#onPost` | LivingDamageEvent.Post | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave22Events.java#onPost` | LivingDamageEvent.Post | on a hit | effects, loop | per hit | OK: event driven |
| `relics/wave/Wave23Events.java#onDamage` | LivingDamageEvent.Pre | on a hit | effects | per hit | OK: event driven |
| `relics/wave/Wave24Events.java#onPre` | LivingDamageEvent.Pre | on a hit | strings | per hit | OK: event driven |
| `relics/wave/Wave24Events.java#onPost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave26Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave28Events.java#onDamagePre` | LivingDamageEvent.Pre | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave28Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave29Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave3Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave5Hooks.java#onDamagePre` | LivingDamageEvent.Pre | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave6Events.java#onDamaged` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `relics/wave/Wave8Events.java#onDamagePost` | LivingDamageEvent.Post | on a hit | trivial | per hit | OK: event driven |
| `song/SongTicker.java#onDamaged` | LivingDamageEvent.Post | on a hit | effects | per hit | OK: event driven |
| `client/SwordDrawClient.java#onRenderHand` | RenderHandEvent | every frame | trivial | per frame | OK: light |
| `client/relics/djinn/DjinnFirstPerson.java#onRenderHand` | RenderHandEvent | every frame | trivial | per frame | OK: light |

#### B. tick() / aiStep() / customServerAiStep() of entities, block entities, goals and schedulers (248 rows)

| Method | Extends | Frequency | Work per call | Scales with | Verdict |
|---|---|---|---|---|---|
| `arc/ArcNpcEntity.java#tick` (ArcNpcEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `arc/ArcNpcEntity.java#tick` (ArcNpcEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `arc/sds/BanBoss.java#baseTick` (BanBoss) | ArcBoss | every tick | trivial | per entity | OK: light |
| `avatar/AvatarEntity.java#aiStep` (AvatarEntity) | TamableAnimal | every 20 ticks | trivial | per entity | OK: throttled |
| `avatar/ai/AvatarLootGoal.java#tick` (AvatarLootGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/ai/AvatarMageGoal.java#tick` (AvatarMageGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/ai/AvatarRangedGoal.java#tick` (AvatarRangedGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/ai/AvatarShieldGoal.java#tick` (AvatarShieldGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/ai/AvatarWatcherGoal.java#tick` (AvatarWatcherGoal) | Goal | every 40 ticks | trivial | per entity | OK: throttled |
| `avatar/ai/PersonalityGoals.java#tick` (Retreat) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/ai/PersonalityGoals.java#tick` (Retreat) | Goal | every 10 ticks | trivial | per entity | OK: throttled |
| `avatar/ai/YandereJealousyTargetGoal.java#tick` (YandereJealousyTargetGoal) | Goal | every 5 ticks | trivial | per entity | OK: throttled |
| `avatar/delve/AvatarDelveGoal.java#tick` (AvatarDelveGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/evolution/AvatarEvolution.java#tick` (AvatarEvolution) |  | every tick | effects | per instance | OK: light |
| `avatar/goal/AvatarFollowCreatorGoal.java#tick` (AvatarFollowCreatorGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/goal/AvatarHuntCreatorGoal.java#tick` (AvatarHuntCreatorGoal) | Goal | every tick | strings | per entity | OK: light |
| `avatar/menu/ForgeMotion.java#tick` (ForgeMotion) |  | every tick | trivial | per instance | OK: light |
| `avatar/menu/ForgeMotion.java#tick` (ForgeMotion) |  | every tick | trivial | per instance | OK: light |
| `avatar/outcome/AvatarOutcomes.java#tick` (AvatarOutcomes) |  | every 100 ticks | trivial | per instance | OK: throttled |
| `avatar/party/AutonomousNamingGoal.java#tick` (AutonomousNamingGoal) | Goal | every tick | trivial | per entity | OK: light |
| `avatar/party/AvatarPartyManager.java#tick` (AvatarPartyManager) | SavedData | every tick | loop | per entity | OK: light |
| `avatar/ritual/SoulForgeRitual.java#tick` (SoulForgeRitual) |  | every tick | blocks, loop | per instance | OK: a ritual that exists only while one is performed |
| `avatar/yandere/YandereFrenzyHandler.java#tick` (YandereFrenzyHandler) |  | every tick | trivial | per instance | OK: light |
| `book/ArmourState.java#tick` (ArmourState) |  | every tick | effects | per instance | OK: light |
| `book/SongRebuildKit.java#tick` (SongRebuildKit) |  | every tick | trivial | per instance | OK: light |
| `book/ext/AttributeEvents.java#serverTick` (AttributeEvents) |  | every tick | trivial | per instance | OK: light |
| `boss/budget/BossBudget.java#tick` (BossBudget) |  | every tick | trivial | per instance | OK: light |
| `butoh/ButohDance.java#tick` (ButohDance) |  | every tick | trivial | per instance | OK: light |
| `celestial/CelestialDress.java#tick` (CelestialDress) |  | every tick | attributes | per instance | OK: light |
| `celestial/CelestialProps.java#tick` (CelestialProps) |  | every tick | trivial | per instance | OK: light |
| `celestial/CelestialProps.java#tick` (CelestialProps) |  | every tick | trivial | per instance | OK: light |
| `celestial/SpiritKingEntity.java#baseTick` (SpiritKingEntity) | Monster | every tick | trivial | per entity | OK: light |
| `celestial/SpiritKingEntity.java#aiStep` (SpiritKingEntity) | Monster | every 20 ticks | particles | per entity | OK: throttled |
| `celestial/SpiritWorld.java#tick` (SpiritWorld) | SavedData | every tick | loop | per entity | OK: light |
| `client/avatar/AppearancePage.java#tick` (AppearancePage) | ForgePage | every tick | trivial | per frame | OK: light |
| `client/avatar/AvatarActionScreen.java#tick` (AvatarActionScreen) | Screen | every tick | trivial | per frame | OK: light |
| `client/avatar/ForgePage.java#tick` (ForgePage) |  | every tick | trivial | per frame | OK: light |
| `client/avatar/IdentityPage.java#tick` (IdentityPage) | ForgePage | every tick | trivial | per frame | OK: light |
| `client/avatar/PlayerAvatarClient.java#tick` (PlayerAvatarClient) |  | every tick | particles, loop | per frame | OK: light |
| `client/avatar/SkillsPage.java#tick` (SkillsPage) | ForgePage | every tick | trivial | per frame | OK: light |
| `client/compat/CompatCodexScreen.java#tick` (CompatCodexScreen) | Screen | every tick | trivial | per frame | OK: light |
| `client/elite/hub/HubClient.java#tick` (HubClient) |  | every tick | loop | per frame | OK: light |
| `client/grimoire/GrimoireFloatClient.java#tick` (GrimoireFloatClient) |  | every tick | loop | per frame | OK: light |
| `client/heroweapon/ClientHeroBond.java#tick` (ClientHeroBond) |  | every tick | trivial | per frame | OK: light |
| `client/heroweapon/HeroKeysClient.java#tick` (HeroKeysClient) |  | every tick | packets, loop | per frame | OK: light |
| `client/heroweapon/curse/CurseClient.java#tick` (CurseClient) |  | every tick | packets | per frame | OK: light |
| `client/heroweapon/forms/FormsClient.java#tick` (FormsClient) |  | every tick | packets, loop | per frame | OK: light |
| `client/heroweapon/forms/WeaponBookScreen.java#tick` (WeaponBookScreen) | Screen | every tick | trivial | per frame | OK: light |
| `client/mega/system/FloorCardClient.java#tick` (FloorCardClient) |  | every tick | packets, loop | per frame | OK: light |
| `client/mega/system/SystemClient.java#tick` (SystemClient) |  | every tick | loop | per frame | OK: light |
| `client/particle/rukh/RukhParticleBase.java#tick` (RukhParticleBase) | TextureSheetParticle | every tick | trivial | per frame | OK: light |
| `client/skilltree/shop/ShopScreen.java#tick` (ShopScreen) | Screen | every tick | trivial | per frame | OK: light |
| `client/song/SongClient.java#tick` (SongClient) |  | every tick | packets | per frame | OK: light |
| `client/ui/CodexScreen.java#tick` (CodexScreen) | Screen | every tick | trivial | per frame | OK: light |
| `client/ui/theme/PageUnlockOverlay.java#tick` (PageUnlockOverlay) |  | every tick | strings, loop | per frame | OK: light |
| `corpse/Corpses.java#tick` (Corpses) |  | every SWEEP ticks | trivial | per instance | OK: throttled |
| `corpse/NamedCorpseEntity.java#aiStep` (NamedCorpseEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `corpse/NamedCorpseEntity.java#tick` (NamedCorpseEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `corpse/NamedCorpseEntity.java#tick` (NamedCorpseEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `corpse/Pacts.java#tick` (Pacts) |  | every 10 ticks | loop | per instance | OK: throttled |
| `dungeon/DungeonCreature.java#aiStep` (DungeonCreature) | PathfinderMob | every 100 ticks | trivial | per entity | OK: throttled |
| `dungeon/DungeonGoals.java#tick` (HuntForSustenance) | Goal | every tick | trivial | per entity | OK: light |
| `dungeon/floor/charybdis/CharybdisSpawnBoss.java#tick` (CharybdisSpawnBoss) | ZogratisBoss | every tick | trivial | per entity | OK: light |
| `dungeon/floor/charybdis/MagiculeScaleEntity.java#tick` (MagiculeScaleEntity) | AbstractHurtingProjectile | every tick | particles | per entity | OK: light |
| `dungeon/floor/goliath/GoliathBoss.java#tick` (GoliathBoss) | ZogratisBoss | every tick | trivial | per entity | OK: light |
| `dungeon/nexus/relic/AmuletHandler.java#tick` (AmuletHandler) |  | every tick | attributes | per instance | OK: light |
| `dungeon/nexus/relic/Attunement.java#tick` (Attunement) |  | every tick | inventory, strings | per instance | OK: no clock is made for a player who never attunes: one item class test per tick |
| `dungeon/nexus/relic/SheathHandler.java#tick` (SheathHandler) |  | every tick | trivial | per instance | OK: light |
| `elite/hub/HubNpcBase.java#tick` (HubNpcBase) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `elite/hub/HubSync.java#tick` (HubSync) |  | every GAP_TICKS ticks | packets, loop | per instance | OK: throttled |
| `entity/CottonCloudEntity.java#tick` (CottonCloudEntity) | Entity | every tick | trivial | per entity | OK: light |
| `entity/CottonSheepEntity.java#tick` (CottonSheepEntity) | PathfinderMob | every 4 ticks | particles | per entity | OK: throttled |
| `entity/ElementalSpiritLordEntity.java#tick` (ElementalSpiritLordEntity) | TamableAnimal | every tick | trivial | per entity | OK: light |
| `entity/ElementalSpiritLordEntity.java#customServerAiStep` (ElementalSpiritLordEntity) | TamableAnimal | every tick | packets | per entity | OK: light |
| `entity/GrimoireDaemonEntity.java#customServerAiStep` (GrimoireDaemonEntity) | Monster | every tick | trivial | per entity | OK: light |
| `entity/GrimoireDaemonEntity.java#aiStep` (GrimoireDaemonEntity) | Monster | every 3 ticks | particles | per entity | OK: throttled |
| `entity/MirrorDoubleEntity.java#tick` (MirrorDoubleEntity) | TamableAnimal | every tick | trivial | per entity | OK: light |
| `entity/MirrorDoubleEntity.java#customServerAiStep` (MirrorDoubleEntity) | TamableAnimal | every tick | trivial | per entity | OK: light |
| `entity/PaintedConstructEntity.java#tick` (PaintedConstructEntity) | TamableAnimal | every 20 ticks | trivial | per entity | OK: throttled |
| `entity/SpiritLordEntity.java#customServerAiStep` (SpiritLordEntity) | TamableAnimal | every tick | strings, packets | per entity | OK: light |
| `entity/SummonBrain.java#tick` (Guard) | Goal | every tick | packets | per entity | OK: light |
| `entity/ZagredBossEntity.java#baseTick` (ZagredBossEntity) | Monster | every tick | trivial | per entity | OK: light |
| `entity/ZagredBossEntity.java#customServerAiStep` (ZagredBossEntity) | Monster | every tick | packets, loop | per entity | OK: light |
| `entity/ZagredBossEntity.java#aiStep` (ZagredBossEntity) | Monster | every 2 ticks | particles, loop | per entity | OK: throttled |
| `entity/ZagredDefense.java#tick` (ZagredDefense) |  | every tick | loop | per instance | OK: light |
| `entity/ZagredDefense.java#tick` (ZagredDefense) |  | every 2 ticks | entity scan, loop | per instance | OK: throttled |
| `entity/zogratis/BloodBeastEntity.java#tick` (BloodBeastEntity) | Monster | every tick | trivial | per entity | OK: light |
| `entity/zogratis/BloodBeastEntity.java#customServerAiStep` (BloodBeastEntity) | Monster | every 6 ticks | trivial | per entity | OK: throttled |
| `entity/zogratis/BloodBeastEntity.java#aiStep` (BloodBeastEntity) | Monster | every 3 ticks | particles | per entity | OK: throttled |
| `entity/zogratis/DarkDiscipleEntity.java#customServerAiStep` (DarkDiscipleEntity) | Monster | every 10 ticks | trivial | per entity | OK: throttled |
| `entity/zogratis/DarkDiscipleEntity.java#aiStep` (DarkDiscipleEntity) | Monster | every 3 ticks | particles | per entity | OK: throttled |
| `entity/zogratis/LuciusArcDirector.java#tick` (LuciusArcDirector) |  | every tick | trivial | per instance | OK: light |
| `entity/zogratis/LuciusBoss.java#baseTick` (LuciusBoss) | ZogratisBoss | every tick | trivial | per entity | OK: light |
| `entity/zogratis/LuciusBoss.java#aiStep` (LuciusBoss) | ZogratisBoss | every tick | trivial | per entity | OK: light |
| `entity/zogratis/LuciusGod.java#tick` (LuciusGod) |  | every tick | trivial | per instance | OK: light |
| `entity/zogratis/ManaPool.java#tick` (ManaPool) |  | every tick | trivial | per instance | OK: light |
| `entity/zogratis/Precognition.java#tick` (Precognition) |  | every tick | trivial | per instance | OK: light |
| `entity/zogratis/ZogratisBoss.java#tick` (ZogratisBoss) | Monster | every tick | trivial | per entity | OK: light |
| `entity/zogratis/ZogratisBoss.java#baseTick` (ZogratisBoss) | Monster | every tick | trivial | per entity | OK: light |
| `entity/zogratis/ZogratisBoss.java#aiStep` (ZogratisBoss) | Monster | every 3 ticks | particles, loop | per entity | OK: throttled |
| `entity/zogratis/ZogratisBoss.java#customServerAiStep` (ZogratisBoss) | Monster | every tick | loop | per entity | OK: light |
| `entity/zogratis/ZogratisCloneEntity.java#tick` (ZogratisCloneEntity) | Monster | every tick | trivial | per entity | OK: light |
| `entity/zogratis/ZogratisCloneEntity.java#customServerAiStep` (ZogratisCloneEntity) | Monster | every tick | trivial | per entity | OK: light |
| `heroweapon/curse/CurseService.java#tick` (CurseService) |  | every tick | trivial | per instance | OK: light |
| `heroweapon/forms/FormsSync.java#tick` (FormsSync) |  | every every ticks | packets, loop | per instance | OK: throttled |
| `heroweapon/gem/GemChoosing.java#tick` (GemChoosing) |  | every 5 ticks | trivial | per instance | OK: throttled |
| `heroweapon/weapon/bow/BowShots.java#tick` (BowShots) |  | every tick | strings | per instance | OK: light |
| `heroweapon/weapon/carriage/CarriageEntity.java#tick` (CarriageEntity) | LivingEntity | every tick | trivial | per entity | OK: light |
| `heroweapon/weapon/carriage/CarriageEntity.java#serverTick` (CarriageEntity) | LivingEntity | every tick | trivial | per entity | OK: light |
| `heroweapon/weapon/carriage/CarriageRules.java#tick` (CarriageRules) |  | every tick | trivial | per instance | OK: light |
| `heroweapon/weapon/shield/GuardMeter.java#tick` (GuardMeter) |  | every tick | trivial | per instance | OK: light |
| `heroweapon/weapon/spear/SpearLust.java#tick` (SpearLust) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/admin/AdminRegistry.java#tick` (AdminRegistry) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/admin/FloorTeleport.java#tick` (FloorTeleport) |  | every CHECK_EVERY ticks | loop | per instance | OK: throttled |
| `labyrinth/mega/admin/SkillAdmin.java#tick` (SkillAdmin) |  | every tick | loop | per instance | OK: light |
| `labyrinth/mega/admin/WfcDebugService.java#tick` (WfcDebugService) |  | every tick | loop | per instance | OK: light |
| `labyrinth/mega/crucible/CatastropheRunner.java#tick` (CatastropheRunner) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/crucible/CatastropheRunner.java#tick` (CatastropheRunner) |  | every tick | loop | per instance | OK: light |
| `labyrinth/mega/enc/DepthMind.java#tick` (DepthMind) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/enc/EncounterDirector.java#tick` (EncounterDirector) |  | every tick | player list, loop | per instance | OK: light |
| `labyrinth/mega/enc/PatrolGoal.java#tick` (PatrolGoal) | Goal | every 10 ticks | trivial | per entity | OK: throttled |
| `labyrinth/mega/enc/WandererDirector.java#tick` (WandererDirector) |  | every 100 ticks | player list | per instance | OK: throttled |
| `labyrinth/mega/floors/FloorTracker.java#tick` (FloorTracker) |  | every GUARD_EVERY ticks | player list, loop | per instance | OK: throttled |
| `labyrinth/mega/foundry/FoundryAbilityRunner.java#tick` (FoundryAbilityRunner) |  | every tick | entity scan, effects, loop | per instance | OK: counters; the entity scan only while a dash runs (0.5 block box) |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every 4 ticks | trivial | per entity | OK: throttled |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every 6 ticks | trivial | per entity | OK: throttled |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/foundry/FoundryGoals.java#tick` (AbilityGoal) | Goal | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/foundry/FoundryMobEntity.java#aiStep` (FoundryMobEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/gen/ExpansionQueue.java#tick` (ExpansionQueue) |  | every 2 ticks | strings | per instance | OK: throttled |
| `labyrinth/mega/guard/DndBoss.java#tick` (DndBoss) |  | every tick | loop | per instance | OK: light |
| `labyrinth/mega/guard/GuardianAltarBlockEntity.java#serverTick` (GuardianAltarBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/guard/GuardianAltarBlockEntity.java#tick` (GuardianAltarBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/guard/GuardianTraits.java#tick` (GuardianTraits) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/guard/LabyrinthGuardianEntity.java#aiStep` (LabyrinthGuardianEntity) | FloorWardenEntity | every 20 ticks | trivial | per entity | OK: throttled |
| `labyrinth/mega/guard/LabyrinthGuardianEntity.java#baseTick` (LabyrinthGuardianEntity) | FloorWardenEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/mega/lock/LockBook.java#tick` (LockBook) |  | every tick | loop | per instance | OK: light |
| `labyrinth/mega/mechanics/MechanicsDirector.java#tick` (MechanicsDirector) |  | every tick | player list, loop | per instance | OK: light |
| `labyrinth/mega/mechanics/Prim.java#tick` (Prim) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/mechanics/Prims.java#tick` (Prims) |  | every tick | trivial | per instance | OK: light |
| `labyrinth/mega/puzzle/PuzzleDirector.java#tick` (PuzzleDirector) |  | every tick | player list, loop | per instance | OK: light |
| `labyrinth/mega/rift/DimensionalRiftBlockEntity.java#tick` (DimensionalRiftBlockEntity) | BlockEntity | every tick | player list | per entity | OK: light |
| `labyrinth/mega/rift/PocketRealm.java#tick` (PocketRealm) | SavedData | every tick | loop | per entity | OK: light |
| `labyrinth/mega/rift/WaypointService.java#tick` (WaypointService) |  | every SAVE_EVERY ticks (staggered by id) | trivial | per instance | OK: throttled |
| `labyrinth/mega/system/FloorInfo.java#tick` (FloorInfo) |  | every 20 ticks | player list, loop | per instance | OK: once a second per dungeon player, signature compared before any message [probe s.mega.floor_info] |
| `labyrinth/mega/system/MinionGoals.java#tick` (OwnerHurtByGoal) | Goal | every 10 ticks | trivial | per entity | OK: throttled |
| `labyrinth/mega/system/SystemSync.java#tick` (SystemSync) |  | every tick | packets, loop | per instance | OK: light [probe s.mega.system_sync] |
| `labyrinth/mega/travel/MegaTravel.java#tick` (MegaTravel) |  | every CHECK_EVERY ticks | strings, loop | per instance | OK: throttled |
| `labyrinth/seal/FloorWardenEntity.java#aiStep` (FloorWardenEntity) | LivingArmorEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/seal/PeakAltarBlockEntity.java#serverTick` (PeakAltarBlockEntity) | BlockEntity | every POLL ticks | trivial | per entity | OK: throttled |
| `labyrinth/seal/PeakAltarBlockEntity.java#tick` (PeakAltarBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/trap/PitfallTileBlock.java#tick` (PitfallTileBlock) | Block | every tick | blocks | per entity | OK: a scheduled block tick (fires when the trap is armed), not a poll |
| `labyrinth/trap/SnufferGlyphBlock.java#tick` (SnufferGlyphBlock) | Block | every tick | entity scan, blocks, effects, loop | per entity | OK: a scheduled block tick with a bounded box |
| `labyrinth/trial/SealedArchwayBlockEntity.java#serverTick` (SealedArchwayBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/trial/SealedArchwayBlockEntity.java#tick` (SealedArchwayBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `labyrinth/vault/RelicPedestalBlockEntity.java#clientTick` (RelicPedestalBlockEntity) | BlockEntity | every tick | trivial | per entity | OK: light |
| `prop/AirProps.java#tick` (AirProps) |  | every tick | trivial | per instance | OK: light |
| `prop/AshProps.java#tick` (AshProps) |  | every tick | trivial | per instance | OK: light |
| `prop/BeastProps.java#tick` (BeastProps) |  | every tick | trivial | per instance | OK: light |
| `prop/BronzeProps.java#tick` (BronzeProps) |  | every tick | trivial | per instance | OK: light |
| `prop/CorundumProps.java#tick` (CorundumProps) |  | every tick | trivial | per instance | OK: light |
| `prop/CorundumProps.java#tick` (CorundumProps) |  | every tick | trivial | per instance | OK: light |
| `prop/EarthConstructProps.java#tick` (EarthConstructProps) |  | every tick | trivial | per instance | OK: light |
| `prop/GravityProps.java#tick` (GravityProps) |  | every tick | trivial | per instance | OK: light |
| `prop/GravityProps.java#tick` (GravityProps) |  | every tick | trivial | per instance | OK: light |
| `prop/KeyProps.java#tick` (KeyProps) |  | every tick | trivial | per instance | OK: light |
| `prop/KeyProps.java#tick` (KeyProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MagicPropEntity.java#tick` (MagicPropEntity) | Entity | every tick | trivial | per entity | OK: light |
| `prop/MemoryRebuildProps.java#tick` (MemoryRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MemoryRebuildProps.java#tick` (MemoryRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MercuryProps.java#tick` (MercuryProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MercuryProps.java#tick` (MercuryProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MercuryProps.java#tick` (MercuryProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MirrorProps.java#tick` (MirrorProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MirrorProps.java#tick` (MirrorProps) |  | every tick | trivial | per instance | OK: light |
| `prop/ModificationProps.java#tick` (ModificationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/ModificationProps.java#tick` (ModificationProps) |  | every 8 ticks | packets | per instance | OK: throttled |
| `prop/ModificationProps.java#tick` (ModificationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MucusProps.java#tick` (MucusProps) |  | every tick | trivial | per instance | OK: light |
| `prop/MudProps.java#tick` (MudProps) |  | every tick | trivial | per instance | OK: light |
| `prop/PermeationProps.java#tick` (PermeationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/PermeationProps.java#tick` (PermeationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/PlantProps.java#tick` (PlantProps) |  | every tick | trivial | per instance | OK: light |
| `prop/PoisonPlantProps.java#tick` (PoisonPlantProps) |  | every tick | trivial | per instance | OK: light |
| `prop/RecombinationProps.java#tick` (RecombinationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/RecombinationProps.java#tick` (RecombinationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/RecombinationProps.java#tick` (RecombinationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/RecombinationProps.java#tick` (RecombinationProps) |  | every tick | packets, loop | per instance | OK: light |
| `prop/ShakudoPropKit.java#tick` (ShakudoPropKit) |  | every tick | trivial | per instance | OK: light |
| `prop/ShakudoPropKit.java#tick` (ShakudoPropKit) |  | every tick | trivial | per instance | OK: light |
| `prop/ShakudoPropKit.java#tick` (ShakudoPropKit) |  | every tick | trivial | per instance | OK: light |
| `prop/ShakudoPropKit.java#tick` (ShakudoPropKit) |  | every tick | trivial | per instance | OK: light |
| `prop/SlashProps.java#tick` (SlashProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SmokeProps.java#tick` (SmokeProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SnowProps.java#tick` (SnowProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SongRebuildProps.java#tick` (SongRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SongRebuildProps.java#tick` (SongRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoulCorpseRebuildProps.java#tick` (SoulCorpseRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoulCorpseRebuildProps.java#tick` (SoulCorpseRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoulRebuildProps.java#tick` (SoulRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoulRebuildProps.java#tick` (SoulRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoundRebuildProps.java#tick` (SoundRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SoundRebuildProps.java#tick` (SoundRebuildProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SpatialProps.java#tick` (SpatialProps) |  | every tick | trivial | per instance | OK: light |
| `prop/SpatialProps.java#tick` (SpatialProps) |  | every tick | trivial | per instance | OK: light |
| `prop/TransmutationProps.java#tick` (TransmutationProps) |  | every tick | trivial | per instance | OK: light |
| `prop/TransmutationProps.java#tick` (TransmutationProps) |  | every 8 ticks | packets | per instance | OK: throttled |
| `prop/TransmutationProps.java#tick` (TransmutationProps) |  | every 20 ticks | packets | per instance | OK: throttled |
| `prop/TreeProps.java#tick` (TreeProps) |  | every tick | trivial | per instance | OK: light |
| `prop/VineProps.java#tick` (VineProps) |  | every tick | trivial | per instance | OK: light |
| `prop/VortexProps.java#tick` (VortexProps) |  | every tick | entity scan, loop | per instance | OK: a spell prop alive for a few seconds; the pull needs the box every tick |
| `prop/WaterPropFlight.java#tick` (WaterPropFlight) |  | every tick | trivial | per instance | OK: light |
| `prop/WingProps.java#tick` (WingProps) |  | every tick | trivial | per instance | OK: light |
| `relics/RelicSummons.java#tick` (RelicSummons) |  | every EVERY ticks | loop | per instance | OK: throttled |
| `relics/djinn/weapon/DjinnWeaponActs.java#tick` (DjinnWeaponActs) |  | every tick | loop | per instance | OK: light |
| `relics/wave/Wave14Zones.java#tick` (Wave14Zones) |  | every tick | loop | per instance | OK: light |
| `relics/wave/Wave15Field.java#tick` (Wave15Field) |  | every 2 ticks | trivial | per instance | OK: throttled |
| `relics/wave/Wave17Dragon.java#tick` (Wave17Dragon) |  | every STEP ticks | trivial | per instance | OK: throttled |
| `relics/wave/Wave19Field.java#tick` (Wave19Field) |  | every 5 ticks | trivial | per instance | OK: throttled |
| `relics/wave/Wave29Portal.java#tick` (Wave29Portal) |  | every tick | loop | per instance | OK: light |
| `relics/wave/Wave30Bond.java#tick` (Wave30Bond) |  | every tick | loop | per instance | OK: light |
| `relics/wave/Wave3Field.java#tick` (Wave3Field) |  | every 5 ticks | trivial | per instance | OK: throttled |
| `relics/wave/Wave5Dragon.java#tick` (Wave5Dragon) |  | every tick | trivial | per instance | OK: light |
| `relics/wave/Wave5Hemlock.java#tick` (Wave5Hemlock) |  | every tick | entity scan, loop | per instance | OK: called by the once-a-second player pass |
| `relics/wave/Wave5Huntress.java#tick` (Wave5Huntress) |  | every tick | entity scan, loop | per instance | OK: called by the once-a-second player pass |
| `relics/wave/Wave5Safeguard.java#tick` (Wave5Safeguard) |  | every tick | strings | per instance | OK: light |
| `relics/wave/Wave5Souls.java#tick` (Wave5Souls) |  | every tick | effects, loop | per instance | OK: light |
| `relics/wave/Wave5Warmage.java#tick` (Wave5Warmage) |  | every tick | trivial | per instance | OK: light |
| `relics/wave/Wave6Zones.java#tick` (Wave6Zones) |  | every tick | loop | per instance | OK: light |
| `shadow/ShadowPackProps.java#tick` (ShadowPackProps) |  | every tick | trivial | per instance | OK: light |
| `shadow/UniteAbilities.java#tick` (UniteAbilities) |  | every SPIKE_EVERY ticks | trivial | per instance | OK: throttled |
| `skill/SkillZones.java#tick` (SkillZones) |  | every tick | loop | per instance | OK: light |
| `skill/Summons.java#tick` (Summons) |  | every tick | entity scan, loop | per instance | OK: called by the owner pass once a second |
| `soul/PaladinChannel.java#tick` (PaladinChannel) |  | every tick | loop | per instance | OK: light |
| `soul/PaladinGoals.java#tick` (DefendOwnerGoal) | Goal | every 10 ticks | trivial | per entity | OK: throttled |
| `thread/RougeEntity.java#tick` (RougeEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `thread/RougeEntity.java#tick` (RougeEntity) | PathfinderMob | every 4 ticks | entity scan, loop | per entity | OK: throttled |
| `thread/RougeEntity.java#tick` (RougeEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `thread/RougeEntity.java#tick` (RougeEntity) | PathfinderMob | every tick | trivial | per entity | OK: light |
| `thread/ThreadTrapEntity.java#tick` (ThreadTrapEntity) | Entity | every tick | entity scan, loop | per entity | OK: projectile catch needs every tick (a bolt moves up to 3 blocks per tick and is tested along its segment); the living-entity touch test runs every 2 ticks |
| `vfx/client/FxRenderer.java#tick` (FxRenderer) |  | every tick | loop | per frame | OK: light |
| `vfx/client/VfxInstance.java#tick` (VfxInstance) |  | every tick | trivial | per frame | OK: light |
| `vfx/client/VfxManager.java#tick` (VfxManager) |  | every tick | strings, loop | per frame | OK: light |
| `vfx/client/VfxShake.java#tick` (VfxShake) |  | every tick | trivial | per frame | OK: light |
| `vfx/client/portal/PortalClient.java#tick` (PortalClient) |  | every 70 ticks | loop | per frame | OK: throttled |

#### C. Client render methods of layers, renderers and overlays (239 rows, 0 marked WATCH)

| Renderer / layer | Extends | Work per frame | Scales with | Verdict |
|---|---|---|---|---|
| `avatar/inventory/client/AvatarInventoryScreen.java#render` (AvatarInventoryScreen) | AbstractContainerScreen<AvatarInventoryM | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/CottonCloudModel.java#render` (CottonCloudModel) |  | trivial | per visible entity or player | OK |
| `client/CottonCloudRenderer.java#render` (CottonCloudRenderer) | EntityRenderer<CottonCloudEntity> | loop | per visible entity or player | OK |
| `client/CottonSheepModel.java#setupAnim` (CottonSheepModel) | EntityModel<CottonSheepEntity> | trivial | per visible entity or player | OK |
| `client/CottonSheepModel.java#renderToBuffer` (CottonSheepModel) | EntityModel<CottonSheepEntity> | trivial | per visible entity or player | OK |
| `client/ElementalSpiritRenderer.java#render` (ElementalSpiritRenderer) | EntityRenderer<ElementalSpiritLordEntity | trivial | per visible entity or player | OK |
| `client/GrimoireDaemonModel.java#setupAnim` (GrimoireDaemonModel) | EntityModel<GrimoireDaemonEntity> | loop | per visible entity or player | OK |
| `client/GrimoireDaemonModel.java#renderToBuffer` (GrimoireDaemonModel) | EntityModel<GrimoireDaemonEntity> | trivial | per visible entity or player | OK |
| `client/GrimoireDaemonRenderer.java#render` (GrimoireDaemonRenderer) | MobRenderer<GrimoireDaemonEntity, Grimoi | trivial | per visible entity or player | OK |
| `client/MirrorDoubleGarbLayer.java#render` (MirrorDoubleGarbLayer) | RenderLayer<MirrorDoubleEntity, PlayerMo | trivial | per visible entity or player | OK |
| `client/MirrorDoubleRenderer.java#render` (MirrorDoubleRenderer) | LivingEntityRenderer<MirrorDoubleEntity, | trivial | per visible entity or player | OK |
| `client/PaintedConstructRenderer.java#render` (PaintedConstructRenderer) | MobRenderer<PaintedConstructEntity, Enti | trivial | per visible entity or player | OK |
| `client/PaintedConstructRenderer.java#render` (PaintedConstructRenderer) | MobRenderer<PaintedConstructEntity, Enti | trivial | per visible entity or player | OK |
| `client/PaintedConstructRenderer.java#render` (PaintedConstructRenderer) | MobRenderer<PaintedConstructEntity, Enti | trivial | per visible entity or player | OK |
| `client/PaintedConstructRenderer.java#setupAnim` (PaintedConstructRenderer) | MobRenderer<PaintedConstructEntity, Enti | loop | per visible entity or player | OK |
| `client/RougeCatLayer.java#render` (RougeCatLayer) | RenderLayer<AbstractClientPlayer, Player | trivial | per visible entity or player | OK |
| `client/RougeCatModel.java#render` (RougeCatModel) |  | trivial | per visible entity or player | OK |
| `client/SpiritOrbRenderer.java#render` (SpiritOrbRenderer) | EntityRenderer<SpiritLordEntity> | loop | per visible entity or player | OK |
| `client/WindSpiritLordRenderer.java#render` (WindSpiritLordRenderer) | EntityRenderer<WindSpiritLordEntity> | trivial | per visible entity or player | OK |
| `client/ZagredModel.java#setupAnim` (ZagredModel) | EntityModel<ZagredBossEntity> | loop | per visible entity or player | OK |
| `client/ZagredModel.java#renderToBuffer` (ZagredModel) | EntityModel<ZagredBossEntity> | trivial | per visible entity or player | OK |
| `client/ZagredRenderer.java#render` (ZagredRenderer) | MobRenderer<ZagredBossEntity, ZagredMode | trivial | per visible entity or player | OK |
| `client/ZagredRenderer.java#render` (ZagredRenderer) | MobRenderer<ZagredBossEntity, ZagredMode | trivial | per visible entity or player | OK |
| `client/ZagredRenderer.java#render` (ZagredRenderer) | MobRenderer<ZagredBossEntity, ZagredMode | trivial | per visible entity or player | OK |
| `client/ZagredRenderer.java#render` (ZagredRenderer) | MobRenderer<ZagredBossEntity, ZagredMode | trivial | per visible entity or player | OK |
| `client/arc/ArcBossRenderer.java#render` (ArcBossRenderer) | ZogratisRenderer<ArcBoss> | trivial | per visible entity or player | OK |
| `client/arc/ArcNpcRenderer.java#render` (ArcNpcRenderer) | ZogratisRenderer<ArcNpcEntity> | trivial | per visible entity or player | OK |
| `client/aura/PlayerAuraLayer.java#render` (PlayerAuraLayer) | RenderLayer<AbstractClientPlayer, Player | loop | per visible entity or player | FIXED: aura LOD by distance |
| `client/avatar/AppearancePage.java#render` (AppearancePage) | ForgePage | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/avatar/AptitudesPage.java#render` (AptitudesPage) | ForgePage | strings, loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/avatar/AvatarEyesLayer.java#render` (AvatarEyesLayer) | RenderLayer<AvatarEntity, PlayerModel<Av | trivial | per visible entity or player | OK |
| `client/avatar/AvatarFeatureLayer.java#render` (AvatarFeatureLayer) | RenderLayer<AvatarEntity, PlayerModel<Av | trivial | per visible entity or player | OK |
| `client/avatar/AvatarRenderer.java#render` (AvatarRenderer) | ZogratisRenderer<DevilAvatarBoss> | trivial | per visible entity or player | OK |
| `client/avatar/ForgeControls.java#render` (ForgeControls) |  | loop | per visible entity or player | OK |
| `client/avatar/IdentityPage.java#render` (IdentityPage) | ForgePage | strings | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/avatar/PlayerAvatarRenderer.java#render` (PlayerAvatarRenderer) | HumanoidMobRenderer<AvatarEntity, Player | trivial | per visible entity or player | OK |
| `client/avatar/SkillsPage.java#render` (SkillsPage) | ForgePage | strings, loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/avatar/SoulForgeScreen.java#render` (SoulForgeScreen) | AbstractContainerScreen<SoulForgeMenu> | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/avatar/TraitsPage.java#render` (TraitsPage) | ForgePage | strings, loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/celestial/SpiritKingRenderer.java#render` (SpiritKingRenderer) | EntityRenderer<SpiritKingEntity> | trivial | per visible entity or player | OK |
| `client/compat/CompatCodexScreen.java#render` (CompatCodexScreen) | Screen | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/corpse/CorpseDecayLayer.java#render` (CorpseDecayLayer) | RenderLayer<T, M> | trivial | per visible entity or player | OK |
| `client/corpse/NamedCorpseRenderer.java#render` (NamedCorpseRenderer) | ZogratisRenderer<NamedCorpseEntity> | trivial | per visible entity or player | OK |
| `client/dungeon/BasiliskRenderer.java#render` (BasiliskRenderer) | EntityRenderer<BasiliskEntity> | trivial | per visible entity or player | OK |
| `client/dungeon/DirewolfRenderer.java#render` (DirewolfRenderer) | EntityRenderer<DirewolfEntity> | trivial | per visible entity or player | OK |
| `client/elite/hub/DungeonMapScreen.java#render` (DungeonMapScreen) | Screen | strings, loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/elite/hub/QuestOverlay.java#render` (QuestOverlay) |  | strings, loop | per visible entity or player | OK: at most five quests, one clipped string each |
| `client/elite/hub/QuestScreen.java#render` (QuestScreen) | AbstractContainerScreen<HubMenus.QuestMe | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/elite/hub/ShopScreen.java#render` (ShopScreen) | AbstractContainerScreen<HubMenus.ShopMen | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/floor/charybdis/CharybdisSpawnRenderer.java#render` (CharybdisSpawnRenderer) | ZogratisRenderer<CharybdisSpawnBoss> | trivial | per visible entity or player | OK |
| `client/floor/charybdis/MagiculeScaleRenderer.java#render` (MagiculeScaleRenderer) | EntityRenderer<MagiculeScaleEntity> | trivial | per visible entity or player | OK |
| `client/floor/goliath/GoliathRenderer.java#render` (GoliathRenderer) | ZogratisRenderer<GoliathBoss> | trivial | per visible entity or player | OK |
| `client/grimoire/GrimoireSlotScreen.java#render` (GrimoireSlotScreen) | AbstractContainerScreen<GrimoireSlot.Men | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/heroweapon/HeroHud.java#render` (HeroHud) |  | trivial | per visible entity or player | OK |
| `client/heroweapon/carriage/CarriageRenderer.java#render` (CarriageRenderer) | EntityRenderer<CarriageEntity> | trivial | per visible entity or player | OK |
| `client/heroweapon/curse/CurseOverlay.java#render` (CurseOverlay) |  | trivial | per visible entity or player | OK |
| `client/heroweapon/forms/WeaponBookScreen.java#render` (WeaponBookScreen) | Screen | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/labyrinth/foundry/FoundryRenderer.java#render` (FoundryRenderer) | ZogratisRenderer<FoundryMobEntity> | trivial | per visible entity or player | FIXED: per-mob kit cached (no strings per frame) [probe c.foundry.render] |
| `client/labyrinth/rift/DimensionalRiftRenderer.java#render` (DimensionalRiftRenderer) |  | loop | per visible entity or player | OK |
| `client/labyrinth/vault/RelicPedestalRenderer.java#render` (RelicPedestalRenderer) |  | trivial | per visible entity or player | OK |
| `client/mega/system/FloorCardClient.java#render` (FloorCardClient) |  | loop | per visible entity or player | OK |
| `client/mega/system/SystemEditScreen.java#render` (SystemEditScreen) | Screen | strings | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/mega/system/SystemOverlay.java#render` (SystemOverlay) |  | trivial | per visible entity or player | OK |
| `client/mega/system/SystemStatsScreen.java#render` (SystemStatsScreen) | Screen | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/mode/ModeArmorLayer.java#render` (ModeArmorLayer) | RenderLayer<AbstractClientPlayer, Player | trivial | per visible entity or player | FIXED: library key built once per mode [probe c.mode.armor_layer] |
| `client/multiverse/FourKingdomsScreen.java#render` (FourKingdomsScreen) | Screen | strings | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/multiverse/MultiverseStatusScreen.java#render` (MultiverseStatusScreen) | Screen | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/playermodel/ExtrasLayer.java#render` (ExtrasLayer) | RenderLayer<T, PlayerModel<T>> | trivial | per visible entity or player | OK |
| `client/playermodel/GlowLayer.java#render` (GlowLayer) | RenderLayer<T, PlayerModel<T>> | trivial | per visible entity or player | OK |
| `client/playermodel/HairLayer.java#render` (HairLayer) | RenderLayer<T, PlayerModel<T>> | inventory | per visible entity or player | OK |
| `client/playermodel/KitCapeLayer.java#render` (KitCapeLayer) | RenderLayer<T, PlayerModel<T>> | inventory | per visible entity or player | OK |
| `client/playermodel/KitPlayerModel.java#setupAnim` (KitPlayerModel) | PlayerModel<T> | trivial | per visible entity or player | OK |
| `client/playermodel/KitRelicLayer.java#render` (KitRelicLayer) | RenderLayer<T, PlayerModel<T>> | curios, streams | per visible entity or player | OK: the "collect" is WornExec.collect (a plain method, no stream); reads the 10-tick relic cache; allocation free |
| `client/playermodel/PlayerModelRegistry.java#render` (Split) | EntityRenderer<T> | trivial | per visible entity or player | OK |
| `client/playermodel/PlayerModelRenderer.java#render` (PlayerModelRenderer) | EntityRenderer<T> | trivial | per visible entity or player | OK |
| `client/playermodel/PlayerModelRenderer.java#render` (PlayerModelRenderer) | EntityRenderer<T> | trivial | per visible entity or player | OK |
| `client/playermodel/ZagredAuraLayer.java#render` (ZagredAuraLayer) | RenderLayer<ZagredBossEntity, PlayerMode | trivial | per visible entity or player | OK |
| `client/prop/MagicPropRenderer.java#render` (MagicPropRenderer) | EntityRenderer<MagicPropEntity> | trivial | per visible entity or player | OK |
| `client/relics/HumanoidRelicLayer.java#render` (HumanoidRelicLayer) | RenderLayer<T, M> | streams | per visible entity or player | OK: the "collect" is WornExec.collect (no stream); the candidate test is allocation free and exits for every entity without a relic [probe c.relic.humanoid_layer] |
| `client/relics/RelicWornLayer.java#render` (RelicWornLayer) | RenderLayer<AbstractClientPlayer, Player | curios, streams, loop | per visible entity or player | FIXED: box quads without arrays; glow location cached [probe c.relic.worn_layer] |
| `client/relics/djinn/DjinnArmorLayer.java#render` (DjinnArmorLayer) | RenderLayer<AbstractClientPlayer, Player | trivial | per visible entity or player | OK [probe c.djinn.armor_layer] |
| `client/relics/djinn/DjinnScreenFx.java#render` (DjinnScreenFx) |  | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/relics/djinn/gui/DjinnGui.java#render` (DjinnGui) |  | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/relics/forge/ForgeScreen.java#render` (ForgeScreen) | AbstractContainerScreen<RelicForgeMenu> | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/relics/order/GodOfOrderRenderer.java#render` (GodOfOrderRenderer) | ZogratisRenderer<GodOfOrder> | trivial | per visible entity or player | OK |
| `client/relics/worn/ArtifactMimicRenderer.java#render` (ArtifactMimicRenderer) |  | trivial | per visible entity or player | OK |
| `client/relics/worn/DanteGravityCrownRenderer.java#render` (DanteGravityCrownRenderer) |  | trivial | per visible entity or player | OK |
| `client/relics/worn/ZenonSpatialRingRenderer.java#render` (ZenonSpatialRingRenderer) |  | trivial | per visible entity or player | OK |
| `client/remodel/AbyssalRenderer.java#render` (AbyssalRenderer) | KitMobRenderer<LivingArmorEntity> | trivial | per visible entity or player | FIXED: spec cached per look and phase |
| `client/remodel/KitBossRenderer.java#render` (KitBossRenderer) | ZogratisRenderer<T> | trivial | per visible entity or player | OK |
| `client/remodel/KitCloudRenderer.java#render` (KitCloudRenderer) | EntityRenderer<CottonCloudEntity> | loop | per visible entity or player | OK |
| `client/remodel/KitMobRenderer.java#render` (KitMobRenderer) | ZogratisRenderer<T> | trivial | per visible entity or player | OK |
| `client/skilltree/shop/ShopScreen.java#render` (ShopScreen) | Screen | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/song/SongClient.java#render` (SongClient) |  | trivial | per visible entity or player | OK |
| `client/soul/PaladinLayer.java#render` (PaladinLayer) | RenderLayer<T, M> | trivial | per visible entity or player | OK |
| `client/thread/RougeModel.java#render` (RougeModel) |  | trivial | per visible entity or player | OK |
| `client/thread/RougeRenderer.java#render` (RougeRenderer) | EntityRenderer<RougeEntity> | trivial | per visible entity or player | OK |
| `client/thread/ThreadTrapRenderer.java#render` (ThreadTrapRenderer) | EntityRenderer<ThreadTrapEntity> | loop | per visible entity or player | OK |
| `client/ui/CodexPages.java#render` (CodexPages) |  | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/CodexPages.java#render` (CodexPages) |  | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/CodexPages.java#render` (CodexPages) |  | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/CodexPages.java#render` (CodexPages) |  | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/CodexScreen.java#render` (CodexScreen) | Screen | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/GuiSizeScreen.java#render` (GuiSizeScreen) | Screen | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/codex/EntryListPage.java#render` (EntryListPage) |  | loop | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/ui/theme/PageUnlockOverlay.java#render` (PageUnlockOverlay) |  | trivial | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/wizard/QuestionnaireScreen.java#render` (QuestionnaireScreen) | Screen | strings | per visible entity or player | OK: screen or HUD page, drawn only while open |
| `client/wizard/TowerWizardRenderer.java#render` (TowerWizardRenderer) | EntityRenderer<TowerWizardEntity> | trivial | per visible entity or player | OK |
| `client/zogratis/ZogratisBeastRenderer.java#render` (ZogratisBeastRenderer) | ZogratisRenderer<BloodBeastEntity> | trivial | per visible entity or player | OK |
| `client/zogratis/ZogratisBossRenderer.java#render` (ZogratisBossRenderer) | ZogratisRenderer<T> | trivial | per visible entity or player | OK |
| `client/zogratis/ZogratisCloneRenderer.java#render` (ZogratisCloneRenderer) | ZogratisRenderer<ZogratisCloneEntity> | trivial | per visible entity or player | OK |
| `client/zogratis/ZogratisDiscipleRenderer.java#render` (ZogratisDiscipleRenderer) | ZogratisRenderer<DarkDiscipleEntity> | trivial | per visible entity or player | OK |
| `vfx/client/FxRenderer.java#render` (FxRenderer) |  | loop | per visible entity or player | FIXED: cull, cached piece textures, scratch vectors |
| `vfx/client/VfxManager.java#render` (VfxManager) |  | strings, loop | per visible entity or player | FIXED: level of detail by distance, vertex budget of the frame, cull, per-layer probe |
| `vfx/client/layer/AirLayer.java#render` (AirLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/AntiMagicLayer.java#render` (AntiMagicLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/ArcaneSpellLayer.java#render` (ArcaneSpellLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ArcaneSpellLayer2.java#render` (ArcaneSpellLayer2) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/AshLayer.java#render` (AshLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/AuraLayer.java#render` (AuraLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BarrierLayer.java#render` (BarrierLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BeastLayer.java#render` (BeastLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BlackOilLayer.java#render` (BlackOilLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BloodLayer.java#render` (BloodLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BodyLayer.java#render` (BodyLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BoneLayer.java#render` (BoneLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BornMagicLayer.java#render` (BornMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BriarLayer.java#render` (BriarLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BronzeLayer.java#render` (BronzeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/BubbleLayer.java#render` (BubbleLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ButohLayer.java#render` (ButohLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CastHoldLayer.java#render` (CastHoldLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ChainLayer.java#render` (ChainLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CherryBlossomLayer.java#render` (CherryBlossomLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CommanderBossLayer.java#render` (CommanderBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CompassLayer.java#render` (CompassLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CopperLayer.java#render` (CopperLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CorpseLayer.java#render` (CorpseLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CorundumLayer.java#render` (CorundumLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CrystalLayer.java#render` (CrystalLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CurseLayer.java#render` (CurseLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/CurseWardingLayer.java#render` (CurseWardingLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DarkFxLayer.java#render` (DarkFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DarkMagicLayer.java#render` (DarkMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DarkSlashLayer.java#render` (DarkSlashLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonBeastLayer.java#render` (DemonBeastLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonFireLayer.java#render` (DemonFireLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonIceLayer.java#render` (DemonIceLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonLightLayer.java#render` (DemonLightLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonSlayerLayer.java#render` (DemonSlayerLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DemonWaterLayer.java#render` (DemonWaterLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DevilBossLayer.java#render` (DevilBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DiceFxLayer.java#render` (DiceFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DiceLayer.java#render` (DiceLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/DreamPaintLayer.java#render` (DreamPaintLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/EarthFamilyLayer.java#render` (EarthFamilyLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/EarthSpellLayer.java#render` (EarthSpellLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ElementIdentityLayer.java#render` (ElementIdentityLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/ElementShapesLayer.java#render` (ElementShapesLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/EyeLayer.java#render` (EyeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/EyeballLayer.java#render` (EyeballLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/FireSpellLayer.java#render` (FireSpellLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/FlameFxLayer.java#render` (FlameFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/FlameMagicLayer.java#render` (FlameMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/FoodLayer.java#render` (FoodLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/FungusLayer.java#render` (FungusLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/GameLayer.java#render` (GameLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/GelLayer.java#render` (GelLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/GlassLayer.java#render` (GlassLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/GravityLayer.java#render` (GravityLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/HairLayer.java#render` (HairLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/IceLayer.java#render` (IceLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/IceWedgeLayer.java#render` (IceWedgeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ImitationLayer.java#render` (ImitationLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/IronLayer.java#render` (IronLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/JuggernautBossLayer.java#render` (JuggernautBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/KeyLayer.java#render` (KeyLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/KotodamaFxLayer.java#render` (KotodamaFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/KotodamaLayer.java#render` (KotodamaLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/LegionLayer.java#render` (LegionLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/LightLayer.java#render` (LightLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/LightTreeLayer.java#render` (LightTreeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/LightningFxLayer.java#render` (LightningFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/LuciusCoreGemLayer.java#render` (LuciusCoreGemLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/MagicCircleLayer.java#render` (MagicCircleLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/ManaZoneLayer.java#render` (ManaZoneLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MemoryFxLayer.java#render` (MemoryFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MercuryLayer.java#render` (MercuryLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MirrorFxLayer.java#render` (MirrorFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MirrorLayer.java#render` (MirrorLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ModificationLayer.java#render` (ModificationLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MucusLayer.java#render` (MucusLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/MudLayer.java#render` (MudLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/NailLayer.java#render` (NailLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/OrderBossLayer.java#render` (OrderBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/PaintStudioLayer.java#render` (PaintStudioLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/PaintingLayer.java#render` (PaintingLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/PermeationFxLayer.java#render` (PermeationFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/PlantLayer.java#render` (PlantLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/PoisonPlantLayer.java#render` (PoisonPlantLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/RecombinationLayer.java#render` (RecombinationLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/RuneCircleLayer.java#render` (RuneCircleLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ScaleLayer.java#render` (ScaleLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SealLayer.java#render` (SealLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SealSecreLayer.java#render` (SealSecreLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ShakudoLayer.java#render` (ShakudoLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SlashCompassMercuryLayer.java#render` (SlashCompassMercuryLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SlashLayer.java#render` (SlashLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/SlashRipFxLayer.java#render` (SlashRipFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SmokeLayer.java#render` (SmokeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SnowLayer.java#render` (SnowLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SongFxLayer.java#render` (SongFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SoulCorpseFxLayer.java#render` (SoulCorpseFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SoulFxLayer.java#render` (SoulFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SoundFxLayer.java#render` (SoundFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SpatialFxLayer.java#render` (SpatialFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/SpatialPortalLayer.java#render` (SpatialPortalLayer) |  | loop | per visible entity or player | OK |
| `vfx/client/layer/SpikeLayer.java#render` (SpikeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/StoneBossLayer.java#render` (StoneBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ThreadFateLayer.java#render` (ThreadFateLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/TideBossLayer.java#render` (TideBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/TimeMagicLayer.java#render` (TimeMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/TransmutationLayer.java#render` (TransmutationLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/TreeLayer.java#render` (TreeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/VineLayer.java#render` (VineLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/VortexLayer.java#render` (VortexLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WaterFxLayer.java#render` (WaterFxLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WaterMagicLayer.java#render` (WaterMagicLayer) | AbstractVfxLayer | loop | per visible entity or player | OK |
| `vfx/client/layer/WaterSpellLayer.java#render` (WaterSpellLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WikiMagicLayer.java#render` (WikiMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WindMagicLayer.java#render` (WindMagicLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WindSpellLayer.java#render` (WindSpellLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WingLayer.java#render` (WingLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/WorldTreeLayer.java#render` (WorldTreeLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ZagredBossLayer.java#render` (ZagredBossLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ZogratisBloodLayer.java#render` (ZogratisBloodLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ZogratisDivineLayer.java#render` (ZogratisDivineLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ZogratisGravityLayer.java#render` (ZogratisGravityLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |
| `vfx/client/layer/ZogratisSpaceLayer.java#render` (ZogratisSpaceLayer) | AbstractVfxLayer | trivial | per visible entity or player | OK |

#### D. VFX layers (vfx/client/layer)

| VFX layer | Shapes | Vertex budget | Verdict |
|---|---|---|---|
| `AirLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.AirLayer) |
| `AntiMagicLayer` | 1 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.AntiMagicLayer) |
| `ArcaneSpellLayer` | 10 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ArcaneSpellLayer) |
| `AshLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.AshLayer) |
| `AuraLayer` | 4 | 400 | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.AuraLayer) |
| `BarrierLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BarrierLayer) |
| `BeastLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BeastLayer) |
| `BlackOilLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BlackOilLayer) |
| `BloodLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BloodLayer) |
| `BodyLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BodyLayer) |
| `BoneLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BoneLayer) |
| `BornMagicLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BornMagicLayer) |
| `BriarLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BriarLayer) |
| `BronzeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BronzeLayer) |
| `BubbleLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.BubbleLayer) |
| `ButohLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ButohLayer) |
| `CastHoldLayer` | ? | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CastHoldLayer) |
| `ChainLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ChainLayer) |
| `CherryBlossomLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CherryBlossomLayer) |
| `CommanderBossLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CommanderBossLayer) |
| `CompassLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CompassLayer) |
| `CopperLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CopperLayer) |
| `CorpseLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CorpseLayer) |
| `CorundumLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CorundumLayer) |
| `CrystalLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CrystalLayer) |
| `CurseLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CurseLayer) |
| `CurseWardingLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.CurseWardingLayer) |
| `DarkFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DarkFxLayer) |
| `DarkMagicLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DarkMagicLayer) |
| `DarkSlashLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DarkSlashLayer) |
| `DemonBeastLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonBeastLayer) |
| `DemonFireLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonFireLayer) |
| `DemonIceLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonIceLayer) |
| `DemonLightLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonLightLayer) |
| `DemonSlayerLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonSlayerLayer) |
| `DemonWaterLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DemonWaterLayer) |
| `DevilBossLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DevilBossLayer) |
| `DiceFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DiceFxLayer) |
| `DiceLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DiceLayer) |
| `DreamPaintLayer` | 8 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.DreamPaintLayer) |
| `EarthFamilyLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.EarthFamilyLayer) |
| `EarthSpellLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.EarthSpellLayer) |
| `ElementIdentityLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ElementIdentityLayer) |
| `ElementShapesLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ElementShapesLayer) |
| `EyeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.EyeLayer) |
| `EyeballLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.EyeballLayer) |
| `FireSpellLayer` | 6 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.FireSpellLayer) |
| `FlameFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.FlameFxLayer) |
| `FlameMagicLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.FlameMagicLayer) |
| `FoodLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.FoodLayer) |
| `FungusLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.FungusLayer) |
| `GameLayer` | 1 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.GameLayer) |
| `GelLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.GelLayer) |
| `GlassLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.GlassLayer) |
| `GravityLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.GravityLayer) |
| `HairLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.HairLayer) |
| `IceLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.IceLayer) |
| `IceWedgeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.IceWedgeLayer) |
| `ImitationLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ImitationLayer) |
| `IronLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.IronLayer) |
| `JuggernautBossLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.JuggernautBossLayer) |
| `KeyLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.KeyLayer) |
| `KotodamaFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.KotodamaFxLayer) |
| `KotodamaLayer` | 6 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.KotodamaLayer) |
| `LegionLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.LegionLayer) |
| `LightLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.LightLayer) |
| `LightTreeLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.LightTreeLayer) |
| `LightningFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.LightningFxLayer) |
| `LuciusCoreGemLayer` | 1 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.LuciusCoreGemLayer) |
| `MagicCircleLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MagicCircleLayer) |
| `ManaZoneLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ManaZoneLayer) |
| `MemoryFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MemoryFxLayer) |
| `MercuryLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MercuryLayer) |
| `MirrorFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MirrorFxLayer) |
| `MirrorLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MirrorLayer) |
| `ModificationLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ModificationLayer) |
| `MucusLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MucusLayer) |
| `MudLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.MudLayer) |
| `NailLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.NailLayer) |
| `OrderBossLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.OrderBossLayer) |
| `PaintStudioLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.PaintStudioLayer) |
| `PaintingLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.PaintingLayer) |
| `PermeationFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.PermeationFxLayer) |
| `PlantLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.PlantLayer) |
| `PoisonPlantLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.PoisonPlantLayer) |
| `RecombinationLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.RecombinationLayer) |
| `RuneCircleLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.RuneCircleLayer) |
| `ScaleLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ScaleLayer) |
| `SealLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SealLayer) |
| `SealSecreLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SealSecreLayer) |
| `ShakudoLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ShakudoLayer) |
| `SlashCompassMercuryLayer` | 9 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SlashCompassMercuryLayer) |
| `SlashLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SlashLayer) |
| `SlashRipFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SlashRipFxLayer) |
| `SmokeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SmokeLayer) |
| `SnowLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SnowLayer) |
| `SongFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SongFxLayer) |
| `SongNotesLayer` | 1 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SongNotesLayer) |
| `SongRingLayer` | 1 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SongRingLayer) |
| `SoulCorpseFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SoulCorpseFxLayer) |
| `SoulFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SoulFxLayer) |
| `SoundFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SoundFxLayer) |
| `SpatialFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SpatialFxLayer) |
| `SpatialPortalLayer` | ? | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SpatialPortalLayer) |
| `SpikeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.SpikeLayer) |
| `StoneBossLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.StoneBossLayer) |
| `ThreadFateLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ThreadFateLayer) |
| `TideBossLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.TideBossLayer) |
| `TimeMagicLayer` | 4 | 400 (default) | exempt from the level of detail (the Time Magic standard) |
| `TransmutationLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.TransmutationLayer) |
| `TreeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.TreeLayer) |
| `VineLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.VineLayer) |
| `VortexLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.VortexLayer) |
| `WaterFxLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WaterFxLayer) |
| `WaterMagicLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WaterMagicLayer) |
| `WaterSpellLayer` | 6 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WaterSpellLayer) |
| `WikiMagicLayer` | ? | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WikiMagicLayer) |
| `WindMagicLayer` | 2 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WindMagicLayer) |
| `WindSpellLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WindSpellLayer) |
| `WingLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WingLayer) |
| `WorldTreeLayer` | 3 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.WorldTreeLayer) |
| `ZagredBossLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ZagredBossLayer) |
| `ZogratisBloodLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ZogratisBloodLayer) |
| `ZogratisDivineLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ZogratisDivineLayer) |
| `ZogratisGravityLayer` | 5 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ZogratisGravityLayer) |
| `ZogratisSpaceLayer` | 4 | 400 (default) | OK: drawn through VfxManager.render (distance LOD, cull, frame budget, probe c.vfx.layer.ZogratisSpaceLayer) |

<!-- inventory:end -->

## 8. Seen and deliberately left alone

| Item | Why not |
|---|---|
| `CuriosLink.flags(entity)` per frame per wearer (reflection `invoke` x3, list and record per stack) | A cache would make the Curios "eye icon" react up to a few ticks late. About 2 to 4 us per wearer per frame; the relic worn layer already skips wearers beyond 32 blocks. Candidate for a second pass if the probe (`c.relic.scan`) shows it |
| `LazyBuffers` (aura painters ask the buffer source for their builder at every vertex) | The wrapper exists to avoid the "Not building!" crash (1.45.0); a cache of the consumer is unsafe if any painter draws through the vanilla source in between |
| `VfxVertexBuffer.flush` / `FxRenderer`: `getTexture(...).setFilter(true, false)` per batch per frame, one draw call per FX piece | the aura render types set the same textures to nearest between frames, so a "already set" cache would show the wrong filter; FX pieces differ in shader uniforms, so they cannot share a draw call without changing the shader |
| `SystemOverlay` rebuilds and lays out its panel every frame | only in the dungeon; the panel carries a time-based shimmer, so a cache would change the animation |
| `GrimoireSlot.onPlayerTick` stack compare every tick | it is the change detector of the hip-book sync; an identity shortcut would miss in-place component edits |
| `ArmorItems.of` registry key per armour slot per visible player per frame | one hash lookup each (about 20 ns) |
| `SpellSeal.shimmer` sends ten one-particle packets per call | one packet with a count would change the random positions |
| `ThreadTrapEntity.tick` two box queries per tick | the projectile test must run every tick (a bolt moves up to 3 blocks per tick), the touch test already runs every second tick |
| `DemonSlayerRenderer.GameSink.quad` (about 400 B per quad: a normal, a corner table, a vector and a `float[12]` per corner) and the procedural `DemonSlayerMesh.draw` | the Genesis Demon-Slayer is the owner's visual standard (rule 7); the geometry is generated per frame by design, so a cache needs a visual review in game; recorded for a second pass with the preview tool `tools/item_preview/preview_weapons.py` |
| `Wave*`, `Skill*`, `Spell*` cast-time scans (about 520 box queries in the whole mod) | cast-time work, once per cast; the continuous ones are in table B and are throttled |
| Mixin on `ServerLevel#sendParticles` for a global particle cap | would also throttle vanilla and other mods; the trail emitters of this mod use `PerfParticles` instead |
| Equip animation math (`EquipAnimation`, the Djinn's own) | ten float operations per piece per frame; a table would cost more than it saves |

## 9. Not touched (other builders' lanes) for a second pass

* Erza Requip armour files, `labyrinth/dnd/economy` (hunger, `DungeonHunger.onPlayerTick`: the code says every 20 ticks), `audio/**`, `ModSounds`: not read.
* `labyrinth/mega/enc/**` (Encounter Director sweep / hunt, Wanderer Director, depth mobs; `GoalAccess` uses reflection: confirm the `Field` is cached), squad / loot packages.
* `labyrinth/mega/decennial/**`, `labyrinth/mega/rift/**` (`RiftService.onServerTick` and `DimensionalRiftBlockEntity.tick` each run a box query: confirm they are throttled), rift renderer.
* Spirit models and renderers: `client/SpiritLordRenderer` builds three locations per call (`getGeoModel`, `getTextureLocation`, `getAnimationResource`) on every frame per spirit; `ElementalSpiritRenderer`,
  `WindSpiritLordRenderer`, `SpiritSlots`, `SpiritBond.onPlayerTick`, the Spirit Dive painters.

## 10. What could not be verified

* **Tests**: 941 test classes were compiled and run one by one against a minimal JUnit shim: 873 passed, 0 failed, 68 could not be compiled without the real Minecraft / JUnit jars (they were not run, by me or before). The eight new classes (`perf/*Test`, `GeoDrawCacheTest`) are among the passing ones. `gradlew build` was not run.
* **Nothing else was run**: no game, no profiler, no benchmark. Every number in section 5 is a static model; the JIT may already remove part of the allocations, and the CPU figures are upper bounds.
* The type check is against API stubs (the five `RenderSystem.setShader` / `setShaderTexture` lines that the stub does not model are old lines, unchanged); `perf.core` and the commands have not run in a real server.
* Command merging relies on Brigadier merging two registrations of the literal `multiverse` (documented behaviour of `CommandNode.addChild`); the `/nusmp perf` client command relies on `RegisterClientCommandsEvent`.
* The config files rely on `ModConfigSpec` / `ModConfigEvent` of NeoForge 21.1 (stubs only); a safety refresh every 100 ticks copies the values even if no reload event arrives.
* The level-of-detail distances, the budgets (160 effect packets, 2000 particles, 400 client particles) and the quality profiles are reasoned defaults; the owner should look at `/nusmp perf report` in a busy scene and adjust.
* Visual identity at `high` is argued (the same floats, the same order, the same batch order) and tested for the arithmetic; the only intended visual differences are the far bands: effects beyond 96 blocks get 3/4 or 1/2 of their segments, beyond 256 are skipped, and a player's third and later auras are not painted between 48 and 96 blocks.

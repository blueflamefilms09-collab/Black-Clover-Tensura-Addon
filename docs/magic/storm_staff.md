# Storm Magic (Fairy Tail): the Stormcaller Staff (round 1.99.0, key `storm_staff`)

Owner request: *"make storm magic feel more like storms and less like wind and lightning, also it's a Fairy Tail Magic, not Black Clover Magic, so it should not be a grimoire, make it tied to a magic staff"* (https://fairytail.fandom.com/wiki/Storm_Magic).

Nothing here was seen in a game. Every picture is the project's headless rasteriser (see "Not verified").

## 1. What it is now

Storm Magic is a **Fairy Tail Caster magic tied to a staff**: the **Stormcaller Staff** (`nusmp:stormcaller_staff`), a real 3D weapon (a lacquered slate-blue staff wound with a silver wire of gust, a silver crown cup holding a thundercloud round a glowing storm core, four horns of wind, a lightning rod with fins and a glowing tip, six drops of rain crystal, a glowing bolt inlaid down the shaft). Hold it and **use** to cast the chosen storm spell, **sneak + use** to turn to the next spell. The spells come out of the staff's sky: each one is a **storm**.

A storm is not wind plus a spark. It is a small weather system with an area, a time and a slow drift:

* a **cloud deck** (two layers of billowing puffs, dark belly, lighter towering top) that builds, rolls and dies away over the cell;
* **rain curtains** that scroll down from the deck, splashing on the ground;
* **hail** in volleys: stones with trails drop and burst into ice where they land;
* **forked bolts** that leave the cloud with a flash inside it and strike a mark (real server damage, no vanilla lightning entity: no fire, no charged creepers);
* **gust rings** that roll out of a big cell and shove everything outward; a **gale front** (a wall of wind) and a **storm surge** (a wall of water with a foaming crest) that run along the ground;
* a **funnel** that walks across the ground and draws things in; the calm **eye** of the big storm where friends are looked after.

The server decides everything (where it is, how long, who is hurt, how much); the clients draw it from a few small effect packets and the same functions of (seed, age) the server uses to hurt things, so a stone's ice breaks where its damage lands and a bolt's flash is where its damage is.

## 2. The spells (the Fairy Tail mapping)

Fairy Tail's Storm Magic is the magic of storms: summoning storm clouds, rain, hail, thunder and lightning out of the cloud, gales and tornado-like winds, surges of storm water. **The wiki page could not be opened in this session** (no network to fandom.com; a web search only found passing mentions), so the eight spells below are mapped from that description, **not copied from the wiki's own spell names**: the names are this mod's. See section 8.

| # | Spell (`id`) | Storm | Parts | Aim / size / time | Cost (of max Aura) | Cooldown | Raw damage (before the balance law) |
|---|---|---|---|---|---|---|---|
| 0 | Storm Cloud: Gathering (`gathering_clouds`) | a cell rolls in where you look, drifting the way you faced | rain, bolts | 26 / r 7 / 8 s, drift 1.6 b/s | 5% | 12 s | bolt 8 (up to 5 bolts), rain patter 1.2 |
| 1 | Downpour (`downpour`) | heavy rain: slows, weakens, puts out fires | rain | 24 / r 8 / 7 s | 4% | 8 s | patter 2.2 |
| 2 | Hailstorm (`hailstorm`) | volleys of hail stones | hail | 24 / r 6.5 / 6 s | 6% | 12 s | stone 3.4 |
| 3 | Thunderhead (`thunderhead`) | a black cloud throws bolt after bolt at the foes under it, each leaping to those near the mark | bolts, rain | 26 / r 6 / 7 s | 8% | 15 s | bolt 9 (up to 7 bolts), 60% to the others at the mark |
| 4 | Gale Front (`gale_front`) | a wall of wind sweeps out in front of you and shoves everything on its line | front | length 20 / half width 5.5 / 2 s | 4% | 7 s | 5.5 once, push 1.15 |
| 5 | Storm Surge (`storm_surge`) | a roll of storm water runs along the ground | surge | length 18 / half width 3 / 2 s | 7% | 13 s | 11 once, push 0.95, slowed |
| 6 | Twister (`twister`) | a funnel walks across the ground, draws in and lifts, then throws out | funnel | 22 / r 3.4 / 6 s, drift 3.2 b/s | 9% | 16 s | pulse 2.4 |
| 7 | Eye of the Storm (`eye_of_the_storm`) | the whole sky on the spot you stand: rain, hail, bolts, gust rings; a calm eye where you and friends get Resistance | rain, hail, bolts, gust, eye | on you / r 12 / 12 s, drift 0.6 b/s | 16% | 45 s | bolt 12 (8), stone 3, patter 1.2 |

Cost is paid from the Aura first and from the magicules when the Aura is short (the rule of the Fairy Tail skills) and scaled by the mana law (`ManaLaw.costMult`: stage, affinity, an open Mana Zone, exhaustion). It never touches the food bar. Cooldowns are per player and per spell, scaled by the game rule `spellCooldown` and the mana law. Damage goes through `BalanceLaw.damage` (EP scaling, the PvP hit cap) times the server's `storm.damageScale`.

## 3. Rules that keep a storm from being a damage faucet

* One creature takes at most **3 bolts, 8 hail stones and 6 rain/funnel pulses** from ONE storm, with gaps of 12, 6 and 20 ticks between two of a kind (`StormHits`). A front or a surge hits each creature once.
* The worst case a spell can deal to one creature, cast again the moment it is ready, is **at most 3 raw damage a second** (a test; the Fairy Tail skills' close blows sustain 2 to 4): see the table of the test `StormSpellTest`.
* Storms hurt only foes (never the caster, allies, pets, summons, creative or spectating players, armour stands; players only if `storm.hurtPlayers` and the server's PvP rules allow it). Nothing changes a block, spawns an entity or drains hunger.

## 4. NEW / REPLACED / KEPT

| | |
|---|---|
| NEW | `fairytail/storm/*` (the staff item, the storm field, the spell table, the budgets, the config, the gate, the self-test); `vfx/client/layer/StormLayer`, `client/storm/StormClientFx`; five effect shapes `STORM_CLOUD, STORM_FALL, STORM_BOLT, STORM_FRONT, STORM_CANCEL` (appended to `VfxShape`); the textures `textures/particle/storm_{cloud,rain,bolt,hail,wave,funnel}.png` (`tools/gen_storm_staff_textures.py`); the 3D staff (`tools/gen_storm_staff_model.py`, mesh, skin, glow skin, icon, models); Better Combat staff file; server config `nusmp-storm-server.toml`; preview scenes `tools/vfx_preview/scenes.d/storm_staff.json`; this page; tests; lang fragment `tools/lang/storm_staff.json` |
| REPLACED | where Storm Magic comes from: it is no longer rolled as a grimoire magic (Wind soul pool, starter pool, wizard tower) while `storm.staffOnly` is true (default). Its magic UI name is now "Storm Magic (Fairy Tail)" (profile override of the display name; lang override of `nusmp.skill.book_storm` / `grimoire_storm`). The old effect (`magic.STORM` lance + wind ring) is not used by the staff |
| KEPT | the Storm grimoire `book_storm` with its four pages and `vortex_shield`, the `STORM` enum constant, the grimoire skill `grimoire_storm`, the creative-tab grimoire, the Kaiser Granvorka captain book and its lock (harmless), every old save and every grimoire already of Storm Magic. `storm.staffOnly = false` restores the old rolling exactly. `MagicKeys.RESTRICTED` is untouched |

## 5. Weapon (rules 6 and 7)

* Real 3D model drawn by `client/WeaponRenderer` (3,100 body quads and 1,900 glow quads, 42 named parts), the old-style 16 x 16 sprite rendered from the mesh is the inventory icon (`models/item/stormcaller_staff_icon.json`). Class `staff` (2.7 blocks in hand). Previews: `docs/previews/storm_staff/stormcaller_staff.png` (four views of the mesh) and `stormcaller_staff_in_hand.png` (icon, front, three-quarter, edge, back as placed in the hand).
* Grip (docs/weapon_grip.md): the mesh states its own `gripxyz 0 68 0` (the middle of the wrapped grip), golden row refreshed with `tools/weapon_grip/grip_table.py --write`.
* Tensura engravings (`item/WeaponEngravings`): elemental_boost 2, magic_weapon 1, barrier_piercing 1, swift 1. Franchise: Fairy Tail.
* Better Combat: `data/nusmp/weapon_attributes/stormcaller_staff.json` (profile `staff`, same as the Wisp Staff); manifest row added by text insertion (the rest of the manifest is stale in the repo and was NOT regenerated).
* Glow: an additive glow skin (storm core, rod tip, bolt inlay, cloud veins, rain drops), through the weapon renderer's own safe additive path; no new render type.

## 6. Server and multiplayer (rule 9)

* All state is on the server, keyed by player UUID (`StormCooldowns`, `StormCell.owner`); the client draws and asks (`use` does only the swing on the client).
* Storms are entity-less records ticked on `ServerTickEvent.Post`; they end when the owner logs out, dies, casts a newer storm over the cap, or the server stops; the clients are told with a `STORM_CANCEL` packet.
* No new packet type: storms travel as the existing `VfxPayload` (server to client, nearby players only, counted by the per-tick effect-packet budget).
* Config is a SERVER config `nusmp-storm-server.toml`: `staffOnly` (true), `maxStormsPerPlayer` (1; range 1..2), `maxStormsGlobal` (12; 1..64), `damageScale` (1.0), `maxRadius` (16), `maxDurationTicks` (400; up to 600), `hurtPlayers` (true). Nothing about looks is a server setting; the splash particles follow the client's existing `particles.custom` switch.
* CI self-test `magic.storm_staff` (below).

## 7. NO LAG (rule 11): the worst-case cost

**Server, per tick** (`StormBudget`, `StormField`; pinned by `StormBudgetTest` and `StormSourceScanTest`):

| piece | bound |
|---|---|
| storms alive | 1 per player (config up to 2), 12 on the server (up to 64) |
| work when no storm is alive | one list-size read |
| entity scans (rain, funnel, gust, eye) | every `max(4, entityScanInterval)` ticks per storm (default 5), staggered by storm number, **at most 6 scans per tick over every storm**; a refused scan is simply done at the next due tick |
| hail | one entity query per VOLLEY (not per stone, no block search per stone), **at most 3 volleys per tick** |
| bolts | **1 per tick and 6 per second over the whole server**, each with one query; a refused bolt is skipped (the cloud mutters), never queued |
| area query | a cylinder, cut at **16 creatures**, radius at most the config cap (16) |
| hits | per creature per storm 3 bolts + 8 stones + 6 pulses at most, with gaps; per bolt/volley at most 16 creatures |
| so in one tick | at most **6 + 3 + 1 = 10** entity queries, whatever the number of storms and players |
| probe | `s.storm.tick` (`PerfProbe`, in `PerfSourceScanTest.PROBES`) |

**Network**: a cast sends 2 effect packets (cloud, fall) or 1 (front/surge), about 60 bytes each, to players within 96 blocks; ending a storm early sends 1; a bolt is 1 packet and at most 6 a second server-wide. All go through `VfxSpawn.send` and so through the shared `maxEffectPacketsPerTick` budget (160). No particle packet is sent by the server for storms.

**Client draw** (`StormLayer`, measured with `tools/vfx_preview`): each effect stays under the 400 vertices of one activation: cloud 160 (funnel storm 312, eye storm 252 with its ring), rain/hail/gust 160, a bolt about 140, a front 144, a surge 100, at full detail; every segment count goes through `ctx.seg` (the quality level and the distance thin it), `VfxManager.plan` culls by distance and by the frame vertex allowance, and the active-effect cap of the quality level applies (a cast is 2 effects). **Nothing is allocated per frame**: scratch vectors, scalar maths (`StormMath.lerpAt`, `hailStoneX/Z`), bolt paths written into kept buffers, the cancelled-seed ring is a primitive array; a source scan test fails if a `new` appears in the render half of the layer.

**Client particles** (`StormClientFx`, custom `particle/fx` types only: `droplet`, `shard_ice`, `water_splash`, `leaf_green`): at most 2 storms emit per client tick, at most 14 particles per storm per tick, none beyond 40 blocks, thinned by the effect quality, through the shared `clientParticlesPerTick` budget (400), options cached (no allocation per spawn), nothing at all when `particles.custom` is off.

## 8. Tests

Pure JUnit (all run in the project's shim): `StormMathTest`, `StormSpellTest`, `StormLimitsTest` (caps, hit book, cooldowns), `StormBudgetTest`, `StormSourceScanTest` (rules 9 and 11, custom particles, no hunger, no lightning entity, no block change, Time Magic untouched, budgets and probe present, render half allocation-free), `StormAssetsTest` (model, skins, icon, mesh grip line, class rows, golden, Better Combat, franchise, engravings, textures incl. the bolt being black under alpha, lang). Existing tests re-run and passing in the shim: `GripTableTest`, `WeaponClassesTest`, `WeaponMeshesTest`, `WeaponSizesTest`, `WeaponAttributesTest`, `FranchiseDataTest`. `PerfSourceScanTest` gets one appended probe line. Self-test **`magic.storm_staff`** (CI smoke): table, staff cycling, the old grimoire registered and findable by name but not rolled for a Wind soul, a real cast by a stand-in player, the cooldown, the per-player cap, a thunderhead run tick by tick over a zombie (hurts it within the caps, no vanilla lightning entity), engravings applied.

Pictures (`docs/previews/storm_staff/`): the staff, the six storm effects (`python3 -B tools/vfx_preview/preview.py storm_gathering ...`; a scene may now list `"also"` shapes drawn into the same frames), the textures sheet.

## 9. Not verified (nothing could be run in a game)

* **The Fairy Tail wiki page was not read** (blocked). The spell set is mapped from the general description of the magic; if the wiki names specific spells, rename or add (the table is one enum, ordinals append-only).
* No game was started: the look of the cloud, rain, bolts, front and surge, their scale against real blocks, the draw order of overlapping alpha billboards, the weapon in a real hand (third and first person), the glow skin in the weapon renderer and the inventory sprite are judged from the headless rasteriser only.
* The cell is a flat disc at the ground height under the aim point: on a slope or under a roof the rain stops at that height and the cloud (9 blocks up) can clip through a ceiling in a cave. No block-aware placement.
* The staff-only gate was written against the code paths that roll a magic (soul pool, starter pool, wizard tower, `STARTER_MAGICS` config); a mod or admin command that picks `MagicType.STORM` by name still can.
* The "Storm Magic (Fairy Tail)" label shows wherever the profile's display name is used; text built straight from the enum's own `displayName` field, and the lang keys `nusmp.skill.book_storm` / `grimoire_storm` (the fragment must override the existing en_us entries when merged), are the other places. Old page descriptions of the grimoire still say "Storm Magic".
* `tools/bettercombat/manifest.json` and `weapon_classes.json` (tools copy) were already out of step with each other in the repo before this work; only the staff's rows were inserted.
* `VfxShape` ordinals changed only by appending; the self-test `visual.deck_summon` used to demand that `DECK_SUMMON` be the LAST shape and now demands its shipped ordinal (439) instead.
* Type-checked with the stub API dump (only the known "abstract" and `Direction.Axis` stub artefacts remain); not compiled by Gradle.
* The unit tests that need Minecraft classes (`MagicEnumAuditTest` and others) could not run in the shim; the change keeps them valid by not touching `MagicKeys.RESTRICTED` or any enum constant.

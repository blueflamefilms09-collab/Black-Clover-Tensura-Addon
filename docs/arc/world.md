# Where the characters appear (1.61.0, builder ARC-WORLD)

The core (`docs/arc/core.md`) gave the characters a roster, a boss layer and a story chain; this part gives them somewhere to be: the dungeon's
tenth-floor halls, a story arena of their own, and the Sanctuary Hub. It also puts the seven bosses the mod had before the arcs into the roster, pays a reward
for a cleared saga, and writes an advancement for every character. Nothing here was run in the game: section 12 lists exactly what is unverified, and
the owner's rule holds that nothing is called verified that was not run.

Sources: none outside this repository. Every API used is already used elsewhere in the mod and was checked against the API stubs of the build (section 11).
The ideas follow the mod's own Final Hour arena (`docs/lucius_arena.md`, `docs/lucius_arc.md`), the Guardian (`docs/labyrinth/mega_guard.md`), the Hub
(`docs/labyrinth/mega_hub.md`) and the Dungeon Master's wanderers (`docs/labyrinth/mega_enc.md`).

## 1. How it is started

`ArcWorldSystem.init(modEventBus, modContainer)` in the mod constructor, after `ArcSystem.init` and **before** `NUItems.ITEMS.register(modEventBus)` (the
Chronicle Sigil goes on `NUItems.ITEMS`). It registers the seven older bosses (a no-op when `ArcCharacters.bootstrap()` already did, see section 2), the item, its own
config file `nusmp-arc-world-server.toml` (no separate `registerConfig` line exists) and the game-bus handlers, and the self-tests `arcworld.*`.
Commands: `root.then(ArcWorldCommand.node())` beside `root.then(ArcCommand.node())` (Brigadier merges two `arc` literals under one parent).

## 2. The seven older bosses as characters (`arc/world/ExistingBosses`, `ExistingBossSpawner`)

| id | entity | saga | tier | floors | after | arena |
|---|---|---|---|---|---|---|
| `goliath` | `nusmp:goliath` | DanMachi | 2 | 10 to 30, both roads | - | the pit |
| `charybdis_spawn` | `nusmp:charybdis_spawn` | DanMachi | 2 | 10 to 30, both roads | - | the pit |
| `devil_avatar` | `nusmp:devil_avatar` | Spade Raid | 3 | 20 to 50, Abyssal | goliath, charybdis_spawn | the throne room |
| `dante` | `nusmp:dante_zogratis` | Spade Raid | 4 | 50 to 90 | `zagred` | the throne room |
| `vanica` | `nusmp:vanica_zogratis` | Spade Raid | 4 | 50 to 90, Abyssal | `zagred` | the throne room |
| `zenon` | `nusmp:zenon_zogratis` | Spade Raid | 4 | 50 to 90, Holy | `zagred` | the throne room |
| `lucius` | `nusmp:lucius_zogratis` | Wizard King | 5 | none | dante, vanica, zenon | the Final Hour's own courtyard |

They are not `ArcBoss` subclasses and nothing about their fights changed: they are spawned by id. Each record's Charter skills are read from `CharterTable` (only the
seven that have a row declare one), so the roster never disagrees with the table. **Not in the roster, on purpose:** the God of Order (it is a relic's summon, not a
place's boss), the Blood-Red Commander (a Convergence anomaly's), Zagred (his remake has its own builder), the Floor Warden and the Labyrinth Guardian (the dungeon's
own, the Guardian is what a floor boss replaces). Ids that other builders have not merged are skipped by the roster, so `after("zagred")` is safe.

`ExistingBossSpawner` finds the entity type from the id in the registry at the moment of use (a missing type is a refusal, not a crash), applies the same rules as
`ArcSpawner.summon` (layer on, story gate, one at a time) and places the boss the way `ZogratisBoss.spawnAt` does. Lucius keeps his own rule: the spawn gate refuses him
outside the Final Hour's arena, and this class reports that as a failed summoning.

## 3. Floor bosses (`arc/world/ArcFloorBosses`, `FloorBossChoice`, `FloorScale`)

On every tenth floor the Guardian Altar, at the moment it would call the Labyrinth Guardian, first asks `ArcFloorBosses.host(level, sector, altarPos)`. The pure
`FloorBossChoice` decides from the dungeon's seed, the sector, the floor and the party's story, and nothing else (no clock, no `Random`, candidates sorted by id): the
coin `arc.floorBossChance` says whether this floor hosts one at all, a boss that names the sector's theme is three times as likely as one that does not care, a boss
whose themes name other themes is never offered, and a boss the party has not opened is skipped (`arc.storyGate` off opens all). Anything missing, any exception, and the plain
Guardian is called instead.

The hosted boss keeps the Guardian's whole contract so nothing downstream can tell the difference:

* the three tags the Guardian carries (`nusmp_peak_boss`, `nusmp_mega_guardian`, `nusmp_sector_guardian`) and the persistent int `nusmp_sector` (so `DecennialReward`,
  `GearDrops` and the economy find it), plus its own tag `nusmp_arc_floor_boss` and its altar's position;
* **its death is reported to the altar** (`LivingDeathEvent`, last, only if not cancelled: `altar.onBossDied(uuid)`), so the Mana Core is forged and
  `MegaDungeon.guardianDefeated` runs exactly as for the Guardian; a boss that only vanishes still resets the altar;
* it grows with depth: `FloorScale` gives health and damage factors (1 on the first floor it was designed for, plus `arc.floorBossScalePerFloor` per floor below, capped at 2.5
  and 1.6) applied as attribute modifiers before it fights;
* it is carried back to its altar when led more than 28 blocks away (the Guardian's own hall leash), once a second.

**Wanderers.** `data/nusmp/mega/wanderers_arc.json` is an additive file listing the first characters (tier 1 and 2: `revchi_salik`, `heath_grice`, `lotus_whomalt`, `mars`,
`vetto`, `fana`, `patolli`, `rades_spirito`) with floor windows and weights; `EncData.Reload` reads it after `wanderers.json` and `WandererRoster.merge` adds its kinds without ever
changing an entry of the main file; an id that is not installed is never drawn. A framework boss that wanders has its fight arena moved to where it stands every second while
it is not fighting (the framework otherwise pulls a boss back to the spot it was made at).

## 4. The story arenas (`arc/arena/*`)

One dimension, `nusmp:arc_arena` (a flat world of the void biome, fixed at noon, `min_y 0`, height 256), holds every arena on a grid of sites 256 blocks apart (sixteen to a row, 256 sites).
A character is given a site the first time its arena is needed and keeps it for ever (the save holds it), so a growing roster never moves an arena under a party.

An arena is a **formula**, not a file: `ArenaShape.column(dx, dz)` gives the cells of a column (`dy` 0 is the topmost floor block, a player stands at 1), `ArenaShape.palette()`
the vanilla block of each role. The six shapes (numbers measured, not estimated):

| id | title | radius | blocks | tallest | for |
|---|---|---|---|---|---|
| `midnight_sun_ruins` | The Ruins of the Midnight Sun | 40 | 18121 | 9 | Elf Reincarnation Saga |
| `spade_throne` | The Throne Room of the Spade Kingdom | 27 | 8127 | 10 | Spade Kingdom Raid Saga |
| `wizard_court` | The Court of the Wizard King | 37 | 16868 | 10 | Ultimate Wizard King Arc |
| `sunlit_plain` | The Sunlit Plain | 40 | 17421 | 7 | The Seven Deadly Sins |
| `orario_pit` | The Pit of the Dungeon | 35 | 34481 | 22 (roof) | DanMachi |
| `stone_ring` | The Ring of Stone | 31 | 10768 | 8 | Allies and anything unknown (the generic fallback) |

`ArenaShapes.forCharacter` picks the arena a character names (`ArcCharacter.arena()`), else its saga's, else the ring. Lucius's record names `zogratis_arena`: he is fought in the
Final Hour's own courtyard, and the Chronicle carries the party there through the Hourglass's own travel (`ArenaTravel.enter`), whose director, guard and Return Sigils already exist.

What the tests prove about every shape (`ArenaShapesTest`): stacks are strictly rising; nothing hangs without support (lanterns on posts or ceilings, stairs facing their high side);
the boss, every arrival and every way home stand on ground with three blocks of air above; the closed arena cannot be left (a walk over the columns from the arrival ring reaches
every standable column and never the edge, walls at least four high, hedges four, fences two); every palette block exists in the game's own table of block states
(`tools/labyrinth/vanilla_block_states_1_21_1.json`) with real property values; nothing harmful (lava, fire, cobweb) is used; leaves are persistent; the formula is deterministic.

**Building** (`ArcArenaBuilder`): on the first arrival, inside `DungeonGuard.internal`, after making the chunks, column by column bottom to top (flag 2, so fences and stairs connect
by the game's own shape updates). `/multiverse arc arena repair <id>` puts back what is missing. **Protection**: `DungeonGuard.on/predictsClient` hold the dimension like the Final Hour's
(always), except while an operator has switched editing on (`/multiverse arc arena edit true`, off at every start). `ArcArenaEvents` refuses placing blocks and buckets and natural spawns;
a click on the lodestone of a way-home pad carries the player home (dormant while the boss fights).

**The fight** (`ArcArenaDirector`): the Final Hour's state machine `ArenaState`, reused per site: a ten-second countdown from the first arrival (five seconds once somebody stands
within 12 blocks of the boss's place), the boss made through the door that fits it (with the spawn gate's tag and the site number), abandoned when everyone is gone or the boss
vanished (60 seconds of rest), resting `arc.arenaCooldownMinutes` after a defeat. The story credit, the Charter Grimoire and the saga reward are NOT given here: they are given
where every death of these bosses is heard (`ArcStory`, `CharterEvents`, `ArcWorldCredit`), so a boss beaten here, in the dungeon or by a wanderer pays the same.

## 5. The Chronicle and the Sigil (`ChronicleSigilItem`, `ChronicleSigilRules`, `ChronicleView`, `ChroniclePick`, `ChronicleLectern`)

`nusmp:chronicle_sigil` is the Hourglass of the Final Hour built for the whole story. At the Chronicle (within 9 blocks of the lectern) a three-second hold carries the user and every player
within eight blocks (party members first, then the nearest, the Hourglass's own `HourglassRules.pickTravellers`) to the arena of the boss the user chose; inside an arena the same hold
takes them home (ten seconds mid-fight); sneaking and right clicking cycles the choice. It refuses away from the Hub and the arenas, while hurt in the last five seconds, while the chosen arena
fights or rests, and when the arenas are switched off. **The lectern** lists the player's story: one page per saga (only sagas with a boss), each boss as done, open or locked with the names it still
waits for, a click-to-choose button on the ones that may be chosen, and a button for the next saga. A boss may be chosen only when open to that player (`ArcStory.unlocked`) unless
`arc.storyGate` is off; a defeated one only while `arc.arenaReplay` is on; a stale choice falls back to the next boss of the story. All of it is pure and tested; the item and the lectern only turn rows into chat lines and clicks into calls.

The item is a real 3D model in hand (`tools/gen_weapon_models.py chronicle_sigil`, drawn by `client/WeaponRenderer`, previewed with `tools/item_preview/preview_weapons.py chronicle_sigil`) with its 16x16 sprite kept
as the inventory icon (`tools/gen_chronicle_assets.py`); it is a hand item, not a weapon, so it has no engravings and is excluded from the Better Combat manifest like the Hourglass.

## 6. The Hub: the Promenade, the Chronicle and the friends (`arc/hub/*`)

The Hub's plaza is a protected template, so the new ground lies outside it: a paved **Promenade** ring (radius 23.5 to 29.5, clear of the plaza's 33 x 33 box, inside the flat disc of the terrain, off the two
roads to the Spire and the Chasm) with 47 posts in a zig-zag, lamp posts on its outer edge, and the **Chronicle**, a quartz rotunda of six columns on the south of the band with a lectern at its centre facing the plaza
and a tall obelisk behind it. Everything is a vanilla block named as text and checked against the game's table; the whole site is the pure `ChronicleSite` (numbers, columns, posts, protected volume), tested (including a walk
from the plaza to the lectern, the entrance width, every post's clear air, facing and spacing, and that the guard holds every block the builder lays).

* `ChronicleBuilder` lays it from a quiet pass (every five seconds, only while a player is within 96 blocks of the plaza, only when EVERY chunk of the site is already loaded: it never loads one), clears only what grows there by
  nature (leaves, logs, grass, flowers, snow), never a player's block or the plaza's own box, and records the build in the Hub's own saved data. `hub build` and `hub repair` do it by command.
* `HubSiteGuard` is the Hub's protection for the site's volume (break, place, blast, the left click, buckets and flames with a margin, tools), creative exempt, off with `arc.chronicle`.
* `ArcHubNpcs` keeps the friendly characters standing every 100 ticks: `HubPosts` gives each one a post that never moves (the guide, `arc.hubGuide`, takes the post nearest the lectern), `NpcPresence` turns
  "wanted" (always, no prerequisites, or at least one ONLINE player has opened it) and what the pass saw into exactly one act (make, keep, remove duplicates, remove). A character is never made twice (the pass counts every
  Hub-placed NPC in the loaded level, makes none unless the ground round the post is loaded, never touches an NPC an operator placed by hand).

## 7. Rewards and credit (`ArcWorldCredit`, `SagaRewards`)

The older bosses' deaths are heard last and only when nobody cancelled them: the online fighters the boss recorded (and, with the share switches on, their nearby FTB teammates) get the same `ArcStory.defeated`
credit and messages as the core gives. For every boss, arc or older, the saga reward is then settled: the first time a player's story shows a saga with every boss defeated it is marked paid FIRST and then given
(Magic Stones of the dungeon and vanilla items, more for later sagas, plus a written book "Chronicle: <saga>" that names the bosses beaten), and `arc.saga_clear` is fired. The Charter Grimoire stays with `CharterEvents`.

## 8. Config (`nusmp-arc-world-server.toml`, section `[arc]`; the core's `nusmp-arc-server.toml` keeps `storyGate`, `floorBosses`, `floorBossChance`)

| key | default | meaning |
|---|---|---|
| `arenas` | true | the story arenas, the Chronicle Sigil and the lectern |
| `arenaReplay` | true | a defeated boss can be fought again from the Chronicle |
| `arenaCooldownMinutes` | 10 (1 to 240) | rest of an arena after its boss fell |
| `sagaRewards` | true | the once-only reward of a cleared saga |
| `chronicle` | true | the Promenade and the Chronicle in the Hub (and their guard); the posts are the Promenade's, so friends stand only with it on |
| `hubNpcs` | true | friends stand on the posts |
| `hubNpcsAlwaysThere` | false | every friend from the start (false: when some online player has opened it) |
| `hubGuide` | `eina_tulle` | the friend who takes the post nearest the lectern |
| `floorBossScalePerFloor` | 0.03 (0 to 0.25) | growth of a floor boss per floor below its design floor |

## 9. Commands (`/multiverse arc ...`, added by `ArcWorldCommand`)

`dump` (op) writes the roster as `id|name|saga|role` lines to `<game dir>/arc_roster.txt` (and to `tools/quests/arc_roster.txt` when run from the repository); `chronicle list|page <n>|pick <id>` (anyone);
`arena leave` (anyone), `arena enter <id>`, `arena status`, `arena repair <id>`, `arena reset <id>`, `arena edit <true|false>` (op); `hub status|build|repair|npcs|clear` (op).

## 10. Advancements and quest keys

`tools/quests/spec_arc.py` (imported by two lines at the end of `spec_adv.py`) reads `tools/quests/arc_roster.txt` and writes, under `data/nusmp/advancement/arc/`: a root, `arc/arena`,
`arc/saga_<saga>` (challenge, one per saga that has a boss), `arc/defeated_<id>` (one per boss) and `arc/met_<id>` (one per friend). Ids never change. After a merge: `/multiverse arc dump`, copy the file to
`tools/quests/arc_roster.txt`, `python tools/quests/gen_arc_adv.py`, `python tools/quests/merge_lang.py`. Keys (all in `AdvancementDataTest.EVENT_KEYS` and in the table of `docs/quests.md`): `arc.defeated` and
`arc.met` (the core fires them, the older bosses through `ArcWorldCredit`), `arc.saga_clear` (arg: saga id), `arc.arena` (arg: boss id).

## 11. What was checked

Pure tests (run here with the JUnit shim): `ArenaShapesTest` (12), `ArenaGridTest`, `BlockSpecTest`, `ChronicleViewTest`, `ChronicleSigilRulesTest`, `ChronicleSiteTest` (20), `HubPostsTest`, `NpcPresenceTest`,
`ExistingBossesTest`, `FloorBossChoiceTest`, `FloorScaleTest`, `SagaRewardsTest`, `RosterDumpTest`, `WandererRosterMergeTest`, `ArcWorldAssetsTest`, and the existing `AdvancementDataTest`, `WeaponMeshesTest`,
`WeaponAttributesTest` with the new files. Mutations that the tests caught: a saga reward paid twice, an NPC pass that acts on what it cannot see, a stale lectern choice kept, a floor boss the party has not opened.
Every game-side class was type-checked against the build's API stubs (the only errors are the stubs' known "abstract `new BlockPos`/`ItemEntity`/packet" artifacts); a deliberate wrong call was reported, so the checker was live.
Looked at: previews of all six arenas (plan, section, isometric, cut) and of the Hub site, the sigil's sprite and its 3D model next to the Hourglass.

## 12. Unverified (nothing of this was run in the game)

* every game-side class (events, the item's channel, the director, the builder in a live world, the Hub pass, the lectern's chat lines);
* that an arc boss of the framework fights correctly in a hall of the Guardian's size (the framework's leash is 56 blocks, the hall's 28; the carry-back is mine) and, as a wanderer, together with the Dungeon Master's patrol goal;
* that the existing bosses (Goliath, the Charybdis Spawn) fight correctly outside their old pits;
* that `ATTACK_DAMAGE` is what the moves of a boss scale from (`meleeDamage()` reads it; the moves' own damage was not read);
* the cost of laying the biggest arena (34,000 blocks, in one tick, as the Final Hour's courtyard does) and the Hub site (about 1,100 cells);
* that the lectern keeps its book texture without a block entity holding a book, and that the lantern, stair and fence states come out as the palettes intend after the game's own shape updates;
* the 3D sigil in a real hand (the headless preview uses the old sprite's box, as for the other relics);
* the roster dump's path when the server is not run from the repository (it always writes the game directory's copy);
* the final-hour arena used from the Chronicle depends on `ArenaTravel` being unchanged.

## 13. Edits this part made to files that belong to other parts (each a few lines, all additive)

| file | edit | why |
|---|---|---|
| `arc/story/ArcStory.wireBoss` | first line: return for an id `ExistingBosses.owns` | the seven older bosses already have their registry, buff and Charter rows; wiring them again would only log warnings |
| `arc/ArcSpawner.summon` | one line after the not-a-boss check: delegate to `ExistingBossSpawner.summon` | they are not `ArcBoss`es, so the core's door cannot make them |
| `arc/ArcSelfTests` | skip the older seven in `entities`, `charter`, and pick the first other boss in `spawn` | their checks are `arcworld.roster` and `arcworld.spawn` |
| `arc/ArcCommand.info` | the entity line names the older bosses' real entity path | it said NOT REGISTERED for them |
| `dungeon/DungeonGuard.on/predictsClient` | one line each: the story arenas are held like the Final Hour's, unless an operator is editing | the guard is the arenas' protection |
| `labyrinth/mega/guard/GuardianAltarBlockEntity` | the SUMMON branch asks `ArcFloorBosses.host` first; a new `adopt` method | a boss of the arcs may take the Guardian's place; `summon` and `summonNow` are untouched, so the Guardian self-tests are unaffected |
| `labyrinth/mega/enc/WandererRoster`, `EncData` | `WandererRoster.merge`; `EncData.Reload` also lists `mega/wanderers_arc.json` and merges it after the main file | the additive wanderers file |
| `client/WeaponRenderer.BASE_WEAPONS` | `ArcWorldItems.CHRONICLE_SIGIL` | the 3D sigil in hand |
| `tools/gen_weapon_models.py`, `tools/relics/weapon_classes.json`, `assets/nusmp/weapon_classes.json`, `tools/bettercombat/manifest.json` | the sigil's builder, class row (hand item) and exclusion | the owner's 3D-in-hand pipeline |
| `tools/quests/spec_adv.py`, `gen_advancements.py`, `gen_docs.py`, `AdvancementDataTest` | two lines importing `spec_arc`, the owned folder `arc`, four docs rows, the folder and four keys in the test | the advancements of the characters |

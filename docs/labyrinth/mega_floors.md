# The bidirectional Mega-Dungeon: Floor 0, the Holy road, the Abyssal road (1.55.0)

The owner's third brief ("Singular, Bidirectional Mega-Dungeon Dimension", sent in two versions; the second supersedes the first) builds on the Mega-Dungeon of 1.54.0
(`docs/labyrinth/mega.md`). One Overworld Gateway now leads to **Floor 0, the Sanctuary Hub** (a safe, buildable plains world). From it one road CLIMBS (the Holy floors,
+1, +2 ...) and one road DESCENDS (the Abyssal floors, -1, -2 ...). A player commits to one of them: after clearing floor +1 or -1 a lock closes the opposite road, though
the way back to Floor 0 is always open. A colossal boss gates every tenth floor, and behind it a Crucible offers exactly three Unique-or-higher relics of which the player
may take one; the relic taken decides the look of the next ten floors.

**State of this document.** It is the contract the wave-2 builders worked to and the owner's reference. Written without a compiler or a game; CI compiles, runs the pure JUnit
tests and the headless server smoke test (`docs/labyrinth/mega_ci.md`). Section 9 lists what is unverified. Every class, JSON file and strategy says WHY (the owner's standing
requirement); per-module details are in `docs/labyrinth/mega_<key>.md`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5): the brief against what the mod already had

| Brief item | Status | Notes |
|---|---|---|
| 1. Overworld Gateway: unbreakable, one per world | KEPT (1.54.0 `nusmp:mega_gateway`, reinforced deepslate, concentric ring) | Its destination CHANGES: with `hub.enabled` (default true) it leads to Floor 0 instead of straight into sector 0. `hub.enabled = false` restores the 1.54.0 behaviour. |
| 1. Floor 0 Sanctuary Hub, locked to a plains biome, buildable, passive mobs only, no foreign structures | NEW | Dimension `nusmp:sanctuary`, biome `nusmp:sanctuary_plains` (a plains copy in no structure tag), a spawn filter, no chunk-generator structures at all. Deliberately not a Labyrinth dimension, so the Dungeon Guard does not hold it. |
| 2. 3D-noise `ChunkGenerator` (spires, chasms) | NEW | Writes the Hub: rolling plains, the Holy Spire (a hollow white tower) and the Abyssal Chasm (a pit with caverns). It does NOT write the dungeon floors (see 3). |
| 2. 3D Wave Function Collapse | NEW | A pure, deterministic 3D WFC solver populates the Spire (trial chambers upward) and the Chasm (caverns downward) around a guaranteed stair path. |
| 2. Directional Lock `PlayerDungeonAlignment` | NEW | A NeoForge data attachment (the 1.21 form of a capability) with a sync payload; enforced at the Hub's gates, at chain travel and by a hard guard. |
| Bidirectional floors, boss every 10th floor | NEW, built on the 1.54.0 sectors | A sector is ten floors (section 3). Sector Guardians are the decennial bosses. |
| 3. Solo Leveling "System" overlay, floor quests, stat rewards, Shadow Core | NEW | Client `LayeredDraw` layer (the 1.21 form of `IGuiOverlay`), server quests, permanent stat boosts, minions extracted from slain Holy enemies. |
| 4. Strict vanilla-particle purge | KEPT (1.36.0, 731 files, guarded by a build test) | Not redone. |
| 4. Custom Rukh particles (`light_rukh`) | KEPT (1.36.0 `nusmp:light_rukh`, `RukhFx`, `RukhClientFx`) | The brief's `tensura_black_clover:light_rukh` is `nusmp:light_rukh` here (the mod id is `nusmp`). |
| 4. Combat flow (Light multiplies, Dark is rejected) | NEW | `LivingIncomingDamageEvent` + a tick drain, per-sector. |
| 4. Decennial Holy boss drops a guaranteed Metal Vessel | NEW drop of an EXISTING item (the Legend-and-above Djinn vessels of 1.24) | The single, owner-requested exception to "Unique+ only from Crucibles". |
| 5. Abyssal living ecology: dark organic palettes, factions that fight over territory | KEPT/NEW | Factions, turf wars: 1.54.0. The dark organic palette is the NEW `abyss` theme (and `radiant` for Holy). |
| 5. Magic Stones and Monster Meat | KEPT (1.36.0 `EconomyDrops`, 1.54.0 `FactionDrops`) | |
| 5. Standard Normal / Special / Rare equipment, dropped without limit | NEW | NOT relics (relics are one-of-a-kind under the Order's ledger). Ordinary items carrying a grade, so there is no world limit. |
| (first version only) hidden exploit drop tables | DROPPED by the owner's second version | Not built. |
| 6. D&D encounter tables by threat, party size, light | KEPT (1.54.0 `mega.enc`) | Polarity tables are added. |
| 6. Safe zones with Farmer's Delight / MrCrayfish furniture, short and long rests | KEPT (1.54.0 `mega.safe`) | |
| 7. Vital Strike vs HP buffers | KEPT (1.36.0) | |
| 8. Crucible: exactly three distinct Unique+ relics, one pick, a catastrophe | REPLACES the 1.54.0 "1 to 3 relics of mixed grades" numbers | Three pedestals always; grades UNIQUE, LEGEND, GOD ("Mythical"), GENESIS only. |
| 8. The chosen relic decides the next ten floors' palette | NEW | `RelicThemes` maps the chosen relic to the next sector's theme. |

Old behaviour stays behind switches (rule 2): `hub.enabled`, `floors.lockEnabled`, `mega.layout = "spire"`, and every older door keeps working.

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The shape of the whole thing

```
 Overworld                         nusmp:sanctuary (Floor 0)                       nusmp:mega_dungeon
 +---------+  gate    +----------------------------------------------+      +--------------------------------------+
 | Gateway |--------->|   Chasm (west)    PLAZA (home gate)  Spire   |      |  lane z = 0       : the HOLY chain   |
 | (1/world)|          |   gate at bottom <--- paths --->  (east)     |----->|     sector 0 (+1..+10) -> sector 1 ..|
 +---------+          |   Abyssal gate   safe, buildable   Holy gate  |      |  lane z = 300 000 : the ABYSSAL chain|
                      +----------------------------------------------+----->|     sector 0 (-1..-10) -> sector 1 ..|
                                                                            +--------------------------------------+
```

* **Floor 0** is its own dimension. Players build there; only passive mobs spawn; nothing from other mods generates.
* **Two chains** of sectors share the one dimension, 300 000 blocks apart (`MegaGrid.LANE_Z`) so they never touch. Each grows the way 1.54.0 grows: the Guardian's Mana Core
  opens the Gatekeeper and the next sector is built off the dead end.
* **A sector is ten floors** (`FloorMap`): the rooms are banded by how many doorways they are from the Landing; the Landing is floor 1, the Guardian's arena is floor 10.
* **The Directional Lock** closes the other road once floor +1 / -1 is cleared.

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Decisions and their WHY

**Floors are bands of a sector, not sectors.** The brief's "clear 5 rooms on this floor", "a staircase to the next floor", "a boss every 10th floor" and "one relic per 10 floors" all
describe a unit of about ten rooms with a boss every ten of them. A 1.54.0 sector has about a hundred rooms, a Guardian and a Crucible. Ten floors per sector makes every statement of the
brief literally true without touching the assembler: the sector Guardian IS the decennial boss, the Crucible behind it IS "one relic per 10 floors", a floor holds about ten rooms (the scale of
"clear 5 rooms"), and the whole 1.54.0 engine, its templates and its tests stay as they are. A floor is a pure function of a room's doorway distance from the Landing (`FloorMap.bandOfDepth`),
computed once after generation and saved in `Room.floor`.

**The dungeon floors stay Jigsaw (the socket-matching assembler), the Hub gets the noise and the WFC.** The owner's references for the look are authored interiors (a workshop, a vault, a cathedral
rotunda); a noise field cannot make those, a template can. The 1.54.0 assembler already is a constraint solver over tiles with matching sockets (the same idea as Wave Function Collapse, specialised for
doorways), and it is deterministic and unit tested over the real template catalog. What the brief asks for in terms of noise and WFC is delivered where it gives something templates cannot: the terrain
and the two colossal landmarks of Floor 0, whose interiors are populated by a real 3D WFC around a guaranteed stair path. If the owner wants WFC rooms inside sectors later, the solver is already a library.

**Floor 0 is a separate dimension, not a region of the Mega-Dungeon.** The Dungeon Guard holds `Labyrinth.in` dimensions (nothing can be broken or placed), which is the opposite of what a Sanctuary needs.
A separate dimension needs no exceptions in the guard, and a flat/plains fallback is one datapack file away if the custom generator ever misbehaves.

**The Hub's gates are the existing `nusmp:labyrinth_gate` block.** A custom block would add a registry entry, model, texture and loot table for nothing: the gate already is a walk-in portal that calls
`LabyrinthTravel.enter`. Which road a gate leads to is decided by its POSITION (`HubLayout.gateNear`), because the three gates stand at fixed places the chunk generator and the plaza agree on. (Vanilla Block
Primacy: no custom block where an existing one does the job.)

**Two chains, 300 000 blocks apart.** The Abyssal road needs a first sector of its own that cannot collide with the Holy road however far either grows (a chain is at most `maxSectors` x 183 blocks).
Distance is free in a void dimension; the alternative (one chain with interleaved signs) would make "the opposite road" impossible to tell from "the next sector".

**The lock is a data attachment plus one policy interface.** NeoForge 1.21 replaced capabilities for player data by attachments (`AttachmentType`, serialised, `copyOnDeath`); a hand-synced payload gives the
client what it needs. Every door asks `MegaDungeon.mayEnter`, so the rule lives once and a server without the module behaves like 1.54.0.

**Exactly three Unique+ relics.** The Crucible's value is the CHOICE; one option is a chest, two is a coin flip, three is a build decision. The three are always UNIQUE or higher (UNIQUE, LEGEND, GOD which the
owner calls Mythical, GENESIS) and distinct. Lower-tier gear is infinite and not a relic, so it never touches the Order's ledger.

**The relic decides the next theme.** `RelicThemes` maps the chosen relic (by explicit table, then by keyword, then by a stable hash) to the theme of the NEXT sector. To make that possible the next sector is
generated only after the Crucible is resolved or the player reaches the Way Onward (the lead changes the 1.54.0 "generate at unseal" rule to "generate when the vault is done or approached").

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The contract (already committed; builders code against it)

| Class (package `labyrinth.mega`) | What |
|---|---|
| `Polarity` | `HOLY(+1)`, `ABYSSAL(-1)`; `id()`, `opposite()`, `fromId`, `ofFloor`. Pure. |
| `FloorMap` | Signed floor arithmetic: `floorOf(p, chainIndex, band)`, `band`, `chainIndex`, `isDecennial`, `bandOfDepth(depth, arenaDepth)`, `threat(floor)`, `label`. Pure. |
| `HubLayout` | Coordinates of the plaza, Spire, Chasm, the three gates and `gateNear(x,y,z)`; the dimension is `min_y -64, height 384`. Pure. |
| `Sector` (extended) | `polarity`, `chainIndex`, `perRegion`, `firstFloor()`, `lastFloor()`; saved ("pol", "chain", "fpr"); a 1.54.0 save reads as HOLY with `chainIndex == id`. |
| `Room` (extended) | `floor` (signed; 0 = not banded yet), saved ("fl"). |
| `MegaData` (extended) | `chain(p)`, `chainSector(p, i)`, `chainTail(p)`. |
| `Expander` (extended) | `requestFirst(server, polarity)` (default: HOLY only). |
| `MegaDungeon` (extended) | `floorAt(level, pos)`, `polarityAt`, `setChainTravel/enterChain`, `setAlignmentPolicy/mayEnter`, `addFloorListener/fireFloorEntered/fireFloorCleared`. |
| `ChainTravel`, `AlignmentPolicy`, `FloorListener` | The three callback interfaces behind those hooks. |
| `MegaGrid.LANE_Z`, `laneZ(p)` | 300 000: where the Abyssal chain starts. |
| `Labyrinth.SANCTUARY_DIMENSION`, `inSanctuary(level)` | Floor 0. `Labyrinth.in(level)` stays FALSE there on purpose. |
| `RegionTheme` (extended) | `RADIANT`, `ABYSS` appended; every theme offers exactly 3 relics. |

Ids fixed by the contract: dimension `nusmp:sanctuary` (type `nusmp:sanctuary`), biome `nusmp:sanctuary_plains`, chunk generator codec `nusmp:sanctuary`, attachments `nusmp:dungeon_alignment` and
`nusmp:system_state`, data component `nusmp:gear_grade`, item `nusmp:shadow_core`, payloads `nusmp:alignment_sync`, `nusmp:system_sync`, `nusmp:system_toast`, entity tags `nusmp_servant` (minions).
Quest keys (call `NusmpEvents.fire(player, key, args...)`): `mega.hub` (arrived in Floor 0), `mega.aligned` (arg: polarity id, when the lock closes), `mega.floor` (arg: signed floor, first time),
`mega.system_quest` (arg: quest id, on success), `mega.shadow` (a minion was extracted), `mega.vessel` (arg: relic id, the decennial Metal Vessel), `mega.gear` (arg: grade, first drop of a grade).
Config files: `nusmp-labyrinth-mega-{floors,hub,macro,system,flow,gear}-server.toml` (section = the key), the System overlay also `nusmp-labyrinth-mega-system-client.toml`.
Lang prefixes: `message.nusmp.mega.<key>.*`. Commands: each module offers `public static LiteralArgumentBuilder<CommandSourceStack> command()` (permission 2) that the lead attaches under `/multiverse mega`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. Modules (wave 2) and who owns what

| Key | Package | Owns |
|---|---|---|
| `mfl` FLOORS | `mega.floors` | `FloorBands` (pure banding), the floor assigner (a `SectorListener`), `PlayerDungeonAlignment` attachment + rules + sync, the floor tracker and listeners, the lock's policy and hard guard, `/mega floors` |
| `mhb` HUB | `mega.hub` | dimension JSONs, the biome, spawn filter, build/anti-grief rules, `HubTravel` (from the Overworld, the gates, the way home), the plaza template, `/mega hub` |
| `mmc` MACRO | `mega.macro` | 3D simplex noise, 3D WFC, the Spire and Chasm shapes, the plains, `SanctuaryGenerator` (a `ChunkGenerator`) and its codec, slice previews |
| `msy` SYSTEM | `mega.system`, `client` | the holographic overlay, floor quests, stat rewards, the Shadow Core and the extraction of minions, `/mega system` |
| `mfw` FLOW | `mega.flow` | the Rukh flow (alignment of magic, multiplier, rejection drain), the decennial Metal Vessel, `/mega flow` |
| `mgr` GEAR | `mega.gear` | grades, the `nusmp:gear_grade` component, drop tables, tooltips, `/mega gear` |

Follow-ups for the wave-1 builders (they keep their context): CORE (chains, lanes, per-polarity first sector, `ChainTravel`, deferring the next sector until the Crucible), THEME (`RelicThemes`, the two new themes),
GUARD (the colossal decennial scaling and the Holy Metal Vessel hook), ENC and ECO (per-polarity tables and faction weights).

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Not verified (honest list)
Nothing was run in the game. Unverified: the look of the Spire and the Chasm, the 1.21.1 `ChunkGenerator` method set (checked against the API dump only), WFC run time at the sizes used, the System
overlay's look, the Shadow Core minions, the damage events of the Rukh flow against the mod's many magic systems, drop rates, and everything in `docs/labyrinth/mega.md` section 13.

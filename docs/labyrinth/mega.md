# The Mega-Dungeon (1.54.0)

One persistent dungeon that grows. A single Overworld Gateway leads into ONE dimension, `nusmp:mega_dungeon`, built from vanilla blocks
only, divided into **Sectors** (Regions). Each sector is a tightly packed, tabletop-style maze of rooms, corridors, safe zones, a boss and a
vault. Clearing a sector's Guardian and opening its Gatekeeper door with the Guardian's Mana Core makes the dungeon **generate the next
sector off the previous one's dead end**, for ever.

**State of this document.** It is the contract the builders worked to and the owner's reference. Everything here was written without a
compiler or a game; CI compiles and runs the pure JUnit tests, nothing was run in game. Section 13 lists what is unverified.

Every class, JSON file and strategy below says WHY it is built that way (the owner's standing requirement). Per-builder details are in
`docs/labyrinth/mega_<key>.md`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status | Notes |
|---|---|---|
| Dimension `nusmp:mega_dungeon` (flat void, no sky, 128 high), its dimension type and biome | NEW | A new id, not an edit of `nusmp:labyrinth`: a world keeps the dimension generators written into its level.dat, so editing the old one would leave every existing world on the Spire. |
| Mega-Dungeon layout (sectors, assembler, expansion, Landings, links) | **REPLACES the Spire as the default** | The Spire (1.35.0, `nusmp:labyrinth`, pools `nusmp:labyrinth/*`, the structure set `labyrinth_spires`) stays registered and generates exactly as before; `mega.layout = "spire"` routes Gates and Maws back to it (rule 2). |
| Gateway structure (`nusmp:mega_gateway`, one per world) | NEW | Added to the `nusmp:dungeon_gate` structure tag, so every existing locate/enter code finds it. |
| Old Dungeon Gate arches and Dungeon Maws | KEPT | They now lead into the Mega-Dungeon too (`mega.legacyGates`, default true). Already generated worlds keep every arch they have. |
| Labyrinth Guardian, Mana Core, `kind=sector` Gatekeeper door | NEW (the door KIND is appended to the existing `GatekeeperDoorBlock`) | |
| Encounter Director, noise and light tracking, Wandering Bosses | NEW | |
| Safe zones, Landings, rest (short / long), no easy passive regeneration | NEW | |
| Mod-compat processor (Farmer's Delight, MrCrayfish's Furniture Mod: Refurbished) | NEW | Data-driven; does nothing when the mods are absent. |
| Factions, turf wars | NEW | |
| Magic Stone + Monster Meat economy | KEPT (1.36.0 `EconomyDrops`, now also active here because `Labyrinth.in` covers the new dimension) + NEW: faction drops, Farmer's Delight recipes | |
| Vital Strike / HP buffer | KEPT (1.36.0, applies in the new dimension through `Labyrinth.in`) | The brief's system 6 already exists. |
| Region themes (`nusmp:region_theme` processor, 9 themes) | NEW | |
| Crucible of Choice (1-3 relic pedestals, one pick, a catastrophe) | NEW (extends the existing Relic Pedestal) | Relics stay 100 % unobtainable by normal play. |
| Sealed Archway trials, trap blocks, puzzle doors, Relic Pedestal, Gatekeeper door, exit gate | KEPT and reused as rooms of the new layout | |
| Living Dungeon (`nusmp:living_dungeon`), `labyrinth.engine = "legacy"` | KEPT | `engine = "legacy"` still wins over both layouts. |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The eight systems of the brief and where each one lives

| # | System | Where | Why this way |
|---|---|---|---|
| 1 | One gateway, one dimension, sectors, tabletop layout | `mega.gen` (assembler, generator), `mega.travel`, `nusmp:mega_gateway`, dimension JSONs, `tools/labyrinth/gen_mega_*.py` | One dimension so progress is shared and persistent; sectors so the world is generated in bounded, testable units; our own assembler so the layout is dense, guaranteed to contain its boss chain, and unit-testable (section 8). |
| 2 | Expansion tracker, live Jigsaw appending | `MegaData` (SavedData), `Sector`, `Expander`, `SectorGenerator`, `ExpansionQueue` | The tracker is saved data in the overworld's storage; a new sector is attached at the previous sector's Way Onward with the same assembler, run over several ticks. |
| 3 | D&D encounters, wandering bosses | `mega.enc` | A Dungeon Master tick rolls a data-driven table from threat, party size, noise and light; a boss patrols the room graph. |
| 4 | Safe zones, vanilla interiors, mod compat, rest | `mega.safe`, templates `structure/mega/safe/*`, `landing/*` | Interiors are templates (references: the stone-brick workshop, the 2-storey checkerboard vault); compat is a processor that reads markers; rest is an attachment fed by right-clicks on marked seats, hearths and beds. |
| 5 | Factions, turf wars, stone and meat economy | `mega.eco`, `labyrinth.dnd.economy` | Goals injected on entity join; the existing global drop event does the economy. |
| 6 | Vital Strike vs HP buffers | `labyrinth.dnd.vital` (1.36.0) | Already built; `Labyrinth.in` now includes the new dimension. |
| 7 | Gatekeeper, Guardian, Mana Core | `mega.guard`, `GatekeeperDoorBlock.Kind.SECTOR` | The door is a blockstate machine (no block entity): the new kind only opens to a Mana Core that names its sector. |
| 8 | Theme processor, Crucible | `mega.theme`, `mega.crucible` | A processor reads the sector from a ThreadLocal; the Crucible is saved in the sector so one pick per region is enforced by data, not by trust. |

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Architecture and ownership

```
 Overworld                                                  nusmp:mega_dungeon (flat void, no sky)
 +---------+  LabyrinthTravel.enter  +-----------+  wakes   +-----------------------------------------------+
 | Gateway |------------------------>| MegaTravel|--------->| Sector 0  (Landing -> filler rooms -> arena   |
 | (1 per  |   mega.layout = "mega"  +-----------+          |   -> [Gatekeeper] -> antechamber -> vault     |
 | world)  |                                                |                                -> Way Onward |-+
 +---------+                                                +-----------------------------------------------+ |
                                                                                                              | Mana Core opens the Gatekeeper:
   MegaData (SavedData, overworld storage): seed, expansionLevel, Sector[]                                    | Expander.requestNext(sector)
   Sector: theme, threat, region box, rooms, edges, markers, landmarks, Crucible                              v
                                                                      Sector 1 attached at the Way Onward's seal (same assembler)
```

Packages (`com.newuniverse.nusmp.labyrinth.mega`), owner in brackets:

| Package | Contents | Owner |
|---|---|---|
| `mega` | `MegaGrid`, `RegionTheme`, `RoomKind`, `MarkerKind`, `MarkerSpec`, `Marker`, `Room`, `Sector`, `MegaData`, `MegaDungeon` (facade), `SectorListener`, `Expander`, `RegionContext`, `MegaConfig`, `MegaSystem` | the lead (contract; CORE may extend `MegaConfig`) |
| `mega.gen`, `mega.travel` | the pure assembler, the planner, the generator and placer, the queue, travel, `/multiverse mega`, the Gateway | CORE |
| `mega.theme`, `mega.crucible`, `labyrinth.vault` (edits) | `RegionThemeProcessor`, theme data, the Crucible | THEME |
| `mega.safe` | `ModCompatProcessor`, rest state and events, regeneration throttle | SAFE |
| `mega.enc` | encounter tables, the director, noise and light, wandering bosses | ENC |
| `mega.eco` | factions, turf wars, faction drops, Farmer's Delight recipes | ECO |
| `mega.guard`, `labyrinth.seal` (edit: one enum entry and one branch) | Guardian, Mana Core, Guardian Altar, the sector door kind | GUARD |

The builders never import each other. They meet in `MegaDungeon` (queries, the three progression events, listeners) and in the data classes.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The grid and the Connector Contract (every template builder obeys this)

All numbers are in `MegaGrid.java` and `tools/labyrinth/mega_lib.py` (identical). `python3 tools/labyrinth/mega_check.py` enforces them.

* **Module = 9 blocks.** Every piece is `9a x 48 x 9b`. WHY odd: a 9-block module has a centre cell (offset 4), so a 3-wide doorway is symmetric about its
  jigsaw cell and is on the same cell when the neighbour is turned 180 degrees (4 maps to 9k-5, which is 4 mod 9). With an even module the two halves of a
  doorway would be one block out of line whenever the neighbour is rotated, and loops (two rooms that merely touch) would line up only half the time.
* **Height 48, floor row 8 in EVERY piece.** The floor is the same template row everywhere, so floors are level wherever pieces meet and no piece needs
  terrain matching (`projection: rigid`). 8 rows below the floor are for pits, crypts and cisterns; 39 above are for domes and two-storey halls. World floor y = 64
  (`MegaGrid.FLOOR_Y`), so the template origin is y = 56 and the dimension (min_y 0, height 128) holds everything.
* **Walls are 2 thick.** Floor row 8, walls rows 9 and up, a 2-thick ceiling. The interior is left UNSET (the void is air): a room is a shell plus furniture.
* **A doorway ("socket")** = a 3 wide x 4 tall hole through BOTH wall layers, a frame (polished pillars, chiselled lintel), and a jigsaw block in the bottom-centre
  cell of the OUTER layer (row 9), at a module centre along its face (offset 4, 13, 22 ...), orientation = the face it sits on (`north_up` on the north face ...),
  `joint: aligned`, `name = target = nusmp:mega_door`, `final_state = minecraft:air`. `MT.socket()` writes all of it.
  * **Connected:** two touching pieces with sockets facing each other make a 4-thick tunnel through both walls.
  * **Unused:** the jigsaw becomes air and the hole is a 2-deep **niche**. Where free space lies outside, a **door cap** fills it (below).
  * **Loops for free:** two rooms that touch and have sockets that face each other at the same position were never "connected" by the assembler, but their open
    holes line up, so the wall between them has a doorway. Because every piece is on the 9-grid with sockets at module centres, touching rooms line up often.
    That is how the tight, interlinked tabletop map (reference image 1) is reached without a tree-only layout.
* **Door caps** (`structure/mega/cap/*`): 3 wide, 6 high (floor row, 4 open rows, roof row), 1-8 deep, jigsaw at local (1,1,0) facing `north_up`, pool
  `minecraft:empty`. They continue the niche outwards (an alcove with a lantern, a chest, a statue, a bookcase, a cell with a skull, banners ...) and end in a
  back wall. The cap's own jigsaw becomes air (open alcove) or the wall block. If a cap does not fit (a neighbour's wall is in the way) none is placed and the niche is
  backed by that wall.
* **Special sockets** (names the checker allows): `nusmp:mega_door` (ordinary), `nusmp:mega_chain` (arena exit to antechamber entry), `nusmp:mega_vault`
  (antechamber to vault), `nusmp:mega_onward` (antechamber to the Way Onward corridor). A socket whose pool is `minecraft:empty` is a sealed end (the generator
  neither fills nor caps it); then `final_state` is the seal block and `hole=False` (the template fills the hole itself).

### Palette and the Vanilla Block Primacy Rule
Architecture uses ONLY the palette constants of `mega_lib.py` (the deepslate family: bricks, tiles, polished, chiselled, cobbled, cracked, plus the dark-oak family for
wood). The Region Theme processor swaps those families for another vanilla family per sector (blackstone, nether brick, end stone ...), so one template looks
different in every region. Everything else (furniture, glass, lanterns, candles, iron bars, chains, skulls, banners, armor stands, lightning rods, trapdoors ...) is left
alone. **No custom block** except those in `mega_lib.CUSTOM`: the Gatekeeper door, the Relic Pedestal, the exit gate, the Sealed Archway family and the 1.36.0 trap blocks, the
Guardian Altar. WHY: vanilla blocks light, collide, path and break correctly with no code, and the owner wants a vanilla-plus look; every custom block is code that must work.

### Data markers
Markers are vanilla **structure blocks in DATA mode** whose metadata is `nusmp:<kind>[;key=value]*` (kinds: `MarkerKind.java`). The game never places a DATA structure
block (the Jigsaw pool element ignores it) and `SinglePoolElement.getDataMarkers(templateManager, pos, rotation, false)` returns them with the piece's position
and rotation already applied, so a marker costs no custom block, leaves no trace, and is read BEFORE placement (no world scan). The marker stands in the AIR cell
above what it describes. A `facing` argument is rotated with the piece. A `;as=<hint>` argument asks the mod-compat processor to swap the block below
the marker (chair, sofa, table, counter, cutting_board, storage, stove, skillet, bed).

### Folders = room kinds, pools
`structure/mega/<folder>/<name>.nbt`; the folder is the `RoomKind` (`landing safe corridor junction hall set cavern trial trap puzzle treasure arena antechamber vault
onward link cap`). Pools (`data/nusmp/worldgen/template_pool/mega/*.json`) are generated by `tools/labyrinth/gen_mega_pools.py` from the folders and the weights in
`tools/labyrinth/mega_pools.json`: dropping a template in the right folder registers it.

| Pool | Used for | Filled from |
|---|---|---|
| `landing` | start piece of a sector | `landing/*` |
| `side` | the default target of a door socket | `corridor junction hall set cavern trial trap puzzle treasure` (weights), `safe` (low) |
| `cap` | fallback of `side` and of everything | `cap/*` |
| `safe` | forced safe rooms | `safe/*` |
| `arena` `antechamber` | the boss chain | `arena/*` `antechamber/*` |
| `vault`, `vault_1`, `vault_2`, `vault_3` | the Crucible Vault (by relic count) | `vault/*`: files named `one_*`, `two_*`, `three_*` |
| `onward`, `onward_straight`, `onward_left`, `onward_right` | the Way Onward | `onward/*`: files named `straight_*`, `left_*`, `right_*` |
| `link` | straight tunnel segments | `link/*` (`straight*`, `waystation*`) |

Processor list `nusmp:mega_interior` = `[nusmp:region_theme, nusmp:mod_compat]` (theme first: it changes the wood the compat swap then replaces).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The Mega-Dungeon assembler (WHY not vanilla `JigsawPlacement.addPieces`)

Vanilla `addPieces` keeps the Jigsaw DATA model (template pools, jigsaw blocks, structure templates, processor lists) and we keep all of it. We replace only the
placement LOOP, with an assembler of our own, because the vanilla one cannot (a) take a set of forbidden boxes (other sectors), (b) guarantee that a mandatory
piece exists (the Guardian's hall), (c) report loops and unused sockets, (d) run in pure Java under JUnit with thousands of seeds, (e) be sliced over ticks, and (f) pick
the start rotation instead of drawing it at random. The assembler works on abstractions (`Shape` = size + sockets; a pool = weighted shapes + fallback) so the same code runs
in the game (an adapter over `StructurePoolElement`, `getShuffledJigsawBlocks`, `getBoundingBox`) and in tests (an adapter over `catalog.json`, exported from the REAL
template files by `tools/labyrinth/mega_catalog.py`).

Algorithm (CORE implements it exactly; the numbers are config):
1. **Start.** Place the start piece (a Landing) so that one of its sockets attaches to the given world socket (or at a fixed origin for sector 0), with the rotation that makes it
   attach. The sector **region** is a box `REGION_MODULES` (19) modules square, 48 high, forward of the Landing; **exclusions** are the other sectors' regions and links.
2. **Fill (BFS).** A queue of placed pieces by depth. For a piece, take its sockets sorted by `selection_priority` descending (ties: shuffled with the seeded random). For each socket: its
   pool's elements in weighted-shuffled order (only while depth < `maxDepth`, 24) followed by the fallback pool's; an `empty` element ends the search. For each candidate and each of the four
   rotations (shuffled) and each socket K of the candidate with `canAttach(socket, K)` (target = name, facings opposite, `aligned`): the origin is `socket.cell + facingVector - K.local`;
   accept the first whose box (a) lies inside the region (door caps may exceed it by `CAP_MARGIN`), (b) overlaps no placed box in more than touching, (c) overlaps no exclusion. Record the junction,
   queue the new piece. A socket that finds nothing stays UNUSED (a niche).
3. **Ensure (the Sector Recipe).** After the fill, force what must exist: the **boss chain** (an `arena` attached to an unused socket at least 8 modules from the Landing and next to the reserved zone
   at the far side; then the antechamber, the vault chosen by `relicCount`, the Way Onward variant that points to free space) by running step 2 on that socket with the pool forced; at least one extra
   `safe` room 5-12 modules from the Landing; at least one `trial`; at least two `treasure`; at most one `set` piece. The recipe is data (`data/nusmp/mega/recipe/default.json`).
4. **Loops.** Two UNUSED sockets that face each other on adjacent cells of different pieces are a loop: an edge in the room graph (no block changes: their open holes already line up).
5. **Audit.** The plan must contain exactly one arena, its antechamber, a vault with `relicCount` PEDESTAL markers, an onward corridor whose EXPANSION_SOCKET marker faces free space, at least one extra
   safe room, at least `minRooms` (40) rooms and `minLoops` (3) loops, every piece inside the region, no overlaps, every room reachable from the Landing in the edge graph. A failed audit retries with the next
   seed (`assembleTries`, 24); then the **fallback layout** (a fixed small sector built only from the recipe pieces) is used and a warning is logged.
6. **Report.** Rooms (door caps excluded), edges, markers (read with `getDataMarkers`), the landmarks of the boss chain.

Everything above step 1's adapter is pure (no Minecraft types). `MegaSim` (a test-scope main class) runs N seeds over `catalog.json` and prints piece counts, fill ratio, loops, failures and writes a plan JSON for
`mega_preview.py --plan` (a map in the style of the reference tabletop dungeon). Pool weights are tuned with it.

### Placement, time slicing, persistence
`SectorPlacer` places `piecesPerTick` (3) pieces per server tick, each with `PoolElementStructurePiece.place(level, structureManager, chunkGenerator, random, BoundingBox.infinite(), pivot, false)` inside
`RegionContext.run(sector, markers, ...)` and `DungeonGuard.internal(...)` (the guard holds the whole dimension). Markers are collected for ALL pieces before the first one is placed. The
sector is saved as GENERATING first; a server stop mid-job is repaired on the next start by re-running the same plan from the saved seed (deterministic, placing the same blocks again is harmless).
When the last piece is placed: the rooms, edges, markers and landmarks are saved, the state becomes ACTIVE, `MegaDungeon.generated` calls the listeners, and (for an expansion) the rubble seal of
the previous sector's Way Onward is removed so the two tunnels join.

### Sectors and the expansion geometry
* Sector 0: its Landing's entry is at a fixed origin (`0, 64, 0` floor, facing +x) and its back door leads to the Landing's exit gate alcove.
* Sector k+1: attached at sector k's `EXPANSION_SOCKET` (position, facing, seal size from the marker). The Landing's entry socket attaches to the cell in front of the seal, and the new region lies
  beyond. The planner tries the three onward variants of sector k (chosen when k was generated) and, if the next region collides with an old one, inserts a **link**: straight 9-block segments
  (`link/straight*`, a waystation every 8th) up to `maxLinkModules`, so the dungeon can always grow.
* `threat = 1 + id`; `theme = RegionTheme.pick(seed, id, previous)`; `relicCount` is 1-3 from the theme definition and the seed.
* The Mana Core opens the Gatekeeper of sector k; `MegaDungeon.unseal` raises the expansion level and `Expander.requestNext` queues sector k+1; players loot the vault while it grows.

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. The sector, front to back (what a player meets)

1. **Landing**: a safe hall (the reference interiors), a way home (the exit gate), rest anchors, the sector's arrival marker.
2. **Filler**: halls, cells, crypts, libraries, forges, alchemy benches, kitchens, shrines, caverns, a cistern, trap and puzzle rooms (1.36.0 blocks), trial halls (Sealed Archways), treasure closets, circulation
   corridors and junctions, and every few rooms a safe room (a Break Level). The Cathedral Rotunda (the central set piece) sits once per sector.
3. **The boss chain**: the Guardian's hall (arena) -> the sector **Gatekeeper** (3x4x2 blocks of `gatekeeper_door[kind=sector]`) -> the antechamber -> the **Crucible Vault** on one side and the **Way Onward**
   on the other (ending in a rubble seal).
4. **The Crucible**: 1-3 relics float over separate pedestals; taking ONE triggers a catastrophe (the others crumble to dust, the room floods with toxic magicules, or a Cursed Spirit is summoned).
5. **The Way Onward** opens when the next sector exists.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. Encounters and the Dungeon Master (`mega.enc`)
A tick handler (every `rollEveryTicks`) per group of players in the dimension, outside safe zones, mimics a DM: it computes an encounter budget from the **region threat** (`Sector.threat`), the **party size**
(players within 48 blocks), the **noise** (a decaying sum of recent noise events near the party: sprinting, combat, block interaction, explosions, via GameEvents) and the **light** (block light at the party and light
items carried), rolls a d20 plus modifiers on a data-driven **encounter table** (`data/nusmp/mega/encounters/<id>.json`, one default and one per theme), and spawns the result out of sight on `SPAWN` markers (or valid cells
chosen from rooms of the sector). It never spawns inside a safe zone or within sight of a player, caps live mobs per party, and cools down. **Wandering bosses** are an entry type: a big mob (roster in JSON) that
walks the room graph of its sector between patrol nodes (`Sector.edges`, room centres, `PATROL` markers), investigates loud noise, loses interest in a player who is out of sight, in the dark and sneaking, and gives chase otherwise,
so the party hides or flees. It stays in its sector, drops no relic, and despawns when no player is in the sector.

## 8. Safe zones, compat, rest (`mega.safe`)
* Rooms of kind LANDING and SAFE are safe zones. No Director spawn inside; rest is possible there only.
* **Rest**: right-clicking a `SEAT` or `HEARTH` marker's block (or the block of a compat furniture piece there) starts a **short rest** (10 s channel, stands still, no hostile within 16 blocks, no damage): heals a share of health
  and mana and refreshes short-rest uses; right-clicking a `BED` starts a **long rest** (30 s): full health and mana, clears gloom, hunger penalty and fatigue, refills short-rest uses, sets the dungeon respawn Landing.
  `RestState` is an attachment. Long rests have a threat-scaled chance to bring an ambush while the party defends the zone.
* **No easy passive regeneration** in the Mega-Dungeon: natural regeneration and the mana regeneration are scaled by `passiveRegenScale` (default 0.25) outside safe zones (0 inside a rest channel's own rules).
* **Mod compat**: `ModCompatProcessor` reads the markers' `as=` hints from `RegionContext` and swaps the block below for a candidate id from `data/nusmp/mega/compat/*.json` if the mod is loaded and the id exists in the
  registry (Farmer's Delight stove / skillet / cutting board / cooking pot; MrCrayfish Refurbished chair / sofa / table / counter / storage / bed). Unverified ids simply do not swap.

## 9. Factions and economy (`mega.eco`)
`data/nusmp/mega/factions/*.json` define factions (member entity ids and tags, rivals, colour). Goals are injected on `EntityJoinLevelEvent` in the dimension (the 1.44 `PaladinGoals` reflection pattern): a mob
attacks members of rival factions that enter its room or come within range, never its own faction; the room's native faction (`Room.faction`, assigned at generation from the theme's ecology) gets a defending
bonus. The economy is the 1.36.0 event pair (stones and meat) plus faction-flavoured drops and conditional Farmer's Delight recipes.

## 10. Gatekeeper, Guardian, Mana Core (`mega.guard`)
`GatekeeperDoorBlock.Kind.SECTOR` (appended): blind to the Shadow Key, opens only to a `nusmp:mana_core` whose component names this sector, then calls `MegaDungeon.unseal`. The **Labyrinth Guardian** is a Floor
Warden subclass scaled by threat and theme, spawned by the **Guardian Altar** on approach (the Peak Altar state machine), with the boss rules of the mod; on death it drops the Mana Core (indestructible, glowing,
never despawns) and calls `MegaDungeon.guardianDefeated`. A lost core can be re-forged at the altar after 5 minutes while the door is still sealed.

## 11. Themes and the Crucible (`mega.theme`, `mega.crucible`)
`ThemeDef` (`data/nusmp/mega/themes/<id>.json`): the family swaps (deepslate to something else, with every stair, slab and wall variant and the blockstate properties kept), the wood swap, ambient particle,
ecology tags, the catastrophe weights and the relic pool (ids, grades, tags). The Crucible: on first entry the vault rolls `relicCount` DISTINCT unissued relics of the theme without issuing them; taking one issues it
through the relic ledger, marks the Crucible CHOSEN in the sector and fires the catastrophe; the other pedestals are emptied in the same tick (a second click cannot take two) and their relics stay unissued.

---------------------------------------------------------------------------------------------------------------------------------------------

## 12. Config and commands
`nusmp-labyrinth-mega-server.toml` (`mega.layout`, `expansion`, `legacyGates`, `lastLanding`, `piecesPerTick`, `maxPiecesPerSector`, `maxSectors`, `assembleTries`, `maxLinkModules`) and one file per builder
(`nusmp-labyrinth-mega-<key>-server.toml`). `/multiverse mega ...` (permission 2): `status`, `sector [id]`, `tp <id>`, `expand`, `regen <id>`, `audit <id>`, `core give`, `unseal <id>`, `plan <id>`.

## 13. Not verified (honest list)
Nothing was run in the game. The type checker has no stubs for `levelgen.structure`; compile errors, if any, are found by CI. The assembler and the planner are pure and covered by JUnit over the real template catalog.
Unverified in game: the rendering and lighting of generated rooms, the doors lining up in practice, the cost of placing a sector, the Jigsaw `getDataMarkers` rotation, every id of Farmer's Delight and MrCrayfish
blocks, the Cursed Spirit and Guardian balance, the Crucible catastrophes, the encounter budget numbers.

## 14. References used for the look
(1) a tabletop dungeon map of rooms, corridors and caverns on a grid (density, loops, crossroads, round rooms, a green central cavern); (2) "Dungeon Interior": a stone-brick workshop with window slits, columns with
iron bars, tables from stairs and trapdoors, an anvil, carpets, banners; (3) a two-storey purple deepslate vault with a chequerboard floor, a balcony, chains, brewing and crafting stations, skulls, glass-pane
cages and wall niches with lanterns and chests; (4) a short clip of a **cathedral rotunda**: a round hall with tiered colonnaded galleries, a glass dome with teal ribs, a giant crimson cross, a field of lit
candles on a dark floor, and a lava shaft behind glass. Image and clip files are NOT in the repository (they are the owner's references); the builders worked from them as listed in the briefs.

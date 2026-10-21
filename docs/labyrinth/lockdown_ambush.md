# Room lockdown and the dynamic ambush (1.107.0, lane "lockdown")

The owner's spec asked for an anti-speedrun layer: a `RoomLockdownEvent` posted when a challenge room is entered, a `LockdownBarrierBlockEntity` that drops unbreakable
boundaries until the room's wave is dead, lockdowns for **every key challenge room flagged in the room data** (not only trial halls), and a `DynamicAmbushDirector` that
answers players who bypass a room. Everything lives in `labyrinth/mega/lockdown/**`; the hooks into the trial machinery and the Dungeon Master are append-only.

## NEW / REPLACED / KEPT

| | What | Where |
|---|---|---|
| NEW | `RoomLockdownEvent` (NeoForge event, cancellable, band and waves mutable) and `RoomLockdownClearedEvent` | `lockdown/RoomLockdownEvent.java`, `RoomLockdownClearedEvent.java` |
| NEW | `LockdownBarrierBlock` + `LockdownBarrierBlockEntity` (the amber wall; the first barrier of a room is its brain, the rest remember where it is) | `lockdown/LockdownBarrierBlock*.java` |
| NEW | `LockdownDirector` (watches the players, decides, finds the doorways, spawns the wave, places the barriers), `LockdownWaves`, `OpeningScan`, `WaveCells` | `lockdown/` |
| NEW | Room data flagging the challenge rooms: `data/nusmp/mega/lockdown/rooms.json`, parsed by `LockdownTable`, held by `LockdownRooms` (reload listener, built-in minimum) | `lockdown/LockdownTable.java`, `LockdownRooms.java`, the json |
| NEW | `DynamicAmbushDirector` + pure `AmbushRules`, `AmbushLedger`, `SpeedTracker`, `AmbushGeometry` | `lockdown/` |
| NEW | `LockdownData` (saved set of cleared rooms), `LockdownConfig` (`nusmp-labyrinth-mega-lockdown-server.toml`), `LockdownText` + `tools/lang/lockdown.json`, `/multiverse mega lockdown ...` | `lockdown/`, `tools/lang/` |
| NEW | The block's art: `tools/gen_lockdown_assets.py` (amber diamond lattice, four animated frames), blockstate, model, item model | `tools/`, `assets/nusmp/...` |
| NEW | Self-test `dungeon.lockdown`, nine JUnit classes | `LockdownSelfTests.java`, `src/test/.../mega/lockdown/` |
| EXTENDED (append-only) | `EncounterDirector.spawnAt(...)` and `liveNearParty(...)`: the Dungeon Master's own spawn with the cells given by the caller (the place search is skipped, everything else runs) | `mega/enc/EncounterDirector.java` |
| EXTENDED (append-only) | `SealedArchwayBlockEntity.activate` posts `RoomLockdownEvent` (source TRIAL, informational) so a listener sees every challenge room | `trial/SealedArchwayBlockEntity.java`, one line |
| EXTENDED (append-only) | one line each: `MegaSystem` init and register, `MegaCommands`, `LabyrinthCreative.IDS` | shared registries |
| REPLACED | nothing. No existing behaviour was swapped out: the trial halls keep their Sealed Archway and Magic Barrier exactly as they were | |
| KEPT | The trial machinery (`TrialState.step`, `TrialMath`) is reused as it is, not copied. The escape rules (`mega/lock`: release rune, timers, `/dungeon release`, `/dungeon unstuck`) apply to every locked room. The Dungeon Master's table, squads and spawner build the waves. Time Magic VFX untouched. | |

## How a room locks

Twice a second the director reads where every Survival or Adventure player stands (Creative only with `lockdown.forCreative`). A room a party is **well inside** (the
box inset by `entryInset`, default 4: the wall is two blocks thick, so two blocks into the room proper) is asked, in this order (`LockdownMath.decide`):

1. is the feature on, is the world not Peaceful, is the room not a safe room (`Sanctuary`: landing, safe, stairwell, arena, antechamber, vault);
2. is it a challenge room (`rooms.json`, below) and not already known to be impossible to close in this session;
3. does it belong to the share of its kind that locks (`chance`, decided once per room from the sector seed), is the floor deep enough (`minFloor`), is the footprint big enough (`minArea`);
4. is it not cleared (saved), not locked now, not cooling down after an escape; is anybody inside, and deep enough.

If all pass: `RoomLockdownEvent` is posted (a listener may cancel, or change the band and the number of waves); the doorways are found in the loaded world; the first wave
is spawned through the Dungeon Master **before** anything is placed (a room whose wave cannot be placed never locks); then the barriers go in. A wave is the Dungeon Master's
own encounter for the sector's table, sized by the band, the party and the depth, formed into a squad of 5 to 8 with roles, and placed on cells inside the room.

When the wave is dead the next appears after `wavePauseTicks` (60); after the last the barriers fall, each cell gets back the block it replaced, the room is marked cleared
**for good** (`nusmp_lockdown.dat` in the overworld's data storage) and `RoomLockdownClearedEvent` is posted.

### The room data (`data/nusmp/mega/lockdown/rooms.json`)

A room's rule is its exact template, else the first matching name pattern, else its kind's folder; the first match wins whole. Shipped: `treasure` and `set` always lock
(set pieces: hard, two waves); about a third of the ordinary `hall` rooms lock from floor 2 (chance 0.35); fifteen name patterns (barracks, armory, great hall, grand
furnace, hatchery, beast den, torture chamber, cell block, crypts of vampires, mausoleum, cocoon larder, drow enclave ...) always lock; trial halls, puzzle rooms, trap rooms,
caverns, corridors, junctions, links, the Onward corridor, landings, safe rooms, stairwells and the boss chain never lock. Depth raises the band one step from floor 20 and
the waves by one from floor 40; a party of four raises the band one step; neither goes past HARD (the ambush band belongs to the ambush director, and a locked room must stay winnable).

### Finding the doorways (`OpeningScan`)

The wall is read in the plane `wallDepth` (1) blocks in from each face of the room's box, from the floor up `openHeight` (9) blocks. A cell counts as a doorway cell when it
**and every cell between it and the outside of the box** has no collision, so a dead niche behind a door cap (which has a back wall) is not a doorway. The cells are grouped
into openings and judged: at least one, at most `maxOpenings` (12), none bigger than `maxPerOpening` (96), at most `maxCells` (320) in all. Four doors of the clearance rule
(7 by 7) are 196 cells and pass; a cavern, a ruined hall or a wall with a hole in it fails and the room is left open ("open plan").

### The escape rules still apply

The master barrier is a `Lockable` kept in the `LockBook`: the release rune appears after 15 s with no hostile or 60 s anyway, `/dungeon release` opens it, it opens by itself
after 10 minutes, and when nobody is inside. Escaping is **not free**: the monsters that can be reached are removed, the room stays **uncleared**, cools down for
`cooldownSeconds` (120), and the ambush director is told (see below). An abandoned room (nobody inside for the trial reset time) resets the way an abandoned trial does.

## The dynamic ambush

Each player has a ledger of **heat**. Signals add heat (defaults): leaving a room with at least `minLiveMobs` (2) of the Director's, a lockdown's or an ambush's monsters
alive and **not after the player** (3 + 0.5 per monster); more than `speedLimit` (8.5 blocks a second) sustained over three seconds in a corridor, junction or link (1.5 a
second; a sprint is 5.6, a sprint jump 7, Speed II about 8); a teleport of `teleportBlocks` (24) or more between two samples half a second apart, on the same floor of the same
sector, between ordinary rooms (4); escaping a lockdown (5). Heat cools 0.25 a second. At 6 and when the budget allows, a squad is placed **ahead**.

* **Where**: `AmbushGeometry` picks cells from the saved floor plan: rooms that are not safe rooms and not the player's own, in a cone of the player's heading (cosine at least 0.35,
  widened to -0.25 if nothing is there), 14 to 40 blocks away, a corridor preferred, a long corridor giving a cell every six blocks. The Dungeon Master's spawner places them.
* **How strong**: the first ambush a player earns is STANDARD; each earlier one in the strike window adds a point of the Dungeon Master's own tension ladder (`Tension.bonus`),
  every eight floors add one more; it tops out at AMBUSH after the fifth.
* **Budget**: 90 s cooldown per player, 3 per 10 minutes, none while 8 of the Director's monsters already live within 64 blocks, and the Director's own cooldown is spent too.
* **Never**: in a safe room (the Hub is another dimension and is never touched), for a party that is itself locked in a room, in Creative or Spectator, in Peaceful, outside the Mega-Dungeon.
* **No hunger**: nothing in the package reads or changes the food bar, exhaustion or saturation (a test scans the sources for it).

## WHY (the reasons behind each decision)

* **The first barrier is the brain.** A trial's keystone is placed by its template; a room that locks has nothing in its template, so the director places the first
  barrier and makes it the master. One block entity with the state and the others as markers costs one ticker and one save, not forty, and a barrier whose master is gone removes itself.
* **The wave is spawned before the barriers.** The worst failure of a lock is to seal a party in with nothing to fight. A room whose wave cannot be placed does not lock.
* **Doorways are read, not stored.** The generator's door sizes are changing (the clearance rule widens them to 7 by 7), a template is rotated at placement, and the sector
  saves boxes, not doors. Reading the world at the moment of locking is correct whatever the generator did, and the judgement refuses what it cannot close.
* **Every cell keeps the block it replaced.** A doorway in a drowned crypt is full of water; a torch hangs in an arch. Restoring what was there leaves no scars.
* **The trial's state machine is reused, not copied.** `TrialState.step` and `TrialMath` are the proven rules for counting a wave and abandoning a room; the two seals behave alike.
* **Escaping costs something.** The owner asked for a guaranteed way out of every seal; the price of using it is the room staying uncleared, a cooldown, and an ambush ahead.
  That keeps "never locked in" and "no free bypass" both true.
* **Ambush answers, it does not punish.** Heat cools, a fair player never reaches the threshold, the squad is ahead (never behind), and the budget is hard. A corridor is preferred
  because a squad in a corridor cannot be walked round.
* **Monsters left behind count only if they are not after you.** Leading a chase out of a room is a tactic; walking past a room's idle monsters is a bypass.
* **A teleport counts only between ordinary rooms of one floor.** Stairs join floors 48 blocks apart, `/dungeon unstuck` lands in a safe room, and the Hub is another dimension.

## Configuration (`nusmp-labyrinth-mega-lockdown-server.toml`)

`[lockdown]`: `enabled`, `forCreative`, `roomData`, `entryInset`, `cooldownSeconds`, `wallDepth`, `openHeight`, `maxCells`, `maxPerOpening`, `maxOpenings`, `wavePauseTicks`.
`[ambush]`: `enabled`, `leftLiveRoom`, `fastTravel`, `teleport`, `escapedLockdown`, `heatThreshold`, `heatDecayPerSecond`, `speedLimit`, `teleportBlocks`, `minLiveMobs`,
`cooldownSeconds`, `budget`, `budgetMinutes`, `crowdCap`, `minDistance`, `maxDistance`, `speedWindowTicks`. The two switches are independent; each, set to false, returns the dungeon to how it was.

## Commands (permission 2)

`/multiverse mega lockdown status | rules | release | forget | ambush`. `status` says why the room you stand in did or did not lock (its rule, the director's last verdict, cleared / locked /
refused) and shows your ambush ledger. `release` opens every locked room with no penalty. `forget` clears the cleared marks. `ambush` places the ambush for you now (skipping heat and cooldown, never the exclusions).

## Self-test and unit tests

`dungeon.lockdown` (CI smoke): the shipped room data parses clean; the block is registered; a stand-in in a real challenge room of the generated sector makes the director lock
it (cells really become barriers, the master is in the escape book, the wave is inside the room, the event is posted once, a cancelling listener keeps the room open); the wave's
death clears it and every cell is given back and the cleared mark is saved; a second lockdown is opened by the escape rules and the room is then uncleared and cooling; the ambush
director places a squad ahead of a hot stand-in (alive, tagged, in no safe room, far enough, not behind), refuses the next attempt, and refuses a safe room outright.
JUnit: `LockdownTableTest`, `LockdownMathTest`, `OpeningScanTest`, `AmbushRulesTest`, `AmbushLedgerTest`, `SpeedTrackerTest`, `AmbushGeometryTest`, `WaveCellsTest`, `LockdownAssetsTest`.

## What could not be verified (nothing here could be run in a game)

* **None of the game classes ran.** They are type-checked against the API stubs and the pure rules are unit-tested; the in-game behaviour is covered only by `dungeon.lockdown`, which has not been run.
* **The doorway scan on the real templates.** The plane read (`wallDepth` 1, two wall layers) is derived from `MegaGrid.WALL = 2` and the door row, not measured on placed rooms.
  If a room's walls are not where the box says, the scan finds an open plan and the room does not lock; `lockdown status` says so ("refused: ..."), and `wallDepth`, `maxCells`
  and `maxOpenings` are the knobs. Whether the 35 percent of halls and the always-lock patterns end up with closable doorways is not known.
* **The clearance rule (lane 3) widens doors to 7 by 7.** The scan reads whatever is there, and four such doors fit the limits, but the two lanes were not tried together.
* **Fast mounts, elytra and boats** read as fast travel (that is the point), but the speed limit was chosen by arithmetic, not by play. Elytra flight through a corridor of the
  dungeon is not specially handled.
* **A teleport out of a locked room** (Tensura's teleport skills, a pearl, a chorus fruit) is not prevented: the barrier blocks walking, not teleporting. If everybody leaves, the room
  resets like an abandoned trial after the reset time and the escape reads as a teleport signal in the ambush director. A tether back into the room was left out on purpose
  (it would fight `/dungeon unstuck`).
* **Monsters stuck behind a pillar** can hold a lock until the Dungeon Master's stuck sweep removes them or the escape rules open the room (10 minutes at most).
* **The art** of the barrier was generated and looked at as a flat image, not in the game; the translucent render type is the model's, as for the trial's barrier.
* **Chunk edges.** A room spanning chunks whose master is in a chunk that unloads leaves its other barriers until the master loads again (they remove themselves within five seconds of that).
* **Balance.** The bands, wave counts, chances and ambush numbers are first guesses; every one is in the data file or the config.
* **Lang.** The text is English with fallbacks; other languages are not translated.

## Integration with clearance7 (1.107.0)

The two lanes were built in parallel and never tried together; the clearance lane reported three seams. The first two are in this lane's reach and are closed; the third is reported
(see `clearance7.md`, section 11, "Integration with clearance7"). Everything below is keyed on `GenData.Stat.clear7` of the room's sector (`ClearanceSettings.sectorCut(server, sectorId)`:
true only for a sector that was finished with `gen.clearance7` on) and changes nothing for a sector that was not cut, so `gen.clearance7 = false` gives the lockdown of before.

**What was wrong with a cut doorway.** The clearance rule cuts every connected doorway 7 wide and 7 high through BOTH wall layers of the room (module centre +- 3, rows 9..15), plus a
3-block apron inside. The director read the wall one layer deep (`wallDepth` 1), started two blocks from each corner (`u` from `min + 2`), and filled one layer. Three consequences: a doorway
in the first or last module of a wall, whose outer column is one block from the corner (x 1 of 1..7), was missed, so the barrier left a one-block gap on one side (and the player-in-doorway
test did not see the column either); the outer wall layer stayed open behind the barrier; and four such doorways are 196 cells but twelve are 588, over the default `maxCells` of 320.

**What it does now (cut sector only).** `OpeningScan.scan` (pure, the world is a callback) reads the wall lines from one block off the corner, looks at least 7 rows high (`rows`, never
fewer than the 7 of a doorway whatever `openHeight` says), and returns two lists: the *plane* (what `judge` counts, one cell per doorway cell of the inner layer, so a 7 x 7 doorway is 49) and the
*fill* (the plane and the outer layer of the same lines, 98 cells per doorway). The director places barriers on the fill, so the whole cut is closed through both layers. `OpeningScan.limitsFor`
lets twelve 7 x 7 doorways pass (`maxTotal` at least 12 x 49, `maxPerOpening` at least 63) and never lowers a configured limit; an opening that is still bigger than that, or a wall that is open
along its whole length, is still an open-plan room. Two doorways at the same corner share a cell and form one L-shaped opening (91 cells); the cell is filled once.

**The apron.** The 3 blocks of apron behind a doorway are inside the room and are not part of any barrier, but they are where a party arrives and where the barrier is next to. The wave
(`WaveCells.pick` with the doorway cells) keeps the cells within `WaveCells.DOOR_CLEAR` (4) blocks of a barrier cell for the END of its list (the reserve the spawner falls back on), so the
first group of the wave stands away from the way in and out when the room has room; a small room still gets its whole wave. The next waves (`LockdownBarrierBlockEntity.spawnWave`) pass the
master's cells the same way.

**Release and restore.** Unchanged and size-independent: every cell, in either layer, keeps the block it replaced, and the escape rules, the clear and the abandon paths give them back
(`DungeonGuard.internal`). The master now holds up to 8 x 49 x 2 cells; the saved long array and the slaves' five-second check scale with that and were not changed.

| | Before (integration) | Now, room of a cut sector | Now, any other room |
|---|---|---|---|
| wall line starts | 2 blocks from the corner | 1 block from the corner | 2 blocks (as before) |
| rows looked at | `openHeight` (9) | at least 7 | `openHeight` |
| cells filled per doorway | one layer | both layers | one layer |
| limits | 320 / 96 / 12 | at least 588 / 96 / 12 | 320 / 96 / 12 |
| wave placement | anywhere inside | out of the 4-block apron first | anywhere inside |

**Sealed Archway (trial hall).** The hall's runes are three per doorway in the inner layer, the columns four high. `trial/ArchwayDoorways` (pure) turns the runes found in the archway's box
into the columns of a cut doorway: each straight line of runes is widened to 7, the same columns are added in the outer layer (the side away from the archway), and `TrialMath.columnHeight(air, cap)`
lets them rise 7 cells. A barrier is still only placed in a cell that is air, so a widening that meets wall places nothing. The doorway test that arms the trial uses the same columns. An
uncut hall gets exactly its runes and the cap of 4.

**Tests.** JUnit: `CutDoorwayTest` (the scan on a made-up 27 x 27 room with doorways cut the way `Clearance7.cuts` cuts them: outer columns, both layers, nothing solid taken, the shared corner,
the old room read exactly as before, the limits, the wave's apron), `ArchwayDoorwaysTest`. Self-test `dungeon.lockdown` now also runs `OpeningScan.cutDoorwayCheck()` and, after a real lock,
re-scans the room (no passable wall cell may remain) and, in a cut sector, counts the doorways of 7 x 7 or more it closed.

**Not verified.** None of the game side ran; the scan is the same callback the pure tests use, with the world's collision shapes behind it. Whether the cut doorways of a real sector are
exactly the rectangles the pure code assumes (rows 9..15, centre +- 3) is what `dungeon.lockdown` and `dungeon.clearance7` check in the game; a doorway with a block that is never cut
(a chest in the apron's doorway line) reads as a smaller opening and is closed around the block. A room that is a cut piece but whose connected doors are all 3-wide door caps would read
as before.

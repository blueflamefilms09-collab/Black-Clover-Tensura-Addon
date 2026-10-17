# GEN2: each floor is a level (1.58.0)

The owner's request, shortened: "fix how the dungeons are so each floor goes up/down and are not together like they are currently; make it how D&D is: you start
at floor one and have to find a descend/ascend to the next." This file is the builder's account of the generator side: what was built, why each part is the way it
is, how to configure and drive it, what was measured, and, honestly, what has NOT been seen running. Read it with `docs/labyrinth/mega_v2.md` (sections 1 and 5.1 are
the contract it implements), `docs/labyrinth/mega_core.md` (the generator it extends) and `docs/labyrinth/mega_floors.md` (the floor tracker it keeps working).

Nothing in it ran in a game. Everything that can be pure is tested; everything that needs the game is type-checked against API stubs and covered by self-tests and a
smoke phase for CI to run (section 9).

## 1. The model in one page

* A **sector is one floor** (`FloorMap.isFloorModel(perRegion)`: `perRegion <= 1`). Config `floors.perSector` (default 1) decides it when a ROAD is opened; a road
  that already exists keeps the length of its sectors, so no world is ever renumbered. A value above 1 keeps the whole old banded path alive, untouched (owner's rule 2).
* A floor stands in a **fixed slot** of its road's lane: its door cell is `x = chainIndex * 198`, `z = MegaGrid.laneZ(road)` (`FloorPlan.entry`), and it grows east. There
  is **no tunnel** between floors (a party changes floor by a stair, the travel code teleports it), so a floor is planned from its own saved data alone and can be
  regenerated, repaired after a crash, or audited without looking at any other floor.
* A floor holds the **arrival stairwell** (the Landing role: the party appears on its platform; its stairs lead back), **about 28 rooms** (`mega.floorRooms`, 18..40; a
  boss floor 8 more), a **Safe Zone**, the traps, puzzles, trials, halls and caverns of the existing pools in the numbers of `floor.json`, and the **forward stairwell**
  (its stairs lead on) hung from the door of the room that is **farthest from the arrival by walking distance** (a ranked search, section 2).
* A **boss floor** (every 10th: `|floor| % 10 == 0`) keeps the whole boss chain (arena with the Guardian, Gatekeeper door, antechamber, vault with the Crucible,
  Way Onward with its rubble seal) at the far end, and the **grand stairwell stands behind the seal**. The seal is opened, as before, when the next floor has been
  generated (`SectorPlacer.clearSeal`), and the next floor of a boss floor waits for the Crucible exactly as before (`mega.expandAfterVault`).
* The theme belongs to a **decade of floors**: only floors 1, 11, 21 ... draw a theme (`RegionTheme.pick` for the decade) or take the one the Crucible's relic chose;
  the nine floors after keep the theme of the floor before.
* **Holy road**: the forward stairs climb, the arrival stairs drop (they lead back down). **Abyssal road**: the other way round. The four kinds are four pools
  (`stairwell_up`, `stairwell_down`, `stairwell_grand_up`, `stairwell_grand_down`) because the direction is decided by the road, not by a weighted draw; the brief's single
  pool name `stairwell` is the folder these four pools draw from.
* **Stair travel** (`StairTravel`): standing on the 3 x 3 landing pad under a STAIR_EXIT marker moves that player, and nobody else, to the linked floor with a short title
  ("Floor +4", "You climb the stairs."). The first floor's back stairs lead out to Floor 0 (the Hub plaza). The deepest floor reached is remembered with the old Landing
  memory (`LANDING_*` keys keep working).
* The **next floor is queued when a player ENTERS a floor** (`FloorGrowth`, a `FloorListener`), so the stairs are normally ready by the time they are found. If the
  player is early the stairs say so ("The stairs are still forming...") and the floor is queued then.

## 2. Decisions and why (the ones that are not obvious)

**Slots instead of neighbour planning.** The old sector planned its position from the Way Onward of the sector before it (a tunnel of up to 40 modules, a keep-clear
strip, other sectors as obstacles). A floor has no tunnel and no neighbours to avoid, so its place is arithmetic (`FloorPlan.entry`, `PITCH = (19 + 3) * 9 = 198`), and the
slot is wide enough for two boss floors (19 modules = 171 blocks) plus the reach of a door cap to never touch (a test pins the gap against `SectorPlanner.GAP`). Why it
matters: a regenerated or repaired floor must plan to the identical result (`RegenReplayTest` is the old proof; `FloorChainTest.aFloorPlannedAloneIsTheFloorPlannedInsideTheChain`
is the new one).

**Region sized by room count.** 28 rooms in the old 19 x 19 module region would be a sparse maze of empty void. `FloorPlan.regionModules` gives the smallest odd
number of modules whose square holds the rooms at about 9 modules each (13..19; odd because the region is centred on the door, which stands at a module centre); a boss
floor always gets 19 (its boss chain alone is 12 modules long). The region is saved in the sector, so a later change of `mega.floorRooms` never moves an existing floor: its
room TARGET is re-read from the config, its region is not (see the unverified list).

**The arrival stairwell is the start piece, and its only door is free.** The old Landing's single entry door led OUT to the tunnel, so its socket was marked used. The
stairwell's only door leads INTO the floor; marking it used left the first room nowhere to attach and every floor failed ("no extra safe room fits") until the start
piece was placed with `ownSocket = -1`. The first room is drawn from `nusmp:mega/stair_door` (corridors, junctions and halls, which all have several doors), so a floor never
begins in a dead end.

**The forward stairs are a search, and the walk is real.** After every room exists and before the door caps, the assembler lists the free doors of all rooms with their
walking distance from the arrival (`WalkGraph`: breadth-first over the joins plus the loops that are already decided, so the distance is the one a walker has), ranks them
farthest first with a seeded tie-break (`StairSearch.rank`), and tries them in that order until the stairwell fits on one (`stairs.candidates`, `stairs.bestDist`,
`stairs.hostDist`, `stairs.skipped` are recorded in the plan stats, so a test and a person can see the search did what it says). Why after the second fill and before the caps:
only then is every room there (so the distance is final) and the doors are still free (the caps would use them up). The audit insists the stairs are at least
`stairMinWalk` (4) doorways from the arrival, or the floor is a doorstep, not a level.

**Boss floors: the stairwell is the fifth piece of the chain.** The chain search places arena, antechamber, vault and Way Onward as before; for a floor it also asks, for
every Way Onward candidate, whether the grand stairwell fits BEHIND its seal (`stairBehind` pushes the Way Onward temporarily, tries the stairwell pool on its
expansion socket and rolls back). A Way Onward with no room behind it for the stairs is simply no candidate (`chain.onwardNoStairs`), so a boss floor can never end in a
Guardian and a wall. There is no keep-clear strip, because there is no tunnel: the stairwell stands there.

**Themes per decade.** `SectorDraft.of` for a floor-model sector: `startsDecade(chainIndex, per)` decides whether a floor draws (`RegionTheme.pick(seed, decadeIndex,
previous, road)`) or takes the Crucible's override; otherwise it keeps `previous`. The Crucible's relic therefore changes the theme of the NEXT decade, as the contract says,
and a relic taken at floor 10 cannot restyle floor 12.

**Theme-restricted templates.** `TemplateThemes` (the contract's pure rule: longest prefix wins, a template nobody claims is free) is filled from
`data/nusmp/mega/template_themes/*.json` by `TemplateThemeFiles.parse` (pure; reports every mistake as a sentence). The assembler asks it at every pool draw (`shapeOf`),
for old-model sectors too, so a room reserved for the Holy Church is never drawn in a Spider Warren. `gen_mega_pools.py` already merges the files' `relative` weights into the
pools; `mega_catalog.py` exports `themes` per template into the test catalog; `mega_check.py` flags a file whose prefix matches no template or whose theme is not an enum id.

**Stairwells are safe rooms.** `RoomKind.STAIRWELL.isSafe()` is true: nobody is ambushed at the foot of a stair and no faction is assigned to it (`NativeFactions`, quests
and the encounter director already ask `isSafe`). The cost: `MegaDungeon.inSafeZone` is true inside a stairwell, so natural regeneration is not throttled there (the Safe
Zone rule), which makes the stairs a place to get one's breath. Resting still needs a bed or campfire anchor. `SanctuaryTest` pins "every other kind is ordinary dungeon" and
was given the new kind.

**The stairs are physical.** A staircase is arithmetic, so the generator verifies every stairwell before writing it: the door is walked to the arrival cell and on to the
exit with the walking model of the light checker (one block up or down per step, two cells of headroom), the exit stands at the promised row, the 3 x 3 pad is standable, the
block light never drops below 6 on a walked cell, and the survival rules of `mega_arch_check` hold.

## 3. The stairwell templates

`tools/labyrinth/gen_mega_stairwell.py` writes `structure/mega/stairwell/*.nbt` (10 templates), builds on `mega_lib.py`, verifies each one (above) and renders it
(`--render DIR`). Frame: x east, z south, floor row 8, standing row 9; a small stairwell is one module (9 x 48 x 9), a grand one 18 x 48 x 18. Every stairwell has ONE door (a
`nusmp:mega_door` socket on the east face at offset 4, pool `nusmp:mega/stair_door`), an ARRIVAL marker on the platform inside the door, and a STAIR_EXIT marker (`dir=up|down`)
above the middle of the pad.

| template | size | what it is | light min |
| --- | --- | --- | --- |
| `up_a`, `up_b`, `up_c` | 9 x 9 | a staircase of 32 stairs round a 3 x 3 newel, two full turns, climbing 32 blocks; stone / tiled with soul lanterns, turning the other way / timber | 9, 7, 9 |
| `down_a`, `down_b`, `down_c` | 9 x 9 | 8 stairs round a 3 x 3 pit, half a turn, dropping 8 blocks to the pad; stone with banners / tiled with a lamp on a long chain over the pit / timber with barrels | 10, 7, 11 |
| `grand_up_a`, `grand_up_b` | 18 x 18 | the boss floor's stair: four flights of 7 round a well, climbing 28 blocks to a landing; stone with banners / tiled with braziers | 9, 6 |
| `grand_down_a`, `grand_down_b` | 18 x 18 | two flights of 4 into a sunken court whose floor is the pad; stone with banners / tiled with braziers | 7, 6 |

A stair block's `facing` is its HIGH side: a stair that climbs faces the way the walker goes, a stair that drops faces the way he came from. Everything architectural uses the
`mega_lib` palette, so the Region Theme restyles it; details are plain vanilla blocks (every state is checked against `vanilla_block_states_1_21_1.json`).

Pictures (looked at): `previews/gen2/<name>_sheet.png` (four cutaways, the plan and the light map of each of the ten), `previews/gen2/small_variants.png`,
`grand_variants.png`, and `previews/gen2/floors/*.png`, the map of four real floors (Holy floor 3, Abyssal floor 5, the first boss floor, Abyssal floor 1) assembled from
the shipped templates with `mega_preview.py --plan`.

## 4. Files, and why each exists

### New classes
* `gen/FloorPlan` (pure): where a floor stands, how big it is, which stairs it uses, how many rooms it aims at: the decisions the planner, the request, the simulator and the tests must share.
* `gen/WalkGraph` (pure): breadth-first walking distances over a doorway graph; the one definition of "far" the assembler and the audit and the tests all use.
* `gen/StairSearch` (pure): the ranking of candidate doors, farthest first, ties broken by a seeded key, independent of input order; and the pick of the first that fits.
* `gen/StairLinks` (pure): where each stair leads from a given floor (hub, floor before, floor after), which states are a place, and where exactly a player must stand to be on a pad.
* `gen/TemplateThemeFiles` (pure): the parser of the theme-room files; every mistake is a sentence for the log and for `mega_check.py`.
* `gen/BossFloors`: "give me a sector that has a boss chain" in either model (sector 0 of a ten-floor world, else the first boss floor), for the self-tests that used sector 0 for that (section 11).
* `travel/StairTravel`: the mover (section 5). `travel/StairSelfTests`: `stairs.layout`, `stairs.travel` for CI.
* `floors/FloorGrowth`: the `FloorListener` that queues the next floor when a real player enters a floor (a fake player never does).

### Edited classes (what changed and why)
* `FloorMap`: `DEFAULT_PER_SECTOR`, `BOSS_EVERY`, `isFloorModel`, `floorsPerSector`, `isBossFloor`, `floorOfSector`, `decadeOf`, `startsDecade`; **`isDecennial(floor, perRegion)` now
  reads every `BOSS_EVERY` floors when `perRegion <= 1`**: with one floor per sector the old rule would have made EVERY floor a boss floor (this keeps `DecennialReward`, the System
  and the flow code correct without edits; `FloorModelTest` pins it next to the old rule).
* `RoomKind.STAIRWELL`, `MarkerKind.STAIR_EXIT` (appended), `Sector.stairBack|stairForward|stairForwardPad` (saved as `sbk|sfw|sfp`), `Sector.isFloor()`, `Sector.isBossFloor()`.
* `MegaConfig.FLOOR_ROOMS`; `maxSectors` default 500 to 1000 (a sector is a floor now). `FloorsConfig.PER_SECTOR`. `FloorsRegistry` registers `FloorGrowth`.
* `gen/Request` (`Floor`, `themeId`, `themes`), `gen/SectorRequests` (the floor start: no tunnel, no onward policy), `gen/SectorPlanner` (`floor`, `regionFor(modules)`),
  `gen/SectorDraft` (decade themes), `gen/Recipe` (`stairMinWalk`, `forFloors`, `fromMap(values, base)`), `gen/VanillaPools` (`floorRecipe`, `themes`).
* `gen/MegaAssembler`: `runFloor`, `placeFloorStart`, `placeForwardStairs`, `stairBehind`, the room budget, the theme filter (all described above).
* `gen/SectorAudit` and `gen/SavedAudit`: a floor is judged as a floor (two stairwells with their markers, no boss chain on an ordinary floor, the whole chain with a stairwell
  behind its seal on a boss floor, the stairs far enough apart, every room walkable from the arrival stairwell). The old audit is untouched for old sectors.
* `gen/SectorFill`, `gen/SectorView`, `gen/SectorGenerator`: carry the stairs from the plan into the saved sector; `probeFloor` for the self-tests; `padOf` accepts a stairwell.
* `gen/ExpansionQueue`: `tryNextFloor` / `draftFloor` (the next floor is a draft with no neighbour planning), `onFloorReached`, `rowOf` counts forward stairs as a way on; the old
  path (`planBehind`, `draft`) is kept whole for banded roads.
* `gen/CoreSelfTests`: the eight checks read floors (probe floor 1 and the first boss floor per road; the seal of a boss floor; the dry run of the floor after the newest; the
  slot of every floor and the gap between two). `gen/MegaCommand`, `gen/CommandLines`: the stairs line and a `floors:` line of `pools`. `gen/MegaGenRegistry`: `StairTravel`,
  `StairSelfTests`.
* `floors/FloorsSelfTests`: `floors.bands` and `floors.tracker` assumed an arena on every sector, which an ordinary floor does not have; they now check "one floor, two stairwells"
  and walk the stand-in over the stairs to the next floor's arrival (one `enter`, one `clear`, the lock closing).
* `travel/MegaTravel`: the arrival message of a floor ("the foot of the stairs of floor +1. Find the stairs that lead up."), and standing in a stairwell counts as standing on a Landing for the deepest-floor memory.
* `tools/labyrinth/mega_catalog.py` (`stairwell` kind, `themes` per template), `mega_check.py` (`check_template_themes`, the enum read from `RegionTheme.java`), `mega_lib.py`
  (`stair_exit` is a known marker kind), `mega_pools.json` (the five pools), `mega_preview.py` (a colour for STAIRWELL).
* `tools/ci/server_smoke.py`: `phase_floors` (+ `phase_floor_pools`, `grow`), one call in `main`, one refresh of the restart baseline, the options `--floors` (default 10: grows the Holy
  road to the first boss floor) and `--per-sector` (writes `floors.perSector` into the world's server config before the first boot, so the old ten-floor model can be smoke-tested too).
* `src/test/.../enc/SanctuaryTest`: the stairwell is a sanctuary now.

### New data
* `data/nusmp/mega/recipe/floor.json`: the recipe of a floor as data (the numbers of `Recipe.forFloors`; `FloorRecipeTest` fails if they drift).
* `data/nusmp/structure/mega/stairwell/*.nbt` (10) and `data/nusmp/worldgen/template_pool/mega/{stair_door,stairwell_up,stairwell_down,stairwell_grand_up,stairwell_grand_down}.json`.
* `src/test/resources/mega/catalog.json` regenerated (132 templates, 20 pools).

## 5. Stair travel, exactly

`StairTravel` looks at every player in the dimension four times a second. A player whose feet are on a pad (`StairLinks.onPad`: within 1.45 blocks sideways of the STAIR_EXIT cell
and 0.6 below to 1.6 above it) of the floor they stand in is moved, in this order:

1. The Directional Lock (`MegaDungeon.mayEnter`) is asked first; it tells the player why when it says no (it only refuses a player who stands on the road their lock has
   closed, whom the hard guard sends out anyway).
2. The forward stairs: if the next floor does not exist the floor is queued (`ExpansionQueue.tryNext`) and the player is told "The stairs are still forming"; if it is being
   generated, the same line; if it FAILED, the line names `/multiverse mega regen <id>`. Otherwise the player is set on the ARRIVAL marker of the next floor's arrival stairwell.
3. The stairs back: from the first floor, out to Floor 0 (`LabyrinthTravel.leaveAny`, which returns to the plaza); from any other floor, onto the ARRIVAL marker of the forward
   stairwell of the floor before (`Sector.stairForwardPad`).
4. Every use and every refusal starts a cooldown (60 and 80 ticks, key `nusmp_mega_stair_until`), so a refusal is spoken once every few seconds (not at every look) and a player is never bounced. Arriving is never
   standing on a pad (the platform is 8 or more blocks above or below the pad), so a party that has just arrived is a staircase away from the next jump (`StairLinksTest`).
5. The deepest floor is remembered (`MegaTravel.remember`), a title with a fade is shown (6, 24, 10 ticks), and the quest hook `mega.stairs` fires.

A player who steps off a stair into the pit of a descending stairwell lands on the pad and takes the stairs the quick way, with the fall damage of at most 8 blocks.

## 6. Configuration

| key | file / section | default | range | meaning |
| --- | --- | --- | --- | --- |
| `floors.perSector` | `nusmp-labyrinth-mega-floors-server.toml`, `[floors]` | 1 | 1..20 | floors per sector for a road that is being opened; 1 = the floor model, more = the old banding (`floors.perRegion` and `floors.banding` then apply) |
| `mega.floorRooms` | `nusmp-labyrinth-mega-server.toml`, `[mega]` | 28 | 18..40 | rooms of an ordinary floor (a boss floor +8) |
| `mega.maxSectors` | `nusmp-labyrinth-mega-server.toml`, `[mega]` | 1000 (was 500) | 1..10000 | the dungeon stops growing at this many sectors, now floors of both roads together; an existing config file that saved 500 keeps it |
| `stairMinWalk` | `data/nusmp/mega/recipe/floor.json` | 4 | 0..60 | least doorways between the arrival stairs and the forward stairs |

Unchanged and still read: `mega.expansion`, `mega.expandAfterVault` (boss floors), `mega.lastLanding`, `mega.maxPiecesPerSector`, `mega.assembleTries`, the floors lock keys.

## 7. Commands (all `/multiverse mega ...`, permission 2)

* `status`: the header, then per road `road holy: sectors=3 ids=0,1,2 floors=+1..+3 state=ACTIVE` and one `sector <id> <STATE> theme=... road=holy chain=2 floors=+3..+3` line per floor.
* `sector [id]`: the sector line, the landmarks, and for a floor the stairs line
  `  stairs back=<x,y,z> forward=<x,y,z> forwardPad=<x,y,z> leadsOn=<id|none> leadsBack=<id|hub|none> boss=<true|false> climbs=<up|down>`.
* `tp <id>`: to the arrival stairwell's platform. `plan <id>`: writes `mega_plan_<id>.json` (a floor plans from its saved region and seed). `verify [id]`: the plan against the
  game's own geometry and the world. `audit <id>`: `audit <id> OK` or the failures. `rooms [id]`: the count by kind, now with `stairwell`.
* `expand [holy|abyssal]`: the first floor of a road, or the next floor off the road's newest one (floor model); `regen <id>` and `unseal <id>` as before. `pools`: also prints a
  `floors:` line (rooms per floor, the floor recipe's numbers, the number of theme-room rules).
* Self-tests (`/multiverse mega selftest`): `core.*` (floor-aware), `floors.*` (floor-aware), `stairs.layout`, `stairs.travel`.

## 8. Quest hook

`mega.stairs`, one argument: the signed label of the floor the player arrived on (`"+4"`, `"-2"`), fired once per use of a stair. Unchanged and still fired by the floor tracker:
`mega.floor` (first time a depth is reached), `mega.floor_cleared`, `mega.aligned`.

## 9. Proof, and what it does not prove

Run here:
* `FloorModelTest`, `SectorDraftFloorTest`, `StairLinksTest`, `StairSearchTest`, `TemplateThemeFilesTest`, `FloorRecipeTest`, `CommandLinesTest` (the exact text of every line the smoke script
  reads, and copies of its regexes), `FloorAuditTest` (one broken invariant at a time against both audits, and the unbroken floor passing), `FloorAssemblerTest` (stand-in catalog),
  `FloorChainTest` (25 floors of each road: slots, gaps, decades, a floor planned alone equals the floor inside the chain, the stairs of every floor lead to the floors around it),
  `FloorCatalogTest` (the shipped templates). A mutation check: turning `isDecennial` back into the naive rule fails `FloorModelTest`; disabling the "exit inside the arrival stairwell"
  check fails `FloorAuditTest`.
* On the shipped templates: 264 floors (22 floors, 6 seeds, both roads) all built and passed the full audit, 0 fallback layouts, 1.05 tries on average, 29.6 rooms on
  average, the forward stairs 6.2 doorways from the arrival on average, 139 of the 240 ordinary floors on the very farthest doorway (on the others the farthest doors had no free space around them for a 9 x 9
  box, so the search went down the ranking to the farthest door where it fits). The old model is unchanged: `MegaCatalogTest` 200 of 200 sectors pass the audit with 0 fallbacks.
* `mega_check.py`: 132 templates, 20 pools, 0 errors (2 old warnings); `mega_arch_check.py stairwell`: 10 templates, 0 problems; `gen_mega_pools.py --check`; `mega_catalog.py --check`.
* `tools/ci/server_smoke.py` against a stand-in server that speaks the exact lines `CommandLines` prints: every new check passes on the good run and each of seven deliberate faults
  (a stair that leads nowhere, a back stair to the wrong floor, a stair that goes the wrong way, a stair cell outside its floor, a missing landing marker, a boss floor without its
  chain, a Landing room on a floor) fails exactly the check it should; the restart check passes with the floors added.
* The pictures of section 3, looked at.

In CI (written, not run): the real server loads the ten templates and five pools (`floor-pool-*` through `/place jigsaw`), the floors grow (`floors-expand-*`, `floors-audit-*`,
`floors-stairs-*`), and the self-tests `core.*`, `floors.*`, `stairs.*` run against real floors.

## 10. Unverified (honest list)

1. **Nothing ran in the game.** All game-side code (`StairTravel`, `FloorGrowth`, the queue's floor path, `SectorGenerator` fill, the self-tests) is type-checked against API stubs
   with only the known stub artifacts left (abstract `new BlockPos`, packet constructors, `Direction` members, `Player` subclasses).
2. **Stair mechanics were judged by a model, not by a player.** The walkability check assumes one block up or down per step and two cells of headroom; a real player on a stair block
   next to a wall, a rail or a lamp may behave differently (for example catch on a hanging lamp). The geometry of a stair block's `facing`/`half` follows the vanilla rule
   (a climbing stair faces the walk direction) and was only checked by reading the iso renders.
3. **The pad is a rectangle in the air above the marker.** `StairLinks.onPad` was chosen for a 4-tick poll (a 3-block pad cannot be walked across in 4 ticks) and has not been tried at
   sprint speed or with elytra.
4. **`LabyrinthTravel.arrive` and `leaveAny`** were used as they are; their side effects (the gate cooldown stamp, the remembered origin, the Hub return) were read, not run. The
   title packets (`ClientboundSetTitlesAnimationPacket(6, 24, 10)`) are the vanilla ones; a stand-in player skips them.
5. **A boss floor's rubble seal** is kept and opened by the old `clearSeal` when the next floor is generated; the self-test `core.seal` checks the seal cells and no longer demands air
   outside them (the grand stairwell stands there). Whether the stairwell's first cells behind the seal are walkable air was checked in the template, not in a placed floor.
6. **Block light** is the light checker's computation, not the game's; the lowest value on a walked cell is 6 to 11 per template (the gloom clock counts 4 and below).
7. **Changing `mega.floorRooms` after floors exist**: the region (from the old room count) is saved, the room target is re-read; a much smaller target in an old region makes
   sparser floors, a larger one more crowded floors, on regeneration only. Nothing renumbers.
8. **`mega.maxSectors`** stays at a saved 500 in an existing config file: 500 floors is 250 per road, still far beyond play, but it is a surprise worth knowing.
9. **Fake players** never start a floor (`FloorGrowth`); the self-tests rely on it. A real player in a mod-provided body would.
10. **The `stair_door` pool** draws from the `hall` folder as well as corridors and junctions; whether a hall right behind the arrival stairs feels right was not seen.
11. Pit falls in a descending stairwell (`down_*`, `grand_down_*`) cost up to 8 blocks of fall damage and end on the pad. It is a shortcut with a price, by design, but unseen.

## 11. For the other builders and the lead

* **SYS2** (`system/`): `FloorMap.isDecennial(floor, sector.perRegion)` is correct for a one-floor sector without edits. `Sector.isFloor()` and `Sector.isBossFloor()` are there for
  new code. `SystemQuests` and `SystemSync` default to `FloorMap.DEFAULT_PER_REGION` (10) only when no sector is found, which is right.
* **PARTY**: `FloorPlan.entry(road, chainIndex)` uses `MegaGrid.laneZ(road)`; a per-party lane means `laneZ(road, party)` there and in `Sector`-lane checks (`core.roads` reads the lane
  from `MegaGrid.laneZ(road)` too). `StairTravel` moves one player; for a party, set the destination player by player (the code already works per player).
* **PRUNE** (merged first by the lead): the four files both of us edit (`MegaConfig`, `gen/ExpansionQueue`, `travel/MegaTravel`, `tools/ci/server_smoke.py`) carry PRUNE's removals
  as well (they were taken from its commit by a three-way merge, so my copies cannot bring `layout`, `legacyGates`, `oldGate`, `atGateway` or `Labyrinth.layoutIsMega()` back, and a
  merge of the two branches is textually clean: tried here file by file). Consequence in MY tree alone: `Labyrinth.java:60` (not mine, PRUNE deletes `layoutIsMega`) no longer
  compiles until PRUNE is merged; nothing of mine uses a removed API. `LabyrinthTravel.arrive`, `leave`, `leaveAny`, `arrivalPad` and `forget`, which `StairTravel` uses, are still in PRUNE's tree.
  `server_smoke.py`: my floor phases are new methods, `main` has one new call.
* **Theme builders**: ship `data/nusmp/mega/template_themes/<theme>.json` as `{"theme": "<enum id>", "templates": ["mega/hall/<prefix>"], "relative": {...}}`; `mega_check.py`
  tells you when a prefix matches no template or the theme is not an enum id; `gen_mega_pools.py` merges `relative` into the pools; a room reserved for a theme is drawn only there.
  Stairwells are free for every theme (the Region Theme restyles them); `RoomKind.isSafe()` includes `STAIRWELL`.
* **Self-tests of other modules that assume sector 0 holds the boss chain or a Landing** (found by reading them; none of them was run). With one floor per sector, sector 0 is floor
  +1: no arena, Gatekeeper, vault or Way Onward, and a stairwell instead of a Landing room. Affected: `guard/GuardSelfTests` (line ~150: `data.sector(0)`; needs `guardianSpawn`,
  the Gatekeeper door), `crucible/CrucibleSelfTests` (~130: pedestals and vault), `theme/ThemeSelfTests` (~196: `pedestals.size() == relicCount`), `safe/SafeSelfTests` (~150 and ~387:
  `RoomKind.LANDING`), `enc/EncSelfTests.floorThreat` (~341 and the "has no Landing" line). The fix is one line each: take the sector from `BossFloors.first(data)` (and fail with
  "no boss floor yet" when it is null); for the `safe` tests and `enc.floor_threat` accept `RoomKind.STAIRWELL` (or `r.kind.isSafe()`) where they ask for `LANDING`. `enc.safe_zone` already falls
  back to `isSafe()`. Until that is done, run the smoke with `--per-sector 10` for the old suite (sector 0
  is then a ten-floor sector, as before) and without it for the floor suite (`core.*`, `floors.*`, `stairs.*`, `phase_floors`); the default `--floors 10` grows the Holy road to floor +10
  so `BossFloors.first` has an answer.
* **Lead**: the pool file `tools/labyrinth/mega_pools.json` was edited as the brief allows (five pools and a note); merge it with any other builder's `relative` entries and run
  `gen_mega_pools.py` and `mega_catalog.py` after the merge. `SMOKE_EXPECT` may list the prefix `stairs`. The lang fragment is `arc/frag/gen2.lang.json`.

## 12. Sources

* The owner's request and `docs/labyrinth/mega_v2.md` (the binding contract); `docs/labyrinth/mega_core.md`, `mega_floors.md` (the code this extends).
* Vanilla block states: `tools/labyrinth/vanilla_block_states_1_21_1.json` (generated from the game data of 1.21.1 by an earlier builder). The Minecraft Wiki page on stairs
  (https://minecraft.wiki/w/Stairs) for the `facing`/`half`/`shape` meaning was recalled, not fetched.
* NeoForge's `FakePlayer` and `ServerTickEvent` as they are used elsewhere in this repository; no web page was fetched for this work.

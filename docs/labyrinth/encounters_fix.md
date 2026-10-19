# Direct encounters: why they did not trigger and why they spawned badly (1.94.0)

Owner report: "fix direct encounters" (the Dungeon Master's encounters are not triggering, and they spawn badly). Package `com.newuniverse.nusmp.labyrinth.mega.enc`,
config `nusmp-labyrinth-mega-enc-server.toml`. Nothing here was run in a game; section 7 is the honest list of what that leaves open. The design this fixes is
described in `mega_enc.md`; nothing in that document is replaced.

## 1. NEW / REPLACED / KEPT

* NEW: `Tension` (the pity timer), `SpawnLayout` + `CellFinder` + `SpawnSearch` (the ladder of places), `HuntRoute` (hunters follow the rooms), `StuckTracker`, `IdleReport`,
  `EncGates` (the `why` report), `EncFixSelfTests` (`enc.trigger`, `enc.spawn`), the commands `why` and `force` and the second name `enc` for the whole tree, nine config keys,
  `PatrolGraph.route`, `EncounterRoll.distribution` / `chance`, `NoiseEvents` counters, and the tests (section 6).
* REPLACED: the single ring of 1.54.0 as the only way of finding a place (it is now the first rung of a ladder and behaves exactly as before when it succeeds); the one shared budget
  of twelve places for a whole encounter (now per monster); the straight walk of a hunter at a player behind walls (now along the doorways, switchable); the "log the first failure of
  any kind and nothing else" rule of the Director; the creative-mode filter that could not be switched off (now `rollForCreative`). `CellSource.candidates` is kept as it was
  (unused by the Director now); `EncounterRoll.Mods` keeps its five-argument constructor.
* KEPT: the d20 maths, the bands, the budgets, the tables, the roster, the wandering bosses, the noise and light rules, the Mob Foundry and depth hooks, every older self-test.

## 2. What was checked and is fine (so nobody looks there again)

* The Director is registered on the server tick: `MegaSystem.register` passes `NeoForge.EVENT_BUS` to `EncRegistry.register`, which adds `EncounterDirector::onServerTick`
  (`MegaSystem.java:41`, `EncRegistry.java`); the config spec is registered (`NUSMP.java:203`) and `EncConfig.get` answers the default while the spec is not loaded. The new
  self-test `enc.trigger` proves it at run time (the handler's last tick is recent) and `why` prints it.
* The noise inputs are fed: `NoiseEvents.onGameEvent` / `onDamage` are registered on the same bus. `why` now prints how many game events reached the handler and how many fed a meter.
* Floor sign: `ThreatInput`, `FloorMap.threat`, `DepthRules.depthOf`, `FoundryScaling.tierOf` all take the absolute value; `MegaDungeon.floorAt` never answers null (it answers the
  sector's first floor for an unbanded room); the sign of a floor, up or down, plays no part in the search either (it sees boxes and markers). Pinned by `EncFixPureTest`.
* Party and dimension: only players of the Mega-Dungeon level are rolled for; the instanced party id only separates groups. Tables: all 12+ themes have a table, the fallback by road
  and then `default` exists, the shipped files parse with zero problems (`EncounterDataTest`), and a theme without a table now shows in `why` as a borrowed one.
* Geometry of the ring: on 3,519 party positions over real planned floors (both roads, the old and the cyclic generator, the real template catalog, a line of sight made from the
  plan itself) the 1.54.0 ring alone found a place for 99.8 percent. So "there is no cell 24 to 48 blocks away" is NOT the main cause; the ladder is the cheap insurance, not the cure
  (`EncRealFloorSpawnTest` prints the numbers). The causes are in the gates, the dice, the world-side checks and the aftermath, below.

## 3. The bugs, with file and line (lines of the code as it was at 1.93.0), the fix and the test

| # | Where (old) | Bug | Fix | Test |
|---|---|---|---|---|
| 1 | `EncounterRoll.java:62-80`, `EncConfig` `encounterDc` 17, `LightRules.mod` | The dice. A party in the dark on the first floor totals light -3, threat 0 (`threat / 2`), noise 0, party 0 against DC 17: only a natural 20 reaches it, one pass in twenty (a hundred seconds on average, and then a cooldown of ninety). The docs' own table assumed a light modifier of 0. A player waiting for the first encounter reads this as "never". | `Tension`: every `tensionEvery` (2) quiet passes add 1 to the party's d20, up to `tensionCap` (5); an encounter spends it. Dark first floor: 100 s becomes 36 s on average; the worst case the game can make (-3 light, -3 table) 58 s. `tensionCap = 0` is the old behaviour. | `EncounterReachTest` (exact odds for every modifier combination, the pity maths, the worst case) |
| 2 | `EncounterDirector.java:144`, `:166` | Creative and Spectator players are never rolled for, with no switch. An owner who tests in Creative sees nothing, ever. | `rollForCreative` (default false, hunts honour it); the `why` command names it. | `why` names the gate; no test (a game-mode switch) |
| 3 | `EncounterDirector.java:136`, `:575-579` | The result of every tick pass was thrown away, and `warn` logged the first failure of any kind in the session and then stayed silent (a handler that threw on every tick left one line and a dead Director). Every `NO_PLACE`, `QUIET`, `COOLDOWN` was invisible. | Distinct failures are logged once each with their stack and counted; the latest is shown by `why`; the outcome of every pass feeds `IdleReport`, which writes one INFO line a minute while players are in the dungeon and nothing spawned (`idleLogSeconds`, 0 = off): the reasons with counts and the latest detail of each. | `EncFixPureTest.anIdleMinute...` |
| 4 | `EncounterDirector.java:194`, `:406-414`, `:451-484` | The crowd lock. A spawned group that cannot reach the party (a maze) stands where it is until vanilla despawns it; eight idle monsters stop every later encounter (`CROWDED`). The count was also taken round the party's centre. | `StuckTracker`: a monster that has not moved 2.5 blocks for `stuckTicks` (900) is not counted, and the sweep removes it when it has no target and no player can see it (a monster whose entry says `hunt: 0` is exempt). The count is round every member. | `EncFixPureTest.aMonsterThatStaysPut...` |
| 5 | `EncounterDirector.java:255`, `:296` | A roll that the dice won and the world could not place (`NO_PLACE`, `TOO_SMALL`) was lost: no cooldown, no memory, a fresh roll in five seconds at the same bad odds. | The band stays owed: the next `retryPasses` (3) passes try the same band again without rolling; the tension is kept until something spawns. | none in JUnit (it is state of the Director); `enc.trigger` drives `pass` |
| 6 | `EncounterDirector.java:253`, `CellSource.java:29-50` | Candidate cells were taken round the CENTRE of the party. A party of three spread over 90 blocks has a centre that is far from all of them. | `CellFinder.candidates` measures reach from every member's eyes. | `SpawnSearchTest.theCandidatesAreTakenRoundEveryMember...` |
| 7 | `EncounterDirector.java:61`, `:254`, `SpawnPicker.java:62` | The picker was allowed 24 ray casts IN TOTAL, one per member per candidate: a party of four looked at six candidates. | 48 per member per rung (`SIGHT_CHECKS_PER_EYE`). | `SpawnSearchTest`, `EncRealFloorSpawnTest` |
| 8 | `EncounterDirector.java:299-317`, `EncounterSpawner.java:77-85` | One budget of twelve places for the WHOLE encounter, eleven of which one bad cell spends on its own offsets. Result: the first monster placed, the rest not, or none. | A budget of `4 x spawnAttempts` places per monster (total cap 400, three monsters in a row without a place end it); a monster that fails at its cell falls back on the group's other cells, then the picker's reserve (6), then the mob's own `LandRandomPos` (a spot on its own ground). Every refusal is counted by reason (`chunk floor fluid block collision too_close too_far seen budget`) and printed. | `enc.spawn` (in game) |
| 9 | `EncounterSpawner.java:87-100`, `CellSource.java:50` | A cell is a row of the floor plan, not a measured floor: the made-up cells stand at the absolute row 65 and a place is judged only at the cell's own row, so a step, a sunken floor or a floor built at another height is "no floor". | `RoomBox.standY` is measured from the room's own box (floor row + 1); the placement looks 4 rows up and down for a floor with room for a body. | `SpawnSearchTest.aFloorBuiltAtAnotherHeight...` |
| 10 | `CellSource.java:42` | Corridors and junctions (two thirds of the rooms of a floor) contributed no cell unless they carried a spawn marker. | The `every room` rung reads them, with the cells just inside each doorway. | `SpawnSearchTest.aFloorOfCorridors...`, `theDenseRung...` |
| 11 | `EncounterDirector.java:432-445` | Hunting. A hunter was told to walk straight at the nearest player once a second; the pathfinder looks only as far as the follow range and then walks to the explored point nearest the goal, which in a maze is a dead end against the nearest wall. It also aimed at a player behind a wall, after which the Director stopped steering it and its own goals did the same blind walk. | `HuntRoute`: a hunter in another room walks to the waypoint of the next room on the shortest way along the doorways (`PatrolGraph.route`, rooms a monster may not enter excluded); it aims only when it can see the player, and a target it cannot see is released so the rooms can steer it. `huntByRooms = false` is the old walk. | `EncFixPureTest.theRoute...`, `aHunterWalks...`, `everyHunterOfAMaze...` |
| 12 | (missing) | No way to see why nothing happened. | `why` (every gate with its value, the verdict), `force [band]` (spawn one now and say which gate stopped it), the INFO line of bug 3, and `enc` as a second name for the whole `dm` tree. | `EncFixPureTest.theFirstGateThatBlocks...` |
| 13 | `EncounterDirector.java:124-139` | `server.getTickCount()` wraps negative after 3.4 years and the `% 100 == 37` tests (sweep, hunt) stop matching. | Masked to non-negative. | review |
| 14 | `EncounterDirector.java:227` | A band with no usable entry fell back only DOWN. A thin table or tags with no member in the toughness window gave `NO_ENTRY`. | One step up is tried, paid for with the rolled band's budget. | review |

## 4. The new ladder for a place (`SpawnSearch`)

| Rung | Ring | Cells | Sight rule |
|---|---|---|---|
| 0 ring | the entry's, else `spawnMinDistance`-`spawnMaxDistance` | markers, three floor cells per ordinary room (exactly 1.54.0) | whole |
| 1 wide ring | 0.6 x the minimum (never under 8) to 1.5 x the maximum | as 0 | whole |
| 2 every room | as 1 | every non-safe room incl. corridors, doorway cells (320 at most) | whole |
| 3 far and in view | to 2 x the maximum | as 2 | a seen cell is allowed three quarters of the ring's far edge away or more (`spawnRelaxSightTier` 4; 5 = never) |

Never nearer than 8 blocks, never in a safe or boss room, cells weighted by doorways from the party's rooms (1.0 up to two, 0.7 to five, 0.45 beyond, 0.15 unreachable).
`spawnWiden = false` keeps only rung 0.

## 5. Commands and self-tests

* `/multiverse mega dm why` or `/multiverse mega enc why`: the switch, the tick handler (ticks seen, last pass), dimension, your mode, sector (state, rooms, spawn markers), room, difficulty,
  table (borrowed or not), cooldown, crowd, party, noise and light (with the counters), modifiers with the tension, the dice (odds and the expected wait), the entries usable now per band,
  a dry run of the place search with its trace, your last roll, the latest failure, and the verdict ("Blocked at ..." or "Nothing blocks ...").
* `/multiverse mega dm force [minor|standard|hard|ambush]` (also `enc force`): spawns one now (dice, cooldown, crowd cap and safe-room filter skipped; default standard); on failure it prints the text and the first gate that blocks.
* `enc.trigger`: with the default configuration, on the generated sector 0, a stand-in party standing still in an ordinary room is met within 20 passes (2000 ticks at the default interval) in
  one of up to six rooms, using the real `pass` (dice, tension, choice, search, spawn); the handler must have run recently; the pass right after a spawn must say `COOLDOWN`.
* `enc.spawn`: forced encounters in up to eight rooms; every monster alive, in a loaded chunk, on solid ground, in no fluid, in a clear gap, at least 8 blocks and inside its rung's distances,
  out of sight (unless it is the last rung and far), in the sector, in no safe room; at least three rooms must produce one; a ring nothing can satisfy (150-160 blocks) still finds a place
  with the ladder and none without it.

## 6. Config keys added (all in `[enc]`)

`rollForCreative` false, `tensionEvery` 2, `tensionCap` 5, `spawnWiden` true, `spawnRelaxSightTier` 4, `huntByRooms` true, `retryPasses` 3, `stuckTicks` 900, `idleLogSeconds` 60.
`spawnAttempts` keeps its default (12) and changes meaning: it is per monster (times four places), not per encounter.

## 7. Tests and what could not be verified

JUnit (pure, 284 pass together with the older enc tests): `SpawnSearchTest` (12), `EncounterReachTest` (8), `EncFixPureTest` (10), `EncRealFloorSpawnTest` (3, over real planned floors); eight
mutations of the new pure code were each killed (the dense rung reading safe rooms, no hard minimum, a hunter that never moves on, an inverted stuck test, no pity bonus, a route through
forbidden rooms, a verdict that ignores blocks, a sight rule eased nowhere).

Not verified (nothing ran in a game): that the ring-and-sight model of the real-floor test matches the game's ray casts through pillars and furniture; every world-side check of the
spawner (`standing`, `noCollision`, `LandRandomPos`) against real chunks; whether Tensura's brains obey a released target and a walk target set from outside; the toughness windows against
Tensura's real health values (the `entries` line of `why` shows the usable counts per band); how often vanilla's own despawn removes hunters 30 blocks out before they arrive; and the
balance of the pity timer (an encounter about every two minutes for a quiet party in the dark on the first floor; `tensionCap`, `encounterDc` and the cooldown tune it). The first thing to
do in a game is `/multiverse mega dm why` while standing in a room: it names the gate that stops the Dungeon Master.

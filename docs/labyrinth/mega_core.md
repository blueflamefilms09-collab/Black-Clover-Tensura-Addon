# Mega-Dungeon core: assembler, planner, generator, expansion, travel, command (1.54.0, builder CORE; two roads since 1.55.0)

This is the WHY of the generator half of the Mega-Dungeon (`labyrinth/mega/gen`, `labyrinth/mega/travel`), the exact vanilla APIs it uses, the recipe schema, the config
keys, the commands, the self-tests and an honest list of what was never run. The master design and the binding connector contract are `docs/labyrinth/mega.md`
(sections 4, 5, 6 are the spec of this package) and, for the two roads, `docs/labyrinth/mega_floors.md`; read those first. Section 14 is what 1.55.0 changed and what the
first run of the engine on the real templates showed.

**State.** Nothing here ran in a game. The pure half (assembler, planner, audits, plan writer, simulator) is covered by JUnit (197 tests in 23 classes, run locally with a
small JUnit shim; CI runs the real JUnit) and was mutation-checked. The game half (pool adapter, generator, placer, queue, travel, command, self-tests) was type-checked
against stubs regenerated from the real API dump (see "How it was checked"), never executed.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status | Notes |
|---|---|---|
| The pure assembler (`MegaAssembler`, `Shape`, `Socket`, `Rot`, ...), the planner, the audits, `PlanJson`, `MegaSim` | NEW (1.54.0) | Nothing like it existed; the Spire used vanilla `JigsawPlacement`. |
| `VanillaPools`, `SectorGenerator`, `SectorPlacer`, `ExpansionQueue`, `GenData`, `GeometryCheck` | NEW (1.54.0) | The game glue. |
| `MegaCommand` (`/multiverse mega ...`) | NEW (1.54.0) | The lead attaches it. `SelfTestCommand` (the lead's) is attached beside it. |
| `MegaTravel` | REPLACEMENT of the contract stub body (`enter` returned false) | The signature is unchanged. |
| `MegaGenRegistry` | REPLACEMENT of the contract stub bodies | `MegaSystem` already calls `init` and `register`. |
| `MegaConfig` | KEPT, one key APPENDED (`genBudgetMs`) | No key renamed or removed. |
| `data/nusmp/mega/recipe/default.json` | NEW (1.54.0) | The Sector Recipe (section 6). Unchanged in 1.55.0. |
| `LabyrinthTravel`, the Spire, the old Gates and Maws, `MegaDungeon`, `Sector`, `MegaData`, every other contract class | KEPT, not touched | |
| The Gateway structure (`nusmp:mega_gateway`) | NOT BUILT here | Built by the architecture builder. `MegaTravel` honours its structure tag. |
| **1.55.0** Two roads: `Expander.requestFirst(server, Polarity)`, the lane origins, `SectorDraft`, `SectorThreat`, the per-road obstacle rule (`WorldObstacles`), `ExpandRule.refusalForNext` | NEW | Section 14. |
| **1.55.0** `MegaTravel.enterChain` (the generator's `ChainTravel`), the per-road memory of the deepest Landing | NEW | `MegaTravel.enter` is KEPT and now means "up the Holy road". |
| **1.55.0** `MegaCommand`: `expand [holy\|abyssal]`, `status` per road, the road and floors on every sector line, `unseal` that does not wait for the vault | REPLACEMENT of the single-chain `expand` and `status` | The words the CI script reads are kept as the prefix of every line. |
| **1.55.0** Self-tests `core.roads`, `core.chain_travel` | NEW | The six 1.54.0 tests are KEPT with road-aware bodies. |
| **1.55.0** The Guardian-distance rule of the assembler (the hall's own centre, not the door it hangs from) | REPLACEMENT of a rule that made every real sector fall back | Section 14.2. |
| **1.55.0** `SectorRequests.effective`: a recipe promise that no template of the door pool can keep is waived and logged | NEW | |
| **1.55.0** Test tools: `SyntheticCatalog.realSized`, `SimWorld` with two roads, `MegaSim --road/--roads/--real`, `MegaCatalogTest` and `MegaSim` that wait for the boss chain | NEW / CHANGED | |
| **1.55.0** `threat = 1 + id` and the single-chain theme draw for NEW sectors | REPLACED (sectors already saved keep their saved values) | `SectorThreat`, `SectorDraft`. |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The pipeline in one picture

```
 ServerTick  ->  ExpansionQueue (one job at a time, FIFO)
                    PLAN   : SectorGenerator.plan  -> SectorRequests.prepare (tunnel, start, exclusions, policy) -> SectorSolver.solve (<= assembleTries seeds,
                             MegaAssembler + SectorAudit each, then the fallback layout)            [pure; a few ms]
                             SectorGenerator.build -> PoolElementStructurePiece per plan piece + world markers
                    PLACE  : SectorPlacer.step    -> piecesPerTick pieces, within genBudgetMs, each inside RegionContext + DungeonGuard.internal
                    FINISH : SectorGenerator.fill (SectorFill) -> saved Sector (rooms, edges, markers, landmarks), state ACTIVE, GenData stats,
                             rubble seal of the previous sector removed, MegaDungeon.generated(...) -> listeners, "the way is open" to players nearby
```

Why it is split like this: the plan is a pure function of SAVED data (seed, entry door, growth direction, relic count, the earlier sectors), so a sector that was half placed
when the server stopped is simply planned again and placed again (the repair), and `/multiverse mega regen` is the same operation on demand. Everything that decides
anything is pure Java under JUnit; the game classes only read data in, turn the plan into the game's piece class and place it.

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. The WHY of every class

### Geometry and data (pure)

| Class | Why |
|---|---|
| `Facing` | North/east/south/west with clockwise turns; the assembler must turn pieces exactly as the game does, and one small enum keeps that in one place. |
| `Vec`, `Box` | Cells and inclusive boxes. `BlockPos` / `BoundingBox` cannot be used: the assembler must run under JUnit with no game on the class path. `Box.overlaps` means "shares a cell", so touching is allowed; `Box.gapXZ` is the clear distance two regions are kept apart by. |
| `Rot` | The single place where "a piece turned r quarter turns" becomes numbers: vanilla `CLOCKWISE_90` is (x,z) -> (-z,x), `CLOCKWISE_180` (-x,-z), `COUNTERCLOCKWISE_90` (z,-x); a piece's box is the min/max of the turned corners moved by the origin. Pinned by hand-computed cases in `RotationTest` and checked against the game by `GeometryCheck`. |
| `Socket`, `Shape`, `ShapeMarker`, `PoolDef`, `PoolEntry`, `PoolSource` | A template as the assembler sees it (size, kind from the folder, jigsaw sockets, data markers, all in the template frame) and a pool as weighted shapes + empty weight + fallback. `PoolSource` is the seam: the game's data pack in the game, `catalog.json` or a synthetic catalog in tests. |
| `Rng` | SplitMix64, no `Math.random`, no hash-order dependence: the same seed gives the same sector on every JVM. |
| `WeightedDraw` | Vanilla's draw (each element repeated `weight` times, shuffled, first appearance decides the order) re-done on `Rng`, with the empty element as "end of the primary candidates". |
| `BoxIndex` | A 9-block grid index so "does this box overlap anything placed" is not a walk over 450 boxes for every candidate. |
| `Placed` | One piece of a plan (shape, rotation 0..3, origin, depth, parent, parent socket) plus its used-socket flags. `origin` is the world cell of template cell (0,0,0), the position the game's piece is built with. |
| `Recipe` | The Sector Recipe as a plain bag of numbers with the defaults of the design (section 6). |
| `SectorDraft`, `SectorThreat` | 1.55.0: everything decided when a sector is CREATED, as one pure function: its road, its place in the road, its floors, its threat (the distance from Floor 0 of its first floor), its seed (the seed of its PLACE, so the dungeon does not depend on which road was opened first), its theme (the Crucible's choice first, else the road-biased draw) and its relic count. The game-side queue only copies the answer into a `Sector`. |
| `Request`, `Result` | One value in, one value out: a plan is a pure function of its request. |
| `OnwardPolicy` | The assembler must not know about other sectors, yet the Way Onward it chooses decides where the next sector can go; the policy is how the planner lends it that knowledge. |

### The assembler, the planner, the checks (pure)

| Class | Why |
|---|---|
| `MegaAssembler` | The vanilla Jigsaw DATA model with the placement LOOP replaced (design doc section 5): keeps out of other sectors, guarantees the boss chain, reports loops, runs thousands of seeds in a test. Details in section 4. |
| `SectorPlanner` | Where the next sector goes: regions, the tunnel between sectors, the Way Onward policy (tunnel length + a penalty per blocked way out + a penalty for curling inward), the obstacle lists. Geometry mistakes cannot be repaired afterwards (two overlapping sectors overwrite each other for ever), so it is pure and tested over ten thousand random expansions. |
| `LinkBuilder` | The tunnel is not searched for, it is built: whole 9-block segments from the old seal to the new Landing door, a waystation every 8th. |
| `SectorRequests` | Builds the `Request` for "sector N behind this seal with these neighbours": the generator and the simulator both need exactly this, and one function means the simulator proves something about the game. |
| `SectorSolver` | One random plan is not enough: tries seeds `trySeed(seed, 0..n-1)`, audits each, prefers a live continuation over a dead end, falls back to the smallest sector that is still a sector, and reports why tries failed. |
| `SectorAudit` | The promises a PLAN must keep before anything is placed (one arena, a vault with `relicCount` pedestals, a seal facing free space, minimum rooms and loops, no overlaps, reachability). Sentences never contain a semicolon: the command joins them with one. |
| `SectorFill` | Decides which marker is "the" guardian spawn, what the arrival cell is, where the pedestals are (one below their markers): the landmarks every other system trusts. The game side only copies its answer. |
| `SectorView`, `SavedAudit` | The audit of the SAVED sector (`/multiverse mega audit`): the boss chain exists once each, rooms inside the region and not overlapping, markers in the rooms they belong to, pedestals match the relics, the seal lies in an onward corridor, every room reachable from the Landing. |
| `WorldObstacles` | The rule "a sector keeps out of the sectors BEFORE it, of its OWN road" (section 5). |
| `LandingRule`, `ExpandRule`, `CommandLines` | Small decisions with awkward corners (which Landing of a road, which sector of a road grows and when a next sector may be requested at all, the exact words the CI script parses) made on plain values so tests pin them. |
| `PlanJson` | The plan as JSON for `tools/labyrinth/mega_preview.py --plan` and for the lead's compositor (every piece with template, rotation, origin, parent). |

### The game glue

| Class | Why |
|---|---|
| `VanillaPools` | Reads the pool JSONs with the resource manager (not the vanilla pool registry, whose element lists are private, so order and weights are exactly the file's), the templates with `StructureTemplateManager`, and the Sector Recipe. A reload listener; warms its template cache a few templates per tick after start-up. Never asks the game to rotate anything. |
| `SectorGenerator` | Saved sector -> request -> solver -> pieces and world markers -> filled `Sector`. Also the probe plan used by the self-tests. |
| `SectorPlacer` | Places a few pieces per tick inside `DungeonGuard.internal` and `RegionContext.run`, with a short chunk ticket per piece, and clears the previous sector's rubble seal. One piece that throws is logged and skipped. |
| `ExpansionQueue` | The `Expander`: FIFO of jobs, PLAN / PLACE / FINISH stages, progress, repair after a stop, regeneration, player feedback, the log lines the CI reads. |
| `GenData` | The generator's own bookkeeping per sector (pieces, caps, loops, tries, fallback, ms) in a second saved-data file, so the contract `Sector` stays what the other builders need. |
| `GeometryCheck` | Asks the game itself whether the plan's rotation maths and placement agree with vanilla (section 9). |
| `MegaCommand`, `CoreSelfTests`, `MegaGenRegistry` | The command, eight self-tests, the wiring (the `Expander` and the `ChainTravel` are registered with the facade here). |
| `travel.MegaTravel` | Where a Gate, a Maw, the enter command or a Hub gate sends a player (section 7). |

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The assembler, step by step (what differs from or adds to the design doc)

1. **Start.** The Landing is drawn from `nusmp:mega/landing`, and for each candidate socket the unique rotation that makes it attach is computed
   (`origin = parentSocketCell + dir(parentFacing) - rotate(r, candidateSocketCell)`; the facings must be opposite). The tunnel pieces of the request (a later sector) are installed first.
2. **Regions (model B).** A region is 171 blocks square (19 modules), 48 high, centred on the Landing's door and growing forward; consecutive regions are at least
   `GAP = max(REGION_GAP, 2 * CAP_MARGIN) = 16` blocks apart, and the tunnel between two sectors is a run of whole 9-block segments from the old seal to the new Landing door
   (at least `ceil((GAP + 1 + far - front) / 9)`, never fewer than 2).
3. **First fill** (breadth first). The far band of the region is reserved for the boss chain (its depth is measured from the real shapes when `reserveDepth` is 0), and door caps are not placed yet.
4. **The boss chain** is a backtracking search with an undo journal (an arena whose own CENTRE is at least `arenaMinDistance` modules from the Landing's centre and whose door is deep enough in the tree, then its antechamber, the vault pool forced by the relic count with the PEDESTAL
   marker count checked, then the Way Onward variant the policy prefers). It collects up to `chainLeaves` complete chains and installs the one whose Way Onward the policy rates best (shortest
   tunnel, most ways out for the next region, leading outward). A seal facing a wall of older sectors is a DEAD END: still built, rated worst, and a live one is preferred on a later try.
5. **Extras**: the extra safe room(s) at the recipe's distance, at least one trial, at least two treasure closets, at most one set piece.
6. **Second fill, caps, loops.** The reserve is released; nothing may touch the keep-clear strip in front of the seal; door caps may enter only the two outer columns of that strip (the tunnel's wall
   overwrites them). An EMPTY element inside a pool ends the primary candidates and the search continues with the fallback pool: an uncapped doorway facing free space would open onto the void.
7. **Audit and retry.** Up to `assembleTries` seeds, each audited in full; then the fallback layout (Landing and boss chain only, distance rules off) audited without the pacing checks; else the sector is FAILED
   with the reasons in the log.

**Numbers over the synthetic catalog** (`MegaSim --synthetic 200 out --chain 16`): 200 of 200 seeds pass the full audit, 0 need the fallback, 0 dead ends; tries avg 1.02; pieces 154 / 173.1 / 199
(min / avg / max); rooms 108 / 122.9 / 143; loops 70 / 93.2 / 119; door caps avg 50.2; fill 82.5% / 89.9% / 97.5%; 13.1 ms per seed. 16-sector worlds: 200 of 200 grew fully; the closest two regions came
18 blocks (the gap is 16). These numbers say the algorithm works; they say nothing about the real templates (the catalog was empty when this was written).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. Determinism, regeneration and the obstacle rule

A sector regenerated from its saved data must be the sector that was placed, or the new rooms would not meet the tunnel and the neighbour that already hang from the old ones. The plan depends on:
the sector's saved seed, entry door, growth direction and relic count; the Way Onward and tunnel of the sector it hangs from; the regions and tunnels of the OTHER sectors; the pools, the recipe and the config
(`maxPiecesPerSector`, `maxLinkModules`, `assembleTries`).

The trap is the other sectors. When sector N was first planned only sectors 0..N-1 existed. A replay that also sees N+1 sees N+1's tunnel starting at N's own seal and judges that seal blocked, then picks
another Way Onward. `WorldObstacles.of(known, before, skipRegion)` therefore returns only the regions and tunnels of sectors with a LOWER id. `RegenReplayTest` grows simulated dungeons and replays every
sector with that rule (identical plan, piece by piece, same winning try); with the naive rule (every other sector is an obstacle) 35 of 35 replays came out different, which is why the rule exists.

**Two roads (1.55.0).** The same rule is applied per road: `WorldObstacles.of(known, before, skip, road)` keeps only the sectors of the sector's own road, because the two roads start
`MegaGrid.LANE_Z` blocks apart and can never collide, and because the planner's sense of "outward" (`SectorPlanner.radial`) measures from the centre of everything explored, which with
two roads would be a point halfway between them. Without the filter a Holy sector replays to a DIFFERENT plan once the Abyssal road has grown (checked: `RoadsTest.everySectorOfTwoInterleavedRoadsReplaysToTheIdenticalPlan` fails
when the filter is taken out). The sector seed is the seed of the PLACE (`SectorDraft.seedKey`), not of the id, so the Holy road's third sector is the same sector in every world with the same dungeon seed whichever road was opened first.

Limits that cannot be removed: a changed data pack (templates, pools, recipe) or a changed config value (`assembleTries` lowered below the winning try, `maxPiecesPerSector`, `maxLinkModules`) changes the plan.
`/multiverse mega audit` and `verify` report when the saved rooms no longer match the plan; `core.determinism` does the same for sector 0.

A regeneration keeps the sector's progress state (ACTIVE, GUARDIAN_DEAD, UNSEALED, COMPLETE) and re-opens its rubble seal when the next sector exists (the template puts the rubble back).

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. The Sector Recipe: `data/nusmp/mega/recipe/default.json`

Keys that start with an underscore are comments. A key the loader does not know is logged and ignored (a typo must not silently keep a default); a value of the wrong type or range is clamped and logged.
`RecipeTest` fails if this file and the defaults compiled into `Recipe` drift apart. Distances are in modules (9 blocks) between box centres in the horizontal plane. Reload with `/reload`.

| Key | Default | Range | Meaning |
|---|---|---|---|
| `maxDepth` | 24 | 2..64 | Longest chain of doors from the Landing; deeper doors only get caps. |
| `minRooms` | 40 | 1..1000 | The full audit rejects a plan with fewer rooms (caps and tunnel excluded). |
| `minLoops` | 3 | 0..1000 | ... fewer loops (two unused doorways facing each other across a shared wall). |
| `arenaMinDistance` | 8 | >= 0 | The Guardian's hall's own CENTRE is at least this many modules from the Landing's centre (1.55.0: it used to be the doorway the hall hangs from, see 14.2) ... |
| `arenaMinDepth` | 5 | 0..64 | ... and at least this many doors deep. |
| `reserveDepth` | 0 | 0..15 | Depth in modules of the band at the far side kept free for the boss chain; 0 = measure the chain pools. |
| `extraSafe` | 1 | 0..10 | Extra SAFE rooms besides the Landing, at `safeMinDistance`..`safeMaxDistance` modules from the Landing. |
| `safeMinDistance` / `safeMaxDistance` | 5 / 12 | >= 0 | |
| `min` | `{"trial": 1, "treasure": 2}` | object | Least rooms per kind; keys are `RoomKind` folder names (`trial`, `treasure`, `hall`, ...). |
| `max` | `{"set": 1, "trial": 2, "trap": 2, "puzzle": 2}` | object | Most rooms per kind. The room builders meant the trap, puzzle and trial rooms to be about one per sector: at most two of each (the dark room is a trap). |
| `shapeMax` | `{"nusmp:mega/hall/great_hall": 1, "nusmp:mega/set/cathedral_rotunda": 1, "nusmp:mega/trap/dark_room": 1}` | object | Most uses of one template id per sector (a template's own `max` also applies; 0 lifts a default). The two landmarks (3 x 5 and 5 x 5 modules) and the dark room never repeat inside a sector. |
| `chainSockets` | 120 | 1..5000 | Candidate doorways tried for the Guardian's hall before a seed is given up. |
| `chainLeaves` | 10 | 1..200 | Complete boss chains compared before the best Way Onward is installed. |
| `stepBudget` | 600000 | >= 1000 | Runaway guard on attach attempts in the boss-chain and extras search (not a tuning knob). |
| `capReserve` | 60 | 0..1000 | Pieces kept out of the first fill for the chain, the extras and the caps. |
| `requireMarkers` | true | boolean | Does the audit demand ARRIVAL, exactly one GUARDIAN_SPAWN, GATEKEEPER, VAULT_CENTER, EXPANSION_SOCKET? |

**A promise the pools cannot keep is waived (1.55.0).** `min` names kinds of room the door pool (`nusmp:mega/side`) must supply and `extraSafe` needs the `nusmp:mega/safe` pool. When the pool holds no
readable template of a kind that `min` asks for (the treasure closets are drawn by another builder than the halls and arrive in a later merge), no seed can ever keep the promise, and failing every try
would turn every sector into the 7-piece fallback layout. `SectorRequests.effective` therefore drops that one promise for the plan (on a copy: the recipe itself is never changed) and `SectorGenerator.plan` logs
a WARN per sector; the audit is held to the effective recipe. A kind that IS offered keeps its promise. `/multiverse mega pools` shows which pool is thin.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. Travel (`MegaTravel`)

Two ways in. `enterChain(player, polarity)` is the `ChainTravel` the generator registers with the facade (`MegaDungeon.setChainTravel(MegaTravel::enterChain)` in `MegaGenRegistry.init`): it is what
the Hub's polarity gates call through `MegaDungeon.enterChain`. `enter(player, from)` is the 1.54.0 entry that the old Gates, the Maws, `/multiverse labyrinth enter` and a server with `hub.enabled = false`
use (`LabyrinthTravel.enter` routes here while `mega.layout` is `"mega"`); it leads up the Holy road, which is what 1.54.0's single chain became. Both do the same, in order:

1. The dimension must exist (else a message). A player already waiting for THIS road is accepted silently (a gate calls the entry every tick the player stands in it).
2. For the old gates only: with `legacyGates` off a player who is not already in a Labyrinth dimension must stand inside a `nusmp:mega_gateway` structure
   (`level.structureManager().getStructureWithPieceAt(pos, Labyrinth.MEGA_GATEWAY_TAG)`), else a message points at the Gateway (at most one message per player every three seconds).
3. The Directional Lock: `MegaDungeon.mayEnter(player, road)`. The lock tells the player why when it refuses. Asked again when a waiting player is finally released, because the lock may have closed meanwhile.
4. The Landing is chosen by `LandingRule` over the CHAIN INDICES of the road: the first finished sector when sneaking or `lastLanding` is off, else the deepest Landing the player ever stood in ON THIS ROAD,
   clamped down to the newest finished sector of the road (a sector being built is not a place yet). The memory is a persistent int per road, `nusmp_mega_landing_holy` and
   `nusmp_mega_landing_abyssal` (the deepest chain index reached). The 1.54.0 key `nusmp_mega_landing` (a sector id; the rest anchors write it too) is still written and still read, as a hint for the road its sector belongs to.
   Sector ids are global and interleave the two roads, so an id says nothing about depth, which is why the memory is the chain index. `rememberLanding` raises both every two seconds while the player stands in a LANDING room,
   so reaching a Landing by any road counts.
5. With no finished sector on the road: the road's first sector is queued (`ExpansionQueue.tryFirst(server, road)`) and the player WAITS: an action-bar line every two seconds
   (`message.nusmp.mega.core.waking`), automatic entry the moment the first sector is playable. A player who walks more than 64 blocks away, changes dimension, dies or logs out stops waiting, a failed first sector tells the
   player which sector to `regen` and drops the wait, and a wait never lasts more than ten minutes.
6. The teleport is `LabyrinthTravel.arrive(p, level, pad, message)` with the sector's ARRIVAL marker cell as the pad (else the middle of its first Landing room). From a Hub gate the player is not in a Labyrinth dimension,
   so `arrive` remembers the gate as the way home. The quest keys `labyrinth.entered` and `mega.entered` (argument: the sector id) are fired.

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Commands (`/multiverse mega ...`, permission 2, every one works from the console except `tp`)

| Command | Output (exact words are pinned by `CommandLinesTest`) |
|---|---|
| `status` | `mega: expansionLevel=<n> sectors=<n> job=<idle or <percent>%>`, then for EACH road (Holy first) a header `road <holy\|abyssal>: sectors=<n> ids=<id,id> floors=<first>..<last> state=<STATE of its newest sector>` (or `road <id>: not opened`) followed by the sector lines of that road: `sector <id> <STATE> theme=<theme id> threat=<n> rooms=<n> edges=<n> pieces=<n> relics=<n> region=<x1>,<z1>,<x2>,<z2> road=<id> chain=<k> floors=<first>..<last>` (floors signed: `+1..+10`, `-11..-20`) |
| `sector [id]` | The sector at the player, else the id, else the newest (console): the same sector line, then indented lines for entry/arrival, guardian/gatekeeper/vault, Way Onward/seal/next/prev and the generation stats. |
| `expand [holy\|abyssal]` | The named road (Holy when none is named): its first sector when the road has none, else the next sector off its newest sector that has a Way Onward and no successor, whatever the state of that sector's Guardian, Gatekeeper and Crucible. ONE line: `expand: queued sector <id> on the <road> road (floors <first>..<last>)` or `expand: refused (<reason>)`. |
| `regen <id>` | Places a sector again from its saved seed: `regen: queued sector <id>` or `regen: refused (<reason>)`. |
| `audit <id>` | `audit <id> OK` or `audit <id> FAIL: <failure>; <failure>` (the saved sector, `SavedAudit`). |
| `verify [id]` | `verify <id> OK pieces=.. boxes=.. jigsaws=.. markers=.. world=<found>/<sampled> worldPieces=..` or `verify <id> FAIL: ...` (`GeometryCheck`). |
| `plan <id>` | Writes `<server dir>/mega_plan_<id>.json` (the plan the saved seed gives now) and prints `plan: <path>`. |
| `rooms [id]` | One line `<kind> <count>` per room kind (lower case, `set_piece`; door caps are never rooms). |
| `unseal <id>` | The admin's Gatekeeper key: `MegaDungeon.unseal`; then, because `mega.expandAfterVault` holds the next sector back until the vault is resolved and an admin's key must not wait for it, the held-back expansion is requested at once: `unseal: sector <id> unsealed, the next sector was requested (sector <n>, the vault was skipped)`. With `expansion` off nothing is requested and the line says so. |
| `tp <id>` | Player only: to that sector's Landing pad. |
| `pools` | The pools the data pack provides, with how many of their templates cannot be read (the first thing to check when a sector FAILS). |

The log lines the CI reads: one INFO per generated sector `MEGA sector <id> <STATE> rooms=<n> edges=<n> pieces=<n> loops=<n> tries=<n> ms=<n> road=<id> chain=<k>` (STATE is ACTIVE for a new sector), and one WARN when the
fallback layout or a retry was needed, when a sector is a dead end, when a recipe promise was waived, or when a piece failed to place. A FAILED sector logs `MEGA sector <id> FAILED: <reasons>`.

Plan file (`PlanJson`): `label, seed, tries, fallback, region [x1,z1,x2,z2], entry [x,y,z,"dir"], audit {ok, failures[]}, rooms [{id, kind, template, link, box[x1,z1,x2,z2]}], doors [[x,z,"dir"]], loops [[x,z]],
pieces [{template, rotation 0..3, x, y, z, depth, parent, kind, link, cap, box[x1,y1,z1,x2,y2,z2]}] (EVERY piece, door caps and the tunnel included; the world cell of template cell c is
origin + rotate(rotation, c), CLOCKWISE_90 = (x,z) -> (-z,x)), expansion {piece, seal[x,y,z], facing, size[w,h,d], deadEnd} or null, stats {...}, warnings [...]`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Self-tests (`/multiverse mega selftest core.*`)

Registered from `MegaGenRegistry.init` with `SelfTests.register`; each runs in well under two seconds and places nothing.

| Key | What it proves |
|---|---|
| `core.assemble` | Two probe sectors on each road's lane, from the real pools and templates, assemble without the fallback and pass the saved-sector audit (with the relic count the road's first sector really has). |
| `core.determinism` | The same probe seed gives the same plan twice and a different seed a different one; the first sector of each road, as saved, equals the plan its saved seed gives now (when it exists). |
| `core.geometry` | `GeometryCheck` on the first finished sector (Holy, else Abyssal): the game's bounding box, jigsaw cells and data-marker cells equal the plan's for every piece; a sample of solid template blocks stands where the plan says in up to 8 pieces; no jigsaw is left at a planned socket cell. Plan-only when no sector is built (the answer says so). |
| `core.seal` | The newest finished sectors with a Way Onward: sealed (rubble in the seal box, nothing in the three cells outside) or, once the next sector exists, open with a tunnel floor outside the seal. |
| `core.travel` | The old entry (the Holy road): `LandingRule` answers for a newcomer, a deep memory (the per-road key), the 1.54.0 key and a sneaking player; `MegaTravel.enter` puts a `FakePlayer` on the Landing pad; standing on a deeper Landing raises both remembered depths. |
| `core.expand` | The registered `Expander` is the queue; a second expansion off a sector that has a successor is refused with the right reason; for EACH road, a dry run (the real request and solver, nothing saved) of the sector behind the road's newest free Way Onward is the right draft (same road, next chain index, same floors per sector, the floors continue, the threat of its floors, the seed of its place), plans, passes the saved audit, and its region is 16 blocks or more from every other region of the road. |
| `core.roads` | Both lanes plan at their origins (z 0 and `MegaGrid.LANE_Z`); each saved chain is whole (chain indices 0..n-1 in order, every sector leads to the next, one number of floors for the road, the floors continue, the threat of the floors, the sector lies near its lane); no region of one road is near the other; a road that exists refuses a second first sector and nothing is created by asking. |
| `core.chain_travel` | For each road with a finished sector: `MegaDungeon.enterChain` (the facade, so the registration is proved too) puts a `FakePlayer` on the pad of the road's first sector; a deep memory on one road offers the newest sector of THAT road and leaves the other road's offer alone; sneaking offers the first sector. Roads that are not opened are named, not skipped silently. |

`GeometryCheck` is also `/multiverse mega verify`; it is the first thing to run on a real server (section 11).

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Config

`nusmp-labyrinth-mega-server.toml`, section `[mega]` (the contract's `MegaConfig`; this builder appended `genBudgetMs`):

| Key | Default | Used here for |
|---|---|---|
| `layout` | `"spire"` (the integration commit flips it to `"mega"`) | Routing in `LabyrinthTravel.enter` (the lead's). |
| `expansion` | true | Whether `MegaDungeon.unseal` asks for the next sector (`expand` ignores it). |
| `legacyGates` | true | The old Gates and Maws also lead in (`MegaTravel`). |
| `lastLanding` | true | Entering takes the player to the deepest Landing (`MegaTravel`, `LandingRule`). |
| `piecesPerTick` | 3 (1..16) | Most pieces placed in one tick. |
| `maxPiecesPerSector` | 450 (100..2000) | Piece budget of one plan, door caps included. |
| `maxSectors` | 500 | The dungeon stops growing. |
| `assembleTries` | 24 (1..64) | Seeds tried before the fallback layout. |
| `maxLinkModules` | 40 (0..60) | Longest tunnel the planner may stretch to get round an older sector (the minimum bridge is always allowed). |
| `genBudgetMs` | 30 (5..200) | Milliseconds one tick may spend placing pieces (at least one piece is always placed, never more than `piecesPerTick`). The one key this builder appended. |
| `expandAfterVault` | true | 1.55.0, the lead's key: `MegaDungeon.unseal` holds the next sector back until the Crucible is resolved or a player reaches the Way Onward. `ExpansionQueue.requestNext` works at any time after the Guardian is dead either way, and the admin's `unseal` does not wait. |

No new config spec: nothing to register beyond the contract's. The number of floors per sector is `FloorMap.DEFAULT_PER_REGION` (10) and is saved in the sector (`Sector.perRegion`); a sector takes the length of the road it continues.

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. Vanilla and NeoForge APIs relied on, and how each was verified

"dump" = present in the javap dump of the real jars (`api_lab`), and the code type-checks against stubs generated from it; "repo" = the identical call already compiles in the repository; "memory" = in no
dump and not used elsewhere in the repository (from memory of 1.21.1 / NeoForge 21.1: the first CI compile is its check).

| API | Used in | Verified |
|---|---|---|
| `StructureTemplateManager.get(ResourceLocation) : Optional<StructureTemplate>` | `VanillaPools`, `GeometryCheck` | dump |
| `StructureTemplate.getSize()`, `filterBlocks(BlockPos, StructurePlaceSettings, Block)`, `save(CompoundTag)`, `StructureBlockInfo.pos()/state()/nbt()` | `VanillaPools`, `GeometryCheck` | dump |
| `JigsawBlock.getFrontFacing(BlockState) : Direction` | `VanillaPools`, `GeometryCheck` | dump |
| `StructurePoolElement.single(String, Holder<StructureProcessorList>)`, `.apply(StructureTemplatePool.Projection.RIGID)` | `SectorGenerator` | dump |
| `new PoolElementStructurePiece(StructureTemplateManager, StructurePoolElement, BlockPos, int, Rotation, BoundingBox, LiquidSettings)`, `LiquidSettings.APPLY_WATERLOGGING` | `SectorGenerator` | dump |
| `PoolElementStructurePiece.place(WorldGenLevel, StructureManager, ChunkGenerator, RandomSource, BoundingBox, BlockPos, boolean)`, `getElement()`, `getPosition()`, `getRotation()` | `SectorPlacer`, `GeometryCheck` | dump |
| `StructurePoolElement.getBoundingBox(mgr, pos, rot)`, `getShuffledJigsawBlocks(mgr, pos, rot, RandomSource)`, `SinglePoolElement.getDataMarkers(mgr, pos, rot, boolean)` | `GeometryCheck` | dump |
| `ServerLevel.getStructureManager()`, `structureManager()`, `getRandom()`, `getChunk(int,int)`, `getEntitiesOfClass(...)`, `getBlockState`, `setBlock` | glue | dump |
| `ServerChunkCache.getGenerator()`, `addRegionTicket(TicketType<T>, ChunkPos, int, T)`, `TicketType.create(String, Comparator<T>, int)` | `SectorPlacer` | dump |
| `BoundingBox(int x6)`, `minX()..maxZ()`, `infinite()`, `equals` | glue | dump |
| `RegistryAccess.registryOrThrow(Registries.PROCESSOR_LIST)`, `Registry.getHolder(ResourceKey)`, `Holder.direct`, `new StructureProcessorList(List)` | `SectorGenerator` | dump |
| `StructureManager.getStructureWithPieceAt(BlockPos, TagKey<Structure>)`, `StructureStart.isValid()` | `MegaTravel` | dump, repo (`LabyrinthCommand`) |
| `CommandSourceStack.getPlayer()`, `getServer()`, `sendSuccess(Supplier<Component>, boolean)`, `hasPermission(int)`; `IntegerArgumentType`; `Commands.literal/argument` | `MegaCommand` | dump, repo |
| `ServerTickEvent.Post`, `PlayerTickEvent.Post`, `PlayerEvent.PlayerLoggedOutEvent`, `AddReloadListenerEvent.addListener` | `MegaGenRegistry` | dump, repo |
| `FMLPaths.GAMEDIR.get() : Path` | `MegaCommand.plan` | dump |
| `ServerPlayer.teleportTo`, `displayClientMessage(Component, boolean)`, `sendSystemMessage`, `isShiftKeyDown()`, `tickCount` | `MegaTravel` | dump |
| `Entity.getPersistentData()` (NeoForge `IEntityExtension`) | `MegaTravel`, `CoreSelfTests` | repo (`LabyrinthTravel`); not in the dump |
| `MinecraftServer.overworld()`, `getLevel(ResourceKey)`, `getPlayerList().getPlayer(UUID)`, `getTickCount()` | glue | repo (`MegaData`, `LabyrinthTravel`, `DreamWorld`, `ForbiddenMagic`); not in the dump |
| `ResourceManager.listResourceStacks(String, Predicate<ResourceLocation>) : Map<ResourceLocation, List<Resource>>`, `Resource.open() : InputStream`, `ResourceManagerReloadListener.onResourceManagerReload(ResourceManager)` | `VanillaPools` | repo (`ChatterReloadListener` makes the identical calls); not in the dump |
| `ServerStartedEvent`, `ServerStoppedEvent` (`getServer()`) | `MegaGenRegistry` | repo (`AvatarAiEvents` for stopped); `ServerStartedEvent` memory |
| `FakePlayer`, `FakePlayerFactory` | the lead's `SelfTests`, `core.travel` | in no dump (the lead's file) |

The game half was compiled with `javac -proc:none` against stubs built by `tools/typecheck/stubgen.py` from the LAB dump (which, unlike the repository's dump, contains `levelgen.structure`), with a small
supplement for the classes in no dump and two dump lines patched (`Direction.Axis` / `Plane` bridge methods) so `Direction.getAxis()` survives. The only errors left are known stub gaps: the `SavedData.Factory`
record constructor, `BlockPos` being generated abstract, and `getPersistentData()` (the same lines fail in the lead's contract `MegaData`). A deliberate wrong call was reported by the same run (negative control).

---------------------------------------------------------------------------------------------------------------------------------------------

## 12. Not verified, and what the lead should know

* **Nothing ran in a game.** CI compiles; the CI smoke test is the first run.
* **The marker rotation.** The design doc says `getDataMarkers(.., false)` returns positions with the rotation applied. From memory of `StructureTemplate.filterBlocks`, `relativePosition = false` returns the raw
  template cell and `SinglePoolElement.place` transforms it afterwards. To be independent of which is right, this package never calls the game's marker reader to PLACE anything: markers are the template cells
  turned by `Rot`. `GeometryCheck` compares them with `getDataMarkers(.., true)` and prints what both settings return when they disagree.
* **`piece.place` with an infinite box and a pivot.** Expected to behave like `/place jigsaw` (same call, whole piece at once, the sector's entry as pivot, which only vanilla processors that use it for noise would
  notice). `core.geometry` / `verify` read the placed blocks back (sampled solid template cells must be non-air at the planned cells; no jigsaw may remain at a planned socket cell).
* **The cost of placing a sector.** A sector is about 120 pieces (the real templates, section 14) and a hundred and more chunks. Placement is sliced (`piecesPerTick`, `genBudgetMs`), pieces hold a ten-second chunk ticket, but the real cost
  (light updates, chunk generation of the void dimension, saving) is unmeasured: watch the first expansion.
* **`FakePlayer` and a same-level teleport** (`core.travel`, `core.chain_travel`) depend on NeoForge's `FakePlayer` having a usable connection; if a test fails with "the player stands at ..." that is the harness, not the travel code
  (the decision part of each test is checked first).
* **Real templates.** The repository's catalog (81 templates today) has no Guardian's hall, antechamber, vault or Way Onward yet (other builders draw them), so `MegaCatalogTest` and `MegaSim` say "cannot make whole sectors yet" and pass.
  The engine WAS run on a local composite of everything that exists in the builders' worktrees (section 14.1); re-run `python3 tools/labyrinth/mega_catalog.py` and `MegaSim` on the merged tree before trusting any number.
* **The two roads never ran in a game.** The first sector at z 300 000 (chunk generation, the tickets, placement), `enterChain` from a real Hub gate (the way home, the Directional Lock) and a server whose world border in `nusmp:mega_dungeon` is
  smaller than 300 000 blocks (the Abyssal road would lie outside it; vanilla's default border is +-29 999 984. Whether a dimension shares the Overworld's border in 1.21.1 was NOT settled: a web search found the
  Minecraft Wiki saying `/worldborder` affects the dimension it is run in, https://minecraft.wiki/w/Commands/worldborder, which could not be fetched here) are the first things to watch.
* **The default layout is still `"spire"`** (contract), so nothing here runs until the integration commit flips `mega.layout`; `/multiverse mega expand [holy|abyssal]` works regardless.
* **A dead end** (a sector whose Way Onward faces older sectors) is built anyway and logged; the dungeon cannot grow past it. None in 1000 simulated sectors on the real-template composite; the policy avoids it when any candidate allows.
* `maxLinkModules` limits how far a tunnel is STRETCHED beyond the minimum bridge; the minimum bridge is always allowed (a sector must be reachable).
* **SAFE's rest anchors write the 1.54.0 landing key** (`nusmp_mega_landing`, `RestEvents.LANDING_KEY`) as `max(old, sector id)`. With two roads that max mixes them (a rest on a high-id Abyssal sector hides a rest on a lower-id Holy one). It is harmless here
  (the per-road keys are the memory; the old key is only a hint), but whatever one day reads it as "the respawn Landing" must read the sector it names, not compare ids across roads.

---------------------------------------------------------------------------------------------------------------------------------------------

## 13. Running things locally

* Unit tests: `WT=<worktree> KEY=<key> bash <scratch>/mega/jt.sh com.newuniverse.nusmp.labyrinth.mega.gen.<TestClass> ...` (the local shim; run it from the project directory, `RecipeTest` and `MegaCatalogTest` read files by relative path) or the real JUnit through Gradle in CI.
* The simulator (test scope): compile `src/test/java/.../gen/MegaSim.java` with `-sourcepath src/main/java:src/test/java`, then
  `java -cp <out> com.newuniverse.nusmp.labyrinth.mega.gen.MegaSim <catalog.json | --synthetic | --real> <seeds> [planOutDir] [--chain N] [--road holy|abyssal] [--roads]`. It prints one line per seed (tries, fallback, pieces, rooms, caps, link,
  loops, safe, depth, fill %, ms), then the failure rate, min / avg / max, the reasons tries failed for, and with `--chain N` the closest two regions of a road and how many worlds grew fully (`--roads` grows both roads side by side in every world and
  also reports the closest regions of the two roads). `--road abyssal` puts the first sector on the Abyssal lane; `--real` is the synthetic stand-in with the boss chain drawn to the real sizes. The first five plans are
  written to `planOutDir` for `python3 tools/labyrinth/mega_preview.py --plan plan_0.json map.png` (rectangles by room kind) or `python3 tools/labyrinth/mega_compose.py plan_0.json map.png --root <resources dir>` (the real templates' floor rows).
* On a real server, in this order: `/multiverse mega pools`, `/multiverse mega expand`, wait for `MEGA sector 0 ACTIVE` in the log, `/multiverse mega verify`, `/multiverse mega audit 0`,
  `/multiverse mega selftest core.geometry`, then `/multiverse mega expand abyssal`, the same checks on its sector, and `/multiverse mega selftest core.roads` and `core.chain_travel`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 14. 1.55.0: two roads, and what the first run on the real templates showed

### 14.1 The decisions and their WHY

| Decision | Why |
|---|---|
| The first sector of a road stands at the road's lane origin: door cell `(0, 64, MegaGrid.laneZ(road))`, forward east. | The contract: the Holy road is where 1.54.0 put sector 0 (so an old world is unchanged), the Abyssal road starts `LANE_Z` = 300 000 blocks to the side where nothing of the Holy road can ever reach. |
| Sector ids stay global (the newest + 1); a sector's road and place are `polarity` + `chainIndex`. | Every other module keys its data by the id; only the generator and the travel need the chain. `MegaData.chain(road)` / `chainSector` / `chainTail` are the lookups. |
| `requestNext(level, from)` continues the road of `from`; `ExpandRule.refusalForNext` is the whole rule of when it may. | The facade holds the request back until the Crucible is resolved (`expandAfterVault`), so it may come at any time after the Guardian is dead; only the sector's own condition (generating, failed, no Way Onward), an existing successor, `maxSectors` and a taken slot stop it. A second request for the same sector is refused, not built twice. |
| A sector keeps out of the sectors of ITS OWN road (`WorldObstacles` road filter). | The roads cannot collide; counting the other road makes a plan depend on how far it has grown and spoils the planner's "outward" measure. Pinned by a mutation check (section 5). |
| The seed is the seed of the PLACE: `sectorSeed(dungeonSeed, SectorDraft.seedKey(road, chainIndex))` with key = chainIndex * 2 (+1 for Abyssal). | Whichever road the players open first, the Holy road's third sector is the same sector in every world with that dungeon seed. The key of the first Holy sector is 0: 1.54.0's sector 0 keeps its seed. |
| Threat = `FloorMap.threat(first floor)`: 1, 11, 21 ... on both roads (`SectorThreat`). | The brief measures difficulty by the distance from Floor 0. The FIRST floor keeps the 1.54.0 number of the first sector, matches `MegaDungeon.floorAt`'s answer for a sector whose rooms are not numbered, and the encounter director reads the floor of the PLACE anyway. A module that wants the Guardian's number reads `FloorMap.threat(sector.lastFloor())`; `SectorThreat.of` is the one line to change if the owner wants the sector-wide number to follow the last floor. Suggested for the lead: `MegaDungeon.threatAt` could return `FloorMap.threat(floorAt(level, pos))` when the floor is known, so the faction drops and the rest ambush scale per floor instead of per sector. |
| Theme: `MegaDungeon.themeOverride(previous)` first, else `RegionTheme.pick(seed, chainIndex, previousTheme, polarity)`. | The Crucible's relic decides the next ten floors; the first sector of a road wears the road's signature (RADIANT / ABYSS). |
| `perRegion` of a new sector is its predecessor's (10 for a first sector). | Floors are numbered `chainIndex * perRegion + band`; two lengths in one road would number floors twice. |
| The memory of the deepest Landing is per road, as a chain index (`nusmp_mega_landing_holy` / `_abyssal`); the 1.54.0 key stays as a hint. | Ids interleave the roads; a chain index grows with depth on its own road. |
| `unseal <id>` (admin) requests the held-back expansion at once. | An admin's key is a shortcut; the CI script and every test that unseals expects the next sector. |
| `SectorRequests.effective` waives a recipe promise no template of the door pool can keep. | Section 6: without it the missing treasure closets would turn every sector into the 7-piece fallback. |

### 14.2 What the first run on the real templates showed: every sector fell back (fixed)

The repository's catalog has no boss-chain templates yet (the Guardian's hall and the Way Onward are the guard builder's, the vaults the theme builder's, the treasure closets and most halls the rooms builder's).
To see the engine on real content anyway, a composite catalog was built OUTSIDE the repository from the repository's 81 templates plus those builders' uncommitted worktrees (the arena 2, the antechamber 1, the Ways Onward 6,
the vaults 6, the halls 28, the treasure closets 3 = 118 templates, 15 pools, written by the repository's own `gen_mega_pools.py` and `mega_catalog.py` in a scratch copy). **With the code as it was, 40 of 40 seeds failed to
find a boss chain and fell back to the 7-piece layout** (`no boss chain: ... arenaFits=6 anteFits=6 vaultFits=8 onwardNone=8`). The cause is arithmetic, not a template defect:

* the recipe says the Guardian's hall must hang from a doorway at least 8 modules from the Landing's centre, and the assembler measured the DOOR;
* the real hall is 5 modules across, the whole chain (hall, antechamber, the Way Onward and the vault on its two sides) needs a band 12 modules deep at the far end of the 19-module region, and the first fill leaves
  the band untouched, so every doorway on the near edge of the band is only 5.6 modules from a 3-module Landing: too close, except at the far corners of the edge, where the chain's wings (about 90 blocks across, and
  the single antechamber has its Way Onward always on the same hand) do not fit.

The rule is now applied to the hall itself: its own centre must be at least `arenaMinDistance` modules from the Landing's centre (`MegaAssembler.ensureChain`, the `where` predicate of the arena attach). The intent of the
recipe (the Guardian is deep in the sector) is kept; the door at the edge of the band is two and a half modules nearer than the hall. `AssemblerTest.theBossChainOfTheRealSizesIsFoundWithoutManyRetries` pins it on a stand-in with the
real boss-chain sizes (1.1 tries on average with the new rule, 2.4 with the old one: checked by putting the old rule back). Nothing in any pool or template was touched.

### 14.3 The numbers (`MegaSim`)

Second composite, the one behind these numbers: the working branch as of the lead's note (103 templates: halls, treasure closets, ROOMS-2, SAFE, ARCH, the guard builder's arena 2, antechamber 2, Ways Onward 9) plus the theme builder's six
vaults from its worktree = 122 templates, 15 pools, with the recipe of section 6 including the room builders' notes (`max` trial / trap / puzzle 2, `shapeMax` great hall, rotunda and dark room 1).

| Run | Result |
|---|---|
| 1000 sectors, Holy lane | 1000 pass the full audit, 0 fallbacks, 0 dead ends; tries 1.25 on average (p95 2, max 5); pieces 50 / 118.8 / 158 (min / avg / max); rooms 42 / 92.9 / 126 (p5 67, 18 below 55); loops 3 / 17.7 / 44; door caps avg 26.0; fill 46.5 % / 88.4 % / 99.4 % (p5 72 %, 34 below 66 %); safe rooms (Landing excluded) median 2; deepest door median 14; about 9 ms per sector on an idle machine |
| 40 sectors, Abyssal lane | 40 pass, 0 fallbacks; rooms 41 / 94.7 / 115; loops 4 / 18.5 / 35; fill 46.5 % / 89.2 % / 97.2 %; tries 1.40 (the same plan the Holy lane would give, moved 300 000 blocks: `RoadsTest`) |
| 40 worlds, both roads, 12 sectors each | 40 of 40 grew fully; closest two regions of one road 18 blocks (the gap is 16); closest regions of the two roads 295 671 blocks |
| Rooms per kind per sector (300 sectors) | trial 1-2, trap 1-2, puzzle 1-2 (before the cap: up to 5, 7 and 6); the great hall in 19 sectors, never twice; **the cathedral rotunda in only 37 of 300 sectors (12 %)**: the recipe caps the set piece at one but nothing asks for one, and a 5 x 5 piece is hard to place; add `"set": 1` to `min` if the owner wants it in every sector (it costs retries) |
| The first composite (118 templates, the recipe before the room builders' notes) | 1000 of 1000 pass, 0 fallbacks, tries 1.48, rooms 40 / 88.1 / 124, fill 44.9 % / 85.1 % / 98.9 % |
| `MegaCatalogTest` on the first composite | 200 of 200 pass the audit, 0 fallbacks; 6 worlds of 6 sectors grow |
| The repository's catalog today | `MegaCatalogTest` and `MegaSim` print which pools are empty (the vault pools, until the theme builder lands) and pass; the old behaviour would have FAILED the build on the incomplete catalog |

What the pictures show (`previews/mgc/` in the session's scratch directory, listed in the report): dense, tabletop-style maps with the Guardian's hall in the far two thirds of the region, the antechamber beside it, the vault on one
hand and the Way Onward on the other with its seal facing out of the region, loops and crossroads everywhere; about one sector in thirty is thin (fill under 66 %, a few hundred blocks of empty region). Raise `minRooms` in the recipe
(55 rejects under 2 % of tries) if the owner wants fewer thin ones; no pool weight was touched.

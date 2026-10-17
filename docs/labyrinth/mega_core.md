# Mega-Dungeon core: assembler, planner, generator, expansion, travel, command (1.54.0, builder CORE)

This is the WHY of the generator half of the Mega-Dungeon (`labyrinth/mega/gen`, `labyrinth/mega/travel`), the exact vanilla APIs it uses, the recipe schema, the config
keys, the commands, the self-tests and an honest list of what was never run. The master design and the binding connector contract are `docs/labyrinth/mega.md`
(sections 4, 5, 6 are the spec of this package); read that first.

**State.** Nothing here ran in a game. The pure half (assembler, planner, audits, plan writer, simulator) is covered by JUnit (about 190 tests, run locally with a small
JUnit shim; CI runs the real JUnit) and was mutation-checked. The game half (pool adapter, generator, placer, queue, travel, command, self-tests) was type-checked against stubs
regenerated from the real API dump (see "How it was checked"), never executed.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

| Part | Status | Notes |
|---|---|---|
| The pure assembler (`MegaAssembler`, `Shape`, `Socket`, `Rot`, ...), the planner, the audits, `PlanJson`, `MegaSim` | NEW | Nothing like it existed; the Spire used vanilla `JigsawPlacement`. |
| `VanillaPools`, `SectorGenerator`, `SectorPlacer`, `ExpansionQueue`, `GenData`, `GeometryCheck` | NEW | The game glue. |
| `MegaCommand` (`/multiverse mega ...`) | NEW | The lead attaches it. `SelfTestCommand` (the lead's) is attached beside it. |
| `MegaTravel` | REPLACEMENT of the contract stub body (`enter` returned false) | The signature is unchanged. |
| `MegaGenRegistry` | REPLACEMENT of the contract stub bodies | `MegaSystem` already calls `init` and `register`. |
| `MegaConfig` | KEPT, one key APPENDED (`genBudgetMs`) | No key renamed or removed. |
| `data/nusmp/mega/recipe/default.json` | NEW | The Sector Recipe (section 6). |
| `LabyrinthTravel`, the Spire, the old Gates and Maws, `MegaDungeon`, `Sector`, `MegaData`, every other contract class | KEPT, not touched | |
| The Gateway structure (`nusmp:mega_gateway`) | NOT BUILT | Named under CORE in the master doc's package table but not in the CORE brief; see section 12. `MegaTravel` already honours its structure tag. |

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
| `Vec`, `Box` | Cells and inclusive boxes. `BlockPos` / `BoundingBox` cannot be used: the assembler must run under JUnit with no game on the class path. `Box.overlaps` means "shares a cell", so touching is allowed. |
| `Rot` | The single place where "a piece turned r quarter turns" becomes numbers: vanilla `CLOCKWISE_90` is (x,z) -> (-z,x), `CLOCKWISE_180` (-x,-z), `COUNTERCLOCKWISE_90` (z,-x); a piece's box is the min/max of the turned corners moved by the origin. Pinned by hand-computed cases in `RotationTest` and checked against the game by `GeometryCheck`. |
| `Socket`, `Shape`, `ShapeMarker`, `PoolDef`, `PoolEntry`, `PoolSource` | A template as the assembler sees it (size, kind from the folder, jigsaw sockets, data markers, all in the template frame) and a pool as weighted shapes + empty weight + fallback. `PoolSource` is the seam: the game's data pack in the game, `catalog.json` or a synthetic catalog in tests. |
| `Rng` | SplitMix64, no `Math.random`, no hash-order dependence: the same seed gives the same sector on every JVM. |
| `WeightedDraw` | Vanilla's draw (each element repeated `weight` times, shuffled, first appearance decides the order) re-done on `Rng`, with the empty element as "end of the primary candidates". |
| `BoxIndex` | A 9-block grid index so "does this box overlap anything placed" is not a walk over 450 boxes for every candidate. |
| `Placed` | One piece of a plan (shape, rotation 0..3, origin, depth, parent, parent socket) plus its used-socket flags. `origin` is the world cell of template cell (0,0,0), the position the game's piece is built with. |
| `Recipe` | The Sector Recipe as a plain bag of numbers with the defaults of the design (section 6). |
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
| `WorldObstacles` | The rule "a sector keeps out of the sectors BEFORE it" (section 5). |
| `LandingRule`, `ExpandRule`, `CommandLines` | Small decisions with awkward corners (which Landing, which sector grows, the exact words the CI script parses) made on plain values so tests pin them. |
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
| `MegaCommand`, `CoreSelfTests`, `MegaGenRegistry` | The command, six self-tests, the wiring. |
| `travel.MegaTravel` | Where a Gate, a Maw or the enter command sends a player (section 7). |

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The assembler, step by step (what differs from or adds to the design doc)

1. **Start.** The Landing is drawn from `nusmp:mega/landing`, and for each candidate socket the unique rotation that makes it attach is computed
   (`origin = parentSocketCell + dir(parentFacing) - rotate(r, candidateSocketCell)`; the facings must be opposite). The tunnel pieces of the request (a later sector) are installed first.
2. **Regions (model B).** A region is 171 blocks square (19 modules), 48 high, centred on the Landing's door and growing forward; consecutive regions are at least
   `GAP = max(REGION_GAP, 2 * CAP_MARGIN) = 16` blocks apart, and the tunnel between two sectors is a run of whole 9-block segments from the old seal to the new Landing door
   (at least `ceil((GAP + 1 + far - front) / 9)`, never fewer than 2).
3. **First fill** (breadth first). The far band of the region is reserved for the boss chain (its depth is measured from the real shapes when `reserveDepth` is 0), and door caps are not placed yet.
4. **The boss chain** is a backtracking search with an undo journal (arena far enough and deep enough from the Landing, then its antechamber, the vault pool forced by the relic count with the PEDESTAL
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
| `arenaMinDistance` | 8 | >= 0 | The Guardian's hall hangs from a doorway at least this many modules from the Landing's centre ... |
| `arenaMinDepth` | 5 | 0..64 | ... and at least this many doors deep. |
| `reserveDepth` | 0 | 0..15 | Depth in modules of the band at the far side kept free for the boss chain; 0 = measure the chain pools. |
| `extraSafe` | 1 | 0..10 | Extra SAFE rooms besides the Landing, at `safeMinDistance`..`safeMaxDistance` modules from the Landing. |
| `safeMinDistance` / `safeMaxDistance` | 5 / 12 | >= 0 | |
| `min` | `{"trial": 1, "treasure": 2}` | object | Least rooms per kind; keys are `RoomKind` folder names (`trial`, `treasure`, `hall`, ...). |
| `max` | `{"set": 1}` | object | Most rooms per kind. |
| `shapeMax` | `{}` | object | Most uses of one template id per sector (a template's own `max` also applies). |
| `chainSockets` | 120 | 1..5000 | Candidate doorways tried for the Guardian's hall before a seed is given up. |
| `chainLeaves` | 10 | 1..200 | Complete boss chains compared before the best Way Onward is installed. |
| `stepBudget` | 600000 | >= 1000 | Runaway guard on attach attempts in the boss-chain and extras search (not a tuning knob). |
| `capReserve` | 60 | 0..1000 | Pieces kept out of the first fill for the chain, the extras and the caps. |
| `requireMarkers` | true | boolean | Does the audit demand ARRIVAL, exactly one GUARDIAN_SPAWN, GATEKEEPER, VAULT_CENTER, EXPANSION_SOCKET? |

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. Travel (`MegaTravel`)

`LabyrinthTravel.enter` routes here while `mega.layout` is `"mega"`. In order: the dimension must exist; a player already waiting is accepted silently (a Gate calls `enter` every tick the player stands in
it); with `legacyGates` off a player who is not already in a Labyrinth dimension must stand inside a `nusmp:mega_gateway` structure (`level.structureManager().getStructureWithPieceAt(pos, Labyrinth.MEGA_GATEWAY_TAG)`),
else a message points at the Gateway (at most one message per player every three seconds). The Landing is chosen by `LandingRule`: the first playable sector when sneaking or `lastLanding` is off, else the
deepest Landing the player ever stood in (persistent int `nusmp_mega_landing`), clamped down to the newest ACTIVE-or-later sector (a sector being built is not a place yet). With no playable sector: sector 0 is
queued (`ExpansionQueue.tryFirst`) and the player WAITS: an action-bar line every two seconds (`message.nusmp.mega.core.waking`), automatic entry the moment sector 0 is playable; a player who walks more than
64 blocks away, changes dimension, dies or logs out stops waiting, a failed sector 0 tells the player and drops the wait, and a wait never lasts more than ten minutes. The teleport is
`LabyrinthTravel.arrive(p, level, pad, message)` with the sector's ARRIVAL marker cell as the pad (else the middle of its first Landing); the quest keys `labyrinth.entered` and `mega.entered` (argument: the
sector id) are fired. `rememberLanding` raises the stored depth every two seconds while the player stands in a LANDING room, so reaching a Landing by any road counts.

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Commands (`/multiverse mega ...`, permission 2, every one works from the console except `tp`)

| Command | Output (exact words are pinned by `CommandLinesTest`) |
|---|---|
| `status` | `mega: expansionLevel=<n> sectors=<n> job=<idle or <percent>%>` then one line per sector: `sector <id> <STATE> theme=<theme id> threat=<n> rooms=<n> edges=<n> pieces=<n> relics=<n> region=<x1>,<z1>,<x2>,<z2>` |
| `sector [id]` | The sector at the player, else the id, else the newest (console): the same sector line, then indented lines for entry/arrival, guardian/gatekeeper/vault, Way Onward/seal/next/prev and the generation stats. |
| `expand` | Sector 0 when there is none, else the next sector off the newest sector that has a Way Onward and no successor. ONE line: `expand: queued sector <id>` or `expand: refused (<reason>)`. |
| `regen <id>` | Places a sector again from its saved seed: `regen: queued sector <id>` or `regen: refused (<reason>)`. |
| `audit <id>` | `audit <id> OK` or `audit <id> FAIL: <failure>; <failure>` (the saved sector, `SavedAudit`). |
| `verify [id]` | `verify <id> OK pieces=.. boxes=.. jigsaws=.. markers=.. world=<found>/<sampled> worldPieces=..` or `verify <id> FAIL: ...` (`GeometryCheck`). |
| `plan <id>` | Writes `<server dir>/mega_plan_<id>.json` (the plan the saved seed gives now) and prints `plan: <path>`. |
| `rooms [id]` | One line `<kind> <count>` per room kind (lower case, `set_piece`; door caps are never rooms). |
| `unseal <id>` | The admin's Gatekeeper key: `MegaDungeon.unseal`; `unseal: sector <id> unsealed ...` or `unseal: refused (...)`. |
| `tp <id>` | Player only: to that sector's Landing pad. |
| `pools` | The pools the data pack provides, with how many of their templates cannot be read (the first thing to check when a sector FAILS). |

The log lines the CI reads: one INFO per generated sector `MEGA sector <id> <STATE> rooms=<n> edges=<n> pieces=<n> loops=<n> tries=<n> ms=<n>` (STATE is ACTIVE for a new sector), and one WARN when the
fallback layout or a retry was needed, when a sector is a dead end, or when a piece failed to place. A FAILED sector logs `MEGA sector <id> FAILED: <reasons>`.

Plan file (`PlanJson`): `label, seed, tries, fallback, region [x1,z1,x2,z2], entry [x,y,z,"dir"], audit {ok, failures[]}, rooms [{id, kind, template, link, box[x1,z1,x2,z2]}], doors [[x,z,"dir"]], loops [[x,z]],
pieces [{template, rotation 0..3, x, y, z, depth, parent, kind, link, cap, box[x1,y1,z1,x2,y2,z2]}] (EVERY piece, door caps and the tunnel included; the world cell of template cell c is
origin + rotate(rotation, c), CLOCKWISE_90 = (x,z) -> (-z,x)), expansion {piece, seal[x,y,z], facing, size[w,h,d], deadEnd} or null, stats {...}, warnings [...]`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Self-tests (`/multiverse mega selftest core.*`)

Registered from `MegaGenRegistry.init` with `SelfTests.register`; each runs in well under two seconds and places nothing.

| Key | What it proves |
|---|---|
| `core.assemble` | Three probe sectors from the real pools and templates assemble without the fallback and pass the saved-sector audit. |
| `core.determinism` | The same probe seed gives the same plan twice and a different seed a different one; sector 0 as saved equals the plan its saved seed gives now (when it exists). |
| `core.geometry` | `GeometryCheck` on sector 0: the game's bounding box, jigsaw cells and data-marker cells equal the plan's for every piece; a sample of solid template blocks stands where the plan says in up to 8 pieces; no jigsaw is left at a planned socket cell. Plan-only when sector 0 is not built (the answer says so). |
| `core.seal` | The newest finished sectors with a Way Onward: sealed (rubble in the seal box, nothing in the three cells outside) or, once the next sector exists, open with a tunnel floor outside the seal. |
| `core.travel` | `LandingRule` answers for a newcomer, a deep memory and a sneaking player; `MegaTravel.enter` puts a `FakePlayer` on the Landing pad; standing on a deeper Landing raises the remembered depth. |
| `core.expand` | The registered `Expander` is the queue; a second expansion off a sector that has a successor is refused with the right reason; a dry run (the real request and solver, nothing saved) of the sector behind the newest free Way Onward plans and passes the saved audit with the region 16 blocks or more from every other. |

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
| `genBudgetMs` | 30 (5..200) | NEW: milliseconds one tick may spend placing pieces (at least one piece is always placed, never more than `piecesPerTick`). |

No new config spec: nothing to register beyond the contract's.

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
* **The cost of placing a sector.** A sector is about 170 pieces and a hundred and more chunks. Placement is sliced (`piecesPerTick`, `genBudgetMs`), pieces hold a ten-second chunk ticket, but the real cost
  (light updates, chunk generation of the void dimension, saving) is unmeasured: watch the first expansion.
* **`FakePlayer` and a same-level teleport** (`core.travel`) depend on NeoForge's `FakePlayer` having a usable connection; if the test fails with "the player stands at ..." that is the harness, not the travel code
  (the decision part of the test is checked first).
* **Real templates.** `src/test/resources/mega/catalog.json` was empty when this was written, so `MegaCatalogTest` only printed its note; once the templates are merged and `tools/labyrinth/mega_catalog.py` has
  exported them it runs 200 real seeds. Run `MegaSim` on it before trusting any weight (section 13). `core.assemble` and `/multiverse mega pools` are the in-game versions of that check.
* **The default layout is still `"spire"`** (contract), so nothing here runs until the integration commit flips `mega.layout`; `/multiverse mega expand` works regardless.
* **The Gateway.** `nusmp:mega_gateway` (an Overworld structure in the `nusmp:dungeon_gate` tag) is listed under CORE in the master doc but was not in the CORE brief and is not built here. A worldgen JSON mistake
  cannot be caught without running the game, so it was left to the lead. Until it exists, keep `legacyGates` true (the default) or no entrance will open.
* **A dead end** (a sector whose Way Onward faces older sectors) is built anyway and logged; the dungeon cannot grow past it. About 1% of simulated sectors; the policy avoids it when any candidate allows.
* `maxLinkModules` limits how far a tunnel is STRETCHED beyond the minimum bridge; the minimum bridge is always allowed (a sector must be reachable).

---------------------------------------------------------------------------------------------------------------------------------------------

## 13. Running things locally

* Unit tests: `WT=<worktree> KEY=<key> bash <scratch>/mega/jt.sh com.newuniverse.nusmp.labyrinth.mega.gen.<TestClass> ...` (the local shim) or the real JUnit through Gradle in CI.
* The simulator (test scope): compile `src/test/java/.../gen/MegaSim.java` with `-sourcepath src/main/java:src/test/java`, then
  `java -cp <out> com.newuniverse.nusmp.labyrinth.mega.gen.MegaSim <catalog.json | --synthetic> <seeds> [planOutDir] [--chain N]`. It prints one line per seed (tries, fallback, pieces, rooms, caps, link,
  loops, safe, depth, fill %, ms), then the failure rate, min / avg / max, the reasons tries failed for, and with `--chain N` the closest two regions and how many worlds grew fully. The first five plans are
  written to `planOutDir` for `python3 tools/labyrinth/mega_preview.py --plan plan_0.json map.png`.
* On a real server, in this order: `/multiverse mega pools`, `/multiverse mega expand`, wait for `MEGA sector 0 ACTIVE` in the log, `/multiverse mega verify`, `/multiverse mega audit 0`,
  `/multiverse mega selftest core.geometry`.

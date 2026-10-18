# Anti-Dead-End Cyclic WFC (1.83.0+)

The owner's brief: dungeon generation that grows branching trees ends in frustrating dead ends; the WFC solver must be made to enforce cyclic graphs, so that open
sockets prefer to loop back into adjacent rooms or to be joined by small transitional hallways, with as few dead ends as possible, across five functional room
categories (Combat Chambers, Puzzles / Hazards, Lore / Scriptoriums, Transitions, Safe Rooms). This document is the WHY, the maths, the metrics and the config.
The code is `labyrinth/mega/cyclic/**` (pure) and `labyrinth/mega/gen/Cyclic*.java` (the seam to the real assembler).

Nothing here was run in a game. The pure layer and the integration over the real template catalog are unit tested (section 9); the game-side parts (config, saved
flag, self-test) are type-checked against API stubs only (section 11).

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. What the generator was (the diagnosis)

There is no grid solver. The Mega-Dungeon is built by `MegaAssembler`, a **jigsaw-style first-fit assembler** over the vanilla template-pool data. In WFC words:

| WFC | Mega-Dungeon |
|---|---|
| tile | a template (a room) turned 0..3 quarter turns |
| socket | a jigsaw block at the centre of a module (9 blocks) on a wall; `nusmp:mega_door` (ordinary) or one of the three boss-chain names |
| adjacency rule | `Socket.canAttach`: the source's target equals the candidate's name and the two face each other; every doorway is on the 9-block grid, so "line up" is an integer comparison |
| cell to collapse | the next open socket of a piece, in breadth-first order |
| collapse | draw templates from the socket's weighted pool; the **first** (template, rotation, socket) that fits inside the region, overlaps nothing and obeys the recipe limits is placed |
| contradiction | none: a socket that finds nothing is capped with a niche |

The sector (a floor since 1.58.0, about 28 rooms in a region of 13 to 19 modules) is: arrival stairwell -> first fill (to the room budget minus a reserve) -> boss
chain (boss floors) and the extras the recipe forces (safe room, trial, treasure) -> second fill (to the budget) -> forward stairwell on the doorway farthest from the
arrival -> door caps -> loops (two unused doorways that happen to face each other at the same module are a loop; nothing makes that happen).

**Where the dead ends come from**, measured on the shipped templates (264 floors: both roads, floors 1 to 22 with their boss floors, 6 seeds each;
`CyclicAssemblerTest`):

* The growth is a tree; its leaves are the places where the room budget ran out. Every room that was placed last, and every doorway left over, is a stub.
* First-fit is blind: it never asks whether the room it places lines its other doorways up with a neighbour. The old generator found 3.7 loops per floor by luck.
* **32.6 % of the rooms were dead ends** (degree 1, not the entrance, the stairs, the boss chain or a Safe Zone); not one of the 264 floors was under 8 %.
  Of the dead ends 61 % were corridors, junctions and caverns leading nowhere, 22 % single-door treasure closets (the recipe wants one; the pools draw more), 11 % halls, the rest
  trap, puzzle, trial and set-piece rooms. Only 34 % of the rooms lay on a cycle.
* The category mix was 64 % Transition (corridors and junctions), 13 % Combat, 13 % Lore, 5 % Puzzle / Hazard, 5 % Safe.

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| Pure cyclic layer `mega/cyclic/**` (room graph, metrics, socket rules, loop planner, hallway search, pruner, categories) | NEW |
| `gen/CyclicAssembly` (placement lookahead, the pass, the hallway builder), `gen/CyclicGraphs`, `gen/CyclicSettings`, `gen/CyclicSelfTests` | NEW |
| `generation.cyclic` and its knobs (`nusmp-labyrinth-mega-gen-server.toml`) | NEW. Default ON for sectors created from now on |
| `Sector.cyclic` (saved as `cyc`, only when true) | NEW (a field appended to the saved sector) |
| The old generator (a tree grown until the budget is spent) | **KEPT, selectable**: `generation.cyclic = false`, and every sector that existed before keeps it on every regeneration and repair |
| The room-count budget `mega.floorRooms`, the recipe, the audit, the pools, the templates | KEPT, not edited |
| Room categories | NEW view over the existing `RoomKind` (`RoomCategory.of(kind, template)`); the folders / kinds are unchanged |

Nothing was replaced. With the switch off, the generator is bit-for-bit the old one: 236 plans (floors of both roads and old banded sectors, seeds fixed) hashed to the same
value in the tree before and after this change.

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. The mathematics

Let G = (V, E) be the room graph: a node per room, an edge per doorway that joins two rooms. n = |V|, m = |E|, c = components (1 for a dungeon), and
mu = m - n + c, the **cyclomatic number** (the number of independent loops). T is the set of terminal rooms (entrance, stairs, boss chain, Safe Zones); a **dead end**
is a room of degree 1 (or 0) that is not in T; D is their set and n1 = |D|.

### 3.1 Dead ends are paid for by loops: n1 = B - 2(mu - 1)

By the handshake lemma, the sum over all rooms of (deg v - 2) = 2m - 2n = 2(mu - c). Rooms of degree 2 add nothing, rooms of degree 1 add -1 each, rooms of degree
b >= 3 add b - 2. Writing B = sum over rooms with degree >= 3 of (deg - 2) (the *branching excess*) and c = 1:

    n1_all = B - 2(mu - 1) = B + 2 - 2 mu          (n1_all counts every room of degree 1, terminals included)

A tree (m = n - 1, so mu = 0) has exactly B + 2 leaves: a branching tree is a dead-end factory, and the more it branches the more leaves it has.
Adding **one edge** between two rooms raises mu by one and raises two degrees by one:

* between two dead ends: both leave degree 1: n1 falls by **2**;
* between a dead end and a room of degree >= 2: n1 falls by 1 and B rises by 1;
* between two rooms of degree >= 2: n1 is unchanged and B rises by 2.

So **each independent loop can cure at most two dead ends, and only if both ends are dead ends.** The least number of links that brings the dead-end ratio to rho is
k_min = ceil((n1 - rho * n') / 2) (n' the room count afterwards). With the measured old floors (n about 29, n1 about 10 non-terminal dead ends, rho = 0.08, so n1' <= 2)
that is **4 links at the very least**, and in practice more, because partners are not always other dead ends. This is why the rule set below has three arms: join
(links), do not create (placement), take away (pruning).

### 3.2 Removing a leaf never cuts a walk

Let v be a non-terminal room of degree 1. Any simple walk between two other rooms that passes through v would have to enter and leave by the single edge of v, so it
cannot pass through v. Removing v therefore changes no distance between the remaining rooms, and cannot disconnect them. Repeating (the parent may become a leaf in
turn) removes exactly the *dangling* rooms: the closure of "remove a non-terminal room of degree 1". Terminals are never removed, so **the walk from the entrance to the
stairs, to the boss and to every Safe Zone cannot be cut by the pruner**. The loop pass only adds rooms and edges, so it cannot cut anything either, and distances only
shrink; the one case where "shorter" matters (the stairs must stay at least `stairMinWalk` doorways from the arrival) is a hard constraint of the planner
(`keepApart`). Both facts are property tests (section 9).

### 3.3 The loop-closing probability and its limits

Take a dead end l with q open doorways besides the one it hangs from. Doorway i faces a module cell f_i.

* **Direct loop.** f_i is inside a built room that has an open doorway at the module opposite, facing back (the "line up" rule: A faces B, front(A) = cell(B), front(B) = cell(A)).
  If phi is the fill ratio of the region (0.66 to 0.99 measured), delta the share of boundary modules that carry a doorway in the templates (0.3 to 0.5) and omega the share of
  those still open (about 0.4), then alpha = phi * delta * omega, about **0.1 to 0.15** per doorway in a layout nobody tuned. The old generator only had these by luck.
* **Hallway loop.** f_i is free ground. A hallway of at most H module cells (default 3) reaches any open doorway B whose front lies in the diamond of radius H-1 around
  f_i over free cells, entering it head-on. With rho_d open doorways per free cell facing free ground and N_H(free) the number of free cells in the diamond (at most 2H(H-1)+1 = 13
  for H = 3), the chance is p_h = 1 - exp(-rho_d * N_H(free)), which is small on the dense interior (N_H(free) is near zero) and large at the frontier of growth, exactly where
  the leaves are.

The chance that l can be cured by at least one doorway, if the doorways are independent, is

    P(cure) = 1 - (1 - alpha_eff)^q          with alpha_eff = 1 - (1 - alpha)(1 - p_h)

so a leaf with q = 3 open doorways and alpha_eff = 0.15 is cured with probability 0.39, one with q = 1 with 0.15, and **a room with q = 0 (a single-door closet) with probability
0: no loop can cure it.** Limits, in the order they bite:

1. q = 0: rooms with one doorway (treasure closets, stairwells, the arena). Handled by being destinations (stairs, boss), required (the recipe's treasure closet) or
   pruned (every other).
2. The frontier faces the region edge (the region is a fixed box) or older sectors: no free ground, N_H(free) = 0.
3. Each partner doorway can serve one link: the pairing is a matching problem; the planner is greedy by score, which is within a factor 2 of the maximum matching.
4. Budgets: `maxLinks` (16), `hallwayShare` (0.20 of all rooms may be hallway rooms), `maxHallway` (3 cells), and the minimum loop length (a link that closes a loop of fewer than
   4 doorways is refused: a triangle is not a detour).
5. Locked rooms (stairs, entrance, boss chain) take no new doorway: a second door into the arena would walk round the Gatekeeper.
6. A hallway that does not fit (the placement test of the assembler refuses it: region, exclusions, overlaps, recipe limits) is not built and the pair is never tried again.

What is left after the loops is the residual E[n1'] = sum over dead ends of (1 - P(cure)), reduced further by the matching conflicts; the pruner removes it down to the target.

### 3.4 The placement rule (why lookahead works)

A candidate (template, rotation, socket) for an open doorway has m other doorways, each aligning with an open doorway of the plan with probability alpha. The probability that a
single candidate yields at least one free loop is 1 - (1 - alpha)^m; the probability that **the best of L candidates** does is 1 - (1 - p1)^L with p1 = 1 - (1 - alpha)^m.
For alpha = 0.1 and m = 2, p1 = 0.19; for L = 12 candidates (each in every turn that fits) that is **0.92**. First-fit takes the first candidate (L = 1: 0.19). That is the whole of
the gain, and it is measured: independent loops per floor go from 3.7 (old) to 21 with the lookahead and the balance term off, and to about 10 with the balance term on (section 8).
The rule is the WFC analogue of "collapse the state that satisfies the most neighbour constraints"; the pool weights remain the prior, because the L candidates are drawn
by the pool's own weighted order and only the winner among them is chosen.

Left alone, the rule runs away: a corridor cross has four doorways, so its m is large and the lookahead picks corridors (89 % Transition measured with the balance term off).
Hence the category term (section 7).

### 3.5 Hallways keep the category balance bounded

A hallway room is a Transition. If a floor has N rooms of which T are Transitions and the pass adds h hallway rooms, the Transition share becomes (T + h) / (N + h), at most
h / (N + h) above T / N. The cap h <= 0.20 (N + h) bounds the shift by 0.20 in the worst case; measured it is about **+4 points** (66 % -> 70 % with the placement and the
balance off), and the full generator ends below the old generator (53 % against 64 %).

### 3.6 Cost

Pairs of open doorways D^2/2 (D about 30), hallway search at most 3^H leaves (H = 3: 27) per pair, graph metrics O(n + m) per candidate that survives the cheap filter (8 per
step), at most `maxLinks` steps. Placement lookahead: L candidates, each scored in O(pieces x sockets). A cyclic floor plans in about the same time as an old one (about 44 ms per floor either way on the build machine, the catalog
loaded) because it also needs fewer retries (1.00 tries against 1.11).

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The algorithms (what the code does, in order)

### 4.1 Socket matching (`cyclic/SocketRules`)

1. **Names**: `namesMatch(targetA, nameA, targetB, nameB)`: each target is the other's name. `mega_door` meets `mega_door`; `mega_chain`, `mega_vault`, `mega_onward` meet only themselves, so
   a loop can never open into the vault.
2. **Facing**: A points one way, B points the opposite way.
3. **Alignment**: `aligned(A, B)`: front(A) is the module B stands in and front(B) the module A stands in (integer cells; all pieces and all doorways sit on the 9-block grid).
4. **Placement score** (`placementScore`): the first free loop of a candidate +4 (second +2.5, each further +1.5), each own doorway walled into a neighbour's blank wall -1, each open doorway of
   somebody else it covers without matching -0.6, each doorway that still faces free ground +0.15, a leaf (no doorway but its parent's) -2.5 unless it is a destination, plus the category
   nudge (section 7). The first of equal scores wins, so with no preference the old behaviour (first fit) is what happens.

### 4.2 The loop pass (`cyclic/LoopPlanner`, driven by `gen/CyclicAssembly.pass`)

Runs once, after the second fill and before the forward stairwell and the door caps (so every loose doorway is still a doorway, and the stairs are chosen on the final graph).

1. Read the plan into a `RoomGraph` (rooms, tree joins, loops that already line up) and list the **open doorways** of rooms that may take one.
2. Candidates: every pair of open doorways of two different rooms. **Direct** if they line up; otherwise a **hallway**: `LoopPlanner.hallway` finds the shortest chain of free module cells (at most
   `maxHallway`), the first fed by doorway A and the last feeding doorway B, no cell twice, no U-turn, straight before bend, a fixed order (so the answer is deterministic). The cells are
   *straight* or *bent* pieces: one-module corridor templates with two doorways (`link/straight_*`, `corridor/bend_1x1_*`; the waystation is excluded).
3. Filters: the loop must be at least `minCycleFix` (4) doorways long when it cures a dead end, `minCycle` (6) otherwise; the hallway share and the link budget; every `keepApart` pair
   (arrival and forward stairs) must stay at least `stairMinWalk` apart.
4. Score = 100 per dead end cured + 1.5 per step of detour (capped at 8) - 4 per hallway cell + 6 per room newly on a cycle + a seeded jitter below 1 (ties break the same way on every machine).
5. Phase 1 cures dead ends until the ratio is at the target; phase 2 adds plain loops until `cycleTarget` (60 %) of the rooms lie on a cycle or the budget is spent.
6. **Building** (`CyclicAssembly.build`): each hallway cell is placed with the assembler's own `tryOne` (region, exclusions, blockers, overlaps, recipe limits, theme rules), hung from the doorway
   of the previous cell, the piece chosen straight or bent as the cell asks and verified against the planned cell and heading; the last piece's free doorway must line up with doorway B.
   If anything fails the hallway is taken back off and the pair is banned. The jigsaw join of a built hallway is real: both ends are tree joins or an aligned pair (the loop finder joins
   the last one), so **the placed doors are real openings** (two wall layers on each side are carved by the templates; the in-game self-test reads the world to check this).

### 4.3 The pruner (`cyclic/DeadEndPruner`)

If the ratio is still over the target: remove a leaf that is not a terminal and not required (`Roles.PROTECTED`: entrance, stairs, boss chain, Safe Zones, and the first rooms of each kind the recipe
requires), never below `minRooms + 2`. Which leaf: the one whose category is most over its target share, then the farthest from the entrance, then the lowest id. The assembler removes the pieces
(`MegaAssembler.removePieces` renumbers; a freed doorway of a kept room is capped by the normal cap pass).

### 4.4 The room budget

The ordinary filler places `fillerShare` (0.9) of the floor's room budget; the hallways make up the rest, so a floor ends near `mega.floorRooms` (27.9 rooms against 29.6 for a budget of 28).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The metrics (`RoomGraph.report`)

| Metric | Definition |
|---|---|
| dead end | non-terminal room of degree 0 or 1 |
| dead-end ratio | dead ends / all rooms (hallway rooms included) |
| dangling rooms | the closure of "remove a non-terminal room of degree 1" (a dead end and the branch that leads only to it) |
| bridges | edges whose removal disconnects the graph (iterative lowlink search) |
| on a cycle | a room that touches at least one non-bridge edge |
| cycle share | rooms on a cycle / all rooms |
| independent loops | the cyclomatic number m - n + c |
| terminal | entrance, stairs, boss chain, Safe Zone (`Roles.TERMINAL`) |

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. Integration (exact points)

Config switch: `generation.cyclic` (section 10). The switch is read **once**, when a sector is created (`ExpansionQueue.create`), and saved with it (`Sector.cyclic`, NBT `cyc`, written only when true):
a sector from before the switch has no `cyc` and keeps the old generator on every regeneration and repair; a cyclic sector keeps the cyclic one.

Edits to existing files (each small; list for review):

| File | Edit |
|---|---|
| `gen/Request.java` | new field `cyclic` (`LoopConfig`, null = off), copied by `withSeed` |
| `gen/MegaAssembler.java` | (1) about 24 members widened from `private` to package-private (state and `tryOne`, `push`, `rollbackTo`, `loopsNow`, `shapeOf`, `order`, `bump` ...) so `CyclicAssembly` can use the assembler's own placement test; `index` is no longer `final`; (2) `fillSocket` collects up to `lookahead` fitting templates and asks `CyclicAssembly.best` when `req.cyclic != null` (returns the first fit otherwise: unchanged); (3) `CyclicAssembly.pass(this)` after the second fill in `run` and in `runFloor` (guarded by `req.cyclic != null`); (4) `grown` = the filler's share of the room budget in `runFloor`; (5) new method `removePieces` |
| `gen/SectorGenerator.java` | four lines: `plan` and `planFloor` set `request.cyclic` from `Sector.cyclic`; the two probes from `generation.cyclic` |
| `gen/ExpansionQueue.java` | one line in `create`: `s.cyclic = CyclicSettings.enabled()` |
| `Sector.java` | field `cyclic`, saved as `cyc` only when true, read with `getBoolean` |
| `gen/MegaGenRegistry.java` | two lines in `init`: `CyclicSettings.register()` (the config file) and `CyclicSelfTests.register()` |

Not edited: `MegaConfig`, the audit, the recipe, the pools and templates, `en_us.json`, any workflow, the version.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. The five categories (`cyclic/RoomCategory`, `cyclic/CategoryBalance`)

`RoomCategory.of(kind, templateId)`: SAFE = Landing and Safe Zone; TRANSITION = corridor, junction, link, Way Onward, antechamber, stairwell and every hallway room;
PUZZLE_HAZARD = puzzle, trap, and halls named cistern, lava, chasm, flooded ...; LORE = treasure, vault, set piece and halls named library, archive, scriptorium, stacks, shrine, chapel ...;
COMBAT = trial, arena, cavern and every other hall. (A new library dropped into `structure/mega/hall/` is Lore without a line of Java.)

Target shares (advice, not a hard rule): Combat 24 %, Puzzle / Hazard 14 %, Lore 20 %, Transition 34 %, Safe 8 %. They enter in two places: the placement score
(`+3 * want`, and a steep penalty once a category is 10 points over its target: that is what stops the lookahead choosing corridors) and the pruner (the most over-represented category goes first).
`balanceWeight` (default 2) scales both; 0 switches the nudge off.

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Measured numbers (real catalog, 264 floors: 2 roads x floors 1..22 incl. boss floors x 6 seeds; `CyclicAssemblerTest`)

| | old generator | cyclic (defaults) |
|---|---|---|
| rooms per floor | 29.6 | 27.9 |
| **dead ends** (non-terminal, % of rooms) | **32.6 %** (0 of 264 floors <= 8 %; p95 44.8 %, worst 55.2 %) | **6.1 %** (256 of 264 floors <= 8 %; p95 7.7 %, worst 20.0 %) |
| rooms on a cycle | 34.1 % | 80.0 % |
| independent loops | 3.7 | 10.0 |
| links / hallway rooms / rooms trimmed per floor | 0 / 0 / 0 | 1.47 / 2.75 / 2.39 |
| assemble tries per floor | 1.11 | 1.00 |
| doorways facing a blank wall (niches) per floor (132 floors) | 45.0 | 30.1 |
| Combat / Puzzle / Lore / Transition / Safe | 13.4 / 5.0 / 12.8 / 63.7 / 5.1 % | 17.3 / 6.6 / 16.6 / 53.1 / 6.5 % |

* What is left of the 6.1 %: the recipe's required treasure closet (252 of the 264 floors have one single-door closet, 3.4 % of the rooms on its own), then some halls and corridors
  where no partner was in reach and trimming would have gone under the room minimum. They are counted, not hidden.
* The same measurement on the 96 floors of `theFiveCategoriesStayBalanced` with **only the loop pass** (no lookahead, no pruner, no balance): dead ends 21.6 %, on a cycle 57.8 %, Transition 69.7 %
  (+4 points over 65.7 %), no category share moved by more than 6 points. With the lookahead and the balance term the mix is **closer to the target** than the old generator's.
* Old banded sectors (`floors.perSector > 1`; 40 sectors): rooms 60.8 -> 57.9, dead ends 29.4 % -> 7.4 %, rooms on a cycle 25.8 % -> 74.5 %.
* The boss chain and the forward stairs got **no extra doorway** in 128 cyclic floors (64 boss floors): 0 against 0 in the old generator.
* The knob: dead ends 17.3 % at `deadEndTarget` 0.30, 6.2 % at 0.08, 3.4 % at 0.04; 30.8 % with every mechanism off.
* The pure layer on 4000 random grown dungeons (a crowded model, 21.8 rooms): dead ends 20.0 % -> 10.6 % after the loop pass -> 6.2 % after the pruner; rooms on a cycle 42.6 % -> 58.1 %; independent loops 5.1 -> 6.7;
  1.6 links and 3.0 hallway rooms per dungeon; 92.6 % of the dungeons at or under 8 %.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Tests

* `mega/cyclic/CyclicLayerTest` (pure, 4000 seeds): metrics on known graphs (path, cycle, tail on a cycle, forests), the socket rules, the hallway search on hand-drawn maps (straight, bend, one cell, detour,
  boxed in), and for every random dungeon: connectivity kept, every walk only gets shorter, the stairs keep their distance, a doorway is used once, no locked room gets a doorway, every hallway is a chain of
  free cells whose first cell is fed by door A and last cell feeds door B (no door without a partner, no cell shared), the hallway cap, no new dead end, determinism (same seed, same links),
  the pruner never removes a protected room, keeps the graph connected, never lengthens a walk, respects the minimum, and the target.
* `mega/gen/CyclicAssemblerTest` (the real catalog through the real request, solver and audit): everything of section 8 as assertions, plus: every cyclic plan passes the full audit and the structural check,
  no doorway opens onto the void, 2149 loops line up exactly, 512 hallway pieces have both doorways joined, the switch off is the old generator (no `cyclic.*` statistic) and cyclic plans repeat exactly.
* The existing 115 tests of the generator still pass; a fingerprint of 236 old plans is identical before and after (section 2).
* `gen.cyclic` (in game, `/multiverse mega selftest gen.cyclic`): probe floors old against cyclic from the same request, audited, deterministic, agreeing with the game's rotation maths for every piece
  (`GeometryCheck`), the dead-end and cycle numbers no worse than the old plan's, the category mix no further from the target; the `Sector.cyclic` flag round-trips and an old save reads as not cyclic; and for
  every finished cyclic sector the plan is made again from the saved data and equals what was saved, and every doorway into a hallway piece (and every loop) is open in the world: 3 x 4 blocks through four wall layers with
  nothing solid in them. **CI: the key prefix is `gen`** (add it to `SMOKE_EXPECT` in `.github/workflows/smoke.yml` if the smoke test should insist that the test is registered; the test runs with `selftest` either way).

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Config (`world/serverconfig/nusmp-labyrinth-mega-gen-server.toml`, section `[generation]`)

| key | default | meaning |
|---|---|---|
| `cyclic` | true | the switch; read once when a sector is created and saved with it; false = the old generator |
| `deadEndTarget` | 0.08 | the most dead ends, as a share of all rooms |
| `cycleTarget` | 0.60 | extra loops until this share of rooms lies on a cycle |
| `maxHallway` | 3 | longest hallway in modules (0 = no hallways) |
| `hallwayShare` | 0.20 | hallway rooms as a share of all rooms |
| `maxLinks` | 16 | most links per floor |
| `prune` | true | may dangling rooms be removed |
| `fillerShare` | 0.9 | the filler places this share of `mega.floorRooms`; hallways make up the rest |
| `lookahead` | 12 | different fitting templates compared per doorway (1 = first fit) |
| `balanceWeight` | 2.0 | strength of the category nudge (0 = off) |

The numbers (not the switch) are read when a cyclic sector is planned, so a regeneration after a change plans with the new numbers, exactly as `mega.floorRooms` already does.

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. Not verified (honest list)

1. **Nothing ran in a game.** The game-side classes (`CyclicSettings`, `CyclicSelfTests`, the `Sector` flag, the registry lines, `SectorGenerator` / `ExpansionQueue` lines) are type-checked against API stubs;
   the only errors left are the known stub artefacts (abstract `new BlockPos`, `Direction.getAxis`). The pure code and the integration over the catalog ran in JUnit.
2. **The catalog is a dump of the template files, not the game.** The assembler is proved against the real shapes (sizes, doorways, kinds); whether the carved doorways of a *hallway piece next to an
   arbitrary room* are open in the world is what the in-game self-test is for.
3. The config file registration uses `ModLoadingContext.get().getActiveContainer()` during mod construction (the pattern of `AvatarInventoryMenus`); if it failed, the defaults apply and a warning is logged.
4. A regeneration after a change of the *numbers* plans with the new numbers (documented in section 10); the switch itself is saved with the sector.
5. How a floor with many 1x1 hallway pieces *feels* (they are the corridor and link templates that ship; the link's waystation is excluded) has not been seen.
6. Hallways are one piece per module cell (straight or bend); longer corridor templates are not used for hallways yet.
7. The target 34 % Transition is not reached (53 %): the pools hold far more corridors than rooms, and the recipe caps traps, puzzles and trials at one each per floor. The balance is better than before, not perfect;
   moving it further is a pool-weight and recipe question, not a generator one.

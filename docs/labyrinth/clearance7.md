# The Mega-Dungeon at Tensura scale: 7 x 7 minimum clearance (1.107.0)

Lane `clearance` of the dungeon-gaps round. Self-test `dungeon.clearance7`, switch `gen.clearance7` (default on), lang fragment `tools/lang/clearance7.json`,
command `/multiverse mega clearance [sector]`. Nothing here has been run in the game; section 9 says exactly what was checked offline, how, and what was NOT.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. What the owner's spec asks for, and WHY

Spec: "all transitional elements (stairways, arches, doorways, bridges, hallways) must have a minimum clearance of 7x7 blocks to accommodate giant Tensura player
races (Ogres, Dragonewts, etc.)".

WHY. The dungeon was drawn for a player two blocks tall and one wide: a doorway of 3 x 4, a corridor 5 wide and 6 high, a spiral stair one block wide. A Tensura
character is not that player. An ogre or a dragonewt is taller and broader than a human, a party of six wants to fight side by side in a hallway instead of in
single file, and a giant mount does not fit through a door a villager fits through. The first symptom of a too-small dungeon is not an error, it is a player stuck
in a doorway; so the minimum is a rule that is measured (section 5), not a size that is hoped for.

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. NEW / REPLACED / KEPT

Rule 5 of the repo (new or replacement?): the spec covers something that exists (the sizes of the transitions), so the old sizes are REPLACED, but the owner's
brief asked for the old size to stay available, so they are kept behind the switch.

| | What |
|---|---|
| NEW | `gen/Clearance7` (the whole decision, pure), `gen/ClearanceGrid` (a template as a grid, pure), `gen/ClearanceCarver` (applies it to the world), `gen/ClearanceSettings` (the switch), `gen/ClearanceSelfTests` (`dungeon.clearance7`), `gen/ClearanceCommand` (`/multiverse mega clearance`), the rule `clearance7` in `GeometryCheck`, `GenData.Stat.clear7`, `tools/lang/clearance7.json`, this doc, three JUnit classes and a test NBT reader (section 8) |
| REPLACED (behind `gen.clearance7`, default on) | doorways 3 x 4 -> 7 x 7 for every connected door; corridors one module wide 5 x 6 -> 7 x 7; bends / tees / crosses 5 x 5 -> a 7 x 7 chamber; the bent Way Onward corridors 5 wide -> 7; the opened rubble seal 3 x 4 -> 7 x 7; stair head room (18 of 336 stair blocks had under 7) -> 7 |
| KEPT | every template, pool, socket, jigsaw name, marker and room footprint (no `.nbt` file changed); floor height 48; the module grid of 9; the plan (pieces, rotations, loops, dead ends, the cyclic planner and the pruner) is byte for byte what it was; the boss-chain doors (Guardian hall, antechamber -> vault) keep their Gatekeeper door block and their size; doors that end in a door cap; the old sizes when `gen.clearance7 = false` |

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. How the sizes were changed, and why not by redrawing the templates

The templates are 244 static `.nbt` files drawn by other builders on a 9-block module with walls TWO layers thick. A 7-wide passage does not fit between two
two-layer walls of a 9-wide piece (9 - 2 - 2 = 5), so a redraw would change every footprint, every socket offset and the whole tested grid, and would leave no old size
behind a switch. Instead each piece is cut right after it is placed (inside the Dungeon Guard span the placer already uses, because the mod's own builder is the
one thing allowed to take blocks away): the cut boxes are computed from the plan alone, never leave the piece's own box, and remove only what may be removed.

| Transition | Before | After (cut box, template frame) | Where |
|---|---|---|---|
| Doorway between two connected rooms | 3 wide, 4 high, through 2 + 2 wall layers | 7 wide (module centre +- 3), 7 high (rows 9..15 above the floor row 8), through both wall layers of each room; rooms other than corridors, stairwells and Way Onward corridors also get a 3-block apron into the room | `Clearance7.cuts` |
| Straight corridor (9 wide, 1xN modules; links; bridge pieces between sectors) | 5 wide, 6 high | strip x 1..7, z 1..len-2, rows 9..15 (7 x 7); the walls beside it are one layer thick; invisible light blocks every 4th block of the ceiling row | `Kind.CORRIDOR` |
| Bend, tee, cross, waystation (9 x 9) | 5 x 5 chamber, 6 high | the 7 x 7 inside, rows 9..15 | `Kind.CHAMBER` |
| Bent Way Onward (27 x 27) | 5 wide | a 7-wide lane from each socket to the far wall's inner face; both end walls stay two layers (the rubble seal is in one) | `Kind.CORRIDOR` |
| Straight Way Onward (9 wide) | 5 wide | strip with both end walls kept two layers thick | `Kind.CORRIDOR` |
| Rubble seal when the next sector is joined | 3 x 4 x 2 | 7 x 7 x 2 (`SectorPlacer.clearSeal` also cuts `Clearance7.sealCut`) | `Clearance7.sealCut` |
| Staircase | free head room below 7 on some steps | every bottom-half stair block gets the 7 cells above it cleared (the steps themselves stay) | `Clearance7.stairCuts` |

Which sockets are cut: only doors that lead somewhere (`Clearance7.connected`: a parent-child join, a loop, both ends of a bridge piece, the entrance of a
piece). A socket that ends in a door cap is an alcove, not a passage, so it is not cut (widening the hole in front of a 3-wide alcove would leave a gap beside the cap).

What a cut may remove (`Clearance7.carvable`): walls, floors' neighbours, furniture, chains, lanterns, banners, signs, heads, and furniture that only happens to be a
block entity and holds nothing (a chiselled bookshelf, a lectern, a furnace). It never removes a chest or barrel, a spawner, any container, a fluid, or the blocks of a
mechanism (Gatekeeper door, altar, pedestal, gates, sealed archway, ward and trap tiles). Blocks are removed without neighbour updates, so nothing pops off as an item
and no water flows in. Armour stands and item frames inside a cut box are discarded (a hanging frame would lose its wall). Where a low ceiling was cut through (the room
inside was lower than 7), a roof block is put at row 16, copied from the wall beside the cut so the sector's theme carries over; 1 317 such blocks in the worst case
over all 222 real templates, nearly all in low-ceilinged halls.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The switch

`gen.clearance7` (server config `nusmp-labyrinth-mega-clearance-server.toml`, section `[gen]`), default `true`; `gen.clearance7Light` (default `true`): after a corridor or
chamber was cut, invisible light blocks (level 12) every 4th block of its top row replace the hanging lanterns the cut removed, so the gloom clock still finds light.

* The switch is read each time a piece is placed. A sector that is already finished is NOT changed by flipping it (turn it off, the cut doorways stay cut; turn it on,
  old sectors stay narrow): `/multiverse mega regen <id>` rebuilds a sector with the switch's current value.
* Each sector's saved statistics (`GenData.Stat.clear7`, written only when true, so an older save is byte for byte the same) remember whether it was cut; the rule
  `clearance7` only judges sectors that say yes.
* Off = exactly the old generator output (no cut is made, the lights are not placed, the seal opens 3 x 4).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The rule `clearance7` (GeometryCheck)

`Clearance7.measure(space, cut)` walks along a transition and, at every station, counts the free columns (free for 7 rows) round the centre line and the free height of
the centre column; the answer is the narrowest and lowest station. `Clearance7.check(result, space, pieces, max)` fails any cut below 7 wide or 7 high and says which
piece, which kind and what it measured. `GeometryCheck.run(level, built, n, clearance7)` calls it on the pieces it reads from the world (`core.geometry`, `/multiverse mega
verify`), `GeometryCheck.clearance7` is the rule on its own. "Free" means no collision (air, torches, banners, light blocks and the like count as free; a lantern, a
chain or a block does not).

When the sector was cut, the existing world sample of `GeometryCheck` (the share of solid template blocks that must stand where the plan says) skips the blocks inside the
cut boxes, because the cut took them away on purpose.

The rule is pure and is proved on the real templates (section 8), in the game on the data pack's own templates (`dungeon.clearance7`, part 3) and on finished sectors in
the world (part 4, and `/multiverse mega clearance <id>`).

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. Connectivity: the cyclic planner, the dead-end pruner, the module grid

The cuts never change the plan: no footprint, rotation, socket or piece moves, so the loop planner (`cyclic/LoopPlanner`), the dead-end pruner (`DeadEndPruner`), the
socket rules and the audit see exactly the pieces they saw before, and the plan of a seed is unchanged by `gen.clearance7` (test `theCutsNeverChangeThePlan`). What
could break is a door that is cut on one side and not the other, or a cut that opens a wall to the outside. `Clearance7.connectivity` checks, on every planned floor:

* every cut lies inside its own piece's box (a neighbour is never changed);
* an interior cut (corridor, chamber) never reaches the outer wall layer; a doorway cut reaches the outside of its piece only at its own door face;
* every ordinary join and every loop has a doorway cut on BOTH sides, and the two lie face to face on the same centre line;
* the room graph restricted to the joins that have cuts (plus the boss-chain joins, which are not cut) is still connected.

On 96 real floors (old and cyclic generator, both roads, ordinary and boss floors) this holds without exception: 7 442 doorway cuts for 2 756 joins and 1 013 loops,
432 corridor strips, 704 chambers, and 2 065 door caps left alone.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. What is deliberately not cut, and why

| What | Why |
|---|---|
| Boss-chain doors (Guardian hall -> antechamber, antechamber -> vault) | the Gatekeeper door is a block of its own size in the doorway; widening the hole around it would make a 3-wide door in a 7-wide gap |
| Sockets that end in a door cap | an alcove, not a passage |
| The spiral staircase's WIDTH | see section 9: it cannot be 7 wide inside the 9-block shaft; its head room is cut, its width is reported, not demanded |
| Plaza and hall interiors | they are 14 to 25 blocks wide and 9 to 30 high already; only their doors and the apron behind them are cut. A fountain or statue ring in the middle of a plaza is not a transition and is not touched |
| Containers and mechanisms in a cut box | a loot chest or a gate is never destroyed to make room; the rule then reports the transition (one case in the 222 real templates: a loot chest on a shelf in the apron of a door of `hall/fungal_underdark_fungus_farm`, the door measures 4 x 7) |

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. Tests

JUnit (shim): `Clearance7Test` (the rule on made-up spaces, the geometry of every cut, the roof rule, the stair merge, the seal cut, whole real floors of both roads, old and
cyclic: connectivity), `Clearance7TemplatesTest` (every one of the 222 real non-cap templates, all doors connected = the worst case, cut and measured; every turn of a
piece; the cut only ever grows the old 3 x 4 opening; the rubble seals are left exactly as they are), `Clearance7PlanWorldTest` (16 whole floors pasted from the real template
files at their real rotations and origins, 596 turned pieces, 2 275 cut boxes and 579 joints measured, plus an independent look at every joint and loop that does not use the
cut boxes at all: the four wall layers at 7 x 7 round the jigsaw line must be free). A deliberate mutation (the cut pointing out of the room instead of into it) is caught by
the unit tests and by that independent look. The existing generator tests (378 JUnit tests in the gen, cyclic, stair, audit, source-scan and guard classes) pass.

In-game self-test `dungeon.clearance7` (CI smoke): the rule behaves; both roads' probe plans keep every module connected; the data pack's own templates for those plans,
cut exactly as the placer cuts them, measure 7 x 7 or better (a transition below that must be blocked by a block the cut may not remove, otherwise the test fails); finished
sectors made with the switch on are read in the world with the same rule. It prints the measured numbers.

Measured over the real templates (worst case, every door connected): 698 doorway cuts, 24 corridor strips and lanes, 12 chambers, 336 stair head-room columns; 75 412 blocks
removed, 3 blocks left standing (blocks that are never cut), 1 317 roof blocks, 145 light blocks; 1 of 1 070 transitions below 7 x 7 (the chest above).

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Honest list: what could NOT be verified, and the limits

* **Nothing was run in the game.** The pure logic is proved on the real template files and on whole planned floors pasted from them; the game half (`ClearanceCarver`: the
  block removal, the roof copy, the light blocks, the entity discard, the chunk handling, the interplay with the Dungeon Guard span and the light engine) is only
  type-checked against API stubs. The CI smoke test runs `dungeon.clearance7`; whether it finds a finished carved sector depends on the smoke world.
* **Stairs are 7 high, not 7 wide.** The small stairwells are a spiral one block wide round a 3 x 3 core inside a 9-block shaft (5 x 5 inside); the grand stairs of a boss floor
  have bands 3 cells wide in an 18-block shaft. A spiral round a newel cannot be 7 wide in a 9-block shaft: the walkable ring would have no room left for the core. A genuinely
  7-wide stair needs NEW stairwell templates (a 27 x 27 shaft, flights 7 wide), drawn with `tools/labyrinth/gen_mega_stairwell.py`; that is a separate piece of work and was not
  done here. The rule reports the head room (it fails below 7); the width is `Clearance7.stairWidth`, reported, not required.
* **Vaulted ceilings / new `StructureTemplatePool` entries** (the second sentence of the spec's template item) were not added: no new template exists in this lane.
* **Bridges** are the bridge pieces between sectors (`link/*`, cut like corridors); bridges drawn inside a room template (`set/necropolis_bone_bridges` and the like) are part
  of that room and are not touched.
* **The invisible light blocks** replace hanging lanterns only in light level, not in looks; with the light item in hand they show. If the owner prefers visible lamps, the ceiling
  of a cut corridor can take a lantern pattern from the theme later (`Clearance7.lightCells` is the single place).
* **The roof block** copied from the wall beside a cut is a guess at the theme's colour (a processor that recolours blocks after placement runs before the cut); a cut in a
  sector whose walls are mixed may show one off-colour block per roofed column.
* **Cut volume.** A door apron removes furniture inside 7 x 7 x 3 in front of every connected door; in the worst case over all templates that is 75 412 blocks, an average of about
  340 per template; furniture the room builders put right behind a door is lost (not chests, not mechanisms).
* **Flipping the switch** does not change a finished sector (section 4); a sector regenerated or repaired mid-job is finished with whatever the switch said when each piece was placed.
* **The Way Onward opening** is widened when the seal is opened (`clearSeal`); the first piece of the next sector's tunnel is cut when it is placed; if the next sector's Landing
  attaches directly (no bridge piece) its entrance door is cut as a connected door. Sector 0's entrance to the Hub side is cut the same way; what stands on the other side of that
  door is not this lane's.
* **Things outside this lane that assume 3 x 4 doorways** (found, not changed):
  * `mechanics/PrimsWorld` `seal_doors` finds the open columns of a room's doors by scanning and seals them 4 high (`MegaGrid.DOOR_H`): in a cut door that is 7 columns wide and 4 high,
    leaving 3 rows open above the barrier (not reachable on foot, reachable by anything that flies or climbs). It should seal `Clearance7.REQUIRED` rows while the switch is on.
  * `labyrinth/trial` Sealed Archway and the lockdown barrier (lane 7) are blocks of their own size; they are never cut, and a lockdown that fills the 3 x 4 hole leaves the 7 x 7 gap
    around it open. The lockdown lane should size its barrier from `Clearance7.cuts` of the room.
  * `rift/pocket` has its own door width (`PocketLayout.DOOR_WIDTH = 3`); the rift pockets are a separate generator and were not touched.

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Files

New: `labyrinth/mega/gen/{Clearance7, ClearanceGrid, ClearanceCarver, ClearanceSettings, ClearanceSelfTests, ClearanceCommand}.java`; tests `Clearance7Test`, `Clearance7TemplatesTest`,
`Clearance7PlanWorldTest`, `TemplateGrids` (test NBT reader); `tools/lang/clearance7.json`; this doc.

Edited (additions): `GeometryCheck` (rule `clearance7`, the 4-argument `run`, the cut-aware world sample), `SectorPlacer` (the cut after each piece, the seal widening), `GenData`
(`Stat.clear7`), `SectorGenerator.stat` (one line), `CoreSelfTests.geometry` and `MegaCommand.verify` (pass whether the sector was cut), `MegaCommand.build` (one line), `MegaGenRegistry.init`
(two lines), test `CatalogSource` (an accessor).

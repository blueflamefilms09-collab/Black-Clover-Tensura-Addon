# Compatibility with Create (the Mega-Dungeon's traps use its machinery)

Owner request: "add compatibility for Create so when it is in the dungeon it will use it to make the traps better".

**Honest status first.** Create is not available in the build sandbox and Gradle cannot download it, so nothing here was run against the real mod, and nothing was run in a game.
Every Create block id below is written from memory of Create 6.x for NeoForge 1.21.1, every one of them is optional, and a wrong id degrades to a vanilla stand-in and one
log line, never to a crash. The Java was type-checked against the API stubs (the only errors left are the known stub gaps: `BlockPos` "is abstract" and `Direction.getAxis`),
the pure rules were run as JUnit tests, and an in-game self-test (`compat.create`, section 9) is written to pass whether or not Create is installed. Section 10 lists exactly
what could not be verified.

**New or replacement (owner's rule 5).** Everything here is NEW. Nothing existing was replaced or removed. The D&D traps (`labyrinth/trap`: pitfall tile, snuffer glyph, sigil
plates), the mechanics library (`mega/mechanics`), the Foundry and the Safe Zones work exactly as before. Edits to existing files: one added line in `NUSMP.java` (the call to
`CreateCompat.init`). With Create absent the compatibility registers a config file, one command and one self-test, and changes nothing in the dungeon.

## 1. How it works, start to finish

```
sector generated ──► CreateTrapDirector.onSectorGenerated ──► TrapPlanner (pure, seeded) ──► CreateTrapData: sites PLANNED   (nothing in the world yet)
                                                                                                     │  every 2 s, one site whose room's chunks are loaded
                                                                                                     ▼
                         LaneFinder over WorldGrid (a 3-wide choke, walls 3 thick) ─► TrapBuilder.check ─► TrapBuilder.build        sites BUILT (or DROPPED: no choke)
                                                                                                     │  a player within 40 blocks wakes the site
                                                                                                     ▼
                         TrapRuntime.step every 2 ticks:  lever? trigger? ─► TrapClock.Machine (IDLE/RUNNING/COOLDOWN/DISARMED) ─► bands, rams, clutches, belt
```

* **Soft by construction.** No file imports a Create class (a test checks it). Create blocks are found with `BuiltInRegistries.BLOCK.getOptional(id)`, state properties are set by name
  through the block's own state definition, damage types are looked up in the damage-type registry, and the two places that need a Create method (the belt's `initBelt` and the
  movement check) use reflection with a try/catch. Every id is a list of candidates; the first that exists wins; none = the vanilla stand-in of `Part`, logged once.
* **Rooms.** Only `corridor` (14% base), `link` (8%), `junction` (6%), `treasure` (8%), `trap` (30%) and `hall` (2%) rooms are candidates, the chance rises up to +80% with depth (200
  floors), is multiplied by the density setting, and a sector holds at most 7% of its rooms (never more than 24). Never a Landing, Safe Zone, stairwell, arena, antechamber, vault or
  a room that opens onto a safe room; never two traps in rooms that open onto each other.
* **Where.** A trap needs a choke: three open lane columns (solid floor, four free cells) with solid wall at least three columns deep on both sides, for the whole strip (8 to 10
  cells). A corridor of the dungeon is exactly that (3 wide, walls 3 thick); a doorway in the side, a junction or a hall fails the wall test, which is also the keep-out. A room
  with no choke drops its plan.
* **Which family.** `TrapRules.pickFamily`: base weight 1 each, times the theme's affinity (`TrapRules.THEME`: the Dwarven Forgehold favours presses 3x, drills 2.5x and spouts 2.5x;
  the Infernal Foundry spouts 3x; Storm nixie sequences 3x; the holy themes 0.35x of everything but sequences; and so on), times the room kind (a belt wants a corridor 2x), times a small
  rise with the sector's threat for the heavy families; a family is out below its floor or when its config switch is off.
* **Restarts.** The drawing is never saved: a site stores (family, core length, headroom, trigger, seed, origin, direction) and the drawing is re-derived. On the first look after a restart
  every clutch is put back to idle and every ram block is taken away, so a server that stopped mid-strike leaves nothing in the lane.

## 2. The Create id table

Everything is looked up under namespace `create`. "Stand-in" is the vanilla block used when no candidate exists. "Used in" lists the families whose drawing places it.

| Part | Candidate ids, in order | Vanilla stand-in | Used in | Role |
|---|---|---|---|---|
| MOTOR | `create:creative_motor` | `minecraft:copper_block` | all kinetic families | the fixed rotation source of each wall line (default speed; see section 10) |
| SHAFT | `create:shaft` | `minecraft:chain` | saw run, press, drill wall, sequence | line filler |
| COGWHEEL | `create:cogwheel` | `minecraft:polished_andesite` | saw run | dressing on the wall behind the saws |
| LARGE_COGWHEEL | `create:large_cogwheel` | `minecraft:polished_andesite` | press | dressing |
| GEARBOX | `create:gearbox` | `minecraft:polished_andesite` | saw run, press, drill wall (tap), sequence | line filler; in the drill wall it feeds each drill |
| CLUTCH | `create:clutch` | `minecraft:polished_andesite` | every family with a motor | cut by a redstone block beside it (idle / disarmed), open for a run |
| CHAIN_DRIVE | `create:encased_chain_drive`, `create:chain_drive` | `minecraft:polished_andesite` | sequence (as the working part), others (filler) | |
| SAW | `create:mechanical_saw` | `minecraft:grindstone` | saw run | the blades in the walls |
| DRILL | `create:mechanical_drill` | `minecraft:dispenser` | drill wall | the lunging drills |
| PISTON | `create:mechanical_piston` | `minecraft:piston` | press, treadmill | the housings of the rams |
| PISTON_POLE | `create:piston_extension_pole` | `minecraft:end_rod` | press, treadmill | behind each piston |
| BELT | `create:belt` | `minecraft:black_concrete` | treadmill | the conveyor in the middle column |
| DEPLOYER | `create:deployer` | `minecraft:dispenser` | press, drill wall | decoration only |
| STORAGE_INTERFACE | `create:portable_storage_interface` | `minecraft:polished_andesite` | treadmill | decoration only |
| SPOUT | `create:spout` | `minecraft:hopper` | pour | hangs from the top of the lane |
| PIPE | `create:fluid_pipe` | `minecraft:lightning_rod` | pour | the line over the spouts, in the ceiling |
| TANK | `create:fluid_tank` | `minecraft:copper_block` | pour | the end of the pipe line (empty: see section 10) |
| NIXIE | `create:nixie_tube` | `minecraft:redstone_lamp` | sequence | lit by the runtime in order |
| LINK | `create:redstone_link` | `minecraft:observer` | sequence | decoration |
| WHISTLE | `create:steam_whistle` | `minecraft:copper_block` | pour | decoration (steam is a particle) |
| STEAM_ENGINE | `create:steam_engine` | `minecraft:copper_block` | pour | decoration |
| RAM | `create:industrial_iron_block` | `minecraft:iron_block` | press, treadmill | the block the runtime puts into the two edge columns of the lane for a strike |

Vanilla parts (no Create id): lever, pressure plate, tripwire hook and wire, redstone block (the power cell), deepslate bricks (the inert block that takes the power cell's place).
Damage types tried first for a hit (`create:` paths, from memory): `mechanical_saw`, `crush`, `mechanical_drill`, `fan_lava`, `cuckoo_surprise`; a missing one means a vanilla source.
Entity types refused in the Dungeon: any `create:` id containing `contraption`.
**Never used:** `create:schematicannon` (and the schematic table and the creative crate / fluid tank), by an explicit list and a test.
**Not used, and why:** the gearshift (reverses, does not cut), the rotation speed controller (needs a configured speed and a correctly oriented input I could not check; the creative
motor is the fixed source), the mechanical piston head and the casings (nothing in a drawing needs them; the piston cannot extend, see section 8).

## 3. The six traps

Every trap is a strip 8 to 10 cells long (6 to 8 cells of "core" and a margin cell at each end) in a 3-wide corridor, drawn in the frame of the lane (`Blueprint`). The machinery sits
inside the walls (columns 2 and 3 from the middle) at floor-row to knee height; **nothing solid ever stands in the walkway**, proved for every family, length, height and trigger by a
test. A trap hurts through bands: full-width slabs of the lane, one cell thick, that warn (sound and particles, at least 10 ticks), strike (6 to 22 ticks) and rest, repeating, staggered
from band to band, so a wave runs down the corridor and there is always a free cell between two bands to wait in.

| Family | From floor | What the player sees and what the machinery does | Hurt | Cycle (ticks): period / warn / strike / stagger |
|---|---|---|---|---|
| `saw_run` ("sawmill") | 1 | Two lines of mechanical saws in the walls, a saw on each side at every other cell, shafts / gearboxes / chain drives between them, cogs on the wall. On a trigger the clutch opens, the saws spin, and each band lashes across the lane (sweep particles, grinding noise). | slash | 44 / 12 / 10 / 14 |
| `press` | 4 | Mechanical pistons with extension poles in the walls, deployers and large cogs for dressing. On a trigger the clutch opens; at each strike a ram (an iron block) slides into both edge columns of the lane at that cell (never the middle column, never onto an entity) and is taken away again afterwards. | crush | 60 / 20 / 12 / 20 |
| `drill_wall` | 7 | A shaft line in the inner wall column with a gearbox tapping off at every drill; a mechanical drill in front of each gearbox facing the lane, a deployer above for dressing. The drills lunge band by band. | bore | 52 / 16 / 10 / 17 |
| `treadmill` | 9 | A conveyor belt down the middle column, fed from the left wall through a clutch, ends at a one-piston press with a portable storage interface beside it. While it runs the belt carries whoever stands on it forward (a push of 0.06 blocks per tick, always slower than walking) towards the press, which strikes once per cycle. The two side columns are plain floor: step off. | crush | 56 / 18 / 14 / - |
| `pour` | 3 | A fluid pipe line in the ceiling over the lane's top row with a fluid tank at one end, three pairs of spouts hanging under it (a pair of columns per cell, the third column free), a steam whistle and a steam engine for dressing. The spouts "pour" molten metal: lava drips, then falls, as particles and sound (nothing is poured into the lane, so nothing can block it). A hit sets a player above half health alight for two seconds. | scald | 64 / 20 / 22 / 21 |
| `sequence` | 12 | Chain-drive lines in the walls; a nixie tube in each wall at every band, with a redstone block behind it that the runtime places while the band winds up and strikes (a real redstone signal into a real nixie tube, so it glows); redstone links at both ends. The tubes light in order and the discharge follows them down the corridor. | shock | 48 / 14 / 8 / 12 |

**Hit sizes.** The base is `3 + 1.6 * log2(1 + floor)` (4.6 on floor 1, 8.5 on floor 10, 13.7 on floor 100), times the family's scale (slash 1.0, crush 1.5, bore 1.2, scald 0.8, shock 0.9),
times the sector threat (up to 1.6), times the config multiplier. See section 5 for the limits.

## 4. Triggers and the disarm

Triggers are the ones the dungeon already uses (the mechanics library's `plate` and `proximity`, plus a tripwire): drawn by the planner, 55% pressure plates (one at each end of the strip),
25% tripwire (two wires between four hooks), 20% proximity (any survival player inside the lane). The plate and the wire are real vanilla blocks whose `powered` state the runtime reads,
so a zombie or a skeleton arrow can set a trap off, but only survival and adventure players are ever hurt or thrown.

**Disarm: a lever.** Four vanilla levers sit at eye height on the walls, one per wall at each end of the strip, in plain sight, and the first time a player comes within 8 blocks they get
the action-bar line "Gears grind behind the walls. A lever on the wall would stop them." Any lever thrown = disarmed: a running trap stops at once (rams retract, clutches cut, nixie
tubes dark), a disarmed trap ignores triggers however often they are pressed, and it re-arms when every lever is back. The levers are ordinary vanilla blocks, so they work with the
dungeon's block protection (the Dungeon Guard stops breaking, not using). No item is needed and none is required.

## 5. Safety: never lock a party in, never kill

All of this is in the pure `TrapRules` / `TrapBlueprints` / `TrapClock`, and pinned by tests:

* The walkway (x -1..1, the whole height of the corridor, the whole length) holds only things a player walks through (levers, plates, hooks, wire) or, at the very top of the lane, a spout or
  pipe. Parts live in the walls. A band is a slab one cell thick with a free cell on each side of it. A band strikes for at most 45% of its cycle, is quiet for at least 8 ticks of it and warns
  for at least 10. A test walks a simulated player through every family and length and direction from many start times, who waits for the quiet, and it always crosses.
* The only blocks the runtime ever puts into the walkway are the rams, in the **edge** columns, never over an entity, taken away at the end of the strike, on disarm, on cooling, after a
  restart, and whenever a trap is put to sleep when nobody is near.
* The belt pushes at 0.06 blocks per tick (a walk is 0.22) and the side columns have no belt.
* A hit takes at most **35%** of the victim's maximum health, one run of one trap at most **70%**, a hit never takes the victim below 10% of their maximum, and a player **below 20%** is only
  thrown, not hurt: a trap does not kill. One victim is hit at most once per band per cycle. Creative and spectator players are ignored.
* A trap exists only where there is a choke, only in the room kinds above, never next to a safe room.

## 6. Dungeon protection

The Dungeon's blocks are unbreakable by any means (`dungeon/DungeonGuard`, the `Level` mixin, the events). The traps work with it, and Create is kept out of it:

* The machinery replaces wall stone and floor row blocks with Create blocks, which is allowed because the builder runs inside `DungeonGuard.internal(...)` and because the guard only refuses a
  change that takes a block AWAY (to air, a fluid or fire): swapping a block for another block is never refused, so the runtime's clutch and nixie swaps need no special scope; only
  taking a ram away (to air) does, and it runs inside `internal`. A site refuses to build over a block with a block entity, an unbreakable block, or the dungeon's own pitfall tile, glyph,
  sigil, riddle stone, altar or gatekeeper door.
* **Contraptions cannot move the Dungeon's blocks or leave the room.** Two layers, both optional and both answering to `refuseContraptions` (on by default):
  1. At server start the class `com.simibubi.create.api.contraption.BlockMovementChecks` (or its older location) is found by reflection and a check is registered with its
     `registerMovementAllowedCheck`, implemented with a `java.lang.reflect.Proxy` of the interface that method takes, answering the enum constant `FAIL` (found by name on the method's
     own return type) for any block of a world the Dungeon Guard holds. Create then refuses to assemble anything there, with its own "cannot move" message. If the class, the method or the
     constants are not as I remember, the registration fails quietly, once, and the status line says why.
  2. An `EntityJoinLevelEvent` listener cancels any `create:*contraption*` entity (not loaded from disk) joining a world the Dungeon Guard holds, so even an assembly path I did not know of
     cannot make a contraption appear (and the Dungeon Guard's own refusal to remove the blocks means nothing is duplicated either).
  Consequence, said plainly: **no Create contraption moves in the Dungeon**, including the mod's own mechanical pistons, which therefore stay as housings (section 8). Static Create blocks
  (saws, drills, belts, motors, fans) work as they do anywhere; a drill or saw that tries to break a Dungeon block goes through `Level.destroyBlock`, which the guard refuses.

## 7. Config: `world/serverconfig/nusmp-labyrinth-create-compat-server.toml`, section `[create]`

| Key | Default | Meaning |
|---|---|---|
| `enabled` | true | master switch; false = no new traps, built ones stop (no hurt, no hints) |
| `densityMultiplier` | 1.0 (0 to 4) | how many traps a sector gets; applies to sectors planned from now on |
| `damageMultiplier` | 1.0 (0 to 3) | scales a hit; the limits of section 5 still hold |
| `sawRun`, `press`, `drillWall`, `treadmill`, `pour`, `sequence` | true | which families may be drawn |
| `refuseContraptions` | true | the two contraption layers of section 6 |

## 8. What differs from the request as written, and why

* **Pistons do not extend.** A Create piston moves a contraption, and contraptions are refused in the Dungeon (section 6); one that extended across the corridor would also be a wall nobody
  could pass. So the pistons are housings with poles, and the "crusher" is the ram block the runtime slides into the two edge columns, with the damage dealt by the mod.
* **The hurt is the mod's, the machinery is Create's.** Saws, drills, belts and spouts are real blocks that spin when the clutch opens, but what hurts, throws and carries is the band logic of
  `TrapRuntime`, so a trap works the same whatever a Create block does with its own entity-damage rules, and the limits of section 5 hold.
* **The motor's speed** is the creative motor's default; I did not try to write its speed into its block entity (I do not know the tag). A slower rotation changes only how fast the blades turn.
* **Spouts do not pour fluid.** A fluid line needs a source; the pipe line ends in an empty tank, and the pour is lava particles, drips and sound. Nothing is placed into the lane.
* **Nixie tubes show a signal level**, not a custom text: they light because a redstone block is placed behind them while the band winds up and strikes.
* **Deployers, portable storage interfaces, redstone links, the steam whistle and the steam engine are decoration**, as the request said for the first two; they do nothing.
* **Belts:** the mod's own push moves the player, so a belt that fails to join up or turns the wrong way would look wrong but not change the trap. After placing the line the mod asks
  Create's belt class to join it (`initBelt`, found by reflection, ignored if absent).

## 9. Commands, tests, files

* `/multiverse compat create` (op): status (Create installed?, parts found in Create / stand-ins, sites by state, traps awake, contraption guard).
  `plan` plans the traps of sectors that existed before Create was installed (they are built when someone comes near). `arm <family>`: builds one trap of that family at a choke within 24 blocks of
  the player in the Mega-Dungeon (to see it and to test it). `fire`: sets off the nearest built trap within 24 blocks.
* **Self-test** `compat.create` (`/multiverse mega selftest compat.create`; registered with `SelfTests.register`; the CI smoke test can expect the prefix `compat.create`). It passes with and
  without Create. It checks the drawings, the rules, the contraption rule, that without Create no part resolves to a Create block and the director is inactive, that every part resolves; then, on a
  scratch corridor in a far corner of the dungeon's dimension, for `saw_run` in all four directions and for each of the other families once: the finder sees exactly the carved corridor, arming
  through the real path works, the walkway is clear and can be walked end to end, four levers are there, vanilla parts stand as placed, the power cells are idle, a forced trigger starts a run, a
  stand-in in the first band is hit within the limits, the clutches open during the run, the nixie tubes light, the rams appear and disappear, a thrown lever stops a run at once with no hurt, and a
  pressed plate / wire / a player in the lane starts a run. Create parts that did not stay as placed are reported as a note, not a failure.
* **JUnit** (`src/test/java/com/newuniverse/nusmp/compat/create/`): `TrapBlueprintsTest` (every family, length, height, trigger and seed is safe; the validator catches a block in the walkway, a
  mid-column ram, a band that strikes too long, a belt too strong, a missing lever; clutches and nixie tubes have their power cells; machinery faces the lane; a walker who waits always crosses),
  `TrapRulesTest`, `TrapPlannerTest`, `LaneFinderTest`, `TrapClockTest`, `CreateLangTest` (lang fragment in step with the code; no file imports a Create class), `CreateDocTest` (this table is in step
  with the code).
* **Files** (`src/main/java/com/newuniverse/nusmp/compat/create/`): pure: `Dir6`, `HazardKind`, `TriggerKind`, `Part`, `CreateIds`, `TrapFamily`, `Blueprint`, `TrapBlueprints`, `TrapRules`,
  `TrapClock`, `LaneFinder`, `TrapPlanner`; Minecraft: `Frame`, `PartResolver`, `WorldGrid`, `TrapBuilder`, `TrapRuntime`, `TrapSite`, `CreateTrapData`, `CreateTrapDirector`, `ContraptionGuard`,
  `CreateConfig`, `CreateCommand`, `CreateSelfTests`, `CreateCompat`. Lang: flat fragment `tools/lang/create_compat.json` (merge into `en_us.json`; not edited here).
  Edit to an existing file: `NUSMP.java`, one line after `MapsCompat.init(modContainer)`.

## 10. What could not be verified (honest list)

1. **Every Create id** in section 2, the `facing` / `axis` / `axis_along_first` property names and the rule that maps a facing and a rotation axis to that flag, and every state property of a
   Create block (belt `slope` / `part`, lever-like props of the stand-ins). A wrong one degrades silently to a default state or a stand-in (the status command lists which parts were found).
2. Whether the **power train really connects**: that a creative motor facing along the lane feeds a clutch and a saw / piston / chain drive in a straight line, that a gearbox standing on its axis
   passes the line and taps the drill in front of it, that a clutch cut by a redstone block beside it stops the line, that the belt takes power from a motor at its side through a clutch.
   If any of it is wrong the blocks are still there and the traps still work (the hurt is the mod's), only the spinning does not follow the strikes.
3. **Create's reaction to being placed by `setBlock`** with no player: block entities, the belt's controller, kinetic network updates; and what a static drill or saw does to an entity beside it
   (their own damage can add to the mod's).
4. **The two contraption guard registrations**: the class and method names, that the check interface has one abstract method returning an enum with `FAIL` and `PASS`, and that `FAIL` means
   "may not move" there. The entity-join layer needs none of that (it only needs the entity id to contain `contraption`).
5. **The Create damage type ids**, and how those damage types read in a death message (a trap does not kill, so only the hurt flash).
6. **Feel**: the belt's push on a client (a velocity packet each two ticks), how the rams look, the particle and sound mix, the timings.
7. **Nothing was run in a game.** The self-test and the type-check are the only checks beyond the pure JUnit tests, and the self-test has never executed.
8. The CI smoke test's `SMOKE_EXPECT` list (`.github/workflows/smoke.yml`) was not edited; add `compat.create` to it to make the CI require the self-test.

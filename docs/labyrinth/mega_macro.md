# MACRO: the Sanctuary Hub's terrain generator (3D noise, 3D Wave Function Collapse, a `ChunkGenerator`) - 1.55.0

Builder `mmc` of the bidirectional Mega-Dungeon (`docs/labyrinth/mega_floors.md`, brief system 2). Package `com.newuniverse.nusmp.labyrinth.mega.macro`, tests under
`src/test/java/.../mega/macro`, config `nusmp-labyrinth-mega-macro-server.toml` (section `[macro]`), command `/multiverse mega macro`.

**State of this document.** Written without a game. The pure model, both solvers and every number below are unit tested and were LOOKED AT as pictures (section 10);
the Minecraft-facing classes compile against the API stubs and were checked line by line against the 1.21.1 `ChunkGenerator` contract (section 8), but nothing was run
inside the game. Section 11 is the honest list of what is unverified.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. NEW, REPLACEMENT, KEPT (owner's rule 5)

| Part | Status | Notes |
|---|---|---|
| The Hub's terrain generator (`SanctuaryGenerator`, codec `nusmp:sanctuary`) | NEW | nothing like it existed; the dungeon floors stay Jigsaw (floors doc, section 3) |
| 3D simplex noise (`Simplex3D`) | NEW | |
| 3D Wave Function Collapse (`Wfc3D`, `SpireTiles`, `ChasmTiles`) | NEW | the 1.54.0 socket-matching assembler is KEPT and untouched: it is the same idea specialised for doorways and it builds the dungeon floors |
| The Holy Spire and the Abyssal Chasm | NEW | the 1.54.0 "Spire" (the old Labyrinth tower, `layout = "spire"`) is KEPT; this is a different building with a different job (the road to the Holy floors) |
| The Hub's plains, roads and lamp posts | NEW | the plaza template is the HUB builder's (`mega.hub`) and lies on top of the flat disc this generator prepares |
| The gate blocks | KEPT | the existing `nusmp:labyrinth_gate` is the only block of this mod used; the position decides which road a gate is (`HubLayout.gateNear`) |
| Anything else in the repo | KEPT | no file outside this package was edited; `MacroRegistry` was the contract stub and is filled in |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. The shape of the module and WHY

```
 HubLayout (contract, pure numbers)                world seed
        |                                              |
        v                                              v
 Simplex3D  Wfc3D  HelixPath/Spiral  TilePattern   SanctuaryGenerator.hubSeed(RandomState)
        \     |        |               |                       |
         +----+--------+---------------+--> SpireModel, ChasmModel --> HubModel.of(seed, params)  (pure, cached per seed)
                                                                              |
        BlockKind --KindBlocks (string table)--> MacroBlocks (BlockState) <---+--- SanctuaryGenerator.fillFromNoise / getBaseHeight / getBaseColumn
```

**Why a PURE model separate from the generator.** A chunk generator is the hardest kind of code to get right without a game: a mistake shows as a world that looks wrong, or a
chunk that never finishes and freezes the player in it. Everything that decides what the Hub looks like is therefore one function `(seed, x, y, z) -> BlockKind` with no Minecraft
import, so it can be unit tested (the walking search proves both gates are reachable on 20 seeds), drawn as pictures by `MacroPreview` (the owner can look at it today), and the
Minecraft part shrinks to "ask the model, place the block".

| Class | What it is and WHY it exists |
|---|---|
| `Simplex3D` | Seeded 2D/3D simplex noise and fractal sums. WHY simplex and not Perlin: no axis-aligned artefacts (Perlin's lattice shows as faint grid ridges, which on a tower or a pit wall looks like brickwork nobody asked for) and 4 corners per 3D sample instead of 8, so the few million samples cost about half. The permutation is shuffled from the seed and every sample is offset by seed-derived amounts (plain simplex is exactly 0 at the lattice origin whatever the permutation). Deterministic on every JVM. |
| `Wfc3D` | A real 3D Wave Function Collapse solver (section 4). |
| `HelixPath` | The table of a spiral road: one row per block of arc, half a block of height per row, holds at landings. WHY a table: the road must exist whatever the noise or the tiles do, so it is computed from a few numbers and consulted BEFORE the noise or a tile. A row is a wedge and its boundary is where the surface steps by half a block; the arc is measured at the INNER edge of the walkway (`arcInset` = half its width), so no two lattice neighbours of the walkway are more than one wedge apart and every column can be walked onto without a jump (measured by `everyColumnOfTheWalkwayCanBeWalkedOntoFromItsNeighboursWithoutAJump`; with the arc measured at the centre the inner lane had full block steps). |
| `Spiral` | Says what every voxel near a `HelixPath` is: walking surface (block or bottom slab), support, carved headroom, curb, rail. WHY: the Spire's stair and the Chasm's ramp are the same object seen from two sides; both ask this class and only choose materials. |
| `TilePattern` | A 6 x 6 x 6 block of `BlockKind`s: the voxels of a tile. Authored once at 6, resampled to the configured cell size (the first and last layer stay one voxel thick, so floors and doorway sills are never doubled). |
| `SpireTiles`, `ChasmTiles` | The two tilesets: sockets, weights, voxel art, and the way each landmark's problem is posed (section 5, 6). |
| `SpireModel`, `ChasmModel` | The two landmarks as pure functions (section 5, 6). |
| `HubModel` | The whole Hub: plains + roads + the two landmarks; `kindAt`, `fillColumn`, `topY`, cached per `(seed, MacroParams)`. |
| `BlockKind` | The closed vocabulary of what the Hub places (49 kinds). WHY closed: the Vanilla Block Primacy Rule is then checkable (the test reads every row against the game's own block-state table) and the tilesets are plain data. |
| `KindBlocks` | The string table `BlockKind -> block id + properties`, and the connection rules of the three railing blocks. WHY pure: so a test can check every row against `vanilla_block_states_1_21_1.json`. |
| `Walkability` | A breadth-first search over what a walking player can do (section 7). |
| `WalkRoutes` | The start cells, target cells and boxes of the two routes, shared by the unit test and the in-game self-test, so they cannot disagree. |
| `Geo`, `MacroParams` | Shared arithmetic (smoothstep, centred sectors, `StrictMath` angles) and the immutable tuning record (the cache key). |
| `MacroConfig` | The server config (plain `define`s, never `defineInList`). |
| `MacroBlocks` | The one place a kind becomes a `BlockState`; built lazily (the block registry is frozen by the time a chunk is generated, not when the class loads); a missing block becomes air with ONE warning. Computes the connection properties of fences, walls and chains from the model's neighbours, because chunk generation places blocks without neighbour updates. |
| `SanctuaryGenerator` | The `ChunkGenerator` (section 8). |
| `MacroRegistry` | Registers the codec on the MOD bus in `init` (the dimension registry is read from the datapack when the server starts, which is after every mod-bus registration), adds the self-tests, drops the cached models on server stop. |
| `MacroCommand` | `/multiverse mega macro info` and `sample <x> <y> <z>` (permission 2, read-only). |
| `MacroSelfTests` | `macro.model`, `macro.wfc`, `macro.generate`, `macro.structures` (section 9). |
| `MacroPreview`, `VoxelView` (test sources) | The picture tools (section 10). |

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. The plains, the roads, and the order of authority

* `surfaceHeight(x, z)` is the y of the FIRST FREE CELL above the ground (the grass block is at `surfaceHeight - 1`, a player stands at `surfaceHeight`; the plaza template and the hub
  self-tests use the same convention). It is `SURFACE_Y` plus `hillAmplitude x fbm2D(x/48, z/48, 2 octaves)` times a mask. WHY these numbers: amplitude 5 and wavelength 48 are the
  brief's "gentle rolling hills"; two octaves so the hills are not a single sine.
* The MASK is the minimum of smoothsteps: 0 inside `PLAZA_RADIUS` and over the 24 blocks after it, 0 within `SPIRE_BASE_RADIUS + 16` of the Spire (and 24 more to blend), 0 within `RIM + 24`
  of the Chasm (it must cover the three rings of caverns, 18 blocks beyond the wall, so the dirt layer above them is intact), and 0 along the two roads (|z| < 3, fading by 15).
  WHY flat roads: a road that follows 5-block hills is a roller coaster of one-block steps; flat roads keep the walk to each door easy. Their ends lie deep inside the flat zones of the plaza and
  the landmarks, so the mask has no seam.
* Strata: grass (1), dirt (3), stone, deepslate below y -8 with the noisy seam up to y 0 (the same seam vanilla uses), bedrock at y -64 and a noisy second layer.
* ROADS: stone bricks, 3 wide, along z = 0 from x 12 to the Spire's door and from x -12 to the Chasm's rim, flush with the terrain (they REPLACE the grass block). A lamp post (a stone-brick wall
  post with a lantern on top) every 12 blocks on both sides at z = +-3. They end where a landmark's own floor begins (the Spire's portico starts about 4 blocks before the nominal wall).
* ORDER OF AUTHORITY (a voxel takes the first rule that claims it): landmark over road over terrain. Inside a landmark: (Spire) stair, crown, chandelier, hall floor, gallery cell, shell, shaft;
  (Chasm) ramp, rim fence, gate and platform, floor, open pit, caverns and wall skin, terrain.
* Nothing hostile and nothing a passive animal can be hurt by: no lava, no fire, no cactus, no spawner; water appears only as sealed pools (section 6); the pit is fenced at the rim and along the ramp.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. The Wave Function Collapse solver (`Wfc3D`) and WHY each choice

* Tiles have SIX face sockets (an `int` 0..31 per face) and a weight; a `Tileset` has an explicit symmetric matching table (a socket always matches itself, further pairs are listed). WHY a table and not
  "equal ids": the tests' three-colouring and the chasm's "open/rock" both need it, and it keeps the solver independent of what the sockets mean.
* Cells are EXCLUDED (the noise says solid rock or outside the volume: nothing is solved), FREE, or FIXED (pre-collapsed: the guaranteed path, the doorways). A FREE cell may be restricted to a subset of the
  tiles (`allowOnly`, `forbid`). WHY: a doorway tile must only exist where the caller pre-collapsed a cell (a landing is behind it); chosen freely it would open a door onto a sheer drop.
* Domains are one `long` per cell (a tileset has at most 64 tiles; ours have 11 and 19), so a propagation step is a few bit operations. WHY: the largest real grid (42 x 19 x 3 = 2394 cells) solves in
  tens of milliseconds, and the solver can run on a chunk-generation thread the first time any chunk of the Hub is made.
* Lowest-entropy-first with a seeded tie-break (a tiny jitter per cell), Shannon entropy of the remaining weights (`StrictMath.log`), constraint propagation with a work queue (arc consistency).
  WHY lowest entropy: it is the heuristic that makes WFC fail rarely (the most constrained cell is decided first, while it still has a choice).
* CONTRADICTION handling in three escalating layers: (1) bounded chronological BACKTRACKING (a trail of `(cell, old domain)`; undo the last collapse and BAN that tile there; budget `max(200, 2 x free cells)`),
  (2) a RESTART with the next seed (up to `wfcTries`, default 12), (3) a documented deterministic FALLBACK that fills every free cell with the caller's plain tile (rock / pier), so a bad tileset can degrade a
  landmark but can never stop world generation or make it loop. An infeasible start (fixed cells that contradict each other, an edge mask that leaves no tile) is detected without any search and is not retried.
* The x axis may WRAP: both landmarks are rings of angular sectors. A face whose neighbour is outside the grid or EXCLUDED is checked against a per-direction EDGE MASK of acceptable sockets (the caller says
  there that the roof must be solid, that the face towards the pit may be a mouth, and so on).
* Deterministic for a given (tileset, problem, seed): `SplittableRandom`, `StrictMath`, nothing depends on hash order, so a world regenerated on another machine gets the same interiors.
* Result: `tileAt(x, y, z)`, the number of restarts, whether the fallback was used, whether the problem was infeasible, collapses, backtracks, nanoseconds.

**Measured** (100 seeds, `LandmarkModelsTest.everyGalleryAndTheChasmAreSolvedWithoutFallbackOnAHundredSeeds`): 600 Spire gallery problems and 100 Chasm problems, ZERO restarts, ZERO backtracks, ZERO fallbacks.
Spire (6 galleries): about 290 collapses in 8 ms; Chasm: about 2360 collapses in about 40 ms (a cold JVM: the first solve is slower than that). The unsolvable-by-design cases (an odd alternating ring, a contradictory pair of
fixed cells) and a fuzz over random tilesets exercise the backtracking, restart and fallback paths (`Wfc3DTest`).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. The Holy Spire (`SpireModel`, `SpireTiles`)

Numbers: base radius 34, crown radius 20, height 150 (`HubLayout`); the outside radius is `lerp(34, 20, smoothstep(t))`, so the foot flares and the crown rounds off. The inside face is 3.5 blocks in. The
outside is moved by 3D simplex noise (`spireNoise`, default 2.0) so it is not a clean cone.

* **The stair** (`HelixPath` + `Spiral`): 3 wide, one half block of rise per block of arc (a full block, a bottom slab, a full block ...: a player walks up without jumping), hugging the inner face (the walkway's
  outer edge IS `rin(row)`, tested to 1e-9), a rail column on the open side (a curb block and an end-stone-brick wall; a lantern on the wall every 8th step), 5 blocks of carved headroom. It starts on the front
  door's landing and makes 2.78 turns (346 blocks of arc) to the crown. Landings every 24 blocks of height (the brief says "about 20": 24 matches the gold band spacing it also names, and leaves 12 blocks of
  collar and 12 of window band between landings). Rails start 4 half steps up, so the first steps can be entered from the hall floor.
* **The crown**: a floating disc of radius 9 at `PLATFORM_TOP` (three blocks thick, gold rings inlaid), joined to the wall by a 5 wide bridge at the angle where the stair arrives (railed), with the Holy gate at
  `HOLY_GATE_*`: a 3 wide, 4 high plane of `nusmp:labyrinth_gate` in a frame of gold blocks. The gap between the disc and the wall is open, so from the hall floor you see the underside of the crown and the sky
  round it. WHY a floating disc and not a full floor: a full floor needs the stair to tunnel through its rim over the last turn (a slot a player could fall into). The parapet rows above carry twelve
  openings, a gold cornice and battlements, and eight slender pinnacles (a quartz pillar nine blocks tall with a gold cap and a lantern) rise above the crown; the tower proper ends at
  `SPIRE_HEIGHT` as the layout says, the pinnacles are decoration above it (the top lantern is at y 227, far under the build limit of 319). A great chandelier (a chain with a lantern every 20 blocks and one at its foot) hangs from the underside of the disc on the axis.
* **The shell** between galleries: noise-roughened quartz bricks, smooth quartz and calcite (a mix by 3D noise), glass windows (16 round the tower, 7 rows, arched at the top, staggered by band), a band of
  yellow concrete every 24 blocks (see KindBlocks: not real gold, because the Hub is buildable and a few thousand gold blocks in a wall would be a gold mine; the Holy gate's 13 frame blocks are real gold and are
  inside the protected sphere round the gate).
* **The galleries (the trial chambers "upwards")**: one at every landing, the cells reach 8 blocks out from the inner face and are 2 cells high. Each gallery is its OWN small Wave Function Collapse problem
  (`SpireTiles.problem(sectors, ground)`): the ring is cut into as many sectors as fit round the shaft at that height (`round(2 pi rin / cell)`: 32 at the foot, 19 at the top, so a cell stays about 6 blocks wide
  and doorways stay 4 wide at every height, which one problem for the whole tower could not do because the tower tapers) and it is ROTATED so that sector 0 is centred on the landing; that cell is pre-collapsed
  to the doorway tile (`hall_door`; on the ground gallery the front door, `passage`), so the door onto the landing is exactly in front of the landing whatever the solver chooses for the rest. The edge masks say: solid
  floor and roof, window or wall towards the shaft and the outside.
* The cell voxel is found from the column: sector and fraction from the angle (`Geo.sector`), `a` from the fraction, `b` from the row, `c` from the distance from the inner face; `TilePattern.sample` does the rest.

### Spire tiles (11)

Sockets (a socket matches itself): tangential `T_WALL` closed wall, `T_DOOR` a 2 x 3 doorway between neighbouring rooms; vertical `V_SLAB` a solid floor, `V_OPEN` a 4 x 4 opening (a double-height hall);
towards the shaft `I_WALL`, `I_WIN` an arched glass window, `I_DOOR` the 4 x 4 doorway onto a landing; towards the outside `O_WALL`, `O_WIN`, `O_DOOR` (the front door). Order of the columns: +a, -a, +b, -b, +c, -c
(a along the ring, b up, c outwards).

| # | tile | weight | +a | -a | +b | -b | +c (outside) | -c (shaft) | what it is |
|---|---|---|---|---|---|---|---|---|---|
| 0 | `pier` | 4.0 | WALL | WALL | SLAB | SLAB | WALL | WALL | solid masonry between rooms |
| 1 | `hall_door` | 1.0 | DOOR | DOOR | SLAB | SLAB | WALL | DOOR | the room behind a landing: gold sill, two chandeliers; PRE-COLLAPSED ONLY |
| 2 | `passage` | 1.0 | WALL | WALL | SLAB | SLAB | DOOR | DOOR | the front door straight through to the hall; PRE-COLLAPSED ONLY (ground gallery) |
| 3 | `room_through` | 3.0 | DOOR | DOOR | SLAB | SLAB | WIN | WIN | a corridor room with windows both ways, one chandelier |
| 4 | `room_pillars` | 2.0 | DOOR | DOOR | SLAB | SLAB | WALL | WIN | four corner pillars, chiselled medallion |
| 5 | `room_end_r` | 2.0 | DOOR | WALL | SLAB | SLAB | WIN | WALL | an end room, door on +a |
| 6 | `room_end_l` | 2.0 | WALL | DOOR | SLAB | SLAB | WIN | WALL | an end room, door on -a |
| 7 | `room_altar` | 1.0 | DOOR | WALL | SLAB | SLAB | WIN | WALL | a chiselled altar with two lanterns |
| 8 | `hall_low` | 1.5 | WALL | WALL | OPEN | SLAB | WIN | WIN | lower half of a double-height hall (roof open) |
| 9 | `hall_high` | 1.5 | WALL | WALL | SLAB | OPEN | WIN | WIN | upper half (floor open), a long chain and lantern |
| 10 | `room_sealed` | 1.0 | WALL | WALL | SLAB | SLAB | WIN | WIN | a vault seen through glass |

WHY 11 tiles: each is a distinct, readable piece of architecture, and the sockets are the whole grammar. The doorway sockets towards the shaft and the outside are kept out of the choices of every cell the
caller did not pre-collapse, so a door can never open onto a drop. The upper tier of a gallery is not reachable on foot except through a tall hall (it is the view from outside); the owner can add a stair tile later
without touching anything else.

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. The Abyssal Chasm (`ChasmModel`, `ChasmTiles`)

Numbers: rim radius 46, floor radius 30, depth 116 (`HubLayout`). The wall radius is `30 + 16 (1 - t)^1.6` for `t` from the rim (0) to the floor (1), a flared bowl: about 13 degrees off vertical at the rim,
vertical at the bottom (precomputed once with `StrictMath.pow`: the curve decides the shape of the whole pit). The pit floor is the standing level of the Abyssal gate plane (y -46), a top block at y -47, 16 blocks above bedrock.

* **The wall**: rock RIBS jut into the pit (`chasmNoise`, default 3.0, a fractal 3D noise, faded to nothing in the top 16 blocks, only ever inward, never a recess, so the cave cells that start at the nominal wall
  radius are always at or behind the real wall). Strata of the wall's skin (2 blocks): stone (the terrain), moss patches high up, deepslate with cobbled patches in the middle, blackstone with basalt and sculk
  near the bottom (y < -20).
* **The ramp**: 4 wide, one half block of descent per block of arc, 1.52 turns (299 blocks of arc), a curb and a dark nether-brick fence on the pit side with a chain along its top (horizontal, its axis
  worked out from the neighbours) and a soul lantern every 12th step, 5 blocks of carved headroom (cut AFTER the ribs, so it is clear at any roughness: `theRoadsSurviveTheRoughestSettingsOfTheConfig`).
  It starts on a landing centred on the rim door (flush with the grass) and ends on the floor. Landings every 18 blocks, each at the standing level of a cave cell's floor.
* **The rim**: a ring of fence with a soul lantern every 9 blocks, with a gap where the road arrives.
* **The floor**: deepslate tiles and polished deepslate with sparse moss and sculk; a platform of radius 7 (polished blackstone bricks, chiselled rings) with the Abyssal gate (3 x 4 plane of the gate block in a crying obsidian frame) and
  two soul lanterns, and a chiselled strip from the platform to where the ramp arrives.
* **The caverns (the "sprawling caverns downwards")**: behind the wall, three rings of cells (18 blocks), 19 levels from just under the dirt down to the floor, 42 sectors round (about 6 blocks wide at 40 blocks
  radius). One Wave Function Collapse problem for the whole grid; at every landing the cells behind it (the sectors the landing covers, ring 0) are pre-collapsed to a MOUTH tile (a tunnel from the pit into the rock),
  so a side cave is always one step from the road and its floor is flush with the landing. The top, the bottom and the deepest ring are rock; towards the pit a cell may be rock or a mouth.
* **Interior dressing** (`ChasmTiles.flavour`): what grows in a cave cell is a stable hash of the seed and the cell's place: plain 30 %, pillar 12, pointed dripstone 18, sculk 10, moss with glow lichen and carpet
  14, a small sealed pool 8, a ledge 8. WHY a hash and not more tiles: seven flavours would make 133 tiles and break the "small tileset", while a hash is stable, free, and never makes two neighbours incompatible,
  because the dressing never touches a face layer except to swap a solid floor block for another non-walkable block (sculk, moss, water), so the openings always line up (the seam test covers every flavour of every tile).
  WATER only on a closed floor, inside a ring of rock (a 2 x 2 pool in the floor row, so it is sealed on every side; its tile has a closed bottom face): it can never flow. There is no lava kind in the vocabulary.

### Chasm tiles (19)

Sockets: `ROCK` (a solid face) and `OPEN` (a round 12-voxel hole of radius 1.9 in the middle of the face), the same hole on every face, so two open faces always line up. A cave cell is a bubble (an ellipsoid of
radii 2.4 x 2.1 x 2.4) with a tunnel to each open face. Tiles 0..15: the 16 subsets of the four horizontal faces (`+a`, `-a` round the pit; `+c` deeper into the rock; `-c` towards the pit = a mouth); the weight is
14 for solid rock and 0.9 / 1.6 / 1.2 / 0.6 for 1 / 2 / 3 / 4 open faces. Tiles 16..18: chimney (a vertical shaft that starts, continues, ends), weight 0.3 each, with all four horizontal faces closed. Index = `A_PLUS 1 | A_MINUS 2 | C_PLUS 4 | C_MINUS 8`.
Result of a typical solve (seed 12345): 49 % solid rock, 51 % cave cells, 32 % of the cells of the first ring (the ones next to the pit wall) are mouths.

WHY this set and not all 64 subsets of six faces: the 16 horizontal subsets are COMPLETE in the plane (any combination of what the neighbours demand has a tile), which is what makes the solve reliable (zero
backtracks in 100 seeds); the vertical faces are the only incomplete part and chimneys are rare, so a contradiction there is handled by the backtracking, which it never needed. 19 tiles is above the brief's "8-16" by the three chimneys.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. The guaranteed roads and the walking search

* The stair and the ramp are PRE-COLLAPSED: `Spiral.part` is consulted before any noise or tile, the headroom is carved after the ribs and the shell, and the only cells of the Wave Function Collapse that touch the
  road are fixed (the doorway behind each landing).
* `Walkability.reach` (breadth-first search over STANDING STATES): a state is a column and the height of the feet in half blocks (a bottom slab is a state half a block above its neighbours); it is standable when the
  block under the feet is solid or a bottom slab and the two cells of the body are passable; a move goes to one of the four neighbouring columns (no diagonals: the strictest reading) and may rise or fall by at most
  one block (a jump needs the three cells above the start free); a railing, water, a dripstone point or a gap is an obstacle; the search is bounded by a box. It runs on the real model: `WalkabilityTest` proves
  both gates reachable from the road outside each door on 20 seeds (route lengths: Spire 441 moves, Chasm 394; about 0.1 s each), at the roughest settings of the config, and at every cell size 4..8; and a NEGATIVE
  CONTROL cuts three blocks out of the walkway and the search must fail (it does; a search that cannot fail proves nothing).
* Mutation checks (a mutated copy of the source in a scratch directory, the relevant tests must fail; all 21 mutants were killed): no rail column; no bridge to the crown; the doorway cell not pre-collapsed;
  slabs not standable; a jump without headroom; plain stretching of tile art (doubles the floor row); a wrong slab property in the block table; the gold trim as real gold; the hills not masked; no road; no pit;
  no ramp surface; no carved headroom; the radius test of the column search dropped; a landing that does not hold; a per-cell restriction ignored; sectors not centred; a different hole size on one axis; a door on the wrong
  side of a tile; the arc of a wedge measured at the centre of the walkway (Spire and Chasm). (Two further mutants of `Wfc3D` and `Simplex3D` were checked by the previous agent: see `Wfc3DTest`, `Simplex3DTest`.)

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. The 1.21.1 `ChunkGenerator` contract (`SanctuaryGenerator`)

Signatures are those of the API dump (`bash $S/apigrep_lab.sh net.minecraft.world.level.chunk.ChunkGenerator '.'`) and the type check compiled every `@Override` against it. Reference sources read online, as
untrusted DATA (nothing was run): `raw.githubusercontent.com/jakob-wlf/LotMCraft/main/src/main/java/de/jakob/lotm/dimension/DreamMazeChunkGenerator.java` (a NeoForge 1.21.1 mod's generator: codec with one `biome_source`
field, the same abstract members), `.../baileyholl/Ars-Nouveau/main/src/main/java/com/hollingsworth/arsnouveau/common/world/dimension/PlanariumChunkGenerator.java` (it overrides `createReferences`,
`findNearestMapStructure`, `applyBiomeDecoration`, `getSpawnHeight` the same way), and the decompiled 1.21 `FlatLevelSource` / `ChunkGenerator` / `ProtoChunk` from `Yeet-Masta/MCP-1.21` (the `fillFromNoise` pattern with
both `_WG` heightmaps, `ProtoChunk.setBlockState`'s heightmap handling, and that `createStructures` is the only place structure starts are made).

| Member | What this generator does and WHY |
|---|---|
| `CODEC` / `codec()` | `MapCodec<SanctuaryGenerator>` with exactly one field, `BiomeSource.CODEC.fieldOf("biome_source")` (the hub's dimension JSON is `{"type": "nusmp:sanctuary", "biome_source": ...}`). Registered under `nusmp:sanctuary` in `Registries.CHUNK_GENERATOR` by `MacroRegistry.init` on the MOD bus. |
| `fillFromNoise` | Asks `HubModel` for every column of the chunk and places non-air blocks with `chunk.setBlockState(pos, state, false)` and updates `OCEAN_FLOOR_WG` and `WORLD_SURFACE_WG` (the pattern of `FlatLevelSource`; vegetation features and mob spawning read those heightmaps). Returns a completed future (like the flat and debug generators). A throwable is caught: ONE warning, and that chunk is drawn as plain plains (the safety net: a bug must never stop chunk generation). |
| `buildSurface`, `applyCarvers` | No-ops: the surface, strata and bedrock are part of `fillFromNoise`, and nothing else may cut the landmarks. |
| `spawnOriginalMobs` | Calls `NaturalSpawner.spawnMobsForChunkGeneration` like the vanilla generators, so the biome's passive animals exist at the first visit. |
| `getGenDepth` 384, `getMinY` -64, `getSeaLevel` 63, `getSpawnHeight` `SURFACE_Y` | The dimension type is `min_y -64, height 384`. |
| `getBaseHeight` | One above the highest block that the given heightmap type counts as opaque, found in the model's column (as `FlatLevelSource` does with its layers). |
| `getBaseColumn` | A `NoiseColumn` of the model's column, so structure code and the debug screen agree with what was generated. |
| `addDebugScreenInfo` | One line: the hub seed and the plains height. |
| `createStructures`, `createReferences`, `findNearestMapStructure` | Do NOTHING / return null. WHY: "absolutely zero structures from other mods". A vanilla noise generator asks every structure set of the registry, and mods add theirs there; this generator never asks. It is the second of three layers (the cloned biome that is in no structure tag; this; the hub's self-test that loads chunks and asserts no start exists). |
| `applyBiomeDecoration` | NOT overridden: the biome's vegetation features (grass, flowers, plains trees) run through the superclass on valid ground, and the landmarks are drawn first so features do not overwrite them (they only place on grass or dirt). |

The world seed is `randomState.getOrCreateRandomFactory("nusmp:sanctuary").at(0, 0, 0).nextLong()`: a function of the world seed alone (verified in the decompiled 1.21 `RandomState`: the constructor builds `random = settings.getRandomSource().newInstance(seed).forkPositional()` and `getOrCreateRandomFactory(id)` is `random.fromHashOf(id).forkPositional()`, cached; a non-noise generator is given the dummy settings, so the source type is fixed), so every chunk of a
world agrees on the Hub in any order and on any thread, and after every restart. The model is cached per `(seed, MacroParams)` in a `ConcurrentHashMap` (at most 6 entries; cleared on server stop); two threads may race to build the same model
(about 60 ms) and one wins, which is safe because the model is immutable.

**With `enabled = false`** (`MacroParams.landmarks`): no Spire and no Chasm; plain rolling plains; the Holy gate stands on a 7 x 7 quartz platform floating at its coordinates and the Abyssal gate in a sealed 9 x 9 x 7 pocket in
the rock at its depth (so the position-based gate lookup still works). Nobody can walk to them: an admin teleports there (`/tp`); the hub module's own commands do not lead there. The config applies to chunks generated
from then on (a changed config over existing chunks would leave a seam).

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Config, command, self-tests, integration

**Config** `nusmp-labyrinth-mega-macro-server.toml`, section `[macro]` (class `MacroConfig`): `enabled` true; `cell` 6 (4..8); `wfcTries` 12 (1..64); `spireNoise` 2.0 (0..4); `chasmNoise` 3.0 (0..6); `hillAmplitude` 5.0 (0..12).

**Command** `/multiverse mega macro` (permission 2): `info` (the hub seed, the landmark switch, the numbers, how long the model took to build, the solver's restarts / fallbacks / collapses / backtracks / time), `sample <x> <y> <z>`
(what the model says is there, what the world has there if the chunk is loaded, never loads a chunk).

**Self-tests** (`SelfTests.register`, run by `/multiverse mega selftest`; each prints OK "skipped" when the level `nusmp:sanctuary` does not exist, i.e. a build without the hub):

| key | what it asserts |
|---|---|
| `macro.model` | for the LIVE world seed: `kindAt` at both gate centres is GATE; the door cells are AIR; a walking search from the road outside each door reaches the cell beside each gate |
| `macro.wfc` | both landmark problems re-solved for the live seed with a fresh (uncached) model: no fallback, under 3 s |
| `macro.generate` | force-loads the 3 x 3 chunks round a point of the plaza disc, the Spire's door and the Chasm's centre and asserts: the plaza disc is grass (or dirt under a tree) at `SURFACE_Y - 1`, the Chasm's centre column is air from the floor to the rim, the Spire's doorway is air, both gate centres are not air; then un-forces them |
| `macro.structures` | no chunk of those 27 has a structure start |

**Lead integration** (exact lines in the report): config registration, the command under `/multiverse mega`. `MegaSystem` already calls `MacroRegistry.init` and `register`; there is no client code and no lang key (every message is an admin literal).

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Pictures (the owner can look at the Hub today)

```
javac -d build/macro_preview -sourcepath src/main/java:src/test/java src/test/java/com/newuniverse/nusmp/labyrinth/mega/macro/MacroPreview.java
java -Djava.awt.headless=true -Xmx2g -cp build/macro_preview com.newuniverse.nusmp.labyrinth.mega.macro.MacroPreview OUT_DIR [seed] [only]
```
`only` keeps the pictures whose file name contains that text (`render`, `slice`, `plan`, `path_side`, `spire`, `chasm`). They are flat colours per block kind (no textures): they show form and are exactly what the generator
builds, because they read the same function. `VoxelView` is a small perspective ray-caster over the model (voxel traversal; glass is see-through; a cutaway removes half a tower or the roof).
Copies of the main pictures are in `docs/labyrinth/mega_macro/`:

| file | what it shows |
|---|---|
| `hub_topdown.png` | the whole Hub from above: plaza disc, the two roads with lamp posts, the white tiered Spire with its gold crown, the dark Chasm with its ramp |
| `spire_render_exterior.png` | the Spire as a player sees it walking up the road: collars, windows, gold bands, crown |
| `spire_render_cutaway.png` | the south half removed: the spiral stair climbing the inner wall, the galleries, the chandelier chain, the crown and the Holy gate at the top |
| `spire_render_shaft_up.png` | looking up the shaft from the hall: the helix of the stair, and the sky round the floating crown disc |
| `spire_render_crown.png` | the crown from above: the disc with its gold rings, the gate, the parapet with its twelve openings |
| `spire_slice_x.png`, `spire_plan_y70.png` | a vertical slice through the axis and a plan at y 70 (the ground gallery: rooms, windows, the front door at the left) |
| `spire_path_side.png` | the stair's side view, unrolled: 346 blocks of arc, 2.78 turns, a landing at every gallery |
| `chasm_render_cutaway.png`, `chasm_render_ramp.png` | the pit with half cut away: the bowl, the ramp winding down, the floor |
| `chasm_render_floor.png` | on the floor: the platform, the Abyssal gate in its frame, the soul lanterns, the end of the ramp |
| `chasm_slice_x.png` | a vertical slice through the pit: the flared walls, the caverns behind them, the strata |
| `chasm_path_side.png` | the ramp's side view, unrolled: 299 blocks of arc, 1.52 turns, six landings, each with a cave mouth |

Everything is written to a scratch directory by default; nothing is generated into the repository except the copies above.

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. Tests, and what is NOT verified

Pure tests (`jt.sh`, CI runs them with real JUnit): `Simplex3DTest`, `Wfc3DTest`, `Wfc3DRestrictTest`, `HelixPathTest`, `GeoTest` (also: every cell size 4..8 builds a walkable Hub), `TileSetsTest` (every pair of matching sockets
shows the same opening, every flavour, doorways wide enough, water sealed), `KindBlocksTest` (every row against the 1.21.1 block-state table), `HubModelTest`, `LandmarkModelsTest`, `WalkabilityTest`.

NOT verified (nothing was run in the game): the look in the real renderer with real textures and light (the pictures are flat colours); that the first chunk of a world is generated without a hitch on a real server thread
(the model build is about 60 ms, the safety net exists for a surprise); that the vegetation features behave round the towers (they only place on grass or dirt, which the towers do not have); the self-tests (they compile and are logically checked, never executed); that the hub module's protected sphere (radius 8) round each gate keeps the
Holy gate's gold frame from being broken (that is the hub module's rule); performance of the real `ProtoChunk.setBlockState` over a full landmark chunk (about 40 000 placements; the
flat generator does similar).

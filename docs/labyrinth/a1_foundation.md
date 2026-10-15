# Labyrinth A1: foundation (template toolchain, masonry, dimension and gate plumbing)

Part of the Labyrinth (1.35.0, `docs/labyrinth.md`). This section covers the generated-template toolchain, the six masonry blocks,
the `nusmp:labyrinth` dimension, the two structures' placement JSON and the two Dungeon Gate pieces. Nothing here was run in game;
the Python tools have `--check` modes and `tools/labyrinth/test_nbt.py`, and `LabyrinthBlocks.java` was type-checked against the
1.21.1 / NeoForge 21.1 stubs.

## 1. The NBT / template toolchain (`tools/labyrinth/nbt.py`, `template.py`, `test_nbt.py`)

**What.** `nbt.py` is a dependency-free writer and reader of the Java-edition NBT format (gzip, big-endian, modified UTF-8, all
thirteen tag types). `template.py` is a small DSL over it: `Template(name, sx, sy, sz)`, `.set`, `.fill`, `.hollow_box`, `.column`,
`.stamp`, `.jigsaw`, `.chest`, `.be`, `.save(root)`, `load(path)`, `ascii_layers()`, and a CLI (`--check`, `--preview NAME`, `--list`).
`save` writes `root/data/nusmp/structure/<name>.nbt` as `{DataVersion:3955, size, palette, blocks, entities}` with a deduplicated
palette (one entry per block id + property set), blocks sorted by (y, z, x) and a gzip mtime of 0, so the same script always
produces the same bytes. Cells that were never set are not written: the world keeps its own block there.

**Why no Blockbench / structure blocks.** The Labyrinth is dozens of Jigsaw pieces built by several people that must agree on sizes,
jigsaw rows and block ids to the block (the vertical chain only works when the upper piece's bottom row and the lower piece's top row
are the jigsaw rows). A hand-saved `.nbt` cannot be reviewed in a pull request, cannot be regenerated when a block id or the palette
convention changes, and silently drifts (a stray torch, an off-by-one jigsaw). A generated template is deterministic, reviewable in a diff
(`--preview` prints the layers, so a review sees what changed), regenerated with one command (`python tools/labyrinth/gen_gate.py`)
and validated in CI by `python tools/labyrinth/template.py --check`, which round-trips every `.nbt` under `data/nusmp/structure`
through the reader and writer, checks DataVersion and bounds, and checks every jigsaw's orientation (one of the twelve),
`name`/`target`/`pool` (namespaced ids), `joint` (rollable/aligned), `final_state` (a block state string) and that it does not face
into a solid cell of its own piece. `nbtlib` is not used because the CI image has no third-party Python, and the format is 200 lines.

**Why these NBT details.** Minecraft's `ListTag` writes an empty list with element type `TAG_End`; the reader accepts that and
`template.py` pins `entities`/`palette`/`blocks` to `TAG_Compound` so an empty list still carries the type vanilla expects.
Strings are Java modified UTF-8 (NUL as `C0 80`, astral characters as surrogate pairs), which matters only for exotic names but is
what `DataInput.readUTF` parses. `LootTableSeed` is written as a `Long` and `placement_priority`/`selection_priority` as `Int`
because `RandomizableContainer` and `JigsawBlockEntity` read those exact types. Block-entity compounds are stored without `x/y/z`
like a structure block saves them; `StructureTemplate.placeInWorld` adds the position on placement.

## 2. The masonry (`labyrinth/LabyrinthBlocks.java`, `tools/labyrinth/gen_block_textures.py`)

**What.** Six blocks: `labyrinth_bricks` (walls), `labyrinth_tiles` (floors), `labyrinth_pillar` (a `RotatedPillarBlock`, `axis`),
`labyrinth_glyph` (light 6), `labyrinth_lantern` (light 15), `magicule_vein` (light 11). All share deepslate-brick numbers
(strength 3.5 / 6, `requiresCorrectToolForDrops`, `SoundType.DEEPSLATE_BRICKS`, map colour deepslate) and plain `Block` classes; the
light comes from `lightLevel` on the state, so no block entity or tick. Block items are registered on `NUItems.ITEMS` like every
other block of the mod, which is why `LabyrinthBlocks.init(modBus)` must run before `NUItems.ITEMS.register(modEventBus)`.

**Why own blocks instead of deepslate.** The spire and the gate are built from a signature palette so that (1) the biome palette
processor (`nusmp:biome_palette`, builder B) can swap "the pillar", "the floor" and "the accent" by block tag (`nusmp:labyrinth/swap_*`)
without touching deepslate a player built next to a gate, (2) the vein processor targets `#nusmp:labyrinth/veinable` without
lighting up every deepslate cave, and (3) the Overworld arch and the Boss Peak read as one place.

**Files.**
* `assets/nusmp/textures/block/labyrinth_bricks.png`, `labyrinth_tiles.png`, `labyrinth_pillar_side.png`, `labyrinth_pillar_top.png`,
  `labyrinth_glyph.png`, `labyrinth_lantern.png`, `magicule_vein.png`: 16x16 RGBA, drawn by Pillow from seeded random (idempotent).
  The stone is a dark blue-black with faint gold in the seams (running-bond bricks with a highlight on each brick's top edge; four
  tiles with gold corner studs; fluted pillar sides with one gold flute and a gold-ringed cap); the glyph is a brick face with a
  cyan diamond-and-bars sigil; the lantern a gold-barred cage round a warm flame; the vein is the stone with three thin cyan-white
  random walks and a dim halo. No animation: these are static so the vein processor can place thousands without an animated
  texture per face costing anything.
* `assets/nusmp/blockstates/<id>.json`, `models/block/<id>.json`, `models/item/<id>.json`: vanilla `cube_all`; the pillar uses
  `cube_column` / `cube_column_horizontal` with the three `axis` variants like a log so `RotatedPillarBlock` rotates visibly.
* `data/nusmp/loot_table/blocks/<id>.json`: self-drops with `survives_explosion` (quarried masonry is reusable; `noLootTable` is
  reserved for the unbreakable gate/door blocks of other builders).
* `data/minecraft/tags/block/mineable/pickaxe.json`: new file; `requiresCorrectToolForDrops` drops nothing unless the block is in a
  mineable tag. Other builders append to it (the generator keeps what is already there).

## 3. The dimension (`dimension/labyrinth.json`, `dimension_type/labyrinth.json`, `worldgen/biome/labyrinth_sky.json`)

**Why a NEW dimension and not the legacy one.** A datapack cannot change the chunk generator of a dimension that an existing world
already has: `level.dat` keeps the generator of every dimension it has created, so an updated `dimension/living_dungeon.json`
would be ignored by every old world. A new id (`nusmp:labyrinth`) is created fresh on first use, in old worlds too. Rule 2 also
wants the legacy dungeon left alive (`labyrinth.engine = "legacy"`).

**Why `minecraft:flat` with `layers: []` and `structure_overrides`.** A flat generator with no layers is a void: no terrain fights the
Jigsaw bounding boxes, no cave carver cuts through a tier, no surface rule paints grass on a floor, and the view is the "sea of
clouds" of the owner's spec (the sky is all there is). A flat generator places ONLY the structure sets named in
`structure_overrides`, so `["nusmp:labyrinth_spires"]` is what makes spires exist at all and what keeps vanilla structure sets
(villages, strongholds) out. `features: false, lakes: false` so the biome's (empty) feature list and the flat generator's lake
decoration never run.

**Why these dimension_type numbers.** `min_y 0, height 320, logical_height 320` holds a 128-block spire starting at y 40 with the
Treasure Vault's crown well under the build limit; `natural false` disables nether-portal spawning and compass/clock behaviour
that do not belong on a floating spire; `has_skylight true` + `has_ceiling false` so the sky lights the exterior walkways and the
tiers are lit by their lanterns and veins inside; `ambient_light 0.1` keeps the shadows readable; `fixed_time 18000` (midnight)
keeps the night palette of the biome permanently; `bed_works`/`respawn_anchor_works` false so a bed explodes like in the Nether
(a run must be left through the exit, not slept through); `coordinate_scale 1.0` so a map position means the same thing;
`effects "nusmp:labyrinth"` is the `DimensionSpecialEffects` builder C registers through `RegisterDimensionSpecialEffectsEvent`.

**Why the biome.** Structure placement is biome-driven, so a labyrinth-only biome (`nusmp:labyrinth_sky`, the flat generator's single
biome) is the lever: `#nusmp:has_structure/labyrinth_spire` lists only it, which is what keeps spires out of the Overworld and any
other dimension. It is a valid 1.21.1 biome with every spawner list empty (nothing spawns naturally on the spire; the Sealed
Archways spawn), `carvers: {}` and `features: []` (vanilla `the_void` uses the same empty shapes), deep violet-blue fog/sky/water
colours and the cave mood sound. `has_precipitation false` so no rain renders over the void.

## 4. The structures (`worldgen/structure/*.json`, `structure_set/*.json`, `tags/worldgen/**`)

**`nusmp:dungeon_gate`.** Jigsaw, `start_pool nusmp:gate/base`, `size 2`, `max_distance_from_center 48` (a single piece, the budget is
irrelevant but legal), `step surface_structures`, `terrain_adaptation beard_thin`, `start_height {"absolute": 0}`,
`project_start_to_heightmap WORLD_SURFACE_WG`.

*Why that trio guarantees a gate on the surface.* `JigsawPlacement.addPieces` samples the heightmap at the centre of the start piece's
box and moves the piece so that `minY + groundLevelDelta == startHeight + heightmap`; with `rigid` projection the delta is 1, so
template row y=0 lands exactly on the terrain's surface block and row y=1 on the first air block. `WORLD_SURFACE_WG` is the
heightmap that exists DURING world generation: the `_WG` variants (`WORLD_SURFACE_WG`, `OCEAN_FLOOR_WG`) are built while the
chunk's noise and surface are generated and are discarded afterwards, while `WORLD_SURFACE`, `MOTION_BLOCKING` and
`MOTION_BLOCKING_NO_LEAVES` are only computed once the chunk is a full `LevelChunk` (they are the ones sent to the client). A structure
start is computed before that, so asking for `MOTION_BLOCKING_NO_LEAVES` at structure time reads a heightmap that has not been
filled and the gate ends up at y=0 or in a cliff. `WORLD_SURFACE_WG` also counts trees/leaves as surface, which is the right
reading for a ruin: a gate under a canopy is placed on the canopy's height and `beard_thin` pulls the ground up to it rather than
burying the arch in a trunk. `beard_thin` is the terrain adaptation that adds terrain density BELOW the box (a "beard" that fills
the gap on slopes) without carving above it, so the ruin keeps its vines and the hill it sits on is not flattened into a box; the two
foundation rows of the template cover the seam where the beard meets the platform. `surface_structures` is the decoration step
whose pieces are placed after the surface rules and before vegetation, so the flagstones replace grass instead of being covered by it.

**`nusmp:dungeon_gates` structure set.** `random_spread`, `spacing 40, separation 16`: one attempt per 40x40-chunk cell with at least
16 chunks between neighbours, i.e. a gate roughly every 500 to 1000 blocks: rarer than a village (34/8), common enough that a
player who walks a thousand blocks finds one. `salt 2026015` seeds the per-cell random so the gate grid never coincides with
another set's grid (two sets with the same salt and spacing put their structures in the same chunks). `exclusion_zone
{minecraft:villages, 6}` skips a cell whose chunk is within 6 chunks of a village placement, so a gate never cuts through a village
or sits on its well (vanilla uses the same mechanism for pillager outposts).

**`nusmp:labyrinth_spire`.** Jigsaw, `start_pool nusmp:labyrinth/base`, `size 20` (the maximum: base + 8 chained levels + side
rooms + caps), `max_distance_from_center 128` (the vertical budget of the contract: base 16 + levels and halls under 128),
`start_height {"absolute": 40}` and NO heightmap projection (there is no terrain; the base island floats at y 40 so the fall from a
walkway is long but the void rescue has room), `terrain_adaptation none` (nothing to adapt), `use_expansion_hack false` (the hack is
the village "expand box upward" fix that breaks exact vertical stacking).
**`nusmp:labyrinth_spires` structure set.** `spacing 24, separation 12, salt 1935104`: a spire every 300 to 600 blocks so several
can exist for several parties, far enough apart that two 128-block jigsaw boxes never touch.

**Biome tags.** `has_structure/dungeon_gate` is built from explicit vanilla lists: `#minecraft:is_forest`, `is_taiga`, `is_mountain`,
`is_hill`, `is_jungle`, `is_savanna`, `is_badlands`, plus the surface biomes vanilla has no family tag for (plains, sunflower plains,
snowy plains, ice spikes, desert, swamp, mangrove swamp, meadow, cherry grove). `#minecraft:is_overworld` itself is not used because it
contains oceans, rivers, beaches and deep dark; a gate on the sea floor would be projected to the kelp line. Then the optional
entries `{"id": "#c:is_magical", "required": false}`, `#c:is_wasteland`, `is_dead`, `is_spooky`, `is_mountain`, `is_plains`, `is_forest`,
`is_desert`, `is_badlands`, `is_savanna`, `is_taiga`, `is_swamp`, `is_jungle`, `is_hill`, `is_snowy_plains`: the `c:` convention tags are
what Biomes O' Plenty, Terralith, Regions Unexplored and NeoForge's own `Tags.Biomes` fill with their biomes, so the gate spawns in
modded biomes natively with no compat code. `required: false` is the tag-loader flag that makes a missing tag a no-op instead of a
datapack load failure, which is what makes it safe to name a tag a mod that is not installed would provide.
`has_structure/labyrinth_spire` = `[nusmp:labyrinth_sky]` only (see section 3). The structure tags `nusmp:dungeon_gate` and
`nusmp:labyrinth_spire` exist for `ServerLevel.findNearestMapStructure(TagKey<Structure>, ...)` (the locate command and the
compass logic of other builders take a tag, not a structure).

## 5. The gate pieces (`tools/labyrinth/gen_gate.py`, `structure/gate/ruin_a.nbt`, `ruin_b.nbt`, `template_pool/gate/base.json`,
`processor_list/gate_weathering.json`)

**Layout shared by both.** 15x15 footprint: a 13x13 platform (x,z 1..13) with foundation rows y=0..1 (the terrain's surface row and
the one below: row y=0 replaces the surface block, see section 4) and the walkable top at y=2, surrounded at y=1 by a crumbling ring
of stone-brick stairs and slabs (the stairs' high half toward the platform) so the platform is climbed in two hops rather than a
two-block wall. About a fifth of the rim flagstones are left unset so the terrain shows through, like a ruin half swallowed by the
ground. Both are start pieces with no jigsaw: the gate is one piece. The `nusmp:labyrinth_gate` plane (builder C's portal block)
is 3 wide x 4 tall, standing on the platform in the middle of the opening.

* `gate/ruin_a` (weight 3), the Black Clover arch: 9 wide x 9 tall x 5 deep of `labyrinth_bricks` with `labyrinth_pillar` corner
  columns, corbels under the lintel, a glyph keystone, bites taken out of the crown and one pillar, cobblestone-wall stumps; the gate
  plane framed by crying obsidian and glyphs; `minecraft:chain` hanging from the lintel; `labyrinth_lantern` set into the four pillar
  faces along the way; broken pillars of four heights at the platform corners, two fallen pillar segments (`axis` x and z), moss
  blocks, moss carpet, short grass, ferns and vines on the arch's outer faces (`vine[east=true]` attaches to the pillar on its east).
* `gate/ruin_b` (weight 2), the Solo Leveling gate: a circular dais (radius 6.5) of `labyrinth_tiles` / deepslate tiles / cracked
  deepslate tiles on cobbled-deepslate footing with a glyph-and-blackstone rim and a cross under the gate; a 7 wide x 8 tall ellipse
  ring of polished blackstone with `labyrinth_pillar` at the four cardinal points and glyph studs at its widest, floating with one
  block of air above the dais (nothing holds it: that is the point); crying obsidian fills the ring round the gate plane; four
  obelisks (pillar columns on their own footing, a lantern under a glyph cap, a chain above) at the corners.

**`template_pool/gate/base.json`.** `fallback minecraft:empty`, both pieces `single_pool_element`, `projection rigid`,
`processors nusmp:gate_weathering`. Rigid, not `terrain_matching`: terrain-matching pieces are re-projected per piece onto the
heightmap and would lift the arch off its platform; the start piece is already projected by the structure.

**`processor_list/gate_weathering.json`.** Vanilla `minecraft:rule` processors so the weathering is random per placement rather than
baked into the template: cobblestone -> mossy 30%, stone bricks -> mossy 14% / cracked 13% (rules are first-match, so about a
quarter of the bricks weather), deepslate tiles/bricks and polished blackstone bricks -> cracked 25%. Our own masonry is NOT
weathered here: the palette and vein processors own it. The trailing `minecraft:block_ignore` of `minecraft:structure_void` is
belt and braces: a structure block drops structure void on SAVE (`fillFromWorld`'s ignored block), not on load, so a hand-written or
generated template that writes `structure_void` explicitly would place the block; with this processor it is skipped. The DSL simply
omits unset cells, which has the same effect.

## 6. Checks

* `python3 tools/labyrinth/test_nbt.py` (11 tests: every tag type round-trips, the byte layout, gzip, a template with jigsaw and chest).
* `python3 tools/labyrinth/template.py --check` (round-trips and validates every template).
* `python3 tools/labyrinth/gen_block_textures.py --check`, `python3 tools/labyrinth/gen_gate.py --preview`.

# Labyrinth, part B: generation (processors, palette data map, locate, command)

Package `com.newuniverse.nusmp.labyrinth.gen`. Registered by `GenRegistry.init(modBus)` from `LabyrinthSystem`. Command attached by
the lead under `/multiverse` with `root.then(LabyrinthCommand.build())`. Nothing in this part was run in game; the pure maths is
unit-tested (`PaletteSwapTest`, `VeinMathTest`), the rest is type-checked against the 1.21.1 / NeoForge 21.1 signatures.

## 1. Why processors, not pre-baked template variants

A Jigsaw piece is an NBT file. Making the Spire adapt to its biome by drawing one template per biome would mean one NBT per piece per
look (nine pool levels times side rooms times a dozen looks), a second set for every new look, and no way for a datapack to add one
without re-exporting our pieces. A `StructureProcessor` runs at PLACEMENT, once per block of a piece, with the template's block as input
and whatever it returns as output; a processor list is attached to a template-pool element in JSON (`"processors": "nusmp:..."`).
So A1/A2 build ONE set of templates in the default look (`nusmp:labyrinth_bricks/tiles/pillar`, glyphs, amethyst, dark oak) and the
same NBT yields a packed-ice spire over the tundra, a mud-brick one over the swamp, with glowing veins that thicken towards the peak,
all decided per block when the chunk generates. Vanilla's `minecraft:rule` processor cannot do either job: its output state is fixed in
the JSON (no biome lookup) and its chance is a constant (no gradient over the structure).

Two types are registered on `minecraft:worldgen/structure_processor` by a `DeferredRegister<StructureProcessorType<?>>`:
`nusmp:biome_palette` and `nusmp:magicule_veins`. `StructureProcessorType` is a functional interface whose only method returns the
processor's `MapCodec`, so the registry entry is literally `() -> CODEC`; the `processor_type` field of a processor JSON names it and
the codec reads the rest of the object. Both codecs make EVERY field optional with the contract values as defaults, so A2's
`labyrinth_interior.json` can be the bare `{"processor_type": "nusmp:biome_palette"}` / `{"processor_type": "nusmp:magicule_veins"}`
and still a datapack can re-point a tag or retune a chance. The exact JSON shapes are in the sections below (and were published early for
A2 in the session's `B_PROCESSORS.md`).

Both processors NEVER throw: every `processBlock` is wrapped and returns the incoming block on any failure. One exception inside a
processor aborts the whole piece placement in `StructureTemplate.placeInWorld`, which would leave a hole in the Spire; a missing
block id or a broken palette entry must cost at most one un-swapped block.

## 2. The palette: a NeoForge data map (`nusmp:labyrinth_palette`)

Value record `LabyrinthPalette(pillar, accent, floor, wood)`, every slot an optional block id. File:
`data/nusmp/data_maps/worldgen/biome/labyrinth_palette.json`, keys are biome ids or `#biome_tags`.

Why a data map and not a `Map<ResourceKey<Biome>, Palette>` in Java or a set of biome tags: a NeoForge data map is a registry-attached
VALUE (vanilla has nothing equivalent; tags can only say "in or out"). It is a datapack file, merged across packs, keyed by biome id OR
biome tag, with per-entry `replace` and a `remove` list; so a modpack gives its own biomes (or a wood mod's planks) a palette by dropping
one JSON into any datapack, without touching this jar, and a server admin can override ours per biome. The type is registered in
`RegisterDataMapTypesEvent` (a mod-bus event) with `DataMapType.builder(id, Registries.BIOME, LabyrinthPalette.CODEC).build()`; it is NOT
synced to clients (no `synced(...)`) because it is read only server-side: during placement and by `/multiverse labyrinth palette`.
The value is read through `Holder<Biome>#getData(GenRegistry.PALETTE)` (NeoForge's `IWithData` on holders); a direct (unregistered)
holder yields null = default look.

Entry semantics worth knowing when editing the file: entries are applied in file order and a later key overrides an earlier one for the
same biome, so the TAG entries come first and the explicit biomes after them (`minecraft:snowy_taiga` beats `#minecraft:is_taiga`,
`minecraft:bamboo_jungle` beats `#minecraft:is_jungle`, `minecraft:dark_forest` beats the absence of a forest entry). A biome with no
entry at all (plains, forest, birch forest, meadow, river...) keeps the template's blocks: that IS the default look, on purpose, so the
"home" spire is the one A1/A2 drew. A block id that does not exist fails the file at load with a logged error, so a palette for another
mod's blocks belongs in its own file under `neoforge:conditions` (`neoforge:mod_loaded`), not in ours. The `c:` biome tags NeoForge
ships (`#c:is_snowy`, `#c:is_desert`, `#c:is_swamp`, `#c:is_mushroom`...) can be used as keys too; ours sticks to vanilla
`#minecraft:is_*` tags plus explicit biomes so the file is readable without the NeoForge tag list at hand.

The entries, all real 1.21.1 block ids (there is no `pale_garden` in 1.21.1, so none here):

| key | pillar | accent | floor | wood | why |
|---|---|---|---|---|---|
| `#minecraft:is_ocean` | dark_prismarine | sea_lantern | prismarine_bricks | oak_planks | the mirrored x/z may be sea: a drowned-temple spire |
| `#minecraft:is_beach` | smooth_sandstone | prismarine | sandstone | oak_planks | shoreline: sand with sea-green inlays |
| `#minecraft:is_mountain` | polished_basalt (axis) | calcite | tuff_bricks | spruce_planks | meadow/peaks: grey stone, white seams |
| `#minecraft:is_hill` | polished_andesite | polished_tuff | tuff_bricks | spruce_planks | windswept hills: plain cold stone |
| `#minecraft:is_taiga` | polished_deepslate | mossy_cobblestone | cobbled_deepslate | spruce_planks | dark northern stone, spruce bridges |
| `#minecraft:is_jungle` | mossy_stone_bricks | moss_block | mossy_cobblestone | jungle_planks | overgrown ruin |
| `#minecraft:is_savanna` | terracotta | orange_terracotta | smooth_sandstone | acacia_planks | baked clay and acacia |
| `#minecraft:is_badlands` | chiseled_red_sandstone | terracotta | cut_red_sandstone | dark_oak_planks | red mesa family |
| `minecraft:desert` | chiseled_sandstone | cut_sandstone | smooth_sandstone | acacia_planks | the only warm vanilla wood |
| `minecraft:swamp`, `mangrove_swamp` | mud_bricks | moss_block | packed_mud | mangrove_planks | 1.19 mud family |
| `minecraft:dark_forest` | polished_blackstone | chiseled_polished_blackstone | polished_blackstone_bricks | dark_oak_planks | black masonry under the canopy |
| `minecraft:cherry_grove` | quartz_pillar (axis) | pink_terracotta | calcite | cherry_planks | white stone, pink inlays |
| `minecraft:bamboo_jungle` | bamboo_block (axis) | moss_block | bamboo_mosaic | bamboo_planks | overrides the jungle tag entry |
| `minecraft:mushroom_fields` | mushroom_stem | shroomlight | (keep tiles) | (keep dark oak) | only the two slots that read as fungal |
| `minecraft:stony_shore`, `stony_peaks` | polished_andesite | tuff / calcite | andesite / tuff_bricks | spruce_planks | bare rock |
| `minecraft:lush_caves` | mossy_cobblestone | moss_block | mossy_stone_bricks | oak_planks | can sit at y 64 under hills |
| `minecraft:dripstone_caves` | dripstone_block | copper_block | tuff | spruce_planks | same reason |
| `minecraft:deep_dark` | polished_deepslate | reinforced_deepslate | deepslate_tiles | dark_oak_planks | no sculk (it spreads and screams); the ancient-city block instead |
| snowy_plains, snowy_taiga, snowy_slopes, snowy_beach, frozen_peaks, jagged_peaks, frozen_river, frozen_ocean, deep_frozen_ocean, ice_spikes, grove | packed_ice | blue_ice | snow_block | spruce_planks | the ice spire; explicit ids because no vanilla tag means "snowy" |

Axis blocks (`polished_basalt`, `quartz_pillar`, `bamboo_block`) are chosen for the pillar slot where the theme has one, because the
template pillar `nusmp:labyrinth_pillar` has an `axis` and the processor carries it over: a horizontal beam stays horizontal.

### The swap tags (`data/nusmp/tags/block/labyrinth/`)

The processor only ever touches blocks in these four tags; everything else in a template is untouchable by design (walls, chests, doors,
jigsaws, water). Our own blocks are listed as `{"id": ..., "required": false}` so the tag still loads if a block id changes name in a
later version; the vanilla ids are plain strings.

* `swap_pillar.json` = `nusmp:labyrinth_pillar`: the columns.
* `swap_accent.json` = `nusmp:labyrinth_glyph`, `minecraft:amethyst_block`: the inlays; BOTH become the one accent block, so a palette
  with a lit accent (`sea_lantern`, `shroomlight`) keeps the glyph's role as a light and a palette with a plain one quiets the room.
* `swap_floor.json` = `nusmp:labyrinth_tiles`: the floors.
* `swap_wood.json` = dark oak planks, fence, stairs, slab, fence gate: the bridges and rails.

### Wood families: `PaletteSwap` (pure, tested)

The palette's `wood` is the family's PLANKS block. A template wood block maps by id suffix: `minecraft:dark_oak_fence` with wood
`minecraft:spruce_planks` becomes `minecraft:spruce_fence` (stem = planks id minus `_planks`, keep the namespace, append the template's
suffix; planks map to the wood block itself). Suffixes known: `_fence_gate, _pressure_plate, _trapdoor, _planks, _stairs, _button,
_fence, _slab, _door`, longest first so `_fence_gate` is never read as `_fence`. Suffix matching instead of a table of vanilla woods is
what makes modded woods work with no code (`biomesoplenty:hellbark_planks` -> `biomesoplenty:hellbark_fence`). The candidate id is then
checked against the block registry and the template block is kept when the family lacks that shape (bamboo mosaic has stairs and slabs
but no fence). A non-planks wood entry is its own stem (`minecraft:bamboo_mosaic` -> `minecraft:bamboo_mosaic_stairs`).

### Property carry-over

`BiomePaletteProcessor.carryProperties(from, to)` walks every property of the template state and, when the target block's
`StateDefinition` has a property of the same NAME, parses the serialized value by name and `trySetValue`s it: `axis`, `facing`, `half`,
`shape`, `waterlogged`, `type` (slab) all survive a material change; a property the target lacks is dropped; a value the target rejects
(a stair `shape` the other stair cannot have is not possible, but a modded property with other values is) leaves the default.
Properties are matched by name rather than by `Property` identity because the vanilla `BlockStateProperties` constants are shared only
among vanilla blocks; a modded planks block may declare its own `AXIS`-named property, and the name is the only contract.

## 3. Magicule Veins: `MagiculeVeinProcessor` + `VeinMath` (pure, tested)

Processor JSON (every field optional; shown with the defaults = the shipped `processor_list/magicule_veins.json`):
```json
{ "processor_type": "nusmp:magicule_veins", "targets": "nusmp:labyrinth/veinable",
  "vein": { "Name": "nusmp:magicule_vein" }, "base_chance": 0.01, "peak_chance": 0.12, "rise": 112, "pull": 0.6 }
```
`targets` is a block tag (no `#`), `vein` a vanilla block-state object. The shipped file OMITS `vein`: the default is resolved lazily by
id to `nusmp:magicule_vein` (A1's block), because a block-state codec default would need the block at class-load and because a missing
block must make the processor a silent no-op, never a datapack load error that kills the whole pool.

`veinable.json` = `nusmp:labyrinth_bricks`, `nusmp:labyrinth_tiles`, `minecraft:deepslate_bricks`, `minecraft:deepslate_tiles`,
`minecraft:polished_deepslate`: the dark masonry a cyan vein reads on. The test is on the block AS PLACED SO FAR, so with the palette
processor first in the list a snow-block floor is not veined and the walls (never swapped) always are.

### The breadcrumb maths

For a tagged block at `dy` blocks above the structure origin and `dxz` blocks from the spire axis:

    chance = lerp(base_chance, peak_chance, clamp(dy / rise, 0, 1)) * (1 - pull * clamp(dxz / 64, 0, 1)) + 0.002

With the defaults, on the axis: 1.2% at the base (`dy` 0), 6.7% at `dy` 56 (around the Warden hall), 12.2% at `dy` 112 and above (the
Peak and the Vault). At the rim of the base piece (`dxz` 20): 0.95% / 5.0% / 9.1%. At 64 blocks out (a far side room) the pull is fully
applied and the chance is 40% of the axis value: 0.6% / 2.8% / 5.0%. Below the origin the base chance holds; the 0.002 floor means even
the base rim shows one vein in 500 masonry blocks, so the first thing a player sees already hints at the colour of the climb. The numbers
are deliberately modest: `nusmp:magicule_vein` is light 11, and at 12% of the peak's masonry the Boss Peak glows like a nebula without
turning into a lit ceiling.

### Why the STRUCTURE origin and not the piece origin

`StructureProcessor.processBlock(level, piecePos, structureOrigin, original, placed, settings)`: the second BlockPos is where the piece's
template was placed, the THIRD is the one `StructureStart.placeInChunk` passes to every piece: `(centre x of the FIRST piece's bounding
box, its minY, centre z)`, i.e. the bottom centre of the base piece = the foot of the spire axis (the base-piece contract puts the axis
at the base's horizontal centre). Keying `dy`/`dxz` to that point is what makes the gradient ONE gradient across the whole spire: the
same `ascent` template placed on tier 2 and on tier 6 gets two densities from one NBT, and a side chamber hanging off tier 5 gets tier 5's
density, not "a fresh piece starts at zero". Keyed to the piece origin, every room would start dim at its own floor and the breadcrumb
would point nowhere.

### Why a positional hash and not the piece's RandomSource

`VeinMath.roll(seed, x, y, z, chance)` hashes (world seed, world position) with SplitMix64 and compares the uniform to the chance.
Jigsaw places a piece chunk by chunk and `placeInWorld` runs EVERY processor over the WHOLE piece for every chunk the piece intersects,
then keeps the blocks inside that chunk's box: a stream draw from `settings.getRandom(pos)` (which during Jigsaw placement is the shared
piece random) would make a block's outcome depend on how many blocks were processed before it in that run, and would also shift the
random stream of every other processor in the list. The hash gives the same answer for a block however often it is processed, keeps the
other processors' stream untouched, and makes a seed reproduce its veins. `VeinMathTest` checks monotonicity in `dy`, the fall with
`dxz`, bounds, determinism, seed sensitivity and that the observed rate matches the chance to within 0.5%.

## 4. Biome mirroring: `BiomeMirror`

The Labyrinth dimension has one biome (`nusmp:labyrinth_sky`), so "the biome at this block" would give every spire the same palette.
The dimension's `coordinate_scale` is 1.0, so the same x/z in the Overworld is the land the spire floats over, and the palette is
resolved from the OVERWORLD biome at the block's x/z, sampled at sea level (y 64, the surface band of the 1.18+ multi-noise biomes).
Outside the Labyrinth (an Overworld gate that chains `nusmp:labyrinth_palette`, a `/place` in a test world) the level's own biome is used,
so one processor serves both structures.

Thread safety is the whole design of that class. Structure processors run on worldgen WORKER threads inside a `WorldGenRegion`.
`overworld.getBiome(pos)` would call `Level.getChunk`, which from a worker thread is routed through the server thread's chunk futures: it
would load or even generate Overworld chunks just to read a biome, and when the server thread is itself blocked waiting for the Labyrinth
chunk that this very processor is building, the two wait on each other. So the mirror never touches chunks: it goes
`server.overworld().getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x >> 2, 64 >> 2, z >> 2, randomState().sampler())`,
a pure function of the seed and the climate density functions, safe from any thread and loading nothing. `MinecraftServer.overworld()`
is a plain map read. The level is reached as `ServerLevelAccessor.getLevel()` (what a `WorldGenRegion` is during worldgen and what a
`ServerLevel` is from a command), and `Labyrinth.in(level)` decides whether to mirror.

## 5. Arrival pad and locate: `LabyrinthLocate`

`ServerLevel.findNearestMapStructure(tag, near, radiusChunks, skipKnown=false)` is the engine behind `/locate structure`, with
`skipKnown=false` so an already generated spire counts. It only answers the structure's LOCATE position, a corner of its start chunk at
y 0, which nobody can stand on. So the structure START is resolved: `StructureManager.startsForStructure(chunkPos, predicate)` returns
the starts referenced by that chunk (a start chunk always references its own start, written at the STRUCTURE_REFERENCES step; the call
loads that one chunk to that status, a few milliseconds on the server thread if it was never generated), the predicate is "structure in
the tag" via `Registry.wrapAsHolder(s).is(tag)`, and the start whose `getChunkPos()` is that chunk is taken (else the nearest).

The arrival pad is then geometry over the START PIECE (`start.getPieces().get(0)`, which Jigsaw always makes the start pool's piece):
the base-piece contract puts the pad floor at local y 4 under air up to y 12, centred horizontally, so

    pad = (box.minX + box.getXSpan()/2, box.minY + 5, box.minZ + box.getZSpan()/2)

Spans instead of the literal 41 so a datapack that replaces the base template still lands players on ITS centre. `nearestGate` does the
same for the Overworld arch and answers the start piece's centre one above its bottom (the gate is projected onto the surface heightmap,
so that is ground level), falling back to the locate position lifted onto `MOTION_BLOCKING` if the start could not be resolved.
Nothing is cached: the structure manager already memoises starts per chunk and a cache here would outlive `/reload` and world switches.

The radius is 64 chunks (1024 blocks). The spire structure set spawns every 24 chunks (spacing 24, separation 12), so the nearest spire is
always well inside it; a wider radius only makes a first `enter` stall on the structure check for nothing.

## 6. `/multiverse labyrinth ...` (`LabyrinthCommand`, permission 2)

* `locate`: inside the Labyrinth, the nearest Spire's arrival pad, distance, piece count and y range; in the Overworld, the nearest Gate.
* `enter` / `leave`: C's `LabyrinthTravel.enter(player, pos)` / `leave(player)`, the same calls the gate block makes.
* `here`: which Labyrinth structure start (spire or gate) the player stands in, its piece count, start chunk and bounds: the quickest check
  that the pool chain built the whole spire (base + 4 ascents + warden + peak + vault + crown = at least 9 pieces before side rooms).
* `palette`: the biome the mirror resolves for this spot and the data-map entry it got: the way to check a datapack palette without
  regenerating a world.

Messages are literals, like `DungeonCommand`; the lang fragment for this part is therefore empty.

## 7. Every data file of this part

* `data/nusmp/worldgen/processor_list/labyrinth_palette.json`: one `nusmp:biome_palette` processor with the four contract tags spelled
  out (documentation value; the defaults are the same). For pools that want only the palette (the gate's `gate_weathering` list can
  chain it).
* `data/nusmp/worldgen/processor_list/magicule_veins.json`: one `nusmp:magicule_veins` processor with the tuned chances, `vein` omitted
  (lazy default, see section 3).
* A2's `processor_list/labyrinth_interior.json` chains both, palette FIRST: processors run per block in list order and each sees the
  previous one's output, so the swap happens before the vein test (section 3).
* `data/nusmp/tags/block/labyrinth/swap_pillar.json`, `swap_accent.json`, `swap_floor.json`, `swap_wood.json`, `veinable.json`: section 2
  and 3.
* `data/nusmp/data_maps/worldgen/biome/labyrinth_palette.json`: section 2.

## 8. Not done / to verify in game

* Nothing ran in game: a first world should be checked with `/multiverse labyrinth palette` in a few biomes and a look at the vein
  density on the base and at the peak (retune `peak_chance` in `magicule_veins.json` if the peak is too bright; no code change needed).
* `StructureStart.getPieces().get(0)` being the base piece relies on Jigsaw's start-pool-first ordering, which holds for every vanilla
  Jigsaw structure; if a future datapack adds pieces with a higher `placement_priority` than the base this still holds (priority orders
  the children, never the start piece).

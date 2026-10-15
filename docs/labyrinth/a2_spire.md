# The Labyrinth, part A2: the Spire (templates, pools, processor list, wildcard example) (1.35.0)

Builder key `a2`. Nothing here has been run in game; CI compiles the Java and runs the tests. What was checked offline (a Python
generator with a jigsaw/geometry/walkability `--check`, JSON parsing, `LabyrinthDataTest` under a JUnit shim) is listed at the end.

Everything in this document is produced by **one script**, `tools/labyrinth/gen_spire.py`, on top of A1's template DSL
(`tools/labyrinth/template.py`, `nbt.py`). Nothing was built by hand and no `.nbt` is edited directly: change the script, run it, commit
the result.

```
python3 tools/labyrinth/gen_spire.py                      # (re)write every template, pool, the processor list and the example pack
python3 tools/labyrinth/gen_spire.py --check              # regenerate in memory, compare with the files, verify every rule below
python3 tools/labyrinth/gen_spire.py --ascii labyrinth/base   # the layers of one piece (any name from section 5)
python3 tools/labyrinth/template.py --check               # A1's round-trip check of every .nbt in the mod (29 templates incl. the gate)
```

---------------------------------------------------------------------------------------------------------------------------

## 1. The room graph

The Spire is a tower on a single axis. Only the **centre column** carries vertical jigsaws; side rooms hang off the outer walls of
each level and cap themselves.

```
                                 y (blocks above the base's bottom row; the stack is 128 high)
   crown/spire_top   15x16x15    112..127   open-air pinnacle, `down` only                         pool crown
   vault/treasury    31x14x31     98..111   grand vault, 6 Relic Pedestals, 4+2 chests            pool vault
   peak/boss_peak    31x18x31     80..97    sealed by gatekeeper[warden], Peak Altar,             pool peak
                                            exit sealed by gatekeeper[boss]
   ascent_4/*        25x12x25     68..79    hall | gallery | shrine                               pool ascent_4
   ascent_3/*        25x12x25     56..67    hall | gallery | shrine                               pool ascent_3
   warden/hall       31x16x31     40..55    Floor Warden trial, Sealed Archway, ward runes        pool warden
   ascent_2/*        25x12x25     28..39    hall | gallery | shrine                               pool ascent_2
   ascent_1/*        25x12x25     16..27    hall | gallery | shrine                               pool ascent_1
   base              41x16x41      0..15    floating island, arrival pad, exit gate, 4 stairways  pool base (start_pool)

   one level, seen from above (the same on every ascent piece):

        +-----------------------------+            S = side doorway (nusmp:side_out, 3 wide x 4 tall)
        |                             |            o = the axis: floor hole + nusmp:down at the bottom row,
        S   hall: 2 doors (E, W)      S                nusmp:up at the top row, the stair beside it
        |   gallery: 3 (E, W, S)      |            side rooms attach to S and cap after one more hop:
        |   shrine: 1 (S)      o      |
        |                             |               level --S--> trial_a | trial_b | treasury_small | balcony
        +-------------S---------------+                       |
                                                              +-> corridor --S--> (side_end pool: no corridor) room | cap
                                                              +-> wild_adapter --W--> wildcard pool (foreign room | plug)
```

The vertical join is exact: the level below ends with its `nusmp:up` jigsaw in its **top row** on the axis, the level above begins with
its `nusmp:down` jigsaw in its **bottom row** on the axis, directly above (a source jigsaw places the next piece so that ITS jigsaw lands
on the cell in front of the source). The two boxes touch and never overlap, which is what Jigsaw needs.

### Why the heights are 16 / 12 / 16 / 18 / 14 / 16

`max_distance_from_center` is 128 and is measured from the structure's start as a cube (x, y and z), so the whole stack has to fit in
128 blocks above the base's bottom row. base 16 + four levels of 12 + warden 16 + peak 18 + vault 14 + crown 16 = 128. The original
brief asked for taller halls (14 / 18 / 20 / 16, 142 in all); that cannot generate (the upper rooms would be silently dropped), so
the heights above are the budget. `--check` fails if `HEIGHTS` ever adds up to more than 128.

---------------------------------------------------------------------------------------------------------------------------

## 2. The pool chain: how a Jigsaw structure "counts" levels

Jigsaw has **no per-branch depth counter**. `size` (20) is a global limit on the length of any chain of connections from the start
piece, and a pool is a bag of pieces with weights; nothing says "this is the third floor". The vanilla way to force an order is a
**different pool per step**: a piece of pool N has a jigsaw whose `pool` is N+1, so the only thing that can ever be attached above it
comes from N+1. Vanilla uses exactly this for the bastion remnant's bridges and for the ancient city's centre, and a village's streets
use it to keep a house from growing a street. Here:

```
 base --up--> ascent_1 --up--> ascent_2 --up--> warden --up--> ascent_3 --up--> ascent_4 --up--> peak --up--> vault --up--> crown
 (start_pool)  hall_1|gallery_1|shrine_1   (3 variants)  hall_2|...   warden/hall   hall_3|...    hall_4|...    boss_peak   treasury    spire_top
```

* Every level piece is generated **per level** with its `nusmp:up` jigsaw pointing at the next pool of the chain, so there are
  twelve ascent templates (3 kinds x 4 levels) instead of three that could repeat. The cost is 12 small files; the gain is that the
  tower is exactly: 2 ascent levels, the Warden, 2 ascent levels, the Peak, the Vault, the Crown, whatever the seed.
* Randomness is kept where it is wanted: inside a level the three variants are weighted 4 : 3 : 2 (halls favoured), the side rooms
  are weighted per pool, and every vertical connection is `rollable` so the next level can face any of the four directions.
* The chain is 9 deep; `size` 20 leaves 11 levels of depth for side rooms. A side branch starts at the level it hangs on, so it
  never eats the spine's depth: the spine jigsaws carry `selection_priority` and `placement_priority` 10, which makes Jigsaw
  expand the vertical connection of every piece before any side room (see section 6).

### Why rooms go up and inward

* Only the centre column has vertical jigsaws and every piece is centred on the axis (all sizes are odd, so the centre is a cell).
  The Spire therefore grows straight up, and the further up the smaller and more precious the rooms become: 41 wide at the base,
  25 on the levels, 31 for the three boss-grade halls (they are the "inward" climax, not a widening), 15 at the crown.
* `max_distance_from_center` 128 bounds the whole structure's box. A Jigsaw structure never places a piece outside it and never
  overlaps two pieces' boxes (the free-space test), so a side room that would collide with the tower, with another side room or
  with the limit is not placed and its jigsaw falls to `caps` (section 3). The tower can never be broken by a side room.
* `use_expansion_hack` is `false` in the structure file: the hack would let a piece reach out of the box.

---------------------------------------------------------------------------------------------------------------------------

## 3. Why dead ends are impossible

A doorway is a 3 x 4 opening in a wall with a jigsaw in its bottom-centre cell. If nothing is attached the player would see a hole into
the void. Every branch pool therefore sets `fallback: nusmp:labyrinth/caps`, and the **fallback is evaluated when no element of the
pool fits** (no candidate has a matching jigsaw, or every candidate's box collides, or the depth limit is reached). The caps pool
holds plugs that carry the matching jigsaw name:

| doorway (source jigsaw)       | target name   | cap the fallback can place              |
|-------------------------------|---------------|-----------------------------------------|
| `nusmp:side_out` (side)       | `nusmp:side_in`  | `caps/sealed_arch` (glyph), `caps/window` |
| `nusmp:wild_out` (wildcard)   | `nusmp:wild_in`  | `caps/wild_sealed_arch`                  |
| `nusmp:up` (the stair's top)  | `nusmp:down`     | `caps/roof_plug` (a sealed closet over the landing) |

A cap is 3 wide x 4 tall x 1 deep (the roof plug is 5 x 4 x 5), a solid brick plane whose jigsaw (final state `labyrinth_bricks`) is
its bottom-centre cell, placed flush outside the doorway: the wall reads as a sealed arch with a glyph, or as a window. Because the
cap carries its own `side_in`/`wild_in` jigsaw whose pool is `caps` and whose target does not exist in `caps`, expanding the cap
itself finds nothing and stops: caps do not chain. The `caps` pool's own fallback is `minecraft:empty`, the end of every chain.

A subtle one the generator takes care of: **a pool with zero elements is skipped by vanilla before its fallback is looked at**
(`JigsawPlacement` returns early when `pool.size() == 0`, with a warning), so an "empty wildcard pool whose fallback is caps"
would leave `wild_out` doorways open. The in-jar `wildcard.json` therefore holds ONE element, `caps/wild_sealed_arch`, which is
simply a plug; the fallback is still `caps`. A datapack that replaces `wildcard.json` replaces the whole file, so its own list
is what is used (section 8).

### Why side chambers cap after one corridor

`side/corridor` and `side/wild_adapter` are rooms of their own with a second doorway at the far end. If the corridor's far jigsaw drew
from the `side` pool again, a corridor could pick a corridor, and so on until `size` or the box ran out: long snaking corridors
around the tower instead of rooms. The corridor's far jigsaw draws from `labyrinth/side_end`, the same pool **without** the corridor,
so a branch is at most `level -> corridor -> room | cap`. (The brief said "pool side again"; the extra pool is the smallest change
that gives the bounded depth the brief describes. `side_end.json` is the only pool beyond the twelve named ones.)

---------------------------------------------------------------------------------------------------------------------------

## 4. Priorities (`selection_priority` and `placement_priority`)

* `selection_priority`: among the jigsaws of ONE piece, the ones with the higher value are **processed first**.
* `placement_priority`: among the pending pieces of the whole structure, the piece that was attached through the jigsaw with the higher
  value is **expanded first** (Jigsaw keeps a priority queue of pieces to expand).

The vertical jigsaws (`up`, `down`) have both set to 10, the side jigsaws 0. Jigsaw is a bounded search: with `size` 20 and a box,
whatever is expanded first gets the space. If side rooms were expanded first, a level's trial chamber could occupy the volume where
the next level's piece has to go, the spine would fail at that jigsaw and fall back to `roof_plug`: a tower three floors tall. With
the spine prioritised, the full 9-piece column is placed first, always, and side rooms take whatever room is left around it.

---------------------------------------------------------------------------------------------------------------------------

## 5. The templates (what each one is and why it is built that way)

Common to all: gzip NBT, DataVersion 3955, unset cells are omitted so the world keeps its block (in the void that is air).
Palette = the contract's: `nusmp:labyrinth_bricks` walls, `labyrinth_tiles` floors, `labyrinth_pillar[axis=y]` columns,
`labyrinth_glyph`, `gold_block`, `amethyst_block` accents, `labyrinth_lantern` / `sea_lantern` light, dark oak planks/fence for
bridges and rails. B's palette processor swaps the pillar / glyph / amethyst / tiles / dark-oak blocks for the biome's look; the
bricks are never swapped, so the magicule veins (which grow in bricks, tiles and deepslate) always have somewhere to grow.

### 5.1 The spine pieces (the 25 x 12 x 25 levels, the three boss-grade halls, base and crown)

All of them share one helper (`stairwell`), so the spine contract holds by construction:

* a 3 x 3 **floor hole** around the axis with the `nusmp:down` jigsaw (`down_north`, target `nusmp:up`, joint rollable) in the bottom
  centre; rows 1-3 around it stay air (the **emergence zone**, because the piece below may be rotated any of four ways and the player
  must come up out of the hole whichever way it faces);
* a **switchback stair** beside the axis: flight A climbs out in +x, a turn landing, flight B climbs back, and its last rises are
  taken inside the hole zone of the piece above;
* the `nusmp:up` jigsaw (`up_north`, target `nusmp:down`, joint rollable, final state air) in the **top row** at the centre.

Why the stair ends the way it does (found by a flood-fill test, not by looking): a full-block staircase is climbed by jumping, and a
jump needs three free cells above the block you take off from. The next piece's floor starts directly above this piece's top row, so a
take-off tread more than two below the top has a floor over its head. The last rises are therefore taken inside the 3 x 3 hole zone of
the piece above, where the head room is open, and the roof of the lower piece is cut over the steps that feed it. `--check` proves
this: it **stacks the actual templates on the axis, each rotated by its own quarter turn, and walks a player from the arrival pad to
the crown** (Gatekeeper doors counted as open), for four rotation sets, and fails if the climb is ever impossible. It also checks
that every side doorway and every chest / pedestal / altar of each piece can be reached from the stair.

| file | size | what it is |
|---|---|---|
| `labyrinth/base` | 41x16x41 | The floating island and the start piece. A deepslate/tuff **underside** (y 0-3, tapering to a point as a disc), the **arrival pad** (tiles with a glyph ring and gold cross inlays) with its floor at y=4 and the contract's air column above the centre to y=12, `nusmp:labyrinth_exit` at (centre.x+3, 5, centre.z) framed by two pillars and a glyph lintel, a parapet, a 16-pillar **colonnade** with lantern and gold capitals joined by an entablature ring at y=11, eight tall pillars around the stair, and **four stairways** (one from each side, three wide, arcaded from d=5 outward) that meet a ring road at y=12 and climb through the hole zone to the landing under the up jigsaw (`nusmp:up`, pool `nusmp:labyrinth/ascent_1`, joint rollable, final state tiles). The Java arrival logic puts the player at (bbox.minX + 20, bbox.minY + 5, bbox.minZ + 20), the pad centre. |
| `labyrinth/ascent_N/hall_N` (N=1..4) | 25x12x25 | The great hall: bricks with pilasters, four lantern-and-gold columns, a glyph and a gold ring in the floor, glyph/lantern accents in the walls, **two** side doorways (E and W). |
| `labyrinth/ascent_N/gallery_N` | 25x12x25 | A ring balcony of dark-oak planks at y=6 round a light well (four sea lanterns in the roof), railed on the inner edge, on four pillars; **three** side doorways (E, W, S). The planks and fences adapt to the biome. |
| `labyrinth/ascent_N/shrine_N` | 25x12x25 | A statue of nine pillars of three heights crowned with gold and a sea lantern, amethyst and glyph at the feet, a glyph and gold floor round it; a `nusmp:chests/labyrinth_trial` chest at its foot (two on even levels); **one** side doorway (S). |
| `labyrinth/warden/hall` | 31x16x31 | The Floor Warden's hall. See 5.2. |
| `labyrinth/peak/boss_peak` | 31x18x31 | The Boss Peak. See 5.3. |
| `labyrinth/vault/treasury` | 31x14x31 | The Treasure Vault. See 5.4. |
| `labyrinth/crown/spire_top` | 15x16x15 | The pinnacle: an open-air tiled terrace with a rail, four pillars with amethyst capitals, a ring and cross of beams carrying a gold collar and a glyph spire with a sea-lantern tip. Only a `nusmp:down` jigsaw: nothing goes above it. |

The three variants per level are the same `stairwell` plus different furniture, so a level is always the same climb: variety is in
the room, never in the way up.

### 5.2 `labyrinth/warden/hall` (31 x 16 x 31)

The arrival from the stair below ends in a small walled **vestibule** (7 x 7 outside, 5 x 5 inside, roof at y=5, a lantern in each inner
corner). Its one exit, on the west side, is a 3 x 4 doorway whose floor row is `nusmp:ward_rune` and whose keystone, in the roof cell
above it, is the **Sealed Archway** block entity. The up stair sits in the south-east of the hall in a walled **stair foot** whose
entrance (also 3 x 4) has its own ward runes. Three `nusmp:spawn_glyph` floor tiles stand in the hall, and the rest is the usual
glyph / gold floor rings, four columns with lanterns and wall accents.

Sealed Archway NBT: `{id:"nusmp:sealed_archway", radius:14, height:12, mobs:[{id:"nusmp:floor_warden",count:1},{id:"nusmp:living_armor",count:2}], reset:90}`.

How it plays, and why the runes are placed at both doorways: the archway watches a box of ±14 around the keystone. The player
walks out of the vestibule through the rune doorway (standing in a rune column does **not** start the trial: the barrier could not
rise through the player), and as soon as they have stepped into the hall the barriers rise over the runes: the vestibule door
closes behind them and the stair foot closes in front of them. The barriers fall when every mob of the trial is dead (the Floor
Warden's death also drops the Shadow Key by code). This relies on the Sealed Archway being **armed by a doorway crossing** (E1's
rule: activate only when a player is inside, nobody is in a rune column now, and some player stood in a rune column since the last
reset). `WARDEN_VESTIBULE_RUNES = True` in the script is the switch: with `False` there are no runes in the vestibule doorway and
only the stair foot is sealed.

### 5.3 `labyrinth/peak/boss_peak` (31 x 18 x 31)

The same vestibule, but its west doorway is closed by a **2 wide x 3 tall wall of `nusmp:gatekeeper_door[kind=warden,open=false]`**
(`half=lower` on the bottom row, `half=upper` on the rows above; a brick column fills the third cell of the doorway, a glyph keystone
sits above). The door opens with the Shadow Key. On top of the vestibule roof, at the **exact centre of the hall**, stands the
`nusmp:peak_altar` block entity on a gold block, ringed by four glyphs, reached by a three-wide stair on its north face. Altar NBT:
`{id:"nusmp:peak_altar", radius:16, bosses:[dante_zogratis w3, vanica_zogratis w3, zenon_zogratis w3, floor_warden w2]}`. Behind it
in the south-east is the **stair foot** to the vault, closed by a second 2 x 3 door wall of `gatekeeper_door[kind=boss]` that the altar
opens within 40 blocks when its boss dies. The hall has four amethyst-capped columns, lanterns, and glyph / gold floor rings.

### 5.4 `labyrinth/vault/treasury` (31 x 14 x 31)

The grand vault: a gold-inlaid floor, a raised **dais** (gold rim) at the west with **six `nusmp:relic_pedestal`** block entities
(`{id:"nusmp:relic_pedestal", LootTable:"nusmp:chests/labyrinth_pedestal", LootTableSeed:0L}`) round a **glyph spire** (an amethyst ring
with clusters, a glyph column, a sea-lantern tip), **four `nusmp:chests/dungeon_relic_vault` chests** in the corners under amethyst
pillars, **two `nusmp:chests/labyrinth_trial` chests** by the east wall, four lantern columns. The up jigsaw goes to `crown`.

### 5.5 The side pieces (the entrance jigsaw of each is `nusmp:side_in`, target `nusmp:side_out`, pool `caps`)

All doorways are 3 wide x 4 tall in the wall plane, with the jigsaw in the bottom centre cell, facing out, `final_state` air, joint
`aligned` (a horizontal join keeps the room upright and only allows the four rotations). A pool element's entrance pool is irrelevant
for a target jigsaw; it is `caps` so that an unmatched one simply caps.

| file | size | what it is |
|---|---|---|
| `labyrinth/side/trial_a` | 17x12x17 | Trial chamber: zombies x3 and skeletons x2 (`radius:8, height:11, reset:60`). The Sealed Archway sits in the **ceiling centre**, so the box it watches (±8, 12 tall) is exactly the chamber's footprint: the approach corridor outside is never "inside", and the doorway runes, on the box edge, are. Ward runes in the doorway floor, four `spawn_glyph` tiles, four corner pillars with lanterns, a glyph ring, one `labyrinth_trial` chest. |
| `labyrinth/side/trial_b` | 17x12x17 | Variant with `nusmp:living_armor` x2 and `nusmp:dungeon_direwolf` x1 (ids from `dungeon/DungeonEntities`), a central pillar with a glyph, a gold ring, the chest on the east wall. |
| `labyrinth/side/treasury_small` | 11x8x11 | A small treasury: two `labyrinth_trial` chests, a gold and glyph runner, a sea lantern. No trial, so the player may take it at once: a reward for finding the door. |
| `labyrinth/side/corridor` | 7x7x13 | A corridor with a `nusmp:side_out` jigsaw (pool `labyrinth/side_end`) at its far end: depth, once. |
| `labyrinth/side/wild_adapter` | 7x7x9 | A corridor ending in a `nusmp:wild_out` jigsaw (pool `labyrinth/wildcard`): the socket for other mods' rooms (section 8). |
| `labyrinth/side/balcony` | 9x7x7 | An open-air balcony (floor, rail, two lantern posts, no roof): a view of the sky. |

### 5.6 The caps

| file | size | what it is |
|---|---|---|
| `labyrinth/caps/sealed_arch` | 3x4x1 | Brick plug with a glyph and a gold keystone: reads as a sealed arch. Jigsaw `nusmp:side_in` (final state bricks). |
| `labyrinth/caps/window` | 3x4x1 | Brick plug with a two-block glass window. |
| `labyrinth/caps/wild_sealed_arch` | 3x4x1 | The plug for `wild_out` doorways (amethyst and glyph); jigsaw `nusmp:wild_in`. It is the only element of the in-jar wildcard pool. |
| `labyrinth/caps/roof_plug` | 5x4x5 | The vertical cap: a closed brick closet with a 3 x 3 floor hole and a `nusmp:down` jigsaw, placed over a stair's landing if the next level could not be placed, so the stair never leads into the sky. |

---------------------------------------------------------------------------------------------------------------------------

## 6. Why the BE NBT is rotation-safe

Jigsaw rotates a piece by changing the placed block states and **not** the numbers inside a block entity's NBT. A template that stored
"the door is at +3,0,0" would point the wrong way in a rotated piece. The three kinds of NBT here therefore hold no positions:

* **Sealed Archway** (`radius`, `height`, `mobs`, `reset`): a scalar box around the keystone, and the keystone SCANS that box for
  `nusmp:ward_rune` (a barrier column rises over each) and `nusmp:spawn_glyph` (a spawn point). The rune and glyph blocks are in the
  template as ordinary blocks, so rotation moves them with everything else. A rotation of 90 degrees maps the square box onto
  itself, so the same scalars remain right.
* **Peak Altar** (`radius`, `bosses`): scalar; the doors it opens are found by scanning for `gatekeeper_door[kind=boss]`.
* **Relic Pedestal**: a loot table id and a seed, nothing positional.
* Chests: `LootTable` + `LootTableSeed:0` (rolled on first open). The `facing` property of a chest is a block-state property, which
  Jigsaw rotates.

Mobs are not spawned by spawner blocks: the archway spawns them when the trial arms, which is why the data has `mobs` and not `SpawnData`.

---------------------------------------------------------------------------------------------------------------------------

## 7. The pools and the processor list (what each file is)

All elements are `minecraft:single_pool_element`, `projection: rigid` (the Spire floats: no terrain to conform to), `processors:
nusmp:labyrinth_interior`. Files under `data/nusmp/worldgen/template_pool/labyrinth/`:

| pool | elements (weight) | fallback | purpose |
|---|---|---|---|
| `base` | `labyrinth/base` (1) | `minecraft:empty` | The structure's `start_pool`. |
| `ascent_1` | hall_1 (4), gallery_1 (3), shrine_1 (2) | `caps` | Level 1. Its pieces' `up` jigsaw points at `ascent_2`. |
| `ascent_2` | hall_2, gallery_2, shrine_2 | `caps` | Level 2 -> `warden`. |
| `warden` | `warden/hall` | `caps` | The Floor Warden's hall -> `ascent_3`. |
| `ascent_3` | hall_3, gallery_3, shrine_3 | `caps` | Level 3 -> `ascent_4`. |
| `ascent_4` | hall_4, gallery_4, shrine_4 | `caps` | Level 4 -> `peak`. |
| `peak` | `peak/boss_peak` | `caps` | The Boss Peak -> `vault`. |
| `vault` | `vault/treasury` | `caps` | The Vault -> `crown`. |
| `crown` | `crown/spire_top` | `caps` | The top. |
| `side` | trial_a (4), trial_b (3), treasury_small (3), corridor (3), wild_adapter (2), balcony (2) | `caps` | What hangs off a level's side doorway. |
| `side_end` | the same without the corridor | `caps` | What hangs off a corridor's far end (section 3). |
| `wildcard` | `caps/wild_sealed_arch` (1) | `caps` | Where other mods' rooms snap in (section 8). |
| `caps` | sealed_arch (3), window (2), wild_sealed_arch (1), roof_plug (1) | `minecraft:empty` | The end of every chain. |

`data/nusmp/worldgen/processor_list/labyrinth_interior.json` chains `{"processor_type":"nusmp:biome_palette"}` and then
`{"processor_type":"nusmp:magicule_veins"}`, the bare forms B documented. **A processor list cannot include another list by
reference**, so the two single-processor lists B ships (`labyrinth_palette`, `magicule_veins`) cannot be composed in JSON; this file
repeats the two processors in order. Order matters: processors run per block in list order and each sees the previous one's output,
so the palette swaps first and the veins grow only in masonry that is still masonry (a snow-block floor of a snowy palette gets no
veins; the never-swapped bricks always do). Every pool element, the crown included, uses this list: the veins are densest at the crown.

---------------------------------------------------------------------------------------------------------------------------

## 8. The wildcard pool and the example datapack

`nusmp:labyrinth/wildcard` is where **other mods' and packs' rooms** snap into the Spire. Three facts make that safe:

1. **A datapack replaces one JSON.** Pool files are plain data; a pack that ships `data/nusmp/worldgen/template_pool/labyrinth/wildcard.json`
   replaces ours (last pack wins). Ours has one element, the plug, and the fallback `caps`: so a pack's file only has to list its rooms
   and keep the fallback line, and a pack that removes the plug still has no dead end because the fallback is `caps`.
2. **Jigsaw's box test keeps foreign rooms from overlapping ours.** Every piece, ours or foreign, has to fit the free space the
   structure still has; a room that collides is simply not chosen and the doorway is capped.
3. **Only the adapter's far end is a socket.** The adapter (`side/wild_adapter`) ends in a `nusmp:wild_out` jigsaw. Our rooms are never
   touched by the pack; the pack's room only has to carry a jigsaw named `nusmp:wild_in`.

`docs/labyrinth/wildcard_example/` (and the same files zipped as `docs/labyrinth/labyrinth_wildcard_example.zip`) is a working
datapack (pack_format 48):

| file | purpose |
|---|---|
| `pack.mcmeta` | pack_format 48 (1.21.1). |
| `data/nusmp/worldgen/template_pool/labyrinth/wildcard.json` | Overrides our wildcard pool: one element, `labyrinth_example:wild/chapel`, fallback `nusmp:labyrinth/caps`. |
| `data/labyrinth_example/structure/wild/chapel.nbt` | A 9 x 8 x 9 chapel: bricks, gold pillars, an amethyst altar with a cluster, a sea lantern; its doorway jigsaw is named `nusmp:wild_in`. Generated by `gen_spire.py` too. |

### How a pack author snaps YUNG's (or any other mod's) rooms in

1. Take the room, and add an **adapter template** of your own: a short corridor or a door frame whose entrance jigsaw is
   `name: nusmp:wild_in`, `target: nusmp:wild_out`, `pool: nusmp:labyrinth/caps` (rotation: the jigsaw is horizontal, joint `aligned`)
   and whose far side carries **their** jigsaw (their `name`, `target` and `pool` unchanged), so the foreign room attaches to the
   adapter exactly the way it attaches in its own structure. The adapter is the only thing that knows both vocabularies.
2. List the adapter (not the foreign room) as an element in your `wildcard.json`. The foreign room's own jigsaws then expand from
   their own pools, and anything that does not fit falls back through THEIR pools' fallbacks.
3. Keep `"fallback": "nusmp:labyrinth/caps"` in the file.
4. Keep the adapter's doorway 3 wide x 4 tall in the wall plane with the jigsaw in its bottom centre cell: that is what the Spire's
   `wild_out` doorway looks like.

---------------------------------------------------------------------------------------------------------------------------

## 9. The other files

| file | purpose |
|---|---|
| `tools/labyrinth/gen_spire.py` | The generator and checker described above. Deterministic (a seeded `random.Random` per piece name), idempotent (it rewrites a file only if its bytes change). `--check` verifies: files on disk equal the script's output; every jigsaw's `pool` names an existing pool; every pool element has a template; every `fallback` is a pool or `minecraft:empty`; every `nusmp:up` is on the top row at the centre, facing up, rollable, aimed at `nusmp:down`; every `nusmp:down` on the bottom row at the centre facing down; side jigsaws on an outer wall at floor level facing out with a fully open 3 x 4 doorway; level pieces fit 25x12x25; no two blocks share a cell (the `Piece` helper refuses a conflicting `put`); the height budget; the 3x3 hole and emergence zone; the base's arrival pad and exit gate; a **flood-fill walk** of each piece and of the stacked tower in four rotation sets. |
| `src/test/java/com/newuniverse/nusmp/labyrinth/LabyrinthDataTest.java` | A pure JUnit test (java.nio and regex; no Minecraft, no JSON library on the test classpath): every pool element has an nbt, every fallback exists, every branch pool falls back to caps, the wildcard pool is non-empty, every processor list a pool names exists, every structure's `start_pool` exists, `labyrinth_interior` names only the palette and vein types in that order, and every `nusmp:labyrinth/...` pool string inside a gzip template exists. |

## 10. Regenerating, and what is not verified

`python3 tools/labyrinth/gen_spire.py` rewrites everything; run `--check` afterwards. Changing a block id in the palette section or a
size in `HEIGHTS` is a one-line edit. Not verified in game: how the pieces look and feel; that E1's armed-by-doorway rule is
implemented as described in 5.2; that vanilla's weighted choice picks the variants as expected; that the Gatekeeper door
blocks and the Sealed Archway block entity accept the block-state and NBT forms above (they are written to the contract). The walk
test models a player as a 1 x 2 body that jumps one block and drops up to three; it is a strong check of the stairs, not a replacement
for walking them.

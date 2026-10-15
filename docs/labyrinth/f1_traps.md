# Labyrinth F1 (1.36.0): traps and puzzles (pitfall, snuffer glyph, sigil plates, riddle stone, puzzle door, three Spire side rooms)

Part of the D&D layer (`docs/labyrinth.md` is the 1.35.0 master, the owner's brief item 4b: "Provide Jigsaw logic to inject D&D-style
resource drains: hidden pitfall rooms, darkness traps that extinguish torches, and puzzle-locked doors requiring creative problem-solving
rather than just combat"). Package `com.newuniverse.nusmp.labyrinth.trap`, generator additions in `tools/labyrinth/gen_spire.py`, assets by
`tools/labyrinth/gen_trap_assets.py`, pure tests in `src/test/java/com/newuniverse/nusmp/labyrinth/trap/`. Nothing here has been run in game:
the Java was type-checked against the 1.21.1 / NeoForge 21.1 dumps (the stubs lack the shape/data-component packages, those errors are
gaps), the pure state machines were run as JUnit tests, the generator's `--check` proves the templates and runs a walk/flood over them.

## The Jigsaw logic: how a resource drain is injected into the Spire

A Jigsaw structure has no "traps" layer; it has pieces, pools and jigsaw connectors. The Spire already hangs optional side rooms off every
level piece: each level has `nusmp:side_out` jigsaws in its walls (name `nusmp:side_out`, target `nusmp:side_in`, pool `nusmp:labyrinth/side`,
fallback `nusmp:labyrinth/caps`). A D&D drain is therefore just another element of the `side` pool:

1. **A side room is a template whose entrance jigsaw is named `nusmp:side_in`** (target `nusmp:side_out`, pool `nusmp:labyrinth/caps`). The
   level's doorway draws from `side` by weight, finds a piece with a `side_in` jigsaw that faces back, and places it so the two jigsaw cells
   coincide. Nothing about the level changes: the drain is chosen by the pool, so adding a trap room is adding a pool element and nothing else.
2. **Low weights make it a surprise, not a toll.** The pool now holds trial_a 4, trial_b 3, treasury 3, corridor 3, wild adapter 2, balcony 2
   and, appended 1.36.0, pit_room 2, dark_room 2, puzzle_room 2: 23 in all, so each new room is drawn by about 9% of side doorways and the three together by about 26%. Because the weights are relative, a server
   pack that wants more traps adds weight in a datapack override of `side.json`; no code reads these numbers.
3. **Dead-end-proofing is inherited, not rebuilt.** Each room's entrance jigsaw names the `caps` pool, and the `side` pool falls back to
   `caps`: when a room cannot be placed (no space left in the structure box) the doorway gets a sealed arch instead of an open hole. The three
   rooms have no outgoing jigsaw, so they are leaves: the chain never grows a second trap behind the first.
4. **Rotation-safety by construction.** Jigsaw rotates the whole piece. Nothing in the three rooms stores a world offset: plates carry their
   colour as a block state, the pitfall's safe/unsafe flag is a block state, the Riddle Stone stores the SEQUENCE (not where anything is),
   and the stone and the snuffer find plates, doors and torches by scanning a box around themselves. The same reason the Sealed Archway scans
   for its runes (see `e1_trial.md`).
5. **One floor row may differ from its parent.** The pit room sinks its floor three blocks to make room for the pit, so its entrance
   jigsaw is on row 4 of its template, not row 1 like every older side room. Jigsaw aligns the two jigsaw cells themselves, so the room simply
   hangs three blocks lower than its parent's floor with the doorways meeting. `gen_spire.py` keeps this in one table, `ENTRY_Y`, which its
   own `--check` honours (the doorway test, the walk test).
6. **Each room drains something different.** The pit room drains health and progress (a fall, damage, a climb back); the dark room drains
   the party's light, which is the resource of the dungeon (see F2's gloom clock); the puzzle room drains time and patience and gates the
   best loot behind thinking, not fighting. Treasure in each is the existing `nusmp:chests/labyrinth_trial` table (mundane: no relics)
   and, in the puzzle alcove only, ONE `nusmp:relic_pedestal` (the pedestal rolls `nusmp:chests/labyrinth_pedestal`, so relics stay
   exclusive to vault chests, pedestals and boss drops; the puzzle room is the "creative problem-solving" source of a pedestal).

## What a player meets

* **Pit room.** A short corridor, a landing, then a field of floor tiles 11 wide and 10 deep that all look like Labyrinth tile. Twenty-four
  of its 110 tiles form a one-wide snake from the landing to the treasure alcove behind the field (the exact count depends on the seeded route); every other tile is a trap. Step on a trap and
  the floor creaks (a quarter second), then gives way: a three-block fall into a dark chamber with pointed dripstone on its floor, damage
  `dnd.pitDamage`, a ladder in the landing's wall to climb back out. Five seconds later the floor closes again (once nothing stands in the
  hole). The route is fixed per template (seeded by the piece's name), so a party that finds it can pass; a party that does not pays for it.
* **Dark room.** A square room with one dark glyph in the floor's middle and no lights of its own. Within six blocks of it every survival
  player gets Darkness (refreshed while near); the first approach snuffs every torch, lantern, campfire and candle within four blocks; and a
  torch or lantern cannot be placed within eight blocks of the glyph. A chest of rations (`labyrinth_trial`) sits five blocks from the glyph,
  inside the dark.
* **Puzzle room.** Four sigil plates (red, blue, gold, green, in a shuffled order) in a row on the floor, a Riddle Stone by the entrance, and
  a partition wall with a teal 2x3 Gatekeeper door. Right-clicking the stone prints the order to press, in colour. Right-clicking the plates
  in that order lights each and opens the door; a wrong plate resets the progress, hurts for 2 magic damage, and darkens the plates for
  `dnd.puzzleResetSeconds`. Behind the door: a chest and one Relic Pedestal.

## Java: every file and why it is built that way

| File | Why |
|---|---|
| `TrapMath.java` | All numbers and yes/no rules of the pitfall and the snuffer as pure arithmetic (who may spring a pit, what a tick does, the Darkness refresh rule, the light-refusal rule, the squared sphere). The blocks read the world and apply the answer; the rules are unit-tested without a level. Only a survival/adventure PLAYER ever springs a pit: a mob walking over the floor would empty the room before the first party arrived. |
| `PuzzleState.java` | The puzzle door's brain as a pure state machine: order, progress, the wrong-order lock, "solved is final". Time is the game tick passed in (a paused server does not count the pause against the lock). The progress lives with the Riddle Stone, never the plates, so two players share one puzzle and moving a plate changes nothing. The default riddle is a seeded permutation of the four colours (position-derived), so a template without a valid sequence still has a deterministic puzzle. |
| `PitfallTileBlock.java` | One block, two states that look the same (`safe=true` solid floor, `safe=false` trap), plus `open`. Driven by its own scheduled ticks, not a block entity: `stepOn` schedules a 5-tick crack, the tick opens it, a second tick 100 ticks later closes it. Scheduled ticks persist with the chunk and de-duplicate by position, so a hundred steps schedule one crack and nothing is stored anywhere. Damage is dealt once at the moment it opens, to those still standing on it. Inert when `dnd.traps` is off; every block change runs in `DungeonGuard.internal`. |
| `SnufferGlyphBlock.java` | The darkness half of the snuffer: a self-scheduling floor block (a once-a-second tick; random ticking is the safety net that restarts a lost schedule). Applies vanilla Darkness (10 s) only when the old one is about to fade (so the screen does not pulse), snuffs torches/lanterns (removed with their drop) and lit campfires/candles (set `lit=false`) within 4 blocks once per approach (`active` flag, reset when the room empties). No block entity, no per-player tick, no new effect: the vanilla Darkness effect is what the Warden-style screen darkening already is. |
| `TrapEvents.java` | The refusal half of the snuffer: a `BlockEvent.EntityPlaceEvent` handler (cancelable; fired when an entity places a block, not when a template places its own lanterns) that cancels a light-emitting placement within 8 blocks of a glyph. Labyrinth only, survival/adventure players only, only after the cheap tests passed, loaded chunks only. Registered with `addListener` on the game bus. |
| `SigilPlateBlock.java` | One block, `color` 0..3 and `lit`. A click, not a pressure plate: nobody solves it by accident, no mob ever does. The colour is a block state so a template writes it and rotation cannot change it; a creative player sneak-clicks to cycle the colour for building. A 2-pixel slab so the floor reads flat. |
| `RiddleStoneBlock.java` | The stone that states the riddle. A `Block` that implements `EntityBlock` (the Peak Altar's pattern) so it keeps its baked model; no ticker. |
| `RiddleStoneBlockEntity.java` | The memory: the target sequence (template nbt `{sequence:[0,2,1,3]}`, read as an int array or the generator's int list), progress, solved, lock time. Finds plates and doors by scanning a box (rotation-safe), lights plates, hurts (2 magic) and speaks, opens every closed `kind=puzzle` door in range with `GatekeeperDoorBlock.openConnected`, and uses the Rukh API for sparks. `dnd.puzzles` off: it only says the sigils are quiet. |
| `RiddleStones.java` | The plates' lookup of their stone: nearest stone in a loaded 25x9x25 box, no stored link (a link would be a rotated offset again). |
| `TrapRegistry.java` | Blocks, the riddle stone's block entity type, block items on `NUItems.ITEMS` (so `LabyrinthSystem.init` must call `init` before that register is handed to the bus), `register(gameBus)` for the one handler, and `sigilPlateStack(colour)` that builds a plate item of any colour with vanilla's `minecraft:block_state` data component (the creative tab can list four). |
| `seal/GatekeeperDoorBlock.java` (edited, additive) | `Kind` gets `PUZZLE("puzzle")` APPENDED; the Shadow Key does nothing to a puzzle door (it says the door answers only to the sigils); with `dnd.puzzles` off a click on a puzzle door opens it, so switching puzzles off can never lock a treasure away. Everything else about the door (the flood opening, unbreakable, no block entity) is unchanged. |

### Config

`DndConfig` (not edited, owned by the lead): `traps` (pitfall + snuffer on/off), `puzzles` (puzzle doors on/off), `pitDamage` (6.0),
`puzzleResetSeconds` (5). The wrong-order damage (2) and the Darkness length are constants, not config keys, to keep the config unchanged.

## Generator: the three rooms (`tools/labyrinth/gen_spire.py`, additive)

`--check` still reports 0 problems (27 templates became 30), and every older piece and pool is byte-identical except
`side.json` and `side_end.json` (the weights). New pieces: `labyrinth/side/pit_room.nbt`, `dark_room.nbt`, `puzzle_room.nbt`.

* `doorway(..., y0=1)`: a new optional parameter, default = every old call. The pit room passes `y0=4`.
* **pit_room 13x12x22.** Row 3 is the floor. Rows 1-2 under the 11x10 field are a carved chamber (air written explicitly: a template must
  carve what it replaces); everything under the corridor, landing and alcove stays brick. The floor is `nusmp:pitfall_tile` EVERYWHERE
  (safe on landing, alcove and the route): the whole floor is one block, so the Spire's biome palette (which swaps `labyrinth_tiles` for the
  local stone) cannot make the safe tiles look different from the traps. `pit_route` is a seeded one-wide snake: a sideways run continues
  the direction of the run above or goes straight down, never back over it, so two rows of the route are never beside each other.
  `pit_problems` proves in `--check`: a route of safe tiles from the doorway to the chest; at least 55% of the field is pits; every field cell
  is a pitfall tile; the field spans wall to wall (no way round); every chamber cell is air or a spike with a floor under it; every free
  chamber cell reaches the foot of the ladder (spikes sit on an even/even lattice, which leaves whole rows free: a checkerboard would
  have walled in the cells between spikes, and the check found exactly that on the first run); the ladder stands against brick with a safe tile
  at its top.
* **dark_room 15x10x15.** `shell_room` plus the glyph at the centre of the floor ringed by polished blackstone (no light), four plain
  pillars, one ration chest 5 blocks from the glyph. `dark_problems` proves: exactly one glyph and it is at the centre; no lantern, glyph
  block, sea lantern, glowstone, shroomlight or torch anywhere; every chest within the glyph's 6-block darkness.
* **puzzle_room 19x11x20.** Plates at row 6, the stone at (3,1,2) with a lantern over it, a partition at z=13 with a 2-wide 3-tall
  `kind=puzzle` door framed by two pillars, an alcove behind with one chest and one pedestal. The colours of the plates and the riddle's sequence
  are shuffled by the piece's seeded rng. `puzzle_problems` proves: the plates are one of each colour; exactly one stone with a valid sequence;
  every door is `kind=puzzle`; exactly one pedestal; **with the door treated as a wall the chest and the pedestal are unreachable** (the lock is
  real), and the plates and the stone ARE reachable from the entrance.

## Resources: every file and why

| File | What and why |
|---|---|
| `structure/labyrinth/side/pit_room.nbt`, `dark_room.nbt`, `puzzle_room.nbt` | The three templates, generated (gzip NBT, DataVersion 3955). Generated so the jigsaw geometry, the route and the colour shuffle are reproducible and `--check`ed; entrance jigsaws name `nusmp:side_in` with the `caps` pool. |
| `worldgen/template_pool/labyrinth/side.json`, `side_end.json` | The two pools that list side rooms, regenerated with the three new elements appended (weights 2/2/2, processor list `nusmp:labyrinth_interior`: the palette and the veins; the pit room's floor is a block neither touches, so it stays camouflaged). |
| `blockstates/pitfall_tile.json`, `models/block/pitfall_tile.json`, `pitfall_tile_open.json`, `models/item/pitfall_tile.json` | The tile has NO texture of its own: its model is `cube_all` of `nusmp:block/labyrinth_tiles`, the very file of the real floor, so no resource pack or model inspection tells the traps from the floor. The open model is a 3-pixel rim of the same texture round the cell's top, open in the middle. |
| `textures/block/snuffer_glyph_top.png` (+`.mcmeta`), `snuffer_glyph_side.png`, `blockstates/snuffer_glyph.json`, `models/block/snuffer_glyph.json`, `models/item/snuffer_glyph.json` | A dark floor glyph: a cold violet ring of eight notches round a hollow, ash-grey snuffed flame whose smoke curl climbs one pixel a frame (4 frames, `frametime` 10, interpolated). It gives no light, and the picture says so. `cube_bottom_top` so the animation is only on top. |
| `textures/block/sigil_plate_0..3.png`, `sigil_plate_0..3_lit.png`, `sigil_plate_side.png`, `blockstates/sigil_plate.json`, `models/block/sigil_plate_<c>[_lit].json`, `models/item/sigil_plate.json` | Four colours, each with its OWN shape (red triangle, blue disc, gold diamond, green cross-in-square) so the four also read apart for a colour-blind player; lit versions flood the shape and the ring. The model is a 14x2x14 slab (elements, not a cube) matching the block's collision shape. Eight models and eight state variants because a state-property-to-model map is the vanilla way (no client code). |
| `textures/block/riddle_stone_side.png`, `riddle_stone_top.png`, `blockstates/riddle_stone.json`, `models/block/riddle_stone.json`, `models/item/riddle_stone.json` | A dressed dark stone with a carved eye above the four sigil colours in order (the hint that the puzzle is about those four), a gold-rimmed top with the four studs round a dark core. |
| `textures/block/gatekeeper_door_puzzle_bottom.png` (+`.mcmeta`), `gatekeeper_door_puzzle_top.png`, `models/block/gatekeeper_door_puzzle_lower.json`, `_upper.json` | The third door face: the E2 door art (`tools/gen_labyrinth_seal_assets.py`'s `door_bottom` / `door_top`) in teal, so warden = violet, boss = crimson, puzzle = teal. Same `frametime` 6 animation. |
| `assets/nusmp/blockstates/gatekeeper_door.json` (edited, additive) | Four variants for `kind=puzzle` appended (closed lower/upper use the new models, open uses the shared frame). Nothing removed. |
| `loot_table/blocks/pitfall_tile.json`, `snuffer_glyph.json`, `sigil_plate.json`, `riddle_stone.json` | The same self-drop table every labyrinth block has (`survives_explosion`): the blocks are guarded in the Labyrinth, and elsewhere an admin who breaks one gets it back. |
| `tags/block/mineable/pickaxe.json` (edited, additive) | The four ids appended so a pickaxe breaks them at normal speed (and `needs_tool` is not set, so any tool works). |

## Why the pitfall is a block with scheduled ticks and not a block entity

A block entity per tile would be 110 block entities in one room, each saved with the chunk, each a ticker. A scheduled tick is a
queue entry that exists only while the tile is cracking or open; a closed tile costs nothing. The state is two booleans in the block state,
which also tells the client what to draw, with no packet.

## Why `stepOn`, not an entity-inside or a ticking block

`Block.stepOn` is called by vanilla for the block under an entity's feet while it walks there: no area scan, no per-player tick. (Whether
vanilla skips it for a sneaking player was not verified in game; the design does not depend on it.) The fall damage is applied explicitly (not left to the 3-block vanilla fall, which is under the damage threshold) so
`dnd.pitDamage` is the single knob.

## Interactions to know about

* **F2's gloom clock** (darkness, no light in hand): the dark room's Darkness is a separate vanilla-effect refresh; they stack harmlessly.
* **Trial chambers** (E1): pitfall tiles only trigger for players, so a trial's mobs never fall.
* **Relic exclusivity** (D): the puzzle alcove's pedestal is a normal `nusmp:relic_pedestal` (`labyrinth_pedestal` table); the chest is
  `labyrinth_trial` (no relics).
* **Rukh** (R1): all sparks go through `RukhFx` (dark for the pit and the glyph and a wrong order, light for the riddle and a right order);
  no vanilla particle type appears anywhere in the package.
* **The Shadow Key** is ignored by a puzzle door on purpose: a key found by fighting must not skip the thinking.

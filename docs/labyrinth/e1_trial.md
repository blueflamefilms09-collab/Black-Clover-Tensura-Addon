# Labyrinth E1: the trial chambers (Sealed Archway, Magic Barrier, Ward Rune, Spawn Glyph)

Part of the Labyrinth (1.35.0, `docs/labyrinth.md`). Package `com.newuniverse.nusmp.labyrinth.trial`, client
`client/labyrinth/trial/TrialClient.java`, assets by `tools/gen_labyrinth_trial_assets.py`, pure tests
`src/test/java/com/newuniverse/nusmp/labyrinth/trial/`. Nothing here has been run in game: the Java was type-checked against the
1.21.1 / NeoForge 21.1 API dumps, the pure rules were run as JUnit tests, CI compiles the rest.

## What a trial is

A chamber piece of the Spire holds one `nusmp:sealed_archway` keystone over its doorway, a `nusmp:ward_rune` tile in the floor of every
doorway and a few `nusmp:spawn_glyph` tiles where mobs should appear. The template writes the keystone's settings into its block entity:

```
{id:"nusmp:sealed_archway", radius:9, height:8, mobs:[{id:"minecraft:zombie", count:3}, {id:"nusmp:floor_warden", count:1}], reset:60}
```

When a survival or adventure player walks into the keystone's box (the keystone's column, `radius` to each side, `height` down, 2 up) through
a doorway (see the armed rule below) and nobody is standing in a doorway now, the archway seals every doorway with a column of `nusmp:magic_barrier`, spawns the mobs on the glyphs
(scaled by how many players are inside), plays the beacon chime and tells the party *"The archway seals. Nothing leaves until the trial
is done."* When the last of those mobs is dead the barriers fall (*"The seal fades."*) and the room is cleared for good. If the party
leaves (or dies) and nobody is inside for `reset` seconds, the trial resets: the leftover mobs are discarded, the barriers fall, and the
next party fights it fresh. A sneaking operator right-clicks the keystone to read its state.

## Why a block entity, not a command block or a spawner

* **It is saved with the chunk and survives reloads.** The state (`IDLE | ACTIVE | CLEARED`), the UUIDs it spawned, the cells it filled and
  the last tick a player was inside are NBT of the block entity; a server restart in the middle of a fight comes back ACTIVE with the
  same doors sealed and the same mobs counted. A spawner knows nothing of doors; a command block cannot be rotated with a piece, saves no
  state and needs command blocks enabled.
* **It ticks only while its chunk ticks** (`EntityBlock.getTicker`, server side only, every tenth tick, spread over the cycle by position
  with `TrialMath.dueThisTick`): a floor of twenty chambers costs nothing while nobody is on that floor.
* **It is rotation-safe because it scans instead of storing offsets.** Jigsaw rotates every piece it places. Door offsets written into the
  keystone's tag would point into a wall three times out of four, and the template tooling would have to rotate them (and get the
  rotation of the keystone's own block state right too). A `ward_rune` or `spawn_glyph` tile in the floor is rotated with the piece by the
  structure placer like every other block, and the archway simply scans its box for the two blocks (3179 block reads for the default
  box, done when a player first stands inside an idle box and at most every ten seconds after that; never while ACTIVE or CLEARED).
* **It is re-triggerable** through the reset rule, which a spawner's "once lit, forever lit" cannot express.
* **`CLEARED` is permanent and saved**, like a beaten room in the games the Labyrinth borrows from: the keystone's eye stays cracked, the
  doors stay open, nothing respawns.

## Why `DungeonGuard.internal`

The Labyrinth dimension is held by the Dungeon Guard (`LabyrinthConfig.unbreakable`, default on): the guard's mixins refuse every
`Level.setBlock` to air, `removeBlock` and `destroyBlock` in it, by whoever calls them, so that nobody digs round a sealed door. The mod's
own builders go through `DungeonGuard.internal(Runnable)`, a thread-local span the guard lets through. The archway runs every block change it
makes inside it: the barrier placement (which would pass anyway, but is wrapped so the three changes read alike), the barrier removal on
clear and reset (which would be refused without it, leaving a sealed room forever), and the keystone's `phase` state swap.

## Why `MobSpawnType.STRUCTURE` plus `setPersistenceRequired`

Mobs are made with `EntityType.create(ServerLevel, Consumer, BlockPos, MobSpawnType.STRUCTURE, false, false)`, which positions them on the
cell over the glyph and runs `finalizeSpawn` through NeoForge's `EventHooks.finalizeMobSpawn` (so the `FinalizeSpawnEvent` fires for other
mods, with a spawn type that says "a structure put me here"), then the template's optional `nbt` compound is merged over the finalized
entity (`saveWithoutId` + `merge` + `load`, with `UUID` and `Pos` stripped: a fixed UUID in a template would collide on the second
activation, and the glyph decides where the mob stands). Every mob gets the scoreboard tag `nusmp_trial` and `setPersistenceRequired()`:

* a persistent mob never despawns when the party backs off to the far end of the room or logs out mid-fight, so a trial cannot "clear" by
  attrition of its own mobs;
* persistent mobs are skipped by `NaturalSpawner`'s cap count, so a full chamber never starves the rest of the floor (or the Overworld
  chunks a player's friend is in) of natural spawns;
* `STRUCTURE` is not `NATURAL`, so nothing that cancels natural spawns (the legacy Dungeon's ecology cancels `STRUCTURE` too, but only in
  `nusmp:living_dungeon`) touches it.

## Counting the living

Every tenth tick while ACTIVE the archway looks each spawned UUID up with `ServerLevel.getEntity(UUID)`. Found and alive: counted, and
its block position remembered. Found but dead or removed: dropped from the list. Not found: if the last remembered position is in a loaded
chunk the mob gets a strike, and three strikes in a row (1.5 s) mean dead, because the entities of a freshly loaded chunk arrive a few ticks
after its blocks; if that chunk is not loaded the mob counts as alive, so a mob that wandered out of view can never clear a trial by
absence. A mob in its death animation (`LivingEntity.isAlive` is health > 0) counts as dead at once, so the doors open the moment the last
blow lands. An activation that spawns nothing (unknown entity ids, an empty list) is ACTIVE with zero alive and clears on the next check,
with a warning in the log.

## The reset rule: an abandoned trial never soft-locks the room

While ACTIVE, `lastPlayerTick` is refreshed every check that finds a survival/adventure player inside. When `now - lastPlayerTick >=
reset * 20` the trial resets: the mobs it can still reach are `discard()`ed, the barrier cells it remembers are set back to air (only the
ones that still hold a barrier: a cell something else changed is left alone), the state goes back to IDLE and the keystone's eye goes dim.
The NBT `reset` (seconds) is the template's own; absent or 0, the server config's `labyrinth.trialResetSeconds` (60, 10..600) is used;
either way it is clamped to 10..600. A party that wiped and respawned at the Spire's base walks back into an open, re-armed room instead
of a sealed one with half its mobs idle behind glass. A mob that was in an unloaded chunk at reset time is left as an ordinary persistent
mob with the `nusmp_trial` tag (the mod never discards what it cannot see).

## The doorway rule: nobody is sealed inside a wall

Before activating, the archway measures each rune's column (the air cells straight over it, at most four) and tests every inside player's
bounding box against those cells (`TrialMath.overlapsColumn`). If any touches one, the trial waits; the player is not sealed inside a
barrier and the barrier is not placed half inside a player. The check uses the hitbox, not the feet position, so a player straddling the
doorway's edge counts too. The rule is in the state machine itself (`TrialState.step`), not an afterthought in the block entity.

## The armed rule: coming through a door, not merely being inside

The Spire's geometry forces a second condition. A trial chamber's keystone sits in the centre of its ceiling with `radius` set to half the
footprint, so its box covers the whole room, and the Warden hall's arrival stair comes up through the middle of that box. "A survival
player is inside" alone would seal a party that never walked through a door. A chamber WITH Ward Runes therefore activates only when
(a) a survival/adventure player is inside, (b) nobody is standing in a rune column right now, and (c) since the last IDLE (a clear or a
reset) some player WAS seen standing in a rune column: the `armed` flag, saved in the block entity's NBT (and in the update tag),
set the moment a player's hitbox touches a column, cleared on clear and on reset. A chamber whose box holds no rune keeps the plain rule
(any player inside), so a hand-built test room with only a keystone still works. The rule is part of `TrialState.step`
(`hasRunes`, `armed`) and covered in `TrialStateTest`. Because a sprinting player crosses a one-block doorway in about three ticks, the
cheap part of the check (who is inside, is anyone in a column: this arms the trial) runs EVERY tick while IDLE and a player is inside the
box; the state step and the mob count stay on the ten-tick cycle. Idle chambers with nobody inside only compare a player list against a box.

## Creative and spectator players

They never trigger a trial, never keep one alive (the reset counter ignores them) and never count for the mob scaling: a builder checking a
chamber in creative, or an operator spectating a party, changes nothing. They do receive the "seal fades" message when inside, since it is
only information.

## Why the barrier is bedrock-hard and piston-proof

`strength(-1, 3600000)` is bedrock's: no survival tool ever cracks it and no explosion removes it (the Dungeon Guard holds creative mode
and explosions too, inside the Labyrinth). `pushReaction(BLOCK)` stops a piston, vanilla or another mod's, from shoving a column aside: a
barrier that moved would stay in the world forever, because the archway only clears the cells it remembers. `noLootTable()` because it is
never dropped (its block item is for creative building and admin repairs). `isSuffocating`/`isViewBlocking` false because the doorway rule
keeps players out of its cells but a mob caught in one must not take suffocation damage, and `isValidSpawn` false so no natural spawn
ever picks a barrier as a floor. Translucency is the model's `"render_type": "minecraft:translucent"`, the NeoForge model key, so no
client code registers a render layer. In Java it behaves like glass: full shape and full collision, `noOcclusion`, `propagatesSkylightDown`,
`getShadeBrightness` 1, an empty `getVisualShape` (the camera is never pushed by it), and `skipRendering` between two barriers so a column
reads as one sheet of light rather than four panes with seams.

## The count scaling

`TrialMath.scaledCount(base, players) = min(12, base + floor((players - 1) * base * 0.5))`: one player gets the template's count, every
extra player adds half the base, rounded down, and no single entry ever grows past twelve. A boss entry of count 1 stays single for two
players and becomes two at three. Mobs are dealt round-robin over the glyphs (or, in a chamber with none, over the first standable cell found
going down from the keystone at eight offsets two blocks out; failing that, on the keystone itself).

## The keystone's face and what the client gets

`SealedArchwayBlock` carries `phase=idle|active|cleared` as a block state property that the block entity mirrors on every transition (same
block, so the block entity survives the `setBlock`). A block state travels in the chunk packet, so the client draws the right eye and the
right light (4 idle, 10 active) with no block-entity sync and no renderer. `getUpdateTag` still sends `state` and `tracked` (how many mobs
are counted) with `ClientboundBlockEntityDataPacket` for a future renderer or HUD. Particles are vanilla `animateTick` on the blocks: sparks
off an active keystone, motes drifting on a barrier, a rare spark over a rune, violet motes rising off a glyph.

## Every file

### Java (`src/main/java/com/newuniverse/nusmp/labyrinth/trial/`, client `client/labyrinth/trial/`)
| File | What |
|---|---|
| `TrialRegistry.java` | Blocks, the mod's first `DeferredRegister<BlockEntityType<?>>`, block items on `NUItems.ITEMS`, `init(IEventBus)`, `creativeItems()`, `MOB_TAG`. |
| `SealedArchwayBlock.java` | `EntityBlock` with the `phase` property, server-only ticker, operator readout on sneak-use, active sparks. |
| `SealedArchwayBlockEntity.java` | The trial: NBT, players-in-box, marker scan, doorway rule, activation (barriers, spawns, effects), counting, clear, reset, update tag. |
| `MagicBarrierBlock.java`, `WardRuneBlock.java`, `SpawnGlyphBlock.java` | The three plain blocks (see above). |
| `TrialMath.java`, `TrialState.java` | Pure: box, clamps, scaling, column height, doorway overlap, reset timing, tick phase; the state machine. Tested. |
| `client/labyrinth/trial/TrialClient.java` | Empty on purpose (translucency is a model key, faces are blockstate variants, particles are `animateTick`). |

### Tests (`src/test/java/com/newuniverse/nusmp/labyrinth/trial/`)
`TrialMathTest` (10) and `TrialStateTest` (12): every rule and transition above, no Minecraft classes.

### Textures (`assets/nusmp/textures/block/`, 16x16, Pillow, deterministic seeds)
| File | What and why |
|---|---|
| `sealed_archway_top.png` | Dark dressed stone with a faint carved ring and a teal centre dot: the keystone's top and bottom (one texture for both, there is no "up" on a keystone). |
| `sealed_archway_eye_idle.png` / `_active.png` / `_cleared.png` | The four side faces per phase: an almond eye carved into the stone; idle a dim teal iris with a slit pupil, active a burning cyan iris with a halo and two glints, cleared a grey iris with a crack across it. Three textures rather than one tinted texture because block tint needs a client colour handler and the pupil/crack shapes differ. |
| `magic_barrier.png` + `.png.mcmeta` | 16x64, four frames (`frametime` 5): a translucent cyan sheet (fill alpha 88) with a hexagon-and-diamond lattice of period 8 (so stacked and adjacent cells tile seamlessly), bright nodes, and a diagonal shimmer that drifts one step per frame; mean alpha 127. Animated through mcmeta so no custom renderer exists. |
| `ward_rune_top.png` | A bevelled slate tile with a cyan ward glyph (bar, diamond, ticks) and a dim halo: the top of the doorway tile. |
| `ward_rune_side.png` | The same tile with a cyan groove: sides and bottom (a rune tile set into a wall still reads as one). |
| `spawn_glyph_top.png` + `.png.mcmeta` | 16x64, four frames (`frametime` 8): a darker tile with a violet summoning circle, an inscribed triangle, four cardinal dots and a centre, pulsing in brightness over the cycle. |
| `spawn_glyph_side.png` | The darker tile with a violet groove: sides and bottom. |

### Models and blockstates (`assets/nusmp/`)
| File | What and why |
|---|---|
| `blockstates/sealed_archway.json` | `phase=idle/active/cleared` to the three keystone models. |
| `models/block/sealed_archway_idle.json`, `_active.json`, `_cleared.json` | `minecraft:block/cube_bottom_top` with the eye face on all four sides and the top texture on top and bottom. |
| `blockstates/magic_barrier.json`, `models/block/magic_barrier.json` | `cube_all` with `"render_type": "minecraft:translucent"` (the NeoForge model key; the block's `noOcclusion` does the rest). |
| `blockstates/ward_rune.json`, `models/block/ward_rune.json` | `cube_bottom_top`: rune on top, groove side on sides and bottom. |
| `blockstates/spawn_glyph.json`, `models/block/spawn_glyph.json` | Same for the glyph. |
| `models/item/sealed_archway.json` (parent `block/sealed_archway_idle`), `magic_barrier.json`, `ward_rune.json`, `spawn_glyph.json` | Block items use their block model, the keystone its idle face. |

### Data (`data/nusmp/`)
| File | What and why |
|---|---|
| `loot_table/blocks/sealed_archway.json`, `ward_rune.json`, `spawn_glyph.json` | Drop themselves (`survives_explosion`, `random_sequence nusmp:blocks/<id>`): a builder outside the Labyrinth gets the block back; inside, the guard never lets them break. No loot table for the barrier (`noLootTable`). No `requiresCorrectToolForDrops`, so no mineable tag is needed; the lead may add the three to `minecraft:mineable/pickaxe` for speed. |

### Tooling
`tools/gen_labyrinth_trial_assets.py` writes all 25 asset files above; `--check` regenerates them in memory and lists any repo file that
differs (exit 1), so CI can prove the textures match the script.

## Lang (fragment `e1.lang.json`, merged by the lead)
`block.nusmp.sealed_archway` Sealed Archway, `block.nusmp.magic_barrier` Magic Barrier, `block.nusmp.ward_rune` Ward Rune,
`block.nusmp.spawn_glyph` Spawn Glyph, `message.nusmp.trial.sealed`, `message.nusmp.trial.fades`.

## Registration
`LabyrinthSystem.init` calls `com.newuniverse.nusmp.labyrinth.trial.TrialRegistry.init(modEventBus)` (blocks and the block entity
type) and `LabyrinthClient.init` calls `com.newuniverse.nusmp.client.labyrinth.trial.TrialClient.init(modEventBus)`. The creative tab
lists `TrialRegistry.creativeItems()` (`nusmp:sealed_archway`, `nusmp:magic_barrier`, `nusmp:ward_rune`, `nusmp:spawn_glyph`).

## Not verified in game
Everything. Specifically unverified: that `BlockEntityType.BlockEntitySupplier` is public under NeoForge's access transformer (the
documented `Builder.of(MyBlockEntity::new, block)` pattern; the un-transformed API dumps show it package-private); that the structure
placer calls `loadAdditional` with the template's tag (1.21.1 `loadCustomOnly`); the look of the translucent barrier against the sky
dimension's fog.

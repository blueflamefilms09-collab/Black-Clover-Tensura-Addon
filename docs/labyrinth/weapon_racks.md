# Weapon racks and the rift extras (1.107.0, lane "props")

Spec followed: the owner's dungeon design spec, section "Anti-Armor-Stand Prop Engine and BER Weapon Racks" (no armour stands as scenery; a
block-entity-renderer weapon rack; solid model blocks) and the rift items "floating, shattering reality shards" and "stencil-based or multi-pass parallax
view" of "Space-Time Fracture Rift Engine". Lang fragment: `tools/lang/weapon_racks.json`. Generator of every rack file:
`python3 -B tools/labyrinth/gen_weapon_rack.py`. Self-tests: `dungeon.props.rack`, `dungeon.props.convert`, `dungeon.props.loot`, `dungeon.props.flag`.

## NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | `labyrinth/props/`: `WeaponRackBlock`, `WeaponRackBlockEntity`, `PropsRegistry`, `PropsConfig`, `RackConverter`, the pure `RackLayout`, `RackMode`, `RackFamilies`, `RackPlacement`, `PropsSelfTests` |
| NEW | `client/labyrinth/props/`: `WeaponRackRenderer` (the block entity renderer), `PropsClient` |
| NEW | block + item `nusmp:weapon_rack` (block state with 4 facings, a 26-pixel-tall frame model, a generated 16x16 texture, recipe, drop, axe tag), six themed loot tables `data/nusmp/loot_table/racks/<family>.json` |
| NEW | rift extras: `client/labyrinth/rift/ShardMath` (pure), `RealityShards`, `ParallaxMath` (pure), `RiftParallax` |
| REPLACED (behind a flag) | the armour stands the dungeon's templates carry: with `props.rackInsteadOfArmourStand` = true (default) the generator's stands become racks; false = the stands as before |
| KEPT | every template NBT file (not one was rewritten), the armour stands they carry (they are what the flag turns back on), the generator scripts (`mega_lib.armor_stand`), `SectorPlacer.clearDecor` (it still clears stands and frames on a repair), every existing rift look (the lens, the rim, the arcs, the glow, the mega-rare core: the shards and the parallax layers are drawn in addition) |

Appended one-liners in shared files: `labyrinth/mega/MegaSystem.java` (two lines: `PropsRegistry.init` and `PropsRegistry.register`),
`NUSMP.java` (one `registerConfig` line for `nusmp-labyrinth-props-server.toml`), `client/labyrinth/LabyrinthClient.java` (one line: `PropsClient.init`).
Edited in my own lane: `DimensionalRiftRenderer` (3 additions: the parallax call, the shards call, two locals for the camera), `MegaRiftRenderer` (1 call).
Nothing in `labyrinth/mega/arch` or `labyrinth/mega/gen` was touched (there are no armour-stand entries in Java templates: they are NBT files, see below).

## How the stands become racks, and why not by editing the NBT

Of the 246 structure files, 84 hold armour stands (one of them, `mega_gateway/gateway.nbt`, is the Overworld ruin and is not a dungeon world, so it keeps its stands).
Rewriting them would (a) lose the old look the flag has to bring back, (b) collide with the clearance lane, which edits those very templates, and (c) have to be
redone for every new room. So the conversion is done where a stand is born. When the sector generator places a piece, the template's entities are created through the
world's normal entity join. `RackConverter.onJoin` (high priority) meets each armour stand and, if all of these hold, replaces it:

1. the flag is on; 2. the world is a Labyrinth dimension; 3. it is the mod's own building (the generator's `RegionContext` is set while a piece is placed, or an
`internal` span of the Dungeon Guard is open); 4. the stand is not loaded from disk (an old world keeps the stands it has); 5. it has the template's signature (the
generator writes invulnerable and no-gravity on every stand, and none is invisible: an invisible stand is a marker, not furniture). A stand a player puts down with the
item is none of these, so it is never touched.

Where the rack goes (`RackPlacement.target`): the generator puts stands either on a floor (air at the stand's feet: the rack takes that block), or on a pedestal slab (a
bottom slab at its feet: the rack replaces the slab and stands on the block under it), or on something else (the rack goes on top if the block above is air). If none
of that fits, the stand is kept: nothing is lost. The rack faces the way the stand faced (the stand's yaw, already rotated and mirrored by the template placement, as one of
four directions). The stand's equipment (main hand, off hand, head, chest, legs, feet) is hung on slots 0, 1, 2, ... in that order; a fully dressed and armed stand
loses its boots (five slots).

Modes (`RackMode`): DECOR (the converted stands: only for show, nothing can be taken, and nothing drops when it is replaced), LOOT (the loot-filled variant: themed
gear, takeable one piece at a time with an empty hand, nothing can be put back), OPEN (the rack a player crafts: take and put). `props.rackLootChance` (default 0.25)
is the share of converted racks that are LOOT; the pick is a stable hash of the position, so a repair of a sector gives the same racks. A bare stand gets themed gear
for show. In the dungeon the rack is a block like every other and is held by the Dungeon Guard (unbreakable; the player-placed exemption of the guard lane applies to a
rack a player placed there, as to any block).

Themed loot: `RackFamilies` maps all 27 region themes to six families (armoury, crypt, sacred, arcane, wild, plain); each has a vanilla-item loot table with 2 to 5 rolls
(so a rack is never overfull), worn damage, and enchantments for the sacred and arcane racks. A theme added later falls back to plain.

## The renderer (performance)

No entity exists, nothing ticks. `WeaponRackRenderer` draws the five stacks with the vanilla item renderer in the FIXED display context (the item-frame transform, so
any item already has a sensible pose), a blade-like sprite (tiered tools, bows, crossbows, tridents) rolled 45 degrees to hang upright from the rail. Level of detail by
distance (`RackLayout.visibleSlots`): all 5 slots up to 14 blocks, 3 up to 28, 2 up to 40, nothing beyond (also the renderer's view distance, so the engine culls it).
Per frame this class allocates nothing of its own: the poses are flat float arrays, the camera vector is the dispatcher's own, the block entity's render box is made once.
(The vanilla item renderer allocates inside itself; that cannot be avoided.)

## Reality shards

`RealityShards` + `ShardMath`. A shard is a four-faced sliver, lit like a crystal (face brightness from a fixed light, the rift's colour lifted towards white on bright
faces), drawn additively with the same render type as the rift's arcs (`RenderType.lightning()`, no texture). Its whole life is a closed-form function of (rift seed,
shard number, tick): it grows in over 12 % of its cycle, drifts slowly outward and up while it tumbles, from 55 % it shrinks, splits into two halves that move apart,
and fades; a cycle is 7.5 to 16 seconds and the shard is reborn somewhere else on the tear with new parameters. Nothing is stored. Budget: 14 shards (448 vertices
worst case, limit 512) up to 18 blocks, 8 up to 40, 3 up to 84, none beyond (the tear's own levels of detail). Scratch objects: the rotation is computed into local
floats, the four vertices of a piece into one static array, the state of a shard into one static array; the face normals into locals. Added to both renderers (the mega-rare
tear and the ordinary one).

## Stencil-style masked parallax: which one it is

**It is the multi-layer mask, not a stencil buffer.** The game's pipeline does not give a mod a stencil buffer it can count on: the main render target has stencil bits only
when the loader asks the driver for them, and shader packs replace the buffers. A stencil pass written against that would work on some setups and draw nothing or garbage on others.
So the mask is geometric (`ParallaxMath`, `RiftParallax`): the layers that show another sky are strips that follow the lens outline, each deeper layer narrower than the one
in front (looking into a recess you see less of it), so nothing a layer shows can pass the opening, which is what a stencil test guarantees. The parallax is the sliding of the
texture: each layer is shifted by the viewing angle in proportion to its depth, so walking round the tear moves the deep layer faster than the near one (a unit test pins
this). The mega-rare tear already had its own three-layer core with a true view-ray shift (1.99.0); it is unchanged. The ordinary tear's lens turns to face the camera, so its
horizontal shift comes from the angle the viewer is at (an honest approximation: a fixed plane would give exact parallax) and the vertical shift from the viewer's height.
Two layers near (224 vertices), one in the middle, none far.

## WHY (the spec's design decisions)

* Armour stands as scenery are banned because each is an entity: created, ticked, synced to every client in range and saved with its chunk. A hall of forty is paid for
  in server tick time and network; a rack is a block entity with a renderer, nothing ticks and nothing is sent until its contents change.
* Racks are blocks with a model (the frame) plus a renderer (the items), not a pure renderer, so the frame is in the chunk mesh (batched, culled, lit by the game) and only the
  five items are drawn per frame.
* Racks are held by the Dungeon Guard like every generated block: the spec wants the dungeon's furniture unbreakable.
* Loot variants are themed so a forge shows hammers and a crypt rusted blades: the room tells where you are.
* The flag keeps the old stands for anyone who prefers them (owner rule: keep the old behind a flag).
* The parallax is a mask of layers and not a stencil because a stencil cannot be relied on (above); the shards are closed-form so a tear costs no memory and no tick.

## What could NOT be verified (nothing here can be run in a game)

* Nothing was seen on screen. The rack frame's proportions, the item slots (positions, the 45 degree roll of blades, the scale of the shelf pieces), the shard shapes and
  brightness, and the two parallax layers are computed to the numbers above and tested as arithmetic, but never rendered. Expect to tune `RackLayout` constants,
  `ShardMath.MAX_SIZE` and the layer alpha after a look.
* That the template entity join reaches `EntityJoinLevelEvent` with the final position and yaw, and that the event can be cancelled there, is read from the engine's
  placement code, not run. The `dungeon.props.convert` self-test drives `RackConverter.convert` directly on a stand (flag, facing, contents, no room), not the event path.
  If the event path does not fire, nothing breaks: the stands stay (the old behaviour).
* `getRenderBoundingBox` is written without an override mark so a loader without that hook still builds; if the hook exists (it does in the loader the mod targets) the tall
  frame is culled by its real box.
* The loot tables use `enchant_with_levels` with `#minecraft:on_random_loot`; if a datapack validation rejects it the sacred and arcane racks log an error and are empty
  (the `dungeon.props.loot` self-test would fail and say which family).
* Sectors already generated keep their armour stands (loaded from disk); a repair or a new sector gets racks.
* Shader packs: the shards and the layers use the same render types as the existing rift (`lightning`, `entityTranslucentEmissive`) and so behave like it under them; not tried.
* Type-check ran against the API stubs (clean for the props and rift files except stub artefacts); JUnit ran in the shim: `PropsLogicTest`, `RackAssetsTest`, `ShardMathTest` (18 tests).

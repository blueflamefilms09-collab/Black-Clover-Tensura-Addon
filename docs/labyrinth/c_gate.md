# The Labyrinth, part C: the Gate, the way in and out, the guard, the sky (1.35.0)

Builder key `c`. Nothing here has been run in game; CI compiles it. What was type-checked and what was not is at the end.

This is the owner's "Total Replacement Setup" explained: how the Jigsaw Labyrinth REPLACES the legacy cave generation as the
dungeon players reach, while not one line, block, mob or dimension of the legacy Living Dungeon is deleted (the owner's rule 2: add the new, keep the old).

---------------------------------------------------------------------------------------------------------------------------

## 1. Why a NEW dimension `nusmp:labyrinth`, not a new generator for `nusmp:living_dungeon`

* **An existing world keeps its generators.** When a world is created, every dimension's `generator` (noise settings, biome
  source, structure sets) is written into `level.dat` (`WorldGenSettings`). A later edit of
  `data/nusmp/dimension/living_dungeon.json` in the mod jar changes nothing for that world: the save loads the generator it was
  born with. Editing the old dimension would therefore split the player base into "old worlds: caves", "new worlds: spire" under
  one id, with one set of blocks and one guard trying to serve both. A dimension that did not exist when the world was created
  is added on load from the datapack (`nusmp:labyrinth` is a datapack dimension, so an old world gains it the first time the
  1.35.0 jar starts), with its own generator. So: old worlds keep their caves AND gain the Spire.
* **A flat void generator is the only one that gives Jigsaw total control.** `minecraft:flat` with `layers: []` places no
  terrain at all; `structure_overrides: ["nusmp:labyrinth_spires"]` is the flat generator's own list of structure sets, so the
  Spire is the only thing the chunk generator ever puts there. A noise generator would carve caves, place ores and run every
  feature of the biome through the Spire's rooms; a `features: false, lakes: false` flat world cannot.
* **Save compatibility.** `nusmp:living_dungeon`, its blocks (`dungeon_maw`, `dungeon_gate`, `maw_root`, ...), its mobs, its
  attachments and its config stay registered exactly as before, so an old world's chunks, chests and persistent player NBT load
  without a missing-registry warning. New enum entries are only appended.
* **`labyrinth.engine` decides where the doors lead.** `nusmp-labyrinth-server.toml`, `[labyrinth] engine = "labyrinth"`
  (default) or `"legacy"`. Both dimensions exist in every world; the switch only changes the routing of Dungeon Maws, the Root
  Gates and `/multiverse dungeon enter` (section 4). A server can flip back to the caves at any time without a world reset.

## 2. Why the legacy generation is "disabled" by routing, not by deletion

Rule 2 says never delete. The legacy dimension still generates its caves if someone goes there (an admin's `/execute in
nusmp:living_dungeon run tp`), its Maws still open in the Overworld when magic is overspent (that system is untouched), but a
player who walks into a Maw is handed to `LabyrinthTravel.enterFromMaw` when the engine is the Labyrinth. The legacy code path
is the `else` branch of the same `if`, byte for byte what it was. Nothing is lost for a server that prefers the caves, and the
Maw economy (credit, closing, roots) keeps working for both.

## 3. The two gate blocks (`labyrinth/gate/GateRegistry.java`, `GateBlock.java`, `ExitGateBlock.java`)

| id | what | where it stands |
|---|---|---|
| `nusmp:labyrinth_gate` | the floating gate in the Overworld arch (A1's `nusmp:dungeon_gate` structure); walk into it | the arch template |
| `nusmp:labyrinth_exit` | the golden-white gate back; walk into it | the Spire's base piece, three blocks east of the arrival pad centre |

* **Why bedrock-hard.** `strength(-1.0F, 3600000.0F)`: a destroy time of -1 is the game's "indestructible" (`Block.INDESTRUCTIBLE`):
  survival mining never starts the crack, pickaxes of any tier do nothing; 3,600,000 explosion resistance is bedrock's own, so
  TNT, creepers, withers and the mod's blasts leave it. `pushReaction(PushReaction.BLOCK)` so a piston cannot move the gate off
  its arch and a sticky piston cannot pull it (a moved portal block would be a portal in the wrong place). `noLootTable()`: no
  drop, ever (creative players can still pick one from the tab). Why so hard: the gate is the one way into a strict-progression
  dungeon; a breakable gate is a gate that gets griefed, moved into a farm, or carried off.
* **Why no collision.** `noCollission()` so the player walks INTO the block like a nether portal; `entityInside` then fires
  (the game calls it for every entity whose bounding box overlaps a no-collision block), server side, for `ServerPlayer` only.
  Mobs and items do not travel. `getShape` returns the full cube so the block can be looked at and targeted.
* **Why noOcclusion.** The model is translucent; without `noOcclusion` the renderer would cull the faces of the blocks behind
  the gate and you would see the sky through its edges.
* **The cooldown.** Both gates (and the legacy Maws) set a game-time stamp in the player's persistent data when they move a
  player, and refuse a player whose stamp is in the future (`LabyrinthTravel.justTravelled`, 80 ticks). Needed because the way
  back sets the player down INSIDE the gate they came in through; without it they would bounce between the two gates forever.
  The stamp is written under BOTH keys, `nusmp_labyrinth_gate` (ours) and `nusmp_dungeon_gate` (the legacy Maw's), because a
  player returning from a Maw-entered Labyrinth lands inside a legacy Maw that checks the legacy key.
* **Light 11** so the arch glows at night and reads as "active" from a distance; the sound type is amethyst (a chime, not stone).
* **Particles** (`animateTick`, client only, random tick): three portal motes wind in and down into the gate, white `END_ROD` sparks,
  the portal hum once in a hundred ticks; the exit gate's motes rise (`REVERSE_PORTAL`) and it chimes.

### Assets (`tools/gen_labyrinth_gate_textures.py`, `--check` verifies them)
* `assets/nusmp/textures/block/labyrinth_gate.png` and `labyrinth_exit.png` (16 x 128, RGBA): eight 16x16 frames of a three-armed
  swirl that turns by exactly one arm per loop (the arm phase is `2*pi*f/8`, so frame 8 = frame 0 and the animation never
  jumps), opaque in the eye, translucent toward the rim (alpha down to 90) so the gate reads as a film of light in the arch, not
  a solid cube. Gate: navy core, violet arms; exit: cream core, gold arms. Six seeded sparks pulse once per loop. Only Pillow.
* `labyrinth_gate.png.mcmeta`, `labyrinth_exit.png.mcmeta`: `{"animation": {"frametime": 3, "interpolate": true}}`. Frametime 3
  = 24 ticks per loop, the pace of the mod's other gates (`descent_gate`); `interpolate` blends consecutive frames so eight
  frames read as a continuous turn (the game's texture atlas does the blending, no code).
* `blockstates/labyrinth_gate.json`, `labyrinth_exit.json`: one variant, no properties (a gate has no state to show).
* `models/block/labyrinth_gate.json`, `labyrinth_exit.json`: `minecraft:block/cube_all` with `"render_type": "minecraft:translucent"`.
  `render_type` is NeoForge's model key for the render layer (the Forge `RenderType` registration call is gone in 21.1): without it
  the alpha channel is dropped and the gate renders solid.
* `models/item/labyrinth_gate.json`, `labyrinth_exit.json`: parent = the block model, so the inventory icon is the gate cube itself
  (the owner's rule 7 is about weapons and items in hand; a block item carries its block).

## 4. The way in and out (`labyrinth/gate/LabyrinthTravel.java`)

* **`enter(p, from)`**: engine "legacy" -> `LivingDungeon.enter(p, from)` unchanged. Otherwise: the Labyrinth level is taken from
  the server (`server.getLevel(Labyrinth.DIMENSION)`, null if a datapack removed it -> "The Labyrinth does not answer: its
  dimension is not loaded."); the arrival pad is asked from builder B, `LabyrinthLocate.arrivalPad(labyrinth, new BlockPos(from.x,
  40, from.z))` (the nearest Spire to the same x/z column, generated if need be; null -> "The Labyrinth does not answer here.");
  the origin is remembered (below) unless the player is already inside; the pad is stored; cooldown; teleport; portal sound; message.
* **Why `ServerPlayer.teleportTo(ServerLevel, x, y, z, yaw, pitch)`** and not `changeDimension(DimensionTransition)`: `teleportTo` is
  the call the legacy path has used since 1.17 and is proven in this mod: it moves the player across dimensions, re-sends the
  level to the client, keeps the inventory, fires NeoForge's `EntityTravelToDimensionEvent` and sets the exact landing. 1.21's
  `changeDimension(DimensionTransition)` exists for blocks that compute their own exit (the nether portal's search, the End's
  obsidian platform) and for non-player entities; for a known landing it only wraps the same numbers in a record, so it was not
  needed. `DimensionTransition`'s signature is in the API reference should a future piece need it (an entity-carrying gate).
* **The origin NBT** (`nusmp_labyrinth_origin`: `Dim`, `X`, `Y`, `Z`, `Yaw`, `Pitch`) and the pad (`nusmp_labyrinth_pad`, a packed
  long) live in `p.getPersistentData()`: it survives a relog and a server restart, which a static map would not, and it is the
  pattern of `LivingDungeon`. A separate key from the legacy origin so a player can hold both (a legacy trip and a Labyrinth trip
  do not overwrite each other's way back).
* **`leave(p)`**: back to the stored origin dimension and spot; if the origin is lost or was the Labyrinth itself (a gate placed
  inside), to the player's respawn dimension and bed, else the Overworld spawn. Tags are forgotten, cooldown set, message.
* **`enterFromMaw(p, pos)`** is the legacy Maw's entry (the same road, kept as its own method so the Maw's call reads clearly and
  can diverge later, e.g. a Maw credit). **`leaveAny(p)`** leaves whichever dungeon the player is in (Labyrinth or legacy), used by
  the Root Gate and the Exit Gate so a legacy-era Root Gate still works for a player who is in the caves.
* **Edited lines (additive):** `dungeon/DungeonBlocks.java` `PortalBlock.entityInside` (engine check first, legacy branch kept);
  `dungeon/DungeonCommand.java` `enter` (same) and `exit` (a player in the Labyrinth is let out too; the legacy check stays).

## 5. Why the guard holds the whole dimension (`dungeon/DungeonGuard.java`, additive)

`DungeonGuard.on(level)` is also true in `nusmp:labyrinth` while `labyrinth.unbreakable` is on (and `predictsClient` likewise, with
the same try/catch for a client that has not received the synced config). The guard's mixins then refuse every `setBlock` to
air/fluid/fire and every `removeBlock`/`destroyBlock` in the dimension, plus explosions, pistons, mob griefing and tool actions.
Why the WHOLE dimension and not just the doors: the Labyrinth is strict progression (Shadow Key -> Boss Peak -> Vault). A player
who can dig one block past a sealed Gatekeeper door has skipped the Floor Warden; a player who can mine through a Magic Barrier
has skipped the trial. Protecting only our own blocks would leave the masonry around them as the weak wall.
**Consequence for the other builders:** the mod's own world writes that TAKE a block away in the Labyrinth (a Magic Barrier
falling to air, a door column that becomes air, a pedestal being emptied to air) must run inside `DungeonGuard.internal(() -> ...)`,
exactly as `LivingDungeon.findFloor` does; state changes that keep a block (door `open=false` -> `open=true`) pass without it.
Chunk generation is untouched: Jigsaw writes into `WorldGenRegion`, not `Level`, which the guard does not hook.

## 6. Why the void rescue exists (`LabyrinthTravel.rescue`, `LabyrinthEvents.onPlayerTick`)

The dimension is a void: a slip off a bridge is a 40-block fall into nothing and, in vanilla, death at y = -64 with the whole
inventory scattered into the void (unrecoverable). Falling is a design hazard of the Spire, not a punishment the mod wants to be
total, so with `labyrinth.voidRescue` (default on) every 10th tick a player under y = 1 is put back on their arrival pad with 4
magic damage (bypasses armour, so the price is the same in any gear; not `fellOutOfWorld`, which bypasses creative
invulnerability) and the line "The Gate refuses to let the fall end you." Fall distance and velocity are reset so the landing
adds nothing. If the pad is unknown, or the player fell twice within 200 ticks (the pad itself is unsafe), they are taken out of
the Labyrinth by `leave` instead, so a bad pad can never become a 4-damage loop. Spectators are left alone. The check fires well
above y = -64, before any vanilla void damage.

## 7. Death, respawn, login (`LabyrinthEvents`)

`LivingDeathEvent` for a `ServerPlayer` inside the Labyrinth clears `nusmp_labyrinth_origin`, `_pad` and `_rescued`, so the respawn
is the ordinary one (bed or world spawn) and nothing later "returns" a player who is already home. `PlayerRespawnEvent` and
`PlayerLoggedInEvent` need no handler: the origin lives in persistent data and is read only when a gate or the exit command asks.

## 8. The sky (`client/labyrinth/gate/GateClient.java`)

The dimension type names `effects: "nusmp:labyrinth"`; `RegisterDimensionSpecialEffectsEvent` (mod bus, the NeoForge hook
`client/dungeon/DungeonSky` uses) registers a `DimensionSpecialEffects` under that id. Choices: `SkyType.NORMAL` keeps sun, moon,
stars and the sunrise band (the Spire is meant to be seen against a sky); **cloud height 24**, UNDER the Spire's base at y = 40,
so the clouds are a sea beneath the island, not a ceiling through the lower tiers; `hasGround = false` because the dark disc the
Overworld draws below the horizon would read as earth under a sky island, and without it the fog colour continues all the way
down as it does in the End; `getBrightnessDependentFogColor` returns a deep indigo scaled by `brightness * 0.75 + 0.25` so night
is dark violet, never black; `isFoggyAt` false so the crown is visible from the pad. The biome's own `sky_color` / `fog_color`
(A1) do the rest; no game-bus fog events are needed.

## 9. The opt-in datapack `docs/labyrinth/vanilla_dungeons_off/` (`vanilla_dungeons_off.zip`)

Drop the zip into `world/datapacks/` (or `/datapack enable`). It switches the vanilla dungeons off so the Labyrinth is the only one:
* `pack.mcmeta`: `pack_format: 48`, the data pack format of 1.21.1 (the number the game checks before it loads the pack).
* `data/nusmp/neoforge/biome_modifier/remove_vanilla_dungeons.json`: type `neoforge:remove_features`, biomes `#minecraft:is_overworld`,
  features `minecraft:monster_room` and `minecraft:monster_room_deep` (the two placed features of the spawner dungeons), step
  `underground_structures` (the decoration step they are placed in; naming it keeps the removal from scanning the other nine steps).
  **Why a biome modifier:** monster rooms are FEATURES of the biome, not structures; vanilla gives no way to subtract a feature
  from a biome without rewriting the whole biome JSON (which would also overwrite every other mod's additions). NeoForge's biome
  modifiers are applied on top of the loaded biomes, in order, after every mod's own modifiers, so a removal composes with
  whatever else is installed. The file is under `data/<namespace>/neoforge/biome_modifier/` because that is the folder NeoForge's
  modifier registry reads.
* `data/minecraft/worldgen/structure_set/trial_chambers.json`: vanilla's own placement (random_spread, spacing 34, separation 12,
  salt 94251327) with an EMPTY `structures` list. **Why a structure_set override:** trial chambers are a structure, placed by the
  structure set that names them; a set with no structures places nothing, and overriding the set under `minecraft:` is the
  datapack way to switch a vanilla structure off (the structure itself stays registered, so `/locate` still knows the id and no
  other pack referencing it breaks). A biome modifier cannot touch structures.
* **Why opt-in** and not built into the mod: spawner dungeons are the base of every vanilla mob farm and trial chambers carry
  their own loot (heavy cores, ominous vaults); a server that wants both the Labyrinth and its farms keeps them. The mod never
  removes vanilla content by default; this pack is offered for servers that want the Labyrinth to be THE dungeon.

## 10. Config (`labyrinth/LabyrinthConfig.java`, `nusmp-labyrinth-server.toml`, `[labyrinth]`)

| key | default | what |
|---|---|---|
| `engine` | `"labyrinth"` | `"labyrinth"` or `"legacy"`: where Maws, gates and the enter command lead |
| `unbreakable` | true | the guard holds the whole Labyrinth dimension |
| `relicsExclusive` | true | relics only from the Labyrinth (D reads it) |
| `voidRescue` | true | section 6 |
| `trialResetSeconds` | 60 (10..600) | E1 reads it |
| `wardenHealth` | 200 (20..2000) | E2 reads it |
| `bossRosterEnabled` | true | E2 reads it |

Only plain `define` / `defineInRange`; `engine` is a String `define` with a Predicate validator (NeoForge falls back to the default
on a bad value instead of throwing). `LabyrinthConfig.get(v)` answers the default while the server config is not loaded yet.

## 11. Registration (the lead's lines)

* `com.newuniverse.nusmp.labyrinth.LabyrinthSystem.init(modEventBus);` **before** `NUItems.ITEMS.register(modEventBus)` (the block
  items of every Labyrinth registry go on `NUItems.ITEMS`).
* `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.LabyrinthConfig.SPEC, "nusmp-labyrinth-server.toml");`
* Client section: `com.newuniverse.nusmp.client.labyrinth.LabyrinthClient.init(modEventBus);`
* Creative tab: `nusmp:labyrinth_gate`, `nusmp:labyrinth_exit`. Lang: `block.nusmp.labyrinth_gate` "Labyrinth Gate",
  `block.nusmp.labyrinth_exit` "Labyrinth Exit Gate".

## 11a. The pure rules and their test (`labyrinth/gate/GateRules.java`, `src/test/.../labyrinth/gate/GateRulesTest.java`)

The decisions of sections 3, 4 and 6 that are made from plain values (cooldown stamps, the 10-tick cadence, "falling", "back to the
pad or out", "home or origin") live in `GateRules`, pure, in the manner of `dungeon/DungeonGuardRules`, so they run under JUnit
without the game; `LabyrinthTravel` and `LabyrinthEvents` only read the level/NBT and hand the values over. Six tests cover them
(`GateRulesTest`, run with the project's `gradlew build`).

## 12. What was checked

Type-checked against the 1.21.1 / NeoForge 21.1 API stubs: every class above and the three edited files; `GateRulesTest` compiled and
passed (6/6) against a JUnit 5 stand-in; the only errors are stub
gaps (`ServerPlayer.teleportTo(ServerLevel, ...)`, `getServer`, `MinecraftServer.getLevel`, `new BlockPos`, the `phys.shapes`,
`biome` and `levelgen.structure` packages), each reproduced by a control run on `LivingDungeon.java`, which CI compiles. A negative
control with wrong argument types was caught. The signatures of `DimensionTransition`, `ServerPlayer.teleportTo`,
`BlockBehaviour.Properties`, `TagKey.create`, `ModConfigSpec.Builder.define(String, T, Predicate)`, `DimensionSpecialEffects`,
`PlayerTickEvent.Post`, `LivingDeathEvent` were verified in the API dumps. Not run in game: the arrival pad call is builder B's
(`LabyrinthLocate.arrivalPad(ServerLevel, BlockPos)` per the contract), the exit gate's position is A2's template.

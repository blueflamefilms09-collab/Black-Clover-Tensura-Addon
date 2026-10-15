# Labyrinth 1.35.0: the seals (E2): Gatekeeper Door, Shadow Key, Floor Warden, Peak Altar

Package `com.newuniverse.nusmp.labyrinth.seal` (+ `client/labyrinth/seal`). Registered by `SealRegistry.init(modBus)` (called from
`LabyrinthSystem`), renderers by `SealClient.init(modBus)` (from `LabyrinthClient`). Nothing here was run in game; CI compiles it.

## The progression in one paragraph
A Floor Warden stands in the warden hall. Killing it drops a **Shadow Key** (always, by code). The **Gatekeeper Door** columns of kind
`warden` before the Boss Peak take that key (one is consumed) and the whole doorway opens. On the Peak the **Peak Altar** wakes when a
survival player comes near, rolls a boss from the roster written into the template, spawns it; when that boss dies the altar opens every
`gatekeeper_door[kind=boss]` within 40 blocks: the Vault. Each step is a different mechanism on purpose, explained below.

## Why the door is a blockstate machine and not a block entity
`GatekeeperDoorBlock` has three properties, `kind` (`warden|boss`), `open`, `half` (`lower|upper`), and NO block entity. Reasons:
* **Cheap and rotation-proof.** A block entity would need NBT, a ticker and, for a double door, stored offsets to its other blocks; Jigsaw
  rotates templates and offsets stored as data must be rotated with them. The door has no facing property at all: the closed face is drawn on
  all four sides, so a doorway reads the same from both approaches whatever rotation the piece got, and nothing has to be rotated.
* **The kind is data in the template.** `kind=warden` vs `kind=boss` is a block property, so the template builder (A2) decides which
  doorway is which, with no Java. A1/A2 place columns of `nusmp:gatekeeper_door[kind=warden,half=lower,open=false]` + `[half=upper]`.
* **It opens as one door by a flood** (`DoorFlood`, pure, tested): from the clicked block, 6-connected (faces only) over "closed door of the
  same kind", at most 64 positions. The halves are NOT linked to each other (no `updateShape` that removes an orphan half, unlike vanilla
  doors), so a column of 2, 3 or 4 blocks all works and `half` is only a texture selector (the upper half shows the arch-and-chain top, the
  lower the glowing seal). The cap of 64 is a safety bound: a hand-built wall of door blocks cannot turn one click into a chunk-wide write.
* **Unbreakable and immovable:** `strength(-1, 3600000)`, `pushReaction(BLOCK)`, `noOcclusion`; the open state has an EMPTY collision
  shape and a thin four-post frame as its outline shape; `isPathfindable` follows `open` so mobs path through open doors only. Closed doors
  give light 6 (the seal glows), open ones none. Opening runs inside `DungeonGuard.internal` because the Labyrinth is a guarded dimension.
* **Interaction:** `useItemOn` with a `ShadowKeyItem` on a closed `warden` door: consume one key (not in creative), flood-open, iron-door
  sound, witch + end-rod particles, "The Shadow Key turns. The Warden's seal breaks." A key on a `boss` door: "Sealed by the floor's master.
  Defeat what waits beyond the altar." Anything else falls through to `useWithoutItem`, which says what the door wants (sneaking passes the
  click through so blocks can still be placed against the door). Return values follow 1.21: `ItemInteractionResult.SUCCESS` on the client,
  `CONSUME` on the server, `PASS_TO_DEFAULT_BLOCK_INTERACTION` for non-keys. `getStateForPlacement` + `setPlacedBy` place the pair when a
  player places the block item (creative testing); templates place both halves themselves.

## Why the key is consumed, and why it is dropped by code
`ShadowKeyItem`: plain item, stack 16, Epic, foil, tooltip. One key is consumed per opening so progression is per run: a party that went up
once does not leave the gate standing open for the server forever (the door state does persist, so the same party can go back and forth).
Keys are dropped per Warden (one always, a second at 20%), so a group can farm one each.
The key is dropped from `FloorWardenEntity.dropCustomDeathLoot`, NOT from the loot table, because a guaranteed progression item must not
depend on a loot table that a datapack can edit or override, on Looting, on luck, or on a global loot modifier filtering it out. The loot
table holds only the mundane rewards.

## Why the Warden is a subclass of the Living Armor, and what is undone
`FloorWardenEntity extends LivingArmorEntity` (the Living Dungeon's tier-2 armour colony): same humanoid model, armour layer and melee AI,
and the lore holds: the Warden is the Dungeon's own armour grown huge on the Spire's magicules. `DungeonCreature` was written for the
food web and three of its rules are wrong for a boss; each is undone:
1. **Starvation.** `hunger` is a protected field; `aiStep` pins it to `HUNGER_MAX` before the parent tick (the decrement is 1 per 100 ticks
   and can never reach 0), `readAdditionalSaveData` pins it on load, and `hurt` drops `DamageTypes.STARVE` damage as a second lock.
2. **Carcass.** `DungeonCreature.die` calls `super.die` and then `CarcassBlock.leave`; Java cannot skip one level of `super`. So `die` notes
   which of the six spots `leave` may use (where it stood, above, the four sides) already hold a Carcass, lets the parent run, and removes
   any Carcass that is new, inside `DungeonGuard.internal` (the Labyrinth is guarded). `CarcassBlock.leave` itself returns early outside the
   legacy dimension, so in the Labyrinth there is nothing to remove, but the Warden can be spawned anywhere (egg, altar, `/summon`).
3. **Loot.** The parent overrides `dropFromLootTable` and `dropCustomDeathLoot` empty. `dropFromLootTable` is re-implemented the way
   `LivingEntity` does it: `getLootTable()` -> `server.reloadableRegistries().getLootTable(key)`, `LootParams` with `LootContextParamSets.ENTITY`
   (THIS_ENTITY, ORIGIN, DAMAGE_SOURCE, optional ATTACKING_ENTITY / DIRECT_ATTACKING_ENTITY, LAST_DAMAGE_PLAYER + luck when a player killed
   it), `getRandomItems(params, getLootTableSeed(), this::spawnAtLocation)`. `dropCustomDeathLoot` drops the key.
Other choices: `registerGoals` is replaced (float, melee, stroll, look, hurt-by, nearest survival player) because the parent's
`HostileToPlayers` only targets when hungry or near home. Attributes are static per type, so the supplier says MAX_HEALTH 200 and
`finalizeSpawn` sets the base value from `labyrinth.wardenHealth`. ARMOR 12, ATTACK 12, KNOCKBACK_RESISTANCE 0.8, SPEED 0.28, FOLLOW 40,
fire immune, 1.4 x 3.2, persistent (`setPersistenceRequired` + `removeWhenFarAway false`), 60 xp, full netherite set with drop chance 0
(found armour, not loot, like the parent's iron and gold). Boss bar: `ServerBossEvent`, purple, NOTCHED_6, players added in
`startSeenByPlayer` / removed in `stopSeenByPlayer` / all removed on `die` and `remove`, progress set in `aiStep`.
Abilities: **Ground Slam** every 120 ticks when the target is within 5: 8 damage + knockback to everything but armour (and creative /
spectator players) within 4 blocks, anvil crash, an explosion puff and a ring of smoke. **Shadow Step** every 160 ticks when the target is
8..24 away: teleports to a free spot beside it (behind it first, then the sides, then in front; needs a floor and `noCollision` for its box)
with smoke at both ends and the enderman sound pitched down. At <50% health, once per life, it summons two `nusmp:living_armor`
(MOB_SUMMONED, persistent, given its target). Cooldowns and the summon flag are saved.
Not a weapon: no Tensura engravings (rule 6 applies to weapons).

## Why the altar spawns on approach rather than at world-gen
`PeakAltarBlock` is an `EntityBlock` on a plain `Block` (not `BaseEntityBlock`, which forces an invisible render shape and a codec); its
`PeakAltarBlockEntity` is a three-state machine, server-ticked every 10 ticks:
* An entity written into a template is placed when the chunk generates, long before a player stands there: it would wander off the
  platform, fall into the void, be despawned, or be mid-fight when the party arrives, and it would be chosen at world-gen with no server
  config to read. Spawning when the first survival/adventure player comes within `radius` (default 16; creative and spectators do not
  wake it) means the boss is chosen at that moment, from the roster in the template's NBT, so **the roster is data**.
* **IDLE -> ACTIVE:** `BossRoster.pickOrFallback(bosses, seed, id -> EntityType.byString(id).isPresent())`: weighted, seeded by the
  altar's position + the game time, unknown ids and non-positive weights skipped, empty -> `nusmp:floor_warden`. With
  `labyrinth.bossRosterEnabled = false` the roster is skipped and the Warden is always called (C's config comment). The entity is created by
  `EntityType.byString`, `moveTo(pos.above())`, `finalizeSpawn(..., MobSpawnType.STRUCTURE, null)`, `setPersistenceRequired`, tagged
  `nusmp_peak_boss`, `addFreshEntity`; end-rod + reverse-portal particles, ender-dragon growl at 0.6 pitch (no lightning: it strikes
  the unbreakable floor and sets fire), "The floor's master answers." to players within 48.
* **ACTIVE -> DONE:** the boss is dead when its entity is `isDeadOrDying` / not alive, or when it is MISSING while the block it was last
  seen on is loaded (killed off-screen, `/kill`, cleared) after a 5-second grace from the spawn. DONE opens every closed
  `gatekeeper_door[kind=boss]` in the box +-40 x, +-12 y, +-40 z around the altar in ONE scan inside `DungeonGuard.internal`
  (`GatekeeperDoorBlock.openOne`), plays END_PORTAL_SPAWN, says "The Vault unseals." **Doors open by scan, not by stored offsets**,
  because the template is rotated by Jigsaw and the block is its own anchor; one scan of ~164k positions once per world is nothing.
  DONE is permanent (saved): a floor is cleared once per world.
* **ACTIVE -> IDLE (abandoned):** if no survival player is within `radius + 16` for 120 s, the boss is discarded and the altar goes back to
  IDLE. A persistent boss left on a platform nobody is on would sit there forever at half health, a Zogratis could walk off and keep
  hunting elsewhere, and a party that wiped gets a fresh fight (a fresh roll too) instead of a boss camped on the stairs.
* Clicking the altar says which state it is in. NBT: `radius`, `bosses:[{id,weight}]` (from the template), `state`, `boss` (UUID),
  `bossPos`, `idleSince`, `since`. An ACTIVE altar loaded without a boss id falls back to IDLE.

## Renderer
`FloorWardenRenderer extends LivingArmorRenderer`: `scale` multiplies the pose by 1.75 (so armour and shadow grow with it; shadow radius
0.875) and `getTextureLocation` returns `textures/entity/floor_warden.png`, a dark-violet re-tint of the Living Armor sheet. The renderer
has no colour hook, and a second 64x64 texture is cheaper than a tinted render type and lets an artist repaint it without Java.
Registered through `EntityRenderersEvent.RegisterRenderers` in `SealClient`.

## Every data file
* `assets/nusmp/blockstates/gatekeeper_door.json`: 8 variants `kind x open x half`; closed -> the kind+half model, open -> the one
  frame model for every kind and half (an open frame has no seal to colour).
* `assets/nusmp/models/block/gatekeeper_door_{warden,boss}_{lower,upper}.json`: `minecraft:block/cube` with the door face on all four sides
  (no facing property, see above) and `gatekeeper_door_end` up/down (what you see from the stairs above a doorway).
* `assets/nusmp/models/block/gatekeeper_door_open.json`: four 2x16x2 corner posts textured `gatekeeper_door_frame`; matches the outline
  shape in Java; nothing between the posts, so it reads as an open doorway.
* `assets/nusmp/models/block/peak_altar.json`: three stacked elements (full 0-5, inset 2 at 5-10, inset 4 at 10-16) = the stepped plinth
  the Java shape mirrors; `peak_altar_side` on the sides, `peak_altar_top` on every up face, polished blackstone below.
* `assets/nusmp/models/item/gatekeeper_door.json` (parent: the warden lower model), `peak_altar.json` (the altar model),
  `shadow_key.json` (`item/generated`, layer0 `item/shadow_key`), `floor_warden_spawn_egg.json` (`template_spawn_egg`, coloured by the
  `DeferredSpawnEggItem` 0x1A1026 / 0x8A4DE0).
* Textures (`tools/gen_labyrinth_seal_assets.py`, Pillow, deterministic, `--check` verifies sizes): `block/gatekeeper_door_warden_bottom.png`
  and `gatekeeper_door_boss_bottom.png` are 16x64 = 4 frames of a pulsing seal (violet for the Warden's gate, crimson for the Vault) with
  `.png.mcmeta` `frametime 6`; `gatekeeper_door_{warden,boss}_top.png` the riveted plate with a gold arch and a chain in the seal's colour;
  `gatekeeper_door_end.png` dark brick with gold corners; `gatekeeper_door_frame.png` plain dark stone; `peak_altar_top.png` obsidian-dark
  with a violet rune ring; `peak_altar_side.png` dark brick with a gold band; `item/shadow_key.png` a black-violet key whose bow holds a
  glowing eye; `entity/floor_warden.png` the Living Armor sheet re-tinted (alpha kept).
* `data/nusmp/loot_table/entities/floor_warden.json`: type `minecraft:entity`, two pools: emerald 2-4 and gold ingot 3-6, each with
  `minecraft:enchanted_count_increase` (+0..1 per Looting level; the 1.21 name of `looting_enchant`). The Shadow Key is NOT in the table
  (see above). Experience is the entity's `xpReward`, not loot.
* Lang fragment `$S/frag_lab/e2.lang.json` (lead merges into `en_us.json`): block/item/entity names, the tooltip, the boss bar name
  (`entity.nusmp.floor_warden.bar`), the door and altar messages (`message.nusmp.gatekeeper.*`, `message.nusmp.peak_altar.*`).

## Tests
`src/test/java/com/newuniverse/nusmp/labyrinth/seal/DoorFloodTest.java` (pack round trip, a 2x4 doorway opens whole from any block, faces
only, stops at a wall, the 64 cap, the predicate is asked once per position) and `BossRosterTest.java` (determinism, weights within 12% over
20k draws, unknown ids / zero weights skipped, empty -> fallback, position and time both change the seed). Pure JUnit 5, no Minecraft.

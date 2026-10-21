# Player-placed block exemption, the Gorgon ban and the themed mob filter (release 1.107.0, lane "guard")

The owner's spec has three requirements in this lane. The Dungeon Guard already held every block of the dungeon unbreakable, so the work is the exception to it
and the two spawn rules next to it.

1. **Player-placed exemption.** The spec: *"If players can break dungeon walls, they bypass the WFC layout. If the dimension is in Adventure Mode, players are
   permanently trapped unable to retrieve placeable utility items (like backpacks, sleeping bags, or portable crafting stations)."* A SavedData tracker logs the
   position of every block a player places; the break handlers allow breaking exactly those and cancel everything the dungeon generated.
2. **The Gorgon ban.** The spec: *"The dungeon's tactical combat loop must not be ruined by external mod cheese ... absolutely prevents game-breaking external
   entities, specifically the Gorgon from Ice and Fire (iceandfire:gorgon), from ever spawning in the Mega-Dungeon dimension."*
3. **Themed mob palettes.** The spec: *"FloorThemeMobFilter ... to strictly cancel the spawn of iceandfire:gorgon and enforce themed palettes."*

## NEW / REPLACED / KEPT

| Part | Status | What |
|---|---|---|
| `dungeon/placed/PlacedSet` (pure), `PlacedRules` (pure) | NEW | the memory of placed positions (packed longs, chunk index, per-dimension cap and per-chunk cap, oldest dropped first) and the truth tables |
| `dungeon/placed/PlacedBlocksData` | NEW | the per-dimension SavedData `nusmp_dungeon_placed` (one long array, placing order); nothing is written until the first placement |
| `dungeon/placed/PlayerPlaced` | NEW | the one public door: `isPlaced`, `mark`, `unmark`, `released`, `forgetOnBuild`, `clearChunk`, `clearBox` (the floor and chunk reset hooks) |
| `dungeon/placed/PlacedEvents`, `PlacedNet`, `PlacedMirror`, `PlacedClientBoot`, `PlacedBoot`, `PlacedConfig` | NEW | the `EntityPlaceEvent` logger (single and multi-block placements), the chunk-sent and chunk-unwatched sync, the client mirror, the config `nusmp-dungeon-placed-server.toml` |
| `DungeonGuard` | KEPT, two lines changed + one method | `refusesSet` and `refusesRemoval` let a marked position through; `refusesSet` drops the mark of a position the mod's own builder writes inside `internal`; new `afterSet` (called from the mixin) drops the mark once a write that took the block away succeeded. Break, break-speed and left-click handlers are untouched: the exemption registers through the existing `addBreakRule` |
| `mixin/LevelGuardMixin` | KEPT + one injection | a `RETURN` injection on `Level.setBlock` calls `DungeonGuard.afterSet` (optional, `require = 0`, like the others) |
| `labyrinth/mega/spawnfilter/SpawnBlacklist`, `FloorThemeMobFilter`, `SpawnFilterRules` (pure) | NEW | the list matching, the palette of a theme made from the faction weights, the one combined verdict |
| `spawnfilter/SpawnFilterEvents`, `SpawnFilterConfig`, `SpawnFilterBoot` | NEW | the three event nets and the config `nusmp-labyrinth-mega-spawnfilter-server.toml` (`spawn.blacklist`, `blacklistAllowsAdmin`, `themeFilter`, `themeMinWeight`) |
| tag `nusmp:dungeon/banned` | NEW | the datapack-extensible form of the ban (the Gorgon) |
| `iceandfire:gorgon` in `dungeon/tier3.json` and `mega/beast.json` | REPLACED (removed) | the owner's spec forbids it in the dungeon, so it leaves both tags. It is added to `dungeon/excluded.json` (the existing veto tag: no tier, no encounter pick) |
| every dungeon block the generator made | KEPT | still unbreakable by any means; the exemption only covers a position a player placed |
| the Sanctuary's own rules (`HubRules`), the arenas of the Final Hour and of the arcs | KEPT | not exempted; only the Labyrinth's two dimensions are |
| the Decennial hall and tomb-seal break rules (`allowBreakWhere`, `addBreakRule`) | KEPT | still asked first; the exemption is one more rule beside them |

## How it works

### The exemption

* **Marking.** `BlockEvent.EntityPlaceEvent` at the lowest priority, only when nothing cancelled it, only when the placer is a player (a mob, a dispenser or a spell
  is not remembered). A bed, a door or a tall plant arrives as `EntityMultiPlaceEvent`: every block it replaced is marked. A backpack block placed by hand needs no
  extra call; another system that places a block for a player calls `PlayerPlaced.mark(level, pos)`.
* **Breaking.** The guard's `onBreak`, `onBreakSpeed` and `onLeftClick` ask `breakAllowed`, which asks the registered rules; `PlayerPlaced::breakRule` is one of them.
  The block then breaks the normal way, with its normal drops: a torch, a crafting table, a sleeping bag or a backpack comes back to the player. The world's block
  writers (the mixin) let the removal through for a marked position, and once the write succeeded the mark is dropped.
* **Clients.** A client that predicts "unbreakable" never starts the crack animation, and it cannot read the server's list, so the server sends each client the marks
  of a chunk when the chunk is sent (nothing for an ordinary chunk), one small packet per placement or removal to the players tracking the chunk, and an empty list
  when a chunk leaves a player's view. The mirror is only a hint for the animation: the server decides.
* **Generated blocks.** A position that is not marked is held exactly as before. Where the mod's own builders (the sector generator, a door that opens, a pocket
  rebuild) write over a position, they run inside `DungeonGuard.internal`; a mark on such a position is dropped, so a rebuilt floor never leaves a breakable block of
  generated stone behind.
* **Floor and chunk reset.** `PlayerPlaced.clearChunk(level, cx, cz)` and `clearBox(level, a, b)` drop all marks of a chunk or a box and re-sync the trackers. Nothing
  in the mod resets a chunk today (a sector is generated once); the internal-span rule above covers every rebuild that goes through the guard, and the two calls are the
  hook for any later reset.
* **Cap and pruning.** 16384 marks per dimension and 4096 per chunk by default (`placed.cap`, `placed.perChunk`). When a cap is reached the OLDEST mark is dropped (the
  block becomes part of the dungeon's stone again), never the newest; the player who placed it gets one grey action-bar line at most every ten seconds
  (`placed.notice`). A lowered cap trims the oldest on load.
* **Switches.** `placed.exempt` (default true; false = every block unbreakable, as before) and `placed.sync` (default true).

### The ban and the palette

* **Three nets**, because creatures reach a level in different ways: `MobSpawnEvent.PositionCheck` fails a natural attempt of a banned type before it costs anything;
  `FinalizeSpawnEvent` cancels a spawn that goes the normal route (and remembers an operator's `/summon` or egg); `EntityJoinLevelEvent` is the net under both, for any
  mod that calls `addFreshEntity` directly, for the mod's own encounter director (which calls `finalizeSpawn` itself, so it never fires the spawn event), and for a
  creature loaded from an old save: a Gorgon already standing in a chunk is removed when the chunk loads. (NeoForge 1.21.1 has no `LivingSpawnEvent.CheckSpawn`; these
  are its replacements.)
* **Where.** Every dungeon dimension: the Mega-Dungeon (its rift pockets live in it), the Spire, the Sanctuary, an old Living Dungeon. Not the Overworld.
* **What is banned.** `spawn.blacklist` (default `["iceandfire:gorgon"]`; `namespace:path` exact, `namespace:*` a whole mod, a bare `path` in any namespace) plus the tag
  `nusmp:dungeon/banned`. An operator's own `/summon` or egg is refused too ("never" means never), unless `spawn.blacklistAllowsAdmin` is turned on.
* **The palette.** `FloorThemeMobFilter` takes the `themeWeights` the faction files already carry (`data/nusmp/mega/factions/*.json`, a weight per Region Theme, 0 =
  never native) and makes the palette of the floor's theme: the factions with a weight above zero and at least `spawn.themeMinWeight` (default 0). A fresh spawn of a
  creature whose faction is not in its floor's palette is refused (a drowned thing in a Necropolis). Never filtered: a creature with no faction (vanilla zombies, ...), a
  boss (`c:bosses`, `nusmp:boss`), an owned creature, a named creature, an operator's creature, a creature loaded from a save, and any floor whose palette would be
  empty (a typo must never empty a floor). `spawn.themeFilter` switches it off.

## WHY (the design decisions)

* **SavedData per dimension, not a global list.** The spec's `PlayerPlacedBlockData`: every dungeon level has its own storage and saves it with the level; a restart cannot
  turn a player's torch into stone. Packed longs instead of a `Set<BlockPos>`: eight bytes a mark, and a long array is the compact NBT.
* **A cap, oldest first.** An unbounded list is a save-file leak (a player can place millions of blocks). Dropping the oldest keeps what a person just built working and
  punishes the stale; the per-chunk cap stops one chunk from eating the budget.
* **The mark is dropped after the write, not before.** One removal passes the guard twice (the world's writer, then the chunk's); consuming the mark on the first pass
  would make the second refuse. The guard only asks, the mixin's `RETURN` consumes.
* **Break handlers are not rewritten.** The guard already has an allow-list for the Decennial halls; a player-placed block is one more rule in it, which keeps the three
  handlers (break, break speed, left click) and the client prediction consistent by construction.
* **The ban in three nets.** The spawn event alone misses everything that does not go through it (the encounter director, other mods, old saves); the join event alone
  wastes the work of a natural spawn attempt. Together they leave no door.
* **The Gorgon is not a tier creature any more.** Banning at spawn while leaving it in the tier and faction tags would make the encounter director pick it and fail over
  and over; so it is out of the tags, in the veto tag, and banned.
* **Palette from the faction weights, not a new table.** The ecology already says which faction is native where; a second table would drift from it.

## What could NOT be verified (nothing here was run in a game)

* No Gradle build and no game start were possible. Verified: the new and changed Java files type-check against the API stubs (only the known stub artefacts remain; a
  deliberate wrong call was caught as the negative control), and the unit tests below pass.
* The mixin's `RETURN` injection on `Level.setBlock` and the hooks in `DungeonGuard` have never run. If the injection fails to apply (it is `require = 0`), a broken
  player-placed block keeps its mark (harmless: the position is air), and a rebuilt position still loses it through the internal-span rule.
* That a real left click on a placed block starts the crack, takes the block with its drops and removes the mark; that the packets arrive and the mirror fills (the
  self-tests `placed`, `pickup` and `sync` drive the same code with a fake player and the codec, but not a real client).
* That `EntityPlaceEvent` is fired for every placement route in 1.21.1 (blocks, beds, doors, buckets). Fluids placed by a bucket are not held by the guard anyway.
* That an Ice and Fire Gorgon exists in the test environment: the Gorgon checks of the self-test `gorgon` are skipped with a note when the mod is not installed, and the
  join net was never exercised against the real entity.
* The themed filter inside a real sector: the self-test `theme` checks it in sector 0 when one is generated. A theme whose palette is narrow will make the encounter
  director lose spawns of a faction its tables name (it retries; the director is not changed).
* The dedicated-server start with the client-only class kept out of the common path (`PlacedClientBoot`).

## Tests

* JUnit: `dungeon/placed/PlacedSetTest`, `PlacedRulesTest`, `labyrinth/mega/spawnfilter/SpawnFilterTest` (the list parser, the palette, the verdict, the shipped data: no
  tier or faction tag lists the Gorgon, every shipped theme has a palette of at least two factions, the Necropolis keeps the aquatic out and the Sunken Temple takes it).
* Self-tests (`/multiverse mega selftest <key>`): `dungeon.guard_exempt.placed`, `.pickup`, `.persist`, `.sync`, `.cap`, `.gorgon`, `.blacklist`, `.theme`.

## Language

`tools/lang/guard_exempt.json` (one key, the prune notice, also given as the fallback text in the code).

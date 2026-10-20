# Pocket fix (release 1.101.0): "the hidden dungeons are one room, or there is no switch"

Owner report after playing 1.99.0 / 1.100.0: *"FIX THE HIDDEN DUNGEONS. THERE IS ONLY ONE ROOM OR THERE IS NO SWITCH."* The hidden dungeons are the pockets
behind the Dimensional Rifts: (a) the instanced hidden realms of `labyrinth/mega/rift/PocketBuilder` + `PocketRealm` (Antechamber, corridor, Hall, barred
door, Vault; boss or puzzle kind; `docs/labyrinth/rifts.md`, `rifts_fix.md`) and (b) the 13 exclusive tombs of 1.99.0 (`labyrinth/mega/rift/pocket/**`;
`docs/labyrinth/rift_pockets.md`). Nobody has played either in a game; this was found by reading the code and by running the generators and the plans in a
harness (below). Nothing here could be run in the real game: see "What could not be verified".

## What was actually wrong (line numbers are those of the code at 1.100.0, commit 51fdb09)

### 1. The root cause: the Dungeon Guard refused every doorway, every opened door and every sweep, and none of the pocket code was inside its internal span

`DungeonGuard` (`dungeon/DungeonGuard.java`) holds every block of the Labyrinth dimensions (the Mega-Dungeon is one) unbreakable. Besides
the break events it has a net under the world's block writers: `mixin/LevelGuardMixin.java:22-25` (`Level.setBlock`) and `mixin/LevelChunkGuardMixin.java:19-22`
(`LevelChunk.setBlockState`) ask `DungeonGuard.refusesSet` (`DungeonGuard.java:119`), which answers "refused" for any change that turns a block that is not air,
fluid or fire into air, a fluid or fire, **unless the thread is inside `DungeonGuard.internal`/`begin()..end()`**. Every other builder of the mod (the arena builders, the
Gatekeeper door, the traps) runs inside that span. **None of the pocket code did**: `grep DungeonGuard labyrinth/mega/rift` found nothing. The consequences, each a
silent `setBlock` that returns false:

| Where (1.100.0) | What it was meant to do | What happened in the Mega-Dungeon |
|---|---|---|
| `PocketBuilder.java:73-75` (`set(level, cx + dx, y + dy, cz + dz, AIR)`) | carve the doorways Antechamber -> corridor -> Hall in the walls just built | refused: **the Antechamber is a closed box, the player stands in ONE ROOM**; the Hall, the Vault and the rest are real but unreachable |
| `PocketBuilder.java:150` (`openVault`, `level.setBlock(b, AIR, 3)`) | lift the 9 iron bars when the boss dies / the plates are solved | refused: the message "the bars lift, the vault is open" is shown and **the bars stay: NO SWITCH opens the vault** |
| `PocketBuilder.java:58`, `PocketRealm.java:392` (`clear`) | give the cell back when the realm collapses | refused: the walls stayed; the next realm in that slot was built on the ruins |
| `PocketPlan.java:120-122` + `PocketBuild.java:153` (`placeOps`, the doorway fills are the last ops) | carve the doorway of every door of a tomb | refused: **every tomb is ONE ROOM (the entry hall); the other 13 to 20 rooms are built and unreachable** |
| `ExclusivePockets.java:493` (`openSeal`, `mega.setBlock(b, AIR, 2 | 16 | 32)`) | remove the crying-obsidian seal when it is answered (magic, Sigil, force) | refused: **whatever answered the seal, it never opened (NO SWITCH)** |
| `PocketClear.java:96` | sweep the cell when a tomb is over | refused, yet the job reported "finished": the tomb stayed standing and its slot was reused over it |
| `PocketCollapse.java:186` | bring rooms down in the collapse, wall up doorways | refused: the collapse did nothing to the blocks |
| `RiftService.java:441` (`collapse`, the rift block to air) | remove an expired rift | refused: in the Mega-Dungeon an expired rift's block stayed, and its block entity (`DimensionalRiftBlockEntity.tick`: `--left <= 0` -> `collapse`) fired the puff and the amethyst sound **every tick for ever** |

The paper reproduction is in the harness (below): with the guard's rule applied to the plan's own fills, **both realm kinds and all 13 tombs, over many seeds, reach
exactly one room from the arrival**. Applying the fills inside the internal span reaches every room.

### 2. The seal's "force" answer never reached its handler

The guard's `onLeftClick` and `onBreakSpeed` (`DungeonGuard.java:153`, `:160`) run at `HIGHEST` and cancel any hit at a held block; `ExclusiveEvents.onLeftClick`
(`:124`) and `.onBreakSpeed` (`:141`) are ordinary listeners that do not receive cancelled events. The seal is crying obsidian, a held block: the strike and the held blow
(the "free" force answer) were cancelled before the tomb's handler saw them. The only registered exception, `DungeonGuard.allowBreakWhere`, is a single slot owned by the
Decennial halls.

### 3. The seal had one layer, not two

`PocketPlan.java:253-254` laid the two seal layers (60 blocks, as the docs and the tests say) and `PocketPlan.java:122` carved the doorways after them; the doorway of
the lair's wall is exactly the first layer's box, so it overwrote it with air (in a world without the guard) and the seal was one layer thick.

### 4. A delver could arrive in a tomb that was not built yet

`ExclusivePockets.join` (`:120`) took the party in the moment the entry hall stood; the doorways are the last fills (about 15 to 40 ticks later, longer if the chunks of the
cell must be generated first). A delver who arrived at once stood in a hall with no door.

### 5. There was no switch to see

* Boss realm: the only "switch" was the boss's death; no lever, no sign, nothing to read. Puzzle realm: four 2-pixel buttons on coloured blocks, no sign, no light.
* Tomb: the treasury's seal answered to magic, the Sigil (dropped by the keeper) or force; no lever, no sign at the door (only a chat line when a delver stood within 6 blocks).
* Nothing opened a door by itself, so a boss that could not be beaten or a puzzle that could not be read meant waiting for the collapse.

### 6. A fill that throws stalled a tomb for ever (defensive)

`PocketBuild.placeOps` did not advance on an exception; `ExclusivePockets.fast` catches per pocket and logs, so the same op threw again every tick and the tomb stayed in
phase 0 with only the entry hall. Nothing seen in the code throws there; it is cheap insurance (the op is skipped and logged once; the watchdog puts the gap right).

### 7. Existing self-tests already asserted these things

`rift.e2e`, `rift.pocket.boss`, `rift.pocket.puzzle`, `rift.pockets` and `rift.mega_rare` assert that bars become air, that the cell is empty after a sweep and that the lair is
reachable from the arrival. In a world with the guard on (its default) they cannot pass against the 1.100.0 code. I cannot tell from here why that was not seen.

## What was changed

* **Guard**: every block writer of the pocket code now runs inside `DungeonGuard.begin()/end()` (`PocketBuilder.build/clear/openVault/repair`, `PocketBuild.entry/step`,
  `PocketClear.step`, `PocketCollapse.step`, `ExclusivePockets.openSeal`, `RiftService.collapse`). The tests' own scenery cleanup does too. A JUnit source scan
  (`PocketFixTest.everyFileThatSetsAirInTheRiftCodeRunsInsideTheGuardsInternalSpan`) keeps it that way.
* **One-line hook in a shared file** (`dungeon/DungeonGuard.java`): `addBreakRule(BiPredicate)` adds a rule to the allow-list without replacing the Decennial halls' (the old
  single `allowBreakWhere` slot is kept); `breakAllowed` asks both. `ExclusiveEvents` registers `sealBreakable` (server: exactly the seal blocks of a live tomb; client: any
  crying obsidian of the pocket field, to let the crack start). `ExclusiveEvents.onBreak` now cancels the break of a seal block even in creative.
* **The realm is five rooms** (`RealmPlan`, new): Antechamber, corridor, Hall, Vault (the same blocks as before) and a **lever chamber** in the Hall's north wall (two walls thick
  doorway). The geometry is data (`RealmPlan.ops`), `PocketBuilder` only places it; the old coordinates (entry, exit, bars, chests, plates) are unchanged, so every old self-test line
  still holds. The Vault gets a second exit rift.
* **The switch, in the realm**: a real vanilla **lever** on the far wall of the lever chamber between two sea lanterns set in the wall, a glowing waxed **sign** over it, a **lantern** on
  the floor each side; a sign over the bars in the Hall and a sign at the chamber's doorway; in the puzzle realm a lantern beside every coloured plate and a sign beside the order on the
  east wall. `PocketSwitch` handles the right click on the server (cancels vanilla's toggle, so it cannot desync; works for any player, every party member, in multiplayer).
* **The switch, in the tombs**: a **seal lever** on a doorless wall of the **key room** (so it is never behind the seal; the layout check already proves the key room is reachable
  without crossing it), two lit lanterns in the wall and two on the floor, a sign over it; **a chest in the key room holding the tomb's Sigil** (with a sign); **two signs on the lair's wall
  beside the seal** ("Magic, Sigil, force or the KEY ROOM LEVER"); **a second exit rift inside the treasury**.
* **When the lever works** (`PocketSwitchRules`, pure): at once when the door's own challenge is done (tomb: the master is dead; realm: it opens by the boss/plates already), otherwise `switchUnlockSeconds`
  (default 120) after the pocket formed; before that it is stiff and says how long (sound + action bar). The door **opens by itself** after `switchAutoSeconds` (default 420, held to three
  fifths of the pocket's life, never sooner than 30 s after the lever works) while somebody is inside. Cue: a wisp over the lever every second (white = works, smoke = stiff), a line to
  everybody inside when it first gives ("A lever clicks somewhere in the realm ..."), another when it is pulled. The lever clicks down with the door, whatever opened it.
* **Arrival is gated**: `ExclusivePockets.join` holds the party at the tear while the tomb forms (a line with the percent, then once a second), and takes them in the tick the build is
  finished. A build that has not finished in 90 s is restarted. (The realm builds in one call before anybody is taken in and is then read back with `verify`; it refuses to open a realm
  that is not whole.)
* **Watchdog**: `PocketWatchdog.missing` (tomb) and `PocketBuilder.verify` (realm) read the finished pocket back (middle of every doorway, floor of every room, a sample of the seal, the lever,
  the Sigil's chest, a sign, the exits, the bars) every five seconds while somebody is inside and before anybody is taken into an existing realm; a gap starts a repair pass (`PocketBuild`
  with `repair`: the plan again, leaving an opened seal and a taken relic alone, features only where missing; the realm: `PocketBuilder.repair`). At most six passes a tomb; each is logged.
* **The seal** is laid after the doorways, so both layers stand (60 blocks).
* **Commands** (append-only under `/multiverse mega rift`, permission 2): `rooms [slot]` lists every room with its place and doors, the switch, and what the watchdog finds missing;
  `switch [slot]` says where the lever is and its state; `switch open [slot]` pulls it as an admin (no delay); `switch tp [slot]` stands you in front of it (the way home is remembered);
  `switch repair [slot]` runs the repair. `RiftAdmin.switchPos` / `RiftAdmin.openSwitch` expose the same to `/dungeon admin`.
* **Config** (`[rift]`): `switchUnlockSeconds` (120), `switchAutoSeconds` (420). **Lang**: `tools/lang/pocket_fix.json`.

## NEW / REPLACED / KEPT

| | Part |
|---|---|
| NEW | `RealmPlan` (realm blueprint), the lever chamber and release lever, `PocketSwitch`, `PocketSwitchRules`, `PocketSigns`, `PocketSignText`, the tombs' seal lever + Sigil chest + signs + treasury exit (`PocketPlan.Type` SWITCH, SIGN, LANTERN, SIGIL_CHEST, EXIT_SHORTCUT appended), `PocketWatchdog`, the arrival queue, `rift switch|rooms`, `RiftAdmin.switchPos/openSwitch`, `DungeonGuard.addBreakRule`, config keys, self-test `rift.pocket_fix`, JUnit `RealmPlanTest`, `PocketFixTest`, this doc |
| REPLACED | the inline geometry of `PocketBuilder` (now `RealmPlan`, the same blocks plus the chamber); the order of the seal layers in `PocketPlan` (after the doorways); the immediate teleport of `ExclusivePockets.join` (held until built); `PocketBuild.features` (a feature that stands is not set again) |
| KEPT | every id, config key, command, saved field (`Pocket` NBT unchanged), enum order (`PocketPlan.Type` only appended), loot tables, chest positions, bar positions, plate positions, the exit rift, the boss, the puzzle order, the collapse, the 13 tomb definitions, all old behaviours; `DungeonGuard.allowBreakWhere` |

## Harness and tests

* `PocketFixTest` (JUnit, pure, no game): for each of the 13 tombs and 24 seeds: 14 to 21 rooms, at least 5 kinds, a trap room, a fighting room, the inverted-gravity room when the tomb asks, a loop;
  a voxel walk from the arrival reaches every room but the treasury with the seal shut and the treasury with it open; the lever hangs on a wall of the key room, in an air cell, outside every
  doorway, with the cell in front of it reached **with the seal shut**; the Sigil's chest is in the key room and reached; the two seal signs are inside the lair; the treasury exit is reached once
  the seal is open; sixty seal blocks stand; **the same walk with the guard's rule applied reaches ONE room for every tomb** (the reproduction); sign texts fit the board and name the lever; the
  source scan above. `printTheRoomsDoorsAndSwitchOfEveryTomb` prints every room, every door (sealed or not) and the switch for each tomb (read the console).
* `RealmPlanTest`: five rooms; the arrival walks to four rooms but not the vault, to all five after the bars lift; with the guard's rule applied the realm is one room; the lever is reachable, not in
  the vault, on a wall, flanked by two lights and two lanterns, under a sign; nine bars, three chests, two exits; the puzzle plates and lanterns; the lever rules never lock anybody in.
* `rift.pocket_fix` (server self-test, run by the CI smoke test under prefix `rift`): says whether the guard is on and whether it refuses a wall turned to air outside the builder's span; builds both realm
  kinds and all 13 tombs in a scratch cell on the real blocks and asserts rooms (>= 4 / >= 5), a flood fill over air from the arrival, the lever, signs, lanterns, chest and exits as blocks, the
  stiff lever refusing, the ready lever really lifting the bars / dropping the seal (blocks are air), the treasury reachable afterwards, the realm's door opening by itself, the arrival held until
  a tomb is built (first tomb, through the real jobs), the watchdog finding a walled-up doorway and the repair pass opening it without putting the seal back, and the sweep emptying the cell.

## What could not be verified

* Nothing could be run in the game (no Gradle offline). The type check passes (only the known stub artefacts), 130+ JUnit tests pass, but `rift.pocket_fix` itself has not run.
* That the Dungeon Guard's mixin is applied in the shipped jar is read from `nusmp.mixins.json` (`LevelGuardMixin`, `LevelChunkGuardMixin`, `require = 0`); the fix is correct whether it is applied or not.
* How the lever, the lanterns and the glowing signs look, and whether four 16-character lines fit the board in the game's font (the limit is a rule of thumb).
* Whether the first block-by-block placement of a tomb into 144 unloaded chunks is smooth on a real server (the arrival is now held, so a slow build is a wait with a percent, not a closed hall).
* Timings (120 s lever, 420 s self-opening) are my choice; both are config.
* The cue particles and the `LEVER_CLICK` sound mix.

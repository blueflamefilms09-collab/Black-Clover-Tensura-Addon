# The `/dungeon` admin tree (round 1.99.0, builder DUNGEON-ADMIN)

The owner's request: "to test a 2,000-floor world, 275+ Unique skills, multi-phase bosses and rifts there must be a permission-gated administrative command tree under
`/dungeon`": teleport to any floor, skip floors, spawn and slay Decennial bosses, open rifts, hand out and take back Unique skills, reset a player to a fresh entry, and a
particle view of the floor's room graph. This file is the account of what was built, WHY each part has the shape it has (the command-tree hierarchy, the permission gating, the
data it reaches, the chunk and generation safety), the numbers, and, honestly, what has not been seen running.

Nothing in it ran in a game. Everything that can be pure is tested (`DungeonAdminMathTest`, `TpRulesTest`, `WfcDebugModelTest`, `AdminSpecTest`, `AdminTextTest`); everything that
needs the game is type-checked against the API stubs and covered by the in-game self-test `dungeon.admin` for the CI smoke run.

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| The admin subtree `tp`, `skip`, `boss spawn / kill`, `rift spawn pocket / shortcut`, `skill unlock / give_sp`, `reset alignment / skills / waypoint / all`, `debug wfc / info`, `admin help` under the literal `dungeon` | NEW (package `labyrinth.mega.admin`) |
| `/dungeon floor`, `status`, `release`, `unstuck [party]`, `help` (player subcommands of `lock.DungeonCommand`) | KEPT, untouched, still open to every player (the admin tree is MERGED into the same literal) |
| `/multiverse mega ...` admin trees (`floors`, `lock`, `core`, `rift`, `party`, `hub`, `skillbook` ...) | KEPT, untouched (the repair tools; the `/dungeon` tree is the quick tester's handle on the same systems) |
| `DungeonAdminHooks` with the interfaces `BossHook` and `RiftHook` and their DEFAULTS (the Labyrinth Guardian and the rift block the dungeon has today) | NEW; the Decennial boss facade and the rift facade of the parallel builders replace the defaults with one line each (section 6) |
| Skill points: `/dungeon skill give_sp <amount>` | NEW command that does NOT bring the Skill Tree back: it adds MASTERY (section 4) |
| Unique skills: `UniqueBooks.grant / forget`, the `UniqueSkills` registry | REUSED as they are (no skill code changed) |
| `PlayerDungeonAlignment` attachment, `Waypoint` record in the player's persistent data, `MegaTravel` landing memory, `ExpansionQueue`, `HubTravel`, `LockPolicy`, `RoomGraph` | REUSED as they are |
| Lang keys `message.nusmp.dungeon.admin.*` in `tools/lang/dungeon_admin.json` | NEW (flat fragment; `en_us.json` not edited) |
| Self-test `dungeon.admin` | NEW |
| `MegaSystem`: one appended line `AdminRegistry.register(bus)` | NEW (the only edit to an existing file) |
| Replacements | NONE: nothing existed that this tree replaces |

## 2. The command tree and why it is shaped like this

```
/dungeon                                   (root: NO permission, it is the player's command)
  floor | status | release | unstuck [party] | help                 existing, everyone
  tp <floor -1000..1000>                   [op 2]
  skip [<floors -1000..1000>]              [op 2]
  boss  spawn <level -100..100> | kill     [op 2]
  rift  spawn pocket | shortcut            [op 2]
  skill unlock <skill|all> | give_sp <amount 1..100000>              [op 2]
  reset alignment | skills | waypoint | all                          [op 2]
  debug wfc | info                         [op 2]
  admin help                               [op 2]
```

* **One literal, merged.** The player's `/dungeon` is registered by `LockRegistry` (`DungeonCommand.player()`). A second `dispatcher.register(Commands.literal("dungeon") ...)`
  does not replace the first: Brigadier's `addChild` finds the node with the same name and merges the children of the new one onto it. So the admin tree is registered under the SAME
  root and nothing was renamed, moved or removed. The alternative (a different root such as `/dng`) would have left two commands for one idea, and the player's tab completion
  would not show the admin ones to an operator at the right place.
* **The gate is on the children, not on the root.** A `requires(hasPermission(2))` on the root would hide `/dungeon floor` and `/dungeon unstuck` from every player below level 2,
  and `unstuck` is the way out of a sealed room. So the root has no requirement and each ADMIN child (`tp`, `skip`, `boss`, `rift`, `skill`, `reset`, `debug`, `admin`) carries
  it (`AdminTree.gated`). Everything below a gated child is reached only through it, so no deeper node needs its own gate. A gated node is also left out of tab completion and out of
  the command tree the server sends to a client below level 2, so a player never even sees the admin words.
* **Ranges in the tree, rules in the executor.** `IntegerArgumentType.integer(-1000, 1000)` makes Brigadier refuse floor 1001 with its own red message and caret, before any code of
  ours runs. Rules that Brigadier cannot state (level 0 does not exist: the Hub has no boss) are checked by the executor and answered in red.
* **Executors are server side and total.** Every executor first finds the PLAYER it acts on (`AdminCommands.player`): a console, a command block or a function has none, and the
  answer is a red line saying how to run it (`/execute as <player> run dungeon ...`), never an exception. Then the body runs inside `guarded(...)`: a bug, a hook that throws or a
  system that is not ready ends as a red line plus a log entry and the number 0. Messages to a player without a connection (a test's stand-in) are dropped.
* **The commands act on the executing player.** Doing a tester's own reset or teleport is the use; for someone else `/execute as <player> run dungeon ...` already works and keeps one
  way to say "who". The repair tools that take a `<target>` stay under `/multiverse mega ...`.
* **The shape is written down twice.** `AdminSpec` lists every usage, every literal, every range as plain data. The JUnit tests check the data against the rules and against the SOURCE
  of `AdminTree` (read as text, because Brigadier cannot run in a unit test), and the in-game self-test compares the data with the REAL dispatcher (section 7).

## 3. The commands

### `tp <floor>` and `skip [floors]`

* **Where the player goes.** A floor is a sector in the player's party chain of the floor's road (the floor model: one sector, one floor, index `|floor| - 1`; an old ten-floor sector
  is found by `FloorMap.chainIndex` and the player stands at its landing, with a note). The arrival is `MegaTravel.go` (the teleporter the gates use: it generates the one landing chunk,
  remembers the way home, stamps the gate cool-down). Floor 0 is the Sanctuary Hub, through `HubTravel.returnToPlaza`.
* **A floor that does not exist yet is QUEUED, never refused, and never built on the command's tick.** The dungeon builds one floor at a time over many ticks, and a floor can only be
  queued once the floor before it stands. So the request becomes a TICKET, and a clock (every 5 ticks) asks the pure `TpRules` what the ticket needs: queue the road's first floor
  (`ExpansionQueue.tryFirst`), queue the next floor (`ExpansionQueue.tryNext`, the calls `/multiverse mega expand` makes; the generator then paces itself with its own per-tick time
  budget, `mega.genBudgetMs`), wait, give up with the reason, or ARRIVE. The player is told how many floors are queued and an estimate; the action bar shows the progress every two
  seconds. A new `tp` replaces the old ticket.
* **Reasons a request ends** (each a red line with the reason): a floor on the way failed to generate (retry with `/multiverse mega regen <id>`), the size limit `mega.maxSectors` is
  reached (it counts per party and both roads together: 1000 by default, so the first 1000 floors of a party in total; raise it for floors beyond), the generator refused, nothing
  moved for three minutes, no free party lane. `mega.expansion = false` only switches off the growth that a Gatekeeper door starts, as its comment says ("the dungeon only grows with
  /multiverse mega expand"), so an admin's teleport grows the dungeon anyway, as `expand` does.
* **Chunk safety.** Before the teleport a portal ticket (`TicketType.PORTAL`, radius 3, the pattern of `AvatarDelve` and the betrayal dimension) is put on the landing's chunks and the
  clock waits (at most 12 looks) for `hasChunk`, so the landing loads while the player is still where they were; then the existing teleporter's own `getChunk` for the single landing
  chunk finds it ready. Nothing is generated by the command.
* **The waypoint.** On arrival the player's `Waypoint` record (dimension, sector, level, floor, position, rotation: the one the Return Crystal, the Gate Key and the Return Portal read,
  stored in the player's persistent data under `nusmp_mega_waypoint`) is written with the landing. The periodic sampler (`WaypointService`) would not do it for a while: it never saves a
  spot on a stair pad, which is where every arrival is.
* **Alignment and events.** The floor tracker looks at the new floor within half a second and fires the floor events (first-time depth, quests). The tracker's memory of the old floor is
  dropped, so jumping from +1 to +50 enters +50 but does NOT "clear" the 49 floors in between (no false lock). Operators pass the Directional Lock when `floors.opsBypass` is on; when it
  is off and the lock would close the other road, `tp` refuses in red and names the fix (`/multiverse mega floors unlock <you>`) instead of teleporting into a place the hard guard
  would throw the player out of a second later.
* **`skip`.** `DungeonAdminMath.skip`: forward (default 1) goes away from the Hub along the player's own road; on Floor 0 the road is the one the player is bound to (their alignment),
  else the road of their waypoint, else the Holy road; outside the dungeon the starting point is the floor of their waypoint, else Floor 0. A negative number goes back towards the Hub and
  stops there, it never crosses it into the other road. The target is cut to +-1000 and the player is told when it was. Then it is `tp`.

### `boss spawn <level>` and `boss kill`

* A **level** is a decade of ten floors; level 4 is floors 31-40 and its Decennial boss stands on floor 40 (`DungeonAdminMath.bossFloorOfLevel`), level -4 on floor -40. The command spawns
  the boss at the player's feet through `DungeonAdminHooks.bossHook()`.
* **Default (until the Decennial facade is wired):** the Labyrinth Guardian, made exactly as a pocket realm makes its secret boss (`LabyrinthGuardianEntity.create` then
  `configureForFloor(bossFloor, theme)`: scaled by the floor's threat and given that floor's D&D profile), tagged `nusmp_admin_boss`. It is a free boss (no altar), so it unseals nothing
  by itself.
* **`boss kill`** slays every Guardian within 128 blocks. It sets the health to 0 inside the boss's own damage-budget bypass (otherwise the 6 % per second budget would cut even a kill
  down) and dies with the `genericKill` damage, which bypasses invulnerability, so the Mythic Trait does not answer it (the same path the guard self-test uses for `/kill`). A boss called
  by an altar forges its own Mana Core when it hears the death; a free boss drops the Mana Core of the sector it stood in (`ManaCoreItem.forSector`, protected like the altar's), which is
  what the Gatekeeper tests need.

### `rift spawn pocket|shortcut`

The crosshair is followed 32 blocks (`Entity.pick`); the rift opens in the air cell in front of the block hit, else three blocks in front of the player, and if the first cell is not
free the second is tried. Default hook: the three calls `/multiverse mega rift open` makes (`RiftService.open`, `aimShortcut` from the floor under it, `announce`); a shortcut needs a
floor of the Mega-Dungeon under it and is closed again, with a red line, anywhere else.

### `skill unlock <skill|all>`

The ids are the bare ids of the Unique skill lines (`UniqueSkills.bares()`: 254 today), tab-completed from the registry plus `all`. One skill is one call of `UniqueBooks.grant`, the call a
Skill Book makes, so a book and the command can never disagree; the player then picks and slots the skill in Tensura's own Ability Selection screen. `all` is that call for every
line, handed out as a JOB of 12 per tick (254 skill-list updates in one tick would be 254 packets in one tick), with a summary at the end ("N learned, M already known, K failed,
of T"). The Unique skills that are not Skill Book lines (Requip, the celestial keys, the Fairy Tail skills) have their own books and give commands (`/requip teach`,
`/multiverse celestial give`, `/multiverse fairytail give`) and are not part of `all`.

### `skill give_sp <amount>` (decided from the code)

Skill Points were REMOVED in 1.96.0 at the owner's request and nothing here brings them back. What replaced them is MASTERY: a line skill's mastery decides which of its arts have
awakened and the level of its passives (`MasteryLevels`: "with the Skill Tree gone there are no Skill Points to buy node levels with ... the skill's MASTERY sets the level every node
counts at"). So the closest living thing to "free Skill Points to test node purchasing" is mastery points, and the command adds `amount` (1 to 100000) mastery points to every Unique
skill line the player owns, and says: "Skill Points were removed in 1.96.0 (skill books teach Unique skills directly); mastery now opens the later arts of a skill." When the player owns
no line skill it grants nothing and says so in red. The other candidate, Black Market gold, buys books at the witch; it is not what Skill Points were for, so it is not used.

### `reset alignment|skills|waypoint|all`

| Kind | What is cleared | What is kept on purpose |
|---|---|---|
| `alignment` | the `PlayerDungeonAlignment` attachment (road, lock, deepest floor of each road) through `AlignmentRules.reset`, the floor tracker's memory of the floor, and the client copy is re-sent | the Hub origin (the way the home gate leads back to) and the gate cool-down stamps: clearing them would strand a player in the Sanctuary |
| `waypoint` | the `Waypoint` record, the deepest-Landing memory of both roads (`nusmp_mega_landing*`, which decides where the next entry puts the player), the Return Crystal and Gate Key cool-downs | the party record |
| `skills` | the Unique skills the skill books taught (`UniqueBooks.forget`, as a job) | Tensura's own skills and every vanilla datum |
| `all` | the three above | |

### `debug wfc`, `debug info`, `admin help`

* **`debug wfc`** toggles a particle view of the floor under the admin. The judge is the graph the cyclic generator already uses (`RoomGraph`: rooms are nodes, doorways are edges), asked
  about the SAVED sector (its rooms and its doorway pairs): nothing is recomputed or generated. Colour code, per room and per doorway: **green** = on a loop (a doorway that is not a
  bridge), **yellow** = single link (a bridge: the only way between two sides; a room with no loop through it), **red** = dead end (a non-terminal room with at most one doorway, and
  the doorway into it). The arrival stairs, forward stairs, boss chain and Safe Zone are terminals and never red, as in the cyclic layer. Three layers of points: the outline of
  every room's box, the graph lines (room centre, doorway, room centre) and a larger node at every doorway. A chat summary follows: rooms, doorways, dead ends (0 expected), loops
  (independent cycles), rooms on a loop, single-link rooms, separate parts, and a verdict in green or red ("no dead ends, one connected floor" is the owner's rule; a floor in two parts
  also fails).
* **Throttle.** A burst every 10 ticks to the toggling admin only (`ServerLevel.sendParticles(player, ...)` with `overrideLimiter`, since the view reaches 72 blocks), at most 160 points
  per burst split 40 % nodes / 35 % lines / 25 % outlines, only points in range, each layer from a cursor so the whole picture builds up over successive bursts, admins staggered by a
  hash of their id, 200 points per tick across all admins as a hard stop. It ends after 5 minutes, when the admin changes dimension, logs out or the server stops. Walking into another
  floor rebuilds the view for that floor and prints its summary.
* **`debug info`**: floor, level (decade), theme and seed; the sector, road, party, state, room count and whether it is a boss floor. On Floor 0 it says so; outside the dungeon it says
  where the player's waypoint is.
* **`admin help`** lists every usage with its one-line help (from `AdminSpec`, so it cannot list a command that is not there).

## 4. What the data is (capabilities, in NeoForge terms)

The owner's spec speaks of "capabilities" (`PlayerDungeonWaypoint`, `PlayerDungeonAlignment`). In NeoForge 1.21 a capability is a way to ask a block, an item or an entity for behaviour;
data that belongs to a player and must be saved, copied on death and synced is a DATA ATTACHMENT or the player's persistent data. This mod already has both, and the commands use
them as they are:

| Spec word | Where it really is | How the commands touch it |
|---|---|---|
| `PlayerDungeonAlignment` | data attachment `nusmp:dungeon_alignment` (`labyrinth.mega.floors.AlignmentAttachments`, value `PlayerDungeonAlignment`) | `reset alignment` -> `AlignmentRules.reset`, `AlignmentNet.send`; `skip` reads `chosen()` |
| `PlayerDungeonWaypoint` | record `Waypoint` in the player's persistent data, key `nusmp_mega_waypoint` (`labyrinth.mega.rift`) | `tp` writes it, `reset waypoint` and `WaypointService.clear` remove it |
| Unique Skill Line | ManasCore skill list, one `LineUniqueSkill` per book | `UniqueBooks.grant / forget / ownedLines`, mastery through the skill instance |
| Floor / dimension / chunk loading | `MegaTravel`, `ExpansionQueue`, portal ticket | section 3 `tp` |

## 5. Numbers

| What | Value |
|---|---|
| Floor range | -1000 to +1000, 0 = Hub (`DungeonAdminMath.MAX_FLOOR`) |
| Level range | -100 to -1 and 1 to 100, ten floors each, boss on the last floor |
| Permission | level 2 on every admin child, none on the root |
| Teleport clock | every 5 ticks; arrival waits at most 12 looks for the landing chunk; gives up after 3 minutes without progress |
| Skill jobs | 12 skills per tick |
| `give_sp` | 1 to 100000 mastery points per skill |
| Debug view | burst every 10 ticks, 160 points per burst, 200 per tick in all, range 72, 5 minutes |
| Estimate shown for a queued teleport | 2 seconds per floor (a guess for the countdown only) |

## 6. Wiring the two parallel systems (for the integrator)

The commands call only `DungeonAdminHooks`. Today the defaults use the dungeon's existing mechanisms. When the Decennial boss facade
(`com.newuniverse.nusmp.labyrinth.mega.decennial.DecennialBosses`) and the rift facade (`com.newuniverse.nusmp.labyrinth.mega.rift.RiftAdmin`) exist, add these two lines at the end of
`AdminRegistry.register(IEventBus)`:

```java
DungeonAdminHooks.setBossHook(DungeonAdminHooks.BossHook.of(DecennialBosses::spawnBoss, DecennialBosses::killBosses));
DungeonAdminHooks.setRiftHook(RiftAdmin::spawnRift);
```

The method references need `boolean spawnBoss(ServerLevel, int, BlockPos)`, `int killBosses(ServerLevel, ServerPlayer)` and `boolean spawnRift(ServerLevel, BlockPos, boolean pocket)`.
For a facade method of another return type use a lambda, for example `(l, lv, p) -> { DecennialBosses.spawnBoss(l, lv, p); return true; }`. Passing `null` to a setter puts the default back.

## 7. Tests

* `DungeonAdminMathTest` (14): the range, the clamp, the side and road of a floor, the level of every floor from -1000 to 1000, the boss floor of every level, boss-floor detection, and
  `skip` forward, from the Hub, at the end of the road, backwards (stops at the Hub), zero, and a property test over 13 starting floors x 18 counts x 3 hub sides.
* `TpRulesTest` (11): arrive, queue first, queue next, wait, give up (stuck, failed, limit, off, lost link), and a simulated walk of one floor at a time that queues each floor once.
* `WfcDebugModelTest` (16): the colour code on a ring with a spur, a chain between terminals, every terminal kind, a floor in two pieces, unknown and duplicate doorways, empty and
  one-room floors; doorway and outline geometry; the throttle (budget, layer shares, range, cursor coverage and wrap, stagger, five minutes).
* `AdminSpecTest` (10): permission, no clash with the player's literals, every usage has help, the owner's ranges, and the SOURCE of `AdminTree` read as text: the gate on each admin
  child and not on the root, the ranges, the suggestion provider, the same names as `DungeonCommand.player()`, one registration line.
* `AdminTextTest` (4): the lang fragment equals `AdminText` key for key, word for word and in order; numbered placeholders; no clash with `en_us.json`; every key used, none missing.
* Brigadier itself cannot run in the JUnit shim, so the tree is checked as data plus source text there, and for real by the self-test.
* Self-test **`dungeon.admin`** (registered in `AdminRegistry`; CI should expect the prefix `dungeon.admin`): on the server's own dispatcher it checks that the player subcommands survived the
  merge and are open to a level-1 source, that every admin literal exists, is hidden from level 1 and usable at level 2, that the usages Brigadier prints at both levels equal `AdminSpec`
  exactly (extra, missing or ungated nodes fail), the argument edges (1000 / 1001, level 100 / 101, amount 0), that every command from a console returns 0 and throws nothing, that the
  commands that need a dungeon refuse cleanly for a stand-in in the Overworld, `reset`, teaching / mastering / forgetting a skill by command, the teleport planner on a built floor
  (ARRIVE) and on floor +-1000 (not ARRIVE, nothing grown), the debug model of a saved sector and `debug wfc` on and off on a built floor.

## 8. What could NOT be verified (honest list)

* Nothing was run in a game. The tree, the merge with the player's `/dungeon`, the gates, the executors and every message are type-checked against API stubs only; `dungeon.admin` is the
  first real check and may find something.
* The Brigadier merge relies on the documented behaviour of `CommandNode.addChild` (children of an equal-named literal are merged, the first node's requirement is kept). The
  self-test pins the result but has not run.
* `tp` to a far floor takes as long as the generator needs (about one floor per 1-3 seconds on a normal server, one floor at a time for the whole server). The "2 seconds per floor"
  estimate is a guess. A far teleport (hundreds of floors) also fills the world with the sectors in between; that is the nature of the request.
* `skill give_sp` uses ManasCore's `addMasteryPoint(entity, double)`; its exact scaling and its effect on a stand-in are not known, so the self-test reports the result instead of
  requiring it. `unlock all` hands 12 skills per tick; the effect on a client with a long skill list was not measured.
* The default `boss kill` relies on `die(genericKill)` after zeroing health inside the budget bypass, the sequence the guard self-test uses; the Mythic Trait and legendary
  resistances were reasoned about, not seen. A boss from a facade that is not a `LabyrinthGuardianEntity` is only slain by the facade's own hook.
* The particle view: colours, sizes and the 72-block reach are reasoned about, not seen; whether 160 dust particles per burst read well is a matter of taste to tune (all constants are in
  `WfcDebugModel`).
* `reset alignment` does not touch the System overlay's per-player records (stats, quests, points): those are not alignment, and `/multiverse mega system ...` owns them.
* The wiring lines of section 6 cannot be compiled until the two facades exist; their signatures are those the integrator's brief named.

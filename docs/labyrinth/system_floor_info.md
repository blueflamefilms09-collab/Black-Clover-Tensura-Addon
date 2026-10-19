# The System tells you the floor, and nobody is locked in a room (1.94.0)

Owner request, two items about the Mega-Dungeon:

1. "the solo leveling system should tell you what the theme is for the dungeon's floor and also what the mechanic".
2. "remove the random being locked in a room or add a way to get out".

Code: `labyrinth/mega/system/FloorInfo*.java` and `FloorCard*.java` (the floor report), `client/mega/system/FloorCard*.java` (its window), `labyrinth/mega/lock/**` (the escape
rules), `labyrinth/mega/gen/StairReach.java` (the generator's flood fill). Lang: `tools/lang/system_floor_info.json` (written by `tools/system/gen_floor_info_lang.py`).
Nothing here was run in a game; section 9 says exactly what is unverified.

## 1. New, replacement, kept (owner rule 5)

Everything is NEW. Before building, the mod was searched for a floor description, a theme description, a way out of a seal and an unstuck command: there is none (the System had
a status window and quests; the mechanics had an action-bar line when you walk into one; the trial chamber had a reset for an abandoned chamber; nothing told you the theme, and
nothing opened a seal around a player who was still inside).

| Part | Status |
|---|---|
| The floor report: payloads `nusmp:system_floor_card` / `nusmp:system_floor_request`, the window, the key, the chat copy, the mid-floor watcher | NEW |
| `/dungeon floor`, `/dungeon status`, `/dungeon release`, `/dungeon unstuck [party]`, `/dungeon help`; `/multiverse mega system floor`, `/multiverse mega lock ...` | NEW |
| The escape rules: `EscapeRules`, `UnstuckRules`, `LockCase`, `LockBook`, `LockRunes`, `MechLock`, `UnstuckService`, `SectorReach` | NEW |
| `StairReach` (the flood fill) and its call in `SectorAudit` | NEW (the audit's old "unreachable rooms" check is KEPT) |
| Configs `nusmp-labyrinth-mega-floorinfo-server.toml`, `nusmp-labyrinth-mega-lock-server.toml`, two keys in the client config | NEW |
| Self-tests `system.floor_info`, `lock.escape` | NEW |
| The Sealed Archway's reset of an abandoned trial, the mechanics' "ends when the area is empty for five seconds", the Directional Lock, the Rift pocket's exit | KEPT, untouched; the new rules sit beside them |
| Nothing was replaced and nothing was removed | |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. Item 1: what the System says on every floor

### What the player sees
A second System window (same holographic style as the status window: dark-cyan panel, bright cyan border, four corner brackets, `[ SYSTEM ]`), centred under the top edge, that fades
in and out over 14 seconds. Example for floor +12 of the Holy Church:

```
[ SYSTEM ]
FLOOR +12  -  UP
HOLY ROAD: the way on climbs UP
You go UP, to floor +12.
THEME: Holy Church
HOLY ROAD ONLY  -  DANGER x0.9
A great cathedral: nave and pews, stained glass, a bell tower, a crypt and a reliquary.
FLOOR MECHANICS (2)
  > Rim Curse
      ACTIVE  -  whole floor
      The edges of the floor drain you.
      Counter: none. Survive it, or leave its area.
  > Lantern Vow
      WAITING  -  whole floor
      Rooms stay dark unless a light is carried.
      Counter: Carry a light through it.
ACTIVE HERE (1)  ...
3 more mechanic(s) wait in the rooms of this floor. You are told when you step into one.
DEPTH 12: monsters +12% health and +8% damage, mind level 1, kills give x1.09 experience.
Press [.], or type /dungeon floor, to read this again.
Stuck? Type /dungeon unstuck.
```

* **Floor and direction.** The signed floor number, and UP or DOWN. The direction is read from the signed number (a higher number is up on any road), so the Holy road climbs and
  the Abyssal road descends, and going back is the opposite word. A login, a respawn or an arrival from the Hub has no previous floor: it is told the direction of the road.
* **Theme.** Name, one line, the road it belongs to (`HOLY ROAD ONLY` for the themes added in 1.58.0, `HOLY ROAD FAVOURED` for an original theme the Holy chain draws most, `BOTH
  ROADS` for the baseline) and the theme's danger multiplier.
* **Mechanics.** Every floor-wide mechanic the Director planned for the sector (state: waiting, active, overcome), each with its description and how to beat it (the counter's
  hint, or "none: survive it or leave"). The room, zone and boss mechanics that are active where the player stands are listed under "ACTIVE HERE", and a count says how many
  more are still hidden in the rooms. Lists are cut at six with a "+N more" line.
* **Depth.** The effect of the depth on the monsters (the Dungeon Master's depth rules: health, damage, mind level, experience), honouring the depth layer's config switches.
* **Boss floor.** A line says the Guardian waits.

### When it appears
Every way into a floor is the one event `FloorListener.onFloorEntered`, because the floor tracker forgets a player's floor at login, respawn and dimension change and announces the
first floor it sees again: a staircase, a new floor, an arrival from the Hub, a respawn into the dungeon, a login, and a party member who joins (they arrive through the same door).
`FloorInfo` is registered as a floor listener, so all of them are covered by one line. No spam (`FloorInfoRules.Throttle`):

* an arrival is shown once per change of floor; the SAME floor again only after `arriveCooldownSeconds` (30), so walking a staircase up and down does not repeat it;
* the key and `/dungeon floor` are answered unless the last request was half a second ago;
* a mid-floor update waits `updateCooldownSeconds` (10) and a moment after the last report;
* the player's own switch is the client config `floorCard` (false = nothing by itself; the key and the command still show it) and `floorCardSeconds`;
* `chatCopy` (server, default off) also writes the report in the chat on every arrival.

### Changes in the middle of a floor
Once a second `FloorInfo.tick` compares what each player was last told about the mechanics around them with what is there now. A room, zone or boss mechanic that WOKE where the
player stands, and any mechanic that was OVERCOME, is announced once with a pop-up line (`MECHANIC: name`, `MECHANIC OVERCOME: name`) and a refreshed, shorter report. A floor-wide
mechanic going from waiting to active is not news (the report listed it), and a mechanic that merely ended because the player left its room is not news either.

### Everything is data
* The theme comes from the sector's `RegionTheme` and its `ThemeDef` (name, danger, catastrophe table). The one-line description is `FloorInfoText.themeDesc(id)` for the 27
  themes that exist; a theme added later falls back to a line made from its data ("Its Crucible leans towards: collapse"), so it needs no edit here (`FloorInfoTextTest` proves
  both).
* The mechanics come from the plan (`MechanicsDirector.planFor(sector)`), their names and descriptions from the library, their lang keys `mechanic.nusmp.<id>.name / .desc`; the
  counter's hint is the existing `message.nusmp.mega.mech.hint.<kind>`. A mechanic added tomorrow is covered the moment it is in the library.
* Fallbacks everywhere: the card carries the English; the reader asks the lang file first (`translatableWithFallback`), so a key with no text, a datapack mechanic, a lang file not
  merged yet all still read. A mechanic with no name reads as its id made readable.

### Commands, key, config
* `/dungeon floor` (every player): the window and the chat lines. `/multiverse mega system floor [player]` (permission 2): the same for another player, printed to the operator.
* Key `key.nusmp.system_floor`, default period, category "Multiverse: System": asks the server again (so a mechanic that changed since is in it); pressed while the report is up it
  dismisses it.
* Server `nusmp-labyrinth-mega-floorinfo-server.toml` `[floorinfo]`: `enabled`, `arriveCooldownSeconds`, `mechanicUpdates`, `updateCooldownSeconds`, `chatCopy`. Client
  `nusmp-labyrinth-mega-system-client.toml` `[system]`: `floorCard`, `floorCardSeconds`.

### Files
| File | What and why |
|---|---|
| `system/FloorInfoText` | the English of every key, and the one-line description of each theme; the lang fragment is written from it |
| `system/FloorInfoRules` | pure: reason, direction, depth numbers, the throttle, the diff of mechanics |
| `system/FloorCard`, `FloorCardLines` | the card (ids, English fallbacks, numbers) and the pure composer of its lines, shared by the window and the chat |
| `system/FloorInfoNet` | the two payloads, every string bounded and cut before it is written |
| `system/FloorInfo` | builds the card from the world, the floor listener, the mid-floor watcher, the chat lines |
| `system/FloorInfoConfig`, `FloorInfoRegistry`, `FloorInfoSelfTests` | config (self-registering), wiring, `system.floor_info` |
| `client/mega/system/FloorCardLayout`, `FloorCardClient` | the pure layout (marks, tested) and the layer, key and config reading |

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Item 2: every way the dungeon can lock a player in, and what was done

Found by reading all of `labyrinth/**`, the room templates (the NBT files were scanned for the blocks that close things) and the 1000 mechanics' data. `LockCase` keeps this list as
data and a test demands that anything that can hold a person has an automatic way out and the command.

| # | Case | Can it hold a person | What was done |
|---|---|---|---|
| 1 | **Trial chamber** (`nusmp:sealed_archway`, 12 templates in `structure/mega/trial`): Magic Barrier over every doorway until the wave is dead | yes | Already reset when nobody was inside for `reset` seconds. New: with a player alive inside the seal now ALSO opens by the escape rules: release rune after 60 s, open after 600 s whatever is happening (a mob stuck in a wall or out of reach no longer keeps the room shut), `/dungeon release`, `/dungeon unstuck`. Opening is the same reset (mobs removed, barriers down, chamber back to idle). The chamber's own `reset` seconds stay the "nobody inside" time. |
| 2 | **Mechanic `seal_doors`** (198 of the 1000: bars across every doorway until a puzzle or fight is beaten) | yes | The big one: 204 room, 7 boss, 2 zone and 1 floor mechanics. Found by `Instance.sealsPlayers()`. Timers and rune as in 1; nobody inside for 10 s opens it; an idle room (no hostile alive) shows the rune after 15 s and is rescued after 5 minutes. Opening ENDS the mechanic (every block put back, its monsters removed) and remembers it as done so it cannot re-arm and shut the next party in (it pays no reward). |
| 3 | **Mechanic `cage`** (16: a ring of bars round the party, e.g. the Sure-Hit Domain) | yes | As 2. |
| 4 | **Seals nobody can beat**: 34 sealing mechanics have counter `none` ("survive it or leave", but the doors stay shut), and 16 plate puzzles need several players at once | yes | Unbeatable for the party present: rune after 15 s, the seal opens by itself after 30 s (`EscapeRules.solvable`). |
| 5 | **Boss-arena mechanics** (cage or doors while a boss lives: 7 boss-scope mechanics) | yes | The arena stays closed while the boss lives and a player is inside. The EXIT rune ("EXIT RUNE") is there from the first second: using it sends everyone inside back to where they came in and heals the boss and clears its target, the seal ends. Opens when the boss dies, when nobody is left inside for 10 s, or after `bossMaxSeconds` (900). |
| 6 | **Obstacles** (`place` with no end time: 38 mechanics) that can wall a doorway | rarely | Tracked softly (no message, no rune): they are retired by the longest-time rule (600 s) and by the nobody-inside rule; `/dungeon unstuck` covers the rest. A floor-wide mechanic is never retired (it is meant to last the floor). |
| 7 | **The Guardian's hall** (`arena/guardian_hall_a, _b`) | no | Checked: its entrance is an open doorway; the Gatekeeper slab at the far end only gates progress to the antechamber and nobody stands inside it. Nothing closes behind a player. (The Gatekeeper, the Mana Core and the altar are untouched.) |
| 8 | **Puzzle doors** (`gatekeeper_door[kind=puzzle]`, 9 puzzle halls) | no | Checked: the door closes a closet with the reward, never the hall, which has its own doors; a solved door only ever opens. |
| 9 | **Pit chamber** (`pitfall_tile`) | briefly | The chamber under the floor has a ladder (checked by the hall's own audit); the floor closes 5 s later. `/dungeon unstuck` works from the chamber. |
| 10 | **Rift pocket realm** | no | The Antechamber holds the exit rift and the realm ends after `pocketMinutes` and throws everyone out. `/dungeon unstuck` inside a pocket takes the same exit (`PocketRealm.leaveForRescue`). The bars to the pocket's vault open when its boss dies or its puzzle is solved, and the Hall always has the way back. |
| 11 | **Crucible catastrophes** (collapse, flood, spirit) | no | Checked: they change no block, so nothing can close. |
| 12 | **Gloom, darkness, the snuffer glyph** | no | Hurt or blind, never block. |
| 13 | **Directional Lock / HardLock** | no | It closes a ROAD, never a room, and the way back to Floor 0 is always open. |
| 14 | **Collapse walls** | none exist | (The "collapse" catastrophe is dust and damage only.) |
| 15 | **A closed pocket left by the generator** (old and cyclic WFC) | yes | The generator's audit now also runs `StairReach.problems`, the explicit flood fill: every placed room must have a walk to the stairs, the arrival must reach every stairwell, every room must have a doorway. A plan that fails is thrown away and the next seed tried. A JUnit test builds 400 floors with each generator and checks all of it. The saved sector gets the same check (`SectorReach`, `/multiverse mega lock reach`, and the self-test over every generated sector). |
| 16 | **Spawn into an unreachable room** | yes | The arrival is the arrival stairwell, which the flood fill proves reaches every room and the forward stairs (15); a teleport or respawn anywhere else is covered by `/dungeon unstuck`. |
| 17 | **Unbreakable walls** (the dungeon guard) | multiplies the others | A player cannot dig out of anything, so the way out never needs a wall to break: the rune, the timers and `/dungeon unstuck` (which moves the player). |
| 18 | **Escort rooms, other "locked" room kinds** | none exist | `RoomKind` has no locked kind; no escort room exists in the mod. |

### The rules (`EscapeRules`, pure, `EscapeRulesTest`)
In the order they are asked: nobody inside for `absentSeconds` (10): open. Boss lock: boss dead: open; fight longer than `bossMaxSeconds` (900): open; otherwise stay closed with the
exit rune visible. Any other lock: longer than `maxSealSeconds` (600): open; unbeatable by the party present for `unsolvableSeconds` (30): open; no hostile alive for
`rescueMinutes` (5): open (the automatic rescue). The rune is visible after `idleRuneSeconds` (15) when the room is idle or the seal cannot be beaten, and after
`runeAfterSeconds` (60) whatever is going on. A test tries every combination of facts at a very late time: every one opens. Every number is clamped, so a bad config value cannot
switch the guarantee off or open a seal the moment it closes; the only switch is `lock.enabled`.

### The rune (`LockRunes`)
No new block, item or texture. Two vanilla entities: an `interaction` entity (a 1.4 x 2.2 box you can right-click or hit) and a `text_display` over it, turned to face you, saying
`[ SYSTEM ]` / `RELEASE RUNE` (or `EXIT RUNE`) / what a click does (translatable with an English fallback), with a ring of end-rod (portal for the exit rune) particles rising once a
second. They carry the tags `nusmp_release_rune` and `nusmp_rune_key:<lock>`, which is how a click finds its lock after a restart; a rune whose lock is gone is discarded when it is
clicked or when a player comes near (`sweep`). Using a rune opens the lock for everyone inside.

### `/dungeon unstuck` (`UnstuckService`, `UnstuckRules`, `UnstuckRulesTest`)
For every player: `/dungeon unstuck` and `/dungeon unstuck party`. It takes the player (the party: everyone who plays with them, through `SystemParty`) to the LAST SAFE ROOM they stood
in (remembered once a second while they stand in a landing, a safe zone or a stairwell), else the NEAREST SAFE ROOM by doorways (a breadth-first search over the saved room graph),
else the arrival stairs of the floor, else the forward stairs, else the Hub. In a pocket realm it uses the realm's exit. Cost and cooldown (config): 3 experience levels, 300 s
(party 600 s), refused while a hostile dungeon creature is within 8 blocks. A player who is really sealed in (inside a closed lock) pays nothing, waits for nothing, and the hostile
test does not apply. Creative players pay nothing. The cost is paid once, by the player who typed the command.

### Other commands
`/dungeon status` (sealed? by what, for how long, when the rune comes, when it opens at the latest), `/dungeon release` (open the seal you stand in once its rune is ready; for a
player who cannot see the rune), `/dungeon help`. Operators: `/multiverse mega lock list | release [player] | unstuck <player> | cases | reach [sector]`.

### Config (`nusmp-labyrinth-mega-lock-server.toml`, `[lock]`)
`enabled`, `runes`, `trials`, `mechanics`, `absentSeconds` 10, `runeAfterSeconds` 60, `idleRuneSeconds` 15, `rescueMinutes` 5, `maxSealSeconds` 600, `bossMaxSeconds` 900,
`unsolvableSeconds` 30, `unstuckCooldownSeconds` 300, `unstuckPartyCooldownSeconds` 600, `unstuckXpLevels` 3, `unstuckHostileRadius` 8. With `enabled = false` the dungeon is as it
was before this update.

### Files
| File | What and why |
|---|---|
| `lock/EscapeRules` | pure: the rules above, the facts, the status countdowns, which mechanics seal, which counters can be beaten |
| `lock/UnstuckRules` | pure: cooldown, cost, refusals, the order of destinations |
| `lock/LockCase` | the inventory of section 3 as data, with the guarantee of each |
| `lock/Lockable`, `LockBook` | the interface a lock implements and the engine that applies the rules once a second to every lock, tells the prisoners, places the rune, opens |
| `lock/MechLock` | the adapter for a sealing mechanic (and the boss exit) |
| `lock/LockRunes` | the rune entities |
| `lock/UnstuckService`, `SectorReach` | the command's work; the flood fill on a saved sector |
| `lock/DungeonCommand`, `LockRegistry`, `LockConfig`, `LockText`, `LockMsg`, `LockSelfTests` | commands, wiring, config, words, the self-test |
| `gen/StairReach` | pure: the flood fill, the nearest room, the pinch rooms |

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. Edits to existing files (all additions)

| File | Edit |
|---|---|
| `labyrinth/trial/SealedArchwayBlockEntity` | implements `Lockable`; two fields (`activatedTick`, `lastAlive`) saved/loaded (`activatedTick`); keeps itself in the lock book while sealed; drops its entry on clear/reset; an `open` method. The trial state machine (`TrialState`, `TrialMath`) is unchanged. |
| `labyrinth/mega/mechanics/Instance` | four public questions: `sealsPlayers()`, `holdsObstacles()`, `playersNeeded()`, `entryOf(uuid)` |
| `labyrinth/mega/mechanics/MechanicsDirector` | `keyOf(sector, slot)` (public wrapper of the key it already builds) and `retire(instance)` |
| `labyrinth/mega/rift/PocketRealm` | `leaveForRescue(level, player)` (public wrapper of the existing exit) |
| `labyrinth/mega/gen/SectorAudit` | one line: the audit also reports `StairReach.problems(result)` |
| `labyrinth/mega/system/SystemText` | `english()` also asks `FloorInfoText` and `LockText` (two lines) |
| `labyrinth/mega/system/SystemCommand` | the subcommand `floor [player]` and its method |
| `labyrinth/mega/MegaSystem` | two lines in `init`, two in the game-bus block (`FloorInfoRegistry`, `LockRegistry`) |
| `labyrinth/mega/MegaCommands` | one line: the `lock` branch |
| `client/mega/system/SystemClient` | one line: `FloorCardClient.init(modBus)` |
| `client/mega/system/SystemClientConfig` | two options (`floorCard`, `floorCardSeconds`) and their readers |
| `.github/workflows/smoke.yml` | `lock` added to `SMOKE_EXPECT` (the `system` prefix already covers `system.floor_info`) |

No version bump, no CHANGELOG, README or `en_us.json` edit: the lang is the flat fragment `tools/lang/system_floor_info.json` (117 keys).

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. Tests

JUnit (all run): `FloorInfoRulesTest` (direction on both roads, depth numbers, the throttle, the diff), `FloorInfoTextTest` (the fragment equals the code's map, placeholders, every
theme described, a later theme falls back), `FloorCardLinesTest` (the lines, both roads, boss floor, no-text fallbacks, mechanics off, translator), `FloorCardLayoutTest` (nothing
leaves the screen at four sizes and two fonts, wrapping, the cut, the fade), `EscapeRulesTest` (every rule, the brute-force "every seal eventually opens", clamping),
`UnstuckRulesTest`, `LockCaseTest`, `StairReachTest` (small graphs, a deliberately broken plan is reported, and 400 floors of each generator, both roads: every room has a path to
the stairs; measured: the cyclic generator leaves a trial or puzzle room on every walk from the arrival to the stairs in 3 of 400 floors, the old generator in 27 of 400).
In-game (run by the CI smoke test): `system.floor_info`, `lock.escape`.

## 6. `system.floor_info`
Stands a player in a room of a generated sector and fires the real floor event: checks the floor, the direction, the sector's theme with name, line and road, the floor mechanics exactly
as the Director planned them with a counter line each, the depth, that the words name them all, that a repeat is not shown but a different floor and a request are, the codec round
trip and the wire limits, the fallbacks, and the watcher's events.

## 7. `lock.escape`
The pure rules; the lock-case list; the flood fill on every saved sector; the rune label through the game's own text parser; a REAL sealing mechanic applied in a real room: the room's
blocks change, the lock book takes it, the rune appears at the time the rules say, the seal opens at the time the rules say with every block back and the rune gone (driven by a
clock the test controls), and an empty seal opens by the nobody-inside rule; `/dungeon unstuck` picks the remembered safe spot, then the nearest safe room, and refuses a player who cannot pay.

## 8. How to check by hand
`/multiverse mega lock cases`, `/multiverse mega lock reach`, `/multiverse mega selftest lock.escape`, `/multiverse mega selftest system.floor_info`; apply a sealing mechanic with
`/multiverse mega mechanic apply <id>` in a room and watch `/dungeon status`.

## 9. Not verified (honest list)
Nothing was run in a game. The Java was type-checked against the project's API stubs (with a negative control) and the pure parts run as JUnit tests. Unverified:
* the look of the floor report window (the layout is tested as arithmetic; colours, spacing and the fade are judged by eye only) and its key;
* that the `interaction` entity receives the right-click as `PlayerInteractEvent.EntityInteract` / `EntityInteractSpecific` and the hit as `AttackEntityEvent` on a dedicated server
  (all three are handled, once), that the `text_display` accepts the label as a JSON string in 1.21.1 (the self-test runs it through the game's parser), and the look of the rune;
* teleporting a real player in `UnstuckService` (a fake player has no connection, so the move itself is not in the self-test; the destination choice and the refusals are);
* that retiring a mechanic puts every block back in a guarded dimension for every one of the 214 sealing mechanics (the self-test does it for one; the journals are the existing
  mechanism);
* that the sealing mechanics' trigger does not re-fire between the release and the end of the instance (the instance is ended in the same call);
* a trial chamber's release in a real chamber (the chamber's code path is the existing reset; its rules are tested purely);
* balance of every number (a 15 s rune, a 5 minute rescue, a 3 level cost): they are config and a taste;
* that a mechanic released by the escape rules, which is remembered as done for the whole world, is acceptable for a puzzle the party gave up on (the alternative is that it shuts the
  next party in again).

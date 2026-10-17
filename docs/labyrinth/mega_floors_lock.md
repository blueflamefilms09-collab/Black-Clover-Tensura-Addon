# Mega-Dungeon: the floors, the floor tracker and the Directional Lock (`mega.floors`, 1.55.0, builder FLOORS)

Package `com.newuniverse.nusmp.labyrinth.mega.floors`. **Nothing here was run in game.** The pure classes (`FloorBands`, `AlignmentRules`,
`PlayerDungeonAlignment`, `ClientAlignment`) are unit tested (JUnit, 58 tests; 60 deliberate breakages of the production code were each caught by at least
one test, section 11); the game classes were type-checked against the project's API stubs (rebuilt from the real API dump, with a negative control that
proves the check can fail); seven IN-GAME self-tests (`floors.*`, section 11) are registered with the shared `SelfTests` registry so the CI server smoke
test exercises the real thing. CI is the first real compile. Section 13 is the honest list of what is unverified. Pictures of a sample sector are listed in
section 14.

## 1. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| Floor numbers of the rooms of a sector (`Room.floor`), computed from the room graph | NEW | The contract added the field; this module is the only one that writes it. |
| Two ways to cut a sector into floors (`floors.banding`: `even` default, `arena`) | NEW | `arena` is the brief's literal rule and stays available; `even` is the default because of what the generated sectors look like (section 4). |
| The floor tracker (which floor a player stands on, `FloorListener` events) | NEW | The contract added the listener interface and the `MegaDungeon.fire*` hooks; this module fires them. |
| `PlayerDungeonAlignment` (the Directional Lock's data) | NEW | A NeoForge data attachment `nusmp:dungeon_alignment`, the 1.21 form of the brief's "capability". |
| The sync packet `nusmp:alignment_sync` and the client holder | NEW | |
| The lock policy (`AlignmentPolicy`) and the hard guard | NEW | The interface is the contract's; nothing existed behind it. |
| The start-up pass that numbers sectors saved before floors existed | NEW | Old 1.54.0 worlds. |
| `/multiverse mega floors ...` | NEW | Attached by the lead. |
| `MegaDungeon.floorAt`, `polarityAt`, `Sector.perRegion`, `FloorMap` | KEPT | Used as they are. One quirk of `floorAt` is worked around, not edited (section 4). |
| The 1.54.0 sectors of an old world | KEPT | They have no floor numbers; they are numbered when the server starts (section 4). |
| `LabyrinthTravel` | KEPT | Called (`leaveAny`), never edited. |
| Nothing was replaced and nothing was removed. | | |

## 2. The brief, sentence by sentence

| The owner wrote (brief 3, system 2 and the introduction) | Where it is |
|---|---|
| "Write the capability `PlayerDungeonAlignment`." | `PlayerDungeonAlignment` + `AlignmentAttachments` (attachment `nusmp:dungeon_alignment`). |
| "Once a player clears Floor +1 or -1, their alignment locks." | `FloorTracker` asks `AlignmentRules.clearsPrevious` / `onFloorCleared` when a player moves on from a floor; at `floors.lockAtFloor` (default 1) the lock closes. |
| "They cannot enter the opposite polarity's staircases/portals." | `LockPolicy`, registered behind `MegaDungeon.mayEnter`: the Hub's gates and the chain travel ask it; `HardLock` catches everything that does not. |
| "once they begin ascending or descending, a dimensional lock prevents them from switching directions (though they can always retreat to Floor 0)" | The lock never blocks Floor 0 (it is not a road: `mayEnter` is true for a null polarity) and retreating changes no state. |
| "A colossal boss strictly gates every 10th floor in both directions." | A sector has `perRegion` (10) floors and its Guardian's arena is the last of them: `FloorBands` makes the arena floor +-10, +-20 ... |
| (system 3 and 6) "Clear 5 rooms on this floor", "floor threat level" | A floor holds about 13 rooms under the default rule, and `FloorMap.threat(floor)` is the distance from Floor 0; the quests and the Dungeon Master read floors from `Room.floor`. |

## 3. The classes and files, and WHY each exists

* **`FloorBands`** (pure). Turns a sector's room graph into floor bands and the bands into signed floors. WHY from graph depth: a room says nothing about
  how far along a sector it is, and the only honest measure the generated graph offers is how many doorways a walker needs from the Landing. WHY two
  strategies: see section 4. WHY pure: the odd sectors a generator can produce (no arena, an unreachable room, loops, a one-room sector) must be pinned by
  tests, and the answer must depend on the graph alone, never on the order the rooms or edges are listed. It also holds `floors` (band to signed floor:
  the road's sign and the sector's place in the chain, so the Abyssal road counting down is pinned by a test) and `lengthFor` (how long a sector that is about
  to be numbered is).
* **`FloorAssigner`** (a `SectorListener`). Writes `Room.floor` when the generator announces a finished sector. WHY a listener: the one moment the whole
  graph is known is the end of generation, and the numbers are saved with the sector, so nothing is recomputed per player or per tick. WHY it also has
  `ensureBanded` and `numberOldSectors`: a world saved by 1.54.0 has no numbers, and a generator that forgot to announce a sector would leave none; the
  server numbers such sectors when it starts, and the tracker numbers one on first contact, instead of reading it as one endless first floor.
* **`PlayerDungeonAlignment`** (pure). The state: road, lock, lock time, deepest floor on each road. WHY a plain class: the rules that change it are pure and
  tested, and the codec needs only its five primitives, so the save can be pinned by a test. WHY the setters are package-private: only the rules and the
  admin command may change it, so no module can leave it half-edited.
* **`AlignmentRules`** (pure). Every move of the lock as a function: `mayEnter`, `onFloorEntered`, `clearsPrevious`, `isNewDeepest`, `onFloorCleared`,
  `shouldCastOut`, `choose`, `lock`, `unlock`, `reset`. WHY one pure class: the same rule must hold for the Hub's gates, the chain travel, the tracker,
  the hard guard and the command, and it can lock a player out of half the game, so it is written once and pinned move by move.
* **`AlignmentAttachments`**. Registers the attachment and its codec. WHY an attachment and not a capability: NeoForge 1.21 capabilities are for asking a
  block, item or entity for behaviour (`IItemHandler`); data that belongs to a player, must be saved and must survive death is a data attachment
  (`copyOnDeath`). WHY a codec with every field optional: an older, hand-edited or damaged save must still load. WHY the default state is not saved: a
  world of players who never touched the dungeon stays small.
* **`AlignmentNet`** (`nusmp:alignment_sync`). Server-to-client packet with the road, the lock and the two deepest values. WHY our own packet: NeoForge
  sends no attachment to the client by itself, and the contract fixed the id and the sync points (login, respawn, dimension change, every change).
  WHY `optional()`: a client without the mod can still join. WHY it skips stand-in players: they have no connection.
* **`ClientAlignment`** (+ optional **`FloorsClient`**). Plain-Java client holder, swapped in whole as one immutable snapshot. WHY: a future gate hint or window
  line needs the road and only the server knows it; WHY immutable: the network thread writes and the render thread reads and must never see the new road
  with the old lock; WHY it answers "free" until the first packet: a server without the module must behave like 1.54.0. `FloorsClient` clears it on logout
  (it names a client event, so only the client initialiser calls it).
* **`LockPolicy`** (an `AlignmentPolicy`). The answer behind `MegaDungeon.mayEnter`. WHY an interface: three doors written by three builders must obey one
  rule without importing the alignment code. WHY ops bypass (config `opsBypass`): so an admin can test both roads. WHY the refusal speaks at most every
  two seconds: a gate block asks again every tick a player stands in it. WHY it fails open: a bug in the lock must never trap anybody.
* **`HardLock`**. Once a second, a locked non-op player standing in a sector of the OTHER road is told why and sent out with
  `LabyrinthTravel.leaveAny`. WHY: the doors cannot see an operator's teleport, a relic, a command block, an old Gate or Maw, and without a backstop the
  lock would be a suggestion. WHY `shouldCastOut` is separate from `check`: a test can ask the question about a player that cannot be teleported, and the
  tracker asks it too (below).
* **`FloorTracker`**. Every `trackEveryTicks` it finds the floor under each player in the Mega-Dungeon and announces a change. WHY here and nowhere else:
  the lock, the System overlay's quests, the Rukh flow and the achievements all need the same moment, from one place. WHY it does not call
  `MegaDungeon.floorAt` (section 4). WHY it skips a player the guard is about to send out: so a teleport the lock undoes within a second cannot earn floor
  events or quest credit on a road that is closed to them.
* **`FloorsCommand`**. `/multiverse mega floors ...` (section 9). WHY: the only way to see the numbering matches the map and to repair a player who locked
  themselves out, without a game's worth of walking.
* **`FloorsConfig`**. `nusmp-labyrinth-mega-floors-server.toml` (section 8). WHY its own file: no two builders edit one spec, and the lock can be switched off
  alone.
* **`FloorsText`**. Every sentence as a translation key with an English fallback. WHY: wording changes without code, and it reads fine before the language
  fragment is merged. Every translatable argument is a string, a number or a component: the game cannot send any other kind to a client.
* **`FloorsSelfTests`**. The seven in-game tests (section 11). WHY: nothing here can be played by its author.
* **`FloorsRegistry`**. The wiring `MegaSystem` already calls. WHY: the lead adds no other line for the code.
* **Tests** (`src/test/java/.../mega/floors`): `FloorBandsTest`, `AlignmentRulesTest`, `ClientAlignmentTest`. WHY: pure logic is where a silent mistake would
  turn into a wrong floor or a wrongly closed road.

## 4. The decisions and their WHY

* **Floors from graph depth, not from markers or sectors.** See `mega_floors.md` section 3: a floor is a band of about ten rooms of a sector. The boss
  chain (arena, antechamber, vault, Way Onward) is the last band whatever its depth (a loop can make the vault close to the Landing, but the Crucible
  belongs to the boss floor); link tunnels are band 1; unreachable rooms are the last band; the Landing is always band 1. These rules are common to both
  strategies below.
* **The default cut is by RANK (`even`), the brief's literal rule is `arena`.** The brief's rule is: depth over the arena's depth, so depths `0..A-1` are
  spread over bands `1..9` as `1 + depth * 9 / A` and everything as deep as the arena is the last band. It is exact only when the arena is at least nine
  doorways from the Landing. The assembler does not promise that: it fills a region with about a hundred rooms and many loops, so it measured, over 200
  simulated sectors (the assembler of the CORE builder over its stand-in catalog; 123 rooms per sector, the arena 5.4 doorways from the Landing on average,
  the deepest room 10.4):

  | floor | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
  |---|---|---|---|---|---|---|---|---|---|---|
  | rooms per floor, `arena` rule | 1.0 | 5.8 | 1.2 | 10.4 | 4.7 | 11.0 | 7.1 | 16.3 | 0.1 | 65.5 |
  | rooms per floor, `even` rule | 14.1 | 13.1 | 13.2 | 13.1 | 13.1 | 13.1 | 13.2 | 13.1 | 13.1 | 4.0 |

  With the literal rule 3.6 of the nine early floors are empty in an average sector and more than half of the rooms are "floor 10", which makes the
  brief's "clear 5 rooms on this floor", "Floor +3 is harder than +2" and "a floor is about ten rooms" meaningless. The `even` rule keeps every promise the
  literal rule makes (the Landing is floor 1, the boss chain alone is floor 10, a deeper room is never on a lower floor than a shallower one, the answer
  depends on the graph alone) and gives every floor about 13 rooms: the plain rooms are sorted by (doorway depth, room id) and cut into nine runs of
  equal size (they differ by at most one room). The price: two rooms at the same depth may land on neighbouring floors (the cut can fall inside a depth
  level; the room id is placement order, so siblings stay together). `floors.banding = "arena"` switches back to the literal rule; nothing else changes.
  The picture `floors_compare_rules.png` (section 14) shows the table above. A real catalog may shape sectors differently (the boss-chain templates were
  not merged when this was measured): the `even` rule needs no particular shape, which is the other reason it is the default.
* **The road follows the last floor entered while the lock is open (a refinement of "the first floor entered sets the road").** The owner's rule is "once a
  player CLEARS Floor +1 or -1, their alignment locks". If the first road stuck, a player could step onto +1, back out, take the Abyssal road and clear all of
  it without ever locking (clearing -1 would not count, because their road is Holy): the loophole the lock exists to close. So while unlocked the road is
  whichever the player is on, and clearing a floor of it locks. The pure test `aDipIntoTheOtherRoadDoesNotEscapeTheLock` pins this (and a mutation that
  restores the literal rule is caught). `floors set` makes an operator's choice stick only until the player steps onto the other road again; `floors lock`
  is the way to fix it.
* **"Cleared" means moving on to a deeper floor of the same road, once per player and floor.** That is the contract's definition. "Once" is derived from the
  deepest distance (a floor the player was already below has been cleared before), so no extra field is saved. A jump from +1 to +4 clears +1 only: +2 and
  +3 were never entered. A jump to the other road clears nothing.
* **`floors.lockAtFloor`.** Default 1 (the owner's rule). With 3, the lock closes when floor 3 is cleared and until then both roads can be tried; the
  first road to reach it wins.
* **The first floor of a road only chooses.** Stepping onto +1 or -1 is not a commitment; moving on is. So the Hub can show the choice, and a player who
  looks into the wrong tower can walk back out.
* **Deepest is kept per road.** The two roads are different places: a player locked Holy who was once forced onto the Abyssal road by an operator has been
  to depth 3 there and depth 12 here, and "have they already been below floor 3 on this road" has to be answerable for each.
* **The length of a sector (`perRegion`) is stored per sector and inherited along a chain** (`FloorBands.lengthFor`). Floors are counted
  `chainIndex * perRegion + band`. In order: another numbered sector of the same road wins (a 5-floor sector after a 10-floor one would reuse floors +6..+10,
  and the lock and the decennial rule would break); else the length the sector already carries wins when it is not the constructor's default 10 (a sector
  that was regenerated has new, unnumbered rooms but keeps its length, so a config change never renumbers it); else the config's `perRegion`. (The brief said
  "from the config at the first assignment"; this is the same, plus the rules that keep a chain consistent.) Other modules must read `Sector.perRegion` /
  `FloorMap` with the sector's value, never the config's. One edge is left: a lone sector that was numbered with the default 10 and is regenerated after the
  config changed to another length is numbered with the new length (nothing else of that road exists to disagree with it).
* **The tracker does not call `MegaDungeon.floorAt`.** That method answers the sector's FIRST floor for a position that is inside a sector but in no room
  (a door-cap alcove, the air above the rooms): a player stepping into a cap on floor +7 would "go down" to +1 and back, two false floor changes (and a
  failed quest). The tracker looks the room up itself and keeps the floor when there is none. A position in no sector keeps the floor too (a link tunnel).
* **A player the hard guard is about to send out is not tracked.** `FloorTracker.check` returns early when `HardLock.shouldCastOut` is true, so no floor
  event and no quest credit comes from a road that is closed to them.
* **Leaving the dimension is the retreat to Floor 0.** The remembered floor is dropped on a dimension change, logout and respawn, and nothing is fired.
  Nothing is saved for it: the floor under a player is a fact of where they stand.
* **Old worlds are numbered when the server starts.** A sector saved by 1.54.0 has rooms with floor 0; `ServerStartedEvent` numbers every finished sector
  that has no numbers (`FloorAssigner.numberOldSectors`), so `MegaDungeon.floorAt` is right from the first tick. The tracker's lazy numbering on first
  contact remains as the safety net.
* **The hard guard is a backstop, not the lock.** The doors enforce the lock; the guard catches everything that does not go through a door. It is
  switchable (`hardLock`) because an owner might prefer to warn only.
* **Fail open.** A bug in the lock must never trap a player, so an exception in the policy answers "yes", and the guard catches and logs.

## 5. Data

| Where | Key | Meaning |
|---|---|---|
| `Room` (saved with the sector, `MegaData`) | `fl` | The signed floor (`+1..+10` Holy, `-1..-10` Abyssal, +11.. for the next sector). Absent = never numbered. |
| `Sector` (saved) | `fpr` | Floors per sector, fixed at the first numbering. |
| Player attachment `nusmp:dungeon_alignment` (saved with the player, copied on death) | `chosen`, `locked`, `holy`, `abyssal`, `lockedAt` | Road id (`holy` / `abyssal`, omitted when none), lock, deepest distance on each road (0 omitted), game time of the lock. Every key optional; the whole attachment is omitted while still the default. |
| Packet `nusmp:alignment_sync` (server to client) | utf(<=16) road id or "", bool locked, var-int holy, var-int abyssal | The lock time is not sent. |
| Runtime only (never saved, cleared on logout, dimension change, respawn and server stop) | UUID to floor in `FloorTracker`; UUID to tick in `LockPolicy` and `HardLock` | |

## 6. The state table of the lock

Road = `chosen`. "Open" = not locked. Floor 0 is not on any road and going back to it never changes anything. (`lock_moves.png`, section 14, shows the same table.)

| State | Event | Result |
|---|---|---|
| any | enter a floor of road R at distance d | `deepest(R) = max(deepest(R), d)` |
| open (road none or any) | enter a floor of road R | road = R (not binding yet) |
| locked (road X) | enter a floor of road R (operator, teleport) | `deepest(R)` updated; road and lock unchanged |
| open | move on from floor f of R to a deeper one | f cleared; if `|f| >= lockAtFloor` and (road none or R): road = R, LOCKED |
| locked (road X) | ask to take road X | allowed |
| locked (road X) | ask to take the other road | refused (message and dull sound, at most every 2 s), fires nothing |
| open | ask to take either road | allowed |
| locked, standing on the other road, hard guard on, not an operator | one second passes | told why, sent out with `LabyrinthTravel.leaveAny`; until then not tracked |
| any | `lockEnabled = false` | `mayEnter` is always true; nothing ever locks |
| any | `floors reset` | no road, open, both deepest 0 |

## 7. The order of events (one change of floor)

`FloorTracker.check(p)` finds floor `f` (a locked player on the closed road is not looked at); if it differs from the remembered `previous`:

1. ask `isNewDeepest` and `clearsPrevious` (both BEFORE the state learns of the move);
2. `MegaDungeon.fireFloorEntered(p, f, previous)` (`previous` is 0 when they came from outside the dungeon);
3. `AlignmentRules.onFloorEntered` (deepest, road);
4. if it clears the previous floor: `MegaDungeon.fireFloorCleared(p, previous)`, then, when the lock is enabled, `AlignmentRules.onFloorCleared`;
5. quest `mega.floor` (argument `FloorMap.label(f)`) the first time this depth is reached on that road;
6. if the lock newly closed: a message, a sound, quest `mega.aligned` (argument the road id);
7. sync the client when anything changed.

## 8. Config (`world/serverconfig/nusmp-labyrinth-mega-floors-server.toml`, section `[floors]`)

| Key | Default | Range | Meaning |
|---|---|---|---|
| `lockEnabled` | true | | The Directional Lock. false = both roads always open, nobody is ever locked. |
| `lockAtFloor` | 1 | 1..10 | The floor that must be cleared on a road before it locks. |
| `hardLock` | true | | The once-a-second backstop that sends a locked player off the other road. |
| `opsBypass` | true | | Operators (permission 2+) pass the lock and the guard. |
| `perRegion` | 10 | 2..20 | Floors per sector for a road that has no numbered sector yet (section 4). |
| `banding` | "even" | "even" or "arena" | How a sector's rooms are cut into floors (section 4). Read when a sector is numbered; `floors assign <sector>` numbers one again. |
| `trackEveryTicks` | 10 | 5..100 | Ticks between two looks at the floor under each player. |

## 9. Commands (permission 2; the lead attaches `FloorsRegistry.command()` under `/multiverse mega`)

* `floors here`: your floor, band, road, sector and room, whether it is a boss floor, and the floor the tracker holds for you.
* `floors player <target>`: road, lock, deepest on each road, what the lock would let them do, whether they are an operator.
* `floors set <target> <holy|abyssal>`: choose the road (the lock is unchanged; see section 4 for how it can be undone by walking).
* `floors lock <target>` / `floors unlock <target>` / `floors reset <target>`.
* `floors bands <sector>`: the numbering of a sector, one line per floor with its rooms as `#id:kind@depth` (and the arena depth).
* `floors assign <sector>`: number the sector again (use after changing a template, the banding or the config while testing).

## 10. For the other modules

* Read a player's alignment: `AlignmentAttachments.get(player)` (live object: use `AlignmentRules`, never write fields).
* Ask whether a road is open: `MegaDungeon.mayEnter(player, polarity)` (the contract's door; it tells the player why).
* Hear the floors: `MegaDungeon.addFloorListener(...)`. `onFloorEntered(player, floor, previous)` fires on every change (previous 0 after a login or from
  outside); `onFloorCleared(player, floor)` once per player and floor.
* The floor of a place: `Room.floor` once the sector is numbered; `FloorTracker.floorAt(level, pos)` (0 for no room; numbers an old sector on demand);
  `FloorTracker.currentFloor(player)`.
* Order: the floor assigner is registered by `FloorsRegistry.init`, which `MegaSystem` calls after the wave-1 registries, so listeners registered before it
  (ECO's faction assigner) hear `onSectorGenerated` before the rooms are numbered. None of them reads a floor then (checked). A module that must read
  `Room.floor` inside its own `onSectorGenerated` and is registered earlier calls `FloorAssigner.assign(sector)` first (public, static, idempotent), or the
  lead moves the `FloorsRegistry.init` line up to just after `MegaGenRegistry.init`.
* Quest keys fired (through `NusmpEvents.fire`): `mega.floor` (argument: the signed floor, e.g. `+4`, the first time that depth is reached on a road) and
  `mega.aligned` (argument: `holy` or `abyssal`, when the lock closes).
* `FloorsSelfTests` registers one permanent, do-nothing `FloorListener` for its tracker test; it ignores everyone but its own stand-in player.

## 11. Tests

Unit tests (`./gradlew test`; pure, no game), 58 in all:
* `FloorBandsTest` (32): the brief's rule (a 30-deep chain uses every band and the arena is the last, breadth-first depths, a shortcut, a star, no arena, an
  unreachable arena, an arena in the Landing's cell, a room called -1 is not "no arena", an orphan, links and the boss chain, the Landing, shorter regions,
  damaged input); the even rule (an exact small example, one room per floor, a few rooms spread over the whole sector, a shallow dense sector where the
  arena rule leaves half the floors empty and the even rule fills every one, the special rooms, an unreachable arena, one- and two-floor sectors, and over
  seeded random graphs: every band in range, a deeper room never on a lower floor, runs of equal size, order independence); the signed floors (the Abyssal
  road counts down, later sectors of a chain continue, no Landing); the length of a sector; the config words; and over random graphs the properties of the
  brief's rule (every band in range, order independence, bands never fall along a shortest path).
* `AlignmentRulesTest` (22): the whole truth table of `mayEnter`, the owner's story, the Abyssal mirror, the retreat, clearing once, a jump (also to the
  other road), the sector boundary, a later lock, the dip loophole, a locked player on the wrong road, an operator's `choose` (also on a locked player), a lock
  time never negative, what counts as the default state, reset, the save round trip and a damaged save.
* `ClientAlignmentTest` (4).

Mutation check (`scratchpad/mfl_mutate2.py`, 60 mutants of the production code: wrong band for links, no Landing rule, a wrong breadth-first step, a reversed
or unstable rank, a broken even formula, a missing one-floor guard, the length rules, `>=` for `>`, the first-road-sticks rule, an inverted `mayEnter`, a lock
without a road, a missing reset field, a lock time that can be negative, `equals` ignoring a field, a client that keeps a damaged packet, and so on): the
first run left four survivors (a room called -1 read as "no arena", a jump to a deeper floor of the other road counted as a clear, `equals` ignoring the
lock flag, `isDefault` ignoring the Abyssal record, which would have lost that record on save); each exposed a missing test, which was added; all 60 are now
killed. A type-check negative control (a deliberately misspelt method and a wrong argument type in a copy of `HardLock`) is reported by the type-check.

In-game self-tests (`/multiverse mega selftest floors.<name>`; each skips with an OK line when the Mega dimension or sector 0 is missing):
`floors.bands` (sector 0 and the first sector of each road that exists: the Landing is the first floor, the arena the last, the boss chain the last, no
number outside the sector, no fall along a doorway, the rule of the configured banding holds, a second run changes nothing), `floors.old_world` (sector 0
stripped of its floors is numbered again by the start-up pass and by the tracker, to the same floors and the same length, and put back), `floors.alignment`,
`floors.save` (the real NBT codec, an empty tag, a damaged tag), `floors.sync`, `floors.tracker` (a stand-in walks from the Landing to the arena of each
tested sector: the events in order, the clear, the lock; no event for the same place or a place in no room), `floors.hard_lock` (the guard's decision for no
road, an open road, the own road and the other road, and that a player about to be cast out is not tracked).

## 12. Lead integration

* Config (NUSMP.java, next to the other mega configs): `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig.SPEC, "nusmp-labyrinth-mega-floors-server.toml");`
* Command: attach `com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.command()` under `/multiverse mega` (`megaNode.then(FloorsRegistry.command())`).
* Client (optional): `com.newuniverse.nusmp.labyrinth.mega.floors.FloorsClient.init();` in the `Dist.CLIENT` block.
* Lang: merge `scratchpad/mega/frag/mfl.lang.json` into `en_us.json` (39 keys under `message.nusmp.mega.mfl.*`).
* The attachment and the payload register themselves in `FloorsRegistry.init` (called by `MegaSystem`): no line in `NUSMP.java` is needed for them.
* The generator must call `MegaDungeon.generated(level, sector)` after every generation and every `regen` (the numbering runs from it; CORE does). If it
  does not, the server numbers the sectors when it starts and the tracker numbers one on first contact anyway.
* Quest keys to register in the questline tools: `mega.floor`, `mega.aligned`.

## 13. Not verified (honest list)

Nothing ran in game. Specifically unverified: (1) the room graphs of the REAL templates (the figures in section 4 come from the assembler over its stand-in
catalog; the boss-chain templates were not merged); the `even` rule needs no particular shape, but the in-game test `floors.bands` prints the rooms per
floor of the real sector 0 so it can be read in the CI log; (2) that the link tunnels between sectors are rooms of the NEW sector (they are banded 1, so a
link stored in the old sector would read as floor 1 there; the tracker keeps the floor in a tunnel that is in no sector); (3) the behaviour of `FakePlayer`
in the self-tests (no connection: messages and sounds are skipped for it, the sync is skipped, `NusmpEvents.fire` ignores it); (4) that
`AttachmentType.Builder.serialize(Codec, Predicate)` plus `copyOnDeath()` copies a non-default state on death exactly as the codec says (the codec itself is
tested by `floors.save`; the same builder chain is used by `DjinnAttachments`); (5) the Hub's way home: the hard guard calls `LabyrinthTravel.leaveAny`,
which takes a player back to where they ENTERED the Labyrinth dimensions from; if the Hub's builder records the Hub as that place, a cast-out player lands in
the Hub, which is the intent; (6) the wording and the dull sound; (7) behaviour with other mods that teleport players (the guard sees only where the player
is, not how they got there); (8) a client without the mod joining a server that has it (the payload is `optional()` and a failed send is caught and logged).

## 14. Pictures (for the owner)

In `scratchpad/previews/mfl/` (made from a simulated sector: the real `FloorBands` over a sector of CORE's assembler and its stand-in catalog, because no
real sector exists without a game):

* `floors_table_holy_sector0.png`, `floors_table_abyssal_sector0.png`: floor, rooms, what is in them, doorways from the Landing, threat, notes, for +1..+10
  and -1..-10.
* `floors_map_holy_sector0.png`, `floors_map_abyssal_sector0.png`: top-down plan of the sector, every room coloured by its floor.
* `floors_compare_rules.png`: rooms per floor under the two rules (section 4).
* `lock_moves.png`: the state table of the lock (section 6) as a picture.
* `floors_sheet.txt`: the two tables and the comparison as plain text.

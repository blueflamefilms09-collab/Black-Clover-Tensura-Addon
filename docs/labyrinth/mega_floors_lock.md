# Mega-Dungeon: the floors, the floor tracker and the Directional Lock (`mega.floors`, 1.55.0, builder FLOORS)

Package `com.newuniverse.nusmp.labyrinth.mega.floors`. **Nothing here was run in game.** The pure classes (`FloorBands`, `AlignmentRules`,
`PlayerDungeonAlignment`, `ClientAlignment`) are unit tested (JUnit, 40 tests, every rule mutation-checked, section 11); the game classes were type-checked
against the project's API stubs (rebuilt from the real API dump, with a negative control that proves the check can fail); six IN-GAME self-tests
(`floors.*`, section 11) are registered with the shared `SelfTests` registry so the CI server smoke test exercises the real thing. CI is the first real
compile. Section 13 is the honest list of what is unverified.

## 1. New, replacement, kept (owner rule 5)

| Part | Status | Notes |
|---|---|---|
| Floor numbers of the rooms of a sector (`Room.floor`), computed from the room graph | NEW | The contract added the field; this module is the only one that writes it. |
| The floor tracker (which floor a player stands on, `FloorListener` events) | NEW | The contract added the listener interface and the `MegaDungeon.fire*` hooks; this module fires them. |
| `PlayerDungeonAlignment` (the Directional Lock's data) | NEW | A NeoForge data attachment `nusmp:dungeon_alignment`, the 1.21 form of the brief's "capability". |
| The sync packet `nusmp:alignment_sync` and the client holder | NEW | |
| The lock policy (`AlignmentPolicy`) and the hard guard | NEW | The interface is the contract's; nothing existed behind it. |
| `/multiverse mega floors ...` | NEW | Attached by the lead. |
| `MegaDungeon.floorAt`, `polarityAt`, `Sector.perRegion`, `FloorMap` | KEPT | Used as they are. One quirk of `floorAt` is worked around, not edited (section 4). |
| The 1.54.0 sectors of an old world | KEPT | They have no floor numbers; they are numbered the first time a player stands in them (section 4). |
| `LabyrinthTravel` | KEPT | Called (`leaveAny`), never edited. |
| Nothing was replaced and nothing was removed. | | |

## 2. The brief, sentence by sentence

| The owner wrote (brief 3, system 2 and the introduction) | Where it is |
|---|---|
| "Write the capability `PlayerDungeonAlignment`." | `PlayerDungeonAlignment` + `AlignmentAttachments` (attachment `nusmp:dungeon_alignment`). |
| "Once a player clears Floor +1 or -1, their alignment locks." | `FloorTracker` asks `AlignmentRules.clearsPrevious` / `onFloorCleared` when a player moves on from a floor; at `floors.lockAtFloor` (default 1) the lock closes. |
| "They cannot enter the opposite polarity's staircases/portals." | `LockPolicy`, registered behind `MegaDungeon.mayEnter`: the Hub's gates and the chain travel ask it. |
| "once they begin ascending or descending, a dimensional lock prevents them from switching directions (though they can always retreat to Floor 0)" | The lock never blocks Floor 0 (it is not a road: `mayEnter` is true for a null polarity) and retreating changes no state. |
| "A colossal boss strictly gates every 10th floor in both directions." | A sector has `perRegion` (10) floors and its Guardian's arena is the last of them: `FloorBands` makes the arena floor +-10, +-20 ... |

## 3. The classes and files, and WHY each exists

* **`FloorBands`** (pure). Turns a sector's room graph into floor bands. WHY from graph depth: a room says nothing about how far along a sector it is, and
  the only honest measure the generated graph offers is how many doorways a walker needs from the Landing. Banding that depth (`FloorMap.bandOfDepth`)
  makes the Landing floor 1 and the arena floor 10, keeps every floor connected to the one before it, and needs no marker in any template. WHY pure: the
  odd sectors a generator can produce (no arena, an unreachable room, loops, a one-room sector) must be pinned by tests, and the answer must depend on the
  graph alone, never on the order the rooms or edges are listed.
* **`FloorAssigner`** (a `SectorListener`). Writes `Room.floor` when the generator announces a finished sector. WHY a listener: the one moment the whole
  graph is known is the end of generation, and the numbers are saved with the sector, so nothing is recomputed per player or per tick. WHY it also has
  `ensureBanded`: a world saved by 1.54.0 has no numbers, and a generator that forgot to announce a sector would leave none; the tracker numbers such a
  sector on first contact instead of reading it as one endless first floor. WHY it takes the length of the chain's other sectors: see section 4.
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
  lock would be a suggestion. WHY `shouldCastOut` is separate from `check`: a test can ask the question about a player that cannot be teleported.
* **`FloorTracker`**. Every `trackEveryTicks` it finds the floor under each player in the Mega-Dungeon and announces a change. WHY here and nowhere else:
  the lock, the System overlay's quests, the Rukh flow and the achievements all need the same moment, from one place. WHY it does not call
  `MegaDungeon.floorAt` (section 4).
* **`FloorsCommand`**. `/multiverse mega floors ...` (section 9). WHY: the only way to see the numbering matches the map and to repair a player who locked
  themselves out, without a game's worth of walking.
* **`FloorsConfig`**. `nusmp-labyrinth-mega-floors-server.toml` (section 8). WHY its own file: no two builders edit one spec, and the lock can be switched off
  alone.
* **`FloorsText`**. Every sentence as a translation key with an English fallback. WHY: wording changes without code, and it reads fine before the language
  fragment is merged. Every translatable argument is a string, a number or a component: the game cannot send any other kind to a client.
* **`FloorsSelfTests`**. The six in-game tests (section 11). WHY: nothing here can be played by its author.
* **`FloorsRegistry`**. The wiring `MegaSystem` already calls. WHY: the lead adds no other line for the code.
* **Tests** (`src/test/java/.../mega/floors`): `FloorBandsTest`, `AlignmentRulesTest`, `ClientAlignmentTest`. WHY: pure logic is where a silent mistake would
  turn into a wrong floor or a wrongly closed road.

## 4. The decisions and their WHY

* **Floors from graph depth, not from markers or sectors.** See `mega_floors.md` section 3: a floor is a band of about ten rooms of a sector. The arena
  (depth `A`) is the last band; depths `0..A-1` are spread over bands `1..9` as `1 + depth * 9 / A`; the boss chain (arena, antechamber, vault, Way
  Onward) is the last band whatever its depth (a loop can make the vault close to the Landing, but the Crucible belongs to the boss floor); link tunnels
  are band 1; unreachable rooms are the last band; the Landing is always band 1.
* **Where the rooms of a sector are close to the arena, floors are wide.** If the arena is only 3 doorways from the Landing, bands jump by 3 per doorway
  (`FloorBands` is correct, the sector is just shallow). The CI self-test `floors.bands` prints the arena depth and the number of distinct floors of the
  real sector 0, so a sector that is too shallow to look like ten floors shows up as a low number there.
* **The road follows the last floor entered while the lock is open (a refinement of "the first floor entered sets the road").** The owner's rule is "once a
  player CLEARS Floor +1 or -1, their alignment locks". If the first road stuck, a player could step onto +1, back out, take the Abyssal road and clear all of
  it without ever locking (clearing -1 would not count, because their road is Holy): the loophole the lock exists to close. So while unlocked the road is
  whichever the player is on, and clearing a floor of it locks. The pure test `aDipIntoTheOtherRoadDoesNotEscapeTheLock` pins this. `floors set` makes an
  operator's choice stick only until the player steps onto the other road again; `floors lock` is the way to fix it.
* **"Cleared" means moving on to a deeper floor of the same road, once per player and floor.** That is the contract's definition. "Once" is derived from the
  deepest distance (a floor the player was already below has been cleared before), so no extra field is saved. A jump from +1 to +4 clears +1 only: +2 and
  +3 were never entered.
* **`floors.lockAtFloor`.** Default 1 (the owner's rule). With 3, the lock closes when floor 3 is cleared and until then both roads can be tried; the
  first road to reach it wins.
* **The first floor of a road only chooses.** Stepping onto +1 or -1 is not a commitment; moving on is. So the Hub can show the choice, and a player who
  looks into the wrong tower can walk back out.
* **Deepest is kept per road.** The two roads are different places: a player locked Holy who was once forced onto the Abyssal road by an operator has been
  to depth 3 there and depth 12 here, and "have they already been below floor 3 on this road" has to be answerable for each.
* **`perRegion` is stored per sector and inherited along a chain.** Floors are counted `chainIndex * perRegion + band`. A sector keeps the length it was
  first numbered with (a config change never renumbers it), and a sector of a road that already has a numbered sector takes THEIR length: a 5-floor sector
  after a 10-floor one would reuse floors +6..+10, and the lock and the decennial rule would break. (The brief said "from the config at the first
  assignment"; this is the same, plus the rule that keeps a chain consistent.) Other modules must read `Sector.perRegion` / `FloorMap` with the sector's
  value, never the config's.
* **The tracker does not call `MegaDungeon.floorAt`.** That method answers the sector's FIRST floor for a position that is inside a sector but in no room
  (a door-cap alcove, the air above the rooms): a player stepping into a cap on floor +7 would "go down" to +1 and back, two false floor changes (and a
  failed quest). The tracker looks the room up itself and keeps the floor when there is none. A position in no sector keeps the floor too (a link tunnel).
* **Leaving the dimension is the retreat to Floor 0.** The remembered floor is dropped on a dimension change, logout and respawn, and nothing is fired.
  Nothing is saved for it: the floor under a player is a fact of where they stand.
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

Road = `chosen`. "Open" = not locked. Floor 0 is not on any road and going back to it never changes anything.

| State | Event | Result |
|---|---|---|
| any | enter a floor of road R at distance d | `deepest(R) = max(deepest(R), d)` |
| open (road none or any) | enter a floor of road R | road = R (not binding yet) |
| locked (road X) | enter a floor of road R (operator, teleport) | `deepest(R)` updated; road and lock unchanged |
| open | move on from floor f of R to a deeper one | f cleared; if `|f| >= lockAtFloor` and (road none or R): road = R, LOCKED |
| locked (road X) | ask to take road X | allowed |
| locked (road X) | ask to take the other road | refused (message and dull sound, at most every 2 s), fires nothing |
| open | ask to take either road | allowed |
| locked, standing on the other road, hard guard on, not an operator | one second passes | told why, sent out with `LabyrinthTravel.leaveAny` |
| any | `lockEnabled = false` | `mayEnter` is always true; nothing ever locks |
| any | `floors reset` | no road, open, both deepest 0 |

## 7. The order of events (one change of floor)

`FloorTracker.check(p)` finds floor `f`; if it differs from the remembered `previous`:

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
| `trackEveryTicks` | 10 | 5..100 | Ticks between two looks at the floor under each player. |

## 9. Commands (permission 2; the lead attaches `FloorsRegistry.command()` under `/multiverse mega`)

* `floors here`: your floor, band, road, sector and room, whether it is a boss floor, and the floor the tracker holds for you.
* `floors player <target>`: road, lock, deepest on each road, what the lock would let them do, whether they are an operator.
* `floors set <target> <holy|abyssal>`: choose the road (the lock is unchanged; see section 4 for how it can be undone by walking).
* `floors lock <target>` / `floors unlock <target>` / `floors reset <target>`.
* `floors bands <sector>`: the numbering of a sector, one line per floor with its rooms as `#id:kind@depth` (and the arena depth).
* `floors assign <sector>`: number the sector again (use after changing a template or the config while testing).

## 10. For the other modules

* Read a player's alignment: `AlignmentAttachments.get(player)` (live object: use `AlignmentRules`, never write fields).
* Ask whether a road is open: `MegaDungeon.mayEnter(player, polarity)` (the contract's door; it tells the player why).
* Hear the floors: `MegaDungeon.addFloorListener(...)`. `onFloorEntered(player, floor, previous)` fires on every change (previous 0 after a login or from
  outside); `onFloorCleared(player, floor)` once per player and floor.
* The floor of a place: `Room.floor` once the sector is numbered; `FloorTracker.floorAt(level, pos)` (0 for no room; numbers an old sector on demand);
  `FloorTracker.currentFloor(player)`.
* Quest keys fired (through `NusmpEvents.fire`): `mega.floor` (argument: the signed floor, e.g. `+4`, the first time that depth is reached on a road) and
  `mega.aligned` (argument: `holy` or `abyssal`, when the lock closes).
* `FloorsSelfTests` registers one permanent, do-nothing `FloorListener` for its tracker test; it ignores everyone but its own stand-in player.

## 11. Tests

Unit tests (`./gradlew test`; pure, no game): `FloorBandsTest` (16: a 30-deep chain uses every band and the arena is the last, breadth-first depths, a
shortcut, a star, no arena, an unreachable arena, an arena in the Landing's cell, an orphan, links and the boss chain, the Landing, shorter regions, damaged
input, and over seeded random graphs: every band in range, order independence, bands never fall along a shortest path), `AlignmentRulesTest` (20: the
whole truth table of `mayEnter`, the owner's story, the Abyssal mirror, the retreat, clearing once, a jump, the sector boundary, a later lock, the dip loophole,
a locked player on the wrong road, an operator's `choose`, reset, the save round trip and a damaged save), `ClientAlignmentTest` (4). Mutation check: 22
deliberate breakages of the production code (a wrong band for links, no Landing rule, a wrong breadth-first step, `>=` for `>`, the first-road-sticks
rule, an inverted `mayEnter`, a lock without a road, a missing reset field, and so on) were each caught by at least one test; one survivor (`equals`
ignoring the lock time) exposed a missing test, which was added.

In-game self-tests (`/multiverse mega selftest floors.<name>`; each skips with an OK line when the Mega dimension or sector 0 is missing):
`floors.bands`, `floors.alignment`, `floors.save` (the real NBT codec, an empty tag, a damaged tag), `floors.sync`, `floors.tracker`, `floors.hard_lock`.

## 12. Lead integration

* Config: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.floors.FloorsConfig.SPEC, "nusmp-labyrinth-mega-floors-server.toml");`
* Command: attach `com.newuniverse.nusmp.labyrinth.mega.floors.FloorsRegistry.command()` under `/multiverse mega`.
* Client (optional): `com.newuniverse.nusmp.labyrinth.mega.floors.FloorsClient.init();` in the `Dist.CLIENT` block.
* Lang: merge `scratchpad/mega/frag/mfl.lang.json` into `en_us.json` (39 keys under `message.nusmp.mega.mfl.*`).
* The generator must call `MegaDungeon.generated(level, sector)` after every generation and every `regen` (the numbering runs from it). If it does not, the
  tracker numbers a sector on first contact anyway.

## 13. Not verified (honest list)

Nothing ran in game. Specifically unverified: (1) that a real sector's graph spreads into ten useful floors (the pure rule is tested on hand-made and random
graphs, not on generated sectors; `floors.bands` reports the arena depth and distinct floors of sector 0 in CI); (2) that the link tunnels between
sectors are rooms of the NEW sector (they are banded 1, so a link stored in the old sector would read as floor 1 there); (3) the behaviour of
`FakePlayer` in the self-tests (no connection: messages and sounds are skipped for it, the sync is skipped, `NusmpEvents.fire` ignores it); (4) that
`AttachmentType.Builder.serialize(Codec, Predicate)` plus `copyOnDeath()` copies a non-default state on death exactly as the codec says (the codec itself is
tested by `floors.save`); (5) the Hub's way home: the hard guard calls `LabyrinthTravel.leaveAny`, which takes a player back to where they ENTERED the
Labyrinth dimensions from; if the Hub's builder records the Hub as that place, a cast-out player lands in the Hub, which is the intent; (6) the wording
and the dull sound; (7) behaviour with other mods that teleport players (the guard sees only where the player is, not how they got there).

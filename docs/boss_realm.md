# The Boss Realm (bosses that are not dungeon bosses)

Goku (`goku_boss`) and Sinbad (`sinbad_boss`) stand on no dungeon floor, so they are not fought where somebody used an egg. They are fought in the **boss realm**: an arena of their own in the story-arena dimension (`anime_multiverse:arc_arena`) that the mod already had. Nothing new is registered as a dimension; the realm is a thin layer of doors, two new arenas and a small server watcher on top of the existing arena builder, referee and way home.

Status: written without a compiler and never played. CI compiles it; the pure tests were run locally (see "Tests").

## Who fights in the realm

A boss fights in the realm when it is a boss, has no dungeon floor (`floorMax` 0), is not one of the seven older bosses (`ExistingBosses`) and names no arena of its own. Today that is exactly **goku_boss** and **sinbad_boss**. A future boss added without floors joins by itself (it gets its saga's arena until it is given one).

Not in the realm, unchanged: every floor boss of the dungeon, the seven older bosses, and Lucius (Final Hour courtyard, the Hourglass's own dimension).

## The "they keep dying" bug: what was found

The theory was that Goku and Sinbad are set up as dungeon bosses and are killed or despawned by the dungeon rules. The code does not say that:

- Both have `floorMax` 0, carry no floor tags, and `ArcRoster.floorBosses` excludes them, so no dungeon spawner, floor scaler or floor despawn applies to them.
- No path was found that kills them unconditionally (searched: `ArcBoss`, `ArcSpawner`, `ArcSystem`, `ZogratisBoss` upkeep / leash / reset / hurt / die / remove, `BossRegistry`, `boss/rules/*`, `BossHealGuard`, the Goku and Sinbad classes).

What the code does show, for a boss that is not a dungeon boss, are these ways to lose the fight or the boss:

1. **The story-arena referee gives up an empty fight after 40 ticks (2 seconds)** and `discard()`s the boss (`ArenaState.EMPTY_GRACE_TICKS`). A party that dies, is knocked out of the site or disconnects loses the boss within 2 seconds.
2. **The arena is a floor in a void.** A boss knocked or blinked below the floor falls out of the world (void damage ignores invulnerability). Nothing put it back.
3. **In the open world** a boss that has been left alone for 10 seconds resets its fight and heals fully, and its leash (56 blocks) drags it back; in a forest, cave or on a hillside this looks like dying, vanishing or being stuck.
4. A fight that nobody ends would stand forever.

The root cause could not be proven to be one single line; this is stated plainly. The realm removes the places where it can happen: a fight in a purpose-built, closed arena (no cave, no hillside, no leash fighting the terrain), a configurable grace before the referee gives up, a rescue for a boss under the floor, a timeout, and a safe ride home for everyone. Please tell us what you see in the log (`[anime_multiverse]` lines are written when a boss is put back, a fight times out, or a site is given up) if a boss still dies.

## NEW / REPLACED / KEPT

NEW
- `arc/realm/BossRealmRules` (pure rules), `BossRealmConfig` (`nusmp-boss-realm-server.toml`), `BossRealm` (doors, clocks, once-a-second pass, login / death / stop listeners), `BossRealmSelfTests`.
- `arc/arena/SevenSeasIsleShape` (Sinbad: a palm island with a gold-runed dais in a sea of light-blue concrete, prismarine wave rings, sea-lantern lamps, seven dark-prismarine pillars, a prismarine-brick sea wall), `TournamentGroundsShape` (Goku: a raised white quartz ring on a tiled apron in a red wasteland with craters, rubble, six spires and a terracotta cliff), `RealmShapes` (their own registry).
- Tests: `BossRealmRulesTest`, `BossRealmSourceScanTest`; self-test keys `boss.realm.rules`, `boss.realm.shapes`, `boss.realm.door`.

REPLACED (only while `bossRealm.enabled` is true; false gives the old behaviour back)
- The spawn egg, `/multiverse arc summon <boss>`, `/multiverse goku_boss spawn` and `/multiverse sinbad_boss spawn` without a position now carry the party to the realm arena instead of making the boss where you stand. With an explicit position they still spawn in place.
- Goku's and Sinbad's arena is no longer their saga's arena; it is their own realm arena.

KEPT
- `ArenaShapes.all()` (the six story arenas, ring last) and every story arena's behaviour, the dimension, the builder, the referee, the Return Book, the Chronicle, the Final Hour, all dungeon bosses, both bosses' own settings, caps and gates (`GokuBosses.gate()` / `SinbadBosses.gate()` are asked before anyone is moved).
- Small edits to shared files: one line in `ArenaShapes.forCharacter`, two lines in `ArcArenaDirector.tickFighting` (the present count goes through `BossRealm.presentWithGrace`, which returns the real count for every story arena), three lines in `ArcSystem.init`, one door line each in the egg, the arc command and the two boss commands. `ArenaShapesTest` now runs its shape invariants over the six story arenas and the two realm arenas.

## The rules in plain words

- Using a door as a player (egg or command with no position) carries you and up to `partyCap - 1` players within `partyRadius` blocks to the boss's arena. The arena is built the first time it is used.
- If the boss is switched off or at its cap, nobody is moved and the door answers as it always did. If every realm arena is in use (`maxActiveSites`) you are told to wait. A short cooldown stops a spam of doors.
- The referee starts the fight (countdown, boss appears). If the whole party falls or disconnects, the fight survives `emptyGraceSeconds` (default 20) plus the referee's own 2 seconds, so a party that respawns and comes back finds the boss still there.
- A fight longer than `fightTimeoutMinutes` (30) ends: the boss withdraws, the arena rests.
- `returnSeconds` (45) after a fight ends (won, withdrawn, timed out) the people still in the arena are carried back to where they stood. A player who logs in inside a realm arena is carried home at once. A player who dies there respawns in the world and the saved way back is dropped. Items dropped in the arena stay for the vanilla 5 minutes; the arena can be re-entered meanwhile (a resting arena is not refused).
- A boss below the floor (y under the arena floor minus 12) is put back on its spot.

## Settings (`nusmp-boss-realm-server.toml`, server config)

| Setting | Default | Range |
|---|---|---|
| `bossRealm.enabled` | true | off restores the old behaviour |
| `partyCap` | 6 | 1-16 |
| `partyRadius` | 16.0 | 1-64 |
| `maxActiveSites` | 4 | 1-16 |
| `emptyGraceSeconds` | 20 | 0-120 |
| `returnSeconds` | 45 | 0-600 |
| `fightTimeoutMinutes` | 30 | 0-240 (0 = none) |
| `doorCooldownSeconds` | 5 | 0-120 |

## Worst-case cost (rule 11)

- State: one `Clock` (3 longs) per used site, at most 256; one door-cooldown entry per player who used a door, capped at 512, forgotten at logout; both cleared on server stop. Nothing is stored per tick or per entity.
- Server work: the realm pass runs once a second (`server.getTickCount() % 20`), exits at once when nobody is in the dimension and no clock exists, and does one player list walk plus, per fighting site, one entity lookup by UUID. A door does one bounded box look for companions (radius at most 64) and one teleport per party member (at most 16). No entity is spawned by the realm (the referee spawns the one boss), no block is written by it (the existing builder builds an arena once per site), no chunk is loaded or forced, no packet is sent by the realm (messages are the vanilla chat/actionbar ones to the party only).
- Builds: at most `maxActiveSites` realm arenas hold people at once; each arena is at most 80 x 80 columns built once by the existing synchronous builder, as for the story arenas (each under 60000 blocks, checked by `ArenaShapesTest`).
- Client: no client code was touched. The arenas are static blocks.
- `PerfProbe` is not wired into the pass (not done; the pass is bounded as above).

## Tests

- Pure: `BossRealmRulesTest` (boss list, shape ids, companions, door verdicts, clock grace / timeout / return, and the 2-second referee versus the realm grace), `ArenaShapesTest` (now eight shapes: clean stacks, closure walk, standable boss / arrivals / sigils, block names exist in 1.21.1, no water or lava, block budget, light), `BossRealmSourceScanTest` (no client classes, no static player, no spawns / block writes / chunk loads, doors and referee wired, server config with ranges). Run locally with javac and the JUnit console jar against stubs for the two classes that need NeoForge: 26 tests, all green.
- Self-test (CI server smoke test): add the prefix **`boss.realm`** to `SMOKE_EXPECT`; keys `boss.realm.rules`, `boss.realm.shapes`, `boss.realm.door`. They move nobody and spawn nothing.

## Honest caveats

- Not compiled and not played. `BossRealm`, `BossRealmSelfTests`, the config and the door hooks use NeoForge types and could only be checked against the signatures in the source. The arenas were checked only by the pure invariants, never looked at.
- The dying bug has no single proven cause (see above); the realm hardens the places where it can happen.
- A slot that was already built for Goku or Sinbad with their saga arena is rebuilt by the existing arena repair on the next entry (not tested).
- Operators lose "spawn it right here" for these bosses unless they give a position.
- Dungeon-less future bosses join the realm automatically and use their saga's arena until a shape is added to `RealmShapes` and `BossRealmRules.shapeId`.
- Not wired: a portal block (the doors are the egg and the commands), `PerfProbe`, lang keys (messages use `translatableWithFallback`).

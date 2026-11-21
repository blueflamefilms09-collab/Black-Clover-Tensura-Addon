# Boss AI: bosses fight the minute a player is there (engage rule)

Owner request: "fix all my bosses' AI so they actually fight back the minute a player is there, and they don't just stand there and watch or look at the player."

Honest limit up front: Minecraft cannot be run in this session. The diagnosis below comes from reading the code, the fix is checked by unit tests, source-scan tests and
a server self-test that builds every boss, and none of it has been watched in a game. Line numbers are those of the code before this change.

## What was found (why they stood and watched)

The whole Zogratis framework (every arc boss, Lucius, Dante, Vanica, Zenon, the Devil Avatar, Goliath, the Tier List, Goku, Kakashi, Sinbad, Ban, Spade, Midnight Sun, the
decennial bosses) fights only when it HAS A TARGET. The fight (`fightActive`, the fighters list, the move chooser, the chase goal) starts inside `customServerAiStep` only
in the branch `valid = target != null && target.isAlive()` (`entity/zogratis/ZogratisBoss.java` lines 576 to 584; `startFight` is called there and nowhere else). The only
ways to get a target were:

1. `NearestAttackableTargetGoal<>(this, Player.class, true)` (`ZogratisBoss.java:391`, same line in `entity/ZagredBossEntity.java:162`,
   `FloorWardenEntity.java:106`, `CharybdisSpawnBoss.java:142`). The `true` is "must see": a player behind a wall, a pillar, a door, in a dark cave mouth or a build, or in
   another arena room is never taken. Vanilla also shrinks the range with the player's visibility (a sneaking player to 80%, an invisible one with no armour to about 7%,
   a mob head to 50%), rolls a one-in-ten chance per check, and DROPS the target again after 60 ticks without a line of sight (the goal's `stop()` sets the target to null,
   and `customServerAiStep` then cancels a cast in progress).
2. `HurtByTargetGoal`: only after the boss was hit.

With no target the boss has nothing to do but `LookAtPlayerGoal` (priority 7, range 24 in `ZogratisBoss.java:388`, 16 in Zagred, 12 in the Floor Warden) and
`RandomLookAroundGoal`: it turns its head to the nearest player and stands there. That is exactly "just looks at the player". The look goal also stayed eligible during a fight (it only needs the LOOK flag), so it could turn the head to the nearest player instead of the target (not verified in a game).

Second cause, inside a fight: the chase goal `Positioning` (`ZogratisBoss.java` 395 to 437) keeps the "band" the ready moves want. Casters with a long band (Merlin 8 to 14,
Gowther 8 to 16, Morris 7 to 14, Zagred act I 6 to 13, Baal and Focalor 8+) strafe at that range; when no move is usable (all on cooldown, or a boss override such as Kakashi's
`chooseMove` returning -1) it only strafes. And `getNavigation().moveTo(t, 1.0)` simply fails when the path is longer than the follow range (48), through a very big hitbox or
over void, leaving the boss standing still while "chasing".

Per class (read, not guessed):

| Boss | What was found | Change |
|---|---|---|
| `ZogratisBoss` and every subclass (Lucius, Dante, Vanica, Zenon, Devil Avatar, Goliath, all `ArcBoss`: Goku, Kakashi, Sinbad, Ban, Meliodas, Escanor, King, Diane, Merlin, Gowther, Lucifero, Morris, Midnight, Zagred (arc), DanMachi, Tier List, Early) | causes 1 and 2 above; Goku, Kakashi, Sinbad, Ban, King, Diane, Merlin, Gowther, Midnight, DanMachi, Tier bosses add no targeting of their own and only override `preferredRange` and `chooseMove`, so they all inherit the fix | engage goal, look goals stand down, chase fallback, straight walk when no path |
| `arc/goku/GokuBoss`, `arc/kakashi/KakashiBoss`, `arc/magi/SinbadBoss` | nothing of their own that waits for a trigger: Goku scans players only to size his health (`scan`), Kakashi's "observe first" (360 ticks) still allows clones, kunai, wire, sabre, mist and wall moves, Sinbad only picks a range. `GokuBoss.java` is not edited | none needed beyond the framework |
| `dungeon/floor/charybdis/CharybdisSpawnBoss` | replaces `registerGoals` with only the two vanilla target goals (no chase goal, it swims by `Swim`), so it had the same line-of-sight gate | engage goal added (not while it rises) |
| `labyrinth/seal/FloorWardenEntity` and `LabyrinthGuardianEntity` (extends it) | `NearestAttackableTargetGoal(10, mustSee true)` and a look goal at 7 | engage goal added, look goals stand down |
| `entity/ZagredBossEntity` (legacy Zagred) | same pattern | engage goal added, look goals stand down |
| `relics/order/GodOfOrder` | the judge: `registerGoals` removes the nearest-player goal on purpose, it answers a provocation only | KEPT neutral by design (`engagesOnSight()` false). Owner decision if the God of Order should hunt too |
| `LuciusBoss` Messiah act | `canWalk()` is false while the arc director holds him still (levitating, channelling). By design; he casts from where he floats | kept; if he should walk and melee during act 1, say so |
| Dungeon creatures (Direwolf, Basilisk, Living Armor) | the dungeon's food web: an apex creature sleeps when full and hunts players only when hungry or when the player is near its den (`DungeonGoals.Sleep`, `HostileToPlayers`). Not counted by `BossIdentity`; ecosystem on purpose | kept (item 9 of the brief: retaliating or hunting by design) |
| `DevilTrialEntity` | a duel: its target filter only accepts the challenger | kept |
| Tagged mobs (`nusmp_juggernaut`, `nusmp_peak_boss`) | nothing in the code spawns a Tensura body with those tags; the Peak Altar's roster entries are mod bosses (framework) or other mods' bosses | other mods' bosses are not touched |

Not found: no boss uses NoAI, an intro cutscene gate, or an arena-wait trigger. The Gates cutscene (`PowerCurve.TRANSFORM_TICKS`) and Light Yagami's chip and pen scenes are short, bounded combat moves, not waits for a trigger.

## What changed

NEW (all behind `bossRules.aggroOnSight`, default true; false = the old behaviour exactly):

* `boss/ai/BossEngage.java` (pure, unit tested): the rule. A player is a target when alive, not spectator, not creative, within `aggroRange`; within 12 blocks (or in striking reach)
  no line of sight is needed; farther out a line of sight or a walkable path is needed. Nearest first, at most 10 players read, at most one path question.
* `boss/ai/BossEngageGoal.java`: the goal on a mob (target flag, priority 0). It does nothing while the boss has a living fair target (so `HurtByTargetGoal`, the vanilla goal and
  the Zagred threat swap keep working). Otherwise at most once per 10 ticks it takes one box query, and sets the target. The fight starts on that same tick (`fightActive`,
  fighters, the move chooser, the chase goal). Also `lookAt` and `lookAround`: the look goals, which now stand down while the boss has a target.
* `ZogratisBoss`: `attachEngage()` in `registerGoals`; `engagesOnSight()` (false only for the God of Order); `mayPickTarget()`; the engage goal ignores a player more than 88 blocks
  from the arena (the boss's own 90-block leash would drop the target every second otherwise).
* `ZogratisBoss`: chase fallback. `lastCast` is the tick of the last move started. When the move chooser returns nothing and 80 ticks (4 seconds) have passed, the band becomes 1.5 to
  3.5 blocks: the boss walks up and uses its plain blow instead of waiting. When a path to the target cannot be made, the boss walks straight at it (move control).
* `BossAiSelfTests` (keys `bossai.goals`, `bossai.engage`), registered in `BossHooks`.
* Settings (SERVER, `nusmp-boss-rules-server.toml`, section `bossRules`): `aggroOnSight` (true) and `aggroRange` (32, range 8 to 96).

CHANGED (old stays, new next to it): `Floor Warden`, `Labyrinth Guardian`, legacy Zagred, Charybdis Spawn add the engage goal first; their look goals are the new stand-down versions.
Nothing was removed: `NearestAttackableTargetGoal`, `HurtByTargetGoal`, `Positioning`, every move list and the old priorities all stay; the look goals keep priorities 7 and 8.

REPLACED: nothing.

Safe zones: nothing is engaged in the Sanctuary Hub dimension (`Labyrinth.inSanctuary`). That is the only safe zone rule the mod has (the Hub's protection classes are block rules).
Peaceful difficulty and invulnerable players are respected through the mob's own `canAttack`.

## Worst-case cost (rule 11)

* Server: per boss, one box query of players every 10 ticks, and only while it has no living target (a boss that is fighting costs two compares per goal evaluation, every other tick).
  At most 10 players read, at most 10 line-of-sight rays, at most one path search per 40 ticks (only when nobody can be seen), no per-tick scan, no chunk load, no entity spawned.
  One small list and one lambda allocation per scan, none between scans.
  With 20 idle bosses loaded: about 20 box queries per 10 ticks (2 per tick on average).
* Network: none. No packet is sent; the target is server state the existing boss bar already follows.
* Client: none. The look goals stand down: slightly less head work.
* Cleanup: the goals die with the entity; nothing is stored per player and nothing static.

## Tests

* `BossEngageTest` (pure): nearest taken, creative, spectator and dead ignored, range limit, line of sight needed only far away, path asked once, 10 players read at most, the
  10-tick gate and the 4-second rush.
* `BossAiSourceScanTest`: no look-at goal at or above an attack or chase goal in any boss class; every boss class that builds its own goals adds the engage goal first and uses the
  stand-down look goals; settings are SERVER keys with the stated defaults; the engage code is common-safe, one query behind the gate, no packet; the God of Order stays a judge.
* Server self-test `bossai.goals`: builds every boss of the registry (never added to the world): engage goal at priority 0, look priority above attack priority.
  `bossai.engage`: the scan of every boss with nobody around answers "no target" without throwing; the chooser on stand-in candidates. Add both keys to the smoke list.

## Not seen in a game / caveats

* Never run in Minecraft. The diagnosis is from reading the code. Not seen: a boss walking up to a player; that the engage goal's pick starts the fight on the same tick (it follows
  from the code: the goal runs in the target selector before `customServerAiStep` reads the target next tick); pathing on big hitboxes and in void arenas (the straight-walk fallback is the
  only guard); the self-test cannot put a player into the world, so the "gets that target" step is covered by the pure chooser and by reading, not by a live scan.
* A boss now notices a sneaking or invisible player (the visibility modifier is not applied). That is the intent, but a stealth build could notice.
* A boss near a wall fights a player on the other side of it within 12 blocks: it cannot reach them, it will path or walk against the wall. Players can use that to stand safe behind walls
  as before; no new exploit.
* The Messiah act of Lucius and the God of Order are unchanged on purpose (see the table).
* Ranged casters still prefer their range while moves are usable; they close in only after 4 seconds with nothing usable.

# Lucius, balanced, and the arena of the Final Hour (1.46.0)

The owner's words: "lastly fix the boss he needs to be a little balanced to fight also make an item to send you to his arena and that he can
only spawn their". This release does three things to the god-tier Lucius of 1.45.0 (`docs/lucius_god.md`, `docs/lucius_arc.md`):
**balances** him (part 1), gives him a **dimension of his own** with an item that carries you there (part 2), and makes him **spawn only there** (part 3).
Nothing was run in Minecraft. See "Unverified in the game".

## New, replacement, kept (rule 5)

| Part | New / replacement | What happens to the old |
|---|---|---|
| Every number of the balance table below | **Replacement** of the defaults of 1.45.0 | The 1.42.0 final-arc fight is untouched (`luciusGodTier = false`); the god tier's old numbers can be had back by config where a config key exists (`luciusTrueDamageScale = 2`, `luciusAuraRadius = 20`, `luciusStarvationPctPerSec = 1`, `luciusStarvationPulsePct = 5`, `luciusMirrorPct = 6`, `luciusDifficulty = 1.5`); the rest are constants of the pure classes |
| `luciusDifficulty`, `ArenaScaling`, `LuciusBalance` | **New** | - |
| The dimension `nusmp:zogratis_arena`, its builder, the director | **New** | Nothing like it existed (the Labyrinth and the Living Dungeon are other places and are untouched) |
| The Hourglass of the Final Hour, the Return Sigil, `/zogratis_arena` | **New** | - |
| The spawn gate (`luciusArenaOnly`) | **New** | `luciusArenaOnly = false` gives the old free spawning back; a Lucius already in a world (loaded from disk) is never removed |
| `DungeonGuard.on` | **Extended** with one line for the arena | The Labyrinth and the Dungeon are held exactly as before |

## Part 1: the balance

Goal: a fight of about 10 to 15 minutes for 2 to 4 players that a well-geared group (not only maxed True Heroes and Demon Lords) can win, where
deaths come from mistakes (telegraphs) and not from sums. **The duration is a design target, not a measurement** (nothing was played): the numbers
below are chosen from the structure of the fight (three stages of one bar each, a 2% hit cap, so a stage needs at least 50 hits; the gem shatter
takes 22% of a bar at once) and must be tuned from a play test with `luciusDifficulty`.

| Rule | Before (1.45.0) | After (1.46.0) | Why |
|---|---|---|---|
| True damage per move tier (chip, light, medium, heavy, finisher), % of the victim's max health | 3, 4.5, 6.5, 9, 12 | **1.5, 2.25, 3.25, 4.5, 6** | halved: a finisher was 12% x the 1.5 critical = 18% of a full bar in one blow |
| Single-blow cap (true damage, mirror, swap) | 40% | **25%** | one mistake must not be a death from full health |
| Shared pressure cap (all of Lucius's side on one player) | 30% / s | **18% / s** in the god tier (30% stays for the 1.42.0 fight) | the sum of overlapping telegraphs, not any one of them, killed people |
| Hit grace after a true hit | none | **0.5 s (10 ticks)** per player | two telegraphs that land together hit once, not twice |
| Relic EMP | chain cooldown 45 s | chain cooldown 45 s kept, **floor 25 s** at any difficulty | a hard setting can never make it a loop |
| Severance | chain cooldown 40 s | kept, **floor 30 s** | same |
| Starvation pulse | every 35 s, 5% | **every 12 s (15 s at the default difficulty), 3%** | frequent but small: a tax, not a spike |
| Ultimate reversal | 10 s | **20 s** (mirror 6% -> 3%) | |
| Spatial swap | 12 s | **20 s** | |
| Checkmate counter (per target) | 8 s | **14 s** | |
| Time Stasis Traps | 6 at once, 20-tick warning | **3 at once, 24-tick warning** | |
| Time Decay (anti-regeneration) radius | 20 | **14** | potions and food heal normally in both |
| Starvation, per second | 1.0% of the pool | **0.5%** | |
| Starvation floor | 1% of the pool | **10%** | above a starter page's 8% cost: one basic spell can always be cast |
| Clones in stage 3 | 3 to 5, a cast every 8 s each | **2, 3 under 15% of the stage, +1 per player beyond two, at most 5; a cast every 12 s each** | |
| Chest gem window (stage 3) | open 8 s of every 14 s, warning 1 s | **open 12 s of every 18 s (+50%), warning 2 s** (same 6 s closed, same first 6 s closed) | body blows in stage 3 still take 50% |
| Stage change | 4 s invulnerable | 4 s invulnerable (kept) **plus 10 s of grace**: Time Decay, the EMP, the severance, the starvation drain and the pulse are off, and every mark of his is taken off the players when the change begins; the chain cooldowns were already reset | players can drink, eat and breathe before the next act |
| Difficulty master | none | **`luciusDifficulty`**, 0.5 easy, 1.0 normal, 1.5 hard, **default 0.8** | multiplies his damage numbers (true damage, mirror, the old base damage) and divides his cooldowns, above the floors |
| Players in the arena | no scaling | **`ArenaScaling`**: max health 70% / 100% / 115% / 130% for 1 / 2 / 3 / 4+ players; true damage 80% / 100% / 106% / 112% | read once when a stage begins (and when the fight begins); his health reset is per stage, so it is the stage's bar that is scaled |

Effective numbers at the default difficulty for two players: a finisher takes 4.8% of a full bar (was 12%, or 18% with a critical), a chip 1.2% (was 3%).

**Why damage follows the players more slowly than health** (the owner's brief said "scale max health and true damage by player count", and added that
scaling health only is wrong): more players spread the pressure over more targets, so a 30% rise in his health for four players needs only a
12% rise in what he deals to each of them to keep the fight about the same length.

**What the difficulty master does not touch**: the telegraph fairness rule (every harmful step shows 12 ticks or more; `LuciusChains.valid`, tested over
every chain), the 2% hit cap on him, his drops, his stats (4800 per stage), the three stages and the stage refills.

### The pure classes

* `boss/god/LuciusBalance`: the difficulty clamp, the cooldown floors (`cooldown(id, base, difficulty)`: base divided by difficulty, never under the id's floor,
  never lengthening a base that is already shorter than its floor), the pressure share, the hit grace and the stage grace numbers.
* `boss/god/ArenaScaling`: the player-count table.
* Changed in place: `TrueDamage` (tiers, cap), `StarvationMath` (floor), `CounterRules` (cooldowns), `TrapLedger` (3, 24), `CloneScheduler`
  (`cloneCountGod`, `setInterval`, `PressureBudget.allow(..., share)`), `CoreGemMath.Timing` (CLASSIC and GOD), `LuciusChains.Runner.start(chain, now, cooldown)`.
* Glue: `LuciusGod` (scaling, grace, difficulty, cooldowns), `LuciusArcDirector` (chain cooldowns, the clone count, the gem timing), `LuciusArcHooks` (the share),
  `LuciusBossEventHandler` (the grace on the aura and the drain), `LuciusGodConfig` (new defaults and `luciusDifficulty`).

Tests: `LuciusBalanceTest`, `ArenaScalingTest` are new; `TrueDamageTest`, `StarvationMathTest`, `TrapLedgerTest`, `CounterRulesTest`, `CloneSchedulerTest` and `CoreGemMathTest` follow the
moved constants. No fairness test was weakened (the telegraph minimum is still 12).

## Part 2: the arena

### The dimension

`data/nusmp/dimension/zogratis_arena.json` (a flat void, biome `minecraft:the_void`, no features, no structures) and `data/nusmp/dimension_type/zogratis_arena.json`
(no natural spawning: `monster_spawn_light_level 0`; fixed time 13000, dusk; `effects: minecraft:the_end`, which gives the black-purple void sky and the full-bright lightmap;
no beds, no respawn anchors, no raids). The courtyard is **built in code the first time somebody arrives** (`ArenaBuilder`, from the formula of `ArenaLayout`, inside
`DungeonGuard.internal`), and remembered in `ArenaSave` (`built`). `/zogratis_arena repair` puts back anything missing.

Layout (`ArenaLayout`, pure and tested; offsets from the centre, the floor surface is y = 64):

* a round floor of radius 42 on two layers of blackstone, a checker of polished blackstone and bricks, a gold ring at radius 12;
* the **dais** (radius 5, one block raised, quartz with a gold edge and a crying obsidian rune in the middle): Lucius stands here;
* **four rune pillars** (3 by 3, nine high, gilded bands, a sea lantern on top) on the diagonals at (+-20, +-20);
* a **raised ring seat** for spectators (radius 33 to 37, one block high inside, two outside);
* a **wall** (radius 41 to 42, five high, merlons on top, a soul lantern every twelfth) that closes the courtyard;
* four **Return Sigils** at the compass points (radius 38).

Everything is vanilla blocks (so no texture can be missing) except the sigil. The arena is held by `DungeonGuard` (`on()` is always true there, also on the client for the crack
animation): nothing is mined, blown up, burned or moved, and `ArenaSpawnEvents` refuses to place a block or a bucket (the item part of a right click is denied; the sigil still answers).
No natural spawning (`FinalizeSpawnEvent`). A player who falls below the courtyard (over the wall) is set back on the arrival ring ("The void gives you back").

### The Hourglass of the Final Hour (`zogratis_hourglass`)

Epic, stack 1, craftable (`data/nusmp/recipe/zogratis_hourglass.json`: gold ingots, a nether star, amethyst shards, a clock, diamonds, an ender pearl) and in the creative tab.
Real 3D in hand (`tools/gen_weapon_models.py`, `zogratis_hourglass`, drawn by `WeaponRenderer`; preview `python tools/item_preview/preview_weapons.py zogratis_hourglass`),
the old sprite (`tools/gen_zogratis_arena_assets.py`) as the inventory icon, a glowing sand and a violet crown gem. **Not a weapon, so no engravings** (rule 6 is about weapons).

* **Hold right click 3 seconds** (a closing ring of Rukh light, a bell, a countdown on the action bar): the holder and every player within **8 blocks** (party members
  of the Magic Knight Squad first, then the nearest, never a player who is fighting the user, at most 7 companions: `HourglassRules.pickTravellers`) are carried into the arena.
  Each traveller's place is remembered (`ReturnBook`: dimension, position, rotation, in `ArenaSave`).
* **Inside**, the same hold takes you home (3 s), or **10 s while Lucius fights** (leaving mid-fight is allowed but costs time). Home is the recorded place, else the respawn point, else the world spawn.
* Refused with a message: in the Labyrinth and the Living Dungeon; within 5 seconds of being hurt by a creature (it is not an escape from another fight; a blow also breaks the channel on the way in);
  to enter while the boss fights (the fight is closed to latecomers). **Cooldown 60 s** (vanilla item cooldown) after it brings someone home.
* The **Return Sigil** (`arena_return_sigil`, unbreakable, lit, creative-tab item): right click sends you home at once, but it is **dormant while Lucius fights** (it would be a free exit);
  it works when he is defeated or the fight is abandoned. Outside the arena it does nothing.
* `/zogratis_arena leave` (anyone in the arena, refused while he fights unless an operator) is what the **clickable "[ Return home ]"** after the victory runs.
  Operators: `enter`, `reset` (state and boss), `repair`, `status`.
* **Dying in the arena**: no game rule changes; the items drop as usual. A message gives the coordinates and says they last five minutes (the vanilla despawn time), and the same is repeated
  after the respawn if the marker (`nusmp_arena_soul`, 5 minutes) is still running. You arrive "2 blocks above the floor beside the dais", on a ring that spreads travellers out.

### The director (`ArenaDirector` + the pure `ArenaState`)

* **READY**: the first player to arrive starts a 10-second countdown (title "The Final Hour approaches"); a player who stands within 12 blocks of the centre for 5 seconds starts it sooner.
  At the end the title "The Final Hour begins" shows and Lucius is spawned once on the dais (`ZogratisBoss.spawnAt`, tagged `nusmp_arena_ok`, arena anchored on the dais). His stages, his scaling by the players present and his voice are his own.
* **FIGHTING**: if no player is left in the arena for 2 seconds (they left or died), or the boss vanished, the fight is **abandoned**: he is removed, the arena rests **60 seconds**, then it is READY again with a fresh boss (no half-health cheesing).
* **Defeat** (`LivingDeathEvent`, which his stage changes never fire): loot as he already drops it (not touched), the arena rests **`luciusArenaCooldownMinutes`** (default 20), the sigils open, everyone gets the return prompt.
* A Lucius that an operator summons by hand inside the arena (creative `/summon`) is allowed and changes no state. The state saves (`ArenaSave`) and survives a restart (a running fight with nobody inside is abandoned on the first look).

## Part 3: he spawns only there (`ArenaSpawnEvents`, the pure `SpawnGate`)

`luciusArenaOnly` (default true). The table: not Lucius or a clone of his: allowed; switch off: allowed; loaded from disk: allowed (a Lucius from an older world is not deleted by this update, and the arena's own boss
comes back after a restart); inside the arena: allowed; carries `nusmp_arena_ok`: allowed; anything else: **denied**.

* **Every way in** ends in `EntityJoinLevelEvent` (cancelled, logged, and the players within 24 blocks are told "He will only answer in his arena: use the Hourglass of the Final Hour."): the spawn egg, `/summon`, a structure, a spawner, another mod.
  `/multiverse boss lucius` explains itself and does not even try.
* **Every way out** is vetoed: `EntityTravelToDimensionEvent` (portals, the Dimensional Nexus, spatial gates cannot carry him or his clones away) and `EntityTeleportEvent` (a teleport that would leave the courtyard).
* Operators in creative can still `/summon` him **inside** the arena.

## Config

`nusmp-lucius-arena-server.toml` (new, `ArenaConfig`; the lead registers it): `luciusArenaOnly` (true), `luciusArenaCooldownMinutes` (20, 1 to 1440).
`nusmp-lucius-god-server.toml` (existing): new `luciusDifficulty` (0.8, 0.5 to 1.5); new defaults `luciusAuraRadius` 14, `luciusStarvationPctPerSec` 0.5, `luciusStarvationPulsePct` 3, `luciusMirrorPct` 3 (cap 25).

## Unverified in the game

Everything was type-checked against API stubs (with a negative control) and its pure part unit tested (and the tests mutation-checked); **nothing was run in Minecraft.** Watch first:

1. The **balance itself**: the 10 to 15 minute target is arithmetic, not a play test. Tune with `luciusDifficulty` first.
2. Building the courtyard on the first arrival (about 17,000 `setBlock` calls in one tick, after loading 15 by 15 chunks): a visible hitch once, never again; `/zogratis_arena repair` rebuilds.
3. `ServerPlayer.teleportTo(ServerLevel, ...)` into a dimension that has never been loaded (the Labyrinth's gate uses the same call); that the first traveller lands on the built floor and not in the void.
4. The `minecraft:the_end` sky and full-bright lightmap in a dimension with a fixed time of dusk; whether weather particles show (the void biome has no precipitation).
5. `PlayerInteractEvent.RightClickBlock#setUseItem(FALSE)` stopping a block or a bucket from placing while the block under the cursor (the sigil) still opens.
6. The use animation and the 200-tick duration on the server against the 60-tick animation on the client (the client cannot know that he is fighting).
7. The arena player count at the start of the fight (`LuciusGod.arenaPlayers`: survival players within 60 blocks of the dais), the max-health modifier `nusmp:arena_scale`, and that removing it at the end of an abandoned fight clamps his health.
8. The 3D model's scale and pose in hand (previewed with the headless rasteriser only).

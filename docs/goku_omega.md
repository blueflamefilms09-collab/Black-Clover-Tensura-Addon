# Goku Omega

Owner request: "Goku should always be the strongest boss in my mod and should be overpowered as hell. He can EP skill against players, he can autonomously train himself, he should be able to level up,
... the most overpowered, most integrated boss with a lot of hacks and abilities, and be able to transform, like in the actual anime, from base all the way to mastered Ultra Instinct."

Status: written without a game and checked only by a compiler, unit tests and source-scan tests. Nothing here has been seen running. See "Not seen in a game" at the end.

Everything below sits behind ONE server setting, `goku_boss.omega.enabled` (file `nusmp-goku-boss-server.toml`, section `[goku_boss.omega]`, default **true**). Set it to `false` and he is exactly
the Goku of before (docs/goku_boss.md): every call into the new code is guarded by that one switch.

## What was built, in plain words

**1. He is always the strongest boss.** On the first scan of a fight he reads the stat line of every other boss of the roster and works out the strongest thing any of them can ever become (its
effective health and its damage rate at the largest multiplier any scaling in the mod can give: x10 boss buff, x8/x5 arena, x2.5/x1.6 floor). His health factor and damage factor are never allowed to
fall below 1.25 times that. A new, stronger boss added later raises his floor by itself; nobody has to remember. A unit test and a self-test fail the build if a rival ever exceeds him, or if the floor
climbs above his ceiling (400 x health, 96 x damage).

**2. A new last form: Mastered Ultra Instinct** (silver hair, silver aura, a silver halo ring on the ground). It comes after True Ultra Instinct, below 2% health or at 98% of the time-based schedule.
Near-perfect dodge (92% chance, still capped by the dodge setting, 10-tick cooldown, 8 dodges per 6 seconds) and a counter after every dodge. Line: "Mastered Ultra Instinct. My body knows. Now, let's finish this."

**3. He uses EP skills on players** (server side, bounded): *Energy Drain* takes a share of a player's magicules and aura (10% plus 0.4% per level, at most 35%; never hunger, never max EP); *Skill Seal*
puts one of the player's active skills on cooldown through Tensura's own skill cooldown (12 s plus half a second per level, at most 30 s; resistance and intrinsic skills are skipped, nothing is
removed or forgotten; a player with no skill gets a short silence).

**4. He levels up.** One level for the whole world (saved with the overworld, `data/nusmp_goku_omega.dat`). He starts at level 5. Each level adds +4% health, +3% damage and +10% to the EP
reading he fights with; high levels unlock more hacks. Experience comes from:
* **fights**: beating players 400 + 150 per player killed (at most 5); a farewell (the players won and he bows out) 300 + 60 per fighter (at most 10); a fight that ended with nobody left 100;
  at most 2,500 per fight and 6,000 per in-game day;
* **autonomous training**: 60 experience per in-game hour (1,000 ticks), at most 1,500 per in-game day, looking back at most 31 days when a world was off. Pure arithmetic, done once an in-game
  hour by one timer and once at the start of every fight ("I've been training while you were gone. Level N now!"). No entity, no chunk, no player is touched.
* level cap: setting `levelCap` (default 50, at most 200). Curve: experience to the next level = 150 x level^1.5.

**5. The hacks** (each unlocks at a level; the order is the level order, new ones are appended):

| Hack | Level | What it does | Cooldown | Per fight | Per player |
|---|---|---|---|---|---|
| Ki Sense | 1 | never loses a player: retargets the nearest fighter when his target is gone or far | 2 s | - | - |
| Counter | 1 | a strike at whoever he just dodged | one per dodge | 40 | - |
| Energy Drain | 3 | magicules and aura of the player he hits | 15 s | 12 | 5 |
| Mirror | 4 | turns the first projectile of each form back at the shooter | per form | 11 | - |
| Great Sage adaptation | 5 | resists a damage kind he keeps being hit with (7 kinds: melee, projectile, magic, fire, explosion, lightning, other); 3 free hits, +6% per hit, cap 30% + 1% per level (at most 75%), forgets 10% every 10 s | - | - | - |
| Skill Seal | 6 | one skill of a player on cooldown | 20 s | 8 | 3 |
| Time Skip | 8 | when burst for 6% of his health in 2 s he blinks behind whoever did it | 12 s | 12 | - |
| Second Wind | 10 | a small heal (3% of his health) at each form change | per form | 5 (15% total) | - |
| Ki Barrier | 12 | turns away one single blow of 5% of his health or more | 30 s | 6 | - |

All of them are cleared on logout, death, dimension change, the end of the fight and the removal of the boss.

**6. Bigger hits.** In Omega mode one hit of his may take up to `goku_boss.omega.maxHitPercent` (default 50%) of a player's maximum health (at most 95%) instead of 30%. The framework's balance law
would stop every boss hit at 40%, so in Omega mode he builds the damage with the same chain as the law (base, EP scale, difficulty, spell-damage rule, mana law, the server's damage ceiling) but without
the law's own 40% cap, and then applies his own cap. With the default, the fewest hits that can kill a full-health player is 2 (at the 95% hard limit it is still 2).

## How you read it in the game

* The boss bar says `Son Goku Lv.N - <form>` and he announces changes of form, training and learning in the chat of the players near him ("Goku is adapting to melee damage.").
* The player gets one action-bar line for each EP skill used on them ("Goku's ki overwhelms yours: 14% of your magicules and aura drained", "Goku sealed your <skill> for 14 seconds").
* `/multiverse goku level` (operators): his level, experience, what he trained today, how many fights he won, and which hacks he has. `/multiverse goku level set N` puts him at the start of level N
  (1 to 200; the level cap setting still holds him). The same two commands also exist as `/multiverse goku_boss level [set N]`.

## Settings (`nusmp-goku-boss-server.toml`, `[goku_boss.omega]`)

| Key | Default | Meaning |
|---|---|---|
| `enabled` | true | the one switch; false = the old Goku, everything else is ignored |
| `epSkills` | true | Energy Drain and Skill Seal |
| `hacks` | true | Ki Sense, Counter, Mirror, adaptation, Time Skip, Ki Barrier |
| `secondWind` | true | the small heal at form changes (see decisions) |
| `healthBonus` | 2.5 | multiplier on his health factor (1 to 10), above the floor |
| `damageBonus` | 1.5 | multiplier on his damage factor (1 to 10), above the floor |
| `maxHitPercent` | 0.5 | one hit's share of a player's maximum health (at most 0.95) |
| `uiDodgeCap` | 0.85 | the most chance his dodge may have (at most 0.95) |
| `levelCap` | 50 | highest level (1 to 200) |
| `training` | true | autonomous training on or off |
| `trainingXpPerHour` | 60 | experience per in-game hour |
| `trainingDailyCap` | 1500 | most training experience per in-game day |
| `fightDailyCap` | 6000 | most fight experience per in-game day |

## The worst case (cost on a public server)

| What | Bound |
|---|---|
| New entities | 0 (nothing is spawned) |
| Bosses at once | the existing global cap of 2 |
| Work per tick, per boss | a few comparisons; every 20 ticks at most 16 lookups of his own fighter list; every 200 ticks 7 multiplications |
| World scans / chunk loads | 0 new (the players come from the fighter list the framework already keeps) |
| Training timer | one modulus per server tick; once per 1,000 ticks one saved-record update (a bounded loop of at most 32 steps) |
| Saved data | one record of about ten numbers; no per-player data |
| Per-player rows | at most 32 per boss, cleared with the fight |
| Network | no new packet type; the existing budgeted particle effects plus one action-bar line to the one player an EP skill hits: at most 12 + 8 per fight for drain and seal, at most 1 per 15 s / 20 s |
| Client draw | the Mastered silver halo is one ring effect per second through the existing particle budget; one new skin pair (two small textures); no new model |
| Experience arithmetic | long, clamped, cannot overflow: a segment is at most 24,000 ticks times a rate of at most 1,000,000 |
| Worst-case hit | 95% of a player's maximum health (default 50%), so never a one-shot from the cap |
| Worst-case fight totals | drain 12, seal 8, counter 40, mirror 11, time skip 12, second wind 5, barrier 6 uses |

## NEW, REPLACED, KEPT

* **NEW:** Mastered Ultra Instinct form and its skin (`arc_goku_mui`, `arc_goku_mui_glow`); the strongest-boss guarantee; the persistent level and the experience; autonomous training; Energy Drain,
  Skill Seal, Ki Sense, Counter, Mirror, adaptation, Time Skip, Second Wind, Ki Barrier; the Omega hit cap and dodge cap; the `[goku_boss.omega]` settings; `/multiverse goku level [set N]`;
  `GokuOmegaMath`, `GokuOmega`, `GokuWorld`, `GokuOmegaSelfTests` and their tests.
* **REPLACED:** nothing was removed. The Goku boss is extended, not swapped: with `omega.enabled = false` every old number is used again.
* **KEPT:** the nine old forms and their order, all five old skins, the five attacks, the old `maxHitPercent` / `uiDodgeCap` settings (still the values when Omega is off), the old self-tests
  (`goku.boss.*`), the arena leash, the global cap, the boss rules (no drops added here, no hunger effects).
* **Changed tests:** `GokuLadderTest` (the canon list now ends with the Mastered form, which has no Saiyan chart row so it is exempt from the "is in the chart" check) and the ladder size line in
  `GokuBossSelfTests` (10 to 11 steps).

## Decisions and interpretations

* **Second Wind breaks the framework's "no heal in combat" rule on purpose**, a small, capped, switchable exception like the Gates refill (3% of his health per form change, 15% per fight, setting
  `secondWind`). The owner may turn it off or want it removed.
* Mastered Ultra Instinct has no Saiyan chart row; its numbers (damage x2.9, defense 1.10, speed 0.32, silver rgb) live in `GokuLadder.EXTRAS`. In classic mode the ladder still ends at True Ultra
  Instinct.
* Kaio-ken is not a ladder step (it already is one of his attacks).
* Silver eyes of Ultra Instinct are shown through the glow layer and the accent colour, not a model change.
* "EP skills" is read as spending the player's EP-style resources (magicules and aura) and their skill cooldowns, never their max EP, never hunger.

## Tests and the smoke list

Unit tests: `GokuOmegaMathTest`, `GokuOmegaSourceScanTest` (plus the changed `GokuLadderTest`). Self-tests (server smoke): the prefix **gokuomega** with the keys `gokuomega.data`,
`gokuomega.world`, `gokuomega.spawn`. The workflow's smoke list must get the token `gokuomega` (before `,scaling,`); this change did not edit the workflow.

## Models, tools and sources

No Blender and no Blockbench model was made: nothing here needs a model. The new form is a skin (generated by `tools/goku_boss/gen_goku_skins.py`, a palette script like the five before it), aura and
particles only. This is reported as the rules ask.

Wiki sources (rule 10): the English and the Japanese Dragon Ball wiki were not fetched in this session. The forms, their order and the "Mastered Ultra Instinct" name are only well-known canon
(Dragon Ball Super: Ultra Instinct Sign, Perfected, Mastered). The hacks (drain, seal, adaptation) are game design, not canon entries. Nothing could be checked against a wiki.

## Not seen in a game

Nothing was run in Minecraft. The numbers are checked by tests and the compiler only. Open risks: the exact balance of the hit cap and the levels at 5 to 50 players; the health attribute's own
ceiling (1,024 by default in vanilla) may clamp very high factors, the same open question as before; Tensura's skill cooldown unit (seconds) was read from the code and not seen working; the silver
skin was generated, not looked at in a game.

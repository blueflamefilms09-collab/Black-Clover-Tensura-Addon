# The five early bosses of Saga 1 (Black Clover: Revchi Salik, Heath Grice, Lotus Whomalt, Mars, Vetto)

Package `com.newuniverse.nusmp.arc.early`. One doc per character: `revchi_salik.md`, `heath_grice.md`, `lotus_whomalt.md`, `mars.md`, `vetto.md` (canon with URLs, the fight
design with the reason for every move and counter, numbers, what is unverified). This page is the map of what they share. The art side is `docs/arc/art_kit.md`, the
systems under them are `docs/arc/core.md`, `docs/zogratis_bosses.md`, `docs/boss_rules.md` and `docs/boss_dps_budget.md`.

## The chain

Story order is the order the anime met them (each follows the one before; the Eye of the Midnight Sun bosses of the next builder follow all five):

| # | boss | arc | tier | road | floors | health | the one thing about him |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Revchi Salik | Magic Knights Entrance | 1 | either | 10, 20 | 750 | a bind that can be torn, cut or dodged |
| 2 | Heath Grice | Saussy Village (first mission) | 2 | Abyssal | 10 to 30 | 1300 | a timetable with one safe corridor, a shield that stops range and not a blade |
| 3 | Lotus Whomalt | Dungeon Exploration | 1 | either | 10 to 30 | 1050 | a slow smoke dose to read and lower |
| 4 | Mars | Dungeon Exploration | 2 | Holy | 20 to 40 | 1600 | stones to crack, an armour with a front |
| 5 | Vetto | Seabed Temple | 2 | Abyssal | 30 to 60 | 2000 | auras that anti-magic tears off |

(Tier 1 health band 700-1100, tier 2 1300-2000; Lucius stays the top at 3600.) None of the five heals, changes a block, or spawns an entity (puppets are moved effects,
the Gates give each one the framework's single refill). Crowd control only goes through `BalanceLaw`. Effects go through `VfxSpawn` and `Tells` (the mod's own shapes,
no vanilla particle spam); sounds are vanilla `SoundEvents`.

## The shape of the code: pure rules, thin game glue

| layer | classes | what it is |
| --- | --- | --- |
| tables (pure) | `ChainTable`, `HeathTable`, `LotusTable`, `MarsTable`, `VettoTable`, `MoveRow` | every move as a row (id, name, power, wind-up, cooldown, range, weight, ultimate, crowd, recovery) and every number of the fight; `MoveRow.problems(rows)` is the list of house rules |
| mechanics (pure) | `BindLedger`, `FrostShield`, `HailPattern`, `Timetable`, `DoseLedger`, `CloudField`, `StoneLedger`, `FrontGuard`, `BeastAuras` | the state and the rules of each signature mechanic, one class each |
| shared (pure) | `EarlyGeometry`, `Window`, `ChannelGuard`, `BurstMeter` | geometry (capsule corridor, cone, polygon, ring waves), a timed window, the break of a long cast by a share of the boss's health, a sliding-window damage counter |
| record (pure) | `EarlyRoster` | the five `ArcCharacter` records |
| game | `EarlyBoss`, `EarlyMoves`, the five `*Boss` classes | the chassis (bar title, speech, aim marks, stagger, guard hooks), the shared effects (circle, cone, corridor, bolt, charge, wisps, ring wave), and each boss's telegraphs and `run` code |

No `net.minecraft` import in the first four layers: they are tested with plain JUnit. The game layer was type-checked against the API stubs, never run.

One lesson is shared by four of the five ultimates (`ChannelGuard`, `EarlyBoss.armGuard`): while the wind-up runs, a share of his maximum health dealt to him breaks the spell
(12% for Revchi, Heath and Lotus, 14% for Vetto) and leaves him staggered (open to blows, starting nothing). A party learns it once and meets it again in every fight.
Mars's ultimate is the exception: its answer is the circle and the two rings that must be jumped.

## House rules every table keeps (`MoveRow.problems`, enforced by `EarlyTablesTest`)

8 to 14 moves; every telegraph at least 12 ticks (an ultimate at least 30) even at the Gates, where the framework takes a quarter off; exactly one ultimate, from 60% power
or later, that leaves the boss exposed (recovery of 40 ticks or more; the table gives 60); cooldowns of three seconds or more; at power 20 there is a move for every distance
up to 22 blocks; a melee, a ranged and a crowd move exist.

## Tests (pure, run with the JUnit shim in the scratch folder: `jt.sh`)

73 cases in 15 classes under `src/test/java/com/newuniverse/nusmp/arc/early/`:

| test | what it proves |
| --- | --- |
| `EarlyGeometryTest`, `WindowTest`, `BurstMeterTest`, `ChannelGuardTest` | the shared maths and windows |
| `BindLedgerTest`, `FrostShieldTest`, `HailPatternTest`, `TimetableTest` | Revchi's links and anti-magic, Heath's shield and cage corridor (hundreds of seeds, the corridor is always clear) and watch |
| `DoseLedgerTest`, `CloudFieldTest` | Lotus's ladder, the cap of his gaze, the thickest-cloud rule and the cap on discs |
| `StoneLedgerTest`, `FrontGuardTest`, `BeastAurasTest` | Mars's stones and front, Vetto's auras, tears and wounds |
| `EarlyTablesTest` | the house rules for all five tables, a deliberately bad table is caught, the mana formula equals the framework's, each fight's signature move sits at its power, and the framework's own `MoveScheduler` played out against each table (only opening moves at the opening, an ultimate never before its power and not back to back, every move used at the Gates) |
| `EarlyRosterTest` | the chain order, each health in its tier band, every floor range reaches a boss floor, every Charter skill id in the Tensura reference, themes known to the road, the voice (ASCII, no banned words) |

Mutation checks (a rule broken on purpose, the test run, the file restored): `DoseLedger` with the sight cap lifted (3 tests fail), `HailPattern` with the impact radius left out of
the corridor clearance (2 fail), `BindLedger` with every blow tearing a whole link (1 fails), `StoneLedger` with anti-magic no longer double (1 fails). Every mutant was caught.

## Where a number lives (to retune a fight)

Everything about a boss's fight is a constant in its table class; the glue only reads them. Health, attack, speed, armour, the floors, the themes and the Charter chance are
in `EarlyRoster`. The framework's own scaling (power tuning, victim EP, the 6% per second DPS budget, the balance law) sits on top of every base damage, and the Gates
(second bar) are the framework's.

## Adding a sixth early boss

1. A table class with `MoveRow`s and the numbers; `EarlyTablesTest` picks it up when it is added to the list in the test.
2. A pure class per signature mechanic and its test.
3. A record in `EarlyRoster`, a `*Boss extends EarlyBoss` with `CHARACTER`, `TYPE = ArcEntities.boss(...)` and `moves()`.
4. A model spec `tools/arc/char_<id>.py`; `ArcRoster.register(<Boss>.CHARACTER)` in `ArcCharacters.bootstrap()`; entity name and messages in the lang file.

## Integration (for the lead)

```java
// ArcCharacters.bootstrap(), in story order (package com.newuniverse.nusmp.arc.early):
ArcRoster.register(RevchiSalikBoss.CHARACTER);
ArcRoster.register(HeathGriceBoss.CHARACTER);
ArcRoster.register(LotusWhomaltBoss.CHARACTER);
ArcRoster.register(MarsBoss.CHARACTER);
ArcRoster.register(VettoBoss.CHARACTER);
```

Naming the `CHARACTER` field loads the class, which registers its entity type through `ArcEntities.boss(...)` before the registry events. No config keys, no quest keys, no
renderer registration (the generic `ArcBossRenderer` reads each `ArcLook`), no commands. The lang fragment is `arc/frag/s1a.lang.json` of the scratch folder (five entity names
and ten messages); after the merge run `python3 tools/gen_arc_grimoire_assets.py` and `tools/gen_arc_dialogue_lang.py`.

## What is not verified

Nothing here has run in a game. The pure layer is tested; the game layer compiles against the API stubs (the only errors are the known stub artefacts: an abstract
`MobEffectInstance` and `INBTSerializable.deserializeNBT` on `Entity` subclasses). Open questions for the first play-test: the felt length of every wind-up, whether the
telegraph lines are readable at 20 blocks, the speed of the wisps against a sprinting player, and the `SEAL_CHAINS`, `ASH_FORMATION`, `CRYSTAL_FX*`, `ICE_FX*`,
`BEAST_FX*` shapes at the radii given. The framework's `beginGates` always announces the Tree of Qliphoth text (private, cannot be overridden from here).

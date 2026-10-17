# Lotus Whomalt, the Sideline General (`lotus_whomalt`)

Boss of the Dungeon Exploration Arc, Saga 1 (saga `ELF_REINCARNATION`), tier 1. Classes: `arc/early/LotusWhomaltBoss`, table `LotusTable`, state `DoseLedger`
and `CloudField`, record `EarlyRoster.LOTUS_WHOMALT`. Model: `tools/arc/char_lotus_whomalt.py`. Overview of the five early bosses: `docs/arc/early.md`.

## Canon (what the sources say, and how sure it is)

The page fetcher could not reach any wiki (DNS refused), so this is from search digests of the pages listed; the digests of the fan wikis differ in detail.

* Adjutant General of the Diamond Kingdom, a Smoke Magic user ("Lotus of the Abyss" in one profile), easy-going and laid-back, always trying to avoid fighting; the
  sources call him either a very experienced strategist or a coward. One profile: 36 years old, 184 cm, tall and slim with a relaxed, slouched posture, a large
  scar on the chest from a later fight with Yami, often wrapped in smoke.
* The Dungeon Exploration Arc (manga chapters 11 to 21, anime episodes 14 to 19): he is sent to take a dungeon on the border with a group of subordinates and the
  kingdom's secret weapon, Mars. Luck Voltia defeats his subordinates while Lotus watches from the sideline; he had been running a weakening spell on Luck since
  Luck arrived (a poisonous smoke that works slowly; once enough has been breathed the targets lose their balance and fall), traps Luck in the Smoke Binding Cross
  Prison (Asta destroys it), then wraps all the Black Bulls in a veil of thick smoke; Asta attacks from inside it and breaks his arm and ribs, and he escapes with the
  treasure and the unconscious Mars when the dungeon collapses.
* Spells: Smoke Garden of Usurpation (Weakening Magic), Smoke Binding Cross Prison and Degenerate King's Smoke Prison (Creation Magic), Hermit's Thick Smoke.
* Sources: https://blackclover.fandom.com/wiki/Lotus_Whomalt , https://blackclover.fandom.com/wiki/Luck_Voltia_vs._Lotus_Whomalt ,
  https://blackclover.fandom.com/wiki/Luck_Voltia,_Asta,_and_Noelle_Silva_vs._Lotus_Whomalt , https://blackclover.fandom.com/wiki/Smoke_Magic ,
  https://blackclover.fandom.com/wiki/Smoke_Garden_of_Usurpation , https://blackclover.fandom.com/wiki/Smoke_Binding_Cross_Prison ,
  https://blackclover.fandom.com/wiki/Degenerate_King%27s_Smoke_Prison , https://blackclover.fandom.com/wiki/Hermit's_Thick_Smoke ,
  https://blackclover.fandom.com/wiki/Chapter_21 , https://anibase.net/en/character/4WK0d/Lotus-Whomalt ,
  https://www.thereviewgeek.com/blackclover-dungeonexploration-explained/ .

## The fight: three mechanics, each with a counter

A fight that is "wait and see" cannot be played, but the slow burn can be made visible and learnable.

| Mechanic | What it does | Counters the party can learn |
| --- | --- | --- |
| **The smoke dose** (`DoseLedger`, `CloudField`) | Every foe carries a dose from 0 to 10, shown on the action bar ("Smoke dose N of 10") when it is 3 or more or changes. It climbs once a second while they stand in a cloud: Smoke Garden +1 (radius 7, 11 s), Hermit's Thick Smoke +2 (radius 9, 7 s, from 40%), Full Bloom +3 (radius 14, 9 s); the thickest cloud counts, never the sum; at most 4 discs stand at once; at the Gates every cloud gives one more. His gaze alone gives +1 every 5 seconds to a foe he can see within 22 blocks, but never above 5. The ladder: Weakness I from 3, Weakness II and Slowness I from 6, Slowness II from 8, and at 10 a stumble (slowness V for 30 ticks under the balance law, pushed to the ground, the dose drops to 5). | **Stay out of the rings** (drawn on the ground in the wind-up). **Break his line of sight** (out of sight and out of smoke the dose falls by 1 every 3 seconds; in his sight it only sinks back to 5). **Catch him**: he backs away, so the answer is to close in and finish him, not to wait the poison out. |
| **The sideline** | He keeps 10 to 16 blocks away (7 to 11 at the Gates: he is cornered). The Smoke Burst (0 to 6 blocks) shoves a party that reaches him and he steps back; the Smoke Step (0 to 30) puts him 11 blocks from the target on the line that runs from the target through him, leaving a small cloud where he stood. | A party with a ranged member or a mobility skill catches him; a melee party uses the walls; the Smoke Step has a 160-tick cooldown and the Burst 140. |
| **Prisons and the Full Bloom** (`ChannelGuard`) | The Cross Prison (two corridors, arms 9 blocks, locked at half the wind-up) and the Degenerate King's Smoke Prison (radius 3.2) hold a foe (slowness V, 50 and 60 ticks, only when the balance law allows) and add dose. **Full Bloom** (ultimate, from 60%): a 56-tick wind-up, then a garden of radius 14 for 9 seconds that gives +3 a second; 10 ticks before it thins it blooms, spending the dose of everyone inside as damage (1.6 per point, so a dose of 10 is 16) and taking 6 points off it. | Step out of the drawn lines. **Anti-magic** is never held by the prisons (a message says the smoke is cut). **Break the Bloom**: 12% of his maximum health inside the wind-up and he drops the spell, staggered for 60 ticks (takes 25% more). Leave the ring before the bloom. |

## Move table (base damage; the framework scales it with power, victim EP and the balance law; DPS budget 6% per second)

| id | name | from | wind-up (Gates) | cooldown | range | damage | tell and counter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `smoke_puff` | Smoke Puff | 20% | 16 (12) | 60 | 4-26 | 6, +1 dose; a bolt of smoke | a line from his mouth to the target |
| `smoke_garden` | Smoke Garden of Usurpation | 20% | 24 (18) | 220 | 6-30 | none, a cloud of +1 | ring on the spot, followed 60% of the wind-up, then locked |
| `cross_prison` | Smoke Binding Cross Prison | 20% | 28 (21) | 260 | 4-24 | 8, +2 dose, hold | a cross on the ground, locked at half |
| `smoke_puppets` | Smoke Puppets (crowd) | 20% | 20 (15) | 200 | 6-30 | 2 to 5 wisps, 7 and +1 dose each | lines from his mouth to every foe; the wisps move 4.4 blocks per second for 84 ticks (slower than a sprint) |
| `smoke_burst` | Smoke Burst | 20% | 16 (12) | 140 | 0-6 | 7 in 5.2 blocks, knock-back, +2 dose, then he steps back | ring that grows |
| `smoke_step` | Smoke Step | 20% | 16 (12) | 160 | 0-30 | none | a ring and a column on his landing spot |
| `thick_smoke_veil` | Hermit's Thick Smoke (crowd) | 40% | 30 (23) | 340 | 6-30 | none, a cloud of +2 | ring, locked after 60% |
| `weakening_pulse` | Weakening Bloom (crowd) | 40% | 26 (20) | 300 | 0-32 | 2.2 per point of dose of every foe with 3 or more; takes 6 off | red lines to the foes who carry a dose; with no dose anywhere it gives +2 to everyone he sees |
| `degenerate_prison` | Degenerate King's Smoke Prison | 60% | 30 (23) | 280 | 3-22 | 12, +3 dose, hold, weakness II | ring, locked after half |
| `full_bloom` | Full Bloom of Usurpation (ultimate) | 60% | 56 (42) | 1500 | 0-30 | 1.6 per point of dose in 14 blocks | see above |

Plain blows come from the framework's melee at 60% of his attack. At the Gates (second bar): clouds one dose thicker, he stays closer, 10% larger.

## Numbers (record)

Health 1050, attack 11, speed 0.26, armor 5 (tier 1 band 700-1100). Either road, floors 10 to 30. Themes: shadow, gravity, haunted_manor, fungal_underdark, storm.
Story: after Heath Grice. Charter Grimoire (40% chance): sleep_mist, haze, poisonous_breath, paralysing_breath, invisible, shadow_motion, darkness,
poison_resistance (all in the Tensura 2.0.1.3 id reference, which `EarlyRosterTest` checks; the in-game self test `arc.charter` checks them against the skill registry).

## Model

`tools/arc/char_lotus_whomalt.py`: slim and tall (2.05 blocks), tired face with eye bags and stubble, messy dark hair with a grey streak, a long violet-grey coat with
the diamond of his kingdom on the back, a cravat, a belt, a pipe in his mouth (its ember is the one warm light on him), an open grimoire in his left hand whose runes
glow violet, and smoke on his own bones that sways in every clip: a plume leaning away from each shoulder, a thread from the pipe, a low wisp behind the coat hem.
Idle `slouch`, cast `book`. Previews: see the report.

## Weapon

None. Lotus is not a member of the Seven Deadly Sins.

## Unverified

* His hair, clothes and colours (no source describes them); the chest scar of the later fight is not drawn.
* All of the numbers: the series gives no numbers, the dose ladder is a design (the canon fact is only that the weakening smoke works slowly and ends in a stumble).
* Nothing runs in a game: the wisps (positions moved by the server), the smoke discs (`ASH_FORMATION`), the dose effects and the action bar line are written against
  the APIs and type-checked, not seen.

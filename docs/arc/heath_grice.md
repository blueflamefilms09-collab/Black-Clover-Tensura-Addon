# Heath Grice, of the Eye of the Midnight Sun (`heath_grice`)

Boss of the first mission of the Black Bulls (the attack on Saussy Village), Saga 1 (saga `ELF_REINCARNATION`), tier 2, Abyssal road. Classes:
`arc/early/HeathGriceBoss`, table `HeathTable`, state `Timetable`, `HailPattern`, `FrostShield`, record `EarlyRoster.HEATH_GRICE`. The model and its second form
(Purified Heath: wings, halo, a floating ice orb) were written by the art builder in `tools/arc/char_heath_grice.py`; this builder wrote only the boss, the moves
and the record. Overview of the five early bosses: `docs/arc/early.md`.

## Canon (what the sources say, and how sure it is)

The page fetcher could not reach any wiki (DNS refused), so this is from search digests of the pages listed. Spellings differ: the Black Clover wiki writes
"Graice", the Western databases "Grice"; Saussy Village is also written Sosshi and Sotsi.

* A commoner of the Clover Kingdom, an Ice Magic user and a member of the Eye of the Midnight Sun. In the anime he attacks Saussy Village looking for a magic
  stone; Asta, Noelle and Magna (the Black Bulls' first mission) fight him. He keeps a pocket watch in his left hand, checks the time constantly, is neurotic and
  irritated when events do not go as he decided, and looks down on commoners with little mana. He combines his ice with the Mist Magic of his subordinates.
* Spells named by the wikis: Crystal Ice Shield (snowflake-shaped barriers raised with an open grimoire; it turned aside Magna's fireballs, and Asta's anti-magic
  sword cut through), Heavenly Ice Fang, Ice Funeral ("the user encases themselves in ice, and anyone trapped within is instantly killed, including the user"), and
  the compound spell Cage of Infinite Hail (official English: Endless Ice Cage) that rains icicles on a wide area from several directions, usually needing two or more
  casters (his ice and his subordinates' mist). When Asta defeated him he used a magic device implanted in his body to freeze himself and two subordinates and
  shatter, so that his employer's secret stayed secret. Years later Lucius Zogratis revives him as a Paladin (the mod's `purified` second form is that).
* Sources: https://blackclover.fandom.com/wiki/Heath_Graice , https://blackclover.fandom.com/wiki/Cage_of_Infinite_Hail ,
  https://blackclover.fandom.com/wiki/Crystal_Ice_Shield , https://blackclover.fandom.com/wiki/Ice_Magic ,
  https://blackclover.fandom.com/wiki/Asta,_Magna_Swing,_and_Noelle_Silva_vs._Heath_Graice , https://anibase.net/en/character/lyBW8/Heath-Grice ,
  https://villains.fandom.com/wiki/Heath_Grice , https://animated-character-database.fandom.com/wiki/Heath_Grice .

## The fight: three mechanics, each with a counter

| Mechanic | What it does | Counters the party can learn |
| --- | --- | --- |
| **The Timetable** (`Timetable`, `HailPattern`) | The Endless Ice Cage is not drawn at random: from 40% power it is the next move whenever his watch strikes (a base gap of 300 ticks, shortened by the framework's cooldown multiplier, never under 120). The wind-up (44 ticks, 33 at the Gates) tolls a bell, draws a ring of radius 8.5 (10.2 at the Gates) and a bright white corridor from the middle of it towards him. Three waves of icicles (6 per wave below 60% power, 8 from 60%, 10 at 100%) land 14 ticks apart, each marked 16 ticks before it lands. No icicle lands within 2.9 blocks of the open corridor (corridor half width 1.4 plus the impact radius 1.5), so the corridor is always safe, even at its inner end. | **Stand in the white corridor.** If nobody takes a single hit, his plan has failed: he is flustered (70 ticks, takes 30% more damage, starts nothing) and says so. The cage is also the move that teaches the rhythm: the bell, about 15 seconds at 40% power (the 300-tick base gap), shorter as his power grows (about 6 seconds at the Gates, the 120-tick floor), then the bell again. |
| **Crystal Ice Shield** (`FrostShield`) | A pool of ice (18% of his maximum health) stands for 140 ticks. It swallows damage that comes from further than 4.5 blocks (spells, arrows) until the pool is gone; a blow struck by the attacker's own hand from within 4.5 blocks passes through whole and takes half of its damage off the pool; anti-magic passes whole and shatters it at once. | **Close in** with a blade: the shield never makes the fight longer for a fighter, and the cage's open corridor faces him, so the way out of the cage leads to him. **Anti-magic** shatters it. A caster outlasts it (the pool is finite). |
| **Ice Funeral** (ultimate, from 80%) (`ChannelGuard`) | A 56-tick wind-up (42 at the Gates): the device in his chest freezes the whole ring of 10.5 blocks (12.6 at the Gates) for 18 damage and a freeze of 40 ticks under the balance law's control rules. | **Leave the ring**, which is drawn on the ground and shrinking. **Break the device**: 12% of his maximum health inside the wind-up and the funeral fails (he is staggered for 80 ticks and takes 30% more). If it fires, the framework's ultimate recovery (60 ticks) leaves him exposed. |

## Move table (base damage; the framework scales it with power, victim EP and the balance law; DPS budget 6% per second)

| id | name | from | wind-up (Gates) | cooldown | range | damage | tell and counter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `ice_fang` | Heavenly Ice Fang | 20% | 18 (14) | 90 | 3-18 | 10 once per foe along a 16-block lane, slow, chill, thrown up | two parallel lines, locked at half the wind-up |
| `icicle_volley` | Icicle Volley | 20% | 18 (14) | 110 | 6-30 | 5 per icicle, 3 to 6 in a fan | fan of lines at the target when it starts |
| `rime_burst` | Rime Burst | 20% | 16 (12) | 130 | 0-6 | 9 in 4.8 blocks, knock-back, slow, chill | ring that grows |
| `crystal_ice_shield` | Crystal Ice Shield | 20% | 16 (12) | 420 | 0-48 | none (a shield) | ring and snowflakes; see above |
| `frost_slide` | Frost Slide | 20% | 16 (12) | 200 | 0-8 | 6 along a 9-block slide away from the target | line behind him |
| `mist_step` | Mist Step | 20% | 18 (14) | 220 | 8-30 | 7 where he lands (3.5 blocks), behind the target | a ring and a column on the landing spot |
| `ice_coffin` | Ice Coffin | 40% | 24 (18) | 240 | 3-24 | 12 in 2.6 blocks, chill, a hard freeze of 40 ticks (balance law) | ring followed 50% of the wind-up, then locked |
| `endless_ice_cage` | Endless Ice Cage (crowd) | 40% | 44 (33) | 330 | 4-28 | 7 per icicle, 3 waves | see above |
| `ice_funeral` | Ice Funeral (ultimate) | 80% | 56 (42) | 1400 | 0-30 | 18 + freeze | see above |

Plain blows come from the framework's melee at 70% of his attack. He wants to stay 6 to 12 blocks away. At the Gates (second bar, the model swaps to the Purified
form): the cage and the funeral are 20% bigger and the watch runs at 0.42 of the gap.

## Numbers (record)

Health 1300, attack 14, speed 0.28, armor 6 (tier 2 band 1300-2000). Abyssal road, floors 10 to 30. Themes: frost, drowned_crypt, haunted_manor, abyss, shadow.
Story: after Revchi Salik. Charter Grimoire (30% chance): icicle_lance, icicle_rain, icicle_spear, ice_wall, ice_breath, ice_blizzard, freeze, cold_resistance
(all in the Tensura 2.0.1.3 id reference, which `EarlyRosterTest` checks; the in-game self test `arc.charter` checks them against the skill registry).

## Model

Not written here: `tools/arc/char_heath_grice.py` (art builder). Previews were rendered again for this report. The look: a man in a dark uniform with a scar across the
face, ice crystals at the shoulders, a floating ice orb; Purified form with wings and a halo. The `ArcLook` and hitbox in `HeathGriceBoss` are the manifest numbers
(`arc_look` of `heath_grice_form2`, the bigger form, for shadow and culling).

## Weapon

None. Heath is not a member of the Seven Deadly Sins. If a weapon were ever made (the pocket watch as a focus, say), fitting Tensura engravings would be
`magic_interference` and `barrier_piercing`; none is built.

## Unverified

* Real numbers of his spells in the series; the fight is a design.
* The device of the Ice Funeral is canon, the 12% "break it" counter is a design rule shared with the other four ultimates.
* Nothing runs in a game: the bell and the watch-tick sounds, the ice VFX shapes (`ICE_FX1..3`, `MIST_MAGIC`) and the chill via frozen ticks are written against
  the APIs and type-checked, not seen.

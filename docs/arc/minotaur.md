# The Minotaur (`minotaur`, Level 2, tier 1)

The first monster of the DanMachi saga: the bull that does not belong on the 5th floor and hunts one boy through it. Part of [`danmachi.md`](danmachi.md) (read its sections 2 and 3 first:
the Dungeon Level and the shared layer). Class `arc/danmachi/MinotaurBoss`, table `MinotaurTable`, record `DanMachiRoster.MINOTAUR`, model `tools/arc/char_minotaur.py`.

## 1. Canon (as summarised) and sources

* A bull-headed monster of the Dungeon, a brute about two and a half metres tall with a dark brown hide, big curved horns, a heavy brow and fists like hammers; the series makes much
  of it standing on the **5th floor**, far above the depth where its kind lives; the Guild rates it **Level 2** (a Level 1 adventurer is no match); it fixes on Bell Cranel and chases
  him; Ais Wallenstein saves him once; he beats it later with the Hestia Knife.
* Its roar can freeze an adventurer who is far below it (as the summaries describe the fight).
* Sources (the standard titles of the pages the search summaries named; **they could not be opened here**, the fetch tool was blocked): the DanMachi wiki, `https://danmachi.fandom.com/wiki/Minotaur`;
  the series' article, `https://en.wikipedia.org/wiki/Is_It_Wrong_to_Try_to_Pick_Up_Girls_in_a_Dungeon%3F`.

## 2. Added for the game (not canon)

The gold nose ring, the wraps on the wrists, the glowing eyes (the model); the Hunt's counters (the louder prey, the wall), the stun of a crash, the Rampage, the Bull Run, the
Level-Up and **Asterius**. "Asterius" is the name the myth gives the Minotaur and the label of this mod's leveled form; whether the books use the name for a reborn Minotaur could not be checked.
The leveled form has ONE horn (the other is a broken stump), a black hide and red glowing veins.

## 3. Numbers

Health 1,000, attack attribute 16 (the plain blow is half: 8), speed 0.30, armor 8, toughness 3. Hitbox 1.6 x 3.09 (the kit's own measure), 1.08 times as big as Asterius.
Floors 5 to 14 on both roads. Charter Grimoire (25% on the first kill, LEGEND): `strengthen_body`, `reinforcement`, `berserk`, `pain_resistance`, `elephant_stampede`,
`earthshatter_kick`, `physical_attack_resistance` (real Tensura ids, checked by a test against the list of the Tensura reference). Lines: "VWOOOOOOH!" / "VWOOOOOOOOOH! The horn grows back, black as the pit!" / "...mooo...".
The framework's power curve applies to every number below: damage x0.6 at 20% power up to x1.6 at the Gates, wind-ups x1 down to x0.75 (never under 8 ticks, ultimates never under 30), 1/1/2/3/4 marks per volley.

## 4. Moves (base numbers, before the power tuning)

| move | id | power | wind-up | cooldown | range | weight | recovery | flags | what it does |
|---|---|---|---|---|---|---|---|---|---|
| Horn Gore | `horn_gore` | 20% | 16 t (0.8 s) | 80 t (4.0 s) | 0,4.8 | 6 | 10 | - | cone 4.4 blocks, +-55 deg, 14 dmg, knock 1.2, lifts |
| Hammer Fist | `hammer_fist` | 20% | 18 t (0.9 s) | 110 t (5.5 s) | 0,7 | 4 | 12 | - | 1 mark(s) r 2.6, 17 dmg, knock 1.0, on the wind-up's end |
| Ground Slam | `ground_slam` | 20% | 22 t (1.1 s) | 160 t (8.0 s) | 0,9.5 | 4 | 12 | crowd | ring wave to 8.5 blocks, 10 dmg, knock 1.0, 1 ring(s) |
| Bull Charge | `bull_charge` | 20% | 22 t (1.1 s) | 190 t (9.5 s) | 7,30 | 5 | 14 | - | charge 24 long, 3.2 wide, 18 dmg, knock 1.5, 1.5 blocks/tick |
| Howl | `howl` | 20% | 28 t (1.4 s) | 420 t (21.0 s) | 0,16 | 2 | 12 | crowd | its own code (reach 12, base 4) |
| Hunter's Leap | `hunters_leap` | 40% | 26 t (1.3 s) | 240 t (12.0 s) | 8,32 | 3 | 12 | - | leap onto the target's mark, ring 6.5 blocks, 12 dmg, knock 1.1, 8 ticks in the air |
| Horn Sweep | `horn_sweep` | 40% | 20 t (1.0 s) | 150 t (7.5 s) | 0,6.5 | 3 | 12 | crowd | circle 5.6 blocks round the body, 11 dmg, knock 1.3 |
| Rubble Throw | `rubble_throw` | 40% | 24 t (1.2 s) | 210 t (10.5 s) | 8,30 | 3 | 10 | - | 2 mark(s) r 2.6, 10 dmg, knock 0.9, lands 14 ticks after the wind-up |
| Rampage | `rampage` | 60% | 30 t (1.5 s) | 380 t (19.0 s) | 4,26 | 3 | 16 | crowd | its own code (reach 11, base 9) |
| The Bull Run | `bull_run` | 60% | 58 t (2.9 s) | 1300 t (65.0 s) | 0,40 | 5 | 60 | ultimate, crowd | its own code (reach 36, base 26) |
| Wild Cross | `wild_cross` | 100% | 26 t (1.3 s) | 200 t (10.0 s) | 0,9 | 4 | 12 | leveled | its own code (reach 9, base 16) |

* **Horn Gore**: the close answer, a cone that lifts. Step out of it during the wind-up (the aim is locked at its start). **Hammer Fist**: one mark under the target. **Ground Slam**: a ring wave, jump it (a foe
  more than 0.75 blocks off the floor is passed over). **Horn Sweep**: a circle round the body, so the answer to a bull is not to stand beside it. **Rubble Throw**: two marks that land 14 ticks after the wind-up.
* **Bull Charge**: 24 blocks at 1.5 blocks a tick along the line locked at the start of the wind-up. Hurts everything in its way once (budget 45% of the victim's health), and **a wall in its way stops it and stuns it** (section 5).
* **Hunter's Leap**: it jumps (8 ticks in the air, along a parabola) onto where the target stood when the wind-up began, and a ring of 6.5 blocks lands.
* **Howl**: a ring of 12 blocks (half a block more per power step): a small shout (4) to everyone in it, and **fear** for every fighter whose Dungeon Level is below the Minotaur's (2 plus its steps):
  Slowness (II when one Level below, III two or three below, IV four or more) and Weakness I for 50 ticks plus 6 per power step, cut by the balance law (3 s for players, halved and quartered when repeated within 30 s). Not on
  users of anti-magic (the mod's own, `AntiMagic.isUser`), not on creative or spectator. It first asks `BalanceLaw.beginControl(boss)`: inside the shared 12 s control cooldown it is only a shout.
* **Rampage** (60%): three dashes at 60%, four at 80%, five at the Gates, each at the nearest foe, each shown for 12 ticks before it runs (9 base damage, 3 blocks wide, 1.3 blocks a tick, a dash is 11 blocks, 8.8 with five), a breath of
  14 ticks (down to 8) between them. A wall ends it in a stun.
* **The Bull Run** (ultimate, 60%): one run of 36 blocks at 1.6 blocks a tick, 4.5 wide, 26 base damage, knock 2.0, then a quake ring of 10 blocks (14) where it ends (a wall ends it sooner and stuns). Exposed for 60 ticks after (x1.25).
* **Wild Cross** (leveled): a "+" of four spokes (9 long, 2.6 wide, 16) at its aim and, twelve ticks later, an "x" between them. One foe is hurt by each stroke at most once.

## 5. The mechanics and their counters (and why)

**The Hunt** (`PreyBook`, pure, tested). The Minotaur of the story does not fight a party: it fixes on the weakest adventurer. So every second it chooses its **prey**: the foe within 40 blocks with the lowest Dungeon Level
(then the nearest, then the lowest id); the mark is kept for 200 ticks and chosen again; its target is set to the prey. The prey takes **x1.15** of its damage and is told ("The Minotaur has marked you...") with a red ring at its feet
each second. The curse must have an answer or it is only a curse on the weakest player: **any other fighter steals the mark** by landing three hits within 60 ticks or one blow of 5% of its health (160 ticks; the prey's own
hits never steal). The strong player draws the horn, the weak one lives.

**The wall.** A charge ends at a wall in a **stun of 60 ticks at the opening, 6 less for every power step, never under 30** (36 at the Gates), and it takes **x1.30** damage while stunned; it cannot act, walk or gore. It learns:
the stun shortens as it grows stronger. The counter is to stand with a wall behind you when the line is locked (the arena of the saga is a pit with walls at 35 blocks, the charge is 24 to 36 long), then step aside.
It is also why the Rampage and the Bull Run can be turned against it.

**The Level.** At the Gates, with a party of Level 2 or more (mean EP 3,162 or more): 1 step (Level 3, +10% damage, +3% speed, Asterius, the Wild Cross); Level 4: 2 steps; Level 6: 3. Under Level 2: it only enrages.

## 6. Verified and not

Pure and tested: the table (every move tested for the pacing rules and played through the real scheduler), the Hunt, the charge and howl numbers, the geometry, the Level. Looked at: the model (front, face, idle, attack, cast, Asterius front and face).
**Not verified**: everything that runs in the game (the charge's wall detection through `horizontalCollision` and the measured speed, the leap's parabola, whether the Hunt's `setTarget` fights the framework's own target goals every
second), how the stone effects look, the sounds (the ravager's hurt, death and roar), the balance of the numbers, the canon points of section 1 (summaries only), the name Asterius.

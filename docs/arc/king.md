# King, the Grizzly's Sin of Sloth (boss, tier 3)

Builder `sdsb`. Saga `SEVEN_DEADLY_SINS`, tier 3, health 2400, floors 30 to 70 on either road. Opens with the other three Sins after the bosses of the Eye of the Midnight
Sun (Saga 1), in any order. Code: `arc/sds/King*.java`, `GuardianShell`, `SafeGardens`, `DisasterBeats`, `SunflowerMath`; model `tools/arc/char_king.py`.

## 1. Canon (what the fight is built from)

King is the Grizzly's Sin of Sloth, Fairy King Harlequin, protector of the Fairy King's Forest. His Sacred Treasure is the Spirit Spear **Chastiefol**, used with his inherent power
**Disaster**: control over life and death (the pages differ on the details; one summary adds plants, water and air). His fairy wings let him fly, and later use up to four forms of the spear at once.
Chastiefol has a series of forms (fan wikis disagree on the count and the numbers; the ones used here agree across the sources I read):

* Form 1, **Chastiefol**: a large ornate spear that he throws; on contact it explodes.
* Form 2, **Guardian**: a bear of moss from the Sacred Tree, four-armed, that King controls. Its body is "heavily resistant to blunt force"; its moisture wards off heat and fire; the same moisture
  makes it vulnerable to attacks that can freeze it solid.
* Form 3, **Fossilization**: a two-headed spear that petrifies what it cuts.
* Form 4, **Sunflower**: a great sunflower that fires energy beams from its centre.
* Form 5, **Increase**: many small spears (one source says blades) controlled by hand.
* Form 8, **Pollen Garden**: a barrier of Sacred Tree pollen that repels enemy attacks, heals the wounds of those inside and removes their pain. (A summary says that in the fight against Zeldris King's
  True Pollen Garden protected the group.)
* Forms 6, 9 and 10 are not known to me. Each form has a "true form" that is stronger.
* The "Nightmare Sins" combined attack uses King's Sunflower with the other Sins' techniques.

Sources (the wiki hosts did not open for me; these are the pages and search summaries I read):
https://nanatsu-no-taizai.fandom.com/wiki/Chastiefol , https://www.cbr.com/spirit-spear-chastiefol-king-facts-seven-deadly-sins/ , https://tasw.fandom.com/wiki/King_(Nanatsu_no_Taizai) ,
https://vsbattles.fandom.com/wiki/King_(Nanatsu_no_Taizai) , https://www.looper.com/782233/kings-powers-from-the-seven-deadly-sins-explained ,
https://www.tumblr.com/woofwong/114937928353/the-various-forms-of-fairy-king-harlequin-s , https://myanimelist.net/character/83801 .

## 2. What the fight is for

The owner's brief: fairy wings, Chastiefol, Disaster as a telegraphed field, aerial combat; and fights that "transcend standard stat checks". Four rules:

| Rule | What it does | The counter |
|---|---|---|
| **The air** | He hovers six blocks up (out of a sword's reach, even from the top of a jump), drifts at most 0.19 blocks a tick (a sprint is 0.28), keeps 7 to 13 blocks from his target, circles slowly. He comes down to 2 blocks for the close moves (gust, Fossilization, the Four Arms), and a dive leaves him on the ground for 2.5 s, exposed (the framework's exposed state: he takes 25% more and none of his defences work). | Ranged blows while he hovers; a charge when he lands; the dive's ring is marked and locked, so the answer is "step out and then run in". |
| **The Guardian** | For 8 s a moss bear stands over him: blades, arrows and blasts land at 35%, fire at 50%, spells and everything else in full. ICE freezes it solid (the shell ends, he is staggered for 2 s and the blow lands in full); anti-magic unmakes it. His Four Arms are only swung while it stands. | Spells, ice, anti-magic. A party of swords and arrows is told so on the action bar ("use spells, or freeze it with ice") at most every 2 s per attacker. |
| **The field** (Pollen Garden, Disaster) | A ring of death on the ground: every beat, green-gold gardens of pollen show; 24 ticks later the field pulses and strikes everyone inside it who is not in a garden (8, or 14 and 16 at the Gates, for the Disaster). **A garden is always within reach**: the planner puts one within 4.5 blocks of every foe in the field (a walking player who needs half a second to react makes it), the gardens never overlap and always stand on floor. | Stand in a garden when the ring closes; burst him (8% of his health in 3 s, anti-magic counts double) and the field ends and he is staggered for 2.5 s. |
| **The Sunflower** | A flower of six lanes (eight at the Gates, and a second volley turned half a petal) leaves a locked point; the lanes are drawn for the whole wind-up and beam together (11 each, not through walls). The centre is never safe, but there is room between the petals from about 3 blocks out (4 for eight petals) for a body 0.8 wide with 0.6 to spare. | Stand BETWEEN two petals. |

Every telegraph is drawn in time and the rules are spelled out in chat the first time each is needed. Nothing here takes a player's body or controls, except Fossilization (below).

## 3. His moves (table of `KingTable`)

| id | label | power | wind-up | cool-down | range | weight | recovery | notes |
|---|---|---|---|---|---|---|---|---|
| chastiefol_throw | Chastiefol: Spirit Spear | 20 | 18 | 110 | 6-30 | 5 | 8 | a lane from his hand to the target that follows for 60% of the wind-up and then locks (white); at the end the spear hits the first foe on the lane (11, a blow: a shield stops it) and bursts there (6 in 2.4 blocks) |
| increase_rain | Chastiefol: Increase | 20 | 24 | 230 | 6-40 | 4 | 10 | crowd; 4 to 7 spears fall 3 ticks apart on marked rings (7 each, ring 1.7): the target, the other foes, then a ring round the target; each foe is struck once per rain |
| dive_strike | Fairy Dive | 20 | 22 | 260 | 6-40 | 3 | 58 | a ring (3.0) and a line from him to it, locked when the wind-up starts; he drops along an eased path (6 to 18 ticks), 14, and lands; he is exposed from the start of the dive for 58 ticks, so 40 to 52 of them are on the ground |
| wing_gust | Fairy Gust | 20 | 16 | 170 | 0-7 | 3 | 8 | a cone 7 long: 4, thrown back, slowed 1.5 s; the way to be given room |
| pollen_garden | Pollen Garden | 40 | 28 | 420 | 0-44 | 3 | 10 | crowd; a field (radius 9) of three beats of 36 ticks, 8 a pulse, three gardens or one for each foe and two over |
| guardian | Chastiefol: Guardian | 40 | 20 | 520 | 0-48 | 2 | 10 | the moss bear of section 2, 8 s |
| guardian_slam | Guardian: Four Arms | 40 | 24 | 200 | 0-12 | 4 | 12 | only while the Guardian stands: a locked ring (3.8), 16, thrown outwards |
| sunflower | Chastiefol: Sunflower | 40 | 26 | 300 | 8-44 | 3 | 10 | the flower of section 2, centred where the target stood when the wind-up began |
| fossilization | Chastiefol: Fossilization | 40 | 18 | 280 | 0-7 | 3 | 12 | a cone 5.5 long, 9; a player is rooted for 2 s at most (the balance law's cap, halved on a repeat, never on creative players, spectators or holders of anti-magic); if the shared control cool-down is running they are only struck |
| disaster | Disaster | 60 | 60 | 1300 | 0-44 | 5 | 60 | ULTIMATE: the field with radius 15, five beats of 32 ticks (six at the Gates, 16 a pulse); two gardens or one for each foe and one over; he hovers at the field's heart, exposed for the first 60 ticks, and starts nothing else; the sacred tree's canopy and a pool of shadow are drawn for its length |

The order of the rows is the index the client reads move names by: only ever append.

## 4. Numbers

* Health 2400, attack 16 (the plain jab is 70%), speed 0.30, armor 7, toughness 5; hitbox 0.5 x 1.59. The Gates (below 25% health) open the second form (a halo, wider wings), refill his bar once.
* Charter Grimoire (30%): air_flight, float, gravity_flight, wind_manipulation, earth_manipulation, multilayer_barrier, spiritual_attack_resistance, poison_resistance (real Tensura 2.0.1.3 ids, tested; the Sin skill "Sloth" is an Ultimate and is not lent).
* Lines: opening "A party in my forest. Fine. I will protect it, even if I would rather be asleep." / Gates "Enough. I have been holding back, and now the Sacred Tree is watching." / death "I... did not want to hurt anyone. Go on. The forest will let you pass."
* Themes feywild_grove, verdant, sky_citadel, celestial_court. Prerequisites: fana, rhya, rades_spirito, patolli.

## 5. Rules kept

No entity spawned, no block changed, nothing heals him (the canon Pollen Garden heals the people inside; here the gardens only spare them, and nobody is healed). Control only through
`BalanceLaw.beginControl / controlTicks` (Fossilization). Damage only through `boss.strike`. Falling damage to him is ignored (he comes down from a hover). No vanilla particle types: `Tells`, and the mod's
own shapes (LIGHT_BLADE, LIGHT_FLARE, TREE_ROOTS, TREE_CANOPY, SHADOW_POOL, WIND_GALE). He hovers by switching gravity off and steering his velocity (the pattern of the Lucius arc's hover:
no ground within 14 blocks and he falls like any mob; a boss saved in the air is freed when it loads).

## 6. Weapon he would give (NOT built; a later job)

**Chastiefol**, the Spirit Spear: a long spear that grows (a real 3D model in the Genesis Demon-Slayer's standard; green-gold glow; its forms are skins of one weapon).
Engravings for `item/WeaponEngravings.java`: `elemental_boost` 2, `vitality` 1, `restoration` 1 (the Pollen Garden's mending), `barrier_piercing` 1.

## 7. Model

`tools/arc/char_king.py`: a small build, a tired face, messy orange hair with a circlet and a flower, a cream tunic with the tree emblem, shorts, a mantle with the tree, green fairy wings,
the spear (1.1 long, glowing gold); clips thrust, magic, hover; second form = a stern face, a halo ring, bigger wings. Previews: `docs/arc/preview/arc_king_front.png`, `arc_king_cast_big.png`, `arc_king_form2_front.png`.
`ArcLook("king", 1.59, 0.99, 1.73, 0.67, 1.45, 1.51, glow)` from `tools/arc/manifest.json` (the wings and the spear are why the growth numbers are big).

## 8. Tests

`KingTableTest` (the scheduler played out; the fields leave a walking player the time to reach a garden; the sunflower leaves room), `GuardianShellTest` (every kind, ice and anti-magic ending it),
`KingFlightTest` (the hover settles from anywhere, the drift is slower than a sprint, a sword does not reach his feet at the high hover, the dive ends exactly on its mark and speeds up),
`SafeGardensTest` (every foe in the field has a garden in reach across 300 random layouts, gardens apart and on floor, a foe at the edge of a pit still gets one), `DisasterBeatsTest`,
`SunflowerMathTest`, `KingSheetTest`, `kit/AimLockTest`. Mutation-checked: see the report.

## 9. Unverified / assumptions

* The numbers of the Guardian's resistances follow the wiki's words (blunt force, heat and fire, freezing); the exact percentages are design. Anti-magic unmaking the Guardian is design.
* Disaster is described only as control over life and death; the field with gardens is design (the Pollen Garden and Disaster combined), not a technique of the story.
* Fossilization as a two-second root is a game form of petrification. Wing Gust, Fairy Dive and the hover are design for "fairy wings".
* The canon list of forms is from fan wikis that disagree on the count; forms 6, 9 and 10 are unknown to me.
* The game glue (the hover, the dive, the zone steps) is type-checked against API stubs and not run in a game; the hover height and the drift were reasoned from the vanilla reach and sprint speeds, not tried.

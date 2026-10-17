# Udaeus (`udaeus`, Level 6, tier 3)

The Monster Rex of the 37th floor, the White Palace: a skeleton giant with a great black sword. Part of [`danmachi.md`](danmachi.md). Class `arc/danmachi/UdaeusBoss`, table `UdaeusTable`, record `DanMachiRoster.UDAEUS`,
model `tools/arc/char_udaeus.py`.

## 1. Canon (as summarised) and sources

* The Monster Rex of the **37th floor**, the White Palace: a huge white skeleton, bare bone, with a **great black sword**; its roar **raises skeleton warriors** (the Spartoi); the ribs move to block blows; the **magic stone** sits behind the sternum.
* Sources (standard page titles, **not opened**): `https://danmachi.fandom.com/wiki/Udaeus`, `https://en.wikipedia.org/wiki/Is_It_Wrong_to_Try_to_Pick_Up_Girls_in_a_Dungeon%3F`.

## 2. Added for the game (not canon)

The tattered dark cloak, the pauldrons and greaves of bone, the violet light in the sword and in the eye sockets, the Rib Cage Cry, the Ossuary Pillars and Rise, the Bone Nova, the Level-Up and the Black Sword Wave; every number.

## 3. Numbers

Health 2,600, attack attribute 26 (plain blow 13), speed 0.25, armor 10, toughness 4. Hitbox 1.0 x 3.91 (the kit's measure for the thin body is 0.55: too thin to hit; widened). Floors 32 to 50 on both roads, after the Amphisbaena.
Charter Grimoire (30%, LEGEND): `create_lesser_undead`, `create_greater_undead`, `heavy_slash`, `aura_sword`, `aura_slash`, `darkness_attack_resistance`, `spiritual_attack_resistance`, `curse`. Lines: "KRAAAAAAAH!" /
"The White Palace rattles: a thousand bones answer! KRAAAAAAH!" / "...clatter...". The voice is the mod's own skeleton sounds, pitched low.

## 4. Moves (base numbers, before the power tuning)

| move | id | power | wind-up | cooldown | range | weight | recovery | flags | what it does |
|---|---|---|---|---|---|---|---|---|---|
| Black Sword Slash | `black_sword_slash` | 20% | 20 t (1.0 s) | 90 t (4.5 s) | 0,7.5 | 6 | 10 | - | cone 6.8 blocks, +-70 deg, 18 dmg, knock 1.5, lifts |
| Overhead Cleave | `overhead_cleave` | 20% | 26 t (1.3 s) | 150 t (7.5 s) | 0,9 | 4 | 14 | - | its own code (reach 9, base 22) |
| Bone Whirl | `bone_whirl` | 20% | 22 t (1.1 s) | 140 t (7.0 s) | 0,7 | 3 | 12 | crowd | circle 6.2 blocks round the body, 13 dmg, knock 1.4 |
| Rib Volley | `rib_volley` | 20% | 24 t (1.2 s) | 150 t (7.5 s) | 6,32 | 4 | 10 | - | 3 projectiles over 36 deg, range 32, 6.5 dmg, 1.3 blocks/tick |
| Ossuary Pillars | `ossuary_pillars` | 20% | 26 t (1.3 s) | 210 t (10.5 s) | 4,34 | 4 | 10 | crowd | 3 mark(s) r 2.3, 11 dmg, knock 1.0, on the wind-up's end |
| Call of the Spartoi | `spartoi_call` | 20% | 30 t (1.5 s) | 720 t (36.0 s) | 0,48 | 2 | 10 | - | its own code (reach 0, base 0) |
| Grave Leap | `grave_leap` | 40% | 26 t (1.3 s) | 230 t (11.5 s) | 8,36 | 3 | 12 | - | leap onto the target's mark, ring 7 blocks, 16 dmg, knock 1.2, 9 ticks in the air |
| Bone Nova | `bone_nova` | 40% | 24 t (1.2 s) | 220 t (11.0 s) | 0,12 | 3 | 12 | crowd | ring wave to 11 blocks, 12 dmg, knock 1.1, 1 ring(s) |
| Rib Cage Cry | `rib_cage_cry` | 40% | 30 t (1.5 s) | 520 t (26.0 s) | 0,40 | 2 | 12 | crowd | its own code (reach 8, base 4) |
| Ossuary Rise | `ossuary_rise` | 60% | 64 t (3.2 s) | 1500 t (75.0 s) | 0,40 | 5 | 60 | ultimate, crowd | its own code (reach 30, base 14) |
| Black Sword Wave | `black_sword_wave` | 100% | 28 t (1.4 s) | 220 t (11.0 s) | 6,34 | 4 | 12 | leveled | 1 lane(s) 30 long, 3.0 wide, 21 dmg, knock 1.4, runs 0.7 tick/block |

* **Black Sword Slash**: a wide lifting cone (70 degrees each side). **Overhead Cleave**: a lane of 9 blocks, 3 wide (22), and a ring of 5 blocks (12) where the blade lands four ticks later. **Bone Whirl**: a circle of 6.2 blocks.
* **Rib Volley**: three ribs at 20% and 40%, five at 60%, seven at 80%, nine at the Gates (the volley size of the power: two more per slot, at most twelve) in a fan of 36 degrees, flying 1.3 blocks a tick for 32 blocks, stopped by a wall or by the first foe they meet (6.5).
* **Ossuary Pillars**: three marks at 20% and 40%, four at 60%, five at 80%, six at the Gates, erupting at the end of the wind-up. **Grave Leap**: a leap of 9 ticks, a ring of 7 blocks (16). **Bone Nova**: a ring of 11 blocks.
* **Call of the Spartoi**: skeletons in iron helmets with iron swords rise round it (36 kit health, 7 attack, scaled), as many as the cap allows.
* **Rib Cage Cry**: shuts the guard wide for 160 ticks (below) and sends three small rings of sound (4) 12 ticks apart.
* **The Ossuary Rise** (ultimate, 60%, 3.2 s wind-up): **four waves of bone pillars** (radius 2.4, 14 each, 4 plus the volley size of pillars: 5 to 8) across the room, 18 ticks apart, each shown 14 ticks before it erupts; the first pillar of
  every wave is on the nearest fighter, the others on the rest and round them, never closer than 1.6 radii. One budget for the whole Rise. Exposed for 60 ticks after.
* **Black Sword Wave** (leveled): a lane of 30 blocks, 3 wide (21) that runs at 0.7 ticks a block (21 ticks to cross).

## 5. The mechanic and its counter (and why): the rib guard (`RibGuard`)

The ribs **move to block the blows aimed at it**. The rule, pure and tested: they face the side the last blow came from; **a blow from within 65 degrees of that side is blocked and does 30%**; the first blow of a fight and every blow
while they are still turning (14 ticks at the opening, 2 fewer for every power step: 6 at the Gates) land in full. **A blow from outside the arc while the guard has settled is a flank**: full damage, **a rib cracks** ("A rib cracks (1/3): strike it
from another side."), and the ribs turn to the new side. **Three cracks (four at the Gates) within 200 ticks of one another break the cage**: Udaeus **staggers for 70 ticks** (it starts nothing and holds still; a wind-up that is not an
ultimate is dropped), **the magic stone shows** (the mod's gem effect follows the chest) and **everything does 160%**. So one fighter hammering from one side is blocked for ever; two fighters on opposite sides, or one who keeps circling, break it in
seconds. **The Rib Cage Cry** shuts the guard wide (100 degrees each side, 80% cut) for eight seconds: strike it from behind. Damage with nobody behind it (fire, poison, the void) is never blocked.

**The Level.** Party Level 6 (mean EP 316,228) gives 1 step (Level 7, Black Sword Wave), 8 gives 2. Udaeus can never take a third step: that would need a party above Level 9, the top.

## 6. Verified and not

Pure and tested: the table, the guard (the arc, the turn, the flank, the cracks, the break, the cry, the memory), the geometry, the Level, the add plan. Looked at: the model (front, face, idle, attack, the leveled form).
**Not verified**: the game side (the angle of a blow as `filterDamage` sees it for projectiles and for the blows of a player's summon, which count as "the summon's owner is not followed"; the volley's flight; the look of the bone effects), the sounds,
the balance, the canon points of section 1, how the kit's `lanky` build looks at 3.9 blocks with a head drawn 1.5 times as big in the game's lighting.

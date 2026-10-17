# The Black Goliath (`black_goliath`, Level 5, tier 2)

The second monster of the saga and the one that teaches that a blow can be the wrong kind of blow. Part of [`danmachi.md`](danmachi.md). Class `arc/danmachi/BlackGoliathBoss`, table
`BlackGoliathTable`, record `DanMachiRoster.BLACK_GOLIATH`, model `tools/arc/char_black_goliath.py`. It is **not** the stone Goliath of the floors (that boss, `goliath`, is its own class and is not touched);
it is that giant's irregular kin and follows it in the story.

## 1. Canon (as summarised) and sources

* The Goliath is the giant boss of the **17th floor**, a stone-skinned giant. The **Black Goliath** is the irregular one: black, faster and much stronger, a "Monster Rex" the Dungeon sent against the Hestia
  Familia's growing power on the 17th and 18th floors (the summaries say anime episode 13 and volume 5 of the novels; **the numbers are the least certain thing here**).
* In the fight as the summaries describe it, the black hide shrugged off the blades of Mikoto and Ouka while the mages' combined spells did enormous damage, and the heavy strikes of Welf and Bell turned it; its roar called more monsters.
* Sources (standard page titles, **not opened**): `https://danmachi.fandom.com/wiki/Goliath`, `https://danmachi.fandom.com/wiki/Black_Goliath`,
  `https://en.wikipedia.org/wiki/Is_It_Wrong_to_Try_to_Pick_Up_Girls_in_a_Dungeon%3F`.

## 2. Added for the game (not canon)

The cracks of ember light over the hide (the sign of the Dungeon's anger), the plates of black stone armour with glowing edges, the crest of stone spikes, the lit mouth; the Fissure Spokes, the Dungeon's Wrath,
the Level-Up and the Titan Slam; the exact numbers of the Black Hide.

## 3. Numbers

Health 1,800, attack attribute 24 (plain blow 12), speed 0.26 (the stone Goliath's is 0.20), armor 12, toughness 4. Hitbox 1.7 x 3.91. Floors 12 to 26 on both roads, after the Minotaur and the stone Goliath.
Charter Grimoire (30%, LEGEND): `giantification`, `steel_strength`, `earth_storm`, `earth_jail`, `earth_attack_resistance`, `body_armor`, `pierce_resistance` (the stone Goliath's own grimoire already lends the wall, the spikes and the kick, so this one lends the other half of a giant). Lines: "GRUOOOOOOH!" / "The black hide cracks and the Dungeon roars through it. GRAAAAAAH!" / "...gruoh...".

## 4. Moves (base numbers, before the power tuning)

| move | id | power | wind-up | cooldown | range | weight | recovery | flags | what it does |
|---|---|---|---|---|---|---|---|---|---|
| Black Fist | `black_fist` | 20% | 20 t (1.0 s) | 90 t (4.5 s) | 0,6 | 6 | 12 | - | cone 5.4 blocks, +-40 deg, 20 dmg, knock 1.6, lifts |
| Giant's Sweep | `giants_sweep` | 20% | 22 t (1.1 s) | 140 t (7.0 s) | 0,8.5 | 4 | 12 | crowd | cone 8.0 blocks, +-100 deg, 14 dmg, knock 1.4 |
| Dungeon Stomp | `dungeon_stomp` | 20% | 24 t (1.2 s) | 170 t (8.5 s) | 0,11 | 4 | 12 | crowd | ring wave to 11 blocks, 13 dmg, knock 1.1, 2 ring(s), 12 ticks apart |
| Boulder Toss | `boulder_toss` | 20% | 26 t (1.3 s) | 200 t (10.0 s) | 8,34 | 4 | 10 | - | 2 mark(s) r 2.8, 12 dmg, knock 1.0, lands 16 ticks after the wind-up |
| Wailing Howl | `wailing_howl` | 20% | 30 t (1.5 s) | 450 t (22.5 s) | 0,18 | 2 | 12 | crowd | its own code (reach 14, base 5) |
| Shoulder Rush | `shoulder_rush` | 20% | 22 t (1.1 s) | 210 t (10.5 s) | 6,28 | 4 | 14 | - | charge 18 long, 3.6 wide, 19 dmg, knock 1.4, 1.2 blocks/tick |
| Fissure Spokes | `fissure_spokes` | 40% | 26 t (1.3 s) | 240 t (12.0 s) | 0,22 | 3 | 12 | crowd | 3 spokes 18 long, 2.4 wide, 15 dmg, knock 1.0 |
| Monster Call | `monster_call` | 40% | 30 t (1.5 s) | 720 t (36.0 s) | 0,48 | 2 | 10 | - | its own code (reach 0, base 0) |
| Dungeon's Wrath | `dungeons_wrath` | 60% | 60 t (3.0 s) | 1400 t (70.0 s) | 0,40 | 5 | 60 | ultimate, crowd | its own code (reach 14, base 14) |
| Titan Slam | `titan_slam` | 100% | 28 t (1.4 s) | 260 t (13.0 s) | 6,34 | 4 | 14 | leveled | leap onto the target's mark, ring 8 blocks, 18 dmg, knock 1.3, 9 ticks in the air |

* **Black Fist** (a lifting cone) and **Giant's Sweep** (a very wide, flat cone: 200 degrees) are the close pair; **Dungeon Stomp** is two rings 12 ticks apart (jump both; one budget); **Boulder Toss**: two marks, 16 ticks after the wind-up.
* **Shoulder Rush**: a charge of 18 blocks at 1.2 blocks a tick; a wall stuns it (same rule as the Minotaur: 60 ticks at the opening down to 36).
* **Fissure Spokes**: three spokes (four at 60%, up to six at the Gates, `scaledCount`) of cracks, one pointing at the target: the gaps between the spokes are the safe ground (at 10 blocks out, three spokes leave gaps of 5 blocks).
* **Wailing Howl**: the same fear as the Minotaur's with the Black Goliath's Level (5 plus steps); the shout is 5.
* **Monster Call**: its roar calls husks and spiders from the floor round it, `AddPlan.toSummon(cap, alive, castSlots)` of them (the cap is `2 + stage`, at most `maxAdds`), 30 kit health and 6 attack scaled by `AddPlan`.
* **The Dungeon's Wrath** (ultimate, 60%, 3 s wind-up): three rings of 14 blocks (14 each) 14 ticks apart, then, after a second of warning drawn on the floor, **six cracks** of 22 blocks (2.6 wide, 15) from its feet at a random phase: the
  gaps are 7.9 blocks wide at 10 blocks out. Exposed for 60 ticks after.
* **Titan Slam** (leveled): a leap of 9 ticks onto the target's mark, a ring of 8 blocks (18), knock 1.3.

## 5. The mechanic and its counter (and why): the Black Hide (`HideState`)

In the story the black hide turned plain blades and answered only to heavy blows and to magic. So: **a plain blow** (a swing of a hand or a weapon, an arrow or other missile that is not magic: `DmBlows.plain`) **does half**
while the hide holds. A **heavy blow** (1.5% of its maximum health or more: 27 at 1,800) does full damage and **splits a plate**; **three plates within 120 ticks (six seconds) break the hide for 160 ticks** (eight seconds), when
everything lands in full. **Anything that is not plain** (a spell, a skill, a technique, anti-magic damage) is never cut. The player learns it from the action bar ("Your blow glances off the black hide. Strike hard, or use a spell."; "A plate of the
black hide splits (2/3)."; the chat when it breaks). Why this and not an armour value: the mod's heavy hitters (a charged strike, a skill) and its casters both have a reason to be there, and a party of one kind is not locked out.

**The Level.** Party Level 5 (mean EP 100,000) gives 1 step (Level 6, Titan Slam, +10%, +3%), 7 gives 2, 9 gives 3. A weaker party only meets the enraged Goliath it would have met anyway.

## 6. Verified and not

Pure and tested: the table, the hide (every threshold, the window, the reset), the geometry, the Level, the add plan. Looked at: the model (front, face, idle, attack, the leveled form).
**Not verified**: the game side (how `filterDamage` sees the sources of other mods' damage, whether a Tensura skill's damage is "not plain" in every case, the spokes' VFX), the sounds (the warden's), the balance, the canon points of section 1.

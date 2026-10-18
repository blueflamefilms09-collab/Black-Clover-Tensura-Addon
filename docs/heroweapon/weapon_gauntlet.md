# Vassal Gauntlet (`vassal_gauntlet`): the fighter's fists

Class SEVEN_STAR, style SWING. In the series the Vassal Gauntlet is one of the seven Seven Star Weapons of Raphtalia's world; its power-up method is Skill Power-up Points and its
holder in the story is Fohl Fayon, the Hakuko (white tiger demi-human) Gauntlet Hero (wiki summaries). The owner names no holder for it. Logic
`heroweapon/weapon/gauntlet/GauntletLogic`, forms `data/nusmp/hero/forms/vassal_gauntlet.json`.
This page is written in sections; the LOOK section comes first because the art builder (`hart2`) reads it first.

## 1. Look (for the art builder)

Source of the look: the owner's text, the seed "an armoured gauntlet (a fist; the hand display must look right)" and the wiki's form names (Small Gauntlets, Sakura Stone of Destiny
Gauntlets). Colours and proportions are DESIGN (the Hakuko holder suggests steel with tiger-orange trim).

* **Silhouette.** ONE right-hand armoured gauntlet, a closed fist: a flared vambrace cuff, a heavy plated back of the hand, four finger plates curled into the palm, the thumb plate
  folded over the fingers, a row of four knuckle studs. Apparent length (cuff to knuckles) 0.55 blocks, largest dimension at least 0.5; the class `handitem` of `weapon_classes.json` (0.5 to 0.9 blocks, measured by the
  largest dimension) fits it. The knuckles are the "tip": the fist points up-forward on the icon's diagonal, the cuff is the butt
  (the hand holds the gauntlet by the cuff).
* **Named parts (10 to 14).** `cuff` (the flared vambrace), `cuff_trim` (a rolled edge), `back_plate` (the back of the hand, slightly domed), `finger_1` to `finger_4` (separate
  curled segments, each two bands), `thumb`, `knuckle_studs` (four), `wrist_strap` (leather), `bezel` (a gold ring) and `gem`.
* **Palette.** Brushed steel plates `#9AA3B2` with darker recesses `#4A5160`; leather strap and lining `#5B3A2A`; trim tiger-orange `#E58A2B` along the cuff edge and the bezel; a small
  band of white fur `#F2EEE6` inside the cuff opening (the Hakuko); gem amber-orange `#FFA43A` with a bright core `#FFD27A`.
* **The gem.** The series puts it on the BACK OF THE HAND: a large oval cabochon (about 0.09 of the length wide) in the gold bezel, in the middle of the back plate, facing up and
  away from the wielder's palm so it shows in first person. It is the strongest spot on the glow skin; the `glow` line colour is `FFA43A`.
* **Glow skin** (additive): the gem (strong), thin orange engraving lines across the back plate (soft), the knuckle studs (faint). Nothing else.
* **In hand.** The mesh origin at the cuff; the fist points up-forward. Sprite (16 x 16, kept as the inventory icon): a steel fist seen from the side with an orange dot on its back.
* **Mood.** Plain, tough and a little feral; nothing delicate.

## 2. Canon and sources

Each fact is tagged [summary] (read in a web search summary of the fandom wiki; the pages themselves were not opened), [memory] or [design].

* [summary] The Vassal Gauntlet (also the Seven Star Gauntlet) is one of the eight Vassal Weapons of Raphtalia's world. Its power-up method is Skill Power-up Points: points on level-up are put into the hero's skills to raise their Rank (I to X), which strengthens a skill and can add effects; separate from the Staff's Magic Power-up Points; reassigned only after 24 hours. https://shield-hero.fandom.com/wiki/Vassal_Gauntlet and https://shield-hero.fandom.com/wiki/Legendary_Weapons/Power-up_Methods
* [summary] The holder is Fohl Fayon, the Gauntlet Hero: half-human, half-Hakuko (white tiger), the Shield Hero's companion, who fights unarmed and uses his fists to break defences; the gauntlets chose him after his sister Atla died in the fight with the Phoenix. https://shield-hero.fandom.com/wiki/Fohl_Fayon
* [summary] The wiki's gauntlet list: Small Gauntlets; Sakura Stone of Destiny Gauntlets (made with technology from Q'ten Lo, especially effective against other heroes); gauntlets effective against spirits; Gauntlet 0 (from the poison of Fitoria's sanctuary; no damage, effective against illegitimate powers; not used here).
* [summary] The wiki's skills: Air Strike Rush, Dragon Slaying Conflagration Fist, Moonlight Kick, Afterimage Palm, Shaking Quaking Fist, Turtle Crush (almost none has a gauntlet attached in the wiki, so the assignment to forms is design).
* [design] The skill point pool, the iron guard stance, Second Wind and Tiger Leap (a leap for the white tiger) are this mod's design; the forms Iron Knuckles, Spirit-Bane Gauntlets and Dragonbreaker Gauntlets are named after the wiki's "effective against spirits" list and the ultimate; Sakura Stone of Destiny Gauntlets is the wiki's name.

## 3. How it plays

A close-range fighter that grows stronger while it keeps hitting. The Skill Power-up Points of the series become a **pool of up to 10 skill points** (`PointPool`, plain arithmetic, tested). The pool FILLS by fighting and is SPENT by the next technique:

* a blow with the gauntlet gives 1 point (2 for a full swing, 3 with Hardened Fist), a technique that hits gives 1, a kill gives 2, each blow absorbed in the Iron Guard gives 1 (3 per stance), Second Wind gives 3;
* each technique spends up to a few points when it is used and hits harder for each one (see the table: +0.5 to +1 raw per point); the Conflagration Fist needs 6 and spends all;
* after 5 idle seconds the pool loses a point every second, so it cannot be banked; the action bar shows the number when it changes.

Spending is decided when the technique is certain to happen (a refused technique spends nothing). The strongest value of every technique, with all its points and against the undead with Spirit Bane, is written in the budget table and a test proves the formula base + points x step + bonus equals it, so the table cannot drift from the mechanics.

State kept per wielder (in `GauntletState`, memory only): the pool, the guard stance, the last number shown. It is dropped on logout, when the weapon is put away and when the server stops.

## 4. Forms

Five forms, no curse series.

| form | name | tier | parents | requirements | mastery | stats | skills (slot: skill) | passives |
|---|---|---|---|---|---|---|---|---|
| `base` | Small Gauntlets | 0 | - | - | 0 | attack_damage 1 | use: rush, sneak: moonlight_kick, key1: iron_guard | rank_points |
| `iron_knuckles` | Iron Knuckles | 1 | base | iron_ingot x40, leather x24 | 120 | attack_damage 2, armor 2, knockback_resistance 0.1 | use: rush, sneak: moonlight_kick, key1: iron_guard, key2: quaking_fist, key3: second_wind | rank_points, hardened_fist |
| `sakura_stone_gauntlets` | Sakura Stone of Destiny Gauntlets | 2 | iron_knuckles | cherry_log x32, pink_petals x24, amethyst_shard x16 | 200 | attack_damage 2, attack_speed 0.3, movement_speed 0.04 | use: rush, sneak: moonlight_kick, key1: afterimage_palm, key2: tiger_leap, key3: iron_guard | rank_points, blossom_step |
| `spirit_bane_gauntlets` | Spirit-Bane Gauntlets | 2 | iron_knuckles | soul_sand x32, bone x32, glowstone_dust x24, tensura:low_quality_magic_crystal x8 | 200 | attack_damage 3, armor_toughness 1, luck 1 | use: rush, sneak: turtle_crush, key1: quaking_fist, key2: iron_guard, key3: second_wind | rank_points, hardened_fist, spirit_bane |
| `dragonbreaker_gauntlets` | Dragonbreaker Gauntlets | 3 | sakura_stone_gauntlets, spirit_bane_gauntlets | blaze_rod x32, dragon_breath x8, diamond x16, tensura:medium_quality_magic_crystal x12 | 320 | attack_damage 5, attack_speed 0.4, armor 3, max_health 4, knockback_resistance 0.2 | use: rush, sneak: turtle_crush, key1: iron_guard, key2: quaking_fist, key3: dragon_slayer | rank_points, hardened_fist, spirit_bane, dragon_heart |

The forms file passes the real forms validator (the job's forms check script); the `tensura:` crystals are ids other systems of the mod already use.

## 5. Techniques

Nine techniques (6 required), one ultimate (only in the top form). Every number lives in `GauntletNumbers` (each technique is a `Rank`: the budget row, how many points it may spend and how much each adds); the live skills are built from the table. Damage only through the shared hit gate, healing through the shared heal gate, no hard control, no block changed, no entity spawned. The wielder is moved only by `Moves` (a lunge of at most 12 blocks on level ground, or a blink to a spot that has room and ground and no lava).

| technique | id | kind | cooldown | cost | raw damage (base..strongest, hits per foe) | foes | heal |
|---|---|---|---|---|---|---|---|
| Air Strike Rush | `gauntlet.rush` | ordinary | 70 ticks (3.5 s) | 1% | 6..9.4 x1 | 3 | 0% |
| Moonlight Kick | `gauntlet.moonlight_kick` | ordinary | 90 ticks (4.5 s) | 1.5% | 7..10 x1 | 8 | 0% |
| Afterimage Palm | `gauntlet.afterimage_palm` | ordinary | 120 ticks (6 s) | 2% | 8..11.4 x1 | 1 | 0% |
| Shaking Quaking Fist | `gauntlet.quaking_fist` | ordinary | 130 ticks (6.5 s) | 2% | 7..11.5 x1 | 8 | 0% |
| Tiger Leap | `gauntlet.tiger_leap` | ordinary | 180 ticks (9 s) | 2% | 7..9.5 x1 | 8 | 0% |
| Turtle Crush | `gauntlet.turtle_crush` | heavy | 200 ticks (10 s) | 3% | 14..19 x1 | 8 | 0% |
| Iron Guard | `gauntlet.iron_guard` | utility | 240 ticks (12 s) | 2% | 5..5 x1 | 1 | 0% |
| Second Wind | `gauntlet.second_wind` | utility | 400 ticks (20 s) | 2% | none | 1 | 12% |
| Dragon Slaying Conflagration Fist | `gauntlet.dragon_slayer` | ultimate | 900 ticks (45 s) | 6% | 20..29 x1 | 7 | 0% |

### Air Strike Rush (`gauntlet.rush`)

* **What.** Lunges about 6 blocks along the horizontal part of the look direction (not while riding), and 4 ticks later strikes every foe along the path it ran (1.3 wide, up to 3 foes) for 6 raw. Spends up to 3 skill points, each adding 0.8 raw (8.4 at most; 9.4 against the undead with Spirit Bane). A technique that hits gives 1 skill point back.
* **Numbers.** ordinary; cooldown 70 ticks (3.5 s); cost 1% of the maximum magicules; raw damage 6..9.4 x1; foes 3; heal 0%. Given by: base:use, iron_knuckles:use, sakura_stone_gauntlets:use, spirit_bane_gauntlets:use, dragonbreaker_gauntlets:use.
* **Tell.** The lunge itself: the wielder is seen crossing the ground in a streak of orange light, with a swing and a whoosh; the blow lands where the streak ended.
* **Counter.** Step aside out of the 1.3 block lane, block, or back away; the lunge cannot turn.

### Moonlight Kick (`gauntlet.moonlight_kick`)

* **What.** A rising crescent kick. Refused when no foe is within 3.8 blocks in front. Otherwise the wielder spends up to 2 points (each +1 raw), the two edges of the arc (55 degrees to either side) are drawn, and 0.2 s later every foe still inside the arc takes 7 raw (9 with points, 10 against the undead with Spirit Bane) and is thrown up (a lift of 0.55 plus 0.1 per point; a boss only stumbles). Up to 8 foes.
* **Numbers.** ordinary; cooldown 90 ticks (4.5 s); cost 1.5% of the maximum magicules; raw damage 7..10 x1; foes 8; heal 0%. Given by: base:sneak, iron_knuckles:sneak, sakura_stone_gauntlets:sneak.
* **Tell.** Two dust lines mark the edges of the arc and a crescent sweeps it, 4 ticks before the blow.
* **Counter.** Leave the arc in the 0.2 s it shows; it does not follow a foe that moves out.

### Afterimage Palm (`gauntlet.afterimage_palm`)

* **What.** Slips behind the nearest foe in front (within 70 degrees of the look direction to either side, up to 9 blocks): the wielder blinks to 1.4 blocks behind it, looking at it, and leaves an afterimage where they stood. 3 ticks later an open palm hits for 8 raw, +0.8 per point spent (3 at most; 10.4, 11.4 against the undead) and knocks a raised shield out of the foe's hands. Refused when there is no foe, or no room and ground behind it.
* **Numbers.** ordinary; cooldown 120 ticks (6 s); cost 2% of the maximum magicules; raw damage 8..11.4 x1; foes 1; heal 0%. Given by: sakura_stone_gauntlets:key1.
* **Tell.** The afterimage (a mirror step) at the start place and a wind ring at the arrival; the foe has 3 ticks.
* **Counter.** The palm lands 0.15 s after the blink and the afterimage shows where the wielder came from: a foe that turns and moves away in time avoids it. A raised shield only gets broken.

### Shaking Quaking Fist (`gauntlet.quaking_fist`)

* **What.** Gathers for 8 ticks and drives a shock along a line 7 blocks long (1.4 wide, stopped by a wall) in the look direction: up to 8 foes take 7 raw, +0.7 per point (5 at most; 10.5, 11.5 undead), a shove of 1.1 and Slowness II for 1 s.
* **Numbers.** ordinary; cooldown 130 ticks (6.5 s); cost 2% of the maximum magicules; raw damage 7..11.5 x1; foes 8; heal 0%. Given by: iron_knuckles:key2, spirit_bane_gauntlets:key1, dragonbreaker_gauntlets:key2.
* **Tell.** A dust line along the lane and a heat aura round the wielder for 8 ticks, then lanes of compressing chevrons along the line and an impact at its end.
* **Counter.** Leave the lane within 0.4 s; it is fixed when the gathering starts.

### Tiger Leap (`gauntlet.tiger_leap`)

* **What.** Leaps about 7 blocks along the look direction (not while riding) and comes down at a landing place that is shown at once; when the wielder lands (11 ticks) the ground breaks: up to 8 foes within 3.2 blocks of the wielder take 7 raw, +0.5 per point (3 at most; 8.5, 9.5 undead) and a shove of 0.9.
* **Numbers.** ordinary; cooldown 180 ticks (9 s); cost 2% of the maximum magicules; raw damage 7..9.5 x1; foes 8; heal 0%. Given by: sakura_stone_gauntlets:key2.
* **Tell.** A dust ring at the landing place at the start and again at the landing, a streak in the air.
* **Counter.** Stay out of the landing ring; the leap is a straight line.

### Turtle Crush (`gauntlet.turtle_crush`)

* **What.** The heavy technique. Raises a heavy fist, then crushes the ground 2.2 blocks in front (radius 3.4): up to 8 foes lose a raised shield, take 14 raw, +1 per point (4 at most; 18, 19 undead), Weakness I for 3 s and a shove of 0.5.
* **Numbers.** heavy; cooldown 200 ticks (10 s); cost 3% of the maximum magicules; raw damage 14..19 x1; foes 8; heal 0%. Given by: spirit_bane_gauntlets:sneak, dragonbreaker_gauntlets:sneak.
* **Tell.** A dust ring on the ground where it will land, a well of crowding rings and a heat aura on the wielder, for 14 ticks (0.7 s).
* **Counter.** Leave the ring in 0.7 s; a shield is no answer (it breaks) but distance is.

### Iron Guard (`gauntlet.iron_guard`)

* **What.** A stance for 3 s: the wielder takes 40 percent less from every blow and shot that has a source (not from fire or falling), each such blow gives 1 skill point (3 per stance at most), and the FIRST blow is answered by a counter on the attacker if it is a foe within 6 blocks: up to 5 raw, and never more than the damage that was about to be taken, with a shove of 0.6.
* **Numbers.** utility; cooldown 240 ticks (12 s); cost 2% of the maximum magicules; raw damage 5..5 x1; foes 1; heal 0%. Given by: base:key1, iron_knuckles:key1, sakura_stone_gauntlets:key3, spirit_bane_gauntlets:key2, dragonbreaker_gauntlets:key1.
* **Tell.** A heat aura on the wielder for the 3 s and a shield sound.
* **Counter.** It only reduces; wait the 3 s out and strike after, or use it as the moment to hit something else. Damage with no attacker (fire, falling, effects) is not reduced by the stance.

### Second Wind (`gauntlet.second_wind`)

* **What.** The wielder heals 12 percent of their health through the heal gate, washes away one harmful effect and gains 3 skill points.
* **Numbers.** utility; cooldown 400 ticks (20 s); cost 2% of the maximum magicules; raw damage none; foes 1; heal 12%. Given by: iron_knuckles:key3, spirit_bane_gauntlets:key3.
* **Tell.** A spirit aura on the wielder for 1.5 s and a breath sound.
* **Counter.** None needed: a heal. Burst the wielder before it, it takes 20 s to come back.

### Dragon Slaying Conflagration Fist (`gauntlet.dragon_slayer`)

* **What.** The ultimate. Needs 6 skill points counted (Dragon Heart counts one more than the pool holds, up to the cap of 10) and a foe in the line (7 blocks, 1.6 wide); otherwise refused and nothing is spent. Then it spends ALL the points. After 1 s the first foe in the line, if it is still within 10 blocks, takes 20 raw plus 2 for each counted point beyond 6 (28 at the cap; 29 against the undead) and a shove of 0.9; every other foe within 3.5 blocks of it (up to 6 more) takes 10 raw. A guard is broken first.
* **Numbers.** ultimate; cooldown 900 ticks (45 s); cost 6% of the maximum magicules; raw damage 20..29 x1; foes 7; heal 0%. Given by: dragonbreaker_gauntlets:key3.
* **Tell.** A dust line from the wielder to the foe, a dust ring of 3.5 blocks round the foe, a flame aura on the wielder and a flame burst at the fist for the whole second.
* **Counter.** Step out of the line and the ring in the 1 s (the blow checks the foe's place when it lands: if the foe is gone or farther than 10 blocks, "The Conflagration Fist missed" and the points are lost).

## 6. Passives

| passive | id | effect | forms that have it |
|---|---|---|---|
| Skill Power-up Points | `rank_points` | Blows, techniques and the hits your guard takes fill a pool of up to 10 skill points; the next technique spends them to hit harder. They fade after 5 idle seconds. | base, iron_knuckles, sakura_stone_gauntlets, spirit_bane_gauntlets, dragonbreaker_gauntlets |
| Hardened Fist | `hardened_fist` | You take 8 percent less damage and a full swing gives one more skill point. | iron_knuckles, spirit_bane_gauntlets, dragonbreaker_gauntlets |
| Blossom Step | `blossom_step` | Afterimage Palm and Tiger Leap can be used again 15 percent sooner. | sakura_stone_gauntlets |
| Spirit Bane | `spirit_bane` | Your techniques deal 1 more damage to undead foes. | spirit_bane_gauntlets, dragonbreaker_gauntlets |
| Dragon Heart | `dragon_heart` | The Conflagration Fist counts 1 skill point more than you hold: it opens at 5 and hits harder with the same pool. | dragonbreaker_gauntlets |

## 7. Engravings (the owner's rule 6: every weapon gets Tensura engravings)

The line for `item/WeaponEngravings.java`:

```java
Map.entry("vassal_gauntlet", List.of(e("crushing", 2), e("barrier_piercing", 1), e("sturdy", 1))),
```

* `crushing` 2: the series' gauntlet "breaks down defences" with a fist; Turtle Crush and the Conflagration Fist are blows of crushing weight.
* `barrier_piercing` 1: the Shaking Quaking Fist drives a shock through what stands in the way and the Afterimage Palm breaks a raised guard.
* `sturdy` 1: a gauntlet is armour as well as a weapon; the Iron Guard and Hardened Fist are about taking blows.

## 8. Files

* Logic: `heroweapon/weapon/gauntlet/`: `GauntletLogic`, `GauntletSpells`, `GauntletNumbers` (no game classes), `GauntletText` (no game classes), `GauntletState`, `PointPool` (no game classes).
* Shared kit (also used by the staff, the projectile and the carriage): `heroweapon/weapon/kit/`, described class by class in `weapon_staff.md` section 8.
* Forms: `src/main/resources/data/nusmp/hero/forms/vassal_gauntlet.json`. Lang: the builder's fragment (`hero.skill.gauntlet.*`, `hero.passive.gauntlet.*`).
* Tests: `PointPoolTest` (cap, spend, fade, re-anchoring), `GauntletNumbersTest` (budget, the formula behind every strongest value, the ultimate's minimum), `GauntletFormsTest`.

## 9. Tests and the self-test

* JUnit: the pool's arithmetic (including the edges: fade after exactly the idle time, a gain restarts it, spending more than held), the budget table, `rawMax == base + points x step + undead bonus` for each rank, the ultimate's formula, the forms file and the texts.
* Self-test `hero.weapon.gauntlet`: resolves the forms file and every skill id of every form, binds the weapon to a stand-in player at the world spawn with the weapon in hand, casts all nine techniques once beside a dummy husk (any exception is a failure), checks the live skills against the table, and removes everything it made.

## 10. What is unverified (honest list)

Nothing has been run in the game. Type-checked against API stubs: yes. Unit-tested: the pool, the numbers, the data. Not verified:

* How the lunge and the leap feel and where they end (`Moves.dash` pushes by an initial speed worked out for ground friction: it is right on level ground and only approximate on slopes, ice and in water; the landing ring is placed from a ray and may not match where the wielder lands).
* That a refusal of Afterimage Palm (no room behind the foe) is common enough to annoy; the foe's back is computed from its look direction.
* How the effects look (the gravity and fire shapes were chosen from their source comments; the colour of `FX_SPIRIT_OVERDRIVE`, `GRAVITY_*` and `MIRROR_STEP` may be their own).
* Whether the dispatcher shows the action bar line "Skill points n/10" without fighting another system's use of the action bar.
* The behaviour of the stand-in player used by the self-test (swing, ray queries, a teleport for the blink).

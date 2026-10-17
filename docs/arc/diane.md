# Diane, the Serpent's Sin of Envy (boss, tier 3)

Builder `sdsb`. Saga `SEVEN_DEADLY_SINS`, tier 3, health 2600, floors 20 to 60 on either road (the first of the four, because a giant needs the most room). Opens with the other
three Sins after the bosses of the Eye of the Midnight Sun (Saga 1), in any order. Code: `arc/sds/Diane*.java`, `StanceClock`, `DanceStacks`, `MetalSkin`, `CrystalShield`, `WaveMath`;
model `tools/arc/char_diane.py`.

## 1. Canon (what the fight is built from)

Diane is the Serpent's Sin of Envy, a member of the Giant Clan (about 30 ft as a giant, 165 cm at human size). Her Sacred Treasure is the war hammer **Gideon** (a two-handed hammer; one
profile gives its weight as 998 kg); her inherent power is **Creation**: a giant can shape the earth and other minerals such as metal and crystal.

* **Techniques of Creation** (fan wikis; the lists and descriptions differ between pages): **Rising Meteor** (she raises Gideon and a star-shaped stone pillar erupts under the opponent and launches them),
  **Rush Rock** (she pulls many rocks from the ground and hurls them), **Heavy Metal** (her body turns to metal: more weight, strength and durability; it can be melted by high heat),
  **Queen Embrace** (she rides a giant earth wave over the enemies and buries them), **Diamond Shield** (descriptions conflict: a giant metal mask raised from the ground, or a barrier of diamond ore).
* **Drole's Dance**: a technique of the giant king Drole, learned after his trial. By dancing she deepens her bond with the earth and her power rises the longer she keeps dancing (one fan wiki says tenfold; one source only).
* Size: she shrinks to human size at times (one source: at the Vaizel Fighting Festival); the cause is not given in the pages I read.
* Look: brown hair in twin tails, purple eyes, an orange suit with boots to the knees, an armband and studded gauntlets, the serpent mark on the outside of her left thigh.

Sources (the wiki hosts did not open for me; these are the pages and search summaries I read):
https://nanatsu-no-taizai.fandom.com/wiki/Diane , https://nanatsu-no-taizai.fandom.com/wiki/Creation , https://nanatsu-no-taizai.fandom.com/wiki/Gideon , https://nanatsu-no-taizai.fandom.com/wiki/Giant_Clan ,
https://tasw.fandom.com/wiki/Diane_(Nanatsu_no_Taizai) , https://vsbattles.fandom.com/wiki/Diane_(Nanatsu_no_Taizai) , https://looper.com/813351/dianes-powers-from-the-seven-deadly-sins-explained ,
http://thesevendeadlysins-boarhat.com/personnages/techniques/diane.php , https://myanimelist.net/character/74653 .

## 2. What the fight is for

The owner's brief: "a big slow body with earth magic and a size-change mechanic". The fight is about SIZE, and every size has a rule a party can read:

| Rule | What it does | The counter / the use |
|---|---|---|
| **Two sizes** | She opens at human size (5 s) and then GROWS (30-tick wind-up: a ring on the ground is the quake, 10 damage, thrown outwards) into a giant whose hitbox is 1.95 times as big (1.4 x 3.9), who walks 15% slower and takes 10% less. A giant shrinks by herself after 45 s. If there is no room for the giant's box (a low ceiling, a wall) she steps up to 4.5 blocks to find some, and if she finds none she stays human and tries again 20 s later (and says so once). | The ring tells you where the quake lands; the size tells you which moves to expect. |
| **Cut down to size** | A burst of 9% of her health in 4 s while she is a giant (14% at the Gates; anti-magic counts double; once a 30 s) cuts her down at once: whatever she was casting is lost, a dance ends and takes its stacks back, her metal and shield go, and she is HUMAN for 10 s: smaller, quicker, and she takes 15% more. Then she grows again, telegraphed. | The reward for burst damage: ten seconds of an easier Diane. |
| **Heavy Metal** (human) | 6 s of metal: EVERY blow loses a fixed amount (0.4% of her health, 10) but at least a fifth of it always passes. Fire is not cut and 4% of her health in fire inside 3 s melts the metal for good (she is open for 1 s). | Big hits (a charged blow, a heavy spell) pass almost whole; swarms of small hits and damage-over-time are walled; fire. |
| **Diamond Shield** (human) | A pool of 6% of her health that absorbs for up to 8 s; when it is gone she is open for 2 s (and takes 15% more). Nothing refills it. | Burst it. A defence is never cast while another stands. |
| **Drole's Dance** (ultimate, giant only) | After a 60-tick wind-up, five stomps (seven at the Gates), one a second: the foot rises for 8 ticks (a ring at the full reach, 10 blocks, and a ring closing in), then a ring of earth runs out from her feet at 0.55 blocks a tick, 10 damage to everything on the ground it crosses. Every stomp she finishes is a stack: +6% damage (five at most, one fades every 5 s). She does not move. She is exposed for the first 50 ticks. | JUMP the ring as it reaches you (a body whose feet are 0.6 blocks off the floor is clear); stay beyond 10 blocks; or cut her down to size, which stops the dance and takes back the stacks it gave. |

Everything is drawn: the smash and the meteors are locked rings that close, the sweep and Queen Embrace are lanes that follow you for 60% (50%) of the wind-up and then lock (white), the dance's rhythm is the foot rising.
She has no control move at all: her answer to being small and fast is her weight.

## 3. Her moves (table of `DianeTable`)

Group: **either** size, **giant**, **human**. The two size changes are free, started by the boss's clock and never drawn by the scheduler.

| id | label | power | wind-up | cool-down | range | weight | recovery | group / notes |
|---|---|---|---|---|---|---|---|---|
| gideon_smash | Gideon: Smash | 20 | 26 | 150 | 0-10 | 5 | 14 | giant: a locked ring (4.2) closing in, 17, thrown outwards |
| gideon_sweep | Gideon: Sweep | 20 | 24 | 130 | 0-8 | 4 | 14 | giant: a cone 8.5 long, 126 degrees, 14; locks like the lane (60%) |
| queen_embrace | Queen Embrace | 40 | 30 | 360 | 6-36 | 3 | 12 | either: a wall of earth down a lane 26 long and 6.4 wide at 0.9 blocks a tick, 15 once per foe, knocked up and along; the lane locks at half the wind-up; jumping does not help, the side does |
| rising_meteor | Rising Meteor | 20 | 28 | 260 | 4-36 | 4 | 10 | either, crowd: 3 to 6 star pillars on locked rings (2.4): the target, the others, then a ring; 12 each, thrown up |
| drole_dance | Drole's Dance | 60 | 60 | 1300 | 0-48 | 5 | 50 | giant, ULTIMATE (section 2) |
| rush_rock | Rush Rock | 20 | 16 | 90 | 4-32 | 5 | 8 | human: 3 to 6 slow straight rocks in a fan, 5 each |
| heavy_metal | Heavy Metal | 20 | 20 | 480 | 0-48 | 2 | 10 | human (section 2) |
| diamond_shield | Diamond Shield | 40 | 22 | 420 | 0-48 | 2 | 10 | human (section 2) |
| hammer_swing | Gideon: Swing | 20 | 14 | 70 | 0-5.5 | 5 | 8 | human: a cone 4.6 long, 11, pushed back |
| giants_pride | Giant's Pride | 20 | 30 | 400 | 0-48 | 1 | 14 | the growth of section 2, free, by the clock |
| human_size | Human Size | 20 | 24 | 400 | 0-48 | 1 | 10 | the shrinking, free, by the clock |

A Diane who cannot grow still has six moves of her own (rush rock, hammer swing, heavy metal, diamond shield, queen embrace, rising meteor). The first rows keep their places (the client reads move names by index): only ever append.

## 4. Numbers

* Health 2600, attack 17 (the plain swat is the full blow as a giant, 80% as a human), speed 0.26, armor 11, toughness 7; hitbox 0.7 x 2.0 at human size (x 1.95 as a giant: she is 1.4 x 3.9).
  She stands 3.5 to 9 blocks from her target as a giant, 5 to 14 as a human.
* The Gates (below 25% health) open the second form (gold light lines, a rune emblem, glowing scale armour), refill her bar once and make a human Diane grow at once; the dance has seven beats and a burst must be 14%.
* Charter Grimoire (30%): giantification, strengthen_body, body_armor, earth_manipulation, earth_wall, earthshatter_kick, stone_shot, reinforced_barrier (real Tensura 2.0.1.3 ids, tested; the Sin skill "Envy" is an Ultimate and is not lent).
* Lines: opening "Hi! You came all this way to fight me? Okay, but I am not going to hold back just because you are small!" / Gates "Now I get to be serious! Watch the ground, it is mine!" / death "Ow... you are really strong. I wanted to dance a bit longer, though."
* Themes mountain_monastery, dwarven_forgehold, crystal_geode, verdant. Prerequisites: fana, rhya, rades_spirito, patolli.

## 5. How the size works in the game (for the lead and for the client)

* The entity type is registered at her HUMAN size (0.7 x 2.0). A synced boolean `GIANT` (her own accessor, so the Gates' second model does not appear when she merely grows) makes `getDefaultDimensions` 1.95 times
  as big on both sides and `refreshDimensions()` follows the flag. The renderer already scales the model by the hitbox over `ArcLook.height` (clamped to 0.25 to 4), so the same model is drawn 1.95 times as big.
* `ArcLook("diane", 2.0, 1.21, 2.15, 1.0, 2.6, 1.8, glow)`: the first three numbers are `tools/arc/manifest.json` (human size); the shadow and the two culling numbers are the manifest's times the giant's scale (0.5, 1.22, 0.92 become 1.0, 2.6, 1.8), so a giant is neither culled at the edge of the screen nor given a human shadow.
* The growth first asks whether the box of 1.4 x 3.9 fits (`Level.noCollision`), so she can never end up in a wall.
* Her walking speed carries a 15% modifier (id `nusmp:diane_giant_speed`) while she is a giant, removed when she is human.

## 6. Rules kept

No entity spawned, no block changed, no healing (the shield is a pool of damage, not health). No control move at all. Damage only through `boss.strike`. The stone effects are the Goliath's own
(`BossMoveVfx`: BOSS_STONE_RING, _COLUMN, _FISSURE) and EARTH_RAMPAGE for the wave; the dust is `Tells`; no vanilla particle types.

## 7. Weapon she would give (NOT built; a later job)

**Gideon**, the war hammer: a heavy two-handed hammer in the Genesis Demon-Slayer's standard (a real 3D model, orange-gold glow).
Engravings for `item/WeaponEngravings.java`: `crushing` 3, `sturdy` 2, `elemental_boost` 1 (earth), `magic_protection` 1.

## 8. Model

`tools/arc/char_diane.py`: an average build 2.0 tall (widened 12%), a cheerful face, purple eyes, brown twin tails, an orange dress and shorts, knee boots, gauntlets and plate bracers, the hammer Gideon (1.25 long, glowing orange);
clips smash, magic, relaxed; second form = gold light lines, a rune emblem, glowing scale armour. Previews: `docs/arc/preview/arc_diane_front.png`, `arc_diane_cast_big.png`, `arc_diane_form2_front.png`.

## 9. Tests

`DianeTableTest` (the scheduler played out at each size: opening moves only before 40%, no dance before 60%, 400 ticks, or as a human, no spam, every move of the size used at the Gates, no dead range at either size),
`StanceClockTest` (the opening, the growth, the 45 s, being cut down, no room, the Gates), `DanceStacksTest`, `MetalSkinTest` (the flat cut, the fifth that always passes, fire and the melt), `CrystalShieldTest`,
`WaveMathTest` (a ring or a wall never skips a body, a jump clears a low ring), `DianeSheetTest`, `kit/BurstGaugeTest`, `kit/MoveRowTest`. Mutation-checked: see the report.

## 10. Unverified / assumptions

* Rising Meteor, Rush Rock, Heavy Metal, Queen Embrace and Diamond Shield are named as the fan wikis name them; their descriptions in the wiki differ. Drole's Dance as a rhythm of ring-stomps is design;
  the tenfold power is one wiki's claim and is NOT used. The cause of her human size is not in my sources: the clock and the burst are design.
* The numbers of the flat cut, the shield pool and the cut-down window are design.
* The `GIANT` flag is not saved with the entity (she is human after a reload, which is also how a fight starts). `refreshDimensions()` on the client follows `onSyncedDataUpdated`; I could not run a client.
* The game glue is type-checked against API stubs and not run in a game.

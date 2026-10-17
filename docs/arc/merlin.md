# Merlin, the Boar's Sin of Gluttony (boss, tier 3)

Builder `sdsb`. Saga `SEVEN_DEADLY_SINS`, tier 3, health 2500, floors 50 to 90 on either road. Opens with the other three Sins after the bosses of the Eye of the
Midnight Sun (Saga 1), in any order. Code: `arc/sds/Merlin*.java`, `CancelStance`, `CubeShell`, `InfinityFields`; model `tools/arc/char_merlin.py`.

## 1. Canon (what the fight is built from)

Merlin is the Boar's Sin of Gluttony and the greatest mage in Britannia, master of Vivian and teacher of King Arthur of Camelot.

* **Infinity**, her inherent power: it stops the time of her own spells, so they last for ever. It can only be used together with another magic (one fan wiki).
* **Absolute Cancel**: she lays her hands on a spell and erases it. A spell that is too powerful is only suspended for a while. One source says it works only on what her
  hands or her enchantment stones touch. It is not stated to work on a sword.
* **Perfect Cube**: a barrier that denies entry to everyone but the caster and reflects attacks, strong or weak, back at the attacker. Meliodas undid it by touching it
  with his cane, which the wiki takes as proof he used Absolute Cancel.
* **Aldan** (the Morning Star), her Sacred Treasure: a crystal orb that floats round her palm.
* **Chrono Coffin**: a forbidden art that stops time in a place. A Spanish source says it takes minutes to prepare. In the fight against Zeldris she finished it after
  the battle was won.
* **Weakness** (a databook, via a fan wiki): extreme concentration, so she forgets her surroundings while she casts.
* Other spells: Location, Power Amplify, Magic Seal, Enchant: Infinity, teleportation (a fan wiki credits her with portals; "Transfer" as a name is NOT confirmed).

Sources (the wiki hosts did not open for me; these are the pages and search summaries I read, so read every detail as second hand):
https://nanatsu-no-taizai.fandom.com/wiki/Merlin , https://nanatsu-no-taizai.fandom.com/wiki/Merlin/Plot , https://nanatsu-no-taizai.fandom.com/wiki/Aldan ,
https://vsbattles.fandom.com/wiki/Merlin_(Nanatsu_no_Taizai) , https://tasw.fandom.com/wiki/Merlin_(Nanatsu_no_Taizai) , https://factvsfiction.fandom.com/wiki/Merlin ,
http://www.thesevendeadlysins-boarhat.com/personnages/techniques/merlin.php , https://myanimelist.net/character/95981 .

## 2. What the fight is for

The owner wants these fights to "transcend standard stat checks": a party that only out-damages the boss must be made to think. Merlin is the boss for the player's own
magic. Three rules, each shown in a telegraph, each with a counter the party can learn:

| Rule | What it does | The counter |
|---|---|---|
| **Absolute Cancel** (a stance) | The first SPELL that reaches her open hand from the front (130 degrees, 170 at the Gates) is erased, its caster's grimoire is silenced for 1.5 s, and she DEVOURS it (Gluttony): 0.6 s later she sends 55% of its power back at the caster as one bolt (6 to 26 base damage). One charge (two at the Gates), 2.2 s (3.2 at the Gates). | Throw a cheap spell first to spend the charge; hit from the side or behind; use a blade, an arrow or a blast (she cannot erase them); anti-magic breaks the stance. |
| **Perfect Cube** | Spells and shots from beyond arm's reach are reflected (a share of the damage back at the thrower, 3 to 14 base), three times (four at the Gates; a multi-hit spell costs one). Blows of the arm cross it at 60%. When the charges are gone it shatters and she is staggered. 4 s (5.5 s at the Gates). | Melee; anti-magic (breaks it outright); spend the charges on small shots. |
| **Infinity** | Every move costs her no mana ("endless magic uptime"). The Infinity fields she lays (two at once, three at the Gates) never end on a timer: they last until she is staggered. A field ticks once a second (2.5, growing 0.6 a second inside, at most 7) and slows. | Leave the field; burst her (7% of her health in three seconds, anti-magic counts double) and every field ends and she is staggered for 2 s. |

Everything she does can be read in time: every move has a telegraph (a cone for the hand, a locked ring for a mark, lines for bolts, a gate for a transfer), the stance
and the cube are announced in chat and drawn on the ground, and the marks lock when the wind-up starts, so the answer is always "move out of the ring" or "choose another kind of attack".

## 3. Her moves (table of `MerlinTable`; every move is free of mana)

| id | label | power | wind-up | cool-down | range | weight | recovery | notes |
|---|---|---|---|---|---|---|---|---|
| arcane_volley | Arcane Volley | 20 | 18 | 100 | 6-34 | 5 | 8 | 3 to 6 homing bolts (+2 at the Gates), 5.5 each, in a fan |
| aldan_strike | Aldan Strike | 20 | 14 | 80 | 0-7 | 5 | 10 | locked ring (3.4), 13, thrown outwards |
| transfer | Transfer | 20 | 16 | 150 | 0-9 | 3 | 6 | a gate marks where she will be (10 to 13 blocks round the target); she steps through and fires one snap (5) |
| absolute_cancel | Absolute Cancel | 20 | 14 | 300 | 5-40 | 4 | 14 | the stance of section 2 |
| perfect_cube | Perfect Cube | 40 | 20 | 360 | 0-48 | 3 | 12 | the shell of section 2; foes within 3.4 are pushed out, unhurt |
| infinity_field | Infinity: Enchant | 20 | 24 | 260 | 4-34 | 4 | 10 | crowd; a locked sigil (5.5) that stays |
| starfall | Starfall | 40 | 24 | 230 | 4-36 | 4 | 10 | crowd; 4 to 7 stars (9 each) on the target, the others, then a ring, never stacked; each foe is hit once per fall |
| magic_seal | Magic Seal | 40 | 20 | 320 | 5-34 | 2 | 8 | locked ring (2.2): grimoire silenced 2.5 s, 4 damage |
| power_amplify | Power Amplify | 60 | 16 | 560 | 0-48 | 1 | 12 | her spells hit 20% harder for 8 s; anti-magic or a burst ends it |
| mass_transfer | Mass Transfer | 60 | 26 | 380 | 0-48 | 2 | 14 | crowd; up to four players are each marked with a ring; whoever is still in theirs is moved 7 to 12 blocks from her, through two gates, and a snap (6) follows 12 ticks later; under the balance law |
| spell_library | Spell Library | 20 | 16 | 260 | 6-30 | 2 | 8 | one spell from Tensura's own magic (wind, space ...) or three bolts if there is none |
| chrono_coffin | Chrono Coffin | 60 | 64 | 1300 | 0-40 | 5 | 60 | ULTIMATE, see below |

The first row positions are the index the client reads move names by: only ever append.

### Chrono Coffin (ultimate)

A cube (half-side 5 blocks, up to 7 at the Gates) closes round the target's place after the longest wind-up she has: 64 ticks. The cube's edges are drawn in dust for the whole
wind-up and the clock face of the Time Magic shape is shown on the floor. Whoever is inside when the wind-up ends is frozen by the mod's own time stop (`TimeStop.freeze`,
60 ticks at the most, shorter on a repeat, not on creative players, spectators, holders of anti-magic or anyone already immune) and two seconds later the cube collapses for 20 to everyone still inside it.
**Counters**: leave the cube during the wind-up (it is locked at the first tick); or deal 9% of her health to her while she casts ("she forgets her surroundings when she concentrates"): the
Coffin falls apart, she is staggered for 2.5 s. It is the only move she cannot be stopped in by the ordinary 7% burst; it takes the larger 9%.

## 4. Numbers

* Health 2500, attack 16 (only the plain swat, halved: she is a caster), speed 0.33, armor 9, toughness 6; hitbox 0.55 x 1.91.
* Infinity as a number: every move of hers costs 0 mana at every power. (The framework frees every boss's mana from 60%; at 20% and 40% the other bosses have a pool of 100 to 140 that a move costs 6 to 32 of, so there the difference is real.)
* She keeps 8 to 14 blocks away. Power steps 20/40/60/80/100%; the Gates (below 25% health) open the second form (a gear halo), refill her bar once, give her 3 fields, 2 cancels, 4 cube charges, 8 bolts.
* Charter Grimoire (30% chance): magic_jamming, chant_annulment, mana_manipulation, thought_acceleration, warp_portal, teleport, multilayer_barrier, magic_sense (all real Tensura 2.0.1.3 ids, tested; no Unique or Ultimate skill: the Sin skill "Gluttony" is an Ultimate and is not lent).
* Lines: opening "Another party that came all this way. Show me everything you know, and I will show you what I have learned." / Gates "Infinity, unbound. Nothing I cast from here on will end." / death "Erased... no. Beaten. I will remember how you did it."
* Themes astral, spatial, cursed_archive, crystal_geode. Prerequisites: fana, rhya, rades_spirito, patolli (the Eye of the Midnight Sun, as named by its builder).

## 5. Rules kept

No entity is spawned and no block is changed. No free healing (nothing heals her; the devoured spell is a bolt, not health). Control only through `BalanceLaw.beginControl / controlTicks`
(Magic Seal, the Mass Transfer, the Coffin, the silence of a cancel; never creative, spectators or a holder of anti-magic). Damage only through `boss.strike`. All effects are the mod's own
(`VfxSpawn` shapes, `Tells`), no vanilla particle types; the Time Magic shapes are only used, never changed.

## 6. Weapon she would give (NOT built; a later job)

**Aldan**, the Morning Star: a floating orb and spell catalyst held in hand (a staff or an orb weapon), a real 3D model in the Genesis Demon-Slayer's standard, glowing violet-gold.
Engravings for `item/WeaponEngravings.java` (Tensura ids; applied once): `magic_weapon` 2, `magic_interference` 1 (Absolute Cancel), `magicule_absorption` 1 (Gluttony devours), `elemental_boost` 1.

## 7. Model

`tools/arc/char_merlin.py` (art kit): slim, pale, black bob with a violet streak, yellow eyes, an indigo dress with a fur collar and bell sleeves, a gold choker and sash, knee boots,
a cloak with a crest, the orb Aldan in her right hand (gold, glowing); clips cast_jab, magic, proud; second form = a gear halo and a brighter palette. Previews:
`docs/arc/preview/arc_merlin_front.png`, `arc_merlin_cast_big.png`, `arc_merlin_form2_front.png` (and attack and face in the scratchpad previews folder `previews/sdsb/`).
`ArcLook("merlin", 1.91, 1.16, 2.06, 0.43, 1.11, 1.26, glow)` from `tools/arc/manifest.json`.

## 8. Tests

`MerlinTableTest` (a fight played through the framework's scheduler: nothing before its power, no spam, the ultimate only after 400 ticks and at 60%), `CancelStanceTest` (every verdict,
the arc, the charge, anti-magic, the devour arithmetic), `CubeShellTest` (charges, the combo rule, dulling, anti-magic), `InfinityFieldsTest` (the cap, the oldest pushed out, height),
`MerlinSheetTest` (record, floors, Charter ids against the real id list), `kit/BurstGaugeTest`, `kit/DamageWindowTest`, `kit/FourSinsMathTest`. Mutation-checked: see the report.

## 9. Unverified / assumptions

* The exact limits of Absolute Cancel (front arc, one charge) and of Perfect Cube (a count) are DESIGN, not canon; the canon only says "erases a spell by touch" and "reflects".
* Chrono Coffin as a 64-tick cube that freezes and collapses is design; the canon says it stops time in a place and needs preparation.
* "Transfer" and "Mass Transfer" are design names for her teleports. Gluttony as "devour and send back" is design (the canon gives no such mechanic for Absolute Cancel).
* The game glue (the entity, the filter on damage, the zone steps) is type-checked against API stubs and not run in a game.

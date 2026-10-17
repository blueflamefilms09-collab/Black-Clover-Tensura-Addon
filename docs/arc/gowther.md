# Gowther, the Goat's Sin of Lust (boss, tier 3)

Builder `sdsb`. Saga `SEVEN_DEADLY_SINS`, tier 3, health 2300, floors 40 to 80 on either road. Opens with the other three Sins after the bosses of the Eye of the Midnight
Sun (Saga 1), in any order. Code: `arc/sds/Gowther*.java`, `ReadWindow`, `MarkMix`, `EchoTrail`, `StringPull`; model `tools/arc/char_gowther.py`.

## 1. Canon (what the fight is built from)

Gowther is the Goat's Sin of Lust, a sentient doll made by a wizard in the likeness of his dead lover (the demon Gowther's creation; the details of the doll's origin differ
between pages and are not needed here). His inherent power is **Invasion**, which he shares with his creator: it manipulates minds and memories. The Invasion page of the wiki
lists named techniques:

* **Light of Prying Eyes**: reads a target's thoughts and subconscious memories.
* **Broadcast**: sends his thoughts to several targets.
* **Rewrite Light**: alters memories; the effect is superficial and what is remembered afterwards is erased.
* **Phantom Garden**: creates illusions.
* **Nightmare Teller**: a combination with other techniques that tortures an opponent with visions (the page gives no more).
* **Herritt**, his Sacred Treasure: a twin energy bow; in its automatic-tracking mode the bows form a cross and techniques used in that mode lock onto the target.
* Look: dark pink hair, amber eyes (MyAnimeList); the Sin of Lust's goat mark on the right collarbone (one profile).

Sources (the wiki hosts did not open for me; these are the pages and search summaries I read):
https://nanatsu-no-taizai.fandom.com/wiki/Invasion , https://nanatsu-no-taizai.fandom.com/wiki/Gowther , https://tasw.fandom.com/wiki/Gowther ,
https://looper.com/822482/gowthers-powers-from-the-seven-deadly-sins-explained , https://myanimelist.net/character/95977 .

## 2. The owner's rule and how the fight keeps it

The owner's note for this fight: mind manipulation and memory alteration, "with the mod's rules: no mind-control of players; use telegraphed illusions or decoys and misdirection
that the player can learn to read". So **nothing here takes a player's aim, body, movement or controls.** Invasion becomes three things a party can SEE and LEARN:

| Rule | What it does | The counter |
|---|---|---|
| **Light of Prying Eyes** (a reading) | He reads the KIND of blow dealt to him the most in the last 8 s (blades, arrows, spells, fire, ice, blasts, lightning, other), if at least 20 damage was dealt; for 8 s (12 s at the Gates) he turns 55% (65%) of that kind aside. Every other kind lands in full. A line on the attacker's action bar says what he read. The reading is a move with a 21 s cool-down, so it comes back. | Change what you use for the length of the reading (blades to spells, spells to arrows); a party with a mix is barely touched; anti-magic ends the reading at once. |
| **The code of an illusion** | In Phantom Garden, Illusory Step and the Nightmare Teller there are two illusions for every real strike. A REAL ring is gold, steady, with a column of light and a ring closing in; an ILLUSION is violet, flickers (drawn every other tick) and has no column, and when the time comes it pops and does nothing. Rings lock when the wind-up starts. | Read the colour. Leave the gold ones. The first time each rule is needed it is spelled out in chat. |
| **Echo of Memory** | He strikes the places each foe stood 1, 2 and 3 s ago (five echoes, 1 to 5 s, at the Gates), oldest first, 4 ticks apart, 1.9 radius, 7 each. A foe who stands still has every echo on their feet. | Keep moving (any path that does not double back). |

Two more effects touch a player, both under the balance law and both avoidable:

* **Rewrite Light**: a lane (20 long, 0.9 half-width) that follows the target for 60% of the wind-up and then locks (white). Hit: 5 damage, the grimoire is silenced, a Mana Zone is switched off
  and the held item let go, for about 2 s (the law caps and halves it). Counter: leave the lane after it locks; anti-magic holders are not touched. Nothing is taken or spent.
* **Marionette Strings**: threads (the mod's Dancing Doll shape) to every foe in his sight within 30 blocks. After 20 ticks each foe still in sight is pulled along the ground towards him,
  at most a block a tick and at most 12 ticks (so at most about 12 blocks), and stops 2.6 blocks in front of him; a foe who arrives takes 9. A foe he can no longer see is not pulled
  (the thread snaps on the wall: a spark marks it). Summons and holders of anti-magic are struck for half where they stand. Counter: stand behind a wall, or beyond 30 blocks.

## 3. His moves (table of `GowtherTable`)

| id | label | power | wind-up | cool-down | range | weight | recovery | notes |
|---|---|---|---|---|---|---|---|---|
| herritt_volley | Herritt: Twin Bows | 20 | 18 | 90 | 5-34 | 5 | 8 | 2, 2, 4, 6, 8 straight arrows from two bows, 4.5 each; the fan is drawn; arrows are slow enough to dodge sideways |
| herritt_cross | Herritt: Cross Lock | 40 | 24 | 210 | 8-36 | 3 | 10 | the cross-mode lock: two lanes that cross on the target's spot (locked when the wind-up starts), 7 each |
| phantom_garden | Phantom Garden | 20 | 28 | 280 | 4-34 | 4 | 10 | crowd; real rings (2 to 5 for each foe, one on their feet) and two illusions for each; 13 in a real ring, once per foe |
| echo_of_memory | Echo of Memory | 40 | 22 | 230 | 0-48 | 3 | 10 | crowd; the places you stood, see section 2 |
| prying_eyes | Light of Prying Eyes | 20 | 18 | 420 | 0-48 | 2 | 12 | the reading; if there is nothing to read (under 20 damage dealt) he says so and fires two arrows |
| nightmare_teller | Nightmare Teller | 60 | 60 | 1300 | 0-40 | 5 | 60 | ULTIMATE, see below |
| marionette | Marionette Strings | 40 | 20 | 300 | 5-30 | 2 | 10 | the strings of section 2 |
| illusory_step | Illusory Step | 20 | 12 | 140 | 0-48 | 3 | 6 | three spots 8 to 12 blocks round the target: the gold one is where he steps (a mirror-glass figure appears at each), he fires two arrows (4) from there |
| rewrite_light | Rewrite Light | 40 | 22 | 260 | 6-32 | 3 | 8 | the lane of section 2 |
| puppet_rush | Puppet Rush | 20 | 12 | 70 | 0-5.5 | 5 | 10 | two swipes of the doll's arm, 9 ticks apart, a cone 4.4 long (8 each); the close-range answer |

### Nightmare Teller (ultimate)

A dome of dream (the mod's Dream Magic dome) closes, radius 13, on the place of the target when the 60-tick wind-up begins (the rim is drawn all through the wind-up). For 7 s it sends five
waves of rings: each wave plans the rings for the foes inside (the code of section 2, at most 18 rings), shows them for 20 ticks, and the real ones strike for 13 (once per foe per wave) while the
illusions pop. He stands still and starts no other move; the first 60 ticks he is exposed (the framework's recovery of an ultimate). **Counters**: read the colours and leave the gold rings; or
deal 8% of his health in 3 s (anti-magic counts double): the dome shatters (the Dream Magic shatter shape), the waves end, he is staggered for 2.5 s and takes 15% more.

## 4. Numbers

* Health 2300, attack 15 (the plain swat is 60%), speed 0.35, armor 8, toughness 5; hitbox 0.55 x 1.84. He keeps 8 to 16 blocks away.
* The Gates (below 25% health) open the second form (long hair, gear wings, a broken halo), refill his bar once, give him 5 echoes, a 12 s reading at 65%, more real rings, 8 arrows.
* Charter Grimoire (30%): mirage, confusion, mental_crush, charm, thought_communication, doppelganger, invisible, clairvoyance (real Tensura 2.0.1.3 ids, tested; Lust is an Ultimate and is not lent,
  and nothing that dominates is lent: `GowtherSheetTest` checks it).
* Lines: opening "I wish to understand what you feel when you are afraid. Please, be afraid." / Gates "My heart... is that what this is? Fascinating. Let me show you." / death "So this is what losing feels like. I do not know the word for it yet."
* Themes haunted_manor, shadow, feywild_grove, cursed_archive. Prerequisites: fana, rhya, rades_spirito, patolli.

## 5. Rules kept

No entity spawned, no block changed, no healing. Control only through `BalanceLaw.beginControl / controlTicks` (Rewrite Light, the Strings), never on creative players, spectators or holders of anti-magic.
No mind control: the only things that touch a player's hands are the 2 s grimoire silence of Rewrite Light and the 12-tick pull of the strings, both avoidable. Damage only through `boss.strike`.
No vanilla particle types: `Tells` (the sanctioned dust), `VfxSpawn` shapes (THREAD_STRINGS, DREAM_DOME, DREAM_SHATTER, MIRROR_STEP, MIRROR_RAY, EYE_FX3, MAGIC_CIRCLE_EXPLOSION).

## 6. Weapon he would give (NOT built; a later job)

**Herritt**, the twin bows (a bow or crossbow in a pair, the cross-mode shown as a pink cross of light): a real 3D model in the Genesis Demon-Slayer's standard.
Engravings for `item/WeaponEngravings.java`: `barrier_piercing` 2 (Invasion goes through wards), `swift` 2, `magic_weapon` 1, `enervation` 1.

## 7. Model

`tools/arc/char_gowther.py`: slim, pale, dark-pink curtain hair, amber eyes with a blush, an indigo coat and cravat, a bow in the left hand and one on the back; clips shoot, magic, relaxed;
second form = long hair, mechanical wings, a broken halo, cracks of light. Previews: `docs/arc/preview/arc_gowther_front.png`, `arc_gowther_cast_big.png`, `arc_gowther_form2_front.png`.
`ArcLook("gowther", 1.84, 1.11, 1.97, 0.69, 1.49, 1.2, glow)` from `tools/arc/manifest.json`.

## 8. Tests

`GowtherTableTest` (the scheduler played out: opening moves only before 40%, no Nightmare before 60% or 400 ticks, no move spammed, every move used at the Gates), `ReadWindowTest` (the tally, the minimum, the
eight-second window, ties, the bounded cut, anti-magic ending it), `MarkMixTest` (a real ring under every foe, more illusions than real rings, nothing stacked, the cap keeps the real ones, seeded),
`EchoTrailTest` (the trail and the string's arithmetic), `GowtherSheetTest`, `kit/DamageKindTest`. Mutation-checked: see the report.

## 9. Unverified / assumptions

* The names Light of Prying Eyes, Broadcast, Rewrite Light, Phantom Garden, Nightmare Teller and Herritt come from the Invasion page as a search summary showed it; I could not open the page. Whether Herritt is two bows
  or one "twin bow" differs between pages. Marionette Strings, Illusory Step, Puppet Rush and Echo of Memory are DESIGN names for what a doll that can only use Invasion would do in a game.
* "Reads the kind of blow you keep dealing" is the game form of Light of Prying Eyes (reading thoughts); the canon technique reads thoughts and memories, not damage.
* What the Nightmare Teller is in the story (a combination with others' techniques) is not what it is here (a dome of waves).
* The game glue is type-checked against API stubs and not run in a game; whether `DamageSource` ids of other mods are sorted into the right kind is by the words of their ids (`DamageKind`).

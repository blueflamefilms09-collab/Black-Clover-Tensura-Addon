# Patolli, Leader of the Eye of the Midnight Sun (boss, saga 1, Holy road)

Why this page exists: it records what the story says about Patolli, how the fight was built from it, why each move and each counter is what it is, the numbers,
and what is not verified. Shared parts (base class, bond, config, lead lines) are in [midnight_sun.md](midnight_sun.md). Code: `arc/midnight/PatolliBoss.java`,
numbers in `PatolliTable.java` and `PatolliMath.java`, the Overreach counter in `Overreach.java`, tests in `PatolliTest.java`. Model: `tools/arc/char_patolli.py`.

## Canon (what the story says)

* Patolli (Patri) is an elf of the old tribe, one of the ten Apostles of Sephirah, who leads the Eye of the Midnight Sun. He wears the face and the name of
  Licht (the elf leader) and for years shared a body with William Vangeance, the Golden Dawn captain, before taking Licht's artificial body. His purpose: to
  gather the magic stones and reincarnate the elves, and to destroy the Clover Kingdom; at the end he sacrificed his followers to finish the ritual.
  (blackclover.fandom.com Patolli, Licht, Eye_of_the_Midnight_Sun; villains.fandom.com Patolli; baike.baidu.com Patolli, William Vangeance)
* His magic is Light Magic (a four-leaf grimoire he obtained because Vangeance could not use his own). Named spells found on the wiki's Light Magic category:
  Light Sword of Judgment, Light Shaft of Divine Punishment, Bright Judgment Whip, Heaven-Splitting Flash, Arrows of Judgment, Lamp of Avior Gloria. He can
  blind and he moves at the speed of light. As a Dark Elf his spells become Demon Light Magic versions (Light Sword, Light Shaft, Bright Judgment Whip).
* The fight the owner named, Julius Novachrono against Patolli: Julius froze one of Patolli's spells (Chrono Stasis Grigora), saw a few seconds ahead and dodged
  every swing of the Bright Judgment Whip, reversed a wound, caught a light blade and sent it back faster; Patolli still got behind him and wounded him, hung a
  sword over every citizen of the kingdom, and finished the fight with a sword through Julius. Sources disagree on whether Julius died; the wiki lists the
  fight as a win for Patolli. (blackclover.fandom.com Julius_Novachrono_vs._Patolli; animehunch.com on Julius; sportskeeda.com on the best fights)
* How light was beaten: in the earlier fight with Yami, Patolli's Light Shaft of Divine Punishment was turned back at him by Gauche's Mirror Magic and he fell;
  Yami remarked that Mirror Magic is a counter to Light Magic. (blackclover.fandom.com Patolli, Yami_Sukehiro_vs._Licht)
* Appearance (first form): tall and slim, white braided hair, gold eyes, red tattoos round the eyes, a kippah-like cap with a gold gem, a long white hooded
  robe with black lines over dark and light layers. Dark Elf form: dark skin, black eyes, black and violet. (blackclover.fandom.com Patolli; villains.fandom.com;
  blackcloveronline.com/blog/patolli) Sources disagree on small details; the model follows the wiki.

## Design in one paragraph

The owner's reference fight is "time prediction against light speed". A boss that is faster than the eye cannot be dodged by reaction, so this one is built to
be READ: every strike is drawn on the floor before it lands, the lanes freeze a few ticks before the blow (the lines turn red), and what cannot be outrun can
be predicted. Julius did not outrun the light, he stood where the strike was not, and that is the first mechanic: a close-in strike that finds only air leaves
Patolli open. The other two are the story's counters: the mirror that turns the Light Shaft back, and the incantation of the swords (Heaven's Verdict) that
Julius countered. At the Gates the light turns black and a move is added.

## Mechanics and counters (the three that only he has)

1. **Overreach** (`Overreach`). The strikes that cross the floor in a blink (Light Step, the whip, the Dance of Cuts) are noted: one that lands on someone
   resets the count, one that finds only air adds one. Three empty ones in a row leave him OPEN for 50 ticks (a flare, a line, no move, +25% damage).
   Counter: do not be where the strike lands. Stand outside the ring, step aside from the lane. The whip is drawn as a cone; the Step as a line and a ring
   behind the target. Why: Julius won the early exchange by being elsewhere.
2. **The Light Shaft can be turned back** (`ReflectRules`). The beam locks 12 ticks before it fires. A foe whose Full Reflection is up, or who holds a live
   mirror array (Gauche's Mirror Magic), turns it: Patolli takes 6% (Full Reflection) or 5% (mirror) of his maximum health and is OPEN for 70 or 60 ticks. A
   raised shield and anti-magic cut it instead (the shield halves it, anti-magic cuts a spell to 55%). One turn per player per five seconds, so a mirror is a
   skill and not a wall. Server switch: `reflectBackfire`.
3. **Heaven's Verdict can be broken** (`BreakWatch`). The ultimate (60 ticks of wind-up, power 60 and above, cooldown 1200, recovery 60) hangs a sword over
   every foe (marked with a ring, a column and a falling blade drawn on each). Counters, any one: take 5% of his maximum health from him during the wind-up
   (the incantation breaks, the swords scatter, he is OPEN for 70 ticks; bar shows the progress); stand under a roof (the blow is cut to 35%); raise a shield
   (60%); hold anti-magic (60%). The cuts add up but never leave less than a fifth of the blow, so protection helps and nothing is a free pass. In the story
   the swords hung over every citizen and Julius answered with a counterspell (Patolli still landed the last blow there; here the answer is yours to find).

At the Gates (100% power) he is the Dark Elf: every white effect is replaced by the mod's Demon Light effects, and one move is added, the Dance of Cuts. The
Dance is a design addition (the story gives the Dark Elf Demon Light versions of three spells, not a new dance); it is the one move that is not taken from a
named spell.

## The moves (base damage before the boss's damage law, the guard and `damageScale`)

| move | min power | wind-up | cooldown | range | weight | recovery | damage and shape |
|------|-----------|---------|----------|-------|--------|----------|------------------|
| Light Sword of Judgment | 20 | 18 | 110 | 5 to 34 | 5 | 8 | 3 to 6 swords in a fan (4.0 blocks a tick, 12 ticks of life), 6.5 each, the lines follow the target until 8 ticks before they fly |
| Bright Judgment Whip | 20 | 16 | 80 | 0 to 8.5 | 6 | 10 | a cone of 8.5 blocks and 55 degrees each side, two lashes 10 ticks apart (the second one is shown first), 11 each |
| Light Step | 20 | 20 | 150 | 5 to 38 | 5 | 8 | a line to a spot 2.2 blocks behind the target and a ring of 3 blocks there: 9 on the lane (1.2 half width), 12 at the arrival |
| Radiant Burst | 20 | 18 | 170 | 0 to 6 | 2 | 12 | a ring of 6.5 blocks round him, 9 and a shove |
| Blinding Flash | 20 | 22 | 360 | 0 to 20 | 2 | 10 | crowd: anyone within 20 blocks who LOOKS at him (72 degrees) and does not block takes 3 and is blinded 40 ticks (control rule); looking away or a shield is the answer; says so on the action bar |
| Solar Magic | 20 | 18 | 240 | 6 to 30 | 2 | 8 | one of his learned Tensura light skills, or two light swords if none is ready |
| Heaven-Splitting Flash | 40 | 30 | 260 | 4 to 38 | 4 | 12 | crowd: 3 to 5 orbs on a ring of 8 blocks round the target, each draws its line, then fires a beam 10 ticks later, 8 each |
| Light Shaft of Divine Punishment | 40 | 30 | 240 | 6 to 36 | 4 | 10 | a beam 40 blocks long, 1.1 half width, locks 12 ticks before; 19; can be turned back |
| Arrows of Judgment | 40 | 34 | 340 | 0 to 30 | 3 | 12 | crowd: 2 to 4 salvos 14 ticks apart, each marks the foes with rings of 2.4 blocks for 14 ticks, 8.5 each |
| Lamp of Avior Gloria | 60 | 28 | 420 | 0 to 28 | 3 | 12 | crowd: a lamp over his head for 60 ticks that sends 1 to 3 swords every 6 ticks at foes within 26 blocks, each line drawn 8 ticks first, 6 each |
| Heaven's Verdict (ultimate) | 60 | 60 | 1200 | 0 to 48 | 5 | 60 | crowd: see mechanic 3; 26 on each foe before cover and guard |
| Demon Light: Dance of Cuts | 100 | 24 | 200 | 0 to 12 | 4 | 12 | 3 to 5 cut marks on a ring of 3.4 blocks round the target, he crosses them 6 ticks apart, 6.5 each; counts for Overreach |

The mix: melee (whip, burst, dance), ranged (swords, flash, shaft, arrows), area (burst, arrows, lamp), mobility (Light Step), crowd control (the Flash blinds
only through the shared control rule, the Verdict marks the party), a signature ultimate. Every wind-up is at least 16 ticks on paper (12 at the Gates).

## Numbers

* `ArcCharacter`: tier 2, 1900 health, 14 attack, 0.36 speed, armour 5, toughness 3; Holy road, floors 50 to 70; themes radiant, astral, holy_church,
  celestial_court, sky_citadel; after fana, rhya, rades_spirito; Charter (25%): solar_beam, solar_rain, solar_flare, solar_wave, light_transform, instant_move,
  thought_acceleration, light_attack_resistance, holy_attack_resistance. Colours 0xFFE3A0 and 0xFFFFFF. Magic id `light`.
* Hit box 0.55 by 2.03. `ArcLook`: from `tools/arc/manifest.json` (`patolli` and `patolli_form2`), the bigger of the two forms for the culling margins:
  walk cycle 0.92, run cycle 1.7, shadow 0.44, grow 1.12 horizontally and 1.2 up.
* Counts by power step (20, 40, 60, 80, 100): swords 3, 3, 4, 5, 6; orbs 3, 3, 4, 4, 5; arrow salvos 2, 2, 3, 3, 4; lamp swords per beat 1, 1, 2, 2, 3; dance
  cuts 3, 3, 4, 4, 5.
* OPEN windows: Overreach 50 ticks, turned Shaft 70 / 60 / 45 (Full Reflection / mirror / anti-magic swing), broken Verdict 70.

## Tests (`PatolliTest`)

The table obeys the framework's laws (wind-up, ultimate, recovery, cooldown, power steps); there is no dead range at the opening; the counts rise with
power; a spread of swords never wraps round the foe; every mark can be left in its warning time; the Flash and the Verdict have real counters (shield, looking
away, roof, anti-magic, the break share); Overreach needs three empty strikes in a row; the Light Step is the bread and butter at mid range. Shared tests
(geometry, the OPEN clock, the break watch, reflection) are in `MidnightMathTest` and `StateClocksTest`.

## Model

`tools/arc/char_patolli.py`, kit build `python3 -B tools/gen_arc_characters.py patolli`. First form: a long white robe edged in black over a dark shirt and
light trousers, a black shawl, a braid of white hair, gold eyes with red eyeshadow and red marks under them, a gold circlet with a gem, and the four-leaf
grimoire open in the left hand with a gold glow (idle clip `proud`, attack `cast_jab`, cast `book`). Second form (the Dark Elf, the Gates): dark violet skin,
black eyes ringed with violet fire, pointed ears, a robe that has turned to night with violet trim, a broken violet halo, violet glow. Added for the game and not
from the source: the grimoire's glow and the broken halo. Previews in the scratchpad `previews/s1b/arc_patolli_*.png` (front, back, face, idle, attack, cast,
cast_big, death, and the second form's front and face).

## Unverified

* Nothing was played: the speed of the swords (4 blocks a tick) and the lock times were chosen so that a lane is leavable on paper (tests) but not felt.
* The Dance of Cuts is a design addition, not canon.
* "Looking at him" for the Flash uses the foe's view vector and a 72 degree cone; whether that feels right with a mouse is unknown.
* The Overreach count follows only the strikes that call `noteStrike`; a foe that is far away and never targeted by them does not feed it.
* Whether the mirror array and Full Reflection of the mod are detected by `MidnightFx.reflectKind` in play (see the group page).
* Canon sources are summaries from web search, not the pages; the order of Patolli's spells in the Julius fight was summarised by the search tool and is not
  quoted from the manga.

## Weapons

None. (If the owner later wants one: the Light Sword of Judgment would carry Tensura engravings of the holy kind, such as holy weapon and barrier piercing.)

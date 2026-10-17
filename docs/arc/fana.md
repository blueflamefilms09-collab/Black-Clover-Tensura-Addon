# Fana, the Hatred of the Third Eye (boss, saga 1, Abyssal road)

Why this page exists: it records what the story says about Fana, how the fight was built from it, why each move and each counter is what it is, the numbers,
and what is not verified. Shared parts (base class, the Third Eye bond, config, lead lines) are in [midnight_sun.md](midnight_sun.md). Code:
`arc/midnight/FanaBoss.java`, numbers in `FanaTable.java` and `FanaMath.java`, the mechanics in `PhoenixRobe.java` and `EvilHeart.java`, tests in `FanaTest.java`.
Model: `tools/arc/char_fana.py`.

## Canon (what the story says)

* Fana is an elf soul reincarnated by forbidden magic into the body of a Diamond Kingdom orphan also called Fana. She is one of the Third Eye of the Eye of the
  Midnight Sun (with Vetto and Rhya), the three strongest members; her epithet is "Hatred" (the opposite of the Clover Kingdom's love). The third eye is a mark
  of the forbidden magic and disappears when the reincarnation magic is removed. (blackclover.fandom.com Fana_(elf), Eye_of_the_Midnight_Sun; tvtropes.org
  BlackCloverEyeOfTheMidnightSun; comicon.com 5-point discussions 36; baike.baidu.com Fana)
* She calls the fire spirit Salamander (the strongest in attack of the four great spirits) and fights with it: Salamander's Breath (a big fire blast that burns
  forests), Salamander's Claw, a charge; her grimoire holds fire spells and she can also use elf magic and mineral magic. Spells on the wiki: Maximum Exploding
  Fireball, Profound Spiral Flames, Calidus Brachium Purgatory (Abyss, Flame Burial), Ignis Columna, Phoenix Feathers Robe, Crimson Eruption, and compound
  techniques with other fire users. (blackclover.fandom.com Fana_(elf); baike.baidu.com Fana; black-clover-kingdom-fall.fandom.com Salamander)
* The fights: in the Witches' Forest raid Fana burns the forest in her anger; against Asta, Vanessa, Finral and Noelle, Asta struck Salamander's fireball back at it
  with the anti-magic sword and Noelle blocked its charge with a barrier; against Asta and Mars, Mars recognised her inside the flames, her anger and confusion
  grew, she declared she would make everything disappear and joined her mana with Salamander in a self-destroying sphere (the Evil Heart); Asta and Mars stopped
  it, her third eye vanished and the girl under it woke while the spirit left. (blackclover.fandom.com Asta_and_Mars_vs._Fana and the four-against-Fana page;
  comicon.com 5-point discussions 59, "Flames of Hatred"; soapcentral.com on whether Fana lives)
* Mars and Fana were childhood friends in the Diamond Kingdom's experiments (not brother and sister).
* Appearance: a short slim girl, thick wavy pink hair to just below the shoulders, large sea-green eyes (one source says blue), a purple mage stone on the
  forehead framed by a red tattoo with the third eye under it, a high-collared furry maroon dress under the light hooded robe of the Eye, knee-high boots and a
  shoulder pouch. (blackclover.fandom.com Fana; hero.fandom.com Fana)

## Design in one paragraph

A fire fight of things the party can read, with three mechanics taken from the story: her fireball can be struck back, her Phoenix Feathers Robe answers fire with
fire and is undone by water or anti-magic, and the Evil Heart is a countdown that the party can stop by hurting her enough (in the story the third eye went out
and the girl under it woke). Salamander itself is not an entity or a model: it is the fire drawn at her side (a floating spark on the model, flame effects in
the fight). She changes no block and heals nothing; the fire she lights is on the bodies it hits, never on the floor.

## Mechanics and counters (the three that only she has)

1. **The Maximum Exploding Fireball can be struck back** (`ReflectRules`, `MidnightBolt`). It is a slow ball (0.85 blocks a tick, 55 ticks of life) that explodes
   in a radius of 3.4 for 14. A foe who swings anti-magic (a demon sword or the Anti-Magic Lord) close to the flying ball, or has a Full Reflection mirror or a
   live mirror array up, turns it back: it flies at her, she takes 4% (anti-magic swing), 5% (mirror) or 6% (Full Reflection) of her maximum health and is OPEN
   for 45, 60 or 70 ticks. One turn per player per five seconds. Why: Asta did exactly this. The first fireball of a fight announces the hint. Server
   switch: `reflectBackfire`.
2. **The Phoenix Feathers Robe** (`PhoenixRobe`, move at power 40 and above). For 4 seconds (80 ticks) she takes 60% less from fire, and 45% less from every other
   damage; whoever strikes her from within 4 blocks is set alight for 3 seconds; water, ice and cold (damage types and magic names with water, ice, frost,
   freezing, sea, tide, rain and so on) go through whole; a blow of anti-magic tears the robe off at once. Counter: wait it out, hit it with water or ice, or
   cut it with the anti-magic blade, and do not stand in front of it. The robe is a defence, so it never lasts more than four seconds and has a cooldown of 760.
3. **The Evil Heart** (`EvilHeart`, ultimate at the Gates, power 100). Wind-up 60 ticks, then she stands still (movement frozen, no move starts) and a sphere
   grows round her from 3 to 17 blocks over 150 ticks (config `evilHeartTicks`); a bar on the screen counts down and shows how much of her health the party has
   taken. If the party takes 6% of her maximum health before it ends (config `evilHeartBreakPercent`) the Third Eye shatters, the sphere dies and she is OPEN for
   100 ticks. If not, it detonates over 17 blocks: 32 at the core, falling in a straight line to 40% at the rim, never reaching foes beyond it; a wall between the
   foe and the sphere cuts it to 45%, a raised shield to 50%, anti-magic to 55%, and the cuts add up but never leave less than a fifth. After a detonation she is
   open for 80 ticks. Counter: damage her, or run, hide behind walls, shield up. Why: the story's last stand, which Asta and Mars broke.

At the Gates she is the Evil Heart: the second model (a crown of flames, burning eyes, a wild mane), the Evil Heart move, and the stronger counts.

## The moves (base damage before the boss's damage law, the guard and `damageScale`)

| move | min power | wind-up | cooldown | range | weight | recovery | damage and shape |
|------|-----------|---------|----------|-------|--------|----------|------------------|
| Maximum Exploding Fireball | 20 | 22 | 120 | 8 to 40 | 5 | 8 | 1 to 3 fireballs in a fan, 14 each in a 3.4 blast, lines drawn from her hand; can be struck back (mechanic 1) |
| Salamander's Breath | 20 | 18 | 100 | 2 to 16 | 5 | 8 | a cone of 13 blocks and 28 degrees each side, 10, burning 3 s; the cone is drawn and locks 8 ticks before |
| Salamander's Claw | 20 | 16 | 70 | 0 to 6.5 | 6 | 10 | a cone of 6 blocks and 50 degrees, 13, a shove; a blow of the body, so only a shield cuts it |
| Salamander's Charge | 20 | 24 | 200 | 6 to 32 | 4 | 10 | she and the spirit run up to 18 blocks along a lane drawn on the floor (1.4 blocks to each side), 14, throwing foes aside, and arrive at its end |
| Profound Spiral Flames | 20 | 22 | 150 | 8 to 36 | 4 | 8 | a spiral beam 26 blocks long, 1.3 half width, locks 10 ticks before, 11 |
| Ignis Columna | 20 | 26 | 190 | 3 to 36 | 4 | 10 | crowd: 3 to 5 pillars (radius 2.4) marked under the foes, 11 each |
| Salamander's Roar | 20 | 20 | 300 | 0 to 11 | 2 | 12 | crowd: a ring of 10 blocks round her, 5, a slow of 2 seconds |
| Fire Magic | 20 | 18 | 240 | 6 to 32 | 2 | 8 | one of her learned Tensura fire skills, or one fireball if none is ready |
| Crimson Eruption | 40 | 28 | 230 | 0 to 14 | 4 | 14 | crowd: 2 to 3 rings of fire run out from her at a block a tick (reach 5, 8.2, 11.4), 10 each, once per foe; a jump over the ring avoids it |
| Phoenix Feathers Robe | 40 | 24 | 760 | 0 to 48 | 1 | 10 | the defence (mechanic 2) |
| Calidus Brachium: Flame Burial | 60 | 30 | 260 | 0 to 36 | 3 | 10 | one mark (radius 3.2) under the target, 12 and a root of 25 ticks through the shared control rule |
| Rain of Embers | 60 | 32 | 340 | 0 to 34 | 3 | 12 | crowd: 5 to 9 embers (radius 2.0) land one after another every 3 ticks, each marked 18 ticks first, 8 each |
| Evil Heart (ultimate) | 100 | 60 | 1500 | 0 to 48 | 6 | 60 | crowd: mechanic 3, 32 at the core |

The mix: melee (claw, charge), ranged (fireball, breath, spiral), area (columns, roar, eruption, embers), mobility (charge), control (the flame burial root,
the roar's slow), a signature ultimate. Every wind-up is at least 16 ticks on paper (12 at the Gates); marks are always leavable (`FanaTest`).

## Numbers

* `ArcCharacter`: tier 2, 1500 health, 12 attack, 0.30 speed, armour 6, toughness 3; Abyssal road, floors 35 to 60; themes flame, infernal_foundry (another
  builder's theme, inert until merged), verdant, abyss; after mars and vetto; Charter (25%): flame_manipulation, flame_domination, flame_breath, fire_storm,
  flame_wall, summon_greater_elemental, flame_attack_resistance, heat_wave. Colours 0xE8421A and 0xFFB347. Magic id `flame`.
* Hit box 0.55 by 1.75. `ArcLook`: from `tools/arc/manifest.json` (`fana` and `fana_form2`, the bigger of the two forms for the margins): walk 1.01, run 1.79,
  shadow 0.5, grow 1.22 horizontally and 1.27 up.
* Counts by power step (20, 40, 60, 80, 100): fireballs 1, 1, 2, 2, 3; pillars 3, 3, 4, 4, 5; embers 5, 5, 6, 7, 9; eruption rings 2, 2, 2, 3, 3.
* The Evil Heart sphere: radius from 3 to 17 blocks, core 35% of the radius, rim 40% of the blast, break 6% of her maximum health, 150 ticks.

## Tests (`FanaTest`)

The table obeys the laws; no dead range at the opening; counts rise with power; every mark can be left; the mix and the order; the eruption rings are ordered and
finish in time; the robe shrugs off fire and not water, and anti-magic tears it; the Evil Heart counts down, can be broken, the sphere grows evenly and the blast
falls off; shelter helps but never completely; a party that hits hard can beat the heart (the break share against a plausible damage per second); she opens with
breath and claw up close and fireballs far away.

## Model

`tools/arc/char_fana.py`, `python3 -B tools/gen_arc_characters.py fana`. First form: a short slim girl, thick wavy pink hair to the shoulders, sea-green eyes, a
violet gem on the forehead with a red sigil and a small third eye under it, a high-collared maroon dress with a fur collar and an eye emblem, a light hooded cloak,
knee boots, a satchel, a grimoire in the left hand with an ember glow, and a floating orange spark at her side (Salamander). Second form (the Evil Heart): a mad
face with burning orange eyes and veins, the third eye and the gem aflame, a wild mane, a redder dress, a ragged cloak and a crown of flames. Added for the game
and not from the source: the spark of Salamander and the glow of the gem. Kit note: the idle clip is `proud` (the kit's `guard` idle breaks its arm rule with a
prop in the left hand only) and a prop in the left hand was needed for `cast_jab`. Previews: scratchpad `previews/s1b/arc_fana_*.png`.

## Unverified

* Nothing was played. The speed of the fireball (0.85) and the sizes of every shape are chosen so that each is leavable on paper.
* The hint about the fireball is announced once per fight; whether the party can read the ball in time at 0.85 blocks a tick is unknown.
* The robe's element test reads the words of the damage type's id (water, ice, frost, sea, tide, rain and so on count as cold; fire, flame, lava, magma and so
  on count as fire); a damage type from another mod with none of these words counts as "other" (45% off).
* Salamander's Roar and Rain of Embers are design additions, not named spells of the story.
* The Evil Heart's break share (6%) is meant to be reachable by a party in 2.5 seconds (the sphere lasts 7.5 s); with one player and a slow weapon it may not be.
* The canon summaries come from web search; the exact order and the exact wording of the Mars fight were not read from the manga.

## Weapons

None.

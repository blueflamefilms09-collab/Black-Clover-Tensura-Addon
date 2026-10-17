# Mars, the Crystal Weapon (`mars`)

Boss of the Dungeon Exploration Arc, Saga 1 (saga `ELF_REINCARNATION`), tier 2, Holy road. Classes: `arc/early/MarsBoss`, table `MarsTable`, state `StoneLedger`
and `FrontGuard`, record `EarlyRoster.MARS`. Model: `tools/arc/char_mars.py` (with a second form). Overview of the five early bosses: `docs/arc/early.md`.

## Canon (what the sources say, and how sure it is)

The page fetcher could not reach any wiki (DNS refused), so this is from search digests of the pages listed. The sources disagree on two points, noted below.

* One of the Diamond Kingdom's Eight Shining Generals (one source says so; he serves under Lotus Whomalt in the dungeon). An orphan picked for the kingdom's experiment
  program: mage stones were set into his body to amplify his magic, and Morris Libardirt (Modification Magic) implanted Fana's fire magic in him. Crystal Magic is
  his attribute: swords, giant figures and clones. The fire magic is described as recovery or healing in two sources, and one says it only provides healing: this
  fight does NOT use it (no healing on a boss), the second form only borrows its colour.
* Spells: Harpe (crystal blades in the shape of a harpe manifest round him), Nemean Armor (a crystal guard strong enough to withstand powerful magical assaults),
  crystal clones of himself that keep multiplying (one holds Klaus), Talos Puppet Cluster and Heavy Armored Titan (giant crystal constructs).
* The dungeon fight is against Asta, Yuno and the Golden Dawn knights. The sources disagree on how it ends: Yuno's wind spirit Sylph delivers a shattering blast
  (one source), or Asta knocks him unconscious, Klaus restrains him and Yuno finishes him by some means (another). The class Javadoc takes the first.
* Sources: https://blackclover.fandom.com/wiki/Mars , https://deltiasgaming.com/who-is-mars-in-black-clover/ , https://vsbattles.fandom.com/wiki/Mars_(Black_Clover) ,
  https://topstrongest.fandom.com/wiki/Mars_(Black_Clover) , https://animated-character-database.fandom.com/wiki/Mars(Black_Clover) ,
  https://www.thereviewgeek.com/blackclover-dungeonexploration-explained/ .

## The fight: three mechanics, each with a counter

| Mechanic | What it does | Counters the party can learn |
| --- | --- | --- |
| **Mage stones** (`StoneLedger`) | Three stones are set in his body and each adds a tenth to every blow of his (so a full Mars hits 30% harder). At 40%, 60% and 80% power, and at the Gates (70 ticks after they open), one stone surfaces on his chest and glows for 12 seconds; the blows he takes meanwhile (anti-magic counts double) are counted towards cracking it, 7% of his maximum health (112 at 1600). A cracked stone is gone for good, his damage falls by a tenth, he staggers 50 ticks (takes 30% more, starts nothing) and a cast in progress is cut short. A stone that is not cracked in time sinks back and keeps its tenth. The count is saved, so a reload does not give them back. | **Focus him while a stone glows**: a party that does is rewarded with a weaker, shakier Mars; one that does not meets all three at the end. Anti-magic counts double, so a holder of it cracks a stone twice as fast. |
| **Nemean Armor** (`FrontGuard`) | While it stands (140 ticks), a blow from within 65 degrees of the way he faces is cut to 40%; a blow from the side or behind is not touched; anti-magic cuts straight through. A chime and a message tell the attacker when the front turns a blow (at most every two seconds). | **Walk round him.** A pair of fighters on either side of him splits his attention (he faces his target). |
| **Crystal Doubles** and the **Heavy Armored Titan** | Doubles (from 40%): three fangs (four from 80% power) erupt at the corners of a ring of radius 4.2 round the target's spot, all at once, 26-tick wind-up, locked at half; the gap in the ring faces Mars. The Titan (ultimate, from 60%): a circle of radius 9 on the ground (locked at 40% of the 56-tick wind-up) falls for 20 damage and a shove, then two rings run out of it to a radius of 12, 14 and 28 ticks later, 8 damage each. | **The middle of the doubles is safe, and so is anywhere outside the ring** (a message says so). **Leave the Titan's circle, then jump the two rings**. He is exposed for 60 ticks after the Titan (the framework's ultimate recovery). |

## Move table (base damage; the framework scales it with power, victim EP and the balance law; DPS budget 6% per second)

| id | name | from | wind-up (Gates) | cooldown | range | damage | tell and counter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `harpe_blades` | Harpe | 20% | 18 (14) | 100 | 4-26 | 5.5 per blade, 3 to 6 blades from an arc round his shoulders, fired two ticks apart at the target | a line from every blade to the target |
| `crystal_slash` | Crystal Slash | 20% | 16 (12) | 70 | 0-5 | 12 in a cone (4.6 blocks, 52 degrees each side), shove | three lines |
| `crystal_lance` | Crystal Lance | 20% | 20 (15) | 130 | 5-26 | 11 along a 22-block lane, 2.2 wide | two lines, locked at half |
| `crystal_field` | Crystal Field | 20% | 22 (17) | 150 | 3-26 | 12 in a circle of 4, thrown up | ring, followed 55% of the wind-up, then locked |
| `nemean_armor` | Nemean Armor | 20% | 18 (14) | 400 | 0-48 | none (a guard) | ring and glow on his chest |
| `crystal_rush` | Crystal Rush | 20% | 18 (14) | 150 | 7-24 | 10 along 16 blocks, 2.6 wide | two lines, locked at half |
| `crystal_doubles` | Crystal Doubles (crowd) | 40% | 26 (20) | 240 | 4-26 | 11 per fang (radius 2.2), thrown up | rings and columns on the corners |
| `shatter_nova` | Shatter Nova (crowd) | 40% | 22 (17) | 200 | 0-8 | 10 in a ring wave to 7 blocks | ring that grows; jump |
| `talos_puppets` | Talos Puppet Cluster (crowd) | 60% | 24 (18) | 320 | 6-30 | 3 wisps (4 at the Gates), 9 each in 2.2 blocks, 90 ticks | lines from his chest to the foes |
| `heavy_armored_titan` | Heavy Armored Titan (ultimate) | 60% | 56 (42) | 1500 | 0-30 | 20 + two rings of 8 | see above |

Plain blows come from the framework's melee at 70% of his attack. He wants to stay 3 to 8 blocks away (the stones make him a swordsman). At the Gates (second bar,
the model swaps to Mars, Stones Aflame): four puppets, the last stone surfaces, 10% larger.

## Numbers (record)

Health 1600, attack 15, speed 0.31, armor 8 (tier 2 band 1300-2000). Holy road, floors 20 to 40. Themes: crystal_geode, astral, radiant, dwarven_forgehold, spatial.
Story: after Lotus Whomalt. Charter Grimoire (30% chance): earth_spikes, earth_wall, earth_jail, body_double, body_armor, scale_armor, physical_attack_resistance,
magic_aura (all in the Tensura 2.0.1.3 id reference, which `EarlyRosterTest` checks; the in-game self test `arc.charter` checks them against the skill registry).

## Model

`tools/arc/char_mars.py`: no source describes him, so everything is the mod's own: pale skin, spiky crystal-coloured hair, cold lavender eyes, cracks in the face and a
gem in the forehead (the first mage stone), a dark indigo uniform with the diamond of his kingdom on the back, lavender plate armour (pauldrons, bracers, greaves: the
Nemean Armor), a crystal sword in the right hand (the Harpe), a crystal that orbits him, and two more glowing stones, one on the chest and one on the back of the left
forearm. The second form is the Gates: the stones burn with Fana's fire (orange), the crystal turns warm and a ring of flames stands behind the head.

## Weapon

None. Mars is not a member of the Seven Deadly Sins.

## Unverified

* His appearance (nothing found), and which of the two conflicting accounts of his defeat is right.
* The numbers, the front guard and the stone rule are designs, not canon.
* Nothing runs in a game: the wisps, the crystal VFX shapes (`CRYSTAL_FX1..3`), the saved stone count and the stagger are written against the APIs and type-checked, not seen.

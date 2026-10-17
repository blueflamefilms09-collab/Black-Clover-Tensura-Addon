# Vetto, the Beast of the Third Eye (`vetto`)

Boss of the Seabed Temple Arc, Saga 1 (saga `ELF_REINCARNATION`), tier 2, Abyssal road, the last and the biggest of the five early bosses. Classes:
`arc/early/VettoBoss`, table `VettoTable`, state `BeastAuras`, record `EarlyRoster.VETTO`. Model: `tools/arc/char_vetto.py` (with a second form). Overview of the
five early bosses: `docs/arc/early.md`.

The owner's brief calls him the reference for "anti-magic against overwhelming physical stats": the fight is built so that a party with anti-magic has a clean answer
and a party without it has a harder, honest one.

## Canon (what the sources say, and how sure it is)

The page fetcher could not reach any wiki (DNS refused), so this is from search digests of the pages listed. The digests disagree on his looks (see Unverified).

* An elf (one of the ten Apostles of Sephirah in one source), reincarnated into a human body and one of the Eye of the Midnight Sun's Third Eye with Rhya and Fana;
  Magic attribute Beast. First appearance chapter 51 of the manga, episode 35 of the anime. A feat: he defeats fifty Magic Knights alone in Raque. Superhuman
  strength, agility and durability, Seal Magic, enormous mana, a body that closes wounds (he regrows an arm Noelle blew off). His Evil Eye raises his magic and
  turns his hair white.
* Beast Magic copies the attributes of animals and shows them as a translucent aura round him (Bear Claw, Cheetah Charge: an aura of cheetah legs round his legs).
  Pushed to his limit he uses the Mythical Beast Magic, which breaks the rules of magic and copies animals that never lived.
* In the Seabed Temple fights he blocks Gio and breaks him, leaves Asta and Kiato heavily injured, and is finally pushed to the wall by Asta: the anti-magic sword
  that stuck in his body weakened the magic that was strengthening it. In the end he releases all his mana to bring the temple down, and Yami slices him apart;
  he is later reborn in an artificial body.
* Sources: https://blackclover.fandom.com/wiki/Vetto , https://villains.fandom.com/wiki/Vetto , https://vsbattles.fandom.com/wiki/Vetto ,
  https://blackclover.fandom.com/wiki/Asta_and_Kiato_vs._Vetto , https://blackclover.fandom.com/wiki/Gio_vs._Vetto ,
  https://blackclover.fandom.com/wiki/Yami_Sukehiro_vs._Vetto , https://blackclover.fandom.com/wiki/Asta,_Vanessa_Enoteca,_and_Finral_Roulacase_vs._Vetto ,
  https://greatestanimebattles.fandom.com/wiki/Noelle_Silva_vs._Vetto , https://deltiasgaming.com/how-powerful-is-vetto-in-black-clover/ ,
  https://www.sportskeeda.com/anime/black-clover-chapter-344-why-beast-magic-seems-familiar-explained .

## The fight: three mechanics, each with a counter

| Mechanic | What it does | Counters the party can learn |
| --- | --- | --- |
| **Beast auras** (`BeastAuras`) | He always wears an aura, shown as a beast territory in its colour under his feet: Bear (damage x1.25, speed x0.92, shrugs off 30% of every ordinary blow), Cheetah (x0.95, x1.28, 10%), Eagle (x1.05, x1.10, 15%), and at the Gates the Mythical aura (x1.35, x1.20, 25%). He changes aura with a telegraphed Aura Shift (bear, cheetah, eagle, bear...) and takes 30% more from blows while it winds up. A bare Vetto has no factors at all. | **Hit him while he shifts** (the wind-up is 24 ticks and a message says so). Everything below. |
| **Tearing an aura** | An anti-magic blow tears the worn aura off at once. So does a burst: 8% of his maximum health inside three seconds (12% for the Mythical aura). A torn Vetto is weaker than a bare one (damage x0.8, speed x0.85, no armour) for 8 seconds after anti-magic, 4 after an overload, and cannot put an aura back on while the wound is open (an Aura Shift is wasted); he staggers briefly. | **Anti-magic** is the clean answer, as Asta's sword was. Without it, **everyone hits at once**: the burst threshold is a share of his health, so a party that spreads its blows out never tears it, and one that times them does. |
| **Mana Release** (ultimate, from 60%) (`ChannelGuard`) | A 70-tick ritual (53 at the Gates), a ring of 15 blocks drawn in red; then a blast for 24 damage at full strength that falls off from 5 blocks to 45% at the edge, and is stopped by walls (a message says a wall stood between). After it he is stripped of his aura for 5 seconds and exposed for 60 ticks. | **Leave the ring**, **put a pillar or a wall between you and him**, or **break the ritual**: 14% of his maximum health inside the wind-up and it fails, tears his aura off for 10 seconds and staggers him for 70 ticks (takes 30% more). |

His closing of wounds is not a heal (a boss may not heal): the framework's single refill at the Gates is the Mythical Beast Magic. The Evil Eye (white hair) is not used
as a mechanic; the second model has the white hair.

## Move table (base damage; the framework scales it with power, victim EP and the balance law; DPS budget 6% per second)

| id | name | from | wind-up (Gates) | cooldown | range | damage | tell and counter |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `bear_claw` | Bear Claw | 20% | 16 (12) | 70 | 0-5.5 | 14 in a cone (5.2 blocks, 52 degrees each side), shove | three lines in his facing |
| `rending_flurry` | Rending Flurry | 20% | 18 (14) | 110 | 0-5 | 5.5 x 3, six ticks apart | three lines |
| `cheetah_charge` | Cheetah Charge | 20% | 18 (14) | 130 | 6-22 | 11 along 18 blocks, 2.8 wide, at 1.9 blocks a tick | two lines, locked at half |
| `talon_dive` | Talon Dive | 20% | 22 (17) | 170 | 7-30 | 14 in 4.5 blocks where he lands; he really leaps (up, over, down on the mark), never into the air with no floor | ring on the mark, locked at half |
| `ground_pound` | Ground Pound (crowd) | 20% | 20 (15) | 150 | 0-8 | 13 in a ring wave to 6.5 blocks | ring that grows; jump |
| `beast_roar` | Beast Roar (crowd) | 20% | 20 (15) | 200 | 0-10 | 6 in 9 blocks, shove (magic damage) | ring that grows |
| `aura_shift` | Aura Shift | 20% | 24 (18) | 380 | 0-48 | none; he is open to blows (x1.3) | ring and glow on his chest |
| `stampede` | Stampede | 40% | 30 (23) | 280 | 6-26 | 14 along 24 blocks, 4 wide, there and back | two lines, locked at half |
| `demon_beast_claws` | Demon Beast Claws (crowd) | 60% | 24 (18) | 240 | 3-24 | 10 on up to 5 marked foes (radius 1.9), thrown up | rings and columns, locked at half |
| `mana_release` | Mana Release (ultimate) | 60% | 70 (53) | 1500 | 0-40 | 24 at the core, 45% at the edge | see above |

Plain blows come from the framework's melee at 70% of his attack, times the aura's damage factor. He wants to stay 2.5 to 4.5 blocks away: a brawler. At the Gates
(second bar, the model swaps to the Mythical form): the Mythical aura on, the Aura Shift always re-forms it, 15% larger.

## Numbers (record)

Health 2000, attack 20, speed 0.32, armor 9 (tier 2 band 1300-2000, the top of it). Abyssal road, floors 30 to 60. Themes: abyss, verdant, spider_warrens,
fungal_underdark, drowned_crypt. Story: after Mars. Charter Grimoire (25% chance): beast_transformation, strengthen_body, agility, swipe, elephant_stampede,
physical_attack_resistance, pain_resistance, self_regeneration, danger_sense (all in the Tensura 2.0.1.3 id reference, which `EarlyRosterTest` checks; the in-game
self test `arc.charter` checks them against the skill registry; the regeneration is lent to the player, not used by the boss).

## Model

`tools/arc/char_vetto.py`: broad and tall (2.45 blocks), tanned, a wild blond mane, a mad grin with a beard, war paint and a scar, a dark vest over a bare chest, bare
arms with wrapped forearms and claws that glow orange, shoulder fur, a red sash. The second form (the Mythical Beast Magic) has white hair, bull horns, a dragon tail
and a red glow. Bare-handed, so his attack clip is the punch and his idle is `proud`. Previews: see the report.

## Weapon

None. Vetto is not a member of the Seven Deadly Sins.

## Unverified

* His looks: one digest says tall, muscular, long blond hair, a beard and a red tattoo across the face; another found nothing about hair, beard, tattoo or clothes.
  The model is the mod's own take. The ten Apostles of Sephirah line is from one digest only.
* All numbers; the aura factors and the burst rule are designs.
* Nothing runs in a game: the leap of the Talon Dive (set by velocity and steered for up to 22 ticks), the aura territories (`BEAST_FX2`, `DEMON_BEAST_FX2`), the speed
  modifier of the aura (an attribute modifier replaced as the aura changes) and the walls that stop the Mana Release are written against the APIs and type-checked, not seen.

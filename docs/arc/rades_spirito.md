# Rades Spirito, the Soul Corpse Mage (boss, saga 1, Abyssal road)

Why this page exists: it records what the story says about Rades Spirito, how the fight was built from it, why each move and each counter is what it is, the
numbers, and what is not verified. Shared parts (base class, config, lead lines) are in [midnight_sun.md](midnight_sun.md). Code: `arc/midnight/RadesBoss.java`,
`RadesThralls.java`, numbers in `RadesTable.java` and `RadesMath.java`, the Soul Abductor's memory in `SoulLedger.java`, tests in `RadesTest.java`.
Model: `tools/arc/char_rades_spirito.py`.

## Canon (what the story says)

* Rades Spirito was a Magic Knight of the Purple Orca squad. His Soul Corpse Magic (his grimoire has a single page) raises and controls corpses by spreading his
  mana through them; the other knights were disturbed by it and he was banished from the Clover Kingdom. He later joined the Eye of the Midnight Sun and was
  betrayed (sacrificed) by Patolli; one source says his power awakened fully in his rage at that betrayal. (blackclover.fandom.com Rades_Spirito,
  Asta_vs._Rades_Spirito, The_Undead_Invasion; villains.fandom.com Rades_Spirito; cbr.com on the toughest enemies)
* The Royal Capital raid: he set an army of corpses on the capital. Two of them are named: No.4 Jimmy, a gunner of cursed bullets, and No.2 Alfred, a floating
  corpse wrapped in lightning. Asta's anti-magic sword cut the mana that moved the corpses, so they stopped; when Asta pressed him he left through a spatial
  magic gate, and Fuegoleon, who took over the fight, was caught in the same gate and ended up in an empty void. (blackclover.fandom.com Episode_22,
  Episode_23, Asta_vs._Rades_Spirito; fandompost.com reviews of episodes 21 to 23)
* The Soul Abductor is his spell that calls a sacrificed soul back to its body, healing what killed it. (blackclover.fandom.com Soul_Abductor)
* Appearance: a man of average height with long messy light-purple hair and one blue eye, dark red bandages over the forehead and the left eye with a light
  green ornament on it, bandages tight round the neck, a scar on the right cheek (from Asta), a dark red sleeveless top and trousers, a glove on the left arm, a
  wide belt, boots with light stripes and a tattered cape. (blackclover.fandom.com Rades_Spirito)

## Design in one paragraph

A fight about CHOOSING TARGETS: he stands behind his dead, and every dead man standing thickens the ward that protects him, so the party must decide whether
to cut the corpses down or to go through them. The story's answer (Asta's anti-magic blade cut the threads that moved the dead) is a real counter, and the
story's ending (the spatial gate) is a real mechanic. His corpses are vanilla zombies, so no model or entity type was added.

## Mechanics and counters (the three that only he has)

1. **The ward of the dead** (`RadesMath.wardFactor`). Every risen corpse that stands soaks 7% of each blow that reaches him, up to 42% (six corpses). Counter:
   cut the corpses down (they are weak and crumble after a minute anyway), or cut the threads: a blow of anti-magic severs the ward for 8 seconds (160 ticks)
   and says so; a Soul Thread that meets a held anti-magic blade is cut and severs it for 4 seconds. While the ward is severed the Soul Abductor fizzles too.
   The OPEN window overrides the ward (an open boss has no guard), but nothing here opens him.
2. **The Soul Abductor** (`SoulLedger`). The ledger remembers up to 32 corpses that fell, with their place and tick. The move calls back the ones that fell in
   the last 15 seconds (300 ticks), once each, as new corpses, as far as the cap allows; if none fell it raises one in front of him. So killing the corpses
   early and all at once is worth less than killing them just before he casts it, and a party that keeps the ward down with anti-magic denies it.
3. **The retreat through a gate** (`RadesMath.shouldRetreat`). Pressed hard, 12% of his maximum health lost within 3 seconds (60 ticks), he leaves through a
   spatial gate to a spot 14 blocks away on the far side of the foes and raises two corpses where he stood; he can do it once per 20 seconds (400 ticks). It is
   drawn (two portals) and announced. Counter: do not burst him in a blink; or be ready for the far side. The gate move also exists as an ordinary move
   (Spatial Retreat, range 0 to 7): he steps to the drawn spot and leaves one corpse behind.

At the Gates (100%) he is the Rades of the betrayal ("his power awakened fully after his rage at being betrayed"): the Wrath of the Betrayed is added, the
army is at its largest (6 by default) and the corpses run wild for 8 seconds after the Legion and the Wrath (speed and strength).

## The army (`RadesThralls`)

Vanilla zombies with the stats of the power step (`thrallHealth` 20 + 8 per step, attack 4 + 1 per step, speed 0.23 + 0.01 per step), a leather helmet, no drops
(the drop handler cancels them), no experience, marked with the UUID of the boss in their persistent data, and never let back into the world from a saved chunk
(the join handler cancels them). The cap by step is 2, 3, 4, 5, 6, and never more than `thrallLimit`. A corpse crumbles after 1200 ticks. They are all removed
when the fight ends or the boss leaves the world.

## The moves (base damage before the boss's damage law, the guard and `damageScale`)

| move | min power | wind-up | cooldown | range | weight | recovery | damage and shape |
|------|-----------|---------|----------|-------|--------|----------|------------------|
| Soul Corpse: Raise the Dead | 20 | 24 | 300 | 0 to 48 | 4 | 10 | 2 to 4 graves in front of him (marked), a corpse rises from each as far as the cap allows; at the cap a ring of soul fire (8) instead |
| No.4 Jimmy: Cursed Bullets | 20 | 20 | 130 | 6 to 36 | 5 | 8 | 4 to 8 bullets in a fan from the gunner's shoulder, 5 each and weakness 3 s, lines drawn and locked 8 ticks before |
| No.2 Alfred: Thunder | 20 | 26 | 190 | 3 to 38 | 4 | 10 | crowd: 3 to 5 marks (radius 2.4) under the foes, lightning, 12 each |
| Soul Thread | 20 | 20 | 220 | 8 to 30 | 3 | 8 | a thread to the target, locks 8 ticks before: 6, a pull toward him (stops 3 blocks away) and a slow of 2 s; cut by a held anti-magic blade |
| Dead Hands | 20 | 24 | 210 | 0 to 16 | 4 | 10 | a pool (radius 2.8) under the target: 9, wither 2 s and a root of 25 ticks through the shared control rule |
| Wraith Wail | 20 | 22 | 170 | 0 to 11 | 3 | 12 | crowd: a ring of 10 blocks runs out from him at a block a tick, 8 once per foe; a jump avoids it |
| Spatial Retreat | 20 | 20 | 260 | 0 to 7 | 3 | 10 | he steps to the drawn spot and leaves a corpse behind; if the way is blocked a ring of soul fire (8) |
| Curse Magic | 20 | 18 | 250 | 6 to 30 | 2 | 8 | one of his learned Tensura dark or curse skills, or three bullets |
| Corpse Burst | 40 | 26 | 280 | 0 to 30 | 3 | 12 | crowd: up to all his corpses within 30 blocks swell and burst in a ring of 3 blocks each, 11; with no corpses, two marks |
| Soul Abductor | 40 | 30 | 520 | 0 to 48 | 2 | 12 | mechanic 2 |
| Legion of the Dead (ultimate) | 60 | 56 | 1300 | 0 to 48 | 5 | 60 | crowd: up to 6 graves marked under the foes, corpses rise from each, they run wild for 8 s and a wail of 8 blocks rings out, 14 |
| Wrath of the Betrayed | 100 | 30 | 240 | 0 to 14 | 4 | 14 | crowd: three wails 12 ticks apart, radius 14, 9 each, the corpses run wild |

The mix: melee and close (hands, wail, burst), ranged (bullets, thunder, thread), area (thunder, wail, burst, legion), mobility (the gate), control (the hands'
root, the thread's pull and slow, both through the shared rules), a signature ultimate. Every wind-up is at least 16 ticks on paper (12 at the Gates).

## Numbers

* `ArcCharacter`: tier 2, 1400 health, 11 attack, 0.27 speed, armour 8, toughness 4; Abyssal road, floors 30 to 55; themes necropolis, cursed_archive,
  haunted_manor, shadow, drowned_crypt (another builder's theme, inert until merged); after heath_grice and lotus_whomalt; Charter (25%): create_lesser_undead,
  create_greater_undead, demon_marionette, curse, curse_bind, mortal_fear, spiritual_attack_resistance, darkness. Colours 0x4A5578 and 0x9AA6C8. Magic id
  `soul_corpse`.
* Hit box 0.62 by 1.91. `ArcLook`: from `tools/arc/manifest.json` (`rades_spirito` and `rades_spirito_form2`, the bigger of the two forms for the margins): walk
  1.11, run 1.97, shadow 0.47, grow 1.19 horizontally and 1.2 up.
* Counts by power step (20, 40, 60, 80, 100): corpses at most 2, 3, 4, 5, 6; graves per Raise the Dead 2, 2, 3, 3, 4; bullets 4, 5, 6, 7, 8; thunder marks 3, 3, 4, 4, 5.

## Tests (`RadesTest`)

The table obeys the laws; the army grows with the power and obeys the server limit; the ward is capped and anti-magic cuts it; marks are fair; the soul ledger
remembers the fallen for fifteen seconds, respects its maximum and is bounded; he runs only when really pressed and only when the gate is ready; close in he
fights with hands and wails and far away with shots and thunder; there is no dead range at the opening once the summons are set aside.

## Model

`tools/arc/char_rades_spirito.py`. First form: long messy light-purple hair, one blue eye, a dark red eyepatch with a light-green ornament, a scar on the right
cheek, a dark red sleeveless top and trousers, a glove on the left arm, bandages on the neck and the right forearm, boots with light cuffs, a tattered cape, and
a grimoire in the left hand with a pale green glow. Second form (the Betrayed): ashen skin, the eye burning green, a wild light-lilac mane, a redder outfit, a
cape torn further and a ring of bone thorns round his head. Added for the game and not from the source: the grimoire and its glow (the hair spikes are the
kit's messy style). Previews: scratchpad `previews/s1b/arc_rades_spirito_*.png`.

## Unverified

* Nothing was played. The caps of the army (6), the 7% per corpse and the numbers of the retreat are first guesses.
* The zombies use the vanilla zombie AI: whether they chase foes through an arena as the design hopes, and whether the sun sets them alight in an open arena
  (they wear a helmet to prevent it), is untested.
* `RadesThralls`: cancelling `LivingDropsEvent`, `LivingExperienceDropEvent` and `EntityJoinLevelEvent` (loaded from disk) is by the documented NeoForge
  semantics; not exercised.
* Which of Soul Thread, Dead Hands, Corpse Burst, Wraith Wail and Wrath of the Betrayed are named in the story: none is a named spell; they are design additions
  built from "controls corpses with his mana", "wraith magic" and the rage at the betrayal.
* The existing player-facing Soul Corpse caster of the mod (`corpse` package, Jimmy, Alfredo, Carl) was not touched; its roles for Jimmy (brawler) and Alfredo
  (bruiser) differ from the story's (gunner, lightning); this boss follows the story.
* Canon summaries come from web search, not from the pages themselves.

## Weapons

None.

# The mana system (1.5.0), after "New Gen's BEST Power System EXPLAINED (Black Clover)" by The Weeb Commander

The video's rules, what the mod already had, and what 1.5.0 adds. Design rule from the owner: **no magic may be a bad draw.** Nothing
here makes you feel you must roll again because you were given something that is not combat focused (the Tensura Uniques problem).

| Video | Before 1.5.0 | 1.5.0 |
|---|---|---|
| Mana is everyone's life force; spending it all makes you pass out or die | a short slowness/weakness at 5% | `ManaLaw.exhaust`: shock at 5%, a black-out at 0.5% (blindness, nausea, root, 30% max health of damage). Lethal only if `mana.lethalExhaustion` is on (default off) |
| Grimoire at 15 at the ceremony, chosen for you | ceremony rolled the magic and the rarity | the grimoire still chooses you, **you choose the magic** (`mana.chooseMagic`, default on): chat buttons / `/multiverse choose <magic>` |
| Every mage has an element + a form; any form can fight | utility books had few attacks | every grimoire gets **Mana Strike** (a 24-block bolt of raw mana, tinted by the book) and **Mana Zone**, appended as the last pages |
| Loved / hated by mana | none | **Affinity**, a balanced trade you can change any time (`/multiverse mana affinity`): loved = 10% cheaper, 10% slower; hated = 10% dearer, 10% quicker. Neither is luck |
| Mage Stages 9 to 0, Arcane, Saint | knight ranks only | **Stage** from rank and class (9 = 5th class junior ... 0 = Wizard King); spells cost 15% more at 9 down to 20% less at 0 and hit 10% weaker to 26% harder. **Arcane** (Anti-Magic users, anyone who has carried a Magic Stone, or set by an admin): 25% cheaper, 30% harder. **Saint** (admin/event): 30% cheaper, 35% harder |
| Mana Zone: tap the mana around you, fly, strike from any angle | a page tag, no effect | the **Mana Zone** page toggles it for 30 s (up to 60 s in thick mana): spells 20% cheaper and up to 40% stronger, light on your feet (slow fall, speed), it drinks mana each second |
| Grand Magic Zones (the Seabed Temple, the Ultime Volcano Trail), thick mana | none | `ManaLaw.density`: deep ocean, the Nether, mountains, the deep dark, the End and great height are denser; entering a Grand Magic Zone is announced and strengthens the Mana Zone |
| Negative mana / Forbidden Magic / the Magic Stones | devil contracts only | the **Magic Stone** item: 60 s of negative mana (spells +60% power, -30% cost), every cast corrupts (40: weakness, 70: darkness and wither, 100: your grimoire seals itself for a minute and you take damage). Anyone below Arcane Stage is torn by it instead |
| Mana Method (Heart Kingdom runes) | Heart cover -15% cost, rune stones | unchanged |
| Dungeons made of mana | none | **not built** (no structure yet) |
| Mana inherited from parents | none | **not built** (a choice replaces the roll instead) |

Commands: `/multiverse mana info`, `affinity <loved|normal|hated>` (anyone, about themselves); admin: `mana stage <player> arcane|saint|clear`,
`mana stone <player>`, `mana corruption <player> <n>`. The status panel (Codex "Status") has a Mana block.
Code: `mana/ManaLaw.java`, `mana/MagicStoneItem.java`, `mana/ManaCommands.java`; hooks in `GrimoireBook` (cost, cooldown, pages, exhaustion),
`BalanceLaw.damage` (power), `multiverse/Ceremony` (the choice), `MultiverseSync` (panel).

## 1.6.0: the Tower Wizard, the summons and the questionnaire (replaces the 1.5.0 plain "choose your magic" offer as the default)

Tensura stats stay as they are (EP, magicules, Tensura race and skills); Black Clover adds buffs and debuffs on top.

1. **The summons.** When you come of age (`wizard.minMinutes` of play time, default 30) and/or have grown enough (`wizard.minLevel` experience levels, default 5; `needBoth` decides "and" or "or"), a **Grimoire Summons** arrives. Read it to learn where the nearest Grimoire Tower is. Admin: `/multiverse wizard summon <player>`.
2. **The Tower Wizard** stands at the tower (spawned when an eligible mage comes near; `/multiverse wizard spawn`). Speak to him with the summons in your pack.
3. **The questionnaire:** nine questions drawn at random (nature, hearth, family story, resolve), the answers in random order, nothing tells you what an answer does. It decides:
   - **magic attribute(s)**: one, and a second (a second grimoire) when two natures are nearly tied (`wizard.dualAttributeChance`);
   - **grimoire type**: the kingdom's suit and the leaves (3 / 4 / 5; five still needs the forbidden-start rule) from your resolve;
   - **kingdom** you hail from: a buff and a debuff (Clover: +2 health, +3% attack / -1 armor; Spade: +8% attack and spell power / -2 health; Heart: spells 5% cheaper / -5% attack; Diamond: +1 luck, +4% speed / -1 armor);
   - **blood of a Tensura race** mixed into yours: a buff and a debuff per race (Heritage table in `mana/Heritage.java`).
4. **The book floats to you** from the wizard to the player, then the grimoire is yours. Mage Stage also gives health and speed.
`wizard.enabled=false` brings back the older ceremony (and `mana.chooseMagic` its choose-or-roll).
Code: `wizard/*`, `mana/Heritage.java`, `client/wizard/*`.

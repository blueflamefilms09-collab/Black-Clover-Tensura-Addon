# Power raise (3.74.0): Djinns, Beast Wills and Vassal Weapons 25 times stronger

**What it is.** The owner asked for the three systems to hit 25 times as hard. One hook on the damage event (`power/PowerEvents`, LOW priority) multiplies the blow of a player who

| system | counts as "out" when | setting (default) |
|---|---|---|
| Djinn | the Djinn is in any phase from the Lesser Summon up (Half Equip, Full Equip, Extreme Magic; the metal vessel's techniques are cast in these) | `djinnFactor` (25) |
| Beast Will | a phase is held (Acquire or Integrate) | `beastWillFactor` (25) |
| Seven Star / Vassal Weapon | the bond is not one of the four Legendary Weapons and the weapon is in the hand | `vassalFactor` (25) |

Blows, spells, techniques and arrows are all raised, because the factor sits on the event every kind of damage passes through.

**Rules** (`power/PowerRules`, pure, `PowerRulesTest`):
* several systems out at once: the LARGEST factor is used, never the product;
* against a player the raised blow is held at the PvP hit cap (`pvpHitCapPercent` / gamerule `nusmpPvpHitCapPercent`, 40 % of the victim's maximum health by default); a blow that was already over the cap is left alone, never lowered;
* a Djinn bearer or Beast Will holder takes `damageTakenFactor` (0.5) of every blow while it is out;
* the Vassal factor multiplies on top of the hero weapon buff of 3.55.0 / 3.73.0 (4 x 2 x level growth), so a level-1 Vassal wielder now deals 200 times a bare blow and a level-100 one 400 times. The four Legendary Weapons are NOT raised by this release (12 to 24 times): raise `holyExtraFactor` in `nusmp-heroweapon-server.toml` if they should stay above the Vassal Weapons.

**Settings** (`serverconfig/nusmp-power-server.toml`, `[power]`): `enabled` (true; false = exactly 3.73.0), `djinnFactor`, `beastWillFactor`, `vassalFactor` (1 to 1000, 1 = off), `damageTakenFactor` (0 to 1).

**Cost (rule 11).** No tick work, no packet, no stored state. Per damage event with a player dealer: two attachment lookups (`hasData`) and, for a hero wielder, the hero context; one multiplication.

**NEW**: package `power`, the config file, self-test `power.rules`. **REPLACED**: nothing. **KEPT**: every number of the three systems (the factor sits on top), the relic balance law, the PvP caps, boss caps (they run after this hook).

**Not seen in a game.** The numbers are the owner's; the PvP cap is the mod's existing one.

## The four Legendary Weapons always stay on top (3.78.1)

The owner's rule. A wielder with the Shield, Sword, Spear or Bow out now gets `vassalFactor x legendaryMargin` (default 25 x 1.5 = 37.5) from the same hook, and the hero weapon buff's Holy factor still multiplies on top. `legendaryMargin` can not be set under 1.1 (`PowerRules.MIN_LEGENDARY_MARGIN`), so no combination of settings puts a Vassal Weapon ahead. With the defaults a level-1 wielder deals, against a bare blow: Vassal 4 x 2 x 25 = 200 times, Legendary 4 x 1.5 x 2 x 37.5 = 450 times; at weapon level 100 both double. With `vassalFactor = 1` neither is raised and the Legendary Weapons keep their 3.73.0 lead (x 1.5). Against players the PvP hit cap holds for both.

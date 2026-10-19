# Grade damage (1.97.0)

Owner request: the damage of relics and weapons starts normal and follows the grade, by weapon type (Tensura weapon types).

| Grade | Range |
|---|---|
| Normal (common) | 20-30 |
| Special | 35-55 |
| Rare | 60-85 |
| Unique | 90-115 |
| Legend | 130-175 |
| God | 190-240 |
| Genesis | 260-350 |

Elsdocia is the one exception (400 base, `item/ElsdociaRules.isLegacyException` of another builder; exempt here by the item id `elsdocia`).

## The code

* `relics/GradeDamage` (pure): `range(grade)`, `damage(grade, weaponType)`, `raised(...)` for tier ascension, `typeOfId(path)` (the class of
  `assets/nusmp/weapon_classes.json`, else the keyword guess of `WeaponClasses`). `relics/GradeDamageItems.weaponTypeOf(ItemStack | RelicDef)`.
* `relics/WeaponGrades` (pure): grade and type of the fixed (non-card) weapons, and `djinn(kind, dark)`.
* The damage is a place in the range: `min + factor * (max - min)`, rounded to half points. Light and fast weapons sit low and make it up in speed,
  swords and spears mid, heavy weapons high, ranged weapons on their own scale. Attack speed and grades are unchanged.

## Type factors and resulting damage (total attack damage, the player's own 1 included)

| Type | factor | Normal | Special | Rare | Unique | Legend | God | Genesis |
|---|---|---|---|---|---|---|---|---|
| dagger | 0.00 | 20 | 35 | 60 | 90 | 130 | 190 | 260 |
| rapier | 0.10 | 21 | 37 | 62.5 | 92.5 | 134.5 | 195 | 269 |
| shortsword | 0.30 | 23 | 41 | 67.5 | 97.5 | 143.5 | 205 | 287 |
| sabre | 0.35 | 23.5 | 42 | 69 | 99 | 146 | 207.5 | 291.5 |
| katana | 0.45 | 24.5 | 44 | 71 | 101 | 150 | 212.5 | 300.5 |
| longsword | 0.50 | 25 | 45 | 72.5 | 102.5 | 152.5 | 215 | 305 |
| spear/trident | 0.55 | 25.5 | 46 | 74 | 104 | 155 | 217.5 | 309.5 |
| mace/axe | 0.65 | 26.5 | 48 | 76 | 106 | 159 | 222.5 | 318.5 |
| hammer | 0.75 | 27.5 | 50 | 79 | 109 | 164 | 227.5 | 327.5 |
| glaive | 0.80 | 28 | 51 | 80 | 110 | 166 | 230 | 332 |
| scythe | 0.90 | 29 | 53 | 82.5 | 112.5 | 170.5 | 235 | 341 |
| greatsword/greathammer | 1.00 | 30 | 55 | 85 | 115 | 175 | 240 | 350 |
| bow | 0.50 | 25 | 45 | 72.5 | 102.5 | 152.5 | 215 | 305 |
| crossbow | 0.55 | 25.5 | 46 | 74 | 104 | 155 | 217.5 | 309.5 |
| staff | 0.45 | 24.5 | 44 | 71 | 101 | 150 | 212.5 | 300.5 |
| wand | 0.05 | 20.5 | 36 | 61 | 91 | 132 | 192.5 | 264.5 |
| shield | 0.20 | 22 | 39 | 65 | 95 | 139 | 200 | 278 |

Other names: knife 0, whip .20, coilwhip .15, cleaver .40, handaxe .40, club .55, lance .55, battleaxe .70, crescent .70, halberd/dadao .85, greataxe .90,
Djinn kinds horn .45, claws .05, claymore 1.0, harpoon .55, daggers 0, anchor .95, fans .25, pincers .30; unknown or item .50.

## Where it is applied

| Family | Edit | Grade |
|---|---|---|
| Relic cards (`relics/**`, waves 0-30) | `RelicDef.baseDamage()` (used by `RelicWeaponItem`, `RelicRangedSupport`); the authored `.melee(damage, speed)` damage stays as `meleeDamage()` and only the speed is used | the card's grade |
| Adopted cards and Black Clover weapons (`MagicWeaponItem.Kind`) | attributes from `WeaponGrades` | as the cards (Wave0) |
| Genesis Demon-Slayer | same, `demon_slayer` | Genesis, greatsword |
| Requip weapons | `RequipItems` | Unique, Nakagami Edge Legend |
| Hero weapons | `HeroWeaponItem.props` (HeroBaseStats rows stay as the design table) | Holy Legend, Seven Star Unique, Vassal Rare |
| Djinn weapons | `DjinnWeaponItem.props` (the rules table keeps its old numbers for the acts) | light Legend, dark God |
| Otherworld Trident, Magic Tool Sword / Spear | `WeaponGrades` | Unique, Normal |
| Celestial keys / Spirit King sceptre, Curios, boss drops | no base damage of their own (bosses drop graded relics), unchanged | |

Tier ascension (`TierEvents`): the damage keeps its place in the range and moves into the range of the effective grade (`GradeDamage.raised`);
the old multiplier table is kept for the exempt item. Aura (`AuraEvents`, `AuraScaling.apply(.., gradeMax)`): the aura bonus cannot carry a hit above the top of
the effective grade's range (a hit already above, from a crit or enchantment, is left alone).

## Before and after (main-hand relic cards, authored damage -> table)

| Grade | cards | before (min / mean / max) | after |
|---|---|---|---|
| Normal | 2 | 1.5 / 2.6 / 3.8 | 20-30 |
| Special | 6 | 2.5 / 4.0 / 5.0 | 35-55 |
| Rare | 67 | 1.5 / 6.5 / 11.5 | 60-85 |
| Unique | 20 | 4.0 / 7.6 / 11.0 | 90-115 |
| Legend | 17 | 4.0 / 8.9 / 12.0 | 130-175 |
| Genesis | 1 | 6.0 | 260-350 |

(Cards without a `.melee` used the default 3 + grade tier.) Other families, old total -> new total: Demon-Slayer 11 -> 350; Severing Greatsword 10 -> 175;
Demon Dweller 8 -> 152.5; Doom Hammer 13 -> 50; Brooch Pin 6 -> 35; Flame Empress Blade 12 -> 102.5; Purgatory Mace 17 -> 104;
Legendary Sword 7 -> 152.5; Legendary Shield 2 -> 139; Vassal Katana 7 -> 71; Vassal Hammer 9 -> 109; Otherworld Trident 19 -> 104;
Magic Tool Sword 6 -> 25; Djinn Amon (light) 13 -> 175, (dark) 14.3 -> 240; Elsdocia unchanged.

## Not changed on purpose

Act and skill damage keep their own numbers (none was derived from the weapon damage; they go through the balance law). Hero form bonuses and
Djinn bonds are added on top of the base, like enchantments and strength.

## Checks

JUnit `GradeDamageTest`, `GradeDamageDataTest`; in-game self-test `relic.grade_damage`.

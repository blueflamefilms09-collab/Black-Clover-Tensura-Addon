# Rankings: one table for everything in the mod

The relics already had grades (Normal, Special, Rare, Unique, Legend, God, Genesis, EX-Singularity: `relics/Grade`, names, colours, stars, loot weights, damage ranges). The ranking system
extends them to everything: a **rank** is a grade plus, optionally, a **place** inside the grade (1 = best). Nothing of the relic code was replaced; `Grade` is the one source of names and colours.

## How an item gets its rank (first match wins)

1. A relic card: its own grade (raised by tier ascension, `Tiers.effectiveGrade`); the place comes from the table if the table agrees with the grade.
2. `assets/anime_multiverse/rankings.json`, `items`: item id (registry path) to `"grade"` or `"grade:place"`.
3. The Djinn order (`djinnOrder` + `djinnTiers`): `djinn_weapon_<id>`, `djinn_weapon_<id>_dark` and `djinn_seal_<id>` take the Djinn's rank (1 = strongest of 36), the tier gives the grade.
4. `rules`: `{"prefix"|"suffix"|"contains": "...", "rank": "grade"}`, first match.
5. `relics/WeaponGrades` (the fixed weapon table: Black Clover swords, hero weapons, vassal weapons ...).
6. The tooltip category's default (`UnifiedTooltipRules`: weapon / armour / book / key Rare, grimoire / core Unique, hero weapon / Djinn Legend, food / block Normal, raised by the item's vanilla rarity).

Ranking is cosmetic (a "Rank: X" tooltip line in the grade's colour). The only gameplay it sets is the Djinn band: the Djinn's place in the order is its weapon hit and Extreme Magic ceiling
(`docs/damage_scale_down.md`). No other item changes in play.

## How it is shown

`tooltip/RankTooltip` (the one shared helper): `rankOf(stack)` and `line(stack)`. A listener on the tooltip event adds "Rank: God Grade #4" under the name of every item of the mod
(spawn eggs left out; relics, Djinn weapons, hero weapons, books, keys and the rest the same). A Djinn weapon adds its number ("Rank: God Grade #4  -  weapon hit 7,657"). The line is a
translatable component with an English fallback (`tooltip.anime_multiverse.rank`, `tools/lang/damage_scale.json`). The existing banner of the unified tooltip and the relic tooltip are untouched. From 2.x the rank line is the relic card (stars, grade, place) and the mod's own older ranks (beast, mana, devil, saiyan, knight, loot, aptitude) map to grades in `rankings.json` `"legacy"`: see `docs/rank_audit.md`. `rankDisplayClassic` brings the plain "Rank: X" text back.

## How to add a rank

* One item: add `"my_item_id": "legend"` (or `"legend:3"` for the third place of Legend) to `items` in `rankings.json`. A place number must be unique inside its grade for the Genesis weapons
  (`RankingsTest` checks that every Genesis main-hand relic of the waves has a unique Genesis place).
* A family: add a rule (`prefix`, `suffix` or `contains`).
* A new Djinn: append its id to `djinnOrder` where it belongs (the order is the strength) and extend the last `djinnTiers` number; `DamageScale.djinnHit` spreads the band over the new count.
* A new relic card: nothing, its grade is the rank.

## Genesis weapons (rank place inside Genesis, strongest first)

Genesis weapons are not in the Djinn band; they sit under the global 10,000 ceiling. Order by lore and power (my reading; change the numbers in `rankings.json`):

| Place | Item |
|---|---|
| 1 | `elsdocia` (the apex weapon: 15,000 ceiling) |
| 2 | `demon_slayer_sword` |
| 3 | `ea_sword_of_rupture` |
| 4 | `sword_of_the_first_dawn` |
| 5 | `astralis_the_eye_of_the_universe` |
| 6 | `void_origin_katana` |
| 7 | `blackrazor` |
| 8 | `magic_emperors_black_halberd` |
| 9 | `primordial_hearth_blade` |
| 10 | `starsplitter_hammer` |
| 11 | `gae_bolg_heartseeker` |
| 12 | `starforged_aeon_bow` |
| 13 | `cardinal_halo_chakram` |
| 14 | `nekrathuls_pact_scythe` |
| 15 | `fairy_glitter_gauntlets` |
| 16 | `requiem_stand_arrow` |
| 17 | `millennium_rod` |

The Djinn table (36 rows: Genesis 1-3, God 4-12, Legend 13-24, Unique 25-36) is in `docs/damage_scale_down.md` section 4.

## Items with a rank and where it comes from

* Explicit: the Genesis weapons above, `genesis_core`, `genesis_seed`, the Legend swords (Demon Dweller / Destroyer, Licht pair, Rimeheart), `miasma_infused_katana`, `dark_rukh_shard`,
  `devils_core`, `grimoire`, `magic_key`, `shadow_key`.
* Djinn: 36 x (weapon, dark weapon, seal) = 108 items.
* Rules: `vassal_*` Rare, `legendary_*` Legend, `*_core` Unique, `*_key` Rare, `magic_tool_*` Normal, spawn eggs Normal.
* Weapon table: every key of `relics/WeaponGrades` that is also an item path (the Black Clover weapons and hero weapons).
* Relic cards: every relic's own grade.

## Still unranked by name (shown by their category's default)

Every other item of the mod gets the tooltip category's default grade (step 6), so none is blank, but these are not ranked on purpose by name: blocks and decorations
(`labyrinth_*`, `weapon_rack`, `relic_pedestal`, altars), food and materials, currency, sigils, quest items and the NPC items. Grimoire pages and Tensura skills are not items, so they carry no rank line
(the books that hold them do). The server self-test `dmgscale.ranks` logs how many items were ranked by each source and which have none, so the exact list is in the server log of the smoke test.

## Tests

`RankingsTest` (pure), `DamageScaleSourceScanTest`, self-test `dmgscale.ranks`.

# elite/items: weapons, armour, consumables, crates and plushies

Source: the "Items, Equipment & Blocks" part of the owner's EliteTensuraAddon inventory (docs/elite/plan.md). The original addon's source is **not available**, so only the NAMES are the
original's; every number, ability, recipe, drop table, texture and model below is designed here. Package `com.newuniverse.nusmp.elite.items` (client part `client/elite/items`), config
`nusmp-elite-items-server.toml`, lang fragment `tools/elite/items_lang.json`.

## Comparison with what the mod already has (CLAUDE.md rule 5)
Searched `src/main`, `tools`, `docs` for: crate, plush, catalyst, health_potion, nightingale, astral, void_edge, fenrir, walpurgis, runic, chronicle.

| Item (ids are `nusmp:<id>`) | Status | What exists / why it is new |
|---|---|---|
| Astral Edge `astral_edge` | **NEW** | no sword by that name. ("astral" is only a dungeon theme and a boss-theme tag.) |
| Void Edge `void_edge` | **NEW** | no sword. The Void Edge SKILL is another builder's (elite/skills); this sword only carries a data-driven ability keyed by its item id and reads the skill through a late-bound predicate. |
| Fenrir Sword `fenrir_sword` | **NEW** | only the staff spell "Fenrir Rod" (heroweapon/staff) exists; that is a spell of the Vassal Staff, not an item. Different thing, both kept. |
| Runic Sword (Renewed / Ancient / Mythical) `runic_sword_renewed`, `runic_sword_ancient`, `runic_sword_mythical` | **NEW** | the Rimeheart *Runeblade* and "rune stone" relics are different items (mana lattice sword, a mana-method relic). Kept. |
| Nightingale armour `nightingale_hood`, `nightingale_cloak`, `nightingale_leggings`, `nightingale_boots` | **NEW** | the 11 Magic Knight robe sets and the relic armour stay as they are. |
| Potential Catalyst `potential_catalyst` | **NEW** | the Devil's Core / Genesis Seed (relics/tier) are tier catalysts of RELICS and the forge's "catalyst" slot is a forge input. A different item: it is a consumable that raises the player (Skill Points + a permanent health step), in the style of the SP Books. |
| Health Potions `health_potion_small|medium|large` | **NEW** | the Recovery Salve is a relic (a slow heal charm), the vanilla potions are effects. These are quick-drink heal items with a regeneration tail and a shared cooldown. |
| Chronicle Book `chronicle_book` | **NEW** | the **Chronicle Sigil** (arc/world, 1.61.0) is a different item and stays. The book only stores a chronicle id (data component `nusmp:chronicle_id`) for the seasons builder. |
| Walpurgis Orb / Seal `walpurgis_orb`, `walpurgis_seal` | **NEW** | nothing named so. Plain items with `nusmp:elite_ref` (free string id) for the council builder. (The mod's "seal" system, the Labyrinth seals, are unrelated.) |
| Crates `common_crate`, `rare_crate`, `elite_crate` (blocks) + keys `common_crate_key`, `rare_crate_key`, `elite_crate_key` | **NEW** | the mod has chests with loot tables and the Black Market, no crate. |
| 54 Mob Plushies `plush_<mob>` (blocks + items) | **NEW** | none. |
| Skill Books / SP Books | **EXISTS, reused** | the `skill_book` crate reward uses the skill-tree's `LootDrops.routeBook` and `SPBookItem`/`SkillTreeApi.addSp`; nothing is changed there. |

Replacements: **none**. Nothing existing is removed or renamed.

## Edits to existing files (all additive)
Listed in the final report of the build (one line in `EliteSystem`, rows in `WeaponEngravings` / `ArmorEngravings` / `weapon_classes.json` / `sounds.json` / `global_loot_modifiers.json`).

## Design notes (the WHY)
Filled in by section while building; see the headings below.

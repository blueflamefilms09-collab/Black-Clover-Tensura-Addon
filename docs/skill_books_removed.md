# The Skill Books and the book skills were removed

Why: the owner asked for the generic Skill Book (`nusmp:skill_book`) and the legacy SP Book (`nusmp:sp_book`) to go, and then for "the skill tied to them" too. CLAUDE.md rule 2 allows a removal the owner asks for; rule 2 also asks that old worlds keep loading, so the ids that old saves can name stay registered (section 4). The Fairy Tail and Celestial books were removed in the same release by a separate change, see `docs/fairy_tail_celestial_books_removed.md`.

## 1. What was removed

* **Items**: `nusmp:skill_book` (`SkillBookItem`) and `nusmp:sp_book` (`SPBookItem`), their models (190 `models/item/skill_book*.json`, `sp_book.json`), their textures (`skill_book.png`, `skill_book_cover.png`, `sp_book.png`, `skill_book_v2/`, `skill_book_3d/`, `skill_book_icons/`), their look/rarity/shimmer code, the unlock banner, the drop effect and the route-unlocked packet (`RouteUnlockedPayload`).
* **Skills tied to the books** (every one had the book system as its ONLY way to be learned; the admin command `/dungeon skill unlock` went through the same books code and was removed with them):
  * the 284 line skills `nusmp:line_<id>` (the list is `LegacyIds.RETIRED_LINES`), with their data (`data/nusmp/unique_skills/*.json`, 284 files), their icons (`textures/skill/line/*.png`, 284 files), the table `assets/nusmp/skill_books.json`, and the line engine that drove them (arts, passives, stances, Evolution and its debt ledger, the catalogue, the cast and gate code, the Evolution config);
  * the 12 Heavy Knight techniques `nusmp:hk_provoke`, `hk_shield_wall`, `hk_shield_bash`, `hk_power_strike`, `hk_swift_step`, `hk_crescent_cut`, `hk_gamblers_coin`, `hk_jackpot`, `hk_ember_bite`, `hk_insanity`, `hk_howl_of_ruin`, `hk_fang_of_insanity` (`LegacyIds.RETIRED_TECHNIQUES`), with `skilltree/hk/**`, `skilltree/ability/**` and `skilltree/kit/TechniqueSkill`.
* **Loot**: the global loot modifiers `nusmp:skill_books` and `nusmp:rift_books` (classes, json files and the two entries in `neoforge/loot_modifiers/global_loot_modifiers.json`), the rarity tables, the pity memory, the boss-book drops and the loot config file.
* **Shop**: the Black Market sold only Skill Books and SP Books. The shelf file `data/nusmp/black_market/stock.json` is now empty (`"entries": []`), `ShopGoods` can no longer make a purchase, and the shop screen no longer reads the route index. The witch, the gold and the sell side (`prices.json`) work as before.
* **Commands**: `/multiverse skillbook list | learn | forget | give` (the `gold` and `shop` subcommands stay), `/dungeon skill unlock | give_sp` and `/dungeon reset skills` (so `/dungeon reset all` clears alignment and waypoint only).
* **Config files** that went with their classes: the loot config (`nusmp-skilltree-loot-server.toml`), the Evolution config (`nusmp-evolution-server.toml`) and the client Skill Book look (`nusmp-skillbook-look-client.toml`).
* **Tests and self-tests**: all of `src/test/.../skilltree/{data,hk,item,kit,line,loot,rarity}`, `SkillRouteTest`, `BannerTimelineTest`, `LineIndex*Test`, `MasteryLevelsTest`, `MigrationPlanTest`, `SkillTextsTest`, `UniqueBooksFilesTest`, `SkillBookLangTest`, the fixtures in `src/test/resources/lines_sample`, and the self-test prefixes `skillbook.rarity.drops`, `skillbook.rarity.read`, `skillbook.blocked`, `skillbook.look.files`, `evolution.counters`, `evolution.debt`, `evolution.ultimatevfx` (also dropped from `SMOKE_EXPECT` in `.github/workflows/smoke.yml`; the bare `skillbook` prefix stays because `skillbook.shop.*` stays).
* **Lang**: 639 keys in `en_us.json` (all `nusmp.skill.line_*` and `nusmp.skill.hk_*` names and descriptions, `item.nusmp.skill_book*`, `item.nusmp.sp_book*`, `tooltip.nusmp.skill_book.*`, `tooltip.nusmp.sp_book.*`, `message.nusmp.skillbook.*`, `title.nusmp.skillbook.*`, `rarity.nusmp.*`, `msg.nusmp.evolution.*`, `nusmp.evolution.*`) and the unused shop blurbs; the fragments `tools/lang/skillbooks_rarity.json`, `evolution.json` and `skillbooks_unique.remove.txt` are gone and `skillbooks_unique.json` keeps only the two `nusmp.skill.legacy*` keys.
* **Tools and docs**: `tools/lines/*` (except `preview_book.py`, see below), `tools/skillbooks/`, `tools/skilltree/gen_item_icons.py`, `tools/tensura_menu/gen_line_icons.py`; the docs `skillbooks_unique.md`, `skillbooks_3_26*.md` and `skills/evolution_*.md`; the admin command doc and a few audit tables were corrected.

## 2. What stays

* The **4 data component ids** `skill_book_id`, `sp_amount`, `sp_kind`, `skill_book_rarity` stay registered (`SkillItems.COMPONENTS`, same codecs, same network sync) so an old inventory that still holds a book stack loads. Nothing reads or writes them any more; the stack itself is an unknown item and Minecraft drops it on load.
* **Inert stubs of every retired skill** (`skilltree/unique/LegacySkills`): the ids of all 284 line skills, the 12 techniques and the older 29 + 30 removed Fairy Tail ids stay registered as hidden `LegacySkill` stubs that do nothing. A player's saved skill list that names one still loads; `LegacyMigration` forgets them at login and at respawn and clears the old Skill Tree record (routes, node levels, toggles, points, class). Nothing can grant one: no item, loot, command or shop does.
* **The shop** (witch, Black Market gold attachment `nusmp:skill_tree`, selling goods for gold, `/multiverse skillbook gold | shop`) with an empty shelf. Decision for the owner: the witch still buys goods for gold that can no longer be spent; either give the shelf new goods (data only: `stock.json`) or remove the shop too.
* Shared helpers that other systems use: `LootRules.Kind`, `LootVictims` (coin loot and quest counts), `KitAllies`, `KitWorld`, `skilltree/state/**` (including `PassiveToggles`), `SkillBookSystem` (one init line for what is left), and the shop art tools.
* `tools/lines/preview_book.py` stays because `tools/canon/*` (the grimoire canon models) imports its item model renderer; its Skill Book only modes are dead.
* The Evolution Ultimate VFX shapes (`VfxShape.ULT_*`, their layers and preview scenes) stay; only the skills that used them are gone.

## 3. Skills kept, with the reason

None of the book skills could be kept: the only other way to get any of them was the admin command, and that was built on the same book code. Skills that are NOT tied to the books and therefore untouched: the Black Clover grimoire skills (`nusmp:book_*`, taught by the grimoires and acceptance), the Requip, Celestial and Fairy Tail skills (their own books and commands, handled by the Fairy Tail / Celestial change), every skill in `skill/NUSkills` (including `SkillEvolution`, which is Tensura's own evolution and does not use the line engine) and Tensura's own skills.

## 4. Save compatibility and cost

* Old worlds: component ids and skill ids stay registered, the attachment `nusmp:skill_tree` stays, so nothing fails to load. Players who owned a retired skill lose it at the next login or respawn (it is replaced by nothing).
* Cost: zero new runtime cost. The removal deletes the per-tick line engine, the Evolution counters, the loot modifiers on every kill and the book packets. The only new work is `LegacyMigration.onJoin`, once per login or respawn, bounded by the size of the player's own skill list; there is no tick handler, no packet and no client code. The 296 stubs are registry entries only.

## 5. New, replaced, kept

* NEW: `LegacySkills` (the stubs register), `RETIRED_LINES` / `RETIRED_TECHNIQUES`, this document.
* REPLACED: `SkillItems` (now only the 4 component ids), `SkillBookCommand` (gold and shop only), `LegacyMigration` (covers all retired ids), `LootRules` (only the kind enum), `ShopGoods`, `ShopScreen` and `ShopSelfTests` (no books), the admin tree.
* KEPT: everything listed in section 2.

## 6. Not checked in a game

Type-checked against API stubs and the pure tests ran; no client was started. Unchecked in a game: loading an old world that holds retired skills, an old inventory that holds book stacks, and the shop screen with an empty shelf.

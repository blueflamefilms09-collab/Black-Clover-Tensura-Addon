# Skill Books 3.26.0: new look, rarity, franchise blacklist, ten Evolution skills

Owner request (3.26.0): "use the images for how the skill books should be designed", plus the two Gemini files (the rarity / loot / 3D-book spec and the "Evolution" format with ten new Unique Skills).
Owner answers: New look + rarity on top of the working system (nothing removed); one look per franchise (rarity adds trim and glow); the WHOLE franchises Tensura, Black Clover and Magi stop dropping and being
sold (behind a setting, files kept, still giveable by command); the ten new skills ship in the same release. This file is the binding contract for the builders. The existing system is described in
`docs/skillbooks_unique.md` (one item `nusmp:skill_book`, component `nusmp:skill_book_id`, `assets/nusmp/skill_books.json`, loot modifiers `skill_books` and `rift_books`, `BossBookDrops`).

## 0. New versus replacement (CLAUDE.md rule 5)

| Part | Status |
|---|---|
| Book covers, in-hand model, inventory icon, ground look (one 3D book per franchise x rarity) | **REPLACEMENT** of the 1.82.0 grey, tinted covers. Old model files and textures stay in the jar and come back with the client setting `legacyLook` |
| Rarity (Common, Uncommon, Rare, Epic, Mythic): table, optional component, name colour, glint, trim, particles | **NEW** |
| Rarity-weighted, Luck-scaled drops; themed Guardian drop; Mythic-only rift pool | **NEW**, and a **REPLACEMENT** of the flat pick, behind the server setting `rarityTiers` (off = the 3.25 rules exactly) |
| Franchise blacklist (tensura, black_clover, magi): no drops, no shop stock | **NEW** behind the server setting `hideBlockedFranchises` (default on) |
| Eleventh franchise `fairy_tail` (look + lines) | **NEW** (appended; the 29 old Fairy Tail books of 1.96.0 stay removed) |
| Ten Evolution Unique skills + books | **NEW** |
| Evolution engine (requirement counters, Ultimate Evolution art gate, Debt kinds) | **NEW** |
| The 254 existing lines, their arts, passives, loot weights, the Black Market, migration, commands | **KEPT** |
| Old covers, old sprites, old model json, component `nusmp:skill_book_id` and the legacy SP components | **KEPT** |

## 1. Rarity (Java package `skilltree/rarity`, builder RARITY)

* `BookRarity` enum, order is the rank, ids saved as lower-case strings: `common`, `uncommon`, `rare`, `epic`, `mythic`. Name colour: common 0xFFFFFF, uncommon 0x55FF55, rare 0x55AAFF, epic 0xC855FF, mythic 0xFFAA2B
  (the tooltip adds a red `Mythic` tag line). Glint (`isFoil`): epic and mythic only. Particle colour for the unlock burst: the same colour.
* Where a book's rarity comes from: the `rarity` field of its line in `assets/nusmp/skill_books.json` (generated, see section 5), read into `LineIndex.Line.rarity()` and `BookLook.Entry`; absent = common.
  A stack may carry the optional component `nusmp:skill_book_rarity` (string, registered APPENDED in `SkillItems`, persistent + network synchronised): when present it wins. `SkillItems.rarityOf(stack)` is the one reader.
* Item name colour is the RARITY colour; the line's own colour stays on the cover gem / ribbon (tint index 1) and on the "Teaches the unique skill: X" row. Tooltip gains `Rarity: <name>` (translatable with fallback).
* Reading (BookUse): already owned = action-bar text "You have already comprehended this path." and the book is NOT consumed; success = shrink 1, sound `UI_TOAST_CHALLENGE_COMPLETE`, particle burst in the rarity colour
  (the existing `UnlockFx` is extended, bounded as before), then the unique skill is taught as today.
* Pure rule `RarityRules` (no Minecraft types, unit tested):
  * `luckFactor(rank, luck)` = `min(20, 1 + 0.5 * max(0, luck) * rank)` (rank 0 for common, so common never changes).
  * Tier 1 (ordinary Abyssal monsters): the chance of a book is `abyssalBookChance(config, default 0.001 = 0.1%)` x `min(10, 1 + 0.25 * looting + 0.05 * max(0, luck))`. The rarity is drawn from the weights
    common 600, uncommon 300, rare 80, epic 10, mythic 0 (so an Epic is about 0.001% a kill at Luck 0), each weight x `luckFactor`. Mythic never drops from monsters or Guardians.
  * Tier 2 (decennial Guardians, Abyssal road; the Holy road only with the existing `holyBossDrops`): `guardianBookChance` (default 1.0 = guaranteed) of ONE themed book. Rarity by depth: floor 10 and 20 rare,
    30 and 40 rare (20% epic), 50 and deeper epic. The book is picked DETERMINISTICALLY from the lines of that rarity (never a blocked franchise, never a mythic) whose `theme` (the 15 backgrounds) fits the sector's
    `RegionTheme` through the table `BossThemes` (all 28 themes mapped to one to three backgrounds, unmapped = any), seeded by (floor, theme id) only, so "the Floor 50 fire boss always drops the same Epic book"
    in every world. A player who already owns it gets the next line of the same list (pity), exactly like `BookPity`.
  * Tier 3 (rift pocket chests, `RiftBookLootModifier`): the pick is MYTHIC lines only. If a rift has no mythic line left to give (all owned) it falls back to epic. Chance from `rift_books.json` x `riftBookScale` x
    `min(3, 1 + 0.1 * max(0, luck))`.
  * `mythic` lines are exclusive to tier 3 (and `/multiverse skillbook give`, the creative tab, the Black Market's flagship entry that already exists).
* Luck = the killer's `Attributes.LUCK` (opening player for chests), looting = the weapon's looting level (0 when unknown). Both read once per drop, never per tick.
* Server settings (`nusmp-skilltree-loot-server.toml`, section `[rarity]`, all plain `define`): `rarityTiers` (true; false = the old pick and the old 0.004 / 0.30 numbers), `guardianBookChance` (1.0),
  `rarityBaseChance` (0.001), `hideBlockedFranchises` (true), `blockedFranchises` (a comma separated STRING, default `tensura,black_clover,magi`; not a list value, those throw while loading).
* Client setting (`nusmp-skillbook-look-client.toml`): `legacyLook` (false; true = the old grey covers, old sprite and always-glint), `dropShimmer` (true).

## 2. The franchise blacklist

`hideBlockedFranchises` on: every door (monster modifier, Guardian drop, rift modifier, pity picks, the Black Market stock and restock, the shop's random lines) treats a line of a blocked franchise as weight 0. Items, route
files, lang, icons, skills, the creative tab, `/multiverse skillbook give/learn` and books already in a player's inventory are untouched (a held book still reads). `RouteTable` builds its weight table with the weights
of blocked lines zeroed (cached per registry version AND per settings value, rebuilt when the setting changes, never per drop). The Black Clover Grimoire items, Magi djinn vessels and Tensura's own skills are NOT skill books and are not touched.

## 3. Look (builder LOOK; tools `tools/lines/book_model_v2.py`, `gen_book_textures_v2.py`, preview `preview_book.py`)

Reference images (owner): (1) the grimoire of "The Exiled Heavy Knight" anime: purple cover, a big dragon-claw fastener on top, a silver chain wrapped across, a red round gem with teal studs, a small teal gem on a brown strap,
gold page edges and gold corner trim, a silver fang ornament at the bottom corner; (2) and (3) two hand-painted icon sheets (book set III by Saarl, book icons by Explorer Games): thick books seen from a 3/4 angle with a visible spine
and page block, embossed covers (swirls, runes, a tree, cracked lava, stone plates, filigree), metal corner caps, straps and clasps, gems, and some open with glowing script above them.

* Franchise look (list in `lines_lib.FRANCHISES` order, APPEND `fairy_tail` as the 11th): exiled_heavy_knight = image 1 (purple, claw, chain, red and teal gems); tensura = teal swirl leather; black_clover = black leather with
  silver filigree and a clover plate; magi = brown tooled leather, leather strap clasp, glowing blue rune; solo_leveling = black cover with a glowing violet rune and shadow-smoke edges; danmachi = deep red leather with gold
  ornaments and a firefly white inlay; tbate = green cloth with a white tree; tower_of_god = blue-grey wood with a steel cross and a cyan gem; god_of_high_school = cracked red-orange lava stone with a brass catch;
  omniscient_readers_viewpoint = grey-violet stone plates with silver rings; fairy_tail = warm brown leather with a gold guild-mark wing and a red eye gem.
* Rarity trim (the SAME book shape at every rarity, trim and glow differ): common = plain leather strap and iron corners; uncommon = bronze corners and a green ribbon; rare = silver corners, a small blue gem, silver strap;
  epic = gold corners, two violet gems, a chain across the cover, a violet inlay that glows (light_emission); mythic = gold + red gems, chain, claw / fang ornaments, an orange-red emissive page edge and a pulsing glow
  (animated emissive texture, `.mcmeta` animation). Rarity trim must be readable at 16 px in the inventory and at 3 blocks on the ground.
* The line's colour tints ONLY the ribbon, the emblem inlay and the small gem (tint index 1), so 254+ books of one franchise differ at a glance but share the franchise cover.
* Geometry: a real 3D book (cover boards, spine with ridges and bands, page block with visible page lines, corner caps, clasp, gems, chain links), at most 140 elements per model, model space 16 units, 9 x 13 x 4 units like the old
  one but richer. Textures 16 px per unit on the cover (front 144 x 208) painted procedurally (numpy / PIL, shading, bevels, noise, edge wear), NOT flat colour blocks; make them look hand-painted like the references.
* Files: `assets/nusmp/models/item/skill_book_v2.json` (the base geometry parent), `skill_book_v2_<franchise>_<rarity>.json` (55 generated, the 11 x 5 combinations), icon models `skill_book_icon_<franchise>_<rarity>.json`
  with 64 x 64 painted 3/4 icons `textures/item/skill_book_icons/<franchise>_<rarity>.png`, covers `textures/item/skill_book_v2/<franchise>.png`, trims `textures/item/skill_book_v2/trim_<rarity>.png`.
  `skill_book.json` gets overrides on THREE predicates `nusmp:look` (0 new, 1 legacy; legacy points at the unchanged old models), `nusmp:franchise` (the existing value) and `nusmp:rarity` ((rank + 1) / 6).
  The old override list stays, below the new ones (add the new, keep the old).
* Display: `gui` = the painted icon; `firstperson` / `thirdperson` hold it like a grimoire, readable and not clipping; `ground` = noticeably larger (scale about 1.15 to 1.4) and tilted, `fixed` for frames, light_emission on
  gems, inlay and mythic page edge. A dropped book also gets the client shimmer `client/skilltree/BookDropFx`: rare and better dropped book entities within 24 blocks emit a slow rarity-coloured particle every 6 ticks;
  at most 24 tracked item entities per client (a small set filled by the client entity-join event and emptied by the leave event; no per-tick world scan), at most 4 particles a tick in all, skipped when the setting is off.
* Registration: `nusmp:rarity` and `nusmp:look` item properties next to `franchise` in `BookClient.registerProperty` (two map lookups, no allocation). `BookLook` grows `rarityValue`.
* Previews: `python3 tools/lines/preview_book.py --model skill_book_v2_<franchise>_<rarity>.json` must render every combination (extend the script, keep its old behaviour); contact sheets at `build/book_preview/` showing, per
  franchise, the five rarities side by side from the gui, hand and ground views. LOOK at the pictures and iterate until they look like the references.

## 4. Ten Evolution Unique skills (builders EVO and ROWS, section filled by the lead after the wiki research)

See `docs/skills/evolution_ten.md` (the designs) once written. They are normal lines (`data/nusmp/skill_trees/<id>.json`, generated from `tools/lines/rows/<franchise>.py`) with these additions:
* route key `evolution`: `{ "requirement": "<counter kind>", "count": N, "text": "Extract 1,000 Souls ...", "ultimate_name": "Ultimate Skill, Shadow Monarch" }`; node flag `ultimate: true` on the Ultimate Evolution art.
* The Ultimate art is locked until BOTH the evolution requirement is met AND the skill is mastered; the passives count at their top level once evolved. Progress is a per-player integer stored on the skill instance, bounded.
* New catalogue kinds for the Debt mechanic (see the doc).

## 5. Rarity table for the books

`tools/lines/rarity_table.py` holds one entry per line (explicit overrides first, then a computed fallback from the line's strongest art), `gen_book_index.py` writes `rarity` into `skill_books.json`. Target spread over all lines:
about 30% common, 28% uncommon, 24% rare, 12% epic, 6% mythic (the mythic ones are the monarch / god / sovereign tier lines). The ten new skills' rarities are fixed by the design doc.

## 6. Who edits what (the lead merges, so builders keep to their own files)

* RARITY: `skilltree/rarity/**` (new), `skilltree/item/{SkillItems,SkillBookItem,BookUse,UnlockFx,BookLook}.java`, `skilltree/loot/**`, `skilltree/shop/**` (stock only), `skilltree/unique/{LineIndex,LineUniqueSkill?}` only to add `rarity()`,
  `tools/lines/rarity_table.py`, `gen_book_index.py`, tests, self-tests `skillbook.rarity.*` and `skillbook.blocked`, `docs/skillbooks_3_26_rarity.md`.
* LOOK: `tools/lines/book_model_v2.py`, `gen_book_textures_v2.py`, `preview_book.py`, all `assets/**` book models and textures, `client/skilltree/{BookClient,BookDropFx}.java`, the client config, `lines_lib.py` (append `fairy_tail`),
  tests, `docs/skillbooks_3_26_look.md`.
* Nobody edits: `lang/en_us.json` (each builder writes a flat fragment `tools/lang/<name>.json` for the lead), `CHANGELOG.md`, `gradle.properties`, `neoforge.mods.toml`, `README.md`, `LIBRARIES.md`, `NUSMP.java` except one registration line.
* Rules for all: CLAUDE.md applies (add the new keep the old, multiplayer / server safe, no lag with the cost stated in the doc, tests plus a self-test, never touch the Time Magic VFX).

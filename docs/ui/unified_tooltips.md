# Unified tooltips: every item of the mod wears the relic tooltip (1.110.0)

Owner request: "all items, weapons, relics and so on should have the same custom tooltip as my relics". Code: `tooltip/UnifiedTooltipRules` (pure decisions),
`tooltip/UnifiedTooltipItems` (reads an item, common code), `tooltip/UnifiedTooltipSelfTests`, `client/ui/tooltip/UnifiedTooltip` (draws, client only).
Lang fragment `tools/lang/unified_tooltips.json` (key `unified_tooltips`). Self-test `ui.unified_tooltips`. Tests `UnifiedTooltipRulesTest`.

## 1. What the relic tooltip is today

`client/relics/RelicTooltip` (wired by `RelicClient` and `UiThemeClient`). For a stack that has a relic definition it:

* recolours the item name in the grade's colour (`Grade.colour()`);
* slips one component in right after the name: a banner row (seven stars and "Rare Grade", in the grade's colour, Genesis cycles through the rainbow), a hairline under it
  (anime look), the "Slot:" row in slate (0xFF8899AA), attunement rows, the summary, the acts (gold, with costs), the passives (green), a "Hold SHIFT" hint;
* wraps to 45% of the screen (120 to 280 px), shrinks to 75% and pages with the mouse wheel when too tall (`RelicTooltipLayout`);
* in the anime look draws a dark glass frame (`0xF2101A30` to `0xF2070B16`) with a border in the grade's colour fading toward violet (`RenderTooltipEvent.Color`);
* never drops the tooltip: any fault falls back to the plain grade and slot lines (`RelicTooltipFallback`).

The other tooltips of the mod are plain `appendHoverText` lines (loot items, grimoires, keys, cores, books, hero weapons via `HeroTooltip`, Djinn weapons ...) with whatever
colours each class chose, and no banner, frame or footer.

## 2. What every other item of the mod gets now

For every stack whose registry namespace is `nusmp`, except relics (they keep their own tooltip, the model) and spawn eggs (the owner excluded them):

| Part | Relic tooltip | Other items now |
|---|---|---|
| Name | grade colour | grade colour (kept as it was if a part of the name already has a colour of its own, e.g. a Djinn's name) |
| Banner row + hairline | stars + "X Grade" | the same row, same hairline, grade by the rule below |
| Slot row | "Slot: Main hand" | "Type: Weapon" in the same slate colour |
| Text / stats / lore | summary, acts, passives | the item's own lines, ALL kept, in their own order and colours; the vanilla attribute block takes the relic palette (green bonus, red malus, slate "When in Main Hand" heading) |
| Footer | (none) | hairline + dim "Multiverse of Anime  .  Weapon" |
| Width | 45% of the screen, 120 to 280 px | the same (`setMaxWidth`) |
| Frame (anime look) | dark glass, border in the grade's colour | the same, from the item's grade |
| Classic look | vanilla colours | vanilla colours |

### The grade of an item without a card

The shown grade is the HIGHER of the category's default and the item's own (vanilla) rarity (common Normal, uncommon Special, rare Rare, epic Unique, above Legend), so a rolled
dungeon weapon keeps its roll's rarity (the loot code writes it as the stack rarity) and a block of plain stone stays Normal. Defaults: Weapon, Armour, Skill Book, Key, Charm
Rare; Tool Special; Grimoire, Core Unique; Hero Weapon, Djinn Legend; Food, Material, Block, Currency, Item Normal.

### The category of an item

`UnifiedTooltipRules.classify`, first match wins: flags the game knows for sure (armour, sword/bow/trident, digger, block item, food component), then the simple class names of the
item's class chain (HeroWeapon, Djinn, Grimoire, SkillBook/SPBook/FtBook, Key, Core, Armor/Robe, Weapon/Sword/Bow/Trident/Spear/Staff/Blade, Tool, Food/Meal,
Trinket/Charm/Seal/Relic, Material/Gem/Fragment/Shard), then the registry path as whole words between underscores for a plain `Item` (`_key`, `_core`, `_book`, `coin`, `ring`,
`ingot`, `stew` ...). Nothing matches: "Item", Normal.

## 3. NEW / REPLACED / KEPT

| Part | Status |
|---|---|
| Generic tooltip for all `nusmp` items (banner, type row, hairlines, footer, frame, stat palette) | NEW |
| Pure rules class + game-side reader + self-test `ui.unified_tooltips` + `UnifiedTooltipRulesTest` | NEW |
| Lang fragment `tools/lang/unified_tooltips.json` (category labels, English fallback in code) | NEW |
| The relic tooltip (`RelicTooltip`, `RelicTooltipLayout`, `RelicTooltipFallback`), the Djinn, socket and attunement lines, `HeroTooltip`, the skill-book "you know this" line, every `appendHoverText` | KEPT, unchanged (this lane did not touch `client/relics/**`) |
| Item stats, attributes, rarities | KEPT, nothing changed |
| Nothing was REPLACED. | |

Wiring (append-only one-liners): `UiThemeClient.init` calls `UnifiedTooltip.init(modBus)`; `NUSMP` registers `UnifiedTooltipSelfTests`.

## 4. Tests

* JUnit `UnifiedTooltipRulesTest`: who gets the tooltip (other mods, eggs, relics excluded), categories from class names / flags / paths, the grade rule (never below the
  category default, negative and huge rarity handled), banner / type / footer shape, the attribute palette, the width rule, the lang fragment covers every category, and a scan
  of the sources: every id registered on a `DeferredRegister.Items` file resolves to a style (the scan sees the ids written as `X.register("id"`; items registered through helper
  loops are covered by the next point).
* Self-test `ui.unified_tooltips` (dedicated server, common code): walks `BuiltInRegistries.ITEM` for `nusmp`, and for every item except eggs and relics checks style, banner
  (contains the grade label), "Type:" row, footer and that the grade is never below the category default; reports the count per category.

## 5. What could NOT be verified (nothing was run in a game)

* The look: no screenshot was possible. Banner, hairline, footer position and the frame colours copy the relic tooltip's numbers, but the `Data` component's height is estimated
  (two rows head, one row footer, +3 px) before the font is known; a banner or footer that wraps (very narrow screens, long translations) could clip one row.
* `RenderTooltipEvent.GatherComponents` ordering with other tooltip mods and with the relic tooltip's own handler: relics are skipped by `Relics.defOf`, so no double banner is expected.
* Recolouring of the vanilla attribute lines relies on the line's translation key (`attribute.modifier.*`, `item.modifiers.*`), also when the key sits in the first part of a
  space-prefixed line; a differently built line stays as it was.
* Category guesses for plain `Item`s rest on the registry path words and may put a few items in a neighbouring category or in "Item"; this only changes the type word and the default
  grade, never a stat. The in-game self-test lists the counts per category so a wrong class can be spotted.
* The name keeps its own colour when a sibling part has one; whether that exactly matches what the owner wants for Djinn / boss items is a look decision.
* No translation of the category labels beyond English; the labels fall back to English through `Language.has`.

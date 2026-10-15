# The Codex: data-driven tabs, rules and learned spells (1.37.0)

The Codex (`client/ui/CodexScreen`, opened with the Multiverse Status key) used to be a Java list of pages. It is now partly a book that
fills itself: new content shows up with no Java change. Two things feed it: JSON files in resource packs, and the game's registries.

## Add a tab, a rule or a lore entry with no Java

Put a file at `assets/<any namespace>/codex/<section>/<name>.json` (in the mod, in a resource pack, in a server pack):

```json
{
  "title": "Relics belong to the Labyrinth",
  "text": "Plain text, or a lang key such as nusmp.codex.rule.relics.text.",
  "keys": "item:nusmp:heart_of_words|mob:nusmp:zagred",
  "icon": "item:nusmp:spirit_charm",
  "order": 10
}
```

* **Section = the directory.** `codex/rules/` is the Rules tab, `codex/lore/` would be a Lore tab. A new directory is a new tab, appended after the
  stock tabs (sections are in alphabetical order). The tab title is the lang key `nusmp.codex.section.<dir>` if you translate it, else the
  directory name made pretty (`spirit_lords` -> `Spirit Lords`). The tab glyph is the first letter of the title.
* **`title`, `text`**: text, or a lang key (lower case, dots, no spaces) that is translated when drawn. A key nobody translated is shown as written.
  A missing title falls back to the file name.
* **`keys`** (optional): discovery keys joined by `|` (`item:`, `mob:`, `magic:`, `page:`, `skill:`, `spiritlord:`, `boss:`, see `docs/discovery.md`).
  With keys the entry reads `???` and shows the discovery hint until the player has found any one of them (creative mode shows all). No keys = always shown.
* **`icon`** (optional): `item:namespace:path`. Do not put an icon of something the player may not have discovered on an entry without keys.
* **`order`** (optional, default 0): lower first; ties go by title, then by id.
* **`"remove": true`**: a pack deletes an entry that a lower pack added (same namespace and file name).
* **Overriding**: the id of an entry is `namespace:file name`. The same id in a higher pack replaces the lower one (the resource manager's own stack
  order, lowest priority first). Another namespace is another entry.
* Press **F3+T** (or switch packs): the book re-reads everything. A screen that is already open keeps its tabs until it is reopened (the tab list is
  fixed for a screen's life); the content of an open tab refreshes.
* A broken file is logged (`[nusmp] Codex file ... skipped`) and skipped; the rest still loads.

### A rule with a live value

A rule entry whose file name is registered in `client/ui/codex/CodexRuleState` shows a dim line `Now: ON`. The stock ones are `relics_exclusive`,
`labyrinth_unbreakable`, `boss_damage_cap`, `hp_buffer`, `vital_strike`, `hunger_and_gloom`, `discovery_hides`. To add one in Java:
`CodexRuleState.register("my_rule", () -> CodexRuleState.bool(MyConfig.SWITCH));`. The server configs are not synced by this mod: when the client cannot
read the value (a dedicated server) the config's default is shown and labelled `(default)`. No packet is involved.

## The generated tabs (from the registries and the player)

Rebuilt each time the Codex opens, so new content appears with no list to edit. They are appended after the stock pages
(Overview, Status, Weapons, Relics, Bestiary, Spirit Lords, Guide), in this order, before the data sections:

| Tab | Source | Text |
|---|---|---|
| Spells | the player's learned grimoires | see below |
| Blocks | every `nusmp:` block in the block registry | lang `nusmp.codex.block.<path>`, else "unbreakable" / hardness |
| Entities | every `nusmp:` entity type | lang `nusmp.codex.entity.<path>`, else the registry name; hidden as `???` until the creature's `mob:` key is found |
| Magic | every `MagicType` | lang `nusmp.magic.<id>.desc`, else a line from its soul and hit effect; `???` until the `magic:` key is found |
| Effects | every `nusmp:` mob effect | lang `nusmp.codex.effect.<path>`, else its category |
| Rules, and any other data section | the JSON files above | |

If you add a lang line with the key in the table, it wins over the generated one.

### Learned spells

The Spells tab lists the grimoire pages the player has opened, grouped by magic (magics alphabetical, spells in the book's own page order). Per spell:
name, description, cost (% of magicule), cooldown and the mastery at which the page opens. Description: the page's written lang line
`nusmp.page.<book>.<page>` if there is one, else a generated line from its cost and cooldown. Pages not yet opened are never named; each magic ends with
one `???` row that counts them. The tab re-reads the player every 2 seconds while open, so a page that mastery opens appears live.
No new packet: the client already holds the grimoire skill instance (with its `Unlocked` page mask); the status summary's `Grimoire.PageList` is the fallback.
The Overview page's "Your magic" list shows a second line with the skill's own description (`nusmp.skill.<id>.description`) when it has one.

## Why it is built this way

* **Resource packs, not a Java list.** The Codex is the one place where the mod explains its rules to the player, and rules change every release. A rule
  is a JSON file next to the lang file; a server pack can add its own house rules or retire a stock one with `remove`, with no mod update.
* **Registry-driven catalogues.** Walking `BuiltInRegistries` at open time means a new block, creature, effect or `MagicType` is listed the moment it is
  registered, and cannot be forgotten.
* **A pure core.** Parsing, merging, ordering, the `???` rule, the column flow and the spell grouping have no Minecraft types and are unit tested
  (`src/test/java/.../client/ui/codex`). The Minecraft-facing classes only read resources and draw.
* **One page class.** Every generated tab is an `EntryListPage` fed by a supplier of `CodexBlock`s; a new tab is a supplier, not a new drawing class.
* **Stable tab list.** `CodexPages.all()` returns a fresh list and `CodexScreen` takes it once when it is created, so a resource reload while the book
  is open cannot shift the tabs under the cursor. More tabs than fit on the edge scroll with the mouse wheel over them.

## Files and why

| File | Why |
|---|---|
| `client/ui/codex/CodexEntryData.java` | The entry as plain data (pure), so the rules around it are testable. |
| `client/ui/codex/CodexEntryParser.java` | Lenient JSON -> entry (pure); a hand-written pack must never break the book. |
| `client/ui/codex/CodexMerger.java` | Pack override / remove / ordering in one place (pure). |
| `client/ui/codex/CodexData.java` | The immutable snapshot of the loaded sections + the small text rules (pretty names, glyph, lang-key test, the `???` rule) (pure). |
| `client/ui/codex/CodexReloadListener.java` | Scans `assets/*/codex/*/*.json` on every resource reload with `listResourceStacks`. |
| `client/ui/codex/CodexClient.java` | Registers the listener on `RegisterClientReloadListenersEvent` (called from `UiThemeClient.init`). |
| `client/ui/codex/CodexRuleState.java` | Rule id -> live value text; reads server configs with the not-loaded fallback, labelled `(default)`. |
| `client/ui/codex/CodexRuleValue.java` | How a rule value is written (pure). |
| `client/ui/codex/CodexLayout.java` | The two-column flow (pure); the same numbers serve height and drawing. |
| `client/ui/codex/CodexRow.java`, `CodexBlock.java` | What a page builder hands to the page. |
| `client/ui/codex/EntryListPage.java` | The one `CodexPage` behind every generated tab (translation, discovery `???`, caching, scrolling height). |
| `client/ui/codex/CodexSources.java` | Registry walkers (blocks, entities, magic, effects) and the data-section bridge. |
| `client/ui/codex/SpellModel.java` | Spell grouping / sorting / fallback lines (pure). |
| `client/ui/codex/SpellSource.java` | Reads the learned grimoires (or the status summary) into `SpellModel`. |
| `assets/nusmp/codex/rules/*.json` | The stock rules, written from the real rules of the mod (configs and docs named in each). |
| `CodexPage.opened()` (modified) | Lets a page that caches forget when the Codex opens. |
| `CodexPages` / `CodexScreen` (modified) | Appended pages, data tabs in `all()`, a fixed page list per screen, scrolling tabs. |

## ui.tensuraOverlay

The Great-Sage analysis popup used to be drawn over Tensura's own Skills window. That is now off by default; see `docs/gui_theme.md`, section
`ui.tensuraOverlay`.

## Known limits

* Nothing here was run in the game; the pure classes are unit tested, the rest was type-checked against stubs that lack the client packages
  (`Minecraft`, `I18n`, `ResourceManager`, `Screen`), so those calls are checked by hand.
* The legacy per-page `GrimoirePageSkill` skills are not listed; only grimoire books (the current system) are.
* A magic's pages are listed under the magic's label; a merged book is listed under its own merged id.

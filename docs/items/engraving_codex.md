# Engravement Codex (`nusmp:engraving_codex`)

Owner request: "add a book/item that says what all the engravements do in my mod".

The Engravement Codex is a hand-held 3D book. Right-click opens a client screen that lists EVERY engraving of the mod, what it does, its cooldowns and caps in words, and which
items carry it. Nothing in the list is typed by hand: it is built from the mod's data every time the book is opened for the first time (and again after a language change or F3+T),
so it follows the data when a number is retuned or an engraving is added.

## NEW / REPLACED / KEPT

| | What |
|---|---|
| NEW | the item `engraving_codex` (grimoire-geometry 3D book, sprite icon, creative-tab entry, shaped recipe), the client screen, the command `/multiverse engravings`, the pure entry builder / filter / layout (`engraving/codex`), the data files `data/nusmp/engraving_codex/*.json`, the lang fragment `tools/lang/engraving_codex.json`, the book textures + generator + previews, the tests, the self-test `item.engraving_codex` |
| REPLACED | nothing. No engraving, enchantment, item, page or screen of the mod was removed or changed in behaviour. The request is new: nothing like an engraving list existed (the older Codex, `Multiverse Status` key, has no engraving tab; the Compatibility Codex is about other mods) |
| KEPT | every engraving table and rule as it was. Appended only: `BookMotif.ENGRAVING` (enum, last), `emblem_engraving` in `GrimoireBookPlan.textures()`, a read-only `codexTable()` accessor in `WeaponEngravings` and `ArmorEngravings`, one line in `GrimoireBakedModel.Overrides`, one entry in `GuiScaleRegistry`, one-liners in `NUSMP`, `NUCreativeTab`, `MultiverseCommands`, one line in `docs/server_safe_allow.txt` |

## What the book lists (105 entries today: 69 + 2 + 34; the live Tensura tag may add more)

| Group in the contents list | Source of the entries | Name | Text |
|---|---|---|---|
| Mod enchantments (2) | the enchantment registry: every `nusmp:` enchantment that is not a franchise engraving (`anti_magic`, `legacy_of_the_wizard_kings`; a new one appears by itself) | lang `enchantment.nusmp.<id>` | lang `.desc` (`anti_magic.desc` is new, in the fragment) |
| Tensura engravings (34 named by the item tables today) | the members of `#tensura:engraving` in the running registry (names and highest level come from there), plus every Tensura engraving id that the item tables name | Tensura's own lang name if the game has it, else made from the id | Tensura's `enchantment.tensura.<id>.desc` if the game has one, else one honest line from `data/nusmp/engraving_codex/tensura_engravings.json` (words only, no number), plus the line "From Tensura Reincarnated, exact numbers are Tensura's." |
| 32 franchises (69 entries, 2 or 3 each) | `data/nusmp/franchise/engravings.json` (the numbers), the lang text generated from it by `tools/franchise/gen_engravings.py` | lang name | the lang description (its two generated closing sentences, "works while ..." and "effects are capped ...", come off: the scope line and the facts say the same with live numbers) |

Every entry shows: the name; the source (the franchise's full name) and the scope in plain words ("Main hand only", "Either hand", "Held or worn anywhere", "On the item itself");
the effect text; facts (cooldown, window, duration, stack limit, hit count, radius, threshold, health floor, and the caps that apply: all damage bonuses together max +15% of the hit and
half against players, all reductions max 20%, a heal never more than 2, a chance never above 25%, magicule back max 5% a minute, a trickle pulse max 0.6%, the level rule); and
"Found on: A, B, C, +N more" (the names of the items, sorted; the number of names shown fits three lines and the "+N more" is never cut off).

Facts are computed by `CodexFacts` from the effect numbers of the table and the constants of `FranchiseRules` (the caps the game really applies), never typed. A test checks that the
shipped lang text still says what the table says (cooldowns, windows, percentages, stack limits, hit counts), so a stale lang text fails the build.

"Found on" is built from the item tables that put the engravings on items, through public read-only views:
`WeaponEngravings.codexTable()`, `ArmorEngravings.codexTable()` (set pieces and armour relics), the relic catalog's `RelicDef.engravings()`, `DjinnWeaponRules.engravings()` (all 36 Djinn
weapons and their dark variants), and `FranchiseMap` plus the item registry for the franchise engravings (every engravable item of the mod that has a franchise: food, potions, spawn
eggs and consumable relics are never engraved and are left out, like in `FranchiseEngravings.engravable`). Item names are the game's own display names.

## The screen (client package only)

* A dark two-page book in the relic tooltip's palette: deep glass, a thin cyan edge, gold brackets, the tooltip's slate for secondary lines and its green for cap lines; drawn flat
  in `renderBackground` (no blur: `ScreenBlurGuardTest` passes), so the search box and the buttons sit on top.
* Left: the title, a search box and the contents list (All, Mod enchantments, Tensura engravings, then one row per franchise with its colour and its count; scroll with the wheel).
  Contents rows use short labels (`data/nusmp/engraving_codex/short_names.json`, e.g. "Tensura (Slime)"); every entry shows the full franchise name.
* Right: a two-page spread. The page turn is a short leaf animation (12 ticks); arrows, PageUp / PageDown, the arrow keys (when the search box is not focused), Home / End, the
  wheel over the pages and the page corners turn it. ESC closes.
* Search: every word must appear (case-insensitive) in the name, source, scope, text, facts or the item names of an entry; entries whose NAME matches come first. The query is cut at 64 characters.
* The text is drawn at full size on a tall page and at 0.85 or 0.75 on a shorter one, so about two entries fit a page. The screen is in the GUI Size list (`engraving_codex`).

## The item

* `nusmp:engraving_codex`, stack size 1, rare. Right-click opens the screen on the client; the server does nothing and sends nothing (`level.isClientSide()` guard, the call to the client
  class is the only mention and is on `docs/server_safe_allow.txt` like `CompatCodexItem`).
* 3D model: the grimoire's own geometry with a fixed look (`EngravingCodexLook`: steel-blue leather, gold frame, pale-gold rune ring cut by a blade, cyan glow while held; textures
  `*_engraving.png` in `textures/item/grimoire_book`, made by `tools/engraving_codex/gen_engraving_codex_assets.py`), model `models/item/engraving_codex.json` (display block of the
  grimoire), 16x16 sprite icon `textures/item/engraving_codex.png` and the flat variant `engraving_codex_icon.json`.
* Tooltip: the three lines from `appendHoverText`, decorated by the unified tooltip like every item of the mod (category Book).
* Creative tab: after the Compatibility Codex. Recipe `data/nusmp/recipe/engraving_codex.json`: a book in the middle, an amethyst shard above and below, an ink sac left and right.
* `/multiverse engravings` (everyone, no argument, no permission): gives the player the book and says where else it comes from (craft or creative tab). From the console it says only
  a player can be handed it.

## Cost (CLAUDE.md rule 11) and worst case

* Server: no tick handler, no packet, no per-player state, no config. The command adds one item stack to the caller's inventory. The CI self-test builds the whole book once.
* Client, once per first open (and after a resource reload or leaving the world): one walk over the item registry (a map lookup per `nusmp:` item), the enchantment registry and the
  item tables, then building about 105 entries. Measured with the real pure classes on this machine: the build of the entries ~90 ms cold, the layout of all entries at one page width ~35 ms
  cold (both once, JIT-cold; warm it is a few ms). Memory: the book is one cached reference, about 100 000 characters of search text and about 40 KB of wrapped lines (bounded by
  the data, never by play time).
* Per change of the search text or the section (never per frame): one filter over about 105 entries plus a pagination over at most that many heights; measured ~tens of microseconds.
* Per frame: the strings of at most two visible pages plus, while a page turns (12 ticks), one more page; about 15 to 30 `drawString` calls per page, a few fills and gradients, two
  scissors and the contents rows (at most about 22). Nothing is split, formatted, translated or allocated in the draw methods (a source-scan test fails the build if a draw method
  uses `split`, `String.format`, `Component.translatable`, `plainSubstrByWidth`, `font.width`, a stream or `new ArrayList`).
* No world, entity or network access from the screen. Worst case with many players online: none of this runs on the server, so server cost is zero.

## Tests

* `CodexEntriesTest`: every enchantment file of `data/nusmp/enchantment` is in the book exactly once and has a name and a text; the lang name and description exist and are used; the
  lang text still carries the cooldowns, windows, percentages, stack limits and hit counts of the table; "Found on" lists come from the item tables and equal the franchise map; group
  order and counts; Tensura entries carry the credit line and contain no number; facts and number words; short labels.
* `CodexFilterLayoutTest`: empty search, group filter, AND search, name-first order, query cut, pagination, oversize entries, word wrap, every entry fits its page at three page sizes,
  the "+N more" is never cut off, laying out and 200 searches are cheap.
* `EngravingCodexAssetsTest`: the look and its textures, the model and icon files, the recipe, the GUI Size entry, the wiring in `NUSMP` / creative tab / command.
* `EngravingCodexSourceScanTest`: no client class in the common package (the one guarded call), the pure classes name no Minecraft type, no packet, no per-player state, the screen draws
  in `renderBackground` and never calls the blurring one, no per-frame allocation or scan, the filter runs only from the text responder, the row click and `init`, the book is cached once and dropped on a reload.
* Existing: `ServerSafetyTest` (allow-list entry), `ScreenBlurGuardTest`, `GuiScaleTest`, `FranchiseDataTest`, `PerfSourceScanTest`, `ConfigFileNamesTest` (no config was added).
* Self-test `item.engraving_codex` (CI server smoke test, dedicated server): the item is registered and gets the unified tooltip, the command node exists, the book builds from the
  server's registries with every franchise engraving once, the two own enchantments and the Tensura engravings, no entry without a name or text, the group counts add up, the filter
  finds Falna first, the group filter, the pagination.

## Previews

`python3 -B tools/engraving_codex/preview_codex_book.py` renders the real book geometry with the real textures (`docs/previews/engraving_codex/book.png`).
`python3 -B tools/engraving_codex/preview_screen.py` lays the real data out with the real pure classes and Minecraft's font widths and draws the screen with the same geometry
(`docs/previews/engraving_codex/screen_*.png`: the first spread, a franchise section, the Tensura section, the own enchantments, two searches, an empty search, a small window,
the second spread and one frame of the page turn). It shows the layout, not the game's own pixels (Pillow's font, not Minecraft's). The preview reads only the weapon and armour tables for the Tensura entries (24 of the 34), so it shows 95 entries; the game also reads the relic and Djinn weapon tables and the live tag.

## Findings while building it

* Seven franchises have engravings in the table but no item of the mod is mapped to them in `franchise_map.json` (Naruto, Bleach, One Piece, Dragon Ball, Hunter x Hunter, JoJo,
  The Exiled Heavy Knight): nothing carries those engravings yet, and the book says so ("Found on: no item of this mod carries it yet") instead of inventing items. The map is up to date
  (`tools/relics/franchise_map.py --check`), so this is the real state, not a stale file.
* `nusmp:anti_magic` had no description. Its enchantment has no effects of its own and nothing in the code reads it; the new lang text says exactly that (a mark that the anti-magic
  blades carry, no numbers of its own).
* The franchise group id `tensura` (That Time I Got Reincarnated as a Slime) collides with a group named after Tensura Reincarnated's engravings: the two non-franchise groups
  use ids that start with an underscore.
* Franchise engravings go on every engravable item of a franchise, so the Dungeons & Dragons engravings list hundreds of items (the book shows six and "+N more").

## What could NOT be verified (nothing can be run in a game here)

* The screen itself has never been opened in the game: layout, text widths with Minecraft's real font, the leaf animation, the EditBox focus and typing, scissor clipping with the GUI-size
  scale hook, hover, and the wheel zones. The preview is a headless layout with Minecraft's glyph widths and another font.
* The 3D book in the hand, in the inventory and on the ground was rendered by the repo's grimoire preview (real geometry and textures), not by the game.
* `CodexGather` (registry walk, tag members, enchantment slots) type-checks against the API stubs and runs in the CI self-test, but has not been run: in particular the
  names and highest levels of the Tensura engravings come from the live `#tensura:engraving` tag, and Tensura's own language file is not in this repository, so what it holds
  (names, any `.desc` text) is unknown. When it has no text the honest fallback lines are shown.
* The fallback lines for the Tensura engravings are short descriptions of how this mod uses each one (no number); Tensura's real effects were not looked up (its code is not here).
  Ten Tensura ids that the item tables name (ruination, stagnation, growth, transcendence, lethargy, intangibility, tsukumogami, sealing, slotting, dead_end_rainbow) have no line yet and show the generic one.
* `EngravingCodexAssetsTest.theLookIsAnEntryOfTheGrimoireCoreWithItsOwnTextures` needs the game classes of the grimoire core (like the Compatibility Codex test) and could not be run in
  the offline test runner here; the same assertions were run against the real pure classes with two stub classes (28 motif looks, all inside the item cube).
* Gradle is not available offline: the whole suite was not run, only the tests above and the existing ones named there, with the offline runner.

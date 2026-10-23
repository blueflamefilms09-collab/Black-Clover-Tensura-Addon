# How many spells does every grimoire really have? (the "15 spells" audit, phase 1)

Owner question: "why do most grimoires only have about 7-9 spells? I want them ALL to have 15 spells."

This is phase 1: it only READS the game (sources, character spell sets, tests). Nothing in the gameplay code was changed. It produces the numbers, the reasons, the limits that would block a 15th page, the minimal recipe
to add a page to any book, and a plan for phase 2 (8 parallel builders). Everything was worked out offline from the sources: **nothing was seen in a game** and the wiki was not fetched (see the last section).

* Counting tool: `python3 -B tools/page_counts.py` (options `--golden`, `--ids <book>`, `--wiki`, `--plan`, `--md`, `--md-update`, `--json`, `--check`).
* Golden file (what phase 2 may only increase): `src/test/resources/pages/page_counts.golden.txt`, guarded by `src/test/java/com/newuniverse/nusmp/book/PageCountsTest.java`.
* A "spell" here is a FAMILY page of a book (`GrimoireBook.familyCount()`). The universal pages every book gets after them (Summon / Store / Draw Weapon, Mana Strike / Zone, Devil Union, the Signature Spell pages, Omni Volley,
  Pressure Space, True Magic, the spirit control pages, and Spirit Dive + Spirit Burst on the Fire / Water / Wind / Earth books) are not spells of the family and are not counted: 18 extra modes per book, 20 on the four spirit books.

## 1. Headline

<!-- The numbers below are produced by the script; the sentences around them are written by hand. -->

* 106 grimoire books (76 registered in `NUSkills` + the 30 generic wiki attributes of `WikiBooks`; the Fairy Tail books are not `GrimoireBook`s and are not part of this). 1062 family pages in all: **10.0 per book on average, 8.2
  per BASIC grimoire** (the grimoire bound to nobody). Only **1 book** (Kotodama, 16) shows 15 or more pages in its basic grimoire; **65 of 106 show 9 or fewer**, 30 show exactly 7 to 9.
* **Missing for "every BASIC grimoire = 15": 719 spells** in 105 books.
* **Missing for "basic AND every character grimoire = 15": 719 new shared pages** (the same 719: once a book's basic grimoire shows 15 shared pages, the pool of every character of that book is also at least 15) **plus 361 list
  entries to put into the 42 character spell sets that are under 15** (43 character grimoires exist; only Rades Spirito's is at 15). If the new pages were character-exclusive instead, the number of pages would be higher (see 5).
* Why most books show 7-9 (details in 2): **550 of the 719 spells were simply never written** (the book has fewer than 15 pages in total; 47 books were written with 9 pages or fewer) and **169 exist but the basic grimoire
  hides them** (190 pages hidden in 43 books: 167 are exclusive to a character's spell set, 23 are reserved in `charset.BasicSignatures`). On top of that a player never sees more than the SLOTS allow: 6 open pages
  at the start, 9 at half mastery, 12 at full mastery (16 only with a spirit or devil contract), so even a 14-page book reads as "7-9 pages" in play.
* **Hard limits that would block page 15+**: the page slots (12, or 16 with a contract), the unlock bit mask (32 family pages per book; the project keeps 24; **Fire would need 33 -> impossible without a second mask int**), and
  the tests that pin the lists (armour policy `new_pages`, the 24-page budget in 7 places). Fixes in 6.

## 2. What was counted, how, and how it was checked

For every book the script rebuilds the list `GrimoireBook.familyAll()` returns at runtime (`book/GrimoireBook.java:140-147`):

1. the book's own pages: its class (field initialiser or constructor), or the factory method of a shared class (`ElementBooks.fire()`, `WikiBooks.transmutation()` ...), or the switch case of `EarthFamilyBooks` /
   `TreeRebuild` / `SongRebuild` for the generic wiki attributes, plus every list it joins (`ElementBook.plus(...)`, `Ext.join(...)`, `XxxExt.pages()`, `XxxArts.pages()`, `XxxSpells.pages()`, `ExclusiveExt.pages(magic)`, local
   helper lists such as `legacyBonePages()`, the Song mode pages whose ids are the `SongMode` enum's);
2. the character pages appended inside the book class: `CharacterPages`, `CharacterPagesBulls`, `CharacterPagesGold` (60 pages);
3. `CharacterPagesRest.append` (15 pages) and `BasicPages.append` (19 pages), both applied by `familyAll()` to every book by its registry path.

Visibility follows `charset.CharacterSetRegistry.hidden` (`charset/CharacterSetRegistry.java:100-110`): the BASIC grimoire hides every page after mode 0 that some character set marks `exclusive` or that
`charset.BasicSignatures` reserves (the numbered Soul Corpse undead stay listed: `SoulCorpseAccess.alwaysListed`); a CHARACTER grimoire shows mode 0, the pages of his set and the pages reserved for him. Both are only in force while the
server settings `magic.characterSpellSets` and `magic.basicBookHidesSignatures` (file `nusmp-character-spells-server.toml`) are true, which is the default.

How it was verified (all offline):

* **By hand, in the sources**: Fire (11 own pages in `FireBook` + 2 Yuno-era + 3 Magna + 6 Mereoleona / Leopold + 2 basic = **24**), Light (5 old + 5 Light-remake + 5 Karna = **15**), Time (12 in `TimeBook`), Kotodama (16
  `word(...)` pages), Wind (6 own + 9 Yuno + 6 basic = **21**), Bone (5 legacy + 2 remake = **7**), Song (5 generic + 4 mode pages + Crescendo + 4 own + 2 Kahono = **16**; the script first missed the 4 mode pages because
  their ids come from the `SongMode` enum, found through the lang cross-check below), Air (8, the javadoc of `AirBook` lists them), Memory (9, its javadoc says 9). `PageCountsTest` repeats six of these counts in plain Java without the script.
* **Every page of every character spell set is found in its book**: 269 spells in 43 sets, 0 missing (`python3 -B tools/page_counts.py --check`). An extractor that lost pages would show here.
* **Reverse check against the lang file**: every `nusmp.page.<book>.<id>` key of `en_us.json` (1041 keys) belongs to a page the script found (the only exceptions are the `spirit_dive` / `spirit_channeling` keys, which are universal pages).
  This is how the Song mode pages were found. 29 pages have no lang key at all (Bronze 13, Sealing 5, Thread 4, Song 5, Soul 2: the in-game audit `MagicAudit` already warns `NO_LANG_PAGES` for them).
* The ORDER of the ids inside a book is exact for plain books and approximate where a book joins several lists; mode 0 is always right. The numbers do not depend on the order.
* Not checked: nothing was run in a game, so a page that the sources define but a runtime condition removes (a config switch) would not show. The only such switches found are `magic.lightBoneJa` (Light and Bone: the table uses the
  default, on, which is also the larger list) and nothing else.

## 3. Why most books show 7-9 (the causes, in numbers)

The generated block shows every number; in words:

1. **The books were written thin.** 27 books have 7 or fewer pages in total, 34 have 8 to 10, 32 have 12 to 14 (the 0.54 attribute expansions wrote exactly 12 per book: 23 books have exactly 12), only 10 have 15 or
   more. The five pages the generic wiki attribute gets (`attribute_bolt`, `_cut`, `_field`, `_domain`, `_guard`) are all that Skin, Switching and Tongue have (5 each). The 1.93.0 rebuilds appended 3 to 5 pages to
   most of the others (8 to 10). Median book: 10 pages.
2. **The engine hides pages in the BASIC grimoire.** The character spell sets mark a character's signature spells `exclusive` (167 pages), and `BasicSignatures` reserves 23 more. Together 190 pages in 43 books
   (World Tree shows 1 of 7, Compass 3 of 4 ...). The hiding is by design (`docs/magic/basic_grimoires.md`): a character's own spells never appear in the basic book.
3. **Both at once** in the 43 books with hidden pages (34 of them are also under 15 in total): their basic grimoires are 357 spells short of 15, of which 188 are never written and 169 are hidden.
4. **The slots.** A player can only have `slots` pages open at a time (see 4.1): 6 + floor(6 x mastery), +4 with a contract, never above 16. A 12-page book at half mastery has 9 open slots. This is the most likely reason the
   owner sees "7-9" even in books that have 12 registered.
5. **The 24-page budget** was a guard rail, not a cause: only Fire (24) is at it.

## 4. The mechanics that cap what a player can use (exact values)

### 4.1 Page slots (`multiverse/MasteryPages.java`)

* `MasteryPages.slots` (lines 36-41): `floor + floor(masteryFrac x (ceiling - floor))`, `+ contractBonus` with a spirit bond, a Spirit Lord skill, a five-leaf cover or a Forbidden pact (`hasContract`, 44-48), `min(..., cap)`.
* Config (SERVER, file `nusmp-multiverse-server.toml`, section `[pages]`, `multiverse/MultiverseConfig.java:60-63`): `slotsFloor = 6` (1..64), `slotsCeiling = 12`, `slotsContractBonus = 4`, `slotsCap = 16`. So: **6 at the start,
  12 at full mastery, 16 with a contract, 16 absolute.** Mode 0 and every open page count; only the Soul Corpse summons are slot-free (`SoulCorpseAccess.slotFree`).
* `tryUnlock` (112-146): when the open pages reach the slots no further page opens (`full`, line 117-118); already open pages stay open. Pages are checked every 40 ticks (`p.tickCount % 40 == 7`, line 105), one page per check,
  in book order; a gated page is skipped and the next may still open.
* The status panel "Pages x / y  slots s  mastery m%" (`client/multiverse/MultiverseStatusScreen.java:82`, data from `multiverse/MultiverseSync.java:84-90`): x = open pages, y = `CharacterSets.visibleCount` (the pages this
  grimoire can ever show: its set, or all minus the hidden ones), s = `slots(...)`. The Four Kingdoms menu, the Grimoire Slot screen and the page-unlock overlay print the same x / y.

### 4.2 The unlock bit mask (`book/GrimoireBook.java`)

* `isUnlocked` (183-185) and `unlock` (187-190) store one bit per family page in an int tag `Unlocked`: `1 << mode`. Java masks a shift by 32, so **mode 32 would alias bit 0 and mode 33 bit 1**: a book can hold at most
  **32 family pages** (modes 0-31; bit 31 is the sign bit and works with `!= 0`). `MasteryPages.remindGate` keeps a second int of the same shape (`GateTold`, `1 << m`, line 150-153).
* Universal pages (`pages()`, 107-134) come after `familyCount()` and use no bit, so they do not count against the 32.
* The project budget is **24** family pages (a margin of 8): `docs/attribute_authoring_guide.md:105-107` and seven test assertions: `charset/BasicGrimoiresTest` (267, 286), `charset/CharacterSetBullsTest` (187),
  `charset/CharacterSetGoldTest` (212), `charset/CharacterSetSourceScanTest` (81), `book/WaterRebuildTest` (166), `vfx/EarthFamilyRebuildTest` (131, 7..24), plus the comment in `BasicPages`. **Fire is at 24 today** (the
  "Fire book limit 24 pages"): 11 own + 11 character pages + 2 basic.
* Other readers of the mask: `book/ForbiddenMagic.java:101-103` and `book/ForbiddenBook.java:240-241` (clear a bit), `core/discovery/DiscoveryTracker.java:248-256` and `DiscoveryRules.unlockedModes`, `avatar/cast/PageTier.mask`
  (already safe up to 31) used by `SimulatedPlayerCaster:180-181`, the client codex `client/ui/codex/SpellSource.java:73-90` (loops `familyCount()`, no cap).

### 4.3 Mastery pacing (`MasteryPages.masteryCost`, 54-59; `needed`, 62-66; `gate`, 86-101)

* A page at place m of n (n = `familyCount()`, hidden pages included) needs mastery >= `max(m / n, costPercent / 35) x masteryScale` (`pages.masteryScale`, default 1.0). Because of the cost term: **starter pages (8 %)
  need 23 % of the bar, mid (12 %) 34 %, zone (20 %) 57 %, signature (35 %) the full bar.** So the tier of a new page decides when it opens, not only its place.
* Rank gates: zone pages (cost >= 20 %) need `pages.gateZone` = INTERMEDIATE:5, signature pages (>= 35 %) `pages.gateSignature` = SENIOR:5; per-page overrides in `pages.pageGates` (defaults: `book_time:chrono_anastasis`,
  `book_fire:leo_rugiens`).
* A grimoire bound to a character with a set opens by HIS rule instead (`acquire` in the set JSON: start, mastery, page_open, cast count, spirit trust ...; `charset/SpellRule`), but the slot rule and the rank gates still apply.
* Mastery comes from successful casts (`GrimoireBook.castPage` -> `addMasteryPoint`, one per cast; Tensura's default per-cast amount and max mastery were not read: the Tensura jar is not in the repo), the Grimoire Altar (+4 per
  300 s, `pages.altarTrainingMastery`, `altarTrainingCooldown`), Spirit Channeling (1 per 100 ticks, `channelMasteryTicks`) and mission gold stars (2.0 each, `masteryPerGoldStar`).

### 4.4 UI and page cycling

* No UI has a hard page cap: the codex list (`SpellSource.fromBook`), the status panel, the Four Kingdoms menu and the slot screen loop over the pages; the status NBT carries a `PageList` entry (id, name, key, cost,
  cooldown, mode) per OPEN page, about 60 bytes each. A 15-page grimoire sends at most 15 entries, only when the status is dirty.
* Cycling: `GrimoireBook.nextMode` (212-223) walks all `pages().size()` modes (family + 18 or 20 universal) and skips unusable ones, so a 15-page book has 33 modes (35 on the spirit books) and one key press scans at
  most that many (`usable`, a few map reads). Each step while the book floats also sends one flip packet to nearby players (`GrimoireSummon.onSpellSwitch`, one per 2 ticks at most).
* Not verified: how Tensura's own skill-mode screen lays out 33-51 modes (it is Tensura's code); the mod's hotbar/key behaviour was read, not played.

### 4.5 Costs, cooldowns, tiers

`book/BookPage.java:31-35`: starter 8 % / floor 150 / 80 ticks, mid 12 / 300 / 160, zone 20 / 600 / 300, signature 35 / 1200 / 600, daily 20 / 600 / 0; `.withCooldown(ticks)`, `.withAnim(clip)`, `.withWeapon(tag)`. Cost goes
through `GrimoireBook.getMagiculeCost` (`BalanceLaw.cost`, cover multiplier, `MagicGear`, `ManaLaw`), the cooldown through `ManaLaw.cooldownMult` x `MagicGear.cooldownMult` x game rule `spellCooldown` (`castPage`, 384-387). The
`SafeCast` guard (core.magic.profile) isolates a faulting magic. The mana zone gate only affects the late-game universal pages.

### 4.6 Tests and files that pin page lists

* `book/ArmourPolicyTest.noPageIdOfTheLastReleaseWasRemovedRenamedOrReordered` (lines 215-245) against `src/test/resources/armour_policy/page_ids_1_98.txt` (page ids per `book/` file at 1.98.0, made by
  `tools/page_id_snapshot.py`): old ids must stay, in order; **every id that is not in the snapshot must be listed in `armour_policy.json` -> `magics.<MAGIC>.new_pages`** (28 magics, 106 ids today; hand-edited, `gen_armour_policy.py`
  does not write them); a NEW file under `book/` that defines pages must have all of them listed too. This test WILL fail for every new page until its id is in `new_pages`.
* `charset/BasicGrimoiresTest` pins exactly the 19 `BasicPages` pages and the hook text in `familyAll`; `CharacterSetBullsTest` / `GoldTest` / `RestTest` pin the pages their registers append. **New pages must therefore not go into
  `BasicPages`, `CharacterPages*`** (add a new appender class, see 5).
* `book/SpatialPageOrderTest`, `WaterRebuildTest`, `vfx/EarthFamilyRebuildTest`, `LightBoneJaDataTest` pin their own books' ids and files.
* `core/magic/profile/MagicAudit` (runtime) warns `NO_LANG_PAGES` for a page without lang text and `NO_PAGES`.
* New in this phase: `PageCountsTest` + `page_counts.golden.txt` (counts may only increase) and `tools/page_counts.py --check` (a set page the book does not have = extractor bug).

### 4.7 What a new page minimally needs

1. A `BookPage` (id lower snake_case, unique in the book and a SAVE KEY: never renamed; name; tier; `.withCooldown`; `.withAnim`) and its cast `(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) -> boolean`.
2. The cast: reuse `ElementBook` shapes (`bolt, volley, cone, nova, field, line, dash, bind, empower, constructs, wall, decoy, healAllies, copy`) and `TensuraShots.shot(...)` for ordinary pages, a custom lambda for a signature
   behaviour; damage ONLY through `GrimoireBook.hurt/hurtAs`; the book's own `VfxShape` / `PropKind` (listed per book in the plan below), so no new asset is needed. Bounds: every entity loop capped, `SpellRuntime.zone` at most 100 ticks
   at 4+ tick steps, at most about 10 effect packets (the `BasicGrimoiresTest.theCastsAreBounded` limits are a good template).
3. A lang text: `nusmp.page.<book registry path>.<id>` in `en_us.json` (one or two sentences; a fragment file `tools/lang/<name>.json` is the repo's convention; there is no single merge script, each builder has its own, e.g.
   `tools/relics/new_wave101/lang.py`). No icon is needed: grimoire icons are per BOOK (`textures/skill/grimoire/<magic>.png`, `tools/gen_skill_icons.py`), not per page.
4. The armour-policy line: the id in `armour_policy.json` -> `magics.<MAGIC>.new_pages` (4.6).
5. Its place in a character set: a `{ "page", "name", "ja", "origin", "exclusive": false, "source", "acquire": {...}, "note" }` entry in each `character_spells/<canon>.json` that should list it (non-exclusive so the basic
   grimoire shows it too). Without an entry a character's grimoire never shows it.
6. The unlock rule: nothing to write; it opens by place, tier and slot (4.3). A gate override only in `pages.pageGates`.
7. A self-test: `SelfTests.register("magic.<key>", ...)` and the key in the smoke list (`.github/workflows/smoke.yml:37`); `PerfProbe` / the perf budgets for anything that repeats; a line in the doc with the worst case
   (entities, packets per second, quads per frame) - rules 9 and 11.
8. Not needed for a spell: engravings (rule 6 is for weapons), models (rule 7 is for weapons and items).

## 5. The recipe: append a page to ANY book (rule 2: append only, modes never move)

1. **One hook, made once by the coordinator before the batches start**: a new class `book/Pages15` with `static List<BookPage> append(String bookPath, List<BookPage> base)`, called as the outermost wrapper in
   `GrimoireBook.familyAll()` (line 144): `Pages15.append(path, BasicPages.append(path, CharacterPagesRest.append(path, f)))`. The text `BasicPages.append(getRegistryName().getPath(), CharacterPagesRest.append(` that
   `BasicGrimoiresTest` looks for stays intact. It returns `base` unchanged (same object) for a book with nothing new. New pages are therefore the LAST pages of the book: no existing mode number, unlock bit, mastery or
   save moves. `Pages15` dispatches to eight batch classes `book/pages15/Batch1.java` ... `Batch8.java` (one per builder, created empty by the coordinator: no two builders touch the same file) and de-duplicates by id.
2. Each batch class has, per book, `static final List<BookPage> X = register("book_x", List.of(BookPage.mid("id", "Name", Batch1Arts::cast).withCooldown(..), ...))` and its casts in `Batch1Arts` (same shape as `BasicPages` /
   `BasicArts`). Behind a server setting `magic.pagesFifteen` (default true) that restores the old lists, as rule 12 asks.
3. Per page: lang fragment, `new_pages` line, set entries, self-test (4.7). The coordinator merges the shared files in one commit: `armour_policy.json`, `en_us.json`, the smoke list, the golden file (`python3 -B
   tools/page_counts.py --golden`), the 24 -> 32 budget in the seven tests.
4. Page tiers for the new pages: keep most of them starter / mid / zone so they open mid-game (4.3); at most one signature per book, because a signature page needs the full mastery bar and Senior rank.
5. If a page should belong to ONE character from the start, put it in the character's JSON as `exclusive: true`; the basic grimoire then hides it again, so it does not count for "basic = 15". The plan assumes
   the 719 new pages are SHARED (visible in the basic book and listable by every character of the book).

## 6. What would block pages 15+, and the proposed fixes

| Limit | Where | Blocks | Proposed fix | Size |
|---|---|---|---|---|
| Page slots 6 -> 12, 16 with a contract | `MasteryPages.slots`, config `pages.slotsCeiling = 12`, `slotsCap = 16` | A 15-page grimoire can never have all pages open without a contract; with the contract 16 is just enough, a 19-page book is not | Defaults `slotsCeiling = 15`, `slotsCap = 19`; and because an existing `nusmp-multiverse-server.toml` keeps the old numbers, also let the code take `ceiling = max(configured ceiling, CharacterSets.visibleCount(inst, book))` and `cap = max(configured cap, ceiling + contractBonus)` | 3 lines in `MasteryPages.slots` |
| Unlock mask = 32 family pages | `GrimoireBook.isUnlocked/unlock`, `GateTold` | **Fire needs 33** (24 + 9 shared pages) | A second int tag `Unlocked2` for modes 32-63 in `isUnlocked`, `unlock`, `ForbiddenMagic:103`, `ForbiddenBook:241`, `DiscoveryRules.unlockedModes`, `PageTier.mask`, `GateTold`; or, for Fire only, make some of its 18 hidden pages non-exclusive so fewer new pages are needed (owner decision, touches the character sets) | about 40 lines + tests |
| Project budget 24 | 7 test assertions, `attribute_authoring_guide.md` | Wind 27, Painting 27, Dark 25 (and Fire 33) after the fix; every other book stays at or under 24 | Raise the assertions to 32 (and the guide), keep a 32 hard assertion | text only |
| Armour policy `new_pages` | `ArmourPolicyTest` | every new page id fails the build until listed | list the ids (coordinator, one pass) | 1 line per magic |
| `BasicPages` / `CharacterPages*` register tests | `BasicGrimoiresTest`, `CharacterSet*Test` | new ids in those lists | use the new `Pages15` appender, not those classes | - |
| Mastery pacing | `masteryCost` | too many zone / signature pages: the late pages are unreachable for most players | tier mix (4 above); optionally `pages.masteryScale` < 1 | tuning only |
| Lang | `en_us.json` | an unlock message shows the raw key | one text per page; fix the 29 existing gaps while at it | text |
| UI | none found | - | - | - |

Books that end above 24 family pages if all new pages are shared (for the mask/budget fix): Fire 33, Wind 27, Painting 27, Dark 25. Every other book ends at 15-24 (Lightning 24, Shadow 23, Water and Light 22 ...).

## 7. The numbers (generated)

<!-- GENERATED:tables (python3 -B tools/page_counts.py --md-update) -->
### Totals

| What | Number |
|---|---|
| Grimoire books | 106 |
| Family pages registered (all books) | 1062 (average 10.0 per book) |
| Pages the BASIC grimoires show | 872 (average 8.2 per book) |
| Books whose basic grimoire is under 15 | 105 of 106 |
| **Spells missing for "every basic grimoire = 15"** | **719** (of which never written: 550; written but hidden in the basic book: 169) |
| Character grimoires (one per character spell set) | 43, of which 42 are under 15 |
| Pages missing from the character grimoires (sum of 15 - pages visible) | 361 |
| **New shared pages so that the basic grimoire AND every character grimoire can reach 15** | **719** (the 719 pages of the basic fix also lift every character's pool to 15; the characters' sets then have to LIST them) |

### Why the numbers are low (the causes, in spells)

| Cause | Books | Spells |
|---|---|---|
| A. The book was only ever written with fewer than 15 pages and nothing in it is hidden from the basic grimoire | 62 | 362 |
| B. The book has pages hidden in the basic grimoire (exclusive to a character set: 167 pages; reserved in `charset.BasicSignatures`: 23 pages), and is ALSO under 15 pages in total | 34 | 188 never written + 169 hidden = 357 |
| C. Total of A + B (the missing spells for "basic = 15") | 105 | 719 |
| Of the 719: spells that do not exist yet (book total below 15) | | 550 |
| Of the 719: spells that exist but the basic grimoire hides | | 169 |
| Books with 15 or more pages registered | 10 | - |
| Of those, books whose basic grimoire still shows fewer than 15 | 9 | 63 |

Histogram of pages per book: 4 pages: 6 books, 5 pages: 8 books, 6 pages: 5 books, 7 pages: 8 books, 8 pages: 10 books, 9 pages: 10 books, 10 pages: 14 books, 11 pages: 3 books, 12 pages: 23 books, 13 pages: 5 books, 14 pages: 4 books, 15 pages: 3 books, 16 pages: 3 books, 17 pages: 2 books, 21 pages: 1 books, 24 pages: 1 books (registered); 1: 1, 3: 2, 4: 12, 5: 15, 6: 5, 7: 6, 8: 13, 9: 11, 10: 16, 11: 3, 12: 17, 13: 2, 14: 2, 16: 1 (shown in the basic grimoire, pages: books).

### Per book (sorted by shortfall of the basic grimoire)

"total" = family pages registered; "hidden" = pages the basic grimoire does not show (exclusive to a character set, or reserved in `charset.BasicSignatures`); "basic" = pages a basic grimoire shows; "short" = 15 - basic; "characters" = `canon: pages visible in his grimoire (short)`. "New pages" = pages to add so the basic book and every character's pool reach 15 (all shared).

| Book | Magic | Total | Hidden | Basic | Short | New pages | Characters |
|---|---|---|---|---|---|---|---|
| `book_world_tree` | World Tree Magic | 7 | 6 | 1 | 14 | 14 | william: 7 (8) |
| `book_compass` | Compass Magic | 4 | 1 | 3 | 12 | 12 | letoile: 3 (12) |
| `book_creation` | Creation Magic | 4 | 1 | 3 | 12 | 12 | - |
| `book_bone` | Bone Magic | 7 | 3 | 4 | 11 | 11 | zenon: 5 (10) |
| `book_copy` | Copy Magic | 4 | 0 | 4 | 11 | 11 | - |
| `book_cotton` | Cotton Magic | 7 | 3 | 4 | 11 | 11 | charmy: 7 (8) |
| `book_dice` | Dice Magic | 4 | 0 | 4 | 11 | 11 | - |
| `book_dream` | Dream Magic | 5 | 1 | 4 | 11 | 11 | dorothy: 4 (11) |
| `book_explosion` | Explosion Magic | 4 | 0 | 4 | 11 | 11 | - |
| `book_mercury` | Mercury Magic | 8 | 4 | 4 | 11 | 11 | nozel: 8 (7) |
| `book_mist` | Mist Magic | 4 | 0 | 4 | 11 | 11 | - |
| `book_poison` | Poison Magic | 7 | 3 | 4 | 11 | 11 | gordon: 5 (10) |
| `book_slash` | Slash Magic | 6 | 2 | 4 | 11 | 11 | jack: 5 (10) |
| `book_storm` | Storm Magic | 6 | 2 | 4 | 11 | 11 | kaiser: 3 (12) |
| `book_thread` | Thread Magic | 9 | 5 | 4 | 11 | 11 | vanessa: 9 (6) |
| `book_ash` | Ash Magic | 9 | 4 | 5 | 10 | 10 | zora: 5 (10) |
| `book_blood` | Blood Magic | 7 | 2 | 5 | 10 | 10 | vanica: 4 (11) |
| `book_dark` | Dark Magic | 15 | 10 | 5 | 10 | 10 | yami: 11 (4) |
| `book_illusion` | Illusion Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_magma` | Magma Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_painting` | Painting Magic | 17 | 12 | 5 | 10 | 10 | rill: 14 (1) |
| `book_reinforcement` | Reinforcement Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_sand` | Sand Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_sealing` | Sealing Magic | 11 | 6 | 5 | 10 | 10 | secre: 7 (8) |
| `book_shadow` | Shadow Magic | 13 | 8 | 5 | 10 | 10 | nacht: 11 (4) |
| `book_skin` | Skin Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_steel` | Steel Magic | 9 | 4 | 5 | 10 | 10 | klaus: 5 (10) |
| `book_switching` | Switching Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_sword` | Sword Magic | 6 | 1 | 5 | 10 | 10 | - |
| `book_tongue` | Tongue Magic | 5 | 0 | 5 | 10 | 10 | - |
| `book_fire` | Flame Magic | 24 | 18 | 6 | 9 | 9 | fuegoleon: 7 (8), leopold: 5 (10), magna: 4 (11), mereoleona: 6 (9) |
| `book_game` | Game Magic | 6 | 0 | 6 | 9 | 9 | - |
| `book_gravity` | Gravity Magic | 9 | 3 | 6 | 9 | 9 | dante: 8 (7) |
| `book_recombination` | Recombination Magic | 10 | 4 | 6 | 9 | 9 | henry: 7 (8) |
| `book_star` | Star Magic | 6 | 0 | 6 | 9 | 9 | - |
| `book_lightning` | Lightning Magic | 16 | 9 | 7 | 8 | 8 | gadjah: 4 (11), luck: 8 (7) |
| `book_mineral` | Mineral Magic | 8 | 1 | 7 | 8 | 8 | mars: 4 (11) |
| `book_red_ochre` | Red Ochre Magic | 7 | 0 | 7 | 8 | 8 | - |
| `book_rock` | Rock Magic | 7 | 0 | 7 | 8 | 8 | - |
| `book_stone` | Stone Magic | 7 | 0 | 7 | 8 | 8 | - |
| `book_transmutation` | Transmutation Magic | 9 | 2 | 7 | 8 | 8 | grey: 3 (12) |
| `book_air` | Air Magic | 8 | 0 | 8 | 7 | 7 | - |
| `book_anti_magic` | Anti-Magic | 8 | 0 | 8 | 7 | 7 | asta: 8 (7) |
| `book_curse` | Curse Magic | 12 | 4 | 8 | 7 | 7 | - |
| `book_light` | Light Magic | 15 | 7 | 8 | 7 | 7 | karna: 6 (9), lemiel: 4 (11) |
| `book_mirror` | Mirror Magic | 13 | 5 | 8 | 7 | 7 | gauche: 9 (6) |
| `book_plant` | Plant Magic | 14 | 6 | 8 | 7 | 7 | mimosa: 6 (9) |
| `book_sandstone` | Sandstone Magic | 8 | 0 | 8 | 7 | 7 | - |
| `book_smoke` | Smoke Magic | 8 | 0 | 8 | 7 | 7 | - |
| `book_snow` | Snow Magic | 8 | 0 | 8 | 7 | 7 | - |
| `book_spatial` | Spatial Magic | 14 | 6 | 8 | 7 | 7 | finral: 4 (11), langris: 4 (11) |
| `book_vortex` | Vortex Magic | 8 | 0 | 8 | 7 | 7 | - |
| `book_wing` | Wing Magic | 8 | 0 | 8 | 7 | 7 | - |
| `forbidden_magic` | Forbidden Magic (devil book) | 8 | 0 | 8 | 7 | 7 | - |
| `book_briar` | Briar Magic | 13 | 4 | 9 | 6 | 6 | charlotte: 8 (7) |
| `book_butoh` | Butoh Magic | 13 | 4 | 9 | 6 | 6 | kiato: 6 (9) |
| `book_cherry_blossom` | Cherry Blossom Magic | 12 | 3 | 9 | 6 | 6 | kirsch: 6 (9) |
| `book_earth` | Earth Magic | 9 | 0 | 9 | 6 | 6 | - |
| `book_ice` | Ice Magic | 9 | 0 | 9 | 6 | 6 | - |
| `book_memory` | Memory Magic | 9 | 0 | 9 | 6 | 6 | - |
| `book_permeation` | Permeation Magic | 9 | 0 | 9 | 6 | 6 | - |
| `book_soul` | Soul Magic | 11 | 2 | 9 | 6 | 6 | lucius: 7 (8) |
| `book_soul_corpse` | Soul Corpse Magic | 15 | 6 | 9 | 6 | 6 | rades: 15 (0) |
| `book_sound` | Sound Magic | 9 | 0 | 9 | 6 | 6 | - |
| `book_wind` | Wind Magic | 21 | 12 | 9 | 6 | 6 | yuno: 13 (2) |
| `book_beast` | Beast Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_body` | Body Magic | 11 | 1 | 10 | 5 | 5 | - |
| `book_demon_water` | Demon Water Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_hair` | Hair Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_modification` | Modification Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_mucus` | Mucus Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_mud` | Mud Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_nail` | Nail Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_poison_plant` | Poison Plant Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_scale` | Scale Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_shakudo` | Shakudo Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_spike` | Spike Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_time` | Time Magic | 12 | 2 | 10 | 5 | 5 | julius: 6 (9) |
| `book_tree` | Tree Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_vine` | Vine Magic | 10 | 0 | 10 | 5 | 5 | - |
| `book_water` | Water Magic | 17 | 7 | 10 | 5 | 5 | noelle: 8 (7) |
| `book_curse_warding` | Curse-Warding Magic | 12 | 1 | 11 | 4 | 4 | - |
| `book_eye` | Eye Magic | 12 | 1 | 11 | 4 | 4 | - |
| `book_food` | Food Magic | 12 | 1 | 11 | 4 | 4 | - |
| `book_barrier` | Barrier Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_black_oil` | Black Oil Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_bubble` | Bubble Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_chain` | Chain Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_copper` | Copper Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_crystal` | Crystal Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_demon_beast` | Demon Beast Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_demon_fire` | Demon Fire Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_demon_ice` | Demon Ice Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_demon_light` | Demon Light Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_eyeball` | Eyeball Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_fungus` | Fungus Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_gel` | Gel Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_glass` | Glass Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_ice_wedge` | Ice Wedge Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_iron` | Iron Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_legion` | Legion Magic | 12 | 0 | 12 | 3 | 3 | - |
| `book_corundum` | Corundum Magic | 13 | 0 | 13 | 2 | 2 | - |
| `book_song` | Song Magic | 16 | 3 | 13 | 2 | 2 | kahono: 5 (10) |
| `book_bronze` | Bronze Magic | 14 | 0 | 14 | 1 | 1 | - |
| `book_key` | Key Magic | 14 | 0 | 14 | 1 | 1 | - |
| `book_kotodama` | Kotodama Magic | 16 | 0 | 16 | 0 | 0 | - |

### Per character grimoire (sorted by shortfall)

| Character | Book | Pages in his grimoire | Short of 15 | Pool he could use (not exclusive to another) |
|---|---|---|---|---|
| grey | `book_transmutation` | 3 | 12 | 9 |
| kaiser | `book_storm` | 3 | 12 | 6 |
| letoile | `book_compass` | 3 | 12 | 4 |
| dorothy | `book_dream` | 4 | 11 | 5 |
| finral | `book_spatial` | 4 | 11 | 11 |
| gadjah | `book_lightning` | 4 | 11 | 10 |
| langris | `book_spatial` | 4 | 11 | 11 |
| lemiel | `book_light` | 4 | 11 | 10 |
| magna | `book_fire` | 4 | 11 | 9 |
| mars | `book_mineral` | 4 | 11 | 8 |
| vanica | `book_blood` | 4 | 11 | 7 |
| gordon | `book_poison` | 5 | 10 | 7 |
| jack | `book_slash` | 5 | 10 | 6 |
| kahono | `book_song` | 5 | 10 | 16 |
| klaus | `book_steel` | 5 | 10 | 9 |
| leopold | `book_fire` | 5 | 10 | 10 |
| zenon | `book_bone` | 5 | 10 | 7 |
| zora | `book_ash` | 5 | 10 | 9 |
| julius | `book_time` | 6 | 9 | 12 |
| karna | `book_light` | 6 | 9 | 13 |
| kiato | `book_butoh` | 6 | 9 | 13 |
| kirsch | `book_cherry_blossom` | 6 | 9 | 12 |
| mereoleona | `book_fire` | 6 | 9 | 11 |
| mimosa | `book_plant` | 6 | 9 | 12 |
| charmy | `book_cotton` | 7 | 8 | 7 |
| fuegoleon | `book_fire` | 7 | 8 | 12 |
| henry | `book_recombination` | 7 | 8 | 10 |
| lucius | `book_soul` | 7 | 8 | 11 |
| secre | `book_sealing` | 7 | 8 | 11 |
| william | `book_world_tree` | 7 | 8 | 7 |
| asta | `book_anti_magic` | 8 | 7 | 8 |
| charlotte | `book_briar` | 8 | 7 | 13 |
| dante | `book_gravity` | 8 | 7 | 9 |
| luck | `book_lightning` | 8 | 7 | 13 |
| noelle | `book_water` | 8 | 7 | 17 |
| nozel | `book_mercury` | 8 | 7 | 8 |
| gauche | `book_mirror` | 9 | 6 | 13 |
| vanessa | `book_thread` | 9 | 6 | 9 |
| nacht | `book_shadow` | 11 | 4 | 13 |
| yami | `book_dark` | 11 | 4 | 15 |
| yuno | `book_wind` | 13 | 2 | 21 |
| rill | `book_painting` | 14 | 1 | 17 |
| rades | `book_soul_corpse` | 15 | 0 | 15 |
<!-- /GENERATED:tables -->

## 8. Plan for phase 2 (8 parallel batches)

Rules the plan follows: every new page is APPENDED (rule 2), checked as new vs replacement (rule 5: all of these are NEW pages; nothing is replaced), canon first and the wiki order of rule 10 (English wiki, Japanese wiki
as fallback, say per page which), bounded and server-friendly (rules 9 and 11), kept behind a setting that restores the old lists (rule 12), version bump and changelog by the coordinator only (rule 4).

**Step 0 (coordinator, before the batches):** the `Pages15` hook and the eight empty batch classes (5); the slot fix and the second mask int (6); the budget 24 -> 32 in the tests; the `magic.pagesFifteen` setting;
the smoke-list keys `magic.pages15_b1` ... `b8`. Without step 0 Fire cannot be finished and nothing past 12 pages opens for a player.

**Step 1 (research, per batch, can start now):** the repo already holds the English wiki pages of 44 books in `.github/wiki/*.txt` (the attribute page with its `-- spell pages:` line; spell pages `spell_*.txt`). For the other 62 books
(listed as "no repo copy" below) the pages are fetched with the existing workflow `.github/workflows/fetch-wiki.yml`: put `magic:<Page title>` lines (and `spell:<Spell title>` for each spell page) into `.github/wiki-request.txt`
and push; the extract is committed to `.github/wiki/<slug>.txt`. Phase 1 could not run it (nothing is pushed here). The sources for each entry are to be written into the batch doc.

**What the research will probably find (measured, not guessed):** in the 44 books with a repo copy of the wiki page, the page lists 151 spells and only **25** of them are not already a page of the book, while those 44 books
still miss 240 spells. So at most about a tenth of the missing spells can come from the wiki's spell lists; the rest has to come from (a) spells the manga / anime / games show without a wiki spell page (the Japanese wiki is
the place to look), (b) the character sets' own source notes (`sources.en_wiki` / `ja_search` in each `character_spells/*.json` name spells that were not built), and (c) **extension pages in the book's own naming**, labelled
as such in the docs (the class (c) "mod's own page" that `basic_grimoires.md` already accepts, and that the Flame / Dark / Earth rebuild docs already use). **Decision for the owner:** whether pages of kind (c) are acceptable for the
majority of the 719; if not, the goal "15 for every book" cannot be met from canon.

<!-- GENERATED:plan (python3 -B tools/page_counts.py --md-update) -->
| Batch | Books | Missing spells (basic = 15) | Character set lists to extend |
|---|---|---|---|
| Batch 1: Flame, Lightning, Star and Thread | 10 | 92 | 74 |
| Batch 2: Water, Ice, Fluids and Art | 17 | 92 | 8 |
| Batch 3: Wind, Air, Sound, Mind and Soul | 13 | 87 | 31 |
| Batch 4: Earth, Stone and Metal | 16 | 96 | 28 |
| Batch 5: Light, Dark and Curse | 11 | 86 | 66 |
| Batch 6: Space, Time, Game and Mirror | 12 | 86 | 66 |
| Batch 7: Nature and Beast | 13 | 86 | 51 |
| Batch 8: Seal, Transform and Body | 14 | 94 | 37 |

#### Batch 1: Flame, Lightning, Star and Thread

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_explosion` | Explosion Magic | 4 / 4 | 11 | - | no repo copy of the wiki page | - |
| `book_storm` | Storm Magic | 6 / 4 | 11 | kaiser 3 (+12 to list) | no repo copy of the wiki page | STORM_CLOUD STORM_FALL STORM_BOLT STORM_FRONT STORM_CANCEL |
| `book_thread` | Thread Magic | 9 / 4 | 11 | vanessa 9 (+6 to list) | no repo copy of the wiki page | THREAD_LINE THREAD_WEB THREAD_STRINGS THREAD_COCOON THREAD_TRAP_LINE THREAD_SKIN |
| `book_magma` | Magma Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_sword` | Sword Magic | 6 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_reinforcement` | Reinforcement Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_fire` | Flame Magic | 24 / 6 | 9 | fuegoleon 7 (+8 to list), leopold 5 (+10 to list), magna 4 (+11 to list), mereoleona 6 (+9 to list) | no repo copy of the wiki page | FLAME_TRAIL FLAME_EXPLOSION FIRE_LION FIRE_SPEAR FIRE_PILLAR FIRE_BURST FIRE_SPIRAL FIRE_WILD FLAMEFX_CAST FLAMEFX_ZONE FLAMEFX_BURST FLAME_1 FLAME_2 |
| `book_star` | Star Magic | 6 / 6 | 9 | - | no repo copy of the wiki page | STAR_MAGIC |
| `book_lightning` | Lightning Magic | 16 / 7 | 8 | gadjah 4 (+11 to list), luck 8 (+7 to list) | no repo copy of the wiki page | LIGHTNING_SPEAR LIGHTNING_FIEND LIGHTNING_GOD LIGHTNINGFX_CAST LIGHTNINGFX_ZONE LIGHTNINGFX_BURST LIGHTNING_1 LIGHTNING_2 |
| `book_demon_fire` | Demon Fire Magic | 12 / 12 | 3 | - | none (all covered) | DEMON_FIRE_FX1 DEMON_FIRE_1 DEMON_FIRE_2 |

#### Batch 2: Water, Ice, Fluids and Art

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_creation` | Creation Magic | 4 / 3 | 12 | - | no repo copy of the wiki page | - |
| `book_mist` | Mist Magic | 4 / 4 | 11 | - | no repo copy of the wiki page | MIST_MAGIC |
| `book_painting` | Painting Magic | 17 / 5 | 10 | rill 14 (+1 to list) | Invasion of Heat-Infused Ice | PAINTING_FX1 PAINTING_1 PAINTING_2 |
| `book_snow` | Snow Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | SNOW_FX1 SNOW_1 |
| `book_ice` | Ice Magic | 9 / 9 | 6 | - | Ice Funeral; Ice Slicer; Ice Needle; Jötunn's Ice Floe; Cage of Infinite Hail; Endless Ice Fangs | ICE_FX1 ICE_WEDGE_FX1 ICE_1 ICE_2 ICE_WEDGE_1 ICE_WEDGE_2 |
| `book_water` | Water Magic | 17 / 10 | 5 | noelle 8 (+7 to list) | no repo copy of the wiki page | WATER_SPLASH WATER_RING WATER_DRAGON WATER_CRADLE WATER_BURST WATER_JAVELIN WATER_NEST WATER_DRESS WATERFX_CAST WATERFX_ZONE WATERFX_BURST WATER_1 WATER_2 |
| `book_demon_water` | Demon Water Magic | 10 / 10 | 5 | - | none (all covered) | DEMON_WATER_FX1 DEMON_WATER_1 DEMON_WATER_2 |
| `book_mucus` | Mucus Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | MUCUS_FX1 MUCUS_FX2 MUCUS_FX3 MUCUS_1 |
| `book_mud` | Mud Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | MUD_FX1 MUD_FX2 MUD_FX3 MUD_1 |
| `book_food` | Food Magic | 12 / 11 | 4 | - | none (all covered) | FOOD_MAW FOOD_FX1 FOOD_1 FOOD_2 |
| `book_eye` | Eye Magic | 12 / 11 | 4 | - | none (all covered) | EYE_FX1 EYE_1 EYE_2 |
| `book_demon_ice` | Demon Ice Magic | 12 / 12 | 3 | - | none (all covered) | DEMON_ICE_FX1 DEMON_ICE_1 DEMON_ICE_2 |
| `book_bubble` | Bubble Magic | 12 / 12 | 3 | - | none (all covered) | BUBBLE_FX1 BUBBLE_1 BUBBLE_2 |
| `book_ice_wedge` | Ice Wedge Magic | 12 / 12 | 3 | - | none (all covered) | ICE_WEDGE_FX1 ICE_WEDGE_1 ICE_WEDGE_2 |
| `book_gel` | Gel Magic | 12 / 12 | 3 | - | none (all covered) | GEL_FX1 GEL_1 GEL_2 |
| `book_black_oil` | Black Oil Magic | 12 / 12 | 3 | - | none (all covered) | BLACK_OIL_FX1 BLACK_OIL_1 BLACK_OIL_2 |
| `book_eyeball` | Eyeball Magic | 12 / 12 | 3 | - | none (all covered) | EYEBALL_FX1 EYEBALL_1 EYEBALL_2 |

#### Batch 3: Wind, Air, Sound, Mind and Soul

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_dream` | Dream Magic | 5 / 4 | 11 | dorothy 4 (+11 to list) | no repo copy of the wiki page | DREAM_TRANSITION DREAM_DOME DREAM_MANIFEST DREAM_SHATTER |
| `book_illusion` | Illusion Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_air` | Air Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | AIR_FX1 AIR_1 |
| `book_smoke` | Smoke Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | SMOKE_FX1 SMOKE_1 |
| `book_vortex` | Vortex Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | VORTEX_FX1 VORTEX_1 |
| `book_wing` | Wing Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | WING_FX1 WING_1 |
| `book_wind` | Wind Magic | 21 / 9 | 6 | yuno 13 (+2 to list) | no repo copy of the wiki page | WIND_SLASH WIND_RING WIND_TORNADO WIND_GALE WIND_EMPEROR WIND_ZEPHYR |
| `book_sound` | Sound Magic | 9 / 9 | 6 | - | no repo copy of the wiki page | SOUND_FX1 SOUND_1 SOUND_2 |
| `book_memory` | Memory Magic | 9 / 9 | 6 | - | no repo copy of the wiki page | MEMORY_FX1 MEMORY_1 MEMORY_2 |
| `book_permeation` | Permeation Magic | 9 / 9 | 6 | - | no repo copy of the wiki page | PERMEATION_FX1 PERMEATION_1 PERMEATION_2 |
| `book_soul` | Soul Magic | 11 / 9 | 6 | lucius 7 (+8 to list) | no repo copy of the wiki page | SOUL_VORTEX SOUL_FX1 SOUL_CORPSE_FX1 SOUL_1 SOUL_2 SOUL_CORPSE_1 SOUL_CORPSE_2 |
| `book_soul_corpse` | Soul Corpse Magic | 15 / 9 | 6 | rades 15 (+0 to list) | no repo copy of the wiki page | SOUL_CORPSE_FX1 SOUL_CORPSE_1 SOUL_CORPSE_2 |
| `book_song` | Song Magic | 16 / 13 | 2 | kahono 5 (+10 to list) | no repo copy of the wiki page | SONG_NOTES SONG_RING SONG_FX1 SONG_1 SONG_2 |

#### Batch 4: Earth, Stone and Metal

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_mercury` | Mercury Magic | 8 / 4 | 11 | nozel 8 (+7 to list) | Dimension Slash Scorching Light | MERCURY_DOME MERCURY_SPEAR MERCURY_EAGLE MERCURY_FX1 MERCURY_1 MERCURY_2 MERCURY_3 |
| `book_sand` | Sand Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | SAND_MAGIC SAND_1 SAND_2 |
| `book_steel` | Steel Magic | 9 / 5 | 10 | klaus 5 (+10 to list) | no repo copy of the wiki page | - |
| `book_rock` | Rock Magic | 7 / 7 | 8 | - | no repo copy of the wiki page | EARTHFAM_CAST EARTHFAM_ZONE EARTHFAM_BURST |
| `book_red_ochre` | Red Ochre Magic | 7 / 7 | 8 | - | no repo copy of the wiki page | EARTHFAM_CAST EARTHFAM_ZONE EARTHFAM_BURST |
| `book_stone` | Stone Magic | 7 / 7 | 8 | - | no repo copy of the wiki page | STONE_SPIKES EARTHFAM_CAST EARTHFAM_ZONE EARTHFAM_BURST |
| `book_mineral` | Mineral Magic | 8 / 7 | 8 | mars 4 (+11 to list) | no repo copy of the wiki page | EARTHFAM_CAST EARTHFAM_ZONE EARTHFAM_BURST |
| `book_sandstone` | Sandstone Magic | 8 / 8 | 7 | - | no repo copy of the wiki page | EARTHFAM_CAST EARTHFAM_ZONE EARTHFAM_BURST |
| `book_earth` | Earth Magic | 9 / 9 | 6 | - | no repo copy of the wiki page | EARTH_SPIKES EARTH_RISE EARTH_FISSURE EARTH_CLAWS EARTH_RAMPAGE EARTH_CONSTRUCT |
| `book_shakudo` | Shakudo Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | SHAKUDO_FX1 SHAKUDO_1 SHAKUDO_2 |
| `book_crystal` | Crystal Magic | 12 / 12 | 3 | - | none (all covered) | CRYSTAL_FX1 CRYSTAL_1 CRYSTAL_2 |
| `book_glass` | Glass Magic | 12 / 12 | 3 | - | Le Château de Verre | GLASS_FX1 GLASS_1 GLASS_2 |
| `book_iron` | Iron Magic | 12 / 12 | 3 | - | none (all covered) | IRON_FX1 IRON_1 IRON_2 |
| `book_copper` | Copper Magic | 12 / 12 | 3 | - | none (all covered) | COPPER_FX1 COPPER_1 COPPER_2 |
| `book_corundum` | Corundum Magic | 13 / 13 | 2 | - | none (all covered) | CORUNDUM_FX1 CORUNDUM_1 CORUNDUM_2 |
| `book_bronze` | Bronze Magic | 14 / 14 | 1 | - | none (all covered) | BRONZE_FX1 BRONZE_1 BRONZE_2 |

#### Batch 5: Light, Dark and Curse

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_bone` | Bone Magic | 7 / 4 | 11 | zenon 5 (+10 to list) | Eternal Fangs; Bone Bullets | BONE_FX1 BONE_1 BONE_2 BONE_JA |
| `book_dark` | Dark Magic | 15 / 5 | 10 | yami 11 (+4 to list) | no repo copy of the wiki page | DARK_SLASH_DIMENSION DARK_SLASH_BLADE DARK_SLASH_AVIDYA DARK_SLASH_WILD DARK_BLACK_HOLE DARK_BLACK_MOON DARK_THRUST DARK_IAI DARKFX_CAST DARKFX_ZONE DARKFX_BURST DARK_1 DARK_2 |
| `book_shadow` | Shadow Magic | 13 / 5 | 10 | nacht 11 (+4 to list) | no repo copy of the wiki page | SHADOW_POOL SHADOW_UNITE SHADOW_PACK_1 |
| `book_ash` | Ash Magic | 9 / 5 | 10 | zora 5 (+10 to list) | no repo copy of the wiki page | ASH_FORMATION ASH_FX1 ASH_1 |
| `book_blood` | Blood Magic | 7 / 5 | 10 | vanica 4 (+11 to list) | Lifeblood Scythe; Curse-Breaking Blood Cocoon; Scaffold of Fresh Blood; Puppet Bloodflow; Jets of Blood | BLOOD_FX1 BLOOD_CRESCENT BLOOD_LANCE BLOOD_SIGIL BLOOD_ARCS BLOOD_DOWNPOUR BLOOD_1 BLOOD_2 |
| `book_light` | Light Magic | 15 / 8 | 7 | karna 6 (+9 to list), lemiel 4 (+11 to list) | Elemental Quintet; Storm of Light Imperial Swords of Conviction; Demon-Dweller Sword: Protecting Light | LIGHT_BLADE LIGHT_FLARE LIGHT_FX1 LIGHT_1 LIGHT_2 LIGHT_JA |
| `book_curse` | Curse Magic | 12 / 8 | 7 | - | none (all covered) | CURSE_FX1 CURSE_WARDING_FX1 CURSE_1 CURSE_2 CURSE_WARDING_1 CURSE_WARDING_2 |
| `book_anti_magic` | Anti-Magic | 8 / 8 | 7 | asta 8 (+7 to list) | no repo copy of the wiki page | ANTI_MAGIC_SLASH |
| `forbidden_magic` | Forbidden Magic (devil book) | 8 / 8 | 7 | - | no repo copy of the wiki page | - |
| `book_curse_warding` | Curse-Warding Magic | 12 / 11 | 4 | - | none (all covered) | CURSE_WARDING_FX1 CURSE_WARDING_1 CURSE_WARDING_2 |
| `book_demon_light` | Demon Light Magic | 12 / 12 | 3 | - | none (all covered) | DEMON_LIGHT_FX1 DEMON_LIGHT_1 DEMON_LIGHT_2 |

#### Batch 6: Space, Time, Game and Mirror

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_compass` | Compass Magic | 4 / 3 | 12 | letoile 3 (+12 to list) | none (all covered) | COMPASS_ARRAY COMPASS_NEEDLE COMPASS_LOCK COMPASS_FX1 COMPASS_1 COMPASS_2 |
| `book_copy` | Copy Magic | 4 / 4 | 11 | - | none (all covered) | IMITATION_FX1 IMITATION_1 IMITATION_2 |
| `book_dice` | Dice Magic | 4 / 4 | 11 | - | none (all covered) | DICE_D6 DICE_D20 DICE_FX1 DICE_1 DICE_2 |
| `book_slash` | Slash Magic | 6 / 4 | 11 | jack 5 (+10 to list) | none (all covered) | SLASH_BLADES SLASH_WAVE SLASH_SCYTHE SLASH_FX1 SLASH_1 SLASH_2 |
| `book_gravity` | Gravity Magic | 9 / 6 | 9 | dante 8 (+7 to list) | Gravity Singularity | GRAVITY_FX1 GRAVITY_WELL GRAVITY_IMPACT GRAVITY_LANES GRAVITY_SINGULARITY GRAVITY_AURA GRAVITY_1 GRAVITY_2 |
| `book_game` | Game Magic | 6 / 6 | 9 | - | no repo copy of the wiki page | GAME_BOARD |
| `book_spatial` | Spatial Magic | 14 / 8 | 7 | finral 4 (+11 to list), langris 4 (+11 to list) | no repo copy of the wiki page | SPATIAL_RIFT SPATIAL_FX1 SPATIAL_1 SPATIAL_2 |
| `book_mirror` | Mirror Magic | 13 / 8 | 7 | gauche 9 (+6 to list) | no repo copy of the wiki page | MIRROR_PANE MIRROR_RAY MIRROR_DOUBLE MIRROR_FRAME MIRROR_ARRAY MIRROR_SHATTER MIRROR_STEP MIRROR_FX1 MIRROR_1 MIRROR_2 |
| `book_time` | Time Magic | 12 / 10 | 5 | julius 6 (+9 to list) | no repo copy of the wiki page | TIME_STASIS TIME_CLOCK TIME_REWIND TIME_ACCEL |
| `book_legion` | Legion Magic | 12 / 12 | 3 | - | none (all covered) | LEGION_FX1 LEGION_BOARD LEGION_1 LEGION_2 |
| `book_key` | Key Magic | 14 / 14 | 1 | - | none (all covered) | KEY_FX1 KEY_1 KEY_2 |
| `book_kotodama` | Kotodama Magic | 16 / 16 | 0 | - | none (all covered) | KOTODAMA_FX1 KOTODAMA_1 KOTODAMA_2 |

#### Batch 7: Nature and Beast

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_world_tree` | World Tree Magic | 7 / 1 | 14 | william 7 (+8 to list) | Dracaena Formation; Demon-Dweller Sword: Spirit Light Tree | WORLD_TREE_FX1 WORLD_TREE_1 WORLD_TREE_2 |
| `book_poison` | Poison Magic | 7 / 4 | 11 | gordon 5 (+10 to list) | no repo copy of the wiki page | POISON_CURTAIN POISON_BREATH POISON_PLANT_FX1 POISON_PLANT_FX2 POISON_PLANT_FX3 POISON_PLANT_1 |
| `book_cotton` | Cotton Magic | 7 / 4 | 11 | charmy 7 (+8 to list) | no repo copy of the wiki page | COTTON_SHEEP |
| `book_plant` | Plant Magic | 14 / 8 | 7 | mimosa 6 (+9 to list) | no repo copy of the wiki page | PLANT_FX1 PLANT_FX2 PLANT_FX3 PLANT_1 |
| `book_cherry_blossom` | Cherry Blossom Magic | 12 / 9 | 6 | kirsch 6 (+9 to list) | none (all covered) | CHERRY_BLOSSOM_FX1 CHERRY_BLOSSOM_1 CHERRY_BLOSSOM_2 |
| `book_briar` | Briar Magic | 13 / 9 | 6 | charlotte 8 (+7 to list) | none (all covered) | BRIAR_FX1 BRIAR_1 BRIAR_2 |
| `book_tree` | Tree Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | TREE_ROOTS TREE_CANOPY TREE_DRAIN TREE_FX1 TREE_FX2 TREE_FX3 TREE_1 |
| `book_vine` | Vine Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | VINE_FX1 VINE_FX2 VINE_FX3 VINE_1 |
| `book_poison_plant` | Poison Plant Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | POISON_PLANT_FX1 POISON_PLANT_FX2 POISON_PLANT_FX3 POISON_PLANT_1 |
| `book_beast` | Beast Magic | 10 / 10 | 5 | - | Holy Hunting; Holy Howling; Bogeyman | BEAST_FX1 BEAST_1 BEAST_2 |
| `book_body` | Body Magic | 11 / 10 | 5 | - | none (all covered) | BODY_FX1 BODY_1 BODY_2 |
| `book_fungus` | Fungus Magic | 12 / 12 | 3 | - | none (all covered) | FUNGUS_FX1 FUNGUS_1 FUNGUS_2 |
| `book_demon_beast` | Demon Beast Magic | 12 / 12 | 3 | - | none (all covered) | DEMON_BEAST_FX1 DEMON_BEAST_1 DEMON_BEAST_2 |

#### Batch 8: Seal, Transform and Body

| Book | Magic | Now (total / basic) | New pages | Character grimoires (pages now, set entries to add) | Wiki spells (repo copy) not yet pages | Reusable effects and props |
|---|---|---|---|---|---|---|
| `book_sealing` | Sealing Magic | 11 / 5 | 10 | secre 7 (+8 to list) | none (all covered) | SEALING_FX1 SEALING_1 SEALING_2 |
| `book_skin` | Skin Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_tongue` | Tongue Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_switching` | Switching Magic | 5 / 5 | 10 | - | no repo copy of the wiki page | - |
| `book_recombination` | Recombination Magic | 10 / 6 | 9 | henry 7 (+8 to list) | no repo copy of the wiki page | RECOMBINATION_FX1 RECOMBINATION_1 RECOMBINATION_2 RECOMBINATION_3 |
| `book_transmutation` | Transmutation Magic | 9 / 7 | 8 | grey 3 (+12 to list) | no repo copy of the wiki page | TRANSMUTATION_FX1 TRANSMUTATION_1 |
| `book_butoh` | Butoh Magic | 13 / 9 | 6 | kiato 6 (+9 to list) | none (all covered) | BUTOH_FX1 BUTOH_DANCE_RIPPLE BUTOH_AFTERIMAGE BUTOH_1 BUTOH_2 |
| `book_modification` | Modification Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | MODIFICATION_FX1 MODIFICATION_1 |
| `book_hair` | Hair Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | HAIR_FX1 HAIR_1 HAIR_2 |
| `book_nail` | Nail Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | NAIL_FX1 NAIL_1 NAIL_2 |
| `book_scale` | Scale Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | SCALE_FX1 SCALE_1 SCALE_2 SCALE_BALANCE |
| `book_spike` | Spike Magic | 10 / 10 | 5 | - | no repo copy of the wiki page | SPIKE_FX1 SPIKE_1 SPIKE_2 |
| `book_chain` | Chain Magic | 12 / 12 | 3 | - | none (all covered) | CHAIN_FX1 CHAIN_1 CHAIN_2 |
| `book_barrier` | Barrier Magic | 12 / 12 | 3 | - | none (all covered) | BARRIER_FX1 BARRIER_1 BARRIER_2 |

<!-- /GENERATED:plan -->

**Per batch, reuse (no heavy new assets):** every book above lists the `VfxShape` and `PropKind` constants that carry its name (the last column); ordinary pages should use the shared shapes (`ElementBook.bolt / volley /
cone / nova / field / line / dash / bind / empower / constructs / wall / decoy / healAllies`, `TensuraShots`), the book's `*_FX1..3` effects and its two props, and the generic `WIKI_MAGIC_CAST / FIELD / BURST`, `EARTHFAM_*`,
`WATERFX_*`, `DARKFX_*`, `LIGHTNINGFX_*`, `FLAMEFX_*` families. A book with "-" (Steel, Sword, Explosion, Magma, Reinforcement, Creation, Illusion, Skin, Switching, Tongue) has no effect of its own: use the element's family
shapes (FLAMEFX for Explosion and Magma, EARTHFAM for Steel, `MAGIC_CIRCLE_*` and `WIKI_MAGIC_*` for the rest). Any new effect has to be added by the batch itself, small, and goes through the perf budgets.

**Worst case per page (stated once for the whole project, rule 11):** a page is one server cast; at most 8-10 hit targets, one zone of at most 100 ticks at 4+ tick steps, at most about 10 effect packets to nearby players,
nothing per tick after it ends. 719 new pages add no per-tick cost at all while nobody casts them; the page list is built once per book and cached (`GrimoireBook.pages()` / `familyAll()` cache).

## 9. Not verified / could not be fetched

* Nothing was run in a game or in Gradle (no Minecraft or NeoForge jars here): the page lists are read from the sources, the Java test was compiled and run against stubs of the JUnit calls only. CI is the real compile.
* The wiki was not fetched (no push possible from here, and `docs/attributes/*.md` say the wiki host was unreachable from earlier builder machines too). The only wiki data used is the repo's own copies in `.github/wiki/`.
  Source for every wiki spell name in the tables: the English wiki (via that workflow); the Japanese fallback was not needed or used in this phase.
* Tensura's default mastery per cast and its maximum mastery, and Tensura's own mode-picking screen, were not read.
* The 106 books count `forbidden_magic` (the devil's book, no universal pages) as a grimoire; if the owner does not mean it, subtract 7 from both totals.
* Hidden-by-engine numbers assume the defaults of `magic.characterSpellSets` and `magic.basicBookHidesSignatures` (both true).

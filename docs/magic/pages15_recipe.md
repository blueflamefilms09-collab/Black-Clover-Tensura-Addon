# "15 spells for every grimoire": the recipe for the batch builders

Owner: "I want every grimoire to have 15 spells." Missing spells are made **canon first, then original** (every original one is marked as original in the docs). The audit is `docs/magic/page_counts.md`
(sections 5 to 8 are the background); this file is the step-0 infrastructure and the exact recipe the eight batch builders follow. Nothing in step 0 adds a page: every `Batch*.java` is empty.

## 1. What step 0 built (all of it is behind `magic.pagesFifteen`)

| Piece | Where | What it does |
|---|---|---|
| Hook | `book/GrimoireBook.familyAll()` | `Pages15.append(path, BasicPages.append(path, CharacterPagesRest.append(path, f)))`: the new pages are the LAST family pages of a book, so no mode number, unlock bit, mastery or save moves (rule 2). |
| Setting | `charset/CharacterSetConfig` (file `nusmp-character-spells-server.toml`, section `[magic]`) | `pagesFifteen`, default **true**, SERVER config. `false` returns every list unchanged (the books exactly as before). |
| Dispatcher | `book/pages15/Pages15.java` + `Pages15Plan.java` | Asks only the batch that owns a book (the book -> batch table equals the plan in `page_counts.md` section 8; a test compares the two). Catches a batch that throws (its books keep their old pages), skips an id the book already has, never goes past 64 family pages. |
| Batches | `book/pages15/Batch1.java` ... `Batch8.java` | Empty. **Each builder edits only its own `BatchN.java`.** |
| Kit | `book/pages15/Pages15Kit.java` | Page factories by tier, the standard casts with the tier's damage, and a bounded `Ctx` for custom spells. Section 3. |
| Rules | `book/pages15/Pages15Rules.java` | Pure: ids, names, the four tiers (cost, cooldown bounds, damage, targets), one signature per book, the limits, the canon / original mark. Used by the unit tests and by the self-test. |
| Self-test | `book/pages15/Pages15SelfTests.java`, key **`magic.pages15`** | ONE shared in-game test for every page of every batch (section 6). |
| 64-bit unlock mask | `book/UnlockMask.java` (pure), `book/PageBits.java` (tags) | Modes 0-31 stay in the item tag `Unlocked`, modes 32-63 are in `Unlocked2` (an old item has none = 0, so every save loads unchanged). `GateTold` has the same second tag. |
| Slots | `multiverse/PageSlots.java` (pure), `MasteryPages.slots` | Defaults `slotsCeiling` 12 -> **15**, `slotsCap` 16 -> **19**; the ceiling is never below the pages the grimoire can show (`max(configured, visible)`), so an old config file (12 / 16) still lets a 15-page book open every page at full mastery. Pacing is unchanged (floor 6, contract +4). |
| Budget | 7 test assertions + `docs/attribute_authoring_guide.md` | "At most 24 family pages" became "a grimoire SHOWS at most 32; a book holds at most 64". |

The Fire book (24 family pages now, 18 hidden from the basic grimoire) ends with 24 + 9 = **33** family pages: that is why the second mask int exists. Its basic grimoire shows 6 + 9 = 15.

## 2. Files a batch builder touches (and nothing else)

1. `src/main/java/com/newuniverse/nusmp/book/pages15/BatchN.java`: the pages (one `register("book_x", List.of(...))` per book).
2. `tools/lang/pages15_batchN.json`: flat `{ "nusmp.page.<book registry path>.<page id>": "one or two sentences" }` (what it does and what scales with EP; the existing fragments such as `tools/lang/basic_spells.json` are the model).
3. `src/test/resources/armour_policy/pages15_batchN_new_pages.json`: `{ "FLAME": ["page_id", ...], "STAR": [...] }`, the ids you appended, grouped by `MagicType` name (the same key as `armour_policy.json` -> `magics.<MAGIC>`). `ArmourPolicyTest` reads every `pages15_batch*_new_pages.json` IN ADDITION to the shared `new_pages`, so eight builders never edit one file.
4. `docs/magic/pages15_batchN.md`: one table row per page (section 5) plus the research sources and the worst case.

Do NOT touch: `armour_policy.json`, `en_us.json` (the coordinator runs `python3 -B tools/lang/merge_pages15.py`), the smoke list in `.github`, the golden file `page_counts.golden.txt` (the coordinator refreshes it with `python3 -B tools/page_counts.py --golden`; the script already reads the `Batch*` classes), `BasicPages`, `CharacterPages*` (their tests pin their lists; **new pages never go there**), version, changelog, README, LIBRARIES.
Character spell sets (`character_spells/*.json`): not part of this step. Listing a new page in a character's set is a separate decision for the coordinator (those files are per character, so they are disjoint per batch if it is allowed); without an entry a character's grimoire simply does not show the page.
Icons: grimoire icons are per BOOK (`textures/skill/grimoire/<magic>.png`), not per page. A page without art shows its book's magic icon; **no per-page art is needed or wanted**.

## 3. How to write a page

```java
// in BatchN.java, inside the class, after PAGES / register(...)
private static final List<BookPage> EXPLOSION = register("book_explosion", List.of(
        Pages15Kit.starter("ember_burst", "Ember Burst",
                Pages15Kit.nova(Tier.STARTER, 3.5, false, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(3))).withAnim("slam"),
        Pages15Kit.mid("cinder_lance", "Cinder Lance",
                Pages15Kit.bolt(Tier.MID, 3.0, VfxShape.FLAME_TRAIL, VfxShape.FLAME_EXPLOSION, Pages15Kit.knock(0.6))).withCooldown(200).withAnim("thrust"),
        Pages15Kit.zone("ash_field", "Ash Field",
                Pages15Kit.field(Tier.ZONE, 5, 80, 10, true, VfxShape.FLAME_EXPLOSION, Pages15Kit.ignite(2))).withCooldown(450)));
```

(`Tier` is `Pages15Rules.Tier`; import it. The id literal must be written out in the call: the armour-policy test and the page counter read the sources for `starter(`, `mid(`, `zone(`, `signature(` followed by a quoted id.)

**Ids** are lower snake_case readable spell names (`cinder_lance`, never `fire_x03` or `spell_7`), 3 to 48 characters, unique across ALL books (also against every older page), and they are **save keys: never renamed, never reordered** once released. **Names** are what the player reads (3 to 64 characters, no stray spaces). Add pages only at the END of your list for a book.

### Tiers (the pacing numbers, `page_counts.md` 4.3 and 4.5; the kit takes them from `Pages15Rules.Tier`)

| Tier | Factory | Cost | Default cooldown (bounds) | Raw hit / zone pulse | Max targets | Opens at |
|---|---|---|---|---|---|---|
| starter | `Pages15Kit.starter` | 8 % (floor 150) | 80 ticks (80..400) | 7 / 2 | 4 | 23 % of the mastery bar or its place |
| mid | `Pages15Kit.mid` | 12 % (floor 300) | 160 (160..700) | 12 / 3 | 6 | 34 % or its place |
| zone | `Pages15Kit.zone` | 20 % (floor 600) | 300 (300..1400) | 16 / 5 | 8 | 57 %, needs Intermediate rank |
| signature | `Pages15Kit.signature` | 35 % (floor 1200) | 600 (600..3000) | 26 / 7 | 10 | the full bar, needs Senior rank |

A page at place m of n needs `max(m/n, cost%/35)` of the bar, and the new pages are the last ones, so most of them open by their place anyway: keep **most pages starter / mid / zone and at most ONE signature per book** (a test fails on two). Suggested mix for n new pages: one signature if n >= 8, about a quarter zone, about 40 % mid, the rest starter, written in that order (cheap first).
`withCooldown(ticks)` must stay inside the tier's bounds; `withAnim("thrust" | "sweep" | "slam" | "up" | "out")` picks the body animation (default by tier). Damage ONLY through the kit or `GrimoireBook.hurt` (the balance law scales it like every other page of the book).

### Standard casts (`Pages15Kit`): the tier fixes the damage, you pick the picture

`bolt(tier, aoe, trail, burst, rider)`, `volley(tier, n, homing, trail, rider)`, `cone(tier, range, width, shape, rider)`, `nova(tier, radius, atAim, shape, rider)`, `field(tier, radius, ticks, interval, atAim, shape, rider)`,
`line(tier, length, width, shape, rider)`, `dash(tier, hits, dist, behind, shape, rider)`, `bind(tier, range, freeze, shape, rider)`, `empower(tier, ticks, thorns, shape, effects...)`, `shot(tier, TensuraShots.Shot, speed, knock, burnTicks)`, `healAllies(radius, fraction)`.
Riders: `NONE, effect(() -> new MobEffectInstance(...)), ignite(s), knock(k), lift(y), leech(hp), strip(), all(...)`. Every number is clamped inside (radius <= 8, range <= 24, a field <= 100 ticks in steps of >= 4 ticks, at most 2 kit zones per player at once, ...).
Pictures: the book's own `VfxShape` / `PropKind` constants listed in `page_counts.md` section 8 (column "Reusable effects and props"); a book with "-" uses its element family (`FLAMEFX_*`, `EARTHFAM_*`, `WATERFX_*`, `DARKFX_*`, `LIGHTNINGFX_*`, `WIKI_MAGIC_*`, `MAGIC_CIRCLE_*`). A new effect is allowed only if it is small and goes through the perf budgets; say so in your doc.

### Custom casts (`Pages15Kit.cast(tier, ctx -> { ... return true; })`)

For a spell the shapes cannot express. Everything a handler may do is a method of `Ctx`, and each one is bounded:

* look: `size()`, `aim(range)`, `target(range)`, `foes(centre, radius)`, `foesAlong(a, b, width)`; the lists never hold more than the tier's targets and never the caster, allies or spectators;
* hurt: `hit(target)` (one standard hit of the tier) and `hit(target, raw)` (clamped to 1.5 standard hits), through the book's balance law;
* draw: `circle(power)`, `fx(shape, from, to, ticks, power)`, `impact(at, power)`: at most 10 packets per cast in all, sent to nearby players only;
* time: `zone(ticks, interval, step)` (<= 100 ticks, step >= 4, at most 2 per player) and `later(delay, run)` (<= 100 ticks); both do nothing for a caster who has died or left;
* `fail("reason")` returns false (no cooldown, nothing spent).

A handler **may not**: spawn entities (except through an existing helper that is already capped, e.g. `LightBoneJaKit`, `SongRebuildKit`), place blocks (except `SpellRuntime.tempBlock`, short-lived), load chunks, scan a world beyond the spell's own box, keep state in a static field that is not keyed by player UUID and cleaned on logout/death/dimension change, call a client class, or damage anything except through `hit` / `GrimoireBook.hurt`. Return false when nothing happened.

## 4. Budgets (rules 9 and 11, the project-wide worst case of `page_counts.md` section 8)

One page is one server cast: at most the tier's targets hit per call (4 / 6 / 8 / 10), one zone of at most 100 ticks at 4+ tick steps, at most about 10 effect packets to nearby players, nothing per tick after it ends. 719 new pages add **no** per-tick cost while nobody casts them; the lists are built once per book and cached by `GrimoireBook`. Client draw cost is that of the shapes you reuse (they are already budgeted by `perf`); do not add per-frame work. State the worst case of any page that goes beyond the table in your batch doc, and why it is still bounded. Cooldowns of a tier are the floor of the cost control: do not set a cooldown below the tier's minimum.

## 5. The mark of every page: `canon: <source>` or `original`

Every page has one row in `docs/magic/pages15_batchN.md`:

```
| Book | Id | Name | Tier | Origin | What it does | Reuses | Worst case |
| `book_explosion` | `ember_burst` | Ember Burst | starter | canon: en-wiki: Explosion Magic (Black Clover wiki) | ... | nova + FLAMEFX | 4 targets, 1 packet |
| `book_explosion` | `cinder_lance` | Cinder Lance | mid | original | ... | bolt | 6 targets |
```

Columns 1 to 5 are read by `Pages15SourceScanTest` (the id and name must equal the code, the tier must equal the factory you used, the origin must be exactly `original` or `canon: <source>` with a real source); the rest is free text. The source names the page it came from: `en-wiki: <title>`, `ja-wiki: <title>`, or `manga ch.<n>` / `anime ep.<n>`. **Research order (rule 10): the English wiki first, the Japanese wiki as the fallback.** Fandom pages are not fetchable with a plain fetch: use `WebSearch` summaries; the repo holds copies of the English attribute pages in `.github/wiki/*.txt` (143 files; the attribute page has a `-- spell pages:` line) for 44 books; each character spell set under `character_spells/` names spells that were never built in its `sources` (`en_wiki` / `ja_search`). Canon first; when a book has no more canon, make original pages in the book's own naming and mark them `original`. In the doc say which source each entry came from and list what could not be fetched at all.

## 6. Tests: what runs, and what every page must satisfy

* **Unit tests (offline, pure)**: `Pages15RulesTest` (rules and the plan against `page_counts.md`), `UnlockMaskTest` (64-bit mask, slots, discovery, avatar tier), `Pages15SourceScanTest` (the hook, the settings, the masks; and for every page in your `BatchN.java`: a unique readable id, a tier inside its cooldown bounds, at most one signature per book, a lang text in your fragment of at least 30 characters, a doc row with a valid origin, the id in your `pages15_batchN_new_pages.json`, nothing in your files that is not a page of yours), `ArmourPolicyTest` (every new id is listed; the listed ids are real), `PageCountsTest` (counts only grow), `BasicGrimoiresTest`, `CharacterSet*Test`.
* **In-game self-test `magic.pages15`** (one shared test, runs in the CI server smoke): for every book of the plan, every page `Pages15` appended is a real family page placed after all older pages in the written order, has a handler (cast), a valid name, a lang text in `en_us.json` (so the coordinator must have merged the fragments), the tier's cost, a cooldown inside the tier's bounds; no id is used twice anywhere or repeats an older page; a book that got pages shows **at least 15 pages in its basic grimoire** and no grimoire shows more than 32; the 64-bit mask keeps mode 33 apart from mode 1; the slot rule opens all pages of a 15-page book. The result line lists the pages and books per batch (`b1 .. b8`). **The coordinator adds `magic.pages15` to `SMOKE_EXPECT` in `.github/workflows/smoke.yml`**; a builder does not touch `.github`.
* A batch that claims a book (any page in its `register("book_x", ...)`) must bring that book's BASIC grimoire to 15 shown pages: the number in the plan ("New pages") already counts the pages the basic grimoire hides.

Run (offline): `WT=$PWD KEY=x bash <scratchpad>/msf_jt.sh com.newuniverse.nusmp.book.pages15.Pages15SourceScanTest ...`; type-check with `tc_wt.sh`. Nothing can be played here: say in your report what was not seen in a game.

## 7. Done means

`BatchN.java` compiles against the stubs, the four files of section 2 are filled in, the unit tests above pass, `python3 -B tools/page_counts.py --ids <book>` shows your pages last and `--golden` shows the new totals (the coordinator commits the golden), and your doc lists NEW (every page; nothing is replaced), the sources, the worst case, and the honest caveats.

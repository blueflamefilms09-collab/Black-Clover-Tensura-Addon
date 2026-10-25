# Star Magic for everybody: the new Star effects for every Star user, not just Spade

Owner request: "The new star VFX should be for all star magic users, not just Spade." Built on the 3.7.0 code (`docs/vfx_remake/star_ingame.md` is the base).

**Never run in a game.** Everything below was type-checked against the API stubs, covered by pure tests and source scans, and added to the CI server self-test; nobody has played it.

## 1. The cause (found end to end)

The look was never limited to Spade. What was limited is **which pages a mage can open and cast**, and the seven StarV2 shapes are only ever sent by those pages.

| Layer checked | Finding |
|---|---|
| Client layer `StarMagicLayer`, `StarPropPainter`, `VfxManager.spawn`, `StarV2Config` | No owner, player, kingdom or cover is read anywhere. A packet that arrives is drawn for every viewer. **Not the cause.** |
| Effect packet `StarFx` / `VfxSpawn` / `PerfBudgets` | One packet type, sent to everybody within 96 blocks, budgeted per tick; nothing about the caster's cover. **Not the cause.** |
| Character sets (`charset/`) | No shipped character has a `book_star` set (a set hides the pages it does not list, only in its own book), and the basic grimoire hides only pages some set marks exclusive. The Star pages are listed for every character and for nobody. **Not the cause** (now pinned by a test, section 5). |
| Book chosen / page list per cover | The Star book is `book_star` for `MagicType.STAR`, whatever the cover; the seven new pages are appended to it for every grimoire by `Pages15`. **Not the cause.** |
| Page block and cast gate: `StarPages.block`, `StarCasts.gate`, the Flagellum switch, all through `StarGame.isHeir` | **The cause.** Six of the seven new pages (Quartile Scutum, Quartile Hasta, Black Neverland, the three Quintile pages) did not open and did not cast without the Grinberryall heir mark, and the Quartile Flagellum threw its new whip only for the heir (everybody else got the old cone). The heir mark = a **Spade** cover + the Spade Kingdom Raid cleared + the signature mastery (`StarGame.isHeir`, the one place that reads `Kingdom.SPADE`). |

So a Star mage of any other kingdom, or with the basic grimoire, only ever got **Star Seed** (`STAR_NODE`) and **Conjunction** (`STAR_CONJ`): 2 of the 7 shapes, never the links, the lance, the whip, the pane or the dome. A Spade mage saw them only after clearing the Raid and filling the mastery bar.

## 2. What changed

* New pure class `star/StarAccess` and server setting **`star.vfxForAll`** (`nusmp-star-server.toml`, default **true**).
* With it on, the pages that were heir-only open for **every** Star grimoire by the ordinary mastery order (rank, race and mastery rules of the book are unchanged), whatever the kingdom cover, character or none, basic grimoire included: **Quartile Scutum, Quartile Hasta, the Quartile Flagellum whip, Quintile: Atrum Scutum / Hasta / Flagellum.**
* **Stays heir-only (whatever the setting): Black Neverland**, with its gate text and its canon gate (Sylph bond + anti-magic source, `star.neverlandCanonGate`). So the dome (`STAR_DOME`) is still the heir's picture; see the decisions.
* The heir keeps what only the heir had besides the pages: the 8th star (`maxPerHeir`), the 1.15 Hasta damage (the old code passed `true` because only heirs got there; it now asks `StarGame.isHeir`, so a non-heir gets the plain number), the halved chance to break to Anti-Magic.
* `star.vfxForAll = false` restores **exactly the 3.7.0 behaviour**: the six pages wait for the heir mark again (page block text and cast refusal), the Flagellum is the cone for a non-heir, the Hasta bonus applies to the (only) casters who can cast it. Takes effect at once for casts and for opening pages (the page lists are the same on client and server either way, so no mode number moves).
* Page text: the five shared pages no longer say "Needs the Grinberryall heir mark". `tools/lang/star_all.json` holds the five replacement texts under the existing keys. **The coordinator must run `python3 -B tools/lang/merge_pages15.py`**: it now treats `star_all.json` as a REPLACE fragment (the old text of those keys in `en_us.json` is replaced in place, on its own line; `star_magic.json` stays as it was; `--check` reports "replace pending" until it is merged). Tested on a copy of `en_us.json`: five lines changed, nothing else, idempotent. `en_us.json` itself was not touched.

## 3. NEW / REPLACED / KEPT (rule 5)

| | What |
|---|---|
| **NEW** | `star/StarAccess` (pure access rule), setting `star.vfxForAll`, `star/StarAccessSelfTests` (key `magic.star_nodes_all`), `StarAccessTest`, `StarAccessSourceScanTest`, `tools/lang/star_all.json`, this doc, a preview copy |
| **REPLACED** | the heir gate of the six shared pages (page block, cast gate, Flagellum whip switch) is now "heir **unless** `star.vfxForAll`"; the Hasta heir bonus now asks the real heir mark; the five page texts (via the replace fragment); two pinned strings of `StarSourceScanTest` (they pinned the old gate text); `merge_pages15.py` (replace fragment, `--check` reports it) |
| **KEPT** | every page, id, order, mode number and unlock bit; the heir mark itself (`StarGame.isHeir`, `StarRules.isHeir`, `star.heirMastery`); Black Neverland and its gate and text; the heir's 8th star, Hasta bonus and halved dispel chance; the old cone, dash and every old Star page; `VfxShape.STAR_MAGIC`; the client layer, painter, textures and budgets (untouched); `star.nodes`, `star.dualMode`; the Time Magic VFX |

## 4. Server setting

| Key | Default | |
|---|---|---|
| `star.vfxForAll` | **true** | true: Scutum, Hasta, Flagellum whip and the three Quintile pages open for every Star grimoire. false: the 3.7.0 behaviour (they need the Grinberryall heir mark). A SERVER setting (it changes what the server lets a mage do). |

## 5. Tests

* `StarAccessTest` (pure): a table over every kingdom + no cover x mastery 0 / 0.5 / 0.99 / 1.0 x Raid cleared or not x both setting values, for all eight Star pages; the shared pages open for all with the setting on; Black Neverland is the Spade + Raid + full mastery heir's only, with either setting; with the setting off the table equals `StarRules.needsHeir` for every page; no shipped character set is a Star set and none of the shipped characters (plus the basic grimoire, plus "nobody") hides or decides the opening of any Star page; the replacement texts no longer ask for the heir mark and keep their "unverified canon" marks, Black Neverland's text keeps its gate.
* `StarAccessSourceScanTest`: the effect path (`StarFx`, `StarProps`, `StarV2`, `VfxSpawn`, `VfxPayload`, `VfxShape`, `StarMagicLayer`, `StarV2Config`, `VfxManager`, `StarPropPainter`, `PropPainterRegistry`, `PerfBudgets`) names no kingdom, cover, heir mark, `StarAccess` or setting (a deliberately planted `Kingdom.SPADE` in `StarMagicLayer` made it fail, then it was removed); in the whole mod only `StarGame.isHeir` reads the Spade kingdom and only the casts, the dispel and the page block ask for the heir mark; every gate goes through `StarAccess` and `star.vfxForAll`; the heir perks are still the heir's.
* Self-test `magic.star_nodes_all` (CI server smoke, covered by the existing smoke prefix `magic.star_nodes`, **no new prefix needed**, `.github` untouched): a stand-in with no grimoire, no cover, no character and no Raid (a plain Star user, `isHeir` false): the pages are not hidden in the basic grimoire; with the setting on, no page block for the shared pages; Star Seed, Quartile Scutum, Quartile Hasta, the Flagellum whip and Quintile: Atrum Scutum (five real stars) **cast and produce effect packets** (counted through the real `PerfBudgets.EFFECT_PACKETS` request counter, at least 4 / 4 / 4 / 5 and never more than the cast's ten plus the circle); Black Neverland refuses and sends none; with the setting off every heir page refuses with no packet. It also re-checks the pure table and that the casts leave no star or pane behind.
* Existing tests re-run, all pass: all star tests (rules, nodes, math, pages, dispel, neverland, source scan), `StarV2Test`, `StarV2AssetTest`, `StarV2SourceScanTest`, `PerfSourceScanTest`, `PerfBudgetTest`, `ArmourPolicyTest`, `PageCountsTest` (golden unchanged: no page table changed), `UnlockMaskTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, the Pages15 rule / source-scan / batch 1 to 8 tests, the character-set tests. Result 56 + 137 + 51 + 36 passed, 0 failed (the offline shim; tests needing game classes were not part of it).

## 6. Worst-case cost (rule 11)

**Per cast: unchanged.** The casts, their caps and their budgets were not touched: a cast sends at most 10 effect packets (`StarFx.Budget`), to players within 96 blocks, through the per-tick `PerfBudgets.EFFECT_PACKETS` (160); damage only through `Ctx.hit`; one pane per mage; 6 stars per mage (8 for the heir), 128 on the server; Hasta at most 7 packets and 6 or 8 targets; the whip 15 segment queries over 12 ticks; delayed parts at most 100 ticks. The only new server work per cast is one `StarGame.isHeir` call (one skill lookup and one story read) on Hasta (for the damage bonus) and, with the setting off, on the gate: the same single call the old gate made.

**What does change is who can use the pages**, so the number of possible users grows from "Spade heirs" to "every Star mage at mid or zone mastery". The bounds hold regardless of how many:

* packets: a mage is limited by the page cooldowns (Scutum 300, Hasta 160, Flagellum 90 / 380, Quintile 360 to 400 ticks), so one mage sends at most about 7 packets per 8 seconds from the heaviest page (Hasta), about 0.9 packets per second; the whole server is capped at 160 effect packets per tick by the existing budget, whatever the player count;
* entities: stars at most 128 on the server, panes at most 8 on the server (one per mage; a ninth mage's pane evicts the oldest), 1 pane query per live pane per tick (at most 8), nothing without a pane;
* client: unchanged quad caps (528 quads for 20 mages with 6 stars each and 8 panes in view, worst per effect 7 to 12 quads, at most 48 stars drawn in full); the more users now actually cast, the more effects exist, but each is capped and the layer budget (64 active, quality-scaled) holds.

## 7. Preview

`docs/previews/star_vfx_for_all/star_for_all_plain_star_user.png` is the real layer's sheet (`python3 -B tools/vfx_preview/star_ingame.py`, run again for this change: the output is byte-identical to `docs/previews/vfx_remake/star/star_ingame.png`). That is the honest point of it: the layer takes no owner, so a plain (non-Spade) Star user's casts draw exactly this: rows Star Seed, Quartile Scutum links + pane, pane hit, pane break, Hasta lance, the whip (the last row, Black Neverland's dome, stays the heir's). It is a headless software render with a mirror of the prop painter, no bloom, the palette a placeholder; there is no way to show "a non-Spade player" in the harness other than this.

## 8. Honest caveats

* Nothing was seen or played in a game; the self-test `magic.star_nodes_all` has not run (no game here). It casts through `BookPage.cast()` directly with a `SelfTests.standIn` player: it covers the server path and the packet counts, not the cooldown, the cost or the unlock bit, and the stand-in has no grimoire (the "non-Spade" case is "no cover at all", a Clover or Heart grimoire is covered by the pure table, not by a live instance).
* The Hasta damage of a non-heir is the plain number (the 1.15 bonus is the heir's); the first-pass tier numbers are unchanged and were never playtested for a wider audience.
* **A merged (fused) grimoire** (`star.dualMode = FUSION`, the default: a Star + other attribute written as one book) was not examined or changed here: `StarGame.starGrimoire` only finds the Star book itself, so such a mage's Star pages (and the heir mark) depend on how the merged book lists them, as before.
* `merge_pages15.py --check` reports "replace pending" until the coordinator runs the merge; until then the in-game text of the five pages still says the heir mark is needed (the casts do not).
* The lang fragment tests of other fragments are untouched. No client option was added (the look options `vfx.element.star`, `starReducedFlashes` stay as they were).

## 9. Decisions for the owner

1. **Black Neverland** stays heir-only (it is Yuno's Union Magic and was decided that way before), so `STAR_DOME` is still only the heir's picture. If the dome's look should be for every Star mage too, the options are a lighter non-heir dome (no anti-magic, no Sylph bond) as a new page, or dropping the heir mark from `ALWAYS_HEIR` (then the Sylph bond and the anti-magic source still gate it).
2. Whether non-heirs should also get the 8th star; today it is the heir's.
3. The five Quintile / Quartile pages are now the strongest pages of the Star book for everybody: the Quintile pages are "unverified canon" and could be gated again by mastery tier if that is too much.

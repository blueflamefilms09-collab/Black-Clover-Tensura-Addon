# Rebirth: the Sigil of Rebirth and the Sigil of the Second Book

Owner request: "add a way for a player to be reborn, in a sense, to reset their grimoire magic types and/or get 2 grimoires like Yuno from Black Clover."
Code: package `com.newuniverse.nusmp.rebirth`. Settings: `nusmp-rebirth-server.toml` (a SERVER config). Self-test: `rebirth.rules`.

## What each sigil does (plain words)

Both are craftable items, one per stack. Using one is a two-click rite, decided only on the server:

1. First right-click (in the air): the sigil only warns in chat. The Rebirth one says "Your grimoire(s), their pages and magic types will be erased. Sneak + right-click again within 15 seconds to be reborn."
2. Second right-click, **sneaking**, inside the window: the checks run again, ONE sigil is consumed (not in creative), and the rite happens. A click that is not a sneak, a click after the window, or a click of the other sigil only warns again.

The window is kept per player UUID in one small map. It is cleared on logout, death, dimension change and server stop.

### Sigil of Rebirth (reset)
Needs: the setting on, the cooldown over, a grimoire. Then it erases every grimoire skill of the player (their pages, fusion and mastery live in the skill's data), the player's own grimoire items (inventory and the Grimoire Slot), and the Signature (its name is freed). The ender chest is not touched. The player's persistent mark becomes `nusmp_rebirth=RESET`, they get a fresh Grimoire Summons. Right-clicking it carries them to the Tower Wizard's library, where the questionnaire runs and a new grimoire is granted by the normal Tower path (kingdom, blood and social class are redrawn by it). When the new grimoire arrives the mark becomes `DONE`.

Kept: race, EP, every skill that is not a grimoire's, mastery of non-grimoire things. The erasure is the same one the admin command `/anime_multiverse grimoire reset <player>` does (forget the grimoire skills, mark "not rolled"); that part was extracted to `GrimoireErasure.skills(p)` and the command calls it, behaving exactly as before. The sigil adds the item and Signature clean-up on top.

### Sigil of the Second Book (the Yuno rite)
Needs: the setting on, the cooldown over, the Tower Wizard setting on, exactly one grimoire of leaf tier 4 or more (setting `secondBookMinLeaves`, default 4), and the stored resolve score `nusmp_resolve`, **if the questionnaire wrote one**, at least `secondBookMinResolve` (default 3, 0 = check off). A missing score counts as fine. It consumes one sigil, sets `nusmp_rebirth=SECOND` and gives a Grimoire Summons. In the library the wizard questions the player although they carry a grimoire. The first book's magic is taken out of the questionnaire pool, so the result is a different attribute (if a fused blend still equals the first magic, one of its two parents is used). The second book is granted with `GrimoireAcceptance.grantExtra` with the first book's cover and **nothing of the first book is removed**; kingdom, blood and social class are not redrawn.

### Two books on a fusion (setting, default off)
`twoBooksOnFusion`: in the ordinary questionnaire, a result of four leaves or more whose top two natures are both strong (`Result.fused()`) and that has no devil cover gets TWO real grimoires (the first parent via `grantExact`, the second via `grantExtra`) instead of one fused grimoire, with the existing message "Two attributes answered you: A and B. You carry two grimoires." Off by default, so nothing changes. A devil (five-leaf) cover stays a single book.

## Recipes (vanilla ingredients only, shapeless)
- Sigil of Rebirth: nether star, totem of undying, enchanted golden apple, 4 amethyst shards.
- Sigil of the Second Book: nether star, 2 emerald blocks, book, gold block, 4 azalea leaves (the four-leaf clover).
No advancement is attached.

## Settings (`nusmp-rebirth-server.toml`, section `[rebirth]`)
| key | default | meaning |
|---|---|---|
| `rebirth` | true | master switch of both rites (the admin command is not affected) |
| `cooldownDays` | 7 | game days (24000 ticks) between two rites of either kind, per player (persistent key `nusmp_rebirth_at`); 0 = none |
| `confirmSeconds` | 15 | seconds the confirming click stays valid (5 to 120) |
| `xpLevelCost` | 0 | experience levels a rite costs besides the sigil (0 to 100) |
| `secondBookMinLeaves` | 4 | leaf tier the first grimoire needs for the Second Book (3 to 5) |
| `secondBookMinResolve` | 3 | resolve needed if known (0 = off) |
| `twoBooksOnFusion` | false | the fusion rule above |

Every value is clamped again in `RebirthRules` when read.

## Erased vs kept
Erased by Rebirth: all grimoire skills with pages, fusion and mastery inside them; own grimoire items in inventory and Grimoire Slot; the Signature. Kept: ender chest, other players' grimoires, race, EP, every non-grimoire skill and its mastery, the wizard's "summoned" flag, the cooldown record.
Not erased (grimoire-adjacent data other systems own): spirit bond, forbidden-magic / devil contract state, and any separate page skill granted by the admin `/grimoire page` command outside a grimoire skill.

## New, replaced, kept
- NEW: package `rebirth` (rules, config, state, erasure, service, item, hooks, boot, self-test), the two items with 16x16 icons (`tools/rebirth/gen_sigil_icons.py`), models, two recipes, lang fragment `tools/lang/rebirth.json`, franchise-map entries (Black Clover), creative-tab entries (grimoire tab), smoke prefix `rebirth`, tests `RebirthRulesTest`, `RebirthSourceScanTest`, `RebirthFilesTest`.
- REPLACED: nothing. The admin command's two lines moved into `GrimoireErasure.skills` (same effect).
- KEPT: everything else. Edits to shared files are small and listed here: `WizardSystem` (hook lines in `talk`, `answer`, `arrive`, and the Star-only `grantExtra(...STAR)` line now guarded by "STAR", with an `else if` for the other second book; the "Two attributes answered you" and "Your magic" lines take the second magic's name instead of the literal "Star Magic", the same text for Star), `Signature` (one appended method `clear`), `GrimoireCommand` (calls the extracted erasure), `NUSMP` (one init line), `NUCreativeTab` (one line), franchise map and overrides.

## Cost (server work, bound)
One-off per use, no per-tick scan, no `TickEvent` anywhere in the package. One use: a few NBT reads, one skill lookup per grimoire skill id, one inventory sweep (at most 41 slots), one ring of at most 24 particles through `Fx.ring` (already under the perf budgets; sent to nearby players only) and one sound. The wizard hooks add the same small lookup once per click on the wizard and once per answered questionnaire. State: one `HashMap<UUID, warning>`, at most one entry per online player, cleared as above. Packets: none new. Client: none (the item has no client code; only a 16x16 icon).

## Tests and what was run
Pure `RebirthRulesTest` (checks, cooldown, window, marks, second-book magic, fusion rule, the map), `RebirthSourceScanTest` (no client class, one UUID-keyed map cleared on logout/death/dimension/stop, SERVER config, hooks in place, no tick event), `RebirthFilesTest` (icons, models, lang, recipes, franchise map, creative tab, smoke list), CI self-test `rebirth.rules` (pure rules, the map, both items and both recipes exist in the booted registries). The whole test tree (12589 tests) was run: all pass except `RelicWearDataTest` (`stones/CarrierKit.java`, not touched by this change).

## Not done / not seen in a game
- Nothing here was run in a real game: the sigil clicks, the questionnaire after a rebirth, the second grimoire arriving, the two-books setting and the effect were never seen. Only the pure rules, the source scans, the file tests and a compile were run; the CI smoke test is what runs the self-test in a booted server.
- `nusmp_resolve` is read from the player's persistent data (root, or the wizard's section); the other worker's writer was not available, so its location and 0..6 scale are assumed (the questionnaire's own `leaves` score is 0..6).
- Not models: the icons are 16x16 PNG sprites drawn with PIL (no in-hand 3D model; not weapons, so no Better Combat file and no engravings). The old `Ceremony` (altar) path after a rebirth with the wizard setting off was not exercised.
- The new longer questionnaire of the other worker may change `Questionnaire.result`; the rebirth hooks use only its public pool parameter and `Result` accessors.

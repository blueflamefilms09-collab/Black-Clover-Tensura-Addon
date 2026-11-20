# Weapon page of the Codex (upgrade options and mastery bars)

## What it is and how it is read

One new page, appended last of the stock pages of the Codex (the Tensura-style menu), glyph `Wp`, title "Weapon".

- Shows the weapon the player holds (or the one picked from the chips at the top when more than one is owned): name, grade (from `rankings.json`), level, current form.
- Left column: the upgrade options the weapons already have, one row each: change form, learn a form, feed the gem an item the player carries (hero / vassal weapons); switch form (the Lords' weapons and the Black Sword); Half-Starvation on/off (Black Sword). A ready row is clickable, a locked or sealed row is greyed with its note.
- Right column: mastery bars. The weapon's own (weapon level, active-form mastery, Black Sword tier, Half-Starved mastery), then "Other masteries": the Grimoire's mastery and each Beast Will's mastery, read from the caches the mod already has.
- Nothing is invented: every option and bar maps to a system that already exists. There is no new currency, no new upgrade gear and no new gameplay number.
- Hide it with the client setting `[weaponpage] show = false` in `nusmp-multiverse-client.toml` (looks only; takes effect the next time the Codex opens).

What is not included: embrace-curse, tag requirements for feeding (the Weapon Book still handles those), new upgrade mechanics, a copy of the page in `MultiverseStatusScreen`.

## New / replaced / kept

- NEW: package `weaponpage` (rules, data, net, service, boot, self-tests), `heroweapon/forms/HeroPageView`, `client/weaponpage/*` (client, layout, page), `client/beastwill/BeastWillMasteryView`, `CodexPage.hidden()`, the client setting `WEAPON_PAGE`, `tools/lang/weaponpage.json` + `merge_weaponpage.py` (21 keys in `en_us.json`), tests `WeaponPageRulesTest` and `WeaponPageLayoutTest`.
- REPLACED: nothing.
- KEPT: the Weapon Book and its `FormsNet.Act`, the Lord and Greed HUD packets, `MultiverseStatusScreen` and its mastery bars, all older Codex pages (only `CodexPages.all()` now skips pages whose `hidden()` is true; no old page overrides it).

## Packets and rules

- C2S `weaponpage_act(weapon, action, arg)`: actions OPEN, FORM, LEARN, FEED, HALF. Registered optional (`registrar("1").optional()`).
- S2C `weaponpage_sync(View)`: at most 6 entries, each at most 4 bars and 24 options, strings bounded in the codec.
- Server (`WeaponPageService`): checks the sender is a real `ServerPlayer`, checks the verdict of `WeaponPageRules.check` (weapon id, action, arg format) before any bookkeeping, applies a per-player gap (10 ticks for a click, 20 for an open, with a guard against a world clock set back), builds the page fresh, and performs an action only if that fresh page offers exactly that option as READY (Half-Starvation is the exception: it is a toggle). Ownership, cost and form rules stay in the existing services (`HeroPageView` -> `FormLearning` / `HeroAbsorb` / `HeroBond`, `LordService.switchForm`, `GreedService.switchForm` / `toggleHalf`). The client decides nothing.
- Dedupe: the client sends the checksum of the page it holds with OPEN; the server sends nothing when it is equal, and after an action sends a page only when the checksum changed.

## Worst-case cost (rule 11)

- Server: one page build per accepted OPEN or click (1 hero entry with at most 24 options and 4 bars, up to 4 Lord entries, 1 Greed entry; at most one pass over the 41 inventory slots for feed options). At most one accepted request per 10 ticks per player. Two small map entries per player, dropped at logout and server stop. No tick handler, no world scan, no chunk load, no entity spawn.
- Network: C2S at most 2 to 6 packets per second per player (client throttle 5 ticks, server gap 10), each about 100 bytes. S2C at most one per accepted request and only when changed, bounded to 6 x (4 bars + 24 options), typically under 2 KB. Sent to the requesting player only.
- Client: text is cooked once per sync or open (a few dozen strings). Per frame, about 30 to 60 `drawString` / `fill` calls with scissor; no strings, lists or resource locations built; the layout record is rebuilt only when the area moves and rows are drawn from int accessors. Rows outside the scroll window are skipped.

## Tests and self-tests

- Pure tests (12, all pass): `WeaponPageRulesTest` (packet validation, checksum, bounds, option lookup) and `WeaponPageLayoutTest` (no overlap or overflow for a grid of widths and counts).
- Self-tests (to add to the smoke prefix list): `weaponpage.rules`, `weaponpage.world` (stand-in player: empty page, open sends once, equal checksum gives nothing, second request inside the gap is refused, click on an unowned weapon is refused, bad requests are rejected before bookkeeping, logout drops bookkeeping).

## What was not seen or run

Gradle was unavailable offline: the Minecraft-touching classes were only checked by eye against existing patterns, not compiled; only the pure classes and their 12 tests were compiled and run with javac and JUnit. `PerfSourceScanTest`, `ServerSafetyTest`, the whole test sweep, the self-tests and the page in a game were not run or seen.

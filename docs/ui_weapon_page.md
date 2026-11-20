# Weapon page of the Codex (upgrade options and mastery bars)

## What it is and how it is read

One new page, appended last of the stock pages of the Codex (the Tensura-style menu), glyph `Wp`, title "Weapon".

- Shows the weapon the player holds (or the one picked from the chips at the top when more than one is owned): name, grade (from `rankings.json`), level, current form.
- Left column: the upgrade options the weapons already have, one row each: change form, learn a form, feed the gem an item the player carries (hero / vassal weapons); switch form (the Lords' weapons and the Black Sword); Half-Starvation on/off (Black Sword). A ready row is clickable, a locked or sealed row is greyed with its note.
- Right column: mastery bars. The weapon's own (weapon level, active-form mastery, Black Sword tier, Half-Starved mastery), then "Other masteries": the Grimoire's mastery and each Beast Will's mastery, read from the caches the mod already has.
- Nothing is invented: every option and bar maps to a system that already exists. There is no new currency, no new upgrade gear and no new gameplay number.
- Hide it with the client setting `[weaponpage] show = false` in `nusmp-multiverse-client.toml` (looks only; takes effect the next time the Codex opens).

What is not included: embrace-curse, tag requirements for feeding (the Weapon Book still handles those), new upgrade mechanics, a copy of the page in `MultiverseStatusScreen` (added later, see "Weapon page v2" below).

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


## Weapon page v2: more mastery tracks, and the page inside the old Status screen

What it is: two additions on top of everything above, both looks only and both client settings, default on (`nusmp-multiverse-client.toml`, `[weaponpage]`):

- `moreMastery = true`: "Other masteries" lists every mastery the server really tracks, each as a bar with its level and its progress to the next (value text `14/100 (40%)`; the percent is the progress to the next level, or to completion for a form's mastery). Off: the page is exactly as before (only the weapon's own bars, the Grimoire and the Beast Wills).
- `statusPage = true`: the same page is a tab of the old Multiverse Status screen (`MultiverseStatusScreen`), a "Weapons" button left of the Grimoire Slot button, after the Beast Will tab (the tab buttons: Status, Beast Will, Weapons; nothing reordered). While it is on, the Beast Will tab hides it (they share a row), and its label turns into "Status" to go back. Off: the screen has no Weapon tab.

Where each track comes from (no number is invented; every one is a value the server already holds and already syncs):

| Track | Source | Shown |
| --- | --- | --- |
| Weapon level, active-form mastery, Lord weapon level, Black Sword mastery and tier | the existing `weaponpage_sync` bars | as before (own bars of the open weapon), now with `(percent)` after the value |
| The hero weapon's other learned forms (started, not finished) | the same sync: `HeroPageView` adds one bar per learned form with progress, up to the bar bound (label key `bar.formmore`, shown like the active form's; a client with `moreMastery` off drops them) | in "Mastery" of the hero weapon |
| Every other weapon's own bars | the same sync (all entries are already sent), listed as `Weapon: bar` | "Other masteries", not for the open weapon (it is above) |
| Grimoire mastery | `MultiverseSync` status tag | "Other masteries" (as before) |
| Each Beast Will's mastery | `BeastWillNet` cache | "Other masteries" (as before) |
| Saiyan form mastery | `SaiyanNet.Sync.formMastery` (awakened Saiyan in a form) through `SaiyanClient.formMasteryRow()` | "Other masteries", only while it applies |
| Skills of this mod in training | the player's own Tensura skill instances (mastery below the maximum, not the Grimoire), the four closest to full | "Other masteries" |

Not shown, because the server holds no per-player value for it that is synced: Mana Zone, Heritage and social-class gains (they change a rate, they have no bar), Spirit Lord trust (trust, not mastery), form mastery of the Lord weapons' forms (the Lord weapons have one level track).

Wire change: one bound only. `WeaponPageRules.MAX_BARS` goes from 4 to 8 (a bound, not a new field; the codec and `View.bounded()` read the constant, client and server of one mod version share the constant; an older client would refuse a page with more than 4 bars, but the packet is registered optional and the extra bars exist only for a hero weapon with several started forms). The packet, its sender checks, the dedupe checksum and the rate limit are unchanged.

NEW: `client/weaponpage/MasteryTracks` (pure model: bounds, fractions, selection, skills-in-training pick), `client/multiverse/WeaponStatusPage` (a rectangle, a scroll and the mouse for the shared body), the settings `moreMastery` and `statusPage`, 3 lang keys (`track.fmt`, `other.saiyan`, `status.tab`), `SaiyanClient.formMasteryRow()`, the extra form bars in `HeroPageView`, tests `MasteryTracksTest` (fractions, bounds, no double listing, skills pick, source pins), the two mock-ups.
REPLACED: nothing.
KEPT: the Codex page and its drawing, the hub tab, the Status screen's blocks, Beast Will page and buttons, every packet and rule; `HubWeaponsBody` is only made public (same code, used by the hub and the Status tab: the page is not duplicated), `MAX_BARS` only raised.

Worst-case cost (rule 11):
- Server: no new work. A page build (one per accepted request, at most one per 10 ticks per player) now loops over the hero weapon's forms (a few dozen) once more to add at most 4 more bars (the bound is 8). No tick, no scan, no spawn.
- Network: C2S unchanged (at most 2 to 6 small packets per second per player). S2C unchanged in count (one per accepted request, only when changed, to the requester only); size grows by at most 4 bars of about 60 bytes (about 240 bytes) for the hero entry. The theoretical bound of a whole view is 6 entries x (8 bars + 24 options), under 16 KB; a real page stays under 3 KB. The Status tab sends the same one open request when it is switched on (client throttle 5 ticks, server gap 20).
- Client: cooked once per sync, open or chip click: one pass over the learned skills (usually well under 300) picking four, at most 24 tracks, a few dozen strings. Per frame: the same body as the hub, in a window of 160 px: heading rows and bars outside the scroll window are skipped, so at most about 8 bar rows (about 3 strings or fills each) plus the head card and option rows are drawn; worst case with everything on screen at once: 8 bars + 24 tracks = 32 rows, about 130 draw calls. One scissor and one pose push for the tab. No allocation per frame (the tab's rectangle is built when the screen opens or resizes).

Tests run: `MasteryTracksTest`, `HubWeaponLayoutTest` (now to 8 bars and 24 tracks), `WeaponPageLayoutTest`, `WeaponPageRulesTest` (now with the 8-bar bound), `HubSourceScanTest`, `HubNodesTest`, `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`: all pass. The Minecraft-touching classes were compiled with `javac` against the real jars. Self-test `weaponpage.rules` gained the bar-bound check (no new prefix).

Previews (PIL mock-ups, not game screenshots): `python tools/ui_preview/preview_ui.py docs/previews/weapon_page_v2 --weapon-v2` writes `hub_weapons_v2.png`, `hub_weapons_v2_scrolled.png` (the long "Other masteries" list), `status_weapons.png` and `status_weapons_scrolled.png` (the Status screen with the Weapon tab).

Not seen: the page in a game (the tab placement on a small window, the real text widths, the skill list with real skills, a Saiyan or hero weapon with several forms).

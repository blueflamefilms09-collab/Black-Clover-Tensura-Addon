# Weapon Tracker (`/multiverse weapons`)

Owner request: "add a way for players to track what vassal weapons are taken and etc." A screen that shows, for the 21 hero weapons (4 Legendary, 7 Seven Star, 10 Vassal), which are free, which are taken, which one is yours.

**Nothing here was run in a game.** There is no Gradle or compiler for the whole mod on the build machine; the pure rules and the source-scan tests ran locally, the rest is read by CI (build + server smoke). Section 8 lists what is unchecked.

## 1. New or replacement (rule 5)

| | what |
|---|---|
| NEW | the whole tracker: `heroweapon/tracker/` (`TrackerRules`, `TrackerLimiter`, `WeaponTrackerNet`, `WeaponTrackerService`, `WeaponTrackerBoot`, `WeaponTrackerSelfTests`), `client/heroweapon/WeaponTrackerScreen` + `WeaponTrackerClient`, the command `/multiverse weapons`, the server setting `trackerShowNames`, the key `key.anime_multiverse.hero_tracker`, the lang fragment `tools/lang/weapon_tracker.json`, 3 test classes, 2 self-tests |
| REPLACED | nothing. No tracker, weapon list screen or "who holds what" screen existed. |
| KEPT | `/multiverse hero list` and `/multiverse hero registry list` (the text list for operators) are untouched; the register, the bind gate, `holyUnique` / `vassalUnique`, `maxBonds` and all hero weapon gameplay are read only |
| APPENDED to shared files | `HeroConfig` (one setting + one reader), `HeroCore.init` (one line), `HeroClient.init` (one line), `MultiverseCommands` (one `root.then`), `GuiScaleRegistry` (one screen entry), `GuiMoveRegistry` (two `fixed` entries), `en_us.json` (35 lines at the end) |

## 2. What the server knows, and when a weapon counts as "taken"

* **The register.** `HeroRegistry` (saved data `nusmp_hero_registry.dat` in the Overworld) maps a weapon item id to the uuids that hold it. A bond claims the weapon (`HeroBondEvents.bound`), a release frees it, and every bonded player re-claims their own at login, respawn and dimension change. It records every holder, unique or not.
* **One weapon per player.** A bond holds exactly one weapon; `maxBonds` is 0 (door closed) or 1.
* **Taken** depends on the class, with the same mapping the bind gate (`HeroBondEvents.gate`) uses:

| class | setting | default | effect |
|---|---|---|---|
| Legendary (Shield, Sword, Spear, Bow) | `holyUnique` | on | a second player is refused: another holder means TAKEN |
| Seven Star (7) and Vassal (10) | `vassalUnique` | off | several players may hold the same weapon: another holder means SHARED, the weapon is still takeable |

## 3. The rows (`TrackerRules`, pure, no Minecraft type)

One row per weapon in catalogue order (`HeroKind.values()`), always 21:

| status | meaning |
|---|---|
| FREE | nobody holds it |
| YOURS | the viewer holds it (the register says so, or their bond does) |
| TAKEN | someone else holds it and the class is unique, so the viewer cannot have it |
| SHARED | someone else holds it, the class is not unique, the viewer still can; the row shows the holder count |

A row also has: kind (id, name, class Legendary / Seven Star / Vassal), `holders` (count), `holder` (one name), `online` (is that holder online), `unique` (does this class refuse a second holder). The table carries the viewer's own weapon and the settings (`enabled`, both unique flags, `showNames`, `maxBonds`), so the screen can show "Your bond: Legendary Shield (1/1)" or "Bonds are closed".

* **Names.** Only when the new server setting `trackerShowNames` is on (default **on**). Off: `holder` is empty, the screen says "another player"; status, count and the online flag still show (they say nothing about who). A holder whose name the server cannot resolve (no profile cache entry) also shows as "another player". Names are cut to 24 characters and stripped of control characters. The name lookup is never made when names are hidden, and at most once per weapon (the first online holder is named, else the first).

## 4. Network

| packet | id | direction | size |
|---|---|---|---|
| `WeaponTrackerNet.Request` | `anime_multiverse:weapon_tracker_request` | client to server | 0 bytes of data (a unit codec), nothing to forge |
| `WeaponTrackerNet.Reply` | `anime_multiverse:weapon_tracker_reply` | server to the one player | 4 header bytes + per row: 1 weapon index, 1 flag byte, 1 to 2 holder count, 1 + name length (0 to 24) |

Worst case: 21 rows x (1 + 1 + 2 + 1 + 24) = 609 + 4 = **613 bytes**. A normal reply (free weapons have an empty name, 5 bytes a row) is about 110 to 250 bytes. Both payloads register `optional()` on the mod bus, like Attune and the hero core, so a client without the mod can still join.

**Validation (server).** The handler needs `ctx.player() instanceof ServerPlayer`, a connected player (`connection != null`), and passes the per-player limiter before it reads anything. **Rate limit:** `TrackerLimiter`, one answer per player per 40 ticks (2 s) by the server tick count; the rest are dropped with no reply (a hammering client never extends its own wait). The table holds one entry per player who asked, sweeps old entries when it passes 256, drops a player at logout and everything at server stop.
**Validation (client reads the reply).** Row count at most 21, weapon index and status in range, holder count at most 9999, name at most 24 characters: a corrupt reply is rejected, not drawn.

**No push, no broadcast, no timer.** The task allowed an optional "changed" notice to players who have the screen open; it is **not built**. The screen asks again every 10 seconds while it stays open (and on Refresh / R), which the 2-second limiter already bounds. This means no UUID set of watchers to keep and clean up.

## 5. Worst-case cost (rule 11)

| thing | bound |
|---|---|
| server work per request | 21 register lookups (one hash read each) + at most 21 name lookups (in-memory profile cache, only when names are shown) + a 21-row record; allocation of a few small lists; no world access, no chunk, no scan, no tick handler |
| requests answered | at most 1 per player per 2 s. Open screen: 1 per 10 s per player (auto refresh); a player who spams Refresh or a modified client is capped at 0.5 per s |
| server to client | 1 packet per answered request, at most 613 bytes, to that player only: at most about 307 bytes per second per player in the worst (hammering) case, about 20 to 25 bytes per second with an open screen |
| memory | one `Long` per player who asked, until logout |
| client draw | at most 7 rows (fills + 2 strings + a pill) and 7 chips per frame; no entity, no particle, no texture upload |
| `/multiverse weapons` | goes through the same limiter (a second run inside 2 s says "Wait a moment before asking again.") |

## 6. The screen (`client/heroweapon/WeaponTrackerScreen`)

Anime-theme glass window built with the mod's toolkit (`UiPanel.panel / well / chip / hover`, `ThemedButton`, `UiTheme` colours), drawn in `renderBackground` (never the blurring vanilla background, `ScreenBlurGuardTest`). It decides nothing.

* Header: the title, "Free: N / 21" (green) and "Your bond: <weapon> (1/1)" (gold) or "none (0/1)" or "Bonds are closed by the server"; a red "switched off" line when `heroweapon.enabled` is false.
* Seven filter chips: All / Legendary / Seven Star / Vassal / Free / Taken / Yours. Taken covers both TAKEN and SHARED.
* A list of 7 visible rows (scroll for the rest): a class stripe (gold / violet / cyan), the weapon name, a second line "<class> - <who holds it>", a coloured status pill (FREE green, TAKEN red, YOURS gold, SHARED cyan). The player's own row is washed and outlined in gold.
* Keyboard: Left / Right or 1 to 7 pick a chip, Up / Down / Page Up / Page Down / Home / End and the wheel scroll, R or F5 refresh, Esc closes, Tab reaches the two buttons.
* Refresh: asks the server and waits 2 s (the server's gap) before it can be pressed again. Auto refresh every 10 s while open. No reply for 5 s shows "No answer from the server yet."
* **GUI Size**: registered in `GuiScaleRegistry` as `weapon_tracker` (360 x 252), so the player's GUI size setting applies; fixed (not movable) in `GuiMoveRegistry` with its reason, like the Meditation screen.
* **Open it** with `/multiverse weapons` (everyone, no permission; the server sends the reply with the "open" flag and the client opens the screen; the command node touches no client class) or with the key `key.anime_multiverse.hero_tracker` (Controls > Multiverse of Anime). **The key is unbound by default**: Z X C B N M O and the arrow keys are taken by the hero keys and curse, and the owner has many keys already. Bind it in Controls.
* A reply that is not an "open" reply only updates a tracker that is already open; a late answer never pops the screen up.

## 7. Settings

| setting | file | type | default |
|---|---|---|---|
| `heroweapon.trackerShowNames` | `serverconfig/nusmp-heroweapon-server.toml` (the core's existing file, no new file, `ConfigFileNamesTest` unaffected) | server, boolean | `true` |

It changes what the server sends (privacy), so it is a SERVER config, read through `HeroConfig.get` (default while not loaded). Nothing else is configurable: the gap (2 s), the refresh (10 s) and the caps are constants.

## 8. Tests, and what was not checked

* `TrackerRulesTest` (18 cases): all four statuses, YOURS by register and by bond, TAKEN with `holyUnique` / `vassalUnique`, SHARED with the unique setting off (holder count), names hidden (and not even looked up), one name lookup per weapon, the viewer's own row, unknown names, name cleaning and cap, filters 4 / 7 / 10 and by status, chip wrap, scroll clamp, at most one row per weapon.
* `TrackerLimiterTest` (6): one answer per gap, a hammering client does not extend the wait, players are independent, a clock set back does not lock out, forget / clear, bounded table.
* `WeaponTrackerSourceScanTest` (9): no client class in the common files, the rules stay pure, the client sends only the empty request, the request path is checked (ServerPlayer, connected, limiter before the register is read), the reply reader is bounded, no tick handler / broadcast / world scan, the setting is a server config in the core's file with no new config file, the wiring, the command has no permission and the key is unbound, every text has its lang line (fragment and `en_us.json`).
* Self-tests (CI smoke, prefix **`weapons.tracker`**): `weapons.tracker.rules` (21 rows, every status, hidden names, filters, limiter, codec round trip) and `weapons.tracker.player` (two stand-in players bind a free Vassal and a free Legendary Weapon in the real register; each sees YOURS for their own and TAKEN or SHARED for the other's by the real settings, FREE after the release, the name by `trackerShowNames`; `open` answered once and dropped the second time inside the tick). `FakePlayer`s have no connection, so no packet leaves in the test.
* **Not checked:** that the code compiles (CI is the compiler: new API uses are `StreamCodec.unit`, `FriendlyByteBuf.writeUtf(String, int)` / `readUtf(int)`, `MinecraftServer.getTickCount()`, `PacketDistributor.sendToPlayer`, the `ThemedButton` / `UiPanel` calls copied from `MeditationScreen`); that the screen looks right (spacing, text width in other languages, the GUI Size scaling) or that the hover, scroll and keys feel right; the packet on a real dedicated server with a real client; a server without the mod (the request is sent into an optional channel; the screen then says "No answer from the server yet."); no chat fallback for a player whose client lacks the mod (`/multiverse weapons` then does nothing visible).
* **Known gaps:** an offline holder's name comes from the profile cache; if the cache has forgotten them the row says "another player". Playing time of a holder, level and form are not shown (not asked). The key has no default.

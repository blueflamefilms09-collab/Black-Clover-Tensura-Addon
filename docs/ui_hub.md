# Multiverse Hub

The Multiverse Hub is the new menu that takes over from the old Codex book. It is client only and read only: the server still decides every rule, the screen draws and asks.

Nothing here has been seen in a game. The previews in `docs/previews/ui_hub/` are PIL mock-ups drawn with the real layout numbers, not game screenshots.

## What it is

A ring of eight nodes round a turning core, drawn from fills and the mod's own magic-circle textures (no new GUI texture). Click a node and the ring slides to a rail on the left and the page opens on the right.

| Node | What it does | Where it lives |
| --- | --- | --- |
| Status | Power (EP, magicule, aura), the status summary the server already sends, your active effects. | Native page, read only. |
| Weapons | The Weapon page redrawn in the hub style: weapon chips, head card, upgrade rows, mastery bars, other masteries (a longer list since 4.63.0: the other weapons, the hero weapon's other forms, Saiyan form, skills in training, each with its progress; setting `weaponpage.moreMastery`, see `docs/ui_weapon_page.md`). Same data, same packet, same rules as the old page. Scrolls, nothing overlaps at any size or count. | Native page. |
| Skills | Opens the Tensura skill screen. | Launcher (Esc comes back to the hub). |
| Quests | The quest tracker and the last dungeon map. | Native page. |
| Attunement | Sends the server's own `/meditate`; the window it opens is the old Meditation screen. | Launcher (no way back, see caveats). |
| Multiverse | Rows for Four Kingdoms, Beast Will, the compat codex, Magic and the old Status book. | Native page, rows open the old screens. |
| Codex | Opens the old Codex book. | Launcher. |
| Settings | Switches for hub effects and UI motion, "replay intro", GUI Size, GUI Move. | Native page. |

Locked features (Weapons with no weapon bound, Quests with nothing taken and no map) show a padlock and a one-line hint instead of an empty page. Every old screen the hub opens returns to the hub on Esc.

Controls: click a node, wheel or Up/Down/PageUp/PageDown/Home/End scroll, Left/Right move between the native pages, Backspace or a click on the centre goes back to the ring, Esc closes, the hub key or the Multiverse Status key closes too.

## Intro and motion

On the first open of a profile the ring builds for about 128 ticks (6.4 s): the core circle builds, the rings expand, the eight nodes light one by one, then the labels fade in; two UI sounds play. A click or any key skips it. It plays once (`ui.hubIntroSeen`), and Settings > "Replay the intro" sets it up again. After that every open is a 320 ms sweep. Click ripple 520 ms, hover lerp, 24 floating motes, slowly turning rings.

With `ui.motion` or `ui.hubFx` off the clock is frozen and the intro does not play: the same layout is drawn still.

## Keys and settings

| What | Default | Notes |
| --- | --- | --- |
| Hub key (`key.anime_multiverse.multiverse_hub`) | F7 | Rebindable under Controls. H was asked for, but H is the Djinn Rukh default and every other letter is taken, so F7 (next to GUI Size F8 and GUI Move F9). Decision for the owner. |
| `ui.hub` | true | In `nusmp-ui-client.toml`. false = exactly the old behaviour: the Multiverse Status key, the Four Kingdoms slot and the status panel open the old Codex, and the hub key does nothing. |
| `ui.hubFx` | true | Motes, intro, sweep, ripple, turning rings. |
| `ui.hubIntroSeen` | false | Set by the hub after the intro. |

All three are CLIENT config (looks only). No new config file.

## NEW / REPLACED / KEPT

NEW: `client/hub/*` (the screen, `HubLayout`, `HubNodes`, `HubText`, `HubWeaponLayout`, `HubMotes`, `HubTimeline`, `HubFx`, `HubListBody`, `HubWeaponsBody`, `HubCards`, `MultiverseHubClient`), the hub key, the three options, `tools/lang/hub.json` + `merge_hub.py`, the tests, the previews.

REPLACED (entry points only, behind `ui.hub`): what the Multiverse Status key, the Four Kingdoms slot and the status panel open: the hub instead of the Codex book.

KEPT (and, since 4.63.0, shared): `HubWeaponsBody` is now also drawn by a Weapon tab of the old Multiverse Status screen (`weaponpage.statusPage`, `docs/ui_weapon_page.md`: same body, same packet, no copy; cost: the same body in a 160 px window, one open request when the tab is switched on).

KEPT: the Codex screen and book, the Weapon page inside it, the Multiverse Status screen, Four Kingdoms, Compat codex, the Meditation screen, GUI Size / Move, the Wiki book (`CodexClient.startOn(...)` still opens the Codex), every packet, every server rule. Nothing deleted.

Appended to existing files: `UiThemeOptions` (three options and `setFlag`), `HubClient` (`lastMap`, `hudEntries()`, `lastMap()`, `mapScreen()`; `map()` now remembers the last map), `MultiverseStatusClient` and `FourKingdomsSlot` (`MultiverseHubClient.menu(...)`, plus `init` and `pollKey` called from the existing tick), `GuiMoveRegistry` (one `fixed(...)` entry).

## Rules 9 and 11: server friendliness and cost

- No server code, no new packet. Opening the Weapons page sends the one existing open request (once per hub open, only answered when the client's copy is old); picking an upgrade uses the existing validated `weaponpage_act` packet.
- No `Minecraft.getInstance()` outside `client/`; everything is under `client/hub`. No new tick handler: the hub key is polled in the status key's existing client tick.
- No work when the screen is closed: it keeps no static state except the one pending "return to hub" screen of a Tensura screen, forgotten on logout.
- Worst case per frame, ring view: at most 3 magic circles (backdrop, core, one hovered flare), at most 8 textured quads each = 24 quads; about 500 flat fills (rings 72+48, spokes ~32, 8 nodes up to ~36 each, motes 24, ripple 48). Page view: 2 circles (rail, one flare) = 16 quads, ring 72, nodes ~290, panel ~40, plus only what is visible in the scrolled body. About 60 strings at most. One scissor. No per-frame allocation: unit circles are tables, the mote pool is a fixed 24, texts are fitted once when the page, width or data changes (a status refresh every 20 ticks while Status or Quests is open: a few dozen strings, 3 a second).
- Far players: not applicable, it is a screen.

## Tests

Pure: `HubNodesTest` (enum order, append-only, lock rules), `HubLayoutTest` (window sizes from 1x1 to 3840x2160: stage inside the window, nodes and labels inside the stage, no overlaps), `HubWeaponLayoutTest` (grid of widths and counts: no overlap, inside the body), `HubMotionTest` (timeline, motes, text fit), `HubSourceScanTest` (no own tick, packet, config or texture code; the screen draws nothing before `super.render`; every lang key used has a translation). Guards: `ScreenBlurGuardTest`, `PerfSourceScanTest`, `GuiMoveRegistryTest`, `ConfigFileNamesTest`, `ServerSafetyTest` all pass. No smoke self-test: the hub is client only and the smoke test runs on a dedicated server (`hub.rules` not added; nothing to add to `smoke.yml`).

## Previews (PIL mock-ups)

`python tools/ui_preview/preview_ui.py --hub docs/previews/ui_hub` writes `hub_ring.png` (idle, Skills hovered), `hub_intro.png` (tick 70), `hub_intro_start.png` (tick 10), `hub_weapons.png` (3 weapons, upgrade rows, one cut text), `hub_weapons_scrolled.png` (mastery bars and other masteries), `hub_status.png`. Text is drawn at 6 px per character, wider than Minecraft's font, so cuts shown are worse than in game.

## Caveats

- Not seen in a game; no Gradle in the session. The hub classes, the edited entry points and all tests were compiled with plain `javac` against the real Minecraft / NeoForge / Tensura / ManasCore jars; the pure tests and guards ran with the JUnit launcher.
- Quests: the quest board itself (a server menu opened from an NPC) cannot be opened from the client, so the page shows the tracker and the last dungeon map; taking a quest still happens at the quest giver.
- Attunement: the hub closes and the server opens the Meditation window; Esc there goes to the game, not back to the hub.
- Tensura screens (Skills, Magic) have no parent argument; the hub reopens when they close to the game, through a `ScreenEvent.Closing` listener. If another mod swaps the screen in between, the hub follows the new one.
- The Wiki book still opens the old Codex.
- Lang: English only (`en_us.json`, appended through `merge_hub.py`).

## Hub v2: a framed window, custom icons, the Codex inside the hub

Asked for by the owner: the Codex book must not survive next to the hub, the node icons must be real custom icons, and the menu must not fill the whole centre of the screen.

What it is:

- **A framed window.** The 400 x 240 stage is drawn as a window of about 62% of the screen width and 70% of its height (`HubLayout.view(w, h, pct)`; at 1280 x 720 / GUI scale 2 that is exactly the 400 x 240 stage, 62% x 67%), centred, clamped so it always fits, snapped to eighths, never under a readable 0.75 scale when the screen has room. A frame is drawn around it, the dim backdrop stays, and the animated background (motes, rings, circles) is clipped to the window with the real pointer mapped back into stage units. Option `ui.hubSize` (client, 40..100, default 62; 100 = the old full-screen fit, which is the old `HubLayout.view(w, h)` unchanged). Settings page: "Window size" steps 50, 62, 75, 100.
- **Custom PNG icons.** 30 icons (32 x 32 and a `_glow` variant each, 60 PNGs): one per hub node (`node_<key>`), one per Codex page (`codex_<id>`, plus `codex_data` for any other data section) and one per Codex category (`cat_<key>`), painted by `tools/ui_icons/gen_hub_icons.py` into `textures/gui/hub/`. Drawn at 24 on the ring, 16 on the strip, 12 and 10 on the Codex rail; the glow variant is blended in for the hovered or open one. Option `ui.hubIcons` (client, default true); false draws the old two-letter glyphs as before. One class (`HubIcons`) names the textures; the lookup is a cached `ResourceLocation` per name.
- **Page view = icon strip + panel.** The old ring rail (a small ring of eight nodes) is replaced, in the page view only, by a vertical strip of the eight node icons on the left with a round back button above it (Backspace still goes back to the ring). The ring view is unchanged apart from the icons.
- **The Codex inside the hub.** The Codex node opens a page (`HubNodes.Tab.CODEX`, now native, no longer a launcher of the old screen): a left rail of every Codex page grouped into categories (You: Overview, Status, Weapon, Beast Will; Collection: Weapons, Relics, Bestiary, Spirit Lords; Magic: Spells, Magic, Effects; World: Blocks, Entities; Reference: Guide, Rules, Wiki, plus any other data section) and the picked page drawn into the body through the very same `CodexPage` objects of `CodexPages.all()` (no copy, a page registered later shows up by itself). The Multiverse page rows ("Codex", "Beast Will pages"), the Wiki book (`CodexClient.openWiki`) and the Multiverse Status key open the hub on the right page (`MultiverseHubClient.openCodex`); Esc closes the hub back to where it was opened from, Backspace goes to the ring, and links inside pages stay inside the hub.
- **The player panel.** The Status page and the Codex "You" overview show the player's own model in a panel (`HubPlayerPanel`): idle sway, a slow turn, and a cycle of poses (`HubPoseCycle`: ready, wind-up and strike with the item in the main hand when there is one, cast, aura-gather, victory) eased through the existing `UiPoses` (three poses appended: `swing_back`, `swing_through`, `aura_charge`), a follow of the mouse of up to 28 degrees, a floor circle. The figure stands still (one pose, no sway) when `ui.motion` or `ui.hubFx` is off. One entity render per frame (`UiDraw.player`, the one the old Codex used), nothing allocated per frame.

Settings (all client, in `nusmp-ui-client.toml`, no new config file): `ui.hub` (false restores the old Codex screen exactly), `ui.hubIcons`, `ui.hubSize`, plus the existing `ui.hubFx` and `ui.motion`.

### NEW / REPLACED / KEPT (Hub v2)

NEW: `HubIcons`, `HubCodexNav`, `HubCodexBody`, `HubPoseCycle`, `HubPlayerPanel`; `HubLayout` Hub v2 block (appended: `view(w, h, pct)`, `nextSize`, the strip, `PANEL2`, `PLAYER`, `STATUS_LIST`, `SUBRAIL`, `CODEX_BODY`); the 60 icon PNGs and mcmeta files, `tools/ui_icons/gen_hub_icons.py`, `tools/ui_icons/preview_hub_v2.py`; options `ui.hubIcons`, `ui.hubSize`; `tools/lang/hub_v2.json` + `merge_hub_v2.py` (15 keys); the tests below; `CodexContext.scissor`, `UiPoses.set(index, blendTicks)` and three poses (appended).

REPLACED: the Codex node of the hub (it opened the old Codex book, it now is a hosted page); the rail of eight nodes in the page view (a strip of icons); the glyphs of the nodes (icons, unless `ui.hubIcons` is false); the full-screen stage (a framed window, unless `ui.hubSize` is 100); the value of the lang key `anime_multiverse.hub.desc.codex` ("Opens the old Codex book." became "Every Codex page in one place: ..."), asked for by the owner.

KEPT: `CodexScreen`, every `CodexPage`, `CodexPages`, the Wiki and Rules data sections, the Weapon page; with `ui.hub=false` the old behaviour is exact. The old rail / glyph / full-size code paths stay (layout constants, `nodeX/nodeY(false, ...)`, the ring drawing in `HubFx`) and their tests still pass. Nothing deleted.

### Worst-case cost (Hub v2)

Client only, no server work, no packet, no tick handler. Per frame, page view: icon quads 8 nodes + 8 glow = 16 (ring view the same), up to 3 magic circles of 8 quads, one entity render (player panel, only on Status and Codex overview), about 250 flat fills, the Codex rail draws only its visible rows (about 12: an icon, a string and a chip each), the body is what the page drew in the book. One extra scissor for the window, one for the rail. Nothing allocated per frame (names cached, rows and labels cooked when the Codex opens, `HubPoseCycle.at` loops over eight steps, one `UiPoses.set` per step change). `HubSourceScanTest` fails the build if the new classes build strings, lists, int arrays or quaternions per frame.

### Tests (Hub v2)

`HubLayoutTest` (window fits any screen and snaps, 100% = the old fit, a bigger option never shrinks the window, strip nodes inside the stage and clear of the back button, clicks map back through the window, every body inside the panel without overlap), `HubNodesTest` (Codex is a native page), `HubCodexNavTest` (every page index appears once, unknown pages go to Reference, every stock title of `CodexPages` maps to its own id), `HubPoseCycleTest` (pose names exist in `UiPoses`, unarmed never shows the swing, wrap-around, clamps), `HubIconsTest` (every node, page and category has a 32 x 32 PNG, a glow PNG and an mcmeta, no stray files), `HubSourceScanTest` (appended: no per-frame allocation in the new classes). Guards run and passing: `ServerSafetyTest`, `ConfigFileNamesTest`, `PerfSourceScanTest`, `ScreenBlurGuardTest`, `GuiMoveRegistryTest`, `merge_hub.py --check`, `merge_hub_v2.py --check`.

### Previews (Hub v2, PIL mock-ups, not game screenshots)

`python3 -B tools/ui_icons/preview_hub_v2.py docs/previews/hub_v2` writes `hub_v2_ring.png` (the ring in the 62% window, Skills hovered), `hub_v2_status.png` (the Status page with the player panel), `hub_v2_codex_spells.png` (a Codex sub-page with the rail), `hub_v2_codex_weapons.png` (a catalog page) and `hub_v2_icons.png` (the icon sheet). The layout numbers and the icons are the real ones; the player, the text (6 px per character) and the page contents are stand-ins.

### Caveats and decisions (Hub v2)

- Not seen in a game. Compiled with plain `javac` against the real jars (hub, the edited Codex classes, tests); the unit tests and guards ran with the JUnit launcher, no Gradle in the session. The real player render, the pose blends and the window clipping are the parts most worth a look in game.
- The icons are drawn by a Python script (PIL, supersampled): a script-made 2D icon set, not hand-painted art; the script is the source, edit and re-run it.
- The aura pose is a stand-in (`aura_charge`): the panel does not read the Saiyan / aura state, so it plays in the cycle for everyone.
- The old Codex pages draw into 252 x 166 units (two columns of 122), narrower than the book; they were patched where they used stage-space scissors (`CodexContext.scissor`), but a long entry title may be cut earlier than in the book.
- Decisions for the owner: keep 62% as the default or go bigger (Settings steps 50 / 62 / 75 / 100); whether the Codex "You" overview should keep the old big figure or this panel (it uses the panel now); whether the ring view should also get a custom background art.

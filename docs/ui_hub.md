# Multiverse Hub

The Multiverse Hub is the new menu that takes over from the old Codex book. It is client only and read only: the server still decides every rule, the screen draws and asks.

Nothing here has been seen in a game. The previews in `docs/previews/ui_hub/` are PIL mock-ups drawn with the real layout numbers, not game screenshots.

## What it is

A ring of eight nodes round a turning core, drawn from fills and the mod's own magic-circle textures (no new GUI texture). Click a node and the ring slides to a rail on the left and the page opens on the right.

| Node | What it does | Where it lives |
| --- | --- | --- |
| Status | Power (EP, magicule, aura), the status summary the server already sends, your active effects. | Native page, read only. |
| Weapons | The Weapon page redrawn in the hub style: weapon chips, head card, upgrade rows, mastery bars, other masteries. Same data, same packet, same rules as the old page. Scrolls, nothing overlaps at any size or count. | Native page. |
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

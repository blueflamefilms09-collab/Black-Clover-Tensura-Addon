# Companion GUI in the Great Spirit style

The companion Talk, Interact, Bond and Pouch screens and the speech bubbles above companions are drawn in the great_spirit skins: B (Parchment and Ink) for normal users, C (Slime Crest) for devil users.
Client only. The server still decides everything; the screen only draws what the server sent and asks with the same two packets as before (pick, close).

## What it shows

| Tab | Content |
| --- | --- |
| Talk | Portrait of the companion (her own skin face, breathing and blink, mood expression), speaker name plate, typed dialogue with a "more" arrow, the choice list with ripple hover and the ink-to-matrix compile on a new line. |
| Interact | Large buttons with icons: high five, follow/wait toggle, growth, five-element actions, stance. |
| Bond | Rapport meter with tier leaves at 3, 10 and 25 and a glow when it rises; gift slot (click or drag an item onto the portrait or the Give button); Likes hint after the first reaction; liked, loved and hated items remembered per companion; stance selector with icons and tooltips; bond-story log of the last 6 events. |
| Pouch | 9-slot pouch (drag or click), hotbar strip, Put / Take all / Use on-off, heal-ready ring (READY, EMPTY, OFF, LOCKED). |

Mood expressions: neutral, happy, shy, angry, hungry. They come from the server's cue for the last line, never from a new packet.
Crest notices (client-only mirror of events already in chat): rapport up, tier reached, gift loved or hated, joined, left, pouch heal.

## Settings

| Option | Type | Values | Effect |
| --- | --- | --- | --- |
| `companions.screenStyle` | client | AUTO, GREAT_SPIRIT, CLASSIC | AUTO follows the theme (`ui.greatSpirit.enabled`). CLASSIC is the old plain screen, kept unchanged. |
| `ui.greatSpirit.skin` | client | AUTO, B, C | AUTO picks C for devil users, B otherwise. |
| `ui.motion` | client | on/off | Off: no breathing, blink, ripple or sparkle (static frame). |
| `ui.typing` | client | on/off | Off: the dialogue shows at once. |

Bubbles use the same skin; with the theme off or `CLASSIC` the old bubble is drawn.

## Cost and worst case

- Network: no new packet. Up (client to server): pick and close (already throttled to 5 picks a second per player on the server) and, for gifts from the inventory, the vanilla carried-slot packet and at most one vanilla SWAP click per drop.
- Server: no new work.
- Screen draw: about 200 fills and sprites a frame; at most 36 item icons (picker) or 9 pouch plus 9 hotbar icons; one compile block; no allocation per frame (one frame object and one layout object reused, texts resolved when the view arrives, layout recomputed only when the window or tab changes).
- Inventory scan: 36 slots every 5 ticks, only while the Bond (gift picker) or Pouch tab is open.
- Memory: gift memory at most 8 companions x 24 items (key length bounded); bond story at most 8 companions x 6 events (text length bounded); item-id cache cleared above 512; everything cleared on logout.
- Bubbles: at most 24 shown, drawn only inside the draw range; per bubble at most 4 lines plus name plate plus pointer (6 text draws), 2 lines beyond 12 blocks; worst case 24 x 6 = 144 draws, usually a handful. Invisible companions are skipped. Text goes through `Font.drawInBatch` (no own RenderType).

## Files

`client/companion/gs/` (pure: `GsTalkLayout`, `GsTalkFrame`, `GsTalkPainter`, `GsHit`, `GsIcons`, `GsCanvas`, `GiftMemory`, `BondStory`, `Mood`, `MoodTimer`, `CompanionCues`, `OptionKinds`, `PortraitPlan`, `FaceInfo`, `TypeWriter`, `RapportMeter`, `PouchRing`, `CompanionStyle`, `BubbleStyle`; game side: `GsTalkScreen`, `GsCanvasImpl`), `client/companion/CompanionScreens.java`, `TalkHost`, edits to `TalkScreen`, `CompanionClient`, `BubbleClient`, `GsNotices`, `GsNoticeAllow`, `GuiMoveRegistry`, `Bond` (one method made public). Lang keys: `nusmp.companion.gs.*` in `tools/lang/companions.json`.

## Tests

`CompanionGsLogicTest`, `GsTalkScreenPureTest` (layout fits 9 window sizes, hit tests, painter bounds through a counting canvas), `CompanionGsSourceScanTest`; plus the existing `CompanionLangTest`, `CompanionSourceScanTest`, `GuiMoveRegistryTest`, `ServerSafetyTest`, `PerfSourceScanTest`, `GsSourceScanTest`, `ConfigFileNamesTest`, `SafeAdditiveSourceScanTest`.

## Previews

`python3 -B tools/gui/preview_companion_gs.py` writes `docs/previews/companions/gs/`: `talk_{b,c}_{talk,interact,bond,pouch}.png`, `talk_{b,c}_sheet.png`, `picker_`, `compile_`, `typing_`, `small_bond_`, `small_pouch_`, `locked_`, `moods_`, `bubble_`.
They come from the real painter and layout with the real textures and texts; only text metrics (a wide monospace, so labels truncate more than in the game), item icons (coloured tiles) and the bubble (a mock-up of the numbers) are stand-ins.

## Not verified in a game

Nothing here has been seen in Minecraft. Gradle is unavailable here, so tests ran through a JUnit shim and a stub type check. There is no ink script font. The heal ring cannot see the server's timer (READY / EMPTY / OFF / LOCKED only). Bond story and gift memory are lost on logout. The portrait assumes the standard skin face layout and falls back to a clover crest with the initial. Windows below 200x150 are not guaranteed to fit.

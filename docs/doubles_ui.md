# Real Double stack: switch between several doubles, and see them stacked

Status: written without a running game. The code compiles against the real libraries and the pure tests, the source-scan tests and the whole sweep of 333 test files (2,725 tests) pass, but nothing here was seen in a game. `docs/doubles_ui_preview.png` is a mock-up drawn with the same sizes and colours (`python3 tools/doubles_ui/preview.py`), not a screenshot.

## What "double" was judged to mean

The owner did not say. Searching the code for everything a player can have more than one of and that is called a double:

| Candidate | Can a player have 2+? | Had a UI problem? | Judged |
|---|---|---|---|
| **Real Double** (Gauche's Mirror grimoire, `MirrorDoubleEntity`, `MirrorWorks.realDouble`): "1-3 doubles per cast, sneak-cast to trade places with the nearest one" | Yes, up to 3 at once (1 + power tier) | Yes. There was no UI for them at all: no list, no health, no time left, and the only choice was "the nearest one". With 2 or 3 out you could not tell them apart or pick which one you trade with. | **The owner's meaning.** It is the only thing the mod calls a "Double" that is a creature you own several of, and the request (switch, or stack on top of each other) fits exactly: switch = choose which one trades places, stack = see them all. |
| Avatars (`avatar/*`, up to 16 per player) | Yes (setting `maxAvatarsPerPlayer`) | No stack to fix. They have no HUD; the only UI is the Name / Info window that opens when you right-click one Avatar, and it already works per Avatar (it carries that Avatar's id, the server checks it is yours and near). Nothing overlaps and nothing shows only the first. | Not touched. Listed here so the owner can say if Avatars were meant. |
| Mystic Reflection (Wavebreaker relic) | No, one watery double at a time | No UI | Not touched |
| Reflection Double (Copy grimoire page) | A decoy page, no entity list | No UI | Not touched |
| Devil "Double Magic" | One contract holder per devil | Not a creature you have several of | Not touched |

If the owner meant Avatars, the same roster/selector pattern would carry over, but it is not built here.

## What was built

NEW (nothing existing was removed):

1. **STACK.** A small panel on the left of the screen (under the System window; movable with GUI Move, sizable with GUI Size, id `doubles_stack`). Header "Real Doubles", then one card per double:
   - the selected double on top, taller, with a gold edge;
   - the others compact below it, in order, never over each other;
   - each card: name ("Double 1"...), distance in blocks, time left (counts down on its own), a health bar and a time bar;
   - when more doubles are out than `maxRows` (default 4), the rest fold into one "+N more" line;
   - with nothing selected, all cards are compact and in order (a sneak-cast then trades with the nearest, as it always did).
2. **SWITCH.** Three keys under the mod's Controls category:
   - **Real Double: next double** (default numpad 9) and **previous double** (default numpad 8): the cycle goes none, Double 1, Double 2, Double 3, none. "None" gives the old behaviour back.
   - **Real Double: pick the double you aim at** (default numpad 0): select the double under the crosshair.
   - The selected double is the one a **sneak-cast of Real Double trades places with**. If it is out of the 48-block range or gone, the nearest one is used (the old rule).
3. Server side: `doublesui/DoublesRoster` (who owns which doubles, the selection per owner UUID), `S2C_DoublesPayload` (roster to the owner only), `C2S_DoubleSelectPayload` (next / previous / pick), `DoublesUiNet`, `DoublesUiConfig`, `DoublesUiMath` (pure cycle and layout math), `DoubleRow`.
4. Client side (all in `client/doublesui/`): `DoublesClient` (keys, layer, packet handler), `DoublesOverlay` (the stack), `DoublesView` (the last roster), `DoublesClientConfig`.
5. `MirrorDoubleEntity` got `lifeLeft()`, `lifeMax()` and joins/leaves the roster in `onAddedToLevel` / `onRemovedFromLevel`. `MirrorWorks.realDouble` asks the roster for the selected double first.

REPLACED: nothing. The old trade rule (nearest double) is kept as the fallback and as the behaviour with `selectedDoubleTrades = false`.

KEPT: Real Double spell, its cost, 1-3 doubles, the 60 s life, the old save keys (`DoubleLife`, `DoublePower`; `DoubleLifeMax` is new and optional), the Time Magic VFX (not touched).

## Settings

Client file `nusmp-doubles-ui-client.toml` (looks only):
- `doublesUi.doublesStackedUi` (default **true**): `false` = the old screen, where nothing is drawn for the doubles. The keys still work.
- `doublesUi.maxRows` (default 4, 1 to 16): cards shown before "+N more".
- `doublesUi.showDistance` (default true).

Server file `nusmp-doubles-ui-server.toml` (gameplay):
- `doublesUi.enabled` (default true): `false` = no roster packets and no selection (the old game).
- `doublesUi.selectedDoubleTrades` (default true): `false` = a sneak-cast always trades with the nearest double.
- `doublesUi.selectGapMillis` (default 100): least time between two selection requests of one player.
- `doublesUi.syncEveryTicks` (default 10): how often players who have doubles are checked for a change.

Keys (Controls, category "Multiverse of Anime"): `key.anime_multiverse.doubles_next` (numpad 9), `doubles_prev` (numpad 8), `doubles_pick` (numpad 0). Nothing else of the mod uses numpad 8, 9 or 0; all three can be rebound or unbound.

## How it is kept server-friendly (rule 9) and cheap (rule 11)

- All state is on the server: the roster and the selection are decided there, per owner UUID, and forgotten on logout, death, respawn, dimension change and server stop. The client only draws and asks.
- The packet from the client carries only an op (next / previous / pick) and an id. The sender is the connection's player, never a value in the packet. Pick by id must name one of that sender's own living doubles, in the sender's level, within 64 blocks; anything else is ignored. Selection is rate limited (100 ms by default). Unknown ops are ignored.
- The roster packet to the client is clamped on read (at most 16 rows, no NaN, no giant numbers).
- Client classes: nothing in `doublesui/` names a vanilla client class; the one reference to the client handler sits in a lambda in `DoublesUiNet` (listed in `docs/server_safe_allow.txt`, like `AvatarNet`).

Worst-case cost:
- Server per tick: one counter and one `isEmpty()` check. Every 10 ticks, only for players who currently own doubles: one pass over at most 16 entities (their health and level), no world scan, no chunk access (the doubles register themselves; `DoublesRoster` has no `getEntitiesOfClass`, which a test checks).
- Network: nothing is sent while nothing changes. A packet (about 30 bytes per double, at most 16 rows, so under 500 bytes) goes to the owner only when a double appears or goes, loses or gains 5 percent of its health, or the selection moves. At most 1 packet per 0.5 s per owner from health changes, plus at most 10 per second from key presses (the rate limit). The select packet is 17 bytes.
- Client per frame: at most 16 cards (default 4 plus one line), so about 6 filled rectangles and 3 text draws per card; up to 16 `getEntity(int)` lookups; no allocation (labels are cached, the distance and time texts are rebuilt only when the number changes). Nothing at all is drawn when you have no doubles, when `hideGui` is on, or when `doublesStackedUi` is false.
- Memory: arrays of 16 on the client; one small map entry per owner who has doubles on the server.

## Tests

- `DoublesUiMathTest` (5): the cycle wraps through "none", empty / one / many, the stack never overlaps for every count 1-16, every row limit and every selection, the selected double is always on top and shown, bars and timers stay in range.
- `DoublesUiSourceScanTest` (7): client code lives in `client/doublesui/` and is only started inside the `Dist.CLIENT` block; the common package names no client class (except the allow-listed packet registration); the roster never scans the world, rate limits, checks owner / level / range and clears on death / logout / dimension change; gameplay settings are SERVER config, the look setting is CLIENT config and defaults to true; the old nearest-double rule is still in `MirrorWorks`; strings, keys, GUI Move and GUI Size registrations exist.
- Server self-tests, smoke prefix **`doublesui`**: `doublesui.math`, `doublesui.roster`, `doublesui.codec` (the lead adds `doublesui` to `SMOKE_EXPECT`).
- Also run and passing: `ServerSafetyTest` (3), `BroadCatchScanTest` (3), `PerfSourceScanTest` (36), `ConfigFileNamesTest` (2), `GuiMoveRegistryTest` (8), `GuiScaleTest` (8), and the sweep of all 333 test files that read the repo (source scans, lang, GUI, config): 2,725 tests, 0 failed. Lang fragment: `tools/lang/doubles_ui.json` (merged), `tools/lang/gui_move.json` (one line appended).

## What is not included

- No clickable cards: the stack is a HUD, drawn without a mouse. Selection is by key (or by aiming at a double).
- Nothing marks the selected double in the world (no outline or arrow above it). The card shows it, and the pick key lets you select by looking. A world marker would need a renderer change and could not be checked without a game.
- No "orders" screen (follow / stay / attack): the doubles have no such commands today, so the selection only decides who you trade places with.
- Avatars, Mystic Reflection and the other things named above were not changed (see the table).
- Only English strings (`en_us.json`), like the other fragments.
- Not seen in a game: the exact spot on the screen (default 6 px from the left, 130 px from the top, next to the System window area), the font fit, and the feel of the keys are all unchecked. If the stack lands on top of another HUD, move it with GUI Move (it has its own entry) or change its size in GUI Size.

## Caveats

- The default keys are numpad keys because nothing else uses them; a laptop without a numpad has to bind them in Controls.
- A double only joins the roster when it enters a ticking level; one in an unloaded chunk is not listed until it loads (and a double whose owner is away shatters on its own, as before).
- Time left on the cards counts down on the client from the value in the last packet; if the server is lagging, the bar can differ from the real value by a second or two, and is only corrected when the next packet is sent (a packet is not sent for time alone, on purpose).
- When the owner is in another dimension, the doubles left behind are not listed (and shatter, as before).

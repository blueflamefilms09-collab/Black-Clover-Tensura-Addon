# The System: stat points you place yourself, and a window you move and resize with the mouse (stat-points update)

Part of the System (`docs/labyrinth/mega_system.md`, changed by `docs/labyrinth/mega_v2_sys2.md`). The owner's words: "also allow player to pick where each skill point
for the dungeons solo leveling system go, allow them to move it around with mouse not key they should also be able to change the size of it as well", and later:
"remove the key moving for the system only keep the toggle to turn on or off to move it".

Nothing here has been run in the game (section 9 lists exactly what was and was not checked).

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| The free pool `free` of unspent stat points in `SystemState` (appended last to the codec, key `free`), `PointRules`, `SystemPoints`, payloads `Allocate`, `Reset` (client to server) and `PointsSync` (server to client) | NEW |
| Server config `system.pointMode` (`choose` default, `random`) and `system.allowRespec` (default true) | NEW |
| Where a quest's stat reward goes: into the free pool in mode `choose` (`RewardRoller.rollChoose`, `RewardRoller.Kind.FREE`) | NEW; the old behaviour (the roller picks the stat) is KEPT as `pointMode = random` |
| The Shadow Core reward and the 70 / 30 dice between a stat reward and a core, `stat give`, `stat` attribute modifiers, the Hub window | KEPT unchanged |
| Command `/multiverse mega system free give|take <player> <n>` and a `Free stat points` line in `status` | NEW |
| Stat screen (`SystemStatsScreen`, `StatScreenLayout`, `ClientPoints`), key `key.nusmp.system_points` (I) | NEW |
| Badge of waiting points in the window's header (`OverlayLayout.badge`) | NEW |
| Edit screen (`SystemEditScreen`, `OverlayDrag`): drag, corner handles, wheel, nine anchors, reset, done | NEW |
| Client config `centerX`, `centerY`; `scale` range 0.5 to 2.5 | NEW keys; the range of the existing `scale` is WIDENED (it was 0.5 to 2.0) |
| Self-test `system.points`; `system.quest` and `system.cover` now accept a reward in the free pool | NEW; the two older tests are adjusted, not weakened |
| The keys that moved the window (`system_left`, `_right`, `_up`, `_down`, `_reset`, `_corner`: arrows, Home, End), `SystemEditMode`, `KeyRepeat` | REMOVED on the owner's later order (section 5) |
| The key edit mode with its pulsing frame and hint box on the HUD | REPLACED by the edit screen; the pure `OverlayLayout.edit` and its test are kept as library code |
| The edit key `key.nusmp.system_edit` (U) | KEPT, the only key left for the window: it now toggles the edit screen |
| Config keys `anchor`, `offsetX`, `offsetY`, `scale` and their meaning | KEPT, so every saved layout loads |

## 2. Stat points you choose

### Why a pool and not a stat in the reward
In 1.55.0 a cleared quest picked a stat by dice. The owner wants the player to decide, so the reward becomes "N free points" in a pool that is saved with the
player. The decision happens later, on a screen, and can be undone. `pointMode = random` keeps the old surprise for servers that want it.

### Saved format
`SystemState.Data` got a ninth component `free`; the codec field is `Codec.INT.optionalFieldOf("free", 0)`, appended after `hint`. Old saves read as 0. The order of the
`points` list and every older field is unchanged. `SystemState.of` clamps the pool to 0..`FREE_MAX` (10000). Two older constructors of `Data` are kept (7 and 8 arguments)
so every old caller compiles. `hasContent()` counts a non-empty pool, so a player who only has free points is saved.

### The rules (`PointRules`, pure, `PointRulesTest`)
* `allocate(state, stat, amount, cap, allowRespec)` moves points between the pool and ONE stat and returns the signed number that went into the stat. A positive amount
  takes from the pool, cut to what the pool holds and to the room under the cap; a negative amount (taking a point back, the mouse wheel down) works only when
  `allowRespec`. Zero, no stat, or an amount beyond +-1000 move nothing.
* `reset(state, allowRespec)` moves every placed point back into the pool.
* Both only MOVE points: the sum of the pool and the five stats never changes (a 20000-step random walk in the test checks this after every step), so no request, however
  crafted, can create or destroy a point. Even the ceiling of the pool cannot destroy one: what would not fit stays placed.
* `room(points, free, cap)` = the room the stats still have under their caps minus what the pool already holds. A reward is paid into the pool only while the room is above
  0; with no room the Shadow Core is paid instead (when the quest allows it), else nothing. This is the same fallback a capped stat has in random mode, and it stops a
  player with every stat full from collecting points that could never be spent.

### The reward (`RewardRoller.rollChoose`, `SystemRewards.grant`)
The same dice as before decide between a stat reward and a Shadow Core (the weights `statWeight` and `shadowCoreWeight`, seeded, reproducible), so the 70 / 30 split is
unchanged (tested over 30000 seeds). A stat reward is `Kind.FREE`: `points` for the pool, cut to the room. `Kind.FREE` is appended at the end of the enum.
`SystemRewards.giveFree` adds to the pool, shows the toast `REWARD: +N STAT POINT(S) TO SPEND` and syncs. `pointMode = random` runs the old `RewardRoller.roll` untouched.

### The request (`SystemPointsNet.Allocate`, `Reset`; `SystemPoints`)
The client sends only a stat index and a number. The server (`SystemPoints.allocate` / `reset`): checks the System is on, checks the index (0..4) and the amount
(1..1000 either sign), lets `PointRules` do the move within `system.maxStatPoints` and `system.allowRespec`, and only then makes it real on the SAME path the admin
command uses: `SystemStats.apply(player, state)` (the attribute modifiers, transient and gated by the dimension) and `SystemSync.request` (batched by the 10-tick
throttle). A request that moves nothing resends the stat screen's picture at the next sync slot, so a screen that shows stale numbers is corrected. Nothing the client
says is trusted; a hostile packet gets at most what a legal click would.

### What the client is told (`SystemPointsNet.PointsSync`, `ClientPoints`)
The old window packet `SystemNet.Sync` is NOT changed (its wire format is part of the contract). A second packet carries what the stat screen needs and the client cannot
know: the pool, the per-stat cap, `allowRespec`, the point mode and the effect table (the attribute, the operation, the amount per point and the ceiling, read from
`stats.json` on the server, because on a dedicated server the client has no copy of that data pack file). It is sent only when its signature (a hash of those facts)
differs from what that player was last told, and always after login, respawn and a dimension change (`requestNow` forgets the signature). All counts and strings are
bounded (32 effects, 96 characters). The stat points themselves still arrive in the old packet.

### Config (server `nusmp-labyrinth-mega-system-server.toml`, `[system]`)
| Key | Default | Meaning |
|---|---|---|
| `pointMode` | `choose` | `choose`: a quest's stat reward goes into the free pool. `random`: the System picks the stat by dice (the 1.55.0 behaviour). Any other word reads as `choose` (a plain string, never a list-validated value, which throws while the server loads) |
| `allowRespec` | `true` | players may take placed points back (the RESET button and the wheel). Free of charge. With `pointMode = random`, leaving this on lets players move their random points anywhere; set it to false for a strict random mode |

### Commands (`/multiverse mega system ...`, permission 2)
`stat give <player> <stat> <n>` is unchanged (it places points directly). New: `free give <player> <n>` and `free take <player> <n>` change the pool; `status` prints the
pool, the mode and the respec switch.

## 3. The stat screen (`SystemStatsScreen`)

Opened by `key.nusmp.system_points` (default **I**, category "Multiverse: System"; U, Y are the other two keys; O was taken by the Curse embrace key) or by the MOVE WINDOW
button of the edit screen. It does NOT open by clicking the HUD window: the window is not drawn while a screen is open and the cursor is captured the rest of the time, so
there is nothing to click (the pattern the brief calls optional does not fit).

* Five rows, STR AGI VIT INT SENSE: name, points against the cap, a small bar, and the lines "what one more point does": `+0.2 Attack Damage per point (now +1.2, at most +6)`.
  The numbers come from the server's table, written by `PointsText.amount` (`+0.2`, `+1%`). An effect on an attribute that is not registered here (Tensura's magicule pool
  without Tensura) is left out, as the server leaves it out.
* Buttons per row: **+1** (Shift-click: 5), **+5**, **MAX** (as many as fit). The footer has **RESET**, **MOVE WINDOW** and **CLOSE**. Hovering a button says what it does
  (or why it is dim: no free points, the stat is full, respec is off). The mouse wheel over a row adds a point (down takes one back when allowed; Shift: 5).
* The look is `OverlayStyle`: the dark-cyan panel, the cyan border and the four corner brackets, amber for the pool when it holds points.
* Every click sends a request; the screen shows only what the server sends back. Where each button sits is the pure `StatScreenLayout` (tested: every button inside the
  panel, none overlapping, a click finds exactly the button drawn there, the panel fits 427 x 240).
* The HUD window shows an amber pill in its header while points wait: `+3 PTS [I]` (the key as the player bound it), the short `+3` when the long form would touch the
  title, nothing when even that does not fit.

## 4. The window you move and resize with the mouse

### The edit screen (`SystemEditScreen`, the pure `OverlayDrag`)
The edit key (**U**) opens it and, pressed again, saves and closes (so does DONE and Escape). It shows the REAL window, or a labelled sample outside the dungeon, at the
place and size in the client config as they are right now.
* **Move**: press inside the window and drag. The window follows the mouse (the grabbed point stays under it) and is clamped to the screen.
* **Resize**: press on a corner handle and drag. The OPPOSITE corner stays put (`OverlayDrag.corner`); the dragged corner follows the mouse along the axis it went further
  in, so the window keeps its shape. Or scroll over the window: one notch is 0.05 (Shift: 0.25), round the point under the mouse (`OverlayDrag.wheel`).
* The size is 0.5 to 2.5 times the standard; **RESET** puts the window back in the top-left corner at 1.0; **STAT POINTS** opens the stat screen; **DONE** saves.
* The place and size are written to the client config when a drag ends and when the screen closes, never on every mouse move.

### Why nine anchors (`OverlayDrag.snap`)
The place is saved as an anchor plus an offset. With only the four corners, a window dragged to the middle would drift off-centre when the resolution or the GUI scale
changes. So an axis can now be measured from the MIDDLE of the screen instead of the corner's edge: two new client config booleans, `centerX` and `centerY`, together with
the corner bits of `anchor` make a 3 x 3 grid. When a window is dropped, `snap` picks the nearest anchor point: an axis is "middle" when the window's centre is in the middle
third of the screen, else it is the near side. The offsets are computed for that anchor, so `placeX/placeY` give back the clamped corner exactly (tested over a grid of several thousand
screens, panels and spots). A window left 40 pixels from the right edge is still 40 pixels from it on any screen; a centred one stays centred. The flags default to off, so
every saved place of the earlier versions reads exactly as before; `anchor` keeps its 0 to 3 meaning (an old build reading a new file sees the nearest corner).

### Scale everywhere
`scale` (0.5 to 2.5, default 1.0) existed in the client config since 1.55.0 and was already applied to the whole pose (text, boxes, toasts); its range is widened to 2.5
(`OverlayMath.clampScale`, `SystemClientConfig`). The window stays the same size in its own pixels at every scale; the screen size in window pixels is the GUI size divided
by the scale (`View.w()/h()`), the mouse hit box is the frame times the scale, and the pure tests check the window stays on the screen at every scale and corner. The offsets
are in the window's own pixels, so a resize that keeps a point of the window fixed rewrites the offsets for the new scale (`OverlayDrag.snapGui`).

## 5. What was removed (the owner's later order) and why nothing else was

"remove the key moving for the system only keep the toggle to turn on or off to move it." Removed: the key mappings `key.nusmp.system_left`, `_right`, `_up`, `_down`
(arrows), `_reset` (Home) and `_corner` (End) from `SystemClient` and their English texts from `SystemText`; the class `SystemEditMode` (the HUD edit mode: nudging, reset,
corner cycling, the drag over the chat and inventory); the class `KeyRepeat` and its test; the in-game hint that told players about the arrow keys (the one-time entry
message now uses `PointsText.MSG_WINDOW_HINT_MOUSE`; the old `SystemText.MSG_WINDOW_HINT` is kept, unused, because a test pins it and the lang file has it).
Kept: the toggle key Y; the edit key U (now the toggle of edit mode, which is the mouse screen); the config keys `anchor`, `offsetX`, `offsetY`, `scale`; the pure maths
`OverlayMath` (placement, nudge, clamp), `OverlayLayout.edit` and their tests (library code the screen does not call any more). The old lang entries of the removed keys stay
in `en_us.json` (not edited; harmless).

## 6. Lang

All new texts are in `PointsText` (the English the code falls back on, read through `SystemText.english`) and, identically, in `tools/system/lang_points.json` (flat key to
text; `PointsTextTest` pins that the two say the same thing, key for key). `assets/nusmp/lang/en_us.json` is not edited. Code uses `Component.translatableWithFallback`.
The key name `key.nusmp.system_points` is in the same fragment.

## 7. Files

Server (`labyrinth/mega/system`): `PointMode`, `PointRules`, `PointsText`, `SystemPointsNet`, `SystemPoints` (new); `SystemState` (+`free`, `FREE_MAX`), `SystemAttachments`
(+codec field), `RewardRoller` (+`Kind.FREE`, `rollChoose`), `SystemRewards` (+mode branch, `giveFree`), `SystemConfig` (+`POINT_MODE`, `ALLOW_RESPEC`), `SystemSync` (the
points packet in the tick, the signature in `requestNow` / `forget` / `clear`), `SystemRegistry` (+`modBus.addListener(SystemPointsNet::register)`), `SystemCommand`
(+`free`, status line), `SystemText` (`english` falls back to `PointsText`; six removed key constants), `SystemQuests.hintOnEntry` (the new hint), `SystemSelfTests`
(`system.points`).
Client (`client/mega/system`): `OverlayDrag`, `StatScreenLayout`, `ClientPoints`, `SystemStatsScreen`, `SystemEditScreen` (new); `OverlayLayout` (`View` +`centerX/centerY`,
`Panel` +`free/freeText`, the badge, placement through `OverlayDrag`), `OverlayMath.clampScale` (2.5), `SystemClientConfig` (+`centerX/centerY`, `place()`, `setPlace`,
`setScale`, range), `SystemOverlay` (the badge words, `drawPreview`, the edit-mode parts removed), `SystemClient` (keys U/I/Y, the points receiver, the removed handlers).
Tools / data: `tools/system/lang_points.json`.

## 8. Tests

Pure (all pass under the JUnit shim; real JUnit in CI): `PointRulesTest` (legal moves, cuts to the pool and the cap, a lowered cap, hostile amounts, respec on and off, reset,
the pool ceiling, `room`, a 20000-step random walk that conserves points and holds the cap, the mode parsing), `ChooseRewardTest` (the 70 / 30 split in choose mode,
determinism, zero weights, no room, cut to the room, the old roll unchanged, the saved pool, an old save, a damaged value, the order of the saved fields), `PointsTextTest`
(the fragment equals the code's map, prefixes, placeholders, number formats), `OverlayDragTest` (nine anchors, place/snap inverses over a grid, always on screen, the distance
from the edge kept across screen sizes, centred stays centred, a GUI scale change, drag lands under the mouse, the edges, scale range, wheel steps without float drift, the
pivot stays put, every corner's opposite corner stays fixed, corner direction and clamping, handle reach, an old view is unchanged), `StatScreenLayoutTest` (inside the panel,
no overlap, order, hit finding, every pixel of a button hits, the wheel's rows, width limits, compact form fits 427 x 240), `OverlayBadgeScaleTest` (no badge at 0, words or
short form, inside the panel and clear of the title, visible while it breathes, size in window pixels, hit box on screen at every scale, centred view). Changed:
`OverlayMathTest` (the top of the range is 2.5), `SystemStateTest` (the removed key constants). Removed with its class: `KeyRepeatTest`.
In game (`/multiverse mega selftest`): `system.points` round-trips the three payloads through the real stream codecs, gives a pool, allocates, checks the modifier amount
against the table, fires hostile requests (bad indices, 0, 5000, MAX and MIN integers) and checks nothing moved, cuts to the pool and to the cap, and checks respec and reset
against `allowRespec`.

## 9. Looked at, and not verified (honest list)

Type-checked against the API stubs (clean apart from the known stub artefacts "KeyMapping / FriendlyByteBuf is abstract"; a deliberate wrong call was caught): every Java file of
both packages. The pure tests were run under the JUnit shim (270 pass). NOT verified, because nothing was run in the game: every screen (that the clicks, the wheel and the drag
events arrive as written, the tooltip call, the look of the stat screen and the badge, the z of the preview over the world), the payload registration (a second `registrar("1")`
call on the same event, as other classes of the repo also do), `PacketDistributor.sendToServer` from the screens, the attribute description ids of Tensura's attributes in the effect
lines, the `ModConfigSpec.set` / `save` of the new keys, `KeyMapping.matches` to close the screens, and the key defaults against other mods (I is unused by vanilla 1.21.1 and
by this repo).
